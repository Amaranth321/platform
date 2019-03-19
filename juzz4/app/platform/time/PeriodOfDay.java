package platform.time;

import lib.util.Util;
import play.Logger;

import java.util.Comparator;

/**
 * @author Aye Maung
 * @since v4.0
 */
public class PeriodOfDay
{
    private final int startMinutes;
    private final int endMinutes;

    public static Comparator<PeriodOfDay> periodSorter = new Comparator<PeriodOfDay>()
    {
        public int compare(PeriodOfDay a, PeriodOfDay b)
        {
            return a.startMinutes - b.startMinutes;
        }
    };

    public static int dayStart()
    {
        return 0;
    }

    public static int dayEnd()
    {
        return 24 * 60;
    }

    public PeriodOfDay(int startMinutes, int endMinutes)
    {
        this.startMinutes = startMinutes;
        this.endMinutes = endMinutes;
    }

    public int getStartMinutes()
    {
        return startMinutes;
    }

    public int getEndMinutes()
    {
        return endMinutes;
    }

    public boolean isValid()
    {
        if (!TimeUtil.isValid(this))
        {
            Logger.error(Util.whichFn() + "period (%s) is invalid", toString());
            return false;
        }

        return true;
    }

    public boolean overlaps(PeriodOfDay other)
    {
        return !(this.endMinutes <= other.startMinutes || other.endMinutes <= this.startMinutes);
    }

    /**
     * @return null if the two periods don't overlap
     */
    public PeriodOfDay union(PeriodOfDay other)
    {
        if (!this.overlaps(other))
        {
            return null;
        }

        int newStart = this.startMinutes < other.startMinutes ? this.startMinutes : other.startMinutes;
        int newEnd = this.endMinutes > other.endMinutes ? this.endMinutes : other.endMinutes;

        return new PeriodOfDay(newStart, newEnd);
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s", startMinutes, endMinutes);
    }

    @Override
    public boolean equals(Object other)
    {
        if (other instanceof PeriodOfDay)
        {
            PeriodOfDay otherPeriod = (PeriodOfDay) other;
            return this.startMinutes == otherPeriod.startMinutes &&
                   this.endMinutes == otherPeriod.endMinutes;
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return Integer.parseInt(startMinutes + "" + endMinutes);
    }
}
