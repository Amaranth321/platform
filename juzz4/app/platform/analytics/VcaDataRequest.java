package platform.analytics;

import com.google.code.morphia.query.Query;
import platform.devices.DeviceChannelPair;
import platform.events.EventType;
import platform.reports.AnalyticsReport;
import platform.reports.EventReport;
import platform.reports.ReportQuery;
import platform.time.UtcPeriod;

import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class VcaDataRequest
{
    private final EventType eventType;
    private final UtcPeriod period;
    private final List<DeviceChannelPair> cameraList;

    public VcaDataRequest(EventType eventType,
                          UtcPeriod period,
                          List<DeviceChannelPair> cameraList)
    {
        this.eventType = eventType;
        this.period = period;
        this.cameraList = cameraList;

        //check if eventType is supported
        AnalyticsReport report = EventReport.getReport(eventType);
        if (report == null)
        {
            throw new UnsupportedOperationException(eventType + "is not supported");
        }
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public UtcPeriod getPeriod()
    {
        return period;
    }

    public List<DeviceChannelPair> getCameraList()
    {
        return cameraList;
    }

    /**
     * @return a query based on the request parameters (ascending time)
     */
    public Query getQuery()
    {
        ReportQuery reportQuery = EventReport.getReport(eventType)
                .query(period.getFromTime().toDate(), period.getToTime().toDate())
                .addDevice(cameraList);
        
        switch (eventType)
        {
        	case VCA_TRAFFIC_FLOW:
        		return reportQuery.getQuery().order("time, from, to");
        	default:
        		return reportQuery.getQuery().order("time");
        }
    }

}
