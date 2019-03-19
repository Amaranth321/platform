package jobs.cloud.independent.delivery;

import models.content.DeliveryItem;
import platform.content.delivery.Deliverable;
import platform.content.delivery.DeliveryManager;
import platform.content.delivery.DeliveryMethod;
import platform.content.ftp.FTPDetails;
import platform.content.ftp.FTPHandler;
import platform.content.ftp.FTPItem;
import platform.db.gridfs.GridFsHelper;
import play.Logger;
import play.jobs.Every;

import java.io.InputStream;

/**
 * Delivers {@link models.content.DeliveryItem} queued as {@link DeliveryMethod#FTP}.
 * <p/>
 * DO NOT run this job directly. Use {@link DeliveryManager}.
 *
 * @author Aye Maung
 * @since v4.3
 */
@Every("10s")
public class FTPDeliveryJob extends QueuedContentDeliveryJob
{
    private final int RETRY_LIMIT = 0;

    @Override
    protected DeliveryMethod getDeliveryMethod()
    {
        return DeliveryMethod.FTP;
    }

    @Override
    protected int getRetryLimit()
    {
        return RETRY_LIMIT;
    }

    @Override
    protected void process(DeliveryItem deliveryItem)
    {
        Deliverable<FTPItem> deliverable = deliveryItem.getDeliverable();
        FTPItem ftpItem = deliverable.getDetails();
        InputStream gridFileInputStream = GridFsHelper.getFileInputStream(ftpItem.getGridFsDetails());

        // connect
        FTPDetails serverDetails = ftpItem.getFtpDetails();
        FTPHandler ftpHandler = new FTPHandler(serverDetails);
        ftpHandler.connect();

        // upload
        boolean success = ftpHandler.upload(gridFileInputStream, serverDetails.getDirectory() + ftpItem.getRemoteFileName());
        if (success)
        {
            deliverySuccessful(deliveryItem);
        }
        else
        {
            Logger.error("Delivery failed for (%s)", ftpItem);
            deliveryFailed(deliveryItem);
        }
    }

}
