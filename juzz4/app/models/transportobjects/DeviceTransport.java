package models.transportobjects;

import lib.util.exceptions.ApiException;
import models.MongoDevice;
import models.MongoDeviceModel;
import models.labels.DeviceLabel;
import models.node.NodeCamera;
import models.node.NodeObject;
import platform.devices.DeviceChannelPair;
import platform.devices.DeviceStatus;
import platform.label.LabelManager;

import java.util.*;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class DeviceTransport
{
    public final long id;
    public final String name;
    public final String deviceId;
    public final String deviceKey;
    public final String host;
    public final String port;
    public final String login;
    public final String password;
    public final String address;
    public final double latitude;
    public final double longitude;
    public final boolean cloudRecordingEnabled;
    public final DeviceStatus status;
    public final Map model;
    public final NodeObjectTransport node;
    public final List<Map<String, Object>> channelLabels;

    //backward compatibility (< v4.5)
    public final Set<String> cameraLabels = new LinkedHashSet<>();
    public final Set<String> label = new LinkedHashSet<>();
    public final String joinedlabels = "";

    public DeviceTransport(MongoDevice dbDevice) throws ApiException
    {
        MongoDeviceModel deviceModel = MongoDeviceModel.getByModelId(dbDevice.getModelId());
        //model
        Map model = new LinkedHashMap();
        model.put("modelId", dbDevice.getModelId());
        model.put("name", deviceModel.getName());
        model.put("channels", deviceModel.getChannels());
        model.put("capabilities", deviceModel.getCapabilities());

        //node
        List<DeviceChannelPair> cameraList = new ArrayList<>();
        NodeObject nodeObj = null;
        if (dbDevice.isKaiNode())
        {
            nodeObj = NodeObject.findByPlatformId(dbDevice.getDeviceId());
            for (NodeCamera camera : nodeObj.getCameras())
            {
                cameraList.add(new DeviceChannelPair(dbDevice.getCoreDeviceId(), camera.nodeCoreDeviceId));
            }
        }
        else
        {
            //platform does not support cameras with multiple channels
            cameraList.add(new DeviceChannelPair(dbDevice.getCoreDeviceId(), "0"));
        }

        List<Map<String, Object>> channelLabelList = new ArrayList<>();
        for (DeviceChannelPair camera : cameraList)
        {
            List<DeviceLabel> dvcLblList = LabelManager.getInstance().getLabelsOf(camera);
            if (dvcLblList.isEmpty())
            {
                continue;
            }
            Map labelMap = new LinkedHashMap();
            labelMap.put("channelId", camera.getChannelId());
            List<String> labelIdList = new ArrayList<>();
            for (DeviceLabel deviceLabel : dvcLblList)
            {
                labelIdList.add(deviceLabel.getLabelId());
                label.add(deviceLabel.getLabelName());
            }
            labelMap.put("labels", labelIdList);
            channelLabelList.add(labelMap);
        }

        this.id = Long.parseLong(dbDevice.getDeviceId());
        this.name = dbDevice.getName();
        this.deviceId = dbDevice.getCoreDeviceId();
        this.deviceKey = dbDevice.getDeviceKey();
        this.host = dbDevice.getHost();
        this.port = dbDevice.getPort();
        this.login = dbDevice.getLogin();
        this.password = dbDevice.getPassword();
        this.address = dbDevice.getAddress();
        this.latitude = dbDevice.getLatitude();
        this.longitude = dbDevice.getLongitude();
        this.cloudRecordingEnabled = dbDevice.isCloudRecordingEnabled();
        this.status = dbDevice.getStatus();
        this.model = model;
        this.node = nodeObj == null ? null : new NodeObjectTransport(nodeObj);
        this.channelLabels = channelLabelList;
    }
}
