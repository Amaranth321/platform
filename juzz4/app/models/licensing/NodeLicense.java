package models.licensing;

import com.google.code.morphia.annotations.Entity;
import org.apache.commons.lang.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.Environment;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;

@Entity
public class NodeLicense extends Model {

    public Long cloudBucketId;
    public Long nodeCloudPlatormId;     //do not fix this typo
    public String licenseNumber;
    public String registrationNumber;
    public LicenseStatus status;
    public List<String> featureNameList;
    public int maxCameraLimit;
    public int maxVcaCount;
    public int cloudStorageGb;
    public int durationMonths;
    public Long created;
    public Long activated;

    public NodeLicense() {
        created = Environment.getInstance().getCurrentUTCTimeMillis();
        licenseNumber = RandomStringUtils.randomAlphanumeric(15).toUpperCase();
        status = LicenseStatus.UNUSED;
        featureNameList = new ArrayList<>();
    }

    public NodeLicense(Long cloudBucketId,
                       int durationMonths,
                       int cloudStorageGb,
                       int maxCameraLimit,
                       int maxVcaCount,
                       List<String> featureNameList) {
        this();
        this.cloudBucketId = cloudBucketId;
        this.durationMonths = durationMonths;
        this.cloudStorageGb = cloudStorageGb;
        this.maxCameraLimit = maxCameraLimit;
        this.maxVcaCount = maxVcaCount;
        this.featureNameList = featureNameList;
    }

    public static NodeLicense getDefault() {
        NodeLicense defaultLicense = new NodeLicense();
        defaultLicense.durationMonths = 12;
        defaultLicense.cloudStorageGb = 1;
        defaultLicense.maxVcaCount = 5;

        return defaultLicense;
    }

    public boolean hasExpired() {
        if (activated == null) {
            return false;
        }
        if (durationMonths == -1)
        	return false;

        long utcMillis = Environment.getInstance().getCurrentUTCTimeMillis();
        long expiry = getExpiryDate();
        return (utcMillis > expiry);
    }

    public long getExpiryDate() {
        DateTime start = new DateTime(activated, DateTimeZone.UTC);
        DateTime end = start.plusMonths(durationMonths);
        return end.getMillis();
    }

    public boolean isAssignedToNode() {
        return (nodeCloudPlatormId != null);
    }
}
