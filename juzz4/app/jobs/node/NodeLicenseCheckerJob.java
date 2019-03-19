package jobs.node;

import lib.util.Util;
import models.licensing.NodeLicense;
import platform.node.NodeManager;
import play.Logger;
import play.jobs.Job;

/**
 * The Node will automatically check it license every 15 seconds.
 * If the license is expired it will suspend itself.
 */
public class NodeLicenseCheckerJob extends Job
{
    private static final int FREQ_SECONDS = 60 * 60;

    @Override
    public void doJob()
    {
        try
        {
            NodeManager nodeManager = NodeManager.getInstance();
            if (!nodeManager.isRegisteredOnCloud())
            {
                return;
            }

            NodeLicense license = nodeManager.getLicense();
            if (license.hasExpired() && !nodeManager.isSuspended())
            {
                Util.printImptLog("Node License has expired");
                NodeManager.getInstance().suspendLicense();
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
        finally
        {
            in(FREQ_SECONDS);
        }
    }
}
