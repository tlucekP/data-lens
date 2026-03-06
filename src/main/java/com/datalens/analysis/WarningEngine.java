package com.datalens.analysis;

import com.datalens.model.ColumnProfile;
import com.datalens.model.ColumnType;
import com.datalens.model.DatasetProfile;
import com.datalens.util.FileValidators;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class WarningEngine {
    public List<String> analyze(DatasetProfile profile) {
        Set<String> warnings = new LinkedHashSet<>();

        if (profile.rowCount() == 0 || profile.columnCount() == 0) {
            warnings.add("Dataset is almost empty and may not contain enough usable rows for analysis.");
        } else {
            double fillRatio = calculateFillRatio(profile);
            if (fillRatio < 0.2d) {
                warnings.add("Dataset is sparse and may be almost empty after excluding missing values.");
            }
        }

        if (profile.sourceFileSizeBytes() > FileValidators.LARGE_FILE_WARNING_BYTES) {
            warnings.add("Source file is relatively large (" + FileValidators.formatFileSize(profile.sourceFileSizeBytes()) + ") and may load more slowly.");
        }

        for (ColumnProfile column : profile.columns()) {
            if (column.missingRatio() >= 0.35d && profile.rowCount() > 0) {
                warnings.add("Column '" + column.name() + "' has a high share of missing values (" + column.missingPercentageText() + ").");
            }

            if (isGenericColumnName(column.name())) {
                warnings.add("Column '" + column.name() + "' has a generic or unclear name.");
            }

            if (column.type() == ColumnType.NUMBER && column.negativeCount() > 0 && looksNonNegativeByBusinessMeaning(column.name())) {
                warnings.add("Column '" + column.name() + "' contains negative values and may need a business-rule check.");
            }

            if (column.type() == ColumnType.NUMBER && column.outlierCount() > 0) {
                warnings.add("Column '" + column.name() + "' has possible numeric outliers (" + column.outlierCount() + ").");
            }

            if (column.type() == ColumnType.TEXT && column.nonEmptyCount() >= 5 && column.uniqueRatio() >= 0.9d) {
                warnings.add("Column '" + column.name() + "' looks highly unique and may behave like an identifier.");
            }

            if (column.type() == ColumnType.DATE && column.mixedDateFormats()) {
                warnings.add("Column '" + column.name() + "' appears to use inconsistent date formats.");
            }
        }

        return List.copyOf(warnings);
    }

    private double calculateFillRatio(DatasetProfile profile) {
        long filledCells = profile.columns().stream().mapToLong(ColumnProfile::nonEmptyCount).sum();
        long possibleCells = (long) profile.rowCount() * Math.max(profile.columnCount(), 1);
        return possibleCells == 0L ? 0.0d : filledCells / (double) possibleCells;
    }

    private boolean isGenericColumnName(String name) {
        String normalized = name == null ? "" : name.strip().toLowerCase(Locale.ROOT);
        return normalized.isBlank()
                || normalized.matches("column\\s+\\d+")
                || normalized.matches("field\\s*\\d*")
                || normalized.startsWith("unnamed")
                || normalized.equals("n/a")
                || normalized.equals("null");
    }

    private boolean looksNonNegativeByBusinessMeaning(String name) {
        String normalized = name == null ? "" : name.strip().toLowerCase(Locale.ROOT);
        return normalized.contains("price")
                || normalized.contains("amount")
                || normalized.contains("revenue")
                || normalized.contains("sales")
                || normalized.contains("qty")
                || normalized.contains("quantity")
                || normalized.contains("stock")
                || normalized.contains("count")
                || normalized.contains("age")
                || normalized.contains("total");
    }
}
