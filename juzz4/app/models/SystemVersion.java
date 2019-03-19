package models;

import com.google.code.morphia.annotations.Entity;
import platform.Environment;
import platform.VersionManager;
import play.modules.morphia.Model;

@Entity
public class SystemVersion extends Model
{
    private double platformVersion;
    private long lastUpgraded;
    private int activeMigrationCount;

    public static SystemVersion findOne()
    {
        SystemVersion systemVersion = q().first();
        if (systemVersion == null)
        {
            systemVersion = new SystemVersion();
            systemVersion.platformVersion = VersionManager.BEFORE_VERSIONING;
            systemVersion.lastUpgraded = Environment.getInstance().getCurrentUTCTimeMillis();
            systemVersion.save();
        }

        return systemVersion;
    }

    public static void incrementActiveMigration()
    {
        SystemVersion system = findOne();
        system.activeMigrationCount++;
        system.save();
    }

    public static void decrementActiveMigration()
    {
        SystemVersion system = findOne();
        system.activeMigrationCount--;
        system.save();
    }

    public static int getActiveMigrationCount()
    {
        SystemVersion systemVersion = findOne();
        return systemVersion.activeMigrationCount;
    }

    public static void resetMigrationCounts()
    {
        SystemVersion systemVersion = findOne();
        systemVersion.activeMigrationCount = 0;
        systemVersion.save();
    }

    public static double getPlatformVersion()
    {
        return findOne().platformVersion;
    }

    public static void setPlatformVersion(Double platformVersion)
    {
        SystemVersion systemVersion = findOne();
        systemVersion.platformVersion = platformVersion;
        systemVersion.lastUpgraded = Environment.getInstance().getCurrentUTCTimeMillis();
        systemVersion.save();
    }

    public static long getLastUpgraded()
    {
        SystemVersion systemVersion = findOne();
        if (systemVersion.lastUpgraded == 0)
        {
            systemVersion.lastUpgraded = systemVersion._getModified();
            systemVersion.save();
        }
        return systemVersion.lastUpgraded;
    }

    private SystemVersion()
    {
    }

}
