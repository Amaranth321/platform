package models.licensing;

import com.google.code.morphia.annotations.Entity;
import lib.util.exceptions.ApiException;
import models.access.AccessKey;
import platform.CloudLicenseManager;
import platform.db.gridfs.GridFsDetails;
import play.Logger;
import play.modules.morphia.Model;

/**
 * @author Aye Maung
 * @since v4.5
 */
@Entity
public class LicenseQRCode extends Model
{
    private final Long bucketId;
    private final Long userId;
    private final String accessKey;
    private final String licenseNumber;
    private final String registrationNumber;
    private final GridFsDetails pdfFile;

    public long getBucketId()
    {
        return bucketId;
    }

    public long getUserId()
    {
        return userId;
    }

    public String getAccessKey()
    {
        return accessKey;
    }

    public String getLicenseNumber()
    {
        return licenseNumber;
    }

    public String getRegistrationNumber()
    {
        return registrationNumber;
    }

    public GridFsDetails getPdfFile()
    {
        return pdfFile;
    }

    public boolean hasExpired()
    {
        AccessKey dbKey = AccessKey.find("key", accessKey).first();
        return dbKey.ok();
    }

    public boolean isAlreadyUsed()
    {
        try
        {
            NodeLicense dbLicense = CloudLicenseManager.getInstance().getDbNodeLicense(licenseNumber);
            return dbLicense.isAssignedToNode();
        }
        catch (ApiException e)
        {
            Logger.error(e, "");
            return true;
        }
    }

    public LicenseQRCode(long bucketId,
                         long userId,
                         String accessKey,
                         String licenseNumber,
                         String registrationNumber,
                         GridFsDetails pdfFile)
    {
        this.bucketId = bucketId;
        this.userId = userId;
        this.accessKey = accessKey;
        this.licenseNumber = licenseNumber;
        this.registrationNumber = registrationNumber;
        this.pdfFile = pdfFile;
    }

}
