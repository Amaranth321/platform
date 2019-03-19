package platform.content.ftp;

import platform.db.gridfs.GridFsDetails;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class FTPItem
{
    private final FTPDetails ftpDetails;
    private final GridFsDetails gridFsDetails;

    public FTPItem(FTPDetails ftpDetails, GridFsDetails gridFsDetails)
    {
        this.ftpDetails = ftpDetails;
        this.gridFsDetails = gridFsDetails;
    }


    public String getRemoteFileName()
    {
        return gridFsDetails.getFilename();
    }

    public FTPDetails getFtpDetails()
    {
        return ftpDetails;
    }

    public GridFsDetails getGridFsDetails()
    {
        return gridFsDetails;
    }

    @Override
    public String toString()
    {
        return String.format("%s -> %s", gridFsDetails.getFilename(), ftpDetails);
    }
}
