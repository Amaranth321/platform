package com.kaisquare.sync;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import models.NodeCommand;

public class NodeCommandFuture implements Future<NodeCommandState> {
	
	private String commandId;
	private StateChangeListener listener;
	private NodeCommandState currentState;
	
	NodeCommandFuture(String commandId)
	{
		this.commandId = commandId;
		TaskManager.getDefaultListener().addFutureListener(commandId, this);
	}
	
	void triggerStateChanged(NodeCommand command, NodeCommandState state)
	{
		currentState = state;
		if (listener != null)
			listener.onCommandStateChanged(command, state);
	}
	
	private NodeCommand getNodeCommand(String id)
	{
		return NodeCommand.find("_id", commandId).first();
	}
	
	public NodeCommand getNodeCommand()
	{
		return getNodeCommand(commandId);
	}
	
	public String getCommandId()
	{
		return commandId;
	}
	
	public void setStateChangedListener(StateChangeListener listener)
	{
		this.listener = listener;
		NodeCommand command = getNodeCommand();
		if (isDone() || isCancelled())
		{
			if (command != null)
			{
				triggerStateChanged(command, command.getState());
				TaskManager.getDefaultListener().removeFutureListener(commandId);
			}
		}
		else if (command.getState() != NodeCommandState.Pending)
			triggerStateChanged(command, command.getState());
	}
	
	/**
	 * Cancel this command, only the {@link NodeCommandState#Pending} is able to cancel
	 * @return
	 */
	public boolean cancel()
	{
		return cancel(true);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		NodeCommand command = getNodeCommand();
		if (command != null && command.getState() == NodeCommandState.Pending)
		{
			TaskManager.getInstance().changeCommandState(NodeCommandState.Cancel, command);
			return true;
		}
		else
			return false;
	}

	@Override
	public boolean isCancelled() {
		NodeCommand command = getNodeCommand();
		if (command == null)
			return true;
		else
			return command.getState() == NodeCommandState.Cancel;
	}

	@Override
	public boolean isDone() {
		NodeCommand command = getNodeCommand();
		if (command == null)
			return true;
		else
			return command.getState() == NodeCommandState.Failed
				|| command.getState() == NodeCommandState.Success;
	}

	@Override
	public NodeCommandState get() throws InterruptedException,
			ExecutionException {
		return getState();
	}

	@Override
	public NodeCommandState get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return getState();
	}
	
	public NodeCommandState getState()
	{
		NodeCommand command = getNodeCommand();
		if (currentState != null)
			return currentState;
		
		if (command == null)
			return null;
		else
			return getNodeCommand().getState();
	}

	/**
	 * Observe the state of current {@link NodeCommand}
	 */
	public interface StateChangeListener
	{
		void onCommandStateChanged(NodeCommand command, NodeCommandState state);
	}
}
