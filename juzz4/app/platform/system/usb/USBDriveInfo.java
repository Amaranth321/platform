package platform.system.usb;

import ext.usbdrivedetector.USBStorageDevice;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class USBDriveInfo
{
    private final String identifier;
    private final String systemDisplayName;
    private final String deviceName;
    private final boolean canRead;
    private final boolean canWrite;
    private final boolean canExecute;
    private final long freeSpace;

    public USBDriveInfo(USBStorageDevice usbDrive)
    {
        this.identifier = usbDrive.getRootDirectory().getAbsolutePath();
        this.systemDisplayName = usbDrive.getSystemDisplayName();
        this.deviceName = usbDrive.getDeviceName();
        this.canRead = usbDrive.canRead();
        this.canWrite = usbDrive.canWrite();
        this.canExecute = usbDrive.canExecute();
        this.freeSpace = usbDrive.getRootDirectory().getUsableSpace();
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public String getDeviceName()
    {
        return deviceName;
    }

    public String getSystemDisplayName()
    {
        return systemDisplayName;
    }

    public boolean isCanRead()
    {
        return canRead;
    }

    public boolean isCanWrite()
    {
        return canWrite;
    }

    public boolean isCanExecute()
    {
        return canExecute;
    }

    public long getFreeSpace()
    {
        return freeSpace;
    }
}
