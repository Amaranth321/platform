package platform.kaisyncwrapper.cloud.tasks;

import com.kaisquare.kaisync.ISyncWriteFile;
import com.kaisquare.kaisync.platform.IPlatformSync;
import com.kaisquare.sync.ITask;
import com.kaisquare.util.Hash;

import models.NodeCommand;
import platform.CloudSyncManager;
import play.Logger;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloudPullLog implements ITask
{

    private static final Object sLock = new Object();
    private static final AtomicBoolean lockIndicator = new AtomicBoolean(false);
    private static final String LOG_FILE = "kainode-log.tar.gz";

    @Override
    public boolean doTask(NodeCommand command)
    {

        BufferedReader reader = null;
        Process process = null;
        boolean ret = false;

        try
        {
            if (!lockIndicator.getAndSet(true))
            {
                synchronized (sLock)
                {
                    File logPath = new File(".");
                    process = new ProcessBuilder("service", "kainode", "exportlog", logPath.getAbsolutePath()).start();

                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        Logger.info("exportlogs: %s", line);
                    }

                    File logFile = new File(logPath, LOG_FILE);
                    if (logFile.exists())
                    {
                        CloudSyncManager csm = CloudSyncManager.getInstance();
                        String hash = Hash.getFileChecksum(logFile);
                        IPlatformSync platformSync = csm.getPlatformSync();
                        ISyncWriteFile uploadFile = csm.uploadFile(hash);
                        uploadFile.setMetadata("category", "logfile");
                        uploadFile.setMetadata("sha1", hash);
                        uploadFile.setMetadata("contentType", "application/tar+gzip");
                        uploadFile.setMetadata("nodeId", command.getNodeId());

                        InputStream is = null;
                        OutputStream os = null;
                        try
                        {
                            is = new BufferedInputStream(new FileInputStream(logFile));
                            os = uploadFile.getOutputStream();

                            byte[] buffer = new byte[8192];
                            int read = 0;

                            while ((read = is.read(buffer)) > 0)
                            {
                                os.write(buffer, 0, read);
                            }
                            Logger.info("uploaded log file: %s", hash);

                            if (platformSync.addLogFile(command.getNodeId(), hash + ".tar.gz"))
                            {
                                Logger.info("added log file to cloud: %s", hash);
                                ret = true;
                            }
                        }
                        catch (IOException e)
                        {
                            Logger.error(e, "uploadFile");
                        }
                        finally
                        {
                            if (is != null)
                            {
                                try
                                {
                                    is.close();
                                }
                                catch (IOException e)
                                {
                                }
                            }
                            if (os != null)
                            {
                                try
                                {
                                    os.close();
                                }
                                catch (IOException e)
                                {
                                }
                            }
                            platformSync.close();
                        }
                        logFile.delete();
                    }
                }
                lockIndicator.set(false);
            }
            else
            {
                Logger.error("previous log exporting job is running");
            }

        }
        catch (Exception e)
        {
            Logger.error(e, this.getClass().getSimpleName());
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e1)
                {
                }
            }
            if (process != null)
            {
                process.destroy();
            }
        }

        return ret;
    }

}
