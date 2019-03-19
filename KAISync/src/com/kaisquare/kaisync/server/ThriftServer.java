package com.kaisquare.kaisync.server;

import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import com.kaisquare.kaisync.utils.ThriftUtil;

abstract class ThriftServer implements ISyncServer {

	private TServer mTServer = null;
	private TServerTransport mTransport = null;
	private int mPort;
	private String mKeystore;
	private String mKeypass;
	protected int mReadTimeout;
	protected int mWriteTimeout;
	
	public ThriftServer(int port)
	{
		mPort = port;
	}
	
	protected void startThriftServer() throws TTransportException
	{
		if (mKeystore != null && !"".equals(mKeypass.trim()))
			mTServer = ThriftUtil.newServiceSSLServer(createProcessor(), mPort, mKeystore, mKeypass);
		else
			mTServer = ThriftUtil.newServiceServer(createProcessor(), mPort);
		
//			mTransport = new TServerSocket(mPort);
//			TProcessor processor = createProcessor();
//			Factory factory = new TBinaryProtocol.Factory(true, true);
//			Args arg = new Args(mTransport);
//			arg.processor(processor);
//			arg.protocolFactory(factory);
//			arg.transportFactory(new TFramedTransport.Factory());
//			mTServer = new TThreadPoolServer(arg);
//			
//			notifyStarted();
//			AppLogger.i(this, getServiceName() + " server bound to " + mPort);
//			mTServer.serve();
		
//			TServerSocket socket = new TServerSocket(mPort);
//		    TTransportFactory transportFactory = new TFramedTransport.Factory();
//		    TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
//		    mTServer = new TSimpleMultiplexServer(socket, transportFactory, protocolFactory);
//		    
//		    AppLogger.i(this, "register StreamControlService service");
//		    mTServer.register("StreamControlService", new StreamControlService.Processor(new StreamControlServiceImpl()));
//		    AppLogger.i(this, "register DataService service");
//		    mTServer.register("DataService", new DataService.Processor(new DataServiceImpl()));
//		    
//		    AppLogger.i(this, "thrift service started on " + mPort);
//		    triggerStarted();
//		    mTServer.serve();
	}
	
	@Override
	public void setKeystore(String keystore, String keypass)
	{
		mKeystore = keystore;
		mKeypass = keypass;
	}
	
	@Override
	public void setReadTimeout(int timeout)
	{
		mReadTimeout = timeout;
	}
	
	@Override
	public void setWriteTimeout(int timeout)
	{
		mWriteTimeout = timeout;
	}
	
	@Override
	public void start()
	{
		try {
			startThriftServer();
		} catch (TTransportException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void stop()
	{
		if (mTransport != null)
			mTransport.close();
		
		if (mTServer != null)
			mTServer.stop();
	}

	public TServer getServer() {
		return mTServer;
	}

	@Override
	public int getPort() {
		return mPort;
	}
	
	/**
	 * Create implemented processor to start a thrift service
	 * @return
	 */
	protected abstract TProcessor createProcessor();
}
