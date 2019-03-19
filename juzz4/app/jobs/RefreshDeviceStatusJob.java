package jobs;

import core.CoreClient;
import models.MongoDevice;
import platform.Environment;
import platform.devices.DeviceLog;
import platform.devices.DeviceStatus;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;

import java.util.List;

@Every("25mn")
public class RefreshDeviceStatusJob extends Job
{
    @Override
    public boolean init()
    {
        if (!Environment.getInstance().isStartupTasksCompleted())
        {
            return false;
        }

        return super.init();
    }

    @Override
    public void doJob()
    {
        List<MongoDevice> devices = MongoDevice.q().fetchAll();
        for (MongoDevice device : devices)
        {
            try
            {
                String statusString = CoreClient.getInstance().deviceControlClient.getDeviceStatus(device.getCoreDeviceId());
                DeviceStatus newStatus;
                switch (statusString.toLowerCase())
                {
                    case "online":
                        newStatus = DeviceStatus.CONNECTED;
                        break;

                    case "offline":
                    case "error":
                    case "incorrect-password":
                        newStatus = DeviceStatus.DISCONNECTED;
                        break;

                    default:
                        Logger.info("[%s] unrecognized status: %s", getClass().getSimpleName(), statusString);
                        newStatus = DeviceStatus.UNKNOWN;
                }

                if (device.getStatus() != newStatus)
                {
                    device.setStatus(newStatus);
                    device.save();
                    DeviceLog.createLog(Long.parseLong(device.getDeviceId()),
                                        String.format("status changed (%s)", statusString));
                }
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }
    }
}

