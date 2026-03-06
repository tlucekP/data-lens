package com.datalens.service;

import com.datalens.analysis.SummaryGenerator;
import com.datalens.analysis.WarningEngine;
import com.datalens.diagnostics.AppDiagnostics;
import com.datalens.loader.CsvLoader;
import com.datalens.loader.LoadedDataset;
import com.datalens.loader.XlsxLoader;
import com.datalens.model.DatasetProfile;
import com.datalens.profiler.DataProfiler;
import com.datalens.util.FileValidators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatasetProcessingService {
    private final CsvLoader csvLoader;
    private final XlsxLoader xlsxLoader;
    private final DataProfiler dataProfiler;
    private final WarningEngine warningEngine;
    private final SummaryGenerator summaryGenerator;
    private final Logger logger = AppDiagnostics.getLogger(DatasetProcessingService.class);

    public DatasetProcessingService() {
        this(new CsvLoader(), new XlsxLoader(), new DataProfiler(), new WarningEngine(), new SummaryGenerator());
    }

    public DatasetProcessingService(
            CsvLoader csvLoader,
            XlsxLoader xlsxLoader,
            DataProfiler dataProfiler,
            WarningEngine warningEngine,
            SummaryGenerator summaryGenerator
    ) {
        this.csvLoader = csvLoader;
        this.xlsxLoader = xlsxLoader;
        this.dataProfiler = dataProfiler;
        this.warningEngine = warningEngine;
        this.summaryGenerator = summaryGenerator;
    }

    public DatasetProfile process(Path file, Consumer<String> progressListener) throws IOException {
        logInfo("load_started", file, -1L, null);
        try {
            notifyProgress(progressListener, "Validating file...");
            FileValidators.validateSelectedFile(file);
            long sizeBytes = Files.size(file);
            logInfo("file_validated", file, sizeBytes, null);

            notifyProgress(progressListener, "Loading dataset...");
            LoadedDataset dataset = switch (FileValidators.extensionOf(file)) {
                case "csv" -> csvLoader.load(file);
                case "xlsx" -> xlsxLoader.load(file);
                default -> throw new IOException("Unsupported file type.");
            };
            logInfo("dataset_loaded", file, dataset.sourceFileSizeBytes(),
                    "rows=" + dataset.rows().size() + " columns=" + dataset.columnNames().size());

            notifyProgress(progressListener, "Profiling columns...");
            DatasetProfile baseProfile = dataProfiler.profile(dataset);
            logInfo("profile_created", file, baseProfile.sourceFileSizeBytes(),
                    "rows=" + baseProfile.rowCount() + " columns=" + baseProfile.columnCount());

            notifyProgress(progressListener, "Generating warnings...");
            List<String> warnings = warningEngine.analyze(baseProfile);
            logInfo("warnings_created", file, baseProfile.sourceFileSizeBytes(), "warningCount=" + warnings.size());

            notifyProgress(progressListener, "Generating summary...");
            String summary = summaryGenerator.generate(baseProfile, warnings);
            DatasetProfile finalProfile = baseProfile.withGeneratedContent(warnings, summary);

            notifyProgress(progressListener, "Refreshing UI...");
            logInfo("load_succeeded", file, finalProfile.sourceFileSizeBytes(), "summaryReady=true");
            return finalProfile;
        } catch (IOException exception) {
            logFailure(file, exception);
            throw exception;
        } catch (RuntimeException exception) {
            logFailure(file, exception);
            throw exception;
        }
    }

    private void notifyProgress(Consumer<String> progressListener, String message) {
        if (progressListener != null) {
            progressListener.accept(message);
        }
    }

    private void logInfo(String event, Path file, long sizeBytes, String extra) {
        logger.info(() -> buildMessage(event, file, sizeBytes, extra));
    }

    private void logFailure(Path file, Exception exception) {
        logger.log(Level.SEVERE, buildMessage("load_failed", file, -1L,
                "reason=" + sanitize(exception.getClass().getSimpleName())), exception);
    }

    private String buildMessage(String event, Path file, long sizeBytes, String extra) {
        StringBuilder builder = new StringBuilder();
        builder.append("event=").append(event);
        builder.append(" file=").append(sanitize(file == null ? "unknown" : file.getFileName().toString()));
        if (file != null) {
            builder.append(" extension=").append(sanitize(FileValidators.extensionOf(file)));
        }
        if (sizeBytes >= 0L) {
            builder.append(" sizeBytes=").append(sizeBytes);
        }
        if (extra != null && !extra.isBlank()) {
            builder.append(' ').append(extra);
        }
        return builder.toString();
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "n_a";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
