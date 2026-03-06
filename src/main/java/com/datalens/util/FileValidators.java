package com.datalens.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class FileValidators {
    public static final long LARGE_FILE_WARNING_BYTES = 25L * 1024L * 1024L;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("csv", "xlsx");

    private FileValidators() {
    }

    public static void validateSelectedFile(Path file) throws IOException {
        if (file == null || !Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IOException("Selected file is not available.");
        }

        String extension = extensionOf(file);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IOException("Only CSV and XLSX files are supported.");
        }

        if ("xlsx".equals(extension) && !looksLikeZipArchive(file)) {
            throw new IOException("The selected XLSX file does not look like a valid Office Open XML archive.");
        }

        if ("csv".equals(extension) && !looksLikeTextFile(file)) {
            throw new IOException("The selected CSV file does not look like readable text data.");
        }
    }

    public static boolean shouldWarnAboutLargeFile(Path file) throws IOException {
        return Files.size(file) > LARGE_FILE_WARNING_BYTES;
    }

    public static String extensionOf(Path file) {
        String fileName = file.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kilobytes = bytes / 1024.0d;
        if (kilobytes < 1024.0d) {
            return "%.1f KB".formatted(kilobytes);
        }
        return "%.1f MB".formatted(kilobytes / 1024.0d);
    }

    private static boolean looksLikeZipArchive(Path file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] header = inputStream.readNBytes(4);
            return header.length == 4
                    && header[0] == 'P'
                    && header[1] == 'K'
                    && (header[2] == 3 || header[2] == 5 || header[2] == 7)
                    && (header[3] == 4 || header[3] == 6 || header[3] == 8);
        }
    }

    private static boolean looksLikeTextFile(Path file) throws IOException {
        byte[] buffer = new byte[4096];
        try (InputStream inputStream = Files.newInputStream(file)) {
            int read = inputStream.read(buffer);
            if (read <= 0) {
                return true;
            }

            int suspiciousControlChars = 0;
            for (int index = 0; index < read; index++) {
                byte current = buffer[index];
                if (current == 0) {
                    return false;
                }
                if (current < 32 && current != '\n' && current != '\r' && current != '\t') {
                    suspiciousControlChars++;
                }
            }
            return suspiciousControlChars < Math.max(4, read / 20);
        }
    }
}
