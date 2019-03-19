package jobs.cloud.independent;

import jobs.cloud.CloudCronJob;
import lib.util.Util;
import models.command.cloud.CloudNodeCommand;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.kaisyncwrapper.CloudCommandQueueStats;
import platform.kaisyncwrapper.cloud.PerNodeQueue;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * This job monitors {@link models.command.cloud.CloudNodeCommand}
 * and assign them to each {@link platform.kaisyncwrapper.cloud.PerNodeQueue} based on node id.
 * <p/>
 * This job exists to ensure that commands to the same node are executed in the same order they were sent on cloud.
 * <p/>
 * There must be only one of this job running. Hence, only cron instance will run this.
 *
 * @author Aye Maung
 * @since v4.4
 */
@Every("2s")
public class CloudCommandQueuesJob extends CloudCronJob
{
    private final ConcurrentHashMap<String, PerNodeQueue> nodeQueues = new ConcurrentHashMap<>();

    //monitoring
    private final ConcurrentHashMap<String, Long> processingStartTimes = new ConcurrentHashMap<>();
    private int unknownQueueResetCount;

    @Override
    public void doJob()
    {
        //check commands without a queue
        List<CloudNodeCommand> noQueueCommands = CloudNodeCommand.q()
                .filter("nodeId nin", nodeQueues.keySet().toArray())
                .fetchAll();
        for (CloudNodeCommand noQueueCommand : noQueueCommands)
        {
            createNodeQueue(noQueueCommand.getNodeId());
        }

        for (PerNodeQueue nodeQueue : nodeQueues.values())
        {
            //remove queues without a device
            CachedDevice cachedDevice = CacheClient.getInstance().getDeviceByPlatformId(nodeQueue.getNodeId());
            if (cachedDevice == null)
            {
                if (!nodeQueue.isProcessing())
                {
                    Logger.info(Util.whichClass() + "Removing (%s) queue without device", nodeQueue.getNodeName());
                    nodeQueues.remove(nodeQueue.getNodeId());
                }
                else
                {
                    nodeQueue.stopAfterCurrentTask();
                }
            }

            //wake up idle queues
            else if (!nodeQueue.isProcessing() && !nodeQueue.isEmpty())
            {
                nodeQueue.process();
            }

            //log
            logStartTime(nodeQueue);
        }
    }

    public CloudCommandQueuesJob()
    {
        startMonitorJob();
        startStatsJob();
    }

    private void createNodeQueue(String nodeId)
    {
        if (nodeQueues.containsKey(nodeId))
        {
            return;
        }

        CachedDevice cachedDevice = CacheClient.getInstance().getDeviceByPlatformId(nodeId);
        if (cachedDevice == null)
        {
            Logger.error(Util.whichClass() + "Device (id=%s) no longer exists. Removing queued commands.", nodeId);
            CloudNodeCommand.q().filter("nodeId", nodeId).delete();
            return;
        }

        nodeQueues.put(nodeId, new PerNodeQueue(cachedDevice.getName(), nodeId));
        Logger.info(Util.whichClass() + "Added node queue (%s)", cachedDevice.getName());
    }

    private void logStartTime(PerNodeQueue nodeQueue)
    {
        String nodeId = nodeQueue.getNodeId();
        if (!nodeQueue.isProcessing())
        {
            processingStartTimes.remove(nodeId);
            return;
        }

        synchronized (processingStartTimes)
        {
            if (!processingStartTimes.containsKey(nodeId))
            {
                processingStartTimes.put(nodeId, System.currentTimeMillis());
            }
        }
    }

    private void startMonitorJob()
    {
        //checks every 15 seconds which node queue is running for more than 30 seconds
        //if there are no pending commands in KaiSync, reset it.
        new Job()
        {
            @Override
            public void doJob()
            {
                try
                {
                    long now = System.currentTimeMillis();
                    for (Map.Entry<String, Long> entry : processingStartTimes.entrySet())
                    {
                        String nodeId = entry.getKey();
                        long queueStartTime = entry.getValue();

                        if (now - queueStartTime > TimeUnit.SECONDS.toMillis(30))
                        {
                            PerNodeQueue nodeQueue = nodeQueues.get(nodeId);
                            if (nodeQueue == null)
                            {
                                processingStartTimes.remove(nodeId);
                                continue;
                            }

                            if (!nodeQueue.pendingInKaiSync())
                            {
                                Logger.error(Util.whichClass() + "%s queue seems stuck. Resetting.",
                                             nodeQueue.getNodeName());
                                nodeQueue.reset();
                                unknownQueueResetCount++;
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    Logger.error(e, "");
                }
            }
        }.every(15);
    }

    private void startStatsJob()
    {
        new Job()
        {
            @Override
            public void doJob()
            {
                try
                {
                    List<CloudCommandQueueStats.QueueStats> activeList = new ArrayList<>();
                    List<CloudCommandQueueStats.QueueStats> idleList = new ArrayList<>();
                    List<CloudCommandQueueStats.QueueStats> combinedList = new ArrayList<>();
                    for (String nodeId : nodeQueues.keySet())
                    {
                        PerNodeQueue queue = nodeQueues.get(nodeId);
                        long waitingTime = 0;
                        if (queue.isProcessing())
                        {
                            if (processingStartTimes.containsKey(nodeId))
                            {
                                waitingTime = System.currentTimeMillis() - processingStartTimes.get(nodeId);
                            }

                            activeList.add(new CloudCommandQueueStats.QueueStats(queue.getNodeName(),
                                                                                 queue.isProcessing(),
                                                                                 queue.query().count(),
                                                                                 waitingTime));
                        }
                        else
                        {
                            idleList.add(new CloudCommandQueueStats.QueueStats(queue.getNodeName(),
                                                                               queue.isProcessing(),
                                                                               queue.query().count(),
                                                                               waitingTime));
                        }
                    }
                    combinedList.addAll(activeList);
                    combinedList.addAll(idleList);
                    CloudCommandQueueStats.set(combinedList, unknownQueueResetCount);
                }
                catch (Exception e)
                {
                    Logger.error(e, "");
                }
            }
        }.every(5);
    }
}
