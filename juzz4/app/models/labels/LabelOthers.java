package models.labels;

import com.google.code.morphia.annotations.Entity;
import platform.devices.DeviceChannelPair;
import platform.label.LabelType;

/**
 * @author Aye Maung
 * @since v4.5
 */
@Entity
public class LabelOthers extends DeviceLabel
{
    public static LabelOthers createNew(long bucketId, String labelName)
    {
        LabelOthers existing = q()
                .filter("bucketId", bucketId)
                .filter("labelName", labelName)
                .first();
        if (existing != null)
        {
            return existing;
        }

        LabelOthers labelOthers = new LabelOthers(bucketId, generateId(), labelName);
        return labelOthers.save();
    }

    /**
     * For switching the type from any label to others
     *
     * @param label
     */
    public static LabelOthers createFrom(DeviceLabel label)
    {
        // create new other label
        LabelOthers labelOthers = new LabelOthers(label.getBucketId(), label.getLabelId(), label.getLabelName());

        // assign devices
        for (DeviceChannelPair camera : label.getCameraList())
        {
            labelOthers.assignCamera(camera);
        }

        label.delete();
        labelOthers.save();
        return labelOthers;
    }

    /**
     * Use static constructors above
     */
    protected LabelOthers(long bucketId, String labelId, String labelName)
    {
        super(bucketId, labelId, labelName, LabelType.OTHERS);
    }
}
