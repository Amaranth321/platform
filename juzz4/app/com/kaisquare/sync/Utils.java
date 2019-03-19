package com.kaisquare.sync;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
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
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.kaisquare.kaisync.utils.AppLogger;

public class Utils {
	
	public static final int FILE_CHUNK_SIZE = 16384;
	
	public static String stringJoin(Iterable<String> strings, String separator)
	{
		StringBuilder sb = new StringBuilder();
		
		for (String s : strings)
		{
			if (sb.length() > 0)
				sb.append(separator);
			
			sb.append(s);
		}
		
		return sb.toString();
	}
	
	public static String loadFileContent(File file)
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
	
	public static void writeFileContent(File f, String content)
	{
		FileChannel fc = null;
		try {
			fc = new FileOutputStream(f).getChannel();
			fc.write(ByteBuffer.wrap(content.getBytes()));
		} catch (Exception e) {
			AppLogger.e("Utils", e, "");
		} finally {
			if (fc != null)
			{
				try {
					fc.close();
				} catch (IOException e) {}
			}
		}
	}

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
    
    public static boolean isStringEmpty(String s)
    {
    	return s == null || "".equals(s.trim());
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		return decrypted;
    }
    
    private static String _salt = "!passS@ltfOrAeS#0612+";
	private static String _password = "kainodeservice";    
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
//			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
		} catch (InvalidKeyException e) {
//			e.printStackTrace();
		} catch (InvalidParameterSpecException e) {
//			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
//			e.printStackTrace();
		} catch (BadPaddingException e) {
//			e.printStackTrace();
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
//			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
		} catch (InvalidKeyException e) {
//			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
//			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
//			e.printStackTrace();
		} catch (BadPaddingException e) {
//			e.printStackTrace();
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
//			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
//			e.printStackTrace();
		}
    	
    	return secret;
    }
    
    public static boolean encryptFile(File file) throws Exception
    {
    	return encryptFile(file, null);
    }
    
    public static boolean encryptFile(File file, String output) throws Exception
    {
    	boolean ret = false;
    	
    	BufferedInputStream bis = null;
    	CipherOutputStream cipherOut = null;
    	try {
    		File encryptedFile = output != null ? new File(output) : new File(file.getAbsolutePath() + ".enc");
    		SecretKey key = generateSecretKey(_password , _salt);
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			AlgorithmParameters params = cipher.getParameters();
			byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
			
			FileOutputStream fos = new FileOutputStream(encryptedFile);
			bis = new BufferedInputStream(new FileInputStream(file));
			cipherOut = new CipherOutputStream(fos, cipher);

			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(iv.length);
			fos.write(bb.array());
			fos.write(iv);
			
			byte[] buffer = new byte[8192];
			int read = 0;
			while ((read = bis.read(buffer)) > 0)
				cipherOut.write(buffer, 0, read);
			
			ret = true;
		} finally {
			if (bis != null)
			{
				try {
					bis.close();
				} catch (IOException e) {}
			}
			if (cipherOut != null)
			{
				try {
					cipherOut.flush();
					cipherOut.close();
				} catch (IOException e) {}
			}
		}
    	
    	return ret;
    }
    
    public static boolean decryptFile(File file) throws Exception
    {
    	return decryptFile(file, null);
    }
    
    @SuppressWarnings("resource")
	public static InputStream createDecryptInputStream(File file) throws IOException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
    	CipherInputStream cipherIn = null;
    	try {
    		FileInputStream fis = new FileInputStream(file);
    		
    		ByteBuffer bb = ByteBuffer.allocate(4);
    		fis.read(bb.array());
    		int ivLength = bb.getInt();
    		if (ivLength > 1024)
    			throw new IllegalBlockSizeException("invalid key size");
    		byte[] iv = new byte[ivLength];
    		fis.read(iv);
    		
    		SecretKey key = generateSecretKey(_password , _salt);
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
			
			cipherIn = new CipherInputStream(fis, cipher);
		} finally {
		}
    	
    	return cipherIn;
    }
    
    public static boolean decryptFile(File file, String output) throws Exception
    {
    	boolean ret = false;
    	
    	BufferedOutputStream bos = null;
    	InputStream cipherIn = null;
		try {
    		String filename = file.getName();
    		if (filename.endsWith(".enc"))
    			filename = new String(filename.substring(0, filename.length() - 4));
    		else
    			filename = file.getAbsolutePath() + ".dec";
    		
    		File decryptedFile = output != null ? new File(output) : new File(filename);
    		
			cipherIn = createDecryptInputStream(file);
			bos = new BufferedOutputStream(new FileOutputStream(decryptedFile));
			
			byte[] buffer = new byte[8192];
			int read = 0;
			while ((read = cipherIn.read(buffer)) > 0)
				bos.write(buffer, 0, read);
			
			ret = true;
		} finally {
			if (cipherIn != null)
			{
				try {
					cipherIn.close();
				} catch (IOException e) {}
			}
			if (bos != null)
			{
				try {
					bos.flush();
					bos.close();
				} catch (IOException e) {}
			}
		}
    	
    	return ret;
    }

	public static String getFileExtension(File file) {
		String name = file.getName();
		int dotPos = name.lastIndexOf(".");
		
		if (dotPos > 0)
			return new String(name.substring(dotPos + 1));
		
		return null;
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
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} finally {
			if (is != null)
			{
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return checksum;
    }

	public static String sha256hash(String text) {
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            hash = hexToString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            AppLogger.e("Utils", e, "");
        } catch (UnsupportedEncodingException e) {
        	AppLogger.e("Utils", e, "");
        }
        return hash;
    }
}
