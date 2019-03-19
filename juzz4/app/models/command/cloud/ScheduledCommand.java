package models.command.cloud;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.query.Criteria;
import com.kaisquare.sync.CommandType;
import lib.util.exceptions.ApiException;
import models.MongoBucket;
import platform.BucketManager;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Schedule a batch of nodes to execute particular command in selected time
 * once.
 *
 * @author Keith
 * @since v4.6
 */
@Entity
@Indexes({
        @Index("scheduledTime"),
        @Index("status, scheduledTime"),
        @Index("bucketId, commandType, scheduledTime")
})
public class ScheduledCommand extends Model
{
    private final String bucketId;
    private final CommandType commandType;
    private final long scheduledTime;
    private final List<String> scheduledNodeIds; // Cloud's Platform Device ID
    private final List<String> commandParams;
    private Status status;

    private ScheduledCommand(String bucketId,
                             CommandType commandType,
                             long scheduledTime,
                             List<String> scheduledNodeIds,
                             List<String> commandParams)
    {
        this.bucketId = bucketId;
        this.commandType = commandType;
        this.scheduledTime = scheduledTime;
        this.scheduledNodeIds = scheduledNodeIds;
        this.commandParams = commandParams;
        this.status = Status.PENDING;
    }

    public static ScheduledCommand createNew(String bucketId,
                                             CommandType commandType,
                                             long scheduledTime,
                                             List<String> scheduledNodeIds,
                                             List<String> commandParams)
    {
        return new ScheduledCommand(bucketId, commandType, scheduledTime, scheduledNodeIds, commandParams).save();
    }

    public static Iterable<ScheduledCommand> findByScheduledTime(long scheduledTime)
    {
        Iterable list = ScheduledCommand.filter("scheduledTime", scheduledTime).fetch();
        return list;
    }

    public static List<ScheduledCommand> findByTimeRange(String bucketId, CommandType commandType, long startTime, long endTime)
            throws ApiException
    {
        MorphiaQuery q = ScheduledCommand.q();
        List<MongoBucket> visibleBuckets = BucketManager.getInstance().getThisAndDescendants(bucketId);
        List<String> bucketList = new ArrayList<>();
        for (MongoBucket bucket : visibleBuckets)
        {
            bucketList.add(bucket.getBucketId());
        }

        q.field("bucketId").hasAnyOf(bucketList);
        q.field("commandType").equal(commandType);

        if (startTime > 0)
            q.filter("scheduledTime >=", startTime);

        if (endTime > 0)
            q.filter("scheduledTime <=", endTime);

        q.order("-scheduledTime");
        return q.asList();
    }

    public String getBucketId()
    {
        return bucketId;
    }

    public CommandType getCommandType()
    {
        return commandType;
    }

    public long getScheduledTime()
    {
        return scheduledTime;
    }

    public List<String> getScheduledNodeIds()
    {
        return scheduledNodeIds;
    }

    public List<String> getCommandParams()
    {
        return commandParams;
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    /**
     * <b>PENDING</b> (Command not yet to process),
     * <b>ERROR</b> (Command is encountered error during the process),
     * <b>COMPLETED</b> (Command is processed without error)
     */
    public enum Status
    {
        PENDING,
        ERROR,
        COMPLETED
    }
}
