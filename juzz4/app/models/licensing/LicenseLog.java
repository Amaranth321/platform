package models.licensing;

import com.google.code.morphia.annotations.Entity;
import platform.Environment;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;

@Entity
public class LicenseLog extends Model {
    public Long time;
    public String licenseNumber;
    public String username;
    public String remoteIp;
    public List<String> changes;

    public LicenseLog() {
        time = Environment.getInstance().getCurrentUTCTimeMillis();
        changes = new ArrayList<>();
    }
}
