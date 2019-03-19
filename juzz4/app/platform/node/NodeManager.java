package platform.node;

import com.kaisquare.util.FileUtil;
import com.kaisquare.util.SysInfoUtil;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidEnvironmentException;
import models.*;
import models.licensing.LicenseStatus;
import models.licensing.NodeLicense;
import models.node.NodeCamera;
import models.node.NodeInfo;
import models.node.NodeRemoteShell;
import models.node.NodeSettings;
import models.notification.BucketNotificationSettings;
import org.apache.commons.lang.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.BucketManager;
import platform.Environment;
import platform.RoleManager;
import platform.access.DefaultBucket;
import platform.access.DefaultRole;
import platform.access.DefaultUser;
import platform.analytics.*;
import platform.events.EventType;
import platform.register.NodeSetupInfo;
import play.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;

/**
 * To be used by node platform only
 *
 * @author Aye Maung
 */
public class NodeManager
{
    private static final String NODE_ROOT = "/root/VCABox/";
    private static final String REMOTE_SHELL_PATH = "/home/node/reversessh";

    private static NodeManager instance = null;
    private static NodeProvisioning nodeProvisioning = null;
    private Process remoteShellProcess;

    /**
     * ONLY on nodes because os time zone is used to get correct timestamp
     */
    public static String buildSearchTimestamp(String cameraName, long from, long to)
    {
        TimeZone timeZone = SysInfoUtil.getOSTimeZone(false);
        DateTimeZone jodaTz = DateTimeZone.forTimeZone(timeZone);
        String timeFormat = "MMM_dd_HHmm";
        String fromTime = new DateTime(from, jodaTz).toString(timeFormat);
        String toTime = new DateTime(to, jodaTz).toString(timeFormat);

        String random = RandomStringUtils.randomAlphabetic(6);

        return String.format("%s_%s_-_%s_%s.zip", cameraName, fromTime, toTime, random);
    }

    private NodeManager()
    {
    }

    public static NodeManager getInstance()
    {
        if (!Environment.getInstance().onKaiNode())
        {
            throw new InvalidEnvironmentException();
        }

        if (instance == null)
        {
            instance = new NodeManager();
            nodeProvisioning = NodeProvisioning.getInstance();
        }
        return instance;
    }

    public NodeInfo getNodeInfo()
    {
        return NodeInfo.find().first();
    }

    public boolean isRegisteredOnCloud()
    {
        NodeInfo nodeInfo = getNodeInfo();
        return nodeInfo != null;
    }

    public boolean isSuspended()
    {
        if (!isRegisteredOnCloud())
        {
            return true;
        }

        NodeInfo nodeInfo = getNodeInfo();
        return nodeInfo.isSuspended();
    }

    public MongoBucket getBucket()
    {
        MongoBucket nodeBucket = MongoBucket.q().get();
        if (nodeBucket == null)
        {
            throw new NullPointerException("Node bucket not found");
        }
        return nodeBucket;
    }

