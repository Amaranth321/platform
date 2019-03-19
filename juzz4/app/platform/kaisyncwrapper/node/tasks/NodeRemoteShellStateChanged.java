package platform.kaisyncwrapper.node.tasks;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;

import models.NodeCommand;
import models.RemoteShellState;
import models.RemoteShellState.ConnectionState;
import play.Logger;

public class NodeRemoteShellStateChanged extends NodeToCloudCommandTask
{
    private long lastStateTime;

    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        if (command.getCreatedTime() < lastStateTime)
        {
            return true;
        }

        lastStateTime = command.getCreatedTime();
        Datastore ds = RemoteShellState.ds();
        Query<RemoteShellState> query = ds.createQuery(RemoteShellState.class);
        query.and(query.criteria("cloudPlatformDeviceId").equal(command.getNodeId()));
        UpdateOperations<RemoteShellState> ops = ds.createUpdateOperations(RemoteShellState.class);

        switch (command.getCommand())
        {
            case NODE_REMOTE_SHELL_STARTED:
                ops.set("connectionState", ConnectionState.NODE_CONNECTED);
                ds.findAndModify(query, ops, false, false);
                break;
            case NODE_REMOTE_SHELL_STOPPED:
                ops.set("connectionState", ConnectionState.NODE_DISCONNECTED);
                ds.findAndModify(query, ops, false, false);
                break;
            default:
                //should not reach here
                Logger.error("remote shell status: %s", command.getCommand());
        }

        return true;
    }
}
