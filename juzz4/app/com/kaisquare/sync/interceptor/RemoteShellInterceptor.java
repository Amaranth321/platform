package com.kaisquare.sync.interceptor;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.kaisquare.kaisync.platform.Command;
import com.rabbitmq.client.Channel;

import platform.mq.CommandPublisher;
import platform.mq.MQConnection;
import play.Logger;
import models.NodeCommand;
import models.RemoteShellState;

/**
 * We have new process on nodes that bind an exchange 'remote_shell' on RabbitMQ for receiving REMOTE_SHELL commands only
 */
public class RemoteShellInterceptor implements CommandInterceptor {
	
	public static final String EXCHANGE_NAME = "remote_shell";
	public static final CommandInterceptor INSTANCE = new RemoteShellInterceptor();
	
	private RemoteShellInterceptor()
	{
	}

	@Override
	public boolean intercept(NodeCommand command) {
		boolean ret = false;
		String macAddress = command.getMacAddress();
		RemoteShellState rss = RemoteShellState.find("macAddress", macAddress).get();
		
		if (rss != null && rss.registered)
		{
			Command cmd = new Command(command.getIdAsStr(), command.getCommand().toString(), command.getSourceId());
			cmd.getParameters().addAll(command.getParameters());
			
			Channel channel = null;
			try {
				channel = MQConnection.createNewChannel();
				channel.basicPublish(EXCHANGE_NAME, command.getMacAddress(), null, CommandPublisher.newCommandPacket(cmd).toBytes());

				ret = true;
			} catch (IOException e) {
				Logger.error(e, "error publishing command by RemoteShellStateHook");
			} finally {
				if (channel != null)
				{
					try {
						channel.close();
					} catch (IOException | TimeoutException e) {}
				}
			}
		}
		
		return ret;
	}
}
