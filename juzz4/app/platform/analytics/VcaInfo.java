package platform.analytics;

import com.google.gson.Gson;
import com.kaisquare.vca.thrift.TVcaInfo;
import models.labels.LabelStore;
import platform.devices.DeviceChannelPair;
import platform.label.LabelManager;
import platform.time.RecurrenceRule;
import platform.time.RepeatOption;
import platform.time.WeeklyPeriods;
import play.Logger;

import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class VcaInfo
{
    private final String instanceId;
    private final String appId;
    private final DeviceChannelPair camera;
    private String settings;
    private RecurrenceRule recurrenceRule;
    private boolean enabled;

    public static VcaInfo createNew(String instanceId,
                                    String appId,
                                    DeviceChannelPair camera,
                                    String settings,
                                    RecurrenceRule recurrenceRule,
                                    boolean enabled)
    {
        return new VcaInfo(instanceId,
                           appId,
                           camera,
                           settings,
                           recurrenceRule,
                           enabled);
    }

    public static VcaInfo fromThrift(TVcaInfo thriftInfo) throws Exception
    {
        DeviceChannelPair camera = new DeviceChannelPair(thriftInfo.getCoreDeviceId(), thriftInfo.getChannelId());
        return new VcaInfo(thriftInfo.getInstanceId(),
                           thriftInfo.getAppId(),
                           camera,
                           thriftInfo.getSettings(),
                           RecurrenceRule.parse(thriftInfo.getRecurrenceRule()),
                           thriftInfo.isEnabled());
    }

    private VcaInfo(String instanceId,
                    String appId,
                    DeviceChannelPair camera,
                    String settings,
                    RecurrenceRule recurrenceRule,
                    boolean enabled)
    {
        this.instanceId = instanceId;
        this.appId = appId;
        this.camera = camera;
        this.settings = settings;
        this.recurrenceRule = recurrenceRule;
        this.enabled = enabled;
    }

    public String getInstanceId()
    {
        return instanceId;
    }

    public String getAppId()
    {
        return appId;
    }

    public DeviceChannelPair getCamera()
    {
        return camera;
    }

    public String getSettings()
    {
        return settings;
    }

    public void setSettings(String settings)
    {
        this.settings = settings;
    }

    public RecurrenceRule getRecurrenceRule()
    {
        return recurrenceRule;
    }

    public void setRecurrenceRule(RecurrenceRule recurrenceRule)
    {
        this.recurrenceRule = recurrenceRule;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void checkAndAddResetHour()
    {
        Map<String, Object> settingsMap = new Gson().fromJson(getSettings(), Map.class);
        LabelStore storeLabel = LabelManager.getInstance().getAssignedStoreLabel(getCamera());

        if(settingsMap.containsKey("reset-hour"))
        {
            settingsMap.remove("reset-hour");
            setSettings(new Gson().toJson(settingsMap));
            Logger.info("[%s] removed cache reset hour", this);
        }

        if (storeLabel == null)
        {
            return;
        }

        WeeklyPeriods weeklyPeriods = storeLabel.getSchedule().getWeeklyPeriods();
        if (weeklyPeriods.getRepeatOption() == RepeatOption.NON_STOP)
        {
            settingsMap.put("reset-hour", weeklyPeriods.getLowestTrafficHour());
            setSettings(new Gson().toJson(settingsMap));
            Logger.info("[%s] added cache reset hour(%s:00)", this, weeklyPeriods.getLowestTrafficHour());
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s", appId, instanceId);
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof VcaInfo))
        {
            return false;
        }

        VcaInfo otherInfo = (VcaInfo) other;
        boolean idOk = this.instanceId.equals(otherInfo.instanceId);
        boolean settingsOk = (this.settings == null && otherInfo.settings == null) ||
                             (this.settings != null && this.settings.equals(otherInfo.settings));
        boolean scheduleOk = (this.recurrenceRule == null && otherInfo.recurrenceRule == null) ||
                             (this.recurrenceRule != null && this.recurrenceRule.equals(otherInfo.recurrenceRule));

        return idOk && settingsOk && scheduleOk;
    }

}
