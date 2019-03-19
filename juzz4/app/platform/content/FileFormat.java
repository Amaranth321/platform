package platform.content;

import lib.util.exceptions.ApiException;
import play.i18n.Messages;

/**
 * Convention here is to set the enum value to be the same as the capitalized form of the extension
 *
 * @author Aye Maung
 * @since v4.3
 */
public enum FileFormat
{
    PDF,
    CSV,
    XLS,
    MP4,
    UNKNOWN;

    public static FileFormat parse(String format) throws ApiException
    {
        try
        {
            return FileFormat.valueOf(format.toUpperCase());
        }
        catch (Exception e)
        {
            throw new ApiException(Messages.get("file-format-not-supported") + ":" + format);
        }
    }

    public String getContentType()
    {
        switch (this)
        {
            case PDF:
                return "application/pdf";

            case CSV:
                return "text/csv";

            case XLS:
                return "application/vnd.ms-excel";

            case MP4:
                return "video/mp4";

            default:
                return "application/octet-stream";
        }
    }

    public String getExtension()
    {
        return name().toLowerCase();
    }
}
