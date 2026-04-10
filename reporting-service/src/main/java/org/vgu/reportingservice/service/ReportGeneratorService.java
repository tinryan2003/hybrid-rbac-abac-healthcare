package org.vgu.reportingservice.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.reportingservice.enums.ReportStatus;
import org.vgu.reportingservice.model.Report;
import org.vgu.reportingservice.repository.ReportRepository;
import org.vgu.reportingservice.service.ReportDataService.ReportData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGeneratorService {

    private final ReportRepository reportRepository;
    private final ReportDataService reportDataService;

    @Value("${reporting.output-directory}")
    private String outputDirectory;

    @Async("reportTaskExecutor")
    @Transactional
    public void generateReportAsync(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        try {
            log.info("Starting report generation: id={}, type={}, format={}",
                    reportId, report.getType(), report.getFormat());
            report.setStatus(ReportStatus.PROCESSING);
            reportRepository.save(report);

            Path outputPath = Paths.get(outputDirectory);
            if (!Files.exists(outputPath)) Files.createDirectories(outputPath);

            // Fetch real data from microservices
            ReportData data = reportDataService.fetch(report);
            log.info("Data fetched for report {}: {} rows", reportId, data.rows().size());

            String filePath = switch (report.getFormat()) {
                case CSV   -> generateCsv(report, data);
                case JSON  -> generateJson(report, data);
                case PDF   -> generatePdf(report, data);
                case EXCEL -> generateExcel(report, data);
            };

            File file = new File(filePath);
            report.setStatus(ReportStatus.COMPLETED);
            report.setFilePath(filePath);
            report.setFileSize(file.length());
            report.setCompletedAt(LocalDateTime.now());
            reportRepository.save(report);
            log.info("Report {} completed: {}", reportId, filePath);

        } catch (Exception e) {
            log.error("Report generation failed for id={}: {}", reportId, e.getMessage(), e);
            report.setStatus(ReportStatus.FAILED);
            report.setErrorMessage(e.getMessage());
            reportRepository.save(report);
        }
    }

    // -------------------------------------------------------------------------
    // CSV
    // -------------------------------------------------------------------------
    private String generateCsv(Report report, ReportData data) throws IOException {
        String fileName = buildFileName(report, "csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Title and summary block
            writer.write("# " + data.title());
            writer.newLine();
            writer.write("# Generated: " + LocalDateTime.now());
            writer.newLine();
            for (Map.Entry<String, Object> entry : data.summary().entrySet()) {
                writer.write("# " + entry.getKey() + ": " + entry.getValue());
                writer.newLine();
            }
            writer.newLine();

            // Headers
            writer.write(csvLine(data.headers()));
            writer.newLine();

            // Data rows
            for (List<String> row : data.rows()) {
                writer.write(csvLine(row));
                writer.newLine();
            }
        }
        log.info("CSV report saved: {}", fileName);
        return fileName;
    }

    private String csvLine(List<String> cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sb.append(',');
            String val = cols.get(i) == null ? "" : cols.get(i);
            // Quote fields that contain commas, quotes, or newlines
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                sb.append('"').append(val.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(val);
            }
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // JSON
    // -------------------------------------------------------------------------
    private String generateJson(Report report, ReportData data) throws IOException {
        String fileName = buildFileName(report, "json");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("{\n");
            writer.write("  \"title\": " + jsonString(data.title()) + ",\n");
            writer.write("  \"generatedAt\": " + jsonString(LocalDateTime.now().toString()) + ",\n");
            writer.write("  \"reportType\": " + jsonString(report.getType().name()) + ",\n");

            // Summary
            writer.write("  \"summary\": {\n");
            List<Map.Entry<String, Object>> summaryEntries = List.copyOf(data.summary().entrySet());
            for (int i = 0; i < summaryEntries.size(); i++) {
                Map.Entry<String, Object> e = summaryEntries.get(i);
                String val = e.getValue() == null ? "null" : jsonString(e.getValue().toString());
                writer.write("    " + jsonString(e.getKey()) + ": " + val);
                if (i < summaryEntries.size() - 1) writer.write(",");
                writer.newLine();
            }
            writer.write("  },\n");

            // Column headers
            writer.write("  \"columns\": [");
            for (int i = 0; i < data.headers().size(); i++) {
                if (i > 0) writer.write(", ");
                writer.write(jsonString(data.headers().get(i)));
            }
            writer.write("],\n");

            // Rows as array of objects
            writer.write("  \"data\": [\n");
            for (int r = 0; r < data.rows().size(); r++) {
                List<String> row = data.rows().get(r);
                writer.write("    {");
                for (int c = 0; c < data.headers().size(); c++) {
                    if (c > 0) writer.write(", ");
                    String key = jsonString(data.headers().get(c));
                    String val = c < row.size() ? jsonString(row.get(c)) : "\"\"";
                    writer.write(key + ": " + val);
                }
                writer.write("}");
                if (r < data.rows().size() - 1) writer.write(",");
                writer.newLine();
            }
            writer.write("  ]\n}\n");
        }
        log.info("JSON report saved: {}", fileName);
        return fileName;
    }

    private String jsonString(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    // -------------------------------------------------------------------------
    // PDF (iText 8)
    // -------------------------------------------------------------------------
    private String generatePdf(Report report, ReportData data) throws IOException {
        String fileName = buildFileName(report, "pdf");
        // Document.close() cascades to PdfDocument and PdfWriter — use only Document in try-with-resources
        Document document = new Document(new PdfDocument(new PdfWriter(fileName)));
        try {

            // Title
            document.add(new Paragraph(data.title())
                    .setFontSize(16).setBold().setMarginBottom(8));
            document.add(new Paragraph("Generated: " + LocalDateTime.now())
                    .setFontSize(10).setItalic().setMarginBottom(4));

            // Summary section
            if (!data.summary().isEmpty()) {
                document.add(new Paragraph("Summary").setFontSize(13).setBold().setMarginTop(10));
                Table summaryTable = new Table(new float[]{3, 3}).setWidth(UnitValue.createPercentValue(60));
                summaryTable.addHeaderCell(styledHeaderCell("Metric"));
                summaryTable.addHeaderCell(styledHeaderCell("Value"));
                for (Map.Entry<String, Object> e : data.summary().entrySet()) {
                    summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(e.getKey()).setFontSize(9)));
                    summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                            e.getValue() == null ? "-" : e.getValue().toString()).setFontSize(9)));
                }
                document.add(summaryTable);
            }

            // Data table
            if (!data.headers().isEmpty()) {
                document.add(new Paragraph("Detail").setFontSize(13).setBold().setMarginTop(12));
                float[] colWidths = new float[data.headers().size()];
                java.util.Arrays.fill(colWidths, 1);
                Table table = new Table(colWidths).setWidth(UnitValue.createPercentValue(100));

                // Header row
                for (String h : data.headers()) {
                    table.addHeaderCell(styledHeaderCell(h));
                }

                // Data rows (cap at 1000 to avoid huge PDFs)
                int maxRows = Math.min(data.rows().size(), 1000);
                for (int i = 0; i < maxRows; i++) {
                    List<String> row = data.rows().get(i);
                    for (int c = 0; c < data.headers().size(); c++) {
                        String val = c < row.size() ? row.get(c) : "";
                        table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(val == null ? "" : val).setFontSize(8)));
                    }
                }
                document.add(table);

                if (data.rows().size() > maxRows) {
                    document.add(new Paragraph("... and " + (data.rows().size() - maxRows) + " more rows (truncated in PDF)")
                            .setFontSize(9).setItalic().setMarginTop(4));
                }
            }
        } finally {
            document.close();
        }
        log.info("PDF report saved: {}", fileName);
        return fileName;
    }

    private com.itextpdf.layout.element.Cell styledHeaderCell(String text) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text).setFontSize(9).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY);
    }

    // -------------------------------------------------------------------------
    // EXCEL (Apache POI)
    // -------------------------------------------------------------------------
    private String generateExcel(Report report, ReportData data) throws IOException {
        String fileName = buildFileName(report, "xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(report.getType().name());

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(data.title());
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Summary rows (starting at row 2)
            int summaryStart = 2;
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            int sumRow = summaryStart;
            for (Map.Entry<String, Object> e : data.summary().entrySet()) {
                Row row = sheet.createRow(sumRow++);
                row.createCell(0).setCellValue(e.getKey());
                Cell valCell = row.createCell(1);
                valCell.setCellValue(e.getValue() == null ? "" : e.getValue().toString());
            }

            // Header row for data table
            int dataStart = sumRow + 2;
            Row headerRow = sheet.createRow(dataStart);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int c = 0; c < data.headers().size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(data.headers().get(c));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int r = 0; r < data.rows().size(); r++) {
                Row row = sheet.createRow(dataStart + 1 + r);
                List<String> rowData = data.rows().get(r);
                for (int c = 0; c < data.headers().size(); c++) {
                    row.createCell(c).setCellValue(c < rowData.size() ? rowData.get(c) : "");
                }
            }

            // Auto-size columns
            for (int c = 0; c < data.headers().size(); c++) {
                sheet.autoSizeColumn(c);
            }

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                workbook.write(fos);
            }
        }
        log.info("Excel report saved: {}", fileName);
        return fileName;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private String buildFileName(Report report, String ext) {
        String safeName = report.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
        return String.format("%s/%s_%d.%s", outputDirectory, safeName, report.getId(), ext);
    }
}
