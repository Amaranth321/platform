package controllers.api.node;

import controllers.api.APIController;
import jobs.node.NetworkCheck;
import lib.util.ResultMap;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidEnvironmentException;
import models.MongoDevice;
import models.licensing.NodeLicense;
import platform.Environment;
import platform.coreengine.RecordingManager;
import platform.devices.DeviceChannelPair;
import platform.node.*;
import play.Logger;
import play.jobs.Job;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Status extends APIController
{

    public static void getNetworkStatus() throws ApiException
    {

        ResultMap map = new ResultMap();
        map.put("result", "ok");

        int status = NetworkCheck.getNetworkStatus();
        map.put("status_code", status);
        if ((status & NetworkCheck.STATUS_CLOUD_SERVER_CONNECTED) != 0)
        {
            map.put("status", "network-connected");
        }
        else if ((status & NetworkCheck.STATUS_ETHERNET_CONNECTED) != 0)
        {
            map.put("status", "network-ethernet-connected");
        }
        else if ((status & NetworkCheck.STATUS_WIRELESS_CONNECTED) != 0)
        {
            map.put("status", "network-wireless-connected");
        }
        else if ((status & NetworkCheck.STATUS_INTERNET_CONNECTED) != 0)
        {
            map.put("status", "network-internet-connected");
        }
        else
        {
            map.put("status", "network-disconnected");
        }

        response.setHeader("Access-Control-Allow-Origin", "*");
        renderJSON(map);
    }

    public static void getNodeInfo() throws ApiException
    {
        NodeManager nodeMgr = NodeManager.getInstance();
        ResultMap map = new ResultMap();
        map.put("result", "ok");
        if (nodeMgr.isRegisteredOnCloud())
        {
            NodeLicense license = nodeMgr.getLicense();

            //for compatibility, don't modify current fields. Add new ones if necessary
            map.put("registered", "true");
            map.put("node", nodeMgr.getNodeInfo());
            map.put("bucketName", nodeMgr.getBucket().getName());
            map.put("license", license.licenseNumber);
            map.put("maxVcaCount", license.maxVcaCount);
            map.put("expiry", license.getExpiryDate());
            map.put("maxDeviceCount", nodeMgr.getCameraLimit());
            map.put("version", nodeMgr.getVersion());
            map.put("updateFile", KaiNodeAdminService.getInstance().getSoftwareUpdate());
            map.put("cloudServer", CloudConnector.getInstance().getServerHost());
        }
        else
        {
            map.put("registered", "false");
        }

        response.setHeader("Access-Control-Allow-Origin", "*");
        renderJSON(map);
    }

    public static void getNodeVersion() throws ApiException
    {
        String version = NodeManager.getInstance().getVersion();
        version = Util.isNullOrEmpty(version) ? "Unknown" : version;

        ResultMap map = new ResultMap();
        map.put("result", "ok");
        map.put("version", version);
        renderJSON(map);
    }

    public static void reset() throws ApiException
    {
        NodeManager nodeMgr = NodeManager.getInstance();
        Map responseMap = new LinkedHashMap();
        try
        {
            if (nodeMgr.isRegisteredOnCloud())
            {
                if (!CloudConnector.getInstance().isCloudReachable(10))
                {
                    throw new ApiException("This node is offline, please make sure internet connection is available.");
                }

                // notify cloud directly about reset first.
                // if API call is successful,
                // cloud will delete the device and send delete command back to node, which will do factory reset
                // if  API call has failed,
                // just proceed with factory reset locally.
                boolean notifyResult = nodeMgr.notifyFactoryResetToCloud();
                if (!notifyResult)
                {
                    new Job()
                    {
                        @Override
                        public void doJob()
                        {
                            NodeProvisioning.getInstance().factoryResetNode();
                        }
                    }.in(5);
                }
            }
            else
            {
                new Job()
                {
                    @Override
                    public void doJob()
                    {
                        NodeProvisioning.getInstance().factoryResetNode();
                    }
                }.in(5);
            }

            responseMap.put("result", "ok");
        }
        catch (Exception e)
        {
            respondError(e);
        }

        response.setHeader("Access-Control-Allow-Origin", "*");
        renderJSON(responseMap);
    }

    public static void storageExpansionStarted()
    {
        try
        {
            if (!Environment.getInstance().onKaiNode())
            {
                throw new InvalidEnvironmentException();
            }

            if (!NodeManager.getInstance().isRegisteredOnCloud())
            {
                respondOK();
            }

            Util.printImptLog("Storage expansion started");

            //inform node core
            List<MongoDevice> allCameras = MongoDevice.q().fetchAll();
            for (MongoDevice camera : allCameras)
            {
                RecordingManager.getInstance().resetAllRecordings(new DeviceChannelPair(camera.getCoreDeviceId(), "0"));
            }

            //inform cloud
            KaiSyncCommandClient cmdClient = CloudConnector.getInstance().getKaiSyncCommandClient();
            cmdClient.nodeStorageExpanded();

            respondOK();
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            respondError(e);
        }
    }

}
