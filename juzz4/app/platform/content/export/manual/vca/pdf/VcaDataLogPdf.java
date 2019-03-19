package platform.content.export.manual.vca.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.Analytics.TickerReport;
import org.joda.time.DateTime;
import platform.analytics.VcaDataRequest;
import platform.content.FileFormat;
import platform.content.export.PdfSettings;
import platform.content.export.ReportBuilder;
import platform.content.export.VcaExportHelper;
import platform.events.EventType;
import play.Logger;
import play.i18n.Messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class VcaDataLogPdf implements ReportBuilder
{
    private final VcaDataRequest dataRequest;
    private final int tzOffsetMins;
    private final String locale;

    public VcaDataLogPdf(VcaDataRequest dataRequest, int tzOffsetMins, String locale) throws ApiException
    {
        this.locale = locale;
        if (dataRequest.getQuery().countAll() == 0)
        {
            throw new ApiException("no-data-logs");
        }

        this.dataRequest = dataRequest;
        this.tzOffsetMins = tzOffsetMins;
    }

    @Override
    public String getFilename()
    {
        return String.format("data_logs_%s_%s.%s",
                             dataRequest.getEventType().name().toLowerCase(),
                             VcaExportHelper.getGeneratedTime(tzOffsetMins),
                             getFileFormat().getExtension());
    }

    @Override
    public FileFormat getFileFormat()
    {
        return FileFormat.PDF;
    }

    @Override
    public InputStream generate()
    {
        try
        {
            Document doc = createDocument();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, outputStream);
            doc.open();

            Paragraph preface = new Paragraph();

            //event type and generation time
            preface.add(new Paragraph(Messages.getMessage(locale, dataRequest.getEventType().toString()),
                                      PdfSettings.titleFont));
            preface.add(new Paragraph(Messages.getMessage(locale, "report-generated-on") + ": " +
                                      VcaExportHelper.getLocalTimeStamp(DateTime.now().getMillis(), tzOffsetMins),
                                      PdfSettings.titleFont));

            preface.add(new Paragraph(" "));
            doc.add(preface);

            //query data and write
            Iterable<TickerReport> iterable = dataRequest.getQuery().fetch();
            PdfPTable table = null;
            for (TickerReport tickerReport : iterable)
            {
                Map<String, Object> dataMap = new LinkedHashMap<>();
                dataMap.put("timestamp", VcaExportHelper.getLocalTimeStamp(tickerReport.time, tzOffsetMins));
                dataMap.put("device-name", VcaExportHelper.getDeviceName(tickerReport.deviceId));
                dataMap.put("channel", VcaExportHelper.getChannelName(tickerReport.deviceId, tickerReport.channelId));
                dataMap.putAll(VcaExportHelper.asExportData(tickerReport));

                if (table == null)
                {
                    //init table and add columns row
                    List<String> columnList = new ArrayList<>(dataMap.keySet());
                    table = new PdfPTable(columnList.size());
                    table.setWidthPercentage(100);
                    table.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);
                    addColumnRow(table, columnList);
                }

                addDataRow(table, dataMap);
            }

            doc.add(table);
            doc.add(new Paragraph(" "));
            doc.close();
            outputStream.close();

            return new ByteArrayInputStream(outputStream.toByteArray());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    private Document createDocument()
    {
        return dataRequest.getEventType().equals(EventType.VCA_PROFILING) ?
               new Document(PageSize.LETTER.rotate()) :
               new Document();
    }

    private void addColumnRow(PdfPTable table, List<String> columnList)
    {
        for (String column : columnList)
        {
            PdfPCell cell = new PdfPCell();
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setBackgroundColor(PdfSettings.headerBgColor);
            cell.setMinimumHeight(PdfSettings.CELL_MIN_HEIGHT);
            cell.setNoWrap(false);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, column), PdfSettings.headerFont));
            table.addCell(cell);
        }
    }

    private void addDataRow(PdfPTable table, Map<String, Object> dataMap)
    {
        for (String column : dataMap.keySet())
        {
            PdfPCell cell = new PdfPCell();
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setMinimumHeight(PdfSettings.CELL_MIN_HEIGHT);
            cell.setNoWrap(false);

            Object data = dataMap.get(column);
            String cellValue = String.valueOf(data);

            //remove decimals
            if (data != null && Util.isDouble(cellValue))
            {
                Double d = Double.parseDouble(cellValue);
                cellValue = String.valueOf(d.longValue());
            }

            cell.setPhrase(new Paragraph(cellValue, PdfSettings.entryFont));
            table.addCell(cell);
        }
    }
}
