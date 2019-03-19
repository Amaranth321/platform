//this namespace will be used for all functions later on
var kupapi = window.kupapi || {};
kupapi.debugMode = false;
kupapi.applicationType = null;
kupapi.timeZoneOffset = 0;
kupapi.TIME_FORMAT = "dd/MM/yyyy HH:mm:ss"; //DO NOT EDIT THIS FORMAT
kupapi.TIME_ONLY_FORMAT = "HH:mm"; //DO NOT EDIT THIS FORMAT
kupapi.API_TIME_FORMAT = "ddMMyyyyHHmmss";
kupapi.CdnPath = null;
kupapi.mapSource = "google";
kupapi.platformVersion = null;
kupapi.internetStatus = null;
kupapi.currentUserId = null;

kupapi.onKaiNode = function()
{
    return (kupapi.applicationType == "node");
};

kupapi.onCloud = function()
{
    return (kupapi.applicationType == "cloud");
};

kupapi.getWsServer = function()
{
    var wsProtocol = window.location.protocol == "https:" ? "wss:" : "ws:";
    return wsProtocol + "//" + window.location.host;
};

var kupBucket = null;
var busyOverlayVisibility = false;

var kupDefaultErrorHandler = function fnDefaultErrorHandler(jqXHR, extra) {};

// Must call this function before any other function in this fkupapi.TIME_FORMATile
function kupInit(bucket, defaultErrorHandler) {
    kupBucket = bucket;
    if (defaultErrorHandler)
        kupDefaultErrorHandler = defaultErrorHandler;
    if (typeof kainode !== 'undefined')
        kainode.init(bucket, defaultErrorHandler);

    kupapi.timeZoneOffset = new Date().getTimezoneOffset() * (-1);
}

function ajax(url, params, succFunc, failFunc, method, extra) {
    if (busyOverlayVisibility)
        utils.showLoadingOverlay();

    return $.ajax({
        type: method,
        url: url,
        data: params,
        cache: false,
        success: function(data) {
            if (busyOverlayVisibility)
                utils.hideLoadingOverlay();

            if (succFunc != null) {
                succFunc(data, extra);
            }
        },
        error: function(jqXHR, status) {
            if (busyOverlayVisibility)
                utils.hideLoadingOverlay();

            if (failFunc) {
                failFunc(jqXHR, extra);
            } else if (kupDefaultErrorHandler) {
                kupDefaultErrorHandler(jqXHR, extra);
            }
        }
    });
}

function ajaxPost(url, params, succFunc, failFunc, extra) {
    return ajax(url, params, succFunc, failFunc, 'POST', extra);
}

function sendGETRequest(paramMap, apiName) {
    var link = "/api/" + kupBucket + "/" + apiName + "?";

    $.each(paramMap, function(key, value) {
        link += key + "=" + value + "&";
    });

    link = utils.replaceAll(link, "-", "%2D");
    window.open(link, "_blank");
}

function requestDownloadUrl(url, params, onSuccess, onFailure) {
    if (onSuccess == null) {
        utils.showLoadingTextOverlay(localizeResource("generating-download-file"), false);
        onSuccess = function(responseData) {
            utils.hideLoadingOverlay();
            if (responseData != null && responseData.result == "ok" && responseData["download-url"] != null) {
                window.open(responseData["download-url"], '_blank');
            } else {
                utils.throwServerError(responseData);
            }
        }
    }

    ajaxPost(url, params, onSuccess, onFailure);
}

/**
 * [defined KUP API]
 * @return {object} must return jQuery deferred Object
 */

