package platform.kaisyncwrapper.node.tasks;

import com.google.gson.Gson;
import com.kaisquare.transports.kaisync.KsVcaInstance;
import com.kaisquare.transports.kaisync.KsVcaOld;
import models.Analytics.NodeTmpVcaInstance;
import models.Analytics.NodeVcaInstance;
import models.MongoDevice;
import models.NodeCommand;
import models.node.NodeObject;
import platform.analytics.*;
import platform.analytics.app.AppVcaTypeMapper;
import platform.time.RecurrenceRule;
import play.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sent from nodes to sync vca
 * <p/>
 * doTask will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeSyncVca extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        String nodeId = command.getNodeId();
        List<String> parameters = command.getParameters();
        String jsonVcaList = parameters.get(0);
        NodeObject nodeObject = getNodeObject();

        //parse
        Map<String, KsVcaInstance> syncInfoMap = createSyncInfoMap(jsonVcaList);

        //update cloud instance list
        MongoDevice mongoDevice = MongoDevice.getByPlatformId(command.getNodeId());
        List<IVcaInstance> cloudList = VcaManager.getInstance().listVcaInstancesOfDevice(mongoDevice);
        for (IVcaInstance cloudInst : cloudList)
        {
            NodeVcaInstance dbCloudInst = (NodeVcaInstance) cloudInst;
            String instanceId = cloudInst.getVcaInfo().getInstanceId();

            //cloud has extra instances :(
            if (!syncInfoMap.containsKey(cloudInst.getVcaInfo().getInstanceId()))
            {
                dbCloudInst.delete();
                Logger.info("[%s] deleted orphaned vca from cloud (%s)", getNodeName(), dbCloudInst.getVcaInfo());
                continue;
            }

            //json sent is not valid
            KsVcaInstance syncInfo = syncInfoMap.get(instanceId);
            if (syncInfo == null)
            {
                Logger.error("[%s] vca list sync: invalid json instance (%s)", getNodeName(), instanceId);
                continue;
            }

            dbCloudInst.sync(syncInfo);
            dbCloudInst.save();

            //remove already sync-ed
            syncInfoMap.remove(syncInfo.instanceId);
        }

        //restore instances missing on Cloud
        for (KsVcaInstance onlyOnNode : syncInfoMap.values())
        {
            Logger.info("[%s] restoring unsync-ed vca instance (%s) on cloud",
                        nodeObject.getName(),
                        onlyOnNode.instanceId);

            NodeVcaInstance.restoreUnsyncInstance(nodeObject, onlyOnNode);
        }

        //remove temp instances of this node
        NodeTmpVcaInstance.findByNode(nodeObject.getNodeCoreDeviceId()).delete();

        return true;
    }

    private Map<String, KsVcaInstance> createSyncInfoMap(String jsonInfoList)
    {
        Map<String, KsVcaInstance> parsedInfoMap = new LinkedHashMap<>();
        boolean useNewFormat = (getNodeObject().getReleaseNumber() >= 4.5);
        Gson gson = new Gson();
        List<Object> rawList = gson.fromJson(jsonInfoList, List.class);
        for (Object rawObj : rawList)
        {
            KsVcaInstance parsedTransport = parseSyncInstance(gson.toJson(rawObj), useNewFormat);
            if (parsedTransport == null)
            {
                continue;
            }
            parsedInfoMap.put(parsedTransport.instanceId, parsedTransport);
        }

        return parsedInfoMap;
    }

    private KsVcaInstance parseSyncInstance(String jsonInfo, boolean useNewFormat)
    {
        try
        {
            Gson gson = new Gson();
            if (useNewFormat)
            {
                return new Gson().fromJson(jsonInfo, KsVcaInstance.class);
            }

            /**
             *
             * The mess below is for backward compatibility with nodes < v4.5
             *
             */
            KsVcaOld oldTransport = gson.fromJson(jsonInfo, KsVcaOld.class);
            String appId = AppVcaTypeMapper.getAppId(Program.KAI_X1, VcaType.parse(oldTransport.type));

            RecurrenceRule parsedRule = null;
            try
            {
                parsedRule = gson.fromJson(gson.toJson(oldTransport.recurrenceRule), RecurrenceRule.class);
            }
            catch (Exception e)
            {
                //not interested why
            }

            VcaStatus vcaStatus;
            if (VcaStatus.isOldStatus(oldTransport.vcaState))
            {
                /**
                 * v4.4 and below
                 */
                vcaStatus = VcaStatus.migrate(oldTransport.vcaState, oldTransport.enabled);
                Logger.info("[%s] migrated %s:%s status (%s to %s)",
                            getClass().getSimpleName(),
                            appId,
                            oldTransport.instanceId,
                            oldTransport.vcaState,
                            vcaStatus);
            }
            else
            {
                vcaStatus = VcaStatus.parse(oldTransport.vcaState);
            }

            return new KsVcaInstance(oldTransport.instanceId,
                                     appId,
                                     oldTransport.coreDeviceId,
                                     oldTransport.thresholds,
                                     gson.toJson(parsedRule),
                                     oldTransport.enabled,
                                     oldTransport.updateRequired,
                                     vcaStatus.name());
        }
        catch (Exception e)
        {
            Logger.debug(e, "");
            return null;
        }
    }
}
