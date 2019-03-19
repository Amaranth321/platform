package models.notification;

import com.google.code.morphia.annotations.Entity;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import platform.config.readers.AccountDefaultSettings;
import lib.util.ListUtil;
import platform.events.EventType;
import play.Logger;
import play.modules.morphia.Model;

import java.util.*;

/**
 * <p/>
 * Bucket-wide notification settings by event type
 * <p/>
 * - {@link #supportedTypeList} and {@link #settingsList} entries must tally.
 * <p/>
 * - Prefer to use the cached version from {@link platform.db.cache.CacheClient#getBucket(long)}
 *
 * @author Aye Maung
 * @since v4.4
 */
@Entity
public class BucketNotificationSettings extends Model
{
    private final long bucketId;
    private List<EventType> supportedTypeList;
    private List<EventTypeSettings> settingsList;

    public static BucketNotificationSettings parse(String jsonSettings)
    {
        Map<String, Object> rawMap = new LinkedHashMap<>();
        rawMap = new Gson().fromJson(jsonSettings, rawMap.getClass());

        Double parsedId = Double.parseDouble(rawMap.get("bucketId").toString());
        BucketNotificationSettings parsedSettings = new BucketNotificationSettings(parsedId.longValue());

        List<EventTypeSettings> typeSettsList = new Gson().fromJson(String.valueOf(rawMap.get("settingsList")),
                                                                    new TypeToken<List<EventTypeSettings>>()
                                                                    {
                                                                    }.getType());

        List<String> rawTypeList = (List<String>) rawMap.get("supportedTypeList");
        for (int i = 0; i < rawTypeList.size(); i++)
        {
            EventType type = EventType.parse(rawTypeList.get(i));
            if (type != EventType.UNKNOWN)
            {
                EventTypeSettings typeSetts = typeSettsList.get(i);
                parsedSettings.updateEventTypeSettings(type,
                                                       typeSetts.isNotificationEnabled(),
                                                       typeSetts.isVideoRequired());
            }
        }

        return parsedSettings;
    }

    public BucketNotificationSettings(long bucketId)
    {
        this.bucketId = bucketId;
    }

    public long getBucketId()
    {
        return bucketId;
    }

    public List<EventType> getSupportedEventTypes()
    {
        return supportedTypeList == null ? new ArrayList<EventType>() : supportedTypeList;
    }

    public EventTypeSettings getSettingsByType(EventType eventType)
    {
        if (!supportedTypeList.contains(eventType))
        {
            return null;
        }

        int index = supportedTypeList.indexOf(eventType);
        return settingsList.get(index);
    }

    public void updateEventTypeSettings(EventType eventType,
                                        boolean notificationEnabled,
                                        boolean videoRequired)
    {
        if (supportedTypeList == null || settingsList == null)
        {
            supportedTypeList = new ArrayList<>();
            settingsList = new ArrayList<>();
        }

        EventTypeSettings typeSetts = getSettingsByType(eventType);
        if (typeSetts == null)
        {
            supportedTypeList.add(eventType);
            settingsList.add(new EventTypeSettings(notificationEnabled, videoRequired));
            return;
        }

        typeSetts.notificationEnabled = notificationEnabled;
        typeSetts.videoRequired = videoRequired;
    }

    public void removeSupportedType(EventType eventType)
    {
        int index = supportedTypeList.indexOf(eventType);
        if (index > -1)
        {
            supportedTypeList.remove(index);
            settingsList.remove(index);
        }
    }

    public void clearAll()
    {
        supportedTypeList = new ArrayList<>();
        settingsList = new ArrayList<>();
    }

    public boolean isNotificationEnabled(EventType eventType)
    {
        EventTypeSettings typeSetts = getSettingsByType(eventType);
        return typeSetts != null && typeSetts.notificationEnabled;
    }

    public boolean isVideoRequired(EventType eventType)
    {
        EventTypeSettings typeSetts = getSettingsByType(eventType);
        return typeSetts != null && typeSetts.videoRequired;
    }

    public void restoreDefaults()
    {
        clearAll();

        Map<EventType, EventTypeSettings> defaultSettings =
                AccountDefaultSettings.getInstance().getNotificationSettings();
        for (EventType eventType : defaultSettings.keySet())
        {
            EventTypeSettings typeSettings = defaultSettings.get(eventType);
            updateEventTypeSettings(
                    eventType,
                    typeSettings.notificationEnabled,
                    typeSettings.videoRequired
            );
        }
    }

    public void syncWithDefaultTypes(Map<EventType, EventTypeSettings> defaultSettings)
    {
        Set<EventType> defaultTypes = defaultSettings.keySet();

        List<EventType> extraList = ListUtil.getExtraItems(defaultTypes, supportedTypeList);
        List<EventType> missingList = ListUtil.getExtraItems(supportedTypeList, defaultTypes);

        //remove
        for (EventType extra : extraList)
        {
            removeSupportedType(extra);
            Logger.info("[bucket] (%s) is not in default settings. Removed.", extra);
        }

        //add
        for (EventType missing : missingList)
        {
            EventTypeSettings typeSettings = defaultSettings.get(missing);
            updateEventTypeSettings(
                    missing,
                    typeSettings.notificationEnabled,
                    typeSettings.videoRequired
            );
            Logger.info("[bucket] new notification type added (%s)", missing);
        }

        if (!extraList.isEmpty() || !missingList.isEmpty())
        {
            save();
        }
    }

    public static class EventTypeSettings
    {
        private boolean notificationEnabled;
        private boolean videoRequired;

        public EventTypeSettings(boolean notificationEnabled, boolean videoRequired)
        {
            this.notificationEnabled = notificationEnabled;
            this.videoRequired = videoRequired;
        }

        public boolean isNotificationEnabled()
        {
            return notificationEnabled;
        }

        public boolean isVideoRequired()
        {
            return videoRequired;
        }

    }
}
