package models.licensing;

import com.google.code.morphia.annotations.Entity;
import play.modules.morphia.Model;

/**
 * License information before the license is suspended.
 *
 * @author Aye Maung
 */
@Entity
public class SuspendedLicenseRecord extends Model {
    private final NodeLicenseInfo licenseInfo;

    public SuspendedLicenseRecord(NodeLicenseInfo licenseInfo) {
        this.licenseInfo = licenseInfo;
    }

    public NodeLicenseInfo getLicenseInfo() {
        return licenseInfo;
    }
}
