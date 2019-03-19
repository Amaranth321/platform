package platform.reports;

import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.Query;
import com.google.gson.JsonSerializer;
import platform.devices.DeviceChannelPair;

import java.util.*;

class DefaultReportQuery<T> implements ReportQuery<T> {

	protected Map<String, Object> parameters = new HashMap<String, Object>();
	protected Query<T> query;
	protected List<JsonSerializer> serializers = new ArrayList<JsonSerializer>();
	protected List<DeviceChannelPair> devicePairs = new ArrayList<DeviceChannelPair>();
	protected Date from;
	protected Date to;

	public DefaultReportQuery(Query<T> query) {
		this.query = query;
	}
	
	public ReportQuery<T> addJsonSerializer(JsonSerializer serializer)
	{
		if (!serializers.contains(serializer))
			serializers.add(serializer);
		
		return this;
	}

	@Override
	public ReportQuery<T> addDevice(DeviceChannelPair pair) {
		devicePairs.add(pair);
		return this;
	}
	
	@Override
	public ReportQuery<T> addDevice(List<DeviceChannelPair> pairs) {
		devicePairs.addAll(pairs);
		return this;
	}

	@Override
	public ReportQuery<T> setDateFrom(Date from) {
		this.from = from;
		return this;
	}

	@Override
	public ReportQuery<T> setDateTo(Date to) {
		this.to = to;
		return this;
	}

	@Override
	public ReportQuery<T> setParameter(String name, Object value) {
		parameters.put(name, value);
		return this;
	}

	@Override
	public Query<T> getQuery() {
		if (!devicePairs.isEmpty())
		{
			ArrayList<Criteria> criteria = new ArrayList<Criteria>();
			for (DeviceChannelPair pair : devicePairs)
			{
				String channelId = pair.getChannelId();
				if (channelId != null && !"".equals(channelId))
				{
					criteria.add(query.and(
							query.criteria("deviceId").equal(pair.getCoreDeviceId()),
							query.criteria("channelId").equal(pair.getChannelId())));
				}
				else
					criteria.add(query.criteria("deviceId").equal(pair.getCoreDeviceId()));
			}
			if (!criteria.isEmpty())
				query.or(criteria.toArray(new Criteria[0]));
		}
		if (from != null)
			query.and(query.criteria("time").greaterThanOrEq(from.getTime()));
		if (to != null)
			query.and(query.criteria("time").lessThan(to.getTime()));

		return query;
	}

	@Override
	public JsonSerializer[] getJsonSerializers() {
		return serializers.toArray(new JsonSerializer[0]);
	}
	
	protected Map<String, Object> getParameters()
	{
		return parameters;
	}
}
