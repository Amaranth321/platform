package platform.node;

import lib.util.Util;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidEnvironmentException;
import platform.Environment;
import platform.config.readers.ConfigsServers;
import play.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Author       : Aye Maung
 * <p/>
 * Description  : for node-cloud communications
 */
public class CloudConnector
{
    private static final CloudConnector instance = new CloudConnector();

    //Cloud Info
    private static String CLOUD_SERVER_HOST;
    private static int CLOUD_SERVER_PORT;
    private static boolean SSL_ENABLED;

    private CloudConnector()
    {
        if (!Environment.getInstance().onKaiNode())
        {
            throw new InvalidEnvironmentException();
        }

        initClient();
    }

    private void initClient()
    {
        try
        {
            InetSocketAddress cloudServer = ConfigsServers.getInstance().cloudServer();
            CLOUD_SERVER_HOST = cloudServer.getHostString();
            CLOUD_SERVER_PORT = cloudServer.getPort();
            SSL_ENABLED = false;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
        }
    }

    public static CloudConnector getInstance()
    {
        return instance;
    }

    public String getServerHost()
    {
        String host = Util.isNullOrEmpty(CLOUD_SERVER_HOST) ? "unknown" : CLOUD_SERVER_HOST;
        return host;
    }

    public String getServerUrl()
    {
        String protocol = SSL_ENABLED ? "https" : "http";
        if (Util.isNullOrEmpty(CLOUD_SERVER_HOST) || CLOUD_SERVER_PORT == 0)
        {
            throw new IllegalStateException("cloud-server is not set in config.json file");
        }

        return String.format("%s://%s:%s", protocol, CLOUD_SERVER_HOST, CLOUD_SERVER_PORT);
    }

    public boolean isCloudReachable(int timeOutSeconds)
    {
        Socket socket = null;
        try
        {
            int timeoutMillis = timeOutSeconds * 1000;
            socket = new Socket();
            socket.connect(new InetSocketAddress(CLOUD_SERVER_HOST, CLOUD_SERVER_PORT), timeoutMillis);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
        finally
        {
            if (socket != null)
            {
                try
                {
                    socket.close();
                }
                catch (IOException e)
                {
                }
            }
        }
    }

    public HttpApiClient getHttpApiClient()
    {
        HttpApiClient httpApiClient = HttpApiClient.getInstance();
        httpApiClient.setCloudUrl(getServerUrl());
        return httpApiClient;
    }

    public NodeEventClient getNodeEventClient()
    {
        boolean accessEnabled = !NodeManager.getInstance().isSuspended();
        NodeEventClient nodeEventClient = NodeEventClient.getInstance();
        nodeEventClient.setEnabled(accessEnabled);
        return nodeEventClient;
    }

    public KaiSyncCommandClient getKaiSyncCommandClient() throws ApiException
    {
        return KaiSyncCommandClient.getInstance();
    }
}