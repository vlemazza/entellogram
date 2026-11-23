package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostReddit;

import java.util.List;

public class TelegramRedditPostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostReddit;
    }

    @Override
    public TelegramPostRenderer.Content render(ExtractedPost post) {
        ExtractedPostReddit reddit = (ExtractedPostReddit) post;
        return TelegramPostRenderer.withDetails(
                reddit.getTitle(),
                List.of(
                        TelegramPostRenderer.field("Subreddit", reddit.getSubreddit()),
                        TelegramPostRenderer.link("Post", "Open", reddit.getUrlExt()),
                        TelegramPostRenderer.field("Upvotes", reddit.getUpvote()),
                        TelegramPostRenderer.field("Comments", reddit.getNumComments())
                )
        );
    }
}
