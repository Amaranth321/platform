package jobs.cloud.independent;

import jobs.cloud.CloudCronJob;
import lib.util.Util;
import models.MongoRole;
import models.MongoUser;
import models.notification.EventToNotify;
import models.notification.LabelEventToNotify;
import models.notification.SentLabelNotification;
import models.notification.SentNotification;
import platform.content.delivery.DeviceEventNotifier;
import platform.content.delivery.EventNotifier;
import platform.content.delivery.LabelEventNotifier;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.db.cache.proxies.CachedStoreLabel;
import platform.db.cache.proxies.CachedUser;
import platform.notification.LabelNotificationInfo;
import platform.notification.NotificationInfo;
import platform.notification.NotifyMethod;
import play.Logger;
import play.jobs.Every;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Only for Cloud notifications. On nodes, refer to {@link jobs.node.NodeNotificationsJob}.
 *
 * @author Aye Maung
 * @since v4.4
 */
@Every("2s")
public class CloudNotificationsJob extends CloudCronJob
{
    private static final CacheClient cacheClient = CacheClient.getInstance();

    @Override
    public void doJob()
    {
        try
        {
            EventToNotify deviceEvent = getNextEventFromDevice();
            LabelEventToNotify labelEvent = getNextEventFromLabel();

            while ((deviceEvent != null && !deviceEvent.isNew()) ||
                   (labelEvent != null && !labelEvent.isNew()))
            {
                if (deviceEvent != null && !deviceEvent.isNew())
                {
                    notifyDeviceEvents(deviceEvent);
                    deviceEvent.delete();
                    deviceEvent = getNextEventFromDevice();
                }

                if (labelEvent != null && !labelEvent.isNew())
                {
                    notifyLabelEvents(labelEvent);
                    labelEvent.delete();
                    labelEvent = getNextEventFromLabel();
                }
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    private EventToNotify getNextEventFromDevice()
    {
        return EventToNotify.getOldest();
    }

    private LabelEventToNotify getNextEventFromLabel()
    {
        return LabelEventToNotify.getOldest();
    }

    private void notifyDeviceEvents(EventToNotify notifyItem)
    {
        NotificationInfo notificationInfo = NotificationInfo.forEvent(notifyItem);
        if (notificationInfo == null)
        {
            return;
        }

        //notify users of the device
        CachedDevice eventDevice = cacheClient.getDeviceByCoreId(notificationInfo.getCamera().getCoreDeviceId());
        Map<Long, Set<NotifyMethod>> notifiedUsersAndMethodsMap = new LinkedHashMap<>();
        
		for (String userId : eventDevice.getUserIdSet()) {
			CachedUser user = cacheClient.getUser(userId);
			MongoUser mUser = new MongoUser().getById(userId);
			if (user == null || !mUser.hasAccessToFeature("historical-alerts")) {
				continue;
			}
			/*
			 * if (user == null) //deleted { continue; }
			 */

            Set<NotifyMethod> methodsUsed = new HashSet<>();
            for (NotifyMethod method : user.getNotifyMethods(notificationInfo.getEventType()))
            {
                try
                {
                    EventNotifier notifier = new DeviceEventNotifier(user, notificationInfo);
                    boolean ok;
                    switch (method)
                    {
                        case ON_SCREEN:
                            ok = notifier.onScreen();
                            break;

                        case EMAIL:
                            ok = notifier.toEmail();
                            break;

                        case SMS:
                            ok = notifier.toSMS();
                            break;

                        case MOBILE_PUSH:
                            ok = notifier.toMobilePush();
                            break;

                        default:
                            ok = false;
                            Logger.error(Util.whichFn() + "%s method not supported", method);
                    }

                    if (ok)
                    {
                        methodsUsed.add(method);
                    }
                }
                catch (Exception e)
                {
                    Logger.error(e, "Failed to deliver via %s", method);
                }
            }

            //save Notification record for each user
            if (!methodsUsed.isEmpty())
            {
                notifiedUsersAndMethodsMap.put(Long.parseLong(user.getUserId()), methodsUsed);
            }
        }

        //record and remove
        SentNotification.createNew(notificationInfo, notifiedUsersAndMethodsMap);
    }

    private void notifyLabelEvents(LabelEventToNotify notifyItem)
    {
        LabelNotificationInfo notificationInfo = LabelNotificationInfo.forEvent(notifyItem);
        if (notificationInfo == null)
        {
            return;
        }

        //notify users of the cameras assigned to the label
        CachedStoreLabel storeLabel = cacheClient.getStoreLabel(notifyItem.getLabelId());
        Map<Long, Set<NotifyMethod>> notifiedUsersAndMethodsMap = new LinkedHashMap<>();
        for (String userId : storeLabel.getCameraUserIdList())
        {
            CachedUser user = cacheClient.getUser(userId);
            if (user == null)   //deleted
            {
                continue;
            }

            Set<NotifyMethod> methodsUsed = new HashSet<>();
            for (NotifyMethod method : user.getNotifyMethods(notificationInfo.getType().getEventType()))
            {
                try
                {
                    EventNotifier notifier = new LabelEventNotifier(user, notificationInfo);
                    boolean ok;
                    switch (method)
                    {
                        case ON_SCREEN:
                            ok = notifier.onScreen();
                            break;

                        case EMAIL:
                            ok = notifier.toEmail();
                            break;

                        case SMS:
                            ok = notifier.toSMS();
                            break;

                        case MOBILE_PUSH:
                            ok = notifier.toMobilePush();
                            break;

                        default:
                            ok = false;
                            Logger.error(Util.whichFn() + "%s method not supported", method);
                    }

                    if (ok)
                    {
                        methodsUsed.add(method);
                    }
                }
                catch (Exception e)
                {
                    Logger.error(e, "Failed to deliver via %s", method);
                }
            }

            //save Notification record for each user
            if (!methodsUsed.isEmpty())
            {
                notifiedUsersAndMethodsMap.put(Long.parseLong(user.getUserId()), methodsUsed);
            }
        }

        //record and remove
        SentLabelNotification.createNew(notificationInfo, notifiedUsersAndMethodsMap);
    }
}
