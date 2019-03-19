package platform.node;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

import lib.util.Util;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.json.JSONObject;

import play.Logger;

import com.google.gson.Gson;
import com.kaisquare.kaisync.ClientConnection;
import com.kaisquare.util.StringCollectionUtil;

/**
 * The class helps communicate with Node device locally
 */
public final class KaiNodeAdminService
{

    public static final String NETWORK_STATIC = "static";
    public static final String NETWORK_DHCP = "dhcp";
	private static final JSONObject EMPTY_JSON = new JSONObject();

    private static KaiNodeAdminService mInstance;

    private int servicePort = 9000;

    private KaiNodeAdminService()
    {
    }

    /**
     * Get node network settings, e.g. IP, netmask, gateway, nameservers
     */
    public NetworkSettings getNetworkSettings()
    {
    	try {
	        JSONObject result = readAsJson(request().get(getUrl("getnetwork")));
	
	        if ("ok".equalsIgnoreCase(result.getString("result")))
	        {
	            JSONObject network = result.getJSONObject("data");
	            NetworkSettings settings = new NetworkSettings();
	            settings.type = network.getString("type");
	            settings.ipAddress = network.getString("ip");
	            settings.netmask = network.getInt("netmask");
	            settings.gateway = network.getString("gateway");
	            settings.nameservers = new ArrayList<String>();
	
	            String nameservers = network.getString("nameservers");
	            if (nameservers != null && !"".equals(nameservers))
	            {
	                String[] list = nameservers.split(" ");
                    Collections.addAll(settings.nameservers, list);
	            }
	
	            return settings;
	        }
    	} catch (Exception e) {
    		Logger.warn(e, "getNetworkSettings");
    	}
    	
    	return null;
    }

