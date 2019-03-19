package platform.access;

import platform.config.readers.AccountDefaultSettings;
import lib.util.exceptions.ApiException;
import models.MongoBucket;
import models.MongoFeature;
import models.MongoRole;
import models.MongoUser;
import platform.BucketManager;
import platform.Environment;
import platform.RoleManager;
import play.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DO NOT change the bucket names.
 * See {@link platform.BucketManager#checkDefaultBuckets()}
 *
 * @author Aye Maung
 * @since v4.4
 */
public enum DefaultBucket
{
    SUPERADMIN(
            "superadmin",
            "Root-level bucket with access to all features",
            Arrays.asList(DefaultRole.ROOT, DefaultRole.SITE_MONITOR)
    ),
    KAISQUARE(
            "kaisquare",
            "KAI Square account",
            Arrays.asList(DefaultRole.ADMINISTRATOR, DefaultRole.NODE_USER)
    );

    public static final String DEFAULT_PATH = "kaisquare";

    public static boolean isDefault(MongoBucket bucket)
    {
        for (DefaultBucket defaultBkt : values())
        {
            if (defaultBkt.bucketName.equalsIgnoreCase(bucket.getName()))
            {
                return true;
            }
        }

        return false;
    }

    private final String bucketName;
    private final String description;
    private final List<DefaultRole> roles;

    private DefaultBucket(String bucketName, String description, List<DefaultRole> roles)
    {
        this.bucketName = bucketName;
        this.description = description;
        this.roles = roles;
    }

    public String getBucketName()
    {
        return bucketName;
    }

    public MongoBucket getBucket()
    {
        return MongoBucket.getByName(bucketName);
    }

    public List<String> getDefaultFeatureNames()
    {
        switch (this)
        {
            case SUPERADMIN:
                List<String> allFeatureNames = new ArrayList<>();
                List<MongoFeature> allFeatures = MongoFeature.q().fetchAll();
                for (MongoFeature feature : allFeatures)
                {
                    allFeatureNames.add(feature.getName());
                }
                return allFeatureNames;
            case KAISQUARE:
                return AccountDefaultSettings.getInstance().getBucketFeatureNames();
            default:
                throw new IllegalArgumentException();
        }
    }

    public void createIfNotExists() throws ApiException
    {
        //exists
        if (getBucket() != null)
        {
            return;
        }

        Logger.info("Creating default bucket (%s)", bucketName);
        MongoBucket newBkt = new MongoBucket(bucketName, DEFAULT_PATH, description, true);
        newBkt.setBucketId(MongoBucket.generateNewId());
        newBkt.save();
        BucketManager.getInstance().addFeatures(newBkt.getBucketId(), getDefaultFeatureNames());

        //create roles
        for (DefaultRole defaultRole : this.roles)
        {
            if (!Environment.getInstance().onKaiNode() && defaultRole.equals(DefaultRole.NODE_USER))
            {
                continue;
            }

            MongoRole newRole = new MongoRole(newBkt.getBucketId(), defaultRole.getRoleName(), defaultRole.getDescription());
            newRole.setRoleId(MongoRole.generateNewId());
            newRole.save();

            //create users
            for (DefaultUser defaultUser : defaultRole.getUsers())
            {
                MongoUser newUser = new MongoUser(newBkt.getBucketId(), defaultUser.getFullName(), defaultUser.getUsername(), defaultUser.getPassword(), "", "", "");
                newUser.setUserId(MongoUser.generateNewId());
                newUser.setActivated(true);
                newUser.addRoleId(newRole.getRoleId());
                newUser.save();
            }

            RoleManager.getInstance().addFeatures(newRole.getRoleId(), defaultRole.getFeatureNames());
        }
    }
}
