package platform.node;

import com.google.gson.Gson;
import com.kaisquare.sync.CommandType;
import com.kaisquare.transports.kaisync.KsVcaAppInfo;
import com.kaisquare.transports.kaisync.KsVcaInstance;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidEnvironmentException;
import models.MongoDevice;
import models.command.QueuedPlatformCommand;
import models.command.ThrottledCommand;
import models.command.node.PrioritizedNodeCommand;
import models.command.node.SequencedNodeCommand;
import models.node.NodeCamera;
import models.node.NodeInfo;
import models.node.NodeSettings;
import platform.Environment;
import platform.analytics.LocalVcaInstance;
import platform.analytics.VcaAppInfo;
import platform.analytics.VcaInfo;
import platform.analytics.VcaStatus;
import platform.devices.DeviceChannelPair;
import platform.devices.DeviceStatus;
import platform.kaisyncwrapper.KaiSyncHelper;
import platform.kaisyncwrapper.node.PrioritizedCommandQueue;
import platform.kaisyncwrapper.node.SequencedCommandQueue;
import play.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Aye Maung
 */
public class KaiSyncCommandClient
{
    private static Gson gson = new Gson();
    private static KaiSyncCommandClient instance = null;
    private static PrioritizedCommandQueue prioritizedQueue = PrioritizedCommandQueue.getInstance();
    private static SequencedCommandQueue sequencedQueue = SequencedCommandQueue.getInstance();
    private static NodeInfo nodeInfo = null;
    private static String updateReadyNotifiedVersion = "";
    private static long updateReadyNotifiedTime = 0L;

    //flags
    public static AtomicBoolean deviceSyncStatus = new AtomicBoolean(false);
    public static AtomicBoolean analyticsSyncStatus = new AtomicBoolean(false);

    private KaiSyncCommandClient() throws ApiException
    {
        NodeInfo nodeInfo = NodeManager.getInstance().getNodeInfo();
        if (nodeInfo == null)
        {
            throw new ApiException("KaiSyncCommandClient: Node is not registered");
        }

        this.nodeInfo = nodeInfo;
    }

    public static KaiSyncCommandClient getInstance() throws ApiException
    {
        if (!Environment.getInstance().onKaiNode())
        {
            throw new InvalidEnvironmentException();
        }

        if (instance == null)
        {
            instance = new KaiSyncCommandClient();
        }
        return instance;
    }

    public static boolean isStartupSyncDone()
    {
        return deviceSyncStatus.get() && analyticsSyncStatus.get();
    }

    public void nodeDeviceAdded(MongoDevice device)
    {
        NodeCamera nodeCamera = device.toNodeCamera();
        String jsonCamera = gson.toJson(nodeCamera);

        SequencedNodeCommand nodeCommand = new SequencedNodeCommand(
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_DEVICE_ADDED,
                Arrays.asList(jsonCamera)
        );

        sequencedQueue.queueCommand(nodeCommand);
    }

    public void nodeDeviceUpdated(MongoDevice device)
    {
        NodeCamera nodeCamera = device.toNodeCamera();
        String jsonCamera = gson.toJson(nodeCamera);

        SequencedNodeCommand nodeCommand = new SequencedNodeCommand(
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_DEVICE_UPDATED,
                Arrays.asList(jsonCamera)
        );

        sequencedQueue.queueCommand(nodeCommand);
    }

    public void nodeDeviceDeleted(MongoDevice device)
    {
        NodeCamera nodeCamera = device.toNodeCamera();
        String jsonCamera = gson.toJson(nodeCamera);
        SequencedNodeCommand nodeCommand = new SequencedNodeCommand(
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_DEVICE_DELETED,
                Arrays.asList(jsonCamera)
        );

        sequencedQueue.queueCommand(nodeCommand);
    }

    public void nodeSyncDeviceList(List<NodeCamera> nodeDeviceList)
    {
        String jsonDevices = gson.toJson(nodeDeviceList);
        PrioritizedNodeCommand nodeCommand = new PrioritizedNodeCommand(
                PrioritizedNodeCommand.PRIORITY_HIGH,
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_SYNC_DEVICE,
                Arrays.asList(jsonDevices)
        );

        prioritizedQueue.queueCommand(nodeCommand);
    }

