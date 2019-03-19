/*
 * DeviceServerException.java
 *
 * Copyright (C) KAI Square Pte Ltd
 */

package com.kaisquare.kaisync.utils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.thrift.TProcessor;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * Convenience class containing Apache Thrift related utility methods.
 *
 * @author Tan Yee Fan
 */
public class ThriftUtil {
	/** Maximum Thrift server read buffer size. */
	private static final int SERVER_MAX_READ_BUFFER_SIZE = 64 * 1024;

	/**
	 * Private constructor.
	 */
	private ThriftUtil() {
	}

	/**
	 * Constructs a new Thrift service server and returns it. The returned
	 * server will be ready to serve Thrift service clients.
	 *
	 * @param processor Thrift processor.
	 * @param serviceServerPort Thrift service server port.
	 * @throws TTransportException If an error occurred when setting up the
	 *         server.
	 */
	public static TServer newServiceServer(TProcessor processor, int serviceServerPort) throws TTransportException {
		TNonblockingServerSocket socket = new TNonblockingServerSocket(serviceServerPort);
		TNonblockingServer.Args serverArgs = new TNonblockingServer.Args(socket);
		serverArgs.processor(processor);
		serverArgs.protocolFactory(new TBinaryProtocol.Factory());
		serverArgs.maxReadBufferBytes = SERVER_MAX_READ_BUFFER_SIZE;
		final TServer server = new TNonblockingServer(serverArgs);
		Thread thread = new Thread(){
			@Override
			public void run() {
				server.serve();
			}
		};
		thread.start();
		return server;
	}

	/**
	 * Constructs a new secure Thrift service server and returns it. The returned
	 * server will be ready to serve Thrift service clients.
	 * 
	 * @param processor Thrift processor.
	 * @param serviceServerPort Thrift service server port.
	 * @param keystore path to keystore
	 * @param keypass password of keystore
	 * @return
	 * @throws TTransportException
	 */
	public static TServer newServiceSSLServer(TProcessor processor, int serviceServerPort, String keystore, String keypass) throws TTransportException {
		TSSLTransportFactory.TSSLTransportParameters params = new TSSLTransportFactory.TSSLTransportParameters();
		params.setKeyStore(keystore, keypass);
		TServerSocket socket;
		File file = new File(keystore);
		if (!file.canRead())
		{
			AppLogger.e("SSL Server", "could not read keystore file '%s'", keystore);
			throw new TTransportException("could not read keystore file");
		}
		socket = TSSLTransportFactory.getServerSocket(
				serviceServerPort, 15000, null, params);
		TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(socket);
		serverArgs.processor(processor);
		serverArgs.protocolFactory(new TBinaryProtocol.Factory());
		//		serverArgs.maxReadBufferBytes = SERVER_MAX_READ_BUFFER_SIZE;
		final TServer server = new TThreadPoolServer(serverArgs);
		Thread thread = new Thread(){
			@Override
			public void run() {
				server.serve();
			}
		};
		thread.start();
		return server;
	}

	/**
	 * Constructs a new Thrift service client and returns it. The returned
	 * client will be ready to connect to the Thrift service server.
	 *
	 * @param serviceIfaceClass Thrift service interface class, as in
	 *        {@code ExampleService.Iface.class}.
	 * @param serviceClientClass Thrift service client class, as in
	 *        {@code ExampleService.Client.class}.
	 * @param serviceServerHost Thrift server hostname or IP address.
	 * @param serviceServerPort Thrift server port.
	 * @param socketTimeout Thrift socket timeout, in milliseconds.
	 * @throws TTransportException If an error occurred when setting up the
	 *         client.
	 */
	public static <T> Client<T> newServiceClient(Class<T> serviceIfaceClass, Class<? extends T> serviceClientClass,
			String serviceServerHost, int serviceServerPort, int socketTimeout) throws TTransportException {
		return newServiceClient(serviceIfaceClass, serviceClientClass,
				serviceServerHost, serviceServerPort, socketTimeout, 1, 0);
	}

