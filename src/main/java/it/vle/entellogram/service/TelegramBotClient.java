package it.vle.entellogram.service;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.LinkPreviewOptions;
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

/** Adapter between EntelloGram operations and the Pengrad library. */
public class TelegramBotClient implements AutoCloseable {

    private final TelegramBot bot;

    public TelegramBotClient(String token) {
        this.bot = new TelegramBot(token);
    }

    public void setUpdatesListener(UpdatesListener listener, ExceptionHandler exceptionHandler) {
        bot.setUpdatesListener(listener, exceptionHandler);
    }

    /** Sends text in chunks that respect Telegram's message limit. */

    public int sendMessage(long chatId, String text, Integer replyToMessageId) throws IOException {
        int start = 0;
        int length = text.codePointCount(0, text.length());
        int lastMessageId = -1;
        while (start < length) {
            int end = Math.min(start + 4_000, length);
            int startIndex = text.offsetByCodePoints(0, start);
            int endIndex = text.offsetByCodePoints(0, end);
            SendMessage request = new SendMessage(chatId, text.substring(startIndex, endIndex))
                    .disableNotification(true);
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

    /** Sends a single HTML message, used for quotes and text-post details. */
    public void sendHtmlMessage(long chatId, String html) throws IOException {
        SendMessage request = new SendMessage(chatId, html)
                .parseMode(ParseMode.HTML)
                .linkPreviewOptions(new LinkPreviewOptions().isDisabled(true))
                .disableNotification(true);
        ensureOk(bot.execute(request));
    }

    public void deleteMessage(long chatId, int messageId) throws IOException {
        ensureOk(bot.execute(new DeleteMessage(chatId, messageId)));
    }

    /** Sends audio as a reply to the message that triggered the meme. */
    public void sendAudio(long chatId, Path path, int replyToMessageId) throws IOException {
        SendAudio request = new SendAudio(chatId, path.toFile())
                .replyParameters(new ReplyParameters(replyToMessageId))
                .disableNotification(true);
        ensureOk(bot.execute(request));
    }

    /** Sends a file as a reply to the message that triggered the download. */
    public void sendFile(long chatId, Path path, String caption, Integer replyToMessageId) throws IOException {
        File file = path.toFile();
        TelegramMediaType mediaType = TelegramMediaType.from(path);
        SendResponse response;
        if (mediaType == TelegramMediaType.PHOTO) {
            SendPhoto request = new SendPhoto(chatId, file)
                    .disableNotification(true);
            if (caption != null && !caption.isBlank()) {
                request.caption(caption).parseMode(ParseMode.HTML);
            }
            if (replyToMessageId != null) {
                request.replyParameters(new ReplyParameters(replyToMessageId));
            }
            response = bot.execute(request);
        } else if (mediaType == TelegramMediaType.VIDEO || caption != null) {
            SendVideo request = new SendVideo(chatId, file)
                    .disableNotification(true);
            if (caption != null && !caption.isBlank()) {
                request.caption(caption).parseMode(ParseMode.HTML);
            }
            if (replyToMessageId != null) {
                request.replyParameters(new ReplyParameters(replyToMessageId));
            }
            response = bot.execute(request);
        } else {
            SendDocument request = new SendDocument(chatId, file)
                    .disableNotification(true);
            if (replyToMessageId != null) {
                request.replyParameters(new ReplyParameters(replyToMessageId));
            }
            response = bot.execute(request);
        }
        ensureOk(response);
    }

    /** Sends each album as a reply to the message that triggered the download. */
    public void sendMediaGroup(
            long chatId,
            List<Path> paths,
            String caption,
            Integer replyToMessageId
    ) throws IOException {
        for (int start = 0; start < paths.size(); start += 10) {
            List<Path> group = paths.subList(start, Math.min(start + 10, paths.size()));
            String groupCaption = start == 0 ? caption : null;

            if (group.size() == 1) {
                sendFile(chatId, group.getFirst(), groupCaption, replyToMessageId);
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

            SendMediaGroup request = new SendMediaGroup(chatId, media.toArray(InputMedia[]::new))
                    .disableNotification(true);
            if (replyToMessageId != null) {
                request.replyParameters(new ReplyParameters(replyToMessageId));
            }
            MessagesResponse response = bot.execute(request);
            ensureOk(response);
        }
    }

    private void ensureOk(BaseResponse response) throws IOException {
        // Pengrad returns a response even for Telegram HTTP errors.
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
