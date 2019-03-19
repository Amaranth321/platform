package com.kaisquare.kaisync.transport;

import org.jboss.netty.channel.*;

import com.kaisquare.kaisync.file.ChunkedData;

import java.io.*;

public class StreamChunkAggregator extends SimpleChannelUpstreamHandler {

    private volatile OutputStream out;

    /**
     * Creates a new instance.
     */
    public StreamChunkAggregator() { }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (!(msg instanceof ChunkedData)) {
            ctx.sendUpstream(e);
            return;
        }

        ChunkedData chunkData = (ChunkedData) msg;
        if (out == null) {
        	PipedInputStream messageInput = new PipedInputStream();
            out = new PipedOutputStream(messageInput);
            Channels.fireMessageReceived(ctx, messageInput, e.getRemoteAddress());
        }
        out.write(chunkData.getData());
    }

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if (out != null)
		{
			try {
				out.flush();
				out.close();
			} catch (IOException e1) {}
		}
		out = null;
		
		super.channelClosed(ctx, e);
	}
}

