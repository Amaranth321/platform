package controllers.api;

import com.google.gson.Gson;
import com.kaisquare.core.thrift.DeviceDetails;
import controllers.interceptors.APIInterceptor;
import lib.util.ResultMap;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InternalException;
import models.*;
import models.abstracts.ServerPagedResult;
import models.backwardcompatibility.DeviceModel;
import models.backwardcompatibility.InventoryItem;
import models.node.NodeCamera;
import models.node.NodeObject;
import models.transients.InvalidInventory;
import models.transients.InventoryInfo;
import models.transportobjects.DeviceTransport;
import models.transportobjects.NodeCameraTransport;
import models.transportobjects.NodeObjectTransport;
import models.transportobjects.VcaTransport;
import org.apache.commons.lang.StringUtils;
import platform.CryptoManager;
import platform.DeviceManager;
import platform.Environment;
import platform.InventoryManager;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaManager;
import platform.config.readers.ConfigsCloud;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedNodeCamera;
import platform.devices.DeviceChannelPair;
import platform.devices.DeviceDiscovery.DiscoveredDevice;
import platform.devices.DeviceDiscoveryStarter;
import platform.devices.DeviceLog;
import platform.node.NodeManager;
import play.Logger;
import play.mvc.With;

import java.io.File;
import java.util.*;

import static lib.util.Util.isNullOrEmpty;

/**
 * @author KAI Square
 * @sectiontitle Device Management
 * @sectiondesc APIs for device CURD operation, device user access management
 * @publicapi
 */

@With(APIInterceptor.class)
public class DeviceProvisioning extends APIController {

