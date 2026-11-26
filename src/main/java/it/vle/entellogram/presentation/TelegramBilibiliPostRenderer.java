package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostBilibili;

public final class TelegramBilibiliPostRenderer implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostBilibili;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostBilibili bilibili = (ExtractedPostBilibili) post;
        return new TelegramMessageBuilder()
                .author(bilibili.getAuthor(), null)
                .body(bilibili.getText())
                .timestamp(bilibili.getTimestamp())
                .source("Source: Bilibili", sourceUrl)
                .build();
    }
}
