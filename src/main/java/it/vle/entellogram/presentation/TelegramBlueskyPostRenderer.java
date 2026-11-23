package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostBluesky;

import java.util.ArrayList;
import java.util.List;

public class TelegramBlueskyPostRenderer implements TelegramPostRenderer {
    @Override
    public boolean supports(ExtractedPost post) { return post instanceof ExtractedPostBluesky; }

    @Override
    public TelegramPostRenderer.Content render(ExtractedPost post) {
        ExtractedPostBluesky bluesky = (ExtractedPostBluesky) post;
        List<TelegramPostRenderer.Field> fields = new ArrayList<>();
        if (bluesky.hasEmbeddedPost()) {
            ExtractedPost quoted = bluesky.getEmbeddedPost();
            fields.add(TelegramPostRenderer.field(
                    "Quoted post",
                    quote(quoted.getAuthor(), quoted.getText())
            ));
        }
        if (bluesky.getEmbedExternal() != null) {
            ExtractedPostBluesky.EmbedExternal external = bluesky.getEmbedExternal();
            fields.add(TelegramPostRenderer.link("External", external.title(), external.uri()));
            fields.add(TelegramPostRenderer.field("Description", external.description()));
        }
        fields.add(TelegramPostRenderer.field("Reply", bluesky.getReplyCount()));
        fields.add(TelegramPostRenderer.field("Repost", bluesky.getRepostCount()));
        fields.add(TelegramPostRenderer.field("Like", bluesky.getLikeCount()));
        return new TelegramPostRenderer.Content(null, null, bluesky.getAuthor(), null, null, true, fields);
    }

    private String quote(String author, String text) {
        StringBuilder value = new StringBuilder();
        if (author != null && !author.isBlank()) value.append(author).append('\n');
        if (text != null && !text.isBlank()) value.append(text);
        return value.toString();
    }

}
