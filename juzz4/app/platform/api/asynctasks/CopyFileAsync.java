package platform.api.asynctasks;

import com.kaisquare.util.FileUtil;
import lib.util.Util;
import platform.api.AsyncAPITask;

import java.io.*;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class CopyFileAsync extends AsyncAPITask
{
    private static final int bufferSize = 4096;

    private final File srcFile;
    private final File destFile;

    //progress tracking
    private long totalSize = 0;
    private long totalCopied = 0;
    private long updateMarker = (1024 * 1024);
    private int currentPercent = 0;

    public CopyFileAsync(long userId,
                         String apiName,
                         File srcFile,
                         File destFolder,
                         String outputFilename) throws FileNotFoundException
    {
        super(userId, apiName);

        if (srcFile == null || !srcFile.exists())
        {
            throw new FileNotFoundException(srcFile.getAbsolutePath());
        }

        if (destFolder == null || !destFolder.exists())
        {
            throw new FileNotFoundException(destFolder.getAbsolutePath());
        }

        this.srcFile = srcFile;
        this.destFile = new File(Util.combine(destFolder.getAbsolutePath(), outputFilename));

        totalSize = srcFile.length();
    }

    @Override
    public void executeTask() throws IOException
    {
        taskStarted("CopyFileAsync");
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(srcFile));
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile));

        byte[] buffer = new byte[bufferSize];
        int lastReadCnt;
        while ((lastReadCnt = bis.read(buffer)) > 0)
        {
            bos.write(buffer, 0, lastReadCnt);

            if (listenerExists())
            {
                totalCopied += lastReadCnt;
                checkProgress();
            }
        }

        bos.flush();
        FileUtil.close(bis);
        FileUtil.close(bos);
    }

    private void checkProgress()
    {
        //updates every 1MB
        if (totalCopied % updateMarker > (updateMarker - 3 * bufferSize))
        {
            int newPercent = Math.round((totalCopied * 100) / totalSize);
            if (newPercent > currentPercent)
            {
                progressChanged(newPercent);
                currentPercent = newPercent;
            }
        }
    }
}
