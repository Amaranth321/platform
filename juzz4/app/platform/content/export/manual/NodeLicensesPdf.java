package platform.content.export.manual;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import models.licensing.NodeLicenseInfo;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import platform.content.FileFormat;
import platform.content.export.PdfSettings;
import platform.content.export.ReportBuilder;
import play.Logger;
import play.i18n.Lang;
import play.i18n.Messages;

import static lib.util.Util.getClientTimeString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

public class NodeLicensesPdf implements ReportBuilder
{
    private final List<NodeLicenseInfo> nodeLicenseList;
    private final String locale;
    private final int  offsetMinutes;

    public NodeLicensesPdf(List<NodeLicenseInfo> nodeLicenseList, String locale, int offsetMinutes)
    {
        this.nodeLicenseList = nodeLicenseList;
        this.locale = locale;
        this.offsetMinutes = offsetMinutes;
    }

    @Override
    public String getFilename()
    {
        return String.format("NodeLicenses_%s.%s",
                             DateTime.now().toString("ddMMyyyyHHmmss"),
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
            Document doc = new Document(PageSize.LETTER.rotate());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, outputStream);
            doc.open();

            //time generated
            DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
            Paragraph preface = new Paragraph();
            Font titleFont = new Font(PdfSettings.baseFont, 11, Font.BOLD, BaseColor.BLACK);
            preface.add(new Paragraph(Messages.getMessage(locale, "license-list-generated-on") + ": "
                                      +/* formatter.print(DateTime.now().getMillis())*/getClientTimeString(DateTime.now(), offsetMinutes),
                                      titleFont));
            preface.add(new Paragraph(" "));
            doc.add(preface);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);

            //headers
            BaseColor headerBgColor = new BaseColor(196, 223, 155);
            int headerHeight = 18;
            PdfPCell cell = new PdfPCell();
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setBackgroundColor(headerBgColor);
            cell.setFixedHeight(headerHeight);

            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "bucket"), PdfSettings.headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "status"), PdfSettings.headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "license-number"), PdfSettings.headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "validity"), PdfSettings.headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "storage"), PdfSettings.headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "expires-on"), PdfSettings.headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "registration-number"), PdfSettings.headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "device-name"), PdfSettings.headerFont));
            table.addCell(cell);

            //Populate Rows
            Font licenseFont = new Font(PdfSettings.baseFont, 8, Font.NORMAL, BaseColor.BLACK);
            int rowHeight = 15;
            PdfPCell newcell = new PdfPCell();
            newcell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            newcell.setFixedHeight(rowHeight);

            if (nodeLicenseList.size() > 0)
            {
                formatter = DateTimeFormat.forPattern("dd/MM/yyyy");

                for (NodeLicenseInfo nodeLicenseInfo : nodeLicenseList)
                {
                    newcell.setPhrase(new Paragraph(nodeLicenseInfo.bucketName, PdfSettings.entryFont));
                    table.addCell(newcell);
                    newcell.setPhrase(new Paragraph(nodeLicenseInfo.status.toString(), PdfSettings.entryFont));
                    table.addCell(newcell);
                    newcell.setPhrase(new Paragraph(nodeLicenseInfo.getFormattedLicenseNumber(), licenseFont));
                    table.addCell(newcell);
                    if (nodeLicenseInfo.durationMonths >= 0)
                    {
                        newcell.setPhrase(new Paragraph(nodeLicenseInfo.durationMonths +
                                                        " Months", PdfSettings.entryFont));
                    }
                    else
                    {
                        newcell.setPhrase(new Paragraph(Messages.getMessage(locale, "perpetual"),
                                                        PdfSettings.entryFont));
                    }
                    table.addCell(newcell);
                    newcell.setPhrase(new Paragraph(nodeLicenseInfo.cloudStorageGb + " GB", PdfSettings.entryFont));
                    table.addCell(newcell);
                    if (nodeLicenseInfo.expiryDate > 0)
                    {
                        newcell.setPhrase(new Paragraph(formatter.print(nodeLicenseInfo.expiryDate),
                                                        PdfSettings.entryFont));
                    }
                    else
                    {
                        newcell.setPhrase(new Paragraph("N/A", PdfSettings.entryFont));
                    }
                    table.addCell(newcell);
                    newcell.setPhrase(new Paragraph(nodeLicenseInfo.registrationNumber, PdfSettings.entryFont));
                    table.addCell(newcell);
                    newcell.setPhrase(new Paragraph(nodeLicenseInfo.deviceName, PdfSettings.entryFont));
                    table.addCell(newcell);
                }
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
