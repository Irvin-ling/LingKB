package com.ling.lingkb.llm.data.parser;

import com.ling.lingkb.entity.LingDocument;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/19
 */
@Slf4j
@Component
public class ExcelParser implements DocumentParser {
    @Value("${data.parser.max.row}")
    private int dataMaxRow;
    @Value("${data.page.break.symbols}")
    private String pageBreakSymbols;

    @Override
    public LingDocument parse(Path filePath) throws Exception {
        log.info("ExcelParser.parse({})...", filePath);
        String fileName = filePath.getFileName().toString();
        LingDocument result = new LingDocument();

        try (Workbook workbook = WorkbookFactory.create(filePath.toFile())) {
            String author = "Unknown";
            long created = 0;
            int sheetCount = workbook.getNumberOfSheets();

            if (workbook instanceof XSSFWorkbook) {
                XSSFWorkbook xssfWorkbook = (XSSFWorkbook) workbook;
                POIXMLProperties props = xssfWorkbook.getProperties();
                if (props != null && props.getCoreProperties() != null) {
                    POIXMLProperties.CoreProperties coreProps = props.getCoreProperties();
                    author = StringUtils
                            .firstNonBlank(coreProps.getCreator(), coreProps.getLastModifiedByUser(), author);
                    if (coreProps.getCreated() != null) {
                        created = coreProps.getCreated().getTime();
                    }
                }
            } else if (workbook instanceof HSSFWorkbook) {
                HSSFWorkbook hssfWorkbook = (HSSFWorkbook) workbook;
                SummaryInformation si = hssfWorkbook.getSummaryInformation();
                if (si != null) {
                    author = StringUtils.defaultIfBlank(si.getAuthor(), author);
                    if (si.getCreateDateTime() != null) {
                        created = si.getCreateDateTime().getTime();
                    }
                }
            }

            result.setAuthor(author);
            result.setSourceFileName(fileName);
            result.setCreationDate(created);
            result.setPageCount(sheetCount);
            // Processing content (using non streaming methods uniformly)
            StringBuilder contentBuilder = new StringBuilder();
            DataFormatter formatter = new DataFormatter();
            processWorkbookContent(workbook, contentBuilder, formatter, result);
            result.setText(contentBuilder.toString());
        }
        return result;
    }

    private void processWorkbookContent(Workbook workbook, StringBuilder contentBuilder, DataFormatter formatter,
                                        LingDocument result) {
        int rowCount = 0;

        for (Sheet sheet : workbook) {
            log.info("Currently parsing sheet {}", sheet.getSheetName());
            if (isSheetEmpty(sheet)) {
                result.setPageCount(result.getPageCount() - 1);
                continue;
            }

            // Add table name as title
            contentBuilder.append(pageBreakSymbols).append(sheet.getSheetName()).append("\n");

            // Traverse rows
            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }
                if (rowCount >= dataMaxRow) {
                    log.warn("current file content truncated due to size limit {} rows", dataMaxRow);
                    break;
                }

                List<String> rowData = new ArrayList<>();
                // Traverse cells
                for (Cell cell : row) {
                    rowData.add(getCellValueAsString(cell, formatter));
                }

                // Add row data
                contentBuilder.append(String.join(" | ", rowData)).append("\n");
                log.debug("Currently parsing row {}", rowCount++);
            }

            // Add separators between tables
            contentBuilder.append("\n---\n\n");
        }
    }

    private boolean isSheetEmpty(Sheet sheet) {
        if (sheet == null) {
            return true;
        }
        // Retrieve the indexes of the first and last rows
        int firstRowNum = sheet.getFirstRowNum();
        int lastRowNum = sheet.getLastRowNum();
        // If there are no rows, or if there is only one row and that row is empty
        if (lastRowNum < firstRowNum) {
            return true;
        }
        // Check if the first line contains any content
        Row firstRow = sheet.getRow(firstRowNum);
        if (firstRow == null) {
            return true;
        }
        // Check if there is content in the first cell of the first row
        Cell firstCell = firstRow.getCell(0);
        return firstCell == null || firstCell.getCellType() == CellType.BLANK;
    }

    private String getCellValueAsString(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getRichStringCellValue().getString();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return formatter.formatCellValue(cell);
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return formatter.formatCellValue(cell);
                } catch (Exception e) {
                    return cell.getCellFormula();
                }
            case BLANK:
                return "";
            default:
                return formatter.formatCellValue(cell);
        }
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("xls", "xlsx");
    }
}