package platform.time;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class UtcPeriod
{
    private long from;
    private long to;

    public static UtcPeriod Unbounded()
    {
        return new UtcPeriod(0, Long.MAX_VALUE);
    }

    public static UtcPeriod tillNow(long millisAgo)
    {
        long now = System.currentTimeMillis();
        long from = now - millisAgo;
        return new UtcPeriod(from, now);
    }

    public UtcPeriod(long fromMillis, long toMillis)
    {
        from = fromMillis;
        to = toMillis;
    }

    public UtcPeriod(DateTime from, DateTime to)
    {
        setFromTime(from);
        setToTime(to);
    }

    public long getFromMillis()
    {
        return from;
    }

    public DateTime getFromTime()
    {
        return new DateTime(from, DateTimeZone.UTC);
    }

    public long getToMillis()
    {
        return to;
    }

    public DateTime getToTime()
    {
        return new DateTime(to, DateTimeZone.UTC);
    }

    public void setFromTime(DateTime fromTime)
    {
        checkUTC(fromTime);
        from = fromTime.getMillis();
    }

    public void setToTime(DateTime toTime)
    {
        checkUTC(toTime);
        to = toTime.getMillis();
    }

    public boolean contains(long millis)
    {
        return (millis >= from && millis < to);
    }

    @Override
    public String toString()
    {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        String start = getFromTime().toString(formatter);
        String end = getToTime().toString(formatter);
        return String.format("%s - %s", start, end);
    }

    public long length()
    {
        return to - from;
    }

    private void checkUTC(DateTime dt)
    {
        if (!dt.getZone().equals(DateTimeZone.UTC))
        {
            throw new IllegalArgumentException("Time zone must be in UTC");
        }
    }
}
