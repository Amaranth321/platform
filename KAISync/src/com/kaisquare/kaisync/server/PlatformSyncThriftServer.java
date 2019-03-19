package com.kaisquare.kaisync.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;

import org.apache.thrift.ProcessFunction;
import org.apache.thrift.TBase;
import org.apache.thrift.TProcessor;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import com.kaisquare.kaisync.platform.IPlatformSyncHandler;
import com.kaisquare.kaisync.thrift.PlatformSynchronizationService;
import com.kaisquare.kaisync.thrift.PlatformSynchronizationService.Iface;
import com.kaisquare.kaisync.utils.AppLogger;

/*package*/class PlatformSyncThriftServer extends ThriftServer {
	
	private IPlatformSyncHandler mHandler;

	public PlatformSyncThriftServer(int port, IPlatformSyncHandler handler) {
		super(port);
		
		mHandler = handler;
	}

	@Override
	public String getName() {
		return "Platform Sync Server";
	}

	@Override
	protected TProcessor createProcessor() {
		return new PlatformSyncProcessor(new PlatformSynchronizationServiceImpl(mHandler));
	}

	protected class PlatformSyncProcessor extends PlatformSynchronizationService.Processor<PlatformSynchronizationService.Iface>
	{
		@SuppressWarnings("rawtypes")
		public PlatformSyncProcessor(Iface iface,
				Map<String, ProcessFunction<Iface, ? extends TBase>> processMap) {
			super(iface, processMap);
		}

		public PlatformSyncProcessor(Iface iface) {
			super(iface);
		}

		@Override
		public boolean beforeProcess(TTransport transport) {
			ClientChannel clientChannel = new ClientChannel();
			InetSocketAddress address = null;
			if (transport instanceof TNonblockingSocket)
			{
				TNonblockingSocket socket = (TNonblockingSocket)transport;
				SocketChannel channel = socket.getSocketChannel();
				try {
					address = (InetSocketAddress) channel.getRemoteAddress();
					clientChannel.setAddress(address);
				} catch (IOException e) {
					AppLogger.e(this, e, "");
				}
			}
			else if (transport instanceof TSocket)
			{
				TSocket socket = (TSocket) transport;
				address = (InetSocketAddress) socket.getSocket().getRemoteSocketAddress();
				clientChannel.setAddress(address);
			}
			if (getIface() instanceof IClientChannelListener)
				return ((IClientChannelListener)getIface()).channelRequest(clientChannel);
			
			return true;
		}
	}
}
