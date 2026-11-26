package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostTiktok;

public final class TelegramTiktokPostRender implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostTiktok;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostTiktok tiktok = (ExtractedPostTiktok) post;
        return new TelegramMessageBuilder()
                .author(tiktok.getAuthor(), tiktok.getUrlProfile())
                .body(tiktok.getText())
                .timestamp(tiktok.getTimestamp())
                .source("Source: TikTok", sourceUrl)
                .build();
    }
}
