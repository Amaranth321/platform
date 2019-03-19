package platform.kaisyncwrapper.node.tasks;

import com.google.code.morphia.query.Query;
import com.google.gson.Gson;
import models.NodeCommand;
import models.node.NodeCamera;
import models.node.NodeObject;
import models.notification.SentNotification;
import platform.coreengine.RecordingManager;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedNodeCamera;
import platform.devices.DeviceChannelPair;
import platform.events.EventType;
import platform.label.LabelManager;
import platform.reports.DecorativeQuery;
import platform.reports.EventReport;
import play.Logger;
import play.modules.morphia.Model;

import java.util.List;


/**
 * Sent from nodes when a device is deleted
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */

public class NodeDeviceDeleted extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        List<String> parameters = command.getParameters();
        String jsonCamera = parameters.get(0);
        NodeCamera deletedCamera = new Gson().fromJson(jsonCamera, NodeCamera.class);
        NodeObject nodeObject = getNodeObject();

        //no longer exists
        if (!nodeObject.getCameras().contains(deletedCamera))
        {
            return true;
        }

        //delete recordings
        DeviceChannelPair deletedIdPair = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), deletedCamera.nodeCoreDeviceId);
        RecordingManager.getInstance().deleteCloudRecordings(deletedIdPair);

        Logger.info("Deleting sent notifications for deviceChannelPair (%s:%s)", deletedIdPair.getCoreDeviceId(), deletedIdPair.getChannelId());
        SentNotification.q()
                .filter("notificationInfo.camera.coreDeviceId", deletedIdPair.getCoreDeviceId())
                .filter("notificationInfo.camera.channelId", deletedIdPair.getChannelId())
                .delete();

        //delete reports
        Logger.info("Deleting event reports");
        for (EventType eventType : EventReport.getSupportedEventTypes())
        {
            try
            {
                Logger.info("Deleting report: " + eventType + " device: " + deletedIdPair.getCoreDeviceId() + " channel: " + deletedIdPair.getChannelId());
                Query query = EventReport.getReport(eventType)
                        .query(null, null)
                        .addDevice(deletedIdPair)
                        .getQuery();

                // for crowddensity
                if (query instanceof DecorativeQuery)
                {
                    query = ((DecorativeQuery) query).getRawQuery();
                }

                Model.ds().delete(query);
            }
            catch (Exception e)
            {
                Logger.error(e.getMessage());
            }
        }

        //inform labels
        LabelManager.getInstance().cameraDeleted(deletedIdPair);

        //remove cache from all node cameras
        CacheClient cacheClient = CacheClient.getInstance();
        for (NodeCamera nodeCamera : nodeObject.getCameras())
        {
            DeviceChannelPair camIdPair = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), nodeCamera.nodeCoreDeviceId);
            CachedNodeCamera cachedObject = cacheClient.getNodeCamera(camIdPair);
            cacheClient.remove(cachedObject);
        }

        //remove
        nodeObject.getCameras().remove(deletedCamera);
        nodeObject.save();
        return true;
    }
}
