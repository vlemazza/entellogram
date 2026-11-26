package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;

public interface TelegramPostRenderer {

    boolean supports(ExtractedPost post);

    TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context);
}
