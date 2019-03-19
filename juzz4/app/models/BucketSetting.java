package models;

import com.google.code.morphia.annotations.Entity;
import com.mongodb.gridfs.GridFSDBFile;
import lib.util.Util;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import platform.BucketManager;
import platform.LocationManager;
import platform.access.DefaultBucket;
import platform.db.gridfs.GridFsHelper;
import play.Logger;
import play.modules.morphia.Model;

import java.io.File;

@Entity
public class BucketSetting extends Model
{
    public static final int DEFAULT_MAX_USER_COUNT = 10;
    public static final String BUCKET_LOGO_BLOB_GRIDFS = "bucketlogoblobs";

    public String bucketId;
    public int userLimit;
    public boolean emailVerificationOfUsersEnabled;
    public String logoBlobId;
    public String mapSource;

    public BucketSetting(String bucketId)
    {
        this.bucketId = bucketId;
        userLimit = DEFAULT_MAX_USER_COUNT;
        emailVerificationOfUsersEnabled = false;
        logoBlobId = "";
        mapSource = LocationManager.DEFAULT_MAP_SOURCE;
    }


    public String getBase64EncodedLogoString()
    {
        String logoString = "";
        try
        {
            if (Util.isNullOrEmpty(this.logoBlobId))
            {
                return logoString;
            }

            GridFSDBFile file = GridFsHelper.getBlobAsGridFSDBFile(this, BucketSetting.BUCKET_LOGO_BLOB_GRIDFS, this.logoBlobId);
            if (file != null)
            {
                byte[] bytes = IOUtils.toByteArray(file.getInputStream());
                logoString = Base64.encodeBase64String(bytes);
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
        return logoString;
    }

    public void setBucketLogo(String base64EncodedString)
    {
        if (Util.isNullOrEmpty(base64EncodedString))
        {
            this.logoBlobId = "";
            return;
        }

        try
        {
            byte[] logoBinary = Base64.decodeBase64(base64EncodedString);
            this.logoBlobId = GridFsHelper.setGridFsFile(this, BucketSetting.BUCKET_LOGO_BLOB_GRIDFS, logoBinary);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            this.logoBlobId = "";
        }
    }

    public File getBucketLogo()
    {
        try
        {
            if (logoBlobId == null || logoBlobId.isEmpty())
            {
                //check parent's logo
                MongoBucket targetBkt = MongoBucket.getById(bucketId);
                MongoBucket superadminBkt = MongoBucket.getByName(DefaultBucket.SUPERADMIN.getBucketName());
                if (targetBkt.getParentId() == null || targetBkt.getParentId().equals(superadminBkt.getBucketId()))
                {
                    return null;
                }

                BucketSetting parentSettings = BucketManager.getInstance().getBucketSetting(targetBkt.getParentId());
                return parentSettings.getBucketLogo();
            }

            File logoFile = GridFsHelper.getBlobAsFile(this, BucketSetting.BUCKET_LOGO_BLOB_GRIDFS, logoBlobId);
            return logoFile;

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }
}
