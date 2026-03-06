package com.datalens.profiler;

import com.datalens.model.ColumnType;
import com.datalens.util.DataValueParser;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatsCalculator {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.###");

    public ColumnStats calculate(ColumnType type, List<String> rawValues, TypeDetector.DetectionResult detection) {
        return switch (type) {
            case NUMBER -> buildNumericStats(detection.numericValues());
            case DATE -> buildDateStats(detection.dateValues());
            case TEXT -> buildTextStats(rawValues);
        };
    }

    public record ColumnStats(String statsText, long negativeCount, long outlierCount) {
        public ColumnStats {
            statsText = statsText == null ? "" : statsText;
            negativeCount = Math.max(0L, negativeCount);
            outlierCount = Math.max(0L, outlierCount);
        }
    }

    private ColumnStats buildNumericStats(List<Double> values) {
        if (values.isEmpty()) {
            return new ColumnStats("No numeric values", 0L, 0L);
        }

        DoubleSummaryStatistics statistics = values.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        long negativeCount = values.stream().filter(value -> value < 0.0d).count();
        long outlierCount = countOutliers(values);

        String statsText = "min %s | max %s | avg %s".formatted(
                NUMBER_FORMAT.format(statistics.getMin()),
                NUMBER_FORMAT.format(statistics.getMax()),
                NUMBER_FORMAT.format(statistics.getAverage())
        );
        return new ColumnStats(statsText, negativeCount, outlierCount);
    }

    private ColumnStats buildDateStats(List<LocalDate> values) {
        if (values.isEmpty()) {
            return new ColumnStats("No date values", 0L, 0L);
        }

        LocalDate minDate = Collections.min(values);
        LocalDate maxDate = Collections.max(values);
        String statsText = "min %s | max %s".formatted(minDate, maxDate);
        return new ColumnStats(statsText, 0L, 0L);
    }

    private ColumnStats buildTextStats(List<String> values) {
        List<String> nonEmptyValues = values.stream()
                .filter(value -> !DataValueParser.isBlank(value))
                .map(String::strip)
                .toList();

        if (nonEmptyValues.isEmpty()) {
            return new ColumnStats("No text values", 0L, 0L);
        }

        Map<String, Integer> frequencies = new LinkedHashMap<>();
        for (String value : nonEmptyValues) {
            frequencies.merge(value, 1, Integer::sum);
        }

        String topValue = "";
        int topCount = 0;
        for (Map.Entry<String, Integer> entry : frequencies.entrySet()) {
            if (entry.getValue() > topCount) {
                topValue = entry.getKey();
                topCount = entry.getValue();
            }
        }

        String statsText = topCount > 1
                ? "top '" + shorten(topValue) + "' (" + topCount + "x)"
                : "mostly unique text";
        return new ColumnStats(statsText, 0L, 0L);
    }

    private long countOutliers(List<Double> values) {
        if (values.size() < 6) {
            return 0L;
        }

        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        double q1 = percentile(sorted, 25.0d);
        double q3 = percentile(sorted, 75.0d);
        double iqr = q3 - q1;
        if (iqr == 0.0d) {
            return 0L;
        }

        double lowerBound = q1 - (1.5d * iqr);
        double upperBound = q3 + (1.5d * iqr);

        return sorted.stream()
                .filter(value -> value < lowerBound || value > upperBound)
                .count();
    }

    private double percentile(List<Double> values, double percentile) {
        if (values.isEmpty()) {
            return 0.0d;
        }

        double index = percentile / 100.0d * (values.size() - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);

        if (lowerIndex == upperIndex) {
            return values.get(lowerIndex);
        }

        double fraction = index - lowerIndex;
        return values.get(lowerIndex) + fraction * (values.get(upperIndex) - values.get(lowerIndex));
    }

    private String shorten(String value) {
        if (value.length() <= 24) {
            return value;
        }
        return value.substring(0, 21) + "...";
    }
}
