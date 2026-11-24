package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;
import it.vle.entellocore.model.ExtractedPostPinterest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
                new TelegramYoutubePostRenderer(),
                new TelegramTiktokPostRender(),
                new TelegramInstagramPostRender()
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

        String author = presentation.authorText() == null ? post.getAuthor() : presentation.authorText();
        String text = presentation.text() == null ? post.getText() : presentation.text();
        if (text == null || text.isBlank()) {
            text = post.getText();
        }

        List<Segment> authorSegments = new ArrayList<>();
        if (author != null && !author.isBlank()) {
            String authorLine = presentation.authorLabel() == null || presentation.authorLabel().isBlank()
                    ? author
                    : presentation.authorLabel() + ": " + author;
            authorSegments.add(new Segment(authorLine, presentation.authorLink()));
        }
        TelegramFooter footer = TelegramFooter.findFor(post);
        if (footer != null) {
            String socialName = authorSegments.isEmpty()
                    ? footer.text()
                    : " (" + footer.text() + ")";
            authorSegments.add(new Segment(socialName, footer.linkUrl()));
        }
        addLine(lines, authorSegments, false, false);

        addLine(lines, text, null, false, true);
        addLine(lines, presentation.title(), null, true, false);

        for (TelegramPostRenderer.Field field : presentation.fields()) {
            if (field == null || isMissingValue(field.value())) {
                continue;
            }
            String label = field.label() == null ? "" : field.label();
            String fieldText = label.isBlank() ? field.value() : label + ": " + field.value();
            addLine(lines, fieldText, field.link(), false, false);
        }

        if (post.getTimestamp() != null) {
            String timestamp = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.of("Europe/Rome")) // Change this to your preferred time zone.
                    .format(Instant.ofEpochSecond(post.getTimestamp()));
            addLine(lines, timestamp, null, false, false);
        }
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            addLine(lines, "Source", sourceUrl, false, false);
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

            int lineLength = line.codePointLength();
            int includedLength = Math.min(lineLength, remaining);
            if (line.quoted()) {
                html.append("<blockquote>");
            }
            if (line.bold()) {
                html.append("<b>");
            }
            int segmentRemaining = includedLength;
            for (Segment segment : line.segments()) {
                if (segmentRemaining <= 0) {
                    break;
                }
                int segmentLength = segment.text().codePointCount(0, segment.text().length());
                int segmentIncludedLength = Math.min(segmentLength, segmentRemaining);
                String includedText = prefixByCodePoints(segment.text(), segmentIncludedLength);
                if (isHttpUrl(segment.url())) {
                    html.append("<a href=\"").append(escapeHtml(segment.url())).append("\">")
                            .append(escapeHtml(includedText)).append("</a>");
                } else {
                    html.append(escapeHtml(includedText));
                }
                segmentRemaining -= segmentIncludedLength;
            }
            if (line.bold()) {
                html.append("</b>");
            }
            if (line.quoted()) {
                html.append("</blockquote>");
            }
            remaining -= includedLength;
            if (includedLength < lineLength) {
                break;
            }
        }
        return html.toString();
    }

    private void addLine(List<Line> lines, String text, String link, boolean bold, boolean quoted) {
        if (text != null && !text.isBlank()) {
            lines.add(new Line(List.of(new Segment(text, link)), bold, quoted));
        }
    }

    private void addLine(List<Line> lines, List<Segment> segments, boolean bold, boolean quoted) {
        List<Segment> nonBlankSegments = segments.stream()
                .filter(segment -> segment.text() != null && !segment.text().isBlank())
                .toList();
        if (!nonBlankSegments.isEmpty()) {
            lines.add(new Line(nonBlankSegments, bold, quoted));
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

    private boolean isMissingValue(String value) {
        return value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim());
    }

    private record Segment(String text, String url) {
    }

    private record Line(List<Segment> segments, boolean bold, boolean quoted) {
        private int codePointLength() {
            return segments.stream()
                    .mapToInt(segment -> segment.text().codePointCount(0, segment.text().length()))
                    .sum();
        }
    }
}
