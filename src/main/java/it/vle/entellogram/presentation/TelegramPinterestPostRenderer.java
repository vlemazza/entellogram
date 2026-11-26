package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostPinterest;

public final class TelegramPinterestPostRenderer implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostPinterest;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostPinterest pinterest = (ExtractedPostPinterest) post;
        return new TelegramMessageBuilder()
                .title(pinterest.getTitle())
                .author(pinterest.getAuthor(), null)
                .body(pinterest.getText())
                .timestamp(pinterest.getTimestamp())
                .source("Source: Pinterest", sourceUrl)
                .build();
    }
}
