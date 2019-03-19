package platform.pubsub;

/**
 * Author:  Aye Maung
 */
public interface PlatformEventTask
{
    void run(Object... params) throws Exception;
}