	/**
	 * Constructs a new Thrift service client and returns it. The returned
	 * client will be ready to connect to the Thrift service server.
	 *
	 * @param serviceIfaceClass Thrift service interface class, as in
	 *        {@code ExampleService.Iface.class}.
	 * @param serviceClientClass Thrift service client class, as in
	 *        {@code ExampleService.Client.class}.
	 * @param serviceServerHost Thrift server hostname or IP address.
	 * @param serviceServerPort Thrift server port.
	 * @param socketTimeout Thrift socket timeout, in milliseconds.
	 * @param numTries Number of tries to attempt a remote procedure call
	 *        before giving up.
	 * @param retryDelay Delay between two successive tries, in
	 *        milliseconds.
	 * @throws TTransportException If an error occurred when setting up the
	 *         client.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Client<T> newServiceClient(Class<T> serviceIfaceClass, Class<? extends T> serviceClientClass,
			String serviceServerHost, int serviceServerPort, int socketTimeout, int numTries, int retryDelay) throws TTransportException {
		if (!serviceIfaceClass.isInterface() || serviceIfaceClass == TServiceClient.class)
			throw new TTransportException("Not a Thrift service interface class.");
		if (serviceClientClass.isInterface() || !TServiceClient.class.isAssignableFrom(serviceClientClass))
			throw new TTransportException("Not a Thrift service client class.");
		InnerClient<T> client = new InnerClient<T>();
		Handler<T> handler = new Handler<T>(client, serviceIfaceClass, serviceClientClass, serviceServerHost, serviceServerPort, socketTimeout, numTries, retryDelay);
		T iface = (T)Proxy.newProxyInstance(serviceClientClass.getClassLoader(), new Class[]{serviceIfaceClass}, handler);
		client.setIface(iface);
		return client;
	}

	public static <T> Client<T> newServiceSSLClient(Class<T> serviceIfaceClass, Class<? extends T> serviceClientClass,
			String serviceServerHost, int serviceServerPort, int socketTimeout, int numTries, int retryDelay,
			String truststore, String keypass) throws TTransportException {
		if (!serviceIfaceClass.isInterface() || serviceIfaceClass == TServiceClient.class)
			throw new TTransportException("Not a Thrift service interface class.");
		if (serviceClientClass.isInterface() || !TServiceClient.class.isAssignableFrom(serviceClientClass))
			throw new TTransportException("Not a Thrift service client class.");
		File file = new File(truststore);
		if (!file.canRead())
			AppLogger.e("SSL Server", "can not read truststore file '%s'", truststore);
		InnerClient<T> client = new InnerClient<T>();
		Handler<T> handler = new Handler<T>(client, serviceIfaceClass, serviceClientClass, serviceServerHost, serviceServerPort, socketTimeout, numTries, retryDelay, truststore, keypass);
		T iface = (T)Proxy.newProxyInstance(serviceClientClass.getClassLoader(), new Class[]{serviceIfaceClass}, handler);
		client.setIface(iface);
		return client;
	}

	/**
	 * Instantiates a new Thrift service client and returns it. The returned
	 * client will be ready to connect to the Thrift service server.
	 *
	 * @param serviceIfaceClass Thrift service interface class, as in
	 *        {@code ExampleService.Iface.class}.
	 * @param serviceClientClass Thrift service client class, as in
	 *        {@code ExampleService.Client.class}.
	 * @param serviceServerHost Thrift server hostname or IP address.
	 * @param serviceServerPort Thrift server port.
	 * @param socketTimeout Thrift socket timeout, in milliseconds.
	 * @throws TTransportException If an error occurred when setting up the
	 *         client.
	 */
	private static <T> InnerClient<T> instantiateClient(Class<T> serviceIfaceClass, Class<? extends T> serviceClientClass,
			String serviceServerHost, int serviceServerPort, int socketTimeout) throws TTransportException {
		if (!serviceIfaceClass.isInterface() || serviceIfaceClass == TServiceClient.class)
			throw new TTransportException("Not a Thrift service interface class.");
		if (serviceClientClass.isInterface() || !TServiceClient.class.isAssignableFrom(serviceClientClass))
			throw new TTransportException("Not a Thrift service client class.");
		TTransport transport = null;
		try {
			Constructor<? extends T> constructor = serviceClientClass.getConstructor(TProtocol.class);
			TSocket socket = new TSocket(serviceServerHost, serviceServerPort, socketTimeout);
			transport = new TFramedTransport(socket);
			transport.open();
			TProtocol protocol = new TBinaryProtocol(transport);
			T iface = constructor.newInstance(protocol);
			InnerClient<T> client = new InnerClient<T>();
			client.setIface(iface);
			client.setTransport(transport);
			return client;
		}
		catch (TTransportException e) {
			if (transport != null)
				transport.close();
			throw e;
		}
		catch (Exception e) {
			if (transport != null)
				transport.close();
			throw new TTransportException("Unable to instantiate service client.", e);
		}
	}
	