    public void nodeSyncVcaList(List<LocalVcaInstance> nodeVcaList)
    {
        String jsonList = KaiSyncHelper.stringifyVcaListForSendingToCloud(nodeVcaList);
        PrioritizedNodeCommand nodeCommand = new PrioritizedNodeCommand(
                PrioritizedNodeCommand.PRIORITY_NORMAL,
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_SYNC_VCA,
                Arrays.asList(jsonList)
        );

        prioritizedQueue.queueCommand(nodeCommand);
    }

    public void nodeVcaAdded(VcaInfo newVcaInfo) throws ApiException
    {
        String jsonSchedule = gson.toJson(newVcaInfo.getRecurrenceRule());
        KsVcaInstance transportInst = new KsVcaInstance(
                newVcaInfo.getInstanceId(),
                newVcaInfo.getAppId(),
                newVcaInfo.getCamera().getCoreDeviceId(),
                newVcaInfo.getSettings(),
                jsonSchedule,
                newVcaInfo.isEnabled(),
                false,
                null);

        SequencedNodeCommand nodeCommand = new SequencedNodeCommand(
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_VCA_ADDED,
                Arrays.asList(gson.toJson(transportInst))
        );

        sequencedQueue.queueCommand(nodeCommand);
    }

    public void nodeVcaUpdated(LocalVcaInstance updatedInstance) throws ApiException
    {
        VcaInfo vcaInfo = updatedInstance.getVcaInfo();
        String jsonSchedule = gson.toJson(vcaInfo.getRecurrenceRule());
        KsVcaInstance transportInst = new KsVcaInstance(
                vcaInfo.getInstanceId(),
                vcaInfo.getAppId(),
                vcaInfo.getCamera().getCoreDeviceId(),
                vcaInfo.getSettings(),
                jsonSchedule,
                vcaInfo.isEnabled(),
                updatedInstance.migrationRequired(),
                updatedInstance.getStatus().name());

        SequencedNodeCommand nodeCommand = new SequencedNodeCommand(
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_VCA_UPDATED,
                Arrays.asList(gson.toJson(transportInst))
        );

        sequencedQueue.queueCommand(nodeCommand);
    }

    public void nodeVcaRemoved(String vcaInstanceId) throws ApiException
    {
        SequencedNodeCommand nodeCommand = new SequencedNodeCommand(
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_VCA_REMOVED,
                Arrays.asList(vcaInstanceId)
        );

        sequencedQueue.queueCommand(nodeCommand);
    }

    public void nodeVcaActivated(String vcaInstanceId) throws ApiException
    {
        SequencedNodeCommand nodeCommand = new SequencedNodeCommand(
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_VCA_ACTIVATED,
                Arrays.asList(vcaInstanceId)
        );

        sequencedQueue.queueCommand(nodeCommand);
    }

    public void nodeVcaDeactivated(String vcaInstanceId) throws ApiException
    {
        SequencedNodeCommand nodeCommand = new SequencedNodeCommand(
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_VCA_DEACTIVATED,
                Arrays.asList(vcaInstanceId)
        );
        Logger.info("[nodeVcaDeactivated] command:"+nodeCommand, "");
        sequencedQueue.queueCommand(nodeCommand);
    }

    public void nodeSettingsChanged(NodeSettings nodeSettings) throws ApiException
    {
        String jsonSettings = gson.toJson(nodeSettings);
        PrioritizedNodeCommand nodeCommand = new PrioritizedNodeCommand(
                PrioritizedNodeCommand.PRIORITY_NORMAL,
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_SETTINGS_CHANGED,
                Arrays.asList(jsonSettings)
        );

        prioritizedQueue.queueCommand(nodeCommand);
    }

