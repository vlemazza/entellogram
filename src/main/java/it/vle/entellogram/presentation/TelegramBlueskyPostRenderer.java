package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostBluesky;

public final class TelegramBlueskyPostRenderer implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostBluesky;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostBluesky bluesky = (ExtractedPostBluesky) post;
        TelegramMessageBuilder message = new TelegramMessageBuilder()
                .author(bluesky.getAuthor(), null)
                .body(bluesky.getText());
        if (bluesky.hasEmbeddedPost()) {
            ExtractedPost quoted = bluesky.getEmbeddedPost();
            message.detail("Quoted post: " + quote(quoted.getAuthor(), quoted.getText()));
        }
        if (bluesky.getEmbedExternal() != null) {
            ExtractedPostBluesky.EmbedExternal external = bluesky.getEmbedExternal();
            message.link(external.title(), external.uri())
                    .detail(value("Description", external.description()));
        }
        return message
                .detail(value("Reply", bluesky.getReplyCount()))
                .detail(value("Repost", bluesky.getRepostCount()))
                .detail(value("Like", bluesky.getLikeCount()))
                .timestamp(bluesky.getTimestamp())
                .source("Source: Bluesky", sourceUrl)
                .build();
    }

    private String quote(String author, String text) {
        if (author == null || author.isBlank()) return text == null ? "" : text;
        return text == null || text.isBlank() ? author : author + "\n" + text;
    }

    private String value(String label, String value) {
        return value == null || value.isBlank() ? null : label + ": " + value;
    }
}
