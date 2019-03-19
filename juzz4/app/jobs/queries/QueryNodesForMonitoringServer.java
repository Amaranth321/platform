package jobs.queries;

import com.google.gson.Gson;
import com.kaisquare.transports.monitoring.*;
import lib.util.Util;
import models.Analytics.NodeTmpVcaInstance;
import models.Analytics.NodeVcaInstance;
import models.Analytics.TickerReport;
import models.MongoBucket;
import models.MongoDevice;
import models.MongoDeviceModel;
import models.UpTimeLog;
import models.abstracts.ServerPagedResult;
import models.node.NodeCamera;
import models.node.NodeObject;
import models.node.NodeSettings;
import models.stats.VcaHourlyStats;
import platform.analytics.*;
import platform.analytics.app.AppVcaTypeMapper;
import platform.db.QueryHelper;
import platform.devices.DeviceChannelPair;
import platform.node.KaiNodeAdminService;
import platform.nodesoftware.NodeSoftwareStatus;
import platform.reports.EventReport;
import platform.time.RecurrenceRule;
import platform.time.UtcPeriod;
import play.Logger;
import play.jobs.Job;
import play.modules.morphia.Model;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Aye Maung
 */
public class QueryNodesForMonitoringServer extends Job<ServerPagedResult<Node>>
{
    private final Model.MorphiaQuery nodeQuery;
    private final int offset;
    private final int limit;
    private final UtcPeriod queryPeriod;

    public QueryNodesForMonitoringServer(int offset, int limit, long since)
    {
        nodeQuery = NodeObject.q();
        this.offset = offset;
        this.limit = limit;
        this.queryPeriod = new UtcPeriod(since, System.currentTimeMillis());

        nodeQuery.order("cloudCoreDeviceId");
    }

    @Override
    public ServerPagedResult<Node> doJobWithResult()
    {
        ServerPagedResult<NodeObject> onePage = QueryHelper.preparePagedResult(nodeQuery, offset, limit);

        //transform
        List<Node> transformedList = new ArrayList<>();
        for (NodeObject nodeObject : onePage.getResultsForOnePage())
        {
            long startTime = System.currentTimeMillis();
            transformedList.add(transform(nodeObject));
            long dur = System.currentTimeMillis() - startTime;
            if (dur > TimeUnit.SECONDS.toMillis(1))
            {
                Logger.info("[%s] node query took more than 1s", nodeObject.getName());
            }
        }

        ServerPagedResult<Node> transformedResults = new ServerPagedResult<>();
        transformedResults.setTotalCount(onePage.getTotalCount());
        transformedResults.setResultsForOnePage(transformedList);
        return transformedResults;
    }

