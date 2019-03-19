/*
 * EventStructures.thrift
 *
 * Copyright (C) KAI Square Pte Ltd
 */

/**
 * This Thrift IDL file defines the event structures. This file is included
 * from other Thrift IDL files which use event structures.
 */

namespace java com.kaisquare.events.thrift
namespace cpp com.kaisquare.events.thrift

/**
 * Detailed Event Information
 * (1) id - Unique identifier of this event
 * (2) data - Additional details about the event; JSON formatted string.
 *            Format depends on event type.
 *            e.g. if event type is PERIMETER_BREACH, info might
 *            be coordinates; if event type is SPEED_LIMIT_BREACH,
 *            info might be the speed and location.
 * (3) type - the type of event
 * (4) time - the timestamp in "dd/MM/yyyy hh:mm:ss" format
 * (5) deviceId - the device from which this event comes
 * (6) channelId - the channel of device from which this event comes
 * (7) binaryData - the binary data associated with this device (blob), e.g. a snapshot from camera that detected motion
 */
struct EventDetails {
    1: string id,
    2: string data,
    3: string type,
    4: string time,
    5: string deviceId,
    6: string channelId,
    7: binary binaryData
}

