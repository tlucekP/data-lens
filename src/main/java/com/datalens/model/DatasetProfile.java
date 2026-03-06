package com.datalens.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record DatasetProfile(
        Path sourcePath,
        String fileName,
        String sheetName,
        int rowCount,
        int columnCount,
        int emptyRowCount,
        boolean headerDetected,
        long sourceFileSizeBytes,
        List<ColumnProfile> columns,
        List<String> warnings,
        String summary
) {
    public DatasetProfile {
        fileName = Objects.requireNonNullElse(fileName, "");
        sheetName = Objects.requireNonNullElse(sheetName, "");
        rowCount = Math.max(0, rowCount);
        columnCount = Math.max(0, columnCount);
        emptyRowCount = Math.max(0, emptyRowCount);
        sourceFileSizeBytes = Math.max(0L, sourceFileSizeBytes);
        columns = List.copyOf(Objects.requireNonNullElse(columns, List.of()));
        warnings = List.copyOf(Objects.requireNonNullElse(warnings, List.of()));
        summary = Objects.requireNonNullElse(summary, "");
    }

    public DatasetProfile withGeneratedContent(List<String> warnings, String summary) {
        return new DatasetProfile(
                sourcePath,
                fileName,
                sheetName,
                rowCount,
                columnCount,
                emptyRowCount,
                headerDetected,
                sourceFileSizeBytes,
                columns,
                warnings,
                summary
        );
    }
}
