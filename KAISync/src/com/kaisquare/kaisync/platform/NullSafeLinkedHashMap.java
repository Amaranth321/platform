package com.kaisquare.kaisync.platform;

import java.util.LinkedHashMap;

public class NullSafeLinkedHashMap extends LinkedHashMap<String, MessagePacket.PacketDataHelper> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6022138101138544455L;

	@Override
	public MessagePacket.PacketDataHelper get(Object key) {
		MessagePacket.PacketDataHelper helper = super.get(key);
		if (helper == null)
			helper = new MessagePacket.PacketDataHelper(null);
		
		return helper;
	}

}
