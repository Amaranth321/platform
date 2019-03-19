package platform.analytics.occupancy;

import java.util.Set;
import java.util.TreeSet;

/**
 * Deprecated. Use {@link models.notification.LabelOccupancySettings} instead
 *
 * @author Aye Maung
 * @since v4.4
 */
@Deprecated
public class OccupancySettings
{
    private boolean enabled;
    private TreeSet<OccupancyLimit> limits;
}