    public String getVersion()
    {
        String nodeVersion = "";
        File f = new File(NODE_ROOT + "kainode.ver");
        if (f.exists())
        {
            BufferedReader reader = null;
            try
            {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                nodeVersion = reader.readLine();
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }

        return nodeVersion;
    }

    public void updateBucket(String bucketName) throws ApiException
    {
        // superadmin should not be on node
        MongoBucket superAdminBucket = MongoBucket.getByName(DefaultBucket.SUPERADMIN.getBucketName());
        if (superAdminBucket != null)
        {
            BucketManager.getInstance().permanentDeleteBucket(superAdminBucket.getBucketId());
        }

        MongoBucket nodeBucket = MongoBucket.q().get();
        if (nodeBucket == null)
        {
            throw new ApiException("Node bucket not found");
        }
        nodeBucket.setName(bucketName);
        nodeBucket.save();
    }

    public NodeSettings getSettings() throws ApiException
    {
        if (!isRegisteredOnCloud())
        {
            throw new ApiException("Node is not registered");
        }

        NodeInfo nodeInfo = getNodeInfo();
        return nodeInfo.getSettings();
    }

    public void setSettings(NodeSettings newSettings, boolean updateKaiAdmin) throws ApiException
    {
        if (!isRegisteredOnCloud())
        {
            throw new ApiException("Node is not registered");
        }

        NodeInfo nodeInfo = getNodeInfo();

        if (updateKaiAdmin)
        {
            KaiNodeAdminService kainodeService = KaiNodeAdminService.getInstance();
            try
            {
                Logger.info("Update network settings");
                KaiNodeAdminService.NetworkSettings netSettings = newSettings.getNetworkSettings();
                boolean netResult = kainodeService.setNetworkSettings(
                        netSettings.getAddress(),
                        netSettings.getNetmask(),
                        netSettings.getGateway(),
                        netSettings.getNameservers()
                );
                if (!netResult)
                {
                    throw new ApiException("Failed to update network settings");
                }

                Logger.info("Update timezone");
                boolean tzResult = kainodeService.setTimezone(newSettings.getTimezone());
                if (!tzResult)
                {
                    throw new ApiException("Failed to update timezone");
                }
            }
            catch (Exception e)
            {
                Logger.error(Util.whichFn() + e.getMessage());
                return;
            }
        }

        nodeInfo.setSettings(newSettings);
        nodeInfo.save();
        Logger.info("Node settings updated");

        //update cloud
        KaiSyncCommandClient kaiSyncClient = CloudConnector.getInstance().getKaiSyncCommandClient();
        kaiSyncClient.nodeSettingsChanged(newSettings);
    }

    public void setBucketNotificationSettings(BucketNotificationSettings updatedSettings)
    {
        BucketNotificationSettings currentSettings = getBucket().getNotificationSettings();

        //clear current settings
        currentSettings.clearAll();

        //update each type
        for (EventType eventType : updatedSettings.getSupportedEventTypes())
        {
            BucketNotificationSettings.EventTypeSettings typeSetts = updatedSettings.getSettingsByType(eventType);
            currentSettings.updateEventTypeSettings(
                    eventType,
                    typeSetts.isNotificationEnabled(),
                    typeSetts.isVideoRequired()
            );
        }

        currentSettings.save();
    }

    public NodeLicense getLicense() throws ApiException
    {
        if (!isRegisteredOnCloud())
        {
            throw new ApiException("Node is not registered");
        }

        NodeLicense nodeLicense = NodeLicenseManager.getInstance().getLicense();
        if (nodeLicense == null)
        {
            throw new ApiException("Node license not found");
        }

        return nodeLicense;
    }

    public void setLicense(NodeLicense nodeLicense) throws ApiException
    {
        NodeLicenseManager.getInstance().setLicense(nodeLicense);
    }

    /**
     * Activation will activate bucket/user/vca in addition to the license itself.
     * For applying license changes only, use refreshLicense()
     */
    public void activateLicense() throws ApiException
    {
        if (!isRegisteredOnCloud())
        {
            throw new ApiException("Node is not registered");
        }

        nodeProvisioning.activateNode();
        NodeLicenseManager.getInstance().setStatus(LicenseStatus.ACTIVE);
        Util.printImptLog("Node license has been activated");
    }

    public void suspendLicense() throws ApiException
    {
        if (!isRegisteredOnCloud())
        {
            throw new ApiException("Node is not registered");
        }

        nodeProvisioning.suspendNode();
        NodeLicenseManager.getInstance().setStatus(LicenseStatus.SUSPENDED);
        Util.printImptLog("Node license has been suspended");
    }

    /**
     * Ensures the platform has the same settings specified in the license
     */
    public void refreshLicense() throws ApiException
    {
        if (!isRegisteredOnCloud())
        {
            return;
        }

        NodeLicense nodeLicense = getLicense();
        NodeProvisioning.getInstance().updateNodeFeatures(nodeLicense.featureNameList);
    }

    public void setupNewNode(NodeSetupInfo setupInfo) throws ApiException
    {
        Logger.info("Creating NodeInfo");
        NodeInfo nodeInfo = new NodeInfo(setupInfo);
        nodeInfo.save();

        Logger.info("Update local bucket name: " + setupInfo.getBucketName());
        updateBucket(setupInfo.getBucketName());

        Logger.info("Add default nodeuser");
        addDefaultNodeUser();

        Logger.info("Applying license");
        setLicense(setupInfo.getNodeLicense());
        activateLicense();

        Logger.info("Applying branding");
        MongoBucket nodeBucket = getBucket();
        BucketSetting bucketSetting = BucketManager.getInstance().getBucketSetting(nodeBucket.getBucketId());
        bucketSetting.setBucketLogo(setupInfo.getBrandingAssets().getBase64HeaderLogo());
        bucketSetting.save();

        Logger.info("Applying notification settings");
        BucketNotificationSettings serverSettings = setupInfo.getNotificationSettings();
        setBucketNotificationSettings(serverSettings);

        Logger.info("Sending node info to cloud");
        sendNodeInfoToCloud();

        Environment.getInstance().setServerStartedTime();
    }

    public boolean startRemoteShell(String host, String port, String user)
    {
        boolean ret = true;
        Scanner scanner = null;
        try
        {
            stopRemoteShell();

            Logger.info("RemoteShellJob command received from Cloud");

            List<String> commands = new ArrayList<>();
            commands.add(REMOTE_SHELL_PATH + "/restart_r_ssh.sh");
            commands.add(host);
            commands.add(port);
            commands.add(user);
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);
            remoteShellProcess = pb.start();
            scanner = new Scanner(remoteShellProcess.getInputStream());
            String line;

            while (scanner.hasNextLine())
            {
                line = scanner.nextLine();
                Logger.info("autossh: %s", line);
            }

            KaiSyncCommandClient.getInstance().nodeRemoteShellStarted();

            NodeRemoteShell nrs = new NodeRemoteShell().find().first();
            if (nrs == null)
            {
                nrs = new NodeRemoteShell();
            }
            nrs.host = host;
            nrs.port = Integer.parseInt(port);
            nrs.username = user;
            nrs.open = true;
            nrs.save();

        }
        catch (Exception exp)
        {
            Logger.warn("%s:%s", "Error in RemoteShellJob", exp.toString());
            Logger.error(lib.util.Util.getStackTraceString(exp));
            ret = false;
        }
        finally
        {
            if (scanner != null)
            {
                scanner.close();
            }
        }

        return ret;
    }

