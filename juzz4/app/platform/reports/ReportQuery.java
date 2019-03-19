package platform.reports;

import java.util.Date;
import java.util.List;

import platform.devices.DeviceChannelPair;

import com.google.code.morphia.query.Query;
import com.google.gson.JsonSerializer;

/**
 * The ReportQuery is a builder of query for report, each reports may need their own parameters to query 
 * or generate different results, and also be able to let JSON render handle proper JSON output by using {@link ReportQuery#getJsonSerializers()}
 * to get serializers for the output
 * @param <T>
 */
public interface ReportQuery<T> {
	
	/**
	 * Add a device and channel pair for the query condition
	 * @param pair the value that is specified the particular deviceId and the channelId of the device
	 */
	public ReportQuery<T> addDevice(DeviceChannelPair pair);
	
	/**
	 * Add list of device and channel pair for the query condition
	 * @param pairs the value that is specified the particular deviceId and the channelId of the device
	 */
	public ReportQuery<T> addDevice(List<DeviceChannelPair> pairs);
	
	/**
	 * Set query period from
	 * @param from
	 */
	public ReportQuery<T> setDateFrom(Date from);
	
	/**
	 * Set query period end
	 * @param to
	 */
	public ReportQuery<T> setDateTo(Date to);
	
	/**
	 * Set parameters needed by the report
	 * @param name parameter name
	 * @param value parameter value
	 */
	public ReportQuery<T> setParameter(String name, Object value);
	
	/**
	 * Get database query object
	 * @return morphia query constructed with conditions by this {@link ReportQuery}
	 */
	public Query<T> getQuery();
	
	/**
	 * Get JSON serializers
	 * @return
	 */
	public JsonSerializer[] getJsonSerializers();

}
