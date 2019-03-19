package models.labels;

import com.google.code.morphia.annotations.Entity;
import platform.common.Location;
import platform.devices.DeviceChannelPair;
import platform.label.LabelManager;
import platform.label.LabelType;
import platform.time.OperatingSchedule;

import java.util.List;

/**
 * @author Aye Maung
 * @since v4.5
 */
@Entity
public class LabelStore extends DeviceLabel
{
    private Location location;
    private OperatingSchedule schedule;

    public static LabelStore createNew(long bucketId, String labelName)
    {
        LabelStore existing = q()
                .filter("bucketId", bucketId)
                .filter("labelName", labelName)
                .first();
        if (existing != null)
        {
            return existing;
        }

        String labelId = generateId();
        LabelStore labelStore = new LabelStore(bucketId, labelId, labelName);
        return labelStore.save();
    }

    /**
     * For switching the type from any label to Store
     *
     * @param label
     */
    public static LabelStore createFrom(DeviceLabel label)
    {
        // create new store label
        LabelStore labelStore = new LabelStore(label.getBucketId(), label.getLabelId(), label.getLabelName());

        // assign devices
        for (DeviceChannelPair camera : label.getCameraList())
        {
            // only assign devices without existing store label
            LabelStore existingStoreLabel = LabelManager.getInstance().getAssignedStoreLabel(camera);
            if (existingStoreLabel == null)
            {
                labelStore.assignCamera(camera);
            }
        }

        label.delete();
        labelStore.save();

        return labelStore;
    }

    /**
     * Use static constructors above
     */
    protected LabelStore(long bucketId, String labelId, String labelName)
    {
        super(bucketId, labelId, labelName, LabelType.STORE);
    }

    public Location getLocation()
    {
        return location;
    }

    public void setLocation(Location location)
    {
        this.location = location;
    }

    public OperatingSchedule getSchedule()
    {
        return schedule;
    }

    public void setSchedule(OperatingSchedule schedule)
    {
        this.schedule = schedule;
    }

}
