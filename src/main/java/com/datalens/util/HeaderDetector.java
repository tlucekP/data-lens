package com.datalens.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HeaderDetector {
    private HeaderDetector() {
    }

    public static boolean detectHeader(List<List<String>> rows) {
        if (rows.isEmpty()) {
            return false;
        }

        List<String> firstRow = rows.get(0);
        int nonBlankCells = countNonBlank(firstRow);
        if (nonBlankCells == 0) {
            return false;
        }

        List<String> secondRow = rows.size() > 1 ? rows.get(1) : List.of();
        double firstTypedRatio = typedRatio(firstRow);
        double secondTypedRatio = typedRatio(secondRow);
        double headerTokenRatio = headerTokenRatio(firstRow);
        boolean mostlyUnique = uniqueNonBlankCount(firstRow) == nonBlankCells;
        boolean shorterThanData = !secondRow.isEmpty() && averageLength(firstRow) <= averageLength(secondRow) + 2.0d;

        int score = 0;
        if (mostlyUnique) {
            score++;
        }
        if (headerTokenRatio >= 0.6d) {
            score++;
        }
        if (firstTypedRatio <= 0.2d) {
            score++;
        }
        if (!secondRow.isEmpty() && secondTypedRatio >= firstTypedRatio + 0.25d) {
            score++;
        }
        if (!secondRow.isEmpty() && shorterThanData) {
            score++;
        }

        return score >= 3 && (headerTokenRatio >= 0.5d || secondTypedRatio > firstTypedRatio);
    }

    private static int countNonBlank(List<String> row) {
        return (int) row.stream().filter(value -> !DataValueParser.isBlank(value)).count();
    }

    private static int uniqueNonBlankCount(List<String> row) {
        Set<String> values = new HashSet<>();
        row.stream()
                .filter(value -> !DataValueParser.isBlank(value))
                .map(String::strip)
                .forEach(values::add);
        return values.size();
    }

    private static double typedRatio(List<String> row) {
        if (row.isEmpty()) {
            return 0.0d;
        }

        int nonBlank = 0;
        int typed = 0;
        for (String value : row) {
            if (DataValueParser.isBlank(value)) {
                continue;
            }
            nonBlank++;
            if (DataValueParser.parseNumber(value).isPresent() || DataValueParser.parseDate(value).isPresent()) {
                typed++;
            }
        }
        return nonBlank == 0 ? 0.0d : typed / (double) nonBlank;
    }

    private static double headerTokenRatio(List<String> row) {
        int nonBlank = 0;
        int headerLike = 0;
        for (String value : row) {
            if (DataValueParser.isBlank(value)) {
                continue;
            }
            nonBlank++;
            String token = value.strip();
            if (token.length() <= 40
                    && !DataValueParser.parseNumber(token).isPresent()
                    && !DataValueParser.parseDate(token).isPresent()
                    && token.matches("[\\p{L}\\p{N}_ /\\-().#]+")) {
                headerLike++;
            }
        }
        return nonBlank == 0 ? 0.0d : headerLike / (double) nonBlank;
    }

    private static double averageLength(List<String> row) {
        int count = 0;
        int totalLength = 0;
        for (String value : row) {
            if (DataValueParser.isBlank(value)) {
                continue;
            }
            count++;
            totalLength += value.strip().length();
        }
        return count == 0 ? 0.0d : totalLength / (double) count;
    }
}
