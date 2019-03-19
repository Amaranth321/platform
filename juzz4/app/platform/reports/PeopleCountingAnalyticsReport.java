package platform.reports;

import lib.util.JsonReader;
import models.Analytics.TickerReport;
import models.archived.ArchivedEvent;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.gson.Gson;
import com.kaisquare.util.CacheProxy;

import org.joda.time.DateTime;

import platform.analytics.aggregation.AggregateOperator;
import platform.events.EventManager;
import platform.reports.PeopleCountingAnalyticsReport.PeopleCountingReport;
import play.Logger;

import java.util.Map;

public class PeopleCountingAnalyticsReport extends TickerAnalyticsReport<PeopleCountingReport> {

	@Entity
	@Indexes({
		@Index("deviceId, channelId, date, time"),
		@Index("deviceId, channelId, time"),
		@Index("deviceId, channelId"),
		@Index("deviceId"),
		@Index("time")
	})
	public static class PeopleCountingReport extends TickerReport
	{
		/**
		 * Total amount of people in
		 */
		public int in;
		/**
		 * Total amount of people out
		 */
		public int out;
		/**
		 * Current real time occupancy
		 */
		public int currentOccupancy;
		/**
		 * Moving average of occupancy
		 */
		public double avgOccupancy;

        @Override
        public TickerReport aggregate(TickerReport other,
                                      AggregateOperator operator,
                                      int hourCount)
        {
            PeopleCountingReport otherReport = (PeopleCountingReport) other;
            this.currentOccupancy = 0;  //not applicable

            switch (operator)
            {
                case SUM:
                    this.in += otherReport.in;
                    this.out += otherReport.out;
                    this.avgOccupancy += otherReport.avgOccupancy;
                    return super.aggregate(other, operator, hourCount);

                case CUSTOM:
                    this.in += otherReport.in;
                    this.out += otherReport.out;

                    //weighted average based on hourCount
                    double newAvg = (this.avgOccupancy * hourCount + otherReport.avgOccupancy) / (hourCount + 1);
                    this.avgOccupancy = newAvg;

                    //don't call super for CUSTOM
                    this.count += otherReport.count;
                    return this;

                default:
                    throw new UnsupportedOperationException(operator.name());
            }
        }
	}

	private CacheProxy cacheProxy = CacheProxy.getInstance();
	private static final Object CACHE_LOCK_CURRENT_RECORD = new Object();

	@Override
	protected Class<PeopleCountingReport> getCollectionClass() {
		return PeopleCountingReport.class;
	}

    /**
     * Returns the last hour's record.
     * Fetches the record either from cache, or the DB. If record doesn't exist at either location,
     * or if an error occurs, then returns null.
     */
	private PeopleCountingReport getLastRecord(Datastore ds,
            Class<PeopleCountingReport> classT,
            ArchivedEvent event) {

        //compose the query & timestamp for last record
		DateTime dt = new DateTime(event.getEventInfo().getTime());
        dt = dt.minusHours(1); //for last hour's record
		String dateString = dt.toString("yyyy/MM/dd HH:00:00");
		long time = dt.getMillis();

        //single instance lock
		PeopleCountingReport cachedRecord = null;
		String cacheKey = null;
		//look up the record in cache first
		cacheKey = String.format("people-counting-%s-%s-%d",
				event.getEventInfo().getCamera().getCoreDeviceId(),
				event.getEventInfo().getCamera().getChannelId(),
				time);
		cachedRecord = (PeopleCountingReport) cacheProxy.get(cacheKey);
		if (cachedRecord == null) {
            //cache miss, look up in DB
            Query<PeopleCountingReport> query = createQuery(ds, classT);
            query.and(query.criteria("deviceId").equal(event.getEventInfo().getCamera().getCoreDeviceId()))
                 .and(query.criteria("channelId").equal(event.getEventInfo().getCamera().getChannelId()))
                 .and(query.criteria("date").equal(dateString))
                 .and(query.criteria("time").equal(time));
			PeopleCountingReport dbRecord = query.get();
            if(dbRecord == null) {
                return null;
            }
            cacheProxy.set(cacheKey, dbRecord);
            cachedRecord = dbRecord;
        }
        return cachedRecord;
	}

