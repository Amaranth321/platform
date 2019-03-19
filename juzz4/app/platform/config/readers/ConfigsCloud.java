package platform.config.readers;

import lib.util.JsonReader;
import lib.util.exceptions.InvalidEnvironmentException;
import platform.Environment;

import java.util.Map;

/**
 * Configurations used by Cloud platform only
 *
 * @author Aye Maung
 * @version v4.5
 */
public class ConfigsCloud extends AbstractReader
{
    private static final ConfigsCloud instance = new ConfigsCloud();

    public static ConfigsCloud getInstance()
    {
        return instance;
    }

    private ConfigsCloud()
    {
    }

    @Override
    protected String configJsonFile()
    {
        return "conf/platform/configs.cloud.json";
    }

    @Override
    public JsonReader reader()
    {
        if (!Environment.getInstance().onCloud())
        {
            throw new InvalidEnvironmentException();
        }

        return super.reader();
    }

    public boolean allowAddDeviceOnCloud()
    {
        return reader().getAsBoolean("misc.allow-add-device", false);
    }

    public String piwikServerUrl()
    {
        return reader().getAsString("providers.piwik-server-url", "/");
    }

    public String loginBrandingLogo(String serverDomain)
    {
        Map<String, String> brandingMap = reader().getAsMap("branding");
        String promoPic = brandingMap.get(serverDomain);
        return promoPic == null ? "promo-default.png" : promoPic;
    }

    public int getVcaEventProcessingBufferSeconds()
    {
        return reader().getAsInt("misc.vca-event-compile-buffer-seconds", 10);
    }

    public int getFutureEventAllowMinutes()
    {
        return reader().getAsInt("misc.future-event-allow-minutes", 5);
    }

    public boolean allowVcaOnCloud()
    {
        return reader().getAsBoolean("misc.allow-vca-on-cloud", false);
    }
}