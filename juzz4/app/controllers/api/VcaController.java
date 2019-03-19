package controllers.api;

import controllers.interceptors.APIInterceptor;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.Analytics.VcaError;
import models.MongoBucket;
import models.MongoDevice;
import models.MongoUser;
import models.abstracts.ServerPagedResult;
import models.node.NodeObject;
import models.transportobjects.VcaTransport;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;

import platform.DeviceManager;
import platform.Environment;
import platform.analytics.*;
import platform.analytics.app.AppVcaTypeMapper;
import platform.devices.DeviceChannelPair;
import platform.time.PeriodOfDay;
import platform.time.RecurrenceRule;
import play.Logger;
import play.mvc.With;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author KAI Square
 *         publicapi (hidden from API documentation)
 * @sectiontitle Video Analytics
 * @sectiondesc APIs for BI and Security analytics: CRUD, activate and deactivate operations
 */
@With(APIInterceptor.class)
public class VcaController extends APIController
{
    private static final VcaManager vcaManager = VcaManager.getInstance();
    private static final DeviceManager deviceMgr = DeviceManager.getInstance();
    private static void checkDeviceAccess(MongoDevice device) throws ApiException
    {
        String callerUserId = getCallerUserId();
        if (!deviceMgr.checkUserDeviceAccess(device.getDeviceId(), callerUserId))
        {
            throw new ApiException("device-access-denied");
        }
    }

    private static void checkVcaAccess(String appId) throws ApiException
    {
        String callerUserId = getCallerUserId();
        MongoUser callerUser = MongoUser.getById(callerUserId);
        VcaType vcaType = AppVcaTypeMapper.getVcaType(appId);
        if (!callerUser.hasAccessToVcaFeature(vcaType.getConfigFeature()))
        {
            throw new ApiException("vca-access-denied");
        }
    }

