package platform.reports;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.gson.annotations.SerializedName;

import models.Analytics.TickerReport;
import models.archived.ArchivedEvent;
import platform.reports.AudienceProfilingAnalyticsReport.AudienceProfilingEventData;
import platform.reports.AudienceProfilingAnalyticsReport.AudienceProfilingReport;
import platform.reports.ObjectDetectionAnalyticsReport.ObjectDetectionReport;

public class ObjectDetectionAnalyticsReport extends TickerAnalyticsReport<ObjectDetectionReport> {

	
	@Entity
	@Indexes({
		@Index("deviceId, channelId, date, time"),
		@Index("deviceId, channelId, time"),
		@Index("deviceId, channelId"),
		@Index("deviceId"),
		@Index("time")
	})
	
	public static class ObjectDetectionReport extends TickerReport {
		public String objLabel;
		public String firstSeen;
		public String lastSeen;
		public String fileName;
	}
	
	
	/*public static class ObjectDetectionEventData
	{
		public String objLabel;
		public String firstSeen;
		public String lastSeen;
		public String fileName;
	}*/
	
	/*@Override
	protected UpdateOperations<ObjectDetectionReport> createUpdateOperations(
			Datastore ds, Query<ObjectDetectionReport> query, Class<ObjectDetectionReport> classT, ArchivedEvent event, long timestamp){
		UpdateOperations<ObjectDetectionReport> ops = ds.createUpdateOperations(classT);
		AudienceProfilingEventData detail = gson.fromJson(event.getJsonData(), AudienceProfilingEventData.class);
		
		return ops;
	}*/

	@Override
	public boolean reportExists() {
		// TODO Auto-generated method stub
		return ObjectDetectionReport.col().count() > 0;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		ObjectDetectionReport.col().drop();
	}

	@Override
	protected Class<ObjectDetectionReport> getCollectionClass() {
		// TODO Auto-generated method stub
		return ObjectDetectionReport.class;
	}
}
