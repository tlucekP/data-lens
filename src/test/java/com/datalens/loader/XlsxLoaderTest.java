package com.datalens.loader;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XlsxLoaderTest {
    private final XlsxLoader loader = new XlsxLoader();

    @TempDir
    Path tempDir;

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

    @Test
    void loadsFormulaCellsUsingEvaluatedValues() throws Exception {
        Path workbookPath = tempDir.resolve("formula.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(workbookPath)) {
            var sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("amount");
            header.createCell(1).setCellValue("double_amount");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(21);
            row.createCell(1).setCellFormula("A2*2");
            workbook.write(outputStream);
        }

        LoadedDataset dataset = loader.load(workbookPath);

        assertFalse(dataset.rows().isEmpty());
        assertEquals("42", dataset.rows().get(0).get(1));
    }

    @Test
    void rejectsZipBombLikeWorkbookPayload() throws Exception {
        Path workbookPath = tempDir.resolve("zip-bomb.xlsx");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(workbookPath), StandardCharsets.UTF_8)) {
            writeEntry(zipOutputStream, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                    """);
            writeEntry(zipOutputStream, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """);
            writeEntry(zipOutputStream, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                              xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets>
                        <sheet name="Sheet1" sheetId="1" r:id="rId1"/>
                      </sheets>
                    </workbook>
                    """);
            writeEntry(zipOutputStream, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    </Relationships>
                    """);
            String suspiciousPayload = "A".repeat(250_000);
            writeEntry(zipOutputStream, "xl/worksheets/sheet1.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <sheetData>
                        <row r="1">
                          <c r="A1" t="inlineStr"><is><t>%s</t></is></c>
                        </row>
                      </sheetData>
                    </worksheet>
                    """.formatted(suspiciousPayload));
        }

        IOException exception = assertThrows(IOException.class, () -> loader.load(workbookPath));

        assertTrue(causeChainContains(exception, "zip bomb"));
    }

    private void writeEntry(ZipOutputStream zipOutputStream, String name, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private boolean causeChainContains(Throwable throwable, String expectedToken) {
        String normalizedToken = expectedToken.toLowerCase();
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains(normalizedToken)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Path projectPath(String... segments) {
        Path path = Path.of("");
        for (String segment : segments) {
            path = path.resolve(segment);
        }
        return path.toAbsolutePath().normalize();
    }
}
