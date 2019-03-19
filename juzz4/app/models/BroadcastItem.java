package models;

import com.google.code.morphia.annotations.Entity;
import com.kaisquare.sync.CommandType;
import play.modules.morphia.Model;

/**
 * These items will be delivered through the normal command channel using {@link #commandType}.
 * {@link #jsonObject} holds the modified object.
 *
 * Refer to {@link jobs.cloud.independent.BroadcastBucketChangesJob}
 *
 *
 * @author Aye Maung
 * @since v4.4
 *
 */
@Entity
public class BroadcastItem extends Model {
    private final long bucketId;
    private final CommandType commandType;
    private final String jsonObject;

    public static void queue(String bucketId, CommandType commandType, String jsonObject) {
        removeOutstandingItems(bucketId, commandType);
        BroadcastItem item = new BroadcastItem(bucketId, commandType, jsonObject);
        item.save();
    }

    public static void removeOutstandingItems(String bucketId, CommandType commandType) {
        BroadcastItem.q()
                .filter("bucketId", Long.parseLong(bucketId))
                .filter("commandType", commandType)
                .delete();
    }

    public static BroadcastItem getOldest() {
        return BroadcastItem.q().order("_created").first();
    }

    private BroadcastItem(String bucketId, CommandType commandType, String jsonObject) {
        this.bucketId = Long.parseLong(bucketId);
        this.commandType = commandType;
        this.jsonObject = jsonObject;
    }

    public long getBucketId() {
        return bucketId;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getJsonObject() {
        return jsonObject;
    }
}
