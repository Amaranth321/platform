var deviceManager = {};
deviceManager.userDevices = [];
deviceManager.userDevicesLabels = [];
deviceManager.userDevicesAndCamerasLabels = [];

deviceManager.labelDeviceDetails = {};
var deviceLength = -1;
var count = 0;
var flagCount = false;


//this will always run on page load
deviceManager.initialize = function () {
    getUserDevices("", function (responseData) {
        deviceManager.userDevices = [];
        if (responseData.result == "ok" && responseData.devices != null) {
            count = 0;
            deviceLength = responseData.devices.length;
            $.each(responseData.devices, function (index, dvc) {
                if (utils.checkDeviceCompleteInfo(dvc)) {
                    deviceManager.userDevices.push(dvc);
                }
                count++;
            });

        }else{
             flagCount = true;
        }
    }, null);
}

deviceManager.WaitForReady = function (callback) {
    utils.showLoadingOverlay();
    if (deviceManager.userDevices != null) {
        utils.hideLoadingOverlay();
        if(count == deviceLength){
            callback();
            return;
        }else if(flagCount){
            callback();
            return;
        }
    }
    
    console.log("Waiting for deviceManager to be ready ... processed device :: " + count + " :: total device :: " + deviceLength);
    setTimeout(function () {
        deviceManager.WaitForReady(callback);
    }, 200);
}

//Providing either platformDeviceId or coreDeviceId is enough
deviceManager.attachDeviceDetails = function (targetObj, platformDeviceId, coreDeviceId, channelId) {

    if (deviceManager.userDevices == null) {
        console.log("deviceManager.userDevices is not ready");
        return;
    }
    targetObj.deviceName = localizeResource("n/a");
    targetObj.channelName = localizeResource("n/a");
    targetObj.nodeVersion = -1;  //not node

    $.each(deviceManager.userDevices, function (index, dvc) {
        if (dvc.id == platformDeviceId || dvc.deviceId == coreDeviceId) {
            targetObj.deviceName = dvc.name;
            targetObj.address = dvc.address;
            targetObj.latitude = dvc.latitude;
            targetObj.longitude = dvc.longitude;
            targetObj.coreDeviceId = dvc.deviceId;
            targetObj.groupType = "type-running-on-" + kupapi.applicationType;
            targetObj.deviceStatus = dvc.status;

            if (channelId == null || channelId === "") {
                targetObj.channelName = "";
            }
            else if (kupapi.applicationType == "node" || dvc.model.capabilities.indexOf("node") == -1) {
                targetObj.channelName = parseInt(channelId) + 1 + "";
            }
            //for Kai Nodes
            else {
                $.each(dvc.node.cameras, function (index, cameraItem) {
                    if (cameraItem.nodeCoreDeviceId == channelId) {
                        targetObj.channelName = cameraItem.name;
                        targetObj.groupType = "type-running-on-node";
                        return false;
                    }
                });

                if (targetObj.channelName == null) {
                    console.log(targetObj.deviceName + " :node camera not found nodeCoreDeviceId=" + channelId);
                    targetObj.channelName = parseInt(channelId) + 1 + "";
                    targetObj.groupType = "unknown-type";
                }

                targetObj.nodeVersion = dvc.node.version;
            }

            return false;
        }
    });

    return targetObj;
}

//Legacy function. Deprecated. Use utils.getLatLngByAddress
deviceManager.devicePageGeocode = function (request, containers) {
    kendo.ui.progress($('.progress'), true);
    utils.getLatLngByAddress(request.address,
        function (latlng) {
            containers.lat.data("kendoNumericTextBox").value(latlng.lat);
            containers.lng.data("kendoNumericTextBox").value(latlng.lng);
            containers.errorBox.hide();
            kendo.ui.progress($('.progress'), false);
        },
        function (error) {
            console.log(error);
            containers.lat.data("kendoNumericTextBox").value(0);
            containers.lng.data("kendoNumericTextBox").value(0);
            containers.errorBox.show();
            kendo.ui.progress($('.progress'), false);
        });
}

deviceManager.isKaiNode = function (device){
    try {
        return (device.model.capabilities.indexOf("node") != -1);
    } catch (e){
        console.error(e);
        return false;
    }
}