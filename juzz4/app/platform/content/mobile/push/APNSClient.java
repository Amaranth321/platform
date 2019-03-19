package platform.content.mobile.push;

import javapns.Push;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotification;
import javapns.notification.ResponsePacket;
import lib.util.Util;
import models.cloud.UIConfigurableCloudSettings;
import models.mobile.MobileDevice;
import play.Logger;
import play.vfs.VirtualFile;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class APNSClient implements MobilePushClient
{
    public APNSClient()
    {
    }

    @Override
    public boolean send(PushNotificationItem pushItem)
    {
        try
        {
            PushNotificationPayload apnsMessage = getAPNSMessage(pushItem.getMessage());
            Logger.debug("pushing notification to APNS device (%s) : %s", pushItem.getDeviceToken(), apnsMessage);

            //credentials
            File p12File = VirtualFile.fromRelativePath("/conf/KupPushNotification.p12").getRealFile();
            String apnsPassword = UIConfigurableCloudSettings.server().externalAccounts().apnsPassword;

            List<PushedNotification> plist = Push.payload(
                    apnsMessage,
                    p12File,
                    apnsPassword,
                    true,
                    pushItem.getDeviceToken()
            );

            /**
             *
             *  post-send operations
             *
             */
            for (PushedNotification p : plist)
            {
                if (p.isSuccessful())
                {
                    continue;
                }

                Logger.error("APNS(production) push FAILED to %s (%s)",
                             p.getDevice().getToken(),
                             p.getException().getMessage());

                ResponsePacket response = p.getResponse();
                if (response != null)
                {
                    Logger.error("APNS response packet: %s", response.getMessage());
                }

                //token no longer valid
                if (p.getException().toString().contains("InvalidDeviceTokenFormatException"))
                {
                    MobileDevice targetDevice = MobileDevice.findByToken(pushItem.getDeviceToken());
                    if (targetDevice != null)
                    {
                        targetDevice.delete();
                        Logger.info(Util.whichFn() + "Removed invalid APNS device (%s:%s)",
                                    targetDevice.getDeviceModel(),
                                    targetDevice.getDeviceToken());
                    }
                }
            }

            return true;

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    private PushNotificationPayload getAPNSMessage(PushMessage pushMessage)
    {
        try
        {
            Map<String, String> payloadMap = pushMessage.getFormattedMap();
            PushNotificationPayload apnsMessage = PushNotificationPayload.complex();
            apnsMessage.addAlert(pushMessage.getSubject());
            for (String key : payloadMap.keySet())
            {
                apnsMessage.addCustomDictionary(key, payloadMap.get(key));
            }

            apnsMessage.addSound("default");
            apnsMessage.addBadge(1);
            return apnsMessage;

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }
}
