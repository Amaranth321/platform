package models;

import com.google.code.morphia.annotations.Entity;
import platform.content.ftp.FTPDetails;
import play.modules.morphia.Model;

import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
@Entity
public class POSImportSettings extends Model
{
    private final long bucketId;
    private boolean enabled;
    private FTPDetails ftpDetails;

    public static POSImportSettings of(long bucketId)
    {
        POSImportSettings settings = q().filter("bucketId", bucketId).get();
        if (settings == null)
        {
            settings = new POSImportSettings(bucketId);
            settings.save();
        }
        return settings;
    }

    private POSImportSettings(long bucketId)
    {
        this.bucketId = bucketId;
        enabled = false;
        ftpDetails = FTPDetails.notConfigured();
    }

    public long getBucketId()
    {
        return bucketId;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public FTPDetails getFtpDetails()
    {
        return ftpDetails;
    }

    public void setFtpDetails(FTPDetails ftpDetails)
    {
        this.ftpDetails = ftpDetails;
    }

    public boolean isDirectoryInUseByOthers()
    {
        List<POSImportSettings> iterable = POSImportSettings.q().filter("bucketId !=", bucketId).fetchAll();
        for (POSImportSettings dbSettings : iterable)
        {
            if (dbSettings.getFtpDetails().hasSameDirectory(ftpDetails))
            {
                return true;
            }
        }

        return false;
    }

}
