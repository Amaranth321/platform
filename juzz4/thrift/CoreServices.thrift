/*
 * CoreServices.thrift
 *
 * Copyright (C) KAI Square Pte Ltd
 */

/**
 * This Thrift IDL file defines the services offered by Core to the Platform.
 * Core is the thrift server for these services.
 *
 * Following services are defined:
 *
 * (1) StreamControlService
 * (2) DataService
 * (3) DeviceManagementService
 * (4) DeviceControlService
 * (5) IndoorLocationService
 * (6) ConfigControlService
 */

//Import shared files
include "EventStructures.thrift"
include "CommStructures.thrift"

namespace java com.kaisquare.core.thrift
namespace cpp com.kaisquare.core.thrift

/**
 * CoreException
 * (1) errorCode - Error code indicating the type of error
 * (2) description - Human readable description of what is the error
 */
exception CoreException {
    1: i64 errorCode,
    2: string description
}

/**
 * Recorded Media Information
 * (1) deviceId - The device ID
 * (2) channelId - The channel ID
 * (3) from - The timestamp when media was started (ddMMyyyyHHmmss format)
 * (4) to - The timestamp when media was stoped (ddMMyyyyHHmmss format)
 * (5) path - The absolute path of the media file.
 */
struct RecordedMediaInfo {
    1: string deviceId,
    2: string channelId,
    3: string from,
    4: string to,
    5: string path
}

/**
 * StreamControlService - this service provides interface for
 * management (control) of a streaming sessions. Platform uses
 * this interface to begin/end streaming.
 * NOTE 1: This is only for control, not for actual video data transfer.
 */
