package models;

import com.google.code.morphia.annotations.Embedded;

/**
 *
 * @author nischal.regmi
 */
@Embedded
public class DeviceSelected {

    public String label;
    public String deviceId;
    public String channelId;

    public DeviceSelected() {
        label = "";
        deviceId = "";
        channelId = "";
    }
}
