package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostReddit;

public final class TelegramRedditPostRenderer implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostReddit;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostReddit reddit = (ExtractedPostReddit) post;
        return new TelegramMessageBuilder()
                .title(reddit.getTitle(), reddit.getUrlExt())
                .author(reddit.getAuthor(), null)
                .body(reddit.getText())
                .detail(reddit.getSubreddit())
                .detail(engagement(reddit.getUpvote(), reddit.getNumComments()))
                .timestamp(reddit.getTimestamp())
                .source("Source: Reddit", sourceUrl)
                .build();
    }

    private String engagement(String upvotes, String comments) {
        String upvoteText = upvotes == null || upvotes.isBlank() ? null : "Upvotes: " + upvotes;
        String commentText = comments == null || comments.isBlank() ? null : "Comments: " + comments;
        if (upvoteText == null) return commentText;
        if (commentText == null) return upvoteText;
        return upvoteText + " · " + commentText;
    }
}
