package platform.kaisyncwrapper.node.tasks;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import models.NodeCommand;
import models.node.NodeCamera;
import models.node.NodeObject;

import java.util.List;

/**
 * Sent from nodes to sync devices
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */

public class NodeSyncDevices extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        List<String> parameters = command.getParameters();
        String jsonDevices = parameters.get(0);

        List<NodeCamera> nodeCameraList = new Gson().fromJson(jsonDevices,
                                                              new TypeToken<List<NodeCamera>>()
                                                              {
                                                              }.getType()
        );

        NodeObject nodeObject = getNodeObject();

        //need to copy currently assigned labels
        for (NodeCamera updatedCam : nodeCameraList)
        {
            if (!nodeObject.getCameras().contains(updatedCam))
            {
                continue;
            }

            int index = nodeObject.getCameras().indexOf(updatedCam);
            NodeCamera currentCam = nodeObject.getCameras().get(index);
            updatedCam.labels = currentCam.labels;
        }

        nodeObject.getCameras().clear();
        nodeObject.getCameras().addAll(nodeCameraList);
        nodeObject.save();

        return true;
    }
}
