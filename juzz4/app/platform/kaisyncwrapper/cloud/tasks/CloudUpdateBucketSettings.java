package platform.kaisyncwrapper.cloud.tasks;

import com.google.gson.Gson;
import models.MongoBucket;
import models.BucketSetting;
import platform.BucketManager;
import platform.node.NodeManager;

import java.util.Map;

/**
 * Sent from Cloud when bucket settings is updated by cloud
 * processCommand will be executed on node
 * <p/>
 * Current only userLimit and logoBinary will be sent to nodes. The rest are not applicable
 *
 * @author Aye Maung
 */

public class CloudUpdateBucketSettings extends CloudToNodeCommandTask
{
    @Override
    protected boolean processCommand() throws Exception
    {
        String jsonInfoMap = getParameter(0);
        Map infoToUpdate = new Gson().fromJson(jsonInfoMap, Map.class);

        //get node's bucket
        MongoBucket nodeBucket = NodeManager.getInstance().getBucket();
        BucketSetting currentSettings = BucketManager.getInstance().getBucketSetting(nodeBucket.getBucketId());

        if (infoToUpdate.containsKey("logoBinary"))
        {
            currentSettings.setBucketLogo(infoToUpdate.get("logoBinary").toString());
        }

        currentSettings.save();
        return true;
    }
}
