package jobs.migration;

import com.google.gson.Gson;
import models.backwardcompatibility.VcaInstance;
import platform.analytics.LocalVcaInstance;
import platform.analytics.Program;
import platform.analytics.VcaInfo;
import platform.analytics.VcaType;
import platform.analytics.app.AppVcaTypeMapper;
import platform.devices.DeviceChannelPair;
import play.Logger;
import play.jobs.Job;

import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class Migrate44NodeVcaJob extends Job<Boolean>
{
    @Override
    public Boolean doJobWithResult()
    {
        Gson gson = new Gson();
        List<VcaInstance> localList = VcaInstance.q().asList();
        if (localList.isEmpty())
        {
            return true; //nothing to migrate
        }

        for (VcaInstance oldInst : localList)
        {
            try
            {
                String appId = AppVcaTypeMapper.getAppId(Program.KAI_X1, VcaType.parse(oldInst.type));
                VcaType vcaType = AppVcaTypeMapper.getVcaType(appId);
                switch (vcaType)
                {
                    //Migrate Area Loitering mask percent, due with vca is updated.
                    case AREA_LOITERING:
                        Logger.info("Migrating AREA_LOITERING 4.4 vca instances.");
                        Map settings = gson.fromJson(oldInst.thresholds, Map.class);
                        if (settings != null)
                        {
                            if (!settings.containsKey("maskPercent"))
                            {
                                settings.put("maskPercent", 10);
                            }
                            else
                            {
                                double maskPercent = (Double) settings.get("maskPercent");
                                if (maskPercent == 0)
                                {
                                    settings.put("maskPercent", 10);
                                    oldInst.thresholds = gson.toJson(settings);
                                }
                            }
                        }
                        break;
                }

                VcaInfo vcaInfo = VcaInfo.createNew(oldInst.instanceId,
                        appId,
                        new DeviceChannelPair(oldInst.coreDeviceId, oldInst.channelId),
                        oldInst.thresholds,
                        oldInst.recurrenceRule,
                        oldInst.enabled);
                LocalVcaInstance.addNew(vcaInfo);
                oldInst.delete();
                Logger.info("Migrated vca instance (%s)", vcaInfo);
            }
            catch (Throwable e)
            {
                Logger.error(e, "Failed to migrate %s", oldInst);
            }
        }

        //retry later if not all migrations are successful
        if (VcaInstance.q().count() > 0)
        {
            return false;
        }

        Logger.info("Migrated all local 4.4 instances. Job will exit now.");
        return true;
    }
}
