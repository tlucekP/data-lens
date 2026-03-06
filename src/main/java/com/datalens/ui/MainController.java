package com.datalens.ui;

import com.datalens.analysis.SummaryGenerator;
import com.datalens.analysis.WarningEngine;
import com.datalens.loader.CsvLoader;
import com.datalens.loader.LoadedDataset;
import com.datalens.loader.XlsxLoader;
import com.datalens.model.ColumnProfile;
import com.datalens.model.DatasetProfile;
import com.datalens.profiler.DataProfiler;
import com.datalens.util.FileValidators;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import java.util.List;
import java.util.Optional;

public class MainController {
    @FXML
    private Button loadFileButton;
    @FXML
    private Button reloadButton;
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

    private final CsvLoader csvLoader = new CsvLoader();
    private final XlsxLoader xlsxLoader = new XlsxLoader();
    private final DataProfiler dataProfiler = new DataProfiler();
    private final WarningEngine warningEngine = new WarningEngine();
    private final SummaryGenerator summaryGenerator = new SummaryGenerator();

    private Path lastLoadedFile;

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
            loadDataset(file.toPath());
        }
    }

    @FXML
    private void handleReload() {
        if (lastLoadedFile != null) {
            loadDataset(lastLoadedFile);
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

    private void loadDataset(Path file) {
        try {
            FileValidators.validateSelectedFile(file);
            if (FileValidators.shouldWarnAboutLargeFile(file) && !confirmLargeFileLoad(file)) {
                return;
            }

            LoadedDataset dataset = switch (FileValidators.extensionOf(file)) {
                case "csv" -> csvLoader.load(file);
                case "xlsx" -> xlsxLoader.load(file);
                default -> throw new IOException("Unsupported file type.");
            };

            DatasetProfile baseProfile = dataProfiler.profile(dataset);
            List<String> warnings = warningEngine.analyze(baseProfile);
            String summary = summaryGenerator.generate(baseProfile, warnings);
            DatasetProfile finalProfile = baseProfile.withGeneratedContent(warnings, summary);

            updateView(finalProfile);
            lastLoadedFile = file;
            reloadButton.setDisable(false);
        } catch (Exception exception) {
            showError("Could not load file", exception.getMessage());
        }
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
        fileValueLabel.setText("No file loaded");
        sheetValueLabel.setText("-");
        rowsValueLabel.setText("0");
        columnsValueLabel.setText("0");
        emptyRowsValueLabel.setText("0");
        headerValueLabel.setText("-");
        columnTable.getItems().clear();
        warningsListView.getItems().clear();
        summaryTextArea.setText("Load a CSV or XLSX file to generate profiling notes.");
        reloadButton.setDisable(true);
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