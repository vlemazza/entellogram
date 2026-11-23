package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;

/** Strategia che prepara titolo e metadati per una piattaforma. */
public interface TelegramPostRenderer {

    boolean supports(ExtractedPost post);

    TelegramPostContent render(ExtractedPost post);

    default TelegramPostContent render(ExtractedPost post, String sourceUrl) {
        return render(post);
    }
}
