package platform.reports.aggregator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

import platform.reports.AnalyticsTracksData.Track;
import platform.reports.CrowdDensityAnalyticsReport;
import platform.reports.CrowdDensityAnalyticsReport.CrowdDensityReport;
import platform.reports.CrowdDensityAnalyticsReport.CrowdDensityReportTrack;

public class CrowdDensityReportAggregator extends RecursiveTask<List<CrowdDensityReport>> {
	
	private Iterable<CrowdDensityReport> iterator;
	
	public CrowdDensityReportAggregator(Iterable<CrowdDensityReport> iterator)
	{
		this.iterator = iterator;
	}
	
	@Override
	protected List<CrowdDensityReport> compute() {
		List<CrowdDensityReport> records = new ArrayList<CrowdDensityReport>();
		List<RecursiveTask<Long>> forks = new LinkedList<RecursiveTask<Long>>();
		for (CrowdDensityReport r : iterator)
		{
			Iterable<CrowdDensityReportTrack> tracks = CrowdDensityAnalyticsReport.retrieveTracks(r);
			AggregationTask task = new AggregationTask(r, tracks);
			forks.add(task);
			task.fork();
			
			records.add(r);
		}
		
		if (records.isEmpty()) {
			return null;
		}
		
		for (RecursiveTask<Long> task : forks)
		{
			task.join();
		}
		forks.clear();
		
		return records;
	}

	private class AggregationTask extends RecursiveTask<Long>
	{
		private CrowdDensityReport record;
		private Iterable<CrowdDensityReportTrack> tracks;
		
		public AggregationTask(CrowdDensityReport r, Iterable<CrowdDensityReportTrack> tracks)
		{
			this.record = r;
			this.tracks = tracks;
		}

		@Override
		protected Long compute() {
			long count = 0;
			
			Track track = new Track();
			record.tracks = new LinkedList<Track>();
			for (CrowdDensityReportTrack t : tracks)
			{
				track.x = t.track.x;
				track.y = t.track.y;
				track.w = t.track.w;
				track.h = t.track.h;
				track.individuals += t.track.individuals;
				track.maxDuration += t.track.maxDuration;
				track.totalDuration += t.track.totalDuration;
				track.value += t.track.value;
				count += t.track.value;
			}
			if (track.individuals == 0 && count > 0)
				track.individuals = 1;
			record.tracks.add(track);
			record.count = count;
			
			return count;
		}
		
	}
}
