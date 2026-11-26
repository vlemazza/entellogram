package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostHackerNews;

public final class TelegramHackerNewsPostRenderer implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostHackerNews;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostHackerNews hn = (ExtractedPostHackerNews) post;
        boolean hasDestination = present(hn.getUrl());
        return new TelegramMessageBuilder()
                .title(hn.getTitle(), hasDestination ? hn.getUrl() : null)
                .space()
                .author(hn.getAuthor(), null)
                .body(hn.getText())
                .source("Source: Hacker News", hasDestination ? sourceUrl : null)
                .build();
    }

    private boolean present(String value) { return value != null && !value.isBlank(); }
}
