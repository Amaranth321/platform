package platform.reports;

import java.awt.Rectangle;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import javax.sound.midi.Track;

import play.Logger;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Transient;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AnalyticsTracksData {
	
	protected static final long MAX_DURATION_PER_TRACK = 6000;

	public String tracks;
	
	private List<Track> _tracks;
	
	public List<Track> toTracks()
	{
		synchronized (this)
		{
			if (_tracks == null)
			{
				SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				format.setTimeZone(TimeZone.getTimeZone("UTC"));
				_tracks = new Gson().fromJson(tracks, new TypeToken<List<Track>>(){}.getType());
				for (Track t : _tracks)
				{
					try {
						t.timestamp = t.timeMillis > 0 ? t.timeMillis : format.parse(t.time).getTime();
					} catch (ParseException e) {
						Logger.error(e, "error parsing time '%s'", t.time);
					}
				}
				Collections.sort(_tracks, TrackComparator.INSTANCE);
			}
		}
		ArrayList<Track> list = new ArrayList<Track>();
		list.addAll(_tracks);
		return list;
	}
	
	public static long calcDuration(Track t1, Track t2)
	{
		long ts = t2.timestamp - t1.timestamp;
		if (ts >= MAX_DURATION_PER_TRACK)
			return 0;
		else
			return ts;
	}
	
	public static double calcDistance(Track t1, Track t2)
	{
		double a = t1.x - t2.x;
		double b = t1.y - t2.y;
		return Math.sqrt(a * a + b * b);
	}
	
	/**
	 * Convert the {@link Track} that position and size are the percentage to the absolute values of specific resolution
	 * @param t
	 * @return
	 */
	public static Track convertTrack(Track t)
	{
		double w = t.w * TickerAnalyticsReport.WIDTH;
		double h = t.h * TickerAnalyticsReport.HEIGHT;
		int x = (int) Math.round(t.x * TickerAnalyticsReport.WIDTH);
		int y = (int) Math.round(t.y * TickerAnalyticsReport.HEIGHT);
		
		Track track = new Track();
		track._id = t._id;
		track.reportId = t.reportId;
		track.id = t.id;
		track.x = x;
		track.y = y;
		track.w = w;
		track.h = h;
		track.value = t.value == 0 ? 1 : t.value;
		track.duration = t.duration;
		track.time = t.time;
		track.timestamp = t.timestamp;
		
		return track;
	}
	
	/**
	 * The Track is stored in database for report process, the Track is converted from VCA's track data
	 * it will be used for any VCA that has tracks of each detected objects
	 */
	@Embedded
	public static class Track implements Serializable
	{
		@Transient
		@Expose(serialize = false)
		public String _id;
		@Transient
		@Expose(serialize = false)
		public String reportId;
		@Expose(serialize = false)
		public int id;
		@Expose(serialize = false)
		public String time;
		/**
		 * The position of the track (x-axis)
		 */
		public double x;
		/**
		 * The position of the track (y-axis)
		 */
		public double y;
		@Expose(serialize = false)
		public double w;
		@Expose(serialize = false)
		public double h;
		/**
		 * total of counted objects/tracks
		 */
		public double value;
		/**
		 * total of individuals detected from tracks
		 */
		public int individuals;
		/**
		 * current duration of the track
		 */
		@Expose(serialize = false)
		public int duration;
		/**
		 * total duration of the tracks
		 */
		@Expose(serialize = false)
		public int totalDuration;
		/**
		 * the max duration of all processed track
		 */
		@Expose(serialize = false)
		public int maxDuration;
		/**
		 * the last timestmap of the track
		 */
		@Expose(serialize = false)
		public long timestamp;
		/**
		 * The timestamp parsed from vca event
		 */
		@Expose(serialize = false)
		public long timeMillis;
	}
	
	public static class TrackComparator implements Comparator<Track>
	{
		public static final TrackComparator INSTANCE = new TrackComparator();

		@Override
		public int compare(Track o1, Track o2) {
			return o1.timestamp == o2.timestamp && o1.id == o2.id ? 0 :
				(o1.timestamp == o2.timestamp && o1.id > o2.id ? 1 : 
					(o1.timestamp > o2.timestamp ? 1 : -1));
		}
		
	}

}
