package models.transportobjects;

import models.labels.DeviceLabel;
import models.labels.LabelRegion;
import models.labels.LabelStore;
import platform.analytics.occupancy.OccupancySettings;
import platform.label.LabelType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class LabelTransport
{
    public final String labelId;
    public final String name;
    public final LabelType type;
    public final Map info;

    public LabelTransport(DeviceLabel label)
    {
        this.labelId = label.getLabelId();
        this.name = label.getLabelName();
        this.type = label.getType();

        Map info = new LinkedHashMap();
        switch (label.getType())
        {
            case STORE:
                LabelStore store = (LabelStore) label;
                info.put("location", store.getLocation());
                info.put("schedule", store.getSchedule());
                info.put("occupancySettings", new OccupancySettings()); //so that new UI won't break
                break;

            case REGION:
                LabelRegion region = (LabelRegion) label;
                info.put("location", region.getLocation());
                info.put("schedule", region.getSchedule());
                break;
        }

        this.info = info;
    }
}
