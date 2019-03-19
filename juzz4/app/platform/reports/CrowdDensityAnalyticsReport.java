package platform.reports;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.*;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.kaisquare.util.CacheProxy;
import models.Analytics.TickerReport;
import models.archived.ArchivedEvent;
import org.bson.types.CodeWScope;
import platform.analytics.aggregation.AggregateOperator;
import platform.reports.AnalyticsTracksData.Track;
import platform.reports.CrowdDensityAnalyticsReport.CrowdDensityReport;
import platform.reports.aggregator.CrowdDensityReportAggregator;
import play.modules.morphia.Model;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

public class CrowdDensityAnalyticsReport extends TickerAnalyticsReport<CrowdDensityReport> {
	
	protected static final int MIN_GRID_SIZE = 20; 
	
	/**
	 * The Crowd density report that calculates heatmap and values by grids
	 * currently the CrowdDensityReport split data into grids, 
	 * the minimum grid size is defined by {@link CrowdDensityAnalyticsReport#MIN_GRID_SIZE},
	 * the number of columns and rows are split according to the ratio of width or height.
	 * if the width is greater than height, then the height should be split into {@link CrowdDensityAnalyticsReport#MIN_GRID_SIZE}
	 * and width will be split according the each grid size of height.
	 * 
	 * for example:
	 * the resolution of the image size is 320x240, the width is greater than height, then
	 * columns = Math.ceil(320 / (240 / 20))
	 * and each tracks will be defined where the track belongs to by the position of the track (track.x, track.y)
	 */
	@Entity
	@Indexes({
		@Index("deviceId, channelId, date, time"),
		@Index("deviceId, channelId, time"),
		@Index("deviceId, channelId"),
		@Index("deviceId"),
		@Index("time")
	})
	public static class CrowdDensityReport extends TickerReport
	{
		/**
		 * the max count/value of all tracks in the record
		 */
		public int maxValue;
		/**
		 * the max duration of the all individuals the record
		 */
		@Expose(serialize = false)
		public int maxDuration;
		/**
		 * total amount of individuals
		 */
		public long totalIndividuals;
		/**
		 * total numbers of durations 
		 */
		@Expose(serialize = false)
		public long totalDuration;
		/**
		 * total columns split for crowd density report 
		 */
		public int columns;
		/**
		 * total rows split for crowd density report
		 */
		public int rows;
		/**
		 * each tracks in the record
		 */
		@Transient
		public List<Track> tracks;

        @Override
        public TickerReport aggregate(TickerReport other,
                                      AggregateOperator operator,
                                      int hourCount)
        {
            //It does not make sense to add distribution info from different cameras.
            //max and total values might be used for aggregation in the future.
            //but there are already People counting and profiling for that purpose.
            throw new UnsupportedOperationException();
        }
    }
	
	@Entity
	@Indexes({
	    @Index("reportId, time, track.x, track.y, track.w, track.h")
	})
	public static class CrowdDensityReportTrack extends Model
	{
		@Indexed
		public String reportId;
		@Indexed
		public long time;
		public Track track;
	}
	
	private static class DeviceTrackCache implements Serializable
	{
		public long maxValue;
		public long maxDuration;
		public long totalDuration;
		public long totalIndividuals;
		public Track[][] gridTracks;
	}
	 
	private static final Object CACHE_LOCK = new Object();

	private int columns;
	private int rows;
	private double minColWidth;
	private double minRowHeight;
	
	private CacheProxy cacheProxy = CacheProxy.getInstance();
	
	public CrowdDensityAnalyticsReport()
	{
		float maxGridSize = Math.min(WIDTH, HEIGHT) / MIN_GRID_SIZE;
		minColWidth = minRowHeight = maxGridSize; 
		columns = (int) Math.ceil(WIDTH / maxGridSize);
		rows = (int) Math.ceil(HEIGHT / maxGridSize);
	}

	@Override
	public boolean reportExists() {
		return CrowdDensityReport.col().count() > 0;
	}

	@Override
	public void clear() {
		CrowdDensityReport.col().drop();
	}

	@Override
	protected Class<CrowdDensityReport> getCollectionClass() {
		return CrowdDensityReport.class;
	}

	@Override
	public void retention(Date from) {
		super.retention(from);
		
		Datastore ds = getDatastore();
		Query<CrowdDensityReportTrack> query = ds.createQuery(CrowdDensityReportTrack.class);
		query.field("time").lessThanOrEq(from.getTime());
		ds.delete(query);
	}

