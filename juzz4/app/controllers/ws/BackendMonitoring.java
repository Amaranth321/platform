package controllers.ws;

import com.google.gson.Gson;
import lib.util.Util;
import platform.api.APITaskTracker;
import platform.api.progress.ProgressToken;
import platform.data.collective.OccupancyData;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedOccupancyData;
import platform.rt.OccupancyRTFeed;
import platform.rt.RTFeedManager;
import platform.rt.RTSubscriber;
import platform.rt.VcaChangeFeed;
import play.Logger;

import java.util.*;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class BackendMonitoring extends DefaultWSController
{
    private static final RTFeedManager rtFeedMgr = RTFeedManager.getInstance();

    public static void trackRunningTasks()
    {
        validateSession();
        String recheckFreq = "2s";
        try
        {
            String userId = params.get("user-id");
            String apiList = params.get("api-list");

            //user id
            long callerUserId = getCallerUserId();
            if (!Util.isNullOrEmpty(userId))
            {
                callerUserId = Long.parseLong(userId);
            }

            //api list
            List<String> toTrackList = new ArrayList<>();
            if (!Util.isNullOrEmpty(apiList))
            {
                String[] splitList = apiList.split(",");
                if (splitList.length > 0)
                {
                    toTrackList = Arrays.asList(splitList);
                }
            }

            String lastSent = "";
            while (inbound.isOpen())
            {
                Set<ProgressToken> tokens = APITaskTracker.getInstance().find(callerUserId, toTrackList);
                List<Map> outList = new ArrayList<>();
                for (ProgressToken token : tokens)
                {
                    outList.add(token.toAPIOutput());
                }

                String jsonList = new Gson().toJson(outList);
                if (!lastSent.equals(jsonList))
                {
                    outbound.send(jsonList);
                    lastSent = jsonList;
                }

                await(recheckFreq);
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        disconnect();
    }

    public static void monitorVcaInstanceChange()
    {
        validateSession();
        try
        {
            //subscribe
            long callerUserId = getCallerUserId();
            RTSubscriber<VcaChangeFeed> subscriber = rtFeedMgr.addVcaChangeSubscriber(callerUserId);
            if (subscriber == null)
            {
                throw new Exception(String.format("failed to addVcaChangeSubscriber : %s", callerUserId));
            }

            //listen
            while (inbound.isOpen())
            {
                VcaChangeFeed feed = await(subscriber.getNext(1000));
                if (feed != null)
                {
                    writeToOutbound(feed.toAPIOutput());
                }
            }

            //close
            rtFeedMgr.removeSubscriber(subscriber);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        disconnect();
    }

    public static void monitorOccupancyChange()
    {
        validateSession();
        try
        {
            //subscribe
            long callerUserId = getCallerUserId();
            String labelId = readApiParameter("label-id", true);
            RTSubscriber<OccupancyRTFeed> subscriber = rtFeedMgr.addOccupancySubscriber(callerUserId, labelId);
            if (subscriber == null)
            {
                throw new Exception(String.format("failed to addOccupancySubscriber : %s (%s)", callerUserId, labelId));
            }

            //send last known occupancy
            CachedOccupancyData cachedData = CacheClient.getInstance().getOccupancyData(labelId);
            if (cachedData != null)
            {
                OccupancyData occupancyData = cachedData.getOccupancyData();
                OccupancyRTFeed occFeed = new OccupancyRTFeed(occupancyData);
                writeToOutbound(occFeed);
            }

            //listen
            while (inbound.isOpen())
            {
                OccupancyRTFeed feed = await(subscriber.getNext(1000));
                if (feed != null)
                {
                    writeToOutbound(feed.toAPIOutput());
                }
            }

            //close
            rtFeedMgr.removeSubscriber(subscriber);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        disconnect();
    }
}
