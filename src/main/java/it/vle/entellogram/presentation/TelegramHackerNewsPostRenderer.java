package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostHackerNews;

import java.util.List;

public class TelegramHackerNewsPostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostHackerNews;
    }

    @Override
    public TelegramPostRenderer.Content render(ExtractedPost post) {
        ExtractedPostHackerNews hackerNews = (ExtractedPostHackerNews) post;
        return TelegramPostRenderer.withDetails(
                hackerNews.getTitle(),
                List.of(
                        TelegramPostRenderer.link("Story", "Open", hackerNews.getUrl()),
                        TelegramPostRenderer.field("Type", hackerNews.getType()),
                        TelegramPostRenderer.field("Score", hackerNews.getScore())
                )
        );
    }
}