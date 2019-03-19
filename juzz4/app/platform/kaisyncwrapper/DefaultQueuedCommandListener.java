package platform.kaisyncwrapper;

import com.kaisquare.sync.NodeCommandState;
import models.NodeCommand;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class DefaultQueuedCommandListener implements QueuedCommandStateListener
{
    @Override
    public void onStateChanged(NodeCommand command, NodeCommandState state)
    {
        //nothing to do
    }
}
