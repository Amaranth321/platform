package platform.api.asynctasks;

import ext.usbdrivedetector.USBStorageDevice;
import lib.util.Util;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import platform.api.AsyncAPITask;
import platform.content.ZipHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class ZipAndExportToUSB extends AsyncAPITask
{
    private static final int bufferSize = 4096;
    private final List<File> fileList;
    private final USBStorageDevice usbDrive;
    private final String outputFilename;

    //track zipping
    private List<Long> zipFileSizes = new ArrayList<>();
    private long zipTotalSize = 0;
    private int zippedCount = 0;
    private long zippedSize = 0;

    //track copying
    private long totalSize = 0;
    private long totalCopied = 0;
    private long updateMarker = (1024 * 1024);
    private int currentPercent = 0;

    public ZipAndExportToUSB(long userId,
                             String apiName,
                             List<File> fileList,
                             USBStorageDevice usbDrive,
                             String outputFilename)
    {
        super(userId, apiName);
        this.fileList = fileList;
        this.usbDrive = usbDrive;
        this.outputFilename = outputFilename;

        //for tracking
        for (File file : fileList)
        {
            zipTotalSize += file.length();
            zipFileSizes.add(file.length());
        }
    }

    @Override
    protected void executeTask() throws ZipException, IOException
    {
        File zippedFile = startZipping();
        //startExporting(zippedFile);
    }

    private File startZipping() throws ZipException
    {
        taskStarted("creating-zip-file");
        progressChanged(0);

        String tmpDir = usbDrive.getRootDirectory().getAbsolutePath(); //RecordingManager.getTempDirectory();

        ZipFile zipped = new ZipFile(Util.combine(tmpDir, outputFilename));
        ZipParameters parameters = ZipHelper.getDefaultParameters();
        for (File file : fileList)
        {
            zipped.addFile(file, parameters);

            if (listenerExists())
            {
                updateZipProgress();
            }
        }

        return zipped.getFile();
    }

    private void updateZipProgress()
    {
        zippedCount++;
        zippedSize += zipFileSizes.get(zippedCount - 1);
        progressChanged(Math.round(zippedSize * 100 / zipTotalSize));
    }

    /*private void startExporting(File zipFile) throws IOException
    {
        taskStarted("copying-zip-to-usb");
        totalSize = zipFile.length();
        progressChanged(0);

        File srcFile = zipFile;
        File destFile = new File(Util.combine(usbDrive.getRootDirectory().getAbsolutePath(), outputFilename));

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(srcFile));
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile));

        byte[] buffer = new byte[bufferSize];
        int lastReadCnt;
        while ((lastReadCnt = bis.read(buffer)) > 0)
        {
            bos.write(buffer, 0, lastReadCnt);

            if (listenerExists())
            {
                updateCopyProgress(lastReadCnt);
            }
        }

        bos.flush();
        FileUtil.close(bis);
        FileUtil.close(bos);

        if (zipFile.exists())
        {
            zipFile.delete();
        }
    }

    private void updateCopyProgress(int lastReadCnt)
    {
        totalCopied += lastReadCnt;

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
    }*/
}
