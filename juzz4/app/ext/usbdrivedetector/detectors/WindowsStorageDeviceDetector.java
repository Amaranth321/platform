/*
 * Copyright 2014 samuelcampos.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ext.usbdrivedetector.detectors;


import ext.usbdrivedetector.USBStorageDevice;
import ext.usbdrivedetector.process.CommandLineExecutor;
import play.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author samuelcampos
 */
public class WindowsStorageDeviceDetector extends AbstractStorageDeviceDetector
{
    /**
     * wmic logicaldisk where drivetype=2 get description,deviceid,volumename
     */
    private static final String windowsDetectUSBCommand = "wmic logicaldisk where drivetype=2 get deviceid";

    private final CommandLineExecutor commandExecutor;

    public WindowsStorageDeviceDetector()
    {
        commandExecutor = new CommandLineExecutor();
    }

    @Override
    public List<USBStorageDevice> getRemovableDevices()
    {
        ArrayList<USBStorageDevice> listDevices = new ArrayList<USBStorageDevice>();

        try
        {
            commandExecutor.executeCommand(windowsDetectUSBCommand);

            String outputLine;
            while ((outputLine = commandExecutor.readOutputLine()) != null)
            {

                if (!outputLine.isEmpty() && !"DeviceID".equals(outputLine))
                {
                    addUSBDevice(listDevices, outputLine + File.separatorChar);
                }
            }

        }
        catch (IOException e)
        {
            Logger.error(e, "");
        }
        finally
        {
            try
            {
                commandExecutor.close();
            }
            catch (IOException e)
            {
                Logger.error(e, "");
            }
        }

        return listDevices;
    }

}
