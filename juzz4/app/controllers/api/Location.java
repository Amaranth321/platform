package controllers.api;

import controllers.interceptors.APIInterceptor;
import lib.util.ResultMap;
import lib.util.exceptions.ApiException;
import models.MongoDevice;
import models.Poi;
import models.transients.LocationPoint;
import platform.LocationManager;
import play.Logger;
import play.mvc.With;

import java.util.List;
import java.util.Map;

import static lib.util.Util.isNullOrEmpty;

/**
 * @author KAI Square
 *         publicapi (hidden from API documentation)
 * @sectiontitle Location Tracking (FMS)
 * @sectiondesc APIs to retrieve live and historical location data and related features.
 */

@With(APIInterceptor.class)
public class Location extends APIController
{
    /**
     * @param device-id The id of device whose location is to be tracked. Mandatory
     *
     * @servtitle Returns current location of a device.
     * @httpmethod POST
     * @uri /api/{bucket}/getlivelocation
     * @responsejson {
     * "result": "ok",
     * "location": [
     * {
     * "latitude": 56.229379823,
     * "longitude": 45.29374892379,
     * "timestamp": 234823648723
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void getlivelocation(String bucket) throws ApiException
    {
        try
        {
            String currentUserId = getCallerUserId();
            String currentBucketId = getCallerBucketId();

            //validate input parameters
            Map<String, String[]> input = params.all();
            String[] deviceId = input.get("device-id");
            if (deviceId == null || deviceId[0].isEmpty())
            {
                throw new ApiException("device-id-missing");
            }

            //Verify device ID
            MongoDevice device = MongoDevice.getByPlatformId(deviceId[0]);
            if (device == null)
            {
                throw new ApiException("invalid-device-id");
            }
            //verify device ownership
            if (!currentBucketId.equals(device.getBucketId()))
            {
                throw new ApiException("invalid-device-id");
            }
            //check permission
            if (!device.getUserIds().contains(currentUserId))
            {
                throw new ApiException("permission-denied");
            }

            Map map = new ResultMap();
            LocationPoint loc = LocationManager.getInstance().getLiveLocation(device);
            if (loc != null)
            {
                map.put("result", "ok");
                map.put("location", loc);
            }
            else
            {
                map.put("result", "error");
                map.put("result", "unavailable");
            }
            renderJSON(map);

        }
        catch (ApiException apie)
        {
            Logger.warn(apie.getMessage());
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", apie.getMessage());
            renderJSON(map);
        }
        catch (Exception exp)
        {
            Logger.warn(exp, "Exception");
            Logger.error(lib.util.Util.getStackTraceString(exp));
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);
        }
    }

    /**
     * @servtitle Returns list of all point of interest of the current bucket
     * @httpmethod GET
     * @uri /api/{bucket}/getbucketpois
     * @responsejson {
     * "result": "ok",
     * "pois": [
     * {
     * "name": "sites",
     * "type": "bus station",
     * "description": "near station from my home",
     * "address": "pulchowk",
     * "latitude": 23.23232323,
     * "longitude": 56.23423423
     * },
     * {
     * "name": "sites",
     * "type": "LandMark",
     * "description": "nearest landmark",
     * "address": "river side",
     * "latitude": 24.23232323,
     * "longitude": 56.23423423
     * },
     * {
     * "name": "sites",
     * "type": "Wifi",
     * "description": "free wifi hurry",
     * "address": "lake side cafe",
     * "latitude": 23.23232323,
     * "longitude": 56.23423423
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void getbucketpois(String bucket) throws ApiException
    {
        try
        {
            String callerBucketId = getCallerBucketId();

            //get list of all poi in the bucket
            List<Poi> poiList = models.Poi.find("bucketId", Long.parseLong(callerBucketId)).asList();

            //return result
            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("pois", poiList);
            renderJSON(map);
        }
        catch (Exception exp)
        {
            Logger.warn(exp, "Exception");
            Logger.error(lib.util.Util.getStackTraceString(exp));
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);
        }
    }

    /**
     * @param name        Name of poi. Mandatory
     * @param type        Type of poi eg. landmark,wifi zone. Mandatory
     * @param description Description of poi
     * @param address     Address of poi eg. Nepal
     * @param latitude    Latitude of poi eg. 1.3873645. Mandatory
     * @param longitude   longitude of poi eg. 103.456328. Mandatory
     *
     * @servtitle Add a new point of interest to the current bucket
     * @httpmethod POST
     * @uri /api/{bucket}/addpoi
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void addpoi(String bucket) throws ApiException
    {
        try
        {
            Map<String, String[]> input = params.all();
            String name = input.get("name") == null ? "" : input.get("name")[0].trim();
            String type = input.get("type") == null ? "" : input.get("type")[0];
            String description = input.get("description") == null ? "" : input.get("description")[0];
            String address = input.get("address") == null ? "" : input.get("address")[0];
            String latitude = input.get("latitude") == null ? "" : input.get("latitude")[0];
            String longitude = input.get("longitude") == null ? "" : input.get("longitude")[0];

            if (isNullOrEmpty(name))
            {
                throw new ApiException("name-missing");
            }

            if (isNullOrEmpty(type))
            {
                throw new ApiException("type-missing");
            }

            if (isNullOrEmpty(latitude))
            {
                throw new ApiException("latitude-missing");
            }

            if (isNullOrEmpty(longitude))
            {
                throw new ApiException("longitude-missing");
            }

            Poi newPoi = new Poi();
            newPoi.name = name;
            newPoi.type = type;
            newPoi.description = description;
            newPoi.address = address;
            newPoi.latitude = latitude;
            newPoi.longitude = longitude;
            newPoi.bucketId = Long.parseLong(getCallerBucketId());
            newPoi.save();

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (ApiException apie)
        {
            Logger.warn(apie.getMessage());
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", apie.getMessage());
            renderJSON(map);
        }
        catch (Exception exp)
        {
            Logger.warn(exp, "Exception");
            Logger.error(lib.util.Util.getStackTraceString(exp));
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);
        }
    }

    /**
     * @param id          Id of poi. Mandatory
     * @param name        Name of poi. Mandatory
     * @param type        Type of poi eg. landmark,wifi zone. Mandatory
     * @param description Description of poi
     * @param address     Address of poi eg. Nepal
     * @param latitude    Latitude of poi eg. 1.3873645. Mandatory
     * @param longitude   Longitude of poi eg. 103.456328. Mandatory
     *
     * @servtitle Update a existing poi details of the current bucket
     * @httpmethod POST
     * @uri /api/{bucket}/updatepoi
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void updatepoi(String bucket) throws ApiException
    {

        try
        {
            Map<String, String[]> input = params.all();
            String id = input.get("id") == null ? "" : input.get("id")[0];
            String name = input.get("name") == null ? "" : input.get("name")[0].trim();
            String type = input.get("type") == null ? "" : input.get("type")[0];
            String description = input.get("description") == null ? "" : input.get("description")[0];
            String address = input.get("address") == null ? "" : input.get("address")[0];
            String latitude = input.get("latitude") == null ? "" : input.get("latitude")[0];
            String longitude = input.get("longitude") == null ? "" : input.get("longitude")[0];

            if (isNullOrEmpty(id))
            {
                throw new ApiException("id-missing");
            }

            if (isNullOrEmpty(name))
            {
                throw new ApiException("name-missing");
            }

            if (isNullOrEmpty(type))
            {
                throw new ApiException("type-missing");
            }

            if (isNullOrEmpty(latitude))
            {
                throw new ApiException("latitude-missing");
            }

            if (isNullOrEmpty(longitude))
            {
                throw new ApiException("longitude-missing");
            }

            Poi targetPoi = Poi.findById(id);
            if (targetPoi == null)
            {
                throw new ApiException("invalid-poi-id");
            }

            String currentBucketId = renderArgs.get("caller-bucket-id").toString();
            if (!currentBucketId.equals(targetPoi.bucketId.toString()))
            {
                throw new ApiException("Unauthorized");
            }

            targetPoi.name = name;
            targetPoi.type = type;
            targetPoi.description = description;
            targetPoi.address = address;
            targetPoi.latitude = latitude;
            targetPoi.longitude = longitude;
            targetPoi.save();

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);

        }
        catch (ApiException apie)
        {
            Logger.warn(apie.getMessage());
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", apie.getMessage());
            renderJSON(map);
        }
        catch (Exception exp)
        {
            Logger.warn(exp, "Exception");
            Logger.error(lib.util.Util.getStackTraceString(exp));
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);
        }
    }

    /**
     * @param poi-id Id of poi to remove. Mandatory
     *
     * @servtitle Remove a poi from current bucket
     * @httpmethod POST
     * @uri /api/{bucket}/removepoi
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void removepoi(String bucket) throws ApiException
    {
        try
        {
            Map<String, String[]> input = params.all();
            String poiId = input.get("poi-id") == null ? "" : input.get("poi-id")[0];

            if (isNullOrEmpty(poiId))
            {
                throw new ApiException("poi-id-missing");
            }

            Poi targetPoi = Poi.findById(poiId);
            if (targetPoi == null)
            {
                throw new ApiException("invalid-poi-id");
            }

            String currentBucketId = getCallerBucketId();
            if (!currentBucketId.equals(targetPoi.bucketId.toString()))
            {
                throw new ApiException("Unauthorized");
            }

            targetPoi.delete();

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (ApiException apie)
        {
            Logger.warn(apie.getMessage());
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", apie.getMessage());
            renderJSON(map);
        }
        catch (Exception exp)
        {
            Logger.warn(exp, "Exception");
            Logger.error(lib.util.Util.getStackTraceString(exp));
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);
        }
    }
}
