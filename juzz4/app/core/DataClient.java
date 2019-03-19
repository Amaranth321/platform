package core;

import com.kaisquare.core.thrift.CoreException;
import com.kaisquare.core.thrift.DataService;
import com.kaisquare.core.thrift.LocationDataPoint;
import com.kaisquare.util.ThriftUtil;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.joda.time.DateTime;
import platform.config.readers.ConfigsServers;
import play.Logger;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.List;

public class DataClient
{
    private static DataClient instance = null;

    private ThriftUtil.Client<DataService.Iface> dataServiceClient;

    private DataClient()
    {
        initClient();
    }

    public static DataClient getInstance()
    {
        if (instance == null)
        {
            instance = new DataClient();
        }

        return instance;
    }

    private void initClient()
    {
        InetSocketAddress serverAddress = ConfigsServers.getInstance().coreDataServer();
        Logger.info("Initializing DataClient (%s)", serverAddress);
        try
        {
            //retry 3 times, with an interval of 1000 milliseconds between each try
            this.dataServiceClient = ThriftUtil.newServiceClient(DataService.Iface.class,
                                                                 DataService.Client.class,
                                                                 serverAddress.getHostName(),
                                                                 serverAddress.getPort(),
                                                                 ThriftUtil.DEFAULT_TIMEOUT_MILLIS);
        }
        catch (TTransportException e)
        {
            Logger.error(e, "");
        }
    }

    public List<LocationDataPoint> getLocation(String deviceId, DateTime startTimestamp, DateTime endTimestamp) throws
                                                                                                                CoreException
    {
        DataService.Iface dataServiceIface = this.dataServiceClient.getIface();
        SimpleDateFormat ddMMyyyyHHmmss = new SimpleDateFormat("ddMMyyyyHHmmss");
        String from, to;

        try
        {
            if (startTimestamp == null && endTimestamp == null)
            {
                //live location requested
                from = "";
                to = "";
            }
            else
            {
                //historical location data requested
                from = ddMMyyyyHHmmss.format(startTimestamp.toDate());
                to = ddMMyyyyHHmmss.format(endTimestamp.toDate());
            }

            List<LocationDataPoint> result;
            result = dataServiceIface.getGPSData(deviceId, from, to);
            if (result == null)
            {
                Logger.info("DataClient: result set null.");
            }
            else if (result.isEmpty())
            {
                Logger.info("DataClient: result set empty.");
            }
            /*
            for(LocationDataPoint loc : result) {
            }
            */
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return null;
        }
    }

}
