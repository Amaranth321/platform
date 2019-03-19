package models;

import java.util.Date;
import java.util.List;

import play.modules.morphia.Model;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;

/**
 * @author edward
 */

@Entity
public class WeeklySummary extends Model {

	public String bucketId;
	public String userId;
	public Date start;
	public Date end;
	public long totalPeopleIn;
	public long totalMales;
	public long totalFemales;
	@Embedded
	public List<RankedLabel> rankLabels;
	
	public WeeklySummary(String bucketId,
						 String userId,
						 Date start,
						 Date end,
						 long totalPeopleIn,
						 long totalMales,
						 long totalFemales,
						 List rankLabels) 
	{
		this.bucketId = bucketId;
		this.userId = userId;
		this.start = start;
		this.end = end;
		this.totalPeopleIn = totalPeopleIn;
		this.totalMales = totalMales;
		this.totalFemales = totalFemales;
		this.rankLabels = rankLabels;
	}
}
