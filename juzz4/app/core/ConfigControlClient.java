package core;

import com.kaisquare.core.thrift.ConfigControlService;
import com.kaisquare.core.thrift.ConfigControlService.Iface;
import com.kaisquare.core.thrift.CoreException;
import com.kaisquare.util.ThriftUtil;
import com.kaisquare.util.ThriftUtil.Client;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import platform.config.readers.ConfigsServers;
import play.Logger;

import java.net.InetSocketAddress;
import java.util.regex.Pattern;

public class ConfigControlClient
{

    private static ConfigControlClient instance = null;
    private Client<Iface> configControlServiceClient;
    private boolean cloudCoreAddressUpdated;

    private ConfigControlClient()
    {
        initClient();
    }

    private void initClient()
    {
        InetSocketAddress serverAddress = ConfigsServers.getInstance().coreConfigServer();
        Logger.info("Initializing ConfigControlClient (%s)", serverAddress);
        try
        {
            //retry 3 times, with an interval of 1000 milliseconds between each try
            this.configControlServiceClient = ThriftUtil.newServiceClient(ConfigControlService.Iface.class,
                                                                          ConfigControlService.Client.class,
                                                                          serverAddress.getHostName(),
                                                                          serverAddress.getPort(),
                                                                          ThriftUtil.DEFAULT_TIMEOUT_MILLIS);
        }
        catch (TTransportException e)
        {
            Logger.error(e, "");
        }
    }

    public boolean setStorageKeepDays(String coreDeviceId, String channelId, long keepDays)
    {
        try
        {
            return configControlServiceClient.getIface().setStorageKeepDays(coreDeviceId, channelId, keepDays);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        return false;
    }

    public boolean setStreamStorageLimit(String coreDeviceId, String channelId, Long storageLimit)
    {
        try
        {
            return configControlServiceClient.getIface().setStreamStorageLimit(coreDeviceId, channelId, storageLimit);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        return false;
    }

    public boolean setCloudCoreServer(String cloudServerHost) throws CoreException
    {
        try
        {
            Logger.info("setting cloud core server: %s", cloudServerHost);
            return configControlServiceClient.getIface().setCloudServer(cloudServerHost);
        }
        catch (TException e)
        {
            Logger.error(e, "");
        }

        return false;
    }

    public void setCloudServerByPlatformCloud()
    {
        try
        {
            InetSocketAddress cloudServer = ConfigsServers.getInstance().cloudServer();
            String cloudPlatformHost = cloudServer.getHostString();

            String corePrefix = "arbiter.";
            String cloudCoreHost;
            if (!Pattern.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$", cloudPlatformHost))
            {
                cloudCoreHost = corePrefix + cloudPlatformHost;
            }
            else
            {
                cloudCoreHost = cloudPlatformHost;
            }

            cloudCoreAddressUpdated = setCloudCoreServer(cloudCoreHost);
        }
        catch (Exception e)
        {
            Logger.error("Error: failed to set cloud server in core engine");
            Logger.error("Exception: %s", e.getMessage());
        }
    }

    public boolean isCloudCoreAddressUpdated()
    {
        return cloudCoreAddressUpdated;
    }

    public static synchronized ConfigControlClient getInstance()
    {
        if (instance == null)
        {
            instance = new ConfigControlClient();
        }

        return instance;
    }
}
