package controllers.api;

import controllers.interceptors.APIInterceptor;
import lib.util.ResultMap;
import lib.util.exceptions.ApiException;
import platform.AnnouncementManager;
import play.Logger;
import play.mvc.With;

import java.util.List;
import java.util.Map;

/**
 * @author KAI Square
 * @sectiontitle Announcements
 * @sectiondesc APIs for accessing and managing public announcements.
 * @publicapi
 */

@With(APIInterceptor.class)
public class AnnouncementController extends APIController
{


    /**
     * @param include-global Optional parameter "true" or "false" to indicate whether result should include
     *                       global announcements along with those for request's domain, or not. The parameter
     *                       defaults to false if not present.
     *
     * @servtitle Returns list of all announcement. This API does not require session-key.
     * @httpmethod GET
     * @uri /api/{bucket}/getannouncementlist
     * @responsejson {
     * "result": "ok",
     * "announcements": [
     * {
     * "description": "public APIs documentation added",
     * "type": "news"
     * },
     * {
     * "description": "v4 have been released, please update your software",
     * "type": "critical"
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void getannouncementlist(String bucket) throws ApiException
    {
        try
        {
            //since v4.2, we support server sharing among multiple domains, but announcements are
            //domain specific
            boolean includeGlobal = Boolean.parseBoolean(readApiParameter("include-global", false));
            String domain = request.domain;
            List<models.Announcement> resultset =
                    AnnouncementManager.getInstance().getAnnouncementListForDomain(domain, includeGlobal);
            Map<String, Object> map = new ResultMap();
            map.put("result", "ok");
            map.put("announcements", resultset);
            renderJSON(map);

        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));

            Map<String, Object> map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);
        }
    }

    /**
     * @param announcement-type The type of announcement e.g News/Critical
     * @param description       The description of announcement
     * @param domain            The description of announcement
     *
     * @servtitle Add announcement
     * @httpmethod POST
     * @uri /api/{bucket}/addannouncement
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void addannouncement(String bucket) throws ApiException
    {
        try
        {
            String type = readApiParameter("announcement-type", true);
            String description = readApiParameter("description", true);
            String domain = readApiParameter("domain", false);
            Logger.warn("domain is %s", domain);

            AnnouncementManager.getInstance().addAnnouncement(type, description, domain);
            Map<String, Object> map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);

        }
        catch (ApiException apie)
        {
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", apie.getMessage());
            renderJSON(map);
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);
        }
    }

    /**
     * @param announcement-id   The id of the announcement
     * @param announcement-type The type of announcement e.g News/Critical
     * @param description       The description of announcement
     *
     * @servtitle Update announcement
     * @httpmethod POST
     * @uri /api/{bucket}/updateannouncement
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void updateannouncement(String bucket) throws ApiException
    {
        try
        {
            String id = readApiParameter("announcement-id", true);
            String type = readApiParameter("announcement-type", true);
            String description = readApiParameter("description", true);
            String domain = readApiParameter("domain", false);

            AnnouncementManager.getInstance().updateAnnouncement(id, type, description, domain);
            Map<String, Object> map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);

        }
        catch (ApiException apie)
        {
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", apie.getMessage());
            renderJSON(map);
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);
        }
    }

    /**
     * @param announcement-id The id of the announcement
     *
     * @servtitle Remove announcement
     * @httpmethod POST
     * @uri /api/{bucket}/removeannouncement
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void removeannouncement(String bucket) throws ApiException
    {
        try
        {
            String id = readApiParameter("announcement-id", true);
            Boolean status = AnnouncementManager.getInstance().removeAnnouncement(id);
            Map<String, Object> map = new ResultMap();
            map.put("result", "ok");
            map.put("status", status);
            renderJSON(map);

        }
        catch (ApiException apie)
        {
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", apie.getMessage());
            renderJSON(map);
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);
        }
    }
}