    public void nodeUpdateInfo(String nodeVersion, List<VcaAppInfo> appList)
    {
        //convert app list to transport objects
        List<KsVcaAppInfo> appTransportList = new ArrayList<>();
        for (VcaAppInfo appInfo : appList)
        {
            appTransportList.add(new KsVcaAppInfo(appInfo.appId,
                                                  appInfo.program.name(),
                                                  appInfo.version,
                                                  appInfo.displayName,
                                                  appInfo.description));
        }

        Map<String, Object> infoMap = new LinkedHashMap<>();
        infoMap.put("version", nodeVersion);
        infoMap.put("vcaAppList", new Gson().toJson(appTransportList));

        String jsonMap = gson.toJson(infoMap);
        PrioritizedNodeCommand nodeCommand = new PrioritizedNodeCommand(
                PrioritizedNodeCommand.PRIORITY_ASAP,
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_UPDATE_INFO,
                Arrays.asList(jsonMap)
        );

        prioritizedQueue.queueCommand(nodeCommand);
    }

    public void nodeRemoteShellStarted()
    {
        SequencedNodeCommand nodeCommand = new SequencedNodeCommand(
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_REMOTE_SHELL_STARTED
        );

        sequencedQueue.queueCommand(nodeCommand);
    }

    public void nodeRemoteShellStopped()
    {
        SequencedNodeCommand nodeCommand = new SequencedNodeCommand(
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_REMOTE_SHELL_STOPPED
        );

        sequencedQueue.queueCommand(nodeCommand);
    }

    public void nodeUpdateFileReady(String newVersion) throws ApiException
    {
        //ensure node doesn't continuous send command when user hasn't yet click 'update' button.
        //it will re-send after 30mins if the node is still not updated yet
        long halfHourAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30);
        if (updateReadyNotifiedVersion.equalsIgnoreCase(newVersion) &&
            updateReadyNotifiedTime > halfHourAgo)
        {
            return;
        }

        PrioritizedNodeCommand nodeCommand = new PrioritizedNodeCommand(
                PrioritizedNodeCommand.PRIORITY_HIGH,
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_UPDATE_FILE_READY,
                Arrays.asList(newVersion)
        );
        prioritizedQueue.queueCommand(nodeCommand);
        Logger.info("Update ready command  sent (%s)", newVersion);

        //store last sent info
        updateReadyNotifiedVersion = newVersion;
        updateReadyNotifiedTime = System.currentTimeMillis();
    }

    public void nodeVcaStateChanged(String vcaInstanceId, VcaStatus status) throws ApiException
    {
        SequencedNodeCommand nodeCommand = new SequencedNodeCommand(
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_VCA_STATE_CHANGED,
                Arrays.asList(vcaInstanceId, status.name())
        );

        //to prevent sending out too frequently
        ThrottledCommand.queue(nodeCommand, 2, new Comparator<QueuedPlatformCommand>()
        {
            @Override
            public int compare(QueuedPlatformCommand o1, QueuedPlatformCommand o2)
            {
                if (o1.getCommandType() == o2.getCommandType() &&
                    o1.getParams().get(0).equals(o2.getParams().get(0))) //instanceId
                {
                    return 0;
                }
                return -1; // non-zero value
            }
        });
    }

    public void nodeCameraStatusChanged(DeviceChannelPair camera, DeviceStatus newStatus)
    {
        if (newStatus.equals(DeviceStatus.UNKNOWN))
        {
            return;
        }

        SequencedNodeCommand nodeCommand = new SequencedNodeCommand(
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_CAMERA_STATUS_CHANGED,
                Arrays.asList(
                        camera.getCoreDeviceId(),
                        newStatus.name()
                )
        );

        //to prevent sending out too frequently
        ThrottledCommand.queue(nodeCommand, 15, new Comparator<QueuedPlatformCommand>()
        {
            @Override
            public int compare(QueuedPlatformCommand o1, QueuedPlatformCommand o2)
            {
                if (o1.getCommandType() == o2.getCommandType() &&
                    o1.getParams().get(0).equals(o2.getParams().get(0))) //coreDeviceId
                {
                    return 0;
                }
                return -1; // non-zero value
            }
        });
    }

    public void nodeStorageExpanded()
    {
        PrioritizedNodeCommand nodeCommand = new PrioritizedNodeCommand(
                PrioritizedNodeCommand.PRIORITY_ASAP,
                nodeInfo.getCloudPlatformDeviceId(),
                CommandType.NODE_STORAGE_EXPANDED
        );

        prioritizedQueue.queueCommand(nodeCommand);
    }
}