    private Node transform(NodeObject nodeObject)
    {
        MongoDevice nodeDevice = nodeObject.getDbDevice();
        MongoBucket bucket = MongoBucket.getById(nodeDevice.getBucketId());
        MongoDeviceModel nodeDeviceModel = MongoDeviceModel.getByModelId(nodeDevice.getModelId());

        //things that can go wrong sometimes for unknown reasons
        String timezone = "";
        String ipAddress = "";
        try
        {
            NodeSettings nodeSettings = nodeObject.getSettings();
            if (nodeSettings != null)
            {
                timezone = nodeSettings.getTimezone();

                KaiNodeAdminService.NetworkSettings netSetts = nodeSettings.getNetworkSettings();
                if (netSetts != null)
                {
                    ipAddress = netSetts.getAddress();
                }
            }
        }
        catch (Exception e)
        {
        }

        //app list
        List<AppInfo> supportedAppList = new ArrayList<>();
        for (VcaAppInfo vcaAppInfo : nodeObject.getSupportedAppList())
        {
            supportedAppList.add(new AppInfo(vcaAppInfo.appId, vcaAppInfo.version));
        }

        //camera list
        List<Camera> cameraList = new ArrayList<>();
        for (NodeCamera nodeCamera : nodeObject.getCameras())
        {
            //list vca running on this camera
            DeviceChannelPair idPair = new DeviceChannelPair(nodeDevice.getCoreDeviceId(), nodeCamera.nodeCoreDeviceId);

            List<IVcaInstance> vcaInstances = VcaManager.getInstance().listVcaInstancesByCamera(idPair);
            List<Vca> vcaTransportList = new ArrayList<>();
            for (IVcaInstance inst : vcaInstances)
            {
                VcaInfo vcaInfo = inst.getVcaInfo();
                RecurrenceRule schedule = vcaInfo.getRecurrenceRule();

                //model specific info
                VcaStatus status;
                long createdTime;
                long activatedTime;
                if (inst instanceof NodeVcaInstance)
                {
                    NodeVcaInstance nodeInst = (NodeVcaInstance) inst;
                    status = nodeInst.getStatus();
                    createdTime = nodeInst._getCreated();
                    activatedTime = nodeInst.getActivatedTime();
                }
                else if (inst instanceof NodeTmpVcaInstance)
                {
                    NodeTmpVcaInstance tmpInst = (NodeTmpVcaInstance) inst;
                    status = VcaStatus.WAITING;
                    createdTime = tmpInst._getCreated();
                    activatedTime = 0;
                }
                else
                {
                    Logger.error(Util.whichFn() + "Unknown instance class: %s (%s)", inst.getClass(), inst.getVcaInfo());
                    continue;
                }

                vcaTransportList.add(
                        new Vca(vcaInfo.getInstanceId(),
                                vcaInfo.getAppId(),
                                schedule == null ? "" : new Gson().toJson(schedule),
                                vcaInfo.getSettings(),
                                vcaInfo.isEnabled(),
                                status.name(),
                                getVcaHourlyRecords(vcaInfo),
                                getUpTimePeriods(vcaInfo),
                                createdTime,
                                activatedTime)
                );
            }

            cameraList.add(new Camera(nodeCamera.nodeCoreDeviceId,
                                      nodeCamera.name,
                                      nodeCamera.model.name,
                                      nodeCamera.host,
                                      Integer.parseInt(nodeCamera.port),
                                      nodeCamera.cloudRecordingEnabled,
                                      nodeCamera.getStatus().name(),
                                      vcaTransportList));
        }

        boolean updateAvailable = (nodeObject.getSoftwareStatus() == NodeSoftwareStatus.UPDATE_AVAILABLE);

        return new Node(nodeObject.getNodeCoreDeviceId(),
                bucket.getName(),
                nodeObject.getName(),
                nodeObject.getNodeVersion(),
                nodeDeviceModel.getName(),
                nodeDevice.getDeviceKey(),
                nodeDevice.getStatus().name(),
                updateAvailable,
                nodeObject.getDownloadedVersion(),
                timezone,
                ipAddress,
                supportedAppList,
                cameraList
        );
    }

    private List<VcaHourlyRecord> getVcaHourlyRecords(VcaInfo vcaInfo)
    {
        //create stats lookup map
        Map<Long, VcaHourlyStats> statsLookup = new LinkedHashMap<>();
        Iterable<VcaHourlyStats> statsList = VcaHourlyStats.q()
                .filter("instanceId", vcaInfo.getInstanceId())
                .filter("time >=", queryPeriod.getFromMillis())
                .fetch();
        for (VcaHourlyStats stats : statsList)
        {
            statsLookup.put(stats.getTime(), stats);
        }

        //create data count lookup
        Map<Long, Integer> dataCountLookup = new LinkedHashMap<>();
        VcaType vcaType = AppVcaTypeMapper.getVcaType(vcaInfo.getAppId());

        Iterable<TickerReport> reports = EventReport.getReport(vcaType.getEventType())
                .query(queryPeriod.getFromTime().toDate(), queryPeriod.getToTime().toDate())
                .addDevice(vcaInfo.getCamera())
                .getQuery()
                .fetch();
        for (TickerReport report : reports)
        {
            long hourMark = report.time;
            dataCountLookup.put(hourMark, Math.round(report.count));
        }

        //combine the two lookup maps
        TreeSet<Long> sortedHours = new TreeSet<>();
        sortedHours.addAll(statsLookup.keySet());
        sortedHours.addAll(dataCountLookup.keySet());
        List<VcaHourlyRecord> compiledRecords = new ArrayList<>();
        for (Long hourMark : sortedHours)
        {
            VcaHourlyStats stats = statsLookup.get(hourMark);
            int stateCount = stats == null ? 0 : stats.getStateChangeCount();
            int errorCount = stats == null ? 0 : stats.getErrorEventCount();

            Integer dataCount = dataCountLookup.get(hourMark);
            dataCount = dataCount == null ? 0 : dataCount;

            compiledRecords.add(new VcaHourlyRecord(hourMark, stateCount, dataCount, errorCount));
        }

        return compiledRecords;
    }

    private List<Period> getUpTimePeriods(VcaInfo vcaInfo)
    {
        List<Period> periodList = new ArrayList<>();
        List<UtcPeriod> utcPeriods = UpTimeLog.findVcaUpPeriods(vcaInfo.getInstanceId(), queryPeriod, 5);
        for (UtcPeriod utcPeriod : utcPeriods)
        {
            periodList.add(new Period(utcPeriod.getFromMillis(), utcPeriod.getToMillis()));
        }

        return periodList;
    }
}
