package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostReddit;

import java.util.List;

public final class TelegramRedditPostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostReddit;
    }

    @Override
    public TelegramPostContent render(ExtractedPost post) {
        ExtractedPostReddit reddit = (ExtractedPostReddit) post;
        return new TelegramPostContent(reddit.getTitle(), List.of(
                TelegramPostRendererSupport.field("Subreddit", reddit.getSubreddit()),
                TelegramPostRendererSupport.field("Upvote", reddit.getUpvote()),
                TelegramPostRendererSupport.field("Commenti", reddit.getNumComments()),
                TelegramPostRendererSupport.link("Link", reddit.getUrlExt(), reddit.getUrlExt())
        ));
    }
}
