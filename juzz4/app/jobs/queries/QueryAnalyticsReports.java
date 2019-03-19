package jobs.queries;

import com.google.code.morphia.query.Query;
import com.google.gson.JsonSerializer;
import lib.util.exceptions.ApiException;
import models.Analytics.TickerReport;
import models.abstracts.ServerPagedResult;
import platform.devices.DeviceChannelPair;
import platform.events.EventType;
import platform.reports.AnalyticsReport;
import platform.reports.EventReport;
import platform.reports.ReportQuery;
import platform.time.UtcPeriod;
import play.jobs.Job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class QueryAnalyticsReports extends Job<ServerPagedResult<TickerReport>>
{
    private final Query query;
    private final int offset;
    private final int limit;
    private final boolean returnEmptyDataSet;
    private final JsonSerializer[] serializers;

    /**
     * @param eventType             event type
     * @param period                period
     * @param cameraList            empty data set will be returned if this is empty
     * @param additionalQueryParams Refer to {@link ReportQuery#setParameter(String, Object)}
     * @param latestFirst           true to sort the results in the ascending time order
     * @param offset                results' starting index (for paging)
     * @param limit                 zero will remove the max return count limit
     */
    public QueryAnalyticsReports(EventType eventType,
                                 UtcPeriod period,
                                 List<DeviceChannelPair> cameraList,
                                 Map<String, Object> additionalQueryParams,
                                 boolean latestFirst,
                                 int offset,
                                 int limit) throws ApiException
    {
        //Validate inputs
        if (eventType == null)
        {
            throw new ApiException("missing-event-type");
        }

        if (period == null)
        {
            throw new ApiException("missing-period");
        }

        AnalyticsReport report = EventReport.getReport(eventType);
        if (report == null)
        {
            throw new ApiException("invalid-event-type");
        }


        //period
        ReportQuery reportQuery = report.query(period.getFromTime().toDate(), period.getToTime().toDate());

        //cameras
        if (cameraList == null || cameraList.isEmpty())
        {
            returnEmptyDataSet = true;
        }
        else
        {
            returnEmptyDataSet = false;
            reportQuery.addDevice(cameraList);
        }

        //additional parameters for reports
        if (additionalQueryParams != null && !additionalQueryParams.isEmpty())
        {
            for (String key : additionalQueryParams.keySet())
            {
                reportQuery.setParameter(key, additionalQueryParams.get(key));
            }
        }

        query = reportQuery.getQuery();
        serializers = reportQuery.getJsonSerializers();
        this.offset = offset;
        this.limit = limit;

        queryOrder(latestFirst, eventType);
    }

    @Override
    public ServerPagedResult<TickerReport> doJobWithResult()
    {
        ServerPagedResult<TickerReport> pagedResult = new ServerPagedResult<>();

        if (returnEmptyDataSet)
        {
            pagedResult.setTotalCount(0);
            pagedResult.setResultsForOnePage(new ArrayList<TickerReport>());
            return pagedResult;
        }

        //set total
        pagedResult.setTotalCount(query.countAll());

        //filter
        if (offset > 0)
        {
            query.offset(offset);
        }
        if (limit > 0)
        {
            query.limit(limit);
        }

        pagedResult.setResultsForOnePage(query.asList());

        return pagedResult;
    }

    public JsonSerializer[] getJsonSerializers()
    {
        if (serializers == null)
        {
            return new JsonSerializer[0];
        }
        return serializers;
    }

    private void queryOrder(boolean latestFirst, EventType eventType)
    {
        String timeSort = "time";

        if (latestFirst)
        {
            timeSort = "-time";
        }

        switch (eventType)
        {
            case VCA_TRAFFIC_FLOW:
                query.order(String.format("%s ,from, to", timeSort));
                break;
            default:
                query.order(timeSort);
                break;
        }

    }
}
