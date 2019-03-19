package platform.content.export.manual;

import com.kaisquare.util.XLSWriter;
import models.MongoBucket;
import models.transients.DeviceInfo;
import org.apache.commons.lang.StringUtils;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import play.Logger;
import play.i18n.Messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BucketNodesXls implements ReportBuilder
{
    private final MongoBucket bucket;
    private final List<DeviceInfo> bucketNodesInfo;
    private final String locale;

    public BucketNodesXls(MongoBucket bucket, List<DeviceInfo> bucketNodesInfo, String locale)
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
        return FileFormat.XLS;
    }

    @Override
    public InputStream generate()
    {
        try
        {
            XLSWriter writer = new XLSWriter("Bucket Nodes");
            int maxRowsPerSheet = writer.getMaxRowsPerSheet();

            //column names row
            List<Object> headerList = new ArrayList<Object>();
            headerList.add(Messages.getMessage(locale, "name"));
            headerList.add(Messages.getMessage(locale, "device-key"));
            headerList.add(Messages.getMessage(locale, "version"));
            headerList.add(Messages.getMessage(locale, "label"));
            writer.addRow(headerList, true, false);

            //entries
            if (bucketNodesInfo.size() > 0)
            {
                int rowIndex = 0;
                for (DeviceInfo deviceinfo : bucketNodesInfo)
                {
                    List<Object> list = new ArrayList<>();
                    list.add(deviceinfo.name);
                    list.add(deviceinfo.deviceKey);
                    list.add(deviceinfo.node.getNodeVersion());
                    String labelList = StringUtils.join(deviceinfo.channelLabels, ", ");
                    list.add(labelList);
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
