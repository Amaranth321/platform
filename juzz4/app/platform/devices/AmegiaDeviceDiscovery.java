package platform.devices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import play.Logger;
import play.vfs.VirtualFile;

/*package*/class AmegiaDeviceDiscovery extends DeviceDiscovery {
	
	private static final String DISCOVERY_BIN = "/resources/ipdisc";
	
	private Process process;
	
	AmegiaDeviceDiscovery() {
		
	}

	@Override
	public void startAutoDiscovery() {
		ProcessBuilder pb = new ProcessBuilder(
				VirtualFile.fromRelativePath(DISCOVERY_BIN).getRealFile().getAbsolutePath());
		try {
			process = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			
			while ((line = reader.readLine()) != null)
			{
				convertDiscoveryOutput(line);
			}
		} catch (IOException e) {
			Logger.error(e, this.getClass().getName());
		}
	}

	@Override
	public void stopAutoDiscovery() {
		if (process != null)
			process.destroy();
		
		process = null;
	}

	@Override
	protected int getDeviceModelId() {
		return MODEL_AMEGIA;
	}

}
