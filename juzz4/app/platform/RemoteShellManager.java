package platform;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.MongoDevice;
import models.RemoteShellState;
import models.RemoteShellState.ConnectionState;

public class RemoteShellManager
{
    private static RemoteShellManager instance = null;
    private static CloudActionMonitor cloudActionMonitor = null;

    public static RemoteShellManager getInstance()
    {
        if (instance == null)
        {
            instance = new RemoteShellManager();
            cloudActionMonitor = CloudActionMonitor.getInstance();
        }
        return instance;
    }

    private boolean validateStartRemoteShellParams(String deviceId, String host, Integer port, String username) throws ApiException
    {
        //validate parameters
        if (Util.isNullOrEmpty(deviceId))
        {
            throw new ApiException("invalid-device-id");
        }

        if (Util.isNullOrEmpty(host))
        {
            throw new ApiException("invalid-host");
        }

        if (port == null || port < 1)
        {
            throw new ApiException("invalid-port");
        }

        if (Util.isNullOrEmpty(username))
        {
            throw new ApiException("invalid-user-name");
        }

        return true;
    }

    /**
     * Sends the start remote shell command to the specified node.
     * 
     * @param deviceId      Node's platform ID on cloud
     * @param host          Hostname of the server with which the node should open SSH session
     * @param port          Port of the server with which the node should open SSH session
     * @param username      Username to use for the ssh session with the server
     * @throws ApiException 
     */
    public void startRemoteShell(String deviceId, String host, Integer port, String username) throws ApiException
    {
        validateStartRemoteShellParams(deviceId, host, port, username);

        MongoDevice device = MongoDevice.getByPlatformId(deviceId);
        if(device == null)
        {
            throw new ApiException("invalid-device-id");
        }

        //for cloud platform to kainode device
        if (device.isKaiNode())
        {
            String nodePlatformId = device.getDeviceId();
            String macAddress = device.getDeviceKey().toLowerCase();
            
            Datastore ds = RemoteShellState.ds();
            Query<RemoteShellState> query = ds.createQuery(RemoteShellState.class);
            query.field("macAddress").equal(macAddress);

            UpdateOperations<RemoteShellState> ops = ds.createUpdateOperations(RemoteShellState.class);
            ops.set("cloudPlatformDeviceId", nodePlatformId);
            ops.set("host", host);
            ops.set("port", port);
            ops.set("username", username);
            ops.set("connectionState", ConnectionState.CLOUD_REQUESTED_START);
            ds.findAndModify(query, ops, false, true);
            cloudActionMonitor.cloudStartedRemoteShell(nodePlatformId, host, port, username);
        }
    }

    /**
     * Sends the start remote shell command to the specified node.
     * 
     * @param deviceId      Node's platform ID on cloud
     * @throws ApiException 
     */
    public void stopRemoteShell(String deviceId) throws ApiException
    {
        MongoDevice device = MongoDevice.getByPlatformId(deviceId);
        if(device == null)
        {
            throw new ApiException("invalid-device-id");
        }

        //for cloud platform to kainode device
        if (device.isKaiNode())
        {
            String nodePlatformId = device.getDeviceId();
            String macAddress = device.getDeviceKey();

            Datastore ds = RemoteShellState.ds();
            Query<RemoteShellState> query = ds.createQuery(RemoteShellState.class);
           	query.field("macAddress").equal(macAddress);

            UpdateOperations<RemoteShellState> ops = ds.createUpdateOperations(RemoteShellState.class);
            ops.set("cloudPlatformDeviceId", nodePlatformId);
            ops.set("connectionState", ConnectionState.CLOUD_REQUESTED_STOP);
            ds.findAndModify(query, ops, false, true);
            
            cloudActionMonitor.cloudStoppedRemoteShell(nodePlatformId);
        }
    }
    
    /**
     * Get current status of remote shell of specific node
     * @param deviceId      Node's platform ID on cloud
     * 
     * @return
     * @throws ApiException
     */
    public RemoteShellState getRemoteShellState(String deviceId) throws ApiException
    {
        MongoDevice device = MongoDevice.getByPlatformId(deviceId);
    	if (device == null)
    	{
    		throw new ApiException("invalid-device-id");
    	}
    	
    	RemoteShellState state = null;
    	//for cloud platform to kainode device
        if (device.isKaiNode())
        {
            String nodePlatformId = device.getDeviceId();
            state = RemoteShellState.find("cloudPlatformDeviceId", nodePlatformId).first();
        }
        
        return state;
    }
}
