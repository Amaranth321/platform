package com.kaisquare.kaisync.transport;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.util.CharsetUtil;

import com.kaisquare.kaisync.file.ChunkedData;
import com.kaisquare.kaisync.utils.Utils;

public class StringSyncDecoder extends DelimiterBasedFrameDecoder {

	protected static enum State
	{
		READ_HEADER,
		READ_CONTENT
	}
	
	private Map<String, String> mHeaders = new HashMap<String, String>();
	private State mState = State.READ_HEADER;
	
	private Class<? extends StringKeyValueHeader> mClass;
	
	public StringSyncDecoder(Class<? extends StringKeyValueHeader> clazz)
	{
		this(262144, Delimiters.lineDelimiter());
		
		mClass = clazz;
	}

	public StringSyncDecoder(int maxFrameLength, boolean stripDelimiter,
			boolean failFast, ChannelBuffer... delimiters) {
		super(maxFrameLength, stripDelimiter, failFast, delimiters);
	}

	public StringSyncDecoder(int maxFrameLength, boolean stripDelimiter,
			boolean failFast, ChannelBuffer delimiter) {
		super(maxFrameLength, stripDelimiter, failFast, delimiter);
	}

	public StringSyncDecoder(int maxFrameLength, boolean stripDelimiter,
			ChannelBuffer... delimiters) {
		super(maxFrameLength, stripDelimiter, delimiters);
	}

	public StringSyncDecoder(int maxFrameLength, boolean stripDelimiter,
			ChannelBuffer delimiter) {
		super(maxFrameLength, stripDelimiter, delimiter);
	}

	public StringSyncDecoder(int maxFrameLength, ChannelBuffer... delimiters) {
		super(maxFrameLength, delimiters);
	}

	public StringSyncDecoder(int maxFrameLength, ChannelBuffer delimiter) {
		super(maxFrameLength, delimiter);
	}
	
	public void resetState()
	{
		mState = State.READ_HEADER;
	}
	
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
			ChannelBuffer buffer) throws Exception {

		ChannelBuffer frame;
		switch (mState)
		{
		case READ_HEADER:
			frame = (ChannelBuffer) super.decode(ctx, channel, buffer);
			
			if (frame != null)
			{
				String s = frame.toString(CharsetUtil.UTF_8);
				if (!Utils.isStringEmpty(s))
				{
					String[] keyValue = s.split("\\=", 2);
					if (keyValue.length == 2)
						mHeaders.put(keyValue[0], keyValue[1]);
				}
				else
				{
					mState = State.READ_CONTENT;
					Constructor<? extends StringKeyValueHeader> ctor = mClass.getDeclaredConstructor(Map.class);
					return ctor.newInstance(mHeaders);
				}
			}
			
			break;
		case READ_CONTENT:
			byte[] chunked = new byte[buffer.readableBytes()];
			buffer.readBytes(chunked);
			return new ChunkedData(chunked.length, chunked);
		}
		
		return null;
	}
}
