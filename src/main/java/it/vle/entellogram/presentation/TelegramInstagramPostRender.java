package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostInstagram;

import java.util.ArrayList;
import java.util.List;

public class TelegramInstagramPostRender implements TelegramPostRenderer {

    @Override
    public boolean supports(ExtractedPost post){
        return post instanceof  ExtractedPostInstagram;
    }

    @Override
    public TelegramPostRenderer.Content render(ExtractedPost post) {
        ExtractedPostInstagram instagram = (ExtractedPostInstagram) post;


        return new TelegramPostRenderer.Content(
                null, instagram.getText(), instagram.getAuthor(), null, instagram.getUrlProfile(), false, null, false
        );
    }
}
