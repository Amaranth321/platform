package controllers.api;

import com.kaisquare.kaisync.ISyncReadFile;
import com.kaisquare.kaisync.file.FileOptions;
import com.kaisquare.kaisync.file.IFileClient;
import controllers.interceptors.APIInterceptor;
import lib.util.ResultMap;
import lib.util.exceptions.ApiException;
import models.MongoBucket;
import models.MongoDevice;
import models.NodeLogFile;
import models.transients.NodeLogDevice;
import platform.CloudActionMonitor;
import platform.CloudSyncManager;
import platform.DeviceManager;
import play.mvc.With;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author KAI Square
 *         publicapi (hidden from API documentation)
 * @sectiontitle Node Logs
 * @sectiondesc APIs to remotely access log files of KAI Nodes
 */

@With(APIInterceptor.class)
public class NodeSupportController extends APIController
{

    /**
     * @param node-id The node id that pull log from. Mandatory
     *
     * @servtitle Start pulling log file from specified node
     * @httpmethod POST
     * @uri /api/{bucket}/pullnodelog
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "failed",
     * "reason": "unable to pull log from node"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void pullnodelog(String bucket)
    {
        try
        {
            String nodeId = readApiParameter("node-id", true);

            //create record to show loading
            NodeLogFile logFile = NodeLogFile.find("nodeId", nodeId).first();
            if (logFile == null)
            {
                logFile = new NodeLogFile(nodeId);
            }
            logFile.status = NodeLogFile.PullingStatus.Pulling;
            logFile.save();

            //send
            CloudActionMonitor.getInstance().pullLogFromNode(nodeId);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id bucket id user selected on web page
     *
     * @servtitle Start pulling log file from specified node
     * @httpmethod POST
     * @uri /api/{bucket}/getnodeloglist
     * @responsejson {
     * "result": "ok",
     * "devices":[
     * {
     * "nodeId": 525e59a3e4b0db2213gd7a3c,
     * "name":"Nepal office",
     * "bucketName":"KFC",
     * "senderName":"Nissal",
     * "status":"running",
     * "address":"Nepal"
     * "availableLog": "g mathina cha",
     * "filename": "file1",
     * "pullingStatus": "Pulling"
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void getnodeloglist(String bucket)
    {
        ResultMap map = new ResultMap();
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            MongoBucket b = MongoBucket.getById(bucketId);
            if (b == null)
            {
                throw new ApiException("Invalid bucket ID");
            }

            List<NodeLogDevice> logList = new ArrayList<NodeLogDevice>();
            List<MongoDevice> devices = DeviceManager.getInstance().getDevicesOfBucket(bucketId);
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            if (devices != null)
            {
                for (MongoDevice device : devices)
                {
                    if (device.isKaiNode())
                    {
                        NodeLogDevice logDevice = new NodeLogDevice();
                        logDevice.status = device.getStatus().toString();
                        logDevice.nodeId = device.getDeviceId();
                        logDevice.bucketName = b.getName();
                        logDevice.name = device.getName();
                        logDevice.address = device.getAddress();
                        NodeLogFile logFile = NodeLogFile.find("nodeId", device.getDeviceId()).first();
                        if (logFile != null)
                        {
                            logDevice.filename = logFile.filename;
                            if (logFile.uploadedDate != null)
                            {
                                logDevice.availableLog = format.format(logFile.uploadedDate);
                            }
                            logDevice.pullingStatus = logFile.status;
                        }
                        else
                        {
                            logDevice.availableLog = "None";
                            logDevice.pullingStatus = NodeLogFile.PullingStatus.Standby;
                            logDevice.filename = "";
                        }
                        logList.add(logDevice);
                    }
                }
            }

            map.put("result", "ok");
            map.put("devices", logList);

        }
        catch (Exception e)
        {
            respondError(e);
        }

        renderJSON(map);
    }

    /**
     * @param filename The specific file to download e.g file1. Mandatory
     *
     * @servtitle Download log file of specified node.
     * @httpmethod POST
     * @uri /api/{bucket}/getnodeloglist
     * @responsebinary {
     * <i>Renders Binary file of node</i>
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void downloadnodelogfile(String filename)
    {
        try
        {
            if (filename == null || filename.isEmpty())
            {
                throw new ApiException("Invalid filename");
            }

            IFileClient client = CloudSyncManager.getInstance().newFileClient();
            String openFilename = new String(filename.substring(0, filename.indexOf(".")));
            ISyncReadFile file = (ISyncReadFile) client.openFile(openFilename, FileOptions.READ);
            file.setReadTimeout(60);
            InputStream in = file.getInputStream();

            renderBinary(in, filename, file.getSize(), false);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
