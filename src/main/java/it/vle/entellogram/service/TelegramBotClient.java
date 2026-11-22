package it.vle.entellogram.service;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.request.ReplyParameters;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendVideo;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Adattatore tra le operazioni usate da entellogram e la libreria Pengrad. */
public class TelegramBotClient implements AutoCloseable {

    private final TelegramBot bot;

    public TelegramBotClient(String token) {
        this.bot = new TelegramBot(token);
    }

    public void setUpdatesListener(UpdatesListener listener, ExceptionHandler exceptionHandler) {
        bot.setUpdatesListener(listener, exceptionHandler);
    }

    /** Invia il testo, dividendolo in messaggi che rispettano il limite Telegram. */
    public int sendMessage(long chatId, String text) throws IOException {
        return sendMessage(chatId, text, null);
    }

    public int sendMessage(long chatId, String text, Integer replyToMessageId) throws IOException {
        int start = 0;
        int length = text.codePointCount(0, text.length());
        int lastMessageId = -1;
        while (start < length) {
            int end = Math.min(start + 4_000, length);
            int startIndex = text.offsetByCodePoints(0, start);
            int endIndex = text.offsetByCodePoints(0, end);
            SendMessage request = new SendMessage(chatId, text.substring(startIndex, endIndex));
            // Se richiesto, collega il messaggio alla richiesta originale dell'utente.
            if (replyToMessageId != null) {
                request.replyParameters(new ReplyParameters(replyToMessageId));
            }
            SendResponse response = bot.execute(request);
            ensureOk(response);
            lastMessageId = response.message().messageId();
            start = end;
        }
        return lastMessageId;
    }

    public void deleteMessage(long chatId, int messageId) throws IOException {
        ensureOk(bot.execute(new DeleteMessage(chatId, messageId)));
    }

    /** Sceglie il metodo Telegram in base al tipo di file riconosciuto dal sistema. */
    public void sendFile(long chatId, Path path) throws IOException {
        File file = path.toFile();
        String contentType = Files.probeContentType(path);
        SendResponse response;
        if (contentType != null && contentType.startsWith("image/") && !contentType.equals("image/gif")) {
            response = bot.execute(new SendPhoto(chatId, file));
        } else if (contentType != null && contentType.startsWith("video/")) {
            response = bot.execute(new SendVideo(chatId, file));
        } else {
            response = bot.execute(new SendDocument(chatId, file));
        }
        ensureOk(response);
    }

    private void ensureOk(BaseResponse response) throws IOException {
        // Pengrad restituisce una risposta anche per gli errori HTTP di Telegram.
        if (response == null || !response.isOk()) {
            String description = response == null ? "Empty response from Telegram" : response.description();
            throw new IOException("Telegram Bot API request failed: " + description);
        }
    }

    @Override
    public void close() {
        bot.removeGetUpdatesListener();
    }
}
