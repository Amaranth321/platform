package models.Analytics;

import org.joda.time.DateTime;
import platform.analytics.aggregation.AggregateOperator;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.db.cache.proxies.CachedNodeObjectInfo;
import platform.events.EventType;
import platform.reports.AudienceProfilingAnalyticsReport.AudienceProfilingReport;
import platform.reports.CameraTamperingAnalyticsReport.CameraTamperingReport;
import platform.reports.CrowdDensityAnalyticsReport.CrowdDensityReport;
import platform.reports.FaceIndexingAnalyticsReport.FaceIndexingReport;
import platform.reports.IntrusionAnalyticsReport.IntrusionReport;
import platform.reports.LoiteringAnalyticsReport.LoiteringReport;
import platform.reports.ObjectDetectionAnalyticsReport.ObjectDetectionReport;
import platform.reports.PasserbyAnalyticsReport;
import platform.reports.PeopleCountingAnalyticsReport.PeopleCountingReport;
import platform.reports.PerimeterAnalyticsReport.PerimeterReport;
import platform.reports.TrafficFlowAnalyticsReport.TrafficFlowReport;
import platform.reports.TripWireCountingAnalyticsReport.TripWireReport;
import play.Logger;
import play.modules.morphia.Model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class TickerReport extends Model
{
    /**
     * Device id
     */
    public String deviceId;
    /**
     * Device channel id
     */
    public String channelId;
    /**
     * total of records
     */
    public long count;
    /**
     * Date of this record, format: yyyy/MM/dd HH:mm:ss
     */
    public String date;
    /**
     * Unix timestamp of this record
     */
    public long time;
    /**
     * the hour in 24-hour format
     */
    public int hour;

    /**
     * Convert the UTC timestamp to the local time zone of the node, without persisting the change to DB.
     */
    public void localizeTimestamp()
    {
        // Determine the timezone of the node from the deviceId
        CachedDevice device = CacheClient.getInstance().getDeviceByCoreId(deviceId);
        if (device == null)
        {
            return;
        }
        CachedNodeObjectInfo node = CacheClient.getInstance().getNodeObject(device);
        int nodeTzOffsetMins = node.getSettings().getTzOffsetMins();

        // Convert the ticker timestamp to the node's timezone
        DateTime localizedDate = new DateTime(time).plusMinutes(nodeTzOffsetMins);
        try
        {
            date = localizedDate.toString("yyyy/MM/dd HH:mm:ss");
        }
        catch (IllegalArgumentException e)
        {
            Logger.error(e, "Invalid format for parsing date in localizeTimestamp");
        }
    }

    /**
     * Get the event type of a ticker object.
     * Because ticker object itself does not have the event type as a member, so have to be determined based on the
     * ticker object's class.
     *
     * @return The event type of the ticker.
     */
    public EventType getType()
    {
        if (this instanceof PeopleCountingReport)
        {
            return EventType.VCA_PEOPLE_COUNTING;
        }
        if (this instanceof PasserbyAnalyticsReport.PasserbyReport)
        {
            return EventType.VCA_PASSERBY;
        }
        else if (this instanceof AudienceProfilingReport)
        {
            return EventType.VCA_PROFILING;
        }
        else if (this instanceof CrowdDensityReport)
        {
            return EventType.VCA_CROWD_DETECTION;
        }
        else if (this instanceof TrafficFlowReport)
        {
            return EventType.VCA_TRAFFIC_FLOW;
        }
        else if (this instanceof IntrusionReport)
        {
            return EventType.VCA_INTRUSION;
        }
        else if (this instanceof PerimeterReport)
        {
            return EventType.VCA_PERIMETER_DEFENSE;
        }
        else if (this instanceof LoiteringReport)
        {
            return EventType.VCA_LOITERING;
        }
        else if (this instanceof CameraTamperingReport)
        {
            return EventType.VCA_VIDEO_BLUR;
        }
        else if (this instanceof TripWireReport)
        {
            return EventType.VCA_OBJECT_COUNTING;
        }
        else if (this instanceof FaceIndexingReport)
        {
            return EventType.VCA_FACE_INDEXING;
        }
        else if(this instanceof ObjectDetectionReport) 
        {
        	return EventType.VCA_OBJECT_DETECTION;
        }
        else
        {
            return EventType.UNKNOWN;
        }
    }

    /**
     * Used for data aggregation.
     * The base implementation only adds up "count" field.
     * Inherited classes with additional fields MUST OVERRIDE this function.
     * <p/>
     * Notes:
     * <p/>
     * The function returns this class itself.
     * So, data from 'other' should be added to 'this', not the other way.
     * Inherited classes MUST follow this convention.
     * <p/>
     * If aggregation is not applicable for a particular ticker report,
     * override this function and throw {@link java.lang.UnsupportedOperationException}
     *
     * @param other     another ticker to be added
     * @param operator  operator
     * @param hourCount total number of tickers already added previously.
     *                  Use this value to aggregate average fields
     */
    public TickerReport aggregate(TickerReport other,
                                  AggregateOperator operator,
                                  int hourCount)
    {
        switch (operator)
        {
            case SUM:
                this.count += other.count;
                return this;

            default:
                throw new UnsupportedOperationException(operator.name());
        }
    }

    /**
     * Changing original ticker fields will be "remembered" even if save() is not called.
     * That will mess up subsequent operations.
     * <p/>
     * Use this function to get a new copy and modify it instead
     */
    public TickerReport newCopy()
    {
        try
        {
            Class<? extends TickerReport> clazz = this.getClass();
            TickerReport copy = clazz.newInstance();
            for (Field field : clazz.getFields())
            {
                if (Modifier.isStatic(field.getModifiers()))
                {
                    continue;
                }

                Field copyField = clazz.getField(field.getName());
                field.setAccessible(true);
                copyField.setAccessible(true);
                copyField.set(copy, field.get(this));
            }
            return copy;
        }
        catch (Exception e)
        {
            Logger.error("[%s] newCopy failed", this.getClass().getSimpleName());
            throw new RuntimeException(e);
        }
    }
}
