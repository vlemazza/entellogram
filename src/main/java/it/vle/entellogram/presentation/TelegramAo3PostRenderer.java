package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostAO3;

import java.util.ArrayList;
import java.util.List;

public class TelegramAo3PostRenderer implements TelegramPostRenderer {
    @Override
    public boolean supports(ExtractedPost post) { return post instanceof ExtractedPostAO3; }

    @Override
    public TelegramPostRenderer.Content render(ExtractedPost post) {
        ExtractedPostAO3 ao3 = (ExtractedPostAO3) post;
        List<TelegramPostRenderer.Field> fields = new ArrayList<>();
        fields.add(TelegramPostRenderer.field("Rating", TelegramPostRenderer.join(ao3.getRating())));
        fields.add(TelegramPostRenderer.field("Category", TelegramPostRenderer.join(ao3.getCategories())));
        fields.add(TelegramPostRenderer.field("Fandom", TelegramPostRenderer.join(ao3.getFandoms())));
        fields.add(TelegramPostRenderer.field("Relationship", TelegramPostRenderer.join(ao3.getRelationships())));
        fields.add(TelegramPostRenderer.field("Characters", TelegramPostRenderer.join(ao3.getCharacters())));
        String stats = "Words: " + ao3.getWords() + "\nChapters: " + ao3.getChapters()
                + "\nKudos: " + ao3.getKudos() + "\nHits: " + ao3.getHits();
        fields.add(TelegramPostRenderer.field("Stats", stats));
        return TelegramPostRenderer.withDetails(ao3.getTitle(), fields);
    }
}
