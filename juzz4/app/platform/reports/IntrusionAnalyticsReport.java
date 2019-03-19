package platform.reports;

import play.modules.morphia.Model.MorphiaQuery;
import models.Analytics.TickerReport;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;

import platform.reports.IntrusionAnalyticsReport.IntrusionReport;

public class IntrusionAnalyticsReport extends TickerAnalyticsReport<IntrusionReport> {

	@Entity
	@Indexes({
		@Index("deviceId, channelId, date, time"),
		@Index("deviceId, channelId, time"),
		@Index("deviceId, channelId"),
		@Index("deviceId"),
		@Index("time")
	})
	public static class IntrusionReport extends TickerReport {
	}

	@Override
	protected Class<IntrusionReport> getCollectionClass() {
		return IntrusionReport.class;
	}

	@Override
	public void clear() {
		IntrusionReport.col().drop();
	}

	@Override
	public boolean reportExists() {
		return IntrusionReport.col().count() > 0;
	}
}
