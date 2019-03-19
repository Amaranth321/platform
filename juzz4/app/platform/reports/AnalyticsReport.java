package platform.reports;

import models.archived.ArchivedEvent;

import java.util.Date;

/**
 * The interface that defines methods to process event data.
 */
public interface AnalyticsReport<T> {
	
	/**
	 * To process this event for the report data
	 * @param event the new coming event data
	 * 
	 * @return true if the event has been processed properly, false otherwise
	 */
	boolean process(ArchivedEvent event);
	
	/**
	 * Get a query from the implementation of {@link AnalyticsReport} object 
	 * that already filtered the following parameters as condition
	 * @param from retrieve the report from the specified date (UTC)
	 * @param to retrieve the report end of the specified date (UTC)
	 * @return {@link ReportQuery} object that already filtered to the specific parameters
	 */
	ReportQuery<T> query(Date from, Date to);
	
	/**
	 * remove not retention report data by "data-retention" value in config.json
	 * @param from The date that the data would be kept from
	 */
	void retention(Date from);
	
	/**
	 * Check if the report data exists
	 * @return true if there're report data for the specific event
	 */
	boolean reportExists();

	/**
	 * Clear all report data relating this analytics report
	 * @return true on success, false otherwise
	 */
	void clear();
}
