package platform.label;

import lib.util.exceptions.ApiException;
import models.MongoDevice;
import models.MongoUser;
import models.labels.DeviceLabel;
import models.labels.LabelStore;
import models.node.NodeCamera;
import models.node.NodeObject;
import models.notification.LabelOccupancySettings;
import platform.DeviceManager;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaManager;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedStoreLabel;
import platform.devices.DeviceChannelPair;
import play.Logger;

import java.util.*;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class LabelManager
{
    private static LabelManager instance = new LabelManager();

    public static LabelManager getInstance()
    {
        return instance;
    }

    private LabelManager()
    {
    }

    public List<DeviceLabel> getBucketLabels(long bucketId)
    {
        List<DeviceLabel> bucketLabels = new ArrayList<>();
        for (LabelType labelType : LabelType.values())
        {
            List<DeviceLabel> listOfType = labelType.getQuery().filter("bucketId", bucketId).asList();
            bucketLabels.addAll(listOfType);
        }

        return bucketLabels;
    }

    public DeviceLabel findLabel(String labelId)
    {
        for (LabelType labelType : LabelType.values())
        {
            DeviceLabel target = labelType.getQuery().filter("labelId", labelId).first();
            if (target != null)
            {
                return target;
            }
        }

        return null;
    }

    public DeviceLabel findLabel(long bucketId, String labelName)
    {
        for (LabelType labelType : LabelType.values())
        {
            DeviceLabel label = labelType.getQuery()
                    .filter("bucketId", bucketId)
                    .filter("labelName", labelName)
                    .first();
            if (label != null)
            {
                return label;
            }
        }

        return null;
    }

    public List<DeviceLabel> getLabelsOf(DeviceChannelPair camera)
    {
        List<DeviceLabel> cameraLabels = new ArrayList<>();
        for (LabelType labelType : LabelType.values())
        {
            Iterable iterable = labelType.getQuery().field("cameraList").hasThisOne(camera).fetch();
            for (Object o : iterable)
            {
                cameraLabels.add((DeviceLabel) o);
            }
        }
        return cameraLabels;
    }

    /**
     * There should only be one store label assigned to a camera
     */
    public LabelStore getAssignedStoreLabel(DeviceChannelPair camera)
    {
        Iterator iterator = LabelType.STORE.getQuery().field("cameraList").hasThisOne(camera).iterator();
        if (!iterator.hasNext())
        {
            return null;
        }

        //just take the first one if there are more than one STORE label assigned
        return (LabelStore) iterator.next();
    }

    public Set<DeviceLabel> getUserAccessibleLabels(long userId)
    {
        Set<DeviceLabel> accessibleList = new LinkedHashSet<>();
        MongoUser user = MongoUser.getById(userId + "");
        if (user == null)
        {
            return accessibleList;
        }

        List<MongoDevice> devices = DeviceManager.getInstance().getDevicesOfUser(user.getUserId());
        for (MongoDevice device : devices)
        {
            if (!device.isKaiNode())
            {
                accessibleList.addAll(getLabelsOf(new DeviceChannelPair(device.getCoreDeviceId(), "0")));
            }
            else
            {
                NodeObject nodeObject = NodeObject.findByCoreId(device.getCoreDeviceId());
                if (nodeObject == null)
                {
                    continue;
                }
                for (NodeCamera nodeCamera : nodeObject.getCameras())
                {
                    DeviceChannelPair idPair = new DeviceChannelPair(device.getCoreDeviceId(), nodeCamera.nodeCoreDeviceId);
                    accessibleList.addAll(getLabelsOf(idPair));
                }
            }
        }

        return accessibleList;
    }

    public void deleteLabel(DeviceLabel deviceLabel) throws ApiException
    {
        deviceLabel.delete();
        labelUpdated(deviceLabel);

        if (deviceLabel.getType() == LabelType.STORE)
        {
            //remove occupancy settings
            LabelOccupancySettings.q().filter("labelId", deviceLabel.getLabelId()).delete();

            //remove cached settings
            removeCachedSettings(deviceLabel.getLabelId());

            //remove cached labels
            CachedStoreLabel storeLabel = CacheClient.getInstance().getStoreLabel(deviceLabel.getLabelId());
            CacheClient.getInstance().remove(storeLabel);
        }
    }

    public void removeCachedSettings(String labelId)
    {
        CacheClient cacheClient = CacheClient.getInstance();
        cacheClient.remove(cacheClient.getOccupancySettings(labelId));
    }

    public void cameraDeleted(DeviceChannelPair camera)
    {
        List<DeviceLabel> assignedLabels = getLabelsOf(camera);
        for (DeviceLabel assignedLabel : assignedLabels)
        {
            Logger.info("Un-assigning label (%s) from the deleted camera (%s).", assignedLabel, camera);
            assignedLabel.unassignCamera(camera);
            assignedLabel.save();
        }
    }

    public void labelUpdated(DeviceLabel label) throws ApiException
    {
        //update VCAs running on the assigned cameras
        VcaManager vcaMgr = VcaManager.getInstance();
        for (DeviceChannelPair camera : label.getCameraList())
        {
            for (IVcaInstance inst : vcaMgr.listVcaInstancesByCamera(camera))
            {
                inst.update(inst.getVcaInfo().getSettings(), inst.getVcaInfo().getRecurrenceRule());
            }
        }
    }


    public void labelCameraUpdate(DeviceChannelPair camera) throws ApiException
    {
        //update VCAs running on the assigned camera
        VcaManager vcaMgr = VcaManager.getInstance();
        for (IVcaInstance inst : vcaMgr.listVcaInstancesByCamera(camera))
        {
            inst.update(inst.getVcaInfo().getSettings(), inst.getVcaInfo().getRecurrenceRule());
        }
    }
}
