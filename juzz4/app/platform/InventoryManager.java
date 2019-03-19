package platform;

import com.opencsv.CSVReader;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidEnvironmentException;
import models.MongoDevice;
import models.MongoDeviceModel;
import models.MongoInventoryItem;
import models.transients.InvalidInventory;
import models.transients.InventoryInfo;
import platform.pubsub.PlatformEventMonitor;
import platform.pubsub.PlatformEventSubscriber;
import platform.pubsub.PlatformEventTask;
import platform.pubsub.PlatformEventType;
import play.Logger;
import play.modules.morphia.Model;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static lib.util.Util.isNullOrEmpty;

public class InventoryManager implements PlatformEventSubscriber
{
    private static final String MAC_PATTERN = "^([0-9A-Fa-f]{2}[:]){5}([0-9A-Fa-f]{2})$";
    private static InventoryManager instance = new InventoryManager();

    private InventoryManager()
    {
    }

    public static InventoryManager getInstance()
    {
        if (!Environment.getInstance().onCloud())
        {
            throw new InvalidEnvironmentException();
        }

        return instance;
    }

    public void addInventory(InventoryInfo inventoryInfo) throws ApiException
    {
        // verify registration-number
        if (isNullOrEmpty(inventoryInfo.registrationNumber))
        {
            throw new ApiException("invalid registration-number");
        }
        if (MongoInventoryItem.getByRegistrationNumber(inventoryInfo.registrationNumber) != null)
        {
            throw new ApiException("duplicate registration number");
        }

        // verify mac-address
        if (isNullOrEmpty(inventoryInfo.macAddress) || !inventoryInfo.macAddress.matches(MAC_PATTERN))
        {
            throw new ApiException("invalid mac address");
        }
        if (MongoDevice.getByDeviceKey(inventoryInfo.macAddress) != null)
        {
            throw new ApiException("duplicate mac address");
        }

        // verify model-number
        List<MongoDeviceModel> deviceModels = DeviceManager.getInstance().getDeviceModels();
        List<String> deviceModelIds = new ArrayList<>();
        for (MongoDeviceModel deviceModel : deviceModels)
        {
            deviceModelIds.add(deviceModel.getModelId());
        }
        if (!deviceModelIds.contains(inventoryInfo.modelNumber))
        {
            throw new ApiException("invalid model number");
        }

        // add
        MongoInventoryItem inventoryItem = new MongoInventoryItem();
        inventoryItem.setInventoryItemId(MongoInventoryItem.generateNewId());
        inventoryItem.setMacAddress(inventoryInfo.macAddress);
        inventoryItem.setRegistrationNumber(inventoryInfo.registrationNumber);
        inventoryItem.setDeviceModelId(inventoryInfo.modelNumber);
        inventoryItem.setActivated(inventoryInfo.activated);
        inventoryItem.save();
    }

