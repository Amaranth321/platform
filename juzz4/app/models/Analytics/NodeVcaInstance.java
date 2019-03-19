package models.Analytics;

import com.google.code.morphia.annotations.Entity;
import com.google.gson.Gson;
import com.kaisquare.transports.kaisync.KsVcaInstance;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidEnvironmentException;
import models.UpTimeLog;
import models.backwardcompatibility.VcaInstance;
import models.node.NodeObject;
import models.stats.VcaHourlyStats;
import platform.CloudActionMonitor;
import platform.Environment;
import platform.analytics.*;
import platform.analytics.app.AppVcaTypeMapper;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.devices.DeviceChannelPair;
import platform.rt.RTFeedManager;
import platform.time.RecurrenceRule;
import play.Logger;
import play.modules.morphia.Model;

/**
 * This model stores VCAs running on nodes and hence, only exists on Cloud
 *
 * @author Aye Maung
 * @since v4.5
 */
@Entity
public class NodeVcaInstance extends Model implements IVcaInstance
{
    private final VcaInfo vcaInfo;
    private VcaStatus status;
    private boolean migrationRequired;
    private boolean pendingRequest;
    private long activatedTime; //started or activated time

    public static NodeVcaInstance saveAsCloudCopy(VcaInfo vcaInfo)
    {
        if (!Environment.getInstance().onCloud())
        {
            throw new InvalidEnvironmentException();
        }

        NodeVcaInstance newInst = new NodeVcaInstance(vcaInfo);
        newInst.save();

        //remove temp record
        NodeTmpVcaInstance.find(vcaInfo).delete();

        return newInst;
    }

    public static void restoreUnsyncInstance(NodeObject nodeObject, KsVcaInstance syncInfo)
    {
        VcaStatus status = null;
        RecurrenceRule parsedRule = null;
        try
        {
            status = VcaStatus.parse(syncInfo.status);
            parsedRule = new Gson().fromJson(syncInfo.schedule, RecurrenceRule.class);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        DeviceChannelPair camera = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), syncInfo.cameraCoreDeviceId);
        VcaInfo syncVcaInfo = VcaInfo.createNew(syncInfo.instanceId,
                                                syncInfo.appId,
                                                camera,
                                                syncInfo.settings,
                                                parsedRule,
                                                syncInfo.enabled);

        NodeVcaInstance newInst = new NodeVcaInstance(syncVcaInfo);
        newInst.status = status;
        newInst.migrationRequired = syncInfo.migrationRequired;
        newInst.pendingRequest = false;
        newInst.save();
    }


    public static boolean migrate(VcaInstance vcaInstance)
    {
        try
        {
            VcaInfo vcaInfo = VcaInfo.createNew(vcaInstance.instanceId,
                                                AppVcaTypeMapper
                                                        .getAppId(Program.KAI_X1, VcaType.parse(vcaInstance.type)),
                                                new DeviceChannelPair(vcaInstance.coreDeviceId, vcaInstance.channelId),
                                                vcaInstance.thresholds,
                                                vcaInstance.recurrenceRule,
                                                vcaInstance.enabled);

            //status text migration
            String statusString = vcaInstance.vcaState.name();
            VcaStatus vcaStatus;
            if (VcaStatus.isOldStatus(statusString))
            {
                /**
                 * v4.4 and below
                 */
                vcaStatus = VcaStatus.migrate(statusString, vcaInfo.isEnabled());
                Logger.info("[NodeVcaInstance] migrated %s status (%s to %s)", vcaInfo, statusString, vcaStatus);
            }
            else
            {
                vcaStatus = VcaStatus.parse(statusString);
            }

            NodeVcaInstance newInst = new NodeVcaInstance(vcaInfo);
            newInst.setStatus(vcaStatus);
            newInst.setMigrationRequired(vcaInstance.updateRequired);
            newInst.save();
            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    @Override
    public VcaInfo getVcaInfo()
    {
        return vcaInfo;
    }

    @Override
    public boolean migrationRequired()
    {
        return migrationRequired;
    }

    @Override
    public VcaStatus getStatus()
    {
        if (pendingRequest)
        {
            return VcaStatus.WAITING;
        }
        return status;
    }

    @Override
    public void update(String settings, RecurrenceRule recurrenceRule) throws ApiException
    {
        setPendingRequest(true);
        save();

        //update
        vcaInfo.setSettings(settings);
        vcaInfo.setRecurrenceRule(recurrenceRule);

        //reset hour
        vcaInfo.checkAndAddResetHour();

        //inform node
        CloudActionMonitor.getInstance().cloudUpdatedNodeVca(getNodePlatformDeviceId(), vcaInfo);
    }

    @Override
    public void activate() throws ApiException
    {
        setPendingRequest(true);
        save();

        //inform node
        CloudActionMonitor.getInstance().cloudActivatedNodeVca(getNodePlatformDeviceId(), vcaInfo.getInstanceId());
    }

    @Override
    public void deactivate() throws ApiException
    {
        setPendingRequest(true);
        save();
        //inform node
        CloudActionMonitor.getInstance().cloudDeactivatedNodeVca(getNodePlatformDeviceId(), vcaInfo.getInstanceId());
    }

    @Override
    public void remove() throws ApiException
    {
        setPendingRequest(true);
        save();

        //inform node
        CloudActionMonitor.getInstance().cloudRemovedNodeVca(getNodePlatformDeviceId(), vcaInfo.getInstanceId());
    }

    public NodeObject getNodeObject()
    {
        String coreDeviceId = getVcaInfo().getCamera().getCoreDeviceId();
        return NodeObject.findByCoreId(coreDeviceId);
    }

    public void setStatus(VcaStatus status)
    {
        this.status = status;
        VcaHourlyStats.statusChanged(vcaInfo.getInstanceId(), status);
    }

    public void setMigrationRequired(boolean migrationRequired)
    {
        this.migrationRequired = migrationRequired;
    }

    public void setPendingRequest(boolean pendingRequest)
    {
        this.pendingRequest = pendingRequest;
        if (pendingRequest)
        {
            //notify UI
            RTFeedManager.getInstance().vcaInstanceChanged(getVcaInfo().getInstanceId(), getVcaInfo().getCamera());
        }
    }

    public long getActivatedTime()
    {
        return activatedTime;
    }

    public void setActivatedTime(long activatedTime)
    {
        this.activatedTime = activatedTime;
    }

    public String getNodePlatformDeviceId()
    {
        CachedDevice cachedDevice = CacheClient.getInstance().getDeviceByCoreId(vcaInfo.getCamera().getCoreDeviceId());
        return String.valueOf(cachedDevice.getPlatformDeviceId());
    }

    public void sync(KsVcaInstance syncInfo)
    {
        VcaStatus status = null;
        RecurrenceRule parsedRule = null;
        try
        {
            status = VcaStatus.parse(syncInfo.status);
            parsedRule = new Gson().fromJson(syncInfo.schedule, RecurrenceRule.class);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        vcaInfo.setSettings(syncInfo.settings);
        vcaInfo.setRecurrenceRule(parsedRule);
        vcaInfo.setEnabled(syncInfo.enabled);
        setStatus(status);
    }

    private NodeVcaInstance(VcaInfo vcaInfo)
    {
        this.vcaInfo = vcaInfo;
        this.setActivatedTime(System.currentTimeMillis());
    }
}
