package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostPinterest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TelegramMessageMapper {

    private static final int MAX_MESSAGE_CODE_POINTS = 4_096;
    private static final int MAX_CAPTION_CODE_POINTS = 1_024;

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
                new TelegramYoutubePostRenderer()
        ));
    }

    public TelegramMessageMapper(List<TelegramPostRenderer> renderers) {
        this.renderers = List.copyOf(renderers);
    }

    public TelegramPostRenderer.Content prepare(ExtractedPost post, String sourceUrl) {
        TelegramPostRenderer.Content rendered = renderers.stream()
                .filter(renderer -> renderer.supports(post))
                .findFirst()
                .map(renderer -> renderer.render(post, sourceUrl))
                .orElseGet(TelegramPostRenderer.Content::empty);
        if (rendered.title().isBlank() && post instanceof ExtractedPostPinterest pinterest) {
            return new TelegramPostRenderer.Content(
                    pinterest.getTitle(),
                    rendered.text(),
                    rendered.authorText(),
                    rendered.authorLabel(),
                    rendered.authorLink(),
                    rendered.authorBeforeText(),
                    rendered.fields()
            );
        }
        return rendered;
    }

    public String formatText(ExtractedPost post, String sourceUrl, TelegramPostRenderer.Content presentation) {
        return format(post, sourceUrl, presentation, MAX_MESSAGE_CODE_POINTS);
    }

    public String formatCaption(ExtractedPost post, String sourceUrl, TelegramPostRenderer.Content presentation) {
        return format(post, sourceUrl, presentation, MAX_CAPTION_CODE_POINTS);
    }

    private String format(
            ExtractedPost post,
            String sourceUrl,
            TelegramPostRenderer.Content presentation,
            int maxCodePoints
    ) {
        List<Line> lines = new ArrayList<>();
        addLine(lines, presentation.title(), null, true);

        String author = presentation.authorText() == null ? post.getAuthor() : presentation.authorText();
        String text = presentation.text() == null ? post.getText() : presentation.text();
        String authorLine = author;
        if (authorLine != null && presentation.authorLabel() != null && !presentation.authorLabel().isBlank()) {
            authorLine = presentation.authorLabel() + ": " + authorLine;
        }
        if (presentation.authorBeforeText()) {
            addLine(lines, authorLine, presentation.authorLink(), false);
            addLine(lines, text, null, false);
        } else {
            addLine(lines, text, null, false);
            addLine(lines, authorLine, presentation.authorLink(), false);
        }

        for (TelegramPostRenderer.Field field : presentation.fields()) {
            if (field == null) {
                continue;
            }
            String value = field.value() == null ? "" : field.value();
            String label = field.label() == null ? "" : field.label();
            String fieldText = label.isBlank() ? value : label + ": " + value;
            addLine(lines, fieldText, field.link(), false);
        }

        TelegramFooter footer = TelegramFooter.findFor(post);
        if (footer != null) {
            addLine(lines, footer.text(), null, false);
        }
        if (post.getTimestamp() != null) {
            addLine(lines, Instant.ofEpochSecond(post.getTimestamp()).toString(), null, false);
        }
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            addLine(lines, "Source", sourceUrl, false);
        }

        StringBuilder html = new StringBuilder();
        int remaining = maxCodePoints;
        for (Line line : lines) {
            if (remaining <= 0) {
                break;
            }
            if (!html.isEmpty()) {
                html.append('\n');
                remaining--;
            }

            int lineLength = line.text().codePointCount(0, line.text().length());
            int includedLength = Math.min(lineLength, remaining);
            String includedText = prefixByCodePoints(line.text(), includedLength);
            if (line.bold()) {
                html.append("<b>");
            }
            if (isHttpUrl(line.link())) {
                html.append("<a href=\"").append(escapeHtml(line.link())).append("\">")
                        .append(escapeHtml(includedText)).append("</a>");
            } else {
                html.append(escapeHtml(includedText));
            }
            if (line.bold()) {
                html.append("</b>");
            }
            remaining -= includedLength;
            if (includedLength < lineLength) {
                break;
            }
        }
        return html.toString();
    }

    private void addLine(List<Line> lines, String text, String link, boolean bold) {
        if (text != null && !text.isBlank()) {
            lines.add(new Line(text, link, bold));
        }
    }

    private String prefixByCodePoints(String value, int length) {
        return value.substring(0, value.offsetByCodePoints(0, length));
    }

    private boolean isHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            java.net.URI uri = java.net.URI.create(value);
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private record Line(String text, String link, boolean bold) {
    }
}
