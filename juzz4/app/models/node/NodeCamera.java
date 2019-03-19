package models.node;

import models.backwardcompatibility.DeviceModel;
import models.MongoDevice;
import platform.devices.DeviceStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NodeCamera
{
    public String name;
    public String nodePlatformDeviceId;
    public String nodeCoreDeviceId;
    public DeviceModel model;
    public String deviceKey;
    public String host;
    public String port;
    public String login;
    public String password;
    public String address;
    public String latitude;
    public String longitude;
    public boolean cloudRecordingEnabled;
    public List<String> labels;

    private DeviceStatus status;

    public static Comparator<NodeCamera> sortByCoreId = new Comparator<NodeCamera>()
    {
        public int compare(NodeCamera cam1, NodeCamera cam2)
        {
            Long first = Long.parseLong(cam1.nodeCoreDeviceId);
            Long second = Long.parseLong(cam2.nodeCoreDeviceId);
            //ascending order
            return first.compareTo(second);
        }
    };

    public NodeCamera()
    {
        labels = new ArrayList<>();
    }

    public MongoDevice updateAndConvert(MongoDevice deviceToUpdate)
    {
        deviceToUpdate.setName(this.name);
        deviceToUpdate.setModelId(this.model.getId().toString());
        deviceToUpdate.setCoreDeviceId(this.nodeCoreDeviceId);
        deviceToUpdate.setDeviceKey(this.deviceKey);
        deviceToUpdate.setHost(this.host);
        deviceToUpdate.setPort(this.port);
        deviceToUpdate.setLogin(this.login);
        deviceToUpdate.setPassword(this.password);
        deviceToUpdate.setAddress(this.address);
        deviceToUpdate.setLatitude(Double.parseDouble(this.latitude));
        deviceToUpdate.setLongitude(Double.parseDouble(this.longitude));
        deviceToUpdate.setCloudRecordingEnabled(this.cloudRecordingEnabled);
        deviceToUpdate.setStatus(getStatus());

        //labels needs to be assigned using setDeviceLabels
        //not here

        return deviceToUpdate;
    }

    @Override
    public String toString()
    {
        return String.format("[%s, %s, nodePlatformDeviceId:%s, nodeCoreDeviceId:%s, deviceKey:%s, %s:%s, %s]",
                name,
                model.name,
                nodePlatformDeviceId,
                nodeCoreDeviceId,
                deviceKey,
                host,
                port,
                address);
    }

    @Override
    public boolean equals(Object other)
    {
        if (other instanceof NodeCamera)
        {
            NodeCamera ncOther = (NodeCamera) other;
            if (this.nodePlatformDeviceId.equals(ncOther.nodePlatformDeviceId) &&
                this.nodeCoreDeviceId.equals(ncOther.nodeCoreDeviceId))
            {
                return true;
            }
        }

        return false;
    }

    public void setStatus(DeviceStatus status)
    {
        this.status = status;
    }

    public DeviceStatus getStatus()
    {
        return status == null ? DeviceStatus.UNKNOWN : status;
    }
}
