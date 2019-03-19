/*
 * PlatformServices.thrift
 *
 * Copyright (C) KAI Square Pte Ltd
 */

/**
 * This Thrift IDL file defines the services offered by Platform to the Core.
 * Platform is the thrift server for these services.
 *
 * Following services are defined:
 *
 * (1) EventService
 */

//Import shared files
include "EventStructures.thrift"


namespace java com.kaisquare.platform.thrift
namespace cpp com.kaisquare.platform.thrift

/**
 * PlatformException
 * (1) errorCode - Error code indicating the type of error
 * (2) description - Human readable description of what is the error
 */
exception PlatformException {
    1: i64 errorCode,
    2: string description
}

/**
 * EventService - this service provides API for event sources to push events to Platform.
 */
service EventService {

	/**
	 * Push an event to the Platform.
	 *
	 * (1) eventId - ID of the event
	 * (2) details - a structure with more details about this event
	 *
	 */
	bool pushEvent(1: string eventId,
                   2: EventStructures.EventDetails details) throws (1:PlatformException platformExp),

}

/**
 * NodeService - this service provides API for KAI Nodes to push information to Platform Cloud Server.
 */
service NodeService {

	/**
	 * Add camera.
	 *
	 * (1) nodeId - ID of the Node (platform side deviceId of the Node on Cloud server).
	 * (2) cameraName - Name of the camera
	 * (3) nodePlatformDeviceId - ID of the camera (platform side deviceID on the Node)
	 * (4) nodeCoreDeviceId - ID of the camera (core side deviceID on the Node)
	 * (5) channels - Number of channels that this camera supports
	 */
	bool addCamera( 1: string nodeId,
			2: string cameraName,
			3: string nodePlatformDeviceId,
			4: string nodeCoreDeviceId,
			5: i32 channels) throws (1:PlatformException platformExp),

	/**
	 * Update camera.
	 *
	 * (1) nodeId - ID of the Node (platform side deviceId of the Node on Cloud server).
	 * (2) cameraName - Name of the camera
	 * (3) nodePlatformDeviceId - ID of the camera (platform side deviceID on the Node)
	 * (4) nodeCoreDeviceId - ID of the camera (core side deviceID on the Node)
	 * (5) channels - Number of channels that this camera supports
	 */
	bool updateCamera( 1: string nodeId,
			2: string cameraName,
			3: string nodePlatformDeviceId,
			4: string nodeCoreDeviceId,
			5: i32 channels) throws (1:PlatformException platformExp),

	/**
	 * Remove camera.
	 *
	 * (1) nodeId - ID of the Node (platform side deviceId of the Node on Cloud server).
	 * (2) cameraId - ID of the camera (platform side deviceID on the Node)
	 */
	bool removeCamera(1: string nodeId, 2: string nodePlatformDeviceId) throws (1:PlatformException platformExp),

	/**
	 * Started a VCA on Node.
	 *
	 * (1) nodeId - ID of the Node (platform side deviceId of the Node on Cloud server).
	 * (2) nodePlatformDeviceId - ID of the camera (platform side deviceID on the Node)
	 * (3) channelNo - The channel number of camera on which VCA is started.
	 * (4) vcaType - Type of the VCA that is started.
	 */
	bool startVca(1: string nodeId,
			2: string nodePlatformDeviceId,
			3: i32 channelNo,
			4: string vcaType) throws (1:PlatformException platformExp),

	/**
	 * Stopped a VCA on Node.
	 *
	 * (1) nodeId - ID of the Node (platform side deviceId of the Node on Cloud server).
	 * (2) nodePlatformDeviceId - ID of the camera (platform side deviceID on the Node)
	 * (3) channelNo - The channel number of camera on which VCA is started.
	 * (4) vcaType - Type of the VCA that is started.
	 */
	bool stopVca(1: string nodeId,
			2: string nodePlatformDeviceId,
			3: i32 channelNo,
			4: string vcaType) throws (1:PlatformException platformExp),

}

