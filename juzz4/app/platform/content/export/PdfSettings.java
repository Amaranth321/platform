package platform.content.export;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;
import play.Logger;
import play.Play;

/**
 * static variable names here are not capitalized yet to match the original unported codes
 *
 * @author Aye Maung
 * @since v4.3
 */
public final class PdfSettings
{
    private static final String fontPath = "/public/files/fonts/";

    public static final int FONT_LARGE = 10;
    public static final int FONT_MEDIUM = 8;
    public static final int FONT_SMALL = 6;

    public static final BaseColor headerBgColor = new BaseColor(196, 223, 155);
    public static final int headerHeight = 18;

    /**
     * Fonts
     */
    public static final String baseTtfFile = "kaiu.ttf";  // must be a font that can display both english and chinese characters

    public static BaseFont baseFont = null;
    public static Font titleFont = null;
    public static Font subtitleFont = null;
    public static Font entryFont = null;
    public static Font headerFont = null;

    /**
     * table
     */
    public static final int CELL_MIN_HEIGHT = 15;

    /**
     *
     * initialize
     *
     */
    static
    {
        try
        {
            String fontFile = Play.applicationPath + fontPath + baseTtfFile;

            baseFont = BaseFont.createFont(fontFile, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            titleFont = new Font(baseFont, FONT_LARGE, Font.BOLD, BaseColor.BLACK);
            subtitleFont = new Font(baseFont, FONT_MEDIUM, Font.NORMAL, BaseColor.BLACK);
            entryFont = new Font(baseFont, FONT_MEDIUM, Font.NORMAL, BaseColor.BLACK);
            headerFont = new Font(baseFont, FONT_MEDIUM, Font.NORMAL, BaseColor.BLACK);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }
}
