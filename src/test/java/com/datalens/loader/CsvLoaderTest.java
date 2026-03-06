package com.datalens.loader;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvLoaderTest {
    private final CsvLoader loader = new CsvLoader();

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

    private Path projectPath(String... segments) {
        Path path = Path.of("");
        for (String segment : segments) {
            path = path.resolve(segment);
        }
        return path.toAbsolutePath().normalize();
    }
}
