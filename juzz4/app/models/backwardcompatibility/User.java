package models.backwardcompatibility;

import com.kaisquare.util.Hash;
import models.MongoRole;
import models.MongoService;
import models.MongoUser;
import models.UserLabel;
import models.notification.UserNotificationSettings;
import models.transients.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import platform.FeatureManager;
import platform.access.DefaultUser;
import platform.analytics.VcaFeature;
import play.Logger;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User.
 *
 * @author kdp
 * @author Aye Maung
 */
@Entity
@Table(name = "`users`")
@Deprecated
public class User extends Model
{
    public String name;
    public String login;
    public String password;
    public String email;
    public Integer two_factor_mode;
    public Integer session_timeout;  // In milliseconds.
    public boolean activated;
    public String creationTimestamp;
    public String phone;
    public String language;

    public Long bucketId;

    @ManyToMany
    public Collection<Role> roles;

    @ManyToMany
    public Collection<Service> services;

    //use public constructors
    private User()
    {
        this.roles = new ArrayList<>();
        this.creationTimestamp = DateTime.now().toString("dd:MM:yy");
    }

    public User(String name,
                String login,
                String password,
                String email,
                String phone,
                String language)
    {
        this();
        this.name = name;
        this.login = login;
        this.password = password;
        this.email = email;
        this.phone = phone;

        //defaults
        this.two_factor_mode = 0;
        this.session_timeout = 300;
        this.activated = true;

        //hash password
        String salt = this.creationTimestamp;
        this.password = Hash.sha256(password + salt);
        this.language = language;

        //common services
        List<Service> sqlServices = new ArrayList<>();
        List<MongoService> mongoServices = FeatureManager.getInstance().getCommonServices();
        for (MongoService mongoService : mongoServices)
        {
            sqlServices.add(new Service(mongoService));
        }
        this.services = sqlServices;
    }

    public User(DefaultUser defaultUser)
    {
        this(
                defaultUser.getFullName(),
                defaultUser.getUsername(),
                defaultUser.getPassword(),
                "",
                "",
                "en"
        );

        //common services
        List<Service> sqlServices = new ArrayList<>();
        List<MongoService> mongoServices = FeatureManager.getInstance().getCommonServices();
        for (MongoService mongoService : mongoServices)
        {
            sqlServices.add(new Service(mongoService));
        }
        this.services = sqlServices;
    }

    // for compatibility
    public User(MongoUser mongoUser)
    {
        this.id = Long.parseLong(mongoUser.getUserId());
        this.bucketId = Long.parseLong(mongoUser.getBucketId());
        this.name = mongoUser.getName();
        this.login = mongoUser.getLogin();
        this.password = mongoUser.getPassword();
        this.email = mongoUser.getEmail();
        this.phone = mongoUser.getPhone();
        this.language = mongoUser.getLanguage();
        this.creationTimestamp = mongoUser.getCreationTimestamp();

        List<Role> sqlRoles = new ArrayList<>();
        for (String roleId : mongoUser.getRoleIds())
        {
            MongoRole mongoRole = MongoRole.getById(roleId);
            if (mongoRole != null)
            {
                sqlRoles.add(new Role(mongoRole));
            }
        }
        this.roles = sqlRoles;
    }

    public boolean setBucket(String bucketId)
    {
        try
        {
            Long bid = Long.parseLong(bucketId);
            Bucket bucket = Bucket.findById(bid);
            if (bucket == null)
            {
                Logger.warn("Invalid bucket ID");
                return false;
            }
            //this.bucket = bucket;
            this.bucketId = bid;
            return true;
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return false;
        }
    }

    /**
     * <b>This function will</b><br>
     * &nbsp&nbsp&nbsp - add service to the user<br>
     *
     * @param serviceId The service ID to be added.
     *
     * @return true on success otherwise false
     */
    public boolean addService(String serviceId)
    {
        try
        {
            Long sid = Long.parseLong(serviceId);
            Service service = Service.findById(sid);
            if (service == null)
            {
                Logger.warn("Invalid service ID");
                return false;
            }
            else if (this.services.contains(service))
            {
                return true;
            }
            else
            {
                this.services.add(service);
            }
            return true;
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return false;
        }
    }

    /**
     * <b>This function will</b><br>
     * &nbsp&nbsp&nbsp - remove service from database<br>
     *
     * @param serviceId The service ID to be removed.
     *
     * @return true on success otherwise false
     */
    public boolean removeService(String serviceId)
    {
        try
        {
            Long sid = Long.parseLong(serviceId);
            for (Service s : this.services)
            {
                if (s.id == sid)
                {
                    this.services.remove(s);
                    return true;
                }
            }
            return false;
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return false;
        }
    }

    public UserInfo getAsUserInfo()
    {
        UserInfo info = new UserInfo();
        info.userId = this.getId();
        info.name = this.name;
        info.login = this.login;
        info.email = this.email;
        info.phone = this.phone;
        info.activated = this.activated;

        //compile role names
        info.roles = StringUtils.join(this.roles.toArray(), " ");

        //compile labels
        Iterable<UserLabel> userLabels = UserLabel.q()
                .filter("bucketId", this.bucketId)
                .filter("userId", this.getId())
                .fetch();

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
        if (o instanceof User)
        {
            User other = (User) o;
            boolean sameBucket = this.bucketId != null && this.bucketId.equals(other.bucketId);
            boolean sameLogin = this.login != null && this.login.equals(other.login);
            return sameBucket && sameLogin;
        }

        return false;
    }

    public UserNotificationSettings getNotificationSettings()
    {
        UserNotificationSettings settings = UserNotificationSettings.q().filter("userId", getId()).first();
        if (settings == null)
        {
            Logger.info("(%s) creating new user notification settings", name);
            //settings = new UserNotificationSettings(this);
            //settings.save();
        }

        return settings;
    }

    public boolean hasAccessTo(String featureName)
    {
        Feature feature = Feature.find("name", featureName).first();
        for (Role r : roles)
        {
            if (r.features.contains(feature))
            {
                return true;
            }
        }

        return false;
    }

    public boolean hasAccessTo(VcaFeature vcaFeature)
    {
        return hasAccessTo(vcaFeature.getName());
    }

    public boolean hasAccessTo(Device device)
    {
        return device.users.contains(this);
    }
}

