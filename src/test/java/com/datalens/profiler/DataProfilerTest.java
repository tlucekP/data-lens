package com.datalens.profiler;

import com.datalens.loader.CsvLoader;
import com.datalens.loader.LoadedDataset;
import com.datalens.model.ColumnProfile;
import com.datalens.model.ColumnType;
import com.datalens.model.DatasetProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataProfilerTest {
    private final CsvLoader csvLoader = new CsvLoader();
    private final DataProfiler profiler = new DataProfiler();

    @Test
    void profilesSampleCsvWithExpectedCoreSignals() throws Exception {
        LoadedDataset dataset = csvLoader.load(projectPath("samples", "sample_valid.csv"));
        DatasetProfile profile = profiler.profile(dataset);

        assertEquals(6, profile.rowCount());
        assertEquals(6, profile.columnCount());
        assertEquals(1, profile.emptyRowCount());
        assertTrue(profile.headerDetected());

        ColumnProfile amountColumn = findColumn(profile, "amount");
        assertEquals(ColumnType.NUMBER, amountColumn.type());
        assertEquals(1L, amountColumn.negativeCount());

        ColumnProfile orderDateColumn = findColumn(profile, "order_date");
        assertEquals(ColumnType.DATE, orderDateColumn.type());
        assertTrue(orderDateColumn.mixedDateFormats());
    }

    private ColumnProfile findColumn(DatasetProfile profile, String name) {
        return profile.columns().stream()
                .filter(column -> column.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Column not found: " + name));
    }

    private Path projectPath(String... segments) {
        Path path = Path.of("");
        for (String segment : segments) {
            path = path.resolve(segment);
        }
        return path.toAbsolutePath().normalize();
    }
}
