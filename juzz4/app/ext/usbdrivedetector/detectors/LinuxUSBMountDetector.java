package ext.usbdrivedetector.detectors;

import ext.usbdrivedetector.USBStorageDevice;
import lib.util.CmdExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * requires usbmount to auto-mount drives
 *
 * @author Aye Maung
 * @since v4.4
 */
public class LinuxUSBMountDetector extends AbstractStorageDeviceDetector
{
    private static final String listMountedUSBCmd = "df | grep /media/usb | awk {'print $6'}";

    /**
     * Returns the storage devices connected to the computer.
     *
     * @return the list of the USB storage devices
     */
    @Override
    public List<USBStorageDevice> getRemovableDevices()
    {
        List<USBStorageDevice> usbDrives = new ArrayList<>();
        List<String> mountedList = CmdExecutor.readTillProcessEnds(listMountedUSBCmd, false);
        for (String mounted : mountedList)
        {
            addUSBDevice(usbDrives, mounted);
        }

        return usbDrives;
    }
}
