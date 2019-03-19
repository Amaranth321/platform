package platform.time;

import lib.util.Util;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import play.Logger;

import java.util.*;

/**
 * @author Aye Maung
 * @since v4.4
 */
public final class TimeUtil
{
    private TimeUtil()
    {
        //utility class
    }

    public static boolean isValid(int dayOfWeek)
    {
        return dayOfWeek >= 1 && dayOfWeek <= 7;
    }

    /**
     * Note: (startMinutes > endMinutes) is valid to allow periods that cross over midnight.
     * Use crossesMidnight() to check that if needed.
     *
     * @param periodOfDay
     */
    public static boolean isValid(PeriodOfDay periodOfDay)
    {
        if (periodOfDay.getStartMinutes() == periodOfDay.getEndMinutes())
        {
            return false;
        }
        if (periodOfDay.getStartMinutes() < PeriodOfDay.dayStart() ||
            periodOfDay.getEndMinutes() > PeriodOfDay.dayEnd())
        {
            return false;
        }
        return true;
    }

    /**
     * checks if the period covers 12pm midnight
     *
     * @param periodOfDay
     */
    public static boolean crossesMidnight(PeriodOfDay periodOfDay)
    {
        return periodOfDay.getStartMinutes() > periodOfDay.getEndMinutes();
    }

    /**
     * Checks for overlaps between {@link platform.time.PeriodOfDay}
     *
     * @param ranges
     */
    public static boolean hasOverlaps(List<PeriodDayRange> ranges)
    {
        //convert
        Map<Integer, List<PeriodOfDay>> periodsMap = toPeriodsOfDay(ranges);

        //check
        for (Integer dayOfWeek : periodsMap.keySet())
        {
            List<PeriodOfDay> periodsOfDays = periodsMap.get(dayOfWeek);
            for (int i = 0; i < periodsOfDays.size() - 1; i++)
            {
                PeriodOfDay current = periodsOfDays.get(i);
                PeriodOfDay next = periodsOfDays.get(i + 1);
                if (current.getEndMinutes() > next.getStartMinutes())
                {
                    Logger.error(Util.whichFn() + "[%s] (%s) overlaps with (%s)", dayOfWeek, current, next);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Converts ranges to the old periodsOfDays map. See {@link platform.time.RecurrenceRule}
     *
     * @param ranges
     */
    public static Map<Integer, List<PeriodOfDay>> toPeriodsOfDay(List<PeriodDayRange> ranges)
    {
        //compile
        Map<Integer, List<PeriodOfDay>> periodsMap = new LinkedHashMap<>();
        for (PeriodDayRange range : ranges)
        {
            for (int current = range.getFromDayOfWeek(); current <= range.getToDayOfWeek(); current++)
            {
                if (!periodsMap.containsKey(current))
                {
                    periodsMap.put(current, new ArrayList<PeriodOfDay>());
                }

                PeriodOfDay period = range.getPeriod();
                if (TimeUtil.crossesMidnight(period))
                {
                    PeriodOfDay firstHalf = new PeriodOfDay(period.getStartMinutes(), PeriodOfDay.dayEnd());
                    PeriodOfDay secondHalf = new PeriodOfDay(PeriodOfDay.dayStart(), period.getEndMinutes());
                    periodsMap.get(current).add(firstHalf);
                    periodsMap.get(current).add(secondHalf);
                }
                else
                {
                    periodsMap.get(current).add(period);
                }
            }
        }

        //sort
        Map<Integer, List<PeriodOfDay>> sortedPeriodsMap = new LinkedHashMap<>();
        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++)
        {
            if (periodsMap.containsKey(dayOfWeek))
            {
                List<PeriodOfDay> periods = periodsMap.get(dayOfWeek);
                Collections.sort(periods, PeriodOfDay.periodSorter);
                sortedPeriodsMap.put(dayOfWeek, periods);
            }
        }

        return sortedPeriodsMap;
    }

    public static LocalDate getStartDayOfWeek(DateTime dt)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dt.toDate());
        calendar.set(Calendar.DAY_OF_WEEK, 1);
        return new DateTime(calendar.getTime()).toLocalDate();
    }

    public static LocalDate getEndDayOfWeek(DateTime dt)
    {
        return getStartDayOfWeek(dt).plusDays(6);
    }

    public static DateTime getCurrentHourStart()
    {
        return DateTime.now(DateTimeZone.UTC)
                .withMinuteOfHour(0)
                .withSecondOfMinute(0)
                .withMillisOfSecond(0);
    }
}
