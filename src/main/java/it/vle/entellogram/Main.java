package it.vle.entellogram;

import com.pengrad.telegrambot.UpdatesListener;
import it.vle.entellocore.service.ContentExtractionEngine;
import it.vle.entellocore.service.EntelloContentExtractionEngine;
import it.vle.entellogram.listener.TelegramUpdateListener;
import it.vle.entellogram.service.TelegramBotClient;
import it.vle.entellogram.service.TelegramPostController;
import it.vle.entellogram.service.TelegramResponseCatalog;
import it.vle.entellogram.utils.ConfigValidator;

import java.io.IOException;
import java.nio.file.Path;
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

    public static void main(String[] args) throws IOException, InterruptedException {

        ConfigValidator.validate();

        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        TelegramBotClient telegram = new TelegramBotClient(token);

        TelegramResponseCatalog responses = new TelegramResponseCatalog(
                Path.of("config/random-replies.txt"),
                Path.of("config/meme.txt")
        );
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
                responses,
                processingExecutor
        );
        TelegramUpdateListener listener = new TelegramUpdateListener(postController);
        CountDownLatch stopped = new CountDownLatch(1);

        LOGGER.info("Telegram bot is running. Press Ctrl+C to stop.");
        // Pengrad esegue long polling e conferma gli aggiornamenti dopo averli consegnati al listener.
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
}
