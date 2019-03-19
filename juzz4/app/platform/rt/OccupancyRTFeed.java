package platform.rt;

import com.google.gson.Gson;
import platform.data.collective.OccupancyData;
import platform.devices.DeviceChannelPair;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class OccupancyRTFeed implements RTFeed
{
    private final String labelId;
    private final Map<DeviceChannelPair, OccupancyData.ExpirableOccupancy> occupancyMap;

    public OccupancyRTFeed(OccupancyData occupancyData)
    {
        this.labelId = occupancyData.getLabelId();
        this.occupancyMap = occupancyData.getOccupancyMap();
    }

    @Override
    public String json()
    {
        return new Gson().toJson(this);
    }

    @Override
    public Map toAPIOutput()
    {
        Map output = new LinkedHashMap();
        output.put("labelId", labelId);
        output.put("occupancyMap", occupancyMap);
        return output;
    }

}
