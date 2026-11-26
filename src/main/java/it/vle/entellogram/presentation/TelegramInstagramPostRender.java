package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostInstagram;

public final class TelegramInstagramPostRender implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostInstagram;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostInstagram instagram = (ExtractedPostInstagram) post;
        return new TelegramMessageBuilder()
                .author(instagram.getAuthor(), instagram.getUrlProfile())
                .body(instagram.getText())
                .timestamp(instagram.getTimestamp())
                .source("Source: Instagram", sourceUrl)
                .build();
    }
}
