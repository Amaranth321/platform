package platform.reports;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.kaisquare.util.CacheProxy;
import lib.util.JsonReader;
import lib.util.exceptions.InvalidJsonException;
import models.Analytics.TickerReport;
import models.archived.ArchivedEvent;
import platform.analytics.aggregation.AggregateOperator;
import platform.reports.PasserbyAnalyticsReport.PasserbyReport;


/**
 * @author Aye Maung
 * @since v4.5
 */
public class PasserbyAnalyticsReport extends TickerAnalyticsReport<PasserbyReport>
{
    private CacheProxy cacheProxy = CacheProxy.getInstance();
    private static final Object CACHE_LOCK_CURRENT_RECORD = new Object();

    @Override
    protected Class<PasserbyReport> getCollectionClass()
    {
        return PasserbyReport.class;
    }

    @Override
    public boolean reportExists()
    {
        return PasserbyReport.col().count() > 0;
    }

    @Override
    public void clear()
    {
        PasserbyReport.col().drop();
    }

    @Override
    protected UpdateOperations<PasserbyReport> createUpdateOperations(Datastore ds,
                                                                      Query<PasserbyReport> query,
                                                                      Class<PasserbyReport> classT,
                                                                      ArchivedEvent event,
                                                                      long timestamp)
    {
        PasserbyEventData parsedData = null;
        try
        {
            parsedData = PasserbyEventData.parse(event.getJsonData());
        }
        catch (InvalidJsonException e)
        {
            throw new IllegalArgumentException(e);
        }

        PasserbyReport record = getCurrentRecord(ds, query, classT, event, timestamp);
        UpdateOperations<PasserbyReport> ops = ds.createUpdateOperations(PasserbyReport.class);
        synchronized (record)
        {
            ops.inc("in", parsedData.in);
            ops.inc("out", parsedData.out);
            ops.inc("count", 1);

            record.in += parsedData.in;
            record.out += parsedData.out;
            record.count++;
        }

        return ops;
    }

    /**
     * Returns the current hour's record.
     * Fetches the record either from cache, or the DB. If record doesn't exist at either location then a new one
     * is created, saved to DB, added to cache and returned.
     */
    private PasserbyReport getCurrentRecord(Datastore ds,
                                            Query<PasserbyReport> query,
                                            Class<PasserbyReport> classT,
                                            ArchivedEvent event,
                                            long timestamp)
    {
        //single instance lock
        PasserbyReport cachedRecord = null;
        String cacheKey = null;
        synchronized (CACHE_LOCK_CURRENT_RECORD)
        {
            //look up the record in cache first
            cacheKey = String.format("passerby-%s-%s-%d",
                                     event.getEventInfo().getCamera().getCoreDeviceId(),
                                     event.getEventInfo().getCamera().getChannelId(),
                                     timestamp);
            cachedRecord = (PasserbyReport) cacheProxy.get(cacheKey);
            if (cachedRecord == null)
            {
                //cache miss, look up in DB
                PasserbyReport dbRecord = query.get();
                if (dbRecord == null)
                {
                    //DB doesn't have the record, probably first event of the hour, or previous record deleted.
                    //Regardless, create new record in DB with default values.
                    UpdateOperations<PasserbyReport> ops = ds.createUpdateOperations(classT);
                    ops.set("in", 0);
                    ops.set("out", 0);
                    ops.set("count", 0);
                    dbRecord = ds.findAndModify(query, ops, false, true);
                }
                cacheProxy.set(cacheKey, dbRecord);
                cachedRecord = dbRecord;
            }
        }
        return cachedRecord;
    }

    @Entity
    @Indexes({
                     @Index("deviceId, channelId, date, time"),
                     @Index("deviceId, channelId, time"),
                     @Index("deviceId, channelId"),
                     @Index("deviceId"),
                     @Index("time")
             })
    public static class PasserbyReport extends TickerReport
    {
        /**
         * Total amount of people from r1 to r2
         */
        public int in;
        /**
         * Total amount of people from r2 to r1
         */
        public int out;

        @Override
        public TickerReport aggregate(TickerReport other,
                                      AggregateOperator operator,
                                      int hourCount)
        {
            PasserbyReport otherReport = (PasserbyReport) other;
            switch (operator)
            {
                case SUM:
                    this.in += otherReport.in;
                    this.out += otherReport.out;
                    return super.aggregate(other, operator, hourCount);

                default:
                    throw new UnsupportedOperationException(operator.name());
            }
        }
    }

    public static class PasserbyEventData
    {
        public final int in;
        public final int out;

        public static PasserbyEventData parse(String jsonData) throws InvalidJsonException
        {
            JsonReader reader = new JsonReader();
            reader.load(jsonData);
            int in = reader.getAsInt("in", 0);
            int out = reader.getAsInt("out", 0);
            return new PasserbyEventData(in, out);
        }

        private PasserbyEventData(int in, int out)
        {
            this.in = in;
            this.out = out;
        }
    }
}
