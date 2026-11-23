package it.vle.entellogram.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostTwitter;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Presenta i post X recuperando anche il testo nei sidecar di gallery-dl o via oEmbed. */
public final class TelegramTwitterPostRenderer implements TelegramPostRenderer {

    private static final Logger LOGGER = Logger.getLogger(TelegramTwitterPostRenderer.class.getName());
    private static final Duration X_TEXT_TIMEOUT = Duration.ofSeconds(20);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern X_STATUS_ID = Pattern.compile("/(?:status|statuses)/(\\d+)(?:[/?#]|$)");

    @Override
    public boolean supports(ExtractedPost post) {
        return post instanceof ExtractedPostTwitter;
    }

    @Override
    public TelegramPostContent render(ExtractedPost post) {
        return render(post, null);
    }

    @Override
    public TelegramPostContent render(ExtractedPost post, String sourceUrl) {
        ExtractedPostTwitter twitter = (ExtractedPostTwitter) post;
        String text = readSidecarText(post, sourceUrl);
        if (text.isBlank() && (post.getText() == null || post.getText().isBlank())) {
            LOGGER.info("X text sidecar missing; requesting public post text from X oEmbed: " + sourceUrl);
            text = fetchTextFromOEmbed(sourceUrl);
        }
        if (text.isBlank() && (post.getText() == null || post.getText().isBlank())) {
            LOGGER.warning("Unable to retrieve X post text from metadata or X oEmbed for: " + sourceUrl);
        }
        removeMetadataFiles(post);

        return TelegramPostContent.withText("", text, twitter.getUrlProfile(), true, List.of(
                /**TelegramPostRendererSupport.field("Reply", twitter.getReplyCount()),
                TelegramPostRendererSupport.field("Retweet", twitter.getRetweetCount()),
                TelegramPostRendererSupport.field("Quote", twitter.getQuoteCount()),*/
                TelegramPostRendererSupport.field("Views", twitter.getViewCount())
        ));
    }

    private String readSidecarText(ExtractedPost post, String sourceUrl) {
        if (post.getMedia() == null) {
            return "";
        }
        List<Path> metadataFiles = post.getMedia().stream()
                .filter(this::isMetadataFile)
                .toList();
        if (metadataFiles.isEmpty()) {
            return "";
        }

        String postId = extractStatusId(sourceUrl);
        if (postId != null) {
            List<Path> matchingFiles = metadataFiles.stream()
                    .filter(path -> path.getFileName().toString().equals(postId + ".txt")
                            || path.getFileName().toString().equals(postId + ".json"))
                    .toList();
            if (!matchingFiles.isEmpty()) {
                metadataFiles = matchingFiles;
            }
        }

        for (Path metadataFile : metadataFiles) {
            try {
                String filename = metadataFile.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                String sidecarText = filename.endsWith(".txt")
                        ? Files.readString(metadataFile, StandardCharsets.UTF_8).trim()
                        : extractPostText(JSON.readTree(metadataFile.toFile()));
                if (!sidecarText.isBlank()) {
                    return sidecarText;
                }
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Unable to read X post metadata: " + metadataFile, exception);
            }
        }
        return "";
    }

    private String fetchTextFromOEmbed(String sourceUrl) {
        String postId = extractStatusId(sourceUrl);
        if (postId == null) {
            return "";
        }

        try {
            String postUrl = sourceUrl;
            int queryStart = postUrl.indexOf('?');
            int fragmentStart = postUrl.indexOf('#');
            int suffixStart = queryStart < 0 ? fragmentStart
                    : fragmentStart < 0 ? queryStart : Math.min(queryStart, fragmentStart);
            if (suffixStart >= 0) {
                postUrl = postUrl.substring(0, suffixStart);
            }
            URI endpoint = URI.create("https://publish.x.com/oembed?url="
                    + URLEncoder.encode(postUrl, StandardCharsets.UTF_8)
                    + "&omit_script=true");
            HttpResponse<String> response = TelegramHttpClient.get(
                    endpoint,
                    X_TEXT_TIMEOUT,
                    "application/json",
                    "Mozilla/5.0 Entellogram/1.0"
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warning("X oEmbed returned HTTP " + response.statusCode() + " for post " + postId);
                return "";
            }

            JsonNode responseJson = JSON.readTree(response.body());
            String html = responseJson.path("html").asText("");
            return Jsoup.parse(html).select("blockquote p").text().trim();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Interrupted while retrieving X oEmbed text for post " + postId, exception);
        } catch (IOException | RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Unable to retrieve X oEmbed text for post " + postId, exception);
        }
        return "";
    }

    private String extractPostText(JsonNode metadata) {
        for (String field : List.of("content", "text", "full_text")) {
            String value = textValue(metadata.path(field));
            if (!value.isBlank()) {
                return value;
            }
        }

        for (String pointer : List.of("/legacy/full_text", "/tweet/content", "/tweet/text")) {
            String value = textValue(metadata.at(pointer));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String textValue(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return "";
        }
        if (value.isTextual()) {
            return value.asText().trim();
        }
        if (value.isArray()) {
            List<String> parts = new ArrayList<>();
            value.forEach(item -> {
                String part = textValue(item);
                if (!part.isBlank()) {
                    parts.add(part);
                }
            });
            return String.join("\n", parts);
        }
        if (value.isObject()) {
            for (String field : List.of("text", "full_text", "content")) {
                String part = textValue(value.path(field));
                if (!part.isBlank()) {
                    return part;
                }
            }
        }
        return "";
    }

    private void removeMetadataFiles(ExtractedPost post) {
        if (post.getMedia() == null) {
            return;
        }
        List<Path> mediaFiles = post.getMedia().stream()
                .filter(path -> !isMetadataFile(path))
                .toList();
        post.replaceMedia(mediaFiles);
    }

    private boolean isMetadataFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return filename.endsWith(".json") || filename.endsWith(".txt");
    }

    private String extractStatusId(String sourceUrl) {
        if (sourceUrl == null) {
            return null;
        }
        Matcher matcher = X_STATUS_ID.matcher(sourceUrl);
        return matcher.find() ? matcher.group(1) : null;
    }
}
