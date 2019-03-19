package jobs.node;

import models.MongoDevice;
import platform.StorageManager;
import play.Logger;
import play.jobs.Job;

import java.util.List;

public class NodeStartupTasks extends Job
{
    @Override
    public void doJob()
    {
        refreshRecordingRetentionDays();
    }

    private void refreshRecordingRetentionDays()
    {
        List<MongoDevice> cameras = MongoDevice.q().fetchAll();
        for (MongoDevice camera : cameras)
        {
            try
            {
                StorageManager.getInstance().setRecordingRetention(camera);
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }
    }
}
