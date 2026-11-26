package it.vle.entellogram.presentation;

import it.vle.entellogram.presentation.TelegramMessageElement.Author;
import it.vle.entellogram.presentation.TelegramMessageElement.Body;
import it.vle.entellogram.presentation.TelegramMessageElement.Detail;
import it.vle.entellogram.presentation.TelegramMessageElement.Link;
import it.vle.entellogram.presentation.TelegramMessageElement.Source;
import it.vle.entellogram.presentation.TelegramMessageElement.Spacer;
import it.vle.entellogram.presentation.TelegramMessageElement.Timestamp;
import it.vle.entellogram.presentation.TelegramMessageElement.Title;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class TelegramMessageFormatter {

    private static final int MAX_MESSAGE_CODE_POINTS = 4_096;
    private static final int MAX_CAPTION_CODE_POINTS = 1_024;

    public List<String> formatTextParts(TelegramMessage message) {
        return format(message, MAX_MESSAGE_CODE_POINTS);
    }

    public List<String> formatCaptionParts(TelegramMessage message) {
        return format(message, MAX_CAPTION_CODE_POINTS);
    }

    private List<String> format(TelegramMessage message, int limit) {
        List<TelegramMessageElement> elements = new ArrayList<>(message.elements());
        List<String> messages = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int used = 0;

        for (TelegramMessageElement element : elements) {
            if (element instanceof Spacer) {
                if (!current.isEmpty()) {
                    if (used == limit) {
                        messages.add(current.toString());
                        current.setLength(0);
                        used = 0;
                    } else {
                        current.append('\n');
                        used++;
                    }
                }
                continue;
            }

            String text = textOf(element);
            if (text == null || text.isBlank()) continue;

            int position = 0;
            int textLength = text.codePointCount(0, text.length());
            boolean firstPart = true;
            while (position < textLength) {
                int separatorLength = current.isEmpty() || !firstPart ? 0 : 1;
                int available = limit - used - separatorLength;
                if (available <= 0) {
                    messages.add(current.toString());
                    current.setLength(0);
                    used = 0;
                    continue;
                }

                int remainingText = textLength - position;
                int included = Math.min(remainingText, available);
                if (included < remainingText) included = preferWordBoundary(text, position, included);
                if (included == 0) included = Math.min(remainingText, available);

                if (separatorLength == 1) {
                    current.append('\n');
                    used++;
                }
                String part = codePointSlice(text, position, included);
                appendElement(current, element, part);
                used += included;
                position += included;
                firstPart = false;

                if (position < textLength) {
                    messages.add(current.toString());
                    current.setLength(0);
                    used = 0;
                }
            }
        }

        if (!current.isEmpty()) messages.add(current.toString());
        return List.copyOf(messages);
    }

    private String textOf(TelegramMessageElement element) {
        if (element instanceof Title value) return value.text();
        if (element instanceof Body(String text)) return text;
        if (element instanceof Author value) return value.text();
        if (element instanceof Detail(String text)) return text;
        if (element instanceof Link value) return value.label();
        if (element instanceof Source value) return value.label();
        if (element instanceof Timestamp(long epochSeconds)) return formatTimestamp(epochSeconds);
        return null;
    }

    private int preferWordBoundary(String text, int startCodePoint, int maxCodePoints) {
        int start = text.offsetByCodePoints(0, startCodePoint);
        int end = text.offsetByCodePoints(start, maxCodePoints);
        int boundary = -1;
        for (int index = start; index < end;) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)) boundary = index;
        }
        return boundary < 0 ? maxCodePoints : text.codePointCount(start, boundary);
    }

    private String codePointSlice(String text, int start, int length) {
        int startIndex = text.offsetByCodePoints(0, start);
        int endIndex = text.offsetByCodePoints(startIndex, length);
        return text.substring(startIndex, endIndex);
    }

    private void appendElement(StringBuilder html, TelegramMessageElement element, String text) {
        String url = urlOf(element);
        boolean link = isHttpUrl(url);
        if (element instanceof Body) html.append("<blockquote expandable>");
        if (element instanceof Title) html.append("<b>");
        if (element instanceof Author) html.append("<i>");
        if (link) html.append("<a href=\"").append(escapeHtml(url)).append("\">");
        html.append(escapeHtml(text));
        if (link) html.append("</a>");
        if (element instanceof Author) html.append("</i>");
        if (element instanceof Title) html.append("</b>");
        if (element instanceof Body) html.append("</blockquote>");
    }

    private String urlOf(TelegramMessageElement element) {
        if (element instanceof Title value) return value.url();
        if (element instanceof Author value) return value.profileUrl();
        if (element instanceof Link value) return value.url();
        if (element instanceof Source value) return value.url();
        return null;
    }

    private String formatTimestamp(long epochSeconds) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.of("Europe/Rome"))
                .format(Instant.ofEpochSecond(epochSeconds));
    }

    private boolean isHttpUrl(String value) {
        if (value == null || value.isBlank()) return false;
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
}
