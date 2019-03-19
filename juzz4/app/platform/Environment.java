package platform;

import lib.util.Util;
import models.MongoUser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.config.readers.ConfigsServers;
import platform.db.MongoInfo;
import play.Logger;
import play.Play;
import play.i18n.Lang;

public enum Environment
{
    INSTANCE;

    private String applicationType;
    private String os = "linux";
    private boolean httpReady = false;
    private long serverStarted;
    private boolean startupTasksCompleted = false;

    private Environment()
    {
        applicationType = ConfigsServers.getInstance().applicationType();
        if (System.getProperty("os.name").toLowerCase().contains("win"))
        {
            os = "win";
        }
        else
        {
            os = "linux";
        }
    }

    public synchronized static Environment getInstance()
    {
        return INSTANCE;
    }

    public void stopServer(String reason)
    {
        Util.printImptLog("Stopping server due to a critical error (%s)", reason);
        System.exit(1);
    }

    public String getApplicationType()
    {
        return applicationType;
    }

    public boolean isHttpReady()
    {
        return httpReady;
    }

    public void setHttpReady(boolean ready)
    {
        httpReady = ready;
        setServerStartedTime();
        Util.printImptLog("Ready for web and api calls");
    }

    public boolean isStartupTasksCompleted()
    {
        return startupTasksCompleted;
    }

    public void setStartupTasksCompleted(boolean startupTasksCompleted)
    {
        this.startupTasksCompleted = startupTasksCompleted;
    }

    public boolean onCloud()
    {
        return "cloud".equalsIgnoreCase(getApplicationType())
               || onSvc()
               || onCron();
    }

    public boolean onKaiNode()
    {
        return "node".equalsIgnoreCase(getApplicationType());
    }

    public boolean onWebInstance()
    {
        return onCloud() || onKaiNode();
    }

    public boolean onSvc()
    {
        return Play.id.equals("svc");
    }

    public boolean onCron()
    {
        return Play.id.equals("cron");
    }

    public boolean onWindows()
    {
        return "win".equals(os);
    }

    public void setPlayLanguage(MongoUser user, String browserLocale)
    {
        try
        {
            if (user != null && !Util.isNullOrEmpty(user.getLanguage()))
            {
                Lang.set(user.getLanguage());
            }
            else
            {
                // use browser default language
                Lang.set(browserLocale);
            }

            //if on node, use message.*-node if it's available
            if (onKaiNode() && !Lang.get().endsWith("-node"))
            {
                String locale = Lang.get() + "-node";
                if (Play.langs.contains(locale))
                {
                    Lang.set(locale);
                }
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    public long getCurrentUTCTimeMillis()
    {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        return now.getMillis();
    }

    public long getServerStartedTime()
    {
        return serverStarted;
    }

    public void setServerStartedTime()
    {
        serverStarted = System.currentTimeMillis();
    }

    public MongoInfo getMongoServerInfo()
    {
        String host = Play.configuration.getProperty("morphia.db.host");
        int port = Integer.parseInt(Play.configuration.getProperty("morphia.db.port"));
        String database = Play.configuration.getProperty("morphia.db.name");
        String username = Play.configuration.getProperty("morphia.db.username");
        String password = Play.configuration.getProperty("morphia.db.password");

        return new MongoInfo(host, port, database, username, password);
    }
}
