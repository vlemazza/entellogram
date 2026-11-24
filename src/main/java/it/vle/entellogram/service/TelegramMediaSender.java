package it.vle.entellogram.service;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.service.media.MediaProcessor;
import it.vle.entellogram.presentation.TelegramMessageMapper;
import it.vle.entellogram.presentation.TelegramPostRenderer;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/** Prepares and sends post media while preserving albums, captions, and file order. */
public final class TelegramMediaSender {

    private static final Logger LOGGER = Logger.getLogger(TelegramMediaSender.class.getName());
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;

    private final TelegramBotClient telegram;
    private final TelegramMessageMapper messageMapper;
    private final MediaProcessor mediaProcessor;

    public TelegramMediaSender(
            TelegramBotClient telegram,
            TelegramMessageMapper messageMapper,
            MediaProcessor mediaProcessor
    ) {
        this.telegram = telegram;
        this.messageMapper = messageMapper;
        this.mediaProcessor = mediaProcessor;
    }

    public PreparedMedia prepare(
            ExtractedPost post,
            String sourceUrl,
            TelegramPostRenderer.Content presentation
    )
            throws IOException {
        if (!post.hasNoMedia()) {
            mediaProcessor.process(post, MAX_FILE_SIZE_BYTES);
        }

        List<Path> media = orderMedia(post.getMedia());
        Path captionMedia = findCaptionMedia(media);
        String caption = captionMedia == null
                ? null
                : messageMapper.formatCaption(post, sourceUrl, presentation);
        return new PreparedMedia(media, captionMedia, caption);
    }

    public void send(long chatId, PreparedMedia prepared) throws IOException {
        List<Path> media = prepared.files();
        if (media.isEmpty()) {
            return;
        }
        if (media.size() > 1 && canSendAsMediaGroup(media)) {
            telegram.sendMediaGroup(chatId, media, prepared.caption());
            return;
        }

        for (Path file : media) {
            if (file.equals(prepared.captionMedia())) {
                telegram.sendFile(chatId, file, prepared.caption());
            } else {
                telegram.sendFile(chatId, file);
            }
        }
    }

    private Path findCaptionMedia(List<Path> mediaFiles) throws IOException {
        for (Path media : mediaFiles) {
            if (TelegramMediaType.from(media) != TelegramMediaType.OTHER) {
                return media;
            }
        }
        return null;
    }

    private boolean canSendAsMediaGroup(List<Path> media) throws IOException {
        for (Path path : media) {
            if (TelegramMediaType.from(path) == TelegramMediaType.OTHER) {
                return false;
            }
        }
        return true;
    }

    /** Sorts by index only when every file has an explicit trailing index. */
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

    public record PreparedMedia(List<Path> files, Path captionMedia, String caption) {
        public PreparedMedia {
            files = files == null ? List.of() : List.copyOf(files);
        }

        public boolean hasCaptionMedia() {
            return captionMedia != null;
        }
    }
}
