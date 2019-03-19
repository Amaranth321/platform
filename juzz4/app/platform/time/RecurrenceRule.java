package platform.time;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static lib.util.Util.isNullOrEmpty;

/**
 * periodsOfDays is the list of periods for each dayOfWeek
 * day integer is the dayOfWeek number (i.e. 1 = Monday, 7 = Sunday)
 *
 * @author Aye Maung
 * @since v4.0
 */
public class RecurrenceRule
{
    public final String summary;
    public final Map<Integer, List<PeriodOfDay>> periodsOfDays;

    public static RecurrenceRule parse(String ruleString) throws ApiException
    {
        if (isNullOrEmpty(ruleString))
        {
            return null;
        }

        RecurrenceRule objRule = new Gson().fromJson(ruleString, new TypeToken<RecurrenceRule>()
        {
        }.getType());

        if (!objRule.isValid())
        {
            throw new ApiException("Invalid RecurrenceRule");
        }

        return objRule;
    }

    public RecurrenceRule(Map<Integer, List<PeriodOfDay>> periodsOfDays, String summary)
    {
        this.periodsOfDays = periodsOfDays;
        this.summary = summary;
    }

    public boolean isNow(TimeZone timeZone)
    {
        if (timeZone == null)
        {
            Logger.error("[%s] time zone is null", Util.getCallerFn());
            return false;
        }
        try
        {
            DateTime utcNow = DateTime.now(DateTimeZone.UTC);
            int localTzOffset = timeZone.getRawOffset();
            DateTime localNow = utcNow.plusMillis(localTzOffset);

            List<PeriodOfDay> todayPeriods = periodsOfDays.get(localNow.getDayOfWeek());
            if (todayPeriods == null || todayPeriods.size() == 0)
            {
                return false;
            }

            int nowMinutes = localNow.getMinuteOfDay();
            for (PeriodOfDay period : todayPeriods)
            {
                if ((nowMinutes >= period.getStartMinutes()) && (nowMinutes < period.getEndMinutes()))
                {
                    return true;
                }
            }

            return false;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    /**
     * summery is compulsory
     * periods must not have overlaps
     */
    public boolean isValid()
    {
        try
        {
            if (isNullOrEmpty(this.summary))
            {
                return false;
            }

            for (int day : this.periodsOfDays.keySet())
            {
                //sort periods
                List<PeriodOfDay> periodList = this.periodsOfDays.get(day);
                Collections.sort(periodList, PeriodOfDay.periodSorter);
                this.periodsOfDays.put(day, periodList);

                for (int i = 0; i < periodList.size() - 1; i++)
                {
                    PeriodOfDay current = periodList.get(i);

                    //check start < end
                    if (current.getStartMinutes() >= current.getEndMinutes())
                    {
                        Logger.error(Util.whichFn() + "invalid period range");
                        return false;
                    }

                    //check for period overlaps
                    PeriodOfDay next = periodList.get(i + 1);
                    if (current.getEndMinutes() > next.getStartMinutes())
                    {
                        Logger.error(Util.whichFn() + "periods overlap");
                        return false;
                    }
                }
            }

            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof RecurrenceRule))
        {
            return false;
        }

        RecurrenceRule otherRule = (RecurrenceRule) other;

        //we only need to know if two rules are identical. So, just compare json strings
        Gson gson = new Gson();
        return gson.toJson(this).equals(gson.toJson(otherRule));
    }

}
