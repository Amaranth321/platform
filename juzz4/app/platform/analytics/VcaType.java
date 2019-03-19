package platform.analytics;

import platform.events.EventType;

import java.util.Arrays;
import java.util.List;

/**
 * To replace previous string literals under AnalyticsType
 *
 * @author Aye Maung
 * @since v4.4
 */
public enum VcaType
{
    /**
     * Business intelligence
     */
    TRAFFIC_FLOW(
            "TRAFFIC",
            VcaGroup.BI,
            EventType.VCA_TRAFFIC_FLOW,
            VcaFeature.CONFIG_TRAFFIC_FLOW,
            VcaFeature.REPORT_TRAFFIC_FLOW
    ),
    PEOPLE_COUNTING(
            "PCOUNTING",
            VcaGroup.BI,
            EventType.VCA_PEOPLE_COUNTING,
            VcaFeature.CONFIG_PEOPLE_COUNTING,
            VcaFeature.REPORT_PEOPLE_COUNTING
    ),
    CROWD_DETECTION(
            "CROWD",
            VcaGroup.BI,
            EventType.VCA_CROWD_DETECTION,
            VcaFeature.CONFIG_CROWD_DENSITY,
            VcaFeature.REPORT_CROWD_DENSITY
    ),
    AUDIENCE_PROFILING(
            "PROFILING",
            VcaGroup.BI,
            EventType.VCA_PROFILING,
            VcaFeature.CONFIG_PROFILING,
            VcaFeature.REPORT_PROFILING
    ),
    PASSERBY(
            "PASSERBY",
            VcaGroup.BI,
            EventType.VCA_PASSERBY,
            VcaFeature.CONFIG_PASSERBY,
            VcaFeature.REPORT_PASSERBY
    ),
    
    OBJECT_DETECTION(
    		"OBJDETECT",
    		/*VcaGroup.BI,*/
    		 VcaGroup.SECURITY,
    		EventType.VCA_OBJECT_DETECTION,
    		VcaFeature.CONFIG_OBJECT_DETECTION,
    		VcaFeature.REPORT_OBJECT_DETECTION
    ),

    /**
     * Security Analytics
     */
    AREA_INTRUSION(
            "INTRUSION",
            VcaGroup.SECURITY,
            EventType.VCA_INTRUSION,
            VcaFeature.CONFIG_AREA_INTRUSION,
            VcaFeature.REPORT_AREA_INTRUSION
    ),
    PERIMETER_DEFENSE(
            "PERIMETER",
            VcaGroup.SECURITY,
            EventType.VCA_PERIMETER_DEFENSE,
            VcaFeature.CONFIG_PERIMETER_DEFENSE,
            VcaFeature.REPORT_PERIMETER_DEFENSE
    ),
    AREA_LOITERING(
            "LOITERING",
            VcaGroup.SECURITY,
            EventType.VCA_LOITERING,
            VcaFeature.CONFIG_LOITERING,
            VcaFeature.REPORT_LOITERING
    ),
    OBJECT_COUNTING(
            "OBJCOUNTING",
            VcaGroup.SECURITY,
            EventType.VCA_OBJECT_COUNTING,
            VcaFeature.CONFIG_OBJ_COUNTING,
            VcaFeature.REPORT_OBJ_COUNTING
    ),
    VIDEO_BLUR(
            "VIDEOBLUR",
            VcaGroup.SECURITY,
            EventType.VCA_VIDEO_BLUR,
            VcaFeature.CONFIG_VIDEO_BLUR,
            VcaFeature.REPORT_VIDEO_BLUR
    ),
    FACE_INDEXING(
            "FACE",
            VcaGroup.SECURITY,
            EventType.VCA_FACE_INDEXING,
            VcaFeature.CONFIG_FACE_INDEXING,
            VcaFeature.REPORT_FACE_INDEXING
    );

    public static VcaType parse(String name)
    {
        for (VcaType vcaType : values())
        {
            if (name.equals(vcaType.name()) || name.equals(vcaType.typeName))
            {
                return vcaType;
            }
        }

        throw new IllegalArgumentException(name);
    }

    public static VcaType of(EventType eventType)
    {
        for (VcaType vcaType : values())
        {
            if (eventType.equals(vcaType.eventType))
            {
                return vcaType;
            }
        }

        throw new IllegalArgumentException(eventType.name());
    }

    public static VcaType of(VcaFeature vcaFeature)
    {
        for (VcaType vcaType : values())
        {
            if (vcaFeature.equals(vcaType.configFeature) ||
                vcaFeature.equals(vcaType.reportFeature))
            {
                return vcaType;
            }
        }

        throw new IllegalArgumentException(vcaFeature.name());
    }

    public static List<VcaType> all()
    {
        return Arrays.asList(values());
    }

    @Override
    public String toString()
    {
        //don't change this. Mongo will throw "No enum constant" error
        return name();
    }

    public boolean isBI()
    {
        return group.equals(VcaGroup.BI);
    }

    public boolean isSecurity()
    {
        return group.equals(VcaGroup.SECURITY);
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public VcaFeature getConfigFeature()
    {
        return configFeature;
    }

    public VcaFeature getReportFeature()
    {
        return reportFeature;
    }

    private String typeName;
    private VcaGroup group;
    private EventType eventType;
    private VcaFeature configFeature;
    private VcaFeature reportFeature;

    VcaType(String typeName,
            VcaGroup group,
            EventType eventType,
            VcaFeature configFeature,
            VcaFeature reportFeature)
    {
        this.typeName = typeName;
        this.group = group;
        this.eventType = eventType;
        this.configFeature = configFeature;
        this.reportFeature = reportFeature;
    }

    public String getVcaTypeName()
    {
        return typeName;
    }

}
