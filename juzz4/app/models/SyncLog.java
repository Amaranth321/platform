package models;

import play.modules.morphia.Model;

import com.google.code.morphia.annotations.Entity;

@Entity
public class SyncLog extends Model {
	
	public String bucket;
	public String name;
	public String fileName;
	public String host;
	public int port;
	public long size;
	public long createdDate;

}
