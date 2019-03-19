package platform.kaisyncwrapper.cloud.tasks;

import com.google.gson.Gson;
import platform.node.NodeManager;

import java.util.HashMap;

/**
 * Sent from Cloud when node is required to open SSH session with a server
 * processCommand will be executed on node
 *
 * @author Kapil Pendse
 */
public class CloudStartRemoteShell extends CloudToNodeCommandTask
{
    @Override
    protected boolean processCommand() throws Exception
    {
        String jsonParams = getParameter(0);

        //parse json
        HashMap<String, String> params = new HashMap<>();
        params = new Gson().fromJson(jsonParams, params.getClass());
        String host = params.get("host");
        String port = params.get("port");
        String username = params.get("user");

        //run command to start autossh
        boolean result = NodeManager.getInstance().startRemoteShell(host, port, username);
        return result;
    }
}
