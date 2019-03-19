package platform.kaisyncwrapper;

import com.google.gson.Gson;
import com.kaisquare.kaisync.ISyncFile;
import com.kaisquare.kaisync.ISyncReadFile;
import com.kaisquare.kaisync.ISyncWriteFile;
import com.kaisquare.kaisync.file.FileOptions;
import com.kaisquare.kaisync.file.IFileClient;
import com.kaisquare.kaisync.platform.IPlatformSync;
import com.kaisquare.sync.NodeCommandState;
import com.kaisquare.transports.kaisync.KsVcaInstance;
import com.kaisquare.transports.kaisync.KsVcaOld;
import lib.util.Util;
import lib.util.exceptions.InvalidEnvironmentException;
import models.NodeCommand;
import models.node.NodeObject;
import platform.CloudSyncManager;
import platform.DeviceManager;
import platform.Environment;
import platform.analytics.*;
import platform.analytics.app.AppVcaTypeMapper;
import platform.common.ACResource;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.db.cache.proxies.CachedNodeObjectInfo;
import platform.db.gridfs.GridFsDetails;
import platform.db.gridfs.GridFsHelper;
import platform.devices.DeviceChannelPair;
import platform.events.EventInfo;
import platform.node.CloudConnector;
import platform.time.RecurrenceRule;
import play.Logger;
import play.modules.morphia.Model;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Author:  Aye Maung
 */
public final class KaiSyncHelper
{
    private KaiSyncHelper()
    {
    }

    public static Model.MorphiaQuery queryAllCommandsInProgress()
    {
        return NodeCommand.q()
                .filter("state nin", Arrays.asList(
                        NodeCommandState.Success, NodeCommandState.Cancel, NodeCommandState.Failed
                ));
    }

    public static Model.MorphiaQuery queryCommandsInProgress(String nodeId)
    {
        return queryAllCommandsInProgress().filter("nodeId", nodeId);
    }

