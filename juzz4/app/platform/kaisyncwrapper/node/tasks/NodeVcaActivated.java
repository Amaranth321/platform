package platform.kaisyncwrapper.node.tasks;

import lib.util.Util;
import models.Analytics.NodeVcaInstance;
import models.NodeCommand;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaManager;
import platform.analytics.VcaStatus;
import platform.rt.RTFeedManager;
import platform.time.RecurrenceRule;
import play.Logger;

import java.util.List;

/**
 * Sent from nodes when vca is activated
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeVcaActivated extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        List<String> parameters = command.getParameters();
        String vcaInstanceId = parameters.get(0);

        IVcaInstance dbInstance = VcaManager.getInstance().getVcaInstance(vcaInstanceId);
        if (dbInstance == null)
        {
            Logger.error(Util.whichFn() + "vca not found (%s : %s)", getNodeName(), vcaInstanceId);
            return false;
        }

        //update
        NodeVcaInstance nodeInstance = (NodeVcaInstance) dbInstance;
        nodeInstance.getVcaInfo().setEnabled(true);
        nodeInstance.setActivatedTime(System.currentTimeMillis());
        nodeInstance.setPendingRequest(false);
        nodeInstance.save();

        //additional stuff for older nodes
        if (getNodeObject().getReleaseNumber() < 4.5)
        {
            //older nodes do not trigger state change events between 'not scheduled' and disabled
            //since vca will not be running in both cases
            //so, the status must be manually updated like this
            RecurrenceRule schedule = nodeInstance.getVcaInfo().getRecurrenceRule();
            if (schedule != null && !schedule.isNow(getNodeObject().getTimeZone()))
            {
                nodeInstance.setStatus(VcaStatus.NOT_SCHEDULED);
                nodeInstance.save();
            }

            //also need to inform UI
            RTFeedManager.getInstance().vcaInstanceChanged(nodeInstance.getVcaInfo().getInstanceId(),
                                                           nodeInstance.getVcaInfo().getCamera());
        }
        return true;
    }
}