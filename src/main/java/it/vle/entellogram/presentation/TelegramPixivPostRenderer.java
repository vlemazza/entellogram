package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostPixiv;

public final class TelegramPixivPostRenderer implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostPixiv;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostPixiv pixiv = (ExtractedPostPixiv) post;
        return new TelegramMessageBuilder().title(pixiv.getTitle())
                .author(pixiv.getAuthor(), null)
                .body(pixiv.getText())
                .timestamp(pixiv.getTimestamp())
                .source("Source: Pixiv", sourceUrl)
                .build();
    }
}
