package com.datalens.profiler;

import com.datalens.model.ColumnType;
import com.datalens.util.DataValueParser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

public class TypeDetector {
    public DetectionResult detect(List<String> values) {
        List<Double> numericValues = new ArrayList<>();
        List<LocalDate> dateValues = new ArrayList<>();
        Set<String> datePatterns = new LinkedHashSet<>();
        int nonEmptyCount = 0;

        for (String value : values) {
            if (DataValueParser.isBlank(value)) {
                continue;
            }

            nonEmptyCount++;

            OptionalDouble numericCandidate = DataValueParser.parseNumber(value);
            if (numericCandidate.isPresent()) {
                numericValues.add(numericCandidate.getAsDouble());
            }

            Optional<LocalDate> dateCandidate = DataValueParser.parseDate(value);
            if (dateCandidate.isPresent()) {
                dateValues.add(dateCandidate.get());
                datePatterns.add(DataValueParser.detectDatePattern(value));
            }
        }

        if (nonEmptyCount == 0) {
            return new DetectionResult(ColumnType.TEXT, List.of(), List.of(), false, 0);
        }

        double numericRatio = numericValues.size() / (double) nonEmptyCount;
        double dateRatio = dateValues.size() / (double) nonEmptyCount;

        ColumnType detectedType;
        if (numericRatio >= 0.6d && numericValues.size() >= dateValues.size()) {
            detectedType = ColumnType.NUMBER;
        } else if (dateRatio >= 0.6d) {
            detectedType = ColumnType.DATE;
        } else {
            detectedType = ColumnType.TEXT;
        }

        return new DetectionResult(
                detectedType,
                numericValues,
                dateValues,
                datePatterns.size() > 1,
                nonEmptyCount
        );
    }

    public record DetectionResult(
            ColumnType type,
            List<Double> numericValues,
            List<LocalDate> dateValues,
            boolean mixedDateFormats,
            int nonEmptyCount
    ) {
        public DetectionResult {
            numericValues = List.copyOf(numericValues);
            dateValues = List.copyOf(dateValues);
            nonEmptyCount = Math.max(0, nonEmptyCount);
        }
    }
}