    public static void cleanupOrphanedCommands()
    {
        try
        {
            List<NodeObject> nodeObjects = DeviceManager.getInstance().getAllNodeObjects();
            List<String> nodeIdList = new ArrayList<>();
            for (NodeObject nodeObject : nodeObjects)
            {
                nodeIdList.add(nodeObject.getNodeId());
            }

            //Clean orphaned commands
            NodeCommand.q().filter("nodeId nin", nodeIdList.toArray()).delete();

        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    public static void clearAllCommands()
    {
        NodeCommand.q().delete();
    }

    /**
     * @param nodeId       this is node's core device id on cloud, NOT the nodeId from NodeCommand
     * @param ownerEvent
     * @param videoDetails
     */
    public static boolean transferEventVideoToCloud(String nodeId,
                                                    EventInfo ownerEvent,
                                                    GridFsDetails videoDetails)
    {
        try
        {
            //init KaiSync client
            String[] fileServerKeyStore = CloudSyncManager.getInstance().getFileServerKeystore();
            IPlatformSync client = CloudSyncManager.getInstance().getPlatformSync();
            ISyncWriteFile remoteFile = client.syncEventVideoFile(
                    ownerEvent.getEventId(),
                    nodeId,
                    videoDetails.getFilename());

            if (remoteFile == null || Util.isNullOrEmpty(remoteFile.getID()))
            {
                if (CloudConnector.getInstance().isCloudReachable(10))
                {
                    Logger.error("Failed to connect to remote file (%s)", videoDetails.getFilename());
                }
                return false;
            }

            remoteFile.setWriteTimeout(60);
            remoteFile.setKeystore(fileServerKeyStore[0], fileServerKeyStore[1]);
            remoteFile.setMetadata("category", videoDetails.getGroup().name());
            remoteFile.setMetadata("contentType", videoDetails.getFormat().getContentType());
            remoteFile.setMetadata("blobId", videoDetails.getBlobId());
            remoteFile.setMetadata("nodeId", nodeId);

            //start remote write
            try (
                    ACResource<OutputStream> acOut = new ACResource<>(remoteFile.getOutputStream());
                    ACResource<InputStream> acIn = new ACResource<>(GridFsHelper.getFileInputStream(videoDetails))
            )
            {
                OutputStream out = acOut.get();
                InputStream in = acIn.get();

                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) > 0)
                {
                    out.write(buf, 0, read);
                }
                out.flush();
            }

            //close KaiSync client
            if (client != null)
            {
                client.close();
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }

        return true;
    }

    public static boolean uploadedFileExists(String filename)
    {
        try
        {
            IFileClient client = CloudSyncManager.getInstance().newFileClient();
            ISyncFile remoteFile = client.openFile(filename, FileOptions.READ);
            return (remoteFile != null && !Util.isNullOrEmpty(remoteFile.getID()));
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    public static ACResource<InputStream> retrieveUploadedFile(String filename)
    {
        try
        {
            IFileClient client = CloudSyncManager.getInstance().newFileClient();
            ISyncReadFile remoteFile = (ISyncReadFile) client.openFile(filename, FileOptions.READ);
            remoteFile.setReadTimeout(60);
            return new ACResource<>(remoteFile.getInputStream());
        }
        catch (Exception e)
        {
            Logger.error(Util.getCallerFn() + e.getMessage());
            return null;
        }
    }

    public static boolean deleteUploadedFile(String filename)
    {
        try
        {
            if (!uploadedFileExists(filename))
            {
                return true;
            }

            IFileClient client = CloudSyncManager.getInstance().newFileClient();
            return client.deleteFile(filename);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    public static VcaInfo parseVcaInfoReceivedOnCloud(NodeObject nodeObject, String jsonInstance)
    {
        // for cloud side only
        if (!Environment.getInstance().onCloud())
        {
            throw new InvalidEnvironmentException();
        }

        Gson gson = new Gson();
        if (nodeObject.getReleaseNumber() >= 4.5)
        {
            KsVcaInstance transport = gson.fromJson(jsonInstance, KsVcaInstance.class);
            DeviceChannelPair camera = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(),
                                                             transport.cameraCoreDeviceId);
            RecurrenceRule rule = Util.isNullOrEmpty(transport.schedule) ?
                                  null :
                                  gson.fromJson(transport.schedule, RecurrenceRule.class);
            return VcaInfo.createNew(transport.instanceId,
                                     transport.appId,
                                     camera,
                                     transport.settings,
                                     rule,
                                     transport.enabled);
        }

        /**
         *
         * backward compatibility for nodes < v4.5
         *
         */
        KsVcaOld oldTransport = gson.fromJson(jsonInstance, KsVcaOld.class);
        String appId = AppVcaTypeMapper.getAppId(Program.KAI_X1, VcaType.parse(oldTransport.type));
        RecurrenceRule parsedRule = oldTransport.recurrenceRule == null ?
                                    null :
                                    gson.fromJson(gson.toJson(oldTransport.recurrenceRule), RecurrenceRule.class);
        return VcaInfo.createNew(oldTransport.instanceId,
                                 appId,
                                 new DeviceChannelPair(oldTransport.coreDeviceId, oldTransport.channelId),
                                 oldTransport.thresholds,
                                 parsedRule,
                                 oldTransport.enabled);
    }

    public static String stringifyVcaInfoForSendingToNode(VcaInfo vcaInfo)
    {
        if (!Environment.getInstance().onCloud())
        {
            throw new InvalidEnvironmentException();
        }

        Gson gson = new Gson();
        CacheClient cacheClient = CacheClient.getInstance();
        CachedDevice cachedDevice = cacheClient.getDeviceByCoreId(vcaInfo.getCamera().getCoreDeviceId());
        CachedNodeObjectInfo nodeObject = cacheClient.getNodeObject(cachedDevice);
        if (nodeObject.getReleaseNumber() >= 4.5)
        {
            KsVcaInstance transport = new KsVcaInstance(
                    vcaInfo.getInstanceId(),
                    vcaInfo.getAppId(),
                    vcaInfo.getCamera().getChannelId(),  //note
                    vcaInfo.getSettings(),
                    gson.toJson(vcaInfo.getRecurrenceRule()),
                    true,
                    false,
                    VcaStatus.WAITING.name()
            );
            return gson.toJson(transport);
        }

        /**
         *
         * backward compatibility with nodes < v4.5
         *
         */
        KsVcaOld oldTransport = new KsVcaOld(
                vcaInfo.getInstanceId(),
                AppVcaTypeMapper.getVcaType(vcaInfo.getAppId()).getVcaTypeName(),
                nodeObject.getPlatformDeviceId(),
                vcaInfo.getCamera().getCoreDeviceId(),
                vcaInfo.getCamera().getChannelId(),
                vcaInfo.getSettings(),
                vcaInfo.getRecurrenceRule(),
                vcaInfo.isEnabled(),
                "WAITING",
                false
        );

        return gson.toJson(oldTransport);
    }

    public static VcaInfo parseVcaInfoReceivedOnNode(String jsonInstance)
    {
        if (!Environment.getInstance().onKaiNode())
        {
            throw new InvalidEnvironmentException();
        }

        KsVcaInstance sentInfo = new Gson().fromJson(jsonInstance, KsVcaInstance.class);
        DeviceChannelPair camera = new DeviceChannelPair(sentInfo.cameraCoreDeviceId, "0");
        RecurrenceRule rule = Util.isNullOrEmpty(sentInfo.schedule) ?
                              null :
                              new Gson().fromJson(sentInfo.schedule, RecurrenceRule.class);

        return VcaInfo.createNew(sentInfo.instanceId,
                                 sentInfo.appId,
                                 camera,
                                 sentInfo.settings,
                                 rule,
                                 sentInfo.enabled);
    }

    public static String stringifyVcaListForSendingToCloud(List<LocalVcaInstance> localList)
    {
        List<KsVcaInstance> transportList = new ArrayList<>();
        for (LocalVcaInstance localInst : localList)
        {
            VcaInfo vcaInfo = localInst.getVcaInfo();
            transportList.add(new KsVcaInstance(
                    vcaInfo.getInstanceId(),
                    vcaInfo.getAppId(),
                    vcaInfo.getCamera().getCoreDeviceId(),
                    vcaInfo.getSettings(),
                    new Gson().toJson(vcaInfo.getRecurrenceRule()),
                    vcaInfo.isEnabled(),
                    localInst.migrationRequired(),
                    localInst.getStatus().name()
            ));
        }
        return new Gson().toJson(transportList);
    }

}
