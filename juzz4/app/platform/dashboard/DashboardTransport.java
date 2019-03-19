package platform.dashboard;

import platform.events.EventType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is solely used as a container for sending data to UI
 * <p/>
 * Structure and names below have to be kept for compatibility with the UI side which uses the previous format.
 *
 * @author Aye Maung
 * @since v4.4
 */
public class DashboardTransport
{
    public final String result = "ok";
    public final List<Integer> eventscount;
    public final Map<String, Integer> securityeventssummary;
    public final Map<String, Integer> genderprofilingcount;
    public final Map<String, Integer> emotionprofilingsummary;
    public final Map<String, Integer> ageprofilingsummary;
    public final Map<String, Object> peoplecounting;
    public final Map<String, Number> faceindexingsummary;

    public DashboardTransport(DashboardSummary summary)
    {
        eventscount = summary.getDailySecurityTotals();

        securityeventssummary = new LinkedHashMap<>();
        securityeventssummary.put("intrusion", summary.getSecurityVcaTotal(EventType.VCA_INTRUSION));
        securityeventssummary.put("perimeterdefense", summary.getSecurityVcaTotal(EventType.VCA_PERIMETER_DEFENSE));
        securityeventssummary.put("tripwire", summary.getSecurityVcaTotal(EventType.VCA_OBJECT_COUNTING));
        securityeventssummary.put("loitering", summary.getSecurityVcaTotal(EventType.VCA_LOITERING));
        securityeventssummary.put("cameratampering", summary.getSecurityVcaTotal(EventType.VCA_VIDEO_BLUR));

        ProfilingSummary profilingSummary = summary.getProfilingSummary();
        genderprofilingcount = new LinkedHashMap<>();
        genderprofilingcount.put("male", profilingSummary.getMaleTotal());
        genderprofilingcount.put("female", profilingSummary.getFemaleTotal());

        emotionprofilingsummary = new LinkedHashMap<>();
        emotionprofilingsummary.put("happy", profilingSummary.getHappyTotal());
        emotionprofilingsummary.put("neutral", profilingSummary.getNeutralTotal());

        ageprofilingsummary = new LinkedHashMap<>();
        ageprofilingsummary.put("ageunder20", profilingSummary.getTotalByAgeGroup(0));
        ageprofilingsummary.put("age21to35", profilingSummary.getTotalByAgeGroup(1));
        ageprofilingsummary.put("age36to55", profilingSummary.getTotalByAgeGroup(2));
        ageprofilingsummary.put("ageover55", profilingSummary.getTotalByAgeGroup(3));

        FaceIndexingSummary faceSummary = summary.getFaceIndexingSummary();
        faceindexingsummary = new LinkedHashMap<>();
        faceindexingsummary.put("total", faceSummary.getTotal());
        faceindexingsummary.put("averageattentionspan", faceSummary.getAvgDuration());

        List<Integer> peopleCounts = summary.getDailyPeopleCounts();
        int total = 0;
        int today = peopleCounts.get(peopleCounts.size() - 1);
        for (int peopleCount : peopleCounts)
        {
            total += peopleCount;
        }
        peoplecounting = new LinkedHashMap<>();
        peoplecounting.put("total", total);
        peoplecounting.put("today", today);
        peoplecounting.put("counts", peopleCounts);

    }
}
