package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;

import java.util.List;

/** Compone i messaggi Telegram usando un renderer dedicato per ciascun social supportato. */
public final class TelegramMessageMapper {

    private static final int MAX_TITLE_CODE_POINTS = 400;
    private static final int MAX_QUOTE_CODE_POINTS = 2_600;
    private static final int MAX_MESSAGE_CODE_POINTS = 4_096;
    private static final int MAX_CAPTION_CODE_POINTS = 1_024;

    private final List<TelegramPostRenderer> renderers;

    public TelegramMessageMapper() {
        this(List.of(
                new TelegramTwitterPostRenderer(),
                new TelegramRedditPostRenderer(),
                new TelegramPixivPostRenderer(),
                new TelegramPinterestPostRenderer(),
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

    /** Crea il messaggio completo per un post testuale, con citazione e dettagli del social. */
    public String formatText(ExtractedPost post, String sourceUrl) {
        return formatText(post, sourceUrl, prepare(post, sourceUrl));
    }

    public String formatText(ExtractedPost post, String sourceUrl, TelegramPostContent platform) {
        String title = truncate(platform.title(), MAX_TITLE_CODE_POINTS);
        String body = bodyOf(post, platform);
        if (title.equals(body)) {
            title = "";
        }
        if (body.isBlank()) {
            body = title;
            title = "";
        }
        body = truncate(body, MAX_QUOTE_CODE_POINTS);

        String author = platform.authorText() == null ? post.getAuthor() : platform.authorText();
        String authorText = author == null || author.isBlank() ? "User name not found :(" : author;

        StringBuilder html = new StringBuilder();
        int visibleLength = 0;
        if (platform.authorBeforeText()) {
            visibleLength += appendAuthor(html, authorText, authorLink(platform, sourceUrl), "");
        }
        if (!title.isBlank()) {
            visibleLength += appendSectionSeparator(html);
            html.append("<b>").append(escapeHtml(title)).append("</b>");
            visibleLength += title.codePointCount(0, title.length());
        }
        if (!body.isBlank()) {
            visibleLength += appendSectionSeparator(html);
            html.append("<blockquote>").append(escapeHtml(body)).append("</blockquote>");
            visibleLength += body.codePointCount(0, body.length());
        }
        if (!platform.authorBeforeText()) {
            visibleLength += appendSectionSeparator(html);
            String label = platform.authorLabel() == null ? "Autore" : platform.authorLabel();
            visibleLength += appendAuthor(html, authorText, authorLink(platform, sourceUrl), label);
        }

        appendFields(html, platform.fields(), Math.max(0,
                Math.min(1_000, MAX_MESSAGE_CODE_POINTS - visibleLength - 2)));
        return html.toString();
    }

    /** Crea la didascalia condivisa da foto e video, entro il limite dell'API Telegram. */
    public String formatMediaCaption(ExtractedPost post, String sourceUrl) {
        return formatMediaCaption(post, sourceUrl, prepare(post, sourceUrl));
    }

    public String formatMediaCaption(ExtractedPost post, String sourceUrl, TelegramPostContent platform) {
        String description = bodyOf(post, platform);
        if (description.isBlank()) {
            description = platform.title().trim();
        }
        String author = platform.authorText() == null ? post.getAuthor() : platform.authorText();
        String linkText = author == null || author.isBlank() ? "User name not found :(" : author;

        int linkBudget = description.isBlank() ? MAX_CAPTION_CODE_POINTS : MAX_CAPTION_CODE_POINTS - 2;
        linkText = truncate(linkText, linkBudget);
        String authorLine = formatAuthorLine(linkText, authorLink(platform, sourceUrl), platform.authorLabel());
        int descriptionBudget = Math.max(0, MAX_CAPTION_CODE_POINTS
                - linkText.codePointCount(0, linkText.length()) - (description.isBlank() ? 0 : 2));
        description = truncate(description, descriptionBudget);

        StringBuilder caption = new StringBuilder();
        int visibleLength = 0;
        if (platform.authorBeforeText()) {
            caption.append(authorLine);
            visibleLength += linkText.codePointCount(0, linkText.length());
            if (!description.isBlank()) {
                caption.append("\n\n<blockquote>").append(escapeHtml(description)).append("</blockquote>");
                visibleLength += 2 + description.codePointCount(0, description.length());
            }
        } else {
            if (!description.isBlank()) {
                description = truncate(description, MAX_CAPTION_CODE_POINTS);
                caption.append("<blockquote>").append(escapeHtml(description)).append("</blockquote>");
                visibleLength = description.codePointCount(0, description.length());
            }
            if (!caption.isEmpty()) {
                caption.append("\n\n");
                visibleLength += 2;
            }
            caption.append(authorLine);
            visibleLength += linkText.codePointCount(0, linkText.length());
        }

        appendFields(caption, platform.fields(), Math.max(0, MAX_CAPTION_CODE_POINTS - visibleLength - 2));
        return caption.toString();
    }

    /** Esegue una volta il renderer del social e conserva i dati necessari all'invio. */
    public TelegramPostContent prepare(ExtractedPost post, String sourceUrl) {
        return renderers.stream()
                .filter(renderer -> renderer.supports(post))
                .findFirst()
                .map(renderer -> renderer.render(post, sourceUrl))
                .orElseGet(TelegramPostContent::empty);
    }

    private String bodyOf(ExtractedPost post, TelegramPostContent platform) {
        String body = post.getText() == null ? "" : post.getText().trim();
        if (body.isBlank() && platform.text() != null) {
            body = platform.text().trim();
        }
        return body;
    }

    private String authorLink(TelegramPostContent platform, String sourceUrl) {
        return platform.authorLink() == null || platform.authorLink().isBlank()
                ? sourceUrl
                : platform.authorLink();
    }

    private int appendAuthor(StringBuilder html, String author, String link, String label) {
        String prefix = label == null || label.isBlank() ? "" : label + ": ";
        html.append(escapeHtml(prefix)).append(linkedValue(author, link));
        return prefix.codePointCount(0, prefix.length()) + author.codePointCount(0, author.length());
    }

    private String formatAuthorLine(String author, String link, String label) {
        String prefix = label == null || label.isBlank() ? "" : label + ": ";
        return escapeHtml(prefix) + linkedValue(author, link);
    }

    private int appendSectionSeparator(StringBuilder html) {
        if (html.isEmpty()) {
            return 0;
        }
        html.append("\n\n");
        return 2;
    }

    private void appendFields(StringBuilder html, List<TelegramPostContent.Field> fields, int budget) {
        StringBuilder rendered = new StringBuilder();
        for (TelegramPostContent.Field field : fields) {
            if (budget <= 0) {
                break;
            }
            String label = field.label() == null ? "" : field.label();
            String prefix = label.isBlank() ? "" : label + ": ";
            int prefixLength = prefix.codePointCount(0, prefix.length());
            int availableValueLength = budget - prefixLength;
            if (availableValueLength <= 0 || field.value() == null || field.value().isBlank()) {
                continue;
            }

            String value = truncate(field.value(), availableValueLength);
            if (!rendered.isEmpty()) {
                rendered.append('\n');
                budget--;
            }
            rendered.append(escapeHtml(prefix)).append(linkedValue(value, field.link()));
            budget -= prefixLength + value.codePointCount(0, value.length());
        }

        if (!rendered.isEmpty()) {
            if (!html.isEmpty()) {
                html.append("\n\n");
            }
            html.append(rendered);
        }
    }

    private String linkedValue(String value, String link) {
        if (link == null || link.isBlank()) {
            return escapeHtml(value);
        }
        return "<a href=\"" + escapeHtmlAttribute(link) + "\">" + escapeHtml(value) + "</a>";
    }

    private String truncate(String value, int maxCodePoints) {
        if (value == null || maxCodePoints <= 0) {
            return "";
        }
        int length = value.codePointCount(0, value.length());
        if (length <= maxCodePoints) {
            return value;
        }
        int end = value.offsetByCodePoints(0, maxCodePoints - 1);
        return value.substring(0, end) + "…";
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String escapeHtmlAttribute(String value) {
        return escapeHtml(value).replace("\"", "&quot;");
    }
}
