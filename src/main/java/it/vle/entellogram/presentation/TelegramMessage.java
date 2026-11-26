package it.vle.entellogram.presentation;

import java.util.List;

public record TelegramMessage(List<TelegramMessageElement> elements) {
    public TelegramMessage {
        elements = List.copyOf(elements);
    }
}
