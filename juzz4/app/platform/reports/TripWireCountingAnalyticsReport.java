package platform.reports;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;

import models.Analytics.TickerReport;
import play.modules.morphia.Model.MorphiaQuery;
import platform.reports.TripWireCountingAnalyticsReport.TripWireReport;

public class TripWireCountingAnalyticsReport extends TickerAnalyticsReport<TripWireReport> {
	
	@Entity
	@Indexes({
		@Index("deviceId, channelId, date, time"),
		@Index("deviceId, channelId, time"),
		@Index("deviceId, channelId"),
		@Index("deviceId"),
		@Index("time")
	})
	public static class TripWireReport extends TickerReport {
	}

	@Override
	protected Class<TripWireReport> getCollectionClass() {
		return TripWireReport.class;
	}

	@Override
	public void clear() {
		TripWireReport.col().drop();
	}

	@Override
	public boolean reportExists() {
		return TripWireReport.col().count() > 0;
	}

}
