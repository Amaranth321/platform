package controllers.api;

import com.kaisquare.kaisync.ISyncReadFile;
import com.kaisquare.kaisync.file.FileOptions;
import com.kaisquare.kaisync.file.IFileClient;
import com.kaisquare.sync.DataSync;

import controllers.interceptors.APIInterceptor;
import lib.util.ResultMap;
import lib.util.exceptions.ApiException;
import models.MongoDevice;
import models.SoftwareUpdateFile;
import models.node.NodeObject;
import models.transportobjects.UpdateFileTransport;
import platform.CloudActionMonitor;
import platform.CloudSyncManager;
import platform.nodesoftware.NodeSoftwareStatus;
import play.Logger;
import play.mvc.With;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author KAI Square
 *         publicapi (hidden from API documentation)
 * @sectiontitle Over-The-Air (OTA) Software Update
 * @sectiondesc APIs managing OTA software updates
 */

@With(APIInterceptor.class)
public class SoftwareUpdateController extends APIController
{
    /**
     * @param updateFile The software updating file. Mandatory
     *
     * @servtitle Upload software update file
     * @httpmethod POST
     * @uri /api/{bucket}/uploadSoftwareUpdate
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void uploadSoftwareUpdate(File updateFile)
    {
        try
        {
            if (updateFile == null)
            {
                throw new ApiException("error-unreadable-upload-file");
            }

            DataSync.addSoftwareUpdateFile(updateFile);
        }
        catch (ApiException apiEx)
        {
            Logger.error(apiEx, "");
            renderText(apiEx.getMessage());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            renderText("error-upload-file-corrupted");
        }
    }

    /**
     * @servtitle Retrieves software update file list
     * @httpmethod POST
     * @uri /api/{bucket}/getSoftwareUpdateList
     * @responsejson {
     * "result": "ok",
     * "files":[
     * {@link models.SoftwareUpdateFile}
     * ]
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getSoftwareUpdateList()
    {
        try
        {
            List<SoftwareUpdateFile> dbList = SoftwareUpdateFile.q().order("-version").asList();
            List<UpdateFileTransport> transportList = new ArrayList<>();
            for (SoftwareUpdateFile dbFile : dbList)
            {
                transportList.add(new UpdateFileTransport(dbFile));
            }
            respondOK("files", transportList);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param file-server-id software update file id. Mandatory
     *
     * @servtitle Removes software update file
     * @httpmethod POST
     * @uri /api/{bucket}/removeSoftwareUpdate
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void removeSoftwareUpdate()
    {
        try
        {
            String fileId = readApiParameter("file-server-id", true);
            SoftwareUpdateFile dbFile = SoftwareUpdateFile.findByServerId(fileId);
            DataSync.removeSoftwareUpdate(dbFile);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param file-name software update file name.e.g kainode.update. Mandatory
     *
     * @servtitle Downloads software update file
     * @httpmethod POST
     * @uri /api/{bucket}/downloadSoftwareUpdate
     * @responsebinary {
     * <i>Renders Binary file of update</i>
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void downloadSoftwareUpdate(String filename)
    {
        SoftwareUpdateFile dbFile = SoftwareUpdateFile.findByServerId(filename);
        if (dbFile == null)
        {
            notFound();
        }

        IFileClient client = CloudSyncManager.getInstance().newFileClient();
        ISyncReadFile file = (ISyncReadFile) client.openFile(dbFile.getFileServerId(), FileOptions.READ);
        file.setReadTimeout(60);
        try
        {
            if (dbFile.getFileSize() > 0)
            {
                renderBinary(file.getInputStream(),
                             "kainode.update",
                             dbFile.getFileSize(),
                             "application/octet-stream",
                             false);
            }
            else
            {
                renderBinary(file.getInputStream(),
                             "kainode.update",
                             "application/octet-stream",
                             false);
            }
        }
        catch (IOException e)
        {
            Logger.error(e, "error downloading file %s", filename);
            ResultMap map = new ResultMap();
            map.put("result", "error");
            renderJSON(map);
        }
    }

    /**
     * @param node-id Node's platform device Id
     *
     * @servtitle Update node software.
     * @httpmethod POST
     * @uri /api/{bucket}/updatenodesoftware
     * @responsejson {
     * "result" : "ok"
     * }
     * @responsejson {
     * "result" : "error",
     * "reason" : "unknow"
     * }
     */
    public static void updatenodesoftware()
    {
        try
        {
            String nodeId = readApiParameter("node-id", true);
            MongoDevice nodeDevice = MongoDevice.getByPlatformId(nodeId);
            if (nodeDevice == null || !nodeDevice.isKaiNode())
            {
                throw new ApiException("invalid-node-id");
            }

            CloudActionMonitor.getInstance().cloudUpdateNodeSoftware(nodeId);

            //update status
            NodeObject nodeObject = NodeObject.findByPlatformId(nodeId);
            nodeObject.setSoftwareStatus(NodeSoftwareStatus.UPDATING, null);
            nodeObject.save();

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
