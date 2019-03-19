package jobs.node;

import jobs.SingletonJob;
import models.MongoDevice;
import models.MongoUser;
import models.notification.EventToNotify;
import models.notification.SentNotification;
import models.notification.UserNotificationSettings;
import platform.NotificationManager;
import platform.notification.NotificationInfo;
import platform.notification.NotifyMethod;
import platform.notification.OnScreenData;
import play.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Notify events to users on node. Currently on-screen notifications only.
 * On cloud, Refer to {@link jobs.cloud.independent.CloudNotificationsJob}.
 * Unlike cloud job, this does not need to use {@link platform.db.cache.CacheClient}.
 *
 * @author Aye Maung
 * @since v4.4
 */
class NodeNotificationsJob extends NodeCronJob implements SingletonJob
{
    private static final NodeNotificationsJob instance = new NodeNotificationsJob();

    private boolean started = false;

    public static SingletonJob getInstance()
    {
        return instance;
    }

    @Override
    public void start()
    {
        if (!started)
        {
            every(getFreqSeconds());
            started = true;
        }
    }

    @Override
    public int getFreqSeconds()
    {
        return 2;
    }

    @Override
    public String getPrintedStatus()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println(String.format("Items left   : %s", EventToNotify.q().count()));
        return sw.toString();
    }

    @Override
    public void doJob()
    {
        EventToNotify item = getNext();
        while (item != null && !item.isNew())
        {
            try
            {
                process(item);
                item.delete();
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }

            item = getNext();
        }
    }

    private EventToNotify getNext()
    {
        return EventToNotify.getOldest();
    }

    private void process(EventToNotify notifyItem) throws Exception
    {
        NotificationInfo notificationInfo = NotificationInfo.forEvent(notifyItem);

        //check camera
        MongoDevice camera = MongoDevice.getByCoreId(notificationInfo.getCamera().getCoreDeviceId());
        if (camera == null)
        {
            return;
        }

        //notify users of the device
        Map<Long, Set<NotifyMethod>> notifiedUsersAndMethodsMap = new LinkedHashMap<>();
        for (String userId : camera.getUserIds())
        {
            MongoUser user = MongoUser.getById(userId);
            UserNotificationSettings settings = user.getNotificationSettings();
            Set<NotifyMethod> methodsUsed = new HashSet<>();
            for (NotifyMethod method : settings.getNotifyMethods(notificationInfo.getEventType()))
            {
                switch (method)
                {
                    case ON_SCREEN:
                        deliverOnScreen(Long.parseLong(userId), notificationInfo);

                        /**
                         *
                         * add more notification handlers here if they are allowed on nodes
                         *
                         */
                }

                methodsUsed.add(method);
            }

            notifiedUsersAndMethodsMap.put(Long.parseLong(userId), methodsUsed);
        }

        //record and remove
        SentNotification.createNew(notificationInfo, notifiedUsersAndMethodsMap);
    }

    private boolean deliverOnScreen(long userId, NotificationInfo info)
    {
        try
        {
            OnScreenData screenData = OnScreenData.forLocalCameraNotification(info);
            String jsonData = screenData.toApiOutput();

            //send to mq
            NotificationManager.getManager().publishEventNotification(userId, jsonData);
            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }
}
