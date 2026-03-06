package com.datalens.model;

import java.util.Objects;

public record ColumnProfile(
        String name,
        ColumnType type,
        double missingRatio,
        int uniqueCount,
        String statsText,
        int nonEmptyCount,
        long negativeCount,
        long outlierCount,
        double uniqueRatio,
        boolean mixedDateFormats
) {
    public ColumnProfile {
        name = Objects.requireNonNullElse(name, "");
        type = Objects.requireNonNullElse(type, ColumnType.TEXT);
        statsText = Objects.requireNonNullElse(statsText, "");
        missingRatio = Math.max(0.0d, missingRatio);
        uniqueCount = Math.max(0, uniqueCount);
        nonEmptyCount = Math.max(0, nonEmptyCount);
        negativeCount = Math.max(0L, negativeCount);
        outlierCount = Math.max(0L, outlierCount);
        uniqueRatio = Math.max(0.0d, uniqueRatio);
    }

    public String missingPercentageText() {
        return "%.1f%%".formatted(missingRatio * 100.0d);
    }
}
