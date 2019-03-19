package com.kaisquare.kaisync.platform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.kaisquare.kaisync.file.IServerSyncFile;
import com.kaisquare.kaisync.utils.AppLogger;
import com.kaisquare.kaisync.utils.Utils;

public class MessagePacket implements Iterable<Entry<String, MessagePacket.PacketDataHelper>> {

	public static final String FIELD_VERSION = "_version";
	public static final String FIELD_STATUS = "_status";
	public static final String FIELD_REASON = "_reason";

	public static final String FIELD_FUNC = "func";
	public static final String FIELD_IDENTIFIER = "identifier";
	public static final String FIELD_MACADDRESS = "macaddress";
	
	public static final int STATUS_OK = 0;
	public static final int STATUS_FAILED = 1;
	public static final int STATUS_ERROR = 99;
	
	static final byte EQUAL_SIGN = 61;
	static final int VERSION = 1;
	private static final ByteOrder DEFAULT_ENDIAN = ByteOrder.LITTLE_ENDIAN;
	
	private ByteBuffer buffer;
	private ByteArrayOutputStream byteArray;
	private LinkedHashMap<String, String> map;
	
	public MessagePacket()
	{
		byteArray = new ByteArrayOutputStream();
		map = new LinkedHashMap<String, String>();
		putAsBytes(FIELD_VERSION, Integer.toString(VERSION));
	}
	
	public MessagePacket(byte[] body)
	{
		load(body);
	}

	public void load(byte[] body) {
		if (body != null)
		{
			buffer = ByteBuffer.wrap(body);
			buffer.order(DEFAULT_ENDIAN);
			buffer.flip();
		}
		else
			AppLogger.w(this, "tried to load bytes, but it's null");
	}
	
	private void checkReadOnly()
	{
		if (map == null)
			throw new IllegalStateException("the packet is a read only object");
	}
	
	public String put(String key, String value)
	{
		checkReadOnly();
		return map.put(key, value);
	}
	
	public String put(String key, int value)
	{
		checkReadOnly();
		return put(key, Integer.toString(value));
	}
	
	public String put(String key, long value)
	{
		checkReadOnly();
		return put(key, Long.toString(value));
	}
	
	public void put(IServerSyncFile syncFile)
	{
		put("fileid", syncFile.getID());
		put("length", syncFile.getSize());
		put("host", syncFile.getHost());
		put("port", syncFile.getPort());
		put("createdtime", syncFile.getCreatedDate().getTime());
		put("hash", syncFile.getHash());
	}
	
	public String remove(String key)
	{
		checkReadOnly();
		return map.remove(key);
	}
	
	/**
	 * Put the key and value into the bytes directly, it's not removable, if you need to change the value before converting into bytes
	 * please use {@link MessagePacket#put(String, String)} instead.
	 * @param key
	 * @param value
	 * @return true if it's able to write into byte array
	 */
	public boolean putAsBytes(String key, String value)
	{
		checkReadOnly();
		checkKeyFormat(key);
	
		String s = String.format("%s=%s", key, Utils.isStringEmpty(value) ? "" : value);
		return putAsBytes(convertToBytes(s));
	}
	
	public boolean putBytes(String key, byte[] bytes)
	{
		checkReadOnly();
		checkKeyFormat(key);
		if (bytes == null)
			throw new NullPointerException("bytes is empty");
		
		byte[] keyBytes = convertToBytes(key + "=");
		byte[] body = new byte[keyBytes.length + bytes.length];
		System.arraycopy(keyBytes, 0, body, 0, keyBytes.length);
		System.arraycopy(bytes, 0, body, keyBytes.length, bytes.length);
		return putAsBytes(body);
	}

	private boolean putAsBytes(byte[] bytes) {
		if (bytes == null || bytes.length == 0)
			return false;
		
		boolean ret = true;
		byte[] body = new byte[bytes.length + 4];
		ByteBuffer bb = ByteBuffer.wrap(body);
		bb.order(DEFAULT_ENDIAN);
		bb.putInt(bytes.length);
		bb.put(bytes);
		
		try {
			byteArray.write(body);
		} catch (IOException e) {
			AppLogger.e(this, e, "");
			ret  = false;
		}
		
		return ret;
	}

	private void checkKeyFormat(String key) {
		if (Utils.isStringEmpty(key))
			throw new IllegalArgumentException("empty key");
		else if (key.endsWith("="))
			throw new IllegalArgumentException("invalid string format, should not end with '='");
	}
	
