package platform.content.export;

import lib.util.ToCSV;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import platform.ReportManager;
import platform.common.ACResource;
import play.Logger;
import play.Play;

import java.io.*;
import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;

/**
 * Utility functions copied from ReportManager
 *
 * @author Aye Maung
 * @since v4.3
 */
public final class ExportUtils
{
    private static final String EXPORT_TMP_DIR = ReportManager.REPORT_DIRECTORY + "exports/";

    private ExportUtils()
    {
    }

    private static String getExportTmpDir()
    {
        File exportDir = new File(Play.applicationPath + EXPORT_TMP_DIR);
        if (!exportDir.exists())
        {
            exportDir.mkdirs();
        }

        return exportDir.getAbsolutePath() + "/";
    }

    public static String getLocalTimeString(String utcString, int offsetMinutes)
    {
        String dtFormat = "dd/MM/yyyy HH:mm:ss";
        DateTime dtUTC = DateTime.parse(utcString, DateTimeFormat.forPattern(dtFormat).withZoneUTC());
        DateTime dtLocal = dtUTC.plusMinutes(offsetMinutes);
        return dtLocal.toString(dtFormat);
    }

    public static String getLocalTimeString(Date utcDate, int offsetMinutes)
    {
        String dtFormat = "dd/MM/yyyy HH:mm:ss";
        DateTime dtUtc = new DateTime(utcDate, DateTimeZone.UTC);
        DateTime dtLocal = dtUtc.plusMinutes(offsetMinutes);
        return dtLocal.toString(dtFormat);
    }

    /**
     * The text localization inside svg file is customized for kendo charts only
     */
    public static String convertSvgToPng(String svgString) throws Exception
    {
        OutputStream ostream = null;
        try
        {
            //change font of texts inside svg file
            String customFontBlock = " <style type=\"text/css\">"
                                     + "@font-face {"
                                     + "    font-family: CustomTTF;"
                                     + "    src: url('../fonts/" + PdfSettings.baseTtfFile + "') format('truetype');"
                                     + "}"
                                     + "</style></svg>";

            StringBuilder sbSvg = new StringBuilder();
            sbSvg.append(svgString.replace("Arial,Helvetica,sans-serif", "CustomTTF")
                                 .replace("Muli, sans-serif", "CustomTTF")
                                 .replace("</svg>", customFontBlock)
                                 .replace(" xmlns=\"http://www.w3.org/2000/svg\"", "")
                                 .replace(" xmlns=\'http://www.w3.org/2000/svg\'", "")
                                 .replace("stroke=''", "")
                                 .replace("<svg ", "<svg xmlns=\"http://www.w3.org/2000/svg\" "));
            String tempDir = Play.applicationPath + ReportManager.REPORT_DIRECTORY;
            int randomInt = new SecureRandom().nextInt(100000);

            //create svg file
            String svgFile = tempDir + randomInt + ".svg";
            String pngFile = tempDir + randomInt + ".png";
            try
            {
                FileWriter fstream = new FileWriter(svgFile, false);
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(sbSvg.toString());
                out.close();
            }
            catch (Exception e)
            {
                Logger.error(e, "");
                return null;
            }

            //transcode
            PNGTranscoder pngCoder = new PNGTranscoder();
            String svgURI = new File(svgFile).toURL().toString();
            TranscoderInput input = new TranscoderInput(svgURI);
            ostream = new FileOutputStream(pngFile);
            TranscoderOutput output = new TranscoderOutput(ostream);

            pngCoder.transcode(input, output);

            return pngFile;
        }
        catch (Exception e)
        {
            throw e;
        }
        finally
        {
            // Flush and close the output.
            if (ostream != null)
            {
                ostream.flush();
                ostream.close();
            }
        }
    }

    /**
     * Helper function to convert xls bytes to csv files
     * To be removed after proper csv generation codes are in place
     *
     * @param xlsBytes
     */
    public static byte[] toCsvFileBytes(byte[] xlsBytes)
    {
        String tmpFilename = UUID.randomUUID().toString();

        //bytes to xls file
        File xlsFile = new File(getExportTmpDir() + tmpFilename + ".xls");
        try
        {
            xlsFile.createNewFile();
            FileUtils.writeByteArrayToFile(xlsFile, xlsBytes);
        }
        catch (IOException e)
        {
            Logger.error(e, "");
        }

        //conversion
        File convertedFolder = new File(getExportTmpDir() + "converted");
        if (!convertedFolder.exists())
        {
            convertedFolder.mkdir();
        }
        boolean csvResult = new ToCSV().convertToCsv(xlsFile.getAbsolutePath(), convertedFolder.getAbsolutePath());
        if (!csvResult)
        {
            return null;
        }

        //csv to bytes
        byte[] csvBytes = null;
        File csvFile = new File(convertedFolder.getAbsolutePath() + "/" + tmpFilename + ".csv");
        try (ACResource<FileInputStream> acIn = new ACResource<>(new FileInputStream(csvFile)))
        {
            csvBytes = IOUtils.toByteArray(acIn.get());
        }
        catch (IOException e)
        {
            Logger.error(e, "");
        }

        //remove tmp files
        xlsFile.delete();
        csvFile.delete();

        return csvBytes;
    }
}
