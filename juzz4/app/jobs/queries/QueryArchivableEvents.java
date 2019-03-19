package jobs.queries;

import com.google.code.morphia.query.CriteriaContainer;
import controllers.api.APIController;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.abstracts.ServerPagedResult;
import models.archived.ArchivedEvent;
import platform.db.QueryHelper;
import platform.devices.DeviceChannelPair;
import platform.events.EventType;
import play.jobs.Job;
import play.modules.morphia.Model;

import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class QueryArchivableEvents extends Job<ServerPagedResult<ArchivedEvent>>
{
    private final Model.MorphiaQuery query = ArchivedEvent.q();
    private int offset = 0;
    private int limit = 0;

    /**
     * - Both empty string and null inputs are accepted
     * - if take is null or zero, query will return all results
     */
    public QueryArchivableEvents(List<EventType> eventTypes,
                                 List<DeviceChannelPair> cameras,
                                 String from,
                                 String to,
                                 String skip,
                                 String take) throws ApiException
    {
        //event types
        if (eventTypes != null && !eventTypes.isEmpty())
        {
            query.filter("eventInfo.type in", eventTypes);
        }

        //cameras
        if (cameras.size() > 0)
        {
            CriteriaContainer[] cameraChecks = new CriteriaContainer[cameras.size()];
            for (int i = 0; i < cameras.size(); i++)
            {
                DeviceChannelPair camera = cameras.get(i);
                if (Util.isNullOrEmpty(camera.getChannelId()))
                {
                    cameraChecks[i] = query.and(
                            query.criteria("eventInfo.camera.coreDeviceId").equal(camera.getCoreDeviceId())
                    );
                }
                else
                {
                    cameraChecks[i] = query.and(
                            query.criteria("eventInfo.camera.coreDeviceId").equal(camera.getCoreDeviceId()),
                            query.criteria("eventInfo.camera.channelId").equal(camera.getChannelId())
                    );
                }
            }
            query.or(cameraChecks);
        }

        if (!Util.isNullOrEmpty(from))
        {
            long fromMillis = APIController.toMilliseconds(from);
            query.filter("eventInfo.time >=", fromMillis);
        }

        if (!Util.isNullOrEmpty(to))
        {
            long toMillis = APIController.toMilliseconds(to);
            query.filter("eventInfo.time <", toMillis);
        }

        query.order("-eventInfo.time");

        /**
         *
         * offset and limit will affect the total count.
         * So, they cannot be in the query until the total count is queried in doJobWithResult()
         *
         */
        if (!Util.isNullOrEmpty(skip) && Util.isInteger(skip))
        {
            offset = Integer.parseInt(skip);
        }
        if (!Util.isNullOrEmpty(take) && Util.isInteger(take))
        {
            limit = Integer.parseInt(take);
        }
    }

    @Override
    public ServerPagedResult<ArchivedEvent> doJobWithResult()
    {
        return QueryHelper.preparePagedResult(query, offset, limit);
    }

}
