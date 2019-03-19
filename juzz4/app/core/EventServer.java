package core;

import com.kaisquare.platform.thrift.EventService;
import com.kaisquare.util.ThriftUtil;
import org.apache.thrift.server.TServer;
import platform.EventServiceImpl;
import platform.config.readers.ConfigsShared;
import play.Logger;

public class EventServer
{
    private TServer thriftServer;

    public EventServer()
    {
        startServer();
    }

    private void startServer()
    {
        int receiverPort = ConfigsShared.getInstance().eventReceiverPort();
        Logger.info("Starting Event receiver server (port:%s)", receiverPort);
        try
        {
            EventServiceImpl handler = new EventServiceImpl();
            EventService.Processor processor = new EventService.Processor(handler);
            this.thriftServer = ThriftUtil.newServiceServer(processor, receiverPort);
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
    }

    public void stopServer()
    {
        if (thriftServer != null)
        {
            thriftServer.stop();
        }
    }
}
