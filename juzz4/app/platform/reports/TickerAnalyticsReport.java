package platform.reports;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lib.util.exceptions.ApiException;
import org.joda.time.DateTime;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;

import play.Logger;
import play.modules.morphia.Model;

import models.Analytics.TickerReport;
import models.archived.ArchivedEvent;

public abstract class TickerAnalyticsReport<T extends TickerReport> implements AnalyticsReport {
	
	public static final float WIDTH = 320.0f;
	public static final float HEIGHT = 240.0f;
	public static final float MIN_RATIO = 0.01f;
	public static final int MIN_WIDTH = Math.round(WIDTH * MIN_RATIO);
	public static final int MIN_HEIGHT = Math.round(HEIGHT * MIN_RATIO);

    /**
     * this function is for parsing the input json of report query parameters.
     * Refer to {@link ReportQuery#setParameter(String, Object)}
     */
    public static Map<String, Object> parseAdditionalParams(String jsonParams) throws ApiException
    {
        Map<String, Object> additionalParams = null;
        try
        {
            additionalParams = new Gson().fromJson(jsonParams, new TypeToken<LinkedHashMap<String, Object>>()
            {
            }.getType());
        }
        catch (Exception e)
        {
            Logger.error(e.getMessage());
        }

        if (additionalParams == null)
        {
            throw new ApiException("invalid-report-params");
        }

        return additionalParams;
    }

	@Override
	public boolean process(ArchivedEvent event) {
		DateTime dt = new DateTime(event.getEventInfo().getTime());
		String dateString = dt.toString("yyyy/MM/dd HH:00:00");
		long time = 0;
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			time = dateFormat.parse(dateString).getTime();
		} catch (ParseException e) {
			Logger.error(e, "error parsing date '%s'", dateString);
			return false;
		}
		Datastore ds = getDatastore();
		Class<T> classT = getCollectionClass();
		Query<T> query = createQuery(ds, classT);
		query.and(query.criteria("deviceId").equal(event.getEventInfo().getCamera().getCoreDeviceId()))
			 .and(query.criteria("channelId").equal(event.getEventInfo().getCamera().getChannelId()))
			 .and(query.criteria("date").equal(dateString))
			 .and(query.criteria("time").equal(time));
		
		UpdateOperations<T> ops = createUpdateOperations(ds, query, classT, event, time);
		if (ops != null)
			ds.findAndModify(query, ops, false, true);
		
		return true;
	}
	
	protected Query<T> getInternalQuery()
	{
		Datastore ds = getDatastore();
		Class<T> classT = getCollectionClass();
		Query<T> query = createQuery(ds, classT);
		
		return query;
	}

	@Override
	public ReportQuery<T> query(Date from, Date to) {
		Query<T> query = getInternalQuery();
		return new DefaultReportQuery<T>(query)
				.setDateFrom(from)
				.setDateTo(to);
	}
	
	@Override
	public void retention(Date from) {
		Datastore ds = getDatastore();
		Class<T> classT = getCollectionClass();
		Query<T> query = createQuery(ds, classT);
		
		long dateFrom = from.getTime();
		query.criteria("time").lessThanOrEq(dateFrom);
		
		ds.delete(query);
	}
	
	protected Query<T> createQuery(Datastore ds, Class<T> classT)
	{
		return ds.createQuery(classT);
	}
	
	protected UpdateOperations<T> createUpdateOperations(Datastore ds, Query<T> query, Class<T> classT, ArchivedEvent event, long timestamp)
	{
		return ds.createUpdateOperations(classT).inc("count", 1);
	}
	
	protected Datastore getDatastore()
	{
		return Model.ds();
	}
	
	protected abstract Class<T> getCollectionClass();
	
}
