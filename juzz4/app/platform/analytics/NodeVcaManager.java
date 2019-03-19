package platform.analytics;

import lib.util.exceptions.ApiException;
import models.licensing.NodeLicense;
import platform.node.KaiSyncCommandClient;
import platform.node.NodeManager;
import platform.time.PeriodOfDay;
import play.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.5
 */
class NodeVcaManager extends VcaManager
{
    private static final NodeVcaManager instance = new NodeVcaManager();

    public static NodeVcaManager getInstance()
    {
        return instance;
    }

    @Override
    public void addNewVca(VcaInfo vcaInfo) throws ApiException
    {
        IVcaInstance newInst = LocalVcaInstance.addNew(vcaInfo);

        //inform cloud
        KaiSyncCommandClient.getInstance().nodeVcaAdded(vcaInfo);
    }

    @Override
    public List<IVcaInstance> listVcaInstances(List<String> coreDeviceIdList)
    {
        return VcaThriftClient.getInstance().getVcaList(coreDeviceIdList);
    }

    @Override
    public IVcaInstance getVcaInstance(String instanceId)
    {
        return VcaThriftClient.getInstance().find(instanceId);
    }

    @Override
    public boolean checkConcurrencyLimit(VcaInfo vcaInfo)
    {
        try
        {
            synchronized (actionLock)
            {
                //compile the vca info list
                List<VcaInfo> vcaInfoList = new ArrayList<>();
                boolean isNewVca = true;
                for (IVcaInstance dbInst : listVcaInstances(null))
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

                NodeLicense license = NodeManager.getInstance().getLicense();

                //check if concurrent limit is breached with the new schedule
                int aboveLimit = license.maxVcaCount + 1;
                Map<Integer, Map<PeriodOfDay, List<VcaInfo>>> overflowPeriods = findConcurrentVcaPeriods(vcaInfoList,
                                                                                                         aboveLimit);
                return overflowPeriods.isEmpty();
            }
        }
        catch (ApiException e)
        {
            Logger.error(e, "");
            return true;
        }
    }

    @Override
    public boolean checkAppSupport(String appId, String platformDeviceId)
    {
        List<VcaAppInfo> appInfoList = VcaThriftClient.getInstance().getVcaAppInfoList();
        for (VcaAppInfo appInfo : appInfoList)
        {
        	Logger.info("supported appInfo:"+appInfo.appId);
            if (appInfo.appId.equals(appId))
            {
                return true;
            }
        }
        return false;
    }

    private NodeVcaManager()
    {
    }
}