    /**
     * Returns the current hour's record.
     * Fetches the record either from cache, or the DB. If record doesn't exist at either location then a new one
     * is created, saved to DB, added to cache and returned.
     */
	private PeopleCountingReport getCurrentRecord(Datastore ds, Query<PeopleCountingReport> query,
            Class<PeopleCountingReport> classT,
            ArchivedEvent event,
            long timestamp) {
		//single instance lock
		PeopleCountingReport cachedRecord = null;
		String cacheKey = null;
		synchronized (CACHE_LOCK_CURRENT_RECORD) {
            //look up the record in cache first
			cacheKey = String.format("people-counting-%s-%s-%d",
					event.getEventInfo().getCamera().getCoreDeviceId(),
					event.getEventInfo().getCamera().getChannelId(),
					timestamp);
			cachedRecord = (PeopleCountingReport) cacheProxy.get(cacheKey);
			if (cachedRecord == null) {
                //cache miss, look up in DB
				PeopleCountingReport dbRecord = query.get();
                if(dbRecord == null) {
                    //DB doesn't have the record, probably first event of the hour, or previous record deleted.
                    //Regardless, create new record in DB with default values.

                    //Retrieve the last hour's record if available, because we need to carry over the occupancy
                    PeopleCountingReport dbRecordOfLastHour = getLastRecord(ds, classT, event);
                    int currentOccupancy = (dbRecordOfLastHour == null) ? 0 : dbRecordOfLastHour.currentOccupancy;
                    double avgOccupancy = (dbRecordOfLastHour == null) ? 0 : dbRecordOfLastHour.avgOccupancy;
                    int count = 0;
                    if(avgOccupancy != 0) {
                        count = 1; //for previous non-zero avg occupancy to have any meaning and effect on this hour, the count must be 1
                    }

                    UpdateOperations<PeopleCountingReport> ops = ds.createUpdateOperations(classT);
                    ops.set("in", 0);
                    ops.set("out", 0);
                    ops.set("currentOccupancy", currentOccupancy); //current occupancy should be carried over from last hour
                    ops.set("avgOccupancy", avgOccupancy); //avg occupancy should be carried over from last hour
                    ops.set("count", count); //see above
                    dbRecord = ds.findAndModify(query, ops, false, true);
                }
                cacheProxy.set(cacheKey, dbRecord);
                cachedRecord = dbRecord;
            }
		}
        return cachedRecord;
	}

	@Override
	protected UpdateOperations<PeopleCountingReport> createUpdateOperations(
			Datastore ds, Query<PeopleCountingReport> query, Class<PeopleCountingReport> classT, ArchivedEvent event, long timestamp) {

        //Manually parse json data to check if occupancy is provided (from new ACV)
		Gson gson = new Gson();
        Map jsonMap = gson.fromJson(event.getJsonData(), Map.class);
        JsonReader reader = new JsonReader();
        reader.load(jsonMap);
        int newIn = reader.getAsInt("in", 0);
        int newOut = reader.getAsInt("out", 0);
        Integer newOccupancy = null;
        if(reader.containsKey("occupancy")) //for new people counting
        {
            newOccupancy = reader.getAsInt("occupancy", 0);
        }

        PeopleCountingReport record = getCurrentRecord(ds, query, classT, event, timestamp);
        UpdateOperations<PeopleCountingReport> ops = ds.createUpdateOperations(PeopleCountingReport.class);
        synchronized (record)
        {
	        //the change introduced by the current event
            int change = newIn - newOut;
            if(newOccupancy == null) //old vca
            {
                newOccupancy = (record.currentOccupancy + change);
            }
            int diffOccupancy = newOccupancy - record.currentOccupancy;

	        //calculate the moving average of occupancy
	        double avgOccupancy = ((newOccupancy) + (record.avgOccupancy * record.count)) / (record.count + 1);
            double diffAvgOccupancy = avgOccupancy - record.avgOccupancy;
	        //Explanation:
	        // - avgOccupancy is the moving average of occupancy as a result of all the changes in this hour
	        // - to calculate the new moving average, we multiply the previous moving average by the # of changes (count)
	        // within the hour, add to that the new occupancy value, and then divide the whole thing by updated count
	        // (previous # of changes plus 1 to include the current event).
	        // - If this is the first event since the last hour mark, we start counting occupancy moving average from
	        // current change, because in this case current change is also the current occupancy and

	        ops.inc("in", newIn);
	        ops.inc("out", newOut);
	        ops.inc("currentOccupancy", diffOccupancy);
	        ops.inc("avgOccupancy", diffAvgOccupancy);
	        ops.inc("count", 1);

	        record.in += newIn;
	        record.out += newOut;
	        record.currentOccupancy = newOccupancy;
	        record.avgOccupancy = avgOccupancy;
	        record.count++;

            //for secondary actions if occupancy changes
            if (diffOccupancy != 0)
            {
                EventManager.getInstance().occupancyChanged(event.getEventInfo().getCamera(), newOccupancy);
            }
        }

        return ops;
	}

	@Override
	public void clear() {
		PeopleCountingReport.col().drop();
	}

	@Override
	public boolean reportExists() {
		return PeopleCountingReport.col().count() > 0;
	}

}
