package platform.reports;

import java.util.*;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.gson.Gson;
import com.kaisquare.util.CacheProxy;

import lib.util.VcaUtils;
import lib.util.exceptions.DeviceNotExistsException;
import models.MongoDevice;
import platform.analytics.*;
import models.Analytics.TickerReport;
import models.archived.ArchivedEvent;
import platform.analytics.app.AppVcaTypeMapper;
import platform.devices.DeviceChannelPair;
import platform.analytics.aggregation.AggregateOperator;
import platform.reports.AnalyticsTracksData.Track;
import platform.reports.TrafficFlowAnalyticsReport.TrafficFlowReport;
import play.modules.morphia.Model;

public class TrafficFlowAnalyticsReport extends TickerAnalyticsReport<TrafficFlowReport> {

	@Entity
	@Indexes({
		@Index("deviceId, channelId, date, time"),
		@Index("deviceId, channelId, time, from, to"),
		@Index("deviceId, channelId, time"),
		@Index("deviceId, channelId"),
		@Index("deviceId"),
		@Index("time")
	})
	public static class TrafficFlowReport extends TickerReport
	{
		/**
		 * The source of the object moves from
		 */
		public String from;
		/**
		 * The source of the object moves to
		 */
		public String to;

        @Override
        public TickerReport aggregate(TickerReport other,
                                      AggregateOperator operator,
                                      int hourCount)
        {
            //It does not make sense to add traffic flows from different cameras
            throw new UnsupportedOperationException();
        }
    }

	@Entity
	public static class TrafficFlowReportTrack extends Model
	{
		/**
		 * The tracks that belong to
		 */
		@Indexed
		public String key;
		@Indexed
		public long time;
		public List<RegionTrack> tracks;
	}

	@Embedded
	private static class RegionTrack
	{
		public String region;
		public Track track;
	}

	private static class DeviceTrackCache
	{
		public long time;
		public List<RegionTrack> tracks;
	}

	private static final Object LOCK = new Object();

	private CacheProxy cacheProxy = CacheProxy.getInstance();

	@Override
	public boolean reportExists() {
		return TrafficFlowReport.col().count() > 0;
	}

	@Override
	public void clear() {
		TrafficFlowReport.col().drop();
	}

	@Override
	protected Class<TrafficFlowReport> getCollectionClass() {
		return TrafficFlowReport.class;
	}

	@Override
	public void retention(Date from) {
		super.retention(from);

		Datastore ds = getDatastore();
		Query<TrafficFlowReportTrack> query = ds.createQuery(TrafficFlowReportTrack.class);
		query.field("time").lessThanOrEq(from.getTime());
		ds.delete(query);
	}

	@Override
	protected UpdateOperations<TrafficFlowReport> createUpdateOperations(
			Datastore ds, Query<TrafficFlowReport> query,
			Class<TrafficFlowReport> classT, ArchivedEvent event, long timestamp) {

		VcaInfo instance = getVcaInfo(event);
		if (instance == null)
			return null;
		List<PolygonRegion> regions = getVcaRegion(instance);
		if (regions == null)
			return null;
		AnalyticsTracksData data = new Gson().fromJson(event.getJsonData(), AnalyticsTracksData.class);
		List<Track> tracks = data.toTracks();

		DeviceTrackCache cache = null;
		String cacheKey;
		synchronized (LOCK) {
			cacheKey = getCacheKey(event.getEventInfo().getCamera().getCoreDeviceId(), event.getEventInfo().getCamera().getChannelId(), timestamp);
			cache = (DeviceTrackCache) cacheProxy.get(cacheKey);
			if (cache == null)
				cache = initCache(ds, cacheKey);
			cache.time = timestamp;
		}

		synchronized (cache) {
			processTracks(ds, query, regions ,cache, tracks);
			saveTracks(ds, cacheKey, cache);
		}

		return null;
	}

	private String getCacheKey(String deviceId, String channelId, long timestamp) {
		return String.format("traffic-flow-%s-%s-%d",
				deviceId, channelId, timestamp);
	}

	private DeviceTrackCache initCache(Datastore ds, String cacheKey) {
		DeviceTrackCache cache = new DeviceTrackCache();
		initTracks(ds, cacheKey, cache);
		cacheProxy.set(cacheKey, cache);

		return cache;
	}

	private void saveTracks(Datastore ds, String cacheKey, DeviceTrackCache cache) {
		Query<TrafficFlowReportTrack> query = ds.createQuery(TrafficFlowReportTrack.class);
		query.and(query.criteria("key").equal(cacheKey))
			 .and(query.criteria("time").equal(cache.time));
		UpdateOperations<TrafficFlowReportTrack> ops = ds.createUpdateOperations(TrafficFlowReportTrack.class);
		ops.set("tracks", cache.tracks);
		ds.findAndModify(query, ops, false, true);
	}

	private void initTracks(Datastore ds, String cacheKey, DeviceTrackCache cache) {
		TrafficFlowReportTrack track = TrafficFlowReportTrack.find("key", cacheKey).first();
		cache.tracks = Collections.synchronizedList(new ArrayList<RegionTrack>());
		if (track != null && track.tracks != null)
			cache.tracks.addAll(track.tracks);
	}

