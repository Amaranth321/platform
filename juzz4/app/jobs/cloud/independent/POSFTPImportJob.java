package jobs.cloud.independent;

import jobs.cloud.CloudCronJob;
import lib.util.CmdExecutor;
import lib.util.Util;
import models.POSImportSettings;
import models.labels.DeviceLabel;
import models.labels.LabelStore;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.vfs2.FileObject;
import platform.Environment;
import platform.content.ftp.FTPDetails;
import platform.content.ftp.FTPHandler;
import platform.db.MongoInfo;
import platform.label.LabelManager;
import play.Logger;
import play.Play;
import play.jobs.On;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
@On("cron.POS.FTPImportJob")
public class POSFTPImportJob extends CloudCronJob
{
    private static final String TMP_DIR = Play.applicationPath + "/public/files/tmp/pos/";
    private static final File PARSER_FILE;
    private static final String PARSER_NAME = "default";
    private static final String REMOTE_SUCCESS_FOLDER = "_POS_IMPORTED_FILES";
    private static final String REMOTE_FAIL_FOLDER = "_POS_IMPORT_FAILED";

    private static MongoInfo mongoServerInfo;

    static
    {
        //verify tmp folder
        File tmpFolder = new File(TMP_DIR);
        if (!tmpFolder.exists())
        {
            tmpFolder.mkdirs();
        }

        //initialize parser file
        String parserFile = Play.applicationPath + "/resources/pos/posimport.jar";
        PARSER_FILE = new File(parserFile);
    }

    @Override
    public boolean init()
    {
        if (!PARSER_FILE.exists())
        {
            Logger.error("[%s] POS parser not found (%s)", getClass().getSimpleName(), PARSER_FILE.getAbsolutePath());
            return false;
        }

        //initialize mongo info
        mongoServerInfo = Environment.getInstance().getMongoServerInfo();

        return super.init();
    }

    @Override
    public void doJob()
    {
        try
        {
            List<POSImportSettings> importSettings = POSImportSettings.q().fetchAll();
            for (POSImportSettings importSetting : importSettings)
            {
                if (!importSetting.isEnabled())
                {
                    continue;
                }

                // connect
                FTPDetails serverDetails = importSetting.getFtpDetails();
                FTPHandler ftpHandler = new FTPHandler(serverDetails);
                ftpHandler.connect();

                Iterable<FileObject> remoteFileObjects = ftpHandler.getRemoteChildren(serverDetails.getDirectory());
                for (FileObject remoteFileObject : remoteFileObjects)
                {
                    String fileName = remoteFileObject.getName().getBaseName();
                    String fileExt = remoteFileObject.getName().getExtension();

                    // filter
                    if (!remoteFileObject.isFile() || !fileExt.equalsIgnoreCase("csv"))
                    {
                        continue;
                    }

                    // download
                    String localFilePath = TMP_DIR + fileName;
                    ftpHandler.download(serverDetails.getDirectory() + fileName, localFilePath);

                    // import
                    boolean success = importFileToDb(importSetting.getBucketId() + "", localFilePath);

                    // move
                    String desRemoteDir = Util.combine(serverDetails.getDirectory(), success ? REMOTE_SUCCESS_FOLDER : REMOTE_FAIL_FOLDER);
                    ftpHandler.createRemoteFolder(desRemoteDir);

                    String srcRemoteFile = Util.combine(serverDetails.getDirectory(), fileName);
                    String desRemoteFile = Util.combine(desRemoteDir, fileName);
                    ftpHandler.moveRemote(srcRemoteFile, desRemoteFile);
                    Logger.info("Moving POS file (%s) to (%s)", srcRemoteFile, desRemoteFile);
                }
            }
        }
        catch (Exception e)
        {
            Logger.error(e.getMessage());
        }
    }

    private boolean importFileToDb(String bucketId, String filePath)
    {
        try
        {
            // verify store label
            List<DeviceLabel> bucketLabels = LabelManager.getInstance().getBucketLabels(Long.parseLong(bucketId));
            File file = new File(filePath);
            String labelName = FilenameUtils.removeExtension(file.getName());

            LabelStore storeLabel = null;
            for (DeviceLabel bucketLabel : bucketLabels)
            {
                if (bucketLabel.getLabelName().equals(labelName) && bucketLabel instanceof LabelStore)
                {
                    storeLabel = (LabelStore) bucketLabel;
                    break;
                }
            }
            if (storeLabel == null)
            {
                throw new Exception("Store label not found: " + labelName);
            }

            // import
            List<String> cmdParamList = new ArrayList<>();
            cmdParamList.add("java");

            cmdParamList.add("-jar");
            cmdParamList.add(PARSER_FILE.getAbsolutePath());

            cmdParamList.add("--db-host");
            cmdParamList.add(mongoServerInfo.getHost());

            cmdParamList.add("--db-port");
            cmdParamList.add(String.valueOf(mongoServerInfo.getPort()));

            if (!Util.isNullOrEmpty(mongoServerInfo.getUsername()) &&
                    !Util.isNullOrEmpty(mongoServerInfo.getPassword()))
            {
                cmdParamList.add("--db-user");
                cmdParamList.add(mongoServerInfo.getUsername());
                cmdParamList.add("--db-pass");
                cmdParamList.add(mongoServerInfo.getPassword());
            }

            cmdParamList.add("--file-path");
            cmdParamList.add(filePath);

            cmdParamList.add("--file-timezone");
            cmdParamList.add(storeLabel.getLocation().getTimeZoneId());

            cmdParamList.add("--kup-bucketid");
            cmdParamList.add(String.valueOf(bucketId));

            cmdParamList.add("--kup-label");
            cmdParamList.add(labelName);

            cmdParamList.add("--parser");
            cmdParamList.add(PARSER_NAME);

            // execute
            int errorCode = CmdExecutor.readErrorCode(cmdParamList);

            boolean success = errorCode == 0;
            if (success)
            {
                Logger.info("POS import success: %s", filePath);
            }
            else
            {
                Logger.error("POS import failure: %s", filePath);
            }

            return success;
        }
        catch (Exception e)
        {
            Logger.error(e, filePath);
            return false;
        }
    }
}
