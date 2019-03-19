package models;

import com.google.code.morphia.annotations.Entity;
import com.kaisquare.util.Hash;
import lib.util.Util;
import models.notification.UserNotificationSettings;
import models.transients.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import platform.FeatureManager;
import platform.analytics.VcaFeature;
import play.Logger;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author tbnguyen1407
 */
@Entity(value="User", noClassnameStored = true)
@Model.NoAutoTimestamp
public class MongoUser extends Model
{
    // region fields

    private String id;
    private String name;
    private String login;
    private String password;
    private String email;
    private boolean activated;
    private String creationTimestamp;
    private String phone;
    private String language;
    private String bucketId;
    private List<String> roleIds;
    private List<String> serviceNames;

    // endregion

    // region getters

    public String getUserId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public String getLogin()
    {
        return this.login;
    }

    public String getPassword()
    {
        return this.password;
    }

    public String getEmail()
    {
        return this.email;
    }

    public boolean isActivated()
    {
        return this.activated;
    }

    public String getCreationTimestamp()
    {
        return this.creationTimestamp;
    }

    public String getPhone()
    {
        return this.phone;
    }

    public String getLanguage()
    {
        return this.language;
    }

    public String getBucketId()
    {
        return this.bucketId;
    }

    public List<String> getRoleIds()
    {
        return roleIds;
    }

    public List<String> getServiceNames()
    {
        return serviceNames;
    }

    // endregion

    // region setters

    public void setUserId(String newId)
    {
        this.id = newId;
    }

    public void setName(String newName)
    {
        this.name = newName;
    }

    public void setLogin(String newLogin)
    {
        this.login = newLogin;
    }

    public void setPassword(String newPassword)
    {
        this.password = newPassword;
    }

    public void setEmail(String newEmail)
    {
        this.email = newEmail;
    }

    public void setActivated(boolean newActivated)
    {
        this.activated = newActivated;
    }

    public void setCreationTimestamp(String newCreationTimestamp)
    {
        this.creationTimestamp = newCreationTimestamp;
    }

    public void setPhone(String newPhone)
    {
        this.phone = newPhone;
    }

    public void setLanguage(String newLanguage)
    {
        this.language = newLanguage;
    }

    public void setBucketId(String newBucketId)
    {
        this.bucketId = newBucketId;
    }

    // endregion

    public MongoUser()
    {
        this.creationTimestamp = DateTime.now().toString("dd:MM:yy");
        this.roleIds = new ArrayList<>();

        this.serviceNames = FeatureManager.getInstance().getCommonServiceNames();
    }

    public MongoUser(String bucketId, String name, String login, String password, String email, String phone, String language)
    {
        this();
        this.bucketId = bucketId;
        this.name = name;
        this.login = login;
        this.password = password;
        this.email = email;
        this.phone = phone;

        //defaults
        this.activated = true;

        //hash password
        String salt = this.creationTimestamp;
        this.password = Hash.sha256(password + salt);
        this.language = language;
    }

    // region public methods

    // NOTE: this method generate a String Id of Long-parsable Value to be compatible with existing mongo models
    // To be called before saving a new user
    public static String generateNewId()
    {
        Long maxId = 0L;

        CollectionSetting collectionSetting = CollectionSetting.getByCollectionName("User");
        if (collectionSetting == null)
        {
            List<MongoUser> entities = MongoUser.q().fetchAll();
            for (MongoUser entity : entities)
            {
                Long curId = Long.parseLong(entity.getUserId());
                if (curId > maxId)
                {
                    maxId = curId;
                }
            }

            // generate new if not exists
            collectionSetting = new CollectionSetting("User", maxId);
        }
        else
        {
            maxId = collectionSetting.getMaxLongId();
        }

        // save new max id
        Long newMaxId = maxId + 1;
        collectionSetting.setMaxLongId(newMaxId);
        collectionSetting.save();

        return newMaxId.toString();
    }

    public static MongoUser getById(String newUserId)
    {
        return MongoUser.q().filter("id", newUserId).get();
    }

    public boolean addRoleId(String newRoleId)
    {
        try
        {
            if (!this.roleIds.contains(newRoleId))
            {
                this.roleIds.add(newRoleId);
            }
            return true;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    public boolean removeRoleId(String newRoleId)
    {
        try
        {
            this.roleIds.removeAll(Collections.singleton(newRoleId));
            return true;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    public boolean addServiceName(String newServiceName)
    {
        try
        {
            if (!this.serviceNames.contains(newServiceName))
            {
                this.serviceNames.add(newServiceName);
            }
            return true;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    public boolean removeServiceName(String newServiceName)
    {
        try
        {
            this.serviceNames.removeAll(Collections.singleton(newServiceName));
            return true;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    public UserInfo getAsUserInfo()
    {
        UserInfo info = new UserInfo();
        info.userId = Long.parseLong(this.getUserId());
        info.name = this.name;
        info.login = this.login;
        info.email = this.email;
        info.phone = this.phone;
        info.activated = this.activated;

        //compile role names
        List<String> roleNames = new ArrayList<>();
        for (String roleId : this.roleIds)
        {
            MongoRole role = MongoRole.getById(roleId);
            if (role != null)
            {
                roleNames.add(role.getName());
            }
        }
        info.roles = StringUtils.join(roleNames, " ");

        //compile labels
        List<UserLabel> userLabels = UserLabel.q()
                .filter("bucketId", Long.parseLong(this.bucketId))
                .filter("userId", Long.parseLong(this.id))
                .fetchAll();
        info.joinedLabels = StringUtils.join(userLabels, ",");

        return info;
    }

    @Override
    public String toString()
    {
        return login;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof MongoUser)
        {
            MongoUser other = (MongoUser) o;
            boolean sameBucket = this.bucketId != null && this.bucketId.equals(other.bucketId);
            boolean sameLogin = this.login != null && this.login.equals(other.login);
            return sameBucket && sameLogin;
        }

        return false;
    }

    public UserNotificationSettings getNotificationSettings()
    {
        UserNotificationSettings settings = UserNotificationSettings.q().filter("userId", Long.parseLong(this.id)).get();
        if (settings == null)
        {
            Logger.info("(%s) creating new user notification settings", name);

            settings = new UserNotificationSettings(this);
            settings.save();
        }

        return settings;
    }

    public boolean hasAccessToVcaFeature(VcaFeature vcaFeature)
    {
        return hasAccessToFeature(vcaFeature.getName());
    }

    public boolean hasAccessToFeature(String featureName)
    {
        for (String roleId : this.roleIds)
        {
            MongoRole role = MongoRole.getById(roleId);

            // loop through all role's features
            for (String curFeatureName : role.getFeatureNames())
            {
                if (featureName.equals(curFeatureName))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasAccessToDevice(MongoDevice device)
    {
        return device != null && device.getUserIds().contains(this.getUserId());
    }

    // endregion
}
