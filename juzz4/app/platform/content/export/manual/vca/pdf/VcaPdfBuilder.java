package platform.content.export.manual.vca.pdf;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.joda.time.DateTime;
import platform.content.FileFormat;
import platform.content.export.PdfSettings;
import platform.content.export.ReportBuilder;
import play.Logger;
import play.i18n.Messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lib.util.Util.getClientTimeString;

/**
 * @author Aye Maung
 * @since v4.3
 */
public abstract class VcaPdfBuilder implements ReportBuilder
{
    protected final VcaPdfData pdfData;

    protected VcaPdfBuilder(VcaPdfData pdfData)
    {
        this.pdfData = pdfData;
    }

    private Paragraph getGeneratedTimeLabel()
    {
        Paragraph generatedLabel = new Paragraph();
        String text = String.format("%s: %s",
                                    Messages.getMessage(pdfData.getLocale(), "report-generated-on"),
                                    getClientTimeString(DateTime.now(), pdfData.getTimeZoneOffsetMins()));
        generatedLabel.add(new Paragraph(text, PdfSettings.titleFont));
        generatedLabel.add(new Paragraph(" "));
        return generatedLabel;
    }

    private PdfPTable getSearchParamsTable()
    {
        try
        {
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
            table.setWidths(new float[]{1, 3});

            //Set cell styles
            PdfPCell newCell = new PdfPCell();
            Font tableCellFont = new Font(PdfSettings.baseFont, PdfSettings.FONT_SMALL, Font.NORMAL, BaseColor.BLACK);
            newCell.setMinimumHeight(15);

            Map<String, String> infoMap = pdfData.getSearchParams();
            for (String key : infoMap.keySet())
            {
                newCell.setPhrase(new Paragraph(Messages.getMessage(pdfData.getLocale(), key), tableCellFont));
                newCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                newCell.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);
                table.addCell(newCell);

                //translate if value is of event type
                if (key.equals("event-type"))
                {
                    newCell.setPhrase(new Paragraph(Messages.getMessage(pdfData.getLocale(), infoMap.get(key)),
                                                    tableCellFont));
                }
                else
                {
                    newCell.setPhrase(new Paragraph(String.valueOf(infoMap.get(key)), tableCellFont));
                }

                newCell.setBackgroundColor(BaseColor.WHITE);
                newCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                table.addCell(newCell);
            }
            return table;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    abstract protected String getAnalyticsType();

    abstract protected void populateBody(Document doc) throws Exception;

    protected List<String> splitSvgString(String svgString)
    {
        List<String> svgList = new ArrayList<>();
        int pos = 0;
        int end = 0;
        do
        {
            pos = svgString.indexOf("<svg", pos);
            if (pos >= 0)
            {
                end = svgString.indexOf("</svg>", pos) + 6;
                String svgStr = new String(svgString.substring(pos, end));
                svgList.add(svgStr);
            }
            else
            {
                break;
            }

            pos = end;
        } while (pos < svgString.length());

        return svgList;
    }

    @Override
    public String getFilename()
    {
        Map<String, String> infoMap = pdfData.getSearchParams();
        String filename = String.format("%s_%s_%s.pdf",
                                        getAnalyticsType(),
                                        infoMap.get("from"),
                                        infoMap.get("to"));
        return filename;
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

            //time generated label
            doc.add(getGeneratedTimeLabel());

            //Search Params box
            doc.add(getSearchParamsTable());
            doc.add(new Paragraph(" "));

            //vca specific body
            populateBody(doc);
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
