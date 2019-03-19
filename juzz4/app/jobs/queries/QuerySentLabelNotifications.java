package jobs.queries;

import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.abstracts.ServerPagedResult;
import models.notification.AckStatus;
import models.notification.SentLabelNotification;
import platform.db.QueryHelper;
import platform.events.EventType;
import platform.time.UtcPeriod;
import play.jobs.Job;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class QuerySentLabelNotifications extends Job<ServerPagedResult<SentLabelNotification>>
{
    private final Model.MorphiaQuery query = SentLabelNotification.q();
    private int offset = 0;
    private int limit = 0;


    public QuerySentLabelNotifications(String eventId)
    {
        query.filter("eventId", eventId);
    }

    /**
     * - userId is compulsory. All other inputs are optional.
     * - Both empty string and null inputs are accepted
     * - if take is null or zero, query will return all results
     */
    public QuerySentLabelNotifications(long userId,
                                       String[] eventTypes,
                                       List<String> userLabelIdList,
                                       UtcPeriod period,
                                       String skip,
                                       String take,
                                       AckStatus ackStatus) throws ApiException
    {

        //userId is compulsory
        if (userId > 0)
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
            query.filter("eventType in", types);
        }

        if (!Util.isNullOrEmpty(userLabelIdList))
        {
            query.filter("labelId in", userLabelIdList);
        }

        //period
        query.filter("eventTime >=", period.getFromMillis());
        query.filter("eventTime <", period.getToMillis());

        //ack status
        if (ackStatus != null)
        {
            query.filter("ackStatus", ackStatus);
        }

        //sort
        query.order("-eventTime");


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
    public ServerPagedResult<SentLabelNotification> doJobWithResult()
    {
        return QueryHelper.preparePagedResult(query, offset, limit);
    }

}
