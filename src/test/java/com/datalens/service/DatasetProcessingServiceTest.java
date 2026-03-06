package com.datalens.service;

import com.datalens.model.DatasetProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasetProcessingServiceTest {
    private final DatasetProcessingService service = new DatasetProcessingService();

    @Test
    void processesDatasetAndReportsProgressWithoutJavafxRuntime() throws Exception {
        List<String> progressMessages = new ArrayList<>();

        DatasetProfile profile = service.process(projectPath("samples", "sample_valid.csv"), progressMessages::add);

        assertFalse(profile.columns().isEmpty());
        assertFalse(profile.warnings().isEmpty());
        assertFalse(profile.summary().isBlank());
        assertTrue(progressMessages.contains("Validating file..."));
        assertTrue(progressMessages.contains("Loading dataset..."));
        assertTrue(progressMessages.contains("Profiling columns..."));
        assertTrue(progressMessages.contains("Generating warnings..."));
        assertTrue(progressMessages.contains("Generating summary..."));
        assertTrue(progressMessages.contains("Refreshing UI..."));
    }

    private Path projectPath(String... segments) {
        Path path = Path.of("");
        for (String segment : segments) {
            path = path.resolve(segment);
        }
        return path.toAbsolutePath().normalize();
    }
}
