package platform.content.export.manual;

import com.kaisquare.util.CSVWriter;
import models.transients.UserInfo;
import org.joda.time.DateTime;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import play.Logger;
import play.i18n.Messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class UserListCsv implements ReportBuilder
{
    private final List<UserInfo> userList;
    private final String locale;

    public UserListCsv(List<UserInfo> userList, String locale)
    {
        this.userList = userList;
        this.locale = locale;
    }

    @Override
    public String getFilename()
    {
        return String.format("user_list_%s.%s",
                             DateTime.now().getMillis(),
                             getFileFormat().getExtension());
    }

    @Override
    public FileFormat getFileFormat()
    {
        return FileFormat.CSV;
    }

    @Override
    public InputStream generate()
    {
        try
        {
            //Generate binary data;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CSVWriter writer = new CSVWriter(outputStream);
            List<Object> headerList = new ArrayList<Object>();
            headerList.add(Messages.getMessage(locale, "label"));
            headerList.add(Messages.getMessage(locale, "login"));
            headerList.add(Messages.getMessage(locale, "full-name"));
            headerList.add(Messages.getMessage(locale, "email"));
            headerList.add(Messages.getMessage(locale, "phone"));
            headerList.add(Messages.getMessage(locale, "user-id"));
            headerList.add(Messages.getMessage(locale, "roles"));

            if (userList.size() > 0)
            {
                int rowIndex = 0;
                for (models.transients.UserInfo uinfo : userList)
                {

                    if (rowIndex == 0)
                    {
                        writer.writeLine(headerList);
                        rowIndex++;
                    }

                    List<Object> list = new ArrayList<>();
                    list.add(uinfo.joinedLabels);
                    list.add(uinfo.login);
                    list.add(uinfo.name);
                    list.add(uinfo.email);
                    list.add(uinfo.phone);
                    list.add(uinfo.userId);
                    list.add(uinfo.roles);
                    writer.writeLine(list);
                    rowIndex++;
                }
            }
            else
            {
                writer.writeLine(headerList);
            }

            outputStream.close();
            writer.close();

            return new ByteArrayInputStream(outputStream.toByteArray());

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }
}
