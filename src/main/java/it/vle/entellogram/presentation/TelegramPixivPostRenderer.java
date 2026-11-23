package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostPixiv;

public class TelegramPixivPostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostPixiv;
    }

    @Override
    public TelegramPostRenderer.Content render(ExtractedPost post) {
        return TelegramPostRenderer.withDetails(((ExtractedPostPixiv) post).getTitle(), java.util.List.of());
    }
}
