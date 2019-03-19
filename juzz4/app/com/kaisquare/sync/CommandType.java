package com.kaisquare.sync;

import com.kaisquare.sync.interceptor.CommandInterceptor;
import com.kaisquare.sync.interceptor.RemoteShellInterceptor;
import platform.kaisyncwrapper.CommandDestination;
import platform.kaisyncwrapper.DefaultQueuedCommandListener;
import platform.kaisyncwrapper.QueuedCommandStateListener;
import platform.kaisyncwrapper.VcaActionListener;
import platform.kaisyncwrapper.cloud.listeners.CloudDeleteLicenseListener;
import platform.kaisyncwrapper.cloud.listeners.CloudPullLogListener;
import platform.kaisyncwrapper.cloud.listeners.CloudStartRemoteShellListener;
import platform.kaisyncwrapper.cloud.listeners.CloudStopRemoteShellListener;
import platform.kaisyncwrapper.cloud.tasks.*;
import platform.kaisyncwrapper.node.listeners.NodeSyncDeviceListListener;
import platform.kaisyncwrapper.node.listeners.NodeSyncVcaListListener;
import platform.kaisyncwrapper.node.tasks.*;

import java.util.Arrays;
import java.util.List;

public enum CommandType
{
    /**
     * Node to Cloud
     */
    NODE_VCA_ADDED(
            CommandDestination.CLOUD,
            NodeVcaAdded.class
    ),
    NODE_VCA_UPDATED(
            CommandDestination.CLOUD,
            NodeVcaUpdated.class
    ),
    NODE_VCA_REMOVED(
            CommandDestination.CLOUD,
            NodeVcaRemoved.class
    ),
    NODE_VCA_ACTIVATED(
            CommandDestination.CLOUD,
            NodeVcaActivated.class
    ),
    NODE_VCA_DEACTIVATED(
            CommandDestination.CLOUD,
            NodeVcaDeactivated.class
    ),
    NODE_DEVICE_ADDED(
            CommandDestination.CLOUD,
            NodeDeviceAdded.class
    ),
    NODE_DEVICE_UPDATED(
            CommandDestination.CLOUD,
            NodeDeviceUpdated.class
    ),
    NODE_DEVICE_DELETED(
            CommandDestination.CLOUD,
            NodeDeviceDeleted.class
    ),
    NODE_SYNC_DEVICE(
            CommandDestination.CLOUD,
            NodeSyncDevices.class
    ),
    NODE_SYNC_VCA(
            CommandDestination.CLOUD,
            NodeSyncVca.class
    ),
    NODE_SETTINGS_CHANGED(
            CommandDestination.CLOUD,
            NodeSettingsChanged.class
    ),
    NODE_REQUEST_LICENSE(
            CommandDestination.CLOUD,
            NodeRequestLicense.class
    ),
    NODE_FACTORY_RESET(
            CommandDestination.CLOUD,
            NodeFactoryReset.class
    ),
    NODE_UPDATE_INFO(
            CommandDestination.CLOUD,
            NodeUpdateInfo.class
    ),
    NODE_REMOTE_SHELL_STARTED(
            CommandDestination.CLOUD,
            NodeRemoteShellStateChanged.class
    ),
    NODE_REMOTE_SHELL_STOPPED(
            CommandDestination.CLOUD,
            NodeRemoteShellStateChanged.class
    ),
    NODE_UPDATE_FILE_READY(
            CommandDestination.CLOUD,
            NodeUpdateFileReady.class
    ),
    NODE_VCA_STATE_CHANGED(
            CommandDestination.CLOUD,
            NodeVcaStateChanged.class
    ),
    NODE_UPDATE_VCA_STATES(
            CommandDestination.CLOUD,
            NodeUpdateVcaStates.class
    ),

    NODE_CAMERA_STATUS_CHANGED(
            CommandDestination.CLOUD,
            NodeCameraStatusChanged.class
    ),
    NODE_STORAGE_EXPANDED(
            CommandDestination.CLOUD,
            NodeStorageExpanded.class
    ),

