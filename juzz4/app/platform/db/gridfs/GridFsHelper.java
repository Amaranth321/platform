package platform.db.gridfs;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import platform.ReportManager;
import platform.content.FileFormat;
import play.Logger;
import play.Play;
import play.modules.morphia.Model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Use to create / retrieve GridFs data in Mongodb
 */
public class GridFsHelper
{
    private static final String TMP_DIR = Play.applicationPath + ReportManager.REPORT_DIRECTORY + "gridfs/";

    static
    {
        File gridDir = new File(TMP_DIR);
        if (!gridDir.exists())
        {
            gridDir.mkdirs();
        }
    }

    /**
     * Save File into Mongo GridFs and return grid id. Exception will return null.
     *
     * @param model      Any class extend by Model
     * @param blobName   Blob Name of the Class
     * @param binaryData file data in byte[]
     *
     * @return null/blobId
     */
    @Deprecated
    public static String setGridFsFile(Model model, String blobName, byte[] binaryData)
    {
        try
        {
            GridFS gfs = new GridFS(model.db(), blobName);
            GridFSInputFile gfsFile = gfs.createFile(binaryData);
            gfsFile.setFilename(UUID.randomUUID().toString());
            gfsFile.save();

            return gfsFile.getId().toString();
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
        return null;
    }

    /**
     * Get Mongo GridFs File from blob id with blob name and return file in Base64String. Exception will return null.
     *
     * @param model    Any class extend by Model
     * @param blobName Blob Name of the Class
     * @param blobId   blob Id
     *
     * @return null/Base64String
     */
    @Deprecated
    public static String getBlobAsBase64String(Model model, String blobName, String blobId)
    {
        try
        {
            GridFS gfs = new GridFS(model.db(), blobName);
            GridFSDBFile file = gfs.findOne(new ObjectId(blobId));
            if (file != null)
            {
                return Base64.encodeBase64String(IOUtils.toByteArray(file.getInputStream()));
            }
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
        return null;
    }

    /**
     * Get Mongo GridFs File from blob id with blob name and return file. Exception will return null.
     *
     * @param model    Any class extend by Model
     * @param blobName Blob Name of the Class
     * @param blobId   blob Id
     *
     * @return null/file
     */
    @Deprecated
    public static File getBlobAsFile(Model model, String blobName, String blobId)
    {
        try
        {
            GridFS gfs = new GridFS(model.db(), blobName);
            GridFSDBFile file = gfs.findOne(new ObjectId(blobId));
            if (file != null)
            {
                return stream2file(file.getInputStream());
            }
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
        return null;
    }

    /**
     * Get Mongo GridFs File from blob id with blob name and return as GridFSDBFile. Exception will return null.
     *
     * @param model    Any class extend by Model
     * @param blobName Blob Name of the Class
     * @param blobId   blob Id
     *
     * @return null/file
     */
    @Deprecated
    public static GridFSDBFile getBlobAsGridFSDBFile(Model model, String blobName, String blobId)
    {
        try
        {
            GridFS gfs = new GridFS(model.db(), blobName);
            GridFSDBFile file = gfs.findOne(new ObjectId(blobId));
            if (file != null)
            {
                return file;
            }
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
        return null;
    }

    /**
     * Stores binary data in gridFs
     *
     * @param filename   File name
     * @param binaryData File bytes
     * @param format     File format
     * @param group      For file grouping, stored files will be grouped under this name.
     *                   Create a new ENUM if needed. Ensure that the file is stored under the related group.
     *
     * @return GridFsDetails
     */
    public static GridFsDetails saveBinaryFile(String filename,
                                               byte[] binaryData,
                                               FileFormat format,
                                               GridFsFileGroup group)
    {
        try
        {
            GridFS gfs = new GridFS(Model.db(), group.name());
            GridFSInputFile gfsFile = gfs.createFile(binaryData);
            gfsFile.setFilename(filename);
            gfsFile.save();

            GridFsDetails details = new GridFsDetails(filename, gfsFile.getId().toString(), group, format);
            return details;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    /**
     * Stores file in gridFs using InputStream
     *
     * @param filename    File name
     * @param inputStream File Inputstream
     * @param format      File format
     * @param group       For file grouping, stored files will be grouped under this name.
     *                    Create a new ENUM if needed. Ensure that the file is stored under the related group.
     *
     * @return GridFsDetails
     */
    public static GridFsDetails saveFileInputStream(String filename,
                                                    InputStream inputStream,
                                                    FileFormat format,
                                                    GridFsFileGroup group)
    {
        try
        {
            GridFS gfs = new GridFS(Model.db(), group.name());
            GridFSInputFile gfsFile = gfs.createFile(inputStream);
            gfsFile.setFilename(filename);
            gfsFile.save();

            GridFsDetails details = new GridFsDetails(filename, gfsFile.getId().toString(), group, format);
            return details;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    /**
     * Gets the stored gridFs File
     *
     * @param details GridFs details to retrieve the stored file
     *
     * @return GridFSDBFile
     */
    public static GridFSDBFile getGridFSDBFile(GridFsDetails details)
    {
        try
        {
            GridFS gfs = new GridFS(Model.db(), details.getGroup().name());
            GridFSDBFile file = gfs.findOne(new ObjectId(details.getBlobId()));
            if (file != null)
            {
                return file;
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
        return null;
    }

    public static InputStream getFileInputStream(GridFsDetails details)
    {
        return getGridFSDBFile(details).getInputStream();
    }

    public static String asBase64(GridFsDetails details)
    {
        try
        {
            GridFSDBFile file = GridFsHelper.getGridFSDBFile(details);
            byte[] bytes = IOUtils.toByteArray(file.getInputStream());
            return Base64.encodeBase64String(bytes);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    /**
     * Removes the stored gridFs file
     *
     * @param details
     *
     * @return true/false
     */
    public static boolean removeFile(GridFsDetails details)
    {
        try
        {
            GridFS gfs = new GridFS(Model.db(), details.getGroup().name());
            gfs.remove(new ObjectId(details.getBlobId()));
            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    /**
     * Use to convert InputStream to temporary File Object
     */
    private static File stream2file(InputStream in) throws IOException
    {
        final File tempFile = File.createTempFile(ReportManager.REPORT_DIRECTORY +
                                                  UUID.randomUUID().toString(), ".gridfs");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile))
        {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }
}
