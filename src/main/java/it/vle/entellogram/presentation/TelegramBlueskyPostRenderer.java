package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostBluesky;

import java.util.ArrayList;
import java.util.List;

public final class TelegramBlueskyPostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostBluesky;
    }

    @Override
    public TelegramPostContent render(ExtractedPost post) {
        ExtractedPostBluesky bluesky = (ExtractedPostBluesky) post;
        List<TelegramPostContent.Field> fields = new ArrayList<>();
        String quotedUrl = blueskyPostUrl(bluesky.getEmbeddedPostUri());
        fields.add(TelegramPostRendererSupport.link("Post citato", quotedUrl, quotedUrl));

        if (bluesky.hasEmbeddedPost()) {
            ExtractedPost quoted = bluesky.getEmbeddedPost();
            fields.add(TelegramPostRendererSupport.field("Autore del post citato", quoted.getAuthor()));
            fields.add(TelegramPostRendererSupport.field("Testo citato", quoted.getText()));
        }
        if (bluesky.getEmbedExternal() != null) {
            ExtractedPostBluesky.EmbedExternal external = bluesky.getEmbedExternal();
            String title = external.title();
            fields.add(TelegramPostRendererSupport.link(
                    title == null || title.isBlank() ? "Link esterno" : title,
                    external.uri(),
                    external.uri()
            ));
            fields.add(TelegramPostRendererSupport.field("Descrizione", external.description()));
        }

        fields.add(TelegramPostRendererSupport.field("Risposte", bluesky.getReplyCount()));
        fields.add(TelegramPostRendererSupport.field("Ripubblicazioni", bluesky.getRepostCount()));
        fields.add(TelegramPostRendererSupport.field("Mi piace", bluesky.getLikeCount()));
        return new TelegramPostContent("", fields);
    }

    private String blueskyPostUrl(String uri) {
        if (uri == null || !uri.startsWith("at://")) {
            return uri;
        }
        String[] parts = uri.substring("at://".length()).split("/");
        if (parts.length != 3 || !parts[1].equals("app.bsky.feed.post")) {
            return uri;
        }
        return "https://bsky.app/profile/" + parts[0] + "/post/" + parts[2];
    }
}