	/**
	 * Instantiates a new Thrift SSL service client and returns it. The returned
	 * client will be ready to connect to the Thrift service server.
	 *
	 * @param serviceIfaceClass Thrift service interface class, as in
	 *        {@code ExampleService.Iface.class}.
	 * @param serviceClientClass Thrift service client class, as in
	 *        {@code ExampleService.Client.class}.
	 * @param serviceServerHost Thrift server hostname or IP address.
	 * @param serviceServerPort Thrift server port.
	 * @param socketTimeout Thrift socket timeout, in milliseconds.
	 * @param truststore The truststore file for instantiating SSL client
	 * @param keypass The password of truststore file
	 * @throws TTransportException If an error occurred when setting up the
	 *         client.
	 */
	private static <T> InnerClient<T> instantiateClient(Class<T> serviceIfaceClass, Class<? extends T> serviceClientClass,
			String serviceServerHost, int serviceServerPort, int socketTimeout, String truststore, String keypass) throws TTransportException {
		if (!serviceIfaceClass.isInterface() || serviceIfaceClass == TServiceClient.class)
			throw new TTransportException("Not a Thrift service interface class.");
		if (serviceClientClass.isInterface() || !TServiceClient.class.isAssignableFrom(serviceClientClass))
			throw new TTransportException("Not a Thrift service client class.");
		TTransport transport = null;
		try {
			Constructor<? extends T> constructor = serviceClientClass.getConstructor(TProtocol.class);
			TSSLTransportFactory.TSSLTransportParameters params = new TSSLTransportFactory.TSSLTransportParameters();
			params.setTrustStore(truststore, keypass);
			transport = TSSLTransportFactory.getClientSocket(serviceServerHost, serviceServerPort, socketTimeout, params);
//			transport = new TFramedTransport(socket);
//			transport.open();
			TProtocol protocol = new TBinaryProtocol(transport);
			T iface = constructor.newInstance(protocol);
			InnerClient<T> client = new InnerClient<T>();
			client.setIface(iface);
			client.setTransport(transport);
			return client;
		}
		catch (TTransportException e) {
			if (transport != null)
				transport.close();
			throw e;
		}
		catch (Exception e) {
			if (transport != null)
				transport.close();
			throw new TTransportException("Unable to instantiate service client.", e);
		}
	}

	/**
	 * A Thrift service client.
	 */
	public static interface Client<T> {
		/**
		 * Returns the Thrift service interface.
		 */
		public T getIface();

		/**
		 * Closes the client.
		 */
		public void close();
	}

	private static class InnerClient<T> implements Client<T> {
		private T iface;
		private TTransport transport;

		public InnerClient() {
			this.iface = null;
			this.transport = null;
		}

		@Override
		public T getIface() {
			return this.iface;
		}

		public void setIface(T iface) {
			this.iface = iface;
		}

		public TTransport getTransport() {
			return this.transport;
		}

		public void setTransport(TTransport transport) {
			this.transport = transport;
		}

		@Override
		public void close() {
			if (this.transport != null)
				this.transport.close();
		}
	}

	/**
	 * Invocation handler.
	 */
	private static class Handler<T> implements InvocationHandler {
		private InnerClient client;
		private Class<T> ifaceClass;
		private Class<? extends T> clientClass;
		private String host;
		private int port;
		private int numTries;
		private int timeout;
		private int delay;
		private T targetIface;
		private boolean usingSSL;
		private String truststore;
		private String keypass;

		public Handler(InnerClient<T> client, Class<T> serviceIfaceClass, Class<? extends T> serviceClientClass,
				String serviceServerHost, int serviceServerPort, int socketTimeout, int numTries, int retryDelay) {
			initHandler(
					client, serviceIfaceClass, serviceClientClass, serviceServerHost, serviceServerPort, socketTimeout, numTries,
					retryDelay, usingSSL, null, null);
		}
		
		public Handler(InnerClient<T> client, Class<T> serviceIfaceClass, Class<? extends T> serviceClientClass,
				String serviceServerHost, int serviceServerPort, int socketTimeout, int numTries, int retryDelay,
				String truststore, String keypass)
		{
			initHandler(client, serviceIfaceClass, serviceClientClass, serviceServerHost, serviceServerPort, socketTimeout, numTries,
			retryDelay, true, truststore, keypass);
		}
		
		private void initHandler(InnerClient<T> client, Class<T> serviceIfaceClass, Class<? extends T> serviceClientClass,
				String serviceServerHost, int serviceServerPort, int socketTimeout, int numTries, int retryDelay,
				boolean usingSSL, String truststore, String keypass)
		{
			this.client = client;
			this.ifaceClass = serviceIfaceClass;
			this.clientClass = serviceClientClass;
			this.host = serviceServerHost;
			this.port = serviceServerPort;
			this.timeout = socketTimeout;
			this.numTries = numTries;
			this.delay = retryDelay;
			this.targetIface = null;
			this.usingSSL = usingSSL;
			this.truststore = truststore;
			this.keypass = keypass;
		}

		@Override
		public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Throwable cause = null;
			for (int i = 0; i < this.numTries; i++) {
				if (this.targetIface == null) {
					try {
						InnerClient<T> client = null;
						if (usingSSL)
							client = instantiateClient(
									this.ifaceClass, this.clientClass, this.host, this.port, this.timeout, this.truststore, this.keypass);
						else
							client = instantiateClient(
									this.ifaceClass, this.clientClass, this.host, this.port, this.timeout);
						this.client.setTransport(client.getTransport());
						this.targetIface = client.getIface();
					}
					catch (TTransportException e) {
						cause = e;
					}
				}
				if (this.targetIface != null) {
					try {
						Object result = method.invoke(this.targetIface, args);
						return result;
					}
					catch (InvocationTargetException e) {
						cause = e.getCause();
						if (!(cause instanceof TTransportException))
							throw cause;
					}
				}
				this.client.close();
				this.targetIface = null;
				if (i < numTries - 1) {
					try {
						Thread.sleep(delay);
					}
					catch (InterruptedException e) {
					}
				}
			}
			throw cause;
		}
	}
}

