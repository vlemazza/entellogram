package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostReddit;

import java.util.ArrayList;
import java.util.List;

public class TelegramRedditPostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostReddit;
    }

    @Override
    public TelegramPostRenderer.Content render(ExtractedPost post) {
        ExtractedPostReddit reddit = (ExtractedPostReddit) post;

        List<TelegramPostRenderer.Field> fields = new ArrayList<>();

        fields.add(TelegramPostRenderer.field("Subreddit", reddit.getSubreddit()));

        if (reddit.getUrlExt() != null && !reddit.getUrlExt().isBlank()) {
            fields.add(TelegramPostRenderer.link("External", "Link", reddit.getUrlExt()));
        }

        fields.add(TelegramPostRenderer.field("Upvotes", reddit.getUpvote()));
        fields.add(TelegramPostRenderer.field("Comments", reddit.getNumComments()));

        return new TelegramPostRenderer.Content(
                reddit.getTitle(),
                null,
                null,
                null,
                null,
                false,
                fields,
                true
        );
    }
}
