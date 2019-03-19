package models.backwardcompatibility;

import models.MongoInventoryItem;
import models.backwardcompatibility.DeviceModel;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Aye Maung
 * @since v2.0
 */
@Entity
@Table(name = "`inventory_items`")
@Deprecated
public class InventoryItem extends Model
{
    public String registrationNumber;
    public String modelNumber;
    public String macAddress;
    public boolean activated;

    public InventoryItem(MongoInventoryItem mongoInventoryItem)
    {
        this.id = Long.parseLong(mongoInventoryItem.getInventoryItemId());
        this.registrationNumber = mongoInventoryItem.getRegistrationNumber();
        this.modelNumber = mongoInventoryItem.getDeviceModelId();
        this.macAddress = mongoInventoryItem.getMacAddress();
        this.activated = mongoInventoryItem.isActivated();
    }

    public DeviceModel getDeviceModel()
    {
        return DeviceModel.find("modelId", Long.parseLong(modelNumber)).first();
    }

    public boolean isKaiNode()
    {
        DeviceModel deviceModel = getDeviceModel();
        return deviceModel.isKaiNode();
    }

    public boolean isNodeOne()
    {
        return getDeviceModel().channels == 1;
    }
}
