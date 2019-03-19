package controllers.api;

import controllers.interceptors.APIInterceptor;
import core.StreamControlClient;
import ext.usbdrivedetector.USBStorageDevice;
import jobs.queries.QueryRecordingUploadRequests;
import lib.util.ResultMap;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidEnvironmentException;
import models.MongoDevice;
import models.MongoUser;
import models.RecordingUploadRequest;
import models.node.NodeCamera;
import models.node.NodeObject;
import platform.Environment;
import platform.api.APITaskTracker;
import platform.api.AsyncAPITask;
import platform.api.asynctasks.ZipAndExportToUSB;
import platform.coreengine.*;
import platform.devices.DeviceChannelPair;
import platform.node.NodeManager;
import platform.system.usb.USBClient;
import platform.time.UtcPeriod;
import play.Logger;
import play.mvc.With;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author KAI Square
 * @sectiontitle Video Streaming
 * @sectiondesc APIs for accessing video streams from the system (Live, recorded as well as event videos)
 * @publicapi
 */

@With(APIInterceptor.class)
public class VideoProvisioning extends APIController
{
    private static void checkDeviceAccess(String platformDeviceId) throws ApiException
    {
        MongoUser callerUser = MongoUser.getById(getCallerUserId());
        MongoDevice device = MongoDevice.getByPlatformId(platformDeviceId);
        if (!callerUser.hasAccessToDevice(device))
        {
            throw new ApiException("access-denied");
        }
    }

    private static CoreSession convertURLtoRequestHost(CoreSession coreSession) throws Exception
    {
        if (Environment.getInstance().onKaiNode())
        {
        	List<String> convertedURLs = new ArrayList<String>();
            for (String url : coreSession.getUrlList())
            {
                String newURL = "";
                String protocol = url.substring(0, 4);
                URL tmpURL = new URL("http" + url.substring(4));
                int port = tmpURL.getPort();
                if (port > -1)
                {
                    newURL = protocol + "://" +
                          request.host + ":" + String.valueOf(tmpURL.getPort()) +
                          tmpURL.getFile();
                }
                else
                {
                    newURL = protocol + "://" +
                          request.host +
                          tmpURL.getFile();
                }
                convertedURLs.add(newURL);
            }
        	CoreSession convertedSession = new CoreSession(
        			convertedURLs,
        			coreSession.getSessionKey(),
        			coreSession.getTtlSeconds(),
        			coreSession.getClientIp());
        	return convertedSession;
        }

        return coreSession;
    }

