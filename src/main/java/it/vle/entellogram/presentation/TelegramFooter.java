package it.vle.entellogram.presentation;

import it.vle.entellocore.model.*;

import java.util.Map;

record TelegramFooter(String text, String iconUrl) {

    private static final Map<Class<? extends ExtractedPost>, TelegramFooter> FOOTERS =
            Map.ofEntries(
                    Map.entry(ExtractedPostTwitter.class,
                            new TelegramFooter("Twitter", "https://raw.githubusercontent.com/homarr-labs/dashboard-icons/refs/heads/main/png/twitter.png")),
                    Map.entry(ExtractedPostFacebook.class,
                            new TelegramFooter("Facebook", "https://raw.githubusercontent.com/homarr-labs/dashboard-icons/refs/heads/main/png/facebook.png")),
                    Map.entry(ExtractedPostBluesky.class,
                            new TelegramFooter("Bluesky", "https://raw.githubusercontent.com/homarr-labs/dashboard-icons/refs/heads/main/png/bluesky.png")),
                    Map.entry(ExtractedPostTiktok.class,
                            new TelegramFooter("TikTok", "https://raw.githubusercontent.com/homarr-labs/dashboard-icons/refs/heads/main/png/tiktok.png")),
                    Map.entry(ExtractedPostInstagram.class,
                            new TelegramFooter("Instagram", "https://raw.githubusercontent.com/homarr-labs/dashboard-icons/refs/heads/main/png/instagram.png")),
                    Map.entry(ExtractedPostReddit.class,
                            new TelegramFooter("Reddit", "https://raw.githubusercontent.com/homarr-labs/dashboard-icons/refs/heads/main/png/reddit.png")),
                    Map.entry(ExtractedPostBilibili.class,
                            new TelegramFooter("Bilibili", "https://raw.githubusercontent.com/homarr-labs/dashboard-icons/refs/heads/main/png/bilibili.png")),
                    Map.entry(ExtractedPostPixiv.class,
                            new TelegramFooter("Pixiv", "https://upload.wikimedia.org/wikipedia/commons/e/ed/Pixiv.png")),
                    Map.entry(ExtractedPostAO3.class,
                            new TelegramFooter("Archive of Our Own", "https://archiveofourown.org/images/ao3_logos/logo_42.png")),
                    Map.entry(ExtractedPostPoipiku.class,
                            new TelegramFooter("Poipiku", "https://cdn.poipiku.com/assets/img/pc_top_title-03.png")),
                    Map.entry(ExtractedPostTumblr.class,
                            new TelegramFooter("Tumblr", "https://raw.githubusercontent.com/homarr-labs/dashboard-icons/refs/heads/main/png/tumblr.png")),
                    Map.entry(ExtractedPostPinterest.class,
                            new TelegramFooter("Pinterest", "https://raw.githubusercontent.com/homarr-labs/dashboard-icons/refs/heads/main/png/pinterest.png")),
                    Map.entry(ExtractedPostHackerNews.class,
                            new TelegramFooter("Hacker News", "https://raw.githubusercontent.com/homarr-labs/dashboard-icons/refs/heads/main/png/hacker-news.png")),
                    Map.entry(ExtractedPostYoutube.class,
                            new TelegramFooter("YouTube", "https://raw.githubusercontent.com/homarr-labs/dashboard-icons/refs/heads/main/png/youtube.png"))
            );

    static TelegramFooter findFor(ExtractedPost post) {
        return FOOTERS.get(post.getClass());
    }

}
