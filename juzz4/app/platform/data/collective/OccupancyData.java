package platform.data.collective;

import platform.devices.DeviceChannelPair;
import play.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class OccupancyData
{
    public static final long DATA_TTL = TimeUnit.HOURS.toMillis(2);

    private final String labelId;

    //occupancies from each camera
    private final Map<DeviceChannelPair, ExpirableOccupancy> occupancyMap;

    //frequency controls
    private int lastNotifiedBreachedLimit;
    private long lastNotifiedTime;
    private boolean droppedBelowLastNotified;
    private boolean droppedBelowAllLimits;

    public OccupancyData(String labelId)
    {
        this.labelId = labelId;
        this.occupancyMap = new LinkedHashMap<>();
    }

    public void update(DeviceChannelPair camera, int newOccupancy)
    {
        ExpirableOccupancy occRecord = occupancyMap.get(camera);
        if (occRecord == null)
        {
            occRecord = new ExpirableOccupancy(DATA_TTL);
        }

        occRecord.setCount(newOccupancy);
        occupancyMap.put(camera, occRecord);
    }

    public String getLabelId()
    {
        return labelId;
    }

    public Map<DeviceChannelPair, ExpirableOccupancy> getOccupancyMap()
    {
        Map<DeviceChannelPair, ExpirableOccupancy> returnList = new LinkedHashMap<>();
        List<DeviceChannelPair> expiredList = new ArrayList<>();
        for (Map.Entry<DeviceChannelPair, ExpirableOccupancy> entry : occupancyMap.entrySet())
        {
            ExpirableOccupancy timedOcc = entry.getValue();
            if (timedOcc.hasExpired())
            {
                expiredList.add(entry.getKey());
                continue;
            }
            returnList.put(entry.getKey(), entry.getValue());
        }

        //remove expired
        for (DeviceChannelPair camera : expiredList)
        {
            occupancyMap.remove(camera);
        }

        return returnList;
    }

    public int getCollectiveOccupancy()
    {
        int collectiveOccupancy = 0;
        for (ExpirableOccupancy expOcc : getOccupancyMap().values())
        {
            collectiveOccupancy += expOcc.getCount();
        }

        return collectiveOccupancy;
    }

    public List<DeviceChannelPair> getCameraList()
    {
        return new ArrayList<>(occupancyMap.keySet());
    }

    public boolean shouldSendNotification(int currentBreachedLimit,
                                          int currentOccupancy,
                                          int minIntervalSeconds)
    {
        //higher than last notified
        if (currentBreachedLimit > lastNotifiedBreachedLimit)
        {
            return true;
        }

        //hitting the first limit again
        if (droppedBelowAllLimits)
        {
            if (tooSoonToNotifyAgain(minIntervalSeconds))
            {
                return false;
            }

            droppedBelowAllLimits = false;
            return true;
        }

        //falls below last notified
        if (currentOccupancy < lastNotifiedBreachedLimit)
        {
            droppedBelowLastNotified = true;
            Logger.info("[%s] dropped below last notified (%s) => %s",
                        labelId,
                        lastNotifiedBreachedLimit,
                        currentOccupancy);
            dropLastNotifiedLimit(currentBreachedLimit, currentOccupancy);
            return false;
        }
        else if (droppedBelowLastNotified && !tooSoonToNotifyAgain(minIntervalSeconds))
        {
            //already-notified limit hit again
            return true;
        }

        return false;
    }

    public void notificationSent(int breachedLimit)
    {
        //update frequency controls
        lastNotifiedBreachedLimit = breachedLimit;
        lastNotifiedTime = System.currentTimeMillis();
        droppedBelowLastNotified = false;
    }

    public void dropsBelowAllLimits(int currentOccupancy)
    {
        if (lastNotifiedBreachedLimit > 0)
        {
            droppedBelowAllLimits = true;
            Logger.info("[%s] dropped below all limits => %s", labelId, currentOccupancy);
        }
    }

    public boolean tooSoonToNotifyAgain(int minIntervalSeconds)
    {
        long elapsedSince = System.currentTimeMillis() - lastNotifiedTime;
        if (TimeUnit.SECONDS.toMillis(minIntervalSeconds) > elapsedSince)
        {
            Logger.info("[%s] too soon to re-notify (%ss remaining)",
                        labelId,
                        minIntervalSeconds - TimeUnit.MILLISECONDS.toSeconds(elapsedSince));
            return true;
        }

        return false;
    }

    //the last notified limit need to drop for trigger future notification
    private void dropLastNotifiedLimit(int currentBreachedLimit, int currentOccupancy)
    {
    	//drop limit 
        if (currentBreachedLimit < lastNotifiedBreachedLimit && 
        		currentOccupancy == currentBreachedLimit)
        {
        	Logger.info("[%s] set last notified limit from %s to %s, current count: %s",
                    labelId,
                    lastNotifiedBreachedLimit,
                    currentBreachedLimit,
                    currentOccupancy);
        	lastNotifiedBreachedLimit = currentBreachedLimit;
        	droppedBelowLastNotified = false;
        }
    }
    
    public static class ExpirableOccupancy
    {
        final long ttl;
        int count;
        long lastUpdated;

        ExpirableOccupancy(long ttl)
        {
            this.ttl = ttl;
        }

        int getCount()
        {
            return count;
        }

        void setCount(int count)
        {
            this.count = count;
            lastUpdated = System.currentTimeMillis();
        }

        boolean hasExpired()
        {
            return System.currentTimeMillis() - lastUpdated > ttl;
        }
    }
}


