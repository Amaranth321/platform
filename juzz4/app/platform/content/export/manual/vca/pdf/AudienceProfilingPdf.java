package platform.content.export.manual.vca.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import platform.analytics.VcaType;
import platform.content.export.ExportUtils;

import java.util.List;


public class AudienceProfilingPdf extends VcaPdfBuilder
{
    private final List<String> donutSvgList;
    private final List<String> areaSvgList;

    public AudienceProfilingPdf(VcaPdfData pdfData, List<String> donutSvgList, List<String> areaSvgList)
    {
        super(pdfData);
        this.donutSvgList = donutSvgList;
        this.areaSvgList = areaSvgList;
    }

    @Override
    protected String getAnalyticsType()
    {
        return VcaType.AUDIENCE_PROFILING.getVcaTypeName();
    }

    @Override
    protected void populateBody(Document doc) throws Exception
    {
        for (String svg : donutSvgList)
        {
            String donutSvg = null;
            if (svg.contains("</svg>"))
            {
                donutSvg = svg;
            }
            else
            {
                donutSvg = svg.concat("</svg>");
            }
            String pngFile1 = ExportUtils.convertSvgToPng(donutSvg);
            com.itextpdf.text.Image image1 = com.itextpdf.text.Image.getInstance(pngFile1);
            image1.setAlignment(Image.ALIGN_CENTER);

            //scale the image to fit into page
            if(image1.getWidth() > 520)
            {
            	float scaleRate = 520 / image1.getWidth() * 100;
            	image1.scalePercent(scaleRate);
            }
            
            doc.add(image1);
            doc.add(new Paragraph(" "));
        }
        for (String area : areaSvgList)
        {
            String areaSvg = null;
            if (area.contains("</svg>"))
            {
                areaSvg = area;
            }
            else
            {
                areaSvg = area.concat("</svg>");
            }
            String pngFile2 = ExportUtils.convertSvgToPng(areaSvg);
            com.itextpdf.text.Image image2 = com.itextpdf.text.Image.getInstance(pngFile2);
            image2.setAlignment(Image.ALIGN_CENTER);

            //scale the image to fit into page
            if(image2.getWidth() > 520)
            {
            	float scaleRate = 520 / image2.getWidth() * 100;
            	image2.scalePercent(scaleRate);
            }
            
            doc.add(image2);
            doc.add(new Paragraph(" "));
        }
    }
}
