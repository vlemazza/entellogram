package it.vle.entellogram.presentation;

import it.vle.entellocore.model.ExtractedPost;

public interface TelegramPostRenderer {

    boolean supports(ExtractedPost post);

    Content render(ExtractedPost post);

    default Content render(ExtractedPost post, String sourceUrl) {
        return render(post);
    }

    static Content withDetails(String title, java.util.List<Field> fields) {
        return new Content(title, null, null, null, null, false, fields, false);
    }

    static Field field(String label, String value) {
        return new Field(label, value, null);
    }

    static Field link(String label, String value, String url) {
        return new Field(label, value, url);
    }

    static String join(java.util.List<String> values) {
        return values == null ? "" : String.join(", ", values);
    }

    record Content(
            String title,
            String text,
            String authorText,
            String authorLabel,
            String authorLink,
            boolean authorBeforeText,
            java.util.List<Field> fields,
            boolean titleFirst
    ) {
        public Content {
            title = title == null ? "" : title;
            fields = fields == null ? java.util.List.of() : java.util.List.copyOf(fields);
        }

        public static Content empty() {
            return new Content("", null, null, null, null, false, java.util.List.of(), false);
        }
    }

    record Field(String label, String value, String link) {
    }
}
