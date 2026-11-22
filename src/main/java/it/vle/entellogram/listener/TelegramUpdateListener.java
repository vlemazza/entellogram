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
        // Gli aggiornamenti diversi dai messaggi (ad esempio callback o query inline) non servono al bot.
        Message message = update.message();
        if (message == null || message.chat() == null) {
            return;
        }

        // Ignora gli altri bot per evitare risposte ricorsive.
        if (message.from() != null && Boolean.TRUE.equals(message.from().isBot())) {
            return;
        }

        long chatId = message.chat().id();
        int messageId = message.messageId();
        // I link possono arrivare sia nel testo sia nella didascalia di un contenuto.
        String text = message.text();
        if (text == null || text.isBlank()) {
            text = message.caption();
        }
        if (text != null && !text.isBlank()) {
            postController.submitMessage(text, chatId, messageId);
        }
    }
}
