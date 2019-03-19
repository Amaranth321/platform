package platform.reports;

import models.archived.ArchivedEvent;
import platform.config.readers.ConfigsShared;
import platform.events.EventType;
import play.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class EventReport {

	private static final Map<EventType, AnalyticsReport> reportProcessors = new HashMap<EventType, AnalyticsReport>();

	static {
		reportProcessors.put(EventType.VCA_INTRUSION, new IntrusionAnalyticsReport());
		reportProcessors.put(EventType.VCA_PERIMETER_DEFENSE, new PerimeterAnalyticsReport());
		reportProcessors.put(EventType.VCA_LOITERING, new LoiteringAnalyticsReport());
		reportProcessors.put(EventType.VCA_OBJECT_COUNTING, new TripWireCountingAnalyticsReport());
		reportProcessors.put(EventType.VCA_VIDEO_BLUR, new CameraTamperingAnalyticsReport());
		reportProcessors.put(EventType.VCA_FACE_INDEXING, new FaceIndexingAnalyticsReport());
		reportProcessors.put(EventType.VCA_PEOPLE_COUNTING, new PeopleCountingAnalyticsReport());
		reportProcessors.put(EventType.VCA_PASSERBY, new PasserbyAnalyticsReport());
		reportProcessors.put(EventType.VCA_PROFILING, new AudienceProfilingAnalyticsReport());
		reportProcessors.put(EventType.VCA_TRAFFIC_FLOW, new TrafficFlowAnalyticsReport());
		reportProcessors.put(EventType.VCA_CROWD_DETECTION, new CrowdDensityAnalyticsReport());
		//add by renzongke 
		reportProcessors.put(EventType.VCA_OBJECT_DETECTION, new ObjectDetectionAnalyticsReport());
	}

	public static boolean process(ArchivedEvent event)
	{
		if (event == null)
			throw new NullPointerException("null event object");

		AnalyticsReport report = reportProcessors.get(event.getEventInfo().getType());
		if (report != null)
			return report.process(event);
		else
			Logger.debug("error processing event (Unknown event type): %s", event.getEventInfo());

		return false;
	}

	public static EventType[] getSupportedEventTypes()
	{
		EventType[] types = new EventType[reportProcessors.size()];
		Iterator<Entry<EventType, AnalyticsReport>> iterator = reportProcessors.entrySet().iterator();
		int n = 0;
		while (iterator.hasNext())
		{
			types[n++] = iterator.next().getKey();
		}

		return types;
	}

	public static boolean reportExists(String eventType) throws IllegalAccessException
	{
		AnalyticsReport report = reportProcessors.get(eventType);
		if (report != null)
			return report.reportExists();
		else
			throw new IllegalAccessException("No report processor for event " + eventType);
	}

	public static AnalyticsReport getReport(EventType type)
    {
        if (ConfigsShared.getInstance().mockHourlyVcaData())
        {
            return new SimulatedAnalyticsReport(type);
        }

        return reportProcessors.get(type);
    }

    public static void clear()
	{
		Iterator<Entry<EventType, AnalyticsReport>> iterator = reportProcessors.entrySet().iterator();
		while (iterator.hasNext())
		{
			iterator.next().getValue().clear();
		}
	}
}
