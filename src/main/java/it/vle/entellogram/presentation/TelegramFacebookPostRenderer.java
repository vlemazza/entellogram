package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostFacebook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Recupera dal markup pubblico di Facebook il testo assente nei metadati di gallery-dl. */
public final class TelegramFacebookPostRenderer implements TelegramPostRenderer {

    private static final Logger LOGGER = Logger.getLogger(TelegramFacebookPostRenderer.class.getName());
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostFacebook;
    }

    @Override
    public TelegramPostContent render(ExtractedPost post) {
        return render(post, null);
    }

    @Override
    public TelegramPostContent render(ExtractedPost post, String sourceUrl) {
        String text = post.getText();
        if (text == null || text.isBlank()) {
            text = fetchPageDescription(sourceUrl);
        }
        return TelegramPostContent.withText("", text, null, false, List.of());
    }

    private String fetchPageDescription(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return "";
        }

        try {
            URI pageUri = URI.create(sourceUrl);
            if (!isFacebookUrl(pageUri)) {
                LOGGER.warning("Skipping Facebook metadata lookup for a non-Facebook URL.");
                return "";
            }

            Document page = fetchPage(pageUri);
            if (page == null) {
                return "";
            }

            String description = findBestDescription(page);
            String canonicalUrl = page.select("link[rel=canonical]").attr("href").trim();
            if (isTruncated(description) && !canonicalUrl.isBlank()) {
                URI canonicalUri = pageUri.resolve(canonicalUrl);
                if (isFacebookUrl(canonicalUri) && !canonicalUri.equals(pageUri)) {
                    Document canonicalPage = fetchPage(canonicalUri);
                    if (canonicalPage != null) {
                        String canonicalDescription = findBestDescription(canonicalPage);
                        if (canonicalDescription.codePointCount(0, canonicalDescription.length())
                                > description.codePointCount(0, description.length())) {
                            description = canonicalDescription;
                        }
                    }
                }
            }

            if (!description.isBlank()) {
                LOGGER.info("Retrieved Facebook post description from page metadata ("
                        + description.codePointCount(0, description.length()) + " characters).");
                return description;
            }
            LOGGER.warning("Facebook post page did not expose a description in its metadata: " + sourceUrl);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Interrupted while retrieving Facebook post description.", exception);
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.log(Level.WARNING, "Unable to retrieve Facebook post description from page metadata.", exception);
        }
        return "";
    }

    private Document fetchPage(URI pageUri) throws IOException, InterruptedException {
        HttpResponse<String> response = TelegramHttpClient.get(
                pageUri,
                REQUEST_TIMEOUT,
                "text/html,application/xhtml+xml",
                "Mozilla/5.0"
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOGGER.warning("Facebook post page returned HTTP " + response.statusCode());
            return null;
        }
        return Jsoup.parse(response.body());
    }

    private String findBestDescription(Document page) {
        String best = "";
        for (String selector : List.of(
                "meta[property=og:description]",
                "meta[name=twitter:description]",
                "meta[name=description]"
        )) {
            String candidate = page.select(selector).attr("content").trim();
            if (candidate.codePointCount(0, candidate.length()) > best.codePointCount(0, best.length())) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean isTruncated(String description) {
        return description.endsWith("...") || description.endsWith("…");
    }

    private boolean isFacebookUrl(URI uri) {
        String host = uri.getHost();
        return host != null && (host.equalsIgnoreCase("facebook.com")
                || host.toLowerCase(Locale.ROOT).endsWith(".facebook.com"));
    }
}
