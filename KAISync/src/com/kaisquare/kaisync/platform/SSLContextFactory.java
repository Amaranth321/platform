package com.kaisquare.kaisync.platform;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SSLContextFactory {

	public static SSLContext create(String protocol, String keystore, String keypass) throws IOException, NoSuchAlgorithmException {
		SSLContext c = null;
		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			FileInputStream keyFile = new FileInputStream(keystore);
			char[] keyPassphrase = keypass.toCharArray();
	        ks.load(keyFile, keyPassphrase);
	
	        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
	        kmf.init(ks, keyPassphrase);
	        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
	        tmf.init(ks);
	
	        c = SSLContext.getInstance(protocol);
	        c.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		} catch (NoSuchAlgorithmException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
        
        return c;
	}

}
