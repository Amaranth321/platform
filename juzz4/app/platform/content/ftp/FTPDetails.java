package platform.content.ftp;

import lib.util.Util;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class FTPDetails
{
    // region fields

    private String protocol;
    private String host;
    private int port;
    private String username;
    private String password;
    private String directory;

    // endregion

    public static FTPDetails notConfigured()
    {
        return new FTPDetails("", "", 0, "", "", "");
    }

    public FTPDetails(String protocol, String host, int port, String username, String password, String directory)
    {
        this.protocol = protocol == null ? "ftp" : protocol;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.directory = normalizePath(directory);
    }

    // region getters

    public String getProtocol()
    {
        return protocol == null ? "ftp" : protocol;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public String getDirectory()
    {
        return normalizePath(directory);
    }

    // endregion

    // region setters

    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public void setDirectory(String directory)
    {
        this.directory = normalizePath(directory);
    }

    // endregion

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof FTPDetails)
        {
            FTPDetails other = (FTPDetails) o;
            return this.protocol.equalsIgnoreCase(other.protocol) &&
                   this.host.equalsIgnoreCase(other.host) &&
                   this.port == other.port &&
                   this.username.equals(other.username) &&
                   this.username.equals(other.username) &&
                   this.directory.equals(other.directory);
        }

        return false;
    }

    @Override
    public String toString()
    {
        return String.format("%s://%s@%s:%s", protocol, username, host, port);
    }

    public boolean hasSameDirectory(FTPDetails other)
    {
        return this.getHost().equals(other.getHost()) &&
               this.getPort() == other.getPort() &&
               this.getDirectory().equals(other.getDirectory());
    }

    // region private methods

    private String normalizePath(String path)
    {
        String normalizedPath = path;
        if (normalizedPath == null)
        {
            return normalizedPath;
        }

        normalizedPath = normalizedPath.trim();
        if (!normalizedPath.isEmpty() && normalizedPath.charAt(normalizedPath.length() - 1) != '/')
        {
            normalizedPath += "/";
        }
        return normalizedPath;
    }

    // endregion
}
