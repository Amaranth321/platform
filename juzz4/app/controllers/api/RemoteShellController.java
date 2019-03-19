package controllers.api;

import controllers.interceptors.APIInterceptor;
import lib.util.ResultMap;
import lib.util.exceptions.ApiException;
import models.MongoBucket;
import models.MongoDevice;
import models.RemoteShellState;
import models.RemoteShellState.ConnectionState;
import models.transients.RemoteShellDevice;
import platform.DeviceManager;
import platform.RemoteShellManager;
import play.mvc.With;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author KAI Square
 *         publicapi (hidden from API documentation)
 * @sectiontitle Remote Troubleshooting
 * @sectiondesc APIs to start and stop remote shell sessions
 */

@With(APIInterceptor.class)
public class RemoteShellController extends APIController
{

    /**
     * @param bucket   e.g. kaisquare, passed intrinsically as part of URL
     * @param deviceId ID of the node
     * @param host     Hostname of server to connect to, e.g. support.i.kaisquare.com
     * @param port     Port number, e.g. 30005
     *
     * @servtitle Instruct a KAI Node to start remote shell
     * @httpmethod POST
     * @uri /api/{bucket}/startremoteshell
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "invalid-parameters"
     * }
     */

    public static void startremoteshell(String bucket) throws ApiException
    {
        try
        {

            String deviceId = readApiParameter("device-id", false);
            String host = readApiParameter("host", false);
            String port = readApiParameter("port", false);
            String username = readApiParameter("user-name", true);

            int shellPort = Integer.parseInt(port);

            RemoteShellManager.getInstance().startRemoteShell(deviceId, host, shellPort, username);

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
     * @param bucket   e.g. kaisquare, passed intrinsically as part of URL
     * @param deviceId ID of the node
     *
     * @servtitle Instruct a KAI Node to stop remote shell
     * @httpmethod POST
     * @uri /api/{bucket}/stopremoteshell
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "invalid-parameters"
     * }
     */

    public static void stopremoteshell(String bucket) throws ApiException
    {
        try
        {
            String deviceId = readApiParameter("device-id", true);

            RemoteShellManager.getInstance().stopRemoteShell(deviceId);

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
     * @param bucket    e.g. kaisquare, passed intrinsically as part of URL
     * @param bucket-id bucket id user selected on web page
     *
     * @servtitle Returns remote shell list
     * @httpmethod POST
     * @uri /api/{bucket}/getremoteshelllist
     * @responsejson {
     * "result": "ok"
     * "devices": [
     * {
     * "nodeId":"1",
     * "name": "Nepal office",
     * "bucketId": "alucio",
     * "address": "lalitpur, Kathmandu, Nepal",
     * "host": "localhost"
     * "port": "9999",
     * "userName": "Nischal",
     * "open": true
     * },
     * {
     * "nodeId":"100",
     * "name": "Taiwan office",
     * "bucketId": "kaisquare",
     * "address": "Taiwan",
     * "host": "localhost"
     * "port": "8888",
     * "userName": "Kapil",
     * "open": false
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "UNKNOWN"
     * }
     */

    public static void getremoteshelllist(String bucket)
    {
        ResultMap map = new ResultMap();
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("Invalid bucket ID");
            }

            List<MongoDevice> devices = DeviceManager.getInstance().getDevicesOfBucket(bucketId);
            List<RemoteShellDevice> shellDevices = new ArrayList<>();

            if (devices != null)
            {
                RemoteShellManager manager = RemoteShellManager.getInstance();
                for (MongoDevice device : devices)
                {
                    if (device.isKaiNode())
                    {
                        RemoteShellDevice shellDevice = new RemoteShellDevice();
                        shellDevice.status = device.getStatus().toString();
                        shellDevice.nodeId = Long.parseLong(device.getDeviceId());
                        shellDevice.name = device.getName();
                        shellDevice.bucketName = targetBucket.getName();
                        shellDevice.address = device.getAddress();
                        shellDevice.connectionState = ConnectionState.NODE_DISCONNECTED;
                        try
                        {
                            RemoteShellState state = manager.getRemoteShellState(device.getDeviceId());
                            if (state != null)
                            {
                                shellDevice.host = state.host;
                                shellDevice.port = state.port;
                                shellDevice.username = state.username;
                                shellDevice.connectionState = state.connectionState;
                            }
                        }
                        catch (ApiException e)
                        {
                        }
                        finally
                        {
                            shellDevices.add(shellDevice);
                        }
                    }
                }
            }

            map.put("result", "ok");
            map.put("devices", shellDevices);

            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
