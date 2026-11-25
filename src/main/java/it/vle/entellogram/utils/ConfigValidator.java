package it.vle.entellogram.utils;

import it.vle.entellocore.utils.CoreConfigValidator;

public final class ConfigValidator {

    private ConfigValidator() {}

    public static void validate() {
        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing TELEGRAM_BOT_TOKEN");
        }
        String randomReplies = System.getenv("RANDOM_REPLIES");
        if (randomReplies == null || randomReplies.isBlank()) {
            throw new IllegalStateException("Missing RANDOM_REPLIES ");
        }
        String memeText = System.getenv("MEME_TEXT");
        if (memeText == null || memeText.isBlank()) {
            throw new IllegalStateException("Missing MEME_TEXT");
        }

        CoreConfigValidator.validate();
        System.out.println("Telegram configuration OK");
    }
}
