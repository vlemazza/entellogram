package it.vle.entellogram.utils;

import it.vle.entellocore.utils.CoreConfigValidator;

public final class ConfigValidator {

    private ConfigValidator() {}

    public static void validate() {
        // Il token viene fornito dall'ambiente e non va salvato nel codice sorgente.
        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing TELEGRAM_BOT_TOKEN");
        }

        // Controlla anche gallery-dl, yt-dlp, ffmpeg e la configurazione di entellocore.
        CoreConfigValidator.validate();
        System.out.println("Telegram configuration OK");
    }
}
