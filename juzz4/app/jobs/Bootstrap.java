package jobs;

import com.kaisquare.core.thrift.CoreException;
import com.mongodb.WriteConcern;
import core.ConfigControlClient;
import jobs.cloud.LogApiJob;
import jobs.migration.MigrationPreDbJob;
import jobs.node.*;
import platform.Environment;
import platform.KaiPlatform;
import platform.VersionManager;
import platform.db.cache.CacheClient;
import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.modules.morphia.Model;

import java.util.concurrent.Callable;

@OnApplicationStart(async = false)
public class Bootstrap extends Job
{
    @Override
    public void doJob()
    {
        //read config files once
        ConfigUpdaterJob.runOnceAtStartup();

        SequentialJob sequence = new SequentialJob();
        WriteConcern writeConcern = WriteConcern.JOURNAL_SAFE;
        try
        {
            String writeConcernConfig = Play.configuration.getProperty("morphia.defaultWriteConcern");
            writeConcern = WriteConcern.valueOf(writeConcernConfig);
        }
        catch (Exception e)
        {
            Logger.error("unable to set WriteConcern, set to JOURNAL_SAFE");
        }
        finally
        {
            Model.db().setWriteConcern(writeConcern);
        }
        Logger.info("mongo writeConcern: %s", Model.db().getWriteConcern());
        /**
         *
         *  Initialize Configurations
         *
         *
         */
        KaiPlatform.reset();
        CacheClient.getInstance().clearAll();

        final Environment env = Environment.getInstance();

        //if Play is not running as HTTP server, which means it will be running as particular service server only.
        if (env.onCron() || env.onSvc())
        {
            //services run in background automatically
            //so only run the tasks that needed for services (all the services should be in platform.services)
            sequence.addTask(new RegisterTasks());
            sequence.addTask(new Callable<Boolean>()
            {
                @Override
                public Boolean call() throws Exception
                {
                    KaiPlatform.start();

                    //start allowing periodic jobs
                    env.setStartupTasksCompleted(true);

                    return true;
                }
            });
        }
        else
        {
            //print versions
            VersionManager.getInstance().printVersions();

            if (Environment.getInstance().onKaiNode())
            {
                ConfigControlClient.getInstance().setCloudServerByPlatformCloud();
            }
            else if (Environment.getInstance().onCloud())
            {
                //synchronization server
                KaiPlatform.startServers();
            }

            /**
             *
             *  One-Time Startup Jobs
             *
             *  These jobs will be executed one by one according to the order added
             *  Do not put continuously running jobs here
             *
             */
            sequence.addTask(new MigrationPreDbJob());
            sequence.addTask(new InitializeDb());
            sequence.addTask(new RegisterTasks());

            if (Environment.getInstance().onKaiNode())
            {
                sequence.addTask(new NodeStartupTasks());
            }

            sequence.addTask(new Callable<Object>()
            {
                @Override
                public Object call() throws Exception
                {
                    KaiPlatform.start();
                    return Boolean.valueOf(true);
                }
            });


            /**
             *
             *  Periodical Jobs
             *
             *  The group of jobs or processes that should be executed in parallel after main startup jobs
             *
             */
            GroupedJob postExecute = new GroupedJob("Post-execute");
            //nodes only
            if (Environment.getInstance().onKaiNode())
            {
                postExecute.addTask(new NetworkCheck());
                postExecute.addTask(new Callable<Object>()
                {
                    @Override
                    public Object call() throws Exception
                    {
                        new NodeSettingCheckerJob().now();
                        new NodeConnectionStatusJob().every(NodeConnectionStatusJob.FREQ_SECONDS);
                        new NodeLicenseCheckerJob().now();
                        new RemoteShellJob().now();

                        //singleton jobs
                        NodeJobsManager.getInstance().startAll();

                        return Boolean.valueOf(true);
                    }
                });
            }
            //cloud only
            else if (Environment.getInstance().onCloud())
            {
                postExecute.addTask(new Callable<Object>()
                {
                    @Override
                    public Object call() throws Exception
                    {
                        LogApiJob.start();

                        //because some Jobs don't return any result
                        //this task that indicates the jobs are done.
                        return Boolean.valueOf(true);
                    }
                });
            }
            //both
            postExecute.addTask(new RefreshDeviceStatusJob());
            postExecute.addTask(new StartupFileGenerationJob());
            postExecute.addTask(new Callable<Object>()
            {
                @Override
                public Object call() throws Exception
                {
                    new CoreEngineSynchronizer().now();

                    return Boolean.valueOf(true);
                }
            });

            sequence.addTask(postExecute);
        }

        sequence.now();
    }
}

