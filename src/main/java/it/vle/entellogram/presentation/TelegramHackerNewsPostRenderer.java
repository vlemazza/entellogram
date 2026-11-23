package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostHackerNews;

import java.util.List;

public final class TelegramHackerNewsPostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostHackerNews;
    }

    @Override
    public TelegramPostContent render(ExtractedPost post) {
        ExtractedPostHackerNews hackerNews = (ExtractedPostHackerNews) post;
        return new TelegramPostContent(hackerNews.getTitle(), List.of(
                TelegramPostRendererSupport.field("Tipo", hackerNews.getType()),
                TelegramPostRendererSupport.field("Punteggio", hackerNews.getScore()),
                TelegramPostRendererSupport.link("Link", hackerNews.getUrl(), hackerNews.getUrl())
        ));
    }
}
