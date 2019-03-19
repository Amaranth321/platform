package com.kaisquare.kaisync.server;

import org.apache.thrift.TProcessor;

import com.kaisquare.kaisync.thrift.SoftwareUpdateService;

/*package*/class SoftwareUpdateServer extends ThriftServer {
	
	private ISoftwareUpdateHandler mHandler;

	public SoftwareUpdateServer(int port, ISoftwareUpdateHandler handler) {
		super(port);
		
		mHandler = handler;
	}

	@Override
	public String getName() {
		return "Software Update Server";
	}

	@Override
	protected TProcessor createProcessor() {
		return new SoftwareUpdateService.Processor<SoftwareUpdateService.Iface>(new SoftwareUpdateServiceImpl(mHandler));
	}

}
