package platform.db.gridfs;

import lib.util.Util;
import platform.content.FileFormat;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class GridFsDetails
{
    private final String filename;
    private final String blobId;
    private final GridFsFileGroup group;
    private final FileFormat format;

    /**
     * @param filename just the file name (including extension if any). Don't include the path.
     * @param blobId   GridFs blob ID
     * @param group    group
     * @param format   file format
     */
    public GridFsDetails(String filename, String blobId, GridFsFileGroup group, FileFormat format)
    {
        this.filename = Util.sanitizeFilename(filename);
        this.blobId = blobId;
        this.group = group;
        this.format = format;
    }

    public String getFilename()
    {
        return filename;
    }

    public String getBlobId()
    {
        return blobId;
    }

    public GridFsFileGroup getGroup()
    {
        return group;
    }

    public FileFormat getFormat()
    {
        return format;
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s", blobId, filename);
    }


}
