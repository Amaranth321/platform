package controllers.api;

import com.google.gson.Gson;
import controllers.interceptors.APIInterceptor;
import jobs.queries.QueryPrecompiledNodeInfo;
import lib.util.exceptions.ApiException;
import models.abstracts.ServerPagedResult;
import models.cloud.CloudServerConfigs;
import models.cloud.UIConfigurableCloudSettings;
import models.node.PrecompiledNodeInfo;
import platform.Environment;
import play.mvc.Before;
import play.mvc.With;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * These are for managing the cloud server by superadmin only.
 * <p/>
 * DO NOT expose them in the API documentation
 *
 * @author Aye Maung
 */
@With(APIInterceptor.class)
public class CloudServerManagement extends APIController
{
    @Before(priority = 2)
    public static void checkAccess()
    {
        try
        {
            Long currentBucketId = Long.parseLong(renderArgs.get("caller-bucket-id").toString());
            if (Environment.getInstance().onCloud() && currentBucketId != 1L)
            {
                throw new ApiException(request.actionMethod + " can only be called by superadmin bucket on cloud");
            }
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void searchregisterednodes()
    {
        try
        {
            String sortBy = readApiParameter("sort-by", false);
            String searchTerm = readApiParameter("search-term", false);
            String platformDeviceId = readApiParameter("platform-device-id", false);
            String coreDeviceId = readApiParameter("core-device-id", false);
            String skip = readApiParameter("skip", false);
            String take = readApiParameter("take", false);
            String nodeOffline = readApiParameter("node-offline", false);
            String cameraOffline = readApiParameter("camera-offline", false);
            String vcaUnstable = readApiParameter("vca-unstable", false);
            String updateAvailable = readApiParameter("update-available", false);

            QueryPrecompiledNodeInfo query = new QueryPrecompiledNodeInfo(sortBy);

            if (!searchTerm.isEmpty())
            {
                query.keyword(searchTerm);
            }
            if (!platformDeviceId.isEmpty())
            {
                query.platformDeviceId(asLong(platformDeviceId));
            }
            if (!coreDeviceId.isEmpty())
            {
                query.coreDeviceId(coreDeviceId);
            }
            if (!skip.isEmpty())
            {
                query.offset(asInt(skip));
            }
            if (!take.isEmpty())
            {
                query.limit(asInt(take));
            }
            if (!nodeOffline.isEmpty() && asBoolean(nodeOffline))
            {
                query.nodeOffline();
            }
            if (!cameraOffline.isEmpty() && asBoolean(cameraOffline))
            {
                query.containsOfflineCamera();
            }
            if (!vcaUnstable.isEmpty() && asBoolean(vcaUnstable))
            {
                query.containsUnstableVca();
            }
            if (!updateAvailable.isEmpty() && asBoolean(updateAvailable))
            {
                query.updateAvailable();
            }

            ServerPagedResult<PrecompiledNodeInfo> pagedResult = await(query.now());
            List resultsForOnePage = pagedResult.getResultsForOnePage();

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("total-count", pagedResult.getTotalCount());
            responseMap.put("nodes", resultsForOnePage);
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void getserverconfigurations()
    {
        try
        {
            respondOK("configurations", UIConfigurableCloudSettings.server());
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void updateserverconfigurations()
    {
        try
        {
            String jsonConfigs = readApiParameter("server-configs", true);

            CloudServerConfigs serverConfigs = new Gson().fromJson(jsonConfigs, CloudServerConfigs.class);
            UIConfigurableCloudSettings.updateServerConfigs(serverConfigs);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}

