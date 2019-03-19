package platform.common;

import org.joda.time.DateTimeZone;

import java.util.TimeZone;

/**
 * For {@link #timeZoneId},
 * refer to <a href="https://en.wikipedia.org/wiki/List_of_tz_database_time_zones">time zone list</a>.
 *
 * @author Aye Maung
 * @since v4.4
 */
public class Location
{
    private final String address;
    private final double lat;
    private final double lng;
    private final String timeZoneId;

    public Location(String address,
                    double lat,
                    double lng,
                    String timeZoneId)
    {
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.timeZoneId = timeZoneId;
    }

    public String getAddress()
    {
        return address;
    }

    public double getLat()
    {
        return lat;
    }

    public double getLng()
    {
        return lng;
    }

    public String getTimeZoneId()
    {
    	return timeZoneId;
    }
    
    public DateTimeZone getTimeZone()
    {
        TimeZone tz = TimeZone.getTimeZone(timeZoneId);
        return DateTimeZone.forTimeZone(tz);
    }
}
