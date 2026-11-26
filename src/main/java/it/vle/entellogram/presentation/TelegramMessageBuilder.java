package it.vle.entellogram.presentation;

import java.util.ArrayList;
import java.util.List;

public final class TelegramMessageBuilder {
    private final List<TelegramMessageElement> elements = new ArrayList<>();

    public TelegramMessageBuilder title(String text) {
        return title(text, null);
    }

    public TelegramMessageBuilder title(String text, String url) {
        if (present(text)) elements.add(new TelegramMessageElement.Title(text, url));
        return this;
    }
    public TelegramMessageBuilder space() {
        elements.add(new TelegramMessageElement.Spacer());
        return this;
    }
    public TelegramMessageBuilder body(String text) {
        if (present(text)) elements.add(new TelegramMessageElement.Body(text));
        return this;
    }
    public TelegramMessageBuilder author(String text, String profileUrl) {
        if (present(text)) elements.add(new TelegramMessageElement.Author(text, profileUrl));
        return this;
    }
    public TelegramMessageBuilder detail(String text) {
        if (present(text)) elements.add(new TelegramMessageElement.Detail(text));
        return this;
    }
    public TelegramMessageBuilder link(String label, String url) {
        if (present(label)) elements.add(new TelegramMessageElement.Link(label, url));
        return this;
    }
    public TelegramMessageBuilder source(String label, String url) {
        if (present(label)) elements.add(new TelegramMessageElement.Source(label, url));
        return this;
    }
    public TelegramMessageBuilder timestamp(Long epochSeconds) {
        if (epochSeconds != null) elements.add(new TelegramMessageElement.Timestamp(epochSeconds));
        return this;
    }
    public TelegramMessage build() {
        return new TelegramMessage(elements);
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
