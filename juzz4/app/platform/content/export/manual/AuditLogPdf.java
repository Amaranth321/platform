package platform.content.export.manual;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import models.AuditLog;
import org.joda.time.DateTime;
import platform.content.FileFormat;
import platform.content.export.ExportUtils;
import platform.content.export.PdfSettings;
import platform.content.export.ReportBuilder;
import play.Logger;
import play.i18n.Messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import static lib.util.Util.getClientTimeString;


public class AuditLogPdf implements ReportBuilder
{
    private final List<AuditLog> auditLogList;
    private final int timeZoneOffsetMins;
    private final String locale;

    public AuditLogPdf(List<AuditLog> auditLogList, int timeZoneOffsetMins, String locale)
    {
        this.auditLogList = auditLogList;
        this.timeZoneOffsetMins = timeZoneOffsetMins;
        this.locale = locale;
    }

    @Override
    public String getFilename()
    {
        return String.format("AuditLogs_%s.%s",
                             getClientTimeString(DateTime.now(), timeZoneOffsetMins),
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
            Document doc = new Document();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, outputStream);
            doc.open();

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);

            //headers
            Font headerFont = new Font(PdfSettings.baseFont, PdfSettings.FONT_LARGE, Font.BOLD, BaseColor.BLACK);
            BaseColor headerBgColor = new BaseColor(196, 223, 155);
            int headerHeight = 18;
            PdfPCell cell = new PdfPCell();
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setBackgroundColor(headerBgColor);
            cell.setFixedHeight(headerHeight);

            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "datetime"), headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "company-name"), headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "username"), headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "activity-type"), headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "ip"), headerFont));
            table.addCell(cell);

            //Populate Rows
            Font entryFont = new Font(PdfSettings.baseFont, PdfSettings.FONT_SMALL, Font.NORMAL, BaseColor.BLACK);

            int rowHeight = 15;
            PdfPCell newcell = new PdfPCell();
            newcell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            newcell.setFixedHeight(rowHeight);

            for (AuditLog auditLog : auditLogList)
            {
                newcell.setPhrase(new Paragraph(ExportUtils.getLocalTimeString(new Date(auditLog.timeobject),
                                                                               timeZoneOffsetMins), entryFont));
                table.addCell(newcell);

                newcell.setPhrase(new Paragraph(auditLog.bucketName, entryFont));
                table.addCell(newcell);

                newcell.setPhrase(new Paragraph(auditLog.userName, entryFont));
                table.addCell(newcell);

                newcell.setPhrase(new Paragraph(auditLog.serviceName, entryFont));
                table.addCell(newcell);

                newcell.setPhrase(new Paragraph(auditLog.remoteIp, entryFont));
                table.addCell(newcell);
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
}