function login(username, password, rememberSession, onSuccess, onFailure) {
    var params = {
        "user-name": username,
        "password": password,
        "remember-me": rememberSession
    };
    var url = "/api/" + kupBucket + "/login";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function logout(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/logout";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function keepSessionAlive(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/keepalive";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBucketUsers(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getbucketusers";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getUserDevices(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getuserdevices";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getUserDevicesByUserId(userId, sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "user-id": userId
    };
    var url = "/api/" + kupBucket + "/getuserdevicesbyuserid";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBucketDevices(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getbucketdevices";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getUserMobileDevices(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getusermobiledevices";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeMobileDeviceOfUser(sessionKey, identifier, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "identifier": identifier
    };
    var url = "/api/" + kupBucket + "/removemobiledeviceofuser";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBucketDeviceLabels(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getbucketdevicelabels";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addDeviceToBucket(sessionKey, deviceDetails, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "registration-number": deviceDetails.regNumber == null ? "" : deviceDetails.regNumber,
        "device-name": deviceDetails.name,
        "model-id": deviceDetails.model,
        "device-key": deviceDetails.deviceKey,
        "device-host": deviceDetails.host,
        "device-port": deviceDetails.port,
        "device-login": deviceDetails.login,
        "device-password": deviceDetails.password,
        "device-address": deviceDetails.address,
        "device-latitude": deviceDetails.latitude,
        "device-longitude": deviceDetails.longitude,
        "cloud-recording-enabled": deviceDetails.cloudRecordingEnabled
    };
    var url = "/api/" + kupBucket + "/adddevicetobucket";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateDevice(sessionKey, deviceDetails, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceDetails.id,
        "device-name": deviceDetails.name,
        "model-id": deviceDetails.model,
        "device-key": deviceDetails.deviceKey,
        "device-host": deviceDetails.host,
        "device-port": deviceDetails.port !== null ? deviceDetails.port.toString() : "",
        "device-login": deviceDetails.login,
        "device-password": deviceDetails.password,
        "device-address": deviceDetails.address,
        "device-latitude": deviceDetails.latitude,
        "device-longitude": deviceDetails.longitude,
        "cloud-recording-enabled": deviceDetails.cloudRecordingEnabled
    };
    var url = "/api/" + kupBucket + "/updatedevice";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeDeviceFromBucket(sessionKey, deviceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId
    };
    var url = "/api/" + kupBucket + "/removedevicefrombucket";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addDeviceUser(sessionKey, deviceId, userId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "user-id": userId
    };
    var url = "/api/" + kupBucket + "/adddeviceuser";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeDeviceUser(sessionKey, deviceId, userId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "user-id": userId
    };
    var url = "/api/" + kupBucket + "/removedeviceuser";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getLiveVideoUrl(sessionKey, deviceId, channelId, streamType, ttlSeconds, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "stream-type": streamType,
        "ttl-seconds": ttlSeconds
    };
    var url = "/api/" + kupBucket + "/getlivevideourl";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function keepAliveLiveVideoUrl(sessionKey, streamingSessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "streaming-session-key": streamingSessionKey
    };
    var url = "/api/" + kupBucket + "/keepalivelivevideourl";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function expireLiveVideoUrl(sessionKey, streamingSessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "streaming-session-key": streamingSessionKey
    };
    var url = "/api/" + kupBucket + "/expirelivevideourl";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getPlaybackVideoUrl(deviceId, channelId, streamType, from, to, ttlSeconds, onSuccess, onFailure) {
    var params = {
        "device-id": deviceId,
        "channel-id": channelId,
        "stream-type": streamType,
        "from": from,
        "to": to,
        "ttl-seconds" : ttlSeconds
    };
    var url = "/api/" + kupBucket + "/getplaybackvideourl";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function keepAlivePlaybackVideoUrl(sessionKey, streamingSessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "streaming-session-key": streamingSessionKey
    };
    var url = "/api/" + kupBucket + "/keepaliveplaybackvideourl";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function expirePlaybackVideoUrl(streamingSessionKey, onSuccess, onFailure) {
    var params = {
        "streaming-session-key": streamingSessionKey
    };
    var url = "/api/" + kupBucket + "/expireplaybackvideourl";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getDashboard(days, timeZone, onSuccess, onFailure) {
    var params = {
        "days": days,
        "time-zone-offset": timeZone
    };
    var url = "/api/" + kupBucket + "/getdashboard";
    return ajaxPost(url, params, onSuccess, onFailure)
}

function getAllEvents(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getallevents";
    return ajaxPost(url, params, onSuccess, onFailure);
}

// all parameters are optional, Specify them to filter events accordingly
function getEvents(sessionKey, eventType, eventId, skip, take, deviceId, channelId, from, to, bound, radius, fields, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "event-type": eventType == (null || KupEvent.ALL) ? KupEvent.ALL : eventType.toString(),
        "event-id": eventId,
        "skip": skip,
        "take": take,
        "device-id": deviceId,
        "bound": bound == null ? bound : bound.toString(),
        "rad": radius,
        "channel-id": channelId,
        "from": from,
        "to": to,
        "fields": fields
    };
    var url = "/api/" + kupBucket + "/getevents";
    return ajaxPost(url, params, onSuccess, onFailure);
}

// all parameters are optional, Specify them to filter events accordingly
function getEventsWithBinary(eventType, skip, take, deviceId, channelId, from, to, onSuccess, onFailure) {
    var params = {
        "event-type": eventType == (null || KupEvent.ALL) ? KupEvent.ALL : eventType.toString(),
        "skip": skip,
        "take": take,
        "device-id": deviceId,
        "channel-id": channelId,
        "from": from,
        "to": to
    };
    var url = "/api/" + kupBucket + "/geteventswithbinary";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function forgotPassword(username, email, bucket, onSuccess, onFailure) {
    var params = {
        "user-name": username,
        "email": email,
        "bucket": bucket
    };
    var url = "/api/forgotpassword";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function resetPasswordWithKey(key, password, onSuccess, onFailure) {
    var params = {
        "key": key,
        "password": password
    };
    var url = "/api/resetpasswordwithkey";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function isUsernameAvailable(sessionKey, username, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "user-name": username
    };
    var url = "/api/" + kupBucket + "/isusernameavailable";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addUser(sessionKey, name, username, email, phone, labels, password, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "name": name,
        "user-name": username,
        "email": email,
        "phone": phone,
        "labels": labels,
        "password": password
    };
    var url = "/api/" + kupBucket + "/adduser";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateUser(sessionKey, userId, name, username, email, phone, labels, password, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "user-id": userId,
        "name": name,
        "user-name": username,
        "email": email,
        "phone": phone,
        "labels": labels,
        "password": password
    };
    var url = "/api/" + kupBucket + "/updateuser";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBucketUserLabels(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getbucketuserlabels";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function activateBucket(sessionKey, bucketId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/activatebucket";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deactivateBucket(sessionKey, bucketId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/deactivatebucket";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function activateUser(sessionKey, userId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "user-id": userId
    };
    var url = "/api/" + kupBucket + "/activateuser";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deactivateUser(sessionKey, userId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "user-id": userId
    };
    var url = "/api/" + kupBucket + "/deactivateuser";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeUser(sessionKey, userId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "user-id": userId
    };
    var url = "/api/" + kupBucket + "/removeuser";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function receiveCometNotification(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/recvcometnotification";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateUserProfile(sessionKey, name, username, email, phone, language, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "name": name,
        "user-name": username,
        "email": email,
        "phone": phone,
        "language": language
    };
    var url = "/api/" + kupBucket + "/updateuserprofile";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function changePassword(sessionKey, oldPassword, newPassword, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "old-password": oldPassword,
        "new-password": newPassword
    };
    var url = "/api/" + kupBucket + "/changepassword";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getDeviceModels(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getdevicemodels";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getUserPrefs(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getuserprefs";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getInventoryList(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getinventorylist";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateInventory(sessionKey, viewModel, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "inventory-id": viewModel.id,
        "registration-name": viewModel.registrationName,
        "model-number": viewModel.modelNumber,
        "mac-address": viewModel.macAddress
    };
    var url = "/api/" + kupBucket + "/updateinventory";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function saveUserprefs(sessionKey, slotSettings, duration, autorotation, autorotationtime, fakeposdatapref, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "slot-settings": JSON.stringify(slotSettings),
        "duration": duration,
        "auto-rotation": autorotation,
        "auto-rotation-time": autorotationtime,
        "fake-pos-data-pref": fakeposdatapref
    };
    var url = "/api/" + kupBucket + "/saveuserprefs";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBucketRoles(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getbucketroles";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addBucketRole(sessionKey, roleName, description, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "role-name": roleName,
        "description": description
    };
    var url = "/api/" + kupBucket + "/addbucketrole";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function editBucketRole(sessionKey, roleId, name, description, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "role-id": roleId,
        "role-name": name,
        "role-description": description
    };
    var url = "/api/" + kupBucket + "/editbucketrole";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeRole(sessionKey, roleId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "role-id": roleId
    };
    var url = "/api/" + kupBucket + "/removerole";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getRoleFeatures(sessionKey, roleId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "role-id": roleId
    };
    var url = "/api/" + kupBucket + "/getrolefeatures";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateRoleFeatures(sessionKey, roleId, featureAssignments, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "role-id": roleId,
        "feature-assignments": featureAssignments
    };
    var url = "/api/" + kupBucket + "/updaterolefeatures";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getUserFeatures(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getuserfeatures";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getUserRolesByUserId(sessionKey, userId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "user-id": userId
    };
    var url = "/api/" + kupBucket + "/getuserrolesbyuserid";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateUserRoles(sessionKey, userId, roleAssignments, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "user-id": userId,
        "role-assignments": roleAssignments
    };
    var url = "/api/" + kupBucket + "/updateuserroles";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addVehicleUser(sessionKey, userId, vehicleId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "user-id": userId,
        "vehicle-id": vehicleId
    };
    var url = "/api/" + kupBucket + "/addvehicleuser";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeVehicleUser(sessionKey, userId, vehicleId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "user-id": userId,
        "vehicle-id": vehicleId
    };
    var url = "/api/" + kupBucket + "/removevehicleuser";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getUserVehicles(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getuservehicles";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getUserVehiclesByUserId(sessionKey, userId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "user-id": userId
    };
    var url = "/api/" + kupBucket + "/getuservehiclesbyuserid";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBucketPois(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getbucketpois";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addPoi(sessionKey, poi, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "name": poi.name,
        "type": poi.type,
        "address": poi.address,
        "latitude": poi.latitude,
        "longitude": poi.longitude,
        "description": poi.description
    };
    var url = "/api/" + kupBucket + "/addpoi";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updatePoi(sessionKey, poi, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "id": poi.id,
        "name": poi.name,
        "type": poi.type,
        "address": poi.address,
        "latitude": poi.latitude,
        "longitude": poi.longitude,
        "description": poi.description
    };
    var url = "/api/" + kupBucket + "/updatepoi";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removePoi(sessionKey, poiId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "poi-id": poiId
    };
    var url = "/api/" + kupBucket + "/removepoi";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBuckets(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getbuckets";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBucketsPlusDeleted(sessionKey, showDeleted, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "show-deleted": showDeleted
    };
    var url = "/api/" + kupBucket + "/getbuckets";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addBucket(sessionKey, bucket, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "parent-bucket-id": bucket.parentId,
        "bucket-name": bucket.name,
        "bucket-path": bucket.path,
        "bucket-description": bucket.description
    };
    var url = "/api/" + kupBucket + "/addbucket";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateBucket(sessionKey, bucket, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucket.id,
        "parent-bucket-id": bucket.parentId,
        "bucket-name": bucket.name,
        "bucket-path": bucket.path,
        "bucket-description": bucket.description
    };
    var url = "/api/" + kupBucket + "/updatebucket";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeBucket(sessionKey, bucketId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/removebucket";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function restoreBucket(sessionKey, bucketId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/restorebucket";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBucketLogs(sessionKey, bucketId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/getbucketlogs";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBucketFeatures(sessionKey, bucketId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/getbucketfeatures";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateBucketFeatures(sessionKey, bucketId, featureAssignments, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId,
        "feature-assignments": featureAssignments
    };
    var url = "/api/" + kupBucket + "/updatebucketfeatures";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeAllInventory(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/removeallinventory";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeInventory(sessionKey, inventoryId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "inventory-id": inventoryId
    };
    var url = "/api/" + kupBucket + "/removeinventory";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getEventBinaryData(eventId, onSuccess, onFailure) {
    var params = {
        "event-id": eventId
    };
    var url = "/api/" + kupBucket + "/geteventbinarydata";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function exportDataLogs(fileFormat, eventType, deviceId, channelId, from, to, onSuccess, onFailure) {
    console.info("exportDataLogs......");
	var params = {
        "file-format": fileFormat,
        "event-type": eventType,
        "from": from,
        "to": to,
        "time-zone-offset": kupapi.timeZoneOffset,
        "device-id": deviceId,
        "channel-id": channelId
    };

    var url = "/api/" + kupBucket + "/exportdatalogs";
    requestDownloadUrl(url, params, onSuccess, onFailure);
}

function getVcaCommands(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/getvcacommands";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function setVcaCommands(sessionKey, instanceId, paramString, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId,
        "param-string": paramString
    };
    var url = "/api/" + kupBucket + "/setvcacommands";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function listRunningAnalytics(sessionKey, analyticsType, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "analytics-type": analyticsType
    };
    var url = "/api/" + kupBucket + "/listrunninganalytics";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateBucketSettings(sessionKey, bucketSetting, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketSetting.bucketId,
        "bucket-userlimit": bucketSetting.userLimit,
        "email-verification-of-users": bucketSetting.emailverificationofusersenabled,
        "custom-logo": bucketSetting.customLogo,
        "binary-data": bucketSetting.binaryData,
        "map-source": bucketSetting.mapSource
    };
    var url = "/api/" + kupBucket + "/updatebucketsettings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function exportVcaSecurityPdf(svgString, reportInfo, onSuccess, onFailure) {
    var params = {
        "time-zone-offset": kupapi.timeZoneOffset,
        "svg-string": svgString,
        "report-info": reportInfo
    };
    var url = "/api/" + kupBucket + "/exportvcasecuritypdf";
    requestDownloadUrl(url, params, onSuccess, onFailure);
}

function exportPeopleCountingPdf(svgString, reportInfo, onSuccess, onFailure) {
    var params = {
        "time-zone-offset": kupapi.timeZoneOffset,
        "svg-string": svgString,
        "report-info": reportInfo
    };

    var url = "/api/" + kupBucket + "/exportpeoplecountingpdf";
    requestDownloadUrl(url, params, onSuccess, onFailure);
}

function exportProfilingChartReport(svgString1, svgString2, reportInfo, onSuccess, onFailure) {
    var params = {
        "time-zone-offset": kupapi.timeZoneOffset,
        "svg-string1": svgString1.toString(),
        "svg-string2": svgString2.toString(),
        "report-info": reportInfo
    };
    var url = "/api/" + kupBucket + "/exportaudienceprofilingpdf";
    requestDownloadUrl(url, params, onSuccess, onFailure);
}

function exportFaceIndexingReport(svgString, reportInfo, eventIdList, onSuccess, onFailure) {
    var params = {
        "time-zone-offset": kupapi.timeZoneOffset,
        "svg-string": svgString,
        "report-info": reportInfo,
        "event-ids": eventIdList
    };
    var url = "/api/" + kupBucket + "/exportfaceindexingpdf";
    requestDownloadUrl(url, params, onSuccess, onFailure)
}

function exportTrafficFlowPdf(base64Image, reportInfo, onSuccess, onFailure) {
    var params = {
        "time-zone-offset": kupapi.timeZoneOffset,
        "base64-image": base64Image,
        "report-info": reportInfo
    };

    var url = "/api/" + kupBucket + "/exporttrafficflowpdf";
    requestDownloadUrl(url, params, onSuccess, onFailure);
}

function exportCrowdReport(sessionKey, deviceId, channelId, base64Image, base64RegionImage, svgImage, reportInfo, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "time-zone-offset": kupapi.timeZoneOffset,
        "base64-image": base64Image,
        "base64-region-image": base64RegionImage,
        "svg-image": svgImage,
        "report-info": reportInfo
    };
    var url = "/api/" + kupBucket + "/exportcrowddensitypdf";
    requestDownloadUrl(url, params, onSuccess, onFailure);
}

function generateSyncFile(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    }
    var url = "/api/" + kupBucket + "/generatesyncfile";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getSoftwareUpdateList(sessionKey, onSuccess, onFailure) {
    var params = {

    }
    var url = "/api/" + kupBucket + "/getsoftwareupdatelist";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateNodeSoftware(nodePlatformId, onSuccess, onFailure) {
    var params = {
        "node-id": nodePlatformId
    }
    var url = "/api/" + kupBucket + "/updatenodesoftware";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeSoftwareUpdate(sessionKey, fileServerId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "file-server-id": fileServerId
    }
    var url = "/api/" + kupBucket + "/removesoftwareupdate";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function downloadSoftwareUpdate(sessionKey, fileName) {
    window.location.href = "/api/" + kupBucket + "/downloadsoftwareupdate/" + fileName;
}

function exportuserlist() {
    var params = {};
    var url = "/api/" + kupBucket + "/exportuserlist";
    requestDownloadUrl(url, params);
}


function getAccessKeyList(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getaccesskeylist";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function generateAccessKey(sessionKey, userId, ttl, maxUseCount, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "user-id": userId,
        "ttl": ttl,
        "max-use-count": maxUseCount
    };
    var url = "/api/" + kupBucket + "/generateaccesskey";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeAccessKey(sessionKey, key, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "key": key
    };
    var url = "/api/" + kupBucket + "/removeaccesskey";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function otpLogin(otp, onSuccess, onFailure) {
    var params = {
        "otp": otp
    };
    var url = "/api/otplogin";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getAnnouncementList(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    if (kupBucket != "")
        var url = "/api/" + kupBucket + "/getannouncementlist";
    else
        var url = "/api/superadmin/getannouncementlist";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addAnnouncement(sessionKey, viewModel, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "announcement-type": viewModel.type,
        "description": viewModel.description,
        "domain": viewModel.domain

    };
    var url = "/api/" + kupBucket + "/addannouncement";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateAnnouncement(sessionKey, viewModel, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "announcement-id": viewModel.id,
        "announcement-type": viewModel.type,
        "description": viewModel.description,
        "domain": viewModel.domain

    };
    var url = "/api/" + kupBucket + "/updateannouncement";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeAnnouncement(sessionKey, id, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "announcement-id": id

    };
    var url = "/api/" + kupBucket + "/removeannouncement";
    return ajaxPost(url, params, onSuccess, onFailure);
}

//all parameters are optional, Specify them to filter alert accordingly
function getAlerts(eventId, eventType, skip, take, deviceId, channelId, from, to, hideResolved, onSuccess, onFailure) {
	var params = {
        "event-id": eventId,
        "event-type": eventType,
        "skip": skip,
        "take": take,
        "device-id": deviceId,
        "channel-id": channelId,
        "from": from,
        "to": to,
        "hide-resolved": hideResolved
    };
    var url = "/api/" + kupBucket + "/getalerts";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getAlertDetails(eventId, onSuccess) {
    var params = {
        "event-id": eventId
    };
    var url = "/api/" + kupBucket + "/getalertdetails";
    return ajaxPost(url, params, onSuccess);
}

function exportAlerts(fileFormat, deviceId, channelId, eventType, from, to, hideResolved) {
    var params = {
        "file-format": fileFormat,
        "time-zone-offset": kupapi.timeZoneOffset,
        "device-id": deviceId,
        "channel-id": channelId,
        "event-type": eventType,
        "from": from,
        "to": to,
        "hide-resolved": hideResolved
    };

    var url = "/api/" + kupBucket + "/exportalerts";
    requestDownloadUrl(url, params);
}

function getAuditLog(sessionKey, bucketName, userName, serviceName, remoteIp, skip, take, start, end, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-name": bucketName,
        "user-name": userName,
        "service-name": serviceName,
        "remote-ip": remoteIp,
        "skip": skip,
        "take": take,
        "from": start,
        "to": end
    };
    var url = "/api/" + kupBucket + "/getauditlog";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getAuditLogDetails(sessionKey, auditId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "audit-id": auditId
    };
    var url = "/api/" + kupBucket + "/getauditlogdetails";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function exportAuditLog(fileFormat, bucketName, userName, serviceName, remoteIp, skip, take, start, end) {
    var params = {
        "file-format": fileFormat,
        "time-zone-offset": kupapi.timeZoneOffset,
        "bucket-name": bucketName,
        "user-name": userName,
        "service-name": serviceName,
        "remote-ip": remoteIp,
        "skip": skip,
        "take": take,
        "from": start,
        "to": end
    };

    var url = "/api/" + kupBucket + "/exportauditlog";
    requestDownloadUrl(url, params);
}

function addSchedulePreset(sessionKey, name, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "name": name,
        "recurrence-rule": JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/addschedulepreset";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getSchedulePresets(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getschedulepresets";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeSchedulePreset(sessionKey, presetId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "preset-id": presetId
    };
    var url = "/api/" + kupBucket + "/removeschedulepreset";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function startRemoteShell(sessionKey, deviceId, host, port, username, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "host": host,
        "port": port,
        "user-name": username
    };
    var url = "/api/" + kupBucket + "/startremoteshell";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function startRemoteShell(sessionKey, deviceId, host, port, username, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "host": host,
        "port": port,
        "user-name": username
    };
    var url = "/api/" + kupBucket + "/startremoteshell";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function stopRemoteShell(sessionKey, deviceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId
    };
    var url = "/api/" + kupBucket + "/stopremoteshell";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getRemoteShellList(sessionKey, bucketId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/getremoteshelllist";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function editNodeCamera(sessionKey, nodeId, updatedCamera, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "node-id": nodeId,
        "node-camera-id": updatedCamera.nodePlatformDeviceId,
        "camera-name": updatedCamera.name,
        "device-key": updatedCamera.deviceKey,
        "host": updatedCamera.host,
        "port": updatedCamera.port,
        "login": updatedCamera.login,
        "password": updatedCamera.password,
        "address": updatedCamera.address,
        "latitude": updatedCamera.latitude,
        "longitude": updatedCamera.longitude,
        "cloudRecordingEnabled": updatedCamera.cloudRecordingEnabled,
        "labels": JSON.stringify(updatedCamera.labels)
    };
    var url = "/api/" + kupBucket + "/editnodecamera";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function startAutoDiscovery(sessionKey, modelId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "model-id": modelId
    };
    var url = "/api/" + kupBucket + "/startautodiscovery";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function stopAutoDiscovery(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/stopautodiscovery";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getDiscoveredDevices(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getdiscovereddevices";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getNodeLicenses(sessionKey, bucketId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/getnodelicenses";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addNodeLicense(sessionKey, bucketId, durationMonths, cloudStorageGb, maxCameraLimit, maxVcaCount, featureNameList, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId,
        "duration-months": durationMonths,
        "cloud-storage-gb": cloudStorageGb,
        "max-camera-limit": maxCameraLimit,
        "max-vca-count": maxVcaCount,
        "features": JSON.stringify(featureNameList)
    };
    var url = "/api/" + kupBucket + "/addnodelicense";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateNodeLicense(sessionKey, licenseNumber, durationMonths, cloudStorageGb, maxCameraLimit, maxVcaCount, featureNameList, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "license-number": licenseNumber,
        "duration-months": durationMonths,
        "cloud-storage-gb": cloudStorageGb,
        "max-camera-limit": maxCameraLimit,
        "max-vca-count": maxVcaCount,
        "features": JSON.stringify(featureNameList)
    };
    var url = "/api/" + kupBucket + "/updatenodelicense";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deleteNodeLicense(sessionKey, licenseNumber, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "license-number": licenseNumber
    };
    var url = "/api/" + kupBucket + "/deletenodelicense";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function suspendNodeLicense(sessionKey, licenseNumber, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "license-number": licenseNumber
    };
    var url = "/api/" + kupBucket + "/suspendnodelicense";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function unsuspendNodeLicense(sessionKey, licenseNumber, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "license-number": licenseNumber
    };
    var url = "/api/" + kupBucket + "/unsuspendnodelicense";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getNodeLicenseLogs(sessionKey, licenseNumber, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "license-number": licenseNumber
    };
    var url = "/api/" + kupBucket + "/getnodelicenselogs";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getNodeLogList(sessionKey, bucketId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/getnodeloglist";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function pullLogFromNode(sessionKey, nodeId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "node-id": nodeId
    };
    var url = "/api/" + kupBucket + "/pullnodelog";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function downloadNodeLogFile(sessionKey, filename) {
    window.location.href = "/api/downloadnodelogfile/" + filename;
}

function getNodeSettings(sessionKey, nodeCloudPlatformId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "node-id": nodeCloudPlatformId
    };
    var url = "/api/" + kupBucket + "/getnodesettings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getNodeCameraList(sessionKey, nodeCloudPlatformId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "node-id": nodeCloudPlatformId
    };
    var url = "/api/" + kupBucket + "/getnodecameralist";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getNodeAnalyticsList(sessionKey, nodeCloudPlatformId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "node-id": nodeCloudPlatformId
    };
    var url = "/api/" + kupBucket + "/getnodeanalyticslist";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getAnalyticsReport(sessionKey, eventType, deviceGroups, deviceId, channelId, from, to, parameters, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "event-type": eventType,
        "device-groups" : deviceGroups? JSON.stringify(deviceGroups) : "",
        "device-id": deviceId,
        "channel-id": channelId,
        "from": from,
        "to": to,
        "parameters": parameters? JSON.stringify(parameters) : ""
    }
    var url = "/api/" + kupBucket + "/getanalyticsreport";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function exportProfilingCrossSiteXls(sessionKey, deviceId, channelId, fromDate, toDate, baseUnit, reportInfo, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId.toString(),
        "channel-id": channelId.toString(),
        "time-zone-offset": kupapi.timeZoneOffset,
        "from": fromDate,
        "to": toDate,
        "base-unit": baseUnit,
        "report-info": reportInfo
    };
    var url = "/api/" + kupBucket + "/exportprofilingcrosssitexls";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getPosSalesReport(sessionKey, from, to, name, parserType, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "from": from,
        "to": to,
        "name": name,
        "parser-type": parserType
    }
    var url = "/api/" + kupBucket + "/getpossalesreport";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function listPosNames(sessionKey, parserType, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "parser-type": parserType
    }
    var url = "/api/" + kupBucket + "/listposnames";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBucketDevicesByBucketId(bucketId, onSuccess, onFailure) {
    var params = {
        "bucket-id": bucketId
    }
    var url = "/api/" + kupBucket + "/getbucketdevicesbybucketid";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function exportLicenseList(fileFormat, bucketId, status, registrationNumber, deviceName) {
    var params = {
        "file-format": fileFormat,
        "bucket-id": bucketId,
        "status": status,
        "registration-number": registrationNumber,
        "device-name": deviceName
    };

    var url = "/api/" + kupBucket + "/exportlicenselist";
    requestDownloadUrl(url, params);
}

function getBucketUsersByBucketId(bucketId, onSuccess, onFailure) {
    var params = {
        "bucketid": bucketId
    };
    var url = "/api/" + kupBucket + "/getbucketusersbybucketid";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function exportUsersFileByBucketId(bucketId, fileFormat) {
    var params = {
        "bucket-id": bucketId,
        "file-format": fileFormat
    };

    var url = "/api/" + kupBucket + "/exportusersfilebybucketid";
    requestDownloadUrl(url, params);
}

function exportNodesFileByBucketId(bucketId, fileFormat) {
    var params = {
        "bucket-id": bucketId,
        "file-format": fileFormat,
        "time-zone-offset": kupapi.timeZoneOffset
    };

    var url = "/api/" + kupBucket + "/exportnodesbybucketid";
    requestDownloadUrl(url, params);
}

function reverseGeocode(address, onSuccess, onFailure) {
    var params = {
        "address": address
    };
    var url = "/api/reversegeocode";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updatePOSSalesData(sessionKey, params, POSJSONString, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "POSName": params.siteName,
        "from": params.startDateStr,
        "to": params.endDateStr,
        "POSData": POSJSONString
    }
    var url = "/api/" + kupBucket + "/updatepossalesdata";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getNetworkStatus(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    }
    var url = "/nodeapi/getnetworkstatus";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function listAnalyticsByBucketId(sessionKey, bucketId, analyticsType, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId,
        "analytics-type": analyticsType
    }
    var url = "/api/" + kupBucket + "/listanalyticsbybucketid";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBucketSetting(bucketId, onSuccess, onFailure) {
    var params = {
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/getbucketsetting";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function saveReportQueryHistory(eventType, dateFrom, dateTo, deviceSelected, onSuccess, onFailure) {
    var params = {
        "event-type": eventType,
        "date-from": dateFrom,
        "date-to": dateTo,
        "device-selected": deviceSelected
    };
    var url = "/api/" + kupBucket + "/savereportqueryhistory";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getReportQueryHistory(eventType, onSuccess, onFailure) {
    var params = {
        "event-type": eventType
    };
    var url = "/api/" + kupBucket + "/getreportqueryhistory";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getVcaErrors(instanceId, offset, take, onSuccess, onFailure) {
    var params = {
        "instance-id": instanceId,
        "offset": offset,
        "take": take
    };
    var url = "/api/" + kupBucket + "/getvcaerrors";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function searchCloudRecordings(deviceId, channelId, from, to, onSuccess, onFailure) {
    var params = {
        "device-id": deviceId,
        "channel-id": channelId,
        "from": from,
        "to": to
    };
    var url = "/api/" + kupBucket + "/searchcloudrecordings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function requestCloudRecordings(deviceId, channelId, from, to, onSuccess, onFailure) {
    var params = {
        "device-id": deviceId,
        "channel-id": channelId,
        "from": from,
        "to": to
    };
    var url = "/api/" + kupBucket + "/requestcloudrecordings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deleteCloudRecordings(deviceId, channelId, from, to, onSuccess, onFailure) {
    var params = {
        "device-id": deviceId,
        "channel-id": channelId,
        "from": from,
        "to": to
    };
    var url = "/api/" + kupBucket + "/deletecloudrecordings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function findPendingUploadRequests(deviceId, channelId, from, to, onSuccess, onFailure) {
    var params = {
        "device-id": deviceId,
        "channel-id": channelId,
        "from": from,
        "to": to
    };
    var url = "/api/" + kupBucket + "/findpendinguploadrequests";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getRecordingUploadRequests(deviceId, channelId, onSuccess, onFailure) {
    var params = {
        "device-id": deviceId,
        "channel-id": channelId
    };
    var url = "/api/" + kupBucket + "/getrecordinguploadrequests";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function exportAggregatedCSVReport(eventType, selectedGroups, fromDate, toDate, baseUnit, onSuccess, onFailure) {
    var params = {
        "event-type": eventType,
        "selected-groups": selectedGroups,
        "time-zone-offset": kupapi.timeZoneOffset,
        "from": fromDate,
        "to": toDate,
        "base-unit": baseUnit
    };
    var url = "/api/" + kupBucket + "/exportaggregatedcsvreport";
    requestDownloadUrl(url, params, onSuccess, onFailure);
}

function getDeviceLogs(platformDeviceId, skip, take, onSuccess, onFailure) {
    var params = {
        "platform-device-id": platformDeviceId,
        "skip": skip,
        "take": take
    };
    var url = "/api/" + kupBucket + "/getdevicelogs";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getBucketNotificationSettings(bucketId, onSuccess, onFailure) {
    var params = {
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/getbucketnotificationsettings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateBucketNotificationSettings(bucketId, eventType, notificationEnabled, videoRequired, onSuccess, onFailure) {
    var params = {
        "bucket-id": bucketId,
        "event-type": eventType,
        "notification-enabled": notificationEnabled,
        "video-required": videoRequired
    };

    var url = "/api/" + kupBucket + "/updatebucketnotificationsettings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function restoreBucketNotificationSettings(bucketId, onSuccess, onFailure) {
    var params = {
        "bucket-id": bucketId
    };

    var url = "/api/" + kupBucket + "/restorebucketnotificationsettings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getUserNotificationSettings(onSuccess, onFailure) {
    var params = {};

    var url = "/api/" + kupBucket + "/getusernotificationsettings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateUserNotificationSettings(eventType, notifyMethods, onSuccess, onFailure) {
    var params = {
        "event-type": eventType,
        "notify-methods": JSON.stringify(notifyMethods)
    };

    var url = "/api/" + kupBucket + "/updateusernotificationsettings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getAllowedNotifyMethods(onSuccess, onFailure) {
    var params = {};

    var url = "/api/" + kupBucket + "/getallowednotifymethods";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getAssignableNodeFeatures(bucketId, onSuccess, onFailure) {
    var params = {
        "bucket-id": bucketId
    };

    var url = "/api/" + kupBucket + "/getassignablenodefeatures";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getPOSSettings(onSuccess, onFailure) {
    var params = {};

    var url = "/api/" + kupBucket + "/getpossettings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updatePOSSettings(updatedSettings, onSuccess, onFailure) {
    var params = {
        "import-enabled": updatedSettings.enabled,
        "ftp-details": JSON.stringify(updatedSettings.ftpDetails)
    };

    var url = "/api/" + kupBucket + "/updatepossettings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addholiday(title, des, isEvent, isHoliday, isSignificant, from, to, countries, onSuccess, onFailure) {
    var params = {
        "title": title,
        "des": des,
        "isEvent": isEvent,
        "isHoliday": isHoliday,
        "isSignificant": isSignificant,
        "from": from,
        "to": to,
        "countries": countries
    }
    var url = "/api/" + kupBucket + "/addholiday";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getHolidays(sessionKey, onSuccess, onFailure) {
    var params = {
        "sessionKey": sessionKey
    }
    var url = "/api/" + kupBucket + "/getholidays";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getCountryList(sessionKey, onSuccess, onFailure) {
    var params = {
        "sessionKey": sessionKey
    }
    var url = "/api/" + kupBucket + "/getcountrylist";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deleteHoliday(holidayId, onSuccess, onFailure) {
    var params = {
        "holidayId": holidayId
    }
    var url = "/api/" + kupBucket + "/deleteholiday";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function editHoliday(id, title, des, isEvent, isHoliday, isSignificant, from, to, countries, onSuccess, onFailure) {
    var params = {
        "id": id,
        "title": title,
        "des": des,
        "isEvent": isEvent,
        "isHoliday": isHoliday,
        "isSignificant": isSignificant,
        "from": from,
        "to": to,
        "countries": countries
    }
    var url = "/api/" + kupBucket + "/updateholiday";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getRecordedFileList(deviceId, channelId, from, to, onSuccess, onFailure) {
    var params = {
        "device-id": deviceId,
        "channel-id": channelId,
        "from": from,
        "to": to
    };
    var url = "/api/" + kupBucket + "/getrecordedfilelist";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function downloadZippedRecordings(deviceId, channelId, from, to) {
    var params = {
        "device-id": deviceId,
        "channel-id": channelId,
        "from": from,
        "to": to
    };

    sendGETRequest(params, "downloadzippedrecordings");
}

function getUSBDrives(onSuccess, onFailure) {
    var params = {};
    var url = "/api/" + kupBucket + "/getusbdrives";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function exportRecordingsToUSB(deviceId, channelId, from, to, usbIdentifier, onSuccess, onFailure) {
    var params = {
        "device-id": deviceId,
        "channel-id": channelId,
        "from": from,
        "to": to,
        "usb-identifier": usbIdentifier
    };
    var url = "/api/" + kupBucket + "/usbexportrecordings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getPlatformInformation(onSuccess, onFailure) {
    var params = {};
    var url = "/api/" + kupBucket + "/getplatforminformation";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getAssignableRoleFeatures(onSuccess) {
    var params = {};
    var url = "/api/" + kupBucket + "/getassignablerolefeatures";
    return ajaxPost(url, params, onSuccess);
}

function getNodeInfoOnCloud(nodeId, onSuccess) {
    var params = {
        "platform-device-id" : nodeId
    };
    var url = "/api/" + kupBucket + "/getnodeinfooncloud";
    return ajaxPost(url, params, onSuccess);
}

function getNodeLocalInfo(onSuccess) {
    var params = {
    };
    var url = "/nodeapi/getnodeinfo";
    return ajaxPost(url, params, onSuccess);
}

function updateMobileDeviceInfo(identifier, newName, onSuccess) {
    var params = {
        "identifier" : identifier,
        "new-name" : newName
    };
    var url = "/api/" + kupBucket + "/updatemobiledeviceinfo";
    return ajaxPost(url, params, onSuccess);
}

function getNodeCameraStorage(nodeId, cameraCoreId, onSuccess) {
    var params = {
        "device-id" : nodeId,
        "channel-id" : cameraCoreId
    };
    var url = "/api/" + kupBucket + "/getnodecamerastorage";
    return ajaxPost(url, params, onSuccess);
}

function getVcaConcurrencyStatus(nodeId, minimumConcurrency, onSuccess) {
    var params = {
        "node-id" : nodeId,
        "minimum-concurrency" : minimumConcurrency
    };
    var url = "/api/" + kupBucket + "/getvcaconcurrencystatus";
    return ajaxPost(url, params, onSuccess);
}

function getServerConfigurations(onSuccess) {
    var params = {
    };
    var url = "/api/" + kupBucket + "/getserverconfigurations";
    return ajaxPost(url, params, onSuccess);
}

function updateServerConfigurations(updatedConfigs, onSuccess) {
    var params = {
        "server-configs" : JSON.stringify(updatedConfigs)
    };
    var url = "/api/" + kupBucket + "/updateserverconfigurations";
    return ajaxPost(url, params, onSuccess);
}

function getLabels(onSuccess)
{
    var params = {
    };
    var url = "/api/" + kupBucket + "/getlabels";
    return ajaxPost(url, params, onSuccess);
}

function getUserAccessibleLabels(onSuccess)
{
    var params = {
    };
    var url = "/api/" + kupBucket + "/getuseraccessiblelabels";
    return ajaxPost(url, params, onSuccess);
}

function getLabelsByBucketId(bucketId, onSuccess)
{
    var params = {
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/getlabelsbybucketid";
    return ajaxPost(url, params, onSuccess);
}

function getLabelOccupancySettings(labelId, onSuccess)
{
    var params = {
       "label-id": labelId
    };
    var url = "/api/" + kupBucket + "/getlabeloccupancysettings";
    return ajaxPost(url, params, onSuccess);
}

function updateLabelOccupancySettings(labelId, enabled, occpancyLimits, minNotifyInterval, onSuccess)
{
    var params = {
        "label-id": labelId,
        "enabled" : enabled,
        "occupancy-limits" : JSON.stringify(occpancyLimits),
        "min-notify-interval" : minNotifyInterval
    };
    var url = "/api/" + kupBucket + "/updatelabeloccupancysettings";
    return ajaxPost(url, params, onSuccess);
}

function getLabelNotifications(eventId, from, to, eventType, labelId, skip, take, hideResolved, onSuccess)
{
    var params = {
        "event-id" : eventId,
        "from" : from,
        "to" : to,
        "event-type": eventType,
        "label-id" : labelId,
        "skip" : skip,
        "take" : take,
        "hide-resolved" : hideResolved
    };
    var url = "/api/" + kupBucket + "/getlabelnotifications";
    return ajaxPost(url, params, onSuccess);
}

function getBucketPasswordPolicy(bucketId, onSuccess, onFailure) {
    var params = {
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/getbucketpasswordpolicy";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateBucketPasswordPolicy(bucketId, passwordPolicy, onSuccess, onFailure) {
    var params = {
        "bucket-id": bucketId,
        "required-uppercase": passwordPolicy.uppercaseRequired,
        "required-lowercase": passwordPolicy.lowercaseRequired,
        "required-numeric": passwordPolicy.numbericRequired,
        "required-special-character": passwordPolicy.specialCharRequired,
        "enabled-password-expiration": passwordPolicy.enablePassExpiration,
        "prevented-password-reuse": passwordPolicy.enablePassReuseCheck,
        "email-when-password-expired": passwordPolicy.emailWhenPasswordExpired,
        "minimum-password-length": passwordPolicy.minPassLength,
        "password-expiration-days": passwordPolicy.passExpirationDays,
        "number-of-reuse-password-prevention": passwordPolicy.passReuseCheckTimes,
        "required-first-login-password-change": passwordPolicy.requiredfirstLoginPasswordChange
    };
    var url = "/api/" + kupBucket + "/updatebucketpasswordpolicy";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function acknowledgeNotification(eventId, message, onSuccess, onFailure)
{
    var params = {
        "event-id": eventId,
        "message" : message
    };
    var url = "/api/" + kupBucket + "/acknowledgenotification";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function listNodeUpdateSchedules(from, to, onSuccess, onFailure)
{
    var params = {
        "from": from,
        "to" : to
    };
    var url = "/api/" + kupBucket + "/listnodeupdateschedules";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function scheduleNodeUpdates(scheduledTime, nodeList, onSuccess, onFailure)
{
    var params = {
        "scheduled-time": scheduledTime,
        "node-list": JSON.stringify(nodeList)
    };
    var url = "/api/" + kupBucket + "/schedulenodeupdates";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deleteNodeUpdateSchedule(scheduleId, onSuccess, onFailure)
{
    var params = {
        "schedule-id": scheduleId
    };
    var url = "/api/" + kupBucket + "/deletenodeupdateschedule";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getNodeUpdateSchedule(scheduleId, onSuccess, onFailure)
{
    var params = {
        "schedule-id": scheduleId
    };
    var url = "/api/" + kupBucket + "/getnodeupdateschedule";
    return ajaxPost(url, params, onSuccess, onFailure);
}

kupapi.addVca = function(vcaTypeName,
                         deviceId,
                         channelId,
                         thresholds,
                         recurrenceRule,
                         program,
                         onSuccess,
                         onFailure) {
	console.info("vcaTypeName:"+vcaTypeName);
	console.info("program:"+program);
    var params = {
        "type": vcaTypeName,
        "device-id": deviceId,
        "channel-id": channelId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule),
        "program" : program
    };
    var url = "/api/" + kupBucket + "/addvca";
    return ajaxPost(url, params, onSuccess, onFailure);
};

kupapi.updateVca = function(instanceId,
                            thresholds,
                            recurrenceRule,
                            onSuccess,
                            onFailure) {
    var params = {
        "instance-id": instanceId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/updatevca";
    return ajaxPost(url, params, onSuccess, onFailure);
};

kupapi.activateVca = function(instanceId, onSuccess, onFailure) {
    var params = {
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/activatevca";
    return ajaxPost(url, params, onSuccess, onFailure);
};

kupapi.deactivateVca = function(instanceId, onSuccess, onFailure) {
    var params = {
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/deactivatevca";
    return ajaxPost(url, params, onSuccess, onFailure);
};

kupapi.removeVca = function(instanceId, onSuccess, onFailure) {
    var params = {
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/removevca";
    return ajaxPost(url, params, onSuccess, onFailure);
};