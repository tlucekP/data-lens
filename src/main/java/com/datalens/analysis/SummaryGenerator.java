package com.datalens.analysis;

import com.datalens.model.ColumnProfile;
import com.datalens.model.ColumnType;
import com.datalens.model.DatasetProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SummaryGenerator {
    public String generate(DatasetProfile profile, List<String> warnings) {
        if (profile.rowCount() == 0 || profile.columnCount() == 0) {
            return "Dataset appears to be empty or structurally unusable. Check the source file before deeper analysis.";
        }

        List<String> sentences = new ArrayList<>();
        sentences.add(describeDataset(profile));
        sentences.add(describeImportantFields(profile));

        if (!warnings.isEmpty()) {
            sentences.add("Main issue: " + normalizeWarningForSummary(warnings.get(0)));
        } else {
            sentences.add("No major structural problems stand out, although header and type detection remain heuristic.");
        }

        if (hasSegmentationFriendlyColumns(profile)) {
            sentences.add("Several columns look suitable for filtering or segmentation.");
        }

        sentences.add(nextStepSuggestion(profile, warnings));
        return String.join(" ", sentences);
    }

    private String describeDataset(DatasetProfile profile) {
        List<String> names = profile.columns().stream().map(ColumnProfile::name).toList();
        if (matchesAny(names, "order", "transaction", "invoice", "payment")) {
            return "Dataset likely contains transaction or order records.";
        }
        if (matchesAny(names, "customer", "email", "phone", "contact")) {
            return "Dataset likely contains customer or contact data.";
        }
        if (matchesAny(names, "product", "sku", "inventory", "stock", "category")) {
            return "Dataset likely contains product or inventory records.";
        }
        if (matchesAny(names, "campaign", "source", "medium", "click", "session", "event")) {
            return "Dataset likely contains marketing or activity records.";
        }
        return "Dataset appears to be a tabular extract with %d rows and %d columns.".formatted(
                profile.rowCount(),
                profile.columnCount()
        );
    }

    private String describeImportantFields(DatasetProfile profile) {
        List<ColumnProfile> importantColumns = profile.columns().stream()
                .sorted(Comparator
                        .comparingInt(this::importanceScore)
                        .reversed()
                        .thenComparing(ColumnProfile::name))
                .limit(3)
                .toList();

        if (importantColumns.isEmpty()) {
            return "No clearly useful columns were detected.";
        }

        List<String> labels = importantColumns.stream()
                .map(column -> "'" + column.name() + "'")
                .toList();

        if (labels.size() == 1) {
            return "The main field appears to be " + labels.get(0) + ".";
        }
        if (labels.size() == 2) {
            return "Main fields appear to be " + labels.get(0) + " and " + labels.get(1) + ".";
        }
        return "Main fields appear to be " + labels.get(0) + ", " + labels.get(1) + ", and " + labels.get(2) + ".";
    }

    private String nextStepSuggestion(DatasetProfile profile, List<String> warnings) {
        boolean hasDateColumns = profile.columns().stream().anyMatch(column -> column.type() == ColumnType.DATE);
        boolean hasGenericNames = profile.columns().stream().anyMatch(column -> column.name().toLowerCase(Locale.ROOT).startsWith("column "));
        if (!warnings.isEmpty() || hasGenericNames) {
            return "Before deeper analysis, review flagged columns, confirm key identifiers, and validate missing or inconsistent values.";
        }
        if (hasDateColumns) {
            return "Before deeper analysis, confirm the date fields and check whether the main numeric columns align with the reporting period.";
        }
        return "Before deeper analysis, confirm the meaning of the main fields and keep in mind that the interpretation is heuristic.";
    }

    private boolean hasSegmentationFriendlyColumns(DatasetProfile profile) {
        return profile.columns().stream()
                .filter(column -> column.type() == ColumnType.TEXT)
                .filter(column -> column.uniqueRatio() >= 0.05d && column.uniqueRatio() <= 0.6d)
                .count() >= 2;
    }

    private int importanceScore(ColumnProfile column) {
        int score = 0;
        String name = column.name().toLowerCase(Locale.ROOT);
        if (column.type() == ColumnType.NUMBER) {
            score += 3;
        }
        if (column.type() == ColumnType.DATE) {
            score += 2;
        }
        if (name.contains("id") || name.contains("date") || name.contains("amount") || name.contains("price") || name.contains("status") || name.contains("email")) {
            score += 3;
        }
        if (column.missingRatio() < 0.2d) {
            score += 2;
        }
        if (column.uniqueRatio() > 0.0d && column.uniqueRatio() < 0.95d) {
            score += 1;
        }
        return score;
    }

    private boolean matchesAny(List<String> names, String... keywords) {
        for (String name : names) {
            String normalized = name.toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (normalized.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalizeWarningForSummary(String warning) {
        String cleaned = warning == null ? "" : warning.strip();
        if (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned + ".";
    }
}