    /**
     * @param vca-type        vca type name
     * @param device-id       Camera's Platform DeviceId
     * @param channel-id      Camera's Channel ID
     * @param recurrence-rule weekly operation periods
     * @param thresholds      JSON string of analytics parameters
     *
     * @servtitle Adds a new VCA
     * @httpmethod POST
     * @uri /api/{bucket}/addvca
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void addvca()
    {
        try
        {
            String vcaTypeName = readApiParameter("type", true);
            String platformDeviceId = readApiParameter("device-id", true);
            String channelId = readApiParameter("channel-id", true);
            String settings = readApiParameter("thresholds", true);
            String jsonRule = readApiParameter("recurrence-rule", false);
            Program program = Program.parse(readApiParameter("program", false));
            //parse schedule
            RecurrenceRule recurrenceRule = RecurrenceRule.parse(jsonRule);

            //verify device
            MongoDevice dbDevice = MongoDevice.getByPlatformId(platformDeviceId);
            if (dbDevice == null)
            {
                throw new ApiException("Invalid device-id");
            }

            //device access
            checkDeviceAccess(dbDevice);

            //check vca type
            VcaType vcaType = null;
            try
            {
                vcaType = VcaType.parse(vcaTypeName);
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
            if (vcaType == null)
            {
                throw new ApiException("Invalid vca-type");
            }

            //vca access
            String appId = AppVcaTypeMapper.getAppId(program, vcaType);
            checkVcaAccess(appId);
            //app support
            
             //Because the thrift bugs has not fixed, so i will ignore this step for the moment
            if (!vcaManager.checkAppSupport(appId, dbDevice.getDeviceId()))
            {
                throw new ApiException("error-unsupported-vca-app");
            }

            //cloud-side checks
            if (Environment.getInstance().onCloud() && dbDevice.isKaiNode())
            {
                //check if node update is required
                NodeObject nodeObject = NodeObject.findByPlatformId(platformDeviceId);
                if (vcaManager.isNodeUpdateRequiredForVca(nodeObject.getReleaseNumber(), vcaType))
                {
                    throw new ApiException("node-update-required");
                }
            }

            //construct info
            String newInstanceId = RandomStringUtils.randomAlphanumeric(10).toUpperCase();
            VcaInfo vcaInfo = VcaInfo.createNew(newInstanceId,
                                                appId,
                                                new DeviceChannelPair(dbDevice.getCoreDeviceId(), channelId),
                                                settings,
                                                recurrenceRule,
                                                true);

            //check vca limit
            if (!vcaManager.checkConcurrencyLimit(vcaInfo))
            {
                throw new ApiException("error-vca-concurrent-capacity-full");
            }

            vcaManager.addNewVca(vcaInfo);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param instance-id     vca instance id
     * @param recurrence-rule weekly operation periods
     * @param thresholds      JSON string of analytics parameters
     *
     * @servtitle updates an existing vca
     * @httpmethod POST
     * @uri /api/{bucket}/updatevca
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updatevca()
    {
        try
        {
            String instanceId = readApiParameter("instance-id", true);
            String settings = readApiParameter("thresholds", true);
            String jsonRule = readApiParameter("recurrence-rule", false);

            IVcaInstance vcaInstance = vcaManager.getVcaInstance(instanceId);
            if (vcaInstance == null)
            {
                throw new ApiException("vca-instance-not-found");
            }

            //parse
            RecurrenceRule updatedRule = RecurrenceRule.parse(jsonRule);

            //check access
            MongoDevice dbDevice = vcaInstance.getVcaInfo().getCamera().getDbDevice();
            checkDeviceAccess(dbDevice);
            checkVcaAccess(vcaInstance.getVcaInfo().getAppId());

            //check vca limit
            VcaInfo updatedInfo = vcaInstance.getVcaInfo();
            updatedInfo.setRecurrenceRule(updatedRule);
            if (!vcaManager.checkConcurrencyLimit(updatedInfo))
            {
                throw new ApiException("error-vca-concurrent-capacity-full");
            }

            vcaInstance.update(settings, updatedRule);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param instance-id vca instance id
     *
     * @servtitle activate a vca
     * @httpmethod POST
     * @uri /api/{bucket}/activatevca
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void activatevca()
    {
    	
    	
        try
        {
            String instanceId = readApiParameter("instance-id", false);
            IVcaInstance vcaInstance = vcaManager.getVcaInstance(instanceId);
            if (vcaInstance == null)
            {
                throw new ApiException("vca-instance-not-found");
            }

            //check access
            MongoDevice dbDevice = vcaInstance.getVcaInfo().getCamera().getDbDevice();
            checkDeviceAccess(dbDevice);
            checkVcaAccess(vcaInstance.getVcaInfo().getAppId());

            vcaInstance.activate();
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param instance-id vca instance id
     *
     * @servtitle deactivate a vca
     * @httpmethod POST
     * @uri /api/{bucket}/deactivatevca
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void deactivatevca()
    {
        try
        {
            String instanceId = readApiParameter("instance-id", false);
            IVcaInstance vcaInstance = vcaManager.getVcaInstance(instanceId);
            if (vcaInstance == null)
            {
                throw new ApiException("vca-instance-not-found");
            }

            //check access
            MongoDevice dbDevice = vcaInstance.getVcaInfo().getCamera().getDbDevice();
            checkDeviceAccess(dbDevice);
            checkVcaAccess(vcaInstance.getVcaInfo().getAppId());

            vcaInstance.deactivate();
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param instance-id vca instance id
     *
     * @servtitle remove vca
     * @httpmethod POST
     * @uri /api/{bucket}/removevca
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void removevca()
    {
        try
        {
            String instanceId = readApiParameter("instance-id", true);

            IVcaInstance vcaInstance = vcaManager.getVcaInstance(instanceId);
            if (vcaInstance != null)
            {
                //check access
                MongoDevice dbDevice = vcaInstance.getVcaInfo().getCamera().getDbDevice();
                checkDeviceAccess(dbDevice);
                checkVcaAccess(vcaInstance.getVcaInfo().getAppId());

                vcaInstance.remove();
            }

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param analytics-type Analytics type. Refer to {@link platform.analytics.VcaType}
     *                       Use "ALL" to retrieve all vca
     *
     * @servtitle list Running Analytics
     * @httpmethod POST
     * @uri /api/{bucket}/listrunninganalytics
     * @responsejson {
     * "result": "ok",
     * "instances": [ {@link VcaTransport} ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void listrunninganalytics() throws ApiException
    {
        try
        {
            String analyticsType = readApiParameter("analytics-type", true);
            VcaType vcaType;
            if (analyticsType.toLowerCase().equals("all"))
            {
                vcaType = null;
            }
            else
            {
                vcaType = VcaType.parse(analyticsType);
            }

            //compile user's device id list
            String callerUserId = getCallerUserId();
            MongoUser callerUser = MongoUser.getById(callerUserId);
            List<MongoDevice> userDevices = DeviceManager.getInstance().getDevicesOfUser(callerUserId);
            List<String> coreDeviceIdList = new ArrayList<>();
            for (MongoDevice userDevice : userDevices)
            {
                if (!userDevice.isSuspended())
                {
                    coreDeviceIdList.add(userDevice.getCoreDeviceId());
                }
            }

            //list by type
            List<IVcaInstance> vcaList = vcaManager.listVcaInstances(coreDeviceIdList);

            //filter
            List<VcaTransport> transports = new ArrayList<>();
            for (IVcaInstance inst : vcaList)
            {
                //filter by access
                VcaType currentInstType = AppVcaTypeMapper.getVcaType(inst.getVcaInfo().getAppId());
                if (!callerUser.hasAccessToVcaFeature(currentInstType.getConfigFeature()) && !callerUser.hasAccessToVcaFeature(currentInstType.getReportFeature()))
                {
                    continue;
                }

                VcaTransport transport = new VcaTransport(inst);
                if (vcaType != null && !vcaType.getVcaTypeName().equals(transport.type))
                {
                    continue;
                }

                transports.add(transport);
            }

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("instances", transports);
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id      target bucket ID
     * @param analytics-type Analytics type. Refer to AnalyticsType.java
     *
     * @servtitle list Running Analytics of the target bucket
     * @httpmethod POST
     * @uri /api/{bucket}/listanalyticsbybucketid
     * @responsejson {
     * "result": "ok",
     * "instances": [
     * {
     * "instanceId":"OIFTA4ZDFG",
     * "type":"TRAFFIC",
     * "platformDeviceId":"2",
     * "coreDeviceId":"2",
     * "channelId":"0",
     * "thresholds":"{
     * \"totalBoxes\":3,\"sourceBox\":\"A2\",
     * \"boxes\":[\"0.3100,0.3933,0.5975,0.7800\",\"0.1700,0.2033,0.4300,0.6367\",
     * \"0.5375,0.6133,0.7700,0.9367\"],\"minWidth\":0.05,\"minHeight\":0.1,\"ksize\":5,
     * \"sigma\":5,\"ccthresh\":0.05,\"tsr\":20,\"additional-params\":{}
     * }",
     * "enabled":true,
     * "vcaState":"RUNNING",
     * "_id":"539aebde60edf425633e577e",
     * "_created":1402661854297,
     * "_modified":1402661854297
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void listanalyticsbybucketid() throws ApiException
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            String analyticsType = readApiParameter("analytics-type", true);

            //validate
            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("bucket-not-found");
            }

            VcaType vcaType;
            if (analyticsType.toLowerCase().equals("all"))
            {
                vcaType = null;
            }
            else
            {
                vcaType = VcaType.parse(analyticsType);
            }

            //bucket access
            MongoBucket callerBucket = MongoBucket.getById(getCallerBucketId());
            if (callerBucket == null || !callerBucket.hasControlOver(targetBucket))
            {
                throw new ApiException("msg-no-rights-to-buckets");
            }

            //compile bucket devices
            List<MongoDevice> bucketDevices = DeviceManager.getInstance().getDevicesOfBucket(targetBucket.getBucketId());
            List<String> coreDeviceIdList = new ArrayList<>();
            for (MongoDevice bktDevice : bucketDevices)
            {
                if (!bktDevice.isSuspended())
                {
                    coreDeviceIdList.add(bktDevice.getCoreDeviceId());
                }
            }

            //find
            List<IVcaInstance> vcaList = vcaManager.listVcaInstances(coreDeviceIdList);
            List<VcaTransport> transports = new ArrayList<>();
            for (IVcaInstance inst : vcaList)
            {
                VcaTransport transport = new VcaTransport(inst);
                if (vcaType != null && !vcaType.getVcaTypeName().equals(transport.type))
                {
                    continue;
                }

                transports.add(transport);
            }

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("instances", transports);
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param instance-id Analytics instance id
     *
     * @servtitle Gets the executed command of a running Analytics
     * @httpmethod GET
     * @uri /api/{bucket}/getvcacommands
     * @responsejson {
     * "result": "ok",
     * "commands": command string,
     * @responsejson {
     * "result": "error",
     * "reason": "failed-to-get-vca-commands"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getvcacommands(String bucket) throws ApiException
    {
        try
        {
            String instanceId = readApiParameter("instance-id", true);
            List<String> commandList = vcaManager.getVcaProcessCommands(instanceId);
            if (commandList == null)
            {
                throw new ApiException("failed-to-get-vca-commands");
            }

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("commands", StringUtils.join(commandList, " "));
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param instance-id Analytics instance id
     * @param offset      starting index
     * @param take        return error count
     *
     * @servtitle Get vca program's error outputs
     * @httpmethod POST
     * @uri /api/{bucket}/getvcaerrors
     * @responsejson {
     * "result": "ok",
     * "total-count": 123,
     * "errors" : [ {@link VcaError} ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getvcaerrors() throws ApiException
    {
        try
        {
            String instanceId = readApiParameter("instance-id", true);
            int offset = Integer.parseInt(readApiParameter("offset", true));
            int take = Integer.parseInt(readApiParameter("take", true));

            ServerPagedResult pagedResult = VcaError.query(instanceId, offset, take);

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("total-count", pagedResult.getTotalCount());
            responseMap.put("errors", pagedResult.getResultsForOnePage());
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param node-id             Node's platform device Id. Mandatory on cloud
     * @param minimum-concurrency [Optional] Periods with smaller concurrences will be filtered out
     *
     * @servtitle Get vca program's error outputs
     * @httpmethod POST
     * @uri /api/{bucket}/getvcaconcurrencystatus
     * @responsejson {
     * "result": "ok",
     * "status": ""
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getvcaconcurrencystatus() throws ApiException
    {
        try
        {
            boolean onCloud = Environment.getInstance().onCloud();
            String nodeId = readApiParameter("node-id", onCloud);
            String minConcurrency = readApiParameter("minimum-concurrency", false);

            int iMinConcurrency = 2;
            if (!minConcurrency.isEmpty())
            {
                if (!Util.isInteger(minConcurrency))
                {
                    throw new ApiException("invalid-minimum-concurrency");
                }

                iMinConcurrency = Integer.parseInt(minConcurrency);
            }

            List<IVcaInstance> instances;
            if (onCloud)
            {
                MongoDevice nodeDevice = MongoDevice.getByPlatformId(nodeId);
                instances = vcaManager.listVcaInstancesOfDevice(nodeDevice);
            }
            else
            {
                instances = vcaManager.listVcaInstances(null);
            }

            //compile info list
            Map<String, IVcaInstance> dbInstLookup = new LinkedHashMap<>();
            List<VcaInfo> vcaInfoList = new ArrayList<>();
            for (IVcaInstance dbInst : instances)
            {
                dbInstLookup.put(dbInst.getVcaInfo().getInstanceId(), dbInst);
                vcaInfoList.add(dbInst.getVcaInfo());
            }

            //find
            Map<Integer, Map<PeriodOfDay, List<VcaInfo>>> concurrencyMap = vcaManager
                    .findConcurrentVcaPeriods(vcaInfoList, iMinConcurrency);

            //convert it to transport objects
            Map<Integer, Map<PeriodOfDay, List<VcaTransport>>> transportData = new LinkedHashMap<>();
            for (Map.Entry<Integer, Map<PeriodOfDay, List<VcaInfo>>> dayEntry : concurrencyMap.entrySet())
            {
                Map<PeriodOfDay, List<VcaTransport>> convertedPeriods = new LinkedHashMap<>();
                for (Map.Entry<PeriodOfDay, List<VcaInfo>> periodEntry : dayEntry.getValue().entrySet())
                {
                    List<VcaTransport> convertedList = new ArrayList<>();
                    for (VcaInfo vcaInfo : periodEntry.getValue())
                    {
                        IVcaInstance dbInst = dbInstLookup.get(vcaInfo.getInstanceId());
                        convertedList.add(new VcaTransport(dbInst));
                    }
                    convertedPeriods.put(periodEntry.getKey(), convertedList);
                }
                transportData.put(dayEntry.getKey(), convertedPeriods);
            }

            respondOK("status", transportData);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
    
    
    
}
