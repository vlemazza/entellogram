package it.vle.entellogram.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Loads and selects random text replies for the bot. */
public class TelegramRandomReplyCatalog {

    private final List<String> randomReplies;

    public TelegramRandomReplyCatalog(Path randomRepliesFile) throws IOException {
        this.randomReplies = readRandomReplies(randomRepliesFile);
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
        if (replies.isEmpty()) {
            throw new IllegalArgumentException("No random replies configured in " + file);
        }
        return replies;
    }
}
