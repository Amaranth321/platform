package models.node;

import platform.node.KaiNodeAdminService.NetworkSettings;

import java.util.TimeZone;

public class NodeSettings
{
    private final NetworkSettings networkSettings;
    private String timezone;
    private int tzOffsetMins;

    public NodeSettings(NetworkSettings networkSettings, TimeZone timeZone)
    {
        this.networkSettings = networkSettings;
        setTimezone(timeZone);
    }

    public NetworkSettings getNetworkSettings()
    {
        if (networkSettings == null)
        {
            return NetworkSettings.emptyObject();
        }

        return networkSettings;
    }

    public String getTimezone()
    {
        return timezone;
    }

    public int getTzOffsetMins()
    {
        return tzOffsetMins;
    }

    public void setTimezone(TimeZone timeZone)
    {
        this.timezone = timeZone.getID();
        this.tzOffsetMins = timeZone.getRawOffset() / (60 * 1000);
    }

}

