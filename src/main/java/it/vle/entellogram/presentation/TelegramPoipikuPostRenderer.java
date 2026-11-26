package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostPoipiku;

public final class TelegramPoipikuPostRenderer implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostPoipiku;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostPoipiku poipiku = (ExtractedPostPoipiku) post;
        return new TelegramMessageBuilder()
                .author(poipiku.getAuthor(), null)
                .body(poipiku.getText())
                .timestamp(poipiku.getTimestamp())
                .source("Source: Poipiku", sourceUrl)
                .build();
    }
}
