package it.vle.entellogram.presentation;

final class TelegramdEmbedText {

    private TelegramdEmbedText() {
    }

    static String truncate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit - 3) + "...";
    }
}
