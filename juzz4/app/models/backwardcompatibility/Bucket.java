package models.backwardcompatibility;

import models.*;
import models.archived.ArchivedBucket;
import models.notification.BucketNotificationSettings;
import platform.BucketManager;
import platform.access.DefaultBucket;
import platform.pubsub.PlatformEventMonitor;
import platform.pubsub.PlatformEventType;
import platform.register.BrandingAssets;
import play.Logger;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author kdp
 */
@Entity
@Table(name = "`buckets`")
@Deprecated
public class Bucket extends Model
{
    public String name;
    public String path; //path from where to pull static content, customizations etc.
    public String description;
    public Long parentId;

    private boolean activated;
    private boolean deleted;

    @ManyToMany
    public Collection<Feature> features;

    @Deprecated
    @ManyToMany
    public Collection<Service> services;

    @OneToMany
    public Collection<User> users;

    @ManyToMany
    public Collection<Role> roles;

    public Bucket()
    {
        features = new ArrayList<Feature>();
        users = new ArrayList<User>();
        roles = new ArrayList<Role>();
    }

    public Bucket(String name, String path, boolean activated)
    {
        this.name = name;
        this.path = path;
        this.activated = activated;

        features = new ArrayList<Feature>();
        users = new ArrayList<User>();
        roles = new ArrayList<Role>();
    }

    public Bucket(String name, String path, String description, boolean activated)
    {
        this.name = name;
        this.path = path;
        this.description = description;
        this.activated = activated;

        features = new ArrayList<>();
        users = new ArrayList<>();
        roles = new ArrayList<>();
    }

    // for compatibility
    public Bucket(MongoBucket mongoBucket)
    {
        this.id = Long.parseLong(mongoBucket.getBucketId());
        this.name = mongoBucket.getName();
        this.path = mongoBucket.getPath();
        this.description = mongoBucket.getDescription();
        this.activated = mongoBucket.isActivated();
        this.deleted = mongoBucket.isDeleted();

        if (mongoBucket.getParentId() != null)
        {
            this.parentId = Long.parseLong(mongoBucket.getParentId());
        }

        // features
        List<Feature> sqlFeatures = new ArrayList<>();
        for (String featureName : mongoBucket.getFeatureNames())
        {
            MongoFeature mongoFeature = MongoFeature.getByName(featureName);
            if (mongoFeature != null)
            {
                sqlFeatures.add(new Feature(mongoFeature));
            }
        }
        this.features = sqlFeatures;

        // roles
        List<Role> sqlRoles = new ArrayList<>();
        List<MongoRole> mongoRoles = MongoRole.q().filter("bucketId", mongoBucket.getBucketId()).fetchAll();
        for (MongoRole mongoRole : mongoRoles)
        {
            sqlRoles.add(new Role(mongoRole));
        }
        this.roles = sqlRoles;

        // users
        List<User> sqlUsers = new ArrayList<>();
        List<MongoUser> mongoUsers = MongoUser.q().filter("bucketId", mongoBucket.getBucketId()).fetchAll();
        for (MongoUser mongoUser : mongoUsers)
        {
            sqlUsers.add(new User(mongoUser));
        }
        this.users = sqlUsers;
    }

    public boolean addUser(User user)
    {
        try
        {
            if (user == null)
            {
                Logger.warn("Invalid user object");
                return false;
            }
            else if (this.users.contains(user))
            {
                return true;
            }
            else
            {
                this.users.add(user);
                user.bucketId = this.getId();
                user.save();
            }
            this.save();
            return true;
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return false;
        }
    }

    public boolean removeUser(String userId)
    {
        try
        {
            Long uid = Long.parseLong(userId);
            for (User u : this.users)
            {
                if (u.id.equals(uid))
                {
                    this.users.remove(u);
                    return true;
                }
            }
            this.save();
            return false;
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return false;
        }
    }

    /**
     * check if other is either himself or child
     *
     * @param other
     */
    public boolean hasControlOver(Bucket other)
    {
        try
        {
            List<Bucket> sqlDecendants = new ArrayList<>();
            List<MongoBucket> mongoDescendants = BucketManager.getInstance().getThisAndDescendants(this.id.toString());
            for (MongoBucket mongoDescendant : mongoDescendants)
            {
                sqlDecendants.add(new Bucket(mongoDescendant));
            }

            return sqlDecendants.contains(other);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    /**
     * @return only the direct child buckets (one level).
     */
    public List<Bucket> getChildren()
    {
        List<Bucket> children = Bucket.find("parentId", this.getId()).fetch();
        return children;
    }

    public Bucket getParentBucket()
    {
        if (this.parentId == null)
        {
            return null;
        }

        Bucket parent = Bucket.findById(this.parentId);
        return parent;
    }

    public String toString()
    {
        return this.name;
    }

    public boolean isDeleted()
    {
        return this.deleted;
    }

    public boolean setDeleted(boolean deleted)
    {
        this.deleted = deleted;
        try
        {
            if (deleted)
            {
                //manage archive record
                ArchivedBucket abkt = new ArchivedBucket(this.getId());
                abkt.save();

                //Notify others
                PlatformEventMonitor.getInstance().broadcast(PlatformEventType.BUCKET_SUSPENDED, this.id);

            }
            else
            {
                ArchivedBucket.filter("bucketId", this.getId()).delete();

                //Notify others
                PlatformEventMonitor.getInstance().broadcast(PlatformEventType.BUCKET_ACTIVATED, this.id);
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }

        return true;
    }

    public boolean isActivated()
    {
        return activated;
    }

    public void setActivated(boolean activated)
    {
        this.activated = activated;
    }

    /**
     * Recursively checks if this or parent buckets are suspended
     */
    public boolean isSuspended()
    {
        boolean suspended = (!activated || deleted);
        if (suspended)
        {
            return true;
        }

        Bucket parent = this.getParentBucket();
        if (parent == null)
        {
            return false;
        }

        return parent.isSuspended();
    }

    public boolean hasAccessTo(String featureName)
    {
        Feature f = Feature.find("name", featureName).first();
        return features.contains(f);
    }

    public BucketNotificationSettings getNotificationSettings()
    {
        BucketNotificationSettings settings = BucketNotificationSettings.q().filter("bucketId", getId()).first();
        if (settings == null)
        {
            Logger.info("(%s) creating new bucket notification settings", name);
            settings = new BucketNotificationSettings(getId());
            settings.restoreDefaults();
            settings.save();
        }

        return settings;
    }

    public boolean isSuperadmin()
    {
        return name.equals(DefaultBucket.SUPERADMIN.getBucketName());
    }

    public Role findRole(String roleName)
    {
        for (Role role : roles)
        {
            if (role.name.equals(roleName))
            {
                return role;
            }
        }
        return null;
    }

    public BrandingAssets getBrandingAssets()
    {
        BrandingAssets assets = new BrandingAssets();
        try
        {
            //Header Logo
            BucketSetting settings = BucketManager.getInstance().getBucketSetting(getId().toString());
            assets.setBase64HeaderLogo(settings.getBase64EncodedLogoString());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
        return assets;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof Bucket)
        {
            Bucket other = (Bucket) o;
            return this.name.equals(other.name);
        }
        return false;
    }
}
