package models.transients;

import org.joda.time.DateTime;

/**
 * Location coordinates.
 *
 * @author kdp
 *
 * NOTE: Objects of this class do not persist. This class is used merely to create transport objects.
 */
public class LocationPoint {

    public Double   latitude;
    public Double   longitude;
    public String   timestamp;

    public LocationPoint() {
        latitude = 0.0;
        longitude = 0.0;
        timestamp = DateTime.now().toString("ddMMyyyyHHmmss");
    }
}
