package platform.config.readers;

import lib.util.JsonReader;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @version v4.5
 */
public class ConfigsServers extends AbstractReader
{
    private static final ConfigsServers instance = new ConfigsServers();

    public static ConfigsServers getInstance()
    {
        return instance;
    }

    private ConfigsServers()
    {
    }

    @Override
    protected String configJsonFile()
    {
        return "app/config.json";
    }

    public String applicationType()
    {
        return reader().getAsString("application-type", null);
    }

    public InetSocketAddress cloudServer()
    {
        List<Map> svrList = reader().getAsList("cloud-server", new ArrayList<>());
        JsonReader subReader = new JsonReader();
        subReader.load(svrList.get(0));
        String host = subReader.getAsString("host", null);
        int port = subReader.getAsInt("port", 0);
        return new InetSocketAddress(host, port);
    }

    public InetSocketAddress coreStreamControlServer()
    {
        String host = reader().getAsString("core-engine.host", null);
        int port = reader().getAsInt("core-engine.ports.stream-control", 0);
        return new InetSocketAddress(host, port);
    }

    public InetSocketAddress coreDataServer()
    {
        String host = reader().getAsString("core-engine.host", null);
        int port = reader().getAsInt("core-engine.ports.data", 0);
        return new InetSocketAddress(host, port);
    }

    public InetSocketAddress coreDeviceManagementServer()
    {
        String host = reader().getAsString("core-engine.host", null);
        int port = reader().getAsInt("core-engine.ports.device-management", 0);
        return new InetSocketAddress(host, port);
    }

    public InetSocketAddress coreDeviceControlServer()
    {
        String host = reader().getAsString("core-engine.host", null);
        int port = reader().getAsInt("core-engine.ports.device-control", 0);
        return new InetSocketAddress(host, port);
    }

    public InetSocketAddress coreArbiterManagementServer()
    {
        String host = reader().getAsString("core-engine.host", null);
        int port = reader().getAsInt("core-engine.ports.arbiter-management", 0);
        return new InetSocketAddress(host, port);
    }

    public InetSocketAddress coreConfigServer()
    {
        String host = reader().getAsString("core-engine.host", null);
        int port = reader().getAsInt("core-engine.ports.config", 0);
        return new InetSocketAddress(host, port);
    }

    public JsonReader kaisyncRabbitmqServerCfg()
    {
        return reader("rabbitmq-server");
    }

    public JsonReader kaisyncPlatformSyncCfg()
    {
        return reader("platform-sync");
    }

    public JsonReader kaisyncFileServerCfg()
    {
        return reader("file-server");
    }

    public JsonReader kaisyncSoftwareUpdateCfg()
    {
        return reader("software-update");
    }
}