package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostAO3;

public final class TelegramAo3PostRenderer implements TelegramPostRenderer {
    @Override public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostAO3;
    }

    @Override
    public TelegramMessage render(ExtractedPost post, TelegramPostRenderContext context) {
        String sourceUrl = context.sourceUrl();
        ExtractedPostAO3 ao3 = (ExtractedPostAO3) post;
        return new TelegramMessageBuilder()
                .title(ao3.getTitle())
                .author(ao3.getAuthor(), null)
                .body(ao3.getText())
                .detail(joined("Rating", ao3.getRating()))
                .detail(joined("Category", ao3.getCategories()))
                .detail(joined("Fandom", ao3.getFandoms()))
                .detail(joined("Relationship", ao3.getRelationships()))
                .detail(joined("Characters", ao3.getCharacters()))
                .detail("Words: " + ao3.getWords() + "\nChapters: " + ao3.getChapters()
                        + "\nKudos: " + ao3.getKudos() + "\nHits: " + ao3.getHits())
                .timestamp(ao3.getTimestamp())
                .source("Source: Archive of Our Own", sourceUrl)
                .build();
    }

    private String joined(String label, java.util.List<String> values) {
        return values == null || values.isEmpty() ? null : label + ": " + String.join(", ", values);
    }
}
