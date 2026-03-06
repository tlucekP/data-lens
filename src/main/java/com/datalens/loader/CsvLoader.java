package com.datalens.loader;

import com.datalens.util.DatasetNormalizer;
import com.datalens.util.DelimiterDetector;
import com.datalens.util.FileValidators;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CsvLoader {
    public LoadedDataset load(Path sourceFile) throws IOException {
        FileValidators.validateSelectedFile(sourceFile);
        char delimiter = DelimiterDetector.detect(sourceFile);

        List<List<String>> rows = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setIgnoreEmptyLines(false)
                .setAllowMissingColumnNames(true)
                .build();

        try (Reader reader = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            boolean firstRecord = true;
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                for (String value : record) {
                    String cleanedValue = value == null ? "" : value;
                    if (firstRecord && row.isEmpty() && !cleanedValue.isEmpty() && cleanedValue.charAt(0) == '\uFEFF') {
                        cleanedValue = cleanedValue.substring(1);
                    }
                    row.add(cleanedValue);
                }
                rows.add(row);
                firstRecord = false;
            }
        } catch (IllegalArgumentException exception) {
            throw new IOException("CSV parsing failed. Check delimiter consistency and quoted values.", exception);
        }

        return DatasetNormalizer.normalize(sourceFile, "Flat file", rows);
    }
}
