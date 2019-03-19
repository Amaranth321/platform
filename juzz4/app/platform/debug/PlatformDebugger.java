package platform.debug;

import com.kaisquare.core.thrift.StorageInfo;
import com.kaisquare.vca.thrift.TVcaServerInfo;
import jobs.node.NodeJobsManager;
import lib.util.exceptions.ApiException;
import models.MongoDeviceModel;
import models.NodeCommand;
import models.SoftwareUpdateFile;
import models.SystemVersion;
import models.node.NodeObject;
import models.notification.BucketNotificationSettings;
import models.notification.EventToNotify;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.DeviceManager;
import platform.Environment;
import platform.StorageManager;
import platform.analytics.VcaAppInfo;
import platform.analytics.VcaThriftClient;
import platform.content.delivery.DeliveryMethod;
import platform.content.delivery.DeliveryStats;
import platform.events.EventType;
import platform.kaisyncwrapper.CloudCommandQueueStats;
import platform.kaisyncwrapper.KaiSyncHelper;
import platform.kaisyncwrapper.node.PrioritizedCommandQueue;
import platform.kaisyncwrapper.node.SequencedCommandQueue;
import platform.node.NodeManager;
import play.Logger;
import play.i18n.Lang;
import play.i18n.Messages;
import play.modules.morphia.Model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Aye Maung
 */
public class PlatformDebugger
{
    private static final String LOG_TIME_FORMAT = "MMM dd HH:mm:ss";
    private static final PlatformDebugger instance = new PlatformDebugger();

    private PlatformDebugger()
    {
    }

    public static PlatformDebugger getInstance()
    {
        return instance;
    }

    public static String timestamp(long millis)
    {
        return new DateTime(millis, DateTimeZone.UTC).toString(LOG_TIME_FORMAT);
    }

