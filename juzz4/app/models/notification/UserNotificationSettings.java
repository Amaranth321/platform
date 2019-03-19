package models.notification;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;
import platform.config.readers.AccountDefaultSettings;
import lib.util.ListUtil;
import models.MongoBucket;
import models.MongoUser;
import platform.analytics.VcaType;
import platform.events.EventType;
import platform.notification.NotifyMethod;
import play.Logger;
import play.modules.morphia.Model;

import java.util.*;

/**
 * <p/>
 * User notification settings by event type
 * <p/>
 * - {@link #supportedTypeList} and {@link #notifyMethodSets} entries must tally.
 * <p/>
 * - Prefer to use the cached version from {@link platform.db.cache.CacheClient#getUser(String)}
 *
 * @author Aye Maung
 * @since v4.4
 */
@Entity
public class UserNotificationSettings extends Model
{
    private final long bucketId;
    @Indexed
    private final long userId;
    private List<EventType> supportedTypeList;
    private List<Set<NotifyMethod>> notifyMethodSets;

    public UserNotificationSettings(MongoUser user)
    {
        bucketId = Long.parseLong(user.getBucketId());
        userId = Long.parseLong(user.getUserId());
        clearAll();

        //initialize based on bucket and default settings
        Map<EventType, Set<NotifyMethod>> defaults = AccountDefaultSettings.getInstance().getNotifyMethods();
        MongoBucket bucket = MongoBucket.getById(this.bucketId + "");
        List<EventType> bucketTypes = bucket.getNotificationSettings().getSupportedEventTypes();
        for (EventType eventType : defaults.keySet())
        {
            if (!bucketTypes.contains(eventType))
            {
                continue;
            }

            setNotifyMethods(eventType, defaults.get(eventType));
        }
    }

    public List<EventType> getSupportedEventTypes()
    {
        List<EventType> accessibleList = new ArrayList<>();
        MongoUser user = MongoUser.getById(this.userId + "");
        if (user == null)
        {
            return accessibleList;
        }

        //verify access
        for (EventType eventType : supportedTypeList)
        {
            if (eventType.isVcaEvent())
            {
                if (!user.hasAccessToVcaFeature(VcaType.of(eventType).getReportFeature()))
                {
                    continue;
                }
            }

            accessibleList.add(eventType);
        }

        return accessibleList;
    }

    public Set<NotifyMethod> getNotifyMethods(EventType eventType)
    {
        if (!supportedTypeList.contains(eventType))
        {
            return new LinkedHashSet<>();
        }

        Set<NotifyMethod> methodSet = notifyMethodSets.get(supportedTypeList.indexOf(eventType));
        if (methodSet == null)
        {
            methodSet = new LinkedHashSet<>();
        }

        return methodSet;
    }

    public void setNotifyMethods(EventType eventType, Set<NotifyMethod> methods)
    {
        if (!supportedTypeList.contains(eventType))
        {
            supportedTypeList.add(eventType);
            notifyMethodSets.add(new LinkedHashSet<NotifyMethod>());
        }

        notifyMethodSets.set(supportedTypeList.indexOf(eventType), methods);

        //replace NULL sets with empty ones to prevent mongo exceptions
        for (int i = 0; i < notifyMethodSets.size(); i++)
        {
            if (notifyMethodSets.get(i) == null)
            {
                notifyMethodSets.set(i, new LinkedHashSet<NotifyMethod>());
            }
        }
    }

    public void removeSupportedType(EventType type)
    {
        int index = supportedTypeList.indexOf(type);
        if (index < 0)
        {
            return;
        }

        supportedTypeList.remove(index);
        notifyMethodSets.remove(index);
    }

    public void syncWithDefaultTypes(Map<EventType, Set<NotifyMethod>> defaultSettings)
    {
        Set<EventType> defaultTypes = defaultSettings.keySet();

        List<EventType> extraList = ListUtil.getExtraItems(defaultTypes, supportedTypeList);
        List<EventType> missingList = ListUtil.getExtraItems(supportedTypeList, defaultTypes);

        //remove
        for (EventType extra : extraList)
        {
            removeSupportedType(extra);
            Logger.info("[user] (%s) is not in default settings. Removed.", extra);
        }

        //add
        for (EventType missing : missingList)
        {
            setNotifyMethods(missing, defaultSettings.get(missing));
            Logger.info("[user] New notification type added (%s)", missing);
        }

        if (!extraList.isEmpty() || !missingList.isEmpty())
        {
            save();
        }
    }

    public long getBucketId()
    {
        return bucketId;
    }

    public long getUserId()
    {
        return userId;
    }

    public void clearAll()
    {
        supportedTypeList = new ArrayList<>();
        notifyMethodSets = new ArrayList<>();
    }
}
