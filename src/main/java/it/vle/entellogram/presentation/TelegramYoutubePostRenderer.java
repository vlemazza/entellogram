package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostYoutube;

import java.util.List;

public final class TelegramYoutubePostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostYoutube;
    }

    @Override
    public TelegramPostContent render(ExtractedPost post) {
        ExtractedPostYoutube youtube = (ExtractedPostYoutube) post;
        String channel = youtube.getChannel();
        if (channel == null || channel.isBlank()) {
            channel = youtube.getAuthor();
        }
        return new TelegramPostContent(
                youtube.getTitle(),
                null,
                channel,
                "Canale",
                null,
                false,
                List.of()
        );
    }
}
