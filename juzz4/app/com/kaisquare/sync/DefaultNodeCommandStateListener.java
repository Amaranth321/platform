package com.kaisquare.sync;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import models.NodeCommand;
import play.Invoker;
import play.Invoker.InvocationContext;
import play.Logger;
import play.Play;
import play.utils.PThreadFactory;

class DefaultNodeCommandStateListener implements ICommandStateListener {
	
	private Map<String, CommandStateHolder> observer = new ConcurrentHashMap<String, CommandStateHolder>();
	
	private Object lock = new Object();
	private ExecutorService stateES;
	
	public DefaultNodeCommandStateListener()
	{
		stateES = Executors.newSingleThreadExecutor(new PThreadFactory("CommandStateListener"));
		stateES.execute(new CommandStateMonitor());
	}

	@Override
	public void onCommandStateChanged(NodeCommand command, NodeCommandState state) {
		String id = command.getIdAsStr();
		if (observer.containsKey(id))
		{
			CommandStateHolder holder = observer.get(id);
			if (holder != null)
			{
				synchronized (holder) {
					if (holder.state != state
						&& holder.state.ordinal() < state.ordinal())
					{
						Logger.debug("command '%s' state changed: %s > %s", id, holder.state, state);
						NodeCommandFuture future = holder.future;
						holder.state = state;
						future.triggerStateChanged(command, holder.state);
					}
				}
			}
		}
		switch (state)
		{
		case Success:
		case Failed:
		case Cancel:
			if (!(command instanceof DeletedNodeCommand))
				command.delete();
		case Responding:
			removeFutureListener(id);
			break;
		}
	}
	
	void addFutureListener(String id, NodeCommandFuture future)
	{
		observer.put(id, new CommandStateHolder(future));
		synchronized (lock) {
			lock.notifyAll();
		}
	}
	
	void removeFutureListener(String id)
	{
		if (observer.containsKey(id))
		{
			observer.get(id).future = null;
			observer.remove(id);
		}
	}
	
	public void quit()
	{
		synchronized (lock) {
			lock.notifyAll();
		}
		stateES.shutdownNow();
	}

	class CommandStateMonitor extends Invoker.Invocation
	{
		boolean quit;

		@Override
		public void execute() throws Exception {
			while (Play.started && !quit) {
				try
				{
					while (Play.started && observer.size() == 0)
					{
						synchronized (lock) {
							try {
								lock.wait(60000);
							} catch (InterruptedException e1) {}
						}
					}
					if (!Play.started)
						break;
					
					Entry[] entries = observer.entrySet().toArray(new Entry[0]);
					for (Entry e : entries)
					{
						CommandStateHolder holder = (CommandStateHolder) e.getValue();
						NodeCommand cmd = NodeCommand.find("_id", holder.future.getCommandId()).get();
						
						if (cmd != null && cmd.getState() != holder.state)
							onCommandStateChanged(cmd, cmd.getState());
						else if (cmd == null)
						{
							cmd = new DeletedNodeCommand(holder.future.getCommandId(), holder.nodeId, holder.macAddress, holder.cmdType);
							onCommandStateChanged(cmd, NodeCommandState.Success);
						}
					}
					synchronized (lock) {
						try {
							lock.wait(2000);
						} catch (InterruptedException e) {}
					}
				} catch (Throwable e) {
					Logger.warn(e, "CommandStateMonitor: something went wrong");
				}
			}
		}

		@Override
		public InvocationContext getInvocationContext() {
			return new InvocationContext("CommandStateListener", getClass().getAnnotations());
		}
	}
	
	static class DeletedNodeCommand extends NodeCommand
	{
		private String id;
		
		public DeletedNodeCommand(String commandId, String nodeId, String macAddress, CommandType command) {
			super(nodeId, macAddress, command);
			
			id = commandId;
		}
		
		@Override
		public Object getId()
		{
			return id;
		}
		
		@Override
		public String getIdAsStr()
		{
			return id;
		}
	}
	
	static class CommandStateHolder
	{
		NodeCommandFuture future;
		public String nodeId;
		String macAddress;
		CommandType cmdType;
		volatile NodeCommandState state;
		
		public CommandStateHolder(NodeCommandFuture future)
		{
			NodeCommand cmd = future.getNodeCommand();
			this.future = future;
			this.nodeId = cmd.getNodeId();
			this.state = cmd.getState();
			this.macAddress = cmd.getMacAddress();
			this.cmdType = cmd.getCommand();
		}
	}
}
