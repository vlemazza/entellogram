package it.vle.entellogram;

import com.pengrad.telegrambot.UpdatesListener;
import it.vle.entellocore.service.ContentExtractionEngine;
import it.vle.entellocore.service.EntelloContentExtractionEngine;
import it.vle.entellocore.service.media.MediaProcessor;
import it.vle.entellogram.listener.TelegramUpdateListener;
import it.vle.entellogram.presentation.TelegramMessageMapper;
import it.vle.entellogram.presentation.TelegramMessageFormatter;
import it.vle.entellogram.service.TelegramBotClient;
import it.vle.entellogram.service.TelegramMediaSender;
import it.vle.entellogram.service.TelegramMemeService;
import it.vle.entellogram.service.TelegramPostController;
import it.vle.entellogram.service.TelegramRandomReplyCatalog;
import it.vle.entellogram.utils.ConfigValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final int PROCESSING_WORKERS = 2;
    private static final int PROCESSING_QUEUE_CAPACITY = 100;

    static void main() throws InterruptedException {

        ConfigValidator.validate();

        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        TelegramBotClient telegram = new TelegramBotClient(token);

        Path randomRepliesPath = optionalPath("RANDOM_REPLIES");
        Path memeTextPath = optionalPath("MEME_TEXT");

        TelegramRandomReplyCatalog randomReplies = new TelegramRandomReplyCatalog(randomRepliesPath);
        TelegramMemeService memeService = new TelegramMemeService(telegram, memeTextPath);

        TelegramMessageMapper messageMapper = new TelegramMessageMapper();
        TelegramMessageFormatter messageFormatter = new TelegramMessageFormatter();
        TelegramMediaSender mediaSender = new TelegramMediaSender(telegram, messageFormatter, new MediaProcessor());
        ContentExtractionEngine extractionEngine = new EntelloContentExtractionEngine();

        ExecutorService processingExecutor = new ThreadPoolExecutor(
                PROCESSING_WORKERS,
                PROCESSING_WORKERS,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(PROCESSING_QUEUE_CAPACITY)
        );
        TelegramPostController postController = new TelegramPostController(
                extractionEngine,
                telegram,
                randomReplies,
                processingExecutor,
                messageMapper,
                messageFormatter,
                mediaSender,
                memeService
        );
        TelegramUpdateListener listener = new TelegramUpdateListener(postController);
        CountDownLatch stopped = new CountDownLatch(1);

        LOGGER.info("Telegram bot is running. Press Ctrl+C to stop.");
        // Pengrad uses long polling and confirms updates after delivering them to the listener.
        telegram.setUpdatesListener(updates -> {
            updates.forEach(listener::onUpdate);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, exception -> {
            if (exception.response() != null) {
                LOGGER.warning("Telegram polling error " + exception.response().errorCode()
                        + ": " + exception.response().description());
            } else {
                LOGGER.log(Level.WARNING, "Telegram polling failed.", exception);
            }
        });
        stopped.await();
    }

    private static Path optionalPath(String environmentVariable) {
        String configuredPath = System.getenv(environmentVariable);
        if (configuredPath == null || configuredPath.isBlank()) {
            return null;
        }
        try {
            return Path.of(configuredPath.trim());
        } catch (InvalidPathException exception) {
            LOGGER.log(Level.WARNING, "Ignoring invalid optional path in " + environmentVariable + ".", exception);
            return null;
        }
    }
}
