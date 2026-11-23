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
        fields.add(TelegramPostRenderer.field("Reply", twitter.getReplyCount()));
        fields.add(TelegramPostRenderer.field("Retweet", twitter.getRetweetCount()));
        fields.add(TelegramPostRenderer.field("Quote", twitter.getQuoteCount()));
        fields.add(TelegramPostRenderer.field("Views", twitter.getViewCount()));
        return new TelegramPostRenderer.Content(
                null, null, twitter.getAuthor(), null, twitter.getUrlProfile(), true, fields
        );
    }
}
