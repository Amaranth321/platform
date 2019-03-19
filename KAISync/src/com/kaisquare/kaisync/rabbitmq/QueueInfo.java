package com.kaisquare.kaisync.rabbitmq;

import com.google.gson.annotations.SerializedName;

public class QueueInfo {
	
	private String name;
	private String vhost;
	private boolean durable;
	@SerializedName("auto_delete")
	private boolean autoDelete;
	private String node;
	private int memory;
	private int messages;
	@SerializedName("messages_ready")
	private int messageReady;
	@SerializedName("messages_unacknowledged")
	private int messageUnacknowledged;
	@SerializedName("messages_ram")
	private int messageRam;
	@SerializedName("messages_ready_ram")
	private int messageReadyRam;
	@SerializedName("messages_persistent")
	private int messagePersistent;
	@SerializedName("message_bytes")
	private int messageBytes;
	@SerializedName("message_bytes_ready")
	private int messageReadyBytes;
	@SerializedName("message_bytes_unacknowledged")
	private int messageUnacknowledgedBytes;
	@SerializedName("message_bytes_ram")
	private int messageRamBytes;
	@SerializedName("message_bytes_persistent")
	private int messagePersistentBytes;
	private int consumers;
	private String state;
	
	public String getName() {
		return name;
	}
	
	public String getVhost() {
		return vhost;
	}
	
	public boolean isDurable() {
		return durable;
	}
	
	public boolean isAutoDelete() {
		return autoDelete;
	}
	
	public String getNode() {
		return node;
	}
	
	public int getMemory() {
		return memory;
	}
	
	public int getMessages() {
		return messages;
	}
	
	public int getMessageReady() {
		return messageReady;
	}
	
	public int getMessageUnacknowledged() {
		return messageUnacknowledged;
	}
	
	public int getMessageRam() {
		return messageRam;
	}
	
	public int getMessageReadyRam() {
		return messageReadyRam;
	}
	
	public int getMessagePersistent() {
		return messagePersistent;
	}
	
	public int getMessageBytes() {
		return messageBytes;
	}
	
	public int getMessageReadyBytes() {
		return messageReadyBytes;
	}
	
	public int getMessageUnacknowledgedBytes() {
		return messageUnacknowledgedBytes;
	}
	
	public int getMessageRamBytes() {
		return messageRamBytes;
	}
	
	public int getMessagePersistentBytes() {
		return messagePersistentBytes;
	}
	
	public int getConsumers() {
		return consumers;
	}
	
	public String getState() {
		return state;
	}

}
