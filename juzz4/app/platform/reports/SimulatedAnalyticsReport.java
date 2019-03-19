package platform.reports;

import com.google.code.morphia.query.Query;
import com.google.gson.Gson;
import lib.util.FixedSizeConcurrentMap;
import lib.util.Util;
import lib.util.exceptions.UnsupportedTypeException;
import models.Analytics.TickerReport;
import models.archived.ArchivedEvent;
import models.node.NodeCamera;
import models.node.NodeObject;
import org.joda.time.DateTime;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.devices.DeviceChannelPair;
import platform.events.EventType;
import platform.time.UtcPeriod;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Use this class in place of ReportQuery to get simulated analytics data.
 * <p/>
 * This works ONLY on cloud
 *
 * @author Aye Maung
 * @since v4.4
 */
public class SimulatedAnalyticsReport extends DefaultReportQuery<TickerReport> implements AnalyticsReport<TickerReport>
{
    private final EventType eventType;

    public SimulatedAnalyticsReport(EventType eventType)
    {
        super(new SimulatedQuery());
        this.eventType = eventType;
    }

    @Override
    public Query<TickerReport> getQuery()
    {
        ((SimulatedQuery) query).setFilters(eventType, devicePairs, from, to);
        return query;
    }

    @Override
    public boolean process(ArchivedEvent event)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReportQuery<TickerReport> query(Date from, Date to)
    {
        super.setDateFrom(from);
        super.setDateTo(to);
        return this;
    }

