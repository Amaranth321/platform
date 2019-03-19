package platform;

import com.kaisquare.core.thrift.LocationDataPoint;
import core.DataClient;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.BucketSetting;
import models.MongoDevice;
import models.transients.LocationPoint;
import play.Logger;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class LocationManager
{
    public static final String DEFAULT_MAP_SOURCE = "google";

    private static LocationManager instance = null;

    private LocationManager()
    {
    }

    public static LocationManager getInstance()
    {
        if (instance == null)
        {
            instance = new LocationManager();
        }
        return instance;
    }

    /**
     * Retrieves current location of a device.
     *
     * @param device The device object.
     *
     * @return A models.LocationPoint object that represents current location
     * of the device.
     *
     * @throws ApiException Various reasons of failure.
     */
    public LocationPoint getLiveLocation(MongoDevice device) throws ApiException
    {
        try
        {
            //Get current location of the device
            DataClient dc = DataClient.getInstance();
            List<LocationDataPoint> loc = dc.getLocation(device.getCoreDeviceId(), null, null);

            //If a location is received from backend, update the device object
            //in KUP database first and then return the current location with
            //timestamp.
            //If backend doesn't provide any location, just return the last known
            //location of the device without any timestamp.
            if (loc != null && !loc.isEmpty())
            {
                device.setLatitude(loc.get(0).getLatitude());
                device.setLongitude(loc.get(0).getLongitude());
                device.save();

                LocationPoint result = new LocationPoint();
                result.latitude = loc.get(0).getLatitude();
                result.longitude = loc.get(0).getLongitude();
                result.timestamp = loc.get(0).getTime();
                return result;
            }
            else
            {
                LocationPoint result = new LocationPoint();
                result.latitude = device.getLatitude();
                result.longitude = device.getLongitude();
                result.timestamp = "";
                return result;
            }
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            throw new ApiException("unknown");
        }
    }

    /**
     * returns map source from bucket setting if on cloud
     * and timezone based map source if on nodes
     * <p/>
     * map source: google or baidu
     *
     * @return google/baidu
     */
    public String getMapSource(String bucketId)
    {
        String defaultMap = DEFAULT_MAP_SOURCE;
        try
        {
            TimeZone tz = Calendar.getInstance().getTimeZone();
            String tzBasedSource = tz.getDisplayName().contains("China") ? "baidu" : DEFAULT_MAP_SOURCE;

            //On nodes, only use time zone
            if (Environment.getInstance().onKaiNode())
            {
                return tzBasedSource;
            }

            //On cloud, read from bucket settings
            else
            {
                if (Util.isNullOrEmpty(bucketId))
                {
                    return DEFAULT_MAP_SOURCE;
                }

                BucketSetting setting = BucketManager.getInstance().getBucketSetting(bucketId);
                if (Util.isNullOrEmpty(setting.mapSource))
                {
                    return DEFAULT_MAP_SOURCE;
                }

                return setting.mapSource;
            }

        }
        catch (Exception e)
        {
            Logger.error(e, "Exception in getMapSource: 'google' will be used");
            return defaultMap;
        }
    }
}
