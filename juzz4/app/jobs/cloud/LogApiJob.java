package jobs.cloud;

import com.google.gson.Gson;
import lib.util.Util;
import models.AuditLog;
import play.Logger;
import play.jobs.Job;
import play.mvc.Http;
import play.mvc.Scope;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Aye Maung
 * @since v4.5
 */
public final class LogApiJob
{
    private static final List<String> ignoredApiList = Arrays.asList(
            "keepalive",
            "getannouncementlist",
            "recvcometnotification"
    );

    private static final LogApiJob instance = new LogApiJob();

    public static void start()
    {
        if (!instance.started)
        {
            instance.started = true;
            instance.job.every(5);
        }
    }

    public static void queue(ApiCall call)
    {
        if (!instance.started || call == null)
        {
            return;
        }

        /**
         * this check is to prevent OOM in case something unexpected happened
         */
        int queueLimit = 1000;
        if (instance.apiCallQueue.size() > queueLimit)
        {
            Logger.error("More than %s items in LogApiJob queue. Stopped audit logging.", queueLimit);
            return;
        }

        instance.apiCallQueue.add(call);
    }

    private boolean started = false;
    private final Queue<ApiCall> apiCallQueue = new ConcurrentLinkedQueue<>();
    private final Job job = new Job()
    {
        @Override
        public void doJob()
        {
            ApiCall nextItem = apiCallQueue.poll();
            while (nextItem != null)
            {
                saveAsAuditLog(nextItem);
                nextItem = apiCallQueue.poll();
            }
        }
    };

    private LogApiJob()
    {
    }

    private void saveAsAuditLog(ApiCall apiCall)
    {
        //content type is needed
        if (Util.isNullOrEmpty(apiCall.contentType))
        {
            return;
        }

        if (ignoredApiList.contains(apiCall.actionMethod))
        {
            return;
        }

        //debugging related
        if (apiCall.action.contains("api.Debugging"))
        {
            return;
        }

        try
        {
            AuditLog auditLog = new AuditLog();
            auditLog.timeobject = apiCall.time;
            auditLog.bucketId = (String) apiCall.renderArgs.get("caller-bucket-id");
            auditLog.bucketName = (String) apiCall.renderArgs.get("bucket");
            auditLog.userId = (String) apiCall.renderArgs.get("caller-user-id");
            auditLog.userName = (String) apiCall.renderArgs.get("username");
            auditLog.serviceName = apiCall.actionMethod;
            auditLog.remoteIp = apiCall.remoteAddress;
            auditLog.headers = apiCall.headers;

            //filter params
            for (String paramKey : apiCall.params.keySet())
            {
                if (paramKey.contains("body"))
                {
                    continue;
                }

                if (paramKey.toLowerCase().contains("password") || paramKey.toLowerCase().contains("session-key"))
                {
                    auditLog.params.put(paramKey, "********");
                    continue;
                }

                auditLog.params.put(paramKey, apiCall.params.get(paramKey));
            }

            //if response is JSON, capture its first 100 characters in audit trail and result status
            if (apiCall.contentType.contains("application/json"))
            {
                try
                {
                    Map responseMap = new Gson().fromJson(apiCall.apiResponseContent, Map.class);
                    if (responseMap.containsKey("result"))
                    {
                        if ("ok".equals(String.valueOf(responseMap.get("result"))))
                        {
                            auditLog.result = "ok";
                        }
                        else
                        {
                            auditLog.result = "failed";
                        }
                    }
                }
                catch (Exception e)
                {
                    Logger.warn("(%s) invalid response.out: %s", apiCall.actionMethod, e.getMessage());
                }
            }

            auditLog.save();
        }
        catch (Exception e)
        {
            Logger.error("logging failed for '%s': %s", apiCall.actionMethod, e.getMessage());
        }
    }

    public static class ApiCall
    {
        private final long time;
        private final Scope.RenderArgs renderArgs;
        private final Map<String, String> params;

        /**
         * from Http.Request
         */
        private final String actionMethod;
        private final String action;
        private final String remoteAddress;
        private final Map<String, Http.Header> headers;

        /**
         * from Http.Response
         */
        private final String contentType;
        private final String apiResponseContent;

        public ApiCall(Http.Request request,
                       Http.Response response,
                       Scope.RenderArgs renderArgs,
                       Map<String, String> params)
        {
            this.time = System.currentTimeMillis();
            this.renderArgs = renderArgs;
            this.params = params;

            this.actionMethod = request.actionMethod;
            this.action = request.action;
            this.remoteAddress = request.remoteAddress;
            this.headers = request.headers;

            this.contentType = response.contentType;
            this.apiResponseContent = response.out.toString();
        }
    }
}
