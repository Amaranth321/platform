package platform.db.cache.proxies;

import models.Analytics.TickerReport;
import models.labels.LabelStore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.data.collective.OccupancyData;
import platform.db.cache.CachedObject;
import platform.devices.DeviceChannelPair;
import platform.events.EventType;
import platform.reports.EventReport;
import platform.reports.PeopleCountingAnalyticsReport;
import platform.reports.ReportQuery;
import play.Logger;

import java.util.List;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class CachedOccupancyData extends CachedObject<CachedOccupancyData>
{
    private OccupancyData occupancyData;

    public CachedOccupancyData(String cacheKey, String labelId)
    {
        super(cacheKey);
        this.occupancyData = new OccupancyData(labelId);
        restoreDataFromHourlyReports(labelId);
    }

    @Override
    public CachedOccupancyData getObject()
    {
        return this;
    }

    @Override
    public long getTtlMillis()
    {
        return OccupancyData.DATA_TTL;
    }

    public OccupancyData getOccupancyData()
    {
        return occupancyData;
    }

    public void setOccupancyData(OccupancyData occupancyData)
    {
        this.occupancyData = occupancyData;
    }

    private void restoreDataFromHourlyReports(String labelId)
    {
        LabelStore storeLabel = LabelStore.q().filter("labelId", labelId).first();
        DateTime now = DateTime.now(DateTimeZone.UTC);

        DateTime currentHourStart = now
                .withMinuteOfHour(0)
                .withSecondOfMinute(0)
                .withMillisOfSecond(0);

        //decide whether to take the current hour only or check prev hour also
        if (now.getMinuteOfHour() < 15)
        {
            currentHourStart = currentHourStart.minusHours(1);
        }

        //cameraList
        List<DeviceChannelPair> cameraList = storeLabel.getCameraList();

        ReportQuery reportQuery = EventReport.getReport(EventType.VCA_PEOPLE_COUNTING)
                .query(currentHourStart.toDate(), now.toDate())
                .addDevice(cameraList);

        Iterable<TickerReport> iterable = reportQuery.getQuery().order("time").fetch();
        for (TickerReport tickerReport : iterable)
        {
            PeopleCountingAnalyticsReport.PeopleCountingReport pcr = (PeopleCountingAnalyticsReport.PeopleCountingReport) tickerReport;
            occupancyData.update(new DeviceChannelPair(pcr.deviceId, pcr.channelId), pcr.currentOccupancy);
            Logger.info("[CachedOccupancyData] restored from %s:%s - %s (%s)",
                        pcr.deviceId, pcr.channelId, pcr.currentOccupancy, new DateTime(pcr.time).toString("HH:mm:ss"));
        }
    }
}
