package platform;

import com.kaisquare.events.thrift.EventDetails;
import com.kaisquare.platform.thrift.EventService;
import org.apache.thrift.TException;
import platform.mq.EventMessageQueue;
import play.Logger;

import java.io.IOException;


public class EventServiceImpl implements EventService.Iface
{
    private volatile static EventMessageQueue mq;
    private static final Object lock = new Object();

    public EventServiceImpl()
    {
        try
        {
            openMQ();
        }
        catch (IOException e)
        {
            Logger.error(e, "error opening event queue");
        }
    }

    @Override
    public boolean pushEvent(String eventId, EventDetails details) throws TException {
    	if (mq != null && mq.isOpen())
    	{
    		try {
				mq.publish(details);
			} catch (IOException e) {
				Logger.error(e, "could not publish message to queue");
				closeMQ();
				return false;
			} catch (Exception e) {
				Logger.error(e, "unable to push event");
				closeMQ();
				return false;
			}
    	}
    	else
    	{
    		try {
				openMQ();
			} catch (IOException e) {
				Logger.error(e, "error opening queue");
				return false;
			}
    	}
        return true;
    }

	private static void openMQ() throws IOException
	{
		synchronized (lock)
		{
			if (mq != null)
				mq.close();
			else
				mq = new EventMessageQueue();
			
			try {
				mq.open();
			} catch (IOException e) {
				try { mq.close(); } catch (Exception e1) {}
				mq = null;
				
				throw e;
			}
		}
	}
	
	private static void closeMQ()
	{
		synchronized (lock)
		{
			try {
				if (mq != null)
					mq.close();
			} catch (Exception e) {
				Logger.warn("unable to close MQ: %s", e.getMessage());
			} finally {
				mq = null;
			}
		}
	}

}
