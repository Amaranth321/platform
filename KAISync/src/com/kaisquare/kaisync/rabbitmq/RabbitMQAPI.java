package com.kaisquare.kaisync.rabbitmq;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.kaisquare.kaisync.ClientConnection;

/**
 * An optional tool that we can use it for Rabbitmq management
 */
public class RabbitMQAPI {
	
	private String host;
	private int port;
	private String username;
	private String password;
	
	public RabbitMQAPI(String host, int port, String username, String password)
	{
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}
	
	public List<QueueInfo> listQueues() throws IOException, TimeoutException
	{
		String json = requestApi().get(getUrl("/api/queues")).getContent().toString(Charset.forName("utf8"));
		return new Gson().fromJson(json, new TypeToken<ArrayList<QueueInfo>>(){}.getType());
	}
	
	public boolean deleteQueue(String vhost, String queueName) throws IOException, TimeoutException
	{
		return requestApi()
				.setMethod("DELETE")
				.get(getUrl(String.format("/api/queues/%s/%s", URLEncoder.encode(vhost, "utf8"), URLEncoder.encode(queueName, "utf8"))))
				.getStatus().getCode() == HttpResponseStatus.NO_CONTENT.getCode();
	}
	
	public List<ExchangeInfo> listExchanges() throws IOException, JsonSyntaxException, TimeoutException
	{
		return new Gson().fromJson(requestApi().get(getUrl("/api/exchanges")).getContent().toString(Charset.forName("utf8")), new TypeToken<ArrayList<ExchangeInfo>>(){}.getType()); 
	}
	
	public boolean deleteExchange(String vhost, String exchange) throws IOException, TimeoutException
	{
		return requestApi()
				.setMethod("DELETE")
				.get(getUrl(String.format("/api/exchanges/%s/%s", URLEncoder.encode(vhost, "utf8"), URLEncoder.encode(exchange, "utf8"))))
				.getStatus().getCode() == HttpResponseStatus.NO_CONTENT.getCode();
	}
	
	private String getUrl(String api)
	{
		if (!api.startsWith("/"))
			api = String.format("/%s", api);
		return String.format("http://%s:%s%s", host, port, api);
	}
	
	ClientConnection requestApi()
	{
		ClientConnection conn = new ClientConnection();
		if (username != null && !"".equalsIgnoreCase(username))
			conn.setAuthorization(username, password);
		
		return conn;
	}

}
