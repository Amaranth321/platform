package com.kaisquare.kaisync.rabbitmq;

import com.google.gson.annotations.SerializedName;

public class ExchangeInfo {
	
	private String name;
	private String vhost;
	private String type;
	private boolean durable;
	@SerializedName("auto_delete")
	private boolean autoDelete;
	private boolean internal;
	
	public String getName() {
		return name;
	}
	
	public String getVhost() {
		return vhost;
	}
	
	public String getType() {
		return type;
	}
	
	public boolean isDurable() {
		return durable;
	}
	
	public boolean isAutoDelete() {
		return autoDelete;
	}
	
	public boolean isInternal() {
		return internal;
	}

}
