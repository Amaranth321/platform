package com.kaisquare.kaisync.platform;

import com.kaisquare.kaisync.ISyncReadFile;

class UpdateFileInfoWrapper implements UpdateFileInfo {
	
	private String version;
	private String model;
	private ISyncReadFile file;
	
	public UpdateFileInfoWrapper(String version, String model, ISyncReadFile file)
	{
		this.version = version;
		this.model = model;
		this.file = file;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public String getModel() {
		return model;
	}

	@Override
	public ISyncReadFile getFile() {
		return file;
	}

}
