package models;

import java.util.HashSet;
import java.util.Set;

import lib.util.Util;
import play.modules.morphia.Model;

import com.google.code.morphia.annotations.Entity;

/**
 * Record holidays, e.g. National holiday or Special holiday or Significant holiday
 * 
 * @author edward.wu@kaisquare.com.tw
 */

@Entity
public class Holiday extends Model {

	private String title;
	private String description;
	private boolean isEvent;
	private boolean isHoliday;
	private boolean isSignificant;
	private long from;
	private long to;
	private Set<String> countries;
	
	public Holiday() {
		title = "";
		description = "";
		countries = new HashSet<>();
	}
	
	public Holiday(String title, boolean isEvent, boolean isHoliday, boolean isSignificant, 
				   long from, long to, Set<String> countries) {
		setTitle(title);
		setIsEvent(isEvent);
		setIsHoliday(isHoliday);
		setIsSignificant(isSignificant);
		setFrom(from);
		setTo(to);
		setCountries(countries);
	}
	
	public void setTitle(String title) {
		if (Util.isNullOrEmpty(title))
			throw new IllegalArgumentException("Invalid Title");
		
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setIsEvent(boolean isEvent) {
		this.isEvent = isEvent;
	}
	
	public boolean getIsEvent() {
		return isEvent;
	}
	
	public void setIsHoliday(boolean isHoliday) {
		this.isHoliday = isHoliday;
	}
	
	public boolean getIsHoliday() {
		return isHoliday;
	}
	
	public void setIsSignificant(boolean isSignificant) {
		this.isSignificant = isSignificant;
	}
	
	public boolean getIsSignificant() {
		return isSignificant;
	}
	
	public void setFrom(long from) {
		this.from = from;
	}
	
	public long getFrom() {
		return from;
	}
	
	public void setTo(long to) {
		if (to < from)
			throw new IllegalArgumentException("Invalid Dates");
		
		this.to = to;
	}
	
	public long getTo() {
		return to;
	}
	
	public void setCountries(Set<String> countries) {
		if (countries.size() == 0) 
			throw new IllegalArgumentException("Empty Country List");
			
		this.countries = countries;
	}
	
	public Set<String> getCountries() {
		return countries;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (null == obj) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final Holiday anoHoliday = (Holiday) obj;
		return title.equals(anoHoliday.title);
	}
	
}
