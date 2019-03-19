package platform.devices;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.json.JSONObject;
import play.Logger;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author Ray Hsu
 *
 * This class helps user discover devices in local area network (Node use only)
 */
public abstract class DeviceDiscovery {
	
	//The Model ID should be equal to Core's model id
	public static final int MODEL_AMEGIA = 102;
	
	protected final static String TYPE_MESSAGE = "message";
	protected final static String TYPE_DISCOVERY = "discovery";
	private static DeviceComparator comparator = new DeviceComparator();
	
	protected Map<String, DiscoveredDevice> discoveredDevices = new HashMap<String, DeviceDiscovery.DiscoveredDevice>();
	private volatile boolean started = false;
	
	public DeviceDiscovery() {
	}
	
	public List<DiscoveredDevice> getDiscoveredDevices()
	{
		List<DiscoveredDevice> list = new ArrayList<DeviceDiscovery.DiscoveredDevice>();
		synchronized (discoveredDevices) {
			Iterator<Entry<String, DiscoveredDevice>> iterator = discoveredDevices.entrySet().iterator();
			while (iterator.hasNext()) 
			{
				list.add(iterator.next().getValue());
			}
		}
		Collections.sort(list, comparator);
		return list;
	}
	
	protected void convertDiscoveryOutput(String line)
	{
		try {
			JSONObject json = new JSONObject(line);
			String type = (String)json.get("type");
			if (TYPE_MESSAGE.equals(type))
				Logger.info("%s: %s", this.getClass().getSimpleName(), json.get("message"));
			else if (TYPE_DISCOVERY.equals(type))
			{
				String deviceJson = json.get("device").toString();
				Gson gson = new Gson();
				DiscoveredDevice device = gson.fromJson(deviceJson, DiscoveredDevice.class);
				putDevice(device);
			}
		} catch (Exception e) {
			Logger.error(e, this.getClass().getSimpleName());
		}
	}
	
	protected final void putDevice(DiscoveredDevice device)
	{
		synchronized (discoveredDevices) {
			device.modelId = getDeviceModelId();
			discoveredDevices.put(device.macAddress, device);
		}
	}
	
	public final void start()
	{
		started = true;
		startAutoDiscovery();
	}
	
	public final void stop()
	{
		stopAutoDiscovery();
		started = false;
	}
	
	public boolean isStarted()
	{
		return started;
	}
	
	/**
	 * The device model id of this discovery
	 * @return model id
	 */
	protected abstract int getDeviceModelId();

	/**
	 * Start discovering devices
	 */
	protected abstract void startAutoDiscovery();
	/**
	 * Stop discovering devices 
	 */
	protected abstract void stopAutoDiscovery();
	
	public static class DiscoveredDevice
	{
		public int modelId;
		@SerializedName("ipaddress")
		public String ipAddress;
		@SerializedName("mac_addr")
		public String macAddress;
		@SerializedName("devicename")
		public String deviceName;
		@SerializedName("devicelocation")
		public String deviceLocation;
		@SerializedName("firmware_version")
		public String firmwareVersion;
		public String model;
		@SerializedName("http_port")
		public int httpPort;
		@SerializedName("rtsp_port")
		public int rtspPort;
	}
	
	private static class DeviceComparator implements Comparator<DiscoveredDevice>
	{

		@Override
		public int compare(DiscoveredDevice o1, DiscoveredDevice o2) {
			return o1.ipAddress.compareTo(o2.ipAddress);
		}
		
	}
}