    public boolean stopRemoteShell()
    {
        if (remoteShellProcess != null)
        {
            remoteShellProcess.destroy();
            remoteShellProcess = null;
        }

        File pidFile = new File(REMOTE_SHELL_PATH, "autossh.pid");
        if (pidFile.exists())
        {
            try
            {
                Runtime.getRuntime().exec("kill " + FileUtil.readFile(pidFile));

                NodeRemoteShell nrs = new NodeRemoteShell().find().first();
                if (nrs == null)
                {
                    nrs = new NodeRemoteShell();
                }
                nrs.open = false;
                nrs.save();
            }
            catch (IOException e)
            {
                Logger.error(e, "");

                return false;
            }
            finally
            {
                try
                {
                    KaiSyncCommandClient.getInstance().nodeRemoteShellStopped();
                }
                catch (Exception e)
                {
                }
            }
        }

        return true;
    }

    public void sendNodeInfoToCloud() throws ApiException
    {
        if (!isRegisteredOnCloud())
        {
            return;
        }

        try
        {
            //Versions
            String nodeVersion = NodeManager.getInstance().getVersion();
            List<VcaAppInfo> vcaAppInfoList = VcaThriftClient.getInstance().getVcaAppInfoList();
            CloudConnector.getInstance().getKaiSyncCommandClient().nodeUpdateInfo(nodeVersion, vcaAppInfoList);

            //Network settings
            KaiNodeAdminService.NetworkSettings networkSettings = KaiNodeAdminService.getInstance()
                    .getNetworkSettings();
            TimeZone timeZone = SysInfoUtil.getOSTimeZone(true);
            NodeSettings latestSettings = new NodeSettings(networkSettings, timeZone);
            NodeManager.getInstance().setSettings(latestSettings, false);

            //for KaiNodeService use
            FileUtil.writeTextFile(new File("node.id"), getNodeInfo().getCloudPlatformDeviceId());
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
        }
    }

