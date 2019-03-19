package jobs.node;

import com.google.gson.Gson;
import com.kaisquare.util.SysInfoUtil;
import lib.util.Util;
import models.node.NodeSettings;
import platform.Environment;
import platform.node.KaiNodeAdminService;
import platform.node.KaiSyncCommandClient;
import platform.node.NodeManager;
import play.Logger;
import play.jobs.Job;

import java.util.TimeZone;

public class NodeSettingCheckerJob extends Job
{
    public static final int FREQ_SECONDS = 30;

    @Override
    public void doJob()
    {
        try
        {
            if (!Environment.getInstance().onKaiNode() ||
                !NodeManager.getInstance().isRegisteredOnCloud())
            {
                return;
            }

            KaiNodeAdminService nodeAdminSvc = KaiNodeAdminService.getInstance();
            KaiSyncCommandClient syncCommandClt = KaiSyncCommandClient.getInstance();

            //Network settings
            NodeSettings dbSettings = NodeManager.getInstance().getSettings();
            KaiNodeAdminService.NetworkSettings networkSettings = nodeAdminSvc.getNetworkSettings();
            TimeZone timeZone = SysInfoUtil.getOSTimeZone(true);
            NodeSettings realSettings = new NodeSettings(networkSettings, timeZone);

            if (!isIdentical(dbSettings, realSettings))
            {
                NodeManager.getInstance().setSettings(realSettings, false);
            }

            //New update file
            KaiNodeAdminService.UpdateFileInfo updateFileInfo = nodeAdminSvc.getSoftwareUpdate();
            if (updateFileInfo != null)
            {
                if (updateFileInfo.getStatus().equalsIgnoreCase("available"))
                {
                    syncCommandClt.nodeUpdateFileReady(updateFileInfo.getServerVersion());
                }
            }
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
        }
        finally
        {
            in(FREQ_SECONDS);
        }
    }

    private boolean isIdentical(NodeSettings a, NodeSettings b)
    {
        if (a == null || b == null)
        {
            return false;
        }

        String strA = new Gson().toJson(a);
        String strB = new Gson().toJson(b);

        boolean same = strA.equals(strB);
        if (!same)
        {
            Logger.info("Old settings : %s", strA);
            Logger.info("New settings : %s", strB);
        }
        return same;
    }
}

