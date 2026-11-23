package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostTumblr;

import java.util.List;

public final class TelegramTumblrPostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostTumblr;
    }

    @Override
    public TelegramPostContent render(ExtractedPost post) {
        ExtractedPostTumblr tumblr = (ExtractedPostTumblr) post;
        return new TelegramPostContent("", List.of(
                TelegramPostRendererSupport.field("Tags", TelegramPostRendererSupport.join(tumblr.getTags()))
        ));
    }
}
