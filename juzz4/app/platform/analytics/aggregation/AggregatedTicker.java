package platform.analytics.aggregation;

import models.Analytics.TickerReport;
import platform.devices.DeviceGroup;
import platform.devices.DeviceGroupType;
import platform.events.EventType;
import platform.reports.PeopleCountingAnalyticsReport;

/**
 * Each instance holds the aggregated data for the corresponding group indicated by groupTime.
 *
 * @author Aye Maung
 * @since v4.4
 */
public class AggregatedTicker implements Comparable<AggregatedTicker>
{
    private final long groupTime;
    private final DeviceGroup deviceGroup;
    private final EventType eventType;
    private TickerReport baseTicker;
    private int tickerTotal;

    public AggregatedTicker(long groupTime,
                            DeviceGroup deviceGroup,
                            EventType eventType)
    {
        this.groupTime = groupTime;
        this.deviceGroup = deviceGroup;
        this.eventType = eventType;
    }
    
    public void setBaseTicker(TickerReport baseTicker)
    {
        this.baseTicker=baseTicker;
    }

    @Override
    public int compareTo(AggregatedTicker o)
    {
        if (this.groupTime != o.groupTime)
        {
            return this.groupTime > o.groupTime ? 1 : -1;
        }

        return this.deviceGroup.getGroupName().compareTo(o.deviceGroup.getGroupName());
    }

    public void addTicker(TickerReport tickerReport, AggregateOperator operator)
    {
        TickerReport adjustedCopy = adjust(tickerReport.newCopy());
        if (adjustedCopy == null)
        {
            return;
        }

        if (this.baseTicker == null)
        {
            this.baseTicker = adjustedCopy.newCopy();
        }
        else
        {
            this.baseTicker.aggregate(adjustedCopy, operator, tickerTotal);
        }

        tickerTotal++;
    }

    public long getGroupTime()
    {
        return groupTime;
    }

    public DeviceGroup getDeviceGroup()
    {
        return deviceGroup;
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public TickerReport getBaseTicker()
    {
        return baseTicker;
    }

    public int getTickerTotal()
    {
        return tickerTotal;
    }

    private TickerReport adjust(TickerReport ticker)
    {
        /**
         *
         * Add special adjustments below to mitigate inaccurate data points
         *
         * return null if this ticker should be ignored
         *
         */
        switch (eventType)
        {
            case VCA_PEOPLE_COUNTING:

                PeopleCountingAnalyticsReport.PeopleCountingReport pc = (PeopleCountingAnalyticsReport.PeopleCountingReport) ticker;
                if (pc.avgOccupancy == 0 && pc.in == 0 && pc.out == 0)
                {
                    return null;
                }
        }

        return ticker;
    }
}
