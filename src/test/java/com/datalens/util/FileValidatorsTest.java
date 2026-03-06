package com.datalens.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileValidatorsTest {
    @Test
    void extensionOfNormalizesCase() {
        assertEquals("csv", FileValidators.extensionOf(Path.of("Data.CSV")));
        assertEquals("xlsx", FileValidators.extensionOf(Path.of("data.Xlsx")));
    }

    @Test
    void validateSelectedFileAcceptsCsv() {
        Path csvPath = projectPath("samples", "sample_valid.csv");
        assertDoesNotThrow(() -> FileValidators.validateSelectedFile(csvPath));
    }

    @Test
    void validateSelectedFileRejectsUnsupportedExtension() throws IOException {
        Path tempFile = Files.createTempFile("datalens-test-", ".txt");
        try {
            Files.writeString(tempFile, "hello", StandardCharsets.UTF_8);
            IOException exception = assertThrows(IOException.class, () -> FileValidators.validateSelectedFile(tempFile));
            assertTrue(exception.getMessage().contains("Only CSV and XLSX files are supported."));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private Path projectPath(String... segments) {
        Path path = Path.of("");
        for (String segment : segments) {
            path = path.resolve(segment);
        }
        return path.toAbsolutePath().normalize();
    }
}
