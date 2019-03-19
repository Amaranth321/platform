package models.licensing;

import java.util.List;

/**
 * For information on UI.
 * Refer to {@link platform.CloudLicenseManager#getNodeLicenseInfo(String)}
 *
 * @author Aye Maung
 */
public class NodeLicenseInfo {
    public Long cloudBucketId;
    public Long nodeCloudPlatormId;
    public String bucketName;
    public String licenseNumber;
    public String registrationNumber;
    public String deviceName;
    public int cloudStorageGb;
    public int durationMonths;
    public int maxCameraLimit;
    public int maxVcaCount;
    public List<String> featureNameList;
    public LicenseStatus status;
    public long created;
    public long activated;
    public long expiryDate;


    public String getFormattedLicenseNumber() {
        int blockSize = 5;
        String formattedString = this.licenseNumber.substring(0, blockSize);
        for (int i = blockSize; i < this.licenseNumber.length(); i += blockSize) {
            int endIndex = i + blockSize;
            if (endIndex > this.licenseNumber.length()) {
                endIndex = this.licenseNumber.length();
            }
            formattedString += "-" + this.licenseNumber.substring(i, endIndex);
        }

        return formattedString;
    }


    public String toString() {
        return this.licenseNumber;
    }
}

