package controllers.api;

import controllers.interceptors.APIInterceptor;
import lib.util.ResultMap;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.mobile.MobileDevice;
import models.transportobjects.MobileDeviceTransport;
import platform.content.mobile.push.PushServiceType;
import platform.devices.MobileDeviceManager;
import play.mvc.Http;
import play.mvc.With;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author KAI Square
 * @sectiontitle Mobile Device Management
 * @sectiondesc APIs for managing mobile devices
 * @publicapi
 */
@With(APIInterceptor.class)
public class MobileController extends APIController
{

    /**
     * @param device-token    The APNS device token to register. Mandatory
     * @param device-location current location of device
     * @param device-model    name of the device
     * @param identifier      Unique identifier for devices
     *
     * @servtitle Registers a user's iOS device for push notification service
     * @httpmethod POST
     * @uri /api/{bucket}/registerapnsdevice
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void registerapnsdevice() throws ApiException
    {
        try
        {
            String currentUserId = getCallerUserId();

            String apnsToken = readApiParameter("device-token", true);
            String location = readApiParameter("device-location", false);
            String deviceModel = readApiParameter("device-model", false);
            String identifier = readApiParameter("identifier", false);

            //detect model automatically if not provided
            if (Util.isNullOrEmpty(deviceModel))
            {
                String userAgent = getUserAgent();
                if (userAgent.contains("iphone"))
                {
                    deviceModel = "iPhone";
                }
                else if (userAgent.contains("ipad"))
                {
                    deviceModel = "iPad";
                }
                else
                {
                    deviceModel = "NIL";
                }
            }

            MobileDeviceManager.getInstance().registerDevice(
                    currentUserId,
                    apnsToken,
                    deviceModel,
                    identifier,
                    PushServiceType.APNS,
                    location
            );

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-token The APNS device token to unregister. Mandatory
     *
     * @servtitle Un-Registers a user's iOS device for push notification service
     * @httpmethod POST
     * @uri /api/{bucket}/unregisterapnsdevice
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void unregisterapnsdevice() throws ApiException
    {
        try
        {
            String currentUserId = getCallerUserId();
            String apnsToken = readApiParameter("device-token", true);

            MobileDevice device = MobileDevice.findByToken(apnsToken);
            if (device == null)
            {
                throw new ApiException("invalid-device-token");
            }

            if (device.getUserId() != Long.parseLong(currentUserId))
            {
                throw new ApiException("no-access-to-mobile-device");
            }

            device.setNotificationEnabled(false);
            device.save();

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-token    The GCM device token to register. Mandatory
     * @param device-location current location of device
     * @param device-model    name of the device
     * @param identifier      Unique identifier for devices
     *
     * @servtitle Registers a user's Android device for push notification service
     * @httpmethod POST
     * @uri /api/{bucket}/registergcmdevice
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void registergcmdevice() throws ApiException
    {
        try
        {
            String currentUserId = getCallerUserId();

            String gcmToken = readApiParameter("device-token", true);
            String location = readApiParameter("device-location", false);
            String deviceModel = readApiParameter("device-model", false);
            String identifier = readApiParameter("identifier", false);

            //detect model automatically if not provided
            if (Util.isNullOrEmpty(deviceModel))
            {
                String userAgent = getUserAgent();
                if (userAgent.contains("android"))
                {
                    deviceModel = "Android";
                }
                else
                {
                    deviceModel = "NIL";
                }
            }

            MobileDeviceManager.getInstance().registerDevice(
                    currentUserId,
                    gcmToken,
                    deviceModel,
                    identifier,
                    PushServiceType.GCM,
                    location
            );

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-token The GCM device token to unregister. Mandatory
     *
     * @servtitle Un-Registers a user's Android device for push notification service
     * @httpmethod POST
     * @uri /api/{bucket}/unregistergcmdevice
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void unregistergcmdevice() throws ApiException
    {
        try
        {
            String currentUserId = getCallerUserId();
            String gcmToken = readApiParameter("device-token", true);

            MobileDevice device = MobileDevice.findByToken(gcmToken);
            if (device == null)
            {
                throw new ApiException("invalid-device-token");
            }

            if (device.getUserId() != Long.parseLong(currentUserId))
            {
                throw new ApiException("no-access-to-mobile-device");
            }

            device.setNotificationEnabled(false);
            device.save();

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Returns list of mobile devices that the current user has access to
     * @httpmethod GET
     * @uri /api/{bucket}/getusermobiledevices
     * @responsejson {
     * "result": "ok",
     * "mobileDevices": [ {@link MobileDeviceTransport} ]
     * }
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getusermobiledevices() throws ApiException
    {
        try
        {
            String currentUserId = getCallerUserId();

            List<MobileDevice> mobileDevices = MobileDevice.findByUserId(Long.parseLong(currentUserId)).asList();
            List<MobileDeviceTransport> results = new ArrayList<>();
            for (MobileDevice mobileDevice : mobileDevices)
            {
                results.add(new MobileDeviceTransport(mobileDevice));
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("mobileDevices", results);
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-id Platform side ID of the device. Mandatory
     *
     * @servtitle Remove a device from own bucket
     * @httpmethod POST
     * @uri /api/{bucket}/removedevicefrombucket
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void removemobiledeviceofuser() throws ApiException
    {
        try
        {
            String currentUserId = getCallerUserId();
            String identifier = readApiParameter("identifier", true);

            MobileDevice mobileDevice = MobileDevice.findByIdentifier(identifier);
            if (mobileDevice != null)
            {
                if (mobileDevice.getUserId() != Long.parseLong(currentUserId))
                {
                    throw new ApiException("no-access-to-mobile-device");
                }
            }

            MobileDeviceManager.getInstance().removeDevice(identifier);
            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param identifier mobile device identifier. Mandatory
     * @param new-name   new name. Mandatory
     *
     * @servtitle Remove a device from own bucket
     * @httpmethod POST
     * @uri /api/{bucket}/updatemobiledeviceinfo
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updatemobiledeviceinfo() throws ApiException
    {
        try
        {
            String currentUserId = getCallerUserId();
            String identifier = readApiParameter("identifier", true);
            String newName = readApiParameter("new-name", true);

            MobileDevice mobileDevice = MobileDevice.findByIdentifier(identifier);
            if (mobileDevice != null)
            {
                if (mobileDevice.getUserId() != Long.parseLong(currentUserId))
                {
                    throw new ApiException("no-access-to-mobile-device");
                }

                mobileDevice.setName(newName);
                mobileDevice.save();
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
