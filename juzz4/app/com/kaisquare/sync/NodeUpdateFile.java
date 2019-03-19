package com.kaisquare.sync;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import com.kaisquare.kaisync.platform.DeviceType;
import com.kaisquare.kaisync.utils.AppLogger;

public class NodeUpdateFile {
	
	private TarArchiveInputStream in;
	private boolean isValidUpdateFile;
	private String nodeVerion;
	private String packagedVersion;
	private DeviceType updatefileType;
	private String modelId;

	public NodeUpdateFile(File file) throws Exception
	{
		initialize(file);
	}
	
	private void initialize(File file) throws Exception
	{
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		try {
			this.in = new TarArchiveInputStream(new GZIPInputStream(is));
			loadFile();
		} catch (ZipException e) {
			AppLogger.i(this, "trying to decrypt file...");
			is = new BufferedInputStream(Utils.createDecryptInputStream(file)); 
			this.in = new TarArchiveInputStream(new GZIPInputStream(is));
			loadFile();
		}
	}
	
	private void loadFile() throws IOException
	{
		TarArchiveEntry entry;
		
		byte[] b;
		int read;
		while ((entry = in.getNextTarEntry()) != null)
		{
			if (!isValidUpdateFile && entry.getName().startsWith("VCABox/")) {
				isValidUpdateFile = true;
				updatefileType = DeviceType.Nodes;
			} else if (entry.getName().equalsIgnoreCase("nodeone.ver")) {
				isValidUpdateFile = true;
				updatefileType = DeviceType.NodeOne;
				
				//node one update file, extract node one version.
				if (entry.getSize() > 0) {
					b = new byte[(int) entry.getSize()];
					read = in.read(b, 0, b.length);
					nodeVerion = new String(b, 0, read);
					
					int breakline = nodeVerion.indexOf("\n");
					if (breakline > -1)
						nodeVerion = new String(nodeVerion.substring(0, breakline));
				}
			}
			
			
			if (entry.getName().equalsIgnoreCase("VCABox/kainode.ver"))
			{
				if (entry.getSize() > 0)
				{
					b = new byte[(int) entry.getSize()];
					read = in.read(b, 0, b.length);
					nodeVerion = new String(b, 0, read);
					
					int breakline = nodeVerion.indexOf("\n");
					if (breakline > -1)
						nodeVerion = new String(nodeVerion.substring(0, breakline));
				}
			} 
			
			
			if (entry.getName().equalsIgnoreCase("VCABox/version")) {
				b = new byte[(int) entry.getSize()];
				read = in.read(b, 0, b.length);
				packagedVersion = new String(b, 0, read);
			} else if (entry.getName().equalsIgnoreCase("release")) {
				b = new byte[(int) entry.getSize()];
				read = in.read(b, 0, b.length);
				packagedVersion = new String(b, 0, read);
			}
			
			if (entry.getName().equalsIgnoreCase("model") || entry.getName().equalsIgnoreCase("VCABox/model"))
			{
				b = new byte[(int) entry.getSize()];
				read = in.read(b, 0, b.length);
				modelId = new String(b, 0, read).replace("\n", "");
			}
			
			if (isValidUpdateFile && 
				!Utils.isStringEmpty(nodeVerion) && 
				!Utils.isStringEmpty(packagedVersion) &&
				null != updatefileType)
				break;
		}
		
		isValidUpdateFile = isValidUpdateFile && 
							!Utils.isStringEmpty(nodeVerion) && 
							!Utils.isStringEmpty(packagedVersion) && 
							null != updatefileType;
		try {
			in.close();
		} catch (IOException e) {}
	}
	
	public boolean isValidUpdateFile()
	{
		return isValidUpdateFile;
	}
	
	public String getKainodeVersion()
	{
		return nodeVerion;
	}
	
	public String getPackagedVersion()
	{
		return packagedVersion;
	}
	
	public DeviceType getUpdatefileType() {
		return updatefileType;
	}
	
	public String getModelId()
	{
		return modelId;
	}
}
