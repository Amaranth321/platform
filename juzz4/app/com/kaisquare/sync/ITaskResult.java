package com.kaisquare.sync;

import models.NodeCommand;

public interface ITaskResult {
	
	public NodeCommand getCommand();
	
	public Object getResult();

}
