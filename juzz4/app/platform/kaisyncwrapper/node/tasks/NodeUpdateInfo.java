package platform.kaisyncwrapper.node.tasks;

import com.google.gson.Gson;
import models.NodeCommand;
import models.node.NodeObject;
import platform.analytics.VcaAppInfo;
import play.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sent from nodes to update info.
 * <p/>
 * processCommand will be executed on cloud.
 * <p/>
 * All updated info are optional for backward compatibility.
 *
 * @author Aye Maung
 */

public class NodeUpdateInfo extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        try
        {
            Gson gson = new Gson();
            List<String> parameters = command.getParameters();
            String jsonInfoMap = parameters.get(0);

            Map<String, Object> infoMap = new LinkedHashMap<>();
            infoMap = gson.fromJson(jsonInfoMap, infoMap.getClass());

            NodeObject nodeObject = getNodeObject();

            //Node Version
            Object objVersion = infoMap.get("version");
            if (objVersion != null)
            {
                nodeObject.setNodeVersion(objVersion.toString());
            }

            //VCA Version from older nodes (< v4.5)
            Object vcaVersion = infoMap.get("vcaVersion");
            if (vcaVersion != null)
            {
                nodeObject.setSupportedAppList(VcaAppInfo.kaiX1Apps(String.valueOf(vcaVersion)));
            }

            //vca program list (v4.5 and above)
            Object objAppList = infoMap.get("vcaAppList");
            if (objAppList != null)
            {
                //parse transport objects to the actual list
                List<Map> supportedApps = new ArrayList<>();
                supportedApps = gson.fromJson(objAppList.toString(), supportedApps.getClass());

                //convert transport objects to actual list
                List<VcaAppInfo> appInfoList = VcaAppInfo.fromKaiSync(supportedApps);

                nodeObject.setSupportedAppList(appInfoList);
            }

            nodeObject.save();
            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }
}
