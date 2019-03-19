package platform.kaisyncwrapper;

import com.kaisquare.sync.NodeCommandState;
import models.NodeCommand;

/**
 * Author:  Aye Maung
 */
public interface QueuedCommandStateListener
{
    public void onStateChanged(NodeCommand command, NodeCommandState state);
}
