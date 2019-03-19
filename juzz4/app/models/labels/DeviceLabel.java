package models.labels;

import com.google.code.morphia.annotations.Indexed;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedStoreLabel;
import platform.devices.DeviceChannelPair;
import platform.label.LabelType;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Switching label type is not allowed because each type holds different information.
 * If the type needs to be changed, a new label will be created with the target type and same information.
 * labelId must remain the same.
 *
 * @author Aye Maung
 * @see LabelStore
 * @see LabelRegion
 * @see models.labels.LabelOthers
 * @since v4.5
 */
public abstract class DeviceLabel extends Model
{
    @Indexed
    private final long bucketId;
    private final String labelId;
    private final LabelType type;
    private List<DeviceChannelPair> cameraList;
    private String labelName;

    protected static String generateId()
    {
        return UUID.randomUUID().toString();
    }

    protected DeviceLabel(long bucketId, String labelId, String labelName, LabelType type)
    {
        this.bucketId = bucketId;
        this.labelId = labelId;
        this.labelName = labelName;
        this.type = type;
    }

    public long getBucketId()
    {
        return bucketId;
    }

    public String getLabelId()
    {
        return labelId;
    }

    public String getLabelName()
    {
        return labelName;
    }

    public void setLabelName(String labelName)
    {
        this.labelName = labelName;
    }

    public LabelType getType()
    {
        return type;
    }

    public List<DeviceChannelPair> getCameraList()
    {
        if (cameraList == null)
        {
            return new ArrayList<>();
        }
        return new ArrayList<>(cameraList);
    }

    public void assignCamera(DeviceChannelPair camera)
    {
        if (cameraList == null)
        {
            cameraList = new ArrayList<>();
        }
        if (!cameraList.contains(camera))
        {
            cameraList.add(camera);
        }

        //update cache
        if (getType() == LabelType.STORE)
        {
            CachedStoreLabel storeLabel = CacheClient.getInstance().getStoreLabel(getLabelId());
            CacheClient.getInstance().remove(storeLabel);
        }
    }

    public void unassignCamera(DeviceChannelPair camera)
    {
        if (cameraList == null)
        {
            cameraList = new ArrayList<>();
        }
        if (cameraList.contains(camera))
        {
            cameraList.remove(camera);
        }

        //update cache
        if (getType() == LabelType.STORE)
        {
            CachedStoreLabel storeLabel = CacheClient.getInstance().getStoreLabel(getLabelId());
            CacheClient.getInstance().remove(storeLabel);
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s", type, labelName);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
        {
            return false;
        }
        if (!this.getClass().isAssignableFrom(o.getClass()))
        {
            return false;
        }
        DeviceLabel other = (DeviceLabel) o;
        return this.labelId.equals(other.labelId);
    }
}
