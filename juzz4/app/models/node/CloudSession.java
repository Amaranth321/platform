package models.node;

import com.google.code.morphia.annotations.Entity;
import play.modules.morphia.Model;

/**
 * OTP authentication works once only
 * So, session key is required to be saved for communicating with cloud
 *
 */
@Entity
public class CloudSession extends Model {
    public String otp;
    public String key;
    public String bucketName;
    public Long userId;
    public Long expiry;
}
