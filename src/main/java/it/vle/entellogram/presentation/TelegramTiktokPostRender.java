package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostTiktok;

import java.util.ArrayList;
import java.util.List;

public class TelegramTiktokPostRender implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post){
        return post instanceof  ExtractedPostTiktok;
    }

    @Override
    public TelegramPostRenderer.Content render(ExtractedPost post) {
        ExtractedPostTiktok tiktok = (ExtractedPostTiktok) post;


        return new TelegramPostRenderer.Content(
                null, tiktok.getText(), tiktok.getAuthor(), null, tiktok.getUrlProfile(), false, null, false
        );
    }
}
