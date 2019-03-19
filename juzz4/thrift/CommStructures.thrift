/*
 * CommStructures.thrift
 *
 * Copyright (C) KAI Square Pte Ltd
 */

/**
 * This Thrift IDL file defines the common structures. This file is included
 * from other Thrift IDL files which use these common structures.
 */

namespace java com.kaisquare.core.thrift
namespace cpp com.kaisquare.core.thrift

/**
 * Stream File Details
 * (1) serverId - Stream file located server/group.
 * (2) from - Begin timestamp of this video/image (ddMMyyyyHHmmss format).
 * (3) to - End timestamp of this video/image (ddMMyyyyHHmmss format).
 * (4) fileSize - The size of this video/image. uint: B.
 * (5) url - The url/path of the video/image.
 * (6) createdTime - The time media file created, on KUP it is the upload time.
 * (7) progress - The video/image upload progress, units : percent, from "0" to "100".
 * (8) status - The upload status. Valid values are:
 *             "unrequested" means video/image has not been requested to upload by Platform.
 *             "requested" means Platform requested to upload video/image.
 *             "uploading" means video/image is being uploaded.
 *             "completed" means video/image upload completed.
 *             "retrying" means upload error, but Core is retrying again.
 *             "aborted" means upload error, and Core gives up the uploading.
 *             "stopped" means upload has been canceled by Platform, Core stops uploading.
 */
struct StreamFileDetails {
    1: string serverId,
    2: string from,
    3: string to,
    4: string fileSize,
    5: string url,
    6: string createdTime,
    7: string progress,
    8: string status
}

/**
 * Stream Client Info
 * (1) clientIp - IP address of the client connected to this stream
 * (2) clientPort - Port number of the client connected to this stream
 */
struct StreamClientInfo {
    1: string clientIp,
    2: string clientPort
}

/**
 * Stream Information
 * (1) deviceId - The device ID
 * (2) channelId - The channel ID
 * (3) isLiveview - Indicates liveview or playback
 * (4) type - The stream type, e.g. "http/jpeg" or "rtmp/h264"
 * (5) url - The stream URL as visible to outside world
 * (6) startTime - The timestamp when stream was started (ddMMyyyyHHmmss format)
 * (7) clients - List of clients connected to this stream
 */
struct StreamInfo {
    1: string deviceId,
    2: string channelId,
    3: bool isLiveview,
    4: string type,
    5: string url,
    6: string startTime,
    7: list<StreamClientInfo> clients
}

/**
 * Recording Server Storage Status
 * (1) serverId - The recording server ID
 * (2) serverHost - The recording server host
 * (3) streamCount - The number of streams that server is processing
 * (4) freeSpace - The available disk space in this server. uint: MB
 * (5) totalSpace - The total disk space in this server. uint: MB
 */
struct StorageInfo {
    1: string serverId,
    2: string serverHost,
    3: i64 streamCount,
    4: i64 freeSpace,
    5: i64 totalSpace
}