service StreamControlService {

    /**
     * Begin a new media session. This is a request from Platform to Core
     * Engine. On success, Core Engine should return the dynamically generated
     * URL of stream.
     *
     * (1) sessionId - A handle for this session for future reference.
     * (2) ttl - UTC time stamp when this session becomes invalid, unless
     *           renewed by keepStreamSessionAlive() defined below.
     * (3) type - the type of stream requested by client. Valid values are:
     *             "http/mjpeg" for M-JPEG video over HTTP protocol
     *             "http/h264" for H.264 video over HTTP protocol
     *             "rtsp/h264" for H.264 video over RTSP protocol
     *             "rtmp/h264" for H.264 video over RTMP protocol
     *             "http/jpeg" for JPEG snapshots over HTTP protocol
     * (4) allowedClientIpAddresses - list of IP address from which the Core
     *      may accept connections for streaming of this URL.
     * (5) deviceId - Unique ID of the device whose stream is requested.
     * (6) channelId - The requested Channel number.
     * (7) startTimestamp - start date/time from where the stream should begin,
     *      or empty string "" if LIVE stream is requested (ddMMyyyyHHmmss format).
     * (8) endTimestamp - end date/time of the stream, or empty string ""
     *      if LIVE stream is requested (ddMMyyyyHHmmss format).
     *
     * RETURN value is a list of the dynamically generated URLs. It is the playlist that contains
     * all required streams, to be played in that order. For example, if backend recording system
     * records 15 minutes of stream to a file, and the requested duration is of 1 hour, the playlist
     * should ideally contain 4 stream URLs, to be played in order. Each with 15 minutes of video.
     * This allows for efficient stream management, and also makes seeking easier.
     */
    list<string> beginStreamSession(1: string sessionId,
                             2: i64 ttl,
                             3: string type,
                             4: list<string> allowedClientIpAddresses,
                             5: string deviceId,
                             6: string channelId,
                             7: string startTimestamp,
                             8: string endTimestamp) throws (1:CoreException coreExp),

    /**
     * Renew an existing session. UP will use this API to update session keys
     * which are about to expire and to inform RS about change of client IP
     * address.
     * (1) sessionId - A handle for this session for future reference.
     * (2) ttl - UTC time stamp when this session becomes invalid, unless
     *              renewed again
     * (3) allowedClientIpAddresses - Updated list of allowed client IP
     *              addresses. If any connections exist from IP addresses not
     *              in this list, they should be terminated.
     */
    bool keepStreamSessionAlive(1: string sessionId,
                               2: i64 ttl,
                               3: list<string> allowedClientIpAddresses) throws (1:CoreException coreExp),

    /**
     * Mark the end of a stream session.
     *
     * Terminate all existing connections from clients for this session's URL,
     * and destroy session information.
     * (1) sessionId - Server ID.
     */
     bool endStreamSession(1: string sessionId) throws (1:CoreException coreExp),

    /**
     * Returns list of active outbound streams.
     */
     list<CommStructures.StreamInfo> getActiveOutboundStreamList() throws (1:CoreException coreExp),
     
    /**
     * Returns list of available recorded media.
     *
     * (1) deviceId - Device ID.
     * (2) channelId - Channel ID.  
     * (3) mediaType - Media type. Valid values are: "video" "image".
     * (4) startTimestamp - Start date/time from where the media should begin (ddMMyyyyHHmmss format).
     * (5) endTimestamp - End date/time from where the media should end (ddMMyyyyHHmmss format).
     */
     list<RecordedMediaInfo> getRecordedMediaList(1: string deviceId,
                             2: string channelId,
                             3: string mediaType,
                             4: string startTimestamp,
                             5: string endTimestamp) throws (1:CoreException coreExp),

    /**
     * Returns list of recording server storage status.
     */
     list<CommStructures.StorageInfo> getStorageStatus() throws (1:CoreException coreExp),
     
    /**
     * Request video/image from Core for playback.
     * On success, Core will begin to upload video/image from remote "Device".
     *
     * (1) sessionId - A handle for this session for future reference. 
     *                 For each request, sessionId should be different.
     * (2) deviceId - Device ID.
     * (3) channelId - Normal Device's channel ID or Node Device's deviceId on this Node.     
     * (4) mediaType - Media type. Valid values are: "video" "image".
     * (5) startTimestamp - Start date/time from where the stream should begin (ddMMyyyyHHmmss format).
     * (6) endTimestamp - End date/time from where the stream should end (ddMMyyyyHHmmss format).
     *
     * Core should returns TRUE if no error or exception occurs.
     */
     bool requestStreamForPlayback(1: string sessionId,
                                  2: string deviceId,
                                  3: string channelId,
                                  4: string mediaType,
                                  5: string startTimestamp,
                                  6: string endTimestamp) throws (1:CoreException coreExp),
     
    /**
     * Cancel video/image uploading.
     * On success, Core will stop uploading and delete the uploaded files.
     *
     * (1) deviceId - Device ID.
     * (2) channelId - Device Channel.
     * (3) mediaType - Media type. Valid values are: "video" "image".
     * (4) fileTime - List of file time (StreamFileDetails.from) that will be canceled for uploading.
     *
     * Core should returns TRUE if no error or exception occurs.
     */
     bool cancelStreamForPlayback(1: string deviceId,
                                  2: string channelId,
                                  3: string mediaType,
                                  4: list<string> fileTime) throws (1:CoreException coreExp),

    /**
     * Get stream file's details. Such as file size, create time and uploading progress. 
     *
     * (1) sessionId - A handle for this session for future reference (not used currently). 
     * (2) deviceId - Device ID.
     * (3) channelId - Normal Device's channel ID or Node Device's deviceId on this Node.     
     * (4) mediaType - Media type. Valid values are: "video" "image".
     * (5) startTimestamp - Start date/time from where the stream should begin (ddMMyyyyHHmmss format).
     * (6) endTimestamp - End date/time from where the stream should end (ddMMyyyyHHmmss format).    
     *
     * Returns list of stream file details.
     */
     list<CommStructures.StreamFileDetails> getStreamFileDetails(1: string sessionId, 
                                            2: string deviceId,
                                            3: string channelId,
                                            4: string mediaType,
                                            5: string startTimestamp,
                                            6: string endTimestamp) throws (1:CoreException coreExp),    
     /**
     * Reset media files.
     * On success, Core will delete the corresponding media files.
     * 
     * (1) deviceId - Device ID.
     * (2) channel - Device channel.
     * RETURN true if the operation succeed, false if fail.
     */
     bool resetMediaFiles(1: i64 deviceId, 2: i32 channel);
}

