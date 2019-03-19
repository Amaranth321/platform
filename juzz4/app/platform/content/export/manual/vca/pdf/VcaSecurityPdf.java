package platform.content.export.manual.vca.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import platform.content.export.ExportUtils;
import play.Logger;

import java.util.List;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class VcaSecurityPdf extends VcaPdfBuilder
{
    private final String svgString;

    public VcaSecurityPdf(VcaPdfData pdfData, String svgString)
    {
        super(pdfData);
        this.svgString = svgString;
    }

    @Override
    protected String getAnalyticsType()
    {
        return "VCA_SECURITY";
    }

    @Override
    protected void populateBody(Document doc) throws Exception
    {
        List<String> svgList = splitSvgString(svgString);
        for (String svgStr : svgList)
        {
            try
            {
                //create png for each svg in the string
                String pngFile = ExportUtils.convertSvgToPng(svgStr);
                com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(pngFile);
                image.setAlignment(Image.ALIGN_CENTER);
                
                //scale the image to fit into page
                if(image.getWidth() > 580)
                {
                	float scaleRate = 580 / image.getWidth() * 100;
                    image.scalePercent(scaleRate);
                }
                
                doc.add(image);
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }
    }
}