	/**
	 * @param device-name
	 *            The name of device. Mandatory
	 * @param model-id
	 *            The ID of device model. Mandatory
	 * @param device-key
	 *            The device key
	 * @param device-host
	 *            The device host name or IP address
	 * @param device-port
	 *            The device port number
	 * @param device-login
	 *            Username to access this device
	 * @param device-password
	 *            Password to access this device
	 * @param device-address
	 *            Address where this device is located. Mandatory
	 * @param device-latitude
	 *            Latitude of device's location
	 * @param device-longitude
	 *            Longitude of device's location
	 * @param cloud-recording-enabled
	 *            Enable or disable cloud recording (true/false). Mandatory
	 *
	 * @servtitle Add a new device to own bucket
	 * @httpmethod POST
	 * @uri /api/{bucket}/adddevicetobucket
	 * @responsejson { 'result': "ok", 'id': Platform side ID of the device e.g 5,
	 *               'device-id': Backend side ID of the device e.g 10 }
	 * @responsejson { "result": "error", "reason": "This registration number has
	 *               already been used" }
	 * @responsejson { "result": "error", "reason": "This device key has already
	 *               been used in inventory" }
	 * @responsejson { "result": "error", "reason": "Device not found on server" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void adddevicetobucket(String bucket) throws ApiException {
		try {
			String deviceName = readApiParameter("device-name", true);
			String modelId = readApiParameter("model-id", false);
			String deviceKey = readApiParameter("device-key", false);
			String deviceHost = readApiParameter("device-host", false);
			String devicePort = readApiParameter("device-port", false);
			String deviceLogin = readApiParameter("device-login", false);
			String devicePassword = readApiParameter("device-password", false);
			String deviceAddress = readApiParameter("device-address", true);
			String deviceLatitude = readApiParameter("device-latitude", false);
			String deviceLongitude = readApiParameter("device-longitude", false);
			String cloudRecordingEnabled = readApiParameter("cloud-recording-enabled", true);

			String bucketId = getCallerBucketId();

			if (Environment.getInstance().onCloud()) {
				if (!ConfigsCloud.getInstance().allowAddDeviceOnCloud()) {
					throw new ApiException("error-add-device-cloud");
				}
			}

			if (Environment.getInstance().onKaiNode()) {
				int totalDevices = DeviceManager.getInstance().getDevicesOfBucket(bucketId).size();
				if (totalDevices >= NodeManager.getInstance().getCameraLimit()) {
					throw new ApiException("msg-device-limit-reach");
				}
			}

			if (StringUtils.isBlank(deviceHost) || StringUtils.isBlank(devicePort)) {
				throw new ApiException("host-or-port-missing");
			}

			if (StringUtils.isBlank(modelId)) {
				throw new ApiException("device-model-required");
			}

			// validate model
			MongoDeviceModel deviceModel = MongoDeviceModel.getByModelId(modelId);
			if (deviceModel == null) {
				throw new ApiException("invalid-model-id");
			}
			if (deviceModel.isKaiNode()) {
				throw new ApiException("adding-node-device-not-allowed");
			}

			Logger.info("Device name: " + deviceName);
			Logger.info("Device key: " + deviceKey);
			Logger.info("Device host: " + deviceHost);
			Logger.info("Device port: " + devicePort);
			Logger.info("Device login: " + deviceLogin);
			Logger.info("Device address: " + deviceAddress);
			Logger.info("Device latitude: " + deviceLatitude);
			Logger.info("Device longitude: " + deviceLongitude);
			Logger.info("Device cloud recording enabled: " + cloudRecordingEnabled);

			DeviceDetails deviceDetails = new DeviceDetails();
			deviceDetails.setName(deviceName);
			deviceDetails.setModelId(modelId);
			deviceDetails.setKey(deviceKey);
			deviceDetails.setHost(deviceHost);
			deviceDetails.setLat(deviceLatitude);
			deviceDetails.setLng(deviceLongitude);
			deviceDetails.setPort(devicePort);
			deviceDetails.setLogin(deviceLogin);
			deviceDetails.setPassword(devicePassword);
			deviceDetails.setAddress(deviceAddress);
			deviceDetails.setCloudRecordingEnabled(cloudRecordingEnabled);

			deviceDetails.setSnapshotRecordingEnabled("true");
			deviceDetails.setSnapshotRecordingInterval("600");

			DeviceManager deviceManager = DeviceManager.getInstance();

			// check existing devices with the same host + port + deviceKey
			List<MongoDevice> bucketDevices = deviceManager.getDevicesOfBucket(bucketId);
			for (MongoDevice bktDvc : bucketDevices) {
				if (bktDvc.getHost().equals(deviceHost) && bktDvc.getPort().equals(devicePort)
						&& bktDvc.getDeviceKey().equals(deviceKey)) {
					throw new ApiException("device-info-already-exists");
				}
			}

			MongoDevice newDevice = deviceManager.addDeviceToBucket(bucketId, deviceDetails);
			Map map = new ResultMap();
			if (newDevice != null) {
				map.put("result", "ok");
				map.put("id", newDevice.getDeviceId());
				map.put("device-id", newDevice.getCoreDeviceId());
			} else {
				map.put("result", "error");
				map.put("reason", "no-device-on-server");
			}
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param device-id
	 *            Platform side ID of the device
	 * @param device-name
	 *            The name of device
	 * @param model-id
	 *            The ID of device model
	 * @param device-key
	 *            The device key
	 * @param device-host
	 *            The device host name or IP address
	 * @param device-port
	 *            The device port number
	 * @param device-login
	 *            Username to access this device
	 * @param device-password
	 *            Password to access this device
	 * @param device-address
	 *            Address where this device is located
	 * @param device-latitude
	 *            Latitude of device's location
	 * @param device-longitude
	 *            Longitude of device's location
	 * @param cloud-recording-enabled
	 *            Enable or disable cloud recording (true/false)
	 *
	 * @servtitle Upadate existing device of own bucket
	 * @httpmethod POST
	 * @uri /api/{bucket}/updatedevice
	 * @responsejson { 'result': "ok" }
	 * @responsejson { "result": "error", "reason": "Invalid device ID" }
	 * @responsejson { "result": "error", "reason": "This device key has already
	 *               been used in inventory" }
	 * @responsejson { "result": "error", "reason": "Device not found on server" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void updatedevice(String bucket) throws ApiException {
		try {
			String deviceId = readApiParameter("device-id", true);
			String deviceName = readApiParameter("device-name", true);
			String modelId = readApiParameter("model-id", true);
			String deviceKey = readApiParameter("device-key", false);
			String deviceHost = readApiParameter("device-host", false);
			String devicePort = readApiParameter("device-port", false);
			String deviceLogin = readApiParameter("device-login", false);
			String devicePassword = readApiParameter("device-password", false);
			String deviceAddress = readApiParameter("device-address", true);
			String deviceLatitude = readApiParameter("device-latitude", false);
			String deviceLongitude = readApiParameter("device-longitude", false);
			String cloudRecordingEnabled = readApiParameter("cloud-recording-enabled", true);

			Logger.info("Device ID: " + deviceId);
			Logger.info("Device name: " + deviceName);
			Logger.info("Device modelId: " + modelId);
			Logger.info("Device key: " + deviceKey);
			Logger.info("Device host: " + deviceHost);
			Logger.info("Device port: " + devicePort);
			Logger.info("Device login: " + deviceLogin);
			Logger.info("Device latitude: " + deviceLatitude);
			Logger.info("Device longitude: " + deviceLongitude);
			Logger.info("Device address: " + deviceAddress);
			Logger.info("Device cloud recording enabled: " + cloudRecordingEnabled);

			MongoDevice platformDevice = MongoDevice.getByPlatformId(deviceId);
			if (platformDevice == null) {
				throw new ApiException("invalid-device-id");
			}

			String bucketId = getCallerBucketId();
			if (!platformDevice.getBucketId().equals(bucketId)) {
				throw new ApiException("device-doesnot-belong-bucket");
			}

			// validate model
			MongoDeviceModel deviceModel = MongoDeviceModel.getByModelId(modelId);
			if (deviceModel == null) {
				throw new ApiException("invalid-model-id");
			}
			if (!platformDevice.getModelId().equals(modelId) && deviceModel.isKaiNode()) {
				throw new ApiException("switching-node-model-not-allowed");
			}

			if (platformDevice.isKaiNode()) {
				// mac address is not allowed to be changed for nodes
				if (!platformDevice.getDeviceKey().equalsIgnoreCase(deviceKey)) {
					throw new ApiException("error-edit-node-mac-address");
				}
			} else {
				if (StringUtils.isBlank(deviceHost) || StringUtils.isBlank(devicePort)) {
					throw new ApiException("host-or-port-missing");
				}
			}

			// check existing devices with the same host + port + deviceKey
			List<MongoDevice> bucketDevices = DeviceManager.getInstance().getDevicesOfBucket(bucketId);
			for (MongoDevice bktDvc : bucketDevices) {
				if (!bktDvc.getDeviceId().equals(platformDevice.getDeviceId()) && bktDvc.getHost().equals(deviceHost)
						&& bktDvc.getPort().equals(devicePort) && bktDvc.getDeviceKey().equals(deviceKey)) {
					throw new ApiException("device-info-already-exists");
				}
			}

			platformDevice.setName(deviceName);
			platformDevice.setModelId(deviceModel.getModelId());
			platformDevice.setDeviceKey(deviceKey);
			platformDevice.setHost(deviceHost);
			platformDevice.setPort(devicePort);
			platformDevice.setLogin(deviceLogin);
			if (!devicePassword.equalsIgnoreCase(platformDevice.getPassword())) {
				platformDevice.setPassword(CryptoManager.aesEncrypt(devicePassword));
			}
			platformDevice.setLatitude(Double.parseDouble(deviceLatitude));
			platformDevice.setLongitude(Double.parseDouble(deviceLongitude));
			platformDevice.setAddress(deviceAddress);
			platformDevice.setCloudRecordingEnabled(cloudRecordingEnabled.equalsIgnoreCase("true"));

			// update core
			DeviceManager.getInstance().updateDevice(platformDevice);

			Map map = new ResultMap();
			map.put("result", "ok");
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param device-id
	 *            Platform side ID of the device
	 *
	 * @servtitle Removes a device from own bucket
	 * @httpmethod POST
	 * @uri /api/{bucket}/removedevicefrombucket
	 * @responsejson { "result": "ok" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void removedevicefrombucket(String bucket) throws ApiException {
		try {
			String bucketId = getCallerBucketId();
			String deviceId = readApiParameter("device-id", true);

			boolean result = DeviceManager.getInstance().removeDeviceFromBucket(bucketId, deviceId);

			Map map = new ResultMap();
			if (result) {
				map.put("result", "ok");
			} else {
				map.put("result", "error");
			}
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @servtitle Returns list of devices in the account
	 * @httpmethod GET
	 * @uri /api/{bucket}/getbucketdevices
	 * @responsejson { "result": "ok", "devices": [ { "name": "TW Office Node 69",
	 *               "deviceId": "200", "model": { "modelId": 115, "name": "KAI
	 *               NODE", "channels": 4, "capabilities": "video mjpeg video mjpeg
	 *               h264 node", "id": 15 }, "deviceKey": "a1:b2:c3:d4:e5:f6",
	 *               "host": "", "port": "", "login": "", "password": "", "address":
	 *               "Taipei", "latitude": "25.091", "longitude": "121.56", "label":
	 *               [], "cloudRecordingEnabled": false, "bucket": 2, "users": [ {
	 *               "id": 2 } ], "id": 27, "node": { "name": "TW Office Node 69",
	 *               "cloudPlatformDeviceId": "27", "cloudCoreDeviceId": "200",
	 *               "cameras": [ { "name": "Amtk112", "nodePlatformDeviceId": "1",
	 *               "nodeCoreDeviceId": "1", "channels": 1 } ], "analytics": [ {
	 *               "instanceId": "525e5a5ce4b096d09d99da61", "type": "INTRUSION",
	 *               "nodePlatformDeviceId": "1", "channel": 1, "info": "" } ],
	 *               "_id": "525e59a3e4b0db62148d7a3c", "_created": 1381915043476,
	 *               "_modified": 1381915280259 } } ] } }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void getbucketdevices(String bucket) throws ApiException {
		try {
			String bucketId = getCallerBucketId();

			List<MongoDevice> bucketDevices = DeviceManager.getInstance().getDevicesOfBucket(bucketId);
			List<DeviceTransport> transports = new ArrayList<>();
			for (MongoDevice bucketDevice : bucketDevices) {
				if (!bucketDevice.isSuspended()) {
					transports.add(new DeviceTransport(bucketDevice));
				}
			}

			Map map = new ResultMap();
			map.put("result", "ok");
			map.put("devices", transports);
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @servtitle Returns unique device labels of current bucket
	 * @httpmethod POST
	 * @uri /api/{bucket}/getbucketdevicelabels
	 * @responsejson { "result": "ok", "labels": "["backboor", "east", "west"]" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void getbucketdevicelabels(String bucket) throws ApiException {
		try {
			List<String> labels = new ArrayList<>();

			Map map = new ResultMap();
			map.put("result", "ok");
			map.put("labels", labels);
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param device-id
	 *            Platform side ID of device
	 * @param user-id
	 *            ID of user
	 *
	 * @servtitle Allow a user to access a device
	 * @httpmethod POST
	 * @uri /api/{bucket}/adddeviceuser
	 * @responsejson { "result": "ok" }
	 * @responsejson { "result": "error", "report": "Unauthorized" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void adddeviceuser(String bucket) throws ApiException {
		try {
			String deviceId = readApiParameter("device-id", true);
			String userId = readApiParameter("user-id", true);
			String bucketId = getCallerBucketId();

			// verify user-id validity
			MongoUser user = MongoUser.getById(userId);
			if (user == null) {
				throw new ApiException("invalid-user-id");
			}

			// verify that the user belongs to the same bucket as logged in user
			if (!bucketId.equals(user.getBucketId())) {
				throw new ApiException("Unauthorized");
			}

			boolean result = DeviceManager.getInstance().addUserToDevice(userId, deviceId);

			Map map = new ResultMap();
			if (result) {
				map.put("result", "ok");
			} else {
				map.put("result", "error");
			}
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param device-id
	 *            Platform side ID of device
	 * @param user-id
	 *            ID of user
	 *
	 * @servtitle Disallow a user from accessing a device
	 * @httpmethod POST
	 * @uri /api/{bucket}/removedeviceuser
	 * @responsejson { "result": "ok" }
	 * @responsejson { "result": "error", "report": "Unauthorized" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void removedeviceuser(String bucket) throws ApiException {
		try {
			String deviceId = readApiParameter("device-id", true);
			String userId = readApiParameter("user-id", true);
			String bucketId = getCallerBucketId();

			// verify user-id validity
			MongoUser targetUser = MongoUser.getById(userId);
			if (targetUser == null) {
				throw new ApiException("invalid-user-id");
			}

			// verify that the user belongs to the same bucket as logged in user
			if (!bucketId.equals(targetUser.getBucketId())) {
				throw new ApiException("Unauthorized");
			}

			boolean result = DeviceManager.getInstance().removeUserFromDevice(userId, deviceId);

			Map map = new ResultMap();
			if (result) {
				map.put("result", "ok");
			} else {
				map.put("result", "error");
			}
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @servtitle Returns list of devices that the current user has access to
	 * @httpmethod GET
	 * @uri /api/{bucket}/getuserdevices
	 * @responsejson { "result": "ok", "devices": [
	 *               {@link models.transportobjects.DeviceTransport} ] } }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void getuserdevices(String bucket) throws ApiException {
		try {
			String userId = getCallerUserId();

			List<DeviceTransport> transports = new ArrayList<>();
			List<MongoDevice> userDevices = DeviceManager.getInstance().getDevicesOfUser(userId);
			for (MongoDevice userDevice : userDevices) {
				if (!userDevice.isSuspended()) {
					transports.add(new DeviceTransport(userDevice));
				}
			}

			Map map = new ResultMap();
			map.put("result", "ok");
			map.put("devices", transports);
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param user-id
	 *            ID of the user whose devices are to be retrieved. Mandatory
	 *
	 * @servtitle Returns list of devices that the specified user has access to
	 * @httpmethod POST
	 * @uri /api/{bucket}/getuserdevicesbyuserid
	 * @responsejson { "result": "ok", "devices": [
	 *               {@link models.transportobjects.DeviceTransport} ] } }
	 * @responsejson { "result": "error", "reason": "Requested User ID is invalid"
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void getuserdevicesbyuserid(String bucket) throws ApiException {
		try {
			String callerUserId = getCallerUserId();
			String targetUserId = readApiParameter("user-id", true);

			// verify user-id validity
			if (callerUserId == null || callerUserId.isEmpty()) {
				throw new InternalException("User ID is invalid in Service, this should not happen!");
			}

			// verify user-id validity
			MongoUser callerUser = MongoUser.getById(callerUserId);
			MongoUser targetUser = MongoUser.getById(targetUserId);
			if (targetUser == null) {
				throw new ApiException("Requested User ID is invalid");
			}
			if (!callerUser.getBucketId().equals(targetUser.getBucketId())) {
				throw new ApiException("Request User ID does not belong to current user's bucket");
			}

			List<MongoDevice> devices = DeviceManager.getInstance().getDevicesOfUser(targetUser.getUserId());
			List<DeviceTransport> transports = new ArrayList<>();
			for (MongoDevice device : devices) {
				if (!device.isSuspended()) {
					transports.add(new DeviceTransport(device));
				}
			}

			Map map = new ResultMap();
			map.put("result", "ok");
			map.put("devices", transports);
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @servtitle Returns list of models of devices supported by the system
	 * @httpmethod GET
	 * @uri /api/{bucket}/getdevicemodels
	 * @responsejson { "result": "ok", "model-list": [ { "modelId": 100, "name":
	 *               "Dahua IP Camera", "channels": 1, "capabilities": "video mjpeg
	 *               video mjpeg h264", "id": 1 }, { "modelId": 101, "name": "Dahua
	 *               DVR 4-Channel", "channels": 4, "capabilities": "video mjpeg
	 *               video mjpeg h264", "id": 2 }, { "modelId": 102, "name": "Amegia
	 *               IP Camera", "channels": 1, "capabilities": "video mjpeg video
	 *               mjpeg h264", "id": 3 } ] }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void getdevicemodels(String bucket) throws ApiException {
		try {
			List<DeviceModel> sqlDeviceModels = new ArrayList<>();
			List<MongoDeviceModel> mongoDeviceModels = DeviceManager.getInstance().getDeviceModels();
			for (MongoDeviceModel mongoDeviceModel : mongoDeviceModels) {
				sqlDeviceModels.add(new DeviceModel(mongoDeviceModel));
			}

			Map map = new ResultMap();
			map.put("result", "ok");
			map.put("model-list", sqlDeviceModels);
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param registration-number
	 *            The name of registration e.g reg123
	 * @param model-number
	 *            The model of device e.g 111
	 * @param mac-address
	 *            The mac address of device e.g 12:1a:1e:f2:2b:9e
	 *
	 * @servtitle Add inventory item
	 * @httpmethod POST
	 * @uri /api/{bucket}/addinventory
	 * @responsejson { "result": "ok" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void addinventory(String bucket) throws ApiException {
		Map result = new ResultMap();

		try {
			String pRegNumber = readApiParameter("registration-number", true);
			String pModelNumber = readApiParameter("model-number", true);
			String pMacAddress = readApiParameter("mac-address", true);

			InventoryInfo inventoryInfo = new InventoryInfo();
			inventoryInfo.macAddress = pMacAddress;
			inventoryInfo.modelNumber = pModelNumber;
			inventoryInfo.registrationNumber = pRegNumber;

			InventoryManager.getInstance().addInventory(inventoryInfo);
			result.put("result", "ok");
			renderJSON(result);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param csvFile
	 *            The CSV file with inventory data.
	 *
	 * @servtitle Add data to inventory by uploading CSV file and return duplicate
	 *            inventory item detail if any
	 * @httpmethod POST
	 * @uri /api/{bucket}/uploadinventory
	 * @responsejson { "result": "ok", "existingItems": [ { "registrationNumber":
	 *               "THF112000541600182", "modelNumber": "115", "macAddress":
	 *               "30:0e:d5:1b:ce:7f", "activated": true }, {
	 *               "registrationNumber": "THF112000541600199", "modelNumber":
	 *               "119", "macAddress": "30:0e:d5:1b:ce:ec", "activated": false },
	 *               { "registrationNumber": "THF112000532900133", "modelNumber":
	 *               "100", "macAddress": "d0:27:88:e7:6f:9e", "activated": true } ]
	 *               }
	 * @responsejson { NOTE: Incorrect format in the inventory file }
	 * @responsejson { NOTE: Error! inventory already exists. Line: 1 }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void uploadinventory(File csvFile) throws ApiException {
		Map map = new ResultMap();
		try {

			if (null == csvFile) {
				throw new ApiException("attached file not found");
			}

			InventoryManager im = InventoryManager.getInstance();
			List<InventoryInfo> inventoryInfos = im.readUploadedFile(csvFile);
			List<InvalidInventory> invalidInventories = im.findInvalidRecords(inventoryInfos);
			if (invalidInventories.size() > 0) {
				map.put("result", "error");
				map.put("invalidInventories", invalidInventories);
			} else {
				List<InventoryItem> sqlInventoryItems = new ArrayList<>();
				List<MongoInventoryItem> mongoInventoryItems = InventoryManager.getInstance()
						.findDuplicateRecords(inventoryInfos);

				for (MongoInventoryItem mongoInventoryItem : mongoInventoryItems) {
					sqlInventoryItems.add(new InventoryItem(mongoInventoryItem));
				}
				map.put("result", "ok");
				map.put("existingItems", sqlInventoryItems);
			}
			renderJSON(map);

		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @servtitle Returns list of items in the inventory
	 * @httpmethod GET
	 * @uri /api/{bucket}/getinventorylist
	 * @responsejson { "result": "ok", "inventory-list": [ { "registrationNumber":
	 *               "THF10P130531800069", "modelNumber": "115", "macAddress":
	 *               "d0:27:88:e7:6f:1c", "username": "", "password": "",
	 *               "activated": true, "inventoryId": 19 "deviceName" : device-1
	 *               "bucketName" : kaisquare }, { "registrationNumber":
	 *               "THF10P130531800076", "modelNumber": "115", "macAddress":
	 *               "d0:27:88:e7:70:c8", "username": "", "password": "",
	 *               "activated": false, "invetoryId": 20 "deviceName" : device-1
	 *               "bucketName" : kaisquare } ] }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void getinventorylist() throws ApiException {
		try {
			List<MongoInventoryItem> inventoryItems = MongoInventoryItem.q().fetchAll();
			List<InventoryInfo> inventoryInfos = new ArrayList<>();
			for (MongoInventoryItem inventoryItem : inventoryItems) {
				InventoryInfo inventoryInfo = new InventoryInfo();
				inventoryInfo.inventoryId = Long.parseLong(inventoryItem.getInventoryItemId());
				inventoryInfo.macAddress = inventoryItem.getMacAddress();
				inventoryInfo.registrationNumber = inventoryItem.getRegistrationNumber();
				inventoryInfo.activated = inventoryItem.isActivated();
				if (inventoryInfo.activated) {
					MongoDevice device = MongoDevice.getByDeviceKey(inventoryItem.getMacAddress());
					MongoBucket bucket = MongoBucket.getById(device.getBucketId());
					if (device != null) {
						inventoryInfo.deviceName = device.getName();
						inventoryInfo.bucketName = bucket.getName();
					}
				}
				try {
					MongoDeviceModel deviceModel = MongoDeviceModel.getByModelId(inventoryItem.getDeviceModelId());
					if (deviceModel != null) {
						inventoryInfo.modelName = deviceModel.getName();
						inventoryInfo.modelNumber = inventoryItem.getDeviceModelId();
					}
				} catch (NumberFormatException e) {
					Logger.info("deviceProvisioning getInventoryList() :: " + e.getMessage());
				}
				inventoryInfos.add(inventoryInfo);
			}

			// sort by id
			Collections.sort(inventoryInfos, new Comparator<InventoryInfo>() {
				@Override
				public int compare(InventoryInfo o1, InventoryInfo o2) {
					return Long.compare(o1.inventoryId, o2.inventoryId);
				}
			});

			Map map = new ResultMap();
			map.put("result", "ok");
			map.put("inventory-list", inventoryInfos);
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param inventory-id
	 *            The id of the inventory
	 * @param registration-name
	 *            The name of registration e.g reg123
	 * @param model-number
	 *            The model of device e.g 111
	 * @param mac-address
	 *            The mac address of device e.g 12:1a:1e:f2:2b:9e
	 *
	 * @servtitle Update inventory
	 * @httpmethod POST
	 * @uri /api/{bucket}/updateinventory
	 * @responsejson { "result": "ok" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void updateinventory(String bucket) throws ApiException {
		try {
			String inventoryId = readApiParameter("inventory-id", true);
			String registrationName = readApiParameter("registration-name", true);
			String modelNumber = readApiParameter("model-number", true);
			String macAddress = readApiParameter("mac-address", true);

			// check if mac is already in the system
			// must loop to check case-insensitive equals
			List<MongoInventoryItem> inventoryItems = MongoInventoryItem.q().fetchAll();
			for (MongoInventoryItem inventoryItem : inventoryItems) {
				if (inventoryItem.getMacAddress().equalsIgnoreCase(macAddress)
						&& !inventoryItem.getInventoryItemId().equals(inventoryId)) {
					throw new ApiException("error-mac-in-use");
				}
			}

			InventoryInfo inventoryInfo = new InventoryInfo();
			inventoryInfo.inventoryId = Long.parseLong(inventoryId);
			inventoryInfo.registrationNumber = registrationName;
			inventoryInfo.modelNumber = modelNumber;
			inventoryInfo.macAddress = macAddress.trim().toLowerCase();

			InventoryManager.getInstance().updateInventory(inventoryInfo);
			Map<String, Object> map = new ResultMap();
			map.put("result", "ok");
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @servtitle Deletes all inventory data from the system
	 * @httpmethod POST
	 * @uri /api/{bucket}/removeallinventory
	 * @responsejson { "result": "ok" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void removeallinventory() {
		try {
			MongoInventoryItem.deleteAll();
			Map map = new ResultMap();
			map.put("result", "ok");
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param inventory-id
	 *            ID of the inventory ID to be deleted. Mandatory
	 *
	 * @servtitle Deletes the specified inventory item from the system
	 * @httpmethod POST
	 * @uri /api/{bucket}/removeinventory
	 * @responsejson { "result": "ok" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void removeinventory(String bucket) throws ApiException {
		try {
			String inventoryId = readApiParameter("inventory-id", true);
			MongoInventoryItem inventoryItem = MongoInventoryItem.getById(inventoryId);
			if (inventoryItem != null) {
				if (inventoryItem.isActivated()) {
					throw new ApiException("Activated inventories cannot be deleted"); // todo: need to inform device if
																						// this is to be allowed
				}
				inventoryItem.delete();
			}

			Map map = new ResultMap();
			map.put("result", "ok");
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param node-id
	 *            ID of the node. mandatory
	 *
	 * @servtitle Returns all the cameras of specified node
	 * @httpmethod POST
	 * @uri /api/{bucket}/getnodecameralist
	 * @responsejson { "result": "ok", "cameras": [ { "name": "TW Office Node 69",
	 *               "nodePlatformDeviceId": "2", "nodeCoreDeviceId": "2", "model":
	 *               { "modelId": 115, "name": "KAI NODE", "channels": 4,
	 *               "capabilities": "video mjpeg video mjpeg h264 node", "id": 15
	 *               }, "deviceKey": "a1:b2:c3:d4:e5:f6", "host": "", "port": "",
	 *               "login": "", "password": "", "address": "Taipei", "latitude":
	 *               "25.091", "longitude": "121.56", "cloudRecordingEnabled":
	 *               false, }, { "name": "Nepal office", "nodePlatformDeviceId":
	 *               "3", "nodeCoreDeviceId": "3", "model": { "modelId": 115,
	 *               "name": "KAI NODE", "channels": 4, "capabilities": "video mjpeg
	 *               video mjpeg h264 node", "id": 15 }, "deviceKey":
	 *               "a1:b2:c3:d5:e5:f6", "host": "", "port": "", "login": "",
	 *               "password": "", "address": "Taipei", "latitude": "86.091",
	 *               "longitude": "121.56", "cloudRecordingEnabled": false, } ] }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void getnodecameralist() {
		Map responseMap = new LinkedHashMap();
		try {
			String nodeCloudPlatormId = readApiParameter("node-id", true);
			NodeObject nodeObject = NodeObject.findByPlatformId(nodeCloudPlatormId);

			List<NodeCamera> sortedList = nodeObject.getCameras();
			Collections.sort(sortedList, NodeCamera.sortByCoreId);

			List<NodeCameraTransport> transports = new ArrayList<>();
			for (NodeCamera nodeCamera : sortedList) {
				transports.add(new NodeCameraTransport(nodeObject, nodeCamera));
			}

			responseMap.put("result", "ok");
			responseMap.put("cameras", transports);
			renderJSON(responseMap);

		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param node-id
	 *            ID of the node. mandatory
	 *
	 * @servtitle Returns all the analytics of specified node
	 * @httpmethod POST
	 * @uri /api/{bucket}/getnodeanalyticslist
	 * @responsejson { "result": "ok", "analytics": [ { "instanceId":
	 *               "525e5a5ce4b096d09d99da61", "type": "PCOUNTING",
	 *               "nodePlatformDeviceId": "1", "channel": 1,
	 *               "thresholds":"{"regions":["0.4150,0.1133,0.9100,0.3400","0.3825,0.6300,0.9325,0.8967"],
	 *               "direction":"r1r2","ksize":1,"sigma":1,"ccthresh":0.03,"tsr":4,"additional-params":{}}"
	 *               "enabled": true, "VcaState": WAITING, "recurrenceRule": }, {
	 *               "instanceId": "525e5a5ceaetyr096d09d99da61", "type":
	 *               "TRAFFICFLOW", "nodePlatformDeviceId": "2", "channel": 2,
	 *               "thresholds":"{"regions":["0.4150,0.1133,0.9100,0.3400","0.3825,0.6300,0.9325,0.8967"],
	 *               "direction":"r1r2","ksize":1,"sigma":1,"ccthresh":0.03,"tsr":4,"additional-params":{}}"
	 *               "enabled": true, "VcaState": RUNNING, "recurrenceRule": }
	 *               <p/>
	 *               ] }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void getnodeanalyticslist() {
		Map responseMap = new LinkedHashMap();
		try {
			String nodeCloudPlatformId = readApiParameter("node-id", true);
			MongoDevice nodeDevice = MongoDevice.getByPlatformId(nodeCloudPlatformId);
			if (nodeDevice == null) {
				throw new ApiException("invalid-node-id");
			}

			List<IVcaInstance> vcaList = VcaManager.getInstance().listVcaInstancesOfDevice(nodeDevice);
			List<VcaTransport> transportList = new ArrayList<>();
			for (IVcaInstance vcaInstance : vcaList) {
				VcaTransport transport = new VcaTransport(vcaInstance);
				transport.updateRequired = false;
				transportList.add(transport);
			}

			responseMap.put("result", "ok");
			responseMap.put("analytics", transportList);
			renderJSON(responseMap);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param node-id
	 *            Node's platform device id. mandatory
	 * @param node-camera-id
	 *            Node Camera's platform device id. mandatory
	 * @param camera-name
	 *            Node camera's new name. mandatory
	 * @param device-key
	 *            The device key. mandatory
	 * @param host
	 *            The device host name or IP address. mandatory
	 * @param port
	 *            The device port number
	 * @param login
	 *            Username to access this device
	 * @param password
	 *            Password to access this device
	 * @param address
	 *            Address where this device is located
	 * @param latitude
	 *            Latitude of device's location
	 * @param longitude
	 *            Longitude of device's location
	 * @param cloud-recording-enabled
	 *            Enable or disable cloud recording (true/false)
	 *
	 * @servtitle Edit node camera details
	 * @httpmethod POST
	 * @uri /api/{bucket}/editnodecamera
	 * @responsejson { "result": "ok" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void editnodecamera() throws ApiException {
		try {
			String currentUserId = renderArgs.get("caller-user-id").toString();

			String nodePlatformId = readApiParameter("node-id", true);
			String nodeCameraPlatformId = readApiParameter("node-camera-id", true);
			String cameraNewName = readApiParameter("camera-name", true);
			String deviceKey = readApiParameter("device-key", false);
			String host = readApiParameter("host", false);
			String port = readApiParameter("port", false);
			String login = readApiParameter("login", false);
			String password = readApiParameter("password", false);
			String address = readApiParameter("address", false);
			String latitude = readApiParameter("latitude", false);
			String longitude = readApiParameter("longitude", false);
			String cloudRecordingEnabled = readApiParameter("cloudRecordingEnabled", false);
			String labels = readApiParameter("labels", false);

			// check missing params
			if (deviceKey.isEmpty() && (host.isEmpty() || port.isEmpty())) {
				throw new ApiException("devicekey-or-hostport");
			} else if ((!host.isEmpty() && port.isEmpty()) || (host.isEmpty() && !port.isEmpty())) {
				throw new ApiException("devicekey-or-hostport");
			}

			// validate params
			NodeObject nObj = NodeObject.findByPlatformId(nodePlatformId);
			if (nObj == null) {
				throw new ApiException("invalid-node-id");
			}

			NodeCamera targetCam = null;
			for (NodeCamera nodeCam : nObj.getCameras()) {
				if (nodeCam.nodePlatformDeviceId.equals(nodeCameraPlatformId)) {
					targetCam = nodeCam;
				} else if (host.equalsIgnoreCase(nodeCam.host) && port.equalsIgnoreCase(nodeCam.port)) {
					throw new ApiException("hostport-repeats");
				}
			}
			if (targetCam == null) {
				throw new ApiException("invalid-node-camera-id");
			}

			// check access
			if (!DeviceManager.getInstance().checkUserDeviceAccess(nodePlatformId, currentUserId)) {
				throw new ApiException("device-access-denied");
			}

			// parse labels
			List<String> labelList = new ArrayList<>();
			if (!isNullOrEmpty(labels)) {
				labelList = new Gson().fromJson(labels, labelList.getClass());
			}

			// update details
			targetCam.name = cameraNewName;
			targetCam.deviceKey = deviceKey;
			targetCam.host = host;
			targetCam.port = port;
			targetCam.login = login;
			if (!password.equalsIgnoreCase(targetCam.password)) {
				targetCam.password = CryptoManager.aesEncrypt(password);
			}
			targetCam.address = address;
			targetCam.latitude = latitude;
			targetCam.longitude = longitude;
			targetCam.cloudRecordingEnabled = cloudRecordingEnabled.equals("true");
			targetCam.labels = labelList;
			DeviceManager.getInstance().editNodeCamera(nodePlatformId, targetCam);

			Map map = new ResultMap();
			map.put("result", "ok");
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param model-id
	 *            Model id of device to discover
	 *
	 * @servtitle Starts descovering device status
	 * @httpmethod POST
	 * @uri /api/{bucket}/startautodiscovery
	 * @responsejson { "result": "ok" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void startautodiscovery(String bucket) throws ApiException {
		try {
			String modelId = readApiParameter("model-id", false);
			int intModelId = 0;
			if (modelId != null && Util.isInteger(modelId)) {
				intModelId = Integer.parseInt(modelId);
			}

			DeviceDiscoveryStarter starter = DeviceDiscoveryStarter.getStarter();
			starter.start(intModelId);

			Map map = new ResultMap();
			map.put("result", "ok");
			renderJSON(map);

		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @servtitle Stops all the running device discovery starter
	 * @httpmethod POST
	 * @uri /api/{bucket}/stopautodiscovery
	 * @responsejson { "result": "ok" }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void stopautodiscovery(String bucket) throws ApiException {
		try {
			DeviceDiscoveryStarter.getStarter().stopAll();
			Map map = new ResultMap();
			map.put("result", "ok");
			renderJSON(map);

		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @servtitle Returns discovered devices
	 * @httpmethod POST
	 * @uri /api/{bucket}/getdiscovereddevices
	 * @responsejson { "result": "ok", "devices": [ { "modelId": 115, "ipAddress":
	 *               1.1.1.1, "macAddress": a1:b2:c3:d5:e5:f6, "deviceName":
	 *               Backdoor, "deviceLocation": Nepal, "firmwareVersion": 2.0.2,
	 *               "model": AMT, "httpPort": 5432, "rtspPort": 8080 }, {
	 *               "modelId": 119, "ipAddress": 1.1.9.1, "macAddress":
	 *               a1:b2:c4:d5:e5:f6, "deviceName": frontdoor, "deviceLocation":
	 *               Taiwan, "firmwareVersion": 2.0.3, "model": AMTT, "httpPort":
	 *               5411, "rtspPort": 9090 } ] }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void getdiscovereddevices(String bucket) throws ApiException {
		try {
			DeviceDiscoveryStarter starter = DeviceDiscoveryStarter.getStarter();
			List<DiscoveredDevice> list = starter.getDiscoveredDevices();

			Map map = new ResultMap();
			map.put("result", "ok");
			map.put("devices", list);
			renderJSON(map);

		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param node-id
	 *            ID of the node. mandatory
	 *
	 * @servtitle Returns setting of the specified node
	 * @httpmethod POST
	 * @uri /api/{bucket}/getnodesettings
	 * @responsejson { "result": "ok", "settings": [ { "NetworkSettings": [ {
	 *               "ipAddress": 192.168.0.1, "netmask": 20, "gateway":
	 *               192.168.0.1, "nameservers":["n1.nameserver.com",
	 *               "n2.nameserver.com", "n3.nameserver.com" ] } ], "timezone":
	 *               "UTC" } ] }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void getnodesettings() {
		Map responseMap = new LinkedHashMap();
		try {
			String nodeCloudPlatormId = readApiParameter("node-id", true);
			NodeObject nodeObject = NodeObject.findByPlatformId(nodeCloudPlatormId);

			responseMap.put("result", "ok");
			responseMap.put("settings", nodeObject.getSettings());
			renderJSON(responseMap);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param bucket-id
	 *            The bucket id. eg. 2. Mandatory
	 *
	 * @servtitle Returns the devices of bucket by bucket id
	 * @httpmethod GET
	 * @uri /api/{bucket}/getbucketdevicesbybucketid
	 * @responsejson { "result": "ok", "totalcount": 1, "devices": [ { "name": "TW
	 *               Office Node 69", "deviceId": "200", "model": { "modelId": 115,
	 *               "name": "KAI NODE", "channels": 4, "capabilities": "video mjpeg
	 *               video mjpeg h264 node", "id": 15 }, "deviceKey":
	 *               "a1:b2:c3:d4:e5:f6", "host": "", "port": "", "login": "",
	 *               "password": "", "address": "Taipei", "latitude": "25.091",
	 *               "longitude": "121.56", "label": [], "cloudRecordingEnabled":
	 *               false, "buckets": [ { "name": "kaisquare", "path": "kaisquare",
	 *               "description": "Kaisquare admin account", "activated": true,
	 *               "features": [ { "name": "common", "type": "common", "services":
	 *               [ { "name": "login", "version": "1", "id": 1 }, { "name":
	 *               "logout", "version": "1", "id": 2 } ], "id": 1 } ], "services":
	 *               [ { "name": "login", "version": "1", "id": 1 }, { "name":
	 *               "logout", "version": "1", "id": 2 } ], "users": [ { "name":
	 *               "Admin", "login": "*******", "password": "******", "email": "",
	 *               "two_factor_mode": 0, "session_timeout": 300, "activated":
	 *               true, "creationTimestamp": "15:05:14", "phone": "", "language":
	 *               "en", "bucketId": 2, "roles": [ { "name": "Administrator",
	 *               "description": "Admin account with access to all features",
	 *               "features": [ "name": "common", "type": "common", "services": [
	 *               { "name": "login", "version": "1", "id": 1 }, { "name":
	 *               "logout", "version": "1", "id": 2 } ], "id": 1 } ], "id": 2 }
	 *               ], "services": [ { "name": "login", "version": "1", "id": 1 },
	 *               { "name": "logout", "version": "1", "id": 2 } ], "id": 2 } ],
	 *               "roles": [ { "name": "Administrator", "description": "Admin
	 *               account with access to all features", "features": [ { "name":
	 *               "remote-shell", "type": "customer-support", "services": [ {
	 *               "name": "startremoteshell", "version": "1", "id": 302 } ],
	 *               "id": 63 } ], "id": 2 }, { "name": "Supervisor", "description":
	 *               "Access to all features except admin settings", "features": [],
	 *               "id": 3 } ], "id": 2 } ], "users": [ { "name": "Admin",
	 *               "login": "*******", "password": "******", "email": "",
	 *               "two_factor_mode": 0, "session_timeout": 300, "activated":
	 *               true, "creationTimestamp": "15:05:14", "phone": "", "language":
	 *               "en", "bucketId": 2, "roles": [ { "name": "Administrator",
	 *               "description": "Admin account with access to all features",
	 *               "features": [ "name": "common", "type": "common", "services": [
	 *               { "name": "login", "version": "1", "id": 1 }, { "name":
	 *               "logout", "version": "1", "id": 2 } ], "id": 1 } ], "id": 2 }
	 *               ], "services": [ { "name": "login", "version": "1", "id": 1 },
	 *               { "name": "logout", "version": "1", "id": 2 } ], "id": 2 } ],
	 *               "id": 27, } ] } }
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */

