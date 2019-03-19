package platform;

import com.kaisquare.kaisync.ISyncWriteFile;
import com.kaisquare.kaisync.KAISync;
import com.kaisquare.kaisync.file.IFileClient;
import com.kaisquare.kaisync.platform.IPlatformSync;

import lib.util.JsonReader;
import platform.config.readers.ConfigsServers;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class CloudSyncManager
{
    private static CloudSyncManager mInstance;

    private String cloudServerHost;
    private String syncServerHost;
    private int serverPort;
    private String keystore;
    private String keypass;
    private String fsKeystore;
    private String fsKeypass;
    private String username;
    private String password;

    private CloudSyncManager()
    {
        InetSocketAddress cloudServer = ConfigsServers.getInstance().cloudServer();
        cloudServerHost = cloudServer.getHostString();

        JsonReader platformSyncCfg = ConfigsServers.getInstance().kaisyncPlatformSyncCfg();
        syncServerHost = platformSyncCfg.getAsString("host", null);
        serverPort = platformSyncCfg.getAsInt("ssl-port", 0);
        keystore = platformSyncCfg.getAsString("keystore", null);
        keypass = platformSyncCfg.getAsString("keypass", null);
        username = platformSyncCfg.getAsString("user", null);
        password = platformSyncCfg.getAsString("password", null);

        JsonReader fileServerCfg = ConfigsServers.getInstance().kaisyncFileServerCfg();
        fsKeystore = fileServerCfg.getAsString("truststore", null);
        fsKeypass = fileServerCfg.getAsString("trustpass", null);
    }

    public String getCloudServerHost()
    {
        return cloudServerHost;
    }
    
    public String getCloudSyncServerHost()
    {
    	return syncServerHost;
    }

    public IPlatformSync getPlatformSync() throws IOException
    {
        return KAISync.newPlatformClient(syncServerHost, serverPort, keystore, keypass, username, password);
    }

    public IFileClient newFileClient()
    {
        JsonReader fileSvrCfg = ConfigsServers.getInstance().kaisyncFileServerCfg();
        String host = fileSvrCfg.getAsString("host", null);
        int port = fileSvrCfg.getAsInt("port", 0);

        return KAISync.newFileClient(host, port, fsKeystore, fsKeypass);
    }

    public ISyncWriteFile uploadFile(String filename) throws IOException
    {
        ISyncWriteFile file = getPlatformSync().uploadFile(filename);
        file.setKeystore(fsKeystore, fsKeypass);

        return file;
    }

    public String[] getFileServerKeystore()
    {
        return new String[]{fsKeystore, fsKeypass};
    }

    public static CloudSyncManager getInstance()
    {
        synchronized (CloudSyncManager.class)
        {
            if (mInstance == null)
            {
                mInstance = new CloudSyncManager();
            }
        }

        return mInstance;
    }

}
