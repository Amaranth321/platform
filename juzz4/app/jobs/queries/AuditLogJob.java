/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jobs.queries;

import lib.util.ResultMap;
import models.AuditLog;
import play.Logger;
import play.jobs.Job;
import play.modules.morphia.Model.MorphiaQuery;

import java.util.List;
import java.util.Map;

/**
 * @author user
 */
public class AuditLogJob extends Job<Map>
{

    private String bucketName;
    private String userName;
    private Long dtFrom;
    private Long dtTo;
    private String ip;
    private int offset;
    private int max;
    private String activities;

    public AuditLogJob(String bucketName,
                       String userName,
                       Long dtFrom,
                       Long dtTo,
                       String ip,
                       int offset,
                       int max,
                       String activities)
    {
        this.bucketName = bucketName;
        this.userName = userName;
        this.dtFrom = dtFrom;
        this.dtTo = dtTo;
        this.ip = ip;
        this.offset = offset;
        this.max = max;
        this.activities = activities;
    }

    @Override
    public Map doJobWithResult()
    {
        try
        {
            //Get list of all events of the specified type from backend.
            //This will be a combined list of events for all devices, sorted chronologically.
            //The sorting is done by backend while looking up it's database; latest first.
            List<AuditLog> auditLogs;
            MorphiaQuery query = AuditLog.q();
            if (bucketName.isEmpty() == false)
            {
                query.filter("bucketName", bucketName);
            }
            if (userName.isEmpty() == false)
            {
                query.filter("userName", userName);
            }
            if (dtFrom != null)
            {
                query.filter("timeobject >=", dtFrom);
            }
            if (dtTo != null)
            {
                query.filter("timeobject <=", dtTo);
            }
            if (ip.isEmpty() == false)
            {
                query.filter("remoteIp", ip);
            }
            if (activities.isEmpty() == false)
            {
                query.filter("serviceName in", activities);
            }
            if (offset > 0)
            {
                query.offset(offset);
            }
            if (max > 0)
            {
                query.limit(max);
            }
            query.order("-timeobject");
            auditLogs = query.asList();
            int totalAudit;
            totalAudit = (int) query.countAll();

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("totalcount", totalAudit);
            map.put("auditLogs", auditLogs);
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
