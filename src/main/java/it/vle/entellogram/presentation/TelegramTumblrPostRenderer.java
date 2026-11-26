package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostTumblr;

public final class TelegramTumblrPostRenderer implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostTumblr;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostTumblr tumblr = (ExtractedPostTumblr) post;
        return new TelegramMessageBuilder()
                .author(tumblr.getAuthor(), null)
                .body(tumblr.getText())
                .detail(tumblr.getTags() == null || tumblr.getTags().isEmpty()
                        ? null : "Tags: " + String.join(", ", tumblr.getTags()))
                .timestamp(tumblr.getTimestamp())
                .source("Source: Tumblr", sourceUrl)
                .build();
    }
}
