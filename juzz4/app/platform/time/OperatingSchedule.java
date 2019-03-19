package platform.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class OperatingSchedule
{
    /**
     * Day of week numbers are 1 to 7, Monday to Sunday respectively
     */
    private final List<Integer> holidays;

    private final WeeklyPeriods weeklyPeriods;

    public OperatingSchedule(List<Integer> holidays, WeeklyPeriods weeklyPeriods)
    {
        this.holidays = holidays;
        this.weeklyPeriods = weeklyPeriods;
    }

    public List<Integer> getHolidays()
    {
        return holidays == null ? new ArrayList<Integer>() : holidays;
    }

    public WeeklyPeriods getWeeklyPeriods()
    {
        return weeklyPeriods;
    }

    public RecurrenceRule toRecurrenceRule(String ruleName)
    {
        List<PeriodDayRange> pdrList;
        if (weeklyPeriods.getRepeatOption().equals(RepeatOption.NON_STOP))
        {
            pdrList = Arrays.asList(new PeriodDayRange(1, 7, new PeriodOfDay(0, 1440)));
        }
        else
        {
            pdrList = weeklyPeriods.getPeriodDayRanges();
        }

        Map<Integer, List<PeriodOfDay>> periodMap = TimeUtil.toPeriodsOfDay(pdrList);
        for (Integer holiday : getHolidays())
        {
            if (periodMap.containsKey(holiday))
            {
                periodMap.remove(holiday);
            }
        }
        return new RecurrenceRule(periodMap, ruleName);
    }

    public boolean isValid()
    {
        for (Integer holiday : getHolidays())
        {
            if (!TimeUtil.isValid(holiday))
            {
                return false;
            }
        }

        return true;
    }

}
