package platform.nodesoftware;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum NodeSoftwareStatus
{
    /**
     * node has the same version as the latest update file
     */
    LATEST,

    /**
     * New update file available, but not yet downloaded to the node.
     */
    NOT_DOWNLOADED,

    /**
     * Update file is downloaded. User needs to click update cloud.
     */
    UPDATE_AVAILABLE,

    /**
     * Update in progress
     */
    UPDATING,

}
