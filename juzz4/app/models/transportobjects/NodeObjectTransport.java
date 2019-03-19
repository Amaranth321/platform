package models.transportobjects;

import models.licensing.LicenseStatus;
import models.licensing.NodeLicenseInfo;
import models.node.NodeCamera;
import models.node.NodeObject;
import models.node.NodeSettings;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaAppInfo;
import platform.analytics.VcaManager;
import platform.analytics.VcaType;
import platform.analytics.app.AppVcaTypeMapper;
import platform.nodesoftware.NodeSoftwareStatus;
import play.i18n.Lang;

import java.util.*;

/**
 * Object to transfer data to UI. DO NOT modify fields.
 *
 * @author Aye Maung
 * @since v4.5
 */
public class NodeObjectTransport
{
    public final String name;
    public final String cloudPlatformDeviceId;
    public final String cloudCoreDeviceId;
    public final List<NodeCameraTransport> cameras;
    public final List<VcaTransport> analytics;
    public final NodeSettings settings;
    public final String version;
    public final String vcaVersion;
    public final double releaseNumber;
    public final NodeSoftwareStatus softwareStatus;
    public final String downloadedVersion;
    public final List<Map> supportedAppList;

    //v4.5
    public final int maxVcaConcurrency;
    public final int maxCameraCount;
    public final int cloudStorageGb;
    public final LicenseStatus licenseStatus;
    public final long licenseExpiry;

    public NodeObjectTransport(NodeObject dbNode)
    {
        //convert camera list
        List<NodeCameraTransport> cameraList = new ArrayList<>();
        for (NodeCamera camera : dbNode.getCameras())
        {
            cameraList.add(new NodeCameraTransport(dbNode, camera));
        }

        //convert vca list
        List<IVcaInstance> vcaList = VcaManager.getInstance().listVcaInstances(Arrays.asList(dbNode.getNodeCoreDeviceId()));
        List<VcaTransport> vcaTransports = new ArrayList<>();
        for (IVcaInstance vcaInstance : vcaList)
        {
            vcaTransports.add(new VcaTransport(vcaInstance));
        }

        this.name = dbNode.getName();
        this.cloudPlatformDeviceId = dbNode.getNodeId();
        this.cloudCoreDeviceId = dbNode.getNodeCoreDeviceId();
        this.cameras = cameraList;
        this.analytics = vcaTransports;
        this.settings = dbNode.getSettings();
        this.vcaVersion = "deprecated field";
        this.version = dbNode.getNodeVersion();
        this.releaseNumber = dbNode.getReleaseNumber();
        this.softwareStatus = dbNode.getSoftwareStatus();
        this.downloadedVersion = dbNode.getDownloadedVersion();

        //license info
        NodeLicenseInfo nodeLicense = dbNode.getLicense();
        this.maxVcaConcurrency = nodeLicense.maxVcaCount;
        this.maxCameraCount = 99; //todo: currently unused.
        this.cloudStorageGb = nodeLicense.cloudStorageGb;
        this.licenseStatus = nodeLicense.status;
        this.licenseExpiry = nodeLicense.expiryDate;

        //app allowed in the license
        this.supportedAppList = new ArrayList<>();
        for (VcaAppInfo appInfo : dbNode.getSupportedAppList())
        {
            VcaType vcaType = AppVcaTypeMapper.getVcaType(appInfo.appId);
            String vcaFeatureName = vcaType.getConfigFeature().getName();
            if (nodeLicense.featureNameList.contains(vcaFeatureName))
            {
                Map<String, String> infoMap = new LinkedHashMap<>();
                infoMap.put("appId", appInfo.appId);
                infoMap.put("program", appInfo.program.name());
                infoMap.put("version", appInfo.version);
                infoMap.put("displayName", appInfo.displayName.get(Lang.get()));
                infoMap.put("description", appInfo.description.get(Lang.get()));
                supportedAppList.add(infoMap);
            }
        }
    }
}
