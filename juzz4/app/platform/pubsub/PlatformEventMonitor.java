package platform.pubsub;

import play.Logger;
import play.jobs.Job;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author:  Aye Maung
 * <p/>
 * For event-triggered tasks that should be executed asynchronously
 */
public class PlatformEventMonitor
{
    private static PlatformEventMonitor instance = new PlatformEventMonitor();

    private ConcurrentHashMap<PlatformEventType, HashSet<PlatformEventTask>> eventTaskMap = new ConcurrentHashMap<>();

    private PlatformEventMonitor()
    {
        //init task lists
        for (PlatformEventType type : PlatformEventType.values())
        {
            eventTaskMap.put(type, new HashSet<PlatformEventTask>());
        }
    }

    public static PlatformEventMonitor getInstance()
    {
        return instance;
    }

    /**
     * Subscribes a task to be executed when the specified event happens.
     * Multiple tasks are allowed to be subscribed to the same event.
     * Hence, the caller must ensure that the same task is subscribed to the same event only once.
     *
     * @param eventType event type
     * @param task      task to be executed when the event occurs
     */
    public void subscribe(PlatformEventType eventType, PlatformEventTask task)
    {
        HashSet<PlatformEventTask> taskList = eventTaskMap.get(eventType);
        taskList.add(task);
    }

    /**
     * broadcast will execute all tasks subscribed to this event
     *
     * @param event
     */
    public void broadcast(final PlatformEventType event, final Object... params)
    {
        new Job<Void>()
        {
            @Override
            public void doJob()
            {
                HashSet<PlatformEventTask> tasks = eventTaskMap.get(event);
                if (tasks.size() == 0)
                {
                    return;
                }

                Logger.info("Executing %s subscribed task(s) for platform event (%s)", tasks.size(), event);
                for (PlatformEventTask task : tasks)
                {
                    try
                    {
                        task.run(params);
                    }
                    catch (Exception e)
                    {
                        Logger.error(e, "");
                    }
                }
            }
        }.now();
    }

}
