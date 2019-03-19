package controllers.api;

import com.google.gson.Gson;
import controllers.interceptors.APIInterceptor;
import lib.util.exceptions.ApiException;
import models.MongoDevice;
import models.labels.DeviceLabel;
import models.labels.LabelOthers;
import models.labels.LabelRegion;
import models.labels.LabelStore;
import models.node.NodeCamera;
import models.node.NodeObject;
import models.transportobjects.LabelTransport;
import platform.common.Location;
import platform.devices.DeviceChannelPair;
import platform.label.LabelManager;
import platform.label.LabelType;
import platform.time.OperatingSchedule;
import platform.time.TimeUtil;
import platform.time.WeeklyPeriods;
import play.mvc.With;

import java.util.*;

/**
 * @author KAI Square
 * @sectiontitle Label Management
 * @sectiondesc APIs to mange camera labels
 * @publicapi
 */
@With(APIInterceptor.class)
public class LabelController extends APIController
{
    private static final LabelManager labelMgr = LabelManager.getInstance();

    /**
     * @servtitle get all device labels of the bucket
     * @httpmethod POST
     * @uri /api/{bucket}/getlabels
     * @responsejson {
     * "result": "ok",
     * "labels": [
     * {@link models.labels.LabelStore},
     * {@link models.labels.LabelRegion},
     * {@link models.labels.LabelOthers}
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getlabels() throws ApiException
    {
        try
        {
            long callerBucketId = Long.parseLong(renderArgs.get("caller-bucket-id").toString());

            List<DeviceLabel> bucketLabels = labelMgr.getBucketLabels(callerBucketId);
            List<LabelTransport> transports = new ArrayList<>();
            for (DeviceLabel bucketLabel : bucketLabels)
            {
                transports.add(new LabelTransport(bucketLabel));
            }

            // sort by name before returning
            Collections.sort(transports, new Comparator<LabelTransport>()
            {
                @Override
                public int compare(LabelTransport o1, LabelTransport o2)
                {
                    return o1.name.compareToIgnoreCase(o2.name);
                }
            });

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("labels", transports);
            renderJSON(response);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle get labels that have the cameras assigned to the caller user
     * @httpmethod POST
     * @uri /api/{bucket}/getuseraccessiblelabels
     * @responsejson {
     * "result": "ok",
     * "labels": [
     * {@link models.labels.LabelStore},
     * {@link models.labels.LabelRegion},
     * {@link models.labels.LabelOthers}
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getuseraccessiblelabels() throws ApiException
    {
        try
        {
            long callerUserId = Long.parseLong(getCallerUserId());
            Set<DeviceLabel> userLabels = LabelManager.getInstance().getUserAccessibleLabels(callerUserId);
            List<LabelTransport> transports = new ArrayList<>();
            for (DeviceLabel bucketLabel : userLabels)
            {
                transports.add(new LabelTransport(bucketLabel));
            }

            // sort by name before returning
            Collections.sort(transports, new Comparator<LabelTransport>()
            {
                @Override
                public int compare(LabelTransport o1, LabelTransport o2)
                {
                    return o1.name.compareToIgnoreCase(o2.name);
                }
            });

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("labels", transports);
            renderJSON(response);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id bucket id. Mandatory
     *
     * @servtitle get all device labels of the bucket specified by 'bucket-id'
     * @httpmethod POST
     * @uri /api/{bucket}/getlabelsbybucketid
     * @responsejson {
     * "result": "ok",
     * "labels": [
     * {@link models.labels.LabelStore},
     * {@link models.labels.LabelRegion},
     * {@link models.labels.LabelOthers}
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getlabelsbybucketid() throws ApiException
    {
        try
        {
            long bucketId = asLong(readApiParameter("bucket-id", true));

            List<DeviceLabel> bucketLabels = labelMgr.getBucketLabels(bucketId);
            List<LabelTransport> transports = new ArrayList<>();
            for (DeviceLabel bucketLabel : bucketLabels)
            {
                transports.add(new LabelTransport(bucketLabel));
            }

            // sort by name before returning
            Collections.sort(transports, new Comparator<LabelTransport>()
            {
                @Override
                public int compare(LabelTransport o1, LabelTransport o2)
                {
                    return o1.name.compareToIgnoreCase(o2.name);
                }
            });

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("labels", transports);
            renderJSON(response);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param label-name label name. Mandatory
     * @param label-type lable type. See {@link platform.label.LabelType}. Mandatory
     * @param label-info label info. Mandatory for STORE and REGION types
     *
     * @servtitle create a new label
     * @httpmethod POST
     * @uri /api/{bucket}/addlabel
     * @responsejson {
     * "result": "ok"
     * "label-id": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void addlabel() throws ApiException
    {
        try
        {
            long callerBucketId = Long.parseLong(getCallerBucketId());

            String labelName = readApiParameter("label-name", true);
            String labelType = readApiParameter("label-type", true);
            String labelInfo = readApiParameter("label-info", false);

            //check name
            if (labelMgr.findLabel(callerBucketId, labelName) != null)
            {
                throw new ApiException("label-name-in-use");
            }

            //check type
            if (!LabelType.isValid(labelType))
            {
                throw new ApiException("invalid-label-type");
            }

            //todo: remove when supported
            if (labelType.equals(LabelType.REGION))
            {
                throw new ApiException("REGION type is currently not supported");
            }

            LabelType enumType = LabelType.valueOf(labelType.toUpperCase());
            String labelId;
            switch (enumType)
            {
                case STORE:
                    //parse
                    Location stLoc = parseLocation(labelInfo);
                    OperatingSchedule stSched = parseSchedule(labelInfo);

                    //create
                    LabelStore store = LabelStore.createNew(callerBucketId, labelName);
                    store.setLocation(stLoc);
                    store.setSchedule(stSched);
                    store.save();
                    labelId = store.getLabelId();
                    break;

                case REGION:
                    //parse
                    Location regLoc = parseLocation(labelInfo);
                    OperatingSchedule regSched = parseSchedule(labelInfo);

                    //create
                    LabelRegion region = LabelRegion.createNew(callerBucketId, labelName);
                    region.setLocation(regLoc);
                    region.setSchedule(regSched);
                    region.save();
                    labelId = region.getLabelId();
                    break;

                case OTHERS:
                    LabelOthers others = LabelOthers.createNew(callerBucketId, labelName);
                    others.save();
                    labelId = others.getLabelId();
                    break;

                default:
                    throw new UnsupportedOperationException();
            }

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("label-id", labelId);
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param label-id   label id. Mandatory
     * @param label-name label name. Mandatory
     * @param label-type lable type. See {@link platform.label.LabelType}. Mandatory
     * @param label-info label info. Mandatory for STORE and REGION types
     *
     * @servtitle updates a label
     * @httpmethod POST
     * @uri /api/{bucket}/updatelabel
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updatelabel() throws ApiException
    {
        try
        {
            long callerBucketId = Long.parseLong(renderArgs.get("caller-bucket-id").toString());

            String labelId = readApiParameter("label-id", true);
            String labelName = readApiParameter("label-name", true);
            String labelType = readApiParameter("label-type", true);
            String labelInfo = readApiParameter("label-info", false);

            //check id
            DeviceLabel targetLabel = labelMgr.findLabel(labelId);
            if (targetLabel == null)
            {
                throw new ApiException("invalid-label-id");
            }

            //check name
            DeviceLabel sameName = labelMgr.findLabel(callerBucketId, labelName);
            if (sameName != null && !sameName.getLabelId().equals(targetLabel.getLabelId()))
            {
                throw new ApiException("label-name-in-use");
            }

            //check type
            if (!LabelType.isValid(labelType))
            {
                throw new ApiException("invalid-label-type");
            }

            LabelType enumType = LabelType.valueOf(labelType.toUpperCase());
            boolean typeChanged = !enumType.equals(targetLabel.getType());
            switch (enumType)
            {
                case STORE:
                    //parse inputs
                    Location stLoc = parseLocation(labelInfo);
                    OperatingSchedule stSched = parseSchedule(labelInfo);

                    LabelStore labelStore;
                    if (typeChanged)
                    {
                        labelStore = LabelStore.createFrom(targetLabel);
                    }
                    else
                    {
                        labelStore = (LabelStore) targetLabel;
                    }
                    labelStore.setLabelName(labelName);
                    labelStore.setLocation(stLoc);
                    labelStore.setSchedule(stSched);
                    labelStore.save();

                    labelMgr.labelUpdated(labelStore);
                    break;

                case REGION:
                    //parse inputs
                    Location regLoc = parseLocation(labelInfo);
                    OperatingSchedule regSched = parseSchedule(labelInfo);

                    LabelRegion labelRegion;
                    if (typeChanged)
                    {
                        labelRegion = LabelRegion.createFrom(targetLabel);
                    }
                    else
                    {
                        labelRegion = (LabelRegion) targetLabel;
                    }

                    labelRegion.setLabelName(labelName);
                    labelRegion.setLocation(regLoc);
                    labelRegion.setSchedule(regSched);
                    labelRegion.save();

                    labelMgr.labelUpdated(labelRegion);
                    break;

                case OTHERS:
                    LabelOthers labelOthers;
                    if (typeChanged)
                    {
                        labelOthers = LabelOthers.createFrom(targetLabel);
                    }
                    else
                    {
                        labelOthers = (LabelOthers) targetLabel;
                    }
                    labelOthers.setLabelName(labelName);
                    labelOthers.save();

                    labelMgr.labelUpdated(labelOthers);
                    break;

                default:
                    throw new UnsupportedOperationException();
            }

            respondOK();

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param label-id label id. Mandatory
     *
     * @servtitle removes a label
     * @httpmethod POST
     * @uri /api/{bucket}/removelabel
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void removelabel() throws ApiException
    {
        try
        {
            long callerBucketId = Long.parseLong(getCallerBucketId());
            String labelId = readApiParameter("label-id", true);
            DeviceLabel target = labelMgr.findLabel(labelId);
            if (target == null)
            {
                //nothing to remove
                respondOK();
                return;
            }

            if (target.getBucketId() != callerBucketId)
            {
                throw new ApiException("access-denied");
            }

            LabelManager.getInstance().deleteLabel(target);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param label-id           label id. Mandatory
     * @param platform-device-id platform device id. Mandatory
     * @param channel-id         channel id. Mandatory
     *
     * @servtitle assign a label to channel of the device
     * @httpmethod POST
     * @uri /api/{bucket}/assignchannellabel
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void assignchannellabel() throws ApiException
    {
        try
        {
            String callerBucketId = getCallerBucketId();
            String callerUserId = getCallerUserId();

            String labelId = readApiParameter("label-id", true);
            String platformDeviceId = readApiParameter("platform-device-id", true);
            String channelId = readApiParameter("channel-id", true);

            //check label
            DeviceLabel targetLabel = labelMgr.findLabel(labelId);
            if (targetLabel == null)
            {
                throw new ApiException("invalid-label-id");
            }
            if (targetLabel.getBucketId() != Long.parseLong(callerBucketId))
            {
                throw new ApiException("access-denied");
            }

            //check device
            MongoDevice device = MongoDevice.getByPlatformId(platformDeviceId);
            if (device == null)
            {
                throw new ApiException("invalid-platform-device-id");
            }

            //check device access
            if (!device.getUserIds().contains(callerUserId))
            {
                throw new ApiException("access-denied");
            }

            //check camera if node
            if (device.isKaiNode())
            {
                NodeObject nodeObject = NodeObject.findByPlatformId(platformDeviceId);
                NodeCamera nodeCamera = null;
                for (NodeCamera camera : nodeObject.getCameras())
                {
                    if (camera.nodeCoreDeviceId.equals(channelId))
                    {
                        nodeCamera = camera;
                    }
                }
                if (nodeCamera == null)
                {
                    throw new ApiException("invalid-channel-id");
                }
            }

            DeviceChannelPair camera = new DeviceChannelPair(device.getCoreDeviceId(), channelId);

            //already assigned
            List<DeviceLabel> cameraLabels = labelMgr.getLabelsOf(camera);
            if (cameraLabels.contains(targetLabel))
            {
                respondOK();
            }

            //only one STORE label per camera
            if (targetLabel.getType().equals(LabelType.STORE))
            {
                for (DeviceLabel cameraLabel : cameraLabels)
                {
                    if (cameraLabel.getType().equals(LabelType.STORE))
                    {
                        throw new ApiException("error-one-store-per-camera");
                    }
                }
            }

            //assign
            targetLabel.assignCamera(camera);
            targetLabel.save();

            labelMgr.labelCameraUpdate(camera);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param label-id           label id. Mandatory
     * @param platform-device-id platform device id. Mandatory
     * @param channel-id         channel id. Mandatory
     *
     * @servtitle unassign a label to channel of the device
     * @httpmethod POST
     * @uri /api/{bucket}/unassignchannellabel
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void unassignchannellabel() throws ApiException
    {
        try
        {
            String callerBucketId = getCallerBucketId();
            String callerUserId = getCallerUserId();

            String labelId = readApiParameter("label-id", true);
            String platformDeviceId = readApiParameter("platform-device-id", true);
            String channelId = readApiParameter("channel-id", true);

            //check label
            DeviceLabel label = labelMgr.findLabel(labelId);
            if (label == null)
            {
                throw new ApiException("invalid-label-id");
            }
            if (label.getBucketId() != Long.parseLong(callerBucketId))
            {
                throw new ApiException("access-denied");
            }

            //check device
            MongoDevice device = MongoDevice.getByPlatformId(platformDeviceId);
            if (device == null)
            {
                throw new ApiException("invalid-platform-device-id");
            }

            //check device access
            if (!device.getUserIds().contains(callerUserId))
            {
                throw new ApiException("access-denied");
            }

            //check camera if node
            if (device.isKaiNode())
            {
                NodeObject nodeObject = NodeObject.findByPlatformId(platformDeviceId);
                NodeCamera nodeCamera = null;
                for (NodeCamera camera : nodeObject.getCameras())
                {
                    if (camera.nodeCoreDeviceId.equals(channelId))
                    {
                        nodeCamera = camera;
                    }
                }
                if (nodeCamera == null)
                {
                    throw new ApiException("invalid-channel-id");
                }
            }

            //unassign
            DeviceChannelPair camera = new DeviceChannelPair(device.getCoreDeviceId(), channelId);
            label.unassignCamera(camera);
            label.save();

            labelMgr.labelCameraUpdate(camera);

            //update cache
            LabelManager.getInstance().removeCachedSettings(labelId);

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    private static Map parseAsMap(String labelInfo) throws ApiException
    {
        try
        {
            Map infoMap = new Gson().fromJson(labelInfo, Map.class);
            if (infoMap == null)
            {
                throw new NullPointerException();
            }

            return infoMap;
        }
        catch (Exception e)
        {
            throw new ApiException("invalid-label-info");
        }
    }

    private static Location parseLocation(String labelInfo) throws ApiException
    {
        Map infoMap = parseAsMap(labelInfo);

        if (!infoMap.containsKey("location"))
        {
            throw new ApiException("missing-location");
        }
        try
        {
            Map locMap = (Map) infoMap.get("location");
            String address = locMap.get("address").toString();
            Double lat = Double.parseDouble(locMap.get("lat").toString());
            Double lng = Double.parseDouble(locMap.get("lng").toString());
            String timeZoneId = locMap.get("timeZoneId").toString();

            return new Location(address, lat, lng, timeZoneId);
        }
        catch (Exception e)
        {
            throw new ApiException("invalid-location");
        }
    }

    private static OperatingSchedule parseSchedule(String labelInfo) throws ApiException
    {
        Map infoMap = parseAsMap(labelInfo);
        if (!infoMap.containsKey("schedule"))
        {
            throw new ApiException("missing-schedule");
        }
        Map scheduleMap = (Map) infoMap.get("schedule");

        /**
         * holidays
         */
        if (!scheduleMap.containsKey("holidays"))
        {
            throw new ApiException("missing-holidays");
        }