/**
 * Location Data
 * (1) time - the timestamp in DDMMYYYYhhmmss format
 * (2) latitude - the latitude
 * (3) longitude - the longitude
 * (4) direction - the current direction of movement, in degrees * 100
 * (5) speed - the current speed, in km/h
 */
struct LocationDataPoint {
    1: string time,
    2: double latitude,
    3: double longitude,
    4: i64 direction,
    5: double speed
}


/**
 * GSensor Data
 * (1) time - the timestamp in DDMMYYYYhhmmss format
 * (2) x - the G value in X direction
 * (3) y - the G value in Y direction
 * (4) z - the G value in Z direction
 */
struct GSensorDataPoint {
    1: string time,
    2: double x,
    3: double y,
    4: double z
}


/**
 * DataService - this service provides various data retrieval API.
 */
service DataService {

     /**
      * Retrieve GPS location data.
      *
      * (1) deviceId - ID of the device whose GPS data is requested
      * (2) startTimestamp - start time, ddMMyyyyHHmmss
      * (3) endTimeStamp - end time, ddMMyyyyHHmmss
      *
      * Note: Set start and end timestamps to "" or null to get last known location.
      */
     list<LocationDataPoint> getGPSData(1: string deviceId,
                                       2: string startTimestamp,
                                       3: string endTimestamp) throws (1:CoreException coreExp),

     /**
      * Retrieve events that occured for specified device within the
      * given time frame.
      *
      * (1) deviceId - ID of the device whose GPS data is requested
      * (2) startTimestamp - start time, ddMMyyyyHHmmss
      * (3) endTimeStamp - end time, ddMMyyyyHHmmss
      * (4) type - the type of events to be fetched
      *
      */
     list<EventStructures.EventDetails> getEvents(1: string deviceId,
                    2: string startTimestamp,
                    3: string endTimestamp,
                    4: string type) throws (1:CoreException coreExp),

     /**
      * Retrieve GSensor data.
      *
      * (1) deviceId - ID of the device whose GSensor data is requested
      * (2) startTimestamp - start time, ddMMyyyyHHmmss
      * (3) endTimeStamp - end time, ddMMyyyyHHmmss
      *
      * Note: Set start and end timestamps to "" or null to get last known gsensor data.
      */
     list<GSensorDataPoint> getGSensorData(1: string deviceId,
                                       2: string startTimestamp,
                                       3: string endTimestamp) throws (1:CoreException coreExp),

}


/**
 * Device Model Structure - a structure of this type represents a model
 * of devices in the system.
 *
 * Most of the fields in this API are mapped to corresponding fields in
 * Core Engine's existing database. RMS+ can safely ignore the fields that
 * it does not require.
 *
 * (1) id - Unique identifier of this model
 * (2) createdAt - Not used
 * (3) createdBy - Not used
 * (4) modifiedAt - Not used
 * (5) modifiedBy - Not used
 * (6) name - Model name
 * (7) channels - Number of channels that the devices of this model have
 * (8) data - Not used
 * (9) action - Not used
 * (10) misc - JSON formatted additional miscellaneous information, e.g.
 *   {
 *     "servertype": "http-camera",
 *     "supportedtasktypes": ["live-image"],
 *     "device_url": "http://#{device:host}:#{device:misc:port}/",
 *     "onboard_recording_server": "http://#{device:host}:#{device:misc:port}/setup/recording.html"
 *   }
 * (11) liveview - Boolean flag, TRUE if this device supports live view, FALSE otherwise.
 *   {
 *     "0": {
 *       "activex": {
 *         "objectcode": "<object …>…</object>",
 *         "ptz": {
 *           "left": "…",
 *           "right": "…",
 *           "up": "…",
 *           "down": "…",
 *           "in": "…",
 *           "out": "…",
 *           "home": "…"
 *         }
 *       },
 *       "mjpeg": {
 *         "objectcode": "<applet …>…</applet>"
 *       }
 *     }
 *   }
 */