    public void nodePlatformUpdated()
    {
        try
        {
            refreshLicense();
        }
        catch (ApiException e)
        {
            Logger.error(e, "");
        }
    }

    /**
     * @return true if API to cloud was sent successfully
     */
    public boolean notifyFactoryResetToCloud()
    {
        if (!isRegisteredOnCloud())
        {
            return false;
        }

        try
        {
            CloudConnector.getInstance().getHttpApiClient().postNodeToCloudCall(
                    getLicense(),
                    NodeToCloudAPI.NOTIFY_RESET,
                    null
            );

            return true;
        }
        catch (Exception e)
        {
            Util.printImptLog("Factory reset failed to notify cloud");
            Logger.error(e, "");
            return false;
        }
    }

    public int getCameraLimit() throws ApiException
    {
        NodeLicense nodeLicense = getLicense();
        return nodeLicense.maxCameraLimit;
    }

    public void syncDevicesWithCloud() throws Exception
    {
        Logger.info("Synchronizing devices with cloud ...");
        List<MongoDevice> devices = MongoDevice.q().fetchAll();
        List<NodeCamera> nodeCameraList = new ArrayList<>();
        for (MongoDevice dvc : devices)
        {
            NodeCamera nc = dvc.toNodeCamera();
            nodeCameraList.add(nc);
        }
        CloudConnector.getInstance().getKaiSyncCommandClient().nodeSyncDeviceList(nodeCameraList);
    }

    public void syncVcaListWithCloud() throws Exception
    {
        Logger.info("Synchronizing vca instances with cloud ...");
        List<IVcaInstance> nodeVcaList = VcaManager.getInstance().listVcaInstances(null);

        List<LocalVcaInstance> filteredList = new ArrayList<>();
        for (IVcaInstance inst : nodeVcaList)
        {
            MongoDevice vcaCamera = inst.getVcaInfo().getCamera().getDbDevice();
            if (vcaCamera == null)
            {
                Logger.error("[%s] Cannot find the camera on node. skipped", inst.getVcaInfo());
                continue;
            }

            filteredList.add((LocalVcaInstance) inst);
        }
        CloudConnector.getInstance().getKaiSyncCommandClient().nodeSyncVcaList(filteredList);
    }

    public void addDefaultNodeUser()
    {
        if (!Environment.getInstance().onKaiNode())
        {
            return;
        }

        NodeManager nodeManager = NodeManager.getInstance();

        if (!nodeManager.isRegisteredOnCloud())
        {
            return;
        }

        MongoBucket nodeBucket = nodeManager.getBucket();

        //default roles
        MongoRole nodeUserRole = MongoRole.q()
                .filter("bucketId", nodeBucket.getBucketId())
                .filter("name", DefaultRole.NODE_USER.getRoleName())
                .get();

        if (nodeUserRole == null)
        {
            nodeUserRole = new MongoRole(nodeBucket.getBucketId(),
                                         DefaultRole.NODE_USER.getRoleName(),
                                         DefaultRole.NODE_USER.getDescription());
            nodeUserRole.setRoleId(MongoRole.generateNewId());
            nodeUserRole.save();
            Logger.info(Util.whichClass() + "Added default role (%s)", nodeUserRole.getName());
        }

        //default users
        for (DefaultUser defaultUser : DefaultRole.NODE_USER.getUsers())
        {
            MongoUser nodeUser = MongoUser.q()
                    .filter("bucketId", nodeBucket.getBucketId())
                    .filter("login", defaultUser.getUsername())
                    .get();
            if (nodeUser != null)
            {
                continue;
            }

            nodeUser = new MongoUser(nodeBucket.getBucketId(),
                                     defaultUser.getFullName(),
                                     defaultUser.getUsername(),
                                     defaultUser.getPassword(),
                                     "",
                                     "",
                                     "");
            nodeUser.setUserId(MongoUser.generateNewId());
            nodeUser.setActivated(true);
            nodeUser.addRoleId(nodeUserRole.getRoleId());
            nodeUser.save();
            Logger.info(Util.whichClass() + "Added default user (%s)", nodeUser.getName());
        }

        RoleManager.getInstance().addFeatures(nodeUserRole.getRoleId(), DefaultRole.NODE_USER.getFeatureNames());
    }
}

