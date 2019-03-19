package controllers.api.node;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaisquare.util.SysInfoUtil;
import controllers.api.APIController;
import controllers.interceptors.LoginInterceptor;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.node.CloudSession;
import platform.node.*;
import platform.pubsub.PlatformEventMonitor;
import platform.pubsub.PlatformEventType;
import platform.register.NodeSetupInfo;
import play.Logger;
import play.mvc.Before;
import play.mvc.With;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Aye Maung
 */
@With(LoginInterceptor.class)
public class Registration extends APIController
{
    private static HttpApiClient httpApiClient = null;
    private static CloudSession cloudSession = null;

    @Before(priority = 2)
    private static void check()
    {
        Map<String, Object> responseMap = new LinkedHashMap<>();
        CloudConnector cloudConnector = CloudConnector.getInstance();
        try
        {
            //check if cloud is online
            if (!cloudConnector.isCloudReachable(10))
            {
                throw new ApiException("cloud-server-offline");
            }

            //login wit OTP
            String otp = readApiParameter("otp", true);
            httpApiClient = cloudConnector.getHttpApiClient();
            cloudSession = httpApiClient.otpLogin(otp);
            if (cloudSession == null)
            {
                throw new ApiException("authentication-failed");
            }
        }
        catch (ApiException apiEx)
        {
            Logger.error(apiEx, "");
            responseMap.put("result", "error");
            responseMap.put("reason", apiEx.getMessage());
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            responseMap.put("result", "error");
            responseMap.put("reason", "unknown");
            renderJSON(responseMap);
        }
    }

    public static void checklicensestatus() throws ApiException
    {
        Map<String, Object> responseMap = new LinkedHashMap<>();
        try
        {
            String licenseNumber = readApiParameter("license-number", true);

            //send api
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("license-number", licenseNumber);
            JsonObject responseData = httpApiClient.postAPICall(cloudSession, NodeToCloudAPI.CHECK_LICENSE, params);

            responseMap.put("result", "ok");
            responseMap.put("status", responseData.get("status").getAsString());

        }
        catch (ApiException apiEx)
        {
            Logger.error(lib.util.Util.getStackTraceString(apiEx));
            responseMap.put("result", "error");
            responseMap.put("reason", apiEx.getMessage());
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            responseMap.put("result", "error");
            responseMap.put("reason", "unknown");
        }
        finally
        {
            renderJSON(responseMap);
        }
    }

    public static void register() throws ApiException
    {
        Map<String, Object> responseMap = new LinkedHashMap<>();
        NodeManager nodeMgr = NodeManager.getInstance();
        try
        {
            String licenseNumber = readApiParameter("license-number", true);
            String registrationNumber = readApiParameter("registration-number", true);
            String deviceName = readApiParameter("device-name", true);
            String address = readApiParameter("device-address", true);
            String latitude = readApiParameter("device-latitude", true);
            String longitude = readApiParameter("device-longitude", true);

            if (nodeMgr.isRegisteredOnCloud())
            {
                throw new ApiException("node-already-registered");
            }

            //Registration api params
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("license-number", licenseNumber);
            params.put("registration-number", registrationNumber);
            params.put("device-name", deviceName);
            params.put("device-address", address);
            params.put("device-latitude", latitude);
            params.put("device-longitude", longitude);

            //Add system information
            params.put("mac-address", SysInfoUtil.getMacAddress(false));
            params.put("version", nodeMgr.getVersion());

            //send api
            JsonObject responseData = httpApiClient.postAPICall(cloudSession, NodeToCloudAPI.REGISTER_NODE, params);
            String infoString = responseData.get("setup-info").toString();
            NodeSetupInfo setupInfo = new Gson().fromJson(infoString, NodeSetupInfo.class);

            Logger.info("Setting up a new node");
            nodeMgr.setupNewNode(setupInfo);

            Logger.info("Start allowing commands");
            KaiSyncCommandClient.deviceSyncStatus.getAndSet(true);
            KaiSyncCommandClient.analyticsSyncStatus.getAndSet(true);
            PlatformEventMonitor.getInstance().broadcast(PlatformEventType.STARTUP_SYNC_COMPLETED);

            responseMap.put("result", "ok");
        }
        catch (ApiException apiEx)
        {
            Logger.error(apiEx, "");
            responseMap.put("result", "error");
            responseMap.put("reason", apiEx.getMessage());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            responseMap.put("result", "error");
            responseMap.put("reason", "unknown");
        }
        finally
        {
            renderJSON(responseMap);
        }
    }

    public static void replace() throws ApiException
    {
        Map<String, Object> responseMap = new LinkedHashMap<>();
        NodeManager nodeMgr = NodeManager.getInstance();
        try
        {
            String licenseNumber = readApiParameter("license-number", true);
            String registrationNumber = readApiParameter("registration-number", true);

            if (nodeMgr.isRegisteredOnCloud())
            {
                throw new ApiException("node-already-registered");
            }

            //Replacement api params
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("license-number", licenseNumber);
            params.put("registration-number", registrationNumber);

            //Add system information
            params.put("mac-address", SysInfoUtil.getMacAddress(false));
            params.put("version", nodeMgr.getVersion());

            //start replacement
            JsonObject responseData = httpApiClient.postAPICall(cloudSession, NodeToCloudAPI.REPLACE_NODE, params);
            String infoString = responseData.get("setup-info").toString();
            NodeSetupInfo setupInfo = new Gson().fromJson(infoString, NodeSetupInfo.class);

            Logger.info("Setting up a new node");
            nodeMgr.setupNewNode(setupInfo);

            Logger.info("Restoring data from replaced node");
            RestorableNodeData restorableData = new Gson().fromJson(responseData.get("restorable-data").toString(),
                                                                    RestorableNodeData.class);
            NodeProvisioning.getInstance().restoreData(restorableData);

            Logger.info("Start allowing commands");
            KaiSyncCommandClient.deviceSyncStatus.getAndSet(true);
            KaiSyncCommandClient.analyticsSyncStatus.getAndSet(true);
            PlatformEventMonitor.getInstance().broadcast(PlatformEventType.STARTUP_SYNC_COMPLETED);

            responseMap.put("result", "ok");
        }
        catch (ApiException apiEx)
        {
            Logger.error(apiEx, "");
            responseMap.put("result", "error");
            responseMap.put("reason", apiEx.getMessage());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            responseMap.put("result", "error");
            responseMap.put("reason", "unknown");
        }
        finally
        {
            renderJSON(responseMap);
        }
    }

}
