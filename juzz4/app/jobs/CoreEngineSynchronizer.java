package jobs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.kaisquare.core.thrift.CoreException;
import core.ConfigControlClient;
import core.CoreClient;
import lib.util.Util;
import models.MongoDevice;
import models.MongoDeviceModel;
import platform.Environment;
import play.Logger;
import play.jobs.Job;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CoreEngineSynchronizer extends Job
{
    private static final int FREQ_SECONDS = 10 * 60;

    @Override
    public void doJob()
    {
        try
        {
            syncDeviceModelsTableWithBackend();

            if (Environment.getInstance().onKaiNode())
            {
                checkAndUpdateCloudCoreAddress();
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
        finally
        {
            in(FREQ_SECONDS);
        }
    }

    public static void syncDeviceModelsTableWithBackend() throws CoreException
    {
        CoreClient coreClient = CoreClient.getInstance();
        List<com.kaisquare.core.thrift.DeviceModel> coreDeviceModels = coreClient.deviceManagementClient.listModels();
        if (coreDeviceModels == null)
        {
            Logger.error(Util.whichFn() + "returns NULL");
            return;
        }

        // remove models that are currently not in use
        Iterable<MongoDeviceModel> platformDeviceModels = MongoDeviceModel.q().fetch();
        Map<MongoDeviceModel, List<MongoDevice>> modelsWithDevices = new LinkedHashMap<>();
        for (MongoDeviceModel platformDeviceModel : platformDeviceModels)
        {
            try
            {
                List<MongoDevice> dependentDevices = MongoDevice.q()
                        .filter("modelId", platformDeviceModel.getModelId())
                        .fetchAll();
                if (dependentDevices.isEmpty())
                {
                    platformDeviceModel.delete();
                }
                else
                {
                    modelsWithDevices.put(platformDeviceModel, dependentDevices);
                }
            }
            catch (Exception e)
            {
                Logger.error(e, "Failed to delete: " + platformDeviceModel.getName());
            }
        }

        // add or update platform models
        List<String> coreDeviceModelIds = new ArrayList<>();
        for (com.kaisquare.core.thrift.DeviceModel coreDevicecModel : coreDeviceModels)
        {
            String modelId = coreDevicecModel.getId();
            coreDeviceModelIds.add(modelId);
            MongoDeviceModel platformDeviceModel = MongoDeviceModel.getByModelId(modelId);
            if (platformDeviceModel == null)
            {
                platformDeviceModel = new MongoDeviceModel();
            }

            platformDeviceModel.setModelId(modelId);
            platformDeviceModel.setChannels(Integer.parseInt(coreDevicecModel.getChannels()));
            platformDeviceModel.setName(coreDevicecModel.getName());
            platformDeviceModel.setCapabilities(retrieveCapabilities(coreDevicecModel.getMisc()));
            platformDeviceModel.save();
        }

        // log dangling device models due to active devices
        for (MongoDeviceModel deviceModel : modelsWithDevices.keySet())
        {
            if (!coreDeviceModelIds.contains(deviceModel.getModelId()))
            {
                Logger.error("Unlisted model (%s). Active devices: %s",
                             deviceModel.getName(),
                             modelsWithDevices.get(deviceModel));
            }
        }
    }

    private static String retrieveCapabilities(String modelMisc)
    {
        //parse json of capabilities
        JsonElement root = new JsonParser().parse(modelMisc);
        JsonArray supportedTasks = root.getAsJsonObject().get("supportedtasktypes").getAsJsonArray();
        String supportedTasksAsString = supportedTasks.toString();

        String convertedStr = "";

        if (supportedTasksAsString.contains("live-image"))
        { //deprecated
            convertedStr += MongoDeviceModel.CAP_VIDEO + " " + MongoDeviceModel.CAP_VIDEO_MJPEG;
        }
        if (supportedTasksAsString.contains("live-mjpeg") ||
            supportedTasksAsString.contains("live-rtsp") ||
            supportedTasksAsString.contains("live-rtmp"))
        {
            convertedStr += " " + MongoDeviceModel.CAP_VIDEO;
        }
        if (supportedTasksAsString.contains("live-mjpeg"))
        {
            convertedStr += " " + MongoDeviceModel.CAP_VIDEO_MJPEG;
        }
        if (supportedTasksAsString.contains("live-rtsp") ||
            supportedTasksAsString.contains("live-rtmp"))
        {
            convertedStr += " " + MongoDeviceModel.CAP_VIDEO_H264;
        }
        if (supportedTasksAsString.contains("live-gps"))
        {
            convertedStr += " " + MongoDeviceModel.CAP_GPS;
        }
        if (supportedTasksAsString.contains("live-gsensor"))
        {
            convertedStr += " " + MongoDeviceModel.CAP_GSENSOR;
        }
        if (supportedTasksAsString.contains("live-audio"))
        {
            convertedStr += " " + MongoDeviceModel.CAP_AUDIO;
        }
        if (supportedTasksAsString.contains("live-indoor-location"))
        {
            convertedStr += " " + MongoDeviceModel.CAP_INDOOR_LOCATION;
        }
        if (supportedTasksAsString.contains("gpio"))
        {
            convertedStr += " " + MongoDeviceModel.CAP_GPIO;
        }
        if (supportedTasksAsString.contains("node"))
        {
            convertedStr += " " + MongoDeviceModel.CAP_NODE;
        }

        return convertedStr;
    }

    private static void checkAndUpdateCloudCoreAddress()
    {
        ConfigControlClient configClient = ConfigControlClient.getInstance();
        if (configClient.isCloudCoreAddressUpdated())
        {
            return;
        }

        configClient.setCloudServerByPlatformCloud();
    }
}
