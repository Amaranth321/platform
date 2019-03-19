package models.access;

import java.util.Date;
import java.util.UUID;

import com.google.code.morphia.annotations.Entity;
import platform.PasswordResetManager;
import play.modules.morphia.Model;

@Entity
public class PasswordResetKey extends Model {
    public String resetKey;
    public Long userId;
    public Long bucketId;
    public Date expiryTime;

    public PasswordResetKey(Long uid, Long bid) {
        resetKey = UUID.randomUUID().toString();
        userId = uid;
        bucketId = bid;

        expiryTime = new Date();
        long ttlMillis = PasswordResetManager.RESET_KEY_TTL_HOURS * 60 * 60 * 1000L;
        expiryTime.setTime(expiryTime.getTime() + ttlMillis);
    }
}

