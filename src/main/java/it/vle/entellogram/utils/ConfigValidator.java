package it.vle.entellogram.utils;

import it.vle.entellocore.utils.CoreConfigValidator;

public final class ConfigValidator {

    private ConfigValidator() {}

    public static void validate() {
        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing TELEGRAM_BOT_TOKEN");
        }
        CoreConfigValidator.validate();
        System.out.println("Telegram configuration OK");
    }
}
