package platform.kaisyncwrapper.cloud.tasks;

import models.notification.BucketNotificationSettings;
import platform.node.NodeManager;

/**
 * Sent from Cloud when bucket alert settings are updated on cloud
 * processCommand will be executed on node
 *
 * @author Aye Maung
 */

public class CloudUpdateAlertSettings extends CloudToNodeCommandTask
{
    @Override
    protected boolean processCommand() throws Exception
    {
        String jsonSettings = getParameter(0);
        BucketNotificationSettings newSettings = BucketNotificationSettings.parse(jsonSettings);
        NodeManager.getInstance().setBucketNotificationSettings(newSettings);
        return true;
    }
}
