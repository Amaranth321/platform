package platform.analytics.aggregation;

import lib.util.Util;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.UnsupportedTypeException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import platform.time.TimeUtil;
import platform.time.UtcPeriod;

/**
 * Aggregation relies on the tzOffsetMins.
 * <p/>
 * Masks are used to forward data points into respective groups. (like IP masks)
 *
 * @author Aye Maung
 * @since v4.4
 */
public enum AggregateType
{
    HOUR("yyyyMMddHH"),
    DAY("yyyyMMdd"),
    WEEK("yyyyMMdd"),
    MONTH("yyyyMM");

    public static AggregateType parse(String typeString) throws ApiException
    {
        if (!Util.isNullOrEmpty(typeString))
        {
            //remove 's' from UI input
            if (typeString.toLowerCase().endsWith("s"))
            {
                typeString = typeString.substring(0, typeString.length() - 1);
            }

            for (AggregateType type : values())
            {
                if (typeString.trim().equalsIgnoreCase(type.name()))
                {
                    return type;
                }
            }
        }

        throw new ApiException("invalid-aggregate-type");
    }

    /**
     * Masked time will be the user's time zone
     *
     * @return the aggregation group that the input time belongs to
     */
    public String mask(long timeMillis, int tzOffsetMins)
    {
        DateTime dt = new DateTime(timeMillis, DateTimeZone.UTC).plusMinutes(tzOffsetMins);

        //week is a trouble maker. Based on locale, week number 53 can exist, which is not parse-able.
        //so, normal mask (yyyyw) doesn't work
        if (this.equals(WEEK))
        {
            LocalDate weekStart = TimeUtil.getStartDayOfWeek(dt);
            return weekStart.toString(mask);
        }

        return dt.toString(mask);
    }

    /**
     * for displaying time in exported files or on UI
     *
     * @param timeMillis   time
     * @param tzOffsetMins time zone offset
     * @param range        printed time zone cannot go beyond this range.
     */
    public String timestamp(long timeMillis, int tzOffsetMins, UtcPeriod range)
    {
        DateTime dt = new DateTime(timeMillis, DateTimeZone.UTC).plusMinutes(tzOffsetMins);
        LocalDate rangeStart = range.getFromTime().plusMinutes(tzOffsetMins).toLocalDate();
        LocalDate rangeEnd = range.getToTime().plusMinutes(tzOffsetMins).toLocalDate();

        switch (this)
        {
            case HOUR:
                return dt.toString("dd/MM/yyyy HH:mm");

            case DAY:
                return dt.toString("dd/MM/yyyy");

            case WEEK:
                LocalDate weekStart = new LocalDate(timeMillis);
                LocalDate weekEnd = weekStart.plusDays(6);

                if (weekStart.isBefore(rangeStart))
                {
                    weekStart = rangeStart;
                }
                if (weekEnd.isAfter(rangeEnd))
                {
                    weekEnd = rangeEnd;
                }

                return String.format("%s-%s", weekStart.toString("dd/MM/yyyy"), weekEnd.toString("dd/MM/yyyy"));

            case MONTH:
                LocalDate mthStart = new LocalDate(timeMillis);
                LocalDate mthEnd = mthStart.plusMonths(1).minusDays(1);

                if (mthStart.isBefore(rangeStart))
                {
                    mthStart = rangeStart;
                }
                if (mthEnd.isAfter(rangeEnd))
                {
                    mthEnd = rangeEnd;
                }

                return String.format("%s-%s", mthStart.toString("dd/MM/yyyy"), mthEnd.toString("dd/MM/yyyy"));

            default:
                throw new UnsupportedTypeException();
        }
    }

    /**
     * @param maskedTime   timestamp returned from {@link #mask}
     * @param tzOffsetMins time zone offset
     */
    public long asMillis(String maskedTime, int tzOffsetMins)
    {
        DateTimeZone tz = DateTimeZone.forOffsetMillis(tzOffsetMins * 60 * 1000);
        DateTimeFormatter formatter = DateTimeFormat.forPattern(mask);
        DateTime dt = formatter.parseDateTime(maskedTime).withZone(tz);
        return dt.getMillis();
    }

    private final String mask;

    private AggregateType(String mask)
    {
        this.mask = mask;
    }

}
