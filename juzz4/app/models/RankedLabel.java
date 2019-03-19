package models;

import com.google.code.morphia.annotations.Embedded;

/**
 * @author edward
 */

@Embedded
public class RankedLabel {
	
	public static final String STATUS_NEW = "new";
	public static final String STATUS_TOP = "top";
	public static final String STATUS_NORMAL = "normal";
	public static final String STATUS_DOWN = "down";

	public String rankStatus;
	public int rank;
	public String name;
	public long totalPeopleIn;
	public int growthRate;
	
	public RankedLabel(String name, long totalPeopleIn) {
		this.rankStatus = STATUS_NEW;
		this.rank = 0;
		this.name = name;
		this.totalPeopleIn = totalPeopleIn;
		this.growthRate = 0;
	}
}
