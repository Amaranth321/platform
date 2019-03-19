package controllers.ws;

import platform.events.EventType;
import platform.rt.EventRTFeed;
import platform.rt.RTFeedManager;
import platform.rt.RTSubscriber;
import play.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Use {@link DefaultWSController#validateSession()} to authenticate callers.
 * DO NOT add interceptors, nor any of @Before, @After, @Finally, etc ...
 *
 * @author Aye Maung
 * @sectiontitle Realtime Video Analytics Feed
 * @sectiondesc Websocket based APIs for receiving results of video analytics in real time.
 * @publicapi
 * @since v4.4
 */
public class VCAFeedController extends DefaultWSController
{
    private static final RTFeedManager rtFeedMgr = RTFeedManager.getInstance();

    /**
     * @servtitle Feed for Human Traffic Flow data.
     * @httpmethod WS
     * @uri ws://{host}:{port}/ws/trafficflowfeed
     * @responsejson {
     * "time":1444707249000,
     * "eventId":"3edd012b-08b6-477f-8a27-e1ba08d890aa",
     * "type":"event-vca-traffic",
     * "deviceId":"31",
     * "channelId":"0",
     * "data":"{\"device-id\":\"31\",\"channel-id\":\"0\",\"tracks\":\"[{\\\"id\\\":\\\"0\\\",\\\"time\\\":\\\"13/10/2015 03:33:15\\\",\\\"x\\\":0.866166,\\\"y\\\":0.301515,\\\"w\\\":0.459375,\\\"h\\\":0.495833,\\\"timeMillis\\\":\\\"1444707195663\\\"},{\\\"id\\\":\\\"0\\\",\\\"time\\\":\\\"13/10/2015 03:33:16\\\",\\\"x\\\":0.589297,\\\"y\\\":0.818386,\\\"w\\\":0.6,\\\"h\\\":0.525,\\\"timeMillis\\\":\\\"1444707196668\\\"},{\\\"id\\\":\\\"0\\\",\\\"time\\\":\\\"13/10/2015 03:33:17\\\",\\\"x\\\":0.752333,\\\"y\\\":0.745948,\\\"w\\\":0.428125,\\\"h\\\":0.429167,\\\"timeMillis\\\":\\\"1444707197675\\\"},{\\\"id\\\":\\\"0\\\",\\\"time\\\":\\\"13/10/2015 03:33:18\\\",\\\"x\\\":0.860805,\\\"y\\\":0.557839,\\\"w\\\":0.175,\\\"h\\\":0.333333,\\\"timeMillis\\\":\\\"1444707198679\\\"},{\\\"id\\\":\\\"2\\\",\\\"time\\\":\\\"13/10/2015 03:34:08\\\",\\\"x\\\":0.663896,\\\"y\\\":0.720052,\\\"w\\\":0.625,\\\"h\\\":0.254167,\\\"timeMillis\\\":\\\"1444707248182\\\"},{\\\"id\\\":\\\"2\\\",\\\"time\\\":\\\"13/10/2015 03:34:09\\\",\\\"x\\\":0.628888,\\\"y\\\":0.788756,\\\"w\\\":0.675,\\\"h\\\":0.270833,\\\"timeMillis\\\":\\\"1444707249190\\\"}]\"}"
     * }
     */
    public static void trafficFlowFeed()
    {
        vcaEventFeed(EventType.VCA_TRAFFIC_FLOW);
    }

    /**
     * @servtitle Feed for People Counting data.
     * @httpmethod WS
     * @uri ws://{host}:{port}/ws/peoplecountingfeed
     * @responsejson {
     * "time":1444707409000,
     * "eventId":"a5cb47af-7eea-4c51-98bb-7ebfc28cdce7",
     * "type":"event-vca-people-counting",
     * "deviceId":"31",
     * "channelId":"0",
     * "data":"{\"in\":\"0\",\"out\":\"1\",\"occupancy\":\"1\"}"
     * }
     */
    public static void peopleCountingFeed()
    {
        vcaEventFeed(EventType.VCA_PEOPLE_COUNTING);
    }


    /**
     * @servtitle Feed for People Counting data.
     * @httpmethod WS
     * @uri ws://{host}:{port}/ws/passerbyFeed
     * @responsejson {
     * "time":1444707409000,
     * "eventId":"a5cb47af-7eea-4c51-98bb-7ebfc28cdce7",
     * "type":"event-vca-passerby",
     * "deviceId":"31",
     * "channelId":"0",
     * "data":"{\"in\":\"0\",\"out\":\"1\"}"
     * }
     */
    public static void passerbyFeed()
    {
        vcaEventFeed(EventType.VCA_PASSERBY);
    }

    /**
     * @servtitle Feed for Crowd Density data.
     * @httpmethod WS
     * @uri ws://{host}:{port}/ws/crowddensityfeed
     * @responsejson {
     * "time":1444707249000,
     * "eventId":"3edd012b-08b6-477f-8a27-e1ba08d890aa",
     * "type":"event-vca-crowd",
     * "deviceId":"31",
     * "channelId":"0",
     * "data":"{\"device-id\":\"31\",\"channel-id\":\"0\",\"tracks\":\"[{\\\"id\\\":\\\"0\\\",\\\"time\\\":\\\"13/10/2015 03:33:15\\\",\\\"x\\\":0.866166,\\\"y\\\":0.301515,\\\"w\\\":0.459375,\\\"h\\\":0.495833,\\\"timeMillis\\\":\\\"1444707195663\\\"},{\\\"id\\\":\\\"0\\\",\\\"time\\\":\\\"13/10/2015 03:33:16\\\",\\\"x\\\":0.589297,\\\"y\\\":0.818386,\\\"w\\\":0.6,\\\"h\\\":0.525,\\\"timeMillis\\\":\\\"1444707196668\\\"},{\\\"id\\\":\\\"0\\\",\\\"time\\\":\\\"13/10/2015 03:33:17\\\",\\\"x\\\":0.752333,\\\"y\\\":0.745948,\\\"w\\\":0.428125,\\\"h\\\":0.429167,\\\"timeMillis\\\":\\\"1444707197675\\\"},{\\\"id\\\":\\\"0\\\",\\\"time\\\":\\\"13/10/2015 03:33:18\\\",\\\"x\\\":0.860805,\\\"y\\\":0.557839,\\\"w\\\":0.175,\\\"h\\\":0.333333,\\\"timeMillis\\\":\\\"1444707198679\\\"},{\\\"id\\\":\\\"2\\\",\\\"time\\\":\\\"13/10/2015 03:34:08\\\",\\\"x\\\":0.663896,\\\"y\\\":0.720052,\\\"w\\\":0.625,\\\"h\\\":0.254167,\\\"timeMillis\\\":\\\"1444707248182\\\"},{\\\"id\\\":\\\"2\\\",\\\"time\\\":\\\"13/10/2015 03:34:09\\\",\\\"x\\\":0.628888,\\\"y\\\":0.788756,\\\"w\\\":0.675,\\\"h\\\":0.270833,\\\"timeMillis\\\":\\\"1444707249190\\\"}]\"}"
     * }
     */
    public static void crowdDensityFeed()
    {
        vcaEventFeed(EventType.VCA_CROWD_DETECTION);
    }

    /**
     * @servtitle Feed for Audience Profiling data. It outputs 2 types of data as shown in examples.
     * @httpmethod WS
     * @uri ws://{host}:{port}/ws/profilingfeed
     * @responsejson {
     * "time":1444648555000,
     * "eventId":"3669a5a6-9e0a-4f2a-b804-7169d64d8c11",
     * "type":"event-feed-profiling (this type of event is output once every second while there is a face in the camera's field of view)",
     * "deviceId":"31",
     * "channelId":"0",
     * "data":"{\"device-id\":\"31\",\"channel-id\":\"0\",\"id\":\"23\",\"duration\":\"19310.0\",\"gender\":\"1.0\",\"genderavg\":\"1.0\",\"smile\":\"1.0\",\"smileavg\":\"1.0\",\"age\":\"1.55\",\"ageavg\":\"1.55\"}"
     * }
     * @responsejson {
     * "time":1444648555000,
     * "eventId":"3669a5a6-9e0a-4f2a-b804-7169d64d8c11",
     * "type":"event-vca-audienceprofiling (this type of event is output after a face leaves the camera's field of view)",
     * "deviceId":"31",
     * "channelId":"0",
     * "data":"{\"device-id\":\"31\",\"channel-id\":\"0\",\"id\":\"23\",\"duration\":\"19310.0\",\"gender\":\"1.0\",\"genderavg\":\"1.0\",\"smile\":\"1.0\",\"smileavg\":\"1.0\",\"age\":\"1.55\",\"ageavg\":\"1.55\"}"
     * }
     */
    public static void profilingFeed()
    {
        vcaEventFeed(EventType.VCA_FEED_PROFILING, EventType.VCA_PROFILING);
    }

    /**
     * @servtitle Feed for Intrusion Detection data.
     * @httpmethod WS
     * @uri ws://{host}:{port}/ws/intrusionfeed
     * @responsejson {
     * "time":1444707475000,
     * "eventId":"9452d1a7-e0ac-4b5d-97d2-a5afb8f0d168",
     * "type":"event-vca-intrusion",
     * "deviceId":"31",
     * "channelId":"0",
     * "data":"{\"device-id\":\"31\",\"channel-id\":\"0\",\"fgpercent\":\"67.70\",\"fgpixels\":\"1356.00\"}"
     * }
     */
    public static void intrusionFeed()
    {
        vcaEventFeed(EventType.VCA_INTRUSION);
    }

    /**
     * @servtitle Feed for Perimeter Defense data.
     * @httpmethod WS
     * @uri ws://{host}:{port}/ws/perimeterdefensefeed
     * @responsejson {
     * "time":1444707409000,
     * "eventId":"a5cb47af-7eea-4c51-98bb-7ebfc28cdce7",
     * "type":"event-vca-perimeter",
     * "deviceId":"31",
     * "channelId":"0",
     * "data":"{\"device-id\":\"31\",\"channel-id\":\"0\",\"fgpercent\":\"67.70\",\"fgpixels\":\"1356.00\"}"
     * }
     */
    public static void perimeterDefenseFeed()
    {
        vcaEventFeed(EventType.VCA_PERIMETER_DEFENSE);
    }

    /**
     * @servtitle Feed for Object Counting (Trip Wire Counting) data.
     * @httpmethod WS
     * @uri ws://{host}:{port}/ws/objectcountingfeed
     * @responsejson {
     * "time":1444708013000,
     * "eventId":"602f7e45-a4ae-49f7-9610-44f4c0381ea2",
     * "type":"event-vca-object-counting",
     * "deviceId":"31",
     * "channelId":"0",
     * "data":"{\"device-id\":\"31\",\"channel-id\":\"0\",\"in\":\"2\",\"out\":\"0\"}"
     * }
     */
    public static void objectCountingFeed()
    {
        vcaEventFeed(EventType.VCA_OBJECT_COUNTING);
    }

    /**
     * @servtitle Feed for Loitering Detection data.
     * @httpmethod WS
     * @uri ws://{host}:{port}/ws/loiteringfeed
     * @responsejson {
     * "time":1444707730000,
     * "eventId":"134dacb2-4c10-4122-b490-1e0b1b2481ed",
     * "type":"event-vca-loitering",
     * "deviceId":"31",
     * "channelId":"0",
     * "data":"{\"device-id\":\"31\",\"channel-id\":\"0\",\"duration\":\"3\"}"
     * }
     */
    public static void loiteringFeed()
    {
        vcaEventFeed(EventType.VCA_LOITERING);
    }

    /**
     * @servtitle Feed for Video Blur Detection (Camera Tampering) data.
     * @httpmethod WS
     * @uri ws://{host}:{port}/ws/videoblurfeed
     * @responsejson {
     * "time":1444708311000,
     * "eventId":"39e57fde-4353-4ec5-ad8a-c473b06efcfa",
     * "type":"event-vca-video-blur",
     * "deviceId":"31",
     * "channelId":"0",
     * "data":"{\"device-id\":\"31\",\"channel-id\":\"0\",\"sharpness\":\"96.27\"}"
     * }
     */
    public static void videoBlurFeed()
    {
        vcaEventFeed(EventType.VCA_VIDEO_BLUR);
    }

    /**
     * @servtitle Feed for Face indexing data
     * @httpmethod WS
     * @uri ws://{host}:{port}/ws/faceindexingfeed
     * @responsejson {
     * "time":1444708311000,
     * "eventId":"39e57fde-4353-4ec5-ad8a-c473b06efcfa",
     * "type":"event-vca-video-blur",
     * "deviceId":"31",
     * "channelId":"0",
     * "data":"{\"device-id\":\"1\",\"channel-id\":\"0\",\"faceId\":\"0\",\"duration\":\"228.000000\"}"
     * }
     */
    public static void faceIndexingFeed()
    {
        vcaEventFeed(EventType.VCA_FACE_INDEXING);
    }


    private static void vcaEventFeed(EventType... eventTypes)
    {
        validateSession();
        try
        {
            //subscribe
            long callerUserId = getCallerUserId();
            List<RTSubscriber<EventRTFeed>> subscriberList = new ArrayList<>();
            for (EventType eventType : eventTypes)
            {
                RTSubscriber<EventRTFeed> subscriber = rtFeedMgr.addEventSubscriber(callerUserId, eventType);
                if (subscriber == null)
                {
                    throw new Exception(String.format("failed to add subscriber : %s %s ", callerUserId, eventType));
                }
                subscriberList.add(subscriber);
            }

            //listen
            while (inbound.isOpen())
            {
                for (RTSubscriber<EventRTFeed> subscriber : subscriberList)
                {
                    EventRTFeed feed = await(subscriber.getNext(1000));
                    if (feed != null)
                    {
                        writeToOutbound(feed.toAPIOutput());
                    }
                }
            }

            //close
            for (RTSubscriber<EventRTFeed> subscriber : subscriberList)
            {
                rtFeedMgr.removeSubscriber(subscriber);
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        disconnect();
    }
}
