package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;

import java.util.List;

public final class TelegramMessageMapper {

    private final List<TelegramPostRenderer> renderers;

    public TelegramMessageMapper() {
        this(List.of(
                new TelegramTwitterPostRenderer(),
                new TelegramRedditPostRenderer(),
                new TelegramPixivPostRenderer(),
                new TelegramAo3PostRenderer(),
                new TelegramTumblrPostRenderer(),
                new TelegramHackerNewsPostRenderer(),
                new TelegramBlueskyPostRenderer(),
                new TelegramYoutubePostRenderer(),
                new TelegramTiktokPostRender(),
                new TelegramInstagramPostRender(),
                new TelegramPinterestPostRenderer(),
                new TelegramFacebookPostRenderer(),
                new TelegramBilibiliPostRenderer(),
                new TelegramPoipikuPostRenderer()
        ));
    }

    public TelegramMessageMapper(List<TelegramPostRenderer> renderers) {
        this.renderers = List.copyOf(renderers);
    }

    public TelegramMessage prepare(ExtractedPost post, String sourceUrl) {
        return renderers.stream()
                .filter(renderer -> renderer.supports(post))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No Telegram renderer registered for " + post.getClass().getName()
                ))
                .render(post, new TelegramPostRenderContext(sourceUrl));
    }
}
