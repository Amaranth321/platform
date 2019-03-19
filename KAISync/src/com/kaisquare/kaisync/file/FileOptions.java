package com.kaisquare.kaisync.file;

public enum FileOptions {
	READ(1),
	WRITE(2),
	DELETE(3);
	
	private int value;
	FileOptions(int value)
	{
		this.value = value;
	}
	
	public int getIntValue()
	{
		return this.value;
	}
}
