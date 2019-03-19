package jobs.queries;

import lib.util.ResultMap;
import models.AuditLog;
import play.Logger;
import play.jobs.Job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuditReportJob extends Job<Map>
{

    private String bucketId;
    private int offset;
    private int max;
    private List<String> activities;

    public AuditReportJob(String bId, int skip, int take, List<String> act)
    {
        bucketId = bId;
        offset = skip;
        max = take;
        activities = new ArrayList<String>();
        activities.addAll(act);
    }

    @Override
    public Map doJobWithResult()
    {
        try
        {
            //Get list of all events of the specified type from backend.
            //This will be a combined list of events for all devices, sorted chronologically.
            //The sorting is done by backend while looking up it's database; latest first.
            List<AuditLog> records = AuditLog.q().filter("bucketId", bucketId)
                    .filter("serviceName in", activities)
                    .offset(offset).limit(max)
                    .asList();

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("report", records);
            return map;
        }
        catch (Exception exp)
        {
            Logger.warn(exp.getMessage());
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", exp.getMessage());
            return map;
        }
    }
}

