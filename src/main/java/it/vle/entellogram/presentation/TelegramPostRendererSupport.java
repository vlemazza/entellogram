package it.vle.entellogram.presentation;

import java.util.List;

final class TelegramPostRendererSupport {

    private TelegramPostRendererSupport() {
    }

    static TelegramPostContent.Field field(String label, String value) {
        return new TelegramPostContent.Field(label, value, null);
    }

    static TelegramPostContent.Field link(String label, String value, String url) {
        return new TelegramPostContent.Field(label, value, url);
    }

    static String join(List<String> values) {
        return values == null ? "" : String.join(", ", values);
    }

    static String positive(int value) {
        return value > 0 ? Integer.toString(value) : "";
    }
}
