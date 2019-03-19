package jobs.queries;

import com.google.code.morphia.query.Criteria;
import controllers.api.APIController;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.abstracts.ServerPagedResult;
import models.notification.AckStatus;
import models.notification.SentNotification;
import platform.db.QueryHelper;
import platform.devices.DeviceChannelPair;
import platform.events.EventType;
import play.i18n.Messages;
import play.jobs.Job;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class QuerySentNotifications extends Job<ServerPagedResult<SentNotification>>
{
    private static final int MAX_QUERY_PERIOD_DAYS = 30;

    private final Model.MorphiaQuery query = SentNotification.q();
    private int offset = 0;
    private int limit = 0;

    public QuerySentNotifications(String eventId)
    {
        query.filter("notificationInfo.eventId", eventId);
    }

    /**
     * - userId is compulsory. All other inputs are optional.
     * - Both empty string and null inputs are accepted
     * - if take is null or zero, query will return all results
     */
    public QuerySentNotifications(long userId,
                                  String[] eventTypes,
                                  List<DeviceChannelPair> cameraList,
                                  String from,
                                  String to,
                                  String skip,
                                  String take,
                                  boolean hideAcknowledged) throws ApiException
    {
        //userId is compulsory
        if (userId >= 0)
        {
            query.field("userList").hasThisOne(userId);
        }
        else
        {
            throw new ApiException("invalid-user-id");
        }

        if (eventTypes != null && eventTypes.length > 0)
        {
            List<EventType> types = new ArrayList<>();
            for (String eventType : eventTypes)
            {
                EventType type = EventType.parse(eventType.trim());
                if (type.equals(EventType.UNKNOWN))
                {
                    throw new ApiException("invalid-event-type");
                }
                types.add(type);
            }
            query.filter("notificationInfo.eventType in", types);
        }

        if (cameraList != null && !cameraList.isEmpty())
        {
            ArrayList<Criteria> cameraCriteria = new ArrayList<>();
            for (DeviceChannelPair camera : cameraList)
            {
                if (!Util.isNullOrEmpty(camera.getChannelId()))
                {
                    cameraCriteria.add(query.and(
                            query.criteria("notificationInfo.camera.coreDeviceId").equal(camera.getCoreDeviceId()),
                            query.criteria("notificationInfo.camera.channelId").equal(camera.getChannelId())
                    ));
                }
                else
                {
                    cameraCriteria.add(query.and(
                            query.criteria("notificationInfo.camera.coreDeviceId").equal(camera.getCoreDeviceId())
                    ));
                }
            }
            if (!cameraCriteria.isEmpty())
            {
                query.or(cameraCriteria.toArray(new Criteria[cameraCriteria.size()]));
            }
        }

        //verify period range
        if (Util.isNullOrEmpty(from))
        {
            throw new ApiException("Missing 'from'");
        }
        if (Util.isNullOrEmpty(to))
        {
            throw new ApiException("Missing 'to'");
        }
        long fromMillis = APIController.toMilliseconds(from);
        long toMillis = APIController.toMilliseconds(to);
        if (toMillis - fromMillis > TimeUnit.DAYS.toMillis(MAX_QUERY_PERIOD_DAYS))
        {
            throw new ApiException(Messages.get("error-select-shorter-period", MAX_QUERY_PERIOD_DAYS));
        }

        query.filter("notificationInfo.eventTime >=", fromMillis);
        query.filter("notificationInfo.eventTime <", toMillis);

        if (hideAcknowledged)
        {
            query.filter("ackStatus !=", AckStatus.ACKNOWLEDGED);
        }

        query.order("-notificationInfo.eventTime");


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
    public ServerPagedResult<SentNotification> doJobWithResult()
    {
        return QueryHelper.preparePagedResult(query, offset, limit);
    }

    public Model.MorphiaQuery getQuery()
    {
        return query;
    }

}
