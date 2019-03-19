package platform.access;

import lib.util.Util;

/**
 * Auto-generated default users.
 * <p/>
 * See {@link platform.BucketManager#checkDefaultBuckets()}
 *
 * @author Aye Maung
 * @since v4.4
 */
public enum DefaultUser
{
    ROOT(
            "Super Admin",
            "root",
            "root",
            "Default root user for superadmin bucket",
            false),

    SITE_MONITOR(
            "Site Monitor",
            "sitemonitor",
            "sitemonitor",
            "Default user to be used by the KUP monitoring server",
            false),

    ADMIN(
            "Admin",
            "admin",
            "admin",
            "Bucket Administrator with access to all bucket features",
            true),

    NODE_USER(
            "Node User",
            "nodeuser",
            "nodeuser",
            "Default non-admin login account on nodes with limited features",
            true);

    private final String fullName;
    private final String username;
    private final String password;
    private final String description;
    private final boolean autoAssignDevices;

    public static boolean isDefault(String username)
    {
        if (Util.isNullOrEmpty(username))
        {
            return false;
        }

        for (DefaultUser defaultUser : values())
        {
            if (defaultUser.username.equals(username.trim()))
            {
                return true;
            }
        }
        return false;
    }

    private DefaultUser(String fullName,
                        String username,
                        String password,
                        String description,
                        boolean autoAssignDevices)
    {
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.description = description;
        this.autoAssignDevices = autoAssignDevices;
    }

    public String getFullName()
    {
        return fullName;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean autoAssignDevices()
    {
        return autoAssignDevices;
    }
}
