package platform.api.progress;

import platform.api.AsyncAPITask;
import play.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class ProgressToken
{
    private final String identifier;
    private final long userId;
    private final String api;
    private final AtomicInteger percent;
    private String currentTask;
    private boolean terminated;

    public ProgressToken(AsyncAPITask task)
    {
        this.identifier = UUID.randomUUID().toString();
        this.userId = task.getUserId();
        this.api = task.getApiName();
        this.percent = new AtomicInteger(0);

        task.addProgressListener(new ProgressListener()
        {
            @Override
            public void taskChanged(String newTask)
            {
                currentTask = newTask;
            }

            @Override
            public void percentChanged(int newPercent)
            {
                percent.getAndSet(newPercent);
            }

            @Override
            public void terminated(boolean withError)
            {
                if (!withError)
                {
                    percent.getAndSet(100);
                }
                else
                {
                    Logger.info("'%s' terminated with error (%s:%s)", currentTask, userId, api);
                }
                terminated = true;
            }
        });
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof ProgressToken)
        {
            ProgressToken other = (ProgressToken) o;
            return other.getIdentifier().equals(this.getIdentifier());
        }

        return false;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public long getUserId()
    {
        return userId;
    }

    public String getApi()
    {
        return api;
    }

    public String getCurrentTask()
    {
        return currentTask;
    }

    public int getPercent()
    {
        return percent.get();
    }

    public boolean isTerminated()
    {
        return terminated;
    }

    public Map toAPIOutput()
    {
        Map map = new LinkedHashMap();
        map.put("userId", userId);
        map.put("api", api);
        map.put("currentTask", currentTask);
        map.put("percent", percent.get());
        return map;
    }

}
