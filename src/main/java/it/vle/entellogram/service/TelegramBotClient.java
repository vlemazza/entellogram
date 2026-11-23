package it.vle.entellogram.service;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.request.ReplyParameters;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.InputMedia;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.model.request.InputMediaVideo;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendAudio;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendVideo;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.MessagesResponse;
import com.pengrad.telegrambot.response.SendResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    public int sendMessage(long chatId, String text, Integer replyToMessageId) throws IOException {
        int start = 0;
        int length = text.codePointCount(0, text.length());
        int lastMessageId = -1;
        while (start < length) {
            int end = Math.min(start + 4_000, length);
            int startIndex = text.offsetByCodePoints(0, start);
            int endIndex = text.offsetByCodePoints(0, end);
            SendMessage request = new SendMessage(chatId, text.substring(startIndex, endIndex));
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

    /** Invia un singolo messaggio HTML, usato per le citazioni e i dettagli dei post testuali. */
    public void sendHtmlMessage(long chatId, String html) throws IOException {
        SendMessage request = new SendMessage(chatId, html).parseMode(ParseMode.HTML);
        ensureOk(bot.execute(request));
    }

    public void deleteMessage(long chatId, int messageId) throws IOException {
        ensureOk(bot.execute(new DeleteMessage(chatId, messageId)));
    }

    /** Invia un audio come risposta al messaggio che ha attivato il meme. */
    public void sendAudio(long chatId, Path path, int replyToMessageId) throws IOException {
        SendAudio request = new SendAudio(chatId, path.toFile())
                .replyParameters(new ReplyParameters(replyToMessageId));
        ensureOk(bot.execute(request));
    }

    /** Sceglie il metodo Telegram in base al tipo di file riconosciuto dal sistema. */
    public void sendFile(long chatId, Path path) throws IOException {
        sendFile(chatId, path, null);
    }

    /** Invia un file con didascalia HTML, usata per tenere descrizione e video nello stesso messaggio. */
    public void sendFile(long chatId, Path path, String caption) throws IOException {
        File file = path.toFile();
        TelegramMediaType mediaType = TelegramMediaType.from(path);
        SendResponse response;
        if (mediaType == TelegramMediaType.PHOTO) {
            SendPhoto request = new SendPhoto(chatId, file);
            if (caption != null && !caption.isBlank()) {
                request.caption(caption).parseMode(ParseMode.HTML);
            }
            response = bot.execute(request);
        } else if (mediaType == TelegramMediaType.VIDEO || caption != null) {
            SendVideo request = new SendVideo(chatId, file);
            if (caption != null && !caption.isBlank()) {
                request.caption(caption).parseMode(ParseMode.HTML);
            }
            response = bot.execute(request);
        } else {
            response = bot.execute(new SendDocument(chatId, file));
        }
        ensureOk(response);
    }

    /** Invia foto e video come album Telegram, dividendoli in gruppi da massimo dieci. */
    public void sendMediaGroup(long chatId, List<Path> paths, String caption) throws IOException {
        for (int start = 0; start < paths.size(); start += 10) {
            List<Path> group = paths.subList(start, Math.min(start + 10, paths.size()));
            String groupCaption = start == 0 ? caption : null;

            if (group.size() == 1) {
                sendFile(chatId, group.getFirst(), groupCaption);
                continue;
            }

            List<InputMedia<?>> media = new ArrayList<>(group.size());
            for (int i = 0; i < group.size(); i++) {
                Path path = group.get(i);
                File file = path.toFile();
                TelegramMediaType mediaType = TelegramMediaType.from(path);
                InputMedia<?> item;
                if (mediaType == TelegramMediaType.PHOTO) {
                    item = new InputMediaPhoto(file);
                } else if (mediaType == TelegramMediaType.VIDEO) {
                    item = new InputMediaVideo(file);
                } else {
                    throw new IOException("Telegram albums support only photos and videos: " + path);
                }

                if (i == 0 && groupCaption != null && !groupCaption.isBlank()) {
                    item.caption(groupCaption).parseMode(ParseMode.HTML);
                }
                media.add(item);
            }

            MessagesResponse response = bot.execute(new SendMediaGroup(chatId, media.toArray(InputMedia[]::new)));
            ensureOk(response);
        }
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
