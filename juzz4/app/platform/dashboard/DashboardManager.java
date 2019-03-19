package platform.dashboard;

import com.google.code.morphia.query.Query;
import lib.util.Util;
import models.Analytics.TickerReport;
import models.MongoDevice;
import models.MongoUser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.DeviceManager;
import platform.analytics.VcaType;
import platform.devices.DeviceChannelPair;
import platform.events.EventType;
import platform.reports.*;
import play.Logger;

import java.util.*;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class DashboardManager
{
    private static final DashboardManager instance = new DashboardManager();

    private static final List<EventType> securityEventTypes = Arrays.asList(
            EventType.VCA_INTRUSION,
            EventType.VCA_PERIMETER_DEFENSE,
            EventType.VCA_LOITERING,
            EventType.VCA_OBJECT_COUNTING,
            EventType.VCA_VIDEO_BLUR,
            EventType.VCA_FACE_INDEXING
    );

    public static DashboardManager getInstance()
    {
        return instance;
    }

    public DashboardSummary compileSummary(String userId, int timeZoneOffsetMins, int summaryDays)
    {
        MongoUser user = MongoUser.getById(userId);

        //list target dates
        List<DateTime> dates = getDateList(summaryDays, timeZoneOffsetMins);
        List<DeviceChannelPair> cameraList = getUserCameraList(userId);
        if (cameraList.isEmpty())
        {
            return DashboardSummary.getEmptySummary(userId, timeZoneOffsetMins, summaryDays);
        }

        //compiled summary
        DashboardSummary summary = new DashboardSummary(
                userId,
                timeZoneOffsetMins,
                compileDailySecurityTotals(user, dates, cameraList),
                compileDailyPeopleCounts(user, dates, cameraList),
                compileProfilingSummary(user, dates, cameraList),
                compileFaceIndexingSummary(user, dates, cameraList),
                compileTotalsBySecurityType(user, dates, cameraList)
        );

        return summary;
    }

    private DashboardManager()
    {
    }

    //returns oldest first
    private List<DateTime> getDateList(int numOfDays, int timeZoneOffsetMins)
    {
        DateTime userNow = DateTime.now(DateTimeZone.forOffsetMillis(timeZoneOffsetMins * 60 * 1000));
        List<DateTime> dateList = new ArrayList<>();
        for (int i = 0; i < numOfDays; i++)
        {
            DateTime from = userNow.minusDays(i)
                    .withHourOfDay(0)
                    .withMinuteOfHour(0)
                    .withSecondOfMinute(0)
                    .withMillisOfSecond(0);
            dateList.add(from);
        }

        Collections.reverse(dateList);
        return dateList;
    }

    private List<DeviceChannelPair> getUserCameraList(String userId)
    {
        List<DeviceChannelPair> cameraList = new ArrayList<>();
        List<MongoDevice> userDevices = DeviceManager.getInstance().getDevicesOfUser(userId);
        for (MongoDevice userDevice : userDevices)
        {
            cameraList.add(new DeviceChannelPair(userDevice.getCoreDeviceId(), ""));
        }
        return cameraList;
    }

    private List<Integer> compileDailyPeopleCounts(MongoUser user, List<DateTime> dates, List<DeviceChannelPair> cameraList)
    {
        if (!user.hasAccessToVcaFeature(VcaType.PEOPLE_COUNTING.getReportFeature()))
        {
            return generateEmptyCountList(dates.size());
        }

        List<Integer> peopleCounts = new ArrayList<>();
        AnalyticsReport report = EventReport.getReport(EventType.VCA_PEOPLE_COUNTING);
        for (DateTime targetDate : dates)
        {
            Date from = targetDate.toDate();
            Date to = getEndTime(targetDate).toDate();

            //query
            Query<PeopleCountingAnalyticsReport.PeopleCountingReport> query = report
                    .query(from, to)
                    .addDevice(cameraList).getQuery();

            Iterable<PeopleCountingAnalyticsReport.PeopleCountingReport> peopleTickers = query.fetch();
            int inTotal = 0;
            for (PeopleCountingAnalyticsReport.PeopleCountingReport ticker : peopleTickers)
            {
                inTotal += ticker.in;
            }

            peopleCounts.add(inTotal);
        }

        return peopleCounts;
    }

    private ProfilingSummary compileProfilingSummary(MongoUser user, List<DateTime> dates, List<DeviceChannelPair> cameraList)
    {

        if (!user.hasAccessToVcaFeature(VcaType.AUDIENCE_PROFILING.getReportFeature()))
        {
            return ProfilingSummary.getEmptySummary();
        }

        AnalyticsReport report = EventReport.getReport(EventType.VCA_PROFILING);

        int maleTotal = 0;
        int femaleTotal = 0;
        int happyTotal = 0;
        int neutralTotal = 0;
        int ageGroup1 = 0;
        int ageGroup2 = 0;
        int ageGroup3 = 0;
        int ageGroup4 = 0;
        for (DateTime targetDate : dates)
        {
            Date from = targetDate.toDate();
            Date to = getEndTime(targetDate).toDate();

            //query
            Query<AudienceProfilingAnalyticsReport.AudienceProfilingReport> query = report.query(from, to)
                    .addDevice(cameraList).getQuery();
            Iterable<AudienceProfilingAnalyticsReport.AudienceProfilingReport> audTickers = query.fetch();
            for (AudienceProfilingAnalyticsReport.AudienceProfilingReport ticker : audTickers)
            {
                maleTotal += ticker.male;
                femaleTotal += ticker.female;
                happyTotal += ticker.happy;
                neutralTotal += ticker.neutral;
                ageGroup1 += ticker.age1;
                ageGroup2 += ticker.age2;
                ageGroup3 += ticker.age3;
                ageGroup4 += ticker.age4;
            }
        }

        int[] ageTotals = new int[]{ageGroup1, ageGroup2, ageGroup3, ageGroup4};
        return new ProfilingSummary(maleTotal, femaleTotal, happyTotal, neutralTotal, ageTotals);
    }

    private FaceIndexingSummary compileFaceIndexingSummary(MongoUser user, List<DateTime> dates, List<DeviceChannelPair> cameraList)
    {
        if (!user.hasAccessToVcaFeature(VcaType.FACE_INDEXING.getReportFeature()))
        {
            return FaceIndexingSummary.getEmptySummary();
        }

        int totalCount = 0;
        float totalDuration = 0f;

        for (DateTime targetDate : dates)
        {
            Date from = targetDate.toDate();
            Date to = getEndTime(targetDate).toDate();

            ReportQuery reportQuery = EventReport.getReport(EventType.VCA_FACE_INDEXING).query(from, to).addDevice(cameraList);

            Iterable<FaceIndexingAnalyticsReport.FaceIndexingReport> iterable = reportQuery.getQuery().fetch();
            for (FaceIndexingAnalyticsReport.FaceIndexingReport fr : iterable)
            {
                totalCount += fr.count;
                totalDuration += fr.duration;
            }
        }

        float avgDuration = totalDuration / totalCount;

        // round to 1 decimal place
        int scale = (int) Math.pow(10, 1);
        avgDuration = (float)Math.round(avgDuration * scale) / scale;

        return new FaceIndexingSummary(totalCount, avgDuration);
    }

    /**
     * @return daily total counts combined from multiple security event types
     */
    private List<Integer> compileDailySecurityTotals(MongoUser user, List<DateTime> dates, List<DeviceChannelPair> cameraList)
    {
        List<Integer> allSecurityDailyCounts = new ArrayList<>();
        for (DateTime targetDate : dates)
        {
            Date from = targetDate.toDate();
            Date to = getEndTime(targetDate).toDate();

            int dailyTotal = 0;
            for (EventType securityEventType : securityEventTypes)
            {
                int typeTotal = getTotalCount(user, securityEventType, cameraList, from, to);
                dailyTotal += typeTotal;
            }

            allSecurityDailyCounts.add(dailyTotal);
        }

        if (dates.size() != allSecurityDailyCounts.size())
        {
            Logger.error(Util.whichFn() + "date counts (%s:%s)", dates.size(), allSecurityDailyCounts.size());
        }

        return allSecurityDailyCounts;
    }

    /**
     * @return a map of total counts for each security event type
     */
    private Map<EventType, Integer> compileTotalsBySecurityType(MongoUser user, List<DateTime> dates, List<DeviceChannelPair> cameraList)
    {

        Map<EventType, Integer> resultsMap = new LinkedHashMap<>();
        for (DateTime targetDate : dates)
        {
            Date from = targetDate.toDate();
            Date to = getEndTime(targetDate).toDate();

            for (EventType securityType : securityEventTypes)
            {
                int dayTotal = getTotalCount(user, securityType, cameraList, from, to);
                Integer currentTotal = resultsMap.get(securityType);
                if (currentTotal == null)
                {
                    resultsMap.put(securityType, dayTotal);
                }
                else
                {
                    resultsMap.put(securityType, currentTotal + dayTotal);
                }
            }
        }

        return resultsMap;
    }

    private int getTotalCount(MongoUser user, EventType vcaEventType, List<DeviceChannelPair> cameraList, Date from, Date to)
    {
        VcaType vcaType = VcaType.of(vcaEventType);
        if (!user.hasAccessToVcaFeature(vcaType.getReportFeature()))
        {
            return 0;
        }

        ReportQuery reportQuery = EventReport.getReport(vcaEventType)
                .query(from, to)
                .addDevice(cameraList);

        Iterable<TickerReport> iterable = reportQuery.getQuery().fetch();
        int total = 0;
        for (TickerReport ticker : iterable)
        {
            total += ticker.count;
        }

        return total;
    }

    private List<Integer> generateEmptyCountList(int listSize)
    {
        List<Integer> retList = new ArrayList<>();
        for (int i = 0; i < listSize; i++)
        {
            retList.add(0);
        }
        return retList;
    }

    private DateTime getEndTime(DateTime startTime)
    {
        return startTime.plusDays(1).minusMillis(1); // exclude the first hour of the next day
    }
}
