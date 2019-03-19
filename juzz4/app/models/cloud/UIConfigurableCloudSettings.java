package models.cloud;

import com.google.code.morphia.annotations.Entity;
import lib.util.Util;
import lib.util.exceptions.InvalidEnvironmentException;
import platform.Environment;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedCloudConfigs;
import play.Logger;
import play.modules.morphia.Model;

/**
 * Only one copy of this will exist.
 * <p/>
 * Do not add new fields here. Add them to {@link CloudServerConfigs} instead.
 * <p/>
 * A new field should be added here only if it belongs to a different UI page
 * with a different set of update APIs
 *
 * @author Aye Maung
 * @since v4.5
 */
@Entity
public class UIConfigurableCloudSettings extends Model
{
    private final CloudServerConfigs server;

    public static CloudServerConfigs server()
    {
        //Only UI configurable settings must be persisted in db. Hence, cloud only.
        //for node configs, use configs.node.json or configs.shared.json
        //so that it can be preset and packaged, rather than manually edited like Cloud
        if (!Environment.getInstance().onCloud())
        {
            Logger.error(Util.getCallerFn() + "called on node");
            throw new InvalidEnvironmentException();
        }

        return CacheClient.getInstance().getCloudConfigs().getServerConfigs();
    }

    public static CachedCloudConfigs createCachedObject()
    {
        return new CachedCloudConfigs(CacheClient.SINGLETON_CACHE_KEY, queryOne().server);
    }

    public static synchronized void updateServerConfigs(CloudServerConfigs serverConfigs)
    {
        UIConfigurableCloudSettings dbCopy = queryOne();
        dbCopy.server.update(serverConfigs);
        dbCopy.save();

        //remove cache
        CachedCloudConfigs cachedConfigs = CacheClient.getInstance().getCloudConfigs();
        CacheClient.getInstance().remove(cachedConfigs);
    }

    private UIConfigurableCloudSettings()
    {
        server = new CloudServerConfigs();
    }

    private static UIConfigurableCloudSettings queryOne()
    {
        UIConfigurableCloudSettings dbCopy = q().first();
        if (dbCopy == null)
        {
            dbCopy = new UIConfigurableCloudSettings();
            dbCopy.save();
        }
        else if (dbCopy.server.initializeNullFields())
        {
            dbCopy.save();
        }
        return dbCopy;
    }
}
