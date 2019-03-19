package models.transients;

import models.node.NodeObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kdp
 *         <p/>
 *         Objects of this class do not persist. This class is used merely to create transport objects.
 */
@Deprecated
public class DeviceInfo
{

    public class DeviceModelInfo
    {
        public Long modelId;
        public String name;
        public Integer channels;
        public String capabilities;
        public Long id;
    }

    public static class UserId
    {
        public Long id;

        public UserId()
        {

        }
    }

    public String name;
    public String deviceId;
    public DeviceModelInfo model;
    public String deviceKey;
    public String host;
    public String port;
    public String login;
    public String password;
    public String address;
    public String latitude;
    public String longitude;
    public List<String> channelLabels;
    public boolean cloudRecordingEnabled;
    public Long bucket;
    public List<UserId> users;
    public Long id;
    public NodeObject node;
    public String status;

    public DeviceInfo()
    {
        model = new DeviceModelInfo();
        users = new ArrayList<UserId>();
        channelLabels = new ArrayList<String>();
    }
}
