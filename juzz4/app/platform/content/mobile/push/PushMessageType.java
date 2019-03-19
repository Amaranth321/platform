package platform.content.mobile.push;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * formatPushData() standardizes the format of the final payload sent to the device.
 * dataValues is an array of values. The corresponding keys MUST be hardcoded in the switch statement.
 * This is to ensure that the keys used here and on mobile device will match.
 * As such, if anything is modified here, the same action should be taken for the mobile device.
 *
 * @author Aye Maung
 * @see platform.content.mobile.push.APNSClient
 * @see platform.content.mobile.push.GCMClient
 * @since v4.4
 */
public enum PushMessageType
{
    ALERT;

    public Map<String, String> formatPushData(String[] dataValues)
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("notification-type", this.toString());

        switch (this)
        {
            case ALERT:
                map.put("deviceId", dataValues[0]);
                return map;

            default:
                throw new IllegalArgumentException(this.toString());

        }
    }
}
