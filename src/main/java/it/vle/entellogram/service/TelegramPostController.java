package it.vle.entellogram.service;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.TextExtractionOutcome;
import it.vle.entellocore.service.ContentExtractionEngine;
import it.vle.entellocore.service.media.MediaProcessor;
import it.vle.entellogram.presentation.TelegramMessageMapper;
import it.vle.entellogram.presentation.TelegramPostContent;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Coordina trigger, estrazione dei post e invio delle risposte nella chat Telegram. */
public class TelegramPostController {

    private static final Logger LOGGER = Logger.getLogger(TelegramPostController.class.getName());
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;
    private static final long DEFAULT_MEME_COOLDOWN_SECONDS = 3_600;

    private final ContentExtractionEngine extractionEngine;
    private final TelegramBotClient telegram;
    private final TelegramResponseCatalog responses;
    private final TelegramMessageMapper messageMapper = new TelegramMessageMapper();
    private final Executor processingExecutor;
    private final MediaProcessor mediaProcessor;
    private final Path memeAudioDirectory;
    private final long memeCooldownNanos;

    public TelegramPostController(
            ContentExtractionEngine extractionEngine,
            TelegramBotClient telegram,
            TelegramResponseCatalog responses,
            Executor processingExecutor
    ) {
        this(extractionEngine, telegram, responses, processingExecutor, new MediaProcessor());
    }

    public TelegramPostController(
            ContentExtractionEngine extractionEngine,
            TelegramBotClient telegram,
            TelegramResponseCatalog responses,
            Executor processingExecutor,
            MediaProcessor mediaProcessor
    ) {
        this.extractionEngine = extractionEngine;
        this.telegram = telegram;
        this.responses = responses;
        this.processingExecutor = processingExecutor;
        this.mediaProcessor = mediaProcessor;
        this.memeAudioDirectory = Path.of(System.getenv().getOrDefault("MEME_AUDIO_DIR", "/audio"));
        this.memeCooldownNanos = readMemeCooldownNanos();
    }

    public void submitMessage(String message, long chatId, int messageId) {
        try {
            processingExecutor.execute(() -> process(message, chatId, messageId));
        } catch (RejectedExecutionException exception) {
            LOGGER.log(Level.WARNING, "Telegram processing queue is full; message ignored.", exception);
        }
    }

    private void process(String rawMessage, long chatId, int messageId) {
        var meme = responses.findMeme(rawMessage);
        if (meme.isPresent()) {
            sendMeme(meme.get(), chatId, messageId);
            return;
        }

        List<TextExtractionOutcome> outcomes;
        try {
            outcomes = extractionEngine.extractFromText(rawMessage);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Unexpected error processing Telegram message.", exception);
            return;
        }

        for (TextExtractionOutcome outcome : outcomes) {
            switch (outcome) {
                case TextExtractionOutcome.Success success -> {
                    try {
                        sendPost(chatId, messageId, success.post(), success.url());
                    } catch (Exception exception) {
                        LOGGER.log(Level.SEVERE, "Failed to send extracted post for URL: " + success.url(), exception);
                    }
                }
                case TextExtractionOutcome.Failure failure ->
                        LOGGER.log(Level.WARNING, "Content extraction failed for URL: " + failure.url(), failure.exception());
            }
        }
    }

    private void sendMeme(TelegramResponseCatalog.Meme meme, long chatId, int sourceMessageId) {
        Path audio = memeAudioDirectory.resolve(meme.filename()).normalize();
        if (!audio.getParent().equals(memeAudioDirectory.normalize()) || !Files.isRegularFile(audio)) {
            try {
                telegram.sendMessage(chatId, meme.filename() + " not found", sourceMessageId);
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Unable to report missing meme audio " + meme.filename(), exception);
            }
            return;
        }
        if (!responses.acquireMemeCooldown(chatId, meme.filename(), memeCooldownNanos)) {
            return;
        }
        try {
            telegram.sendAudio(chatId, audio, sourceMessageId);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to send meme audio " + meme.filename(), exception);
        }
    }

    private long readMemeCooldownNanos() {
        String configured = System.getenv("MEME_COOLDOWN");
        double seconds = configured == null || configured.isBlank()
                ? DEFAULT_MEME_COOLDOWN_SECONDS
                : Double.parseDouble(configured);
        if (!Double.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("MEME_COOLDOWN must be a non-negative number of seconds");
        }
        return (long) (seconds * 1_000_000_000L);
    }

