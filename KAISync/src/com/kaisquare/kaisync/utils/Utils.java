package com.kaisquare.kaisync.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AlgorithmParameters;
import java.security.DigestInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

public class Utils {

	private static final String TAG = "Utils";
	public static int bytesIndexOf(byte[] data, byte[] pattern, int startIndex)
	{
		return bytesIndexOf(data, pattern, startIndex, computeFailure(pattern));
	}
	
	/**
     * Finds the first occurrence of the pattern in the text.
     */
    public static int bytesIndexOf(byte[] data, byte[] pattern, int startIndex, int[] failure) {
        int j = 0;
        if (data.length == 0) return -1;

        for (int i = startIndex; i < data.length; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) { j++; }
            if (j == pattern.length) {
                return i - pattern.length + 1;
            }
        }
        return -1;
    }

    /**
     * Computes the failure function using a boot-strapping process,
     * where the pattern is matched against itself.
     */
    public static int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];

        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }

        return failure;
    }
    
    public static void copyFile(File src, File dst) {
		if (src.exists())
		{			
			FileChannel srcChannel = null;
			FileChannel dstChannel = null;
			try {
				if (!dst.exists())
					dst.createNewFile();
				
				srcChannel = new FileInputStream(src).getChannel();
				dstChannel = new FileOutputStream(dst).getChannel();
				dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (srcChannel != null)
				{
					try {
						srcChannel.close();
					} catch (IOException e) {}
				}
				if (dstChannel != null)
				{
					try {
						dstChannel.close();
					} catch (IOException e) {}
				}
			}
		}
	}
    
    public static boolean isStringEmpty(String s)
    {
    	return s == null || "".equals(s.trim());
    }
    
    public static boolean isDigitString(String s)
    {
    	char[] chars = s.toCharArray();
    	for (char c : chars)
    	{
    		int n = (int)c;
    		if (n < 48 || n > 57)
    			return false;
    	}
    	
    	return true;
    }
    
    public static String padString(String source, int length)
	{
		return padString(source, length, " ");
	}
	
	public static String padString(String source, int length, String str)
	{
		if (source == null)
			source = "";
		if (source.length() >= length)
			return source;
		
		StringBuilder sb = new StringBuilder(source);
		int remaining = length - source.length();
		for (int i = 0; i < remaining; i++)
			sb.append(str);
			
		return sb.toString();
	}
    
    private static byte[] rawKey = null;
    private static byte[] generateAesKey() throws NoSuchAlgorithmException
    {
    	if (rawKey == null)
    	{
    		KeyGenerator keygen = KeyGenerator.getInstance("AES");
    		keygen.init(128);
    		SecretKey key = keygen.generateKey();
    		rawKey = key.getEncoded();
    	}
    	
    	return rawKey;
    }
    
    public static byte[] encryptAES(byte[] plainData)
    {
    	SecretKeySpec keyspec;
    	byte[] encrypted = null;
    	
		try {
			keyspec = new SecretKeySpec(generateAesKey(), "AES");
	    	Cipher cipher = Cipher.getInstance("AES");
	    	cipher.init(Cipher.ENCRYPT_MODE, keyspec);
	    	encrypted = cipher.doFinal(plainData);
		} catch (NoSuchAlgorithmException e) {
			AppLogger.e("Utils", e, "");
		} catch (NoSuchPaddingException e) {
			AppLogger.e("Utils", e, "");
		} catch (InvalidKeyException e) {
			AppLogger.e("Utils", e, "");
		} catch (IllegalBlockSizeException e) {
			AppLogger.e("Utils", e, "");
		} catch (BadPaddingException e) {
			AppLogger.e("Utils", e, "");
		}
		
		return encrypted;
    }
    
    public static byte[] decryptAES(byte[] cipherData)
    {
    	byte[] decrypted = null;
    	SecretKeySpec keyspec;
		try {
			keyspec = new SecretKeySpec(generateAesKey(), "AES");
	    	Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
	    	cipher.init(Cipher.DECRYPT_MODE, keyspec);
	    	decrypted = cipher.doFinal(cipherData);
		} catch (NoSuchAlgorithmException e) {
			AppLogger.e("Utils", e, "");
		} catch (NoSuchPaddingException e) {
			AppLogger.e("Utils", e, "");
		} catch (InvalidKeyException e) {
			AppLogger.e("Utils", e, "");
		} catch (IllegalBlockSizeException e) {
			AppLogger.e("Utils", e, "");
		} catch (BadPaddingException e) {
			AppLogger.e("Utils", e, "");
		}
    	
		return decrypted;
    }
    
    private static String _salt = "ArbiterService@kup-core";    
    public static List<byte[]> encrypt(byte[] plainData, String password)
    {
    	byte[] iv = null;
    	byte[] cipherText = null;
    	
    	try {
			SecretKey secret = generateSecretKey(password, _salt);
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secret);
			AlgorithmParameters params = cipher.getParameters();
			iv = params.getParameterSpec(IvParameterSpec.class).getIV();
			cipherText = cipher.doFinal(plainData);
		} catch (NoSuchAlgorithmException e) {
//			AppLogger.e("Utils", e, "");
		} catch (NoSuchPaddingException e) {
//			AppLogger.e("Utils", e, "");
		} catch (InvalidKeyException e) {
//			AppLogger.e("Utils", e, "");
		} catch (InvalidParameterSpecException e) {
//			AppLogger.e("Utils", e, "");
		} catch (IllegalBlockSizeException e) {
//			AppLogger.e("Utils", e, "");
		} catch (BadPaddingException e) {
//			AppLogger.e("Utils", e, "");
		}
    	
		if (iv == null || cipherText == null)
			return null;
		
		List<byte[]> list = new ArrayList<byte[]>();
		list.add(cipherText);
		list.add(iv);
		
		return list;
    }
    
    public static byte[] decrypt(byte[] cipherData, byte[] iv, String password)
    {
    	SecretKey secret = generateSecretKey(password, _salt);
    	Cipher cipher;
    	byte[] decryptedData = null;
    	
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
			decryptedData = cipher.doFinal(cipherData);
		} catch (NoSuchAlgorithmException e) {
//			AppLogger.e("Utils", e, "");
		} catch (NoSuchPaddingException e) {
//			AppLogger.e("Utils", e, "");
		} catch (InvalidKeyException e) {
//			AppLogger.e("Utils", e, "");
		} catch (InvalidAlgorithmParameterException e) {
//			AppLogger.e("Utils", e, "");
		} catch (IllegalBlockSizeException e) {
//			AppLogger.e("Utils", e, "");
		} catch (BadPaddingException e) {
//			AppLogger.e("Utils", e, "");
		}
    	
    	return decryptedData;
    }
    
    private static SecretKey generateSecretKey(String password, String salt)
    {
    	char[] passchars = new char[password.length()];
    	password.getChars(0, passchars.length, passchars, 0);
    	
    	SecretKey secret = null;
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
	    	KeySpec spec = new PBEKeySpec(passchars, salt.getBytes(), 65536, 128);
	    	SecretKey tmp = factory.generateSecret(spec);
	    	secret = new SecretKeySpec(tmp.getEncoded(), "AES");
		} catch (NoSuchAlgorithmException e) {
//			AppLogger.e("Utils", e, "");
		} catch (InvalidKeySpecException e) {
//			AppLogger.e("Utils", e, "");
		}
    	
    	return secret;
    }
    
    public static String sha1Hash(String raw)
    {
    	try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			return new String(Hex.encodeHex(sha1.digest(raw.getBytes())));
		} catch (NoSuchAlgorithmException e) {
			AppLogger.e("Utils", e, "");
			return raw;
		}
    }
    
    private static String hexToString(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }
	
	public static String getFileChecksum(File file)
    {
    	String checksum = null;
    	InputStream is = null;
    	try {
    		MessageDigest md = MessageDigest.getInstance("sha1");
			is = Files.newInputStream(Paths.get(file.getAbsolutePath()));
			DigestInputStream dis = new DigestInputStream(is, md);
			
			byte[] b = new byte[8192];
			while (dis.read(b) > 0)
			{
			}
			checksum = hexToString(md.digest());
		} catch (IOException e) {
			AppLogger.e("Utils", e, "");
		} catch (NoSuchAlgorithmException e) {
			AppLogger.e("Utils", e, "");
		} finally {
			if (is != null)
			{
				try {
					is.close();
				} catch (IOException e) {
					AppLogger.e("Utils", e, "");
				}
			}
		}
		
		return checksum;
    }
	
	public static String readFileContent(File file)
	{
		BufferedInputStream bis = null;
		StringBuilder sb = new StringBuilder();
		try {			
			bis = new BufferedInputStream(new FileInputStream(file));
			
			byte[] buffer = new byte[16384];
			int read = 0;
			while ((read = bis.read(buffer)) > 0)
				sb.append(new String(buffer, 0, read));
			
			return sb.toString();
		} catch (FileNotFoundException e) {
			AppLogger.e("Utils", "File not found: " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bis != null)
			{
				try {
					bis.close();
				} catch (IOException e) {}
			}
		}
		
		return null;
	}
	
	public static void writeFileContent(File file, byte[] content)
	{
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			fos.write(content);
		} catch (Exception e) {
			AppLogger.e("Utils", e, "");
		} finally {
			if (fos != null)
			{
				try {
					fos.flush();
					fos.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public static String getMacAddress(String iface)
    {
		StringBuilder sb = new StringBuilder();
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			
			loop:
			while (ifaces.hasMoreElements())
			{
				NetworkInterface nic = ifaces.nextElement();
				if (iface != null && !nic.getName().equals(iface))
					continue;
				
				Enumeration<InetAddress> addrs = nic.getInetAddresses();
				while (addrs.hasMoreElements())
				{
					InetAddress addr = addrs.nextElement();
					if (!addr.getHostAddress().startsWith("127.0"))
					{
						byte[] mac = nic.getHardwareAddress();
						for (int i = 0; i < mac.length; i++) {
							sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));		
						}
						break loop;
					}
				}
			}
		} catch (SocketException e) {
			AppLogger.e(TAG, "error reading mac address: %s", e.getMessage());
		}
		
		return sb.toString().toLowerCase();
    }
    
    public static String runCommand(int lineLimit, String...params)
    {
    	StringBuilder sb = new StringBuilder();
    	ProcessBuilder builder = new ProcessBuilder(params);
    	Process p = null;
    	try {
    		p = builder.start();
    		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    		String line;
    		int lines = 0;
    		while ((line = reader.readLine()) != null)
    		{
    			sb.append(line);
    			if (lineLimit > 0)
    			{
    				lines++;
    				if (lines >= lineLimit)
    					break;
    			}
    		}
    	} catch (Exception e) {
//    		AppLogger.e(this, e, "");
    	} finally {
    		if (p != null)
    			p.destroy();
    	}
    	return sb.toString();
    }
}
