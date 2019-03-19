package platform.reports;

import models.Analytics.TickerReport;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;

import platform.reports.CameraTamperingAnalyticsReport.CameraTamperingReport;
import play.modules.morphia.Model.MorphiaQuery;

public class CameraTamperingAnalyticsReport extends TickerAnalyticsReport<CameraTamperingReport> {
	
	@Entity
	@Indexes({
		@Index("deviceId, channelId, date, time"),
		@Index("deviceId, channelId, time"),
		@Index("deviceId, channelId"),
		@Index("deviceId"),
		@Index("time")
	})
	public static class CameraTamperingReport extends TickerReport {
	}

	@Override
	protected Class<CameraTamperingReport> getCollectionClass() {
		return CameraTamperingReport.class;
	}

	@Override
	public void clear() {
		CameraTamperingReport.col().drop();
	}

	@Override
	public boolean reportExists() {
		return CameraTamperingReport.col().count() > 0;
	}
}
