package platform.content.export.manual;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import models.MongoBucket;
import models.transients.DeviceInfo;
import org.apache.commons.lang.StringUtils;
import platform.content.FileFormat;
import platform.content.export.PdfSettings;
import platform.content.export.ReportBuilder;
import play.Logger;
import play.i18n.Messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

public class BucketNodesPdf implements ReportBuilder
{
    private final MongoBucket bucket;
    private final List<DeviceInfo> bucketNodesInfo;
    private final String locale;

    public BucketNodesPdf(MongoBucket bucket, List<DeviceInfo> bucketNodesInfo, String locale)
    {
        this.bucket = bucket;
        this.bucketNodesInfo = bucketNodesInfo;
        this.locale = locale;
    }

    @Override
    public String getFilename()
    {
        return String.format("Nodes_%s.%s", bucket.getName(), getFileFormat().getExtension());
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

            PdfPTable table = new PdfPTable(4);
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

            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "name"), headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "device-key"), headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "version"), headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "label"), headerFont));
            table.addCell(cell);

            //Populate Rows
            Font entryFont = new Font(PdfSettings.baseFont, PdfSettings.FONT_SMALL, Font.NORMAL, BaseColor.BLACK);

            PdfPCell newcell = new PdfPCell();
            newcell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);

            if (bucketNodesInfo.size() > 0)
            {
                for (DeviceInfo deviceinfo : bucketNodesInfo)
                {
                    newcell.setPhrase(new Paragraph(deviceinfo.name, entryFont));
                    table.addCell(newcell);
                    newcell.setPhrase(new Paragraph(deviceinfo.deviceKey, entryFont));
                    table.addCell(newcell);
                    newcell.setPhrase(new Paragraph(deviceinfo.node.getNodeVersion(), entryFont));
                    table.addCell(newcell);
                    String labelList = StringUtils.join(deviceinfo.channelLabels, ", ");
                    newcell.setPhrase(new Paragraph(labelList, entryFont));
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
