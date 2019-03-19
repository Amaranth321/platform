package com.kaisquare.sync;

import java.util.concurrent.Future;

import com.kaisquare.kaisync.platform.Command;
import com.kaisquare.util.StringCollectionUtil;

import play.Logger;
import play.Play;
import play.jobs.Job;
import models.NodeCommand;

/*package*/class TaskLooper {
	
	public TaskLooper()
	{
	}
	
	public Future<Object> newTask(NodeCommand command, ITask task)
	{
		TaskThread t = new TaskThread(command, task);
		return t.now();
	}

	private static class TaskThread extends Job<Object> implements ITaskResult
	{
		private NodeCommand command;
		private ITask task;
		private boolean isDone = false;
		private boolean result;
		
		public TaskThread(NodeCommand command, ITask task)
		{
			this.command = command;
			this.task = task;
		}
		
		@Override
		public NodeCommand getCommand()
		{
			return command;
		}

		@Override
		public Object getResult() {
			if (isDone)
				return result;
			else
				return null;
		}

		@Override
		public Object doJobWithResult() throws Exception {
			if (!isDone)
			{
				long start = System.nanoTime();
				try {
					result = this.task.doTask(command);
				} catch (Exception e) {
					Logger.error(e, "failed to execute command '%s', %s",
							command.getCommand(),
							StringCollectionUtil.join(command.getParameters(), ","));
					result = false;
				}
				isDone = true;
				command.respond(result);
				
				//send a response message to the queue for the newer version of nodes
				long end = System.nanoTime();
				long spent = (end - start) / 1000000;
				if (spent > 100)
					Logger.warn("task for command '%s' took %d ms", command.getCommand(), spent);
				
				Command cmd = new Command(command.getIdAsStr(), command.getCommand().toString(), command.getSourceId());
				cmd.getParameters().add(command.getResult() ? NodeCommandState.Success.toString() : NodeCommandState.Failed.toString());
				if (TaskManager.getInstance().sendCommand(command.getNodeId(), command.getMacAddress(), cmd))
				{
					if (!command.isNew())
						command.delete();
				}
				else
					command.save();
			}
			
			return this;
		}
	}
}
