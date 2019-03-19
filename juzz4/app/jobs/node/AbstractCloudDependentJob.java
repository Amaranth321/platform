package jobs.node;

import models.node.NodeInfo;
import platform.node.CloudConnector;
import platform.node.KaiSyncCommandClient;
import platform.node.NodeManager;
import play.Logger;

/**
 * Template for cloud-dependent jobs on nodes. Refer to {@link #check} for run conditions
 * <p/>
 * Notes:
 * <p/>
 * doJob() is intentionally prevented to be overridden. DO NOT change it.
 * Make use of {@link #setStatus} inside {@link #doTask} implementation if a more accurate status is required.
 * If the task is about clearing items in a queue, inherit from {@link SingletonCloudDependentQueueJob} instead.
 *
 * @author Aye Maung
 * @since v4.3
 */
abstract class AbstractCloudDependentJob extends NodeCronJob
{
    private NodeInfo nodeInfo;
    private NodeJobStatus status;

    @Override
    public final void doJob()
    {
        try
        {
            if (check())
            {
                doTask();
                setStatus(NodeJobStatus.IDLE);
            }
        }
        catch (Throwable e)
        {
            Logger.error(e, "");
            setStatus(NodeJobStatus.ERROR);
        }
    }

    protected abstract void doTask() throws Exception;

    protected NodeInfo getNodeInfo()
    {
        return nodeInfo;
    }

    protected NodeJobStatus getStatus()
    {
        return status;
    }

    protected void setStatus(NodeJobStatus newStatus)
    {
        status = newStatus;
    }

    private boolean check()
    {
        //not registered yet
        if (!NodeManager.getInstance().isRegisteredOnCloud())
        {
            setStatus(NodeJobStatus.UNREGISTERED);
            return false;
        }

        //suspended
        if (NodeManager.getInstance().isSuspended())
        {
            setStatus(NodeJobStatus.SUSPENDED);
            return false;
        }

        //waiting for startup sync
        if (!KaiSyncCommandClient.isStartupSyncDone())
        {
            setStatus(NodeJobStatus.WAITING_STARTUP_SYNC);
            return false;
        }

        //cloud offline
        if (!CloudConnector.getInstance().isCloudReachable(10))
        {
            setStatus(NodeJobStatus.CLOUD_UNREACHABLE);
            return false;
        }

        //set nodeInfo
        if (nodeInfo == null)
        {
            nodeInfo = NodeManager.getInstance().getNodeInfo();
        }

        return true;
    }
}
