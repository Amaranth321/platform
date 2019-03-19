package core;

import com.kaisquare.core.thrift.CoreException;
import com.kaisquare.core.thrift.DeviceDetails;
import com.kaisquare.core.thrift.DeviceManagementService;
import com.kaisquare.core.thrift.DeviceModel;
import com.kaisquare.util.ThriftUtil;
import lib.util.Util;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import platform.config.readers.ConfigsServers;
import play.Logger;

import java.net.InetSocketAddress;
import java.util.List;

public class DeviceManagementClient
{
    private static DeviceManagementClient instance = null;

    private ThriftUtil.Client<DeviceManagementService.Iface> deviceManagementServiceClient;

    private DeviceManagementClient()
    {
        initClient();
    }

    public static DeviceManagementClient getInstance()
    {
        if (instance == null)
        {
            instance = new DeviceManagementClient();
        }

        return instance;
    }

    private void initClient()
    {
        InetSocketAddress serverAddress = ConfigsServers.getInstance().coreDeviceManagementServer();
        Logger.info("Initializing DeviceManagementClient (%s)", serverAddress);
        try
        {
            //retry 3 times, with an interval of 1000 milliseconds between each try
            this.deviceManagementServiceClient = ThriftUtil.newServiceClient(DeviceManagementService.Iface.class,
                                                                             DeviceManagementService.Client.class,
                                                                             serverAddress.getHostName(),
                                                                             serverAddress.getPort(),
                                                                             ThriftUtil.DEFAULT_TIMEOUT_MILLIS);
        }
        catch (TTransportException e)
        {
            Logger.error(e, "");
        }
    }

    /**
     * Add a new device model.
     *
     * @param model A DeviceModel object with properties set as desired. Id should be empty.
     *
     * @return Id of the new device model.
     */
    public String addModel(DeviceModel model) throws CoreException
    {
        Logger.info("Adding a device model.");
        DeviceManagementService.Iface deviceManagementServiceIface = this.deviceManagementServiceClient.getIface();
        String result = "";
        try
        {
            result = deviceManagementServiceIface.addModel(model);
            //this.model.setId(result);
            //this.details.setModelId(this.model.getId());
            Logger.info("New Model ID: " + model.getId());
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return "";
        }
    }

    /**
     * Update the properties of a device model.
     *
     * @param model A DeviceModel object with all properties set as desired. Id should be a valid model ID which is to be updated.
     *
     * @return true on success, false otherwise.
     */
    public boolean updateModel(DeviceModel model) throws CoreException
    {
        Logger.info("Updating a device model (model ID = %s).", model.getId());
        DeviceManagementService.Iface deviceManagementServiceIface = this.deviceManagementServiceClient.getIface();
        boolean result = false;
        try
        {
            result = deviceManagementServiceIface.updateModel(model);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return false;
        }
    }

    /**
     * Delete a device model.
     *
     * @param model The DeviceModel object to be deleted. ID must be valid.
     *
     * @return true on success, false otherwise.
     */
    public boolean deleteModel(DeviceModel model) throws CoreException
    {
        Logger.info("Deleting a device model (model ID = %s).", model.getId());
        DeviceManagementService.Iface deviceManagementServiceIface = this.deviceManagementServiceClient.getIface();
        boolean result = false;
        try
        {
            result = deviceManagementServiceIface.deleteModel(model.getId());
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return result;
        }
    }

    /**
     * Gets list of all device models in the system.
     *
     * @return On success, returns a list of all device models available, null otherwise.
     */
    public List<DeviceModel> listModels() throws CoreException
    {
        DeviceManagementService.Iface deviceManagementServiceIface = this.deviceManagementServiceClient.getIface();
        List<DeviceModel> allModels;
        try
        {
            allModels = deviceManagementServiceIface.listModels();
            return allModels;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return null;
        }
    }

    /**
     * Add a new device.
     *
     * @param device A DeviceDetails object filled with the desired details. ID must be empty.
     *
     * @return On success, returns the ID of the newly created device; returns empty string otherwise.
     */
    public String addDevice(DeviceDetails device) throws CoreException
    {
        DeviceManagementService.Iface deviceManagementServiceIface = this.deviceManagementServiceClient.getIface();
        String result = "";
        try
        {
            result = deviceManagementServiceIface.addDevice(device);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return "";
        }
    }

    /**
     * Updates a device's properties.
     *
     * @param device A DeviceDetails object with a valid device ID and other properties set as desired.
     *
     * @return true on success, false otherwise.
     */
    public boolean updateDevice(DeviceDetails deviceDetails) throws CoreException
    {
        DeviceManagementService.Iface deviceManagementServiceIface = this.deviceManagementServiceClient.getIface();
        try
        {
            return deviceManagementServiceIface.updateDevice(deviceDetails);
        }
        catch (TException e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    /**
     * Deletes a device.
     *
     * @param device Object that represents the device to be deleted. ID must be valid.
     *
     * @return true on success, false otherwise.
     */
    public boolean deleteDevice(DeviceDetails device) throws CoreException
    {
        DeviceManagementService.Iface deviceManagementServiceIface = this.deviceManagementServiceClient.getIface();
        boolean result = false;
        try
        {
            result = deviceManagementServiceIface.deleteDevice(device.getId());
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return result;
        }
    }

    /**
     * @param filter "all" for all devices, "pending" for devices which are
     *               not yet activated, "active" for all active devices.
     */
    public List<DeviceDetails> listDevices(String filter) throws CoreException
    {
        DeviceManagementService.Iface deviceManagementServiceIface = this.deviceManagementServiceClient.getIface();
        List<DeviceDetails> allDevices;
        try
        {
            allDevices = deviceManagementServiceIface.listDevices(filter);
            return allDevices;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return null;
        }
    }

    /**
     * Activates the specified device.
     *
     * @param device Object that represents the device to be activated. ID must be valid.
     *
     * @return true on success, false otherwise.
     */
    public boolean activateDevice(DeviceDetails device) throws CoreException
    {
        Logger.info("Activating a device (device ID = %s).", device.getId());
        DeviceManagementService.Iface deviceManagementServiceIface = this.deviceManagementServiceClient.getIface();
        boolean result = false;
        try
        {
            result = deviceManagementServiceIface.activateDevice(device);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return false;
        }
    }

}
