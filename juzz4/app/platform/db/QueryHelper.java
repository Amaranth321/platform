package platform.db;

import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.abstracts.ServerPagedResult;
import platform.Environment;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.devices.DeviceChannelPair;
import platform.time.UtcPeriod;
import play.Logger;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.3
 */
public final class QueryHelper
{
    private QueryHelper()
    {
    }

    public static <T extends Model> ServerPagedResult<T> preparePagedResult(Model.MorphiaQuery query,
                                                                            int offset,
                                                                            int take)
    {
        //get total
        ServerPagedResult<T> pagedResult = new ServerPagedResult<>();
        pagedResult.setTotalCount(query.count());

        //filter
        if (offset > 0)
        {
            query.offset(offset);
        }
        if (take > 0)
        {
            query.limit(take);
        }
        List forOnePage = query.asList();
        pagedResult.setResultsForOnePage(forOnePage);

        return pagedResult;
    }

    /**
     * This function uses _created field to check expiry
     *
     * @param days
     * @param query
     */
    public static void removeOlderThan(int days, Model.MorphiaQuery query)
    {
        removeOlderThan(days, query, "_created");
    }

    /**
     * @param days
     * @param query
     * @param timeMillisField millisecond field of the model to check expiry
     */
    public static void removeOlderThan(int days, Model.MorphiaQuery query, String timeMillisField)
    {
        long count = getEntriesOlderThan(days, query, timeMillisField).delete();
        //Logger.info("[%s] removed %s entries older than % days", query.first().getClass().getSimpleName(), count, days);
    }

    /**
     * @param days
     * @param query
     */
    public static Model.MorphiaQuery getEntriesOlderThan(int days, Model.MorphiaQuery query)
    {
        return getEntriesOlderThan(days, query, "_created");
    }

    /**
     * @param days
     * @param query
     * @param timeMillisField millisecond time field of the model to check expiry
     */
    public static Model.MorphiaQuery getEntriesOlderThan(int days, Model.MorphiaQuery query, String timeMillisField)
    {
        long daysMillis = days * 24 * 60 * 60 * 1000L;
        long now = Environment.getInstance().getCurrentUTCTimeMillis();
        query = query.filter(timeMillisField + " <", now - daysMillis);
        return query;
    }

    /**
     * This is a helper function for deviceId/channelId inputs received from API request.
     * Use this only in the API controllers.
     * <p/>
     * Non-empty deviceIds and empty channelIds are accepted, but not the other way.
     *
     * @param platformDeviceIds comma-separated String of platform device Ids
     * @param channelIds        comma-separated String of channel Ids
     */
    public static List<DeviceChannelPair> asCameraList(String platformDeviceIds, String channelIds) throws ApiException
    {

        //check empty strings
        String[] platformDeviceIdArr = Util.isNullOrEmpty(platformDeviceIds) ?
                                       new String[0] :
                                       platformDeviceIds.split(",");
        String[] channelIdArr = Util.isNullOrEmpty(channelIds) ? new String[0] : channelIds.split(",");

        //check empty entries
        for (String id : platformDeviceIdArr)
        {
            if (id.isEmpty())
            {
                throw new ApiException("empty-device-id");
            }
        }
        for (String id : channelIdArr)
        {
            if (id.isEmpty())
            {
                throw new ApiException("empty-channel-id");
            }
        }

        //if channel ids are provided, they must tally with device ids
        if (channelIdArr.length > 0 && (platformDeviceIdArr.length != channelIdArr.length))
        {
            throw new ApiException("devices-channels-do-not-tally");
        }

        //compile camera list
        List<DeviceChannelPair> cameraList = new ArrayList<>();

        //use inputs
        if (platformDeviceIdArr.length != 0)
        {
            if (channelIdArr.length == 0)
            {
                channelIdArr = new String[platformDeviceIdArr.length];
                Arrays.fill(channelIdArr, "");
            }

            for (int i = 0; i < platformDeviceIdArr.length; i++)
            {
                //need core device id to query events
                String platformDeviceId = platformDeviceIdArr[i];
                CachedDevice cachedDevice = CacheClient.getInstance().getDeviceByPlatformId(platformDeviceId);
                if (cachedDevice == null)
                {
                    throw new ApiException("invalid-platform-device-id");
                }
                cameraList.add(new DeviceChannelPair(cachedDevice.getCoreDeviceId(), channelIdArr[i]));
            }
        }

        return cameraList;
    }

    /**
     * This function cannot be used if the time fields are not in milliseconds
     *
     * @param query        Morphia query. This will be modified by this function, hence no return value
     * @param dbFromField  period start field of db object
     * @param dbToField    period end field of db object
     * @param searchPeriod period to check overlap against
     */
    public static void mustOverlap(Model.MorphiaQuery query, String dbFromField, String dbToField, UtcPeriod searchPeriod)
    {
        // db period must cover part of the search period
        // dbTo < searchFrom || searchTo < dbFrom     => no overlap
        // dbTo >= searchFrom && searchTo >= dbFrom   => overlaps
        long searchFrom = searchPeriod.getFromMillis();
        long searchTo = searchPeriod.getToMillis();
        query.filter(dbToField + " >=", searchFrom).filter(dbFromField + " <", searchTo);
    }
}
