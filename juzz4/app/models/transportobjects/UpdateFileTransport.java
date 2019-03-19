package models.transportobjects;

import models.backwardcompatibility.DeviceModel;
import models.MongoDeviceModel;
import models.SoftwareUpdateFile;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class UpdateFileTransport
{
    public final String fileServerId;
    public final DeviceModel model;
    public final String version;
    public final double releaseNumber;
    public final String host;
    public final int port;
    public final long fileSize;
    public final long uploadedTime;

    public UpdateFileTransport(SoftwareUpdateFile dbFile)
    {
        this.fileServerId = dbFile.getFileServerId();
        this.model = new DeviceModel(MongoDeviceModel.getByModelId(dbFile.getModelId() + ""));
        this.version = dbFile.getVersion();
        this.releaseNumber = dbFile.getReleaseNumber();
        this.host = dbFile.getHost();
        this.port = dbFile.getPort();
        this.fileSize = dbFile.getFileSize();
        this.uploadedTime = dbFile.getUploadedTime();
    }
}
