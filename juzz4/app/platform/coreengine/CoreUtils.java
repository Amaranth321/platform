package platform.coreengine;

import models.MongoDevice;
import models.node.NodeCamera;
import models.node.NodeObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import platform.devices.DeviceChannelPair;
import play.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.3
 */
public final class CoreUtils
{
    public static final String TIME_FORMAT = "ddMMyyyyHHmmss";

    public static long getMillis(String timestamp)
    {
        DateTime dt = DateTime.parse(timestamp, DateTimeFormat.forPattern(TIME_FORMAT).withZoneUTC());
        return dt.getMillis();
    }

    public static String convertToTimestamp(long millis)
    {
        DateTime dt = new DateTime(millis, DateTimeZone.UTC);
        return dt.toString(CoreUtils.TIME_FORMAT);
    }

    public static String getCameraCoreChannelId(DeviceChannelPair camera)
    {
        try
        {
            MongoDevice device = MongoDevice.getByCoreId(camera.getCoreDeviceId());
            if (!device.isKaiNode())
            {
                return camera.getChannelId();
            }

            NodeObject node = NodeObject.findByPlatformId(device.getDeviceId());
            List<Integer> cameraIds = new ArrayList<>();
            for (NodeCamera c : node.getCameras())
            {
                cameraIds.add(Integer.parseInt(c.nodeCoreDeviceId));
            }

            Collections.sort(cameraIds);
            int index = cameraIds.indexOf(Integer.parseInt(camera.getChannelId()));
            return String.valueOf(index);

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    public static long roundTo15MinMarks(long origMillis)
    {
        DateTime dtOrig = new DateTime(origMillis);
        DateTime dtZero = dtOrig
                .withMinuteOfHour(0)
                .withSecondOfMinute(0)
                .withMillisOfSecond(0);

        int plusOrMinus = 2;
        List<DateTime> roundTimes = Arrays.asList(
                dtZero,
                dtZero.plusMinutes(15),
                dtZero.plusMinutes(30),
                dtZero.plusMinutes(45),
                dtZero.plusMinutes(60)
        );

        for (DateTime roundTime : roundTimes)
        {
            Interval plusMinusRange = new Interval(roundTime.minusMinutes(plusOrMinus),
                                                   roundTime.plusMinutes(plusOrMinus));

            if (plusMinusRange.contains(origMillis))
            {
                return roundTime.getMillis();
            }
        }

        //no rounding if not within plusOrMinus range
        return origMillis;
    }
}