    /**
     * Cloud to Node
     */
    CLOUD_ADD_NODE_VCA(
            CommandDestination.NODE,
            CloudAddNodeVca.class,
            true
    ),
    CLOUD_UPDATE_NODE_VCA(
            CommandDestination.NODE,
            CloudUpdateNodeVca.class,
            true
    ),
    CLOUD_REMOVE_NODE_VCA(
            CommandDestination.NODE,
            CloudRemoveNodeVca.class,
            true
    ),
    CLOUD_ACTIVATE_NODE_VCA(
            CommandDestination.NODE,
            CloudActivateNodeVca.class,
            true
    ),
    CLOUD_DEACTIVATE_NODE_VCA(
            CommandDestination.NODE,
            CloudDeactivateNodeVca.class,
            true
    ),
    CLOUD_START_REMOTE_SHELL(
            CommandDestination.NODE,
            CloudStartRemoteShell.class,
            true
    ),
    CLOUD_STOP_REMOTE_SHELL(
            CommandDestination.NODE,
            CloudStopRemoteShell.class,
            true
    ),
    CLOUD_EDIT_NODE_DEVICE(
            CommandDestination.NODE,
            CloudEditNodeDevice.class,
            true
    ),
    CLOUD_PULL_LOG(
            CommandDestination.NODE,
            CloudPullLog.class,
            false
    ),
    CLOUD_SUSPEND_LICENSE(
            CommandDestination.NODE,
            CloudSuspendLicense.class,
            true
    ),
    CLOUD_UNSUSPEND_LICENSE(
            CommandDestination.NODE,
            CloudUnsuspendLicense.class,
            true
    ),
    CLOUD_UPDATE_LICENSE(
            CommandDestination.NODE,
            CloudUpdateLicense.class,
            true
    ),
    CLOUD_DELETE_LICENSE(
            CommandDestination.NODE,
            CloudDeleteLicense.class,
            false
    ),
    CLOUD_UPDATE_BUCKET_SETTINGS(
            CommandDestination.NODE,
            CloudUpdateBucketSettings.class,
            true
    ),
    CLOUD_UPDATE_NODE(
            CommandDestination.NODE,
            CloudUpdateNode.class,
            true
    ),
    CLOUD_UPDATE_ALERT_SETTINGS(
            CommandDestination.NODE,
            CloudUpdateAlertSettings.class,
            true
    ),

    /*
     * End of supported types
     */
    @Deprecated
    NODE_SEND_STATISTICS(
            CommandDestination.CLOUD,
            NodeSendStatistics.class
    );

    public static CommandType parse(String typeString)
    {
        for (CommandType commandType : values())
        {
            if (commandType.name().equalsIgnoreCase(typeString))
            {
                return commandType;
            }
        }

        return null;
    }

    /**
     * vca actions require special handling sometimes, hence this preset list
     */
    private static final List<CommandType> VCA_ACTIONS = Arrays.asList(
            NODE_VCA_ADDED,
            NODE_VCA_UPDATED,
            NODE_VCA_REMOVED,
            NODE_VCA_ACTIVATED,
            NODE_VCA_DEACTIVATED,

            CLOUD_ADD_NODE_VCA,
            CLOUD_UPDATE_NODE_VCA,
            CLOUD_REMOVE_NODE_VCA,
            CLOUD_ACTIVATE_NODE_VCA,
            CLOUD_DEACTIVATE_NODE_VCA
    );

    static
    {
        //assign custom node listeners
        NODE_SYNC_DEVICE.listener = new NodeSyncDeviceListListener();
        NODE_SYNC_VCA.listener = new NodeSyncVcaListListener();

        //assign custom cloud listeners
        CLOUD_START_REMOTE_SHELL.listener = new CloudStartRemoteShellListener();
        CLOUD_START_REMOTE_SHELL.interceptor = RemoteShellInterceptor.INSTANCE;
        CLOUD_STOP_REMOTE_SHELL.listener = new CloudStopRemoteShellListener();
        CLOUD_STOP_REMOTE_SHELL.interceptor = RemoteShellInterceptor.INSTANCE;
        CLOUD_DELETE_LICENSE.listener = new CloudDeleteLicenseListener();
        CLOUD_PULL_LOG.listener = new CloudPullLogListener();


        //vca actions
        for (CommandType vcaAction : VCA_ACTIONS)
        {
            vcaAction.listener = new VcaActionListener();
        }
    }

    private final CommandDestination destination;
    private final Class<? extends ITask> taskClass;
    private QueuedCommandStateListener listener;
    private CommandInterceptor interceptor;

    /**
     * Some command types need to be executed in the same order.
     * Usually they are actions, like start, stop, suspend, etc ...
     * Only used on the cloud side.
     * <p/>
     * DO NOT set this to false unless you are absolutely sure.
     */
    private final boolean sequentialExecutionForSameNode;

    private CommandType(CommandDestination destination,
                        Class<? extends ITask> taskClass)
    {
        this.taskClass = taskClass;
        this.listener = new DefaultQueuedCommandListener();
        this.destination = destination;
        this.sequentialExecutionForSameNode = true;
    }

    private CommandType(CommandDestination destination,
                        Class<? extends ITask> taskClass,
                        boolean sequentialExecutionForSameNode)
    {
        this.destination = destination;
        this.taskClass = taskClass;
        this.listener = new DefaultQueuedCommandListener();
        this.sequentialExecutionForSameNode = sequentialExecutionForSameNode;
    }

    public CommandDestination destination()
    {
        return destination;
    }

    public void register() throws IllegalAccessException, InstantiationException
    {
        TaskManager.getInstance().register(this, taskClass.newInstance());
    }

    public QueuedCommandStateListener getListener()
    {
        return listener;
    }

    public CommandInterceptor getInterceptor()
    {
        return interceptor;
    }

    public boolean isSequentialExecutionForSameNode()
    {
        return sequentialExecutionForSameNode;
    }

    public boolean isVcaAction()
    {
        return VCA_ACTIONS.contains(this);
    }
}
