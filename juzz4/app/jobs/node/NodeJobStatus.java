package jobs.node;

/**
 * @author Aye Maung
 * @since v4.4
 */
enum NodeJobStatus
{
    RUNNING,
    ERROR,
    PAUSED,
    IDLE,
    UPLOADING,
    UNREGISTERED,
    SUSPENDED,
    WAITING_STARTUP_SYNC,
    CLOUD_UNREACHABLE
}
