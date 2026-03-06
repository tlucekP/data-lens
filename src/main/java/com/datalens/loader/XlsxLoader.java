package com.datalens.loader;

import com.datalens.util.DataValueParser;
import com.datalens.util.DatasetNormalizer;
import com.datalens.util.FileValidators;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class XlsxLoader {
    static final double MIN_INFLATE_RATIO = 0.01d;
    static final long MAX_ENTRY_SIZE_BYTES = 50L * 1024L * 1024L;

    static {
        if (System.getProperty("log4j2.loggerContextFactory") == null) {
            System.setProperty(
                    "log4j2.loggerContextFactory",
                    "org.apache.logging.log4j.simple.SimpleLoggerContextFactory"
            );
        }
    }

    public LoadedDataset load(Path sourceFile) throws IOException {
        FileValidators.validateSelectedFile(sourceFile);
        ZipSecureFile.setMinInflateRatio(MIN_INFLATE_RATIO);
        ZipSecureFile.setMaxEntrySize(MAX_ENTRY_SIZE_BYTES);

        try (OPCPackage packageSource = OPCPackage.open(sourceFile.toFile(), PackageAccess.READ);
             XSSFWorkbook workbook = new XSSFWorkbook(packageSource)) {
            DataFormatter formatter = new DataFormatter(Locale.US, true);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet selectedSheet = findFirstRelevantSheet(workbook, formatter, evaluator);

            if (selectedSheet == null) {
                return DatasetNormalizer.normalize(sourceFile, "First sheet", List.of());
            }

            List<List<String>> rawRows = new ArrayList<>();
            int lastRowNumber = selectedSheet.getLastRowNum();
            for (int rowIndex = 0; rowIndex <= lastRowNumber; rowIndex++) {
                Row row = selectedSheet.getRow(rowIndex);
                rawRows.add(extractRow(row, formatter, evaluator));
            }

            return DatasetNormalizer.normalize(sourceFile, selectedSheet.getSheetName(), rawRows);
        } catch (Exception exception) {
            throw new IOException("XLSX parsing failed. The workbook may be damaged or unsupported.", exception);
        }
    }

    private Sheet findFirstRelevantSheet(XSSFWorkbook workbook, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (workbook.getNumberOfSheets() == 0) {
            return null;
        }

        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            if (!isSheetEmpty(sheet, formatter, evaluator)) {
                return sheet;
            }
        }

        return workbook.getSheetAt(0);
    }

    private boolean isSheetEmpty(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (Row row : sheet) {
            List<String> values = extractRow(row, formatter, evaluator);
            if (values.stream().anyMatch(value -> !DataValueParser.isBlank(value))) {
                return false;
            }
        }
        return true;
    }

    private List<String> extractRow(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null || row.getLastCellNum() < 0) {
            return List.of();
        }

        List<String> values = new ArrayList<>(row.getLastCellNum());
        for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
            Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            values.add(cell == null ? "" : formatter.formatCellValue(cell, evaluator));
        }
        return values;
    }
}
