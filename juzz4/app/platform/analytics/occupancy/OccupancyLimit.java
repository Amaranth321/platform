package platform.analytics.occupancy;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class OccupancyLimit implements Comparable<OccupancyLimit>
{
    private final int limit;
    private final String alertMessage;

    public OccupancyLimit(int limit)
    {
        this(limit, "Above " + limit);
    }

    public OccupancyLimit(int limit, String alertMessage)
    {
        this.limit = limit;
        this.alertMessage = alertMessage;
    }

    public int getLimit()
    {
        return limit;
    }

    public String getAlertMessage()
    {
        return alertMessage;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof OccupancyLimit)
        {
            OccupancyLimit other = (OccupancyLimit) o;
            return other.limit == this.limit;
        }
        return false;
    }

    @Override
    public int compareTo(OccupancyLimit o)
    {
        return this.limit - o.limit;
    }
}
