package platform.system.usb;

import ext.usbdrivedetector.USBStorageDevice;
import ext.usbdrivedetector.detectors.AbstractStorageDeviceDetector;

import java.util.List;

/**
 * Uses the modified version of an external library from
 * <a href="https://github.com/samuelcampos/usbdrivedetector">usbdrivedetector</a>
 *
 * @author Aye Maung
 * @since v4.4
 */
public class USBClient
{
    private static final USBClient instance = new USBClient();

    public static USBClient getInstance()
    {
        return instance;
    }

    public List<USBStorageDevice> getUsbStorageDevices()
    {
        return AbstractStorageDeviceDetector.getInstance().getRemovableDevices();
    }

    public USBStorageDevice find(String usbIdentifier)
    {
        List<USBStorageDevice> drives = getUsbStorageDevices();
        for (USBStorageDevice drive : drives)
        {
            if (drive.getRootDirectory().getAbsolutePath().equals(usbIdentifier))
            {
                return drive;
            }
        }

        return null;
    }

    private USBClient()
    {
    }
}
