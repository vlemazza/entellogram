package it.vle.entellogram.presentation;

import java.util.List;

/** Titolo e dettagli specifici di una piattaforma, pronti per la resa in Telegram. */
public record TelegramPostContent(
        String title,
        String text,
        String authorText,
        String authorLabel,
        String authorLink,
        boolean authorBeforeText,
        List<Field> fields
) {

    public static TelegramPostContent withDetails(String title, List<Field> fields) {
        return new TelegramPostContent(title, null, null, null, null, false, fields);
    }

    public static TelegramPostContent withAuthor(
            String title,
            String authorText,
            String authorLabel,
            List<Field> fields
    ) {
        return new TelegramPostContent(title, null, authorText, authorLabel, null, false, fields);
    }

    public static TelegramPostContent withText(
            String title,
            String text,
            String authorLink,
            boolean authorBeforeText,
            List<Field> fields
    ) {
        return new TelegramPostContent(title, text, null, null, authorLink, authorBeforeText, fields);
    }

    public TelegramPostContent {
        title = title == null ? "" : title;
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public static TelegramPostContent empty() {
        return new TelegramPostContent("", null, null, null, null, false, List.of());
    }

    public record Field(String label, String value, String link) {
    }
}
