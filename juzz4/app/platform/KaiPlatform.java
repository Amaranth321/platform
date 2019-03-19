package platform;

import com.kaisquare.kaisync.platform.IPlatformSyncHandler;
import com.kaisquare.kaisync.server.ISyncServer;
import com.kaisquare.kaisync.server.ServerBinder;
import com.kaisquare.sync.FileTransferHandler;
import com.kaisquare.sync.PlatformSynchronizationHandler;
import com.kaisquare.sync.SoftwareUpdateHandler;

import lib.util.JsonReader;
import platform.config.readers.ConfigsServers;
import play.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class handles whole platform status
 */
public final class KaiPlatform {
	
	/**
	 * The status of device synchronization with cloud
	 */
	public static final String STATUS_DEVICE_SYNC = "status_device_sync";
	/**
	 * The status of analytics synchronization with cloud
	 */
	public static final String STATUS_ANALYTICS_SYNC = "status_analytics_sync";
	
	private static final PlatformLatch latch = new PlatformLatch();
	private static final Map<String, StatusMonitor> statusHolder = new ConcurrentHashMap<String, StatusMonitor>();
	private static ISyncServer platformSrv, fileSrv, softwareUpdateSrv, platformSslSrv;
	
	private KaiPlatform() {}
	
	public static void start()
	{
		latch.start();
	}
	
	public static void stop()
	{
		latch.stop();
	}
	
	public static void reset()
	{
		latch.reset();
	}
	
	public static void awaitIfNotReady()
	{
		latch.await();
	}
	
	public static boolean isStarted()
	{
		return latch.isStarted();
	}
	
	public static boolean isStopped()
	{
		return latch.isStopped();
	}
	
	public static void startServers()
	{
		String keystore = null;
		String keypass = null;

        JsonReader platformSyncCfg = ConfigsServers.getInstance().kaisyncPlatformSyncCfg();
        IPlatformSyncHandler handler = PlatformSynchronizationHandler.getInstance();

        int platformPort = platformSyncCfg.getAsInt("port", 0);
        platformSrv = ServerBinder.bindPlatformSyncServer(platformPort, handler);
        platformSrv.start();
        Logger.info("Started KaiSync Platform Sync server (port:%s)", platformPort);

        int platformSslPort =  platformSyncCfg.getAsInt("ssl-port", 0);
        keystore = platformSyncCfg.getAsString("keystore", null);
        keypass = platformSyncCfg.getAsString("keypass", null);
        platformSslSrv = ServerBinder.bindPlatformSyncServer(platformSslPort, handler);
        platformSslSrv.setKeystore(keystore, keypass);
        platformSslSrv.start();
        Logger.info("Started KaiSync Platform Sync SSL server (port:%s)", platformSslPort);

        int fileServerPort = ConfigsServers.getInstance().kaisyncFileServerCfg().getAsInt("port", 0);
        //the KAISync still need to bind two ports that are 16321, and 16331
        fileSrv = ServerBinder.bindFileTransferServer(fileServerPort, new FileTransferHandler());
        if (keystore != null && !"".equals(keystore)) {
            fileSrv.setKeystore(keystore, keypass);
        }
        fileSrv.start();
        Logger.info("Started KaiSync File server (port:%s)", fileServerPort);

        JsonReader softwareUpdateCfg = ConfigsServers.getInstance().kaisyncSoftwareUpdateCfg();
        int softwareUpdatePort = softwareUpdateCfg.getAsInt("port", 0);
        softwareUpdateSrv = ServerBinder.bindSoftwareUpdateServer(softwareUpdatePort, SoftwareUpdateHandler.getInstance());
        softwareUpdateSrv.start();
        Logger.info("Started KaiSync software update server (port:%s)", softwareUpdatePort);
	}
	
	public static void stopServers()
	{
		Logger.info("stop platform service");
		if (platformSrv != null)
			platformSrv.stop();
		if (platformSslSrv != null)
			platformSslSrv.stop();
        Logger.info("stop file server");
        if (fileSrv != null)
        	fileSrv.stop();
        Logger.info("stop software-update service");
        if (softwareUpdateSrv != null)
        	softwareUpdateSrv.stop();
        Logger.info("services stoppped");
	}
	
	public static synchronized StatusMonitor newStatusMonitor(String name, IPropertyDelegate<Boolean> delegate)
	{
		StatusMonitor monitor = statusHolder.get(name);
		if (monitor != null)
			throw new RuntimeException("'" + name + "' already exists");
		
		monitor = new StatusMonitor(delegate);
		statusHolder.put(name, monitor);
		
		return monitor;
	}
	
	public static Boolean getStatus(String name)
	{
		StatusMonitor monitor = getStatusMonitor(name);
		return monitor != null ? monitor.get() : Boolean.valueOf(false);
	}
	
	public static StatusMonitor getStatusMonitor(String name)
	{
		return statusHolder.get(name);
	}

	private static class PlatformLatch
	{
		private AtomicBoolean started = new AtomicBoolean(false);
		private AtomicBoolean stopped = new AtomicBoolean(false);
		
		public void reset() {
			started.set(false);
			stopped.set(false);
		}
		
		public void start()
		{
			started.set(true);
			synchronized (this) {
				notifyAll();
			}
		}

		public void stop()
		{
			stopped.set(true);
			synchronized (this) {
				notifyAll();
			}
		}
		
		public void await()
		{
			while (!isStarted() && !isStopped()) {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {}
				}
			}
		}
		
		public boolean isStarted()
		{
			return started.get();
		}
		
		public boolean isStopped()
		{
			return stopped.get();
		}
	}

	public static class StatusMonitor implements IPropertyDelegate<Boolean>
	{
		private IPropertyDelegate<Boolean> delegate;
		
		public StatusMonitor(IPropertyDelegate<Boolean> d)
		{
			delegate = d;
		}
		
		@Override
		public Boolean get() {
			return delegate.get();
		}

		@Override
		public void addObserver(IPropertyObserver<Boolean> observer) {
			delegate.addObserver(observer);
		}

		@Override
		public void removeObserver(IPropertyObserver<Boolean> observer) {
			delegate.removeObserver(observer);
		}
		
	}
	
	public static class StatusDelegate implements IPropertyDelegate<Boolean> {
        private AtomicBoolean status = new AtomicBoolean(false);
        private List<IPropertyObserver<Boolean>> observers = Collections.synchronizedList(new ArrayList<IPropertyObserver<Boolean>>());

        public void setSyncStatus(boolean status) {
            Boolean oldStatus = Boolean.valueOf(this.status.getAndSet(status));
            synchronized (observers) {
                for (IPropertyObserver<Boolean> observer : observers)
                    observer.onPropertyChanged(oldStatus, status);
            }
        }

        @Override
        public Boolean get() {
            return Boolean.valueOf(status.get());
        }

        @Override
        public void addObserver(IPropertyObserver<Boolean> observer) {
            synchronized (observers) {
                if (!observers.contains(observer))
                    observers.add(observer);
            }
        }

        @Override
        public void removeObserver(IPropertyObserver<Boolean> observer) {
            synchronized (observers) {
                observers.remove(observer);
            }
        }
    }
}
