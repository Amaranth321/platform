package platform.analytics;

import platform.events.EventType;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum VcaFeature
{
    CONFIG_TRAFFIC_FLOW("analytics-traffic-flow"),
    CONFIG_PEOPLE_COUNTING("analytics-people-counting"),
    CONFIG_CROWD_DENSITY("analytics-crowd-detection"),
    CONFIG_PROFILING("analytics-audience-profiling"),
    CONFIG_PASSERBY("analytics-passerby"),

    CONFIG_AREA_INTRUSION("analytics-area-intrusion"),
    CONFIG_PERIMETER_DEFENSE("analytics-perimeter-defense"),
    CONFIG_LOITERING("analytics-area-loitering"),
    CONFIG_OBJ_COUNTING("analytics-object-counting"),
    CONFIG_VIDEO_BLUR("analytics-video-blur"),
    CONFIG_FACE_INDEXING("analytics-face-indexing"),
    //add by renzongke
    CONFIG_OBJECT_DETECTION("analytics-object-detection"),
    
    REPORT_TRAFFIC_FLOW("report-traffic-flow"),
    REPORT_PEOPLE_COUNTING("report-people-counting"),
    REPORT_CROWD_DENSITY("report-crowd-detection"),
    REPORT_PROFILING("report-audience-profiling"),
    REPORT_PASSERBY("report-passerby"),

    REPORT_AREA_INTRUSION("report-area-intrusion"),
    REPORT_PERIMETER_DEFENSE("report-perimeter-defense"),
    REPORT_LOITERING("report-area-loitering"),
    REPORT_OBJ_COUNTING("report-object-counting"),
    REPORT_VIDEO_BLUR("report-video-blur"),
    REPORT_FACE_INDEXING("report-face-indexing"),
	//add by renzongke
	REPORT_OBJECT_DETECTION("report-object-detection");

    public static VcaFeature parse(String featureName)
    {
        for (VcaFeature vcaFeature : values())
        {
            if (vcaFeature.featureName.equals(featureName))
            {
                return vcaFeature;
            }
        }

        throw new IllegalArgumentException(featureName);
    }

    public static boolean isVcaFeature(String featureName)
    {
        try
        {
            parse(featureName);
            return true;
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    private String featureName;

    private VcaFeature(String featureName)
    {
        this.featureName = featureName;
    }

    public String getName()
    {
        return toString();
    }

    public EventType getEventType()
    {
        return VcaType.of(this).getEventType();
    }

    @Override
    public String toString()
    {
        return featureName;
    }
}
