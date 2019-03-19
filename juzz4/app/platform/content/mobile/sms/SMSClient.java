package platform.content.mobile.sms;

import models.cloud.UIConfigurableCloudSettings;
import platform.content.providers.MachSMSNotifier;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class SMSClient
{
    private static final SMSClient instance = new SMSClient();

    public static SMSClient getInstance()
    {
        return instance;
    }

    public static boolean isEnabled()
    {
        return UIConfigurableCloudSettings.server().delivery().allowSms;
    }

    public boolean send(SMSItem smsItem)
    {
        if (!instance.isEnabled())
        {
            return true;
        }

        return sendWithMach(smsItem);
    }

    private boolean sendWithMach(SMSItem smsItem)
    {
        String username = UIConfigurableCloudSettings.server().externalAccounts().machSmsUserName;
        String password = UIConfigurableCloudSettings.server().externalAccounts().machSmsPassword;

        MachSMSNotifier notifier = new MachSMSNotifier(username, password);
        return notifier.sendMessage(smsItem);
    }

    private SMSClient()
    {
    }
}
