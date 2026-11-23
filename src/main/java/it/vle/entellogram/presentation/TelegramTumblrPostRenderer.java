package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostTumblr;

import java.util.List;

public class TelegramTumblrPostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostTumblr;
    }

    @Override
    public TelegramPostRenderer.Content render(ExtractedPost post) {
        ExtractedPostTumblr tumblr = (ExtractedPostTumblr) post;
        return TelegramPostRenderer.withDetails(
                null,
                List.of(TelegramPostRenderer.field(
                        "Tags",
                        TelegramPostRenderer.join(tumblr.getTags())
                ))
        );
    }
}
