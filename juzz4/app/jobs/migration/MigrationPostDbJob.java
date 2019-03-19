package jobs.migration;

import jobs.node.SyncNodeInfoTask;
import models.MongoBucket;
import models.MongoRole;
import models.SystemVersion;
import models.MongoUser;
import platform.Environment;
import platform.RoleManager;
import platform.VersionManager;
import platform.access.DefaultBucket;
import platform.access.DefaultRole;
import platform.access.DefaultUser;
import platform.node.KaiNodeAdminService;
import platform.node.NodeManager;
import play.Logger;
import play.Play;
import play.jobs.Job;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import static lib.util.Util.getStackTraceString;

/**
 * Migration tasks that should be run after {@link jobs.InitializeDb} is completed.
 */
public class MigrationPostDbJob extends Job
{
    @Override
    public void doJob()
    {
        try
        {
            VersionManager verMgr = VersionManager.getInstance();
            Double currentVersion = verMgr.getPlatformVersion();
            Double releaseVersion = verMgr.getLatestPlatformVersion();

            //End of migration. Handle post-migration
            if (currentVersion < releaseVersion)
            {
                //nodes only
                if (Environment.getInstance().onKaiNode())
                {
                    NodeManager.getInstance().nodePlatformUpdated();
                }

                //update the version to the latest
                verMgr.setPlatformVersion(releaseVersion);
            }

            if (currentVersion >= 4.6)
            {
                // disable play's sql plugins
                disableSqlPlugins();
            }

            //always-check tasks
            createMissingMonitoringAccount();

            //sync info with cloud
            new SyncNodeInfoTask().now().get();

            //start allowing web and api requests
            Environment.getInstance().setHttpReady(true);

            //start allowing periodic jobs
            Environment.getInstance().setStartupTasksCompleted(true);

            //start long running migrations
            startLongMigrations();
        }
        catch (Exception e)
        {
            Logger.error(getStackTraceString(e));
        }
        finally
        {
            Logger.info("MigrationPostDbJob completed");
        }
    }

    // region private methods

    private void startLongMigrations()
    {
        //reset migration counts
        SystemVersion.resetMigrationCounts();

        /**
         * Inside each doJob, don't forget to update.
         *
         * SystemVersion.incrementActiveMigration();
         * SystemVersion.decrementActiveMigration();
         *
         */
    }

    private void createMissingMonitoringAccount()
    {
        if (!Environment.getInstance().onCloud())
        {
            return;
        }

        try
        {
            MongoBucket superadminBucket = MongoBucket.getByName(DefaultBucket.SUPERADMIN.getBucketName());
            Pattern monitorRoleNamePattern = Pattern.compile("^" + DefaultRole.SITE_MONITOR.getRoleName() + "$", Pattern.CASE_INSENSITIVE);
            MongoRole monitorRole = MongoRole.q()
                    .filter("bucketId", superadminBucket.getBucketId())
                    .filter("name", monitorRoleNamePattern)
                    .get();

            if (monitorRole != null)
            {
                return;
            }

            monitorRole = new MongoRole(superadminBucket.getBucketId(), DefaultRole.SITE_MONITOR.getRoleName(), DefaultRole.SITE_MONITOR.getDescription());
            monitorRole.setRoleId(MongoRole.generateNewId());
            monitorRole.save();
            Logger.info("Default role (%s) created", DefaultRole.SITE_MONITOR.getRoleName());

            superadminBucket.save();

            //create users
            for (DefaultUser defaultUser : DefaultRole.SITE_MONITOR.getUsers())
            {
                MongoUser newUser = new MongoUser(superadminBucket.getBucketId(), defaultUser.getFullName(), defaultUser.getUsername(), defaultUser.getPassword(), "", "", "");
                newUser.setUserId(MongoUser.generateNewId());
                newUser.setActivated(false);
                newUser.addRoleId(monitorRole.getRoleId());
                newUser.save();
                Logger.info("Default user (%s) created", defaultUser.getUsername());

                superadminBucket.save();
            }

            RoleManager.getInstance().addFeatures(monitorRole.getRoleId(), DefaultRole.SITE_MONITOR.getFeatureNames());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    private void disableSqlPlugins()
    {
        List<String> targetPlugins = new ArrayList<>(Arrays.asList(
                "plugins.disable.0=play.db.DBPlugin",
                "plugins.disable.1=play.db.jpa.JPAPlugin"));

        try
        {
            // read application.conf
            String applicationConfFile = Play.applicationPath + "/conf/application.conf";
            String contents = new String(Files.readAllBytes(Paths.get(applicationConfFile)), StandardCharsets.UTF_8);

            // uncomment lines to disable sql plugins
            boolean alreadyModified = false;
            for (String targetPlugin : targetPlugins)
            {
                if (!contents.contains("#" + targetPlugin))
                {
                    alreadyModified = true;
                    break;
                }
                contents = contents.replace("#" + targetPlugin, targetPlugin);
            }

            // save application.conf
            Logger.info("Config file needs modification to disable SQL plugins: %s", !alreadyModified);
            if (!alreadyModified)
            {
                FileWriter fileWriter = new FileWriter(applicationConfFile);
                fileWriter.write(contents);
                fileWriter.close();
            }
        }
        catch (Exception e)
        {
            Logger.error(getStackTraceString(e));
        }
    }

    // endregion
}
