package platform.rt;

import com.google.gson.Gson;
import platform.devices.DeviceChannelPair;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class VcaChangeFeed implements RTFeed
{
    private final String instanceId;
    private final DeviceChannelPair camera;

    public VcaChangeFeed(String instanceId, DeviceChannelPair camera)
    {
        this.instanceId = instanceId;
        this.camera = camera;
    }

    @Override
    public String json()
    {
        return new Gson().toJson(this);
    }

    @Override
    public Map toAPIOutput()
    {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("instanceId", instanceId);
        return output;
    }
}