struct DeviceModel {
    1: string id,
    2: string createdAt,
    3: string createdBy,
    4: string modifiedAt,
    5: string modifiedBy,
    6: string name,
    7: string channels,
    8: string data,
    9: string action,
    10: string misc,
    11: string liveview
}

/**
 * Detailed Device Information
 * Most of the fields in this API are mapped to corresponding fields in
 * Core Engine's existing database. RMS+ can safely ignore the fields that
 * it does not require.
 *
 * (1) id - Unique identifier of this device
 * (2) name - Device Name
 * (3) key - MAC address of the device in the common notational format, e.g. 01:23:45:67:89:ab
 * (4) host - Device IP address or hostname
 * (5) port - Device port.
 * (6) login - Device login (user name). Ignore if not required.
 * (7) password - Device password. Ignore if not required.
 * (8) address - The default physical address of the location where the device is installed (postal address).
 * (9) lat - The default Latitude
 * (10) lng - The default Longitude
 * (11) accountId - ID of account this device is associated with.
 * (12) modelId - Model of this device
 * (13) statusId - "pending" or "active". Default status is "pending" until
 *                it is activated by the activateDevice() function.
 * (14) functionalityId - Not used
 * (15) alertFlag - Not used
 * (16) alive - Not used
 * (17) currentPositionId - Not used
 * (18) action - Not used
 * (19) eventSettings - Not used
 * (20) deviceServerUrls - Not used
 * (21) liveview - Not used
 * (22) snapshotRecordingEnabled - "true" to enable snapshot recording, "false" to disable
 * (23) snapshotRecordingInterval - Save the snapshot for this device every "snapshotRecordingInterval" seconds. Unit is "second".
 * (24) cloudRecordingEnabled - "true" to enable recording of device in cloud, "false" to disable
 */
struct DeviceDetails {
    1: string id,
    2: string name,
    3: string key,
    4: string host,
    5: string port,
    6: string login,
    7: string password,
    8: string address,
    9: string lat,
    10: string lng,
    11: string accountId,
    12: string modelId,
    13: string statusId,
    14: string functionalityId,
    15: string alertFlag,
    16: string alive,
    17: string currentPositionId,
    18: string action,
    19: string eventSettings,
    20: string deviceServerUrls,
    21: string liveview,
    22: string snapshotRecordingEnabled,
    23: string snapshotRecordingInterval,
    24: string cloudRecordingEnabled
}

/**
 * DeviceManagementService - this service provides API for management of devices
 */
