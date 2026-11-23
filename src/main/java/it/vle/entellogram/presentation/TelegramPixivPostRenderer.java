package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostPixiv;

import java.util.List;

public final class TelegramPixivPostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostPixiv;
    }

    @Override
    public TelegramPostContent render(ExtractedPost post) {
        ExtractedPostPixiv pixiv = (ExtractedPostPixiv) post;
        return new TelegramPostContent(pixiv.getTitle(), List.of());
    }
}
