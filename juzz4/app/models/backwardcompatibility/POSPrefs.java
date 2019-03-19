package models.backwardcompatibility;


import com.google.code.morphia.annotations.Entity;

import play.modules.morphia.Model;


@Entity
@Deprecated
public class POSPrefs extends Model {
	
	public boolean importPOSHourly;
	public boolean importPOSDaily;
	public boolean importPOSWeekly;
	public boolean importPOSMonthly;
    public String ftpServerName;
    public String port;
    public String ftpUserName;
    public String ftpPassword;
    public String ftpFolder;
    public boolean deleteAfterImport;
    
    public Long userId;
    public Long bucketId;

	
	
}
