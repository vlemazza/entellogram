package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostPinterest;

import java.util.List;

public final class TelegramPinterestPostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostPinterest;
    }

    @Override
    public TelegramPostContent render(ExtractedPost post) {
        ExtractedPostPinterest pinterest = (ExtractedPostPinterest) post;
        return new TelegramPostContent(pinterest.getTitle(), List.of());
    }
}
