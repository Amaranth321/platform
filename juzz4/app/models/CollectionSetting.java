package models;

import com.google.code.morphia.annotations.Entity;
import play.modules.morphia.Model;

/**
 * @author tbnguyen1407
 */
@Entity(value="CollectionSetting", noClassnameStored = true)
@Model.NoAutoTimestamp
public class CollectionSetting extends Model
{
    // region fields

    private String collectionName;
    private Long maxLongId;

    // endregion

    // region getters

    public String getCollectionName()
    {
        return collectionName;
    }

    public Long getMaxLongId()
    {
        return maxLongId;
    }

    // endregion

    // region setters

    public void setCollectionName(String newCollectionName)
    {
        this.collectionName = newCollectionName;
    }

    public void setMaxLongId(Long newMaxLongId)
    {
        this.maxLongId = newMaxLongId;
    }

    // endregion

    public CollectionSetting(String newCollectionName, Long newMaxLongId)
    {
        this.collectionName = newCollectionName;
        this.maxLongId = newMaxLongId;
    }

    public static CollectionSetting getByCollectionName(String newCollectionName)
    {
        return CollectionSetting.q().filter("collectionName", newCollectionName).get();
    }
}