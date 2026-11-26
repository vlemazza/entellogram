package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostYoutube;

public final class TelegramYoutubePostRenderer implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostYoutube;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostYoutube youtube = (ExtractedPostYoutube) post;
        return new TelegramMessageBuilder()
                .title(youtube.getTitle())
                .author(youtube.getAuthor(), null)
                .link(youtube.getChannel(), youtube.getChannelUrl())
                .timestamp(youtube.getTimestamp())
                .source("Source: YouTube", sourceUrl)
                .build();
    }
}
