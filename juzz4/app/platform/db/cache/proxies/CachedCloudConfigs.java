package platform.db.cache.proxies;

import models.cloud.CloudServerConfigs;
import platform.db.cache.CachedObject;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class CachedCloudConfigs extends CachedObject<CachedCloudConfigs>
{
    private final CloudServerConfigs serverConfigs;

    public CachedCloudConfigs(String cacheKey, CloudServerConfigs serverConfigs)
    {
        super(cacheKey);
        this.serverConfigs = serverConfigs;
    }

    @Override
    public CachedCloudConfigs getObject()
    {
        return this;
    }

    public CloudServerConfigs getServerConfigs()
    {
        return serverConfigs;
    }
}
