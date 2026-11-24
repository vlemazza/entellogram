package it.vle.entellogram.listener;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import it.vle.entellogram.service.TelegramPostController;

public class TelegramUpdateListener {

    private final TelegramPostController postController;

    public TelegramUpdateListener(TelegramPostController postController) {
        this.postController = postController;
    }

    public void onUpdate(Update update) {
        // Ignore updates other than messages (for example, callback or inline queries).
        Message message = update.message();
        if (message == null || message.chat() == null) {
            return;
        }

        // Ignore other bots to prevent recursive replies.
        if (message.from() != null && Boolean.TRUE.equals(message.from().isBot())) {
            return;
        }

        long chatId = message.chat().id();
        int messageId = message.messageId();
        // Links may appear in either the message text or its caption.
        String text = message.text();
        if (text == null || text.isBlank()) {
            text = message.caption();
        }
        if (text != null && !text.isBlank()) {
            postController.submitMessage(text, chatId, messageId);
        }
    }
}
