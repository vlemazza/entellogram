package it.vle.entellogram.presentation;

/** Typed presentation parts; the formatter handles their Telegram-specific markup. */
public sealed interface TelegramMessageElement permits
        TelegramMessageElement.Title,
        TelegramMessageElement.Body,
        TelegramMessageElement.Author,
        TelegramMessageElement.Detail,
        TelegramMessageElement.Link,
        TelegramMessageElement.Source,
        TelegramMessageElement.Timestamp,
        TelegramMessageElement.Spacer {

    record Title(String text, String url) implements TelegramMessageElement { }
    record Body(String text) implements TelegramMessageElement { }
    record Author(String text, String profileUrl) implements TelegramMessageElement { }
    record Detail(String text) implements TelegramMessageElement { }
    record Link(String label, String url) implements TelegramMessageElement { }
    record Source(String label, String url) implements TelegramMessageElement { }
    record Timestamp(long epochSeconds) implements TelegramMessageElement { }
    record Spacer() implements TelegramMessageElement { }
}
