package com.datalens.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvLoaderTest {
    private final CsvLoader loader = new CsvLoader();

    @TempDir
    Path tempDir;

    @Test
    void loadsSampleValidCsv() throws Exception {
        LoadedDataset dataset = loader.load(projectPath("samples", "sample_valid.csv"));

        assertEquals("sample_valid.csv", dataset.fileName());
        assertTrue(dataset.headerDetected());
        assertEquals(1, dataset.emptyRowCount());
        assertEquals(List.of("order_id", "order_date", "customer_email", "status", "amount", "discount"), dataset.columnNames());
        assertEquals(6, dataset.rows().size());
        assertEquals("ORD-1001", dataset.rows().get(0).get(0));
        assertFalse(dataset.rows().isEmpty());
    }

    @Test
    void loadsUtf8BomCsvWithSemicolonDelimiter() throws Exception {
        Path csvPath = tempDir.resolve("bom.csv");
        Files.writeString(csvPath,
                "\uFEFFid;note\n1;\"quoted;value\"\n2;plain\n",
                StandardCharsets.UTF_8);

        LoadedDataset dataset = loader.load(csvPath);

        assertEquals(List.of("id", "note"), dataset.columnNames());
        assertEquals("quoted;value", dataset.rows().get(0).get(1));
    }

    @Test
    void loadsCsvWithVeryLongCellValue() throws Exception {
        Path csvPath = tempDir.resolve("long-row.csv");
        String longValue = "A".repeat(20_000);
        Files.writeString(csvPath,
                "id,description\n1," + longValue + "\n",
                StandardCharsets.UTF_8);

        LoadedDataset dataset = loader.load(csvPath);

        assertEquals(1, dataset.rows().size());
        assertEquals(longValue, dataset.rows().get(0).get(1));
    }

    private Path projectPath(String... segments) {
        Path path = Path.of("");
        for (String segment : segments) {
            path = path.resolve(segment);
        }
        return path.toAbsolutePath().normalize();
    }
}
