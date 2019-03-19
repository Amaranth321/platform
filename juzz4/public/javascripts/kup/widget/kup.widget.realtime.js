/**
 * [Javascript Module Pattern]
 * widget for realtime
 * @param  {object} $   plug-in:jQuery
 * @param  {object} kup KUP module
 * @return {object}     public member
 * @author keith.chong@kaisquare.com
 */

KUP.widget.realtime = (function($, kup) {
    var _option = {
            //current vca info
            monitorType: '', //current monitor page          
            menuTabType: '#currentOccupancyTab', //if url not to assign report type
            //for check status
            isSuccessReport: false, //true is success generate reports           
            isMultiDeviceSelected: false, // true if label containing multiple device is selected and disable excel
            isDragging: false,
            isDefaultDropData: false,//default true
            isUpdateSelectedItem: false,
            isOnlyShowVcaTreeView: false,
            defaultElementList: {},
            defaultItemDataList: {},
            //for save selected item info
            selectedElementList: {},
            selectedItemDataList: {},
            selectedSaveDataList: [],
            selectedGroupNames: [],
            //for other vca report
            selectedDeviceList: [],
            selectedChannelList: [],
            selectedInstance: [],
            selectedDevices: [],
            //from api reponse data
            apiUserDevicesList: [],
            apiUserLabelList: [],
            apiRunningAnalyticsList: [],
            apiQueryHistoryData: {},
            //other info 
            treeItems: [],
            groupNames: [], //for only select label
            showVcaTreeItemsList: [],
            intervalCache: null
        },
        _map = {
    		monitorType: ['currentoccupancy'],
            vcaEventType: {
            	currentoccupancy: kup.getOpt('event').PEOPLE_COUNTING
            },
            analyticsType: {
            	currentoccupancy: kup.getOpt('analyticsType').PEOPLE_COUNTING
            },
            queryType: {
            	currentoccupancy: 'current-occupancy'
            },
            menuTab: {
            	currentoccupancy: '#currentOccupancyTab'
            },
            title: {
            	currentoccupancy: 'current-occupancy'
            },
            successReportToShowDiv: {
            	currentoccupancy: ['.occupancy_wrapper']
            }
        },
        _self = {
            type: {},
            setOpt: function(config) {
                _option = $.extend(false, {}, _option, config || {});
            },
            getOpt: function(key) {
                var deepCopy = $.extend(true, {}, _option);
                return (!!key) ? deepCopy[key] : deepCopy;
            },
            getMap: function(key) {
                var deepCopy = $.extend(true, {}, _map);
                return (!!key) ? deepCopy[key] : deepCopy;
            },
            getData: {
                selectedItemsLength: function() {
                    var opt = _self.getOpt(),
                        count = 0,
                        selectedItemDataList = opt.selectedItemDataList;
                    $.each(selectedItemDataList, function(uid, itemData) {
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
                        count++;
                    });
                    return count;
                }
            },
            setData: {
                selectedInstance: function() {

                    var opt = _self.getOpt(),
                        selectedItemDataList = opt.selectedItemDataList;
                    opt.selectedInstance = [];
                    $.each(selectedItemDataList, function(uid, itemData) {
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
                        if (itemData.isAll || itemData.isLabel) { //drag root or labels
                            var device = itemData.items || [];
                            $.each(device, function(i, deviceData) {
                                var camera = deviceData.items || [];
                                $.each(camera, function(j, cameraData) {
                                    opt.selectedInstance.push(cameraData.data);
                                });
                            });
                        } else if (itemData.isDevice) { //drag device
                            var camera = itemData.items || [];
                            $.each(camera, function(i, cameraData) {
                                opt.selectedInstance.push(cameraData.data);
                            });
                        } else if (itemData.isCamera) { //drag camera
                            if (itemData.hasChildren) {
                                return;
                            }
                            var cameraData = itemData;
                            opt.selectedInstance.push(cameraData.data);
                        }
                    });
                    _self.setOpt(opt);
                    return opt.selectedInstance;
                },
                selectedSaveDataList: function() {
                    var opt = _self.getOpt(),
                        selectedItemDataList = opt.selectedItemDataList;

                    opt.selectedSaveDataList = [];
                    $.each(selectedItemDataList, function(uid, itemData) {
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
                        if (itemData.isAll || itemData.isLabel) { //drag root or labels
                            opt.selectedSaveDataList.push({
                                label: itemData.text,
                                deviceId: null,
                                channelId: null
                            });
                        } else if (itemData.isDevice) { //drag device
                            opt.selectedSaveDataList.push({
                                label: itemData.parentName,
                                deviceId: itemData.deviceId,
                                channelId: null
                            });
                        } else if (itemData.isCamera) { //drag camera
                            if (itemData.hasChildren) {
                                return;
                            }
                            opt.selectedSaveDataList.push({
                                label: itemData.group,
                                deviceId: itemData.id,
                                channelId: itemData.cameras.nodeCoreDeviceId
                            });
                        }
                    });
                    _self.setOpt(opt);
                    return opt.selectedSaveDataList;
                },
                selectedDevices: function() {
                    var opt = _self.getOpt(),
                        selectedInstance = opt.selectedInstance,
                        deviceIds = [],
                        channelIds = [],
                        selectedGroup = {};

                    opt.selectedDevices = [];
                    $.each(selectedInstance, function(index, inst) {
                        deviceIds.push(inst.platformDeviceId);
                        channelIds.push(inst.channelId);
                    });
                    selectedGroup.platformDeviceId = deviceIds;
                    selectedGroup.channelId = channelIds;
                    opt.selectedDevices.push(selectedGroup);

                    _self.setOpt(opt);
                    return opt.selectedDevices;
                },
                groupNames: function() {
                    var opt = _self.getOpt(),
                        selectedItemDataList = opt.selectedItemDataList,
                        groupNames = [];

                    $.each(selectedItemDataList, function(uid, itemData) {
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
                        if (!itemData.isLabel) {
                            groupNames = [];
                            return false;
                        }
                        groupNames.push(itemData.text);
                    });
                    opt.groupNames = groupNames;
                    _self.setOpt(opt);
                    return opt.groupNames;
                },
                defaultItemDataList: function() {
                    var opt = _self.getOpt(),
                        itemDataQuery = opt.apiQueryHistoryData.deviceSelected || [],
                        itemData = $("#deviceTree").data('kendoTreeView').dataSource.data();
                    opt.defaultItemDataList = {};
                    $.each(itemDataQuery, function(i, queryData) {
                        if (queryData.label !== "" && queryData.deviceId !== "" && queryData.channelId !== "") { // is camera
                            $.each(itemData, function(j, labelData) {
                                if (labelData.text === queryData.label) {
                                    $.each(labelData.items, function(k, deviceData) {
                                        if (deviceData.deviceId === parseInt(queryData.deviceId, 10)) {
                                            var uid = "";
                                            $.each(deviceData.items, function(l, cameraData) {
                                                if (parseInt(cameraData.cameras.nodeCoreDeviceId, 10) === parseInt(queryData.channelId, 10)) {
                                                    uid = cameraData.uid;
                                                    opt.defaultItemDataList[uid] = cameraData;
                                                    return false;
                                                }
                                            });
                                            return false;
                                        }
                                    });
                                    return false;
                                }
                            });
                        } else if (queryData.deviceId !== "") { //is device
                            $.each(itemData, function(j, labelData) {
                                if (labelData.text === queryData.label) {
                                    $.each(labelData.items, function(k, deviceData) {
                                        if (deviceData.deviceId === parseInt(queryData.deviceId, 10)) {
                                            var uid = deviceData.uid;
                                            opt.defaultItemDataList[uid] = deviceData;
                                            return false;
                                        }
                                    });
                                    return false;
                                }
                            });
                        } else {
                            $.each(itemData, function(j, labelData) { //is root or label
                                if (labelData.text === queryData.label) {
                                    var uid = labelData.uid;
                                    opt.defaultItemDataList[uid] = labelData;
                                    return false;
                                }
                            });
                        }
                    });
                    _self.setOpt(opt);
                    return opt.defaultItemDataList;
                },
                defaultElementList: function() {
                    var opt = _self.getOpt(),
                        defaultItemDataList = opt.defaultItemDataList;

                    opt.defaultElementList = {};
                    $.each(defaultItemDataList, function(i, data) {
                        var uid = data.uid;
                        opt.defaultElementList[uid] = $("li[data-uid='" + uid + "']").get(0);
                    });
                    _self.setOpt(opt);
                    return opt.defaultElementList;
                },
                treeItems: function() {
                    var opt = _self.getOpt(),
                        deviceList = opt.apiUserDevicesList,
                        labelList = opt.apiUserLabelList,
                        kupOpt = kup.getOpt(),
                        utils = kup.utils.default,
                        i18n = kup.utils.i18n;

                    var deviceGroups = {},
                        labelGroups = {},
                        randomDeviceKey = 0,
                        labels = [],
                        all = i18n('all'); //Unlabelled.
                    
                    var setBasicDevice = function(label, device) {
                        var labelId = label.labelId;
                        var labelName = label.name;
                        var deviceId = device.deviceId;
                        var deviceName = device.name;

                        var deviceData = {
                            isDevice: true,
                            isOnline: (device.status !== KupEvent.DEVICE_DISCONNECTED) ? true : false,
                            labelId: labelId,
                            labelName: labelName,
                            parentName: labelName,
                            deviceId: deviceId,
                            deviceName: deviceName,
                            text: deviceName,
                            data: {
                                capabilities: device.model.capabilities.split(" "),
                            },
                            items: [],
                        };
                        if(!deviceData.isOnline){
                        	deviceData.imageUrl = kupapi.CdnPath + "/common/images/device_offline.png";
                        }
                        return deviceData;
                    };
                    
                    var setBasicCamera = function(label, device, camera) {
                        var labelId = label.labelId;
                        var labelName = label.name;
                        var deviceId = device.deviceId;
                        var platformDeviceId = device.id;
                        var deviceName = device.name;
                        var cameraId = camera.nodeCoreDeviceId;
                        var cameraName = camera.name;

                        var cameraData = {
                            isCamera: true,
                            labelId: labelId,
                            labelName: labelName,
                            deviceId: deviceId,
                            deviceName: deviceName,
                            cameraId: cameraId,
                            cameraName: cameraName,
                            text: cameraName,
                            data: {
                                platformDeviceId: platformDeviceId,
                                deviceId: deviceId,
                                deviceName: deviceName,
                                channelId: cameraId,
                                channelName: cameraName,
                                camera: camera,
                            },
                            cameras: camera
                        };
                        return cameraData;
                    };
                    
                    var setBasicLabel = function(label) {
                        var labelId = label.labelId;
                        var labelName = label.name;

                        var labelData = {
                            isLabel: true,
                            labelId: labelId,
                            labelName: labelName,
                            text: labelName,
                            data: {
                                type: label.type,
                                info: label.info
                            },
                            items: [],
                        };
                        return labelData;
                    };

                    var isDeviceInLabel = function(labelId, deviceId) {
                        var check = false;
                        var isBreak = false;
                        $.each(deviceList, function(i, device) {
                            if (parseInt(device.deviceId, 10) === deviceId) {
                                $.each(device.channelLabels, function(j, camera) {
                                    $.each(camera.labels, function(k, label) {
                                        if (label === labelId) {
                                            check = true;
                                            isBreak = true;
                                            return false;
                                        }
                                    });
                                    if (isBreak) {
                                        return false;
                                    }
                                });
                            }
                            if (isBreak) {
                                return false;
                            }

                        });
                        return check;
                    };

                    var isCameraInLabel = function(labelId, deviceId, cameraId) {
                        var check = false;
                        var isBreak = false;
                        $.each(deviceList, function(i, device) {
                            if (parseInt(device.deviceId, 10) === deviceId) {
                                $.each(device.channelLabels, function(j, camera) {
                                    $.each(camera.labels, function(k, label) {
                                        if (label === labelId && parseInt(camera.channelId, 10) === cameraId) {
                                            check = true;
                                            isBreak = true;
                                            return false;
                                        }
                                    });
                                    if (isBreak) {
                                        return false;
                                    }
                                });
                            }
                            if (isBreak) {
                                return false;
                            }

                        });
                        return check;
                    };
                    
                    $.each(deviceList, function(index, dItem) {

                        if (dItem.model.capabilities.indexOf("video") == -1) {
                            return;
                        }

                        randomDeviceKey++;
                        deviceGroups[randomDeviceKey] = deviceGroups[randomDeviceKey] || [];
						/** Disabled "all" label */
                        //create a "all" label include all device.
                        //labelGroups[all] = labelGroups[all] || [];
                        if (dItem.model.capabilities.indexOf("node") != -1) { //device is the node (for this side is setting sub point e.g. ipcamera,channel).
                            $.each(dItem.node.cameras, function(index, camera) {
                                deviceGroups[randomDeviceKey].push({
                                    isCamera: true,
                                    id: dItem.id,
                                    deviceId: dItem.deviceId,
                                    text: camera.name,
                                    group: all,
                                    cameras: camera,
                                    data: {
                                        platformDeviceId: dItem.id,
                                        deviceId: dItem.deviceId,
                                        channelId: camera.nodeCoreDeviceId,
                                        deviceName: dItem.name,
                                        channelName: camera.name,
                                        _id: utils.getRandomInteger(1000, 99999)
                                    }
                                });
                            });
                        } else {
                            var i = 0;
                            while (i < dItem.model.channels) { //device isn't node
                                i++;
                                deviceGroups[randomDeviceKey].push({
                                    isCamera: true,
                                    id: dItem.id,
                                    deviceId: dItem.deviceId,
                                    text: i18n('channel') + " " + i,
                                    group: all,
                                    cameras: {
                                        "name": i18n('channel') + " " + i,
                                        "nodeCoreDeviceId": i - 1
                                    },
                                    data: {
                                        platformDeviceId: dItem.id,
                                        deviceId: dItem.deviceId,
                                        channelId: (i - 1).toString(),
                                        deviceName: dItem.name,
                                        channelName: i,
                                        _id: utils.getRandomInteger(1000, 99999)
                                    }
                                });
                            }
                        }

						/** Disabled "all" label */
                        //check connection situation to set status icon (device).
                        /*if (dItem.status == DvcMgr.DeviceStatus.DISCONNECTED) {
                            labelGroups[all].push({
                                isDevice: true,
                                parentName: all,
                                deviceId: dItem.id,
                                text: dItem.name + "",
                                imageUrl: kupapi.CdnPath + "/common/images/device_offline.png",
                                items: deviceGroups[randomDeviceKey]
                            });
                        } else {
                            labelGroups[all].push({
                                isDevice: true,
                                parentName: all,
                                deviceId: dItem.id,
                                text: dItem.name + "",
                                items: deviceGroups[randomDeviceKey] //set sub point.
                            });
                        }*/ //create a "all" label finish.

                        if (dItem.label.length > 0) {
                            $.each(dItem.label, function(index, lItem) {
                                /*var lItem = lItem.toUpperCase(); */

                                labelGroups[lItem] = labelGroups[lItem] || [];
                                if (deviceGroups[randomDeviceKey].length < 1) {
                                    if (dItem.model.capabilities.indexOf("node") != -1) { //device is the node (for this side is setting sub point e.g. ipcamera,channel).
                                        $.each(dItem.node.cameras, function(index, camera) {
                                            deviceGroups[randomDeviceKey].push({
                                                isCamera: true,
                                                id: dItem.id,
                                                deviceId: dItem.deviceId,
                                                text: camera.name,
                                                group: all,
                                                cameras: camera,
                                                data: {
                                                    platformDeviceId: dItem.id,
                                                    deviceId: dItem.deviceId,
                                                    channelId: camera.nodeCoreDeviceId,
                                                    deviceName: dItem.name,
                                                    channelName: camera.name,
                                                    _id: utils.getRandomInteger(1000, 99999)
                                                }
                                            });
                                        });
                                    } else {
                                        var i = 0;
                                        while (i < dItem.model.channels) { //device isn't node
                                            i++;
                                            deviceGroups[randomDeviceKey].push({
                                                isCamera: true,
                                                id: dItem.id,
                                                deviceId: dItem.deviceId,
                                                text: i18n('channel') + " " + i,
                                                group: all,
                                                cameras: {
                                                    "name": i18n('channel') + " " + i,
                                                    "nodeCoreDeviceId": i - 1
                                                },
                                                data: {
                                                    platformDeviceId: dItem.id,
                                                    deviceId: dItem.deviceId,
                                                    channelId: (i - 1).toString(),
                                                    deviceName: dItem.name,
                                                    channelName: i,
                                                    _id: utils.getRandomInteger(1000, 99999)
                                                }
                                            });
                                        }
                                    }
                                }
                                if (dItem.status == DvcMgr.DeviceStatus.DISCONNECTED) {
                                    labelGroups[lItem].push({
                                        isDevice: true,
                                        parentName: lItem,
                                        deviceId: dItem.id,
                                        text: dItem.name + "",
                                        imageUrl: kupapi.CdnPath + "/common/images/device_offline.png",
                                        items: deviceGroups[randomDeviceKey]
                                    });
                                } else {
                                    labelGroups[lItem].push({
                                        isDevice: true,
                                        parentName: lItem,
                                        deviceId: dItem.id,
                                        text: dItem.name + "",
                                        items: deviceGroups[randomDeviceKey]
                                    });
                                }
                            });
                        } //Non node device finish

                        if (dItem.cameraLabels.length > 0) { //Node device start
                            $.each(dItem.cameraLabels, function(index, lItem) {
                                /*var lItem = lItem.toUpperCase(); */
                                randomDeviceKey++;
                                if (deviceGroups[randomDeviceKey] == null)
                                    deviceGroups[randomDeviceKey] = [];

                                if (labelGroups[lItem] == null) {
                                    labels.push(lItem);
                                    labelGroups[lItem] = [];
                                }
                                $.each(dItem.node.cameras, function(index, camera) {
                                    if ($.inArray(lItem, camera.labels) !== -1) {
                                        deviceGroups[randomDeviceKey].push({
                                            isCamera: true,
                                            id: dItem.id,
                                            deviceId: dItem.deviceId,
                                            text: camera.name,
                                            group: lItem,
                                            cameras: camera,
                                            data: {
                                                platformDeviceId: dItem.id,
                                                deviceId: dItem.deviceId,
                                                channelId: camera.nodeCoreDeviceId,
                                                deviceName: dItem.name,
                                                channelName: camera.name,
                                                _id: utils.getRandomInteger(1000, 99999)
                                            }
                                        });
                                    }
                                })
                                if (dItem.status == DvcMgr.DeviceStatus.DISCONNECTED) {
                                    labelGroups[lItem].push({
                                        isDevice: true,
                                        parentName: lItem,
                                        deviceId: dItem.id,
                                        text: dItem.name + "",
                                        imageUrl: kupapi.CdnPath + "/common/images/device_offline.png",
                                        items: deviceGroups[randomDeviceKey]
                                    });
                                } else {
                                    labelGroups[lItem].push({
                                        isDevice: true,
                                        parentName: lItem,
                                        deviceId: dItem.id,
                                        text: dItem.name + "",
                                        items: deviceGroups[randomDeviceKey]
                                    });
                                }
                            });
                        } //Node device finish
                    }); //deviceList loop finish
                    
                    
                    //set label in tree data
                    $.each(labelList, function(i, label) {
                        var labelId = label.labelId;
                        var labelData = setBasicLabel(label);
                        $.each(deviceList, function(j, device) {
                            var deviceId = parseInt(device.deviceId, 10);
                            if (isDeviceInLabel(labelId, deviceId)) {
                                var deviceData = setBasicDevice(label, device);
                                    $.each(device.node.cameras, function(k, camera) {
                                        var cameraId = parseInt(camera.nodeCoreDeviceId, 10);
                                        if (isCameraInLabel(labelId, deviceId, cameraId)) {
                                            var cameraData = setBasicCamera(label, device, camera);
                                            deviceData.items.push(cameraData);
                                        }
                                    })
                                labelData.items.push(deviceData);
                            }
                        })
                        if(labelData.data.type === "STORE")
                        {
                            opt.treeItems.push(labelData);
                        }

                    });

                    //set treeItems
                    (function() {
                        opt.treeItems.sort(function(a, b) {
                        	var aName = a.text.toLowerCase();
                        	var bName = b.text.toLowerCase(); 
                            if (aName < bName)
                                return -1;
                            if (aName > bName)
                                return 1;
                            return 0;
                        });
                    })();
                    
                    //filter the treeItem which no contains any item
                    var filteredEmptyTree = [];
                    $.each(opt.treeItems, function(index, treeItem) {
                    	if(treeItem.items.length > 0)
                    		filteredEmptyTree.push(treeItem);
                    });
                    opt.treeItems = filteredEmptyTree;
                    
                    _self.setOpt(opt);
                    return opt.treeItems;
                },
                showVcaTreeItemsList: function() {
                    var opt = _self.getOpt(),
                        runningAnalyticsList = opt.apiRunningAnalyticsList,
                        treeItemData = $("#deviceTree").data('kendoTreeView').dataSource.data(),
                        showVcaTreeItemsList = [];

                    //get runing vca's uid list'
                    $.each(treeItemData, function(i, item) {
                        var itemData = item.items;
                        var isShowLabel = false
                        $.each(itemData, function(j, device) {
                            var deviceData = device.items;
                            var isShowDevice = false;
                            $.each(deviceData, function(k, camera) {
                                var cameraData = camera.data;
                                $.each(runningAnalyticsList, function(l, vcaData) {
                                    if (vcaData.channelId === cameraData.channelId && vcaData.coreDeviceId === cameraData.deviceId && vcaData.enabled) {
                                        isShowLabel = true;
                                        isShowDevice = true;
                                        showVcaTreeItemsList.push(camera.uid);
                                        return false;
                                    }
                                });
                            });
                            if (isShowDevice) {
                                showVcaTreeItemsList.push(device.uid);
                            }
                        });
                        if (isShowLabel) {
                            showVcaTreeItemsList.push(item.uid);
                        }
                    });
                    opt.showVcaTreeItemsList = showVcaTreeItemsList;
                    _self.setOpt(opt);
                    return opt.showVcaTreeItemsList;
                },
            },
            setUI: {
                menuTab: function() {
                    var opt = _self.getOpt(),
                        callBack = {
                            onSelect: function onSelect(e) {
                                var opt = _self.getOpt(),
                                    isDefaultDropData = opt.isDefaultDropData;
                                opt.selectedType = "#" + e.item.id;
                                $(e.item).addClass('is_active').siblings().removeClass("is_active");
                                _self.setOpt(opt);
                                if (!isDefaultDropData) {
                                    _self.updateUI.removeDropPlace();
                                }

                            }
                        }
                    $("#subMenuTab").kendoTabStrip({
                        select: callBack.onSelect
                    });
                    _self.updateUI.title();
                    _self.updateUI.menuTab();
                },
                /**
                 * [semantic ui]
                 * @http://semantic-ui.com/modules/checkbox.html
                 */
                menuOptions: function() {
                    $('#showVcaSwitch').checkbox({
                        onChecked: function() {
                            var opt = _self.getOpt();
                            opt.isOnlyShowVcaTreeView = true;
                            _self.setOpt(opt);
                            _self.updateUI.switchTreeView();
                        },
                        onUnchecked: function() {
                            var opt = _self.getOpt();
                            opt.isOnlyShowVcaTreeView = false;
                            _self.setOpt(opt);
                            _self.updateUI.switchTreeView();
                        }
                    });
                    $('#deivceOptionsBtn').popup({
                        position: 'bottom center',
                    });
                    $('#deivceOptionsBtn').on('click', function(event) {
                        event.preventDefault();
                        $('#deivceOptions').slideToggle("slow");
                    });
                    $('body').on('click', function(event) {
                        if ($('#deivceOptions').css('display') === 'none') {
                            return;
                        }
                        if ($(event.target).parents('#deivceOptions').attr('id') === $('#deivceOptions').attr('id') ||
                            $(event.target).attr('id') === $('#deivceOptions').attr('id') ||
                            $(event.target).attr('id') === $('#deivceOptionsBtn').attr('id')) {
                            return;
                        }
                        $('#deivceOptions').slideToggle("slow");
                    });
                },
                updateRunningAnalytics: function(callback) {
                	var opt = _self.getOpt();
                	window.listRunningAnalytics("", _map.analyticsType[opt.monitorType], function(responseData) {
                		if(responseData.result == "ok" && responseData.instances.length > 0){
                			opt.apiRunningAnalyticsList = responseData.instances;
                    		_self.setOpt(opt);
                		}
                		callback();
                	});
                },
                intervalUpdate: function() {
                	//Store in localStorage
                	var opt = _self.getOpt();
                	var intervalTime = localStorage.getItem("intervalTime"+_map.queryType[opt.monitorType]);
                	
                	if(intervalTime == null){
                		intervalTime = 30;
                		localStorage.setItem("intervalEnable"+_map.queryType[opt.monitorType], "true");
                		localStorage.setItem("intervalTime"+_map.queryType[opt.monitorType], intervalTime);
                	}
                	
                	$("#intervalTime").kendoNumericTextBox({
                		format: "### secs",
                		min: 30,
                		max: 300,
                		step: 1,
                		value: intervalTime
                	});

                	if(localStorage.getItem("intervalEnable"+_map.queryType[opt.monitorType]) == "false"){
                		$("#intervalTime").data("kendoNumericTextBox").enable(false);
                		$(".interval-status-icon").removeClass("fa-pause").addClass("fa-play");
                	}else{
                		opt.intervalCache = setInterval(function(){
                			_self.exec.generateReport();
                		}, intervalTime * 1000);
                		_self.setOpt(opt);
                	}
                	
                	//user click play or pause
                	$("#intervalStatusBtn").on('click', function() {
                		var optSub = _self.getOpt();
                		if(localStorage.getItem("intervalEnable"+_map.queryType[opt.monitorType]) == "false"){
                    		$("#intervalTime").data("kendoNumericTextBox").enable(true);
                    		$(".interval-status-icon").removeClass("fa-play").addClass("fa-pause");
                    		localStorage.setItem(
                    				"intervalEnable"+_map.queryType[opt.monitorType], 
                    				"true"
                    		);
                    		_self.exec.generateReport();
                    		optSub.intervalCache = setInterval(function(){
                    			_self.exec.generateReport();
	                   		}, localStorage.getItem("intervalTime"+_map.queryType[opt.monitorType]) * 1000);
                    		_self.setOpt(optSub);
                    	}else {
                    		$("#intervalTime").data("kendoNumericTextBox").enable(false);
                    		$(".interval-status-icon").removeClass("fa-pause").addClass("fa-play");
                    		localStorage.setItem(
                    				"intervalEnable"+_map.queryType[opt.monitorType], 
                    				"false"
                    		);
                    		clearInterval(optSub.intervalCache);
                    		optSub.intervalCache = null;
                    		_self.setOpt(optSub);
                    	}
                		
                    });
                	
                	//user save interval time
                	$("#saveIntervalBtn").on('click', function() {

                		if($("#intervalTime").data("kendoNumericTextBox").value() == null){
                			$("#intervalTime").data("kendoNumericTextBox").value(intervalTime);
                		}
                		
                		localStorage.setItem(
                				"intervalTime"+_map.queryType[opt.monitorType], 
                				$("#intervalTime").data("kendoNumericTextBox").value()
                		);
                		var optSub = _self.getOpt();
                		if(optSub.intervalCache != null){
                			clearInterval(optSub.intervalCache);
                		}
                		if(localStorage.getItem("intervalEnable"+_map.queryType[optSub.monitorType]) == "true"){
                			_self.exec.generateReport();
                			optSub.intervalCache = setInterval(function(){
                				_self.exec.generateReport();
	                   		}, $("#intervalTime").data("kendoNumericTextBox").value() * 1000);
                		}
                		_self.setOpt(optSub);
                    });
                },
                dropPlace: function() {},
                updateBtn: function() {
                    var utils = kup.utils.default,
                        i18n = kup.utils.i18n;

                    $('#applyBtn').on('click', function(event) {
                        event.preventDefault();
                        _self.exec.saveMonitoringQueryHistory();
                        _self.exec.generateReport();
                    });
                    $('#cancelBtn').on('click', function(event) {
                        event.preventDefault();
                        var opt = _self.getOpt(),
                            applyArg = [];

                        applyArg.push(_self.exec.getMonitoringQueryHistory());
                        opt.isDefaultDropData = true;
                        _self.setOpt(opt);

                        $.when.apply(
                            $, applyArg
                        ).always(function() {
                            //do something
                        }).done(function() {
                            _self.updateUI.removeDropPlace();
                            _self.updateUI.defaultDropPlace();
                            _self.exec.generateReport();
                        }).fail(function() {
                            var opt = _self.getOpt();
                            opt.isDefaultDropData = false;
                            _self.setOpt(opt);
                            utils.popupAlert(i18n('server-error'));
                        });
                    });
                },
                treeView: function() {
                    var opt = _self.getOpt(),
                        kupOpt = kup.getOpt(),
                        KupEvent = kupOpt.event,
                        deviceManager = kup.utils.deviceManager,
                        i18n = kup.utils.i18n;

                    var treeItems = opt.treeItems,
                        draggedElementList = {},
                        draggedDeviceList = {},
                        parent = null,
                        treeview = null;
                    
                    var removeSelectedDevice = function(e) {
                        e.preventDefault();
                        var opt = _self.getOpt(),
                            selectedDevice = $(e.currentTarget).parent('span'),
                            deviceIds = selectedDevice.attr('deviceId').split(","),
                            channelId = selectedDevice.attr('channelId'),
                            uid = selectedDevice.attr('data-uid'),
                            labelName = opt.selectedItemDataList[uid].id || '',
                            selectedGroupNames = opt.selectedGroupNames,
                            setOption = (function() {
                                //set tree view  
                                $(selectedDevice).remove();
                                $("#deviceTree").removeClass('isDrag');
                                $(opt.selectedElementList[uid]).removeClass('isDrag');

                                //set selected var
                                $.each(deviceIds, function(index, id) {
                                    opt.selectedDeviceList.splice(opt.selectedDeviceList.indexOf(id), 1);
                                })
                                if (channelId != "") {
                                    opt.selectedChannelList.splice(opt.selectedChannelList.indexOf(channelId), 1);
                                }
                                $.each(selectedGroupNames, function(i, name) {
                                    if (labelName === name) {
                                        opt.selectedGroupNames.splice(i, 1);
                                    }
                                });
                                //clear object,if use 'delete',the 'labelName' have some unknow error 
                                opt.selectedElementList[uid] = {};
                                opt.selectedItemDataList[uid] = {};

                                opt.isUpdateSelectedItem = true;
                                _self.setOpt(opt);
                            })(),
                            setData = (function() {
                                _self.setData.selectedInstance();
                                _self.setData.selectedDevices();
                                _self.setData.selectedSaveDataList();
                                _self.setData.groupNames();
                            })(),
                            setUI = (function() {
                                _self.updateUI.showUpdateBtn();
                            })();
                    };
                    //render tree    
                    $("#deviceTree").kendoTreeView({
                        dragAndDrop: true,
                        loadOnDemand: false,
                        /** Only show Label name & hide all the childs in dropdown */
                        dataSource: new kendo.data.HierarchicalDataSource({
                        	data: treeItems,
                        	schema: {
                        		model: {
                        	    	id: "labelId",
                        	    	hasChildren: "HasNothing"
                        	    }
                        	}
                        }),
                        dragstart: function(e) {
                            var opt = _self.getOpt(),
                                isDefaultDropData = opt.isDefaultDropData,
                                isSingleType = opt.isSingleType;
                            var treeview = $("#deviceTree").data("kendoTreeView");
                            //clear object    
                            draggedElementList = {};
                            draggedDeviceList = {};
//                            isDefaultDropData = false;
                            if (isDefaultDropData) {
                                draggedElementList = opt.defaultElementList;
                                draggedDeviceList = opt.defaultItemDataList;
                            } else {
                                var uid = treeview.dataItem(e.sourceNode).uid;
                                draggedElementList[uid] = e.sourceNode;
                                draggedDeviceList[uid] = treeview.dataItem(e.sourceNode);
                            }
                            $.each(draggedDeviceList, function(uid, draggedDevice) {
                                var draggedElement = draggedElementList[uid];
                                if ((isSingleType && $("#deviceTree").hasClass('isDrag')) ||
                                    (!isSingleType && $(draggedElement).hasClass('isDrag'))) {
                                    e.preventDefault();
                                    return false;
                                }
                                if (!draggedDevice.hasChildren) {
                                    parent = treeview.parent(draggedElement);
                                }
                                if (!isDefaultDropData) {
                                    $(".drop-here").css('display', 'block');
                                    opt.isDragging = true;
                                    _self.setOpt(opt);
                                }
                            });
                        },
                        drag: function(e) {
                            var opt = _self.getOpt(),
                                isDefaultDropData = opt.isDefaultDropData;
                            if (!isDefaultDropData) {
                                if (e.dropTarget.className == "drop-here" ||
                                    $(e.dropTarget).parents('#main-header').attr('id') == 'main-header' ||
                                    $(e.dropTarget).parents('#main-charts').attr('id') == 'main-charts' ||
                                    $(e.dropTarget).attr('id') == 'main-charts') { //if drag over "drop-here" change icon for "+".
                                    e.setStatusClass("k-add");
                                }
                            }
                        },
                        drop: function(e) {
                            e.preventDefault();
                            var opt = _self.getOpt(),
                                isDefaultDropData = opt.isDefaultDropData;

                            if (!isDefaultDropData) {
                                if (e.dropTarget.className != "drop-here" &&
                                    $(e.dropTarget).parents('#main-header').attr('id') != 'main-header' &&
                                    $(e.dropTarget).parents('#main-charts').attr('id') != 'main-charts' &&
                                    $(e.dropTarget).attr('id') == 'main-charts') { //if drag drop is "drop-here" doesn't execute dropping.
                                    e.setValid(false);
                                }
                                $(".drop-here").css('display', 'none');
                                opt.isDragging = false;
                                if (opt.selectedDeviceList.length > 1)
                                    opt.isMultiDeviceSelected = true;
                                else
                                    opt.isMultiDeviceSelected = false;
                                _self.setOpt(opt);
                            }
                        }
                    }).data("kendoTreeView");
                    $("#deviceTree").find('img').addClass("node-status-image");
                    //tree drop area
                    $(".drop-here, #main-header, #main-charts").kendoDropTarget({
                        drop: function(e) { //execute faster than treeview's drop event.
                            var opt = _self.getOpt(),
                                isDefaultDropData = opt.isDefaultDropData,
                                isSingleType = opt.isSingleType;
                            var treeview = $("#deviceTree").data("kendoTreeView"),
                                setOption = (function() {
                                    $.each(draggedDeviceList, function(uid, draggedDevice) {
                                        var deviceId = "",
                                            channelId = "",
                                            name = draggedDevice.text;
                                        //set selected var
                                        if (draggedDevice.isAll || draggedDevice.isLabel) { //drag root or label
                                            var len = draggedDevice.items.length;
                                            if (draggedDevice.isLabel) {
                                                opt.selectedGroupNames.push(draggedDevice.text);
                                            }
                                            var deviceDragged = draggedDevice.items;
                                            $.each(deviceDragged, function(index, device) {
                                                opt.selectedDeviceList.push(device.deviceId);
                                                if (index == len - 1) {
                                                    deviceId += device.deviceId;
                                                } else {
                                                    deviceId += device.deviceId + ",";
                                                }
                                            });
                                        } else if (draggedDevice.isDevice) { //drag device
                                            opt.selectedGroupNames = [];
                                            opt.selectedDeviceList.push(draggedDevice.deviceId);
                                            deviceId += draggedDevice.deviceId;
                                            name = draggedDevice.parentName + " - " + draggedDevice.text;
                                        } else if (draggedDevice.isCamera) { //drag camera.
                                            if (draggedDevice.hasChildren) {
                                                return;
                                            }
                                            opt.selectedGroupNames = [];
                                            opt.selectedDeviceList.push(draggedDevice.id);
                                            opt.selectedChannelList.push(draggedDevice.cameras.nodeCoreDeviceId);
                                            deviceId += draggedDevice.id;
                                            channelId += draggedDevice.cameras.nodeCoreDeviceId;
                                            name = draggedDevice.data.deviceName + " - " + draggedDevice.text;
                                        }

                                        opt.selectedElementList[uid] = draggedElementList[uid];
                                        opt.selectedItemDataList[uid] = draggedDeviceList[uid];
                                        opt.isUpdateSelectedItem = true;
                                        //add drop place div
                                        var htmlString = "<span class='drop-item pos-r isActive' data-uid='" + uid + "' deviceId='" + deviceId + "' channelId='" + channelId +
                                            "'>" + name + "<a class='pos-r btn-remove ir'></a></span>";
                                        $(htmlString).insertBefore('.drop-here');
                                    })
                                    _self.setOpt(opt);
                                })(),
                                setData = (function() {
                                    _self.setData.selectedInstance();
                                    _self.setData.selectedDevices();
                                    _self.setData.selectedSaveDataList();
                                    _self.setData.groupNames();
                                })(),
                                setEvent = (function() {
                                    //bind event & set isDrag item
                                    $('.drop-item > a').on('click', removeSelectedDevice);
                                    if ((isDefaultDropData && !$.isEmptyObject(draggedDeviceList)) ||
                                        (!isDefaultDropData && isSingleType)) {
                                        $("#deviceTree").addClass('isDrag');
                                        //$('#deviceTree').off("mouseover", "span");
                                    }
                                    $.each(draggedElementList, function(uid, draggedElement) {
                                        $(draggedElement).addClass('isDrag');
                                    });
                                })(),
                                setUI = (function() {
                                    _self.updateUI.showUpdateBtn();
                                })();
                        }
                    });
                    $("#subMenu").kendoTooltip({
                        filter: "li span.k-in",
                        content: "Loading...",
                        position: "down",
                        showAfter: 1000,
                        show: function(e) {
                            var treeview = $("#deviceTree").data("kendoTreeView");
                            var tooltip = this;
                            var text = treeview.text(tooltip.target());
                            tooltip.content.text(text);
                        }
                    }).data("kendoTooltip");

                    $('#deviceTree').on("mouseover", "span", function() {
                        var $item = $(this).parent().parent();
                        if (!$item.hasClass('isDrag') && !$("#deviceTree").hasClass('isDrag')) {
                            $(".drop-here").css('display', 'block')
                        }

                    });
                    $('#deviceTree').on("mouseout", "span", function() {
                        var opt = _self.getOpt();
                        if (!opt.isDragging) {
                            $(".drop-here").css('display', 'none')
                        }
                    });
                },
                searchTreeView: function() {
                    var treeview = $("#deviceTree").data("kendoTreeView");

                    //override jquery contains make it case insensitive
                    $.expr[":"].contains = $.expr.createPseudo(function(arg) {
                        return function( elem ) {
                            return $(elem).text().toUpperCase().indexOf(arg.toUpperCase()) >= 0;
                        };
                    });

                    //hide or show when searching
                    $('#search-term').keyup(function (e) {
                        var filterText = $(this).val();
                        if (filterText !== "")
                        {
                            treeview.expand(".k-item");
                            $("#deviceTree .k-group .k-group .k-in").closest("li").hide();
                            $("#deviceTree .k-group .k-in").closest("li").hide();
                            $("#deviceTree .k-group .k-group .k-in:contains(" + filterText + ")").each(function () {
                                $(this).parents("ul, li").each(function () {
                                    $(this).show();
                                });
                            });
                            $("#deviceTree .k-group .k-in:contains(" + filterText + ")").each(function () {
                                $(this).parents("ul, li").each(function () {
                                    $(this).show();
                                });

                                //list node's cameras
                                $(this).parent("div").siblings("ul").children("li").each(function ()
                                {
                                    $(this).show();
                                });
                                $(this).parent("div").siblings("ul").show();
                            });
                        }
                        else {
                            $("#deviceTree .k-group .k-in").closest("li").show();
                            $("#deviceTree .k-group .k-group .k-in").closest("li").show();
                            treeview.collapse(".k-item");
                        }
                    });
                }
            },
            updateUI: {
                title: function() {
                    var opt = _self.getOpt(),
                        monitorType = opt.monitorType,
                        title = _map.title,
                        i18n = kup.utils.i18n;
                    $('#reportTitle').html(i18n(title[monitorType]));
                },
                menuTab: function() {
                    var opt = _self.getOpt(),
                        monitorType = opt.monitorType,
                        title = _map.title,
                        i18n = kup.utils.i18n;
                    $('#subMenuTitle').removeClass(function() {
                        var toReturn = '',
                            classes = this.className.split(' ');
                        for (var i = 0; i < classes.length; i++) {
                            if (/report-\w*-\w*/.test(classes[i])) { /* Filters */
                                toReturn += classes[i] + ' ';
                            }
                        }
                        return toReturn; /* Returns all classes to be removed */
                    }).addClass(title[monitorType]).parent().contents().last()[0].textContent = i18n(title[monitorType]);
                },
                defaultDropPlace: function() {
                    var opt = _self.getOpt(),
                        dataLength = Object.keys(opt.defaultItemDataList).length;
                    $("#deviceTree").data('kendoTreeView').trigger('dragstart');
                    $("#deviceTree").data('kendoTreeView').trigger('drag');
                    $("#deviceTree").data('kendoTreeView').trigger('drop');
                    $(".drop-here").data('kendoDropTarget').trigger('drop');

                    var opt = _self.getOpt();
                    opt.isDefaultDropData = false;
                    _self.setOpt(opt);
                },
                removeDropPlace: function(uid) {
                    var $selector = (uid) ? $('.drop-item[data-uid="' + uid + '"] > a') : $('.drop-item > a')
                    $selector.click();
                },
                switchTreeView: function() {
                    var opt = _self.getOpt(),
                        isOnlyShowVcaTreeView = opt.isOnlyShowVcaTreeView,
                        showVcaTreeItemsList = opt.showVcaTreeItemsList,
                        $treeView = $("#deviceTree");
                    //show all div
                    $treeView.find('li[data-uid]').show();
                    //currect vca  to hide div
                    if (isOnlyShowVcaTreeView) {
                        $treeView.find('li[data-uid]').hide();
                        $.each(showVcaTreeItemsList, function(i, uid) {
                            $treeView.find('li[data-uid="' + uid + '"]').show();
                        });
                    }
                },
                showReport: function() {
                	var opt = _self.getOpt(),
                    applyArg = [];

	                applyArg.push(_self.exec.getMonitoringQueryHistory());
	                opt.isDefaultDropData = true;
	                _self.setOpt(opt);
	
	                $.when.apply(
	                    $, applyArg
	                ).always(function() {
	                    //do something
	                }).done(function() {
	                    _self.updateUI.removeDropPlace();
	                    _self.updateUI.defaultDropPlace();
	                    
	                    var opt = _self.getOpt();
	                    if (Object.keys(opt.defaultItemDataList).length > 0) {
		                    _self.exec.generateReport();
	                    }
	                }).fail(function() {
	                    var opt = _self.getOpt();
	                    opt.isDefaultDropData = false;
	                    _self.setOpt(opt);
	                    utils.popupAlert(i18n('server-error'));
	                });
                },
                showDiv: function() {
                    var opt = _self.getOpt(),
                        isSuccessReport = opt.isSuccessReport,
                        monitorType = opt.monitorType,
                        showDiv = _map.successReportToShowDiv[monitorType];

                    $('#main-charts > div').show();
                    $.each(showDiv, function(i, val) {
                        $(val).hide();
                        if (isSuccessReport) {
                            $(val).show();
                        }
                    });

                },
                showUpdateBtn: function() {
                    var opt = _self.getOpt(),
                        selectedItemsLength = _self.getData.selectedItemsLength(),
                        isUpdateSelectedItem = opt.isUpdateSelectedItem,
                        $updateBtn = $('#main-header .modifyConfirm'),
                        $applyBtn = $('#main-header #applyBtn'),
                        $cancelBtn = $('#main-header #cancelBtn');

                    $updateBtn.show();
                    if (isUpdateSelectedItem && selectedItemsLength > 0) {
                        $applyBtn.show();
                    } else {
                        $applyBtn.hide();
                    }

                    if (isUpdateSelectedItem) {
                        $cancelBtn.show();
                    } else {
                        $cancelBtn.hide();
                    }
                },
                cleanDiv: function() {
                    $('#main-charts > div').hide();
                }
            },
            verification: {
                monitorType: function(monitorType) {
                    var opt = _self.getOpt(),
                        reportAll = _map.monitorType,
                        isVer = false;
                    $.each(reportAll, function(i, report) {
                        if (report === monitorType) {
                            isVer = true;
                            return false;
                        }
                    });
                    return isVer;
                }
            },
            exec: {
                generateReport: function() {
                    var opt = _self.getOpt();
                    opt.isUpdateSelectedItem = false;
                    _self.setOpt(opt);
                    _self.updateUI.showUpdateBtn();
                    _self.type[opt.monitorType].generateReport();
                },
                saveMonitoringQueryHistory: function() {
                    var opt = _self.getOpt(),
                        kupOpt = kup.getOpt(),
                        utils = kup.utils.default,
                        vcaEventType = _map.queryType[opt.monitorType],
                        requestOpt = {
                            eventType: vcaEventType,
                            dateFrom: kendo.toString(utils.convertToUTC(new Date()), kupOpt.dateFormat),
                            dateTo: kendo.toString(utils.convertToUTC(new Date()), kupOpt.dateFormat),
                            deviceSelected: JSON.stringify(opt.selectedSaveDataList),
                            onSuccess: function() {},
                            onFailure: function() {}
                        };
                    return window.saveReportQueryHistory(
                        requestOpt.eventType,
                        requestOpt.dateFrom,
                        requestOpt.dateTo,
                        requestOpt.deviceSelected,
                        requestOpt.onSuccess,
                        requestOpt.onFailure
                    );
                },
                getMonitoringQueryHistory: function() {
                    var opt = _self.getOpt(),
                        vcaEventType = _map.queryType[opt.monitorType],
                        requestOpt = {
                            eventType: vcaEventType,
                            onSuccess: function(data) {
                                var opt = _self.getOpt();
                                opt.apiQueryHistoryData = data.query || {};
                                _self.setOpt(opt);
                                _self.setData.defaultItemDataList();
                                _self.setData.defaultElementList();
                            },
                            onFailure: function() {}
                        };
                    return window.getReportQueryHistory(
                        requestOpt.eventType,
                        requestOpt.onSuccess,
                        requestOpt.onFailure
                    );
                }
            },
            initOpt: function() {
                var opt = _self.getOpt();
                opt.isMultiDeviceSelected = false;
                opt.isSuccessReport = false;
                opt.isSingleType = true;
                opt.isDragging = false;
                opt.isDefaultDropData = true;
                opt.isUpdateSelectedItem = false;
                _self.setOpt(opt);
            },
            init: function() {
                var opt = _self.getOpt(),
                    getUrl = window.location.href,
                    getLocation = getUrl.indexOf('#'),
                    getMonitoring = getUrl.substring(getLocation + 1),
                    monitorTab = (getLocation === -1 || !getMonitoring || !_self.verification.monitorType(getMonitoring)) ? opt.menuTabType : _map.menuTab[getMonitoring],
                    monitorType = (function(tab) {
                        var monitor = '';
                        $.each(_map.menuTab, function(k, v) {
                            if (v === tab) {
                            	monitor = k;
                                return false;
                            }
                        })
                        return monitor;
                    })(monitorTab);
                var getUserDevicesOpt = {
                        sessionKey: '',
                        onSuccess: function(data) {
                            var opt = _self.getOpt(),
                                devices = data.devices || [];
                            $.each(devices, function(i, dvc) {
                                if (utils.checkDeviceCompleteInfo(dvc)) {
                                    opt.apiUserDevicesList.push(dvc);
                                }
                            });
                            _self.setOpt(opt);
                        },
                        onFailure: function() {}
                    };
                var getLabelsOpt = {
                		onSuccess: function(data) {
                            var opt = _self.getOpt(),
                            labels = data.labels || [];
                            $.each(labels, function(i, label) {
                                if (label.type.toLowerCase() == "store") {
                                    opt.apiUserLabelList.push(label);
                                }
                            });
                            _self.setOpt(opt);
                        },
                        onFailure: function() {}	
                };

                opt.monitorType = monitorType;
                _self.setOpt(opt);
                
                //get data by ajax
                $.when(
                    window.getUserDevices(
                        getUserDevicesOpt.sessionKey,
                        getUserDevicesOpt.onSuccess,
                        getUserDevicesOpt.onFailure
                    ),
                    window.getLabels(
                    	getLabelsOpt.onSuccess,
                    	getLabelsOpt.onFailure
                    )
                ).always(function() {
                    //set data
                    _self.setData.treeItems();
                    //set sub menu ui
                    _self.setUI.updateRunningAnalytics(function(){
                    	_self.setUI.treeView();
                        _self.setUI.searchTreeView();
                        _self.setUI.menuTab();
                        _self.setUI.menuOptions();
                        _self.setUI.intervalUpdate();
                        //set content ui
                        _self.setUI.dropPlace();
                        _self.setUI.updateBtn();
                        //trigger submenu events
                        $(monitorTab).find('a').click();
                        //load report
                        _self.updateUI.showReport();
                        //update vca tree items
                    	_self.setData.showVcaTreeItemsList();
                    });
                    
                });
            },
            getDeviceGroups: getDeviceGroups,
        };
    return _self;
    /*******************************************************************************
     *
     *  Function Definition
     *
     *******************************************************************************/
    function getDeviceGroups() {
        var opt = _self.getOpt();
        var selectedGroups = [];

        $.each(opt.selectedItemDataList, function(uid, itemData) {
            var groupname = $('span[data-uid="' + uid + '"]').text() || '';
            var devices = [];
            var type = "";
            if ($.isEmptyObject(itemData)) {
                return true;
            }
            if (itemData.isAll || itemData.isLabel) { //drag root or labels
                var device = itemData.items || [];
                $.each(device, function(i, deviceData) {
                    var camera = deviceData.items || [];
                    $.each(camera, function(j, cameraData) {
                        devices.push({
                            "coreDeviceId": cameraData.data.platformDeviceId + "",
                            "channelId": cameraData.data.channelId
                        });
                    });
                });
                type = 'labels';
            } else if (itemData.isDevice) { //drag device
                var camera = itemData.items || [];
                $.each(camera, function(i, cameraData) {
                    devices.push({
                        "coreDeviceId": cameraData.data.platformDeviceId + "",
                        "channelId": cameraData.data.channelId
                    });
                });
                type = 'devices';
            } else if (itemData.isCamera) { //drag camera
                if (itemData.hasChildren) {
                    return;
                }
                var cameraData = itemData;
                devices.push({
                    "coreDeviceId": cameraData.data.platformDeviceId + "",
                    "channelId": cameraData.data.channelId
                });
                type = 'devices';
            }
            selectedGroups.push({
                "groupName": groupname,
                "devicePairs": devices,
                "type": type
            });
        });
        return selectedGroups;
    }
})(jQuery, KUP);
