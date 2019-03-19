package platform.content.export.manual;

import com.kaisquare.util.XLSWriter;
import models.MongoBucket;
import models.MongoUser;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import play.Logger;
import play.i18n.Messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class BucketUsersXls implements ReportBuilder
{
    private final MongoBucket bucket;
    private final String locale;

    public BucketUsersXls(MongoBucket bucket, String locale)
    {
        this.bucket = bucket;
        this.locale = locale;
    }

    @Override
    public String getFilename()
    {
        return String.format("Users_%s.%s", bucket.getName(), getFileFormat().getExtension());
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
            XLSWriter writer = new XLSWriter("Bucket Users");
            int maxRowsPerSheet = writer.getMaxRowsPerSheet();

            //column names row
            List<Object> headerList = new ArrayList<>();
            headerList.add(Messages.getMessage(locale, "user-name"));
            headerList.add(Messages.getMessage(locale, "name"));
            headerList.add(Messages.getMessage(locale, "email"));
            headerList.add(Messages.getMessage(locale, "phone"));
            writer.addRow(headerList, true, false);

            //entries
            List<MongoUser> bucketUsers = MongoUser.q().filter("bucketId", bucket.getBucketId()).fetchAll();
            if (bucketUsers.size() > 0)
            {
                int rowIndex = 0;
                for (MongoUser user : bucketUsers)
                {

                    List<Object> list = new ArrayList<>();
                    list.add(user.getLogin());
                    list.add(user.getName());
                    list.add(user.getEmail());
                    list.add(user.getPhone());
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
