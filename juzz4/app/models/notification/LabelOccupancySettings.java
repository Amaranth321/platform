package models.notification;

import com.google.code.morphia.annotations.Entity;
import models.labels.LabelStore;
import platform.analytics.occupancy.OccupancyLimit;
import play.modules.morphia.Model;

import java.util.TreeSet;

/**
 * @author Aye Maung
 * @since v4.5
 */
@Entity
public class LabelOccupancySettings extends Model
{
    private final String labelId;
    private boolean enabled;
    private TreeSet<OccupancyLimit> limits;
    private int minNotifyIntervalSeconds;

    public static LabelOccupancySettings findByLabelId(String labelId)
    {
        LabelOccupancySettings dbSetts = q().filter("labelId", labelId).first();
        if (dbSetts == null)
        {
            LabelStore storeLabel = LabelStore.q().filter("labelId", labelId).first();
            if (storeLabel == null)
            {
                return null;
            }
            else
            {
                dbSetts = new LabelOccupancySettings(labelId);
                dbSetts.save();
            }
        }

        if (dbSetts.limits == null)
        {
            dbSetts.limits = new TreeSet<>();
        }

        return dbSetts;
    }

    public LabelOccupancySettings(String labelId)
    {
        this.labelId = labelId;
        this.enabled = false;
        this.minNotifyIntervalSeconds = 1800;
    }

    public String getLabelId()
    {
        return labelId;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public TreeSet<OccupancyLimit> getLimits()
    {
        return limits;
    }

    public void setLimits(TreeSet<OccupancyLimit> limits)
    {
        this.limits = limits;
    }

    public int getMinNotifyIntervalSeconds()
    {
        return minNotifyIntervalSeconds;
    }

    public void setMinNotifyIntervalSeconds(int minNotifyIntervalSeconds)
    {
        this.minNotifyIntervalSeconds = minNotifyIntervalSeconds;
    }
}