    /**
     * @param device-id   Core side ID of the device whose stream is requested.
     *                    It is the devices[i].deviceId field from response of getuserdevices() API. Mandatory
     * @param channel-id  0 indexed channel ID for a device without the 'node' capability.
     *                    But for a device with 'node' capability, devices[i].node.cameras[j].nodeCoreDeviceId from
     *                    response of getuserdevices() API. Mandatory
     * @param stream-type One of the supported stream types: Mandatory
     *                    <ul>
     *                    <li> http/mjpeg - MJPEG stream over HTTP </li>
     *                    <li> http/jpeg - JPEG snapshot over HTTP </li>
     *                    <li> http/h264 - H.264 stream over HTTP (under development) </li>
     *                    <li> rtsp/h264 - H.264 stream over RTSP </li>
     *                    <li> rtmp/h264 - H.264 stream over RTMP </li>
     *                    </ul>
     *
     * @servtitle Returns live video URL of the specified device
     * @httpmethod POST
     * @uri /api/{bucket}/getlivevideourl
     * @responsejson {
     * "result":"ok",
     * "streaming-session-key":"8556d709-3ca9-4673-bfb9-a437308a70bb",
     * "url":["rtmp://fms.12E5.edgecastcdn.net/0012E5/mp4:videos/8Juv1MVa-485.mp4"]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getlivevideourl() throws ApiException
    {
        try
        {
            String deviceId = readApiParameter("device-id", true);
            String channelId = readApiParameter("channel-id", true);
            StreamType streamType = StreamType.parse(readApiParameter("stream-type", true));
            String ttl = readApiParameter("ttl-seconds", false);

            //Verify device ID
            MongoDevice device = MongoDevice.getByCoreId(deviceId);
            if (device == null)
            {
                throw new ApiException("invalid-device-id");
            }

            //ttl
            int ttlSeconds = 0;
            if (!Util.isNullOrEmpty(ttl))
            {
                if (!Util.isInteger(ttl))
                {
                    throw new ApiException("invalid-ttl-seconds");
                }
                ttlSeconds = Integer.parseInt(ttl);
            }

            //check device permission
            String currentUserId = getCallerUserId();
            if (!device.getUserIds().contains(currentUserId))
            {
                throw new ApiException("permission-denied");
            }

            //params
            DeviceChannelPair camera = new DeviceChannelPair(deviceId, channelId);
            String clientIp = request.remoteAddress;
            CoreSession coreSession;
            if (streamType.equals(StreamType.HTTP_JPEG))
            {
                coreSession = StreamingManager.getInstance().getCameraSnapshot(camera, ttlSeconds, clientIp);
            }
            else
            {
                coreSession = StreamingManager.getInstance().startLiveStreamSession(camera, streamType, ttlSeconds, clientIp);
            }

            coreSession = convertURLtoRequestHost(coreSession);
            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("url", coreSession.getUrlList());
            map.put("streaming-session-key", coreSession.getSessionKey());
            map.put("ttl-seconds", coreSession.getTtlSeconds());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param streaming-session-key The stream's session key.
     *
     * @servtitle To keep a live stream alive, call this API before it times out.
     * @httpmethod POST
     * @uri /api/{bucket}/keepalivelivevideourl
     * @responsejson {
     * "result":"ok",
     * "status": true
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void keepalivelivevideourl() throws ApiException
    {
        try
        {
            String streamingSessionKey = readApiParameter("streaming-session-key", true);

            boolean status = StreamingManager.getInstance().keepSessionAlive(streamingSessionKey);
            respondOK("status", status);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param streaming-session-key The stream's session key.
     *
     * @servtitle Force expire a live stream immediately
     * @httpmethod POST
     * @uri /api/{bucket}/expirelivevideourl
     * @responsejson {
     * "result":"ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void expirelivevideourl() throws ApiException
    {
        try
        {
            String streamSessionKey = readApiParameter("streaming-session-key", true);

            StreamingManager.getInstance().expireSession(streamSessionKey);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-id   Core side ID of the device whose stream is requested.
     *                    It is the devices[i].deviceId field from response of getuserdevices() API. Mandatory
     * @param channel-id  0 indexed channel ID for a device without the 'node' capability.
     *                    But for a device with 'node' capability, devices[i].node.cameras[j].nodeCoreDeviceId from
     *                    response of getuserdevices() API. Mandatory
     * @param stream-type One of the supported stream types: Mandatory
     *                    <ul>
     *                    <li> http/mjpeg - MJPEG stream over HTTP </li>
     *                    <li> http/jpeg - JPEG snapshot over HTTP </li>
     *                    <li> http/h264 - H.264 stream over HTTP (under development) </li>
     *                    <li> rtsp/h264 - H.264 stream over RTSP </li>
     *                    <li> rtmp/h264 - H.264 stream over RTMP </li>
     *                    </ul>
     * @param from        Start of search time span, in ddMMyyyyHHmmss format. Mandatory
     * @param to          End of search time span, in ddMMyyyyHHmmss format. Mandatory
     *
     * @servtitle Returns a list of recorded videos of the specified device,
     * for the specified period, as stream URLs.
     * @httpmethod POST
     * @uri /api/{bucket}/getplaybackvideourl
     * @responsejson {
     * "result":"ok",
     * "streaming-session-key":"8556d709-3ca9-4673-bfb9-a437308a70bb",
     * "url":["rtmp://fms.12E5.edgecastcdn.net/0012E5/mp4:videos/8Juv1MVa-485.mp4",
     * "rtmp://fms.12E5.edgecastcdn.net/0012E5/mp4:videos/8Juv1MVa-486.mp4"]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getplaybackvideourl() throws ApiException
    {
        try
        {
            String deviceId = readApiParameter("device-id", true);
            String channelId = readApiParameter("channel-id", true);
            StreamType streamType = StreamType.parse(readApiParameter("stream-type", true));
            long from = toMilliseconds(readApiParameter("from", true));
            long to = toMilliseconds(readApiParameter("to", true));
            String ttl = readApiParameter("ttl-seconds", false);

            //ttl
            int ttlSeconds = 0;
            if (!Util.isNullOrEmpty(ttl))
            {
                if (!Util.isInteger(ttl))
                {
                    throw new ApiException("invalid-ttl-seconds");
                }
                ttlSeconds = Integer.parseInt(ttl);
            }

            //Verify device ID
            MongoDevice device = MongoDevice.getByPlatformId(deviceId);
            if (device == null)
            {
                throw new ApiException("invalid-device-id");
            }

            //check device access
            String currentUserId = getCallerUserId();
            MongoUser currentUser = MongoUser.getById(currentUserId);
            if (!currentUser.hasAccessToDevice(device))
            {
                throw new ApiException("access-denied");
            }

            //params
            DeviceChannelPair camera = new DeviceChannelPair(device.getCoreDeviceId(), channelId);
            UtcPeriod period = new UtcPeriod(from, to);
            String clientIp = request.remoteAddress;
            CoreSession coreSession = StreamingManager.getInstance().startPlaybackStreamSession(camera, period, streamType, ttlSeconds, clientIp);
            coreSession = convertURLtoRequestHost(coreSession);
            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("streaming-session-key", coreSession.getSessionKey());
            map.put("urls", coreSession.getUrlList());
            map.put("ttl-seconds", coreSession.getTtlSeconds());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param streaming-session-key The stream's session key. Mandatory
     *
     * @servtitle To keep a playback stream alive, call this API before it times out.
     * for the specified period, as stream URLs.
     * @httpmethod POST
     * @uri /api/{bucket}/keepaliveplaybackvideourl
     * @responsejson {
     * "result":"ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void keepaliveplaybackvideourl() throws ApiException
    {
        try
        {
            String streamingSessionKey = readApiParameter("streaming-session-key", true);

            StreamingManager.getInstance().keepSessionAlive(streamingSessionKey);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param streaming-session-key The stream's session key. Mandatory
     *
     * @servtitle Force expire a playback stream immediately
     * @httpmethod POST
     * @uri /api/{bucket}/expireplaybackvideourl
     * @responsejson {
     * "result":"ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void expireplaybackvideourl() throws ApiException
    {
        try
        {
            String streamSessionKey = readApiParameter("streaming-session-key", true);

            StreamingManager.getInstance().expireSession(streamSessionKey);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-id  Node platform ID on cloud
     * @param channel-id Camera core Device id on node
     * @param from       start time (ddMMyyyyHHmmss)
     * @param to         end time (ddMMyyyyHHmmss)
     *
     * @servtitle Search node recordings on cloud
     * @httpmethod POST
     * @uri /api/{bucket}/searchcloudrecordings
     * @responsejson {
     * "result" : "ok"
     * "files" : []
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void searchcloudrecordings() throws ApiException
    {
        try
        {
            String deviceId = readApiParameter("device-id", true);
            String channelId = readApiParameter("channel-id", true);
            long startDt = toMilliseconds(readApiParameter("from", true));
            long endDt = toMilliseconds(readApiParameter("to", true));

            //check access
            checkDeviceAccess(deviceId);

            //verify node and camera
            NodeObject nodeObject = NodeObject.findByPlatformId(deviceId);
            NodeCamera nodeCamera = null;
            for (NodeCamera nc : nodeObject.getCameras())
            {
                if (nc.nodeCoreDeviceId.equals(channelId))
                {
                    nodeCamera = nc;
                }
            }
            if (nodeCamera == null)
            {
                throw new ApiException("invalid-camera-core-id");
            }

            //create request parameters
            DeviceChannelPair camera = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), nodeCamera.nodeCoreDeviceId);
            UtcPeriod period = new UtcPeriod(startDt, endDt);

            //search
            List<UploadedRecordingFile> fileList = RecordingManager.getInstance().searchRecordingsOnCloud(camera, period);

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("files", fileList);
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-id  Node platform ID on cloud
     * @param channel-id Camera core Device id on node
     * @param from       start time (ddMMyyyyHHmmss)
     * @param to         end time (ddMMyyyyHHmmss)
     *
     * @servtitle request node recordings to be uploaded to cloud
     * @httpmethod POST
     * @uri /api/{bucket}/requestcloudrecordings
     * @responsejson {
     * "result" : "ok"
     * "files" : []
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void requestcloudrecordings() throws ApiException
    {
        try
        {
            String deviceId = readApiParameter("device-id", true);
            String channelId = readApiParameter("channel-id", true);
            long startDt = toMilliseconds(readApiParameter("from", true));
            long endDt = toMilliseconds(readApiParameter("to", true));

            //check access
            checkDeviceAccess(deviceId);

            //verify node and camera
            NodeObject nodeObject = NodeObject.findByPlatformId(deviceId);
            NodeCamera nodeCamera = null;
            for (NodeCamera nc : nodeObject.getCameras())
            {
                if (nc.nodeCoreDeviceId.equals(channelId))
                {
                    nodeCamera = nc;
                }
            }
            if (nodeCamera == null)
            {
                throw new ApiException("invalid-camera-core-id");
            }

            //create request parameters
            DeviceChannelPair camera = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), nodeCamera.nodeCoreDeviceId);
            UtcPeriod period = new UtcPeriod(startDt, endDt);
            String callerUserId = getCallerUserId();

            //send
            RecordingManager.getInstance().sendVideoUploadRequest(camera, period, callerUserId);

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-id  Node platform ID on cloud
     * @param channel-id Camera core Device id on node
     * @param from       start time (ddMMyyyyHHmmss)
     * @param to         end time (ddMMyyyyHHmmss)
     *
     * @servtitle find a list of pending upload requests
     * @httpmethod POST
     * @uri /api/{bucket}/findpendinguploadrequests
     * @responsejson {
     * "result" : "ok"
     * "files" : []
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void findpendinguploadrequests() throws ApiException
    {
        try
        {
            String deviceId = readApiParameter("device-id", true);
            String channelId = readApiParameter("channel-id", true);
            long startDt = toMilliseconds(readApiParameter("from", true));
            long endDt = toMilliseconds(readApiParameter("to", true));

            //check access
            checkDeviceAccess(deviceId);

            //verify node and camera
            NodeObject nodeObject = NodeObject.findByPlatformId(deviceId);
            NodeCamera nodeCamera = null;
            for (NodeCamera nc : nodeObject.getCameras())
            {
                if (nc.nodeCoreDeviceId.equals(channelId))
                {
                    nodeCamera = nc;
                }
            }
            if (nodeCamera == null)
            {
                throw new ApiException("invalid-camera-core-id");
            }

            //create request parameters
            DeviceChannelPair camera = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), nodeCamera.nodeCoreDeviceId);
            UtcPeriod period = new UtcPeriod(startDt, endDt);

            //find and filter out empty requests
            List<RecordingUploadRequest> filteredList = new ArrayList<>();
            List<RecordingUploadRequest> iterable = RecordingUploadRequest.find(camera, period).fetchAll();
            for (RecordingUploadRequest request : iterable)
            {
                if (request.isEffectivelyEmpty())
                {
                    request.delete();
                    Logger.info("Deleted 'empty' recording request (%s)", request);
                    continue;
                }
                filteredList.add(request);
            }

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("requests", filteredList);
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-id  Node platform ID on cloud
     * @param channel-id Camera core Device id on node
     * @param from       start time (ddMMyyyyHHmmss)
     * @param to         end time (ddMMyyyyHHmmss)
     *
     * @servtitle request node recordings to be uploaded to cloud
     * @httpmethod POST
     * @uri /api/{bucket}/deletecloudrecordings
     * @responsejson {
     * "result" : "ok"
     * "files" : []
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void deletecloudrecordings() throws ApiException
    {
        try
        {
            String deviceId = readApiParameter("device-id", true);
            String channelId = readApiParameter("channel-id", true);
            long startDt = toMilliseconds(readApiParameter("from", true));
            long endDt = toMilliseconds(readApiParameter("to", true));

            //check access
            checkDeviceAccess(deviceId);

            //verify node and camera
            NodeObject nodeObject = NodeObject.findByPlatformId(deviceId);
            NodeCamera nodeCamera = null;
            for (NodeCamera nc : nodeObject.getCameras())
            {
                if (nc.nodeCoreDeviceId.equals(channelId))
                {
                    nodeCamera = nc;
                }
            }
            if (nodeCamera == null)
            {
                throw new ApiException("invalid-camera-core-id");
            }

            //create request parameters
            DeviceChannelPair camera = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), nodeCamera.nodeCoreDeviceId);
            UtcPeriod period = new UtcPeriod(startDt, endDt);

            //send
            RecordingManager.getInstance().deleteCloudRecordings(camera, period);

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-id  Node platform ID on cloud
     * @param channel-id Camera core Device id on node
     *
     * @servtitle request node recordings to be uploaded to cloud
     * @httpmethod POST
     * @uri /api/{bucket}/getrecordinguploadrequests
     * @responsejson {
     * "result" : "ok"
     * "requests" : []
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getrecordinguploadrequests() throws ApiException
    {
        try
        {
            String deviceId = readApiParameter("device-id", true);
            String channelId = readApiParameter("channel-id", true);

            //check access
            checkDeviceAccess(deviceId);

            //verify node and camera
            NodeObject nodeObject = NodeObject.findByPlatformId(deviceId);
            NodeCamera nodeCamera = null;
            for (NodeCamera nc : nodeObject.getCameras())
            {
                if (nc.nodeCoreDeviceId.equals(channelId))
                {
                    nodeCamera = nc;
                }
            }
            if (nodeCamera == null)
            {
                throw new ApiException("invalid-camera-core-id");
            }

            //create request parameters
            DeviceChannelPair camera = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), nodeCamera.nodeCoreDeviceId);

            //query
            QueryRecordingUploadRequests queryJob = new QueryRecordingUploadRequests(camera, null);
            respondOK("requests", await(queryJob.now()));
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-id  Node platform ID on cloud
     * @param channel-id Camera core Device id on node
     * @param from       start time (ddMMyyyyHHmmss)
     * @param to         end time (ddMMyyyyHHmmss)
     *
     * @servtitle get a list of recorded local files (available on Nodes only)
     * @httpmethod POST
     * @uri /api/{bucket}/getrecordedfilelist
     * @responsejson {
     * "result" : "ok"
     * "files" : [
     * {
     * "startTime" : milliseconds,
     * "endTime" : milliseconds,
     * "fileSize" : number of bytes,
     * "status" : COMPLETED or MISSING,
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getrecordedfilelist() throws ApiException
    {
        try
        {
            String deviceId = readApiParameter("device-id", true);
            String channelId = readApiParameter("channel-id", true);
            long startDt = toMilliseconds(readApiParameter("from", true));
            long endDt = toMilliseconds(readApiParameter("to", true));

            //verify device
            MongoDevice device = MongoDevice.getByPlatformId(deviceId);
            if (device == null)
            {
                throw new ApiException("invalid-device-id");
            }

            //create request parameters
            UtcPeriod period = new UtcPeriod(startDt, endDt);

            //search
            List<RecordedLocalFile> localFileList = StreamControlClient.getInstance().getRecordedLocalFiles(device.getCoreDeviceId(), channelId, period);

            List<Map> outputList = new ArrayList<>();
            for (RecordedLocalFile localFile : localFileList)
            {
                outputList.add(localFile.toAPIObject());
            }

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("files", outputList);
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-id  Node platform ID on cloud
     * @param channel-id Camera core Device id on node
     * @param from       start time (ddMMyyyyHHmmss)
     * @param to         end time (ddMMyyyyHHmmss)
     *
     * @servtitle zip and download a list of recorded local files (available on Nodes only)
     * @httpmethod POST
     * @uri /api/{bucket}/downloadzippedrecordings
     * @responsejson zipped file
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void downloadzippedrecordings() throws ApiException
    {
        try
        {
            if (!Environment.getInstance().onKaiNode())
            {
                throw new InvalidEnvironmentException();
            }

            String deviceId = readApiParameter("device-id", true);
            String channelId = readApiParameter("channel-id", true);
            long from = toMilliseconds(readApiParameter("from", true));
            long to = toMilliseconds(readApiParameter("to", true));

            //verify device
            MongoDevice device = MongoDevice.getByPlatformId(deviceId);
            if (device == null)
            {
                throw new ApiException("invalid-device-id");
            }

            //search
            UtcPeriod period = new UtcPeriod(from, to);
            List<RecordedLocalFile> localFileList = StreamControlClient.getInstance().getRecordedLocalFiles(device.getCoreDeviceId(), channelId, period);

            if (localFileList.isEmpty())
            {
                throw new ApiException("no-recorded-files-found");
            }

            //zip
            File zipFile = await(RecordingManager.getInstance().zipRecordings(localFileList));
            String downloadFilename = NodeManager.buildSearchTimestamp(device.getName(), from, to);
            renderBinary(zipFile, downloadFilename);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param device-id      Node platform ID on cloud
     * @param channel-id     Camera core Device id on node
     * @param from           start time (ddMMyyyyHHmmss)
     * @param to             end time (ddMMyyyyHHmmss)
     * @param usb-identifier usb identifier returned from getusbdrives API
     *
     * @servtitle export recordings to an external USB drive
     * @httpmethod POST
     * @uri /api/{bucket}/usbexportrecordings
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void usbexportrecordings() throws ApiException
    {
        try
        {
            String deviceId = readApiParameter("device-id", true);
            String channelId = readApiParameter("channel-id", true);
            long from = toMilliseconds(readApiParameter("from", true));
            long to = toMilliseconds(readApiParameter("to", true));
            String usbIdentifier = readApiParameter("usb-identifier", true);

            //verify USB
            USBStorageDevice usbDrive = USBClient.getInstance().find(usbIdentifier);
            if (usbDrive == null)
            {
                throw new ApiException("invalid-usb-identifier");
            }

            //verify device
            MongoDevice device = MongoDevice.getByPlatformId(deviceId);
            if (device == null)
            {
                throw new ApiException("invalid-device-id");
            }

            //search
            UtcPeriod period = new UtcPeriod(from, to);
            List<RecordedLocalFile> recordingList = StreamControlClient.getInstance().getRecordedLocalFiles(device.getCoreDeviceId(), channelId, period);

            if (recordingList.isEmpty())
            {
                throw new ApiException("no-recorded-files-found");
            }

            //verify files
            List<File> localFileList = new ArrayList<>();
            for (RecordedLocalFile recording : recordingList)
            {
                if (recording.available())
                {
                    localFileList.add(new File(recording.getPath()));
                }
            }

            //run task
            String outputFilename = NodeManager.buildSearchTimestamp(device.getName(), from, to);
            String callerUserId = getCallerUserId();
            AsyncAPITask exportTask = new ZipAndExportToUSB(Long.parseLong(callerUserId), request.actionMethod, localFileList, usbDrive, outputFilename);
            APITaskTracker.getInstance().startTaskWithListener(exportTask);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
