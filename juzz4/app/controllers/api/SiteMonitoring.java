package controllers.api;

import com.kaisquare.transports.KupMonitorClient;
import com.kaisquare.transports.monitoring.Node;
import controllers.interceptors.APIInterceptor;
import jobs.queries.QueryNodesForMonitoringServer;
import lib.util.exceptions.ApiException;
import models.MongoBucket;
import models.abstracts.ServerPagedResult;
import platform.access.DefaultBucket;
import play.mvc.Before;
import play.mvc.With;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * These APIs are to be used by the monitoring server only.
 * <p>
 * DO NOT add/modify them.
 *
 * @author Aye Maung
 */
@With(APIInterceptor.class)
public class SiteMonitoring extends APIController
{
    @Before(priority = 2)
    private static void verifyCaller()
    {
        try
        {
            String callerBucketId = getCallerBucketId();
            MongoBucket callerBucket = MongoBucket.getById(callerBucketId);
            if (!callerBucket.getName().equals(DefaultBucket.SUPERADMIN.getBucketName()))
            {
                throw new ApiException("must-be-superadmin-bucket");
            }
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    private static void renderJsonWithVersion(Map<String, Object> responseMap)
    {
        responseMap.put("monitor-client-version", KupMonitorClient.JAR_VERSION);
        renderJSON(responseMap);
    }

    public static void fetchnodesformonitoring()
    {
        try
        {
            int offset = asInt(readApiParameter("offset", true));
            int limit = asInt(readApiParameter("limit", true));
            long since = asLong(readApiParameter("since", true));

            if (limit > 50)
            {
                throw new ApiException("error-max-limit-50");
            }

            //limit 'since' to one week
            long twoWeeksAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
            since = since < twoWeeksAgo ? twoWeeksAgo : since;

            //query
            QueryNodesForMonitoringServer query = new QueryNodesForMonitoringServer(offset, limit, since);
            ServerPagedResult<Node> pagedResult = await(query.now());

            Map<String, Object> responseMap = new LinkedHashMap<>();
            responseMap.put("result", "ok");
            responseMap.put("total-count", pagedResult.getTotalCount());
            responseMap.put("nodes", pagedResult.getResultsForOnePage());

            renderJsonWithVersion(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
