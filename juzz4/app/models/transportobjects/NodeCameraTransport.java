package models.transportobjects;

import models.backwardcompatibility.DeviceModel;
import models.node.NodeCamera;
import models.node.NodeObject;
import platform.devices.DeviceStatus;

import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class NodeCameraTransport
{
    public final String name;
    public final String nodePlatformDeviceId;
    public final String nodeCoreDeviceId;
    public final DeviceModel model;
    public final String deviceKey;
    public final String host;
    public final String port;
    public final String login;
    public final String password;
    public final String address;
    public final String latitude;
    public final String longitude;
    public final boolean cloudRecordingEnabled;
    public final List<String> labels;
    public final DeviceStatus status;

    public NodeCameraTransport(NodeObject nodeObject, NodeCamera dbCamera)
    {
        this.name = dbCamera.name;
        this.nodePlatformDeviceId = dbCamera.nodePlatformDeviceId;
        this.nodeCoreDeviceId = dbCamera.nodeCoreDeviceId;
        this.model = dbCamera.model;
        this.deviceKey = dbCamera.deviceKey;
        this.host = dbCamera.host;
        this.port = dbCamera.port;
        this.login = dbCamera.login;
        this.password = dbCamera.password;
        this.address = dbCamera.address;
        this.latitude = dbCamera.latitude;
        this.longitude = dbCamera.longitude;
        this.cloudRecordingEnabled = dbCamera.cloudRecordingEnabled;
        this.labels = dbCamera.labels;
        this.status = dbCamera.getStatus();
    }
}