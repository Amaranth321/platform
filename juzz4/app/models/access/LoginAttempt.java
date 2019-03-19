package models.access;

import com.google.code.morphia.annotations.Entity;
import play.modules.morphia.Model;

@Entity
public class LoginAttempt extends Model {
    public String remoteIp;
    public Integer failCount;
    public Long lastTried;
    public boolean locked;

    public LoginAttempt() {
        failCount = 0;
        locked = false;
    }
}
