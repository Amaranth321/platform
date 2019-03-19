package platform.reports;

import lib.util.exceptions.DeviceNotExistsException;
import models.UnprocessedVcaEvent;
import models.archived.ArchivedEvent;
import platform.Environment;
import platform.config.readers.ConfigsCloud;
import play.Logger;
import play.jobs.Job;
import play.libs.F.Promise;
import play.modules.morphia.Model;
import play.modules.morphia.Model.MorphiaQuery;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * To process events for reports by this class
 * Instantiating multiple classes for event process at same time is NOT recommended.
 */
public class ReportParallelProcess implements Runnable
{
    private static final Object EventMutex = new Object();

    //set max events for a queue to prevent retrieving too many events that causes out of memory
    private static final int MAX_EVENTS_PER_QUEUE = 500;

    private int maximumThreads = Runtime.getRuntime().availableProcessors() + 1;
    private Iterator<Model> iterator;
    private volatile ConcurrentHashMap<String, BlockingQueue<ArchivedEvent>> deviceEventQueues;
    private volatile Queue<BlockingQueue<ArchivedEvent>> freeQueues;

    public ReportParallelProcess()
    {
        deviceEventQueues = new ConcurrentHashMap<>();
        freeQueues = new LinkedList<>();
    }

    public void setMaximumThreads(int value)
    {
        if (value >= 1)
        {
            maximumThreads = value;
        }
    }

    @Override
    public void run()
    {
        Promise[] promises = new Promise[maximumThreads];

        for (int i = 0; i < maximumThreads; i++)
        {
            promises[i] = new ReportProcessingJob().now();
        }

        Promise p = Promise.waitAll(promises);
        try
        {
            p.get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            Logger.error(e, "");
        }
        iterator = null;
    }

    private ArchivedEvent get(ArchivedEvent lastProcessedEvent)
    {
        synchronized (this)
        {
            if (iterator == null)
            {
                MorphiaQuery query = UnprocessedVcaEvent.q();
                query.order("eventInfo.time");

                //On cloud side,
                // in order to reduce the possibility of events being processed out of their original order,
                // a buffer period based on the event insertion time will be used.
                if (Environment.getInstance().onCloud())
                {
                    long bufferMillis = ConfigsCloud.getInstance().getVcaEventProcessingBufferSeconds() * 1000;
                    long eligibleAge = System.currentTimeMillis() - bufferMillis;
                    query.filter("_created <", eligibleAge);
                }

                iterator = query.fetch().iterator();
            }
        }

        ArchivedEvent event = null;
        while (event == null)
        {
            try
            {
                synchronized (EventMutex)
                {
                    //let the job keep processing the same device
                    if (lastProcessedEvent != null && deviceEventQueues.containsKey(lastProcessedEvent.getEventInfo().getCamera().getCoreDeviceId()))
                    {
                        BlockingQueue<ArchivedEvent> queue = deviceEventQueues.get(lastProcessedEvent.getEventInfo().getCamera().getCoreDeviceId());
                        event = queue.poll();
                        if (event == null)
                        {
                            //all the events in queue are already processed, so we reuse the queue for other deviceId
                            freeQueues.add(queue);
                            deviceEventQueues.remove(lastProcessedEvent.getEventInfo().getCamera().getCoreDeviceId());
                            event = nextEvent();
                        }
                    }
                    else
                    {
                        event = nextEvent();
                    }
                }
                break;
            }
            catch (NoAvailableSpaceException e)
            {
                event = null;
                //too many same device events, wait for them to be processed
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e1)
                {
                    break;
                }
            }
            catch (Exception e)
            {
                Logger.error(e, "error getting next unprocessed event from DB");
                //Unexpected exception, clean up everything, then start over
                Iterator<Entry<String, BlockingQueue<ArchivedEvent>>> queueIterator = deviceEventQueues.entrySet().iterator();
                while (queueIterator.hasNext())
                {
                    Entry<String, BlockingQueue<ArchivedEvent>> entry = queueIterator.next();
                    entry.getValue().clear();
                    queueIterator.remove();
                }
                iterator = null;
                event = null;
                break;
            }
        }

        return event;
    }

    private ArchivedEvent nextEvent() throws NoAvailableSpaceException
    {
        ArchivedEvent event = null;
        try
        {
            while (iterator.hasNext())
            {
                event = (ArchivedEvent) iterator.next();
                if (event == null)
                {
                    break;
                }

                String deviceId = event.getEventInfo().getCamera().getCoreDeviceId();
                if (!deviceEventQueues.containsKey(deviceId))
                {
                    //create a temporary queue for a deviceId, later we have to put the next event with same deviceId in this queue
                    BlockingQueue<ArchivedEvent> queue = freeQueues.poll();
                    if (queue == null)
                    {
                        queue = new LinkedBlockingQueue<>(MAX_EVENTS_PER_QUEUE);
                    }

                    deviceEventQueues.put(deviceId, queue);
                    break;
                }
                else
                {
                    BlockingQueue<ArchivedEvent> queue = deviceEventQueues.get(event.getEventInfo().getCamera().getCoreDeviceId());
                    try
                    {
                        if (queue.size() < MAX_EVENTS_PER_QUEUE)
                        {
                            queue.put(event);
                        }

                        if (queue.size() >= MAX_EVENTS_PER_QUEUE)
                        {
                            Logger.warn("slow processing the event of the device '%s'", deviceId);
                            throw new NoAvailableSpaceException(); //throw an exception to avoid deadlock
                        }
                    }
                    catch (InterruptedException e)
                    {
                        Logger.error(e, "error queuing events: interrupted");
                        break;
                    }
                    finally
                    {
                        event = null;
                    }
                }
            }
        }
        catch (NoAvailableSpaceException e)
        {
            throw e;
        }

        return event;
    }

    private class ReportProcessingJob extends Job
    {

        @Override
        public void doJob()
        {
            ArchivedEvent e = null;
            while ((e = get(e)) != null)
            {
            	
            	
            	
            	
                try
                {
                    /**
                     * TODO: FIXED ME, the mongodb died that causes morphia is not able to resume
                     */
                    if (e.isNew())
                    {
                        Logger.fatal("EventProcess: morphia got stuck, please restart system");
                        break;
                    }
                    EventReport.process(e);
                    if (e instanceof UnprocessedVcaEvent)
                    {
                        e.delete();
                    }
                }
                catch (DeviceNotExistsException ex)
                {
                    Logger.warn("%s, remove unprocessed event: %s", ex.getMessage(), e.getIdAsStr());
                    e.delete();
                }
                catch (Exception ex)
                {
                    Logger.error(ex, "error occurred during processing event, info: %s, device-id:'%s', channel-id'%s' ",
                            e.getEventInfo(),
                            e.getEventInfo().getCamera().getCoreDeviceId(),
                            e.getEventInfo().getCamera().getChannelId());
                }
            }
        }
    }
    
    

    public static class NoAvailableSpaceException extends Exception
    {
        public NoAvailableSpaceException()
        {
            super();
            // TODO Auto-generated constructor stub
        }

        public NoAvailableSpaceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
        {
            super(message, cause, enableSuppression, writableStackTrace);
            // TODO Auto-generated constructor stub
        }

        public NoAvailableSpaceException(String message, Throwable cause)
        {
            super(message, cause);
            // TODO Auto-generated constructor stub
        }

        public NoAvailableSpaceException(String message)
        {
            super(message);
            // TODO Auto-generated constructor stub
        }

        public NoAvailableSpaceException(Throwable cause)
        {
            super(cause);
            // TODO Auto-generated constructor stub
        }
    }
}
