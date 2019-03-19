package platform.node;

/**
 * @author Aye Maung
 * @since v4.3
 */
public enum NodeToCloudAPI
{
    CHECK_LICENSE("checklicensestatus"),
    REGISTER_NODE("registernode"),
    REPLACE_NODE("replacenode"),
    NOTIFY_RESET("notifynodereset"),
    RETRIEVE_CAMERAS("getnodecameralist"),
    RETRIEVE_VCA_LIST("getnodeanalyticslist"),
    RETRIEVE_SETTINGS("getnodesettings");

    final String apiName;

    NodeToCloudAPI(String apiName)
    {
        this.apiName = apiName;
    }

    public String getApiName()
    {
        return apiName;
    }
}
