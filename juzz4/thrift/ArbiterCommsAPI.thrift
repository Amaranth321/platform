/*
 * ArbiterCommsAPI.thrift
 *
 * Copyright (C) KAI Square Pte Ltd
 */

/**
 * This Thrift IDL file defines the communications API between Arbiter Server and 
 * all other servers. One service is defined:
 * (1) ArbiterManagementService.
 */

namespace java com.kaisquare.arbiter.thrift
namespace cpp com.kaisquare.arbiter.thrift

/**
 * Server status.
 * (1) serverHost - Server host.
 * (2) serverPort - Server port.
 * (3) cpuUsage - Cpu usage percent, 0-100.
 * (4) memUsage - Mem usage percent, 0-100.
 * (5) availDiskSpace - Available disk space, unit: MB.
 * (6) totalDiskSpace - Total disk space, unit: MB.
 * (7) activeStreams - Number of streams processing.
 * (8) maxStreams - Number of streams can process.
 */
struct ServerDetails {
  1: string serverHost;
  2: i32 serverPort;
  3: i32 cpuUsage;
  4: i32 memUsage;
  5: i32 availDiskSpace;
  6: i32 totalDiskSpace;
  7: i32 activeStreams;
  8: i32 maxStreams;
}

/**
 * Arbiter management communication interface.
 */
service ArbiterManagementService {
  /**
   * Register server to Arbiter Server.
   * (1) serverId - Server ID.
   * (2) serverType - Server type.
   * (3) serverDetails - Detailed information of the server.
   * RETURN true if the operation succeed, false if fail.
   */
  bool registerServer(1: i64 serverId, 
                      2: string serverType, 
                      3: ServerDetails serverDetails);

  /**
   * Deregister server from Arbiter Server.
   * (1) serverId - Server ID.
   * (2) serverType - Server type.
   * RETURN true if the operation succeed, false if fail.
   */
  bool deregisterServer(1: i64 serverId, 2: string serverType);
  
  /**
   * Sends data generated by a task to Arbiter Server.
   * (1) deviceId - Device ID.
   * (2) channel - Device channel.
   * (3) taskType - Task type.
   * (4) stringData - Json formatted string data.
   * (5) binaryData - Binary data.
   * RETURN true if the operation succeed, false if fail.
   */
  bool sendTaskData(1: i64 deviceId, 
                    2: i32 channel,
                    3: string taskType, 
                    4: string stringData, 
                    5: binary binaryData);

  /**
   * Sends data generated by an event to Arbiter Server.
   * (1) deviceId - Device ID.
   * (2) channel - Device channel.
   * (3) eventType - Type of the event.
   * (4) eventTime - Time of the event.
   * (5) stringData - Json formatted string data.
   * (6) binaryData - Binary data.
   * RETURN true if the operation succeed, false if fail.
   */
  bool sendEventData(1: i64 deviceId, 
                     2: i32 channel, 
                     3: string eventType, 
                     4: i64 eventTime, 
                     5: string stringData, 
                     6: binary binaryData);

  /**
   * The following are deprecated interfaces.
   * They are kept for the compatibility concern.
   */

  /**
   * For V4 or lower Nodes.
   * Get device server information from arbiter server.
   * (1) deviceInfo - device information.
   * Return the detailed server information.
   * input:  {"device-key":"50:e5:49:b4:e3:21"}
   * return: {"device-id":170, server-host":"10.101.10.210", "server-port":10790}
   * or
   * input:  {"device-id":170}
   * return:
   * {"device-key":"50:e5:49:b4:e3:21:", server-host":"10.101.10.210", "server-port":10790}
   */
  string getDeviceServer(1: string deviceInfo);
}
