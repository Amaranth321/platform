package models.backwardcompatibility;

import com.google.code.morphia.annotations.Entity;

import play.modules.morphia.Model;

/**
 *
 * @author Nischal
 */
@Entity
@Deprecated
public class DeviceLabel extends Model {

    public Long bucketId;
    public Long deviceId;
    public String label;

    public DeviceLabel() {
        bucketId = 0L;
        deviceId = 0L;
        label = "";
    }
}
