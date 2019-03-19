package com.kaisquare.kaisync.server;

import org.apache.thrift.TProcessor;

import com.kaisquare.kaisync.file.IFileTransferHandler;
import com.kaisquare.kaisync.thrift.FileTransferService;

/*package*/class FileTransferServer extends ThriftServer {
	
	private IFileTransferHandler mHandler;

	public FileTransferServer(int port, IFileTransferHandler handler) {
		super(port);
		
		mHandler = handler;
	}

	@Override
	public String getName() {
		return "File Transfer Server";
	}

	@Override
	protected TProcessor createProcessor() {
		return new FileTransferService.Processor<FileTransferService.Iface>(new FileTransferServiceImpl(mHandler));
	}

}
