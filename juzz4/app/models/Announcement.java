package models;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.PostLoad;
import play.modules.morphia.Model;

@Entity
public class Announcement extends Model{
	public String description;
	public String type;
	public String domain;

    // Added field "domain". Set to "" if doesn't exist (older data)
    @PostLoad void postLoad() {
        if(domain == null) {
            domain = "";
        }
    }
}