        List<Integer> verifiedHolidays = new ArrayList<>();
        try
        {
            List holidayList = (List) scheduleMap.get("holidays");
            for (Object dayObj : holidayList)
            {
                Double dDay = Double.parseDouble(dayObj.toString());
                int dayInt = dDay.intValue();
                if (!TimeUtil.isValid(dayInt))
                {
                    throw new Exception();
                }
                verifiedHolidays.add(dayInt);
            }
        }
        catch (Exception e)
        {
            throw new ApiException("invalid-holidays");
        }

        /**
         * weeklyPeriods
         */
        if (!scheduleMap.containsKey("weeklyPeriods"))
        {
            throw new ApiException("missing-weeklyPeriods");
        }

        WeeklyPeriods weeklyPeriods;
        try
        {
            weeklyPeriods = new Gson().fromJson(
                    scheduleMap.get("weeklyPeriods").toString(),
                    WeeklyPeriods.class
            );
            if (weeklyPeriods == null || !weeklyPeriods.isValid())
            {
                throw new Exception();
            }
        }
        catch (Exception e)
        {
            throw new ApiException("invalid-weeklyPeriods");
        }

        //Complete schedule
        OperatingSchedule schedule = new OperatingSchedule(verifiedHolidays, weeklyPeriods);
        return schedule;
    }
}