service DeviceManagementService {

     /**
      * Add a new device model to the system. This function will be typically called
      * from an administrative user interface. This happens when a new model of
      * devices is about to be introduced.
      *
      * Model should be added to database and model ID should be assigned. This
      * device is not yet "activated". When a user buys this device, they will log
      * in to the system and activate this device.
      *
      * (1) model - The model object/structure with the modelId field empty. The
      *              modelId is generated by Core Engine/RMS+.
      *
      * Returns the "model ID" generated by Core Engine/RMS+.
      */
     string addModel(1: DeviceModel model) throws (1:CoreException coreExp),

     /**
      * Update model.
      *
      * (1) model - The model object/structure with a valid modelId. The
      *              corresponding model gets updated in database.
      *
      * Returns TRUE on success, FALSE otherwise.
      */
     bool updateModel(1: DeviceModel model) throws (1:CoreException coreExp),

     /**
      * Delete model.
      *
      * (1) modelId - ID of the model to be deleted.
      *
      * Returns TRUE on success, FALSE otherwise.
      */
     bool deleteModel(1: string modelId) throws (1:CoreException coreExp),

     /**
      * Get list of models.
      *
      * Returns a list of all models in the system.
      */
     list<DeviceModel> listModels() throws (1:CoreException coreExp),

     /**
      * Add a new device to the system. This function will be typically called
      * from an administrative user interface. This happens when a new batch of
      * devices is about to be shipped out to market.
      *
      * Device should be added to database and device ID should be assigned. This
      * device is not yet "activated". When a user buys this device, they will log
      * in to the system and activate this device.
      *
      * If the 'key' field contains MAC address and modelId contains a valid model ID,
      * rest of the fields can be empty and backend should accept this. In this situation
      * the backend should set rest of the fields to their default values. The defaults
      * could be different for different device models.
      *
      * (1) device - The device object/structure with the deviceId field empty. The
      *              deviceId is generated by Core Engine/RMS+.
      *
      * Returns the "device ID" generated by Core Engine/RMS+.
      */
     string addDevice(1: DeviceDetails device) throws (1:CoreException coreExp),

     /**
      * Update device.
      *
      * (1) device - The device object/structure with a valid deviceId. The
      *              corresponding device gets updated in database.
      *
      * Returns TRUE on success, FALSE otherwise.
      */
     bool updateDevice(1: DeviceDetails device) throws (1:CoreException coreExp),

     /**
      * Delete device.
      *
      * (1) deviceId - ID of the device to be deleted.
      *
      * Returns TRUE on success, FALSE otherwise.
      */
     bool deleteDevice(1: string deviceId) throws (1:CoreException coreExp),

     /**
      * Get list of devices.
      *
      * (1) filter - "all" for all devices, "pending" for devices which are
      *              not yet activated, "active" for all active devices.
      *
      * Returns a list of devices in the system.
      */
     list<DeviceDetails> listDevices(1: string filter) throws (1:CoreException coreExp),

     /**
      * Get details of a device.
      *
      * (1) deviceId - ID of the device.
      *
      * Returns details of the specified device.
      */
     DeviceDetails getDevice(1: string deviceId) throws (1:CoreException coreExp),

     /**
      * Activate device. This function will typically be called from user
      * account interface.
      *
      * (1) device - The device to be activated.
      *
      * Returns TRUE on success, FALSE otherwise.
      */
     bool activateDevice(1: DeviceDetails device) throws (1:CoreException coreExp),
}

/**
 * DeviceControlService - this service provides API for interactive control of devices
 */
