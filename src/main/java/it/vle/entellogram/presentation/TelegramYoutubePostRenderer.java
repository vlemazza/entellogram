package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostYoutube;

import java.util.List;

public class TelegramYoutubePostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostYoutube;
    }

    @Override
    public TelegramPostRenderer.Content render(ExtractedPost post) {
        ExtractedPostYoutube youtubePost = (ExtractedPostYoutube) post;
        return TelegramPostRenderer.withDetails(
                null,
                List.of(TelegramPostRenderer.link("Channel", youtubePost.getChannel(), youtubePost.getChannelUrl()))
        );
    }
}