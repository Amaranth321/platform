/*
 * XLSWriter.java
 *
 * Copyright (C) KAI Square Pte Ltd
 */

package com.kaisquare.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import play.i18n.Lang;
import play.i18n.Messages;

/**
 * A class for writing an Excel XLS file.
 *
 * @author Tan Yee Fan
 */
public class XLSWriter {
    private static final int MAX_ROWS_PER_SHEET = 65536;

    private Workbook workbook;
    private String sheetName;
    private int sheetIndex;
    private Sheet sheet;
    private int rowIndex;
    private int numColumns;
    private CellStyle[] styles;

    /**
     * Creates a new XLS file writer.
     */
    public XLSWriter(String sheetName) {
        this.workbook = new HSSFWorkbook();
        this.sheetName = WorkbookUtil.createSafeSheetName(sheetName);
        this.sheetIndex = 0;
        this.sheet = null;
        this.rowIndex = 0;
        this.numColumns = 0;
        this.styles = new CellStyle[4];
        this.styles[0] = createCellStyle(false, false);
        this.styles[1] = createCellStyle(false, true);
        this.styles[2] = createCellStyle(true, false);
        this.styles[3] = createCellStyle(true, true);
    }

    /**
     * Returns the maximum number of rows per sheet.
     */
    public int getMaxRowsPerSheet() {
        return MAX_ROWS_PER_SHEET;
    }

    /**
     * Creates a new cell style.
     *
     * @param bold Whether the style should be bold.
     * @param bold Whether the style should be italic.
     */
    private CellStyle createCellStyle(boolean bold, boolean italic) {
        Font font = this.workbook.createFont();
        if (bold) {
            font.setBold(true);
        }
        if (italic) {
            font.setItalic(true);
        }
        CellStyle style = this.workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    /**
     * Adds a new sheet to the XLS file.
     */
    private void addSheet() {
        this.sheetIndex++;
        String sheetName = this.sheetName + " - Page " + this.sheetIndex;
        this.sheet = this.workbook.createSheet(sheetName);
        this.rowIndex = 0;
    }

    /**
     * Adds a row to the XLS file.
     *
     * @param list List of items to be written.
     */
    public void addRow(List<Object> list) {
        addRow(list, false, false);
    }

    /**
     * Adds a row to the XLS file.
     *
     * @param list List of items to be written.
     * @param bold Whether the style should be bold.
     * @param bold Whether the style should be italic.
     */
    public void addRow(List<Object> list, boolean bold, boolean italic) {
        if (this.sheet == null) {
            addSheet();
        }
        Row row = this.sheet.createRow(this.rowIndex);
        int columnIndex = 0;
        for (Object item : list) {
            Cell cell = row.createCell(columnIndex);
            if (item instanceof Boolean) {
                cell.setCellValue((Boolean) item);
            } else if (item instanceof Number) {
                cell.setCellValue(((Number) item).doubleValue());
            } else if (item instanceof Calendar) {
                cell.setCellValue((Calendar) item);
            } else if (item instanceof Date) {
                cell.setCellValue((Date) item);
            } else if (item != null) {
                cell.setCellValue(String.valueOf(item));
            }
            int styleIndex = 0;
            if (bold) {
                styleIndex += 2;
            }
            if (italic) {
                styleIndex += 1;
            }
            cell.setCellStyle(this.styles[styleIndex]);
            columnIndex++;
        }
        if (this.numColumns < list.size()) {
            this.numColumns = list.size();
        }
        this.rowIndex++;
        if (this.rowIndex >= MAX_ROWS_PER_SHEET) {
            for (int i = 0; i < this.numColumns; i++) {
                this.sheet.autoSizeColumn(i);
            }
            this.sheet = null;
        }
    }

    /**
     * Writes the XLS file to the given output stream. The output stream
     * will be closed after the writing.
     *
     * @param outputStream Output stream.
     * @throws IOException If the XLS file could not be written.
     */
    public void writeFile(OutputStream outputStream) throws IOException {
        for (int i = 0; i < this.numColumns; i++) {
            this.sheet.autoSizeColumn(i);
        }
        this.workbook.write(outputStream);
        outputStream.close();
    }

    /**
     * Added by nguyenhhien
     * <p/>
     * Add header row to XLS file;
     * Translate to the current language;
     *
     * @param list List of items to be written.
     * @param bold Whether the style should be bold.
     * @param bold Whether the style should be italic.
     */
    public void addRow2(List<Object> list, boolean bold, boolean italic) {
        if (this.sheet == null) {
            addSheet();
        }
        Row row = this.sheet.createRow(this.rowIndex);
        int columnIndex = 0;
        for (Object item : list) {
            Cell cell = row.createCell(columnIndex);

            String translatedStr = translateToCurrentLocale(item.toString());
            cell.setCellValue(translatedStr);

            int styleIndex = 0;
            if (bold) {
                styleIndex += 2;
            }
            if (italic) {
                styleIndex += 1;
            }
            cell.setCellStyle(this.styles[styleIndex]);
            columnIndex++;
        }
        if (this.numColumns < list.size()) {
            this.numColumns = list.size();
        }
        this.rowIndex++;
        if (this.rowIndex >= MAX_ROWS_PER_SHEET) {
            for (int i = 0; i < this.numColumns; i++) {
                this.sheet.autoSizeColumn(i);
            }
            this.sheet = null;
        }
    }

    private static String getKey(String msgVal, String locale) {
        Properties properties = Messages.all(locale);
        Enumeration em = properties.keys();

        while (em.hasMoreElements()) {
            String tmpKey = em.nextElement().toString();
            String tmpVal = properties.get(tmpKey).toString();

            if (tmpVal.equalsIgnoreCase(msgVal))
                return tmpKey;
        }

        return null;
    }

    public static String translateToCurrentLocale(String enStr) {

        String locale = Lang.get();

        //System.err.println("The current locale: " + locale);
        if (locale.equals("en"))
            return enStr;

        String key = getKey(enStr, "en");
        if (key == null)
            return "NOT FOUND";

        return Messages.getMessage(locale, key);
    }
}

