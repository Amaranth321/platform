package models;

import java.util.ArrayList;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;

import play.modules.morphia.Model;

@Entity
public class SystemPreference extends Model {
	
	public static final String LAST_PROCESSED_EVENT_ID = "last_processed_event_id";
	public static final String SUPPORTED_VCA_TYPES = "supported_vca_types";
	
	@Indexed
	private String name;
	private String value;
	
	public static void setPreference(String name, String value)
	{
		Datastore ds = SystemPreference.ds();
		Query<SystemPreference> query = ds.createQuery(SystemPreference.class);
		query.field("name").equal(name);
		UpdateOperations<SystemPreference> ops = ds.createUpdateOperations(SystemPreference.class);
		ops.set("value", value);
		ds.findAndModify(query, ops, false, true);
	}

	public static String getPreference(String name)
	{
		String value = null;
		SystemPreference pref = SystemPreference.find("name", name).get();
		if (pref != null)
			value = pref.value;
		
		return value;
	}
	
	public static void removePreference(String name)
	{
		Datastore ds = SystemPreference.ds();
		Query<SystemPreference> query = ds.createQuery(SystemPreference.class);
		query.field("name").equal(name);
		ds.delete(query);
	}
}