	@Override
	protected UpdateOperations<CrowdDensityReport> createUpdateOperations(
			Datastore ds, Query<CrowdDensityReport> query, Class<CrowdDensityReport> classT, ArchivedEvent event, long timestamp) {
		
		/*
		 * The crowd event procedure should not process event which is from same device at same time,
		 * which means that there's only one procedure for a device, but the devices could have their own procedures.
		 * Because the crowd event relies on existing data, it needs the previous data for analysis reference.
		 * 
		 * If the crowd-event parser needs to be run in multiple instances, it must implement cross-instance lock mechanism
		 * 
		 */
		UpdateOperations<CrowdDensityReport> ops = ds.createUpdateOperations(classT);
		ops.set("columns", columns);
		ops.set("rows", rows);
		ds.findAndModify(query, ops, false, true);
		
		CrowdDensityReport record = query.get();
		while (record == null) {
			record = record == null ? 
					query.get() : (CrowdDensityReport) record.refresh();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				return null;
			}
		}
		
		DeviceTrackCache cache = null;
		String cacheKey = null;
		//single instance lock
		synchronized (CACHE_LOCK) {
			cacheKey = String.format("crowd-density-%s-%s-%d", 
					event.getEventInfo().getCamera().getCoreDeviceId(),
					event.getEventInfo().getCamera().getChannelId(),
					timestamp);
			cache = (DeviceTrackCache) cacheProxy.get(cacheKey);
			if (cache == null)
				cache = initCache(ds, record, cacheKey);
		}
		
		synchronized (cache) {
			int maxValue;
			long maxDuration, totalDuration, totalIndividuals;
			maxValue = record.maxValue;
			maxDuration = record.maxDuration;
			totalDuration = record.totalDuration;
			totalIndividuals = record.totalIndividuals;
			AnalyticsTracksData data = new Gson().fromJson(event.getJsonData(), AnalyticsTracksData.class);
			List<Track> tracks = data.toTracks();
			for (Track t : tracks)
			{
				processGrid(cache, AnalyticsTracksData.convertTrack(t));
			}
			saveGridTracks(ds, record, cache);
			
			long deltaMaxValue = cache.maxValue - maxValue;
			long deltaMaxDuration = cache.maxDuration - maxDuration;
			long deltaTotalDuration = cache.totalDuration - totalDuration;
			long deltaTotalIndividuals = cache.totalIndividuals - totalIndividuals;
			if (deltaMaxValue > 0 || deltaMaxDuration > 0)
			{
				ops = ds.createUpdateOperations(classT);
				if (deltaMaxValue > 0)
					ops.inc("maxValue", deltaMaxValue);
				if (deltaMaxDuration > 0)
					ops.inc("maxDuration", deltaMaxDuration);
				if (deltaTotalDuration > 0)
					ops.inc("totalDuration", deltaTotalDuration);
				if (deltaTotalIndividuals > 0)
					ops.inc("totalIndividuals", deltaTotalIndividuals);
				ds.findAndModify(query, ops, false, true);
			}
		}
		
