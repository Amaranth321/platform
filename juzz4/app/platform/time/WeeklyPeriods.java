package platform.time;

import lib.util.Util;
import play.Logger;

import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class WeeklyPeriods
{
    private final RepeatOption repeat;
    private final List<PeriodDayRange> periods;
    private final int lowestTrafficHour;

    public WeeklyPeriods(RepeatOption repeat,
                         List<PeriodDayRange> periods,
                         int lowestTrafficHour)
    {
        if (Util.isNullOrEmpty(periods))
        {
            throw new NullPointerException();
        }

        this.repeat = repeat;
        this.periods = periods;
        this.lowestTrafficHour = lowestTrafficHour;
    }

    public RepeatOption getRepeatOption()
    {
        return repeat;
    }

    public List<PeriodDayRange> getPeriodDayRanges()
    {
        return periods;
    }

    public int getLowestTrafficHour()
    {
        return lowestTrafficHour;
    }

    public boolean isValid()
    {
        switch (repeat)
        {
            case NON_STOP:
                if (lowestTrafficHour < 0 || lowestTrafficHour > 23)
                {
                    Logger.error(Util.whichFn() + "invalid lowestTrafficHour (%s)", lowestTrafficHour);
                    return false;
                }
                return true;

            case EVERYDAY:
                if (periods.size() > 1)
                {
                    Logger.error(Util.whichFn() + "%s option cannot have multiple ranges", repeat);
                    return false;
                }
                PeriodDayRange range = periods.get(0);
                if (range.getFromDayOfWeek() != 1 || range.getToDayOfWeek() != 7)
                {
                    Logger.error(Util.whichFn() + "invalid range for %s", repeat);
                    return false;
                }
                if (!range.isValid())
                {
                    return false;
                }
                return true;

            case CUSTOM:
                for (PeriodDayRange pdr : periods)
                {
                    if (!pdr.isValid() || TimeUtil.crossesMidnight(pdr.getPeriod()))
                    {
                        return false;
                    }
                }
                if (TimeUtil.hasOverlaps(periods))
                {
                    return false;
                }

                return true;

            default:
                throw new IllegalArgumentException(repeat.name());
        }
    }
}
