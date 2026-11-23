package it.vle.entellogram.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/** Carica da file le frasi casuali e le associazioni tra trigger e meme audio. */
public class TelegramResponseCatalog {

    private final List<String> randomReplies;
    private final List<Meme> memes;
    private final ConcurrentHashMap<ChatMeme, Long> lastMemeByChat = new ConcurrentHashMap<>();

    public TelegramResponseCatalog(Path randomRepliesFile, Path memesFile) throws IOException {
        this.randomReplies = readRandomReplies(randomRepliesFile);
        this.memes = readMemes(memesFile);
    }

    /** Sceglie una frase casuale tra le righe valide di random-replies.txt. */
    public String randomReply() {
        int index = ThreadLocalRandom.current().nextInt(randomReplies.size());
        String reply = randomReplies.get(index);
        if (reply.contains("{rand1}")) {
            int randomNumber = ThreadLocalRandom.current().nextInt(1, 1_000_000);
            reply = reply.replace("{rand1}", Integer.toString(randomNumber));
        }
        return reply;
    }

    public Optional<Meme> findMeme(String input) {
        String normalizedInput = input.toLowerCase(Locale.ROOT);
        return memes.stream().filter(meme -> meme.triggers().stream()
                .anyMatch(trigger -> normalizedInput.contains(trigger.toLowerCase(Locale.ROOT))))
                .findFirst();
    }

    public boolean acquireMemeCooldown(long chatId, String filename, long cooldownNanos) {
        long now = System.nanoTime();
        ChatMeme key = new ChatMeme(chatId, filename);
        AtomicBoolean acquired = new AtomicBoolean();
        lastMemeByChat.compute(key, (ignored, previous) -> {
            if (previous == null || now - previous >= cooldownNanos) {
                acquired.set(true);
                return now;
            }
            return previous;
        });
        return acquired.get();
    }

    private List<Meme> readMemes(Path file) throws IOException {
        List<Meme> entries = new ArrayList<>();
        int lineNumber = 0;
        for (String rawLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('|');
            if (separator <= 0 || separator == line.length() - 1) {
                throw new IllegalArgumentException("Expected filename|trigger,trigger on line "
                        + lineNumber + " of " + file);
            }
            String filename = line.substring(0, separator).trim();
            List<String> triggers = java.util.Arrays.stream(line.substring(separator + 1).split(","))
                    .map(String::trim).filter(trigger -> !trigger.isEmpty()).toList();
            if (filename.isEmpty() || filename.contains("/") || filename.contains("\\")
                    || !filename.toLowerCase(Locale.ROOT).endsWith(".opus") || triggers.isEmpty()) {
                throw new IllegalArgumentException("Invalid meme entry on line " + lineNumber + " of " + file);
            }
            entries.add(new Meme(filename, triggers));
        }
        return List.copyOf(entries);
    }

    private List<String> readRandomReplies(Path file) throws IOException {
        // Le righe vuote e quelle che iniziano con # servono solo come spaziatura e commenti.
        List<String> replies = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
        if (replies.isEmpty()) {
            throw new IllegalArgumentException("No random replies configured in " + file);
        }
        return replies;
    }

    public record Meme(String filename, List<String> triggers) { }

    private record ChatMeme(long chatId, String filename) { }
}
