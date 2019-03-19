package platform.reports;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import models.Analytics.TickerReport;
import platform.reports.LoiteringAnalyticsReport.LoiteringReport;

public class LoiteringAnalyticsReport extends TickerAnalyticsReport<LoiteringReport> {
	
	@Entity
	@Indexes({
		@Index("deviceId, channelId, date, time"),
		@Index("deviceId, channelId, time"),
		@Index("deviceId, channelId"),
		@Index("deviceId"),
		@Index("time")
	})
	public static class LoiteringReport extends TickerReport {
	}

	@Override
	protected Class<LoiteringReport> getCollectionClass() {
		return LoiteringReport.class;
	}

	@Override
	public void clear() {
		LoiteringReport.col().drop();
	}

	@Override
	public boolean reportExists() {
		return LoiteringReport.col().count() > 0;
	}

}
