package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostTwitter;

public final class TelegramTwitterPostRenderer implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostTwitter;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostTwitter twitter = (ExtractedPostTwitter) post;
        return new TelegramMessageBuilder()
                .author(twitter.getAuthor(), twitter.getUrlProfile())
                .body(twitter.getText())
                .detail(value("Reply", twitter.getReplyCount()))
                .detail(value("Retweet", twitter.getRetweetCount()))
                .detail(value("Quote", twitter.getQuoteCount()))
                .detail(value("Views", twitter.getViewCount()))
                .timestamp(twitter.getTimestamp())
                .source("Source: Twitter", sourceUrl)
                .build();
    }

    private String value(String label, String value) {
        return value == null || value.isBlank() ? null : label + ": " + value;
    }
}
