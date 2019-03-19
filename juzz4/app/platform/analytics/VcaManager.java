package platform.analytics;

import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.Analytics.NodeVcaInstance;
import models.MongoDevice;
import platform.Environment;
import platform.analytics.app.AppVcaTypeMapper;
import platform.devices.DeviceChannelPair;
import platform.pubsub.PlatformEventMonitor;
import platform.pubsub.PlatformEventSubscriber;
import platform.pubsub.PlatformEventTask;
import platform.pubsub.PlatformEventType;
import platform.time.PeriodOfDay;
import platform.time.RecurrenceRule;
import play.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Aye Maung
 * @since v4.5
 */
public abstract class VcaManager implements PlatformEventSubscriber
{
    private final ConcurrentHashMap<Double, Set<VcaType>> migratedVcaTypes = new ConcurrentHashMap<>();

    protected final Object actionLock = new Object();

    public static VcaManager getInstance()
    {
        if (Environment.getInstance().onKaiNode())
        {
            return NodeVcaManager.getInstance();
        }
        else
        {
            return CloudVcaManager.getInstance();
        }
    }

    public abstract void addNewVca(VcaInfo vcaInfo) throws ApiException;

    /**
     * @param coreDeviceIdList null to disable device filter
     */
    public abstract List<IVcaInstance> listVcaInstances(List<String> coreDeviceIdList);

    public abstract IVcaInstance getVcaInstance(String instanceId);

    public abstract boolean checkConcurrencyLimit(VcaInfo vcaInfo);

    public abstract boolean checkAppSupport(String appId, String platformDeviceId);

    public void restart(IVcaInstance instance)
    {
        try
        {
            VcaInfo vcaInfo = instance.getVcaInfo();
            Logger.info(Util.getCallerFn() + "Restarting vca (%s)", vcaInfo);

            //just call update with the same configurations
            instance.update(vcaInfo.getSettings(), vcaInfo.getRecurrenceRule());
        }
        catch (ApiException e)
        {
            Logger.error(e, "");
        }
    }

    public List<String> getVcaProcessCommands(String instanceId)
    {
        return VcaThriftClient.getInstance().getVcaProcessCommands(instanceId);
    }

    public List<IVcaInstance> listVcaInstancesOfDevice(MongoDevice device)
    {
        return listVcaInstances(Collections.singletonList(device.getCoreDeviceId()));
    }

    public List<IVcaInstance> listVcaInstancesByCamera(DeviceChannelPair camera)
    {
        List<IVcaInstance> instanceList = listVcaInstances(Collections.singletonList(camera.getCoreDeviceId()));
        List<IVcaInstance> filtered = new ArrayList<>();
        for (IVcaInstance instance : instanceList)
        {
            if (instance.getVcaInfo().getCamera().equals(camera))
            {
                filtered.add(instance);
            }
        }
        return filtered;
    }

    public NodeVcaInstance checkAndMigrate(Double nodeVersion, NodeVcaInstance inst) throws Exception
    {
        if (inst.migrationRequired())
        {
            //vca has been migrated already. Awaiting node's update
            return inst;
        }

        if (nodeVersion < 4.4)
        {
            VcaType vcaType = AppVcaTypeMapper.getVcaType(inst.getVcaInfo().getAppId());
            switch (vcaType)
            {
                //mask changed from paint to polygon
                case AREA_INTRUSION:
                case PERIMETER_DEFENSE:
                case AREA_LOITERING:
                    addMigratedVcaType(nodeVersion, vcaType);
                    inst.setMigrationRequired(true);
                    break;
            }
        }

        return inst;
    }

    public boolean isNodeUpdateRequiredForVca(Double nodeVersion, VcaType vcaType)
    {
        Set<VcaType> types = migratedVcaTypes.get(nodeVersion);
        if (types == null)
        {
            return false;
        }

        return types.contains(vcaType);
    }

    @Override
    public void subscribePlatformEvents()
    {
        PlatformEventMonitor evtMon = PlatformEventMonitor.getInstance();

        /**
         *  DEVICE_UPDATED
         */
        evtMon.subscribe(PlatformEventType.DEVICE_UPDATED, new PlatformEventTask()
        {
            @Override
            public void run(Object... params) throws Exception
            {
                MongoDevice device = (MongoDevice) params[0];
                if (device == null)
                {
                    throw new IllegalArgumentException();
                }

                List<IVcaInstance> deviceVcaList = listVcaInstancesOfDevice(device);
                for (IVcaInstance instance : deviceVcaList)
                {
                    restart(instance);
                }
            }
        });

        /**
         *  DEVICE_DELETED
         */
        evtMon.subscribe(PlatformEventType.DEVICE_DELETED, new PlatformEventTask()
        {
            @Override
            public void run(Object... params) throws Exception
            {
                MongoDevice device = (MongoDevice) params[0];
                if (device == null)
                {
                    throw new IllegalArgumentException();
                }

                List<IVcaInstance> deviceVcaList = listVcaInstancesOfDevice(device);
                for (IVcaInstance instance : deviceVcaList)
                {
                    try
                    {
                        instance.remove();
                    }
                    catch (Exception e)
                    {
                        Logger.error(e, "Failed to restart %s", instance.getVcaInfo());
                    }
                }
            }
        });
    }

