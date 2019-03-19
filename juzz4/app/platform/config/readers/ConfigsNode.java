package platform.config.readers;

import lib.util.JsonReader;
import lib.util.exceptions.InvalidEnvironmentException;
import platform.Environment;

/**
 * Configurations used by node platform only
 *
 * @author Aye Maung
 * @version v4.5
 */
public class ConfigsNode extends AbstractReader
{
    private static final ConfigsNode instance = new ConfigsNode();

    public static ConfigsNode getInstance()
    {
        return instance;
    }

    private ConfigsNode()
    {
    }

    @Override
    protected String configJsonFile()
    {
        return "conf/platform/configs.node.json";
    }

    @Override
    public JsonReader reader()
    {
        if (!Environment.getInstance().onKaiNode())
        {
            throw new InvalidEnvironmentException();
        }

        return super.reader();
    }

    public int allowedOfflineDays()
    {
        return reader().getAsInt("misc.offline-duration-allowed", 14);
    }

    public int incorrectLoginLockMins()
    {
        return reader().getAsInt("misc.incorrect-login-lock-mins", 15);
    }

    public int defaultSessionTimeoutMins()
    {
        return reader().getAsInt("misc.default-session-timeout-mins", 1440);
    }

    public int getRecordingRetentionDays(long modelId)
    {
        int defaultDays = reader().getAsInt("recording-retention-days.default", 30);
        String key = "recording-retention-days.models." + modelId;
        if (!reader().containsKey(key))
        {
            return defaultDays;
        }
        return reader().getAsInt(key, defaultDays);
    }

    public boolean mockHourlyVcaData()
    {
        return reader().getAsBoolean("debugging.mock-hourly-vca-data", false);
    }

    public String getNetworkInterface()
    {
        return reader().getAsString("system.network-interface", "eth0");
    }
}