package jobs.queries;

import com.google.code.morphia.query.Criteria;
import lib.util.Util;
import models.abstracts.ServerPagedResult;
import models.node.PrecompiledNodeInfo;
import platform.db.QueryHelper;
import platform.devices.DeviceStatus;
import platform.nodesoftware.NodeSoftwareStatus;
import play.jobs.Job;
import play.modules.morphia.Model;

import java.util.Arrays;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class QueryPrecompiledNodeInfo extends Job<ServerPagedResult<PrecompiledNodeInfo>>
{
    private final Model.MorphiaQuery query;

    private final List<String> keywordSearchableFields = Arrays.asList(
            "bucketName",
            "nodeName",
            "version",
            "timezone",
            "modelName",
            "ipAddress",
            "mac"
    );

    private int offset = 0;
    private int limit = 0;

    public QueryPrecompiledNodeInfo(String sortBy)
    {
        if (Util.isNullOrEmpty(sortBy))
        {
            sortBy = "bucketName"; // default ordering
        }

        query = PrecompiledNodeInfo.q();
        query.order(sortBy);
    }

    public QueryPrecompiledNodeInfo offset(int offset)
    {
        this.offset = offset;
        return this;
    }

    public QueryPrecompiledNodeInfo limit(int limit)
    {
        this.limit = limit;
        return this;
    }

    public QueryPrecompiledNodeInfo keyword(String keyword)
    {
        Criteria[] criteriaArray = new Criteria[keywordSearchableFields.size()];
        for (int i = 0; i < keywordSearchableFields.size(); i++)
        {
            String fieldName = keywordSearchableFields.get(i);
            criteriaArray[i] = query.criteria(fieldName).containsIgnoreCase(keyword);
        }

        query.or(criteriaArray);
        return this;
    }

    public QueryPrecompiledNodeInfo platformDeviceId(long platformDeviceId)
    {
        query.filter("platformDeviceId", platformDeviceId);
        return this;
    }

    public QueryPrecompiledNodeInfo platformDeviceIdIn(List<Long> platformDeviceIds)
    {
        Criteria[] criteriaArray = new Criteria[platformDeviceIds.size()];
        for (int i = 0; i < platformDeviceIds.size(); i++)
        {
            criteriaArray[i] = query.criteria("platformDeviceId").equal(platformDeviceIds.get(i));
        }

        query.or(criteriaArray);
        return this;
    }

    public QueryPrecompiledNodeInfo coreDeviceId(String coreDeviceId)
    {
        query.filter("coreDeviceId", coreDeviceId);
        return this;
    }

    public QueryPrecompiledNodeInfo nodeOffline()
    {
        query.filter("status", DeviceStatus.DISCONNECTED);
        return this;
    }

    public QueryPrecompiledNodeInfo containsOfflineCamera()
    {
        query.filter("offlineCameraCount >", 0);
        return this;
    }

    public QueryPrecompiledNodeInfo containsUnstableVca()
    {
        query.filter("unstableVcaCount >", 0);
        return this;
    }

    public QueryPrecompiledNodeInfo updateAvailable()
    {
        query.filter("softwareStatus", NodeSoftwareStatus.UPDATE_AVAILABLE);
        return this;
    }

    @Override
    public ServerPagedResult<PrecompiledNodeInfo> doJobWithResult()
    {
        return QueryHelper.preparePagedResult(query, offset, limit);
    }
}
