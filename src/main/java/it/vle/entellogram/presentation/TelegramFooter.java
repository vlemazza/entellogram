package it.vle.entellogram.presentation;

import it.vle.entellocore.model.*;

import java.util.Map;

record TelegramFooter(String text, String linkUrl) {

    private static final Map<Class<? extends ExtractedPost>, TelegramFooter> FOOTERS =
            Map.ofEntries(
                    Map.entry(ExtractedPostTwitter.class,
                            new TelegramFooter("Twitter", "https://x.com/home")),
                    Map.entry(ExtractedPostFacebook.class,
                            new TelegramFooter("Facebook", "https://www.facebook.com/")),
                    Map.entry(ExtractedPostBluesky.class,
                            new TelegramFooter("Bluesky", "https://bsky.app/")),
                    Map.entry(ExtractedPostTiktok.class,
                            new TelegramFooter("TikTok", "https://www.tiktok.com")),
                    Map.entry(ExtractedPostInstagram.class,
                            new TelegramFooter("Instagram", "https://www.instagram.com/")),
                    Map.entry(ExtractedPostReddit.class,
                            new TelegramFooter("Reddit", "https://www.reddit.com/")),
                    Map.entry(ExtractedPostBilibili.class,
                            new TelegramFooter("Bilibili", "https://www.bilibili.tv/")),
                    Map.entry(ExtractedPostPixiv.class,
                            new TelegramFooter("Pixiv", "https://www.pixiv.net/")),
                    Map.entry(ExtractedPostAO3.class,
                            new TelegramFooter("Archive of Our Own", "https://archiveofourown.org/")),
                    Map.entry(ExtractedPostPoipiku.class,
                            new TelegramFooter("Poipiku", "https://poipiku.com/")),
                    Map.entry(ExtractedPostTumblr.class,
                            new TelegramFooter("Tumblr", "https://www.tumblr.com/")),
                    Map.entry(ExtractedPostPinterest.class,
                            new TelegramFooter("Pinterest", "https://pinterest.com/")),
                    Map.entry(ExtractedPostHackerNews.class,
                            new TelegramFooter("Hacker News", "https://news.ycombinator.com/")),
                    Map.entry(ExtractedPostYoutube.class,
                            new TelegramFooter("YouTube", "https://www.youtube.com/"))
            );

    static TelegramFooter findFor(ExtractedPost post) {
        return FOOTERS.get(post.getClass());
    }

}