	public static void getbucketdevicesbybucketid() {
		try {
			String bucketId = readApiParameter("bucket-id", true);

			List<MongoDevice> devices = DeviceManager.getInstance().getDevicesOfBucket(bucketId);
			List<DeviceTransport> transports = new ArrayList<>();
			for (MongoDevice device : devices) {
				transports.add(new DeviceTransport(device));
			}

			Map map = new ResultMap();
			map.put("result", "ok");
			map.put("totalcount", transports.size());
			map.put("devices", transports);
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param platform-device-id
	 *            The id of device. Mandatory
	 * @param skip
	 *            starting log offset index
	 * @param take
	 *            number of logs to return
	 *
	 * @servtitle Returns connection logs of a device
	 * @httpmethod POST
	 * @uri /api/{bucket}/getdevicelogs
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */
	public static void getdevicelogs() {
		try {
			String platformDeviceId = readApiParameter("platform-device-id", true);
			String skip = readApiParameter("skip", false);
			String take = readApiParameter("take", false);

			int iSkip = 0;
			if (Util.isInteger(skip)) {
				iSkip = Integer.parseInt(skip);
			}

			int iTake = 0;
			if (Util.isInteger(take)) {
				iTake = Integer.parseInt(take);
			}

			ServerPagedResult<DeviceLog> pagedResult = DeviceLog.query(Long.parseLong(platformDeviceId), iSkip, iTake);
			Map map = new ResultMap();
			map.put("result", "ok");
			map.put("total-count", pagedResult.getTotalCount());
			map.put("logs", pagedResult.getResultsForOnePage());
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param platform-device-id
	 *            The id of device. Mandatory
	 *
	 * @servtitle Returns node object info
	 * @httpmethod POST
	 * @uri /api/{bucket}/getnodeinfo
	 * @responsejson { "result": "error", "reason": "unknown" }
	 */
	public static void getnodeinfo() {
		try {
			String nodeId = readApiParameter("node-id", true);

			MongoDevice nodeDevice = MongoDevice.getByPlatformId(nodeId);
			if (nodeDevice == null) {
				throw new ApiException("invalid-node-id");
			}

			NodeObject nodeObject = NodeObject.findByPlatformId(nodeDevice.getDeviceId());

			Map map = new ResultMap();
			map.put("result", "ok");
			map.put("info", new NodeObjectTransport(nodeObject));
			renderJSON(map);
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param platform-device-id
	 *            The id of device. Mandatory
	 *
	 * @servtitle Returns connection logs of a device
	 * @httpmethod POST
	 * @uri /api/{bucket}/getnodeinfooncloud
	 * @responsejson { "result": "ok", "info" :
	 *               {@link models.transportobjects.NodeObjectTransport} }
	 */
	public static void getnodeinfooncloud() {
		try {
			String platformDeviceId = readApiParameter("platform-device-id", true);
			MongoDevice nodeDevice = MongoDevice.getByPlatformId(platformDeviceId);
			if (nodeDevice == null) {
				throw new ApiException("invalid-platform-device-id");
			}
			if (!nodeDevice.isKaiNode()) {
				throw new ApiException("not-node-device");
			}

			// check bucket access
			MongoBucket callerBucket = MongoBucket.getById(getCallerBucketId());
			MongoBucket nodeBucket = MongoBucket.getById(nodeDevice.getBucketId());
			if (!callerBucket.hasControlOver(nodeBucket)) {
				throw new ApiException("access-denied");
			}

			NodeObject nodeObject = NodeObject.findByPlatformId(nodeDevice.getDeviceId());
			respondOK("info", new NodeObjectTransport(nodeObject));
		} catch (Exception e) {
			respondError(e);
		}
	}

	/**
	 * @param device-id
	 *            platform device id of the node. Mandatory
	 * @param channel-id
	 *            core device id of the node camera. Mandatory
	 *
	 * @servtitle Returns connection logs of a device
	 * @httpmethod POST
	 * @uri /api/{bucket}/getnodecamerastorage
	 * @responsejson { "result": "ok",
	 *               <p/>
	 *               }
	 */
	public static void getnodecamerastorage() {
		try {
			String nodeId = readApiParameter("device-id", true);
			String cameraCoreId = readApiParameter("channel-id", true);

			NodeObject nodeObject = NodeObject.findByPlatformId(nodeId);
			if (nodeObject == null) {
				throw new ApiException("invalid-node-id");
			}

			DeviceChannelPair idPair = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), cameraCoreId);
			CachedNodeCamera nodeCamera = CacheClient.getInstance().getNodeCamera(idPair);
			if (nodeCamera == null) {
				throw new ApiException("invalid-channel-id");
			}

			Map<String, Object> storageInfo = new LinkedHashMap<>();
			storageInfo.put("recordingLimitMB", nodeCamera.getRecordingLimitMB());
			storageInfo.put("recordingUsageMB", nodeCamera.getRecordingUsageMB());

			Map response = new LinkedHashMap();
			response.put("result", "ok");
			response.put("info", storageInfo);
			renderJSON(response);
		} catch (Exception e) {
			respondError(e);
		}
	}
}
