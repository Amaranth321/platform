package platform.kaisyncwrapper.cloud.listeners;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.kaisquare.sync.NodeCommandState;

import models.NodeCommand;
import models.RemoteShellState;
import models.RemoteShellState.ConnectionState;
import platform.kaisyncwrapper.QueuedCommandStateListener;
import play.Logger;


public class CloudStopRemoteShellListener implements QueuedCommandStateListener
{
    @Override
    public void onStateChanged(NodeCommand command, NodeCommandState state)
    {
        String nodeId = command.getNodeId();
        Datastore ds = RemoteShellState.ds();
        Query<RemoteShellState> query = ds.createQuery(RemoteShellState.class);
        query.and(query.criteria("cloudPlatformDeviceId").equal(nodeId));

        UpdateOperations<RemoteShellState> ops = ds.createUpdateOperations(RemoteShellState.class);
        switch (state)
        {
            case Success:
                ops.set("connectionState", ConnectionState.NODE_DISCONNECTED);
                ds.findAndModify(query, ops, false, false);
                break;
            case Failed:
            case Cancel:
            {
                Logger.warn("unable to stop remote shell for device %s", nodeId);
                break;
            }
        }
    }
}
