package models.transients;

import lib.util.Util;
import platform.CryptoManager;

/**
 *
 * @author Nischal Objects of this class do not persist. This class is used merely to create inventory information.
 */
public class InventoryInfo {

    public Long inventoryId;
    public String registrationNumber;
    public String modelNumber;
    public String macAddress;
    public String username;
    private String password;
    public boolean activated;
    public String deviceName;
    public String bucketName;
    public String modelName;

    public InventoryInfo() {
        inventoryId = 0L;
        registrationNumber = "";
        modelNumber = "";
        macAddress = "";
        username = "";
        password = Util.sha256hash(Util.generatePassword());
        activated = false;
        deviceName = "";
        bucketName = "";
        modelName = "";
    }

    public String getPassword() {
        return CryptoManager.aesDecrypt(password);
    }

    public void setPassword(String password) {
        this.password = CryptoManager.aesEncrypt(password);
    }

    public void setPasswordInPlainText(String password) {
        this.password = password;
    }
}
