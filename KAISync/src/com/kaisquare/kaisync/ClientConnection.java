package com.kaisquare.kaisync;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.codec.binary.Base64;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import org.jboss.netty.handler.codec.http.multipart.FileUpload;
import org.jboss.netty.handler.codec.http.multipart.HttpDataFactory;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import org.jboss.netty.handler.stream.ChunkedStream;

import com.kaisquare.kaisync.utils.AppLogger;

public class ClientConnection extends SimpleChannelUpstreamHandler implements ChannelHandler {

	private final Object MUTEX = new Object();
	private ClientBootstrap bootstrap;
	private HttpDataFactory factory;
	private volatile HttpResponse response;
	private CountDownLatch latch;
	private boolean readingChunks;
	private Channel channel;
	private ChannelBuffer chunkedBuffer;
	private ChunkedContentListener listener;
	private Map<String, Object> additionalHeaders;
	private String authorization;
	private String method = "GET";
	private int timeout = 60000;
	
	public ClientConnection()
	{
		this(2097152);
	}
	
	public ClientConnection(int dataMemory)
	{
		bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));
		bootstrap.setPipelineFactory(new HttpClientPipelineFactory(this));
		bootstrap.setOption("child.tcpNoDelay", true);
		factory = new DefaultHttpDataFactory(dataMemory);
		additionalHeaders = new HashMap<String, Object>();
	}
	
	public ClientConnection setChunkedContentListener(ChunkedContentListener listener)
	{
		this.listener = listener;
		return this;
	}
	
	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}
	
	public ClientConnection setMethod(String method)
	{
		this.method = method;
		return this;
	}

	public ClientConnection setAuthorization(String username, String password) {
		authorization = "Basic " + Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes()).replaceAll("\n",  "").replaceAll("\r", "");
		return this;
	}
	
	public ClientConnection addHeader(String name, Object value)
	{
		additionalHeaders.put(name, value);
		return this;
	}
	
	public boolean isConnected()
	{
		return channel != null;
	}
	
	private CountDownLatch newCountDown() throws IOException
	{
		synchronized (MUTEX) {
			if (channel == null && latch == null)
				return new CountDownLatch(1);
			else
				throw new IOException("multi-thread call not supported");
		}
	}
	
	public HttpResponse get(String url) throws IOException, TimeoutException
	{
		latch = newCountDown();
		URI uri = URI.create(url);
		InetSocketAddress socketAddress = new InetSocketAddress(uri.getHost(), uri.getPort() <= 0 ? 80 : uri.getPort());
		try {
			channel = bootstrap.connect(socketAddress).sync().getChannel();
			HttpRequest request = newRequest(getMethod(method, false), uri);
	        channel.write(request).sync();
	        if (!latch.await(timeout, TimeUnit.MILLISECONDS))
	        	throw new TimeoutException("request timeout in " + timeout + "ms");
		} catch (InterruptedException e) {
		} catch (TimeoutException e) {
			throw e;
		} catch (Exception e) {
			if (channel != null)
				channel.close();
			throw new IOException(e);
		} finally {
			try {
				if (channel != null)
					channel.getCloseFuture().syncUninterruptibly();
			} catch (Exception e) {}
			latch = null;
			close();
		}
		
		return response;
	}
	
	public HttpResponse post(String url, Map<String, String> postData) throws IOException, TimeoutException
	{
		return post(url, postData, null, null);
	}
	
	public HttpResponse post(String url, File file, String fileContentType) throws IOException, TimeoutException
	{
		return post(url, null, file, fileContentType);
	}
	
	public HttpResponse post(String url, Map<String, String> postData, File file, String fileContentType) throws IOException, TimeoutException
	{
		latch = newCountDown();
		URI uri = URI.create(url);
		InetSocketAddress socketAddress = new InetSocketAddress(uri.getHost(), uri.getPort() <= 0 ? 80 : uri.getPort());
		try {
			channel = bootstrap.connect(socketAddress).sync().getChannel();
			HttpRequest request = newRequest(getMethod(method, true), uri);
			
			HttpPostRequestEncoder body =
	                new HttpPostRequestEncoder(factory, request, file != null && file.exists());
			
			if (postData != null)
			{
				Iterator<Entry<String, String>> iterator = postData.entrySet().iterator();
				while (iterator.hasNext())
				{
					Entry<String, String> entry = iterator.next();
					body.addBodyAttribute(entry.getKey(), entry.getValue());
				}
			}
			if (file != null && file.exists())
				body.addBodyFileUpload("file", file, fileContentType, false);
			
			request = body.finalizeRequest();
	        channel.write(request);
	        
	        if (body.isChunked())
	        	channel.write(body).sync();
	        
	        if (!latch.await(timeout, TimeUnit.MILLISECONDS))
	        	throw new TimeoutException("request timeout in " + timeout + "ms");
	        body.cleanFiles();
		} catch (InterruptedException e) {
		} catch (TimeoutException e) {
			throw e;
		} catch (Exception e) {
			if (channel != null)
				channel.close();
			throw new IOException(e);
		} finally {
			try {
				if (channel != null)
					channel.getCloseFuture().syncUninterruptibly();
			} catch (Exception e) {}
			latch = null;
			close();
		}
		
		return response;
	}
	
	public HttpResponse post(String url, String name, String contentType, long contentLength, InputStream in) throws IOException, TimeoutException
	{
		if (in == null)
			throw new NullPointerException("empty InputStream");
		if (isStringEmpty(contentType))
			throw new NullPointerException("empty contentType");
		if (contentLength <= 0)
			throw new IllegalArgumentException("contentLength <= 0");
		
		latch = newCountDown();
		URI uri = URI.create(url);
		InetSocketAddress socketAddress = new InetSocketAddress(uri.getHost(), uri.getPort() <= 0 ? 80 : uri.getPort());
		FileUpload fileUpload = null;
		try {
			channel = bootstrap.connect(socketAddress).sync().getChannel();
			HttpRequest request = newRequest(getMethod(method, true), uri);
			
			HttpPostRequestEncoder body =
	                new HttpPostRequestEncoder(factory, request, true);
			
			fileUpload = factory.createFileUpload(request, name, "file", contentType, HttpHeaders.Values.BINARY, null, contentLength);
			fileUpload.setContent(in);
			body.addBodyHttpData(fileUpload);
			
			request = body.finalizeRequest();
	        channel.write(request);
	        
	        if (body.isChunked())
	        	channel.write(body).sync();
	        
	        if (!latch.await(timeout, TimeUnit.MILLISECONDS))
	        	throw new TimeoutException("request timeout in " + timeout + "ms");
	        body.cleanFiles();
		} catch (InterruptedException e) {
		} catch (TimeoutException e) {
			throw e;
		} catch (Exception e) {
			if (channel != null)
				channel.close();
			throw new IOException(e);
		} finally {
			try {
				if (channel != null)
					channel.getCloseFuture().syncUninterruptibly();
			} catch (Exception e) {}
			
			try {
				in.close();
			} catch (Exception e) {}
			
			if (fileUpload != null)
				fileUpload.delete();
			latch = null;
			close();
		}
		
		return response;
	}
	
	private boolean isStringEmpty(String str) {
		return str == null || "".equals(str);
	}

	public HttpResponse post(String url, InputStream in, long length) throws IOException
	{
		latch = newCountDown();
		URI uri = URI.create(url);
		InetSocketAddress socketAddress = new InetSocketAddress(uri.getHost(), uri.getPort() <= 0 ? 80 : uri.getPort());
		
		try {
			channel = bootstrap.connect(socketAddress).sync().getChannel();
			HttpRequest request = newRequest(getMethod(method, true), uri);
			HttpHeaders.setHeader(request, HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED);
			HttpHeaders.setHeader(request, HttpHeaders.Names.CONTENT_LENGTH, length);
			channel.write(request);
			channel.write(new ChunkedStream(in)).sync();
			if (!latch.await(timeout, TimeUnit.MILLISECONDS))
	        	throw new TimeoutException("request timeout in " + timeout + "ms");
		} catch (InterruptedException e) {
		} catch (Exception e) {
			if (channel != null)
				channel.close();
			throw new IOException(e);
		} finally {
			try {
				if (channel != null)
					channel.getCloseFuture().syncUninterruptibly();
			} catch (Exception e) {}
			
			try {
				in.close();
			} catch (Exception e) {}
			latch = null;
			close();
		}
		
		return response;
	}
	
	public HttpMethod getMethod(String method, boolean hasData)
	{
		HttpMethod m = HttpMethod.valueOf(method);
		return hasData && m == HttpMethod.GET ? HttpMethod.POST : m;
	}

	private HttpRequest newRequest(HttpMethod method, URI uri) {
		String path = uri.getRawPath();
		String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";
		HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, path + query);
		HttpHeaders.setHeader(request, HttpHeaders.Names.HOST, uri.getHost());
		HttpHeaders.setHeader(request, HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        HttpHeaders.setHeader(request, HttpHeaders.Names.ACCEPT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        HttpHeaders.setHeader(request, HttpHeaders.Names.USER_AGENT, "Client connection agent");
        
        if (authorization != null && !"".equalsIgnoreCase(authorization))
        	HttpHeaders.setHeader(request, HttpHeaders.Names.AUTHORIZATION, authorization);
        
        Iterator<Entry<String, Object>> iterator = additionalHeaders.entrySet().iterator();
        while (iterator.hasNext())
        {
        	Entry<String, Object> entry = iterator.next();
        	HttpHeaders.setHeader(request, entry.getKey(), entry.getValue());
        }
        
        return request;
	}

	public synchronized void close()
	{
		try {
			listener = null;
			if (channel != null)
				channel.close().syncUninterruptibly();
		} catch (Exception e) {}
		try {
			if (bootstrap != null)
			{
				bootstrap.releaseExternalResources();
				bootstrap = null;
			}
		} catch (Exception e) {}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (!readingChunks) {
            response = (HttpResponse) e.getMessage();

            if (response.isChunked()) {
                readingChunks = true;
                if (listener == null)
                {
                    response.setChunked(false);
                    response.removeHeader(HttpHeaders.Names.TRANSFER_ENCODING);
                	chunkedBuffer = ChannelBuffers.dynamicBuffer();
                }
                else
                	listener.onReceivedChunk(response, e.getChannel(), response.getContent());
            } else
            {
            	if (listener != null)
            		listener.onReceivedChunk(response, e.getChannel(), response.getContent());
            	latch.countDown();
            }
        } else {
            HttpChunk chunk = (HttpChunk) e.getMessage();
            if (chunk.isLast()) {
                readingChunks = false;
                if (listener == null)
                	response.setContent(chunkedBuffer);
                latch.countDown();
            } else {
            	if (listener != null)
            		listener.onReceivedChunk(response, e.getChannel(), chunk.getContent());
            	else if (chunkedBuffer != null)
            		chunkedBuffer.writeBytes(chunk.getContent());
            }
        } 		
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if (latch != null)
			latch.countDown();
		channel = null;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		if (e.getCause() != null && !(e.getCause() instanceof ClosedChannelException))
			AppLogger.e(this, e.getCause(), "");
		
		e.getChannel().close();
	}
	
	public interface ChunkedContentListener
	{
		public void onReceivedChunk(HttpResponse response, Channel src, ChannelBuffer buffer);
	}
}
