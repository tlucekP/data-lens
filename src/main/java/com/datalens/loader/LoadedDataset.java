package com.datalens.loader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record LoadedDataset(
        Path sourcePath,
        String fileName,
        String sheetName,
        boolean headerDetected,
        int emptyRowCount,
        long sourceFileSizeBytes,
        List<String> columnNames,
        List<List<String>> rows
) {
    public LoadedDataset {
        fileName = Objects.requireNonNullElse(fileName, "");
        sheetName = Objects.requireNonNullElse(sheetName, "");
        emptyRowCount = Math.max(0, emptyRowCount);
        sourceFileSizeBytes = Math.max(0L, sourceFileSizeBytes);
        columnNames = columnNames == null ? List.of() : List.copyOf(columnNames);

        List<List<String>> safeRows = new ArrayList<>();
        if (rows != null) {
            for (List<String> row : rows) {
                safeRows.add(row == null ? List.of() : List.copyOf(row));
            }
        }
        rows = List.copyOf(safeRows);
    }
}