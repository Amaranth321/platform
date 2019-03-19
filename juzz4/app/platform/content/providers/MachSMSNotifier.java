/*
 * SMSNotifier.java
 *
 * Copyright (C) KAI Square Pte Ltd
 */

package platform.content.providers;

import platform.content.mobile.sms.SMSItem;
import play.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * A notifier that sends alerts through SMS. The SMS gateway used is
 * <a href="http://www.mach.com/">MACH</a>.
 *
 * @author Zin Zin
 */
public class MachSMSNotifier
{
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;
    private static final List<String> smsServerList = Arrays.asList(
            "smsgw.mmea.mes.syniverse.com",
            "smsgw-fra1.mmea.mes.syniverse.com",
            "smsgw-ams1.mmea.mes.syniverse.com"
    );

    private String machUsername;
    private byte[] machPassword;
    private int split;

    public MachSMSNotifier(String machUsername, String machPassword)
    {
        this(machUsername, machPassword, 1);
    }

    public MachSMSNotifier(String machUsername, String machPassword, int split)
    {
        try
        {
            this.machUsername = machUsername;
            this.machPassword = MessageDigest.getInstance("MD5")
                    .digest(machPassword.getBytes());    //First encryption of the password
            this.split = split;
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

    public String getMACHUsername()
    {
        return this.machUsername;
    }

    public byte[] getMACHPassword()
    {
        return this.machPassword;
    }

    public int getSplit()
    {
        return this.split;
    }

    public boolean sendMessage(SMSItem smsItem)
    {
        boolean success = false;
        for (String server : smsServerList)
        {
            OutputStream os = null;
            BufferedReader reader = null;
            try
            {

                //Second encryption for the password
                StringBuffer tempPass = new StringBuffer()
                        .append(lib.util.Util.hexToString(this.machPassword))
                        .append(smsItem.getSenderName())
                        .append(smsItem.getRecipientNumber())
                        .append(smsItem.getText());

                byte[] md5Pass = MessageDigest.getInstance("MD5").digest(tempPass.toString().getBytes());
                StringBuffer encryptedPassword = new StringBuffer().append(lib.util.Util.hexToString(md5Pass));

                // Encode the query
                String postData =
                        "id=" + URLEncoder.encode(this.machUsername, "US-ASCII") +
                        "&pw=" + URLEncoder.encode(encryptedPassword.toString(), "US-ASCII") +
                        "&snr=" + URLEncoder.encode(smsItem.getSenderName(), "US-ASCII") +
                        "&dnr=" + URLEncoder.encode(smsItem.getRecipientNumber(), "US-ASCII") +
                        "&msg=" + URLEncoder.encode(smsItem.getText(), "US-ASCII") +
                        "&split=" + this.split;

                // Connect to sms gateway by post method
                URL url = new URL("http://" + server + "/sms.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", String.valueOf(postData.length()));

                os = connection.getOutputStream();
                os.write(postData.getBytes());

                // Get response
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response = reader.readLine();
                reader.close();
                success = false;
                if (response != null)
                {
                    response = response.trim();
                    if (response.startsWith("+OK"))
                    {
                        success = true;
                    }
                    else
                    {
                        Logger.error("SMS server returned failure response: %s", response);
                    }
                }
                else
                {
                    Logger.error("SMS server returned an empty response.");
                }
            }
            catch (Exception e)
            {
                Logger.error(e, "Error sending SMS message.");
                success = false;
            }
            finally
            {
                if (os != null)
                {
                    try
                    {
                        os.close();
                    }
                    catch (IOException e)
                    {
                    }
                }
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (IOException e)
                    {
                    }
                }
            }
            if (success)
            {
                break;
            }
        }
        return success;
    }
}
