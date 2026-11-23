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

    public TelegramPostContent(String title, List<Field> fields) {
        this(title, null, null, null, null, false, fields);
    }

    public TelegramPostContent(String title, String authorLink, List<Field> fields) {
        this(title, null, null, null, authorLink, false, fields);
    }

    public TelegramPostContent(String title, String authorLink, boolean authorBeforeText, List<Field> fields) {
        this(title, null, null, null, authorLink, authorBeforeText, fields);
    }

    public TelegramPostContent(
            String title,
            String text,
            String authorLink,
            boolean authorBeforeText,
            List<Field> fields
    ) {
        this(title, text, null, null, authorLink, authorBeforeText, fields);
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
