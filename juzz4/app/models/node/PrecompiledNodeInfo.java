package models.node;

import com.google.code.morphia.annotations.Entity;
import jobs.cloud.independent.PrecompileNodeInfoJob;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaInfo;
import platform.analytics.VcaStatus;
import platform.analytics.app.AppVcaTypeMapper;
import platform.devices.DeviceStatus;
import platform.nodesoftware.NodeSoftwareStatus;
import play.i18n.Messages;
import play.modules.morphia.Model;

import java.util.List;

/**
 * The periodically pre-compiled data used by Node browser and Malfunctioning node notifier
 *
 * @author Aye Maung
 * @see PrecompileNodeInfoJob
 * @since v4.5
 */
@Entity
public class PrecompiledNodeInfo extends Model
{
    //IDs
    public final long platformDeviceId;
    public final String coreDeviceId;

    //data
    public final String bucketName;
    public final String nodeName;
    public final String version;
    public final String timezone;
    public final String modelName;
    public final String ipAddress;
    public final String mac;
    public final String downloadedVersion;
    public final List<CameraSummary> cameraSummaryList;
    public final List<VcaSummary> vcaSummaryList;

    //statuses
    public final DeviceStatus status;
    public final NodeSoftwareStatus softwareStatus;
    public final int unstableVcaCount;
    public final int noRecentDataVcaCount;
    public final int offlineCameraCount;

    public PrecompiledNodeInfo(long platformDeviceId,
                               String coreDeviceId,
                               String bucketName,
                               String nodeName,
                               String version,
                               String timezone,
                               String modelName,
                               String ipAddress,
                               String mac,
                               String downloadedVersion,
                               List<CameraSummary> cameraSummaryList,
                               List<VcaSummary> vcaSummaryList,
                               DeviceStatus status,
                               NodeSoftwareStatus softwareStatus,
                               int unstableVcaCount,
                               int noRecentDataVcaCount,
                               int offlineCameraCount)
    {
        this.platformDeviceId = platformDeviceId;
        this.coreDeviceId = coreDeviceId;
        this.bucketName = bucketName;
        this.nodeName = nodeName;
        this.version = version;
        this.timezone = timezone;
        this.modelName = modelName;
        this.ipAddress = ipAddress;
        this.mac = mac;
        this.downloadedVersion = downloadedVersion;
        this.cameraSummaryList = cameraSummaryList;
        this.vcaSummaryList = vcaSummaryList;
        this.status = status;
        this.softwareStatus = softwareStatus;
        this.unstableVcaCount = unstableVcaCount;
        this.noRecentDataVcaCount = noRecentDataVcaCount;
        this.offlineCameraCount = offlineCameraCount;
    }

    //just the essential fields
    public static class CameraSummary
    {
        public final String cameraCoreDeviceId;
        public final String name;
        public final String modelName;
        public final String host;
        public final String port;
        public final boolean recordingEnabled;
        public final DeviceStatus status;

        public CameraSummary(NodeCamera nodeCamera)
        {
            this.cameraCoreDeviceId = nodeCamera.nodeCoreDeviceId;
            this.name = nodeCamera.name;
            this.modelName = nodeCamera.model.name;
            this.host = nodeCamera.host;
            this.port = nodeCamera.port;
            this.recordingEnabled = nodeCamera.cloudRecordingEnabled;
            this.status = nodeCamera.getStatus();
        }
    }

    //just the essential fields
    public static class VcaSummary
    {
        public final String cameraCoreDeviceId;
        public final String instanceId;
        public final String type;
        public final String scheduleSummary;
        public final boolean enabled;
        public final VcaStatus status;
        public final int avgStatusChangeCount;
        public final boolean noRecentData;

        public VcaSummary(IVcaInstance vcaInstance,
                          int avgStatusChangeCount,
                          boolean noRecentData)
        {
            VcaInfo vcaInfo = vcaInstance.getVcaInfo();

            this.cameraCoreDeviceId = vcaInfo.getCamera().getChannelId();
            this.instanceId = vcaInfo.getInstanceId();
            this.type = AppVcaTypeMapper.getVcaType(vcaInfo.getAppId()).getVcaTypeName();
            this.scheduleSummary = vcaInfo.getRecurrenceRule() == null ?
                                   Messages.get("not-scheduled") :
                                   vcaInfo.getRecurrenceRule().summary;
            this.enabled = vcaInfo.isEnabled();
            this.status = vcaInstance.getStatus();
            this.avgStatusChangeCount = avgStatusChangeCount;
            this.noRecentData = noRecentData;
        }
    }

}
