package models.labels;

import com.google.code.morphia.annotations.Entity;
import platform.common.Location;
import platform.label.LabelType;
import platform.time.OperatingSchedule;

/**
 * @author Aye Maung
 * @since v4.5
 */
@Entity
public class LabelRegion extends DeviceLabel
{
    private Location location;
    private OperatingSchedule schedule;

    public static LabelRegion createNew(long bucketId, String labelName)
    {
        LabelRegion existing = q()
                .filter("bucketId", bucketId)
                .filter("labelName", labelName)
                .first();
        if (existing != null)
        {
            return existing;
        }

        LabelRegion labelRegion = new LabelRegion(bucketId, generateId(), labelName);
        return labelRegion.save();
    }

    /**
     * For switching the type from any label to Region
     *
     * @param label
     */
    public static LabelRegion createFrom(DeviceLabel label)
    {
        LabelRegion labelRegion = new LabelRegion(
                label.getBucketId(),
                label.getLabelId(),
                label.getLabelName());

        labelRegion.save();
        label.delete();
        return labelRegion;
    }

    /**
     * Use static constructors above
     */
    protected LabelRegion(long bucketId, String labelId, String labelName)
    {
        super(bucketId, labelId, labelName, LabelType.REGION);
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
