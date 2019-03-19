package platform;

import lib.util.Util;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import platform.config.readers.ConfigsNode;
import platform.config.readers.ConfigsShared;
import play.Logger;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.SecureRandom;

/**
 * Use to encrypt or decrypt data
 *
 * @author Keith Chong
 */
public class CryptoManager
{

    /**
     * Use to encrypt string and store into a file
     *
     * @param text     The data that what to be store
     * @param filepath File location
     * @param filename File Name
     *
     * @return true/false success or unsuccess
     */
    public static boolean createEncryptFile(byte[] bytes, String filepath, String filename)
    {

        try
        {
            //Create file
            InputStream is = new ByteArrayInputStream(bytes);
            FileOutputStream fos = new FileOutputStream(filepath + filename);

            //Encrypt file's data
            String aesKey = getEncryptionKey();
            Key key = new SecretKeySpec(aesKey.getBytes(), "AES");
            byte[] iv = aesKey.getBytes();
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(cipher.ENCRYPT_MODE, key, ivspec);
            CipherInputStream cis = new CipherInputStream(is, cipher);

            //Generate file
            byte[] data = new byte[64];
            int numBytes;
            while ((numBytes = cis.read(data)) != -1)
            {
                fos.write(data, 0, numBytes);
            }
            fos.flush();
            fos.close();
            cis.close();
            is.close();

            return true;
        }
        catch (Exception e)
        {
            Logger.error(e.getMessage());
        }
        return false;
    }

    /**
     * Use to decrypt file and return byte
     *
     * @param filepath File location
     * @param filename File Name
     *
     * @return byte/null
     */
    public static byte[] readEncryptedFile(String filepath, String filename)
    {

        try
        {
            //Read file
            FileInputStream fis = new FileInputStream(filepath + filename);

            //Decrypt file's data
            String aesKey = getEncryptionKey();
            Key key = new SecretKeySpec(aesKey.getBytes(), "AES");
            byte[] iv = aesKey.getBytes();
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(cipher.DECRYPT_MODE, key, ivspec);
            CipherInputStream cis = new CipherInputStream(fis, cipher);

            //convert file to byte[]
            byte[] bytes = IOUtils.toByteArray(cis);
            cis.close();
            fis.close();

            return bytes;
        }
        catch (Exception e)
        {
            Logger.error(e.getMessage());
        }

        return null;
    }

    /**
     * Use to encrypt plain text to encrypted text
     *
     * @param plainText
     *
     * @return encrypteText / null
     */
    public static String aesEncrypt(String plainText)
    {
        if (Util.isNullOrEmpty(plainText))
        {
            return plainText;
        }
        try
        {
            String aesKey = getEncryptionKey();
            Key key = new SecretKeySpec(aesKey.getBytes(), "AES");
            byte[] iv = new SecureRandom().generateSeed(16);
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Cipher chiper = Cipher.getInstance("AES/CBC/PKCS5Padding");
            chiper.init(Cipher.ENCRYPT_MODE, key, ivspec);
            byte[] encVal = chiper.doFinal(plainText.getBytes());
            String encryptedValue = new BASE64Encoder().encode(iv) + new BASE64Encoder().encode(encVal);
            return encryptedValue;
        }
        catch (Exception e)
        {
            Logger.error(e, "Unable to process AES ecryption (plainText:%s)", plainText);
            return null;
        }
    }

    /**
     * Use to decrypt encrypted text to plain text
     *
     * @param encryptedText
     *
     * @return plain text /encrypted text
     */
    public static String aesDecrypt(String encryptedText)
    {
        if (Util.isNullOrEmpty(encryptedText))
        {
            return encryptedText;
        }
        try
        {
            String aesKey = getEncryptionKey();
            Key key = new SecretKeySpec(aesKey.getBytes(), "AES");
            byte[] iv = Base64.decodeBase64(encryptedText.substring(0, encryptedText.indexOf("==") + 2));
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Cipher chiper = Cipher.getInstance("AES/CBC/PKCS5Padding");
            chiper.init(Cipher.DECRYPT_MODE, key, ivspec);
            byte[] decordedValue = new BASE64Decoder().
                    decodeBuffer(encryptedText.substring(encryptedText.indexOf("==") + 2, encryptedText.length()));
            byte[] decValue = chiper.doFinal(decordedValue);
            String decryptedValue = new String(decValue);
            return decryptedValue;
        }
        catch (Exception e)
        {
            Logger.error(e, "Unable to process AES decryption (encryptedText:%s)", encryptedText);
            return encryptedText;
        }
    }

    private static String getEncryptionKey()
    {
        return ConfigsShared.getInstance().licenseEncryptionKey();
    }
}
