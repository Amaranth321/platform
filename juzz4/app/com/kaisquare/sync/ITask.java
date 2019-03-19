package com.kaisquare.sync;

import java.util.List;

import models.NodeCommand;

/**
 * The command task which assigned by remote
 */
public interface ITask {
	
	boolean doTask(NodeCommand command);

}
