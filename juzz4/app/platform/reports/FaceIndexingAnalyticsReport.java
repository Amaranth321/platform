package platform.reports;

import models.Analytics.TickerReport;
import models.archived.ArchivedEvent;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import platform.analytics.aggregation.AggregateOperator;
import platform.reports.FaceIndexingAnalyticsReport.FaceIndexingReport;

public class FaceIndexingAnalyticsReport extends TickerAnalyticsReport<FaceIndexingReport> {
	
	@Entity
	@Indexes({
		@Index("deviceId, channelId, date, time"),
		@Index("deviceId, channelId, time"),
		@Index("deviceId, channelId"),
		@Index("deviceId"),
		@Index("time")
	})
	public static class FaceIndexingReport extends TickerReport {
		/**
		 * Duration of the detected face
		 */
		public double duration;
		public int dur0_5s;
		public int dur5_10s;
		public int dur10_20s;
		public int dur20_30s;
		public int dur30_60s;
		public int dur1_3m;
		public int dur3_5m;
		public int dur5_8m;
		public int dur8_10m;
		public int dur10_15m;
		public int dur15_30m;

        @Override
        public TickerReport aggregate(TickerReport other,
                                      AggregateOperator operator,
                                      int hourCount)
        {
            FaceIndexingReport otherReport = (FaceIndexingReport) other;

            this.duration += otherReport.duration;
            this.dur0_5s += otherReport.dur0_5s;
            this.dur5_10s += otherReport.dur5_10s;
            this.dur10_20s += otherReport.dur10_20s;
            this.dur20_30s += otherReport.dur20_30s;
            this.dur30_60s += otherReport.dur30_60s;
            this.dur1_3m += otherReport.dur1_3m;
            this.dur3_5m += otherReport.dur3_5m;
            this.dur5_8m += otherReport.dur5_8m;
            this.dur8_10m += otherReport.dur8_10m;
            this.dur10_15m += otherReport.dur10_15m;
            this.dur15_30m += otherReport.dur15_30m;

            return super.aggregate(other, operator, hourCount);
        }
	}
	public static class FaceIndexingEventData {
		public String faceId;
		public float duration;
	}

	@Override
	protected Class<FaceIndexingReport> getCollectionClass() {
		return FaceIndexingReport.class;
	}
	
	@Override
	protected UpdateOperations<FaceIndexingReport> createUpdateOperations(
			Datastore ds, Query<FaceIndexingReport> query, Class<FaceIndexingReport> classT, ArchivedEvent event, long timestamp) {
		UpdateOperations<FaceIndexingReport> ops = ds.createUpdateOperations(classT);

        Gson gson = new Gson();
		FaceIndexingEventData detail = gson.fromJson(event.getJsonData(), FaceIndexingEventData.class);
		double duration = (double)detail.duration / 1000.0;  //duration unit is second.
		ops.inc("count", 1);
		ops.inc("duration", duration);
		
		int dur0_5s, dur5_10s, dur10_20s, dur20_30s, dur30_60s;
		int dur1_3m, dur3_5m, dur5_8m, dur8_10m, dur10_15m, dur15_30m;
		dur0_5s = dur5_10s = dur10_20s = dur20_30s = dur30_60s = 0;
		dur1_3m = dur3_5m = dur5_8m = dur8_10m = dur10_15m = dur15_30m = 0;
		if(duration < 5) {
			dur0_5s = 1;
		} else if(duration >= 5 && duration < 10) {
			dur5_10s = 1;
		} else if(duration >= 10 && duration < 20) {
			dur10_20s = 1;
		} else if(duration >= 20 && duration < 30) {
			dur20_30s = 1;
		} else if(duration >= 30 && duration < 60) {
			dur30_60s = 1;
		} else if(duration >= 60 && duration < 180) {
			dur1_3m = 1;
		} else if(duration >= 180 && duration < 300) {
			dur3_5m = 1;
		} else if(duration >= 300 && duration < 480) {
			dur5_8m = 1;
		} else if(duration >= 480 && duration < 600) {
			dur8_10m = 1;
		} else if(duration >= 600 && duration < 900) {
			dur10_15m = 1;
		} else if(duration >= 900 && duration < 1800) {
			dur15_30m = 1;
		}	
		ops.inc("dur0_5s", dur0_5s);
		ops.inc("dur5_10s", dur5_10s);
		ops.inc("dur10_20s", dur10_20s);
		ops.inc("dur20_30s", dur20_30s);
		ops.inc("dur30_60s", dur30_60s);
		ops.inc("dur1_3m", dur1_3m);
		ops.inc("dur3_5m", dur3_5m);
		ops.inc("dur5_8m", dur5_8m);
		ops.inc("dur8_10m", dur8_10m);
		ops.inc("dur10_15m", dur10_15m);
		ops.inc("dur15_30m", dur15_30m);
		
		return ops;
	}

	@Override
	public void clear() {
		FaceIndexingReport.col().drop();
	}

	@Override
	public boolean reportExists() {
		return FaceIndexingReport.col().count() > 0;
	}

}
