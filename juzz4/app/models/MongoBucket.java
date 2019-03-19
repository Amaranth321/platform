package models;

import com.google.code.morphia.annotations.Entity;
import lib.util.Util;
import models.archived.ArchivedBucket;
import models.backwardcompatibility.Bucket;
import models.backwardcompatibility.Feature;
import models.notification.BucketNotificationSettings;
import platform.BucketManager;
import platform.pubsub.PlatformEventMonitor;
import platform.pubsub.PlatformEventType;
import platform.register.BrandingAssets;
import play.Logger;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author tbnguyen1407
 */
@Entity(value="Bucket", noClassnameStored = true)
@Model.NoAutoTimestamp
public class MongoBucket extends Model
{
    // region fields

    private String id;
    private String name;
    private String path;
    private String description;
    private String parentId;
    private boolean activated;
    private boolean deleted;
    private List<String> featureNames;

    // endregion

    // region getters

    public String getBucketId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public String getPath()
    {
        return this.path;
    }

    public String getDescription()
    {
        return this.description;
    }

    public String getParentId()
    {
        return this.parentId;
    }

    public boolean isActivated()
    {
        return this.activated;
    }

    public boolean isDeleted()
    {
        return this.deleted;
    }

    public List<String> getFeatureNames()
    {
        return this.featureNames;
    }

    // endregion

    // region setters

    public void setBucketId(String newId)
    {
        this.id = newId;
    }

    public void setName(String newName)
    {
        this.name = newName;
    }

    public void setPath(String newPath)
    {
        this.path = newPath;
    }

    public void setDescription(String newDescription)
    {
        this.description = newDescription;
    }

    public void setParentId(String newParentId)
    {
        this.parentId = newParentId;
    }

    public void setActivated(boolean newActivated)
    {
        this.activated = newActivated;
    }

    public boolean setDeleted(boolean newDeleted)
    {
        this.deleted = newDeleted;

        try
        {
            // move to archive
            if (deleted)
            {
                //manage archive record
                ArchivedBucket archivedBucket = new ArchivedBucket(Long.parseLong(this.getBucketId()));
                archivedBucket.save();

                //Notify others
                PlatformEventMonitor.getInstance().broadcast(PlatformEventType.BUCKET_SUSPENDED, this.id);
            }
            // remove from archive and activate
            else
            {
                ArchivedBucket.filter("bucketId", Long.parseLong(this.id)).delete();

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

    // endregion

    public MongoBucket()
    {
        this.featureNames = new ArrayList<>();
    }

    public MongoBucket(String name, String path, boolean activated)
    {
        this.name = name;
        this.path = path;
        this.activated = activated;

        this.featureNames = new ArrayList<>();
    }

    public MongoBucket(String name, String path, String description, boolean activated)
    {
        this.name = name;
        this.path = path;
        this.description = description;
        this.activated = activated;

        this.featureNames = new ArrayList<>();
    }

    // region public methods

    // NOTE: this method generate a String Id of Long-parsable Value to be compatible with existing mongo models
    // To be called before saving a new bucket
    public static String generateNewId()
    {
        Long maxId = 0L;

        CollectionSetting collectionSetting = CollectionSetting.getByCollectionName("Bucket");
        if (collectionSetting == null)
        {
            List<MongoBucket> entities = MongoBucket.q().fetchAll();
            for (MongoBucket entity : entities)
            {
                Long curId = Long.parseLong(entity.getBucketId());
                if (curId > maxId)
                {
                    maxId = curId;
                }
            }

            // generate new if not exists
            collectionSetting = new CollectionSetting("Bucket", maxId);
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

    public static MongoBucket migrateFromSqlModel(Bucket sqlBucket)
    {
        MongoBucket mongoBucket = new MongoBucket();
        mongoBucket.id = sqlBucket.getId().toString();
        mongoBucket.activated = sqlBucket.isActivated();
        mongoBucket.deleted = sqlBucket.isDeleted();
        mongoBucket.description = sqlBucket.description;
        mongoBucket.name = sqlBucket.name.toLowerCase();
        mongoBucket.path = sqlBucket.path;

        if (sqlBucket.parentId != null)
        {
            mongoBucket.parentId = sqlBucket.parentId.toString();
        }
        for (Feature sqlFeature : sqlBucket.features)
        {
            MongoFeature mongoFeature = MongoFeature.getByName(sqlFeature.name);
            if (mongoFeature != null)
            {
                mongoBucket.addFeatureName(sqlFeature.name);
            }
        }
        return mongoBucket;
    }

    public static MongoBucket getById(String bucketId)
    {
        return MongoBucket.q().filter("id", bucketId).get();
    }

    public static MongoBucket getByName(String bucketName)
    {
        return MongoBucket.q().filter("name", bucketName).get();
    }

    public boolean addFeatureName(String removeFeatureName)
    {
        try
        {
            return this.featureNames.contains(removeFeatureName) || this.featureNames.add(removeFeatureName);
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    public boolean removeFeatureName(String removeFeatureName)
    {
        return removeFeatureNames(Collections.singletonList(removeFeatureName));
    }

    public boolean removeFeatureNames(List<String> removeFeatureNames)
    {
        try
        {
            return this.featureNames.removeAll(removeFeatureNames);
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    public boolean isSuspended()
    {
        boolean suspended = (!activated || deleted);
        if (suspended)
        {
            return true;
        }

        // recursively check ancestors
        MongoBucket parent = MongoBucket.q().filter("id", this.parentId).get();
        if (parent == null)
        {
            return false;
        }

        return parent.isSuspended();
    }

    public boolean hasAccessTo(String featureName)
    {
        return featureNames.contains(featureName);
    }

    public boolean hasControlOver(MongoBucket otherBucket)
    {
        try
        {
            List<MongoBucket> meAndChildren = BucketManager.getInstance().getThisAndDescendants(this.id);
            return meAndChildren.contains(otherBucket);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    public BucketNotificationSettings getNotificationSettings()
    {
        BucketNotificationSettings settings = BucketNotificationSettings.q().filter("bucketId", Long.parseLong(this.id)).get();
        if (settings == null)
        {
            Logger.info("(%s) creating new bucket notification settings", name);
            settings = new BucketNotificationSettings(Long.parseLong(this.id));
            settings.restoreDefaults();
            settings.save();
        }

        return settings;
    }

    public BrandingAssets getBrandingAssets()
    {
        BrandingAssets assets = new BrandingAssets();
        try
        {
            //Header Logo
            BucketSetting settings = BucketManager.getInstance().getBucketSetting(this.id);
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
        if (o instanceof MongoBucket)
        {
            MongoBucket other = (MongoBucket) o;
            return this.name.equals(other.name);
        }
        return false;
    }

    // endregion
}
