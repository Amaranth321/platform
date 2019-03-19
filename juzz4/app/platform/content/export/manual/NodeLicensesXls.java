package platform.content.export.manual;

import com.kaisquare.util.XLSWriter;
import models.licensing.NodeLicenseInfo;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import play.Logger;
import play.i18n.Messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class NodeLicensesXls implements ReportBuilder
{
    private final List<NodeLicenseInfo> nodeLicenseList;
    private final String locale;

    public NodeLicensesXls(List<NodeLicenseInfo> nodeLicenseList, String locale)
    {
        this.nodeLicenseList = nodeLicenseList;
        this.locale = locale;
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
        return FileFormat.XLS;
    }

    @Override
    public InputStream generate()
    {
        try
        {
            XLSWriter writer = new XLSWriter("Bucket Licenses");
            int maxRowsPerSheet = writer.getMaxRowsPerSheet();

            //column names row
            List<Object> headerList = new ArrayList<Object>();
            headerList.add(Messages.getMessage(locale, "bucket"));
            headerList.add(Messages.getMessage(locale, "status"));
            headerList.add(Messages.getMessage(locale, "license-number"));
            headerList.add(Messages.getMessage(locale, "validity"));
            headerList.add(Messages.getMessage(locale, "storage"));
            headerList.add(Messages.getMessage(locale, "expires-on"));
            headerList.add(Messages.getMessage(locale, "registration-number"));
            headerList.add(Messages.getMessage(locale, "device-name"));
            writer.addRow(headerList, true, false);

            //entries
            if (nodeLicenseList.size() > 0)
            {
                int rowIndex = 0;
                DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy");

                for (NodeLicenseInfo nodeLicenseInfo : nodeLicenseList)
                {
                    List<Object> list = new ArrayList<>();
                    list.add(nodeLicenseInfo.bucketName);
                    list.add(nodeLicenseInfo.status);
                    list.add(nodeLicenseInfo.getFormattedLicenseNumber());
                    if (nodeLicenseInfo.durationMonths >= 0)
                    {
                        list.add(nodeLicenseInfo.durationMonths + " Months");
                    }
                    else
                    {
                        list.add("Perpetual");
                    }
                    list.add(nodeLicenseInfo.cloudStorageGb + " GB");
                    if (nodeLicenseInfo.expiryDate > 0)
                    {
                        list.add(formatter.print(nodeLicenseInfo.expiryDate));
                    }
                    else
                    {
                        list.add("N/A");
                    }
                    list.add(nodeLicenseInfo.registrationNumber);
                    list.add(nodeLicenseInfo.deviceName);
                    writer.addRow(list);
                    rowIndex++;

                    if (rowIndex >= maxRowsPerSheet)
                    {
                        rowIndex = 0;
                    }
                }
            }

            //Generate outputStream
            byte[] fileBytes = null;
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
            {
                writer.writeFile(outputStream);
                fileBytes = outputStream.toByteArray();
            }

            return new ByteArrayInputStream(fileBytes);

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }
}
