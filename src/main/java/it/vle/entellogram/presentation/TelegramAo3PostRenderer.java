package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostAO3;

import java.util.List;

public final class TelegramAo3PostRenderer implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostAO3;
    }

    @Override
    public TelegramPostContent render(ExtractedPost post) {
        ExtractedPostAO3 ao3 = (ExtractedPostAO3) post;
        return TelegramPostContent.withDetails(ao3.getTitle(), List.of(
                TelegramPostRendererSupport.field("Rating", TelegramPostRendererSupport.join(ao3.getRating())),
                TelegramPostRendererSupport.field("Categorie", TelegramPostRendererSupport.join(ao3.getCategories())),
                TelegramPostRendererSupport.field("Fandom", TelegramPostRendererSupport.join(ao3.getFandoms())),
                TelegramPostRendererSupport.field("Relazioni", TelegramPostRendererSupport.join(ao3.getRelationships())),
                TelegramPostRendererSupport.field("Personaggi", TelegramPostRendererSupport.join(ao3.getCharacters())),
                TelegramPostRendererSupport.field("Parole", TelegramPostRendererSupport.positive(ao3.getWords())),
                TelegramPostRendererSupport.field("Capitoli", ao3.getChapters()),
                TelegramPostRendererSupport.field("Kudos", TelegramPostRendererSupport.positive(ao3.getKudos())),
                TelegramPostRendererSupport.field("Visite", TelegramPostRendererSupport.positive(ao3.getHits()))
        ));
    }
}
