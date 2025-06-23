package com.ling.lingkb.data.parser;

import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import org.springframework.stereotype.Component;

@Component
public class ExcelParser implements DocumentParser {
    private static final int DEFAULT_MAX_ROWS = 10000;
    private static final int MAX_CELL_LENGTH = 32767;

    @Override
    public DocumentParseResult parse(Path filePath) throws DocumentParseException {
        String fileName = filePath.getFileName().toString();
        DocumentParseResult result = new DocumentParseResult();

        try (Workbook workbook = WorkbookFactory.create(filePath.toFile())) {
            // Extract metadata
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
            DocumentParseResult.DocumentMetadata documentMetadata =
                    DocumentParseResult.DocumentMetadata.builder().author(author).sourceFileName(fileName)
                            .creationDate(created).pageCount(sheetCount).build();
            // Processing content (using non streaming methods uniformly)
            StringBuilder contentBuilder = new StringBuilder();
            DataFormatter formatter = new DataFormatter();
            processWorkbookContent(workbook, contentBuilder, formatter, documentMetadata);
            result.setTextContent(contentBuilder.toString());
            result.setMetadata(documentMetadata);
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse Excel file: " + fileName, e);
        } catch (Exception e) {
            throw new DocumentParseException("Unexpected error while parsing Excel file: " + fileName, e);
        }

        return result;
    }

    private void processWorkbookContent(Workbook workbook, StringBuilder contentBuilder, DataFormatter formatter,
                                        DocumentParseResult.DocumentMetadata documentMetadata) {
        int rowCount = 0;

        for (Sheet sheet : workbook) {
            if (isSheetEmpty(sheet)) {
                documentMetadata.setPageCount(documentMetadata.getPageCount() - 1);
                continue;
            }

            // Add table name as title
            contentBuilder.append("Sheet: ").append(sheet.getSheetName()).append("\n");

            // Traverse rows
            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }
                if (rowCount >= DEFAULT_MAX_ROWS) {
                    break;
                }

                List<String> rowData = new ArrayList<>();
                // Traverse cells
                for (Cell cell : row) {
                    rowData.add(truncateCellValue(getCellValueAsString(cell, formatter)));
                }

                // Add row data
                contentBuilder.append(String.join(" | ", rowData)).append("\n");
                rowCount++;
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

    private String truncateCellValue(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_CELL_LENGTH ? value.substring(0, MAX_CELL_LENGTH) + "...[truncated]" : value;
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("xls", "xlsx");
    }
}