package platform.dashboard;

import org.apache.commons.lang.ArrayUtils;
import platform.events.EventType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * compiled data for the past specified days (oldest first in lists)
 *
 * @author Aye Maung
 * @since v4.4
 */
public class DashboardSummary
{
    private final String userId;
    private final int timeZoneOffsetMins;
    private final List<Integer> dailySecurityTotals;
    private final List<Integer> dailyPeopleCounts;
    private final ProfilingSummary profilingSummary;
    private final FaceIndexingSummary faceIndexingSummary;
    private final Map<EventType, Integer> securityVcaTotals;

    public static DashboardSummary getEmptySummary(String userId, int timeZoneOffsetMins, int summaryDays)
    {
        int[] emptyCounts = new int[summaryDays];
        Arrays.fill(emptyCounts, 0);
        List<Integer> emptyList = Arrays.asList(ArrayUtils.toObject(emptyCounts));

        return new DashboardSummary(
                userId,
                timeZoneOffsetMins,
                emptyList,
                emptyList,
                ProfilingSummary.getEmptySummary(),
                FaceIndexingSummary.getEmptySummary(),
                new LinkedHashMap<EventType, Integer>()
        );
    }

    public DashboardSummary(String userId,
                            int timeZoneOffsetMins,
                            List<Integer> dailySecurityTotals,
                            List<Integer> dailyPeopleCounts,
                            ProfilingSummary profilingSummary,
                            FaceIndexingSummary faceIndexingSummary,
                            Map<EventType, Integer> securityVcaTotals)
    {

        this.userId = userId;
        this.timeZoneOffsetMins = timeZoneOffsetMins;
        this.dailySecurityTotals = dailySecurityTotals;
        this.dailyPeopleCounts = dailyPeopleCounts;
        this.profilingSummary = profilingSummary;
        this.faceIndexingSummary = faceIndexingSummary;
        this.securityVcaTotals = securityVcaTotals;
    }

    public String getUserId()
    {
        return userId;
    }

    public int getTimeZoneOffsetMins()
    {
        return timeZoneOffsetMins;
    }

    public List<Integer> getDailySecurityTotals()
    {
        return dailySecurityTotals;
    }

    public ProfilingSummary getProfilingSummary()
    {
        return profilingSummary;
    }

    public List<Integer> getDailyPeopleCounts()
    {
        return dailyPeopleCounts;
    }

    public FaceIndexingSummary getFaceIndexingSummary()
    {
        return faceIndexingSummary;
    }

    public int getSecurityVcaTotal(EventType securityType)
    {
        if (!securityType.isSecurityVcaEvent())
        {
            throw new IllegalArgumentException();
        }

        if (!securityVcaTotals.containsKey(securityType))
        {
            return 0;
        }

        return securityVcaTotals.get(securityType);
    }
}
