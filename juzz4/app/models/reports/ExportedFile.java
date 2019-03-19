package models.reports;

import com.google.code.morphia.annotations.Entity;
import lib.util.Util;
import org.apache.commons.lang.RandomStringUtils;
import platform.content.FileFormat;
import platform.db.gridfs.GridFsDetails;
import platform.db.gridfs.GridFsFileGroup;
import platform.db.gridfs.GridFsHelper;
import platform.db.QueryHelper;
import play.Logger;
import play.modules.morphia.Model;

import java.io.InputStream;

/**
 * Exported files are stored in gridfs to support the load-balanced setup.
 * {@link jobs.DbCleanupJob} will clean up these files using <code>removeFilesOlderThan()</code> function
 *
 * @author Aye Maung
 * @since v4.3
 */
@Entity
public class ExportedFile extends Model {
    private final String downloadIdentifier;
    private final String bucketName;
    private final GridFsDetails gridFsDetails;

    public static ExportedFile createNew(String bucketName,
                                         String filename,
                                         FileFormat format,
                                         InputStream inputStream) {
        GridFsDetails gridFsDetails = GridFsHelper.saveFileInputStream(
                filename,
                inputStream,
                format,
                GridFsFileGroup.TMP_EXPORTED_FILES);

        if (gridFsDetails == null) {
            return null;
        }

        ExportedFile exportedFile = new ExportedFile(gridFsDetails, bucketName);
        return exportedFile.save();
    }

    public static ExportedFile findByIdentifier(String downloadIdentifier) {
        return ExportedFile.q()
                .filter("downloadIdentifier", downloadIdentifier)
                .first();
    }

    public static void removeEntriesOlderThan(int days) {
        //must loop in order to call overridden delete() function
        Iterable<ExportedFile> expiredFiles = QueryHelper.getEntriesOlderThan(days, q()).fetch();
        for (ExportedFile file : expiredFiles) {
            file.delete();
        }
    }

    private ExportedFile(GridFsDetails gridFsDetails, String bucketName) {
        this.bucketName = bucketName;
        this.gridFsDetails = gridFsDetails;
        downloadIdentifier = RandomStringUtils.randomAlphanumeric(50);
    }

    public String getDownloadIdentifier() {
        return downloadIdentifier;
    }

    public GridFsDetails getFileDetails() {
        return gridFsDetails;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getDownloadUrl() {
        return String.format("/api/%s/downloadexportedfile?identifier=%s", bucketName, downloadIdentifier);
    }

    @Override
    public ExportedFile delete() {
        boolean result = GridFsHelper.removeFile(gridFsDetails);
        if (!result) {
            Logger.error(Util.whichFn() + "failed: %s", gridFsDetails.getFilename());
        } else {
            super.delete();
        }
        return this;
    }

}
