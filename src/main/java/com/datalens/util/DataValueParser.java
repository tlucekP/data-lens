package com.datalens.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;

public final class DataValueParser {
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            formatter("d/M/uuuu"),
            formatter("M/d/uuuu"),
            formatter("d.M.uuuu"),
            formatter("uuuu/M/d"),
            formatter("uuuu-M-d")
    );

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            formatter("uuuu-MM-dd HH:mm[:ss]"),
            formatter("uuuu-MM-dd'T'HH:mm[:ss]"),
            formatter("d/M/uuuu HH:mm[:ss]"),
            formatter("M/d/uuuu HH:mm[:ss]"),
            formatter("d.M.uuuu HH:mm[:ss]")
    );

    private DataValueParser() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static OptionalDouble parseNumber(String rawValue) {
        if (isBlank(rawValue)) {
            return OptionalDouble.empty();
        }

        String value = rawValue.strip()
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace("_", "");

        if (value.endsWith("%") || value.chars().anyMatch(Character::isLetter)) {
            return OptionalDouble.empty();
        }

        String normalized = normalizeNumericRepresentation(value);
        if (normalized == null) {
            return OptionalDouble.empty();
        }

        try {
            return OptionalDouble.of(Double.parseDouble(normalized));
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }

    public static Optional<LocalDate> parseDate(String rawValue) {
        if (isBlank(rawValue)) {
            return Optional.empty();
        }

        String value = rawValue.strip();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(value, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return Optional.of(LocalDateTime.parse(value, formatter).toLocalDate());
            } catch (DateTimeParseException ignored) {
            }
        }

        return Optional.empty();
    }

    public static String detectDatePattern(String rawValue) {
        if (isBlank(rawValue)) {
            return "";
        }

        String datePart = rawValue.strip().split("[T ]", 2)[0];
        if (datePart.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
            return "yyyy-mm-dd";
        }
        if (datePart.matches("\\d{4}/\\d{1,2}/\\d{1,2}")) {
            return "yyyy/mm/dd";
        }
        if (datePart.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            return "slash-date";
        }
        if (datePart.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{4}")) {
            return "dot-date";
        }
        return "other-date";
    }

    private static DateTimeFormatter formatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH)
                .withResolverStyle(ResolverStyle.SMART);
    }

    private static String normalizeNumericRepresentation(String value) {
        if (value.matches("[-+]?\\d+(\\.\\d+)?")) {
            return value;
        }
        if (value.matches("[-+]?\\d+,\\d+")) {
            return value.replace(',', '.');
        }
        if (value.matches("[-+]?\\d{1,3}(,\\d{3})+(\\.\\d+)?")) {
            return value.replace(",", "");
        }
        if (value.matches("[-+]?\\d{1,3}(\\.\\d{3})+(,\\d+)?")) {
            return value.replace(".", "").replace(',', '.');
        }
        return null;
    }
}
