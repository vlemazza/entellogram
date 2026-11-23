package it.vle.entellogram.service;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.TextExtractionOutcome;
import it.vle.entellocore.service.ContentExtractionEngine;
import it.vle.entellogram.presentation.TelegramMessageMapper;
import it.vle.entellogram.presentation.TelegramPostContent;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Coordina trigger, estrazione dei post e invio delle risposte nella chat Telegram. */
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
                        reportPostError(
                                chatId,
                                messageId,
                                "Failed to send extracted post for URL: " + success.url(),
                                exception
                        );
                    }
                }
                case TextExtractionOutcome.Failure failure -> {
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

    private void sendPost(long chatId, int sourceMessageId, ExtractedPost post, String sourceUrl) throws IOException {
        try {
            TelegramPostContent presentation = messageMapper.prepare(post, sourceUrl);
            TelegramMediaSender.PreparedMedia media = mediaSender.prepare(post, sourceUrl, presentation);

            int temporaryReplyId = telegram.sendMessage(chatId, randomReplies.randomReply(), sourceMessageId);
            try {
                String textPost = media.hasCaptionMedia()
                        ? ""
                        : messageMapper.formatText(post, sourceUrl, presentation);
                if (!textPost.isBlank()) {
                    telegram.sendHtmlMessage(chatId, textPost);
                }
            } finally {
                deleteTemporaryReply(chatId, temporaryReplyId);
            }

            mediaSender.send(chatId, media);
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

    private void closePost(ExtractedPost post) {
        try {
            post.close();
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to remove temporary media files.", exception);
        }
    }

}
