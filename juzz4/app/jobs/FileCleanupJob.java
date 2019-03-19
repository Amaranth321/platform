package jobs;

import lib.util.Util;
import platform.Environment;
import platform.ReportManager;
import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.On;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Clean up stuff located on the server. As such, this job should run on all instances
 *
 * @author Aye Maung
 */
@On("cron.Cleanup.files")
public class FileCleanupJob extends Job
{
    private static final List<String> directories = Arrays.asList(
            ReportManager.REPORT_DIRECTORY
    );

    private int cleanupCount = 0;

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
        try
        {
            cleanupCount = 0;
            for (String dir : directories)
            {
                Logger.info(Util.whichClass() + "Cleaning up server directory (%s)", dir);
                File reportDir = new File(Play.applicationPath + dir);
                recursiveCleanup(reportDir);
            }
            Logger.info(Util.whichClass() + "%s files deleted", cleanupCount);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    private void recursiveCleanup(File directory) throws IOException
    {
        if (!directory.exists())
        {
            Logger.error(Util.whichClass() + "Skipped (directory not found : %s)", directory.getAbsolutePath());
            return;
        }

        for (File file : directory.listFiles())
        {
            if (file.isDirectory())
            {
                recursiveCleanup(file);
            }
            else
            {
                file.delete();
                cleanupCount++;
            }
        }
    }

}

