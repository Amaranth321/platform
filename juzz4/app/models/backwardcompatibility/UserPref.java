package models.backwardcompatibility;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import models.MongoUser;
import models.MongoUserPreference;
import play.db.jpa.Model;

/**
 * This object stores a user's various preferences such as 4/9/16 live view configuration,
 * notification settings etc.
 *
 * @author kdp
 */
@Entity
@Table(name = "`userprefs`")
@Deprecated
public class UserPref extends Model
{
    public static final Long DEFAULT_DURATION = (7L * 24 * 60); //days in minute

    @OneToOne
    public User user;

    public String APNSDeviceToken;
    public String GCMDeviceToken;
    public boolean pushNotificationEnabled;
    public boolean emailNotificationEnabled;
    public boolean smsNotificationEnabled;
    public Integer numberOfViews;
    public String slotSettingAssignments;
    public Long duration;
    public Boolean autoRotation;
    public Long autoRotationTime;
    public Boolean POSFakeDataEnabled;
    public String theme;

    public UserPref()
    {
        // APNSDeviceToken = "";
        // GCMDeviceToken = "";
        // pushNotificationEnabled = false;
        this.emailNotificationEnabled = false;
        // smsNotificationEnabled = false;

        this.numberOfViews = 1;
        this.duration = UserPref.DEFAULT_DURATION;
        this.slotSettingAssignments = "{}";
        this.autoRotation = false;
        this.autoRotationTime = (long) (15);
        this.POSFakeDataEnabled = false;
        this.theme = "";
    }

    // for compatibility
    public UserPref(MongoUserPreference mongoUserPreference)
    {
        this.APNSDeviceToken = mongoUserPreference.getApnsDeviceToken();
        this.GCMDeviceToken = mongoUserPreference.getGcmDeviceToken();
        this.pushNotificationEnabled = mongoUserPreference.isPushNotificationEnabled();
        this.emailNotificationEnabled = mongoUserPreference.isEmailNotificationEnabled();
        this.smsNotificationEnabled = mongoUserPreference.isSmsNotificationEnabled();
        this.numberOfViews = mongoUserPreference.getNumberOfViews();
        this.slotSettingAssignments = mongoUserPreference.getSlotSettingAssignments();
        this.duration = mongoUserPreference.getDuration();
        this.autoRotation = mongoUserPreference.isAutoRotation();
        this.autoRotationTime = mongoUserPreference.getAutoRotationTime();
        this.POSFakeDataEnabled = mongoUserPreference.isPosFakeDataEnabled();
        this.theme = mongoUserPreference.getTheme();

        MongoUser mongoUser = MongoUser.getById(mongoUserPreference.getUserId());
        if (mongoUser != null)
        {
            this.user = new User(mongoUser);
        }
    }
}
