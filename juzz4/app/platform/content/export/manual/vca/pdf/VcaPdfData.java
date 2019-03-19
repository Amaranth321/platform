package platform.content.export.manual.vca.pdf;

import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class VcaPdfData
{
    private final Map<String, String> searchParams;
    private final int timeZoneOffsetMins;
    private final String locale;

    public VcaPdfData(Map<String, String> searchParams, int timeZoneOffsetMins, String locale)
    {
        this.searchParams = searchParams;
        this.timeZoneOffsetMins = timeZoneOffsetMins;
        this.locale = locale;
    }

    public Map<String, String> getSearchParams()
    {
        return searchParams;
    }

    public int getTimeZoneOffsetMins()
    {
        return timeZoneOffsetMins;
    }

    public String getLocale()
    {
        return locale;
    }
}
