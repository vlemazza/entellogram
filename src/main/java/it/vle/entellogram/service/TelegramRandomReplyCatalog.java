package it.vle.entellogram.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ThreadLocalRandom;

/** Loads and selects random text replies for the bot. */
public class TelegramRandomReplyCatalog {

    private static final Logger LOGGER = Logger.getLogger(TelegramRandomReplyCatalog.class.getName());
    private static final List<String> DEFAULT_REPLIES = List.of(
            "processing the link.",
            "link received. Starting the download"
    );

    private final List<String> randomReplies;

    public TelegramRandomReplyCatalog(Path randomRepliesFile) {
        this.randomReplies = loadReplies(randomRepliesFile);
    }

    private List<String> loadReplies(Path file) {
        if (file == null || !Files.isRegularFile(file) || !Files.isReadable(file)) {
            LOGGER.info("RANDOM_REPLIES is unavailable; using built-in replies.");
            return DEFAULT_REPLIES;
        }
        try {
            List<String> replies = readRandomReplies(file);
            if (replies.isEmpty()) {
                LOGGER.warning("RANDOM_REPLIES contains no entries; using built-in replies.");
                return DEFAULT_REPLIES;
            }
            return replies;
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.log(Level.WARNING, "Unable to load RANDOM_REPLIES; using built-in replies.", exception);
            return DEFAULT_REPLIES;
        }
    }

    /** Selects a phrase from the valid lines in random-replies.txt. */
    public String randomReply() {
        String reply = randomReplies.get(ThreadLocalRandom.current().nextInt(randomReplies.size()));
        if (reply.contains("{rand1}")) {
            int randomNumber = ThreadLocalRandom.current().nextInt(1, 1_000_000);
            reply = reply.replace("{rand1}", Integer.toString(randomNumber));
        }
        return reply;
    }

    private List<String> readRandomReplies(Path file) throws IOException {
        List<String> replies = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
        return replies;
    }
}
