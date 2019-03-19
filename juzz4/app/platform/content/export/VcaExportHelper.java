package platform.content.export;

import models.Analytics.TickerReport;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.Environment;
import platform.analytics.VcaDataRequest;
import platform.analytics.aggregation.AggregateOperator;
import platform.analytics.aggregation.AggregateType;
import platform.analytics.aggregation.AggregatedTicker;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.db.cache.proxies.CachedNodeCamera;
import platform.devices.DeviceChannelPair;
import platform.devices.DeviceGroup;
import platform.events.EventType;
import platform.reports.*;
import platform.time.UtcPeriod;
import play.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @author Keith
 * @since v4.4
 */
public final class VcaExportHelper
{
    private static final String EXPORTED_TIME_FIELD_FORMAT = "dd/MM/yyyy HH:mm:ss";

    private VcaExportHelper()
    {
        //utility class
    }

    public static LinkedHashMap<String, Object> asExportData(TickerReport tickerReport)
    {
        //common fields
        LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
        EventType eventType = tickerReport.getType();
        switch (eventType)
        {
            case VCA_TRAFFIC_FLOW:
                TrafficFlowAnalyticsReport.TrafficFlowReport tf =
                        (TrafficFlowAnalyticsReport.TrafficFlowReport) tickerReport;
                dataMap.put("region-from", tf.from);
                dataMap.put("region-to", tf.to);
                dataMap.put("count", tf.count);
                return dataMap;

            case VCA_PEOPLE_COUNTING:
                PeopleCountingAnalyticsReport.PeopleCountingReport pc =
                        (PeopleCountingAnalyticsReport.PeopleCountingReport) tickerReport;
                long adjustedAvg = pc.avgOccupancy < 0 ? 0 : Math.round(pc.avgOccupancy);

                dataMap.put("in", pc.in);
                dataMap.put("avg-occupancy", adjustedAvg);
                return dataMap;

            case VCA_PASSERBY:
                PasserbyAnalyticsReport.PasserbyReport pbr = (PasserbyAnalyticsReport.PasserbyReport) tickerReport;
                dataMap.put("no-of-passerbys", pbr.in + pbr.out);
                return dataMap;

            case VCA_CROWD_DETECTION:
                CrowdDensityAnalyticsReport.CrowdDensityReport cd =
                        (CrowdDensityAnalyticsReport.CrowdDensityReport) tickerReport;

                int activityCount = 0;
                for (AnalyticsTracksData.Track track : cd.tracks)
                {
                    activityCount += track.value;
                }
                dataMap.put("activities", activityCount);
                return dataMap;

            case VCA_PROFILING:
                AudienceProfilingAnalyticsReport.AudienceProfilingReport ap =
                        (AudienceProfilingAnalyticsReport.AudienceProfilingReport) tickerReport;
                dataMap.put("male", ap.male);
                dataMap.put("female", ap.female);
                dataMap.put("below-21", ap.age1);
                dataMap.put("21-to-35", ap.age2);
                dataMap.put("36-to-55", ap.age3);
                dataMap.put("above-55", ap.age4);
                dataMap.put("happy", ap.happy);
                dataMap.put("neutral", ap.neutral);
                dataMap.put("angry", ap.angry);
                dataMap.put("number-of-faces", ap.count);
                dataMap.put("under-5s", ap.dur0_5s);
                dataMap.put("5-to-10s", ap.dur5_10s);
                dataMap.put("10-to-20s", ap.dur10_20s);
                dataMap.put("20-to-30s", ap.dur20_30s);
                dataMap.put("30-to-60s", ap.dur30_60s);
                dataMap.put("1-to-3m", ap.dur1_3m);
                dataMap.put("3-to-5m", ap.dur3_5m);
                dataMap.put("5-to-8m", ap.dur5_8m);
                dataMap.put("8-to-10m", ap.dur8_10m);
                dataMap.put("10-to-15m", ap.dur10_15m);
                dataMap.put("15-to-30m", ap.dur15_30m);
                return dataMap;

            case VCA_INTRUSION:
            case VCA_PERIMETER_DEFENSE:
            case VCA_LOITERING:
            case VCA_OBJECT_COUNTING:
            case VCA_VIDEO_BLUR:
            case VCA_FACE_INDEXING:
                dataMap.put("count", tickerReport.count);
                return dataMap;

            default:
                throw new UnsupportedOperationException(eventType.toString());
        }
    }

