package com.datalens.loader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XlsxLoaderTest {
    private final XlsxLoader loader = new XlsxLoader();

    @Test
    void loadsSampleValidXlsx() throws Exception {
        LoadedDataset dataset = loader.load(projectPath("samples", "sample_valid.xlsx"));

        assertFalse(dataset.columnNames().isEmpty());
        assertFalse(dataset.rows().isEmpty());
        assertTrue(dataset.headerDetected());
    }

    @Test
    void rejectsBrokenXlsx() {
        IOException exception = assertThrows(IOException.class,
                () -> loader.load(projectPath("samples", "sample_broken.xlsx")));

        assertTrue(exception.getMessage().contains("does not look like a valid Office Open XML archive"));
    }

    private Path projectPath(String... segments) {
        Path path = Path.of("");
        for (String segment : segments) {
            path = path.resolve(segment);
        }
        return path.toAbsolutePath().normalize();
    }
}
