package models.coresimulator;

import com.google.code.morphia.annotations.Entity;
import play.Logger;
import play.modules.morphia.Model;

/**
 *
 * @author kdp
 * 
 */
@Entity
public class SimulatedDevice extends Model {

    public String	deviceId;

    public String 	name;
    public String   modelId;
    public String 	deviceKey;
    public String 	host;
    public String 	port;
	public String 	latitude;
	public String 	longitude;
    public String 	login;
    public String 	password;
    public String	address;
	public boolean  cloudRecordingEnabled;
	public String   status;

    public SimulatedDevice() {
    }

    @OnAdd void beforeAddNew(){
        Logger.info("About to create new device record in core engine simulator");
        deviceId = String.valueOf(SimulatedDevice.count() + 1);
    }
}