    @Override
    public void retention(Date from)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean reportExists()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    private static class SimulatedQuery extends DecorativeQuery<TickerReport>
    {
        //save the last few results
        private static FixedSizeConcurrentMap<String, List<TickerReport>> previousResults = new FixedSizeConcurrentMap(50);

        private EventType eventType;
        private List<DeviceChannelPair> cameraList;
        private UtcPeriod period;

        public SimulatedQuery()
        {
            super(null);
        }

        @Override
        public long countAll()
        {
            return asList().size();
        }

        @Override
        public Iterable<TickerReport> fetch()
        {
            return asList();
        }

        @Override
        public List<TickerReport> asList()
        {
            String queryKey = stringifyCurrentQuery();
            if (previousResults.containsKey(queryKey))
            {
                return previousResults.get(queryKey);
            }

            return generateTickers();
        }

        @Override
        public Query<TickerReport> limit(int limit)
        {
            return this;
        }

        @Override
        public Query<TickerReport> offset(int offset)
        {
            return this;
        }

        @Override
        public Query<TickerReport> order(String order)
        {
            return this;
        }

        private void setFilters(EventType eventType,
                                List<DeviceChannelPair> cameraList,
                                Date from,
                                Date to)
        {
            this.eventType = eventType;
            this.cameraList = cameraList;
            this.period = new UtcPeriod(from.getTime(), to.getTime());
        }

        private List<TickerReport> generateTickers()
        {
            String queryKey = stringifyCurrentQuery();
//            Logger.info("Generating tickers %s", queryKey);

            List<TickerReport> tickerList = new ArrayList<>();
            if (cameraList.isEmpty())
            {
                return tickerList;
            }

            DateTime start = period.getFromTime();
            DateTime limit = period.getToTime();
            DateTime current = new DateTime(start);

            while (!current.isAfter(limit))
            {
                TickerReport baseTicker;
                switch (eventType)
                {
                    case VCA_TRAFFIC_FLOW:
                        TrafficFlowAnalyticsReport.TrafficFlowReport tf = new TrafficFlowAnalyticsReport.TrafficFlowReport();
                        tf.from = "R1";
                        tf.to = "R2";
                        baseTicker = tf;
                        break;

                    case VCA_PEOPLE_COUNTING:
                        PeopleCountingAnalyticsReport.PeopleCountingReport pc = new PeopleCountingAnalyticsReport.PeopleCountingReport();
                        pc.in = Util.randInt(1, 100);
                        pc.out = Util.randInt(1, 100);
                        pc.avgOccupancy = Util.randInt(40, 80);
                        pc.currentOccupancy = Util.randInt(1, 100);
                        baseTicker = pc;
                        break;

                    case VCA_CROWD_DETECTION:
                        CrowdDensityAnalyticsReport.CrowdDensityReport cd = new CrowdDensityAnalyticsReport.CrowdDensityReport();
                        cd.maxValue = Util.randInt(1, 100);
                        cd.maxDuration = Util.randInt(10, 30);
                        cd.totalIndividuals = Util.randInt(10, 30);
                        cd.totalDuration = Util.randInt(100, 300);
                        cd.columns = Util.randInt(10, 30);
                        cd.rows = Util.randInt(10, 30);
                        cd.tracks = new ArrayList<>();
                        baseTicker = cd;
                        break;

                    case VCA_PROFILING:
                        AudienceProfilingAnalyticsReport.AudienceProfilingReport ap = new AudienceProfilingAnalyticsReport.AudienceProfilingReport();
                        ap.age1 = Util.randInt(1, 20);
                        ap.age2 = Util.randInt(1, 20);
                        ap.age3 = Util.randInt(1, 20);
                        ap.age4 = Util.randInt(1, 20);
                        ap.male = Util.randInt(1, 20);
                        ap.female = Util.randInt(1, 20);
                        ap.happy = Util.randInt(1, 20);
                        ap.neutral = Util.randInt(1, 20);
                        ap.dur0_5s = Util.randInt(1, 20);
                        ap.dur5_10s = Util.randInt(1, 20);
                        ap.dur10_20s = Util.randInt(1, 20);
                        ap.dur20_30s = Util.randInt(1, 20);
                        ap.dur30_60s = Util.randInt(1, 20);
                        ap.dur1_3m = Util.randInt(1, 20);
                        ap.dur3_5m = Util.randInt(1, 20);
                        ap.dur5_8m = Util.randInt(1, 20);
                        ap.dur8_10m = Util.randInt(1, 20);
                        ap.dur10_15m = Util.randInt(1, 20);
                        ap.dur15_30m = Util.randInt(1, 20);
                        baseTicker = ap;
                        break;

                    case VCA_INTRUSION:
                        baseTicker = new IntrusionAnalyticsReport.IntrusionReport();
                        break;

                    case VCA_PERIMETER_DEFENSE:
                        baseTicker = new PerimeterAnalyticsReport.PerimeterReport();
                        break;

                    case VCA_LOITERING:
                        baseTicker = new LoiteringAnalyticsReport.LoiteringReport();
                        break;

                    case VCA_OBJECT_COUNTING:
                        baseTicker = new TripWireCountingAnalyticsReport.TripWireReport();
                        break;

                    case VCA_VIDEO_BLUR:
                        baseTicker = new CameraTamperingAnalyticsReport.CameraTamperingReport();
                        break;

                    case VCA_FACE_INDEXING:
                        baseTicker = new FaceIndexingAnalyticsReport.FaceIndexingReport();
                        break;

                    default:
                        throw new UnsupportedTypeException();
                }

                baseTicker.time = current.getMillis();
                baseTicker.date = current.toString("yyyy/MM/dd HH:mm:ss");
                baseTicker.hour = current.getHourOfDay();
                baseTicker.count = Util.randInt(1, 50);
                DeviceChannelPair camera = getRandomCamera(cameraList);
                baseTicker.deviceId = camera.getCoreDeviceId();
                baseTicker.channelId = camera.getChannelId();
                tickerList.add(baseTicker);

                //next
                current = current.plusHours(1);
            }

            //save results
            previousResults.put(queryKey, tickerList);

            return tickerList;
        }

        private DeviceChannelPair getRandomCamera(List<DeviceChannelPair> cameraList)
        {
            DeviceChannelPair randNode = cameraList.get(0);
            if (cameraList.size() > 1)
            {
                int index = Util.randInt(0, cameraList.size() - 1);
                randNode = cameraList.get(index);
            }

            if (Util.isNullOrEmpty(randNode.getChannelId()))
            {
                CachedDevice device = CacheClient.getInstance().getDeviceByCoreId(randNode.getCoreDeviceId());
                if (device.isKaiNode())
                {
                    NodeObject nodeObject = NodeObject.findByPlatformId(String.valueOf(device.getPlatformDeviceId()));
                    int index = nodeObject.getCameras().size() == 1 ?
                                0 :
                                Util.randInt(0, nodeObject.getCameras().size() - 1);
                    NodeCamera randCam = nodeObject.getCameras().get(index);
                    randNode = new DeviceChannelPair(randNode.getCoreDeviceId(), randCam.nodeCoreDeviceId);
                }
                else
                {
                    randNode = new DeviceChannelPair(randNode.getCoreDeviceId(), "0");
                }
            }

            return randNode;
        }

        private String stringifyCurrentQuery()
        {
            Gson gson = new Gson();
            List<String> coreIdList = new ArrayList<>();
            for (DeviceChannelPair cam : cameraList)
            {
                String idPair = cam.getCoreDeviceId();
                if (!Util.isNullOrEmpty(cam.getChannelId()))
                {
                    idPair += ":" + cam.getChannelId();
                }
                coreIdList.add(idPair);
            }
            return gson.toJson(eventType) + gson.toJson(coreIdList) + gson.toJson(period);
        }
    }
}
