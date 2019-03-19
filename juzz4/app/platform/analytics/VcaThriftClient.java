package platform.analytics;

import com.google.gson.Gson;
import com.kaisquare.util.ThriftUtil;
import com.kaisquare.vca.thrift.*;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.MongoDevice;
import org.apache.thrift.transport.TTransportException;
import platform.Environment;
import platform.common.ACResource;
import platform.config.readers.ConfigsShared;
import platform.node.NodeManager;
import platform.time.RecurrenceRule;
import play.Logger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.5
 */
public enum VcaThriftClient
{
    INSTANCE;

    private InetSocketAddress vcaServerAddress;
    private VcaServices.Iface clientIface;

    public static VcaThriftClient getInstance()
    {
        return INSTANCE;
    }

    public boolean isVcaServerOnline()
    {
        try (ACResource<Socket> acRes = new ACResource<>(new Socket()))
        {
            int timeoutMillis = 10 * 1000;
            acRes.get().connect(vcaServerAddress, timeoutMillis);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public List<IVcaInstance> getVcaList(List<String> coreDeviceIdList)
    {
    	
    	
        if (coreDeviceIdList != null && coreDeviceIdList.isEmpty())
        {
            return new ArrayList<>();
        }

        try
        {
            List<IVcaInstance> resultList = new ArrayList<>();
            for (TVcaInstance tVcaInstance : getIface().getVcaList())
            {
            	
                LocalVcaInstance platformInst = new LocalVcaInstance(tVcaInstance);
                String coreDeviceId = platformInst.getVcaInfo().getCamera().getCoreDeviceId();

                //check camera existence
                MongoDevice camera = platformInst.getVcaInfo().getCamera().getDbDevice();
                if (camera == null)
                {
                    platformInst.remove();
                    Logger.info("Removed vca instance with missing camera device (%s)", platformInst);
                    continue;
                }

                //filter
                if (coreDeviceIdList == null || coreDeviceIdList.contains(coreDeviceId))
                {
                    resultList.add(platformInst);
                }
            }
            return resultList;
        }
        catch (Exception e)
        {
            if (Environment.getInstance().onKaiNode())
            {
                Logger.error(Util.whichFn() + e.getMessage());
            }
        }

        return new ArrayList<>();
    }

    public IVcaInstance find(String instanceId)
    {
        try
        {
            for (TVcaInstance tVcaInstance : getIface().getVcaList())
            {
                if (tVcaInstance.getVcaInfo().getInstanceId().equals(instanceId))
                {
                    LocalVcaInstance platformInst = new LocalVcaInstance(tVcaInstance);
                    return platformInst;
                }
            }
            return null;
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
            return null;
        }
    }

    public LocalVcaInstance addVca(VcaInfo vcaInfo)
    {
        try
        {
            String ruleString = vcaInfo.getRecurrenceRule() == null ?
                                "" :
                                new Gson().toJson(vcaInfo.getRecurrenceRule(), RecurrenceRule.class);

            TVcaInfo vcaDetails = new TVcaInfo(
                    vcaInfo.getInstanceId(),
                    vcaInfo.getAppId(),
                    vcaInfo.getCamera().getCoreDeviceId(),
                    vcaInfo.getCamera().getChannelId(),
                    vcaInfo.getSettings(),
                    ruleString,
                    vcaInfo.isEnabled()
            );

            TVcaInstance created = getIface().addVca(vcaDetails);
            return new LocalVcaInstance(created);
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
            return null;
        }
    }

    public boolean updateVca(String instanceId, String settings, RecurrenceRule recurrenceRule)
    {
        try
        {
            String ruleString = recurrenceRule == null ? "" :
                                new Gson().toJson(recurrenceRule, RecurrenceRule.class);

            return getIface().updateVca(instanceId,
                                        settings,
                                        ruleString);
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
            return false;
        }
    }

    public boolean removeVca(String instanceId)
    {
        try
        {
            return getIface().removeVca(instanceId);
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
            return false;
        }
    }

    public boolean activateVca(String instanceId)
    {
        try
        {
            return getIface().activateVca(instanceId);
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
            return false;
        }
    }

    public boolean deactivateVca(String instanceId)
    {
        try
        {
            return getIface().deactivateVca(instanceId);
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
            return false;
        }
    }

    public List<String> getVcaProcessCommands(String instanceId)
    {
        try
        {
            return getIface().getVcaProcessCommands(instanceId);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public TVcaServerInfo getServerInformation()
    {
        try
        {
            return getIface().getServerInformation();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public List<VcaAppInfo> getVcaAppInfoList()
    {
        try
        {
            List<String> excludedApps = new ArrayList<>();
            if (Environment.getInstance().onKaiNode())
            {
                long modelId = NodeManager.getInstance().getNodeInfo().getDeviceModel().modelId;
                excludedApps = ConfigsShared.getInstance().excludedApps(modelId);
            }
            Logger.info("vca thrift server is online???"+isVcaServerOnline());
            List<TVcaAppInfo> tInfoList = getIface().getSupportedApps();
            List<VcaAppInfo> retList = new ArrayList<>();
            for (TVcaAppInfo tInfo : tInfoList)
            {
                VcaAppInfo appInfo = VcaAppInfo.fromThrift(tInfo);
                if (excludedApps.contains(appInfo.appId))
                {
                    Logger.info("%s app is excluded from this node", appInfo.appId);
                    continue;
                }

                retList.add(appInfo);
            }

            return retList;
        }
        catch (Exception e)
        {
        	Logger.error(Util.whichFn()+e.toString());
        	Logger.error(getStackMsg(e));
            return new ArrayList<>();
        }
    }

    private VcaServices.Iface getIface() throws TTransportException, ApiException
    {
        if (clientIface == null)
        {
            initClient();
        }

        if (!isVcaServerOnline())
        {
            throw new ApiException("vca server is offline");
        }

        return clientIface;
    }

    private void initClient() throws TTransportException
    {
        vcaServerAddress = ConfigsShared.getInstance().vcaServerAddress();
        if (vcaServerAddress == null)
        {
            Logger.error(Util.whichFn() + "servers.analytics is empty");
            return;
        }

        String host = vcaServerAddress.getHostName();
        int port = vcaServerAddress.getPort();

        Logger.info("Initializing VcaThriftClient (%s:%s)", host, port);
        ThriftUtil.Client<VcaServices.Iface> vcaClient = ThriftUtil.newServiceClient(
                VcaServices.Iface.class,
                VcaServices.Client.class,
                host,
                port,
                ThriftUtil.DEFAULT_TIMEOUT_MILLIS);

        clientIface = vcaClient.getIface();
    }
    
    private String getStackMsg(Exception e) {
    	StringBuffer sb = new StringBuffer();
    	StackTraceElement[] arr = e.getStackTrace();
    	for(StackTraceElement ele:arr) {
    		sb.append(ele.toString()+"\n");
    	}
    	return sb.toString();
    }

}