    /**
     * Get node software update status.
     *
     * @return UpdateFileInfo
     */
    public UpdateFileInfo getSoftwareUpdate()
    {
        try
        {
            JSONObject result = readAsJson(request().get(getUrl("softwareupdate")));
            if (!"ok".equalsIgnoreCase(result.getString("result")))
            {
                Logger.info(Util.whichFn() + result);
                return null;
            }

            String data = String.valueOf(result.get("data"));
            UpdateFileInfo fileInfo = new Gson().fromJson(data, UpdateFileInfo.class);

            return fileInfo;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    /**
     * Update node's software.
     *
     * @return true on success, false otherwise.
     */
    public boolean updateNodeSoftware()
    {
    	try {
	        JSONObject result = readAsJson(request().get(getUrl("ota-update")));
	        return "ok".equalsIgnoreCase(result.getString("result"));
    	} catch (Exception e) {
    		Logger.warn(e, "updateNodeSoftware");
    	}
    	
    	return false;
    }

    /**
     * Set node network settings
     *
     * @param ipaddress   IP address
     * @param netmask     CIDR number
     * @param gateway     default gateway
     * @param nameservers a list of name server
     *
     * @return True on success, false otherwise
     */
    public boolean setNetworkSettings(String ipaddress, int netmask, String gateway, List<String> nameservers)
    {
        try
        {
            int mask = 0xffffffff << (32 - netmask);
            String netmaskString = String.format("%d.%d.%d.%d",
                                                 mask >>> 24,
                                                 mask >> 16 & 0xff,
                                                 mask >> 8 & 0xff,
                                                 mask & 0xff);
            String nameserversString = StringCollectionUtil.join(nameservers, " ");
            return setNetworkSettings(ipaddress, netmaskString, gateway, nameserversString);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    /**
     * Set node network settings
     *
     * @param ipaddress   IP address
     * @param netmask     CIDR number
     * @param gateway     default gateway
     * @param nameservers it should be a comma separated string e.g. "8.8.8.8, 8.8.1.1"
     *
     * @return True on success, false otherwise
     */
    public boolean setNetworkSettings(String ipaddress, String netmask, String gateway, String nameservers)
    {
    	try {
	        Map<String, String> params = new HashMap<String, String>();
	        params.put("ip", ipaddress);
	        params.put("netmask", netmask);
	        params.put("gateway", gateway);
	        params.put("nameservers", nameservers);
	        JSONObject result = readAsJson(request().post(getUrl("setnetwork"), params));
	
	        return "ok".equalsIgnoreCase(result.getString("result"));
    	} catch (Exception e) {
    		Logger.warn(e, "setNetworkSettings");
    		return false;
    	}
    }

    /**
     * Get time zone from local node
     */
    public String getTimezone()
    {
    	try {
	        JSONObject result = readAsJson(request().get(getUrl("gettimezone")));
	        if ("ok".equalsIgnoreCase(result.getString("result")))
	            return result.getString("data");
    	} catch (Exception e) {
    		Logger.warn(e, "getTimezone");
    	}
    	
    	return null;
    }

    /**
     * Set specific time zone on this node
     *
     * @param timezone the time zone string should already exist on Ubuntu
     */
    public boolean setTimezone(String timezone)
    {
        try
        {
            Map<String, String> params = new HashMap<String, String>();
            params.put("timezone", timezone);
            JSONObject result = readAsJson(request().post(getUrl("settimezone"), params));

            return "ok".equalsIgnoreCase(result.getString("result"));
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    /**
     * Shutdown device
     */
    public void shutdownDevice()
    {
        try
		{
			request().get(getUrl("shutdowndevice"));
		} catch (Exception e)
		{
			Logger.warn(e, "shutdownDevice");
		}
    }

    /**
     * Reboot device
     */
    public void rebootDevice()
    {
        try
		{
			request().get(getUrl("rebootdevice"));
		} catch (Exception e)
		{
			Logger.warn(e, "rebootDevice");
		}
    }

    /**
     * Wipe out all of data on this node
     */
    public void factoryReset()
    {
        try
		{
			request().get(getUrl("factoryreset"));
		} catch (Exception e)
		{
			Logger.warn(e, "factoryReset");
		}
    }

    private String getUrl(String path)
    {
        String url = String.format("http://localhost:%d/%s",
                                   servicePort, path);

        return url;
    }

    private ClientConnection request() throws IOException, TimeoutException
    {
        return new ClientConnection(0x4000);
    }
    
    private JSONObject readAsJson(HttpResponse response)
    {
    	if (response.getStatus().getCode() == HttpResponseStatus.OK.getCode())
    	{
    		try
			{
    			ChannelBuffer buffer = response.getContent();
    			byte[] content = new byte[buffer.readableBytes()];
    			buffer.readBytes(content);
    			String s = new String(content);
    			Logger.debug("read content from ks: %s", s);
				return new JSONObject(s);
			} catch (Exception e)
			{
				Logger.warn(e, "readAsJson");
			}
    	}
    	else
    		Logger.warn("KaiNodeService: connection failed: status=%s", response.getStatus());
    	
    	return EMPTY_JSON;
    }

    public static KaiNodeAdminService getInstance()
    {
        synchronized (KaiNodeAdminService.class)
        {
            if (mInstance == null)
            {
                mInstance = new KaiNodeAdminService();
            }
        }

        return mInstance;
    }

    public static class NetworkSettings
    {
        public String type;
        private String ipAddress;
        private int netmask;
        private String gateway;
        private List<String> nameservers;

        public static NetworkSettings emptyObject()
        {
            NetworkSettings emptySetts = new NetworkSettings();
            emptySetts.type = "";
            emptySetts.ipAddress = "";
            emptySetts.netmask = 0;
            emptySetts.gateway = "";
            emptySetts.nameservers = new ArrayList<>();
            return emptySetts;
        }

        private NetworkSettings()
        {
        }

        public String getAddress()
        {
            return ipAddress;
        }

        public int getNetmask()
        {
            return netmask;
        }

        public String getGateway()
        {
            return gateway;
        }

        public List<String> getNameservers()
        {
            return nameservers;
        }

    }

    public static class UpdateFileInfo
    {
        private String currentVersion;
        private String serverVersion;
        private String status;  // unavailable, pending, available
        private int percent;

        public String getCurrentVersion()
        {
            return currentVersion;
        }

        public String getServerVersion()
        {
            return serverVersion;
        }

        public String getStatus()
        {
            return status;
        }

        public int getPercent()
        {
            return percent;
        }
    }

}
