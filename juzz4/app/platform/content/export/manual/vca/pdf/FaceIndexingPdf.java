package platform.content.export.manual.vca.pdf;

import com.google.gson.Gson;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.mongodb.gridfs.GridFSDBFile;
import models.events.EventWithBinary;
import org.apache.commons.io.IOUtils;
import platform.analytics.VcaType;
import platform.content.export.ExportUtils;
import platform.content.export.PdfSettings;
import platform.db.gridfs.GridFsHelper;
import platform.events.EventInfo;
import platform.events.EventManager;
import play.Play;
import play.i18n.Messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class FaceIndexingPdf extends VcaPdfBuilder
{
    private static final String emptyFaceIndexImage = Play.applicationPath + "/resources/temp/empty.jpg";

    private final String chartSvg;
    private final List<String> eventIdList;
    private final String locale;

    public FaceIndexingPdf(VcaPdfData pdfData, String chartSvg, List<String> eventIdList, String locale)
    {
        super(pdfData);
        this.chartSvg = chartSvg;
        this.eventIdList = eventIdList;
        this.locale = locale;
    }

    @Override
    protected String getAnalyticsType()
    {
        return VcaType.FACE_INDEXING.getVcaTypeName();
    }

    @Override
    protected void populateBody(Document doc) throws Exception
    {
        //add chart image
    	if(chartSvg != null)
    	{
    		String pngFile = ExportUtils.convertSvgToPng(chartSvg);
            com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(pngFile);
            image.setAlignment(Image.ALIGN_CENTER);

            //scale the image to fit into page
            if(image.getWidth() > 520)
            {
            	float scaleRate = 520 / image.getWidth() * 100;
                image.scalePercent(scaleRate);
            }
            
            doc.add(image);
            doc.add(new Paragraph(" "));
    	}

        //Add face snapshot's table
        int columnCount = 6;
        PdfPTable faceTable = new PdfPTable(columnCount);
        faceTable.setWidthPercentage(100);
        faceTable.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        Font captionFont = new Font(PdfSettings.baseFont, 7, Font.NORMAL, BaseColor.DARK_GRAY);

        //loop and get face blobs
        for (String eventId : eventIdList)
        {
            EventWithBinary evt = EventWithBinary.q().filter("eventInfo.eventId", eventId).first();
            EventInfo eventInfo = evt.getEventInfo();
            String jsonData = evt.getJsonData();

            PdfPCell cell = new PdfPCell();
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setBorderWidth(2);
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            cell.setBorderColor(BaseColor.WHITE);
            cell.setPadding(6);
            Image faceSnapshot = null;
            try
            {
                EventWithBinary binary = EventWithBinary.find(eventId);
                GridFSDBFile dbFile = GridFsHelper.getGridFSDBFile(binary.getFileDetails());
                faceSnapshot = Image.getInstance(IOUtils.toByteArray(dbFile.getInputStream()));
            }
            catch (IOException e)
            {
                InputStream IS = new FileInputStream(new File(emptyFaceIndexImage));
                faceSnapshot = Image.getInstance(IOUtils.toByteArray(IS));
                IS.close();
            }

            faceSnapshot.scaleToFit(75, 75);
            cell.addElement(faceSnapshot);

            //get duration from event.data
            Map<String, String> eventData = new LinkedHashMap<String, String>();
            eventData = new Gson().fromJson(jsonData, eventData.getClass());
            String time = EventManager.getOriginalTime(eventInfo.getTime());

            cell.addElement(new Paragraph(ExportUtils.getLocalTimeString(time, pdfData.getTimeZoneOffsetMins()), captionFont));
            Double duration = Double.parseDouble(String.valueOf(eventData.get("duration"))) / 1000;
            cell.addElement(new Paragraph(Messages.getMessage(locale, "duration") +
                                          ": " +
                                          String.format("%.1f", duration) +
                                          "s", captionFont));
            faceTable.addCell(cell);
        }

        //populate empty cells in the last row
        int emptyCellCount = columnCount - (eventIdList.size() % columnCount);
        if (emptyCellCount != 0)
        {
            for (int i = 0; i < emptyCellCount; i++)
            {
                PdfPCell cell = new PdfPCell();
                cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                cell.setBorderWidth(2);
                cell.setBackgroundColor(BaseColor.WHITE);
                cell.setBorderColor(BaseColor.WHITE);
                cell.setPadding(6);
                cell.setPhrase(new Paragraph(""));
                faceTable.addCell(cell);
            }
        }
        doc.add(faceTable);
    }
}
