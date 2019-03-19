package platform.kaisyncwrapper.cloud.tasks;

import com.google.gson.Gson;
import models.licensing.NodeLicense;
import platform.node.NodeManager;

/**
 * Sent from Cloud when node license is updated by cloud
 * processCommand will be executed on node
 *
 * @author Aye Maung
 */

public class CloudUpdateLicense extends CloudToNodeCommandTask
{
    @Override
    protected boolean processCommand() throws Exception
    {
        String jsonInstance = getParameter(0);
        NodeLicense updatedLicense = new Gson().fromJson(jsonInstance, NodeLicense.class);

        NodeManager nodeMgr = NodeManager.getInstance();
        nodeMgr.setLicense(updatedLicense);
        nodeMgr.refreshLicense();
        return true;
    }
}