    public String getCommandQueuesStatus() throws ApiException
    {
        try
        {
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);

            /**
             *      Nodes Only
             *
             */
            if (Environment.getInstance().onKaiNode())
            {
                out.println(SequencedCommandQueue.getInstance().getPrintedStatus());
                out.println();
                out.println(PrioritizedCommandQueue.getInstance().getPrintedStatus());
            }

            /**
             *      Cloud Only
             *
             */
            if (Environment.getInstance().onCloud())
            {
                int displayLimit = 20;
                int count = 0;
                int unknownResetCount = CloudCommandQueueStats.getUnknownResetCount();
                out.println(String.format("Cloud command queues (unknown resets : %s)", unknownResetCount));
                out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                for (CloudCommandQueueStats.QueueStats stats : CloudCommandQueueStats.getQueueStats())
                {
                    count++;
                    out.println(String.format("[%-7s] %6s mins : %2s in queue : %s",
                                              stats.processing ? "Pending" : "Idle",
                                              TimeUnit.MILLISECONDS.toMinutes(stats.waitingTime),
                                              stats.queueCount,
                                              stats.name));
                    if (count == displayLimit)
                    {
                        break;
                    }
                }
                out.println();
            }

            /**
             *      Both Nodes and Cloud
             *
             */
            Model.MorphiaQuery query = KaiSyncHelper.queryAllCommandsInProgress();
            int recentCount = 30;
            out.println(String.format("KaiSync commands in progress (Last %s)", recentCount));
            out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

            Iterator<NodeCommand> iterator = query.order("-createdTime").limit(recentCount).iterator();
            while (iterator.hasNext())
            {
                NodeCommand cmd = iterator.next();
                out.println(printNodeCommand(cmd));
            }

            return sw.toString();

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return "";
        }
    }

    public String getSyncTasksStatus()
    {
        StringBuilder builder = new StringBuilder();

        //node
        if (Environment.getInstance().onKaiNode())
        {
            builder.append(NodeJobsManager.getInstance().getPrintedStatusList());
        }

        return builder.toString();
    }

    public String getDeliveryJobsStatus()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        //node
        if (Environment.getInstance().onKaiNode())
        {
            out.println(String.format("%-25s (%s : items left)", "NodeNotificationsJob", EventToNotify.q().count()));
        }

        //cloud only
        else if (Environment.getInstance().onCloud())
        {
            out.println(String.format("%-25s (%s : items left)", "CloudNotificationsJob", EventToNotify.q().count()));
            out.println();

            List<DeliveryMethod> methods = Arrays.asList(
                    DeliveryMethod.EMAIL,
                    DeliveryMethod.FTP,
                    DeliveryMethod.SMS,
                    DeliveryMethod.MOBILE_PUSH
            );

            DeliveryStats deliveryStats = DeliveryStats.get();
            String format = "%-25s (Sent:%5s, Failed:%5s, Left:%5s)";
            for (DeliveryMethod method : methods)
            {
                out.println(String.format(format,
                                          method,
                                          deliveryStats.getSuccessCount(method),
                                          deliveryStats.getFailCount(method),
                                          deliveryStats.getRemaining(method)));
            }
        }

        return sw.toString();
    }

    public String getMigrationStatus()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        String format = "%-20s: %s";
        out.println("Migration status");
        out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        out.println(String.format(format, "Last upgraded", timestamp(SystemVersion.getLastUpgraded())));
        out.println(String.format(format, "Active jobs", SystemVersion.getActiveMigrationCount()));
        return sw.toString();
    }

    public String getNodePlatformSettings() throws ApiException
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        BucketNotificationSettings settings = NodeManager.getInstance().getBucket().getNotificationSettings();
        out.println("Notification Settings");
        out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        String format = "%-30s: notification (%4s): video (%4s)";
        for (EventType eventType : settings.getSupportedEventTypes())
        {
            BucketNotificationSettings.EventTypeSettings typeSetts = settings.getSettingsByType(eventType);
            out.println(String.format(format,
                                      Messages.get(eventType),
                                      typeSetts.isNotificationEnabled() ? "true" : "",
                                      typeSetts.isVideoRequired() ? "true" : ""));
        }

        return sw.toString();
    }

    public String getVcaServerInfo()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println("Vca Server info");
        out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

        TVcaServerInfo serverInfo = VcaThriftClient.getInstance().getServerInformation();
        if (serverInfo == null)
        {
            out.println("offline");
        }
        else
        {
            out.println(String.format("Started @ %s", timestamp(serverInfo.getServerStartTime())));
            out.println(String.format("Release : %s", serverInfo.getReleaseNumber()));
            out.println();

            out.println("Threads");
            out.println("~~~~~~~");
            for (String threadName : serverInfo.getThreads())
            {
                out.println(threadName);
            }
            out.println();

            out.println("Vca programs");
            out.println("~~~~~~~~~~~~");
            List<VcaAppInfo> programInfoList = VcaThriftClient.getInstance().getVcaAppInfoList();
            String format = "%-30s : %s";
            for (VcaAppInfo appInfo : programInfoList)
            {
                out.println(String.format(format,
                                          appInfo.displayName.get(Lang.get()),
                                          appInfo.version));
            }
            out.println();
        }

        return sw.toString();
    }

    private String printNodeCommand(NodeCommand cmd)
    {
        String time = new DateTime(cmd._getCreated()).toString("dd/MM/yyyy hh:mm:ss");
        String format = "%s %s (%s) > %s";
        String stringForm = "";
        try
        {
            if (Environment.getInstance().onCloud())
            {
                String nodeName = "Node no longer exists";
                if (DeviceManager.getInstance().nodeObjectExists(cmd.getNodeId()))
                {
                    NodeObject nodeObject = NodeObject.findByPlatformId(cmd.getNodeId());
                    nodeName = nodeObject.getName();
                }

                stringForm = String.format(format, time, cmd.getCommand(), cmd.getState(), nodeName);
            }
            else if (Environment.getInstance().onKaiNode())
            {
                stringForm = String.format(format, time, cmd.getCommand(), cmd.getState(), "Cloud");
            }
        }
        catch (Exception e)
        {
            stringForm = String.format(format,
                                       time, cmd.getCommand(), cmd.getState(), e.getMessage());
        }

        return stringForm;
    }

    public String listOTAFiles()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println("Eligible OTA files");
        out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

        List<MongoDeviceModel> deviceModels = MongoDeviceModel.q().fetchAll();
        Set<?> releaseSet = SoftwareUpdateFile.q().distinct("releaseNumber");
        List<Double> sortedReleases = new ArrayList<>();
        for (Object o : releaseSet)
        {
            sortedReleases.add(((Double) o));
        }
        Collections.sort(sortedReleases);

        for (MongoDeviceModel model : deviceModels)
        {
            if (!model.isKaiNode())
            {
                continue;
            }
            for (Double releaseNo : sortedReleases)
            {
                SoftwareUpdateFile file = SoftwareUpdateFile.findEligibleUpdate(Long.parseLong(model.getModelId()), releaseNo);
                out.println(String.format("%-30s : %s => %s",
                        model.getName(),
                        releaseNo,
                        file == null ? "no file" : file.getVersion()));
            }
        }

        return sw.toString();
    }

    public String cloudCoreEngineStatus()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println("Core Engine");
        out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

        out.println("Storage :");
        List<StorageInfo> serverList = StorageManager.getInstance().coreStorageStatus();
        for (StorageInfo storageInfo : serverList)
        {
            out.println(String.format("[%s] Total : %s MB, Free : %s MB",
                                      storageInfo.getServerHost(),
                                      storageInfo.getTotalSpace(),
                                      storageInfo.getFreeSpace())
            );
        }

        return sw.toString();
    }
}
