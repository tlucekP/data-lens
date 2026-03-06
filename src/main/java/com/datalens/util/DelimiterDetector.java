package com.datalens.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DelimiterDetector {
    private static final char[] CANDIDATES = {',', ';', '\t', '|'};

    private DelimiterDetector() {
    }

    public static char detect(Path file) throws IOException {
        Map<Character, Integer> nonZeroLineHits = new LinkedHashMap<>();
        Map<Character, Integer> totalDelimiterHits = new LinkedHashMap<>();
        for (char candidate : CANDIDATES) {
            nonZeroLineHits.put(candidate, 0);
            totalDelimiterHits.put(candidate, 0);
        }

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int processedLines = 0;
            while ((line = reader.readLine()) != null && processedLines < 20) {
                if (processedLines == 0 && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
                    line = line.substring(1);
                }
                if (line.isBlank()) {
                    continue;
                }

                processedLines++;
                for (char candidate : CANDIDATES) {
                    int delimiterCount = countOutsideQuotes(line, candidate);
                    if (delimiterCount > 0) {
                        nonZeroLineHits.computeIfPresent(candidate, (key, value) -> value + 1);
                        totalDelimiterHits.computeIfPresent(candidate, (key, value) -> value + delimiterCount);
                    }
                }
            }
        }

        char bestCandidate = ',';
        int bestLineHits = -1;
        int bestTotalHits = -1;
        for (char candidate : CANDIDATES) {
            int lineHits = nonZeroLineHits.get(candidate);
            int totalHits = totalDelimiterHits.get(candidate);
            if (lineHits > bestLineHits || (lineHits == bestLineHits && totalHits > bestTotalHits)) {
                bestCandidate = candidate;
                bestLineHits = lineHits;
                bestTotalHits = totalHits;
            }
        }

        return bestLineHits <= 0 ? ',' : bestCandidate;
    }

    private static int countOutsideQuotes(String line, char delimiter) {
        boolean inQuotes = false;
        int count = 0;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && current == delimiter) {
                count++;
            }
        }
        return count;
    }
}