service DeviceControlService {

     /**
      * Gets a device's current status.
      *
      * (1) deviceId - ID of the device.
      *
      * Returns the device's status:
      * "online" if the device is currently connected and able to communicate with the backend (Core Engine/RMS+).
      * "offline" if the device is not connected to the backend.
      * "error" if the device is connected but in an error state.
      * "incorrect-password" if the backend is able to connect to the device, but not log in due to invalid login credentials.
      */
     string getDeviceStatus(1: string deviceId) throws (1:CoreException coreExp),

     /**
      * Gets the current status of an I/O pin. This is applicable only to devices which
      * have ON/OFF type I/O pins.
      *
      * (1) deviceId - ID of the device.
      * (2) ioNumber - The digital I/O number of the device, starting with 0.
      *
      * Returns the result of the operation. 
      * "on" if the pin status is ON or HIGH
      * "off" if the pin status is OFF or LOW
      * "error" on failure to read pin status. There could be several reasons of failure e.g. device is offline or device doesn't
      * have the specified I/O control.
      */
     string getGPIO(1: string deviceId, 2: string ioNumber) throws (1:CoreException coreExp),

     /**
      * Sets an I/O control pin ON or OFF. This is applicable only to devices which
      * have ON/OFF type digital I/O control pins.
      *
      * (1) deviceId - ID of the device.
      * (2) ioNumber - The digital I/O port identifier.
      * (3) value - The new value to set - "on" means ON/HIGH; "off" means OFF/LOW.
      *
      * Returns the result of the operation. 
      * "ok" on successful completion of the operation.
      * "error" on failure. There could be several reasons of failure e.g. device is offline or device doesn't
      * have the specified I/O control.
      */
     string setGPIO(1: string deviceId, 2: string ioNumber, 3: string value) throws (1:CoreException coreExp),

     /**
      * Starts to pan a PTZ device in the specified direction.
      *
      * (1) deviceId - ID of the device.
      * (2) channelId - channel of the device.
      * (3) direction - The direction of panning: "left" or "right".
      *
      * Returns the result of the operation. 
      * "ok" on successful completion of the operation.
      * "error" on failure. There could be several reasons of failure e.g. device is offline or device doesn't
      * have Pan feature.
      */
     string startPanDevice(1: string deviceId, 2: string channelId, 3: string direction) throws (1:CoreException coreExp),

     /**
      * Stops panning of a PTZ device.
      *
      * (1) deviceId - ID of the device.
      * (2) channelId - channel of the device.
      *
      * Returns the result of the operation. 
      * "ok" on successful completion of the operation.
      * "error" on failure. There could be several reasons of failure e.g. device is offline or device doesn't
      * have Pan feature.
      */
     string stopPanDevice(1: string deviceId, 2: string channelId) throws (1:CoreException coreExp),

     /**
      * Starts to tilt a PTZ device in the specified direction.
      *
      * (1) deviceId - ID of the device.
      * (2) channelId - channel of the device.
      * (3) direction - The direction of panning: "left" or "right".
      *
      * Returns the result of the operation. 
      * "ok" on successful completion of the operation.
      * "error" on failure. There could be several reasons of failure e.g. device is offline or device doesn't
      * have Tilt feature.
      */
     string startTiltDevice(1: string deviceId, 2: string channelId, 3: string direction) throws (1:CoreException coreExp),

     /**
      * Stops tilting of a PTZ device.
      *
      * (1) deviceId - ID of the device.
      * (2) channelId - channel of the device.
      *
      * Returns the result of the operation. 
      * "ok" on successful completion of the operation.
      * "error" on failure. There could be several reasons of failure e.g. device is offline or device doesn't
      * have Tilt feature.
      */
     string stopTiltDevice(1: string deviceId, 2: string channelId) throws (1:CoreException coreExp),

     /**
      * Starts to zoom a PTZ device in the specified direction.
      *
      * (1) deviceId - ID of the device.
      * (2) channelId - channel of the device.
      * (3) direction - The direction of panning: "in" or "out".
      *
      * Returns the result of the operation. 
      * "ok" on successful completion of the operation.
      * "error" on failure. There could be several reasons of failure e.g. device is offline or device doesn't
      * have Zoom feature.
      */
     string startZoomDevice(1: string deviceId, 2: string channelId, 3: string direction) throws (1:CoreException coreExp),

     /**
      * Stops zooming of a PTZ device.
      *
      * (1) deviceId - ID of the device.
      * (2) channelId - channel of the device.
      *
      * Returns the result of the operation. 
      * "ok" on successful completion of the operation.
      * "error" on failure. There could be several reasons of failure e.g. device is offline or device doesn't
      * have Zoom feature.
      */
     string stopZoomDevice(1: string deviceId, 2: string channelId) throws (1:CoreException coreExp),

     /**
      * Writes data to a data port of the specified device.
      *
      * (1) deviceId - ID of the device.
      * (2) portNumber - The data port number.
      * (3) data - The data to be written out.
      *
      * Returns the result of the operation. 
      * "ok" on successful completion of the operation.
      * "error" on failure. There could be several reasons of failure e.g. device is offline or device doesn't
      * have the specified I/O control.
      */
     string writeData(1: string deviceId, 2: string portNumber, 3: list<byte> data) throws (1:CoreException coreExp),

     /**
      * Reads data from a data port of the specified device.
      *
      * (1) deviceId - ID of the device.
      * (2) portNumber - The data port number.
      *
      * Returns the data read from the device's data port.
      */
     list<byte> readData(1: string deviceId, 2: string portNumber) throws (1:CoreException coreExp),

}

