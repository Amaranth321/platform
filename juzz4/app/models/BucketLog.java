package models;

import com.google.code.morphia.annotations.Entity;
import platform.Environment;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;

@Entity
public class BucketLog extends Model {
    public Long bucketId;
    public Long time;
    public String bucketName;
    public String username;
    public String remoteIp;
    public List<String> changes;

    public BucketLog() {
        time = Environment.getInstance().getCurrentUTCTimeMillis();
        changes = new ArrayList<>();
    }
}


