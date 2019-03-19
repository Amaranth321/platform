package platform.reports;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;

import models.Analytics.TickerReport;
import play.modules.morphia.Model.MorphiaQuery;
import platform.reports.PerimeterAnalyticsReport.PerimeterReport;

public class PerimeterAnalyticsReport extends TickerAnalyticsReport<PerimeterReport> {
	
	@Entity
	@Indexes({
		@Index("deviceId, channelId, date, time"),
		@Index("deviceId, channelId, time"),
		@Index("deviceId, channelId"),
		@Index("deviceId"),
		@Index("time")
	})
	public static class PerimeterReport extends TickerReport {
	}

	@Override
	protected Class<PerimeterReport> getCollectionClass() {
		return PerimeterReport.class;
	}

	@Override
	public void clear() {
		PerimeterReport.col().drop();
	}

	@Override
	public boolean reportExists() {
		return PerimeterReport.col().count() > 0;
	}

}
