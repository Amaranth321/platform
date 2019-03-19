package platform.content.mobile.push;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import lib.util.Util;
import models.cloud.UIConfigurableCloudSettings;
import models.mobile.MobileDevice;
import play.Logger;

import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class GCMClient implements MobilePushClient
{
    private static final int RETRIES = 3;

    private final String gcmApiKey;

    public GCMClient()
    {
        gcmApiKey = UIConfigurableCloudSettings.server().keyValues().gcmApiKey;
        if (gcmApiKey.isEmpty())
        {
            throw new RuntimeException("Missing GCM API key");
        }
    }

    @Override
    public boolean send(PushNotificationItem pushItem)
    {
        try
        {
            Sender sender = new Sender(gcmApiKey);
            Message gcmMessage = getGCMMessage(pushItem.getMessage());

            Logger.debug("pushing notification to GCM device (%s) : %s", pushItem.getDeviceToken(), gcmMessage);
            Result sendResult = sender.send(gcmMessage, pushItem.getDeviceToken(), RETRIES);

            /**
             *
             *  post-send operations
             *
             */
            MobileDevice targetDevice = MobileDevice.findByToken(pushItem.getDeviceToken());

            if (sendResult.getMessageId() != null)
            {
                String canonicalRegId = sendResult.getCanonicalRegistrationId();
                if (canonicalRegId != null)
                {
                    // same device has more than one registration ID
                    targetDevice.setDeviceToken(canonicalRegId);
                    targetDevice.save();
                    Logger.info(Util.whichFn() + "Updated GCM device token (%s:%s)",
                                targetDevice.getDeviceModel(),
                                targetDevice.getDeviceToken());
                }
            }
            else
            {
                String error = sendResult.getErrorCodeName();
                if (error.equals(Constants.ERROR_NOT_REGISTERED))
                {
                    // app has been removed from device
                    targetDevice.delete();
                    Logger.info(Util.whichFn() + "Removed invalid GCM device (%s:%s)",
                                targetDevice.getDeviceModel(),
                                targetDevice.getDeviceToken());
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

    private Message getGCMMessage(PushMessage pushMessage)
    {
        Map<String, String> formattedMap = pushMessage.getFormattedMap();
        Message.Builder builder = new Message.Builder().collapseKey(pushMessage.getMessageType().toString());
        //payload
        for (String key : formattedMap.keySet())
        {
            builder.addData(key, formattedMap.get(key));
        }

        return builder.build();
    }

}
