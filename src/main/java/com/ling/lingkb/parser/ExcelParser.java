package com.ling.lingkb.parser;
/*
 * ------------------------------------------------------------------
 * Copyright @ 2025 Hangzhou Ling Technology Co.,Ltd. All rights reserved.
 * ------------------------------------------------------------------
 * Product: LingKB
 * Module Name: LingKB
 * Date Created: 2025/6/19
 * Description:
 * ------------------------------------------------------------------
 * Modification History
 * DATE            Name           Description
 * ------------------------------------------------------------------
 * 2025/6/19       spt
 * ------------------------------------------------------------------
 */

import com.alibaba.fastjson.JSON;
import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import com.ling.lingkb.common.exception.UnsupportedDocumentTypeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Excel解析器
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Component
public class ExcelParser implements DocumentParser {
    /**
     * 默认最大处理行数
     */
    private static final int DEFAULT_MAX_ROWS = 10000;

    @Override
    public DocumentParseResult parse(File file) throws DocumentParseException {
        String fileName = file.getName();
        DocumentParseResult result = new DocumentParseResult();
        result.setSourceFileName(fileName);

        try (FileInputStream fis = new FileInputStream(file)) {
            DocumentParseResult.DocumentMetadata.DocumentMetadataBuilder builder =
                    DocumentParseResult.DocumentMetadata.builder();

            if (fileName.endsWith(".xlsx")) {
                // 使用SAX事件模型处理大文件
                try (OPCPackage pkg = OPCPackage.open(fis)) {
                    XSSFReader reader = new XSSFReader(pkg);
                    builder.author(pkg.getPackageProperties().getCreatorProperty().orElse("")).creationDate(
                            pkg.getPackageProperties().getCreatedProperty().orElse(new Date()).toString());

                    // 流式解析每个sheet
                    Map<String, List<List<String>>> allSheetData = new LinkedHashMap<>();
                    XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();

                    while (sheets.hasNext()) {
                        InputStream sheetStream = sheets.next();
                        String sheetName = sheets.getSheetName();
                        List<List<String>> sheetData = new ArrayList<>();
                        SharedStrings sst = reader.getSharedStringsTable();

                        SheetHandler handler = new SheetHandler(sheetData, sst);

                        XMLReader parser = XMLReaderFactory.createXMLReader();
                        parser.setContentHandler(
                                new XSSFSheetXMLHandler(reader.getStylesTable(), null, sst, handler, null, false));

                        parser.parse(new InputSource(sheetStream));
                        sheetStream.close();

                        allSheetData.put(sheetName, sheetData);
                    }

                    builder.pageCount(String.valueOf(allSheetData.size()));
                    result.setTextContent(JSON.toJSONString(allSheetData));
                }
            } else if (fileName.endsWith(".xls")) {
                // 旧格式仍使用HSSF，但限制最大行数
                try (HSSFWorkbook hssfWorkbook = new HSSFWorkbook(fis)) {
                    String textContent = getTextContent(hssfWorkbook);
                    SummaryInformation summary = hssfWorkbook.getSummaryInformation();

                    builder.author(summary.getAuthor()).creationDate(summary.getCreateDateTime().toString())
                            .pageCount(String.valueOf(hssfWorkbook.getNumberOfSheets()));

                    result.setTextContent(textContent);
                }
            } else {
                throw new UnsupportedDocumentTypeException("不支持的Excel文件格式: " + fileName);
            }

            result.setMetadata(builder.build());
            return result;
        } catch (Exception e) {
            throw new DocumentParseException("解析Excel文件失败: " + fileName, e);
        }
    }

    /**
     * 处理.xls格式的方法（限制最大行数）
     */
    private String getTextContent(Workbook workbook) {
        Map<String, List<List<String>>> allSheetData = new LinkedHashMap<>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String sheetName = workbook.getSheetName(i);
            Sheet sheet = workbook.getSheet(sheetName);
            List<List<String>> sheetData = extractSheetData(sheet);
            allSheetData.put(sheetName, sheetData);
        }

        return JSON.toJSONString(allSheetData);
    }

    /**
     * 提取sheet数据（限制最大行数）
     */
    private List<List<String>> extractSheetData(Sheet sheet) {
        List<List<String>> sheetData = new ArrayList<>();
        int rowCount = 0;

        for (Row row : sheet) {
            if (rowCount++ >= DEFAULT_MAX_ROWS) {
                break; // 限制最大行数，防止OOM
            }

            List<String> rowData = new ArrayList<>();
            for (Cell cell : row) {
                rowData.add(getCellValueAsString(cell));
            }
            sheetData.add(rowData);
        }

        return sheetData;
    }

    /**
     * 处理单元格值的方法
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // 这里可以选择计算公式结果而不是返回公式本身
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * SAX事件处理器，用于处理.xlsx格式的流式解析
     */
    private static class SheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final List<List<String>> sheetData;
        private List<String> currentRow;
        private int rowCount = 0;
        private final SharedStrings sst;

        SheetHandler(List<List<String>> sheetData, SharedStrings sst) {
            this.sheetData = sheetData;
            this.sst = sst;
        }

        // 处理单元格值时，可能需要解析共享字符串索引
        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment xssfComment) {
            // 如果formattedValue是索引，从sst中获取实际值
            if (formattedValue != null && formattedValue.matches("^\\d+$")) {
                int idx = Integer.parseInt(formattedValue);
                String value = sst.getItemAt(idx).getString();
                currentRow.add(value);
            } else {
                currentRow.add(formattedValue != null ? formattedValue : "");
            }
        }

        @Override
        public void startRow(int rowNum) {
            currentRow = new ArrayList<>();
            rowCount++;
        }

        @Override
        public void endRow(int rowNum) {
            // 限制最大行数，避免内存溢出
            if (rowCount <= DEFAULT_MAX_ROWS) {
                sheetData.add(currentRow);
            }
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // 忽略页眉页脚
        }
    }

    @Override
    public List<String> supportedTypes() {
        return Arrays.asList("xls", "xlsx");
    }
}
