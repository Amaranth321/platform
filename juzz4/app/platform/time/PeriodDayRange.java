package platform.time;

import lib.util.Util;
import play.Logger;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class PeriodDayRange
{
    private final int from;
    private final int to;
    private final PeriodOfDay period;

    public PeriodDayRange(int from, int to, PeriodOfDay period)
    {
        this.from = from;
        this.to = to;
        this.period = period;
    }

    /**
     * Day of week numbers are 1 to 7, Monday to Sunday respectively
     */
    public int getFromDayOfWeek()
    {
        return from;
    }

    /**
     * Day of week numbers are 1 to 7, Monday to Sunday respectively
     */
    public int getToDayOfWeek()
    {
        return to;
    }

    /**
     * @return Period that is common to all days between from and to inclusive
     */
    public PeriodOfDay getPeriod()
    {
        return period;
    }

    public boolean isValid()
    {
        if (from > to)
        {
            Logger.error(Util.whichFn() + "from must be smaller than to");
            return false;
        }

        if (!TimeUtil.isValid(from))
        {
            Logger.error(Util.whichFn() + "from (%s) is invalid", from);
            return false;
        }

        if (!TimeUtil.isValid(to))
        {
            Logger.error(Util.whichFn() + "to (%s) is invalid", to);
            return false;
        }

        if (!TimeUtil.isValid(period))
        {
            Logger.error(Util.whichFn() + "period (%s) is invalid", period);
            return false;
        }

        return true;
    }
}
