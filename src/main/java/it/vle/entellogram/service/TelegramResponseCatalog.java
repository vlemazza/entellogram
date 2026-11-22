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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/** Carica da file di testo le frasi casuali e le risposte associate ai trigger. */
public class TelegramResponseCatalog {

    // Limita a una ogni 90 secondi le risposte ai trigger per ciascuna chat.
    private static final long CUSTOM_REPLY_COOLDOWN_NANOS = TimeUnit.SECONDS.toNanos(90);

    private final List<String> randomReplies;
    private final List<TriggerReply> triggerReplies;
    private final ConcurrentHashMap<Long, Long> lastCustomReplyByChat = new ConcurrentHashMap<>();

    public TelegramResponseCatalog(Path randomRepliesFile, Path triggerRepliesFile) throws IOException {
        this.randomReplies = readRandomReplies(randomRepliesFile);
        this.triggerReplies = readTriggerReplies(triggerRepliesFile);
    }

    /** Sceglie una frase casuale tra le righe valide di random-replies.txt. */
    public String randomReply() {
        int index = ThreadLocalRandom.current().nextInt(randomReplies.size());
        return randomReplies.get(index);
    }

    public Optional<TriggerReply> findTriggerReply(String input) {
        // Se più trigger corrispondono, viene usato il primo definito nel file.
        return triggerReplies.stream()
                .filter(reply -> reply.matches(input))
                .findFirst();
    }

    public boolean acquireCustomReplyCooldown(long chatId) {
        long now = System.nanoTime();
        AtomicBoolean acquired = new AtomicBoolean();
        // compute rende atomico il controllo anche se arrivano più messaggi insieme nella stessa chat.
        lastCustomReplyByChat.compute(chatId, (ignored, previous) -> {
            if (previous == null || now - previous >= CUSTOM_REPLY_COOLDOWN_NANOS) {
                acquired.set(true);
                return now;
            }
            return previous;
        });
        return acquired.get();
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

    private List<TriggerReply> readTriggerReplies(Path file) throws IOException {
        List<TriggerReply> replies = new ArrayList<>();
        int lineNumber = 0;
        for (String rawLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Il primo | separa trigger e risposta; eventuali | nella risposta restano intatti.
            int separator = line.indexOf('|');
            if (separator <= 0 || separator == line.length() - 1) {
                throw new IllegalArgumentException(
                        "Expected trigger|reply on line " + lineNumber + " of " + file
                );
            }
            String trigger = line.substring(0, separator).trim();
            String reply = line.substring(separator + 1).trim();
            if (trigger.isEmpty() || reply.isEmpty()) {
                throw new IllegalArgumentException(
                        "Trigger and reply must be non-empty on line " + lineNumber + " of " + file
                );
            }
            replies.add(new TriggerReply(trigger, reply));
        }
        return List.copyOf(replies);
    }

    public record TriggerReply(String trigger, String reply) {
        public boolean matches(String input) {
            String normalizedInput = input.toLowerCase(Locale.ROOT);
            String normalizedTrigger = trigger.toLowerCase(Locale.ROOT);
            if (normalizedTrigger.codePoints().anyMatch(Character::isLetterOrDigit)) {
                // Per le parole usa confini lessicali, così un trigger come "ciao" non trova "ciaone".
                Pattern word = Pattern.compile(
                        "(?<![\\p{L}\\p{N}_])" + Pattern.quote(normalizedTrigger) + "(?![\\p{L}\\p{N}_])",
                        Pattern.UNICODE_CHARACTER_CLASS
                );
                return word.matcher(normalizedInput).find();
            }
            // Per emoji e altri simboli basta che il trigger compaia nel testo.
            return normalizedInput.contains(normalizedTrigger);
        }
    }
}
