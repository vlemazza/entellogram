package it.vle.entellogram.service;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.TextExtractionOutcome;
import it.vle.entellocore.service.ContentExtractionEngine;
import it.vle.entellocore.service.media.MediaProcessor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Coordina trigger, estrazione dei post e invio delle risposte nella chat Telegram. */
public class TelegramPostController {

    private static final Logger LOGGER = Logger.getLogger(TelegramPostController.class.getName());
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;

    private final ContentExtractionEngine extractionEngine;
    private final TelegramBotClient telegram;
    private final TelegramResponseCatalog responses;
    private final Executor processingExecutor;
    private final MediaProcessor mediaProcessor;

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
    }

    public void submitMessage(String message, long chatId, int messageId) {
        try {
            // Lavoro asincrono per lasciare libero il thread che riceve gli update.
            processingExecutor.execute(() -> process(message, chatId, messageId));
        } catch (RejectedExecutionException exception) {
            LOGGER.log(Level.WARNING, "Telegram processing queue is full; message ignored.", exception);
        }
    }

    private void process(String rawMessage, long chatId, int messageId) {
        // I trigger personalizzati hanno precedenza sull'estrazione dei link.
        var customReply = responses.findTriggerReply(rawMessage);
        if (customReply.isPresent()) {
            if (responses.acquireCustomReplyCooldown(chatId)) {
                try {
                    telegram.sendMessage(chatId, customReply.get().reply(), messageId);
                } catch (Exception exception) {
                    LOGGER.log(Level.WARNING, "Unable to send custom Telegram response.", exception);
                }
            }
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
                        sendPost(chatId, messageId, success.post());
                    } catch (Exception exception) {
                        LOGGER.log(Level.SEVERE, "Failed to send extracted post for URL: " + success.url(), exception);
                    }
                }
                case TextExtractionOutcome.Failure failure ->
                        LOGGER.log(Level.WARNING, "Content extraction failed for URL: " + failure.url(), failure.exception());
            }
        }
    }

    private void sendPost(long chatId, int sourceMessageId, ExtractedPost post) throws IOException {
        try {
            // Comprimi i video troppo grandi prima di inviare qualsiasi risposta all'utente.
            if (!post.hasNoMedia()) {
                mediaProcessor.process(post, MAX_FILE_SIZE_BYTES);
            }

            // La frase casuale fa da conferma temporanea e risponde direttamente al link.
            int temporaryReplyId = telegram.sendMessage(chatId, responses.randomReply(), sourceMessageId);
            try {
                String text = formatPost(post);
                if (!text.isBlank()) {
                    telegram.sendMessage(chatId, text);
                }
            } finally {
                deleteTemporaryReply(chatId, temporaryReplyId);
            }

            // Rimuove la conferma prima di pubblicare gli allegati, riducendo i messaggi superflui.
            if (!post.hasNoMedia()) {
                for (Path file : post.getMedia()) {
                    telegram.sendFile(chatId, file);
                }
            }
        } finally {
            // I file temporanei creati durante l'estrazione vanno rimossi anche in caso di errore.
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

    private String formatPost(ExtractedPost post) {
        String author = post.getAuthor();
        String text = post.getText();
        if (author == null || author.isBlank()) {
            return text == null ? "" : text;
        }
        if (text == null || text.isBlank()) {
            return author;
        }
        return author + "\n\n" + text;
    }

    private void closePost(ExtractedPost post) {
        try {
            post.close();
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to remove temporary media files.", exception);
        }
    }
}
