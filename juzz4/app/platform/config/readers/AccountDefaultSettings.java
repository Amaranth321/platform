package platform.config.readers;

import models.MongoFeature;
import models.notification.BucketNotificationSettings;
import platform.events.EventType;
import platform.notification.NotifyMethod;
import play.Logger;

import java.util.*;

/**
 * Default account settings. Add account-related settings only.
 *
 * @author Aye Maung
 * @version v4.4
 */
public class AccountDefaultSettings extends AbstractReader
{
    private static final AccountDefaultSettings instance = new AccountDefaultSettings();

    public static AccountDefaultSettings getInstance()
    {
        return instance;
    }

    private AccountDefaultSettings()
    {
    }

    @Override
    protected String configJsonFile()
    {
        return "conf/platform/account.defaults.json";
    }

    public List<String> getBucketFeatureNames()
    {
        // get default features
        List<String> featureNames = reader().getAsList("default-bucket-features", new ArrayList());
        List<String> resultFeatureNames = new ArrayList<>();
        for (String featureName : featureNames)
        {
            MongoFeature feature = MongoFeature.getByName(featureName);
            if (feature == null)
            {
                Logger.error("invalid feature (%s) under default-bucket-features", featureName);
                continue;
            }
            resultFeatureNames.add(feature.getName());
        }
        return resultFeatureNames;
    }

    public Map<EventType, BucketNotificationSettings.EventTypeSettings> getNotificationSettings()
    {
        Map<EventType, BucketNotificationSettings.EventTypeSettings> settingsMap = new LinkedHashMap<>();
        List<Map<String, Object>> defaultsByType = reader().getAsList("default-notification-settings", new ArrayList<>());
        for (Map<String, Object> map : defaultsByType)
        {
            try
            {
                EventType type = EventType.parse(map.get("event-type").toString());
                if (type.equals(EventType.UNKNOWN))
                {
                    Logger.error("invalid event type in default alert settings");
                    continue;
                }

                boolean notification = (boolean) map.get("notification-enabled");
                boolean video = (boolean) map.get("video-required");

                settingsMap.put(type, new BucketNotificationSettings.EventTypeSettings(notification, video));
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }
        return settingsMap;
    }

    public Map<EventType, Set<NotifyMethod>> getNotifyMethods()
    {
        Set<NotifyMethod> serverEnabledMethods = NotifyMethod.getServerEnabledMethods();
        Map<EventType, Set<NotifyMethod>> settingsMap = new LinkedHashMap<>();
        List<Map<String, Object>> defaultsByType = reader().getAsList("default-notification-settings", new ArrayList<>());
        for (Map<String, Object> map : defaultsByType)
        {
            try
            {
                EventType type = EventType.parse(map.get("event-type").toString());
                if (type.equals(EventType.UNKNOWN))
                {
                    Logger.error("invalid event type in default alert settings");
                    continue;
                }

                List<String> methodStrings = (List<String>) map.get("notify-methods");
                Set<NotifyMethod> allowedMethods = new LinkedHashSet<>();
                for (String methodString : methodStrings)
                {
                    NotifyMethod method = NotifyMethod.valueOf(methodString);
                    if (serverEnabledMethods.contains(method))
                    {
                        allowedMethods.add(method);
                    }
                }

                settingsMap.put(type, allowedMethods);
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }
        return settingsMap;
    }

    public List<String> getNodeUserRoleFeatureNames()
    {
        // get default features
        List<String> featureNames = reader().getAsList("node-user-role.features", new ArrayList());
        List<String> resultFeatureNames = new ArrayList<>();
        for (String featureName : featureNames)
        {
            MongoFeature feature = MongoFeature.getByName(featureName);
            if (feature == null)
            {
                Logger.error("invalid feature (%s) under node-user-role.features", featureName);
                continue;
            }
            resultFeatureNames.add(feature.getName());
        }
        return resultFeatureNames;
    }
}