	private byte[] convertToBytes(String s) {
		byte[] bytes = null;
		try {
			bytes = s.getBytes("utf8");
		} catch (UnsupportedEncodingException e) {
			AppLogger.e(this, e, "error converting to bytes: %s", s);
		}
		return bytes;
	}
	
	public Map<String, PacketDataHelper> toMap()
	{
		NullSafeLinkedHashMap byteMap = new NullSafeLinkedHashMap();
		if (buffer == null)
			return byteMap;
		
		Iterator<Entry<String, PacketDataHelper>> iterator = iterator();
		while (iterator.hasNext())
		{
			Entry<String, PacketDataHelper> entry = iterator.next();
			byteMap.put(entry.getKey(), entry.getValue());
		}
		
		return byteMap;
	}

	public byte[] toBytes()
	{
		if (byteArray != null)
		{
			Iterator<Entry<String, String>> iterator = map.entrySet().iterator();
			while (iterator.hasNext())
			{
				Entry<String, String> entry = iterator.next();
				putAsBytes(entry.getKey(), entry.getValue());
			}
			return byteArray.toByteArray();
		}
		else if (buffer != null)
			return buffer.array();
		else
			return null;
	}

	@Override
	public Iterator<Entry<String, PacketDataHelper>> iterator() {
		return new PacketIterator(buffer.array());
	}
	
	public static Object readObject(byte[] bytes)
	{
		if (bytes == null)
			return null;
		
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
			return ois.readObject();
		} catch (Exception e) {
			AppLogger.e("MessagePacket", e, "");
		} 
		
		return null;
	}
	
	public static byte[] writeObject(Object obj)
	{
		byte[] bytes = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.flush();
			
			bytes = baos.toByteArray();
			oos.reset();
			oos.close();
		} catch (IOException e) {
			AppLogger.e("MessagePacket", e, "");
		}
		
		return bytes;
	}
	
	static class PacketIterator implements Iterator<Entry<String, PacketDataHelper>>
	{
		private ByteBuffer buffer;
		
		public PacketIterator(byte[] body)
		{
			buffer = ByteBuffer.wrap(body);
			buffer.order(DEFAULT_ENDIAN);
		}

		@Override
		public boolean hasNext() {
			return buffer.remaining() > 4;
		}

		@Override
		public Entry<String, PacketDataHelper> next() {
			PacketEntry entry;
			int size = buffer.getInt();
			if (size > 0)
			{
				byte[] buf = new byte[size];
				buffer.get(buf);
				String key = "";
				byte[] value = null;
				int i = 0;
				while (i < buf.length) {
					if (buf[i++] == EQUAL_SIGN)
					{
						key = new String(buf, 0, i - 1);
						value = new byte[buf.length - i];
						System.arraycopy(buf, i, value, 0, value.length);
						break;
					}
				}
				entry = new PacketEntry(key, value);
			}
			else
				entry = new PacketEntry("", null);
			
			return entry;
		}

		@Override
		public void remove() {
			//not available
		}
		
	}
	
	static class PacketEntry implements Map.Entry<String, PacketDataHelper>
	{
		private String key;
		private PacketDataHelper value;
		
		PacketEntry(String key, byte[] value)
		{
			this.key = key;
			this.value = new PacketDataHelper(value);
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public PacketDataHelper getValue() {
			return value;
		}

		@Override
		public PacketDataHelper setValue(PacketDataHelper value) {
			PacketDataHelper oldValue = this.value;
			this.value = value;
			return oldValue; 
		}
		
	}
	
	public static class PacketDataHelper
	{
		private String str;
		private byte[] data;
		
		public PacketDataHelper(byte[] b)
		{
			data = b;
		}
		
		public int readAsInt()
		{
			String s = readAsString();
			if (Pattern.matches("^[\\-]?\\d+$", s))
				return Integer.parseInt(s);
			else
				return 0;
		}
		
		public long readAsLong()
		{
			String s = readAsString();
			if (Pattern.matches("^[\\-]?\\d+$", s))
				return Long.parseLong(s);
			else
				return 0;
		}

		public synchronized String readAsString() {
			if (data == null)
				return "".intern();
			if (str == null)
				str = new String(data);
			
			return str;
		}
		
		public byte[] getRaw()
		{
			return data;
		}
	}
}
