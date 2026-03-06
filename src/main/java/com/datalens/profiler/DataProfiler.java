package com.datalens.profiler;

import com.datalens.loader.LoadedDataset;
import com.datalens.model.ColumnProfile;
import com.datalens.model.DatasetProfile;
import com.datalens.util.DataValueParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DataProfiler {
    private final TypeDetector typeDetector = new TypeDetector();
    private final StatsCalculator statsCalculator = new StatsCalculator();

    public DatasetProfile profile(LoadedDataset dataset) {
        int rowCount = dataset.rows().size();
        int columnCount = dataset.columnNames().size();

        List<ColumnProfile> columnProfiles = new ArrayList<>(columnCount);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            List<String> columnValues = extractColumnValues(dataset.rows(), columnIndex);
            long missingCount = columnValues.stream().filter(DataValueParser::isBlank).count();
            Set<String> uniqueValues = new LinkedHashSet<>();
            columnValues.stream()
                    .filter(value -> !DataValueParser.isBlank(value))
                    .map(String::strip)
                    .forEach(uniqueValues::add);

            TypeDetector.DetectionResult detection = typeDetector.detect(columnValues);
            StatsCalculator.ColumnStats stats = statsCalculator.calculate(detection.type(), columnValues, detection);

            double missingRatio = rowCount == 0 ? 0.0d : missingCount / (double) rowCount;
            double uniqueRatio = detection.nonEmptyCount() == 0
                    ? 0.0d
                    : uniqueValues.size() / (double) detection.nonEmptyCount();

            columnProfiles.add(new ColumnProfile(
                    dataset.columnNames().get(columnIndex),
                    detection.type(),
                    missingRatio,
                    uniqueValues.size(),
                    stats.statsText(),
                    detection.nonEmptyCount(),
                    stats.negativeCount(),
                    stats.outlierCount(),
                    uniqueRatio,
                    detection.mixedDateFormats()
            ));
        }

        return new DatasetProfile(
                dataset.sourcePath(),
                dataset.fileName(),
                dataset.sheetName(),
                rowCount,
                columnCount,
                dataset.emptyRowCount(),
                dataset.headerDetected(),
                dataset.sourceFileSizeBytes(),
                columnProfiles,
                List.of(),
                ""
        );
    }

    private List<String> extractColumnValues(List<List<String>> rows, int columnIndex) {
        List<String> values = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            values.add(columnIndex < row.size() ? row.get(columnIndex) : "");
        }
        return List.copyOf(values);
    }
}
