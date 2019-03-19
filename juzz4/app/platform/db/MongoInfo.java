package platform.db;

import lib.util.Util;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class MongoInfo
{
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public MongoInfo(String host,
                     int port,
                     String database,
                     String username,
                     String password)
    {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = Util.isNullOrEmpty(username) ? "" : username;
        this.password = Util.isNullOrEmpty(password) ? "" : password;
    }

    @Override
    public String toString()
    {
        return String.format("%s@%s:%s, %s", username, host, port, database);
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getDatabase()
    {
        return database;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

}
