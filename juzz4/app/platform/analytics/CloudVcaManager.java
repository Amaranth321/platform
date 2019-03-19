package platform.analytics;

import lib.util.exceptions.ApiException;
import models.Analytics.NodeTmpVcaInstance;
import models.Analytics.NodeVcaInstance;
import models.MongoDevice;
import models.licensing.NodeLicense;
import models.node.NodeObject;
import platform.CloudActionMonitor;
import platform.CloudLicenseManager;
import platform.config.readers.ConfigsCloud;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedNodeObjectInfo;
import platform.time.PeriodOfDay;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.5
 */
class CloudVcaManager extends VcaManager
{
    private static final CloudVcaManager instance = new CloudVcaManager();
    private static final CacheClient cacheClient = CacheClient.getInstance();

    public static CloudVcaManager getInstance()
    {
        return instance;
    }

    @Override
    public void addNewVca(VcaInfo vcaInfo) throws ApiException
    {
        MongoDevice dbDevice = vcaInfo.getCamera().getDbDevice();

        //running on cloud camera
        if (!dbDevice.isKaiNode())
        {
            if (!ConfigsCloud.getInstance().allowVcaOnCloud())
            {
                throw new ApiException("vca-on-cloud-not-allowed");
            }
            LocalVcaInstance.addNew(vcaInfo);
            return;
        }

        //create tmp record
        if (NodeTmpVcaInstance.find(vcaInfo).count() > 0)
        {
            throw new ApiException("error-same-pending-vca-exists");
        }
        NodeTmpVcaInstance.addNew(dbDevice, vcaInfo);

        //reset hour
        vcaInfo.checkAndAddResetHour();

        //inform node
        CloudActionMonitor.getInstance().cloudAddedNodeVca(dbDevice.getDeviceId(), vcaInfo);
    }

    @Override
    public List<IVcaInstance> listVcaInstances(List<String> coreDeviceIdList)
    {
        if (coreDeviceIdList != null && coreDeviceIdList.isEmpty())
        {
            return new ArrayList<>();
        }

        List<IVcaInstance> combinedList = new ArrayList<>();

        //compile node instances
        List<Model.MorphiaQuery> queries = new ArrayList<>();
        queries.add(NodeVcaInstance.q());       //running on nodes
        queries.add(NodeTmpVcaInstance.q());    //temp instances
        for (Model.MorphiaQuery query : queries)
        {
            if (coreDeviceIdList != null)
            {
                query.filter("vcaInfo.camera.coreDeviceId in", coreDeviceIdList);
            }

            List<NodeVcaInstance> results = query.asList();
            combinedList.addAll(results);
        }

        //add those running on cloud
        if (ConfigsCloud.getInstance().allowVcaOnCloud())
        {
            List<IVcaInstance> cloudList = VcaThriftClient.getInstance().getVcaList(coreDeviceIdList);
            combinedList.addAll(cloudList);
        }

        return combinedList;
    }

    @Override
    public IVcaInstance getVcaInstance(String instanceId)
    {
        IVcaInstance nodeInst = NodeVcaInstance.find("vcaInfo.instanceId", instanceId).first();
        if (nodeInst != null)
        {
            return nodeInst;
        }

        if (ConfigsCloud.getInstance().allowVcaOnCloud())
        {
            return VcaThriftClient.getInstance().find(instanceId);
        }

        return null;
    }

    @Override
    public boolean checkConcurrencyLimit(VcaInfo vcaInfo)
    {
        MongoDevice nodeDevice = vcaInfo.getCamera().getDbDevice();
        if (!nodeDevice.isKaiNode())
        {
            return true;
        }

        synchronized (actionLock)
        {
            //compile the vca info list
            List<VcaInfo> vcaInfoList = new ArrayList<>();
            boolean isNewVca = true;
            for (IVcaInstance dbInst : listVcaInstancesOfDevice(nodeDevice))
            {
                if (dbInst.getVcaInfo().getInstanceId().equals(vcaInfo.getInstanceId()))
                {
                    //use the updated vcaInfo
                    vcaInfoList.add(vcaInfo);
                    isNewVca = false;
                }
                else
                {
                    vcaInfoList.add(dbInst.getVcaInfo());
                }
            }
            if (isNewVca)
            {
                vcaInfoList.add(vcaInfo);
            }

            NodeLicense license = CloudLicenseManager.getInstance().getLicenseByNode(nodeDevice.getDeviceId());

            //node 4 versions below 4.5 will not take schedules into consideration while checking the limit
            CachedNodeObjectInfo nodeInfo = cacheClient.getNodeObject(cacheClient.getDeviceByPlatformId(nodeDevice.getDeviceId()));
            if (!nodeInfo.getModelInfo().isNodeOne() && nodeInfo.getReleaseNumber() < 4.5)
            {
                return vcaInfoList.size() <= license.maxVcaCount;
            }

            //check if concurrent limit is breached with the new schedule
            int aboveLimit = license.maxVcaCount + 1;
            Map<Integer, Map<PeriodOfDay, List<VcaInfo>>> overflowPeriods = findConcurrentVcaPeriods(vcaInfoList, aboveLimit);
            return overflowPeriods.isEmpty();
        }
    }

    @Override
    public boolean checkAppSupport(String appId, String platformDeviceId)
    {
        NodeObject nodeObject = NodeObject.findByPlatformId(platformDeviceId);
        if (nodeObject != null)
        {
            for (VcaAppInfo vcaAppInfo : nodeObject.getSupportedAppList())
            {
                if (vcaAppInfo.appId.equals(appId))
                {
                    return true;
                }
            }
            return false;
        }

        //analytics on cloud
        if (ConfigsCloud.getInstance().allowVcaOnCloud())
        {
            List<VcaAppInfo> appInfoList = VcaThriftClient.getInstance().getVcaAppInfoList();
            for (VcaAppInfo appInfo : appInfoList)
            {
                if (appInfo.appId.equals(appId))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private CloudVcaManager()
    {
    }
}
