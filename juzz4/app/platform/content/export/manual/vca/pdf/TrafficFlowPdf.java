package platform.content.export.manual.vca.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import platform.analytics.VcaType;
import platform.content.export.ExportUtils;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class TrafficFlowPdf extends VcaPdfBuilder
{
    private final String base64Jpeg;

    public TrafficFlowPdf(VcaPdfData pdfData, String base64Jpeg)
    {
        super(pdfData);
        this.base64Jpeg = base64Jpeg;
    }

    @Override
    protected String getAnalyticsType()
    {
        return VcaType.TRAFFIC_FLOW.getVcaTypeName();
    }

    @Override
    protected void populateBody(Document doc) throws Exception
    {
        String pngFile = ExportUtils.convertSvgToPng(base64Jpeg);
        com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(pngFile);
        image.setAlignment(Image.ALIGN_CENTER);
        image.scaleToFit(480, 360);
        doc.add(image);
    }
}
