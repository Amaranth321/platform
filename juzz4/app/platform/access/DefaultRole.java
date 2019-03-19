package platform.access;

import lib.util.Util;
import models.MongoFeature;
import platform.config.readers.AccountDefaultSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * For generating default roles. See {@link platform.BucketManager#checkDefaultBuckets()}
 *
 * @author Aye Maung
 * @since v4.4
 */
public enum DefaultRole
{
    ROOT(
            "Root",
            "Default role for superadmin root user",
            Arrays.asList(DefaultUser.ROOT)
    ),
    SITE_MONITOR(
            "Site Monitor",
            "Role to be assigned only to the site monitor account",
            Arrays.asList(DefaultUser.SITE_MONITOR)
    ),
    ADMINISTRATOR(
            "Administrator",
            "Admin role with access to all bucket features",
            Arrays.asList(DefaultUser.ADMIN)
    ),
    NODE_USER(
            "Default Node User",
            "Default node user role with limited features.",
            Arrays.asList(DefaultUser.NODE_USER)
    );

    private final String roleName;
    private final String description;
    private final List<DefaultUser> users;

    public static boolean isDefault(String roleName)
    {
        if (Util.isNullOrEmpty(roleName))
        {
            return false;
        }

        for (DefaultRole defaultRole : values())
        {
            if (defaultRole.roleName.equals(roleName.trim()))
            {
                return true;
            }
        }
        return false;
    }

    private DefaultRole(String roleName, String description, List<DefaultUser> users)
    {
        this.roleName = roleName;
        this.description = description;
        this.users = users;
    }

    public String getRoleName()
    {
        return roleName;
    }

    public String getDescription()
    {
        return description;
    }

    public List<DefaultUser> getUsers()
    {
        return users;
    }

    public List<String> getFeatureNames()
    {
        switch (this)
        {
            case ROOT:
                List<String> featureNames = new ArrayList<>();
                List<MongoFeature> features = MongoFeature.q().fetchAll();
                for (MongoFeature feature : features)
                {
                    featureNames.add(feature.getName());
                }
                return featureNames;
            case SITE_MONITOR:
                List<String> smFeatureNames = new ArrayList<>();
                List<MongoFeature> features1 = MongoFeature.q().filter("name", "site-monitoring").fetchAll();
                for (MongoFeature feature : features1)
                {
                    smFeatureNames.add(feature.getName());
                }
                return smFeatureNames;
            case ADMINISTRATOR:
                return AccountDefaultSettings.getInstance().getBucketFeatureNames();
            case NODE_USER:
                return AccountDefaultSettings.getInstance().getNodeUserRoleFeatureNames();
            default:
                throw new IllegalArgumentException();
        }
    }
}
