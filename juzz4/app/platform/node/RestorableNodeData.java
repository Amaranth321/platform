package platform.node;

import lib.util.exceptions.ApiException;
import models.MongoDevice;
import models.node.NodeCamera;
import models.node.NodeObject;
import models.node.NodeSettings;
import models.transportobjects.NodeCameraTransport;
import models.transportobjects.VcaTransport;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaManager;
import play.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Aye Maung
 */
public class RestorableNodeData
{
    private final List<NodeCameraTransport> cameraList;
    private final List<VcaTransport> vcaList;
    private final NodeSettings nodeSettings;

    public static RestorableNodeData of(String nodeCloudPlatformId) throws ApiException
    {
        NodeObject nodeObject = NodeObject.findByPlatformId(nodeCloudPlatformId);
        MongoDevice mongoDevice = MongoDevice.getByPlatformId(nodeCloudPlatformId);
        if (nodeObject == null || mongoDevice == null)
        {
            throw new ApiException(String.format("Old node not found (nodeId=%s)", nodeCloudPlatformId));
        }

        //cameras
        List<NodeCamera> sortedList = nodeObject.getCameras();
        Collections.sort(sortedList, NodeCamera.sortByCoreId);
        List<NodeCameraTransport> cameraTransports = new ArrayList<>();
        for (NodeCamera nodeCamera : sortedList)
        {
            cameraTransports.add(new NodeCameraTransport(nodeObject, nodeCamera));
        }

        //vca
        List<IVcaInstance> vcaList = VcaManager.getInstance().listVcaInstancesOfDevice(mongoDevice);
        List<VcaTransport> vcaTransports = new ArrayList<>();
        for (IVcaInstance vcaInstance : vcaList)
        {
            vcaTransports.add(new VcaTransport(vcaInstance));
        }

        Logger.info("[nodeId=%s] Restorable Data: %s cameras, %s vca instances",
                    nodeCloudPlatformId,
                    cameraTransports.size(),
                    vcaTransports.size());
        return new RestorableNodeData(cameraTransports, vcaTransports, nodeObject.getSettings());
    }

    private RestorableNodeData(List<NodeCameraTransport> cameraList,
                               List<VcaTransport> vcaList,
                               NodeSettings nodeSettings)
    {
        this.cameraList = cameraList;
        this.vcaList = vcaList;
        this.nodeSettings = nodeSettings;
    }

    public List<NodeCameraTransport> getCameraList()
    {
        return cameraList;
    }

    public List<VcaTransport> getVcaList()
    {
        return vcaList;
    }

    public NodeSettings getNodeSettings()
    {
        return nodeSettings;
    }
}
