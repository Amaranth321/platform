package platform.kaisyncwrapper.node.tasks;

import com.google.gson.Gson;
import lib.util.Util;
import models.NodeCommand;
import models.node.NodeObject;
import models.node.NodeSettings;
import play.Logger;

import java.util.List;

/**
 * Sent from nodes to update settings changes
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeSettingsChanged extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        List<String> parameters = command.getParameters();
        String jsonNodeSettings = parameters.get(0);
        NodeSettings nodeSettings = new Gson().fromJson(jsonNodeSettings, NodeSettings.class);
        if (nodeSettings == null)
        {
            Logger.error(Util.whichClass() + "failed to parse node settings (%s)", jsonNodeSettings);
            return false;
        }

        NodeObject nodeObject = getNodeObject();
        nodeObject.setSettings(nodeSettings);
        nodeObject.save();

        return true;
    }
}
