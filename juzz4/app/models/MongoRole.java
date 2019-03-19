package models;

import com.google.code.morphia.annotations.Entity;
import lib.util.Util;
import play.Logger;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author tbnguyen1407
 */
@Entity(value="Role", noClassnameStored = true)
@Model.NoAutoTimestamp
public class MongoRole extends Model
{
    // region fields

    private String id;
    private String bucketId;
    private String name;
    private String description;
    public List<String> featureNames;

    // endregion

    // region getters

    public String getBucketId()
    {
        return this.bucketId;
    }

    public String getRoleId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public List<String> getFeatureNames()
    {
        return this.featureNames;
    }

    // endregion

    // region setters

    public void setRoleId(String newId)
    {
        this.id = newId;
    }

    public void setBucketId(String newBucketId)
    {
        this.bucketId = newBucketId;
    }

    public void setName(String newName)
    {
        this.name = newName;
    }

    public void setDescription(String newDescription)
    {
        this.description = newDescription;
    }

    // endregion

    public MongoRole()
    {
        this.featureNames = new ArrayList<>();
    }

    public MongoRole(String bucketId, String name, String description)
    {
        this.name = name;
        this.description = description;
        this.bucketId = bucketId;
        featureNames = new ArrayList<>();
    }

    // region public methods

    // NOTE: this method generate a String Id of Long-parsable Value to be compatible with existing mongo models
    // To be called before saving a new entry
    public static String generateNewId()
    {
        Long maxId = 0L;

        CollectionSetting collectionSetting = CollectionSetting.getByCollectionName("Role");
        if (collectionSetting == null)
        {
            List<MongoRole> entities = MongoRole.q().fetchAll();
            for (MongoRole entity : entities)
            {
                Long curId = Long.parseLong(entity.getRoleId());
                if (curId > maxId)
                {
                    maxId = curId;
                }
            }

            // generate new if not exists
            collectionSetting = new CollectionSetting("Role", maxId);
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

    public boolean addFeatureName(String newFeatureName)
    {
        try
        {
            if (!this.featureNames.contains(newFeatureName))
            {
                this.featureNames.add(newFeatureName);
            }
            return true;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    public boolean removeFeatureName(String newFeatureName)
    {
        try
        {
            this.featureNames.removeAll(Collections.singleton(newFeatureName));
            return true;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    public static MongoRole getById(String newRoleId)
    {
        return MongoRole.q().filter("id", newRoleId).get();
    }

    public String toString()
    {
        return name;
    }

    // endregion
}
