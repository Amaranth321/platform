package models.transportobjects;

import platform.analytics.IVcaInstance;
import platform.analytics.Program;
import platform.analytics.VcaInfo;
import platform.analytics.VcaStatus;
import platform.analytics.app.AppVcaTypeMapper;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.time.RecurrenceRule;
import play.Logger;

/**
 * This class exists for communicating with UI and older nodes.
 * <p/>
 * DO NOT modify field names or types.
 * <p/>
 * Use this class for API responses only.
 *
 * @author Aye Maung
 * @since v4.5
 */
public class VcaTransport
{
    public final String instanceId;
    public final String type;
    public final long platformDeviceId;
    public final String coreDeviceId;
    public final String channelId;
    public final String thresholds;
    public final RecurrenceRule recurrenceRule;
    public final boolean enabled;
    public final String vcaState;

    public boolean updateRequired;
    public String appId;
    public Program program;

    public VcaTransport(IVcaInstance instance)
    {
        VcaInfo vcaInfo = instance.getVcaInfo();
        CachedDevice cachedDevice = CacheClient.getInstance().getDeviceByCoreId(vcaInfo.getCamera().getCoreDeviceId());

        this.instanceId = vcaInfo.getInstanceId();
        this.type = AppVcaTypeMapper.getVcaType(vcaInfo.getAppId()).getVcaTypeName();
        this.platformDeviceId = Long.parseLong(cachedDevice.getPlatformDeviceId());
        this.coreDeviceId = vcaInfo.getCamera().getCoreDeviceId();
        this.channelId = vcaInfo.getCamera().getChannelId();
        this.thresholds = vcaInfo.getSettings();
        this.recurrenceRule = vcaInfo.getRecurrenceRule();
        this.enabled = vcaInfo.isEnabled();
        this.vcaState = instance.getStatus().name();
        this.updateRequired = instance.migrationRequired();
        this.appId = vcaInfo.getAppId();
        this.program = AppVcaTypeMapper.getProgram(vcaInfo.getAppId());
    }

    @Override
    public String toString()
    {
        return String.format("[%s, %s, %s, enabled:%s, platformDeviceId:%s, coreDeviceId:%s, channelId:%s, updateRequired:%s]",
                             instanceId,
                             type,
                             vcaState,
                             enabled,
                             platformDeviceId,
                             coreDeviceId,
                             channelId,
                             updateRequired);
    }
}
