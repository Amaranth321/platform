package com.kaisquare.kaisync.file;

public class ChunkedData {
	
	private int mSize;
	private byte[] mData;
	
	public ChunkedData(int size, byte[] data)
	{
		mSize = size;
		mData = data;
	}

	public int getSize()
	{
		return mSize;
	}
	
	public byte[] getData()
	{
		return mData.clone();
	}
	
	public int copy(byte[] dest, int off, int len)
	{
		if (dest == null)
			throw new NullPointerException("destination buffer is invalid");
		else if (mData == null)
			throw new NullPointerException("no available data in this chunked");
		
		int length = Math.min(len, mData.length);
		System.arraycopy(mData, 0, dest, off, length);
		
		return length;
	}
}
