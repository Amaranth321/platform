package jobs.queries;

import models.RecordingUploadRequest;
import models.transportobjects.UploadRequestTransport;
import platform.db.QueryHelper;
import platform.devices.DeviceChannelPair;
import platform.time.UtcPeriod;
import play.jobs.Job;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class QueryRecordingUploadRequests extends Job<List<UploadRequestTransport>>
{
    private final Model.MorphiaQuery query = RecordingUploadRequest.q();

    public QueryRecordingUploadRequests(DeviceChannelPair camera, UtcPeriod period)
    {
        query.order("-_created");

        if (camera != null)
        {
            query.filter("camera", camera);
        }

        if (period != null)
        {
            QueryHelper.mustOverlap(query, "period.from", "period.to", period);
        }
    }

    @Override
    public List<UploadRequestTransport> doJobWithResult()
    {
        Iterable<RecordingUploadRequest> requests = query.fetch();
        List<UploadRequestTransport> transportList = new ArrayList<>();
        for (RecordingUploadRequest uploadRequest : requests)
        {
            if (uploadRequest.isEffectivelyEmpty())
            {
                uploadRequest.delete();
                continue;
            }

            transportList.add(new UploadRequestTransport(uploadRequest));
        }

        return transportList;
    }
}
