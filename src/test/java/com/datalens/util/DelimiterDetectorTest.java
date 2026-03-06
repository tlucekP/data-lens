package com.datalens.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DelimiterDetectorTest {
    @TempDir
    Path tempDir;

    @Test
    void countOutsideQuotesIgnoresEscapedDoubleQuotes() {
        String line = "\"alpha \"\"quoted, value\"\"\",beta,gamma";

        assertEquals(2, DelimiterDetector.countOutsideQuotes(line, ','));
    }

    @Test
    void detectPrefersSemicolonWhenCommasAppearOnlyInsideEscapedQuotes() throws Exception {
        Path csvPath = tempDir.resolve("escaped-quotes.csv");
        String csvContent = String.join("\n",
                "\"name\";\"note\";\"status\"",
                "\"Alice\";\"said \"\"hello, team\"\"\";\"ok\"",
                "\"Bob\";\"left, then returned\";\"ok\"",
                "");
        Files.writeString(csvPath, csvContent, StandardCharsets.UTF_8);

        assertEquals(';', DelimiterDetector.detect(csvPath));
    }
}
