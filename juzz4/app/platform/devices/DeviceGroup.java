package platform.devices;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lib.util.JsonReader;
import lib.util.exceptions.ApiException;
import models.MongoDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class exists to support any kind of arbitrary device groupings on the UI.
 * <p/>
 * Note: UI convention of device-channel pair on cloud is PlatformDeviceId of Node and CoreDeviceId of Camera,
 * where as the backend uses both coreDeviceIds (like vca reports).
 * parse function will handle the necessary conversion.
 *
 * @author Aye Maung
 * @since v4.3
 */
public class DeviceGroup
{
    private final String groupName;
    private final DeviceGroupType type;
    private final List<DeviceChannelPair> devicePairs;

    public static List<DeviceGroup> parseAsList(String jsonGroups) throws ApiException
    {
        List<Map<String, Object>> parsedList = new Gson().fromJson(jsonGroups, new TypeToken<List<Map>>()
        {
        }.getType());

        List<DeviceGroup> deviceGroups = new ArrayList<>();
        for (Map<String, Object> groupMap : parsedList)
        {
            String groupName = String.valueOf(groupMap.get("groupName"));

            DeviceGroupType type = DeviceGroupType.parse(String.valueOf(groupMap.get("type")));
            List<Map<String, String>> cameraListMap = (List<Map<String, String>>) groupMap.get("devicePairs");
            List<DeviceChannelPair> convertedList = new ArrayList<>();
            for (Map<String, String> cameraMap : cameraListMap)
            {
                //UI sends platform device Id, although DeviceGroup uses coreDeviceId
                //conversion is needed here
                JsonReader jsonReader = new JsonReader();
                jsonReader.load(cameraMap);

                String devicePlatformId = jsonReader.getAsString("coreDeviceId", null);
                MongoDevice dbDevice = MongoDevice.getByPlatformId(devicePlatformId);
                convertedList.add(new DeviceChannelPair(dbDevice.getCoreDeviceId(),
                                                        jsonReader.getAsString("channelId", null)));
            }

            deviceGroups.add(new DeviceGroup(groupName, type, convertedList));
        }

        return deviceGroups;
    }

    public DeviceGroup(String groupName,
                       DeviceGroupType type,
                       List<DeviceChannelPair> cameraList)
    {
        this.groupName = groupName;
        this.type = type;
        this.devicePairs = cameraList;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public DeviceGroupType getType()
    {
        return type;
    }

    public List<DeviceChannelPair> getCameraList()
    {
        return devicePairs == null ? new ArrayList<DeviceChannelPair>() : devicePairs;
    }
}
