package platform.kaisyncwrapper;

import com.kaisquare.sync.NodeCommandFuture;
import com.kaisquare.sync.NodeCommandState;
import com.kaisquare.sync.TaskManager;
import com.kaisquare.util.SysInfoUtil;
import lib.util.Util;
import models.NodeCommand;
import models.command.QueuedPlatformCommand;
import platform.Environment;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.debug.CommandDebugger;
import play.Logger;

/**
 * @author Aye Maung
 * @since 4.3
 */
public abstract class PlatformCommandQueue<T extends QueuedPlatformCommand>
{
    protected static CommandDebugger debugger = CommandDebugger.getInstance();

    public abstract void queueCommand(T command);

    public abstract void clearAll();

    protected abstract void processNextInQueue();

    protected abstract T getNext();

    protected abstract void dequeue(T command);

    protected abstract void onCommandCompleted(T command, NodeCommandState state);

    protected NodeCommandFuture sendToKaiSync(T command) throws Exception
    {
        String macAddress = command.getMacAddress();

        //this check can be removed in newer versions (> v4.4rc7 and > v4.4.0.9)
        if (Util.isNullOrEmpty(macAddress))
        {
            if (Environment.getInstance().onKaiNode())
            {
                macAddress = SysInfoUtil.getMacAddress(false);
            }
            else
            {
                CachedDevice cachedDevice = CacheClient.getInstance().getDeviceByPlatformId(command.getNodeId());
                macAddress = cachedDevice.getDeviceKey();
            }
        }

        NodeCommandFuture future = TaskManager.getInstance().sendCommand(
                command.getNodeId(),
                macAddress,
                command.getCommandType(),
                command.getParamsAsArray());

        future.setStateChangedListener(getKaiSyncStateListener(command));
        return future;
    }

    private NodeCommandFuture.StateChangeListener getKaiSyncStateListener(final T queuedCommand)
    {
        NodeCommandFuture.StateChangeListener kaiSyncListener = new NodeCommandFuture.StateChangeListener()
        {
            @Override
            public void onCommandStateChanged(NodeCommand command, NodeCommandState state)
            {
                //Type-specific actions
                try
                {
                    command.getCommand().getListener().onStateChanged(command, state);
                }
                catch (Exception e)
                {
                    Logger.error(e, "%s listener task failed (nodeId:%s)", command.getCommand(), command.getNodeId());
                }

                //notify if the command has completed
                if (state.equals(NodeCommandState.Success) ||
                    state.equals(NodeCommandState.Failed) ||
                    state.equals(NodeCommandState.Cancel))
                {
                    onCommandCompleted(queuedCommand, state);
                }
            }
        };

        return kaiSyncListener;
    }

}
