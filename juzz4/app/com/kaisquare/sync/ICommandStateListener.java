package com.kaisquare.sync;

import models.NodeCommand;

public interface ICommandStateListener {
	void onCommandStateChanged(NodeCommand command, NodeCommandState state);
}
