package it.vle.entellogram.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Handles meme triggers, cooldowns, and audio delivery. */
public final class TelegramMemeService {

    private static final Logger LOGGER = Logger.getLogger(TelegramMemeService.class.getName());
    private static final long DEFAULT_COOLDOWN_SECONDS = 90;

    private final TelegramBotClient telegram;
    private final Path audioDirectory;
    private final List<Meme> memes;
    private final long cooldownNanos;
    private final ConcurrentHashMap<ChatMeme, Long> lastMemeByChat = new ConcurrentHashMap<>();

    public TelegramMemeService(TelegramBotClient telegram, Path memeConfigFile) throws IOException {
        this.telegram = telegram;
        this.audioDirectory = Path.of(System.getenv().getOrDefault("MEME_AUDIO_DIR", "/audio")).normalize();
        this.memes = readMemes(memeConfigFile);
        this.cooldownNanos = readCooldownNanos();
    }

    /** Handles the message if it contains a meme trigger. */
    public boolean handleIfTriggered(String input, long chatId, int sourceMessageId) {
        if (input == null || input.isBlank()) {
            return false;
        }

        String normalizedInput = input.toLowerCase(Locale.ROOT);
        Meme meme = memes.stream()
                .filter(candidate -> candidate.triggers().stream()
                        .anyMatch(trigger -> normalizedInput.contains(trigger)))
                .findFirst()
                .orElse(null);
        if (meme == null) {
            return false;
        }

        sendMeme(meme, chatId, sourceMessageId);
        return true;
    }

    private void sendMeme(Meme meme, long chatId, int sourceMessageId) {
        Path audio = audioDirectory.resolve(meme.filename()).normalize();
        if (!audio.getParent().equals(audioDirectory) || !Files.isRegularFile(audio)) {
            try {
                telegram.sendMessage(chatId, meme.filename() + " not found", sourceMessageId);
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Unable to report missing meme audio " + meme.filename(), exception);
            }
            return;
        }
        if (!acquireCooldown(chatId, meme.filename())) {
            return;
        }
        try {
            telegram.sendAudio(chatId, audio, sourceMessageId);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to send meme audio " + meme.filename(), exception);
        }
    }

    private boolean acquireCooldown(long chatId, String filename) {
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
            List<String> triggers = Arrays.stream(line.substring(separator + 1).split(","))
                    .map(String::trim)
                    .map(trigger -> trigger.toLowerCase(Locale.ROOT))
                    .filter(trigger -> !trigger.isEmpty())
                    .toList();
            if (filename.isEmpty() || filename.contains("/") || filename.contains("\\")
                    || !filename.toLowerCase(Locale.ROOT).endsWith(".opus") || triggers.isEmpty()) {
                throw new IllegalArgumentException("Invalid meme entry on line " + lineNumber + " of " + file);
            }
            entries.add(new Meme(filename, triggers));
        }
        return List.copyOf(entries);
    }

    private long readCooldownNanos() {
        String configured = System.getenv("MEME_COOLDOWN");
        double seconds = configured == null || configured.isBlank()
                ? DEFAULT_COOLDOWN_SECONDS
                : Double.parseDouble(configured);
        if (!Double.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("MEME_COOLDOWN must be a non-negative number of seconds");
        }
        return (long) (seconds * 1_000_000_000L);
    }

    private record Meme(String filename, List<String> triggers) {
    }

    private record ChatMeme(long chatId, String filename) {
    }
}
