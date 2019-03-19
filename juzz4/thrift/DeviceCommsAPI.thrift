/*
 * DeviceCommsAPI.thrift
 *
 * Copyright (C) KAI Square Pte Ltd
 */

/**
 * This Thrift IDL file defines the device communications API between device server
 * and all other servers. One service is defined:
 * (1) DeviceServerService.
 * When a device server starts up, it should register with the Arbiter Server
 * through ArbiterManagementService. Subsequently, registration should be done
 * periodically to inform Arbiter that it is still alive. For devices
 * that need to connect to the device server before communications with it can
 * take place, the device server should also register the device to Arbiter
 * through ArbiterManagementService as well. The Arbiter Server will then
 * communicate with the device server through DeviceServerService, including
 * starting and stopping of tasks. As data are generated through tasks and
 * other events, the device server sends them to the Arbiter Server through
 * ArbiterManagementService.
 */

namespace java com.kaisquare.device.thrift
namespace cpp com.kaisquare.device.thrift

/**
 * Device server interface.
 */
service DeviceServerService {
    /**
     * Returns the server ID of this device server.
     */
    i64 getServerID();

    /**
     * Returns the type for this device server.
     */
    string getServerType();
    
    /**
     * Set device server configurations from arbiter.
     * (1) configurations - Detailed configurations for device server, example of the json string:
     *                      {"cloud-server":"core.up.uat.kaisquare.com"}
     */
    bool setConfiguration(1: string configurations);

    /**
     * Get device server configurations.
     * (1) option - config option string.
     *     example option: "public-host", return: "210.23.28.67".
     */
    string getConfiguration(1: string option);

    /**
     * Adds a device to this device server.
     * (1) deviceId - Device ID as assigned by the Core Engine.
     * (2) deviceKey - Unique device key that identifies a device from a
     *     particular vendor, typically assigned by the vendor.
     * (3) supportedTaskTypes - Supported task types of the device.
     * (4) deviceInfo - Additional information associated with the device.
     */
    bool addDevice(1: i64 deviceId, 2: string deviceKey, 3: list<string> supportedTaskTypes, 4: string deviceInfo);

    /**
     * Removes a device from this device server.
     * (1) deviceId - Device ID as assigned by the Core Engine.
     */
    bool removeDevice(1: i64 deviceId);

    /**
     * Returns the devices in this device server.
     */
    list<i64> getDevices();

    /**
     * Returns the connected devices in this device server. If devices do
     * not need to connect to the device server, an empty list is returned.
     */
    list<i64> getConnectedDevices();

    /**
     * Returns whether a device is alive.
     */
    bool isAlive(1: i64 deviceId);

    /**
     * Keep a device to be alive.
     * It is used when device is controlled by arbiter not device server.
     * Such as kai node, kai one and so on.
     * (1) deviceId - Device ID as assigned by the Core Engine.
     * (2) channelMask - binary mask for channels
     * (3) ttl - Time to live, unit: seconds.
     */
    bool keepAlive(1: i64 deviceId, 2: i64 channelMask, 3: i32 ttl);

    /**
     * Starts a task.
     * (1) deviceId - Device ID.
     * (2) channel - Device channel.
     * (3) taskType - Task type.
     * (4) taskInfo - Additional information associated with the task.
     */
    bool startTask(1: i64 deviceId, 2: i32 channel, 3: string taskType, 4: string taskInfo);

    /**
     * Stops a task.
     * (1) deviceId - Device ID.
     * (2) channel - Device channel.
     * (3) taskType - Task type.
     * (4) taskInfo - Additional information associated with the task.
     */
    bool stopTask(1: i64 deviceId, 2: i32 channel, 3: string taskType, 4: string taskInfo);

    /**
     * Get device stream related profile parameters.
     * (1) deviceId - Device ID.
     * (2) channel - Device channel.
     * (3) streamType - Requested stream type, e.g. "rtsp/h264","http/mjpeg".
     * RETURN json formatted stream details, for example
     * {"video-codec":"h264", "video-payload-type":96, 
     *  "audio-codec":"aac", "audio-channels":1, "audio-frequency":9000, "audio-payload-type":97}
     */
    string getDeviceStreamProfile(1: i64 deviceId, 2: i32 channel, 3: string streamType);
 
    /**
     * Update a stream profile.
     * It is called when camera stream setting is updated.
     * (1) deviceId - Device ID.
     * (2) channel - Device Channel.
     * (3) streamType - Stream type.
     * (4) streamProfile - Detailed device stream related parameters.
     * RETURN true if the operation succeed, false if fail.
     */
    bool updateDeviceStreamProfile(1: i64 deviceId, 2: i32 channel, 3: string streamType, 4: string streamProfile);

    /**
     * Get snapshot from device.
     * (1) deviceId - Device ID.
     * (2) channel - Device channel.
     * (3) timeout - Time out.
     * RETURN snapshot buffer, or empty if failed.
     */
    binary getDeviceSnapshot(1: i64 deviceId, 2: i32 channel, 3: i32 timeout);

    /**
     * Set snapshot to node device server.
     * (1) deviceId - Device ID.
     * (2) channel - Device channel.
     * (3) snapshot - Snapshot buffer.
     * RETURN true if the operation succeed, false if fail.
     */
    bool setDeviceSnapshot(1: i64 deviceId, 2: i32 channel, 3: binary snapshot);

    /**
     * Get device server report for diagnosis.
     * (1) deviceId - Device ID.
     * If deviceId is 0, return the general report, 
     * else return the device specified report.
     * RETURN json formatted device server report.
     */
    string getDeviceServerReport(1: i64 deviceId);

    /**
     * The following are deprecated interfaces.
     * They are kept for the compatibility concern.
     */

    /**
     * For V4 or lower Nodes.
     * Send cmmand to device server.
     * The main differences with startTask: 
     * - designed for external services like node device server;
     * - the target of the command is server rather than device;
     * - return a json string as the command result.
     * (1) command - JSON formated command string.
     * return json formated result.
     */
    string sendCommand(1: string command); 
}
