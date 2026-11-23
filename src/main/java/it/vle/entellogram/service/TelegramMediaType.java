package it.vle.entellogram.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Classifica i file multimediali in base al MIME type o all'estensione. */
enum TelegramMediaType {
    PHOTO,
    VIDEO,
    OTHER;

    static TelegramMediaType from(Path path) throws IOException {
        String contentType = Files.probeContentType(path);
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);

        if ((contentType != null && contentType.startsWith("image/") && !contentType.equals("image/gif"))
                || filename.endsWith(".jpg") || filename.endsWith(".jpeg")
                || filename.endsWith(".png") || filename.endsWith(".webp")) {
            return PHOTO;
        }
        if ((contentType != null && contentType.startsWith("video/"))
                || filename.endsWith(".mp4") || filename.endsWith(".mov")
                || filename.endsWith(".m4v") || filename.endsWith(".webm")) {
            return VIDEO;
        }
        return OTHER;
    }
}
