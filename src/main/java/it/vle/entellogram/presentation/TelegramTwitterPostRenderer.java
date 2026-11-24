package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostTwitter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TelegramTwitterPostRenderer implements TelegramPostRenderer {

    private static final Logger LOGGER = Logger.getLogger(TelegramTwitterPostRenderer.class.getName());
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

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

    @Override
    public TelegramPostRenderer.Content render(ExtractedPost post, String sourceUrl) {
        ExtractedPostTwitter twitter = (ExtractedPostTwitter) post;
        String text = twitter.getText();
        if (text == null || text.isBlank()) {
            text = fetchDescription(sourceUrl);
        }

        List<TelegramPostRenderer.Field> fields = new ArrayList<>();
        fields.add(TelegramPostRenderer.field("Reply", twitter.getReplyCount()));
        fields.add(TelegramPostRenderer.field("Retweet", twitter.getRetweetCount()));
        fields.add(TelegramPostRenderer.field("Quote", twitter.getQuoteCount()));
        fields.add(TelegramPostRenderer.field("Views", twitter.getViewCount()));
        return new TelegramPostRenderer.Content(
                null, text, twitter.getAuthor(), null, twitter.getUrlProfile(), true, fields
        );
    }

    private String fetchDescription(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return "";
        }

        try {
            URI uri = URI.create(sourceUrl);
            if (!isTwitterUrl(uri)) {
                LOGGER.warning("Skipping X post metadata lookup for a non-X URL.");
                return "";
            }

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "text/html")
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warning("X post page returned HTTP " + response.statusCode());
                return "";
            }

            Document page = Jsoup.parse(response.body());
            for (String selector : List.of(
                    "meta[property=og:description]",
                    "meta[name=description]",
                    "meta[name=twitter:description]"
            )) {
                String description = page.select(selector).attr("content").trim();
                if (!description.isBlank()) {
                    return description;
                }
            }
            LOGGER.warning("X post page did not expose a description in its metadata.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Interrupted while retrieving X post description.", exception);
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.log(Level.WARNING, "Unable to retrieve X post description from page metadata.", exception);
        }
        return "";
    }

    private boolean isTwitterUrl(URI uri) {
        String host = uri.getHost();
        boolean supportedScheme = "http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme());
        return supportedScheme && host != null && (host.equalsIgnoreCase("x.com")
                || host.equalsIgnoreCase("www.x.com")
                || host.equalsIgnoreCase("twitter.com")
                || host.equalsIgnoreCase("www.twitter.com"));
    }
}
