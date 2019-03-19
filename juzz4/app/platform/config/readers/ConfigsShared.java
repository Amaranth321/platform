package platform.config.readers;

import lib.util.JsonReader;
import models.cloud.UIConfigurableCloudSettings;
import platform.Environment;
import platform.config.RetentionDays;
import platform.events.EventType;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Configurations used by both node and cloud platforms.
 *
 * @author Aye Maung
 * @version v4.5
 */
public class ConfigsShared extends AbstractReader
{
    private static final ConfigsShared instance = new ConfigsShared();
    private static final ConfigsNode configsNode = ConfigsNode.getInstance();
    private static final ConfigsCloud configsCloud = ConfigsCloud.getInstance();
    private static final Environment env = Environment.getInstance();

    public static ConfigsShared getInstance()
    {
        return instance;
    }

    private ConfigsShared()
    {
    }

    @Override
    protected String configJsonFile()
    {
        return "conf/platform/configs.shared.json";
    }

    public boolean printApiCallLog()
    {
        return reader().getAsBoolean("debugging.print-api-call-log", false);
    }

    public boolean forceHttps()
    {
        if (env.onKaiNode())
        {
            return false;
        }
        else
        {
            return UIConfigurableCloudSettings.server().access().forceHttps;
        }
    }

    public int incorrectLoginLockMins()
    {
        if (env.onCloud())
        {
            return UIConfigurableCloudSettings.server().access().incorrectLoginLockMins;
        }

        return configsNode.incorrectLoginLockMins();
    }

    public int defaultSessionTimeOutMins()
    {
        if (env.onCloud())
        {
            return UIConfigurableCloudSettings.server().access().defaultSessionTimeOutMins;
        }

        return configsNode.defaultSessionTimeoutMins();
    }

    public boolean isRealTimeFeedEnabled(EventType eventType)
    {
        JsonReader jsonReader = env.onKaiNode() ? configsNode.reader() : configsCloud.reader();
        String key = "realtime-feed.events." + eventType.toString();
        if (!jsonReader.containsKey(key))
        {
            return false;
        }

        return jsonReader.getAsBoolean(key, false);
    }

    public InetSocketAddress vcaServerAddress()
    {
        //VCA server currently runs on nodes only, hence always 'localhost'
        //this can be made to be configurable in the future if VCA server needs to run on Cloud as well
        String host = "localhost";
        int port = reader().getAsInt("servers.vca-server-port", 0);
        return new InetSocketAddress(host, port);
    }

    public int eventReceiverPort()
    {
        return reader().getAsInt("servers.event-receiver-port", 0);
    }

    public int notificationSoundMinGapSeconds()
    {
        return reader().getAsInt("misc.notification-sound-min-gap-seconds", 0);
    }

    public RetentionDays getRetentionDays()
    {
        if (env.onCloud())
        {
            return UIConfigurableCloudSettings.server().retentionDays();
        }

        return RetentionDays.forNode();
    }

    public String licenseEncryptionKey()
    {
        return reader().getAsString("misc.license-encryption-key", null);
    }

    public List<String> excludedApps(long modelId)
    {
        String key = "model-lookup.excluded-apps." + modelId;
        if (!reader().containsKey(key))
        {
            return new ArrayList<>();
        }
        return (List<String>) reader().getAsList(key, new ArrayList<String>());
    }

    public boolean mockHourlyVcaData()
    {
        if (env.onCloud())
        {
            return UIConfigurableCloudSettings.server().miscellaneous().simulateVcaData;
        }

        return configsNode.mockHourlyVcaData();
    }
}