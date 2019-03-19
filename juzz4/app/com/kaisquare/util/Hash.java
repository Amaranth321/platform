/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.kaisquare.util;

import play.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

/**
 *
 * @author Technie
 */
public class Hash {

    public static final String default_encoding = "UTF-8";
    //public static final String salt = "juzz4";

    private static boolean showDebug = false;

    private Hash () {
        //No-op
    }

    public static void setDebugPrint(boolean flag) {
        showDebug = flag;
    }

    private static void _debug (String msg) {
        if ((showDebug) && (null != msg)) {
            Logger.info(Calendar.getInstance().getTime().toString() + " - " + msg);
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


    public static String sha1(String text) {
        return sha1(text, default_encoding);
    }

    public static String sha1(String text, String encoding) {
        String hash = null;
        try {
            byte[] res = sha1raw(text, encoding);
            if ((null != res) && (res.length > 0)) {
                hash = hexToString(res);
            }
        } catch (Exception e) {
            _debug("Unable to generate SHA-1 hash. " + e);
        }
        return hash;
    }

    public static byte[] sha1raw(String text) {
        return hashraw(text, default_encoding, "SHA-1");
    }

    public static byte[] sha1raw(String text, String encoding) {
        return hashraw(text, encoding, "SHA-1");
    }



    public static String sha256(String text) {
        return sha1(text, default_encoding);
    }

    public static String sha256(String text, String encoding) {
        String hash = null;
        try {
            byte[] res = sha256raw(text, encoding);
            if ((null != res) && (res.length > 0)) {
                hash = hexToString(res);
            }
        } catch (Exception e) {
            _debug("Unable to generate SHA-256 hash. " + e);
        }
        return hash;
    }

    public static byte[] sha256raw(String text) {
        return hashraw(text, default_encoding, "SHA-256");
    }

    public static byte[] sha256raw(String text, String encoding) {
        return hashraw(text, encoding, "SHA-256");
    }

    /**
     *
     * @param text
     * @return
     */
    public static byte[] hashraw(String text, String encoding, String hashtype) {
        if (null == text) {
            throw new IllegalArgumentException("String to be hashed must not be null.");
        }
        if (null == encoding) {
            throw new IllegalArgumentException("Encoding of the string must be provided.");
        }
        if (encoding.isEmpty()) {
            throw new IllegalArgumentException("Encoding of the string must be provided.");
        }
        if (null == hashtype) {
            throw new IllegalArgumentException("Hash type must be given.");
        }
        if (hashtype.isEmpty()) {
            throw new IllegalArgumentException("Hash type must be given.");
        }
        try {
            MessageDigest md = MessageDigest.getInstance(hashtype);
            //text = text+salt;

            md.update(text.getBytes(encoding), 0, text.length());
            //text = salt+hexToString(md.digest());

            //md.update(text.getBytes(encoding), 0, text.length());
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            _debug("No such algorithm: " + hashtype);
        } catch (UnsupportedEncodingException e) {
            _debug("Encoding not supported: " + encoding);
        }
        return new byte[] {0};
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
}
