package controllers.api;

import controllers.interceptors.APIInterceptor;
import lib.util.exceptions.ApiException;
import models.MigrationLog;
import models.abstracts.ServerPagedResult;
import models.events.*;
import models.notification.SentNotification;
import platform.Environment;
import platform.VersionManager;
import platform.db.QueryHelper;
import platform.db.gridfs.GridFsHelper;
import platform.debug.CommandDebugger;
import platform.debug.PlatformDebugger;
import play.jobs.JobsPlugin;
import play.modules.morphia.Model;
import play.mvc.Before;
import play.mvc.With;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

@With(APIInterceptor.class)
public class Debugging extends APIController
{
    @Before(priority = 2) //APIInterceptor first
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

    public static void getplaystatus()
    {
        try
        {
            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("status", new JobsPlugin().getStatus());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void getcommandqueues()
    {
        try
        {
            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("status", PlatformDebugger.getInstance().getCommandQueuesStatus());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void getcommandlogs()
    {
        try
        {
            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("status", CommandDebugger.getInstance().getPrintedLogs());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void getsynctasksstatus()
    {
        try
        {
            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("status", PlatformDebugger.getInstance().getSyncTasksStatus());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void getdeliveryjobsstatus()
    {
        try
        {
            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("status", PlatformDebugger.getInstance().getDeliveryJobsStatus());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void getrejectedevents()
    {
        try
        {
            int skip = Integer.parseInt(readApiParameter("skip", true));
            int take = Integer.parseInt(readApiParameter("take", true));

            ServerPagedResult<RejectedEvent> pagedResult = RejectedEvent.query(skip, take);

            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("total-count", pagedResult.getTotalCount());
            map.put("events", pagedResult.getResultsForOnePage());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void browseeventvideos()
    {
        try
        {
            int skip = Integer.parseInt(readApiParameter("skip", true));
            int take = Integer.parseInt(readApiParameter("take", true));

            ServerPagedResult<EventVideo> pagedResult = EventVideo.query(skip, take);

            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("total-count", pagedResult.getTotalCount());
            map.put("videos", pagedResult.getResultsForOnePage());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void getunsyncedevents()
    {
        try
        {
            int skip = Integer.parseInt(readApiParameter("skip", true));
            int take = Integer.parseInt(readApiParameter("take", true));

            ServerPagedResult<EventToCloud> pagedResult = EventToCloud.query(skip, take);

            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("total-count", pagedResult.getTotalCount());
            map.put("events", pagedResult.getResultsForOnePage());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void getunsyncedeventvideos()
    {
        try
        {
            int skip = Integer.parseInt(readApiParameter("skip", true));
            int take = Integer.parseInt(readApiParameter("take", true));

            ServerPagedResult<UnsyncedEventVideo> pagedResult = UnsyncedEventVideo.query(skip, take);

            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("total-count", pagedResult.getTotalCount());
            map.put("videos", pagedResult.getResultsForOnePage());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void geteventvideorequests()
    {
        try
        {
            int skip = Integer.parseInt(readApiParameter("skip", true));
            int take = Integer.parseInt(readApiParameter("take", true));

            ServerPagedResult<EventVideoRequest> pagedResult = EventVideoRequest.query(skip, take);

            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("total-count", pagedResult.getTotalCount());
            map.put("requests", pagedResult.getResultsForOnePage());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void deleteeventvideo()
    {
        try
        {
            EventVideo eventVideo = EventVideo.find(readApiParameter("event-id", true));
            if (eventVideo == null)
            {
                throw new ApiException("invalid-event-id");
            }

            boolean result = GridFsHelper.removeFile(eventVideo.getVideoDetails());
            if (!result)
            {
                throw new ApiException("remove grid fs failed");
            }

            eventVideo.delete();

            Map map = new LinkedHashMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void getallnotifications()
    {
        try
        {
            int skip = Integer.parseInt(readApiParameter("skip", true));
            int take = Integer.parseInt(readApiParameter("take", true));

            Model.MorphiaQuery query = SentNotification.q().order("-notificationInfo.eventTime");
            ServerPagedResult<SentNotification> pagedResult = QueryHelper.preparePagedResult(query, skip, take);

            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("total-count", pagedResult.getTotalCount());
            map.put("notifications", pagedResult.getResultsForOnePage());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void getmigrationerrorlogs()
    {
        try
        {
            int skip = Integer.parseInt(readApiParameter("skip", true));
            int take = Integer.parseInt(readApiParameter("take", true));

            ServerPagedResult<MigrationLog> pagedResult = MigrationLog.list(skip, take);

            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("total-count", pagedResult.getTotalCount());
            map.put("errors", pagedResult.getResultsForOnePage());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void getserverstatus()
    {
        try
        {
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);

            long startTime = Environment.getInstance().getServerStartedTime();
            out.println(String.format("Started @ %s", PlatformDebugger.timestamp(startTime)));
            out.println(String.format("Release      : %s", VersionManager.getInstance().getPlatformVersion()));
            out.println(String.format("Version Tag  : %s", VersionManager.getInstance().getReleaseTag()));
            out.println(String.format("Startup done : %s", Environment.getInstance().isStartupTasksCompleted()));
            out.println();

            out.println(PlatformDebugger.getInstance().getMigrationStatus());
            out.println(PlatformDebugger.getInstance().getVcaServerInfo());

            if (Environment.getInstance().onCloud())
            {
                out.println(PlatformDebugger.getInstance().listOTAFiles());
                out.println(PlatformDebugger.getInstance().cloudCoreEngineStatus());
            }

            if (Environment.getInstance().onKaiNode())
            {
                out.println(PlatformDebugger.getInstance().getNodePlatformSettings());
            }

            Map map = new LinkedHashMap();
            map.put("result", "ok");
            map.put("status", sw.toString());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

}
