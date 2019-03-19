package platform.register;

import lib.util.Util;
import platform.nodesoftware.SoftwareManager;
import play.Logger;

/**
 * @author Aye Maung
 */
public class NodeSetupRequest
{
    //verification
    private long bucketId;
    private long userId;
    private String licenseNumber;
    private String registrationNumber;
    private String macAddress;
    private String version;

    //info
    private String deviceName;
    private String address;
    private String latitude;
    private String longitude;

    private NodeSetupRequest()
    {
        //use static constructors
    }

    public static NodeSetupRequest createRegisterRequest(long bucketId,
                                                         long userId,
                                                         String licenseNumber,
                                                         String registrationNumber,
                                                         String deviceName,
                                                         String address,
                                                         String latitude,
                                                         String longitude,
                                                         String macAddress,
                                                         String version)
    {
        Logger.info("Node registration request");
        Logger.info("Bucket ID          :" + bucketId);
        Logger.info("licenseNumber      :" + licenseNumber);
        Logger.info("registrationNumber :" + registrationNumber);
        Logger.info("MAC Address        :" + macAddress);
        Logger.info("version            :" + version);
        Logger.info("deviceName         :" + deviceName);
        Logger.info("address            :" + address);
        Logger.info("latitude           :" + latitude);
        Logger.info("longitude          :" + longitude);

        NodeSetupRequest req = new NodeSetupRequest();
        req.bucketId = bucketId;
        req.userId = userId;
        req.licenseNumber = licenseNumber;
        req.registrationNumber = registrationNumber;
        req.macAddress = macAddress;
        req.version = Util.isNullOrEmpty(version) ? SoftwareManager.UNSET_NODE_VERSION : version;
        req.deviceName = deviceName;
        req.address = address;
        req.latitude = latitude;
        req.longitude = longitude;

        return req;
    }

    public static NodeSetupRequest createReplaceRequest(long bucketId,
                                                        long userId,
                                                        String licenseNumber,
                                                        String registrationNumber,
                                                        String macAddress,
                                                        String version)
    {
        Logger.info("Node replacement request");
        Logger.info("Bucket ID          : " + bucketId);
        Logger.info("licenseNumber      : " + licenseNumber);
        Logger.info("registrationNumber : " + registrationNumber);
        Logger.info("MAC Address        : " + macAddress);
        Logger.info("Version            : " + version);

        NodeSetupRequest req = new NodeSetupRequest();
        req.bucketId = bucketId;
        req.userId = userId;
        req.licenseNumber = licenseNumber;
        req.registrationNumber = registrationNumber;
        req.macAddress = macAddress;
        req.version = Util.isNullOrEmpty(version) ? SoftwareManager.UNSET_NODE_VERSION : version;
        return req;
    }

    public Long getBucketId()
    {
        return bucketId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public String getLicenseNumber()
    {
        return licenseNumber;
    }

    public String getRegistrationNumber()
    {
        return registrationNumber;
    }

    public String getMacAddress()
    {
        return macAddress;
    }

    public String getVersion()
    {
        return version;
    }

    public String getDeviceName()
    {
        return deviceName;
    }

    public String getAddress()
    {
        return address;
    }

    public String getLatitude()
    {
        return latitude;
    }

    public String getLongitude()
    {
        return longitude;
    }

    public double getReleaseNumber()
    {
        return SoftwareManager.getInstance().getReleaseNumber(version);
    }
}