    public List<InventoryInfo> readUploadedFile(File csvFile) throws ApiException
    {
        try
        {
            CSVReader reader = new CSVReader(new FileReader(csvFile));
            String[] nextLine = reader.readNext();   //header line
            int lineCount = 1;

            if (!nextLine[0].trim().toLowerCase().equals("registration no")
                || !nextLine[1].trim().toLowerCase().equals("model no")
                || !nextLine[2].trim().toLowerCase().equals("mac address"))
            {
                throw new ApiException("Incorrect format in the inventory file");
            }
            List<InventoryInfo> inventoryInfo = new ArrayList<>();
            while ((nextLine = reader.readNext()) != null)
            {
                if ((isNullOrEmpty(nextLine[0].trim())) 
                        || (isNullOrEmpty(nextLine[1].trim())) 
                        || (isNullOrEmpty(nextLine[2].trim()))) 
                {
                    throw new ApiException("Incorrect format in the inventory file");
                }
                
                InventoryInfo invInfo = new InventoryInfo();
                invInfo.registrationNumber = nextLine[0].trim();
                invInfo.modelNumber = nextLine[1].trim();
                invInfo.macAddress = nextLine[2].trim().toLowerCase();
                inventoryInfo.add(invInfo);
            }

            if (inventoryInfo.size() <= 0)
            {
                throw new ApiException("Empty inventory file");
            }

            return inventoryInfo;
        }
        catch (ApiException apie)
        {
            throw apie;
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
        return null;
    }

    public List<InvalidInventory> findInvalidRecords(List<InventoryInfo> invInfo) throws ApiException
    {
        List<InvalidInventory> invalidInventories = new ArrayList<>();
        List<MongoDeviceModel> deviceModels = DeviceManager.getInstance().getDeviceModels();
        List<String> deviceModelIds = new ArrayList<>();
        for (MongoDeviceModel deviceModel : deviceModels)
        {
            deviceModelIds.add(deviceModel.getModelId());
        }

        int count = 1;
        try
        {
            for (InventoryInfo inventoryInfo : invInfo)
            {
                count++;
                InvalidInventory invalidInventory = new InvalidInventory();

                if (isNullOrEmpty(inventoryInfo.registrationNumber))
                {
                    invalidInventory.data = InvalidInventory.EMPTY_DATA;
                    invalidInventory.columnName = InvalidInventory.REG_COLUMN;
                    invalidInventory.error = InvalidInventory.REG_COMPULSORY_FIELD;
                    invalidInventory.rowNumber = count;
                    invalidInventories.add(invalidInventory);
                    continue;
                }
                if (isNullOrEmpty(inventoryInfo.modelNumber))
                {
                    invalidInventory.data = InvalidInventory.EMPTY_DATA;
                    invalidInventory.columnName = InvalidInventory.MODEL_COLUMN;
                    invalidInventory.error = InvalidInventory.MODEL_COMPULSORY_FIELD;
                    invalidInventory.rowNumber = count;
                    invalidInventories.add(invalidInventory);
                    continue;
                }
                if (isNullOrEmpty(inventoryInfo.macAddress))
                {
                    invalidInventory.data = InvalidInventory.EMPTY_DATA;
                    invalidInventory.columnName = InvalidInventory.MAC_COLUMN;
                    invalidInventory.error = InvalidInventory.MAC_COMPULSORY_FIELD;
                    invalidInventory.rowNumber = count;
                    invalidInventories.add(invalidInventory);
                    continue;
                }

                boolean invalidModel = !deviceModelIds.contains(inventoryInfo.modelNumber);

                if (invalidModel)
                {
                    invalidInventory.data = inventoryInfo.modelNumber;
                    invalidInventory.columnName = InvalidInventory.MODEL_COLUMN;
                    invalidInventory.error = InvalidInventory.MODEL_INVALID;
                    invalidInventory.rowNumber = count;
                    invalidInventories.add(invalidInventory);
                    continue;
                }

                if (!inventoryInfo.macAddress.matches(MAC_PATTERN))
                {
                    invalidInventory.data = inventoryInfo.macAddress;
                    invalidInventory.columnName = InvalidInventory.MAC_COLUMN;
                    invalidInventory.error = InvalidInventory.MAC_INVALID;
                    invalidInventory.rowNumber = count;
                    invalidInventories.add(invalidInventory);
                    continue;
                }

                MongoDevice deviceKeyNode = MongoDevice.getByDeviceKey(inventoryInfo.macAddress);
                if (deviceKeyNode != null)
                {
                    invalidInventory.data = inventoryInfo.macAddress;
                    invalidInventory.columnName = InvalidInventory.MAC_COLUMN;
                    invalidInventory.error = InvalidInventory.USED_DEVICE;
                    invalidInventory.rowNumber = count;
                    invalidInventories.add(invalidInventory);
                    continue;
                }
                MongoInventoryItem dbItems = MongoInventoryItem.getByRegistrationNumber(inventoryInfo.registrationNumber);
                if (dbItems != null)
                {
                    invalidInventory.data = inventoryInfo.registrationNumber;
                    invalidInventory.columnName = InvalidInventory.REG_COLUMN;
                    invalidInventory.error = InvalidInventory.REG_INVALID;
                    invalidInventory.rowNumber = count;
                    invalidInventories.add(invalidInventory);
                    continue;
                }

                int macCount = 1;
                for (InventoryInfo inInfo : invInfo)
                {
                    macCount++;
                    if ((count != macCount) && inInfo.macAddress.equals(inventoryInfo.macAddress))
                    {
                        invalidInventory = new InvalidInventory();
                        invalidInventory.data = inventoryInfo.macAddress;
                        invalidInventory.columnName = InvalidInventory.MAC_COLUMN;
                        invalidInventory.error = InvalidInventory.MAC_DUPLICATE_VALUE;
                        invalidInventory.rowNumber = count;
                        invalidInventories.add(invalidInventory);
                        break;
                    }
                }

                int regCount = 1;
                for (InventoryInfo inInfo : invInfo)
                {
                    regCount++;
                    if ((count != regCount) && inInfo.registrationNumber.equals(inventoryInfo.registrationNumber))
                    {
                        invalidInventory = new InvalidInventory();
                        invalidInventory.data = inventoryInfo.registrationNumber;
                        invalidInventory.columnName = InvalidInventory.REG_COLUMN;
                        invalidInventory.error = InvalidInventory.REG_DUPLICATE_VALUE;
                        invalidInventory.rowNumber = count;
                        invalidInventories.add(invalidInventory);
                        break;
                    }
                }
            }
            return invalidInventories;
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
        return null;
    }

    public List<MongoInventoryItem> findDuplicateRecords(List<InventoryInfo> inventoryInfos)
    {
        List<MongoInventoryItem> existingInventories = new ArrayList<>();
        int rowCount = 1;
        try
        {
            for (InventoryInfo inventoryInfo : inventoryInfos)
            {
                rowCount++;
                Model.MorphiaQuery dbItemQuery = MongoInventoryItem.q();
                dbItemQuery.or(
                        dbItemQuery.criteria("registrationNumber").equal(inventoryInfo.registrationNumber),
                        dbItemQuery.criteria("macAddress").equal(inventoryInfo.macAddress));
                MongoInventoryItem dbItem = dbItemQuery.get();

                if (dbItem != null)
                {
                    MongoInventoryItem existingInventory = new MongoInventoryItem();
                    existingInventory.setInventoryItemId(dbItem.getInventoryItemId());
                    existingInventory.setMacAddress(dbItem.getMacAddress());
                    existingInventory.setDeviceModelId(dbItem.getDeviceModelId());
                    existingInventory.setRegistrationNumber(dbItem.getRegistrationNumber());
                    existingInventories.add(existingInventory);
                    continue;
                }
                //ignore duplicate records
                MongoInventoryItem invItem = new MongoInventoryItem();
                invItem.setInventoryItemId(MongoInventoryItem.generateNewId());
                invItem.setRegistrationNumber(inventoryInfo.registrationNumber);
                invItem.setDeviceModelId(inventoryInfo.modelNumber);
                invItem.setMacAddress(inventoryInfo.macAddress);
                invItem.setActivated(false);
                invItem.save();
            }
            return existingInventories;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
        }
        return null;
    }

    public boolean isRegistrationNumberValid(String regNumber)
    {
        try
        {
            MongoInventoryItem dbItem = MongoInventoryItem.getByRegistrationNumber(regNumber);
            return dbItem != null && !dbItem.isActivated();

        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    public void updateInventory(InventoryInfo updatedItem) throws ApiException
    {
        MongoInventoryItem dupItem = MongoInventoryItem.q()
                .filter("registrationNumber", updatedItem.registrationNumber)
                .filter("deviceModelId", updatedItem.modelNumber)
                .filter("macAddress", updatedItem.macAddress)
                .filter("id <>", updatedItem.inventoryId.toString())
                .get();

        if (dupItem != null)
        {
            throw new ApiException("Duplicate inventory");
        }

        MongoInventoryItem dbItem = MongoInventoryItem.getById(updatedItem.inventoryId.toString());
        if (dbItem == null)
        {
            throw new ApiException("Inventory Item does not exist");
        }

        if (!updatedItem.macAddress.matches(MAC_PATTERN))
        {
            throw new ApiException("Mac address pattern does not matched");
        }

        // check registration number uniqueness
        MongoInventoryItem sameRegNo = MongoInventoryItem.getByRegistrationNumber(updatedItem.registrationNumber);
        if (sameRegNo != null && !sameRegNo.getInventoryItemId().equals(dbItem.getInventoryItemId()))
        {
            throw new ApiException("This registration number is in use");
        }

        dbItem.setRegistrationNumber(updatedItem.registrationNumber);
        dbItem.setDeviceModelId(updatedItem.modelNumber);
        dbItem.setMacAddress(updatedItem.macAddress);
        dbItem.save();
    }

    public void deleteInventory(String inventoryId) throws ApiException
    {
        try
        {
            MongoInventoryItem dbItem = MongoInventoryItem.getById(inventoryId);
            if (dbItem == null)
            {
                throw new ApiException("Inventory Item does not exist");
            }

            dbItem.delete();
        }
        catch (ApiException apie)
        {
            throw apie;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
        }
    }

    public void activateInventory(String regNumber) throws ApiException
    {
        try
        {
            MongoInventoryItem dbItem = MongoInventoryItem.getByRegistrationNumber(regNumber);
            if (dbItem == null)
            {
                throw new ApiException("Inventory Item does not exist");
            }

            dbItem.setActivated(true);
            dbItem.save();
        }
        catch (ApiException apie)
        {
            throw apie;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
        }
    }

    /**
     * Call this when the corresponding device is deleted/ de-registered
     *
     * @param macAddress
     */
    public void deactivateInventory(String macAddress)
    {
        try
        {
            MongoInventoryItem dbItem = MongoInventoryItem.getByMacAddress(macAddress);
            if (dbItem == null)
            {
                return;
            }

            dbItem.setActivated(false);
            dbItem.save();
        }
        catch (Exception e)
        {
            Logger.error(e.getMessage());
        }
    }

    public void subscribePlatformEvents()
    {
        PlatformEventMonitor evtMon = PlatformEventMonitor.getInstance();

        /**
         *
         *  DEVICE_DELETED
         *
         */
        evtMon.subscribe(PlatformEventType.DEVICE_DELETED, new PlatformEventTask()
        {
            @Override
            public void run(Object... params) throws Exception
            {
                MongoDevice device = (MongoDevice) params[0];
                if (device == null)
                {
                    throw new IllegalArgumentException();
                }

                deactivateInventory(device.getDeviceKey());
            }
        });
    }
}
