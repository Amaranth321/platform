var KupEvent = {};

KupEvent.ALL = "all";
KupEvent.ALL_SECURITY = "all-security";

KupEvent.INTRUSION = "event-vca-intrusion";
KupEvent.PERIMETER_DEFENSE = "event-vca-perimeter";
KupEvent.LOITERING = "event-vca-loitering";
KupEvent.PEOPLE_COUNTING = "event-vca-people-counting";
KupEvent.PASSERBY = "event-vca-passerby";
KupEvent.OBJECT_COUNTING = "event-vca-object-counting";
KupEvent.VIDEO_BLUR = "event-vca-video-blur";
KupEvent.FACE_INDEXING = "event-vca-face";
KupEvent.TRAFFIC_FLOW = "event-vca-traffic";
KupEvent.CROWD_DETECTION = "event-vca-crowd";
KupEvent.PROFILING = "event-vca-audienceprofiling";
KupEvent.VCA_ERROR = "event-vca-internal-error";

KupEvent.POOR_CONNECTION = "event-connection-poor";
KupEvent.DEVICE_CONNECTED = "event-connected";
KupEvent.DEVICE_DISCONNECTED = "event-connection-lost";
KupEvent.UNRECOGNIZED_NODE = "event-node-unregistered";
KupEvent.NODE_REGISTERED = "event-node-registered";
KupEvent.NODE_UPSTREAM_FAILED = "event-upstream-failed";
KupEvent.RECORDING_STARTED = "event-storage-started";
KupEvent.RECORDING_STOPPED = "event-storage-stopped";
KupEvent.RECORDING_DISK_FULL = "event-disk-full";

KupEvent.SPEEDING = "event-veh-speeding";
KupEvent.IDLING = "event-veh-idle";
KupEvent.SUDDEN_ACCELERATION = "event-veh-sudden-acceleration";
KupEvent.SUDDEN_BRAKING = "event-veh-sudden-braking";
KupEvent.SUDDEN_LEFT = "event-veh-sudden-left";
KupEvent.SUDDEN_RIGHT = "event-veh-sudden-right";
KupEvent.SUDDEN_UP = "event-veh-sudden-up";
KupEvent.SUDDEN_DOWN = "event-veh-sudden-down";

KupEvent.PIR = "event-passive-infrared";
KupEvent.AUDIO = "event-audio";
KupEvent.INTERCOM_ALERT = "event-intercom-visitor-alert";

KupEvent._FOR_USERS = [
	KupEvent.ALL_SECURITY,
	KupEvent.INTRUSION,
	KupEvent.PERIMETER_DEFENSE,
	KupEvent.LOITERING,
	KupEvent.OBJECT_COUNTING,
	KupEvent.VIDEO_BLUR
];