/**
 * Tag information
 * (1) tagId - ID of the tag
 * (2) macAddress - MAC address of the tag in the common notational format, e.g. 01:23:45:67:89:ab
 */
struct TagInfo {
     1: string tagId,
     2: string macAddress
}

/**
 * Indoor location
 * (1) mapId - ID of the indoor map on which this location is present
 * (2) x - x coordinate on the map
 * (3) y - y coordinate on the map
 * (4) timestamp - timestamp of this location, in DDMMYYYYhhmmss format
 */
struct IndoorLocationInfo {
     1: string mapId,
     2: string x,
     3: string y,
     4: string timestamp
}

/**
 * Indoor map information
 * (1) mapId - ID of the indoor map
 * (2) map - Map file content (binary data)
 */
struct IndoorMapInfo {
     1: string mapId,
     2: binary mapBlob
}

/**
 * IndoorLocationService - Indoor location tracking service.
 */
service IndoorLocationService {
     /**
      * Get list of all maps associated with a controller.
      * (1) deviceId - Device ID of the controller.
      * Returns maps in system.
      */
     list<IndoorMapInfo> getMaps(1: string deviceId) throws (1:CoreException coreExp),

     /**
      * Get list of all tags associated with a controller.
      * (1) deviceId - Device ID of the controller.
      * Returns tags in system.
      */
     list<TagInfo> getTags(1: string deviceId) throws (1:CoreException coreExp),

     /**
      * Retrives list of locations of a tag during a specified time frame.
      *
      * (1) deviceId - Device ID of the controller
      * (2) tagId - ID of the tag to locate
      * (3) startTimestamp - start time, ddMMyyyyHHmmss format
      * (4) endTimestamp - end time, ddMMyyyyHHmmss format
      *
      * Note: set startTimestamp and endTimestamp to null or "" to get last known location
      */
     list<IndoorLocationInfo> getTagLocation(1: string deviceId,
                              2: string tagId,
                              3: string startTimestamp,
                              4: string endTimestamp) throws (1:CoreException coreExp),
}

/**
 * ConfigControlService - this service provides interface for management 
 * of configurations. Platform use this interface to set parameters for 
 * core engine.
 */
service ConfigControlService {
     /**
      * Set device storage limit size for each channel.
      * (1) deviceId - Unique ID of the device.
      * (2) channelId - The specified channel number.
      * (3) storageLimit - Max storage size of this stream, default value is 102400. unit: MB.
      */
     bool setStreamStorageLimit(1: string deviceId, 2: string channelId, 3: i64 storageLimit) throws (1:CoreException coreExp),

     /**
      * Set device storage keep time for each channel.
      * (1) deviceId - Unique ID of the device.
      * (2) channelId - The specified channel number.
      * (3) keepDays - Storage keep days of this stream, default value is 30. unit: Day.
      */
     bool setStorageKeepDays(1: string deviceId, 2: string channelId, 3: i64 keepDays) throws (1:CoreException coreExp),

     /**
      * Set the length of segment file for stream storage.
      * (1) chunkSize - Segment file duration, default value is 15. unit: Minute.
      */
     bool setChunkSize(1: i64 chunkSize) throws (1:CoreException coreExp),

     /**
      * Set reserved disk space for storage.
      * (1) reservedSpace - Reserved disk space for storage, default value is 5120. unit: MB.
      */
     bool setReservedSpace(1: i64 reservedSpace) throws (1:CoreException coreExp),

     /**
      * Set cloud server for node.
      * Cloud server here indicates kup core engine arbiter server.
      * NOTE 1: For node only.
      * (1) cloudServerHost - IP address or hostname of cloud arbiter server, default is "localhost".
      */
     bool setCloudServer(1: string cloudServerHost) throws (1:CoreException coreExp),
}