    /**
     * Note:
     * Aggregated results will be different based on the time zone offset.
     * As such, be careful with date time manipulation.
     *
     * @param deviceGroup   Device group (This should be a meaningful grouping. e.g. by label).
     *                      Aggregation will simply compile all data from the camera list in that group.
     * @param eventType     event type
     * @param period        period in utc
     * @param tzOffsetMins  time zone offset to be used for aggregation
     * @param aggregateType type of aggregation
     * @param operator      aggregation operator
     */
    public static List<AggregatedTicker> aggregateVcaReports(DeviceGroup deviceGroup,
            EventType eventType,
            UtcPeriod period,
            int tzOffsetMins,
            AggregateType aggregateType,
            AggregateOperator operator) {
		VcaDataRequest request = new VcaDataRequest(eventType, period, deviceGroup.getCameraList());

//query data as ascending time
		Iterable<TickerReport> iterable = request.getQuery().fetch();
		long temptime = period.getFromMillis();
		List<TickerReport> reportList = new ArrayList();
		for (TickerReport ticker : iterable) {
			if (ticker.time == temptime) {
				reportList.add(ticker);
				temptime += 3600000;
				continue;
			} else {
				try {
					for (; temptime < ticker.time; temptime += 3600000) {
						TickerReport report = (TickerReport) Class.forName(ticker.getClass().getName()).newInstance();
						report.time = temptime;
						reportList.add(report);
					}
					reportList.add(ticker);
					temptime += 3600000;
				} catch (Exception e) {
					Logger.error(e, "");
					return null;
				}
			}

		}
		if (temptime < period.getToMillis()) {
			try {
				for (; temptime < period.getToMillis(); temptime += 3600000) {
					TickerReport report = (TickerReport) Class.forName(reportList.get(0).getClass().getName())
							.newInstance();
					report.time = temptime;
					report.deviceId = reportList.get(0).deviceId;
					report.deviceId = reportList.get(0).deviceId;
					reportList.add(report);
				}
			} catch (Exception e) {
				Logger.error(e, "");
				return null;
			}
		}

		/**
		 *
		 * Hourly data across different devices in the same group must be summed up
		 * first The operator must be strictly summation
		 *
		 */
		Map<Long, TickerReport> hourlyBins = new LinkedHashMap<>();
//for (TickerReport ticker : iterable)
//{
//if (hourlyBins.get(ticker.time) == null)
//{
//TickerReport clone = ticker.newCopy();
//hourlyBins.put(ticker.time, clone);
//}
//else
//{
//hourlyBins.get(ticker.time).aggregate(ticker, AggregateOperator.SUM, hourlyBins.size());
//}
//}

		for (TickerReport ticker : reportList) {
			if (hourlyBins.get(ticker.time) == null) {
				TickerReport clone = ticker.newCopy();
				hourlyBins.put(ticker.time, clone);
			} else {
				hourlyBins.get(ticker.time).aggregate(ticker, AggregateOperator.SUM, hourlyBins.size());
			}
		}

//already in hourly, just create the wrapper
		if (aggregateType.equals(AggregateType.HOUR)) {
			List<AggregatedTicker> aggTickers = new ArrayList<>();
			for (Long time : hourlyBins.keySet()) {
				AggregatedTicker aggTicker = new AggregatedTicker(time, deviceGroup, eventType);
				aggTicker.addTicker(hourlyBins.get(time), AggregateOperator.SUM);
				aggTickers.add(aggTicker);
				if (aggTicker.getBaseTicker() == null) {
					aggTicker.setBaseTicker(hourlyBins.get(time));
				}
			}
			return aggTickers;
		}

/**
*
* Secondary aggregation for Day/Week/Month.
* specified aggregation operator will be used here
*
*/

//data map to accumulate reports by masked time
Map<String, AggregatedTicker> dataBins = new LinkedHashMap<>();
for (TickerReport tickerReport : hourlyBins.values())
{
String maskedTime = aggregateType.mask(tickerReport.time, tzOffsetMins);
if (!dataBins.containsKey(maskedTime))
{
long groupTime = aggregateType.asMillis(maskedTime, tzOffsetMins);
AggregatedTicker aggTicker = new AggregatedTicker(groupTime, deviceGroup, eventType);
dataBins.put(maskedTime, aggTicker);
}

dataBins.get(maskedTime).addTicker(tickerReport, operator);
}

return new ArrayList<>(dataBins.values());
}


    /**
     * Returns the current time formatted to be appended to exported reports
     */
    public static String getGeneratedTime(int tzOffsetMins)
    {
        return DateTime.now(DateTimeZone.UTC).plusMinutes(tzOffsetMins).toString("ddMMyyyy_HHmmss");
    }

    public static String getLocalTimeStamp(long timeMillis, int tzOffsetMins)
    {
        return new DateTime(timeMillis, DateTimeZone.UTC)
                .plusMinutes(tzOffsetMins)
                .toString(EXPORTED_TIME_FIELD_FORMAT);
    }

    public static String getDeviceName(String coreDeviceId)
    {
        CachedDevice device = CacheClient.getInstance().getDeviceByCoreId(coreDeviceId);
        return device == null ? "N/A" : device.getName();
    }

    public static String getChannelName(String coreDeviceId, String channelId)
    {
        if (Environment.getInstance().onKaiNode())
        {
            return "1"; // only single channel cameras are supported
        }

        DeviceChannelPair idPair = new DeviceChannelPair(coreDeviceId, channelId);
        CachedNodeCamera nodeCam = CacheClient.getInstance().getNodeCamera(idPair);
        return nodeCam == null ? "N/A" : nodeCam.getCameraName();
    }
}
