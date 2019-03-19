package jobs.cloud.independent;

import jobs.cloud.CloudCronJob;
import lib.util.Util;
import models.BroadcastItem;
import models.MongoBucket;
import models.MongoDevice;
import models.node.NodeObject;
import platform.CloudActionMonitor;
import platform.DeviceManager;
import play.Logger;
import play.jobs.Every;

import java.util.ArrayList;
import java.util.List;

/**
 * Only for bucket-related changes.
 * The changes will be broadcast to all nodes of the target bucket.
 * <p/>
 * Use {@link platform.CloudActionMonitor#broadcastCommand(java.util.List, com.kaisquare.sync.CommandType, String)}.
 *
 * @author Aye Maung
 * @since v4.4
 */
@Every("60s")
public class BroadcastBucketChangesJob extends CloudCronJob
{
    @Override
    public void doJob()
    {
        BroadcastItem item = getNext();
        while (item != null && !item.isNew())
        {
            try
            {
                process(item);
                item.delete();
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
            item = getNext();
        }
    }

    private BroadcastItem getNext()
    {
        return BroadcastItem.getOldest();
    }

    private void process(BroadcastItem broadcastItem) throws Exception
    {
        List<Long> nodeIdList = new ArrayList<>();
        String bucketId = broadcastItem.getBucketId() + "";
        MongoBucket bucket = MongoBucket.getById(bucketId);
        List<MongoDevice> bucketDevices = DeviceManager.getInstance().getDevicesOfBucket(bucketId);

        for (MongoDevice bucketDevice : bucketDevices)
        {
            if (!bucketDevice.isKaiNode())
            {
                continue;
            }

            //only versions >= 4.4
            NodeObject nodeObject = NodeObject.findByPlatformId(bucketDevice.getDeviceId());
            double releaseNumber = nodeObject.getReleaseNumber();
            if (releaseNumber < 4.4)
            {
                continue;
            }

            nodeIdList.add(Long.parseLong(bucketDevice.getDeviceId()));
        }

        if (nodeIdList.isEmpty())
        {
            Logger.info(Util.whichFn() + "no nodes for %s to broadcast %s", bucket.getName(), broadcastItem.getCommandType());
            return;
        }

        //broadcast command to each node
        CloudActionMonitor.getInstance().broadcastCommand(
                nodeIdList,
                broadcastItem.getCommandType(),
                broadcastItem.getJsonObject()
        );
    }
}
