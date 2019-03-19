package controllers.api;

import controllers.interceptors.APIInterceptor;
import ext.usbdrivedetector.USBStorageDevice;
import platform.system.usb.USBClient;
import platform.system.usb.USBDriveInfo;
import play.mvc.With;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.4
 */
@With(APIInterceptor.class)
public class SystemController extends APIController
{
    public static void getusbdrives()
    {
        try
        {
            List<USBStorageDevice> drives = USBClient.getInstance().getUsbStorageDevices();
            List<USBDriveInfo> infoList = new ArrayList<>();
            for (USBStorageDevice drive : drives)
            {
                infoList.add(new USBDriveInfo(drive));
            }

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("drives", infoList);
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
