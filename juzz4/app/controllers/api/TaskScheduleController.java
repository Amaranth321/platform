package controllers.api;

import com.google.gson.Gson;
import com.kaisquare.sync.CommandType;
import controllers.interceptors.APIInterceptor;
import jobs.queries.QueryPrecompiledNodeInfo;
import lib.util.exceptions.ApiException;
import models.MongoBucket;
import models.MongoDevice;
import models.abstracts.ServerPagedResult;
import models.command.cloud.ScheduledCommand;
import models.node.PrecompiledNodeInfo;
import play.mvc.With;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Keith
 * @since v4.6
 * @sectiondesc APIs for managing schedule task for sending command to Nodes
 * @publicapi
 */
@With(APIInterceptor.class)
public class TaskScheduleController extends APIController
{
    /**
     * @param scheduled-time    Time of execution (eg: ddMMyyyyHHmmss). Mandatory
     * @param node-list         The list of selection nodes for execution. Mandatory
     *                          format: Array of String with cloudPlatformDeviceId
     *                          eg: ["1","2","3"]
     *
     * @servtitle assign new schedule for batch OTA node updates
     * @httpmethod POST
     * @uri /api/{bucket}/schedulenodeupdates
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "failed",
     * "reason": "invalid-command-type"
     * }
     * @responsejson {
     * "result": "failed",
     * "reason": "no-node-selected"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void schedulenodeupdates(String id) throws ApiException
    {
        try
        {
            MongoBucket callerBucket = MongoBucket.getById(getCallerBucketId());
            long scheduledTime = toMilliseconds(readApiParameter("scheduled-time", true));
            String nodeListJson = readApiParameter("node-list", true);

            List<String> nodeList = new ArrayList<>();
            nodeList = new Gson().fromJson(nodeListJson, nodeList.getClass());
            if(nodeList.isEmpty())
            {
                throw new ApiException("no-node-selected");
            }

            //device access check
            for(String nodePlatformId : nodeList)
            {
                MongoDevice device = MongoDevice.getByPlatformId(nodePlatformId);

                if (device == null)
                {
                    throw new ApiException("invalid-device-id");
                }
                if (!callerBucket.hasControlOver(MongoBucket.getById(device.getBucketId())))
                {
                    throw new ApiException("device-doesnot-belong-bucket");
                }
            }

            ScheduledCommand.createNew(getCallerBucketId(),
                                       CommandType.CLOUD_UPDATE_NODE,
                                       scheduledTime,
                                       nodeList,
                                       new ArrayList<String>());

            respondOK();
        }
        catch(Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param schedule-id       The scheduled task id. Mandatory
     *
     * @servtitle delete scheduled Batch OTA Updates
     * @httpmethod POST
     * @uri /api/{bucket}/deletenodeupdateschedule
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "failed",
     * "reason": "no-schedule-found"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void deletenodeupdateschedule(String id) throws ApiException
    {
        try
        {
            String callerBucket = getCallerBucketId();
            String scheduleId = readApiParameter("schedule-id", true);
            ScheduledCommand command = ScheduledCommand.findById(scheduleId);
            if (command == null || !command.getBucketId().equals(callerBucket))
            {
                throw new ApiException("no-schedule-found");
            }
            command.delete();
            respondOK();
        }
        catch(Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param from              Time start of execution (eg: ddMMyyyyHHmmss). Optional
     * @param to                Time end of execution (eg: ddMMyyyyHHmmss). Optional
     *
     * @servtitle list all the scheduled batch OTA Nodes in the given time range.
     * @httpmethod POST
     * @uri /api/{bucket}/listnodeupdateschedules
     * @responsejson {
     * "result": "ok"
     * "tasks": []  //{@link ScheduledCommand}
     * }
     * }
     * @responsejson {
     * "result": "failed",
     * "reason": "invalid-command-type"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void listnodeupdateschedules(String id) throws ApiException
    {
        try
        {
            String callerBucket = getCallerBucketId();
            String fromStr = readApiParameter("from", false);
            String toStr = readApiParameter("to", false);
            long from = 0, to = 0;

            if(!fromStr.isEmpty())
            {
                from = toMilliseconds(fromStr);
            }

            if (!toStr.isEmpty())
            {
                to = toMilliseconds(toStr);
            }

            List<ScheduledCommand> commands = ScheduledCommand.findByTimeRange(callerBucket,
                                                                               CommandType.CLOUD_UPDATE_NODE,
                                                                               from,
                                                                               to);
            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("tasks", commands);
            renderJSON(responseMap);
        }
        catch(Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param schedule-id       The scheduled task id. Mandatory
     *
     * @servtitle get paticular scheduled Batch OTA Nodes detail info
     * @httpmethod POST
     * @uri /api/{bucket}/getnodeupdateschedule
     * @responsejson {
     * "result": "ok"
     * "nodes" :[], //{@link PrecompiledNodeInfo}
     * "taskInfo": {}  //{@link ScheduledCommand}
     * }
     * @responsejson {
     * "result": "failed",
     * "reason": "no-schedule-found"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getnodeupdateschedule(String id) throws ApiException
    {
        try
        {
            MongoBucket callerBucket = MongoBucket.getById(getCallerBucketId());
            String scheduleId = readApiParameter("schedule-id", true);

            ScheduledCommand command = ScheduledCommand.findById(scheduleId);
            if (command == null || !callerBucket.hasControlOver(MongoBucket.getById(command.getBucketId())))
            {
                throw new ApiException("no-schedule-found");
            }

            QueryPrecompiledNodeInfo query = new QueryPrecompiledNodeInfo("");
            List<Long> nodeIDs = new ArrayList<Long>();
            for(String platformDeviceId : command.getScheduledNodeIds())
            {
                nodeIDs.add(Long.parseLong(platformDeviceId));
            }
            query.platformDeviceIdIn(nodeIDs);

            ServerPagedResult<PrecompiledNodeInfo> pagedResult = await(query.now());
            List resultsForOnePage = pagedResult.getResultsForOnePage();

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("taskInfo", command);
            responseMap.put("nodes", resultsForOnePage);
            renderJSON(responseMap);
        }
        catch(Exception e)
        {
            respondError(e);
        }
    }
}
