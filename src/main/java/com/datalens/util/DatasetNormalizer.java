package com.datalens.util;

import com.datalens.loader.LoadedDataset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DatasetNormalizer {
    private DatasetNormalizer() {
    }

    public static LoadedDataset normalize(Path sourcePath, String sheetName, List<List<String>> rawRows) throws IOException {
        int emptyRowCount = 0;
        List<List<String>> nonEmptyRows = new ArrayList<>();

        for (List<String> row : rawRows) {
            List<String> cleanedRow = cleanRow(row);
            if (isRowEmpty(cleanedRow)) {
                emptyRowCount++;
            } else {
                nonEmptyRows.add(cleanedRow);
            }
        }

        int width = nonEmptyRows.stream().mapToInt(List::size).max().orElse(0);
        long fileSize = Files.size(sourcePath);

        if (width == 0) {
            return new LoadedDataset(
                    sourcePath,
                    sourcePath.getFileName().toString(),
                    sheetName,
                    false,
                    emptyRowCount,
                    fileSize,
                    List.of(),
                    List.of()
            );
        }

        List<List<String>> normalizedRows = nonEmptyRows.stream()
                .map(row -> padRow(row, width))
                .toList();

        boolean headerDetected = HeaderDetector.detectHeader(normalizedRows);
        List<String> columnNames = headerDetected
                ? buildColumnNames(normalizedRows.get(0), width)
                : buildGeneratedColumnNames(width);

        List<List<String>> dataRows = headerDetected
                ? normalizedRows.stream().skip(1).toList()
                : normalizedRows;

        return new LoadedDataset(
                sourcePath,
                sourcePath.getFileName().toString(),
                sheetName,
                headerDetected,
                emptyRowCount,
                fileSize,
                columnNames,
                dataRows
        );
    }

    private static List<String> cleanRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return List.of();
        }
        return row.stream()
                .map(value -> value == null ? "" : value.strip())
                .toList();
    }

    private static boolean isRowEmpty(List<String> row) {
        return row.stream().allMatch(DataValueParser::isBlank);
    }

    private static List<String> padRow(List<String> row, int width) {
        List<String> padded = new ArrayList<>(row);
        while (padded.size() < width) {
            padded.add("");
        }
        return List.copyOf(padded);
    }

    private static List<String> buildColumnNames(List<String> headerRow, int width) {
        Map<String, Integer> seenNames = new LinkedHashMap<>();
        List<String> names = new ArrayList<>(width);
        for (int index = 0; index < width; index++) {
            String baseName = index < headerRow.size() ? headerRow.get(index).strip() : "";
            if (baseName.isBlank()) {
                baseName = "Column " + (index + 1);
            }

            int duplicateCount = seenNames.merge(baseName, 1, Integer::sum);
            if (duplicateCount > 1) {
                names.add(baseName + " (" + duplicateCount + ")");
            } else {
                names.add(baseName);
            }
        }
        return List.copyOf(names);
    }

    private static List<String> buildGeneratedColumnNames(int width) {
        List<String> names = new ArrayList<>(width);
        for (int index = 0; index < width; index++) {
            names.add("Column " + (index + 1));
        }
        return List.copyOf(names);
    }
}
