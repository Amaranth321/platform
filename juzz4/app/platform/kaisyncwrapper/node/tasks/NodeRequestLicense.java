package platform.kaisyncwrapper.node.tasks;

import com.google.gson.Gson;
import lib.util.Util;
import models.NodeCommand;
import models.licensing.NodeLicense;
import models.node.NodeObject;
import platform.CloudActionMonitor;
import platform.CloudLicenseManager;
import play.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sent from nodes to request the currently-registered license
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeRequestLicense extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        List<String> parameters = command.getParameters();
        String jsonInfoMap = parameters.get(0);

        Map<String, Object> infoMap = new LinkedHashMap<>();
        infoMap = new Gson().fromJson(jsonInfoMap, infoMap.getClass());

        //verify data
        if (!infoMap.containsKey("registration-number"))
        {
            Logger.error(Util.whichFn() + "no registration number");
            return false;
        }
        if (!infoMap.containsKey("license-number"))
        {
            Logger.error(Util.whichFn() + "no license number");
            return false;
        }

        String regNumber = infoMap.get("registration-number").toString();
        String licenseNumber = infoMap.get("license-number").toString();

        //authenticate
        NodeObject nodeObject = getNodeObject();
        NodeLicense license = CloudLicenseManager.getInstance().getDbNodeLicense(licenseNumber);
        if (!nodeObject.getNodeId().equals(license.nodeCloudPlatormId.toString()) ||
            !license.registrationNumber.equals(regNumber))
        {
            Logger.error(Util.whichFn() + "authentication failed (%s)", nodeObject.getName());
            return false;
        }

        //reply
        CloudActionMonitor.getInstance().cloudUpdatedNodeLicense(command.getNodeId(), license);

        return true;
    }
}