    private void sendPost(long chatId, int sourceMessageId, ExtractedPost post, String sourceUrl) throws IOException {
        try {
            TelegramPostContent presentation = messageMapper.prepare(post, sourceUrl);

            if (!post.hasNoMedia()) {
                mediaProcessor.process(post, MAX_FILE_SIZE_BYTES);
            }

            List<Path> media = orderMedia(post.getMedia());
            Path captionMedia = findCaptionMedia(media);

            int temporaryReplyId = telegram.sendMessage(chatId, responses.randomReply(), sourceMessageId);
            try {
                String textPost = captionMedia == null
                        ? messageMapper.formatText(post, sourceUrl, presentation)
                        : "";
                if (!textPost.isBlank()) {
                    telegram.sendHtmlMessage(chatId, textPost);
                }
            } finally {
                deleteTemporaryReply(chatId, temporaryReplyId);
            }

            if (!post.hasNoMedia()) {
                if (media.size() > 1 && canSendAsMediaGroup(media)) {
                    String caption = captionMedia == null
                            ? null
                            : messageMapper.formatMediaCaption(post, sourceUrl, presentation);
                    telegram.sendMediaGroup(chatId, media, caption);
                } else {
                    for (Path file : media) {
                        if (file.equals(captionMedia)) {
                            telegram.sendFile(chatId, file,
                                    messageMapper.formatMediaCaption(post, sourceUrl, presentation));
                        } else {
                            telegram.sendFile(chatId, file);
                        }
                    }
                }
            }
        } finally {
            closePost(post);
        }
    }

    private void deleteTemporaryReply(long chatId, int messageId) {
        try {
            telegram.deleteMessage(chatId, messageId);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to remove the temporary Telegram reply before sending media.", exception);
        }
    }

    private Path findCaptionMedia(List<Path> mediaFiles) throws IOException {
        if (mediaFiles.isEmpty()) {
            return null;
        }
        for (Path media : mediaFiles) {
            if (isVideo(media) || isImage(media)) {
                return media;
            }
        }
        return null;
    }

    private boolean isVideo(Path media) throws IOException {
        String contentType = Files.probeContentType(media);
        if (contentType != null && contentType.startsWith("video/")) {
            return true;
        }
        String filename = media.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return filename.endsWith(".mp4") || filename.endsWith(".mov")
                || filename.endsWith(".m4v") || filename.endsWith(".webm");
    }

    private boolean isImage(Path media) throws IOException {
        String contentType = Files.probeContentType(media);
        String filename = media.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return (contentType != null && contentType.startsWith("image/") && !contentType.equals("image/gif"))
                || filename.endsWith(".jpg") || filename.endsWith(".jpeg")
                || filename.endsWith(".png") || filename.endsWith(".webp");
    }

    private boolean canSendAsMediaGroup(List<Path> media) throws IOException {
        for (Path path : media) {
            if (!isImage(path) && !isVideo(path)) {
                return false;
            }
        }
        return true;
    }

    /** Riordina per indice solo quando ogni file espone un indice finale esplicito. */
    private List<Path> orderMedia(List<Path> originalMedia) {
        if (originalMedia == null || originalMedia.size() < 2) {
            return originalMedia == null ? List.of() : List.copyOf(originalMedia);
        }

        List<Path> ordered = new ArrayList<>(originalMedia);
        if (ordered.stream().allMatch(path -> trailingSequenceNumber(path.getFileName().toString()) != null)) {
            ordered.sort(Comparator.comparing(path -> trailingSequenceNumber(path.getFileName().toString())));
        }
        return List.copyOf(ordered);
    }

    private BigInteger trailingSequenceNumber(String filename) {
        int extensionStart = filename.lastIndexOf('.');
        if (extensionStart <= 0) {
            return null;
        }
        int separator = filename.lastIndexOf('_', extensionStart);
        if (separator < 0 || separator == extensionStart - 1) {
            return null;
        }
        String suffix = filename.substring(separator + 1, extensionStart);
        if (!suffix.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return new BigInteger(suffix);
    }

    private void closePost(ExtractedPost post) {
        try {
            post.close();
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to remove temporary media files.", exception);
        }
    }

}
