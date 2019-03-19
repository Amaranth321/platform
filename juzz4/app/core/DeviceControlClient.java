package core;

import com.kaisquare.core.thrift.CoreException;
import com.kaisquare.core.thrift.DeviceControlService;
import com.kaisquare.util.ThriftUtil;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import platform.config.readers.ConfigsServers;
import play.Logger;

import java.net.InetSocketAddress;
import java.util.List;

public class DeviceControlClient
{

    private static DeviceControlClient instance = null;

    private ThriftUtil.Client<DeviceControlService.Iface> deviceControlServiceClient;

    private DeviceControlClient()
    {
        initClient();
    }

    public static DeviceControlClient getInstance()
    {
        if (instance == null)
        {
            instance = new DeviceControlClient();
        }

        return instance;
    }

    private void initClient()
    {
        InetSocketAddress serverAddress = ConfigsServers.getInstance().coreDeviceControlServer();
        Logger.info("Initializing DeviceControlClient (%s)", serverAddress);
        try
        {
            //retry 3 times, with an interval of 1000 milliseconds between each try
            this.deviceControlServiceClient = ThriftUtil.newServiceClient(DeviceControlService.Iface.class,
                                                                          DeviceControlService.Client.class,
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
     * Get device status.
     *
     * @param deviceId The device ID (recognized by backend).
     *
     * @return Returns the device's status:
     * "online" if the device is currently connected and able to communicate with the backend (Core Engine/RMS+).
     * "offline" if the device is not connected to the backend.
     * "error" if the device is connected but in an error state.
     * "incorrect-password" if the backend is able to connect to the device, but not log in due to invalid login credentials.
     */
    public String getDeviceStatus(String deviceId) throws CoreException
    {
        DeviceControlService.Iface deviceControlServiceIface = this.deviceControlServiceClient.getIface();
        String result;
        try
        {
            result = deviceControlServiceIface.getDeviceStatus(deviceId);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return "";
        }
    }

    /**
     * Gets the current status of an I/O pin. This is applicable only to devices
     * which have ON/OFF type I/O pins.
     *
     * @param deviceId The device ID (recognized by backend).
     * @param ioNumber - The digital I/O number of the device, starting with 0.
     *
     * @return Returns the result of the operation.
     * "on" if the pin status is ON or HIGH
     * "off" if the pin status is OFF or LOW
     * "error" on failure to read pin status. There could be several reasons of
     * failure e.g. device is offline or device doesn't have the specified I/O control.
     */
    public String getGPIO(String deviceId, String ioNumber) throws CoreException
    {
        DeviceControlService.Iface deviceControlServiceIface = this.deviceControlServiceClient.getIface();
        String result;
        try
        {
            result = deviceControlServiceIface.getGPIO(deviceId, ioNumber);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return "error";
        }
    }

    /**
     * Sets an I/O control pin ON or OFF. This is applicable only to devices which
     * have ON/OFF type digital I/O control pins.
     *
     * @param deviceId The device ID (recognized by backend).
     * @param ioNumber - The digital I/O number of the device, starting with 0.
     * @param value    - The new value to set - "on" means ON/HIGH; "off" means OFF/LOW.
     *
     * @return Returns the result of the operation.
     * "ok" on successful completion of the operation.
     * "error" on failure. There could be several reasons of failure e.g. device
     * is offline or device doesn't have the specified I/O control.
     */
    public String setGPIO(String deviceId, String ioNumber, String value) throws CoreException
    {
        DeviceControlService.Iface deviceControlServiceIface = this.deviceControlServiceClient.getIface();
        String result;
        try
        {
            result = deviceControlServiceIface.setGPIO(deviceId, ioNumber, value);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return "error";
        }
    }

    /**
     * Starts to pan a PTZ device in the specified direction.
     *
     * @param deviceId  ID of the device.
     * @param channelId Channel of the device.
     * @param direction The direction of panning: "left" or "right".
     *
     * @return Returns the result of the operation.
     * "ok" on successful completion of the operation.
     * "error" on failure. There could be several reasons of failure e.g. device
     * is offline or device doesn't have Pan feature.
     */
    public String startPanDevice(String deviceId, String channelId, String direction) throws CoreException
    {
        DeviceControlService.Iface deviceControlServiceIface = this.deviceControlServiceClient.getIface();
        String result;
        try
        {
            result = deviceControlServiceIface.startPanDevice(deviceId, channelId, direction);
            Logger.info("startPanDevice: " + result);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return "error";
        }
    }

    /**
     * Stops panning of a PTZ device.
     *
     * @param deviceId  ID of the device.
     * @param channelId Channel of the device.
     *
     * @return Returns the result of the operation.
     * "ok" on successful completion of the operation.
     * "error" on failure. There could be several reasons of failure e.g. device
     * is offline or device doesn't have Pan feature.
     */
    public String stopPanDevice(String deviceId, String channelId) throws CoreException
    {
        DeviceControlService.Iface deviceControlServiceIface = this.deviceControlServiceClient.getIface();
        String result;
        try
        {
            result = deviceControlServiceIface.stopPanDevice(deviceId, channelId);
            Logger.info("stopPanDevice: " + result);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return "error";
        }
    }

    /**
     * Starts to tilt a PTZ device in the specified direction.
     *
     * @param deviceId  ID of the device.
     * @param channelId Channel of the device.
     * @param direction The direction of panning: "up" or "down".
     *
     * @return Returns the result of the operation.
     * "ok" on successful completion of the operation.
     * "error" on failure. There could be several reasons of failure e.g. device
     * is offline or device doesn't have Tilt feature.
     */
    public String startTiltDevice(String deviceId, String channelId, String direction) throws CoreException
    {
        DeviceControlService.Iface deviceControlServiceIface = this.deviceControlServiceClient.getIface();
        String result;
        try
        {
            result = deviceControlServiceIface.startTiltDevice(deviceId, channelId, direction);
            Logger.info("startTiltDevice: " + result);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return "error";
        }
    }

    /**
     * Stops tilting of a PTZ device.
     *
     * @param deviceId  ID of the device.
     * @param channelId Channel of the device.
     *
     * @return Returns the result of the operation.
     * "ok" on successful completion of the operation.
     * "error" on failure. There could be several reasons of failure e.g. device
     * is offline or device doesn't have Tilt feature.
     */
    public String stopTiltDevice(String deviceId, String channelId) throws CoreException
    {
        DeviceControlService.Iface deviceControlServiceIface = this.deviceControlServiceClient.getIface();
        String result;
        try
        {
            result = deviceControlServiceIface.stopTiltDevice(deviceId, channelId);
            Logger.info("stopTiltDevice: " + result);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return "error";
        }
    }

    /**
     * Starts to zoom a PTZ device in the specified direction.
     *
     * @param deviceId  ID of the device.
     * @param channelId Channel of the device.
     * @param direction The direction of panning: "in" or "out".
     *
     * @return Returns the result of the operation.
     * "ok" on successful completion of the operation.
     * "error" on failure. There could be several reasons of failure e.g. device
     * is offline or device doesn't have zoom feature.
     */
    public String startZoomDevice(String deviceId, String channelId, String direction) throws CoreException
    {
        DeviceControlService.Iface deviceControlServiceIface = this.deviceControlServiceClient.getIface();
        String result;
        try
        {
            result = deviceControlServiceIface.startZoomDevice(deviceId, channelId, direction);
            Logger.info("startZoomDevice: " + result);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return "error";
        }
    }

    /**
     * Stops zooming of a PTZ device.
     *
     * @param deviceId  ID of the device.
     * @param channelId Channel of the device.
     *
     * @return Returns the result of the operation.
     * "ok" on successful completion of the operation.
     * "error" on failure. There could be several reasons of failure e.g. device
     * is offline or device doesn't have Tilt feature.
     */
    public String stopZoomDevice(String deviceId, String channelId) throws CoreException
    {
        DeviceControlService.Iface deviceControlServiceIface = this.deviceControlServiceClient.getIface();
        String result;
        try
        {
            result = deviceControlServiceIface.stopZoomDevice(deviceId, channelId);
            Logger.info("stopZoomDevice: " + result);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return "error";
        }
    }

    /**
     * Writes data to a data port of the specified device.
     *
     * @param deviceId   ID of the device.
     * @param portNumber The data port number.
     * @param data       The data to be written out.
     *
     * @return Returns the result of the operation.
     * "ok" on successful completion of the operation.
     * "error" on failure. There could be several reasons of failure e.g. device
     * is offline or device doesn't have the specified I/O control.
     */
    public String writeData(String deviceId, String portNumber, List<Byte> data) throws CoreException
    {
        DeviceControlService.Iface deviceControlServiceIface = this.deviceControlServiceClient.getIface();
        String result;
        try
        {
            result = deviceControlServiceIface.writeData(deviceId, portNumber, data);
            Logger.info("writeData: " + result);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return "error";
        }
    }

    /**
     * Reads data from a data port of the specified device.
     *
     * @param deviceId   ID of the device.
     * @param portNumber The data port number.
     *
     * @return Returns the data read from the device's data port, or null on failure.
     */
    public List<Byte> readData(String deviceId, String portNumber) throws CoreException
    {
        DeviceControlService.Iface deviceControlServiceIface = this.deviceControlServiceClient.getIface();
        List<Byte> result;
        try
        {
            result = deviceControlServiceIface.readData(deviceId, portNumber);
            Logger.info("readData: " + result);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return null;
        }
    }
}
