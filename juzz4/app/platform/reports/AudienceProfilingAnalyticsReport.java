package platform.reports;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import models.Analytics.TickerReport;
import models.archived.ArchivedEvent;
import platform.analytics.aggregation.AggregateOperator;
import platform.reports.AudienceProfilingAnalyticsReport.AudienceProfilingReport;

public class AudienceProfilingAnalyticsReport extends TickerAnalyticsReport<AudienceProfilingReport> {

	@Entity
	@Indexes({
		@Index("deviceId, channelId, date, time"),
		@Index("deviceId, channelId, time"),
		@Index("deviceId, channelId"),
		@Index("deviceId"),
		@Index("time")
	})
	public static class AudienceProfilingReport extends TickerReport
	{
		//age below 20;
		public int age1;
		//age 21-35
		public int age2;
		//age 36-55
		public int age3;
		//age 55 above
		public int age4;
		
		//
		public int age5;
		
		public int age6;
		
		public int age7;
		
		public int age8;
		
		public int age9;
		
		public int male;
		public int female;
		public int happy;
		public int neutral;
		//add by RenZongKe
		public int angry;

		@Deprecated
		public int duration; //this is the total duration in seconds of all faces in this hour.

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
		
		// add by RenZongKe .0-Chinese, 1-Malay, 2-Indian, 3-White, 4-Black
//		public int race_0;
//		public int race_1;
//		public int race_2;
//		public int race_3;
//		public int race_4;
		
		
		
        @Override
        public TickerReport aggregate(TickerReport other,
                                      AggregateOperator operator,
                                      int hourCount)
        {
            AudienceProfilingReport otherReport = (AudienceProfilingReport) other;

            this.age1 += otherReport.age1;
            this.age2 += otherReport.age2;
            this.age3 += otherReport.age3;
            this.age4 += otherReport.age4;

            this.male += otherReport.male;
            this.female += otherReport.female;

            this.happy += otherReport.happy;
            this.neutral += otherReport.neutral;
            this.angry += otherReport.angry;
            
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
            
            //add by RenZongKe
//            this.race_0 += otherReport.race_0;
//            this.race_1 += otherReport.race_1;
//            this.race_2 += otherReport.race_2;
//            this.race_3 += otherReport.race_3;
//            this.race_4 += otherReport.race_4;
            
            return super.aggregate(other, operator, hourCount);
            
        }

		@Override
		public String toString() {
			return "AudienceProfilingReport [age1=" + age1 + ", age2=" + age2 + ", age3=" + age3 + ", age4=" + age4
					+ ", age5=" + age5 + ", age6=" + age6 + ", age7=" + age7 + ", age8=" + age8 + ", age9=" + age9
					+ ", male=" + male + ", female=" + female + ", happy=" + happy + ", neutral=" + neutral + ", angry="
					+ angry + ", duration=" + duration + ", dur0_5s=" + dur0_5s + ", dur5_10s=" + dur5_10s
					+ ", dur10_20s=" + dur10_20s + ", dur20_30s=" + dur20_30s + ", dur30_60s=" + dur30_60s
					+ ", dur1_3m=" + dur1_3m + ", dur3_5m=" + dur3_5m + ", dur5_8m=" + dur5_8m + ", dur8_10m="
					+ dur8_10m + ", dur10_15m=" + dur10_15m + ", dur15_30m=" + dur15_30m + "]";
		}
    }

	public static class AudienceProfilingEventData
	{
		@SerializedName("id")
		public String detailId;
		public float duration;
		public float gender;
		public float genderavg;
		public float smile;
		public float smileavg;
		public float age;
		public float ageavg;
		
		//add by RenZongKe for KAI_X3 audience-profiling
		public boolean isKaiX3 = false;
		public float emotion;
	}

	@Override
	protected Class<AudienceProfilingReport> getCollectionClass() {
		return AudienceProfilingReport.class;
	}

	@Override
	protected UpdateOperations<AudienceProfilingReport> createUpdateOperations(
			Datastore ds, Query<AudienceProfilingReport> query, Class<AudienceProfilingReport> classT, ArchivedEvent event, long timestamp) {
		UpdateOperations<AudienceProfilingReport> ops = ds.createUpdateOperations(classT);
		Gson gson = new Gson();
		AudienceProfilingEventData detail = gson.fromJson(event.getJsonData(), AudienceProfilingEventData.class);
		int duration = (int)detail.duration / 1000; 
		ops.inc("count", 1);
		ops.inc("duration", duration);
		int male, female, happy, neutral,angry, age1, age2, age3, age4;
		male = female = happy = neutral = angry = age1 = age2 = age3 = age4 = 0;

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
		
		/**
		 * * Compare to KAI_X1,the execute output data has changed a lot
     			1) Age:the range of Age value has bean changed  0-[0-3], 1-[4-10], 
     			       2-[11-20], 3-[21-30], 4-[31-40], 5-[41-50], 6-[51-60], 7-[61-70], 8-[71,100]
     			2) Emotion :add a new feature "Angry" 0-Angry,1-Disgust, 2-Fear, 
     			            3-Happy, 4-Sad, 5-Surprise, 6-Neutral
		 */
		if(detail.isKaiX3) {//KAI_X3 parse
			//gender
			if(detail.gender==0f) {
				male++;
			}else {
				female++;
			}
			//age
			if(detail.age==0f||detail.age==1f||detail.age==2f) {
				age1++;
			}else if(detail.age==3f||detail.age==4f) {
				age2++;
			}else if(detail.age==5f||detail.age==6f) {
				age3++;
			}else {
				age4++;
			}
			//emotion
			if(detail.emotion==0f) {
				angry++;
			}else if(detail.emotion==3f) {
				happy++;
			}else {
				neutral++;
			}
			
		}else {//KAI_X1 parse
			if (detail.genderavg < 0.5)
				male++;
			else
				female++;

			if (detail.smileavg < 0.5)
				happy++;
			else
				neutral++;

			if (detail.ageavg < 0.49)
				age1++;
			else if (detail.ageavg < 1.26)
				age2++;
			else if (detail.ageavg < 2.35)
				age3++;
			else
				age4++;
		}

		

		ops.inc("male", male);
		ops.inc("female", female);
		ops.inc("happy", happy);
		ops.inc("neutral", neutral);
		ops.inc("angry", angry);
		ops.inc("age1", age1);
		ops.inc("age2", age2);
		ops.inc("age3", age3);
		ops.inc("age4", age4);

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
		AudienceProfilingReport.col().drop();
	}

	@Override
	public boolean reportExists() {
		return AudienceProfilingReport.col().count() > 0;
	}
	
	
	
	
	
	
}
