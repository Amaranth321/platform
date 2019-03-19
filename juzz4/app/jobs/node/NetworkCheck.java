package jobs.node;

import com.kaisquare.util.FileUtil;
import platform.Environment;
import platform.common.ACResource;
import platform.config.readers.ConfigsNode;
import platform.config.readers.ConfigsServers;
import play.Logger;
import play.jobs.Job;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Check network status
 * This job is only for KAI Node
 */
public class NetworkCheck extends Job
{

    private static AtomicInteger networkStatus = new AtomicInteger(0);
    private static AtomicBoolean connectionFailed = new AtomicBoolean(false);
    private static boolean RUNNING = false;

    public static final int STATUS_INTERNET_CONNECTED = 1;
    public static final int STATUS_CLOUD_SERVER_CONNECTED = STATUS_INTERNET_CONNECTED << 1;
    public static final int STATUS_ETHERNET_CONNECTED = STATUS_INTERNET_CONNECTED << 2;
    public static final int STATUS_WIRELESS_CONNECTED = STATUS_INTERNET_CONNECTED << 3;
    public static final int STATUS_UNKNOWN = 0;

    @Override
    public void doJob() throws Exception
    {
        try
        {
            if (RUNNING)
            {
                return;
            }

            if (Environment.getInstance().onWindows())
            {
                return;
            }

            int status = STATUS_UNKNOWN;
            String iface = ConfigsNode.getInstance().getNetworkInterface();
            File carrier = new File(String.format("/sys/class/net/%s/carrier", iface));
            String ethLink = "1";
            if (carrier.exists())
            {
                ethLink = FileUtil.readFile(carrier);
            }
            if (ethLink.startsWith("1")) //ethernet is up
            {
                status = STATUS_ETHERNET_CONNECTED;
                RUNNING = true;
                InetSocketAddress cloudServer = ConfigsServers.getInstance().cloudServer();

                try (ACResource<Socket> acRes = new ACResource<>(new Socket()))
                {
                    Socket socket = acRes.get();
                    socket.connect(cloudServer, 8000);

                    status = STATUS_CLOUD_SERVER_CONNECTED | STATUS_INTERNET_CONNECTED;
                    connectionFailed.getAndSet(false);

                }
                catch (Exception e)
                {
                    //prevent printing the same exception continuously
                    if (!connectionFailed.get())
                    {
                        Logger.error("Exception in NetworkCheck: %s", e.getMessage());
                        connectionFailed.getAndSet(true);
                    }
                }
            }
            networkStatus.set(status);

            RUNNING = false;
        }
        catch (Exception e)
        {
            Logger.error("Exception in NetworkCheck: %s", e.getMessage());
        }
        finally
        {
            //keep checking network status every 10 seconds
            new NetworkCheck().in(10);
        }
    }

    /**
     * Get network status, get status type from {@link NetworkCheck} static members STATUS_xxxx
     *
     * @return
     */
    public static int getNetworkStatus()
    {
        return networkStatus.get();
    }
}
