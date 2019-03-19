package core;

import com.kaisquare.arbiter.thrift.ArbiterManagementService;
import com.kaisquare.util.ThriftUtil;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import platform.config.readers.ConfigsServers;
import platform.events.EventInfo;
import platform.events.EventType;
import play.Logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class ArbiterManagementClient
{

    private static ArbiterManagementClient instance = null;

    private ThriftUtil.Client<ArbiterManagementService.Iface> ddrServiceClient;

    private ArbiterManagementClient()
    {
        try
        {
            initClient();
        }
        catch (Exception e)
        {
            System.err.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static ArbiterManagementClient getInstance()
    {
        if (instance == null)
        {
            instance = new ArbiterManagementClient();
        }

        return instance;
    }

    private void initClient()
    {
        InetSocketAddress serverAddress = ConfigsServers.getInstance().coreArbiterManagementServer();
        Logger.info("Initializing ArbiterManagementClient (%s)", serverAddress);
        try
        {
            //retry 3 times, with an interval of 1000 milliseconds between each try
            this.ddrServiceClient = ThriftUtil.newServiceClient(ArbiterManagementService.Iface.class,
                                                                ArbiterManagementService.Client.class,
                                                                serverAddress.getHostName(),
                                                                serverAddress.getPort(),
                                                                ThriftUtil.DEFAULT_TIMEOUT_MILLIS);

        }
        catch (TTransportException e)
        {
            Logger.error(e, "");
        }
    }


    public boolean sendEventData(long deviceId,
                                 String eventType,
                                 long eventTime,
                                 String stringData,
                                 ByteBuffer binaryData)
    {
        ArbiterManagementService.Iface svcIface = this.ddrServiceClient.getIface();
        try
        {
            //events from kup-analytics always have serverId = 0
            return svcIface.sendEventData(deviceId, 0, eventType, eventTime, stringData, binaryData);
        }
        catch (TException e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    public boolean requestEventVideo(EventInfo ownerEventInfo, String requestData)
    {
        boolean result = sendEventData(
                Long.parseLong(ownerEventInfo.getCamera().getCoreDeviceId()),
                EventType.CAPTURE_EVENT_VIDEO.toString(),
                ownerEventInfo.getTime(),
                requestData,
                ByteBuffer.wrap(new byte[0])
        );

        return result;
    }
}