    /**
     * This method uses intervals for overlap counting, which means if two periods are in the same interval,
     * they are considered to have an overlap.
     * Smaller interval sizes will have a finer check, but processing will take longer.
     *
     * @param vcaInfoList        vca list
     * @param minConcurrentCount periods with overlap counts less than this will be excluded
     *
     * @return a map of dayOfWeek with a map of the overlapping periods by vca instances
     */
    public Map<Integer, Map<PeriodOfDay, List<VcaInfo>>> findConcurrentVcaPeriods(List<VcaInfo> vcaInfoList, int minConcurrentCount)
    {
        int intervalSize = 15; //must be a factor of 60

        //basically, this will check from monday to sunday, 0 to 1440 mins based on interval size
        //and each overlap will be recorded in the finalResults
        Map<Integer, Map<PeriodOfDay, List<VcaInfo>>> finalResults = new LinkedHashMap<>();
        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++)
        {
            Map<PeriodOfDay, List<VcaInfo>> countsStore = new LinkedHashMap<>();

            //initialize periods with zero counts
            for (int startMins = 0; startMins < 24 * 60; startMins += intervalSize)
            {
                PeriodOfDay pod = new PeriodOfDay(startMins, startMins + intervalSize);
                countsStore.put(pod, new ArrayList<VcaInfo>());
            }

            for (VcaInfo vcaInfo : vcaInfoList)
            {
                RecurrenceRule rule = vcaInfo.getRecurrenceRule();
                if (rule == null)
                {
                    for (PeriodOfDay periodOfDay : countsStore.keySet())
                    {
                        countsStore.get(periodOfDay).add(vcaInfo);
                    }
                    continue;
                }

                List<PeriodOfDay> dayPeriods = rule.periodsOfDays.get(dayOfWeek);
                if (dayPeriods == null)
                {
                    continue;
                }

                for (PeriodOfDay period : dayPeriods)
                {
                    for (PeriodOfDay halfHourPeriod : countsStore.keySet())
                    {
                        if (halfHourPeriod.overlaps(period))
                        {
                            countsStore.get(halfHourPeriod).add(vcaInfo);
                        }
                    }
                }
            }

            //remove entries less than minConcurrentCount
            List<PeriodOfDay> keyList = new ArrayList<>(countsStore.keySet());
            for (PeriodOfDay periodOfDay : keyList)
            {
                if (countsStore.get(periodOfDay).size() < minConcurrentCount)
                {
                    countsStore.remove(periodOfDay);
                }
            }

            if (countsStore.isEmpty())
            {
                continue;
            }

            //join continuous periods
            Map<PeriodOfDay, List<VcaInfo>> jointCountsStore = new LinkedHashMap<>();
            List<Map.Entry<PeriodOfDay, List<VcaInfo>>> entryList = new ArrayList<>(countsStore.entrySet());
            Map.Entry<PeriodOfDay, List<VcaInfo>> start = entryList.get(0);
            Map.Entry<PeriodOfDay, List<VcaInfo>> prev = start;
            for (int i = 1; i < entryList.size(); i++)
            {
                Map.Entry<PeriodOfDay, List<VcaInfo>> current = entryList.get(i);

                //last entry
                if (i == entryList.size() - 1)
                {
                    PeriodOfDay jointPeriod = new PeriodOfDay(start.getKey().getStartMinutes(),
                                                              current.getKey().getEndMinutes());
                    jointCountsStore.put(jointPeriod, start.getValue());
                    break;
                }

                if (prev.getKey().getEndMinutes() == current.getKey().getStartMinutes() &&
                    prev.getValue().equals(current.getValue()))
                {
                    prev = current;
                    continue;
                }

                //cut
                PeriodOfDay jointPeriod = new PeriodOfDay(start.getKey().getStartMinutes(), prev.getKey().getEndMinutes());
                jointCountsStore.put(jointPeriod, start.getValue());

                start = current;
                prev = current;
            }


            finalResults.put(dayOfWeek, jointCountsStore);
        }

        return finalResults;
    }

    private void addMigratedVcaType(Double nodeVersion, VcaType vcaType)
    {
        Set<VcaType> types = migratedVcaTypes.get(nodeVersion);
        if (types == null)
        {
            types = new LinkedHashSet<>();
        }

        types.add(vcaType);
        migratedVcaTypes.put(nodeVersion, types);
    }
}
