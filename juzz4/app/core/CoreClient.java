package core;

/**
 * A class that encapsulates all interfaces with CoreClient (or RMS+).
 *
 * @author kapil
 */
public class CoreClient
{
    private static CoreClient instance = null;

    public StreamControlClient streamControlClient;
    public DeviceManagementClient deviceManagementClient;
    public DeviceControlClient deviceControlClient;
    public DataClient dataClient;
    public EventServer eventServer;
    public ConfigControlClient configClient;

    private CoreClient()
    {
        streamControlClient = StreamControlClient.getInstance();
        deviceManagementClient = DeviceManagementClient.getInstance();
        deviceControlClient = DeviceControlClient.getInstance();
        dataClient = DataClient.getInstance();
        eventServer = new EventServer();
    }

    public static CoreClient getInstance()
    {
        synchronized (CoreClient.class)
        {
            if (instance == null)
            {
                instance = new CoreClient();
            }
        }

        return instance;
    }

    public void stop()
    {
        eventServer.stopServer();
    }
}
