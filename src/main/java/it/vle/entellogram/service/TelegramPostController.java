package it.vle.entellogram.service;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.TextExtractionOutcome;
import it.vle.entellocore.service.ContentExtractionEngine;
import it.vle.entellogram.presentation.TelegramMessageMapper;
import it.vle.entellogram.presentation.TelegramPostRenderer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Coordinates triggers, post extraction, and replies in Telegram chats. */
public class TelegramPostController {

    private static final Logger LOGGER = Logger.getLogger(TelegramPostController.class.getName());

    private final ContentExtractionEngine extractionEngine;
    private final TelegramBotClient telegram;
    private final TelegramRandomReplyCatalog randomReplies;
    private final Executor processingExecutor;
    private final TelegramMessageMapper messageMapper;
    private final TelegramMediaSender mediaSender;
    private final TelegramMemeService memeService;

    public TelegramPostController(
            ContentExtractionEngine extractionEngine,
            TelegramBotClient telegram,
            TelegramRandomReplyCatalog randomReplies,
            Executor processingExecutor,
            TelegramMessageMapper messageMapper,
            TelegramMediaSender mediaSender,
            TelegramMemeService memeService
    ) {
        this.extractionEngine = extractionEngine;
        this.telegram = telegram;
        this.randomReplies = randomReplies;
        this.processingExecutor = processingExecutor;
        this.messageMapper = messageMapper;
        this.mediaSender = mediaSender;
        this.memeService = memeService;
    }

    public void submitMessage(String message, long chatId, int messageId) {
        try {
            processingExecutor.execute(() -> process(message, chatId, messageId));
        } catch (RejectedExecutionException exception) {
            LOGGER.log(Level.WARNING, "Telegram processing queue is full; message ignored.", exception);
        }
    }

    private void process(String rawMessage, long chatId, int messageId) {
        if (memeService.handleIfTriggered(rawMessage, chatId, messageId)) {
            return;
        }

        if (!extractionEngine.hasSupportedUrls(rawMessage)) {
            return;
        }

        int acknowledgementId;
        try {
            acknowledgementId = telegram.sendMessage(
                    chatId, randomReplies.randomReply(), messageId
            );
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Unable to send the download acknowledgement.", exception);
            return;
        }

        DownloadAcknowledgement acknowledgement =
                new DownloadAcknowledgement(chatId, acknowledgementId);
        try {
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
                            sendPost(chatId, messageId, success.post(), success.url(), acknowledgement);
                        } catch (Exception exception) {
                            acknowledgement.delete();
                            LOGGER.log(Level.SEVERE, "Failed to send extracted post for URL: " + success.url(), exception);
                            reportPostError(
                                    chatId,
                                    messageId,
                                    "Failed to send extracted post for URL: " + success.url(),
                                    exception
                            );
                        }
                    }
                    case TextExtractionOutcome.Failure failure -> {
                        acknowledgement.delete();
                        LOGGER.log(Level.WARNING,
                                "Content extraction failed for URL: " + failure.url(), failure.exception());
                        reportPostError(
                                chatId,
                                messageId,
                                "Content extraction failed for URL: " + failure.url(),
                                failure.exception()
                        );
                    }
                }
            }
        } finally {
            acknowledgement.delete();
        }
    }

    private void reportPostError(long chatId, int sourceMessageId, String context, Throwable exception) {
        StringBuilder message = new StringBuilder(context);
        Throwable cause = exception;
        boolean first = true;
        while (cause != null) {
            message.append(first ? "\n" : "\nCaused by: ")
                    .append(cause.getClass().getName());
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                message.append(": ").append(cause.getMessage());
            }
            cause = cause.getCause();
            first = false;
        }

        try {
            telegram.sendMessage(chatId, message.toString(), sourceMessageId);
        } catch (IOException sendException) {
            LOGGER.log(Level.WARNING, "Unable to send the post processing error to Telegram.", sendException);
        }
    }

    private void sendPost(
            long chatId,
            int sourceMessageId,
            ExtractedPost post,
            String sourceUrl,
            DownloadAcknowledgement acknowledgement
    ) throws IOException {
        try {
            TelegramPostRenderer.Content presentation = messageMapper.prepare(post, sourceUrl);
            TelegramMediaSender.PreparedMedia media = mediaSender.prepare(post, sourceUrl, presentation);

            String textPost = media.hasCaptionMedia()
                    ? ""
                    : messageMapper.formatText(post, sourceUrl, presentation);
            acknowledgement.delete();
            if (!textPost.isBlank()) {
                telegram.sendHtmlMessage(chatId, textPost);
            }

            mediaSender.send(chatId, media, sourceMessageId);
        } finally {
            closePost(post);
        }
    }

    private void deleteAcknowledgement(long chatId, int messageId) {
        try {
            telegram.deleteMessage(chatId, messageId);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to remove the Telegram download acknowledgement.", exception);
        }
    }

    private final class DownloadAcknowledgement {

        private final long chatId;
        private final int messageId;
        private boolean deleted;

        private DownloadAcknowledgement(long chatId, int messageId) {
            this.chatId = chatId;
            this.messageId = messageId;
        }

        private void delete() {
            if (!deleted) {
                deleted = true;
                deleteAcknowledgement(chatId, messageId);
            }
        }
    }

    private void closePost(ExtractedPost post) {
        try {
            post.close();
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to remove temporary media files.", exception);
        }
    }

}
