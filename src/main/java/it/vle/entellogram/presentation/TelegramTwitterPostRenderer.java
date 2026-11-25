package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostTwitter;

import java.util.ArrayList;
import java.util.List;

public class TelegramTwitterPostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostTwitter;
    }

    @Override
    public TelegramPostRenderer.Content render(ExtractedPost post) {
        ExtractedPostTwitter twitter = (ExtractedPostTwitter) post;

        List<TelegramPostRenderer.Field> fields = new ArrayList<>();

        addField(fields, "Reply", twitter.getReplyCount());
        addField(fields, "Retweet", twitter.getRetweetCount());
        addField(fields, "Quote", twitter.getQuoteCount());
        addField(fields, "Views", twitter.getViewCount());

        return new TelegramPostRenderer.Content(
                null,
                twitter.getText(),
                twitter.getAuthor(),
                null,
                twitter.getUrlProfile(),
                true,
                fields
        );
    }

    private void addField(
            List<TelegramPostRenderer.Field> fields,
            String name,
            String value
    ) {
        if (value != null && !value.isBlank()) {
            fields.add(TelegramPostRenderer.field(name, value));
        }
    }
}