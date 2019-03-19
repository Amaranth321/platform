package platform.devices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import platform.devices.DeviceDiscovery.DiscoveredDevice;
import play.Logger;

public class DeviceDiscoveryStarter {
	
	private static DeviceDiscoveryStarter instance;
	private static Map<Integer, Class<? extends DeviceDiscovery>> discoveries = new HashMap<Integer, Class<? extends DeviceDiscovery>>();
	private static Map<Integer, DeviceDiscovery> startedDiscoveries = new ConcurrentHashMap<Integer, DeviceDiscovery>();
	private ExecutorService executorService;
	
	static {
		discoveries.put(DeviceDiscovery.MODEL_AMEGIA, AmegiaDeviceDiscovery.class);
	}
	
	private DeviceDiscoveryStarter()
	{
		executorService = Executors.newFixedThreadPool(discoveries.size());
	}
	
	public synchronized void start(int modelId)
	{
		DeviceDiscovery discovery = null;
		
		discovery = startedDiscoveries.get(modelId);
		if (discovery != null)
		{
			if (!discovery.isStarted())
				discovery.start();
			
			return;
		}
		
		Class<? extends DeviceDiscovery> clazz = discoveries.get(modelId);
		if (clazz == null)
			throw new NullPointerException("No device auto-discovery for model: " + modelId);
		
		try {
			discovery = clazz.newInstance();
			executorService.execute(new DiscoveryThread(discovery));
			synchronized (startedDiscoveries) {
				startedDiscoveries.put(modelId, discovery);
			}
		} catch (Exception e) {
			Logger.error(e, this.getClass().getSimpleName());
		}
	}
	
	public void stop(int modelId)
	{
		DeviceDiscovery discovery = startedDiscoveries.get(modelId);
		if (discovery != null)
		{
			discovery.stop();
			startedDiscoveries.remove(modelId);
		}		
	}
	
	public void stopAll()
	{
		synchronized (startedDiscoveries) {
			Iterator<Entry<Integer, DeviceDiscovery>> iterator = startedDiscoveries.entrySet().iterator();
			while (iterator.hasNext())
			{
				iterator.next().getValue().stop();
				iterator.remove();
			}
		}
	}
	
	public List<DiscoveredDevice> getDiscoveredDevices()
	{
		List<DiscoveredDevice> list = new ArrayList<DeviceDiscovery.DiscoveredDevice>();
		synchronized (startedDiscoveries) {
			Iterator<Entry<Integer, DeviceDiscovery>> iterator = startedDiscoveries.entrySet().iterator();
			while (iterator.hasNext())
			{
				DeviceDiscovery discovery = iterator.next().getValue();
				list.addAll(discovery.getDiscoveredDevices());
			}
		}
		
		return list;
	}

	public static synchronized DeviceDiscoveryStarter getStarter()
	{
		if (instance == null)
			instance = new DeviceDiscoveryStarter();
		
		return instance;
	}
	
	private class DiscoveryThread implements Runnable
	{
		private DeviceDiscovery discovery;
		
		public DiscoveryThread(DeviceDiscovery discovery)
		{
			this.discovery = discovery;
		}

		@Override
		public void run() {
			discovery.start();
		}
		
	}
}
