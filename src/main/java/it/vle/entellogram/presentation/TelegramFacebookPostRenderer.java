package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostFacebook;

/** Facebook-specific message composition. */
public final class TelegramFacebookPostRenderer implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostFacebook;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostFacebook facebook = (ExtractedPostFacebook) post;
        return new TelegramMessageBuilder()
                .author(facebook.getAuthor(), null)
                .body(facebook.getText())
                .detail(facebook.isVideo() ? "Video" : null)
                .timestamp(facebook.getTimestamp())
                .source("Source: Facebook", sourceUrl)
                .build();
    }
}
