package platform.db.cache.proxies;

import models.MongoUser;
import models.notification.UserNotificationSettings;
import platform.db.cache.CachedObject;
import platform.events.EventType;
import platform.notification.NotifyMethod;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class CachedUser extends CachedObject<CachedUser>
{
    private final String bucketId;
    private final String userId;
    private final String name;
    private final String login;
    private final String email;
    private final String phone;
    private final boolean activated;
    private final String language;
    private final Map<EventType, Set<NotifyMethod>> notificationSettings;

    public CachedUser(String cacheKey, MongoUser dbUser)
    {
        super(cacheKey);

        bucketId = dbUser.getBucketId();
        userId = dbUser.getUserId();
        name = dbUser.getName();
        login = dbUser.getLogin();
        email = dbUser.getEmail();
        phone = dbUser.getPhone();
        activated = dbUser.isActivated();
        language = dbUser.getLanguage();

        notificationSettings = new LinkedHashMap<>();
        UserNotificationSettings notiSetts = dbUser.getNotificationSettings();
        for (EventType eventType : notiSetts.getSupportedEventTypes())
        {
            notificationSettings.put(eventType, notiSetts.getNotifyMethods(eventType));
        }
    }

    @Override
    public CachedUser getObject()
    {
        return this;
    }

    public String getBucketId()
    {
        return bucketId;
    }

    public String getUserId()
    {
        return userId;
    }

    public String getName()
    {
        return name;
    }

    public String getLogin()
    {
        return login;
    }

    public String getEmail()
    {
        return email;
    }

    public String getPhone()
    {
        return phone;
    }

    public boolean isActivated()
    {
        return activated;
    }

    public String getLanguage()
    {
        return language;
    }

    public Set<NotifyMethod> getNotifyMethods(EventType eventType)
    {
        if (!notificationSettings.containsKey(eventType))
        {
            return new LinkedHashSet<>();
        }

        return notificationSettings.get(eventType);
    }
}
