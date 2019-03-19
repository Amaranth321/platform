package models;

import com.google.code.morphia.annotations.Entity;
import play.modules.morphia.Model;

import java.util.List;

/**
 * @author tbnguyen1407
 */
@Entity(value="InventoryItem", noClassnameStored = true)
@Model.NoAutoTimestamp
public class MongoInventoryItem extends Model
{
    // region fields

    private String id;
    private String registrationNumber;
    private String deviceModelId;
    private String macAddress;
    private boolean activated;

    // endregion

    // region getters

    public String getInventoryItemId()
    {
        return this.id;
    }

    public String getRegistrationNumber()
    {
        return this.registrationNumber;
    }

    public String getDeviceModelId()
    {
        return this.deviceModelId;
    }

    public String getMacAddress()
    {
        return this.macAddress;
    }

    public boolean isActivated()
    {
        return this.activated;
    }

    // endregion

    // region setters

    public void setInventoryItemId(String newId)
    {
        this.id = newId;
    }

    public void setRegistrationNumber(String newRegistrationNumber)
    {
        this.registrationNumber = newRegistrationNumber;
    }

    public void setDeviceModelId(String newDeviceModelId)
    {
        this.deviceModelId = newDeviceModelId;
    }

    public void setMacAddress(String newMacAddress)
    {
        this.macAddress = newMacAddress;
    }

    public void setActivated(boolean newActivated)
    {
        this.activated = newActivated;
    }

    // endregion

    public MongoInventoryItem()
    {

    }

    // region public methods

    public static MongoInventoryItem getById(String newId)
    {
        return MongoInventoryItem.q().filter("id", newId).get();
    }

    public static MongoInventoryItem getByRegistrationNumber(String newRegistrationNumber)
    {
        return MongoInventoryItem.q().filter("registrationNumber", newRegistrationNumber).get();
    }

    public static MongoInventoryItem getByMacAddress(String newMacAddress)
    {
        return MongoInventoryItem.q().filter("macAddress", newMacAddress).get();
    }

    public boolean isKaiNode()
    {
        MongoDeviceModel deviceModel = getDeviceModel();
        return deviceModel != null && deviceModel.isKaiNode();
    }

    public boolean isNodeOne()
    {
        MongoDeviceModel deviceModel = getDeviceModel();
        return deviceModel.getChannels() == 1;
    }

    public MongoDeviceModel getDeviceModel()
    {
        return MongoDeviceModel.getByModelId(deviceModelId);
    }

    // NOTE: this method generate a String Id of Long-parsable Value to be compatible with existing mongo models
    // To be called before saving a new bucket
    public static String generateNewId()
    {
        Long maxId = 0L;

        CollectionSetting collectionSetting = CollectionSetting.getByCollectionName("InventoryItem");
        if (collectionSetting == null)
        {
            List<MongoInventoryItem> entities = MongoInventoryItem.q().fetchAll();
            for (MongoInventoryItem entity : entities)
            {
                Long curId = Long.parseLong(entity.getInventoryItemId());
                if (curId > maxId)
                {
                    maxId = curId;
                }
            }

            // generate new if not exists
            collectionSetting = new CollectionSetting("InventoryItem", maxId);
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

    // endregion
}
