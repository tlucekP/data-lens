package com.datalens.ui;

import com.datalens.model.ColumnProfile;
import com.datalens.model.DatasetProfile;
import com.datalens.service.DatasetProcessingService;
import com.datalens.util.FileValidators;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController {
    private static final DateTimeFormatter STATUS_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private Button loadFileButton;
    @FXML
    private Button reloadButton;
    @FXML
    private Label statusValueLabel;
    @FXML
    private Label fileValueLabel;
    @FXML
    private Label sheetValueLabel;
    @FXML
    private Label rowsValueLabel;
    @FXML
    private Label columnsValueLabel;
    @FXML
    private Label emptyRowsValueLabel;
    @FXML
    private Label headerValueLabel;
    @FXML
    private TableView<ColumnProfile> columnTable;
    @FXML
    private TableColumn<ColumnProfile, String> columnNameColumn;
    @FXML
    private TableColumn<ColumnProfile, String> typeColumn;
    @FXML
    private TableColumn<ColumnProfile, String> missingColumn;
    @FXML
    private TableColumn<ColumnProfile, String> uniqueColumn;
    @FXML
    private TableColumn<ColumnProfile, String> statsColumn;
    @FXML
    private ListView<String> warningsListView;
    @FXML
    private TextArea summaryTextArea;

    private final DatasetProcessingService datasetProcessingService = new DatasetProcessingService();
    private final ExecutorService processingExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "datalens-dataset-processing");
        thread.setDaemon(true);
        return thread;
    });

    private Path lastLoadedFile;
    private Task<DatasetProfile> activeLoadTask;

    @FXML
    private void initialize() {
        configureTable();
        warningsListView.setPlaceholder(new Label("No warnings detected."));
        summaryTextArea.setWrapText(true);
        summaryTextArea.setEditable(false);
        summaryTextArea.setFocusTraversable(false);
        resetView();
    }

    @FXML
    private void handleLoadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load dataset");
        fileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Supported files", "*.csv", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV files", "*.csv"),
                new FileChooser.ExtensionFilter("Excel workbooks", "*.xlsx")
        );

        if (lastLoadedFile != null && lastLoadedFile.getParent() != null) {
            fileChooser.setInitialDirectory(lastLoadedFile.getParent().toFile());
        }

        Stage stage = (Stage) loadFileButton.getScene().getWindow();
        java.io.File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            startDatasetLoad(file.toPath());
        }
    }

    @FXML
    private void handleReload() {
        if (lastLoadedFile != null) {
            startDatasetLoad(lastLoadedFile);
        }
    }

    private void configureTable() {
        columnTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        columnNameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().name()));
        typeColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().type().displayName()));
        missingColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().missingPercentageText()));
        uniqueColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(Integer.toString(cellData.getValue().uniqueCount())));
        statsColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().statsText()));
    }

    private void startDatasetLoad(Path file) {
        if (activeLoadTask != null && activeLoadTask.isRunning()) {
            return;
        }

        try {
            FileValidators.validateSelectedFile(file);
            if (FileValidators.shouldWarnAboutLargeFile(file) && !confirmLargeFileLoad(file)) {
                return;
            }
        } catch (Exception exception) {
            showError("Could not load file", exception.getMessage());
            return;
        }

        Task<DatasetProfile> loadTask = new Task<>() {
            @Override
            protected DatasetProfile call() throws Exception {
                updateMessage("Preparing load...");
                return datasetProcessingService.process(file, this::updateMessage);
            }
        };

        activeLoadTask = loadTask;
        statusValueLabel.textProperty().unbind();
        statusValueLabel.textProperty().bind(loadTask.messageProperty());
        setControlsLoading(true);

        loadTask.setOnSucceeded(event -> {
            statusValueLabel.textProperty().unbind();
            DatasetProfile profile = loadTask.getValue();
            updateView(profile);
            lastLoadedFile = file;
            activeLoadTask = null;
            setControlsLoading(false);
            statusValueLabel.setText("Loaded " + file.getFileName() + " at " + timestampNow());
        });
        loadTask.setOnFailed(event -> {
            statusValueLabel.textProperty().unbind();
            activeLoadTask = null;
            setControlsLoading(false);
            statusValueLabel.setText("Load failed at " + timestampNow());
            Throwable exception = loadTask.getException();
            showError("Could not load file", exception == null ? null : exception.getMessage());
        });
        loadTask.setOnCancelled(event -> {
            statusValueLabel.textProperty().unbind();
            activeLoadTask = null;
            setControlsLoading(false);
            statusValueLabel.setText("Load cancelled.");
        });

        processingExecutor.submit(loadTask);
    }

    private boolean confirmLargeFileLoad(Path file) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Large file warning");
        alert.setHeaderText("Selected file is relatively large.");
        alert.setContentText("File size: " + FileValidators.formatFileSize(java.nio.file.Files.size(file)) + ". Loading may take longer. Continue?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void updateView(DatasetProfile profile) {
        fileValueLabel.setText(profile.fileName());
        sheetValueLabel.setText(profile.sheetName());
        rowsValueLabel.setText(Integer.toString(profile.rowCount()));
        columnsValueLabel.setText(Integer.toString(profile.columnCount()));
        emptyRowsValueLabel.setText(Integer.toString(profile.emptyRowCount()));
        headerValueLabel.setText(profile.headerDetected() ? "Yes" : "No");

        columnTable.getItems().setAll(profile.columns());
        warningsListView.getItems().setAll(profile.warnings());
        summaryTextArea.setText(profile.summary());
    }

    private void resetView() {
        statusValueLabel.setText("Ready");
        fileValueLabel.setText("No file loaded");
        sheetValueLabel.setText("-");
        rowsValueLabel.setText("0");
        columnsValueLabel.setText("0");
        emptyRowsValueLabel.setText("0");
        headerValueLabel.setText("-");
        columnTable.getItems().clear();
        warningsListView.getItems().clear();
        summaryTextArea.setText("Load a CSV or XLSX file to generate profiling notes.");
        setControlsLoading(false);
    }

    private void setControlsLoading(boolean loading) {
        loadFileButton.setDisable(loading);
        reloadButton.setDisable(loading || lastLoadedFile == null);
    }

    private String timestampNow() {
        return LocalTime.now().format(STATUS_TIME_FORMAT);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank()
                ? "The file could not be processed. Please check the format and try again."
                : message);
        alert.showAndWait();
    }
}
