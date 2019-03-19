package models.backwardcompatibility;

import com.google.code.morphia.annotations.Entity;
import com.kaisquare.analytics.thrift.VcaState;
import platform.time.RecurrenceRule;
import org.apache.commons.lang.RandomStringUtils;
import play.modules.morphia.Model;

/**
 * Author:  Aye Maung
 */
@Entity
@Deprecated
public class VcaInstance extends Model {
    public String instanceId;
    public String type;
    public String platformDeviceId;
    public String coreDeviceId;
    public String channelId;
    public String thresholds;
    public RecurrenceRule recurrenceRule;
    public boolean enabled;
    public VcaState vcaState;
    public boolean updateRequired;          //true means node itself must be updated

    public VcaInstance() {
    }

    public VcaInstance(String type,
                       String platformDeviceId,
                       String coreDeviceId,
                       String channelId,
                       String thresholds,
                       RecurrenceRule recurrenceRule) {
        this();
        this.instanceId = RandomStringUtils.randomAlphanumeric(10).toUpperCase();
        this.type = type;
        this.platformDeviceId = platformDeviceId;
        this.coreDeviceId = coreDeviceId;
        this.channelId = channelId;
        this.thresholds = thresholds;
        this.recurrenceRule = recurrenceRule;
        this.enabled = true;
        this.vcaState = VcaState.WAITING;
        this.updateRequired = false;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof VcaInstance) {
            VcaInstance vcaOther = (VcaInstance) other;
            return this.instanceId.equals(vcaOther.instanceId);
        }

        return false;
    }

    @Override
    public String toString() {
        return String.format("[%s, %s, %s, enabled:%s, platformDeviceId:%s, coreDeviceId:%s, channelId:%s, updateRequired:%s]",
                instanceId,
                type,
                vcaState,
                enabled,
                platformDeviceId,
                coreDeviceId,
                channelId,
                updateRequired);
    }

}