		return null;
	}
	
	private DeviceTrackCache initCache(Datastore ds, CrowdDensityReport record, String cacheKey) {
		DeviceTrackCache devCache = new DeviceTrackCache();
		initGrid(ds, record, devCache);
		devCache.maxValue = record.maxValue;
		devCache.maxDuration = record.maxDuration;
		devCache.totalDuration = record.totalDuration;
		devCache.totalIndividuals = record.totalIndividuals;
		cacheProxy.set(cacheKey, devCache);
		return devCache;
	}

	private void initGrid(Datastore ds, CrowdDensityReport record, DeviceTrackCache cache)
	{
		cache.gridTracks = new Track[columns][rows];
		
		Query<CrowdDensityReportTrack> query = ds.createQuery(CrowdDensityReportTrack.class);
		query.and(query.criteria("reportId").equal(record.getIdAsStr()));
		Iterable<CrowdDensityReportTrack> tracks = query.fetch();
		for (CrowdDensityReportTrack t : tracks)
		{
			Track track = cache.gridTracks[(int) t.track.x][(int) t.track.y];
			if (track == null)
			{
				track = new Track();
				cache.gridTracks[(int) t.track.x][(int) t.track.y] = track;
			}
			track.reportId = record.getIdAsStr();
			track.x = t.track.x;
			track.y = t.track.y;
			track.w = t.track.w;
			track.h = t.track.h;
			track.duration = t.track.duration;
			track.maxDuration = t.track.maxDuration;
			track.value = t.track.value;
			track.time = t.track.time;
			track.timestamp = t.track.timestamp;
			track.totalDuration = t.track.totalDuration;
			track.individuals = t.track.individuals;
		}
	}

	private void updateGridTrack(Datastore ds, CrowdDensityReport record, Track t)
	{
		Query<CrowdDensityReportTrack> trackQ = ds.createQuery(CrowdDensityReportTrack.class);
		UpdateOperations<CrowdDensityReportTrack> trackOps = ds.createUpdateOperations(CrowdDensityReportTrack.class);
		trackQ.and(trackQ.criteria("reportId").equal(record.getIdAsStr()))
			  .and(trackQ.criteria("time").equal(record.time))
			  .and(trackQ.criteria("track.x").equal(t.x))
			  .and(trackQ.criteria("track.y").equal(t.y))
			  .and(trackQ.criteria("track.w").equal(t.w))
			  .and(trackQ.criteria("track.h").equal(t.h));
		trackOps.set("track.value", t.value);
		trackOps.set("track.duration", t.duration);
		trackOps.set("track.maxDuration", t.maxDuration);
		trackOps.set("track.time", t.time);
		trackOps.set("track.timestamp", t.timestamp);
		trackOps.set("track.totalDuration", t.totalDuration);
		trackOps.set("track.individuals", t.individuals);
		ds.findAndModify(trackQ, trackOps, false, true);
	}
	
	protected void processGrid(DeviceTrackCache cache, Track track)
	{
		int c = (int) Math.floor(track.x / minColWidth);
		int r = (int) Math.floor(track.y / minRowHeight);
		processGrid(cache, c, r, track);
	}
	
	private void processGrid(DeviceTrackCache cache, int c, int r, Track t)
    {
		if (c < 0 || r < 0 || c >= columns || r >= rows)
    		return;
			
        Track[][] gridTracks = cache.gridTracks;
        Track track = gridTracks[c][r];
        if (track == null) {
            track = new Track();
            gridTracks[c][r] = track;
        }
        if (track._id == null)
            track._id = t._id;
        if (track.reportId == null)
            track.reportId = t.reportId;
        track.x = c;
        track.y = r;
        track.w = minColWidth;
        track.h = minRowHeight;
        track.time = t.time;
        track.value++;
        cache.maxValue = Math.max((int) track.value, cache.maxValue);

        double ms = AnalyticsTracksData.calcDuration(track, t);
        if (ms > 0) {
            track.duration += ms;
            track.maxDuration = Math.max(track.duration, track.maxDuration);
            cache.maxDuration = Math.max(track.duration, cache.maxDuration);
        }
        else
        {
        	if (track.duration > 0)
        	{
            	track.totalDuration += track.duration;
                track.individuals++;
                cache.totalDuration += track.duration;
                cache.totalIndividuals++;
        	}
            
            track.duration = 0;
        }

        track.timestamp = t.timestamp;
    }

    private void saveGridTracks(Datastore ds, CrowdDensityReport record, DeviceTrackCache cache)
	{
		Track[][] gridTracks = cache.gridTracks;
		for (int i = 0; i < gridTracks.length; i++)
		{
			for (int j = 0; j < gridTracks[i].length; j++)
			{
				Track t = gridTracks[i][j];
				if (t == null || (t.value == 0 && t.duration == 0))
					continue;
				
				updateGridTrack(ds, record, t);
			}
		}
	}

	protected int processTrack(List<Track> tracks, Track track)
	{
		if (tracks.size() == 0)
		{
			tracks.add(track);
			return 1;
		}
		int max = 1;
		Track last = tracks.get(tracks.size() - 1);
		double d = AnalyticsTracksData.calcDistance(last, track);
		long t = AnalyticsTracksData.calcDuration(last, track);
		if (d <= MIN_WIDTH && d <= MIN_HEIGHT && t > 0)
		{
			for (int i = tracks.size() - 1; i >= 0; i--)
			{
				Track pTrack = tracks.get(i);
				double d1 = AnalyticsTracksData.calcDistance(pTrack, track);
				long t1 = AnalyticsTracksData.calcDuration(track, pTrack);
				if (d1 <= MIN_WIDTH && d1 <= MIN_HEIGHT && t1 > 0)
				{
					pTrack.duration += t1;
					pTrack.value += t1;
					max = Math.max(max, (int)pTrack.value);
					Track p1 = pTrack;
					Track p2 = track;
					double x = (p1.x + p2.x) / 2;
					double y = (p1.y + p2.y) / 2;
					pTrack.x = x;
					pTrack.y = y;
					pTrack.w = track.w;
					pTrack.h = track.h;
					pTrack.id = track.id;
					pTrack.time = track.time;
					pTrack.timestamp = track.timestamp;
					track = pTrack;
					tracks.remove(i);
				}
			}
		}
		
		tracks.add(track);
		
		return max;
	}
	
	protected int accessCircle(int[][] arr, int x, int y, int r, int v, int max)
	{
		int r2 = r * r;
		for (int i = x - r; i < x + r; i++)
		{
			for (int j = y - r; j < y + r; j++)
			{
				if (i < 0 || j < 0 || i >= WIDTH || j >= HEIGHT)
					continue;
				int dx = x - i;
				int dy = y - j;
				int d2 = dx * dx + dy * dy;
				if (d2 <= r2)
				{
					if (v != 0)
						arr[i][j] += v;
					else
						arr[i][j] = r2 - d2;
					
					max = Math.max(arr[i][j], max);
				}
			}
		}
		
		return max;
	}
	
	protected void normalize(List<Track> tracks, int max)
	{
		for (Track t : tracks)
		{
			t.value = Math.round(t.value / max * 100000) / 10000;
		}
	}
	
	protected Track convertTrack(CrowdDensityReportTrack t)
	{
		Track track = new Track();
		track._id = t.getIdAsStr();
		track.reportId = t.reportId;
		track.id = t.track.id;
		track.x = t.track.x;
		track.y = t.track.y;
		track.w = t.track.w;
		track.h = t.track.h;
		track.value = t.track.value;
		track.duration = t.track.duration;
		track.time = t.track.time;
		track.timestamp = t.track.timestamp;
		return track; //convertTrack(track);
	}
	
	protected Track rollbackTrack(Track t, String reportId)
	{
		double w = t.w / WIDTH;
		double h = t.h / HEIGHT;
		double x = t.x / WIDTH;
		double y = t.y / HEIGHT;
		
		Track track = new Track();
		track._id = t._id;
		track.reportId = t.reportId;
		track.id = t.id;
		track.x = x;
		track.y = y;
		track.w = w;
		track.h = h;
		track.value = t.value;
		track.duration = t.duration;
		track.time = t.time;
		track.timestamp = t.timestamp;
		
		return track;
	}

	/**
	 * available parameters:<br />
	 * aggregative (boolean): aggregative to aggregate all of result
	 * @see platform.reports.TickerAnalyticsReport#query(java.util.List, java.lang.String, java.util.Date, java.util.Date)
	 */
	@Override
	public ReportQuery<CrowdDensityReport> query(Date from, Date to) {
		
		Query<CrowdDensityReport> query = super.getInternalQuery();
		CrowdDensityReportQuery reportQuery = new CrowdDensityReportQuery(query);
		
		return reportQuery.setDateFrom(from).setDateTo(to);
	}		
	
	public static Iterable<CrowdDensityReportTrack> retrieveTracks(CrowdDensityReport r) {
		return CrowdDensityReportTrack.find("reportId", r.getIdAsStr())
				.retrievedFields(true, "track").fetch();
	}
	
	static final class CrowdDensityReportQuery extends DefaultReportQuery<CrowdDensityReport>
	{
		CrowdDensityReportQuery(Query<CrowdDensityReport> query) {
			super(query);
		}

		@Override
		public Query<CrowdDensityReport> getQuery() {
			boolean aggregative = false;
			if (parameters.containsKey("aggregative"))
			{
				Object obj = parameters.get("aggregative");
				if (obj instanceof Boolean)
					aggregative = Boolean.valueOf((boolean)obj);
				else
					throw new IllegalArgumentException("aggregative is not type 'boolean'");
			}
			
			return new CrowdDensityQuery(super.getQuery(), aggregative);
		}
		
	}
	
	private static final class CrowdDensityQuery extends DecorativeQuery<CrowdDensityReport> 
		implements Iterable<CrowdDensityReport>, Iterator<CrowdDensityReport>
	{

		private boolean aggregative;
		private List<CrowdDensityReport> list;
		private Iterator<CrowdDensityReport> iterator;
		
		public CrowdDensityQuery(Query<CrowdDensityReport> query, boolean aggregative) {
			super(query);
			this.aggregative = aggregative;
		}
		
		private void fillTracks(CrowdDensityReport r)
		{
			r.tracks = new LinkedList<Track>();
			Iterable<CrowdDensityReportTrack> list = retrieveTracks(r);
			if (list != null)
			{
				if (aggregative)
				{
					Track track = new Track();
					for (CrowdDensityReportTrack t : list)
					{
						track.x = t.track.x;
						track.y = t.track.y;
						track.w = t.track.w;
						track.h = t.track.h;
						track.individuals += t.track.individuals;
						track.maxDuration += t.track.maxDuration;
						track.totalDuration += t.track.totalDuration;
						track.value += t.track.value;
					}
					if (track.individuals == 0 && track.value > 0)
						track.individuals = 1;
					r.tracks.add(track);
					r.count = (long) track.value;
				}
				else
				{
					for (CrowdDensityReportTrack t : list)
					{
						r.tracks.add(t.track);
						r.count += t.track.value;
					}
				}
			}
		}
		
		@Override
		public Iterable<CrowdDensityReport> fetch()
		{
			return this;
		}

		@Override
		public List<CrowdDensityReport> asList() {
			Iterable<CrowdDensityReport> iterator = super.fetch();
			
			if (aggregative)
			{
				CrowdDensityReportAggregator aggregator = new CrowdDensityReportAggregator(iterator);
				ForkJoinPool pool = new ForkJoinPool();
				list = pool.invoke(aggregator);
				pool.shutdown();
			}
			else
			{
				list = new ArrayList<CrowdDensityReport>();
				for (CrowdDensityReport r : iterator)
				{
					fillTracks(r);
					list.add(r);
				}
			}
			
			return list;
		}

		@Override
		public Query<CrowdDensityReport> filter(String condition, Object value) {
			super.filter(condition, value);
			return this;
		}

		@Override
		public Query<CrowdDensityReport> limit(int value) {
			super.limit(value);
			return this;
		}

		@Override
		public Query<CrowdDensityReport> batchSize(int value) {
			super.batchSize(value);
			return this;
		}

		@Override
		public Query<CrowdDensityReport> offset(int value) {
			super.offset(value);
			return this;
		}

		@Override
		public Query<CrowdDensityReport> enableValidation() {
			super.enableValidation();
			return this;
		}

		@Override
		public Query<CrowdDensityReport> disableValidation() {
			super.disableValidation();
			return this;
		}

		@Override
		public Query<CrowdDensityReport> hintIndex(String idxName) {
			super.hintIndex(idxName);
			return this;
		}

		@Override
		public Query<CrowdDensityReport> retrievedFields(boolean include, String... fields) {
			super.retrievedFields(include, fields);
			return this;
		}

		@Override
		public Query<CrowdDensityReport> enableSnapshotMode() {
			super.enableSnapshotMode();
			return this;
		}

		@Override
		public Query<CrowdDensityReport> disableSnapshotMode() {
			super.disableSnapshotMode();
			return this;
		}

		@Override
		public Query<CrowdDensityReport> queryNonPrimary() {
			super.queryNonPrimary();
			return this;
		}

		@Override
		public Query<CrowdDensityReport> queryPrimaryOnly() {
			super.queryPrimaryOnly();
			return this;
		}

		@Override
		public Query<CrowdDensityReport> disableCursorTimeout() {
			super.disableCursorTimeout();
			return this;
		}

		@Override
		public Query<CrowdDensityReport> enableCursorTimeout() {
			super.enableCursorTimeout();
			return this;
		}

		@Override
		public Class<CrowdDensityReport> getEntityClass() {
			return super.getEntityClass();
		}

		@Override
		public Query<CrowdDensityReport> clone() {
			throw new UnsupportedOperationException("function not supported");
		}

		@Override
		public Query<CrowdDensityReport> where(String js) {
			super.where(js);
			return this;
		}

		@Override
		public Query<CrowdDensityReport> where(CodeWScope js) {
			super.where(js);
			return this;
		}

		@Override
		public Query<CrowdDensityReport> order(String condition) {
			super.order(condition);
			return this;
		}

		@Override
		public Query<CrowdDensityReport> skip(int value) {
			super.skip(value);
			return this;
		}

		@Override
		public CrowdDensityReport get() {
			CrowdDensityReport r = super.get();
			fillTracks(r);
			return r;
		}

		@Override
		public long countAll() {
			return list != null ? list.size() : super.countAll();
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public CrowdDensityReport next() {
			CrowdDensityReport r = iterator.next();
			fillTracks(r);
			return r;
		}

		@Override
		public void remove() {
			iterator.remove();
		}

		@Override
		public Iterator<CrowdDensityReport> iterator() {
			iterator = super.iterator();
			return this;
		}

		@Override
		public Iterator<CrowdDensityReport> tail() {
			iterator = getRawQuery().tail();
			return this;
		}

		@Override
		public Iterator<CrowdDensityReport> tail(boolean awaitData) {
			iterator = getRawQuery().tail(awaitData);
			return this;
		}
	}
}
