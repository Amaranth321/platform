package platform.kaisyncwrapper.cloud.listeners;

import models.NodeCommand;

import com.kaisquare.sync.NodeCommandState;
import com.kaisquare.sync.TaskManager;

import platform.kaisyncwrapper.QueuedCommandStateListener;

public class CloudDeleteLicenseListener implements QueuedCommandStateListener {

	@Override
	public void onStateChanged(NodeCommand command, NodeCommandState state) {
		if (state.ordinal() >= NodeCommandState.Sending.ordinal())
		{
			TaskManager.getInstance().removeCommandQueue(command.getNodeId(), command.getMacAddress(), false);
			if (!command.isNew())
				command.delete();
		}
	}

}
