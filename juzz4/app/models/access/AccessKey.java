package models.access;

import com.google.code.morphia.annotations.Entity;
import models.MongoBucket;
import models.MongoUser;
import platform.Environment;
import play.modules.morphia.Model;

@Entity
public class AccessKey extends Model {

    public Long bucketID;
    public String bucket;
    public Long userID;
    public String userName;
    public String key;
    public Long ttl;    //expiry date in milliseconds
    public String payload;
    public int maxUseCount;
    public int currentUseCount;
    public boolean isValid;

    public AccessKey() {
        currentUseCount = 0;
        maxUseCount = -1;
        isValid = true;
    }

    public boolean ok() {
        //check bucket
        MongoBucket bucket = MongoBucket.getById(bucketID.toString());
        if (bucket == null || bucket.isDeleted()) {
            return false;
        }

        //check user
        MongoUser user = MongoUser.getById(userID.toString());
        if (user == null || !user.isActivated()) {
            return false;
        }

        Long nowMillis = Environment.getInstance().getCurrentUTCTimeMillis();

        //expired
        if (nowMillis > ttl) {
            return false;
        }

        //unlimited usage
        if (maxUseCount < 0) {
            return true;
        }

        //limit reached
        if (currentUseCount >= maxUseCount) {
            return false;
        }

        return true;
    }
}
