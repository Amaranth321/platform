/**
 * Improved version of the previous manager.device.js.
 * ready() must be called before using any of the functions
 *
 * @author Aye Maung
 *
 */
var DvcMgr = (function ()
{
    var cfg = {
        cacheExpiry: 30 * 1000
    };

    var DeviceStatus = {
        UNKNOWN: "UNKNOWN",
        CONNECTED: "CONNECTED",
        DISCONNECTED: "DISCONNECTED",

        parse: function (statusString)
        {
            var status = DeviceStatus.UNKNOWN;
            $.each(DeviceStatus, function (key, value)
            {
                if (value == statusString)
                {
                    status = DeviceStatus[key];
                    return false;
                }
            });

            return status;
        }
    };

    var cache = {
        lastRequested: 0,       //updated time
        userDevices: {},        //key is coreDeviceId, value is device obj
        nodeObjectMap: {},      //key is coreDeviceId, value is nodeObject
        platformIdMap: {},      //key is platformDeviceId, value is coreDeviceId
        coreIdMap: {},          //key is coreDeviceId, value is platformDeviceId
        deviceNameMap: {},      //key is coreDeviceId, value is device name
        channelNameMap: {},     //key is id pair, value is channel name
        channelStatusMap: {},   //key is id pair, value is channel camera status
        snapshots: {}           //key is id pair, value is snapshot url
    };

    var updateDeviceCache = function (deviceList)
    {
        cache.lastRequested = new Date().getTime();
        cache.userDevices = {};
        cache.nodeObjectMap = {};
        cache.platformIdMap = {};
        cache.coreIdMap = {};
        cache.deviceNameMap = {};
        cache.channelNameMap = {};
        $.each(deviceList, function (i, dvc)
        {
            cache.userDevices[dvc.deviceId] = dvc;
            cache.platformIdMap[dvc.id] = dvc.deviceId;
            cache.coreIdMap[dvc.deviceId] = dvc.id;
            cache.deviceNameMap[dvc.deviceId] = dvc.name;

            if (isKaiNode(dvc))
            {
                cache.nodeObjectMap[dvc.deviceId] = dvc.node;
                $.each(dvc.node.cameras || [], function (i2, cam)
                {
                    var idPairKey = composeIdPairKey(dvc.deviceId, cam.nodeCoreDeviceId);
                    cache.channelNameMap[idPairKey] = cam.name;
                    cache.channelStatusMap[idPairKey] = cam.status;
                });
            }
            else
            {
                var idPairKey = composeIdPairKey(dvc.deviceId, 0);
                cache.channelNameMap[idPairKey] = "1";
                cache.channelStatusMap[idPairKey] = dvc.status;
            }
        });
    };


    var init = function (callback, forceRefresh)
    {
        if (!forceRefresh)
        {
            var now = new Date().getTime();
            if ((now - cache.lastRequested) < cfg.cacheExpiry)
            {
                callback();
                return;
            }
        }

        getUserDevices("", function (responseData)
        {
            if (responseData.result != "ok" || responseData.devices == null)
            {
                utils.throwServerError(responseData);
                updateDeviceCache([]);
                callback();
                return;
            }

            updateDeviceCache(responseData.devices);
            callback();
        });
    };

    var composeIdPairKey = function (coreDeviceId, channelId)
    {
        return coreDeviceId + "_" + channelId;
    };

    var isKaiNode = function (device)
    {
        return (device.model.capabilities.indexOf("node") != -1);
    };

    var toPlatformId = function (coreDeviceId)
    {
        return cache.coreIdMap[coreDeviceId];
    };

    var toCoreId = function (platformDeviceId)
    {
        return cache.platformIdMap[platformDeviceId];
    };

    var getUserDeviceList = function ()
    {
        return _.values(cache.userDevices);
    };

    var getNodeObject = function (coreDeviceId)
    {
        return cache.nodeObjectMap[coreDeviceId];
    };

    var getDevice = function (coreDeviceId)
    {
        return cache.userDevices[coreDeviceId];
    };

    var getDeviceName = function (coreDeviceId)
    {
        var name = cache.deviceNameMap[coreDeviceId];
        return utils.isNullOrEmpty(name) ? localizeResource("deleted-db-entry") : name;
    };

    var getChannelName = function (coreDeviceId, channelId)
    {
        var name = cache.channelNameMap[composeIdPairKey(coreDeviceId, channelId)];
        return utils.isNullOrEmpty(name) ? localizeResource("deleted-db-entry") : name;
    };

    var getDeviceStatus = function (coreDeviceId)
    {
        var device = cache.userDevices[coreDeviceId];
        return device == null ? DeviceStatus.UNKNOWN : DeviceStatus.parse(device.status);
    };

    var getChannelStatus = function (coreDeviceId, channelId)
    {
        var status = cache.channelStatusMap[composeIdPairKey(coreDeviceId, channelId)];
        return utils.isNullOrEmpty(status) ? DeviceStatus.UNKNOWN : DeviceStatus.parse(status);
    };

    var getCameraSnapshot = function (coreDeviceId, channelId, callback)
    {
        var cacheKey = coreDeviceId + "-" + channelId;
        if (cache.snapshots[cacheKey])
        {
            callback(cache.snapshots[cacheKey]);
            return;
        }

        getLiveVideoUrl("", coreDeviceId, channelId, "http/jpeg", 900, function (responseData)
        {
            if (responseData.result == "ok" &&
                responseData.url != null &&
                responseData.url.length > 0)
            {

                var jpegUrl = responseData.url[0];
                cache.snapshots[cacheKey] = jpegUrl;
                callback(jpegUrl);
            }
            else
            {
                callback(null);
            }
        }, null);
    }

    var viewNodeLocalInfo = function ()
    {
        var contentPage = "/node/localinfo";
        utils.openPopup(localizeResource('node-information'), contentPage, null, null, true, function ()
        {
        });
    };

    var getModelIdList = function (nodeType)
    {
        var info = backend.NodeEnvInfo[nodeType];
        return info ? info.modelIdList : [];
    };

    return {
        //fields
        DeviceStatus: DeviceStatus,
        NodeType: backend.NodeEnv,

        //functions
        ready: init,
        composeIdPairKey: composeIdPairKey,
        isKaiNode: isKaiNode,
        toPlatformId: toPlatformId,
        toCoreId: toCoreId,
        getUserDeviceList: getUserDeviceList,
        getNodeObject: getNodeObject,
        getDevice: getDevice,
        getDeviceName: getDeviceName,
        getChannelName: getChannelName,
        getDeviceStatus: getDeviceStatus,
        getChannelStatus: getChannelStatus,
        getCameraSnapshot: getCameraSnapshot,
        viewNodeLocalInfo: viewNodeLocalInfo,
        getModelIdList: getModelIdList
    }
})();