package controllers.api;

import controllers.interceptors.NodeLicenseOwner;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.licensing.LicenseStatus;
import models.licensing.NodeLicenseInfo;
import platform.CloudLicenseManager;
import platform.DeviceManager;
import platform.access.UserSessionManager;
import platform.db.cache.proxies.CachedLoginSession;
import platform.node.RestorableNodeData;
import platform.register.CloudRegisterManager;
import platform.register.NodeSetupInfo;
import platform.register.NodeSetupRequest;
import play.Logger;
import play.mvc.With;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * These APIs are to be called by node platform only.
 *
 * @author Aye Maung
 * @see platform.node.HttpApiClient
 * @since v4.0
 */
@With(NodeLicenseOwner.class)
public class NodeToCloud extends APIController
{
    public static void registernode()
    {
        Logger.info("----");
        try
        {
            CachedLoginSession cachedSession = verifySession();
            String bucketId = cachedSession.getBucketId();
            String userId = cachedSession.getUserId();

            String licenseNumber = readApiParameter("license-number", true);
            String registrationNumber = readApiParameter("registration-number", true);
            String deviceName = readApiParameter("device-name", true);
            String address = readApiParameter("device-address", true);
            String latitude = readApiParameter("device-latitude", true);
            String longitude = readApiParameter("device-longitude", true);
            String macAddress = readApiParameter("mac-address", true);
            String version = readApiParameter("version", false);

            //sanitize license number
            licenseNumber = Util.removeNonAlphanumeric(licenseNumber);

            NodeSetupRequest setupRequest = NodeSetupRequest.createRegisterRequest(
                    Long.parseLong(bucketId),
                    Long.parseLong(userId),
                    licenseNumber,
                    registrationNumber,
                    deviceName,
                    address,
                    latitude,
                    longitude,
                    macAddress,
                    version
            );

            NodeSetupInfo setupInfo = CloudRegisterManager.getInstance().registerNode(setupRequest);

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("setup-info", setupInfo);
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }

        Logger.info("----");
    }

    public static void replacenode()
    {
        Logger.info("----");
        try
        {
            CachedLoginSession cachedSession = verifySession();
            String bucketId = cachedSession.getBucketId();
            String userId = cachedSession.getUserId();

            String licenseNumber = readApiParameter("license-number", true);
            String registrationNumber = readApiParameter("registration-number", true);
            String macAddress = readApiParameter("mac-address", true);
            String version = readApiParameter("version", false);

            //sanitize license number
            licenseNumber = Util.removeNonAlphanumeric(licenseNumber);

            NodeSetupRequest setupRequest = NodeSetupRequest.createReplaceRequest(
                    Long.parseLong(bucketId),
                    Long.parseLong(userId),
                    licenseNumber,
                    registrationNumber,
                    macAddress,
                    version
            );

            //compile restorable data first
            Logger.info("Compiling restorable data from old node.");
            NodeLicenseInfo licenseInfo = CloudLicenseManager.getInstance()
                    .getNodeLicenseInfo(setupRequest.getLicenseNumber());
            RestorableNodeData restorableData = RestorableNodeData.of(String.valueOf(licenseInfo.nodeCloudPlatormId));

            //start
            NodeSetupInfo setupInfo = CloudRegisterManager.getInstance().replaceNode(setupRequest);

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("setup-info", setupInfo);
            responseMap.put("restorable-data", restorableData);
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }

        Logger.info("----");
    }

    public static void notifynodereset()
    {
        try
        {
            String licenseNumber = Util.removeNonAlphanumeric(readApiParameter("license-number", true));
            String registrationNumber = readApiParameter("registration-number", true);

            //check
            NodeLicenseInfo licenseInfo = CloudLicenseManager.getInstance().getNodeLicenseInfo(licenseNumber);
            if (licenseInfo.status.equals(LicenseStatus.UNUSED))
            {
                throw new ApiException("unactivated-license");
            }
            if (!licenseInfo.registrationNumber.equals(registrationNumber))
            {
                throw new ApiException("license-registration-not-match");
            }

            //get bucket
            String bucketId = licenseInfo.cloudBucketId.toString();
            renderArgs.put("caller-bucket-id", bucketId);

            DeviceManager.getInstance().removeDeviceFromBucket(bucketId, licenseInfo.nodeCloudPlatormId.toString());
            Map response = new LinkedHashMap();
            response.put("result", "ok");
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * Calls this to retrieve session details.
     * <p/>
     * Note: session-key is not a requirement for some API here. Then, you can't use this.
     */
    private static CachedLoginSession verifySession() throws ApiException
    {
        String sessionKey = readApiParameter("session-key", true);
        if (!UserSessionManager.getInstance().isSessionValid(sessionKey))
        {
            throw new ApiException("session-expired");
        }

        return UserSessionManager.getInstance().findSession(sessionKey);
    }
}
