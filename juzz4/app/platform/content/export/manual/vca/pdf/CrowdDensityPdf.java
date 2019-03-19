package platform.content.export.manual.vca.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import lib.util.Util;
import org.apache.commons.codec.binary.Base64;
import platform.analytics.VcaType;
import platform.content.export.ExportUtils;
import play.Play;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class CrowdDensityPdf extends VcaPdfBuilder
{
    private static final String heatmapLegend = "/public/css/common/images/heatmap_legend.jpg";

    private final byte[] liveSnapshot;
    private final String base64Jpeg;
    private final String base64RegionJpeg;
    private final String chartSvg;

    public CrowdDensityPdf(VcaPdfData pdfData,
                           byte[] liveSnapshot,
                           String base64Jpeg,
                           String base64RegionJpeg,
                           String chartSvg)
    {
        super(pdfData);
        this.liveSnapshot = liveSnapshot;
        this.base64Jpeg = base64Jpeg;
        this.base64RegionJpeg = base64RegionJpeg;
        this.chartSvg = chartSvg;
    }

    @Override
    protected String getAnalyticsType()
    {
        return VcaType.CROWD_DETECTION.getVcaTypeName();
    }

    @Override
    protected void populateBody(Document doc) throws Exception
    {
        //legend image
        com.itextpdf.text.Image legend = com.itextpdf.text.Image.getInstance(Play.applicationPath + heatmapLegend);
        legend.setAlignment(Image.LEFT | Image.TEXTWRAP);
        legend.scaleToFit(320, 240);

        //crowd heatmap image
        byte[] jpegBinary = null;
        if (liveSnapshot != null)
        {
            BufferedImage bg = ImageIO.read(new ByteArrayInputStream(liveSnapshot));
            BufferedImage heatmap = ImageIO.read(new ByteArrayInputStream(Base64.decodeBase64(base64Jpeg)));
            BufferedImage heatmapRegion = null;
            int regionImageHeight = 0;
            int regionImageWidth = 0;
            if (!Util.isNullOrEmpty(base64RegionJpeg))
            {
                heatmapRegion = ImageIO.read(new ByteArrayInputStream(Base64.decodeBase64(base64RegionJpeg)));
                regionImageWidth = heatmap.getWidth() * 2;
                regionImageHeight = heatmap.getHeight() * 2;
            }
            BufferedImage combined = null;
            int w = heatmap.getWidth();
            int h = heatmap.getHeight();
            combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics g = combined.getGraphics();
            g.drawImage(bg, 0, 0, w, h, null);
            g.drawImage(heatmap, 0, 0, w, h, null);
            if (heatmapRegion != null)
            {
                g.drawImage(heatmapRegion, -260, -195, regionImageWidth, regionImageHeight, null);
            }
            g.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(combined, "PNG", output);
            jpegBinary = output.toByteArray();
        }
        else
        {
            jpegBinary = Base64.decodeBase64(base64Jpeg);
        }

        com.itextpdf.text.Image heatmapImage = com.itextpdf.text.Image.getInstance(jpegBinary);
        heatmapImage.scaleToFit(1000, 240);

        Paragraph imageBody = new Paragraph();
        imageBody.setIndentationLeft(75);
        imageBody.add(legend);
        imageBody.add(heatmapImage);
        doc.add(imageBody);

        if (!Util.isNullOrEmpty(chartSvg))
        {
            doc.add(new Paragraph(" "));
            String stackSvgStr = null;
            if (chartSvg.contains("</svg>"))
            {
                stackSvgStr = chartSvg;
            }
            else
            {
                stackSvgStr = chartSvg.concat("</svg>");
            }
            String pngFile1 = ExportUtils.convertSvgToPng(stackSvgStr);
            com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(pngFile1);
            image.setAlignment(Image.ALIGN_CENTER);
            
            //scale the image to fit into page
            if(image.getWidth() > 520)
            {
            	float scaleRate = 520 / image.getWidth() * 100;
                image.scalePercent(scaleRate);
            }
            
            doc.add(image);
        }
    }

}
