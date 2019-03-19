package models.archived;

import com.google.code.morphia.annotations.Entity;
import platform.Environment;
import play.modules.morphia.Model;

@Entity
public class ArchivedBucket extends Model {
    public Long bucketId;
    public Long time;

    public ArchivedBucket(Long bucketId) {
        this.bucketId = bucketId;
        time = Environment.getInstance().getCurrentUTCTimeMillis();
    }
}


