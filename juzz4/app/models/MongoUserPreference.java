package models;

import com.google.code.morphia.annotations.Entity;
import play.modules.morphia.Model;

/**
 * @author tbnguyen1407
 */
@Entity(value="UserPreference", noClassnameStored = true)
@Model.NoAutoTimestamp
public class MongoUserPreference extends Model
{
    // region constants

    public static final Long DEFAULT_DURATION = (7L * 24 * 60); //days in minute

    // endregion

    // region fields

    private String id;
    private String userId;
    private String apnsDeviceToken;
    private String gcmDeviceToken;
    private boolean emailNotificationEnabled;
    private boolean pushNotificationEnabled;
    private boolean smsNotificationEnabled;
    private Integer numberOfViews;
    private String slotSettingAssignments;
    private Long duration;
    private boolean autoRotation;
    private Long autoRotationTime;
    private boolean posFakeDataEnabled;
    private String theme;

    // endregion

    // region getters

    public String getUserId()
    {
        return this.userId;
    }

    public String getApnsDeviceToken()
    {
        return this.apnsDeviceToken;
    }

    public String getGcmDeviceToken()
    {
        return this.gcmDeviceToken;
    }

    public boolean isEmailNotificationEnabled()
    {
        return this.emailNotificationEnabled;
    }

    public boolean isPushNotificationEnabled()
    {
        return this.pushNotificationEnabled;
    }

    public boolean isSmsNotificationEnabled()
    {
        return this.smsNotificationEnabled;
    }

    public Integer getNumberOfViews()
    {
        return this.numberOfViews;
    }

    public String getSlotSettingAssignments()
    {
        return this.slotSettingAssignments;
    }

    public Long getDuration()
    {
        return this.duration;
    }

    public boolean isAutoRotation()
    {
        return this.autoRotation;
    }

    public Long getAutoRotationTime()
    {
        return this.autoRotationTime;
    }

    public boolean isPosFakeDataEnabled()
    {
        return this.posFakeDataEnabled;
    }

    public String getTheme()
    {
        return this.theme;
    }

    // endregion

    // region setters

    public void setUserId(String newUserId)
    {
        this.userId = newUserId;
    }

    public void setApnsDeviceToken(String newApnsDeviceToken)
    {
        this.apnsDeviceToken = newApnsDeviceToken;
    }

    public void setGcmDeviceToken(String newGcmDeviceToken)
    {
        this.gcmDeviceToken = newGcmDeviceToken;
    }

    public void setEmailNotificationEnabled(boolean newEmailNotificationEnabled)
    {
        this.emailNotificationEnabled = newEmailNotificationEnabled;
    }

    public void setPushNotificationEnabled(boolean newPushNotificationEnabled)
    {
        this.pushNotificationEnabled = newPushNotificationEnabled;
    }

    public void setSmsNotificationEnabled(boolean newSmsNotificationEnabled)
    {
        this.smsNotificationEnabled = newSmsNotificationEnabled;
    }

    public void setNumberOfViews(Integer newNumberOfViews)
    {
        this.numberOfViews = newNumberOfViews;
    }

    public void setSlotSettingAssignments(String newSlotSettingAssignments)
    {
        this.slotSettingAssignments = newSlotSettingAssignments;
    }

    public void setDuration(Long newDuration)
    {
        this.duration = newDuration;
    }

    public void setAutoRotation(boolean newAutoRotation)
    {
        this.autoRotation = newAutoRotation;
    }

    public void setAutoRotationTime(Long newAutoRotationTime)
    {
        this.autoRotationTime = newAutoRotationTime;
    }

    public void setPosFakeDataEnabled(boolean newPosFakeDataEnabled)
    {
        this.posFakeDataEnabled = newPosFakeDataEnabled;
    }

    public void setTheme(String newTheme)
    {
        this.theme = newTheme;
    }

    // endregion

    public MongoUserPreference()
    {
        this.apnsDeviceToken = "";
        this.gcmDeviceToken = "";
        this.emailNotificationEnabled = false;
        this.pushNotificationEnabled = false;
        this.smsNotificationEnabled = false;

        this.numberOfViews = 1;
        this.duration = DEFAULT_DURATION;
        this.slotSettingAssignments = "{}";
        this.autoRotation = false;
        this.autoRotationTime = (long)(15);
        this.posFakeDataEnabled = false;
        this.theme = "";
    }

    // region public methods

    public static MongoUserPreference getByUserId(String newUserId)
    {
        return MongoUserPreference.q().filter("userId", newUserId).get();
    }

    // endregion
}