	private void processTracks(Datastore ds, Query<TrafficFlowReport> query,
			List<PolygonRegion> regions, DeviceTrackCache cache, List<Track> tracks) {
		for (Track t : tracks)
		{
            Track track = AnalyticsTracksData.convertTrack(t);
			int index = findLastTrack(cache, track);
			if (index != -1)
			{
				RegionTrack lastTrack = cache.tracks.get(index);
				RegionTrack currentRegion = getRegionTrack(regions, track);
				if (currentRegion != null && !lastTrack.region.equals(currentRegion.region)
					&& assumeSameTrack(lastTrack.track, track))
				{
					updateTrafficCount(ds, query, lastTrack.region, currentRegion.region);
				}
			}

			addTrack(regions, cache, track);
		}
	}

	private int findLastTrack(DeviceTrackCache cache, Track track) {
		List<RegionTrack> list = cache.tracks;
		int lb = 0, ub = list.size() - 1;
		int m = -1;
		while (lb <= ub) {
			m = (lb + ub) / 2;
			if (track.id < list.get(m).track.id)
				ub = m - 1;
			else if (track.id > list.get(m).track.id)
				lb = m + 1;
			else
				break;
		}

		return m;
	}

	private void addTrack(List<PolygonRegion> regions, DeviceTrackCache cache, Track track) {
		RegionTrack regionTrack = getRegionTrack(regions, track);
		int index = findLastTrack(cache, track);
		if (index == -1)
		{
			if (regionTrack != null)
				cache.tracks.add(0, regionTrack);
		}
		else
		{
			RegionTrack lastTrack = cache.tracks.get(index);
			boolean replaceExisting = assumeSameTrack(lastTrack.track, track);

			if (regionTrack != null)
			{
				if (track.id >= lastTrack.track.id)
					cache.tracks.add(index + 1, regionTrack);
				else
					cache.tracks.add(index++, regionTrack);
			}
			if (replaceExisting)
				cache.tracks.remove(index);
		}
	}

	private boolean assumeSameTrack(Track t1, Track t2)
	{
		long duration = AnalyticsTracksData.calcDuration(t1, t2);
		double d = AnalyticsTracksData.calcDistance(t1, t2);
		return (t1.id == t2.id)
				|| (Math.abs(t2.id - t1.id) <= 3
				&& d < TickerAnalyticsReport.MIN_WIDTH && d < TickerAnalyticsReport.MIN_HEIGHT
				&& duration > 0);
	}

	private RegionTrack getRegionTrack(List<PolygonRegion> regions, Track track)
	{
		for (PolygonRegion region : regions)
		{
			if (isInRegion(region, track))
			{
				RegionTrack regionTrack = new RegionTrack();
				regionTrack.region = region.name;
				regionTrack.track = track;
				return regionTrack;
			}
		}

		return null;
	}

	private boolean isInRegion(PolygonRegion region, Track track)
	{
		boolean isInRegion = false;
		List<NormalizedPoint> points = region.points;
		int i, j = points.size() - 1;

		for (i = 0; i < points.size(); i++)
		{
			NormalizedPoint point1 = points.get(i);
			NormalizedPoint point2 = points.get(j);
			double polyX1 = Math.round(point1.getX() * TickerAnalyticsReport.WIDTH);
			double polyY1 = Math.round(point1.getY() * TickerAnalyticsReport.HEIGHT);
			double polyX2 = Math.round(point2.getX() * TickerAnalyticsReport.WIDTH);
			double polyY2 = Math.round(point2.getY() * TickerAnalyticsReport.HEIGHT);

			if ((polyY1 < track.y && polyY2 >= track.y
					|| polyY2 < track.y && polyY1 >= track.y)
					&&  (polyX1 <= track.x || polyX2 <= track.x)) {
				if (polyX1 + (track.y - polyY1) / (polyY2 - polyY1) * (polyX2 - polyX1) < track.x)
				{
					isInRegion = !isInRegion;
				}
			}
			j = i;
		}

		return isInRegion;
	}

	private void updateTrafficCount(Datastore ds, Query<TrafficFlowReport> query, String from, String to) {
		query.and(query.criteria("from").equal(from))
			 .and(query.criteria("to").equal(to));

		UpdateOperations<TrafficFlowReport> ops = ds.createUpdateOperations(TrafficFlowReport.class);
		ops.inc("count");
		ds.findAndModify(query, ops, false, true);
	}

	private List<PolygonRegion> getVcaRegion(VcaInfo vcaInfo)
	{
        Map<String, String> jsonMap = new Gson().fromJson(vcaInfo.getSettings(), Map.class);

        List<PolygonRegion> regions = null;
        try {
			regions = VcaUtils.parsePolygonRegions(jsonMap.get("regions"));
		} catch (Exception e) {}

        return regions;
	}

	private VcaInfo getVcaInfo(ArchivedEvent event)
	{
        DeviceChannelPair camera = event.getEventInfo().getCamera();
		MongoDevice device = camera.getDbDevice();
        if (device == null)
			throw new DeviceNotExistsException(event.getEventInfo().getCamera().getCoreDeviceId());

        List<IVcaInstance> vcaList = VcaManager.getInstance().listVcaInstances(Arrays.asList(camera.getCoreDeviceId()));
        for (IVcaInstance instance : vcaList)
        {
            VcaType vcaType = AppVcaTypeMapper.getVcaType(instance.getVcaInfo().getAppId());
            if (vcaType == VcaType.TRAFFIC_FLOW &&
                instance.getVcaInfo().getCamera().equals(camera))
            {
                return instance.getVcaInfo();
            }
        }

        return null;
	}
}
