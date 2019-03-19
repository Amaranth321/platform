package com.kaisquare.kaisync.server;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.kaisquare.kaisync.utils.AppLogger;

public class SSLContextProvider {
	
	public static SSLContext getSslContext(String keystore, String keypass)
	{
		return getContext("key", keystore, keypass);
	}
	
	public static SSLContext getTrustSslContext(String keystore, String keypass)
	{
		return getContext("trust", keystore, keypass);
	}

	private static SSLContext getContext(String keyOrTrust, String keystore, String keypass)
	{
		SSLContext sslContext = null;
		KeyManager[] km = null;
		TrustManager[] tm = null;
		
		try {
			sslContext = SSLContext.getInstance("SSL");
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream(new File(keystore)), keypass.toCharArray());
			if ("trust".equals(keyOrTrust))
			{
				TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init(ks);
				tm = tmf.getTrustManagers();
			}
			else if ("key".equals(keyOrTrust))
			{
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(ks, keypass.toCharArray());
				km = kmf.getKeyManagers();
			}
			else
				throw new IllegalArgumentException("Unknown '" + keyOrTrust + "'");
			
			sslContext.init(km, tm, null);
		} catch (Exception e) {
			AppLogger.e(SSLContextProvider.class, e, "Error creating SSL context: %s", keystore);
		}
		
		return sslContext;
	}
}
