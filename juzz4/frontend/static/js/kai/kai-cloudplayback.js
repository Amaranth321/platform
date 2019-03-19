/**
 * [Old Platform Setting]
 * 
 */
var angularService = angular.element(document.body).injector();
var kupBucket = angularService.get('AuthTokenFactory').getBucket();
var kupSessionKey = angularService.get('AuthTokenFactory').getSessionKey();
var kupUserId = angularService.get('AuthTokenFactory').getUserId();

var apiRootUrl = angularService.get('KupOption').sysApiRootUrl;
var localizeResource = angularService.get('UtilsService').i18n;
var kupapi = window.kupapi || {};
kupapi.CdnPath = apiRootUrl + "/public/css";
kupapi.applicationType = "cloud";
kupapi.currentUserId = parseInt(kupUserId, 10);

var jwplayer = window.jwplayer || {};
jwplayer.key = angularService.get('KupOption').jwplayerKey;

/**
 * [Angular Module Setting]
 * 
 */
angular.module('kai.cloudplayback', []);

angular
    .module('kai.cloudplayback')
    .controller('CloudplaybackController',
        function(
            KupOption, RouterStateService, UtilsService, AuthTokenFactory,
            $scope, $timeout, $q
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var block = UtilsService.block;
            var mainCtrl = $scope.$parent.mainCtrl;
            var cloudplaybackCtrl = this;

            cloudplaybackCtrl.apiRootUrl = kupOpt.sysApiRootUrl;
            //cloudplaybackCtrl.backendTypesUrl = kupOpt.sysApiRootUrl + '/public/javascripts/_generated/backend.types.js';
            init();
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function init() {
                var callback = function() {
                    var dfd = $q.defer();
                    $timeout(function() {
                        window.cloudPlayback.init();
                        dfd.resolve();
                    },2000);
                    return dfd.promise;
                };
                mainCtrl.fn.toggleSideBar(function() {
                    mainCtrl.block.promise = callback();
                });
                //mainCtrl.block.promise = callback();
            }
        });

/**
 *
 * This library is designed for selecting cameras only
 * DO NOT modify this file to allow the selection of other device types (Label, Node, etc ...)
 *
 * @author Aye Maung
 */
var camsltr = {
    treeItemTemplate: "camSelectorItemTmpl",
    //iconFolder: "/public/css/common/images/treeicons/",
    iconFolder: apiRootUrl + "/public/css/common/images/treeicons/",
    jqTreeId: null,
    kendoTree: null,
    preSelectedId: null,
    cancelled: true,
    selectedCamera: null,
    parentLookupMap: {},    // nodes
    cameraLookupMap: {},
    searchSource: [],
    deviceFilter: {
        models: null,
        minRelease: null,
        recordingEnabled: null
    }
};

camsltr.ItemType = Object.freeze({
    LABEL: "label",
    NODE4: "node4",
    NODE1: "node1",
    CAMERA: "camera"
});

//key must match ItemType
camsltr.icons = {
    label: camsltr.iconFolder + "label.png",
    node4: camsltr.iconFolder + "node.png",
    node1: camsltr.iconFolder + "node.png",
    camera: camsltr.iconFolder + "node_cam.png"
}

camsltr.open = function (title, preSelectedId, deviceFilter, onSelected, onCancelled)
{
    camsltr.preSelectedId = preSelectedId;
    camsltr.deviceFilter = $.extend(camsltr.deviceFilter, deviceFilter);
    //var contentPage = "/device/camselector";
    var contentPage = "app/cloudplayback/camselector.html";

    utils.openPopup(title, contentPage, null, null, true, function ()
    {
        if (camsltr.cancelled)
        {
            onCancelled();
        }
        else
        {
            var parentDevice = camsltr.parentLookupMap[camsltr.selectedCamera.id];
            onSelected(parentDevice, camsltr.selectedCamera.data);
        }
    });
}

camsltr.close = function (cancelled)
{
    camsltr.cancelled = cancelled;
    $(camsltr.jqTreeId).closest(".k-window-content").data("kendoWindow").close();
}

camsltr.generateTree = function (divId)
{
    camsltr.jqTreeId = "#" + divId;
    camsltr._loading(true);

    camsltr.getDevicesAsTree(function (deviceTree)
    {
        camsltr.kendoTree = $(camsltr.jqTreeId).kendoTreeView({
            template: kendo.template($("#" + camsltr.treeItemTemplate).html()),
            dataSource: deviceTree,
            select: function (e)
            {
                var selectedItem = camsltr.kendoTree.dataItem(e.node);
                camsltr._selectTreeItem(selectedItem, false);
            }
        }).data("kendoTreeView");

        $(camsltr.jqTreeId + " .k-in").on("dblclick", function (e)
        {
            var dbClickedNode = $(e.target).closest(".k-item");
            var dataItem = camsltr.kendoTree.dataItem(dbClickedNode);
            if (dataItem.items.length == 0)
            {
                var camera = dataItem;
                if (camera.selectable)
                {
                    camsltr._selectTreeItem(camera, false);
                    camsltr.close(false);
                }
            }
        });

        camsltr._initSearchBox(deviceTree);
        camsltr._customizeTreeStyle();
        camsltr._allowSubmit(false);
        camsltr.kendoTree.enable(".disabled_item", false);
        camsltr.kendoTree.expand("> .k-group > .k-item");
        camsltr._loading(false);

        if (camsltr.preSelectedId)
        {
            camsltr._selectTreeItem(camsltr.cameraLookupMap[camsltr.preSelectedId], true);
        }
    });
}

camsltr.collapseFullTree = function ()
{
    camsltr.kendoTree.collapse(".k-item");
}

camsltr.getDevicesAsTree = function (callback)
{
    getUserDevices("", function (responseData)
    {
        if (responseData.result != "ok" || responseData.devices == null)
        {
            utils.throwServerError(responseData.reason);
            callback([]);
            return;
        }

        var deviceTree = camsltr._convertToDeviceTree(responseData.devices);
        callback(deviceTree);
    }, null);
}

camsltr._convertToDeviceTree = function (deviceList)
{
    camsltr.searchSource = [];
    var deviceTree = [];

//    var l1 = "All";
//    var labelNodeCams = camsltr._buildTreeItem(
//        camsltr._getLabelIdentifier(l1),
//        l1,
//        camsltr.ItemType.LABEL,
//        false,
//        l1
//    );

    //compile nodes and cameras
    $.each(deviceList, function (idx, dvc)
    {
        var isNode = DvcMgr.isKaiNode(dvc);

        //cleaned Obj
        var cleanedDvc = dvc;
        delete cleanedDvc.cameras;  //make the object lighter

        //nodes only
        if (isNode)
        {
            //filter version
            if (camsltr.deviceFilter.minRelease &&
                dvc.node.releaseNumber < camsltr.deviceFilter.minRelease)
            {
                return true;
            }

            //filter model
            if (camsltr.deviceFilter.models &&
                camsltr.deviceFilter.models.indexOf(dvc.model.modelId) == -1)
            {
                return true;
            }

            var nodeItem = camsltr._buildTreeItem(
                camsltr._getNodeIdentifier(dvc),
                dvc.name,
                camsltr.ItemType.NODE4,
                false,
                cleanedDvc
            );

            $.each(dvc.node.cameras, function (idx, nodeCam)
            {
                var camDisabled = false;

                //filter recording enable flag
                if (camsltr.deviceFilter.recordingEnabled &&
                    nodeCam.cloudRecordingEnabled !== camsltr.deviceFilter.recordingEnabled)
                {
                    camDisabled = true;
                }

                var cameraItem = camsltr._buildTreeItem(
                    camsltr._getCameraIdentifier(dvc, nodeCam),
                    nodeCam.name,
                    camsltr.ItemType.CAMERA,
                    camDisabled,
                    nodeCam
                );

                nodeItem.items.push(cameraItem);

                if (!camDisabled)
                {
                    //update search dataSource
                    cameraItem.searchableName = cleanedDvc.name + "  -  " + nodeCam.name;
                    camsltr.searchSource.push(cameraItem);

                    //update lookup maps
                    camsltr.parentLookupMap[cameraItem.id] = cleanedDvc;
                    camsltr.cameraLookupMap[cameraItem.id] = cameraItem;
                }
            });

            //only display nodes with cameras
            if (!utils.isListEmpty(nodeItem.items))
            {
//                labelNodeCams.items.push(nodeItem);
                deviceTree.push(nodeItem);
            }
        }
    });

//    deviceTree.push(labelNodeCams);

    //update root types
    $.each(deviceTree, function (i, root)
    {
        root.isRoot = true;
    });

    return deviceTree
}

camsltr._initSearchBox = function ()
{
    var kendoInput = $(".camera_tree .search_bar .input").kendoAutoComplete({
        dataSource: camsltr.searchSource,
        dataTextField: "searchableName",
        filter: "contains",
        placeholder: localizeResource("select-by-node-or-cam"),
        select: function (e)
        {
            var dataItem = this.dataItem(e.item.index());
            camsltr._selectTreeItem(dataItem, true);
        },
        change: function (e)
        {
            this.value("");
        }
    }).data("kendoAutoComplete");

    kendoInput.list.width(345);
    $(".camera_tree .search_bar .input").show();
}

camsltr._selectTreeItem = function (cameraItem, expandToSelection)
{
    camsltr.kendoTree.select($("#" + cameraItem.id));

    if (expandToSelection)
    {
        $("#" + cameraItem.id).parentsUntil('.k-treeview').filter('.k-item').each(
            function (index, element)
            {
                camsltr.kendoTree.expand($(this));
            }
        );
    }

    if (cameraItem.selectable)
    {
        camsltr.selectedCamera = cameraItem;
        camsltr._allowSubmit(true);
    }
    else
    {
        camsltr._allowSubmit(false)
    }
};

camsltr._loading = function (loading)
{
    kendo.ui.progress($(".cam_selector"), loading);
}

camsltr._buildTreeItem = function (identifier, displayName, itemType, disabled, dataObj)
{
    return {
        id: identifier,
        name: displayName,
        type: itemType,
        imageUrl: camsltr.icons[itemType],
        isRoot: false,
        selectable: !disabled && (itemType == camsltr.ItemType.CAMERA) ? true : false,   //don't modify this
        disabled: disabled,
        data: dataObj,
        items: []
    };
}

camsltr._getLabelIdentifier = function (label)
{
    return label;
};

camsltr._getNodeIdentifier = function (nodeDevice)
{
    return nodeDevice.id;
};

camsltr._getCameraIdentifier = function (nodeDevice, nodeCamera)
{
    if (nodeDevice == null)
    {
        return nodeCamera.nodeCoreDeviceId;
    }
    return nodeDevice.id + "_" + nodeCamera.nodeCoreDeviceId;
};

camsltr._customizeTreeStyle = function ()
{
    var childItem = $(".camera_tree .child_item").closest(".k-top, .k-mid, .k-bot");
    childItem.addClass("lineage_line");

    var rootItem = $(".camera_tree .root_item").closest(".k-top, .k-mid, .k-bot");
    rootItem.addClass("lineage_line");
    rootItem.addClass("separate_root");

    var rootGroup = $(".camera_tree .root_item").closest(".k-item");
    rootGroup.css("border-left", "0px");
}

camsltr._allowSubmit = function (allow)
{
    var $btnSubmit = $(".cam_selector .select");
    if (allow)
    {
        $btnSubmit.show();
    }
    else
    {
        $btnSubmit.hide();
    }
};
/**
 * Note: All dates here are using moment.js types
 *
 * @author Aye Maung
 *
 */
var cloudPlayback = {
    periodBarId: "recStatusBar",
    jw: {
        playerId: "jwClPlyr"
    },
    selected: {
        node: null,
        nodeCamera: null,
        date: null,
        type: null,
        queryablePeriod: null, //period shrunk to nearest files, null if there are no files in the selection
        files: [],
        useNodeTimezone: null
    },
    kendo: {
        datePicker: null
    },
    autoRefresh: {
        enabled: true,
        freq: 10000
    },
    cache: {
        bucketUsers: {}
    },
    storageWidget: null,
    API_TIME_FORMAT: "DDMMYYYYHHmmss"
};

cloudPlayback.Type = Object.freeze({
    VIDEO: "video",
    IMAGE: "image" //currently not applicable
});

cloudPlayback.init = function() {
    //cloudPlayback._adjustUI();
    $('.filter_toolbar').removeClass('kupFakeHide');
    cloudPlayback._initDatePicker();

    setTimeout(function() { //wait for left menu
        recSB.init(cloudPlayback.periodBarId, cloudPlayback._periodChangedHandler);
    }, 300);

    //init search parameters
    cloudPlayback.selected.type = cloudPlayback.Type.VIDEO;
    cloudPlayback._setCamera(null, null);
    cloudPlayback._setDate(moment());
    cloudPlayback.setTimeZoneFlag(true);
    cloudPlayback._startRefreshTimer();
    cloudPlayback._initCache();
    cloudPlayback._initStorageInfo();
};

cloudPlayback.triggerCameraChange = function() {
    var selectedCameraId = null;
    if (cloudPlayback.selected.node != null && cloudPlayback.selected.nodeCamera != null) {
        selectedCameraId = camsltr._getCameraIdentifier(cloudPlayback.selected.node, cloudPlayback.selected.nodeCamera);
    }

    var nodeFilter = {
        models: DvcMgr.getModelIdList(DvcMgr.NodeType.UBUNTU),
        minRelease: 4.4,
        recordingEnabled: true
    };

    camsltr.open(localizeResource("select-camera"), selectedCameraId, nodeFilter,
        function(node, selectedCamera) {
            cloudPlayback._setCamera(node, selectedCamera);
        },
        function() {});
};

cloudPlayback.triggerDateChange = function() {
    cloudPlayback.kendo.datePicker.open();
};

cloudPlayback.setTimeZoneFlag = function(useNodeTimezone) {
    cloudPlayback.selected.useNodeTimezone = useNodeTimezone;
    $(".rec_mgr .filter_toolbar .tz_option input[type=checkbox]").prop("checked", useNodeTimezone);
    cloudPlayback.searchRecordings(true);
};

cloudPlayback.searchRecordings = function(paramsChanged, onCompleted) {
    onCompleted = onCompleted || utils.doNothing;

    if (
        cloudPlayback.selected.node == null ||
        cloudPlayback.selected.nodeCamera == null ||
        cloudPlayback.selected.date == null ||
        cloudPlayback.selected.type == null
    ) {
        onCompleted();
        return;
    }

    cloudPlayback._checkAllowTzToggle();

    //    //mock
    //    recSB.generate(
    //        cloudPlayback.selected.date.toDate(),
    //        cloudPlayback._getMockFiles(cloudPlayback.selected.date),
    //        paramsChanged
    //    );
    //    onCompleted();
    //    return;

    var target = moment(cloudPlayback.selected.date);
    var from = moment(target.startOf("day"));
    var to = moment(target.endOf("day"));

    //send api
    cloudPlayback._loading(paramsChanged);
    searchCloudRecordings(
        cloudPlayback.selected.node.id,
        cloudPlayback.selected.nodeCamera.nodeCoreDeviceId,
        cloudPlayback._getAPITimestamp(from),
        cloudPlayback._getAPITimestamp(to),
        function(responseData) {
            cloudPlayback._loading(false);

            if (responseData.result != "ok") {
                utils.throwServerError(responseData);
            } else {
                var files = cloudPlayback._adjustRecordingTimes(responseData.files);
                recSB.generate(cloudPlayback.selected.date.toDate(), files, paramsChanged);
            }

            //storage
            cloudPlayback._updateStorageInfo();

            onCompleted();
        }, null
    );
};

cloudPlayback.requestRecordings = function() {
    if (
        cloudPlayback.selected.node == null ||
        cloudPlayback.selected.nodeCamera == null ||
        cloudPlayback.selected.date == null ||
        cloudPlayback.selected.type == null ||
        cloudPlayback.selected.queryablePeriod == null
    ) {
        return;
    }

    //estimate total size
    var totalSize = 0;
    $.each(cloudPlayback.selected.files, function(i, f) {
        if (recSB.RecStatus.isRequestable(f.status)) {
            totalSize += f.fileSize;
        }
    });

    utils.popupConfirm(localizeResource('confirmation'),
        localizeResource('msg-confirm-recording-request', utils.bytesToMBString(totalSize)),
        function(choice) {
            if (!choice) {
                return;
            }

            var from = cloudPlayback.selected.queryablePeriod[0];
            var to = cloudPlayback.selected.queryablePeriod[1];

            //request backend
            cloudPlayback._loading(true);
            requestCloudRecordings(
                cloudPlayback.selected.node.id,
                cloudPlayback.selected.nodeCamera.nodeCoreDeviceId,
                cloudPlayback._getAPITimestamp(from),
                cloudPlayback._getAPITimestamp(to),
                function(responseData) {
                    cloudPlayback._loading(false);

                    if (responseData.result != "ok") {
                        utils.throwServerError(responseData);
                        return;
                    }

                    cloudPlayback.searchRecordings(false);
                }, null
            );
        });
};

cloudPlayback.deleteRequestedFiles = function() {
    if (
        cloudPlayback.selected.node == null ||
        cloudPlayback.selected.nodeCamera == null ||
        cloudPlayback.selected.date == null ||
        cloudPlayback.selected.type == null ||
        cloudPlayback.selected.queryablePeriod == null
    ) {
        return;
    }

    var from = cloudPlayback.selected.queryablePeriod[0];
    var to = cloudPlayback.selected.queryablePeriod[1];

    cloudPlayback._loading(true);
    findPendingUploadRequests(
        cloudPlayback.selected.node.id,
        cloudPlayback.selected.nodeCamera.nodeCoreDeviceId,
        cloudPlayback._getAPITimestamp(from),
        cloudPlayback._getAPITimestamp(to),
        function(responseData) {
            cloudPlayback._loading(false);

            if (responseData.result != "ok") {
                utils.throwServerError(responseData);
                return;
            }

            var confirmationMsg = localizeResource("confirm-cancel-delete-recordings");

            //requester list
            var pendingRequests = responseData.requests;
            if (pendingRequests.length > 0) {
                var uniqueIdList = [];
                var userList = [];
                $.each(pendingRequests, function(i, req) {
                    var userId = req.requesterUserId;
                    if (uniqueIdList.indexOf(userId) == -1 && (userId !== kupapi.currentUserId)) {
                        uniqueIdList.push(userId);
                        userList.push(cloudPlayback.cache.bucketUsers[userId]);
                    }
                });

                if (userList.length > 0) {
                    var template = kendo.template($("#requesterTmpl").html());
                    confirmationMsg = template(userList) + confirmationMsg;
                }
            }

            utils.popupConfirm(localizeResource('confirmation'), confirmationMsg,
                function(choice) {
                    if (!choice) {
                        return;
                    }

                    //request backend
                    cloudPlayback._loading(true);
                    deleteCloudRecordings(
                        cloudPlayback.selected.node.id,
                        cloudPlayback.selected.nodeCamera.nodeCoreDeviceId,
                        cloudPlayback._getAPITimestamp(from),
                        cloudPlayback._getAPITimestamp(to),
                        function(responseData) {
                            cloudPlayback._loading(false);

                            if (responseData.result != "ok") {
                                utils.throwServerError(responseData);
                                return;
                            }

                            cloudPlayback.searchRecordings(false);
                        }, null
                    );
                });
        }
    );
};

cloudPlayback.clearPeriodSelection = function() {
    cloudPlayback._setPlayerVisibility(false);
    recSB.fn.clearBrush();
};

cloudPlayback.downloadSelection = function() {
    var downloadableList = [];

    $.each(cloudPlayback.selected.files, function(i, f) {
        if (f.status == recSB.RecStatus.COMPLETED) {
            downloadableList.push(f);
        }
    });

    utils.popupConfirm(
        localizeResource("confirmation"),
        localizeResource("confirm-uploaded-file-download", downloadableList.length),
        function(proceed) {
            if (!proceed) {
                return;
            }

            $.each(downloadableList, function(i, f) {
                window.open(cloudPlayback._getDownloadLink(f));
            });
        });
}

cloudPlayback._setCamera = function(node, nodeCamera) {
    //update display
    var $cameraSelector = $(".rec_mgr .filter_toolbar .camera");
    var nodeName = localizeResource("select-camera");
    var cameraName = ". . .";
    if (node && nodeCamera) {
        nodeName = node.name;
        cameraName = nodeCamera.name;
    }
    $cameraSelector.find(".primary").text(nodeName);
    $cameraSelector.find(".secondary").text(cameraName);

    //Update selection
    cloudPlayback.selected.node = node;
    cloudPlayback.selected.nodeCamera = nodeCamera;

    cloudPlayback.searchRecordings(true);
    cloudPlayback._updateStorageInfo();
};

cloudPlayback._setDate = function(selectedDate) {
    if (!selectedDate.isValid()) {
        console.log("invalid date");
        return;
    }

    cloudPlayback.selected.date = selectedDate;
    cloudPlayback.searchRecordings(true);
};

cloudPlayback._startRefreshTimer = function() {
    (function delayedRefresh() {
        setTimeout(function() {
            if (cloudPlayback.autoRefresh.enabled && !recSB.isSelectionActive()) {
                cloudPlayback.searchRecordings(false, delayedRefresh);
            } else {
                delayedRefresh();
            }

        }, cloudPlayback.autoRefresh.freq);
    })();
};

cloudPlayback._setAutoRefresh = function(enabled) {
    cloudPlayback.autoRefresh.enabled = enabled;
};

cloudPlayback._initCache = function() {
    //users
    getBucketUsers("", function(responseData) {
        if (responseData.result != "ok") {
            utils.throwServerError(responseData);
            return;
        }

        $.each(responseData.users, function(i, user) {
            cloudPlayback.cache.bucketUsers[user.userId] = user;
        });
    });
};

cloudPlayback._initStorageInfo = function() {
    var color = {
        ok: "#3498DB",
        warning: "#f6ae40",
        danger: "#CF000F"
    };

    cloudPlayback.storageWidget = new ProgressBar.Line('.camera_info_box .storage_info .progress-bar', {
        duration: 500,
        step: function(state, circle) {
            var percent = circle.value();
            var colorHex;
            if (percent > 0.9) {
                colorHex = color.danger;
            } else if (percent > 0.7) {
                colorHex = color.warning;
            } else {
                colorHex = color.ok;
            }
            circle.path.setAttribute('stroke', colorHex);
        }
    });
};

cloudPlayback._updateStorageInfo = function() {
    var node = cloudPlayback.selected.node;
    var nodeCam = cloudPlayback.selected.nodeCamera;
    var $cameraInfoBox = $(".camera_info_box");

    if (!node || !nodeCam) {
        $cameraInfoBox.hide();
        return;
    }

    getNodeCameraStorage(node.id, nodeCam.nodeCoreDeviceId, function(responseData) {
        //storage values
        var limitMB = responseData.info.recordingLimitMB;
        var usageMB = responseData.info.recordingUsageMB;
        var remainingMB = limitMB - usageMB;
        var percentUsed = usageMB / limitMB;

        //if over the limit, adjust the values
        if (usageMB > limitMB) {
            console.log("Storage usage over the limit (MB):", remainingMB);
            remainingMB = 0;
            percentUsed = 1;
        }

        //text
        var $storageInfo = $cameraInfoBox.find(".storage_info");
        var infoText = localizeResource("storage-limit-tmpl", remainingMB, limitMB);

        $cameraInfoBox.show();
        $storageInfo.find(".details").html(infoText);

        //progress bar
        cloudPlayback.storageWidget.animate(limitMB === 0 ? 0 : percentUsed);
    });
};

cloudPlayback._updatePlayList = function(selectedFiles) {
    //prepare list for jw player
    var playList = [];
    $.each(selectedFiles, function(idx, file) {
        if (recSB.RecStatus.allowedToPlay(file.status) && !utils.isNullOrEmpty(file.url)) {
            var periodName = cloudPlayback._formatPeriodForJw([moment(file.startTime), moment(file.endTime)]);
            playList.push({
                title: periodName,
                image: kupapi.CdnPath + "/common/images/play_jw_opaque.png",
                file: file.url.play
            });
        }
    });

    //handle empty playlist
    var isEmpty = playList.length == 0;
    var $selectMsg = $(".rec_mgr .select_file_msg");
    var $jwPlayer = $(".rec_mgr #" + cloudPlayback.jw.playerId);
    $selectMsg.toggle(isEmpty);
    $jwPlayer.toggle(!isEmpty);

    if (!isEmpty) {
        cloudPlayback._playWithJW(playList);
    }
};

cloudPlayback._playWithJW = function(playlist) {
    var jwInstance = jwplayer(cloudPlayback.jw.playerId).setup({
        playlist: playlist,
        width: "100%",
        height: "100%",
        autostart: false,
        mute: true,
        listbar: {
            position: 'right',
            size: 280
        },
        flashplayer: "static/js/plugin/jwplayer/jwplayer.flash.swf",
        html5player: "static/js/plugin/jwplayer/jwplayer.html5.min.js",
    });

    jwInstance.onError(function(e) {
        if (e && e.message === "Error loading media: File could not be played") {
            utils.popupAlert(localizeResource("error-cloud-bandwidth-limit"));
        }
    });
};

cloudPlayback._adjustUI = function() {
    //hide left menu
    mainJS.toggleLeftBar();

    //this box uses absolute positioning, so we need to manually adjust it
    var $toolBox = $("#movingToolBox");
    var leftBarWidth = $("#sidebar").outerWidth();

    mainJS.whenLeftBarOpened(function() {
        if ($toolBox.is(":visible")) {
            $toolBox.animate({
                right: "-=" + leftBarWidth
            }, 200);
        }

        utils.disableWithOverlay($(".rec_mgr"), true);
    });

    mainJS.whenLeftBarClosed(function() {
        if ($toolBox.is(":visible")) {
            $toolBox.animate({
                right: "+=" + leftBarWidth
            }, 200);
        }

        utils.disableWithOverlay($(".rec_mgr"), false);
    });
};

cloudPlayback._initDatePicker = function() {
    var now = moment();
    cloudPlayback.kendo.datePicker = $(".rec_mgr .kendo_picker").kendoDatePicker({
        format: "d MMM yyyy",
        max: now.toDate(),
        value: now.toDate(),
        footer: false,
        origin: "top right",
        animation: {
            open: {
                effects: "slideIn:right",
                duration: 200
            },
            close: {
                duration: 100
            }
        },
        change: function() {
            cloudPlayback._setDate(moment(this.value()));
        }
    }).data("kendoDatePicker");

    cloudPlayback.kendo.datePicker.readonly();
    $(".rec_mgr .date_selector").show();
};

cloudPlayback._periodChangedHandler = function(newPeriod, selectedFiles) {
    var momentPeriod = [moment(newPeriod[0]), moment(newPeriod[1])];

    cloudPlayback.selected.files = selectedFiles;
    if (selectedFiles.length > 0) {
        //shrink selection to the actual period with files
        var firstFile = selectedFiles[0];
        var lastFile = selectedFiles[selectedFiles.length - 1];
        cloudPlayback.selected.queryablePeriod = [moment(firstFile.startTime), moment(lastFile.endTime)];
    }

    //for debugging
    if (selectedFiles.length == 1) {
        var f = selectedFiles[0];
        console.log(
            "Status:", f.status,
            ", Period:", moment(f.startTime).format("hh:mm:ss"), "-", moment(f.endTime).format("hh:mm:ss"),
            ", Size:", utils.bytesToMBString(f.fileSize),
            ", Progress:", f.progress,
            ", url:", f.url
        );
    }

    //detect selection cancelled
    var selectionCancelled = momentPeriod[0].isSame(momentPeriod[1]);
    if (selectionCancelled) {
        cloudPlayback.selected.queryablePeriod = null;
    }

    cloudPlayback._setAutoRefresh(selectionCancelled);
    cloudPlayback._updateToolBoxBtns(selectedFiles);
    cloudPlayback._updatePlayList(selectedFiles);
    cloudPlayback._setPlayerVisibility(!selectionCancelled);
};

cloudPlayback._updateToolBoxBtns = function(selectedFiles) {
    //check if request, download, cancel options should be available
    var showRequestBtn = false;
    var showCancelBtn = false;
    var showDownloadBtn = false;
    var $requestBtn = $("#movingToolBox .options .request");
    var $cancelBtn = $("#movingToolBox .options .cancel");
    var $downloadBtn = $("#movingToolBox .options .download");

    $.each(selectedFiles, function(idx, file) {
        if (recSB.RecStatus.isRequestable(file.status)) {
            showRequestBtn = true;
        }
        if (recSB.RecStatus.isDeletable(file.status)) {
            showCancelBtn = true;
        }
        if (file.status == recSB.RecStatus.COMPLETED) {
            showDownloadBtn = true;
        }
    });

    $requestBtn.toggle(showRequestBtn);
    $cancelBtn.toggle(showCancelBtn);
    $downloadBtn.toggle(showDownloadBtn);
};

cloudPlayback._getDownloadLink = function(file) {
    var filename = cloudPlayback.selected.nodeCamera.name + "_" + moment(file.startTime).format("HHmmss") + "-" + moment(file.endTime).format("HHmmss") + ".mp4";

    var errorLink = window.location.host + "/errorlanding/corebandwidthlimit";

    return file.url.download + "?action=download" +
        "&customname=" + utils.sanitizeForURL(filename, "") +
        "&errorlink=" + encodeURIComponent(errorLink);
};

cloudPlayback._formatPeriod = function(period) {
    var format = "h:mm a";
    var start = period[0].format(format);
    var end = period[1].format(format);
    return start + " - " + end;
};

cloudPlayback._formatPeriodForJw = function(period) {
    var format = "h:mm:ss";
    var start = period[0].format(format);
    var end = period[1].format(format);
    return start + " - " + end;
};

cloudPlayback._getMockFiles = function(dt) {
    var mockList = [];
    var states = Object.keys(recSB.RecStatus);
    var zeroZero = dt.startOf("day");
    var start = zeroZero.valueOf();

    for (var i = 0; i < 96; i++) {
        var oneSlot = 15 * 60 * 1000;
        var current = start + (i * oneSlot);
        if (utils.getRandomInteger(0, 4) == 1) {
            continue;
        }

        mockList.push({
            startTime: current,
            endTime: current + oneSlot - 1,
            fileSize: utils.getRandomInteger(10000, 100000),
            url: "rtmp://fms.12E5.edgecastcdn.net/0012E5/mp4:videos/8Juv1MVa-485.mp4",
            progress: utils.getRandomInteger(0, 100),
            status: states[utils.getRandomInteger(0, states.length)]
        });
    }

    return mockList;
};

cloudPlayback._setPlayerVisibility = function(visible) {
    var $displayArea = $(".rec_mgr .display_area");
    $displayArea.toggle(visible);
};

cloudPlayback._getNodeTimeZone = function() {
    var tz = {
        name: cloudPlayback.selected.node.node.settings.timezone,
        tzOffsetMins: cloudPlayback.selected.node.node.settings.tzOffsetMins
    };
    return tz;
};

cloudPlayback._getAPITimestamp = function(browserDt) {
    var shiftedDt;
    if (cloudPlayback.selected.useNodeTimezone) {
        var nodeTz = cloudPlayback._getNodeTimeZone();
        shiftedDt = cloudPlayback._shiftTimeZone(browserDt, nodeTz.tzOffsetMins);
    } else {
        shiftedDt = browserDt;
    }

    var utcDt = moment.utc(shiftedDt);
    return utcDt.format(cloudPlayback.API_TIME_FORMAT);
};

cloudPlayback._shiftTimeZone = function(dt, offsetMins) {
    var newDt = moment(dt);
    newDt.utcOffset(offsetMins);
    newDt.add(dt.utcOffset() - newDt.utcOffset(), 'minutes');
    return newDt;
};

cloudPlayback._adjustRecordingTimes = function(files) {
    if (cloudPlayback.selected.useNodeTimezone) {
        var nodeTz = cloudPlayback._getNodeTimeZone();
        var tzDiffMillis = (nodeTz.tzOffsetMins - moment().utcOffset()) * 60 * 1000;
        $.each(files, function(i, f) {
            f.startTime += tzDiffMillis;
            f.endTime += tzDiffMillis;
        });
    }
    return files;
};

cloudPlayback._checkAllowTzToggle = function() {
    var nodeTz = cloudPlayback._getNodeTimeZone();
    var allow = (nodeTz.tzOffsetMins != moment().utcOffset());
    var $tzToggleBtn = $(".rec_mgr .filter_toolbar .tz_option");
    $tzToggleBtn.toggle(allow);

    if (allow) {
        //update tz name
        $(".rec_mgr .filter_toolbar .tz_name").text("(" + nodeTz.name + ")");
    }
};

cloudPlayback._loading = function(loading) {
    kendo.ui.progress($(".rec_mgr"), loading);
};

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
        lastRequested: 0,
        userDevices: {},
        platformIdMap: {},
        coreIdMap: {},
        deviceNameMap: {},
        channelNameMap: {},
        channelStatusMap: {},
        snapshots: {}
    };

    var updateDevices = function (deviceList)
    {
        cache.lastRequested = new Date().getTime();
        cache.userDevices = {};
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
                $.each(dvc.node.cameras || [], function (i2, cam)
                {
                    cache.channelNameMap[dvc.deviceId + "_" + cam.nodeCoreDeviceId] = cam.name;
                    cache.channelStatusMap[dvc.deviceId + "_" + cam.nodeCoreDeviceId] = cam.status;
                });
            }
            else
            {
                cache.channelNameMap[dvc.deviceId + "_" + 0] = "1";
                cache.channelStatusMap[dvc.deviceId + "_" + 0] = dvc.status;
            }
        });
    };

    var init = function (callback)
    {
        var now = new Date().getTime();
        if ((now - cache.lastRequested) < cfg.cacheExpiry)
        {
            callback();
            return;
        }

        getUserDevices("", function (responseData)
        {
            if (responseData.result != "ok" || responseData.devices == null)
            {
                utils.throwServerError(responseData);
                updateDevices([]);
            }
            else
            {
                updateDevices(responseData.devices);
            }
            callback();
        });
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

    var getDevice = function (coreDeviceId)
    {
        return cache.userDevices[coreDeviceId];
    };

    var getDeviceName = function (coreDeviceId)
    {
        var name = cache.deviceNameMap[coreDeviceId];
        return utils.isNullOrEmpty(name) ? localizeResource("n/a") : name;
    };

    var getChannelName = function (coreDeviceId, channelId)
    {
        var name = cache.channelNameMap[coreDeviceId + "_" + channelId];
        return utils.isNullOrEmpty(name) ? localizeResource("n/a") : name;
    };

    var getDeviceStatus = function (coreDeviceId)
    {
        var device = cache.userDevices[coreDeviceId];
        return device == null ? DeviceStatus.UNKNOWN : DeviceStatus.parse(device.status);
    };

    var getChannelStatus = function (coreDeviceId, channelId)
    {
        var status = cache.channelStatusMap[coreDeviceId + "_" + channelId];
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

        getLiveVideoUrl("", coreDeviceId, channelId, "http/jpeg", function (responseData)
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
        isKaiNode: isKaiNode,
        toPlatformId: toPlatformId,
        toCoreId: toCoreId,
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
//this namespace will be used for all functions later on
//var kupapi = window.kupapi || {};
kupapi.debugMode = false;
//kupapi.applicationType = null;
kupapi.timeZoneOffset = 0;
kupapi.TIME_FORMAT = "dd/MM/yyyy HH:mm:ss"; //DO NOT EDIT THIS FORMAT
kupapi.TIME_ONLY_FORMAT = "HH:mm"; //DO NOT EDIT THIS FORMAT
kupapi.API_TIME_FORMAT = "ddMMyyyyHHmmss";
//kupapi.CdnPath = null;
kupapi.privateCloud = false;
kupapi.mapSource = "google";
kupapi.platformVersion = null;
kupapi.internetStatus = null;
//kupapi.currentUserId = null;

//var kupBucket = null;
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
    //cors setting     
    url = apiRootUrl + url;  
    params["session-key"] = kupSessionKey;
    
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

function getLiveVideoUrl(sessionKey, deviceId, channelId, streamType, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "stream-type": streamType
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

function addAreaIntrusion(sessionKey, deviceId, channelId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/addareaintrusion";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateAreaIntrusion(sessionKey, instanceId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/updateareaintrusion";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeAreaIntrusion(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/removeareaintrusion";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function activateAreaIntrusion(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/activateareaintrusion";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deactivateAreaIntrusion(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/deactivateareaintrusion";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addPerimeterDefense(sessionKey, deviceId, channelId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/addperimeterdefense";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updatePerimeterDefense(sessionKey, instanceId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/updateperimeterdefense";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removePerimeterDefense(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/removeperimeterdefense";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function activatePerimeterDefense(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/activateperimeterdefense";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deactivatePerimeterDefense(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/deactivateperimeterdefense";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addAreaLoitering(sessionKey, deviceId, channelId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/addarealoitering";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateAreaLoitering(sessionKey, instanceId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/updatearealoitering";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeAreaLoitering(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/removearealoitering";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function activateAreaLoitering(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/activatearealoitering";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deactivateAreaLoitering(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/deactivatearealoitering";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addObjectCounting(sessionKey, deviceId, channelId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/addobjectcounting";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateObjectCounting(sessionKey, instanceId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/updateobjectcounting";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeObjectCounting(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/removeobjectcounting";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function activateObjectCounting(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/activateobjectcounting";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deactivateObjectCounting(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/deactivateobjectcounting";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addVideoBlur(sessionKey, deviceId, channelId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/addvideoblur";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateVideoBlur(sessionKey, instanceId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/updatevideoblur";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeVideoBlur(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/removevideoblur";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function activateVideoBlur(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/activatevideoblur";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deactivateVideoBlur(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/deactivatevideoblur";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addFaceIndexing(sessionKey, deviceId, channelId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/addfaceindexing";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateFaceIndexing(sessionKey, instanceId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/updatefaceindexing";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeFaceIndexing(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/removefaceindexing";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function activateFaceIndexing(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/activatefaceindexing";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deactivateFaceIndexing(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/deactivatefaceindexing";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addTrafficFlow(sessionKey, deviceId, channelId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/addtrafficflow";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateTrafficFlow(sessionKey, instanceId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/updatetrafficflow";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeTrafficFlow(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/removetrafficflow";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function activateTrafficFlow(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/activatetrafficflow";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deactivateTrafficFlow(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/deactivatetrafficflow";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addPeopleCounting(sessionKey, deviceId, channelId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/addpeoplecounting";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updatePeopleCounting(sessionKey, instanceId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/updatepeoplecounting";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removePeopleCounting(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/removepeoplecounting";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function activatePeopleCounting(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/activatepeoplecounting";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deactivatePeopleCounting(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/deactivatepeoplecounting";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addCrowdDetection(sessionKey, deviceId, channelId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/addcrowddetection";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateCrowdDetection(sessionKey, instanceId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/updatecrowddetection";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeCrowdDetection(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/removecrowddetection";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function activateCrowdDetection(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/activatecrowddetection";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deactivateCrowdDetection(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/deactivatecrowddetection";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addAudienceProfiling(sessionKey, deviceId, channelId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/addaudienceprofiling";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateAudienceProfiling(sessionKey, instanceId, thresholds, recurrenceRule, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId,
        "thresholds": thresholds,
        "recurrence-rule": utils.isNullOrEmpty(recurrenceRule) ? "" : JSON.stringify(recurrenceRule)
    };
    var url = "/api/" + kupBucket + "/updateaudienceprofiling";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeAudienceProfiling(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/removeaudienceprofiling";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function activateAudienceProfiling(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/activateaudienceprofiling";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function deactivateAudienceProfiling(sessionKey, instanceId, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "instance-id": instanceId
    };
    var url = "/api/" + kupBucket + "/deactivateaudienceprofiling";
    return ajaxPost(url, params, onSuccess, onFailure);
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
        "max-vca-count": bucketSetting.maxvcacount,
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
function getAlerts(sessionKey, eventId, eventType, skip, take, deviceId, channelId, from, to, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "event-id": eventId,
        "event-type": eventType,
        "skip": skip,
        "take": take,
        "device-id": deviceId,
        "channel-id": channelId,
        "from": from,
        "to": to
    };
    var url = "/api/" + kupBucket + "/getalerts";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function exportAlerts(fileFormat, deviceId, channelId, eventType, from, to) {
    var params = {
        "file-format": fileFormat,
        "time-zone-offset": kupapi.timeZoneOffset,
        "device-id": deviceId,
        "channel-id": channelId,
        "event-type": eventType,
        "from": from,
        "to": to
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

function addNodeLicense(sessionKey, bucketId, durationMonths, cloudStorageGb, maxVcaCount, featureNameList, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "bucket-id": bucketId,
        "duration-months": durationMonths,
        "cloud-storage-gb": cloudStorageGb,
        "max-vca-count": maxVcaCount,
        "features": JSON.stringify(featureNameList)
    };
    var url = "/api/" + kupBucket + "/addnodelicense";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function updateNodeLicense(sessionKey, licenseNumber, durationMonths, cloudStorageGb, maxVcaCount, featureNameList, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "license-number": licenseNumber,
        "duration-months": durationMonths,
        "cloud-storage-gb": cloudStorageGb,
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

function getBucketLabels(sessionKey, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey
    };
    var url = "/api/" + kupBucket + "/getbucketlabels";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function addBucketLabel(sessionKey, labelName, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "label-name": labelName
    };
    var url = "/api/" + kupBucket + "/addbucketlabel";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function removeBucketLabel(sessionKey, labelName, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "label-name": labelName
    };
    var url = "/api/" + kupBucket + "/removebucketlabel";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function unassignDeviceLabel(sessionKey, deviceId, labelName, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "label-name": labelName
    };
    var url = "/api/" + kupBucket + "/unassigndevicelabel";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function unassignNodeCameraLabel(sessionKey, deviceId, channelId, labelName, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "label-name": labelName
    };
    var url = "/api/" + kupBucket + "/unassignnodecameralabel";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function assignDeviceLabel(sessionKey, deviceId, labelName, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "label-name": labelName
    };
    var url = "/api/" + kupBucket + "/assigndevicelabel";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function assignNodeCameraLabel(sessionKey, deviceId, channelId, labelName, onSuccess, onFailure) {
    var params = {
        "session-key": sessionKey,
        "device-id": deviceId,
        "channel-id": channelId,
        "label-name": labelName
    };
    var url = "/api/" + kupBucket + "/assignnodecameralabel";
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

function getPeriodicReportSettings(bucketId, onSuccess, onFailure) {
    var params = {
        "bucket-id": bucketId
    };
    var url = "/api/" + kupBucket + "/getperiodicreportsettings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function savePeriodicReportSettings(bucketId, settings, onSuccess, onFailure) {
    var params = {
        "bucket-id": bucketId,
        "frequencies": JSON.stringify(settings.frequencies),
        "export-formats": JSON.stringify(settings.exportFormats),
        "delivery-methods": JSON.stringify(settings.deliveryMethods),
        "mailing-list": JSON.stringify(settings.mailingList),
        "ftp-details": JSON.stringify(settings.ftpDetails)
    };
    var url = "/api/" + kupBucket + "/saveperiodicreportsettings";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function getPeriodicReports(frequency, from, to, skip, take, onSuccess, onFailure) {
    var params = {
        "frequency": frequency,
        "from": from,
        "to": to,
        "skip": skip,
        "take": take
    };
    var url = "/api/" + kupBucket + "/getperiodicreports";
    return ajaxPost(url, params, onSuccess, onFailure);
}

function downloadPeriodicReport(reportId, format) {
    var params = {
        "report-id": reportId,
        "format": format
    };

    sendGETRequest(params, "downloadperiodicreport");
}

function removePeriodicReport(reportId, onSuccess, onFailure) {
    var params = {
        "report-id": reportId
    };
    var url = "/api/" + kupBucket + "/removeperiodicreport";
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

function getNodeObjectInfo(nodeId, onSuccess) {
    var params = {
        "platform-device-id" : nodeId
    };
    var url = "/api/" + kupBucket + "/getnodeobjectinfo";
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


var recSB = {
    containerId: null,
    showLegend: (kupapi.applicationType == "cloud"),
    current: {
        date: new Date(),
        periodMillis: [0, 0],
        files: []
    },
    minDuration: 15,    // hardcoded to match 15-min files
    fn: {
        clearBrush: null
    },
    evt: {
        periodChanged: null
    },
    padding: {top: 15, right: 30, bottom: 10, left: 20}
};

recSB.RecStatus = Object.freeze({
    UNREQUESTED: "UNREQUESTED",
    REQUESTED: "REQUESTED",
    UPLOADING: "UPLOADING",
    COMPLETED: "COMPLETED",
    RETRYING: "RETRYING",
    ABORTED: "ABORTED",
    MISSING: "MISSING",

    isRequestable: function (status)
    {
        var list = [
            this.UNREQUESTED,
            this.ABORTED
        ];
        return list.indexOf(status) != -1;
    },
    isDeletable: function (status)
    {
        var list = [
            this.REQUESTED,
            this.UPLOADING,
            this.COMPLETED,
            this.RETRYING
        ];
        return list.indexOf(status) != -1;
    },
    allowedToPlay: function (status)
    {
        var list = [
            this.UPLOADING,
            this.COMPLETED
        ];
        return list.indexOf(status) != -1;
    }
});

recSB.statusColors = {
    UNREQUESTED: "#004D60", // dark blue tint
    REQUESTED: "#f6ae40",   // orange
    UPLOADING: "#428bca",   // blue
    COMPLETED: "#8dc051",   // green
    RETRYING: "#7E3F12",    // brown
    ABORTED: "#ff2a3f",     // red
    MISSING: "transparent"
};

recSB.init = function (containerId, periodChanged)
{
    recSB.containerId = containerId;
    recSB.evt.periodChanged = periodChanged;
    recSB._draw();
};

recSB.generate = function (targetDate, streamFiles, animate)
{
    recSB.current.date = targetDate;
    recSB.current.files = recSB._prepareFiles(streamFiles);
    recSB._draw(animate);
    recSB._handleEmptyRecordingsList();
};

recSB.reset = function ()
{
    recSB.current.files = [];
    recSB._draw(false);
    $(".rec_mgr .msg_no_recordings").hide();
};

recSB.remove = function ()
{
    if (recSB.containerId == null)
    {
        return;
    }

    if (recSB.fn.clearBrush)
    {
        recSB.fn.clearBrush();
    }

    var container = document.getElementById(recSB.containerId);
    if (container == null)
    {
        return;
    }

    while (container.firstChild)
    {
        container.removeChild(container.firstChild);
    }
};

recSB.isSelectionActive = function ()
{
    return $("#movingToolBox").is(":visible");
};

recSB._prepareFiles = function (files)
{
    $.each(files, function (i, f)
    {
        //add rounded start and end times to remove minor gaps (< 1 mins)
        //this is just to make UI look nicer
        f.roundedStart = recSB._roundTo15MinMarks(f.startTime);
        f.roundedEnd = recSB._roundTo15MinMarks(f.endTime);
    });

    return files;
};

recSB._draw = function (paramsChanged)
{
    recSB.remove();

    //set container size
    var $wrapper = $("#" + recSB.containerId).parent();
    var svgW = $wrapper.width() - recSB.padding.right;
    var svgH = $wrapper.height();

    //set draw area size
    var innerW = svgW - (recSB.padding.left + recSB.padding.right);
    var innerH = svgH - (recSB.padding.top + recSB.padding.bottom);

    //horizontal scale
    var current = recSB.current.date;
    var start = new Date(current.getFullYear(), current.getMonth(), current.getDate(), 0, 0, 0);
    var end = new Date(current.getFullYear(), current.getMonth(), current.getDate() + 1, 0, 0, 0);

    //constants
    var barCount = 96; //15min segments
    var tickLabelHeight = 42;
    var legendHeight = 10;
    var legendColorWidth = 10;
    var legendLabelWidth = 100;

    var chartH = innerH - (tickLabelHeight + legendHeight);
    var barWidth = innerW / barCount;
    var barMaxHeight = chartH;


    var xScale = d3.time.scale()
        .domain([start, end])
        .range([0, innerW]);

    //selector
    var brush = d3.svg.brush()
        .x(xScale)
        .on("brush", brushed)
        .on("brushend", brushEnded);

    /**
     *
     * Drawing starts
     *
     */
    var svgContainer = d3.select("#" + recSB.containerId)
        .append("svg")
        .attr("width", svgW)
        .attr("height", svgH - 5);

    var contentWrapper = svgContainer.append("g")
        .attr("transform", "translate(" + recSB.padding.left + ", " + recSB.padding.top + ")");

    //Color segments
    var segments = contentWrapper.selectAll("rect")
        .data(recSB.current.files)
        .enter()
        .append("rect")
        .attr("shape-rendering", "crispEdges")
        .attr("stroke-width", 0)
        .attr("stroke", "#505050")
        .attr("fill-opacity", function (d)
        {
            return d.status == recSB.RecStatus.UNREQUESTED ? 0.5 : 1;
        })
        .attr("x", function (d, i)
        {
            var startDt = new Date(d.roundedStart);
            return xScale(startDt);
        })
        .attr("y", function (d)
        {
            return chartH - recSB._getSegmentHeight(d, chartH);
        })
        .attr("width", function (d)
        {
            var durationWidth = barWidth * ((d.roundedEnd - d.roundedStart) / (15 * 60 * 1000));
            return durationWidth;
        })
        .attr("height", function (d, i)
        {
            return recSB._getSegmentHeight(d, barMaxHeight);
        })
        .attr("fill", function (d, i)
        {
            return recSB.statusColors[d.status];
        })
        .attr("opacity", 0.9);

    //animate all for first time loading
    if (paramsChanged)
    {
        var stillBars = segments.filter(function (d)
        {
            return d.status != recSB.RecStatus.UPLOADING;
        });
        stillBars.attr("opacity", 0);
        stillBars.transition()
            .duration(500)
            .attr("opacity", 0.9);
    }

    /**
     *
     * blink uploading segment
     *
     */
    var progressBars = segments.filter(function (d)
    {
        return (d.status == recSB.RecStatus.UPLOADING);
    });

    var dur = 500;
    (function blinkUploading()
    {
        progressBars
            .transition()
            .duration(dur)
            .attr("fill", recSB.statusColors[recSB.RecStatus.COMPLETED])
            .each("end", function ()
            {
                d3.select(this)
                    .transition()
                    .duration(dur)
                    .attr("fill", function (d, i)
                    {
                        return recSB.statusColors[d.status];
                    });
            });
        setTimeout(blinkUploading, 2 * dur);
    })();

    //hour labels
    contentWrapper.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + chartH + ")")
        .call(d3.svg.axis()
            .scale(xScale)
            .orient("bottom")
            .ticks(d3.time.hours, 1)
            .tickPadding(4))
        .selectAll("text")
        .attr("x", -16)
        .style("text-anchor", null)
        .style("font-size", 11);

    //hour ticks
    contentWrapper.append("g")
        .attr("class", "x grid")
        .attr("transform", "translate(0," + chartH + ")")
        .call(d3.svg.axis()
            .scale(xScale)
            .orient("bottom")
            .ticks(d3.time.minutes, 15)
            .tickSize(-chartH)
            .tickFormat(""))
        .selectAll(".tick")
        .classed("minor", function (d)
        {
            return d.getMinutes();
        });

    /**
     *
     * Legend
     *
     */
    if (recSB.showLegend)
    {
        var legendWrapper = svgContainer.append("g")
            .attr("transform", "translate(" + 10 + ", " + innerH + ")");

        var legendEntries = [
            recSB.RecStatus.UNREQUESTED,
            recSB.RecStatus.REQUESTED,
            recSB.RecStatus.UPLOADING,
            recSB.RecStatus.COMPLETED,
            recSB.RecStatus.RETRYING,
            recSB.RecStatus.ABORTED
        ];

        //legend colors
        // legendWrapper.selectAll("rect")
        //     .data(legendEntries)
        //     .enter()
        //     .append("rect")
        //     .attr("width", legendColorWidth)
        //     .attr("height", legendHeight)
        //     .attr("fill", function (d, i)
        //     {
        //         return recSB.statusColors[d];
        //     })
        //     .attr("x", function (d, i)
        //     {
        //         var posX = i * (legendColorWidth + legendLabelWidth);
        //         return posX;
        //     })
        //     .attr("y", 0);

        //legend labels
        // legendWrapper.selectAll("text")
        //     .data(legendEntries)
        //     .enter()
        //     .append("text")
        //     .attr("width", legendLabelWidth)
        //     .attr("height", legendHeight)
        //     .attr("fill", "#999")
        //     .attr("x", function (d, i)
        //     {
        //         var posX = legendColorWidth + (i * (legendColorWidth + legendLabelWidth));
        //         return posX + 5;
        //     })
        //     .attr("y", legendHeight - 1)
        //     .text(function (d, i)
        //     {
        //         return localizeResource("rec-status-" + d);
        //     });
        var legendWrapper = function (){
            var html = '<ul class="legendWrapper" style="width:'+svgW+'px;">';
            $.each(legendEntries, function(index, text) {
                html += "<li class='status'>";
                html += '<span class="colors" style="width:'+legendColorWidth+'px;height:'+legendHeight+'px;background-color:'+recSB.statusColors[text]+'"></span>';
                html += localizeResource("rec-status-" + text);
                html += "</li>";
             });
            html += "</ul>";
            return html;
        };

        $("#" + recSB.containerId).append(legendWrapper);
    }

    //Disable brush if there are no selectable files
    if (utils.isListEmpty(recSB.current.files))
    {
        return;
    }

    /**
     *
     * Brush
     *
     */
    var gBrush = contentWrapper.append("g")
        .attr("class", "brush")
        .call(brush);
    gBrush.selectAll("rect")
        .attr("height", chartH + tickLabelHeight)
        .attr("transform", "translate(0," + -(recSB.padding.top) + ")");

    /**
     *
     *  Internal Functions
     *
     */
    function brushed(e)
    {
        var origExtent = brush.extent();

        //floor start and end
        var rounded0 = floorMinutes(origExtent[0]);
        var rounded1 = floorMinutes(origExtent[1]);

        //minimum check
        if ((rounded1.getTime() - rounded0.getTime()) < recSB.minDuration)
        {
            rounded1.setMinutes(rounded1.getMinutes() + recSB.minDuration);
        }

        var snappedExtent = [rounded0, rounded1];
        d3.select(this).call(brush.extent(snappedExtent));

        //update info box
        var rectExtent = gBrush.select("rect.extent");
        var position = rectExtent.node().getBoundingClientRect();
        recSB._updatePeriodInfoBox(snappedExtent, position)
    }

    function floorMinutes(dt)
    {
        var minutes = dt.getMinutes();
        var intervals = [0, 15, 30, 45, 60];

        for (var i = 1; i < intervals.length; i++)
        {
            if (minutes < intervals[i])
            {
                var rounded = intervals[i - 1];
                return new Date(dt.getFullYear(), dt.getMonth(), dt.getDate(), dt.getHours(), rounded, 0);
            }
        }

        return 0;
    }

    function brushEnded()
    {
        var period = brush.extent();
        var from = period[0].getTime();
        var to = period[1].getTime();

        //check if period changes
        if (recSB.current.periodMillis[0] == from &&
            recSB.current.periodMillis[1] == to)
        {
            return;
        }

        recSB.current.periodMillis = [from, to];
        var selectedFiles = [];
        recSB.current.files.forEach(function (file)
        {
            var startTime = file.roundedStart;
            if (startTime >= from && startTime < to)
            {
                selectedFiles.push(file);
            }
        });

        recSB.evt.periodChanged(period, selectedFiles);
    }

    recSB.fn.clearBrush = function ()
    {
        //temporarily set min duration to zero
        var tmpDur = recSB.minDuration
        recSB.minDuration = 0;

        //clear
        brush.clear().event(d3.select(".brush"));
        var $movingInfoBox = $("#movingToolBox");
        $movingInfoBox.hide();

        //restore min duration
        recSB.minDuration = tmpDur;
    }
};

recSB._updatePeriodInfoBox = function (period, position)
{
    //update period label
    var $movingInfoBox = $("#movingToolBox");
    var $periodInfo = $movingInfoBox.find(".period_status");
    $periodInfo.html(recSB._formatPeriod(period));

    //magic numbers
    var top = -58;
    var borderWidthOffset = 2;

    //calculate position
    var $svgBox = $("#" + recSB.containerId);
    var boxWidth = $movingInfoBox.outerWidth();
    var right = $svgBox.outerWidth() - (position.left + boxWidth) + borderWidthOffset + 22;

    //detect right boundary
    if (right < 0)
    {
        right = 0;
    }

    //move box
    $movingInfoBox.css("position", "absolute");
    $movingInfoBox.css("top", top);
    $movingInfoBox.css("right", right);
    $movingInfoBox.show();
};

recSB._handleEmptyRecordingsList = function ()
{
    var empty = utils.isListEmpty(recSB.current.files);
    var $emptyMsg = $(".rec_mgr .msg_no_recordings");
    if (empty)
    {
        $emptyMsg.show();
    }
    else
    {
        $emptyMsg.hide();
    }
};

recSB._formatPeriod = function (period)
{
    var format = "h:mm tt";
    var start = kendo.toString(period[0], format);
    var end = kendo.toString(period[1], format);
    return start + " - " + end;
};

recSB._roundTo15MinMarks = function (origMillis)
{
    var dtOrig = moment(origMillis);
    var dtZero = moment(dtOrig).minutes(0).seconds(0).milliseconds(0);

    var plusOrMinus = 1;
    var roundTimes = [
        dtZero,
        moment(dtZero).add(15, "minutes"),
        moment(dtZero).add(30, "minutes"),
        moment(dtZero).add(45, "minutes"),
        moment(dtZero).add(60, "minutes")
    ];

    var roundedMillis = origMillis;
    $.each(roundTimes, function (i, roundTime)
    {
        var minus = moment(roundTime).subtract(plusOrMinus, "minutes");
        var plus = moment(roundTime).add(plusOrMinus, "minutes");
        if (dtOrig.isBefore(minus) || dtOrig.isAfter(plus))
        {
            return true;
        }

        roundedMillis = roundTime.toDate().getTime();
        return false;
    });

    return roundedMillis;
};

recSB._getSegmentHeight = function (segment, maxHeight)
{
    if (segment.progress > 0)
    {
        return maxHeight * (segment.progress / 100);
    }

    return maxHeight;
};

var utils = {};
var alertMsgTimeOut = null;
var infoMsgTimeOut = null;
var activePopupId = "";
var flag = 0;
var flag1 = 0;
var loadingTimer = null;

utils.throwServerError = function (responseData)
{

    utils.hideLoadingOverlay();

    var errorMsg = "";
    var redirect = false;

    if (responseData == null || responseData.reason == null)
    {
        console.warn("Server error! responseData:" + JSON.stringify(responseData));
        return;
    }
    else
    {
        if (responseData.reason == "timeout")
        {
            errorMsg = localizeResource("session-expired") + "!";
            redirect = true;
        }
        else
        {
            errorMsg = localizeResource(responseData.reason);
        }
    }

    utils.popupAlert(errorMsg, function ()
    {
        if (redirect)
        {
            if (document.getElementById("iframePopup"))
            {
                window.parent.location.href = "/" + kupBucket;
            }
            else
            {
                window.location.href = "/" + kupBucket;
            }
        }
    });
}

utils.popupAlert = function (alertMsg, callback)
{

    var contentHtml = '<div style="min-height:40px">' + alertMsg + '</div>';

    var kendoWindow = $("<div />").kendoWindow({
        visible: false,
        title: localizeResource("system-message"),
        resizable: false,
        modal: true,
        width: 300,
        close: callback
    });

    kendoWindow.data("kendoWindow").content(contentHtml).center().open();
    $(".k-window-title").css("height", "30px");
}

utils.detectBrowser = function ()
{
    function testCSS(prop)
    {
        return prop in document.documentElement.style;
    }

    var isOpera = !!(window.opera && window.opera.version);
    var isFirefox = testCSS('MozBoxSizing');
    var isSafari = Object.prototype.toString.call(window.HTMLElement).indexOf('Constructor') > 0;
    var isChrome = !isSafari && testCSS('WebkitTransform');
    var isIE = /* @cc_on!@ */ false || testCSS('msTransform');

    var browser = "";
    if (isOpera)
    {
        browser = "opera";
    }
    else if (isFirefox)
    {
        browser = "firefox";
    }
    else if (isSafari)
    {
        browser = "safari";
    }
    else if (isChrome)
    {
        browser = "chrome";
    }
    else if (isIE)
    {
        browser = "ie";
    }

    return browser;
}

utils.detectOS = function ()
{
    var OSName = "Unknown OS";
    if (navigator.appVersion.indexOf("Win") != -1)
    {
        OSName = "Windows";
    }
    if (navigator.appVersion.indexOf("Mac") != -1)
    {
        OSName = "MacOS";
    }
    if (navigator.appVersion.indexOf("X11") != -1)
    {
        OSName = "UNIX";
    }
    if (navigator.appVersion.indexOf("Linux") != -1)
    {
        OSName = "Linux";
    }
    return OSName;
}

utils.convertToUTC = function (dateObj)
{
    return new Date(dateObj.getUTCFullYear(),
        dateObj.getUTCMonth(),
        dateObj.getUTCDate(),
        dateObj.getUTCHours(),
        dateObj.getUTCMinutes(),
        dateObj.getUTCSeconds());
}

utils.convertToHourBoundriesUTC = function (dateObj, type)
{
    if (type == "from")
    {
        dateObj.setUTCMinutes(00);
        dateObj.setUTCSeconds(00);
        return new Date(dateObj.getUTCFullYear(),
            dateObj.getUTCMonth(),
            dateObj.getUTCDate(),
            dateObj.getUTCHours(),
            dateObj.getUTCMinutes(),
            dateObj.getUTCSeconds());
    }
    else if (type == "to")
    {
        if (dateObj.getUTCMinutes() > 0)
        {
            dateObj.setUTCHours(dateObj.getUTCHours() + 1);
            dateObj.setUTCMinutes(00);
            dateObj.setUTCSeconds(00);
            return new Date(dateObj.getUTCFullYear(),
                dateObj.getUTCMonth(),
                dateObj.getUTCDate(),
                dateObj.getUTCHours(),
                dateObj.getUTCMinutes(),
                dateObj.getUTCSeconds());
        }
        else
        {
            dateObj.setUTCMinutes(00);
            dateObj.setUTCSeconds(00);
            return new Date(dateObj.getUTCFullYear(),
                dateObj.getUTCMonth(),
                dateObj.getUTCDate(),
                dateObj.getUTCHours(),
                dateObj.getUTCMinutes(),
                dateObj.getUTCSeconds());
        }
    }
}

/**
 * Convert UTC to Local time.
 * @param {Date} dateObj
 * @return {Date}
 */
utils.convertUTCtoLocal = function (dateObj)
{
    if (dateObj != null)
    {
        var d = new Date();
        dateObj.setMinutes(dateObj.getMinutes() - d.getTimezoneOffset());
        return dateObj;
    }
}

utils.popupConfirm = function (title, confirmMsg, callback)
{
    var choice = false;
    $("#popupWin .confirm_msg").html(confirmMsg);
    var contentHtml = $("#popupWin").html();
    var kendoWindow = $("#popupWin").kendoWindow({
        visible: false,
        title: title,
        resizable: false,
        modal: true,
        width: 300,
        close: onClosed
    });
    $("#popupWin").parent().find(".k-window-action").css("visibility", "hidden");

    function onClosed()
    {
        callback(choice);
    }

    kendoWindow.data("kendoWindow").content(contentHtml).center().open();

    $("#btnConfirmOk").click(function ()
    {
        choice = true;
        $("#popupWin").data("kendoWindow").close();
    });

    $("#btnConfirmCancel").click(function ()
    {
        choice = false;
        $("#popupWin").data("kendoWindow").close();
    });
}

utils.slideDownAlert = function (timestamp, alertType, deviceName)
{

    var localDt = utils.convertUTCtoLocal(kendo.parseDate(timestamp, kupapi.TIME_FORMAT));
    timestamp = kendo.toString(localDt, kupapi.TIME_FORMAT);

    var msgHtml = '<div class="alert_box_title">' + timestamp + '</div>' + '<div class="alert_box_content">' + '<label class="alert_box_label">' + localizeResource('device-name') + '</label>: ' + deviceName + '<br/>' + '<label class="alert_box_label">' + localizeResource('event-type') + '</label>: ' + localizeResource(alertType) + '<br/>' + '</div>';

    var notifyBox = document.getElementById("notifyBox") ? $("#notifyBox") : window.parent.$("#notifyBox");
    var alertBox = document.getElementById("alertBox") ? $("#alertBox") : window.parent.$("#alertBox");

    clearTimeout(alertMsgTimeOut);
    clearTimeout(infoMsgTimeOut);
    notifyBox.hide();

    alertBox.html(msgHtml);
    alertBox.show("slide", {
        direction: "up"
    }, 1000);
    alertMsgTimeOut = setTimeout(function ()
    {
        alertBox.hide("slide", {
            direction: "up"
        }, 1000);
    }, 8000);
}

utils.slideDownInfo = function (notifyMsg)
{

    var msgHtml = '<div class="success_image"><span>' + notifyMsg + '</span></div>';

    var notifyBox = null;
    var alertBox = null;

    if (document.getElementById("notifyBox"))
    {
        notifyBox = $("#notifyBox");
        alertBox = $("#alertBox");

        clearTimeout(alertMsgTimeOut);
        clearTimeout(infoMsgTimeOut);
        alertBox.hide();
        notifyBox.html(msgHtml);
        notifyBox.fadeIn(1000);

        infoMsgTimeOut = setTimeout(function ()
        {
            notifyBox.fadeOut(1000);
        }, 3000);
    }
    else
    {
        notifyBox = window.parent.$("#notifyBox");
        alertBox = window.parent.$("#alertBox");

        clearTimeout(window.parent.alertMsgTimeOut);
        clearTimeout(window.parent.infoMsgTimeOut);
        alertBox.hide();
        notifyBox.html(msgHtml);
        notifyBox.fadeIn(1000);

        window.parent.infoMsgTimeOut = window.parent.setTimeout(function ()
        {
            notifyBox.fadeOut(1000);
        }, 3000);
    }
}

utils.removeLineBreaks = function (nlStr)
{
    return nlStr.replace(new RegExp('\n', 'g'), ' ');
}

// converts string to json
utils.getJSonObject = function (value)
{
    return $.parseJSON(value.replace(/&quot;/ig, '"'));
}

utils.showLoadingOverlay = function ()
{
    $(".darkened-overlay").show();
    $(".loading-icon-overlay").show();
    kendo.ui.progress($(".k-window-content"), true);
}

utils.showLoadingTextOverlay = function (displayText, showCloseButton)
{
    $("#loadingText").html(displayText);
    $(".darkened-overlay").show();
    $(".loading-text-overlay").show();

    // count the time taken for close button
    if (showCloseButton)
    {
        $("#loadingTimeSeconds").html("");
        $("#loadingTimeSeconds").show();
        var timeTaken = 0;

        loadingTimer = setInterval(function ()
        {
            timeTaken++;
            $("#loadingTimeSeconds").html(timeTaken + "s");

            if (timeTaken > 10)
            {
                $("#btnLoadingCancel").show();
            }
        }, 1000);
    }
}

utils.hideLoadingOverlay = function ()
{
    $(".darkened-overlay").hide();
    $(".loading-icon-overlay").hide();
    $(".loading-text-overlay").hide();
    $("#btnLoadingCancel").hide();
    $("#loadingTimeSeconds").hide();
    kendo.ui.progress($(".k-window-content"), false);

    if (loadingTimer)
    {
        clearInterval(loadingTimer);
    }
}

utils.loadIframe = function (iframeName, url)
{
    var $iframe = $('#' + iframeName);
    if ($iframe.length)
    {
        $iframe.attr('src', url);
        return false;
    }
}

utils.preloadImage = function (imgUrl, onSuccess, onFailure)
{
    var img = new Image();
    utils.showLoadingTextOverlay(localizeResource("loading-image"), true);
    $(img).load(function ()
    {
        utils.hideLoadingOverlay();
        onSuccess();
    }).error(function ()
    {
        utils.hideLoadingOverlay();
        onFailure();
    }).attr('src', imgUrl);
}

// divId can be id or class name
utils.createTooltip = function (divId, position, content)
{
    var contentHtml = "<span style='font-size:11px;text-align:left; display: inline-block; max-width:200px'>" + content + "</span>"

    var domElement;
    if (document.getElementById(divId))
    {
        domElement = $("#" + divId);
    }
    else
    {
        domElement = $("." + divId);
    }

    domElement.kendoTooltip({
        position: position,
        content: contentHtml
    }).data("kendoTooltip");
}

utils.createDateTimeRangeSelection = function (fromDivId, toDivId)
{

    var now = new Date();
    var todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
    var todayEnd = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 0, 0, -1);

    var start = $("#" + fromDivId).kendoDateTimePicker({
        interval: 60,
        format: "dd/MM/yyyy HH:mm",
        timeFormat: "HH:mm",
        value: todayStart,
        change: function (e)
        {
            var startDate = start.value();
            if (startDate)
            {
                end.min(new Date(startDate));
            }
        }
    }).data("kendoDateTimePicker");

    var end = $("#" + toDivId).kendoDateTimePicker({
        interval: 60,
        format: "dd/MM/yyyy HH:mm",
        timeFormat: "HH:mm",
        value: todayEnd,
        change: function (e)
        {
            var endDate = end.value();
            if (endDate)
            {
                start.max(new Date(endDate));
            }
        }
    }).data("kendoDateTimePicker");

    start.max(end.value());
    end.min(start.value());
    end.max(todayEnd);
}

utils.checkInternetAccess = function (onlineCallback, offlineCallback)
{
    var img = document.createElement("img");
    img.onload = onlineCallback;
    img.onerror = offlineCallback;
    img.src = "http://pic2.pbsrc.com/navigation/brand-logo.png?v=" + Math.random();
}

utils.getRandomInteger = function (min, max)
{
    return Math.floor((Math.random() * max) + min);
}

utils.getRandomDecimal = function (min, max)
{
    return (Math.random() * max) + min;
}

utils.getScript = function (src)
{
    document.write('<script type="text/javascript" src="' + src + '"></script>');
}

utils.captureEnterKey = function (divId, callback)
{

    $("#" + divId).bind('keypress', function (e)
    {
        var code = (e.keyCode ? e.keyCode : e.which);
        if (code == 13)
        { // ENTER keycode
            callback();
        }
    });
}

utils.httpGet = function (theUrl)
{
    var xmlHttp = null;
    xmlHttp = new XMLHttpRequest();
    xmlHttp.open("GET", theUrl, false);
    xmlHttp.send(null);
    return xmlHttp.responseText;
}

// width and height fields are optional, set null to auto-resize based on
// content
utils.openPopup = function (title, contentUrl, width, height, isModel, onPopupClosed)
{
    var winId = Math.random().toString(8).slice(2);
    $(document.body).append('<div id="' + winId + '" style="overflow: hidden"></div>');

    function onClosed(e)
    {
        $(".k-animation-container").css("display", "none");
        $(".k-list-container").css("display", "none");
        $(".pac-container").css("display", "none");
        $(".k-list-container").css("transform", "");
        $("#" + winId).html("");
        onPopupClosed(e);
    }

    var winOptions = {};
    winOptions.title = title;
    winOptions.content = contentUrl;
    //winOptions.content = "/" + kupBucket + contentUrl;
    winOptions.resizable = false;
    winOptions.modal = isModel;
    winOptions.close = onClosed;
    winOptions.visible = false;

    if (!utils.isNullOrEmpty(width))
    {
        winOptions.width = width;
    }

    if (!utils.isNullOrEmpty(height))
    {
        winOptions.height = height;
    }

    var autoAdjust = utils.isNullOrEmpty(width) || utils.isNullOrEmpty(height);
    if (autoAdjust)
    {
        winOptions.refresh = function ()
        {
            this.center();
        }
    }

    var kendoWin = $("#" + winId).kendoWindow(winOptions).data("kendoWindow").center().open();
}

utils.formatDrmsDate = function (datetime)
{
    // datetime=parse_datetime(datetime);
    var dateObj = new Date(datetime);
    var hr = dateObj.getHours();
    var min = dateObj.getMinutes();
    if (hr < 10)
    {
        hr = "0" + hr;
    }
    if (min < 10)
    {
        min = "0" + min;
    }
    var dateStr = hr + ":" + min;
    return dateStr;
}

utils.formatDate = function (date)
{
    if (date != "")
    {
        var tempDate = date.split(" ");
        var tempTime = tempDate[3].split(":");
        if (tempDate[1] == 'Jan')
        {
            tempDate[1] = '01';
        }
        else if (tempDate[1] == 'Feb')
        {
            tempDate[1] = '02';
        }
        else if (tempDate[1] == 'Mar')
        {
            tempDate[1] = '03';
        }
        else if (tempDate[1] == 'Apr')
        {
            tempDate[1] = '04';
        }
        else if (tempDate[1] == 'May')
        {
            tempDate[1] = '05';
        }
        else if (tempDate[1] == 'Jun')
        {
            tempDate[1] = '06';
        }
        else if (tempDate[1] == 'Jul')
        {
            tempDate[1] = '07';
        }
        else if (tempDate[1] == 'Aug')
        {
            tempDate[1] = '08';
        }
        else if (tempDate[1] == 'Sep')
        {
            tempDate[1] = '09';
        }
        else if (tempDate[1] == 'Oct')
        {
            tempDate[1] = '10';
        }
        else if (tempDate[1] == 'Nov')
        {
            tempDate[1] = '11';
        }
        else if (tempDate[1] == 'Dec')
        {
            tempDate[1] = '12';
        }
        var formatedDate = tempDate[2] + tempDate[1] + tempDate[5] + tempTime[0] + tempTime[1] + tempTime[2];
        return formatedDate;
    }
    return "";
}
utils.convertDrmsDate = function (timeType, date, range)
{
    var tempDate = null;
    tempDate = date;
    if (range == "start")
    {
        if (timeType == "daily")
        {
            return tempDate;
        }
        else if (timeType == "weekly")
        {
            return (new Date(tempDate - (tempDate.getDay() * 86400000)));
        }
        else if (timeType == "monthly")
        {
            tempDate.setMonth(tempDate.getMonth(), 1);
            return tempDate;
        }
        else if (timeType == "yearly")
        {
            tempDate.setMonth(0, 1);
            return tempDate;
        }
    }
    else if (range == "end")
    {
        if (timeType == "daily")
        {
            tempDate.setHours(23, 59, 59);
            return tempDate;
        }
        else if (timeType == "weekly")
        {
            tempDate.setDate(tempDate.getDate() + (6 - tempDate.getDay()));
            tempDate.setHours(23, 59, 59);
            return tempDate;
        }
        else if (timeType == "monthly")
        {
            tempDate.setMonth(tempDate.getMonth() + 1, 0);
            return tempDate;
        }
        else if (timeType == "yearly")
        {
            tempDate.setMonth(11, 31);
            tempDate.setHours(23, 59, 59);
            return tempDate;
        }
    }
}

utils.populateChannels = function (channelCount, divId)
{
    var channelListId = "#" + divId;
    var channList = [];
    var dataSource = null;
    for (var i = 0; i < channelCount; i++)
    {
        var value = i + 1;
        channList.push({
            "name": value,
            "nodeCoreDeviceId": i
        });
    }
    if (channList.length > 0)
    {
        dataSource = new kendo.data.DataSource({
            data: channList
        });
        $(channelListId).data("kendoDropDownList").setDataSource(dataSource);
    }
    else
    {
        dataSource = new kendo.data.DataSource({
            data: []
        });
        $(channelListId).data("kendoDropDownList").setDataSource(dataSource);
        $(channelListId).data("kendoDropDownList").element.val("");
        $(channelListId).data("kendoDropDownList").text("");
    }
}

utils.populateNodeNames = function (nodeList, divId)
{
    var channelListId = "#" + divId;
    var dataSource = null;
    if (nodeList.length > 0)
    {
        dataSource = new kendo.data.DataSource({
            data: nodeList
        });
        $(channelListId).data("kendoDropDownList").setDataSource(dataSource);
    }
    else
    {
        dataSource = new kendo.data.DataSource({
            data: []
        });
        $(channelListId).data("kendoDropDownList").setDataSource(dataSource);
        $(channelListId).data("kendoDropDownList").element.val("");
        $(channelListId).data("kendoDropDownList").text("");
    }
}

utils.collapseAllRows = function (gridId)
{
    var grid = $('#' + gridId).data('kendoGrid');
    grid.tbody.find('>tr.k-grouping-row').each(function (e)
    {
        grid.collapseRow(this);
    });
}

utils.expandAllRows = function (gridId)
{
    var grid = $('#' + gridId).data('kendoGrid');
    grid.tbody.find('>tr.k-grouping-row').each(function (e)
    {
        grid.expandRow(this);
    });
}

utils.isNullOrEmpty = function (str)
{
    return str == null || str == "";
}

/**
 *
 * @param presetCompanyId
 *            optional, if specified, companyId input will be set and hidden
 *            from user
 * @param allowOTP
 *            (true/false) allow OTP login option
 * @param callback
 *            Upon login success, sessionKey will be passed in
 *            callback(sessionKey), empty string on cancellation
 */
utils.popupAuthentication = function (presetCompanyId, allowOTP, callback)
{

    var presetInfo = {
        "companyId": presetCompanyId,
        "allowOTP": allowOTP
    };
    var url = "/login/authenticate/" + JSON.stringify(presetInfo);

    utils.openPopup(localizeResource("authentication"), url, 335, null, true, function ()
    {
        callback(authWin.sessionKeyResult);
    });
}

utils.centerDivObject = function (jqDivObject)
{
    var winWidth = $(window).width();
    var winHeight = $(window).height();

    var divWidth = jqDivObject.width();
    var divHeight = jqDivObject.height();

    var top = (winHeight - divHeight) / 2;
    var left = (winWidth - divWidth) / 2;

    jqDivObject.css("position", "absolute");
    jqDivObject.css("top", top);
    jqDivObject.css("left", left);
}

utils.sameday = function (date1, date2)
{
    return date1.getUTCFullYear() == date2.getUTCFullYear() && date1.getUTCMonth() == date2.getUTCMonth() && date1.getUTCDate() == date2.getUTCDate();
}

utils.tryParseJson = function (str)
{
    try
    {
        var ret = JSON.parse(str);
        if (ret == "")
        {
            ret = {};
        }
        return ret;
    } catch (e)
    {
        return {};
    }
}

utils.getMapSize = function (map)
{
    return Object.keys(map).length;
};

utils.modulo = function (number, divisor)
{
    return number % divisor;
}

utils.checkFlashPlayer = function (browser)
{
    if (browser == 'ie')
    {
        try
        {
            new ActiveXObject("ShockwaveFlash.ShockwaveFlash");
        } catch (e)
        {
            return false;
        }
    }

    if (browser == 'firefox' || browser == 'chrome' || browser == 'opera')
    {
        var isExistFlashPlayer = navigator.plugins["Shockwave Flash"];
        if (!isExistFlashPlayer)
        {
            return false;
        }
    }

    return true;
}

utils.viewSnapshot = function (coreDeviceId, channelId)
{
    var contentPage = "/device/viewsnapshot/" + coreDeviceId + "-" + channelId;
    utils.openPopup(localizeResource("view-snapshot"), contentPage, null, null, true, function ()
    {
        utils.hideLoadingOverlay();
    });
}

utils.checkDeviceCompleteInfo = function (device)
{

    if (device.model.capabilities.indexOf("node") != -1 && device.node === undefined)
    { //Whether is a node checking in the kup cloud
        console.log(device.name + " doesnot have node properties");
        return false;
    }
    else if (device.model.capabilities.indexOf("node") === -1 && device.model.channels < 1)
    { //Whether is a camera checking in the node
        console.log(device.name + " doesnot have channel on it");
        return false;
    }
    return true;
}

utils.formatSerialNumber = function (serialNumber)
{
    var dashedNumber = "";
    var groupSize = 5;

    for (var i = 0; i < serialNumber.length; i++)
    {
        dashedNumber += serialNumber[i];
        if (serialNumber.length != (i + 1) && utils.modulo(i + 1, groupSize) == 0)
        {
            dashedNumber += " - ";
        }
    }

    return dashedNumber;
}

utils.removeArrayEntry = function (value, Array)
{
    var index = $.inArray(value, Array);
    if (~index)
    {
        Array.splice(index, 1);
    }
}

utils.isValidEmail = function isValidEmailAddress(emailAddress)
{
    var pattern = new RegExp(/^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i);
    return pattern.test(emailAddress);
};

utils.convertToCurrency = function (value)
{
    if (!isNaN(value))
    {
        var number = parseFloat(value);
        if (number >= 1000000000)
        {
            return (number / 1000000000) + "BN"
        }
        else if (number >= 1000000)
        {
            return (number / 100000) + "M"
        }
        else if (number >= 1000)
        {
            return (number / 1000) + "K"
        }
        else
        {
            return value;
        }
    }
    else
    {
        return value;
    }
}

utils.getLatLngByAddress = function (address, callback, onError)
{
    reverseGeocode(address, function (responseData)
    {
        if (responseData.result != "ok")
        {
            onError(responseData);
            return;
        }
        var location = {
            lng: responseData.lng,
            lat: responseData.lat
        }
        callback(location);
    }, onError);
}

utils.getDateDifference = function (date1, date2)
{
    // Convert both dates to milliseconds
    var date1Millis = date1.getTime();
    var date2Millis = date2.getTime();
    var differenceObject = {};
    // Calculate the difference in milliseconds
    var differenceMillis = (date2Millis > date1Millis) ? (date2Millis - date1Millis) : (date1Millis - date2Millis);
    //get units
    differenceObject.days = Math.floor((differenceMillis / (60 * 60 * 1000)) / 24);
    differenceObject.hours = Math.floor((differenceMillis / (60 * 60 * 1000)) % 24);
    differenceObject.minutes = Math.floor((differenceMillis / (60 * 1000)) % 60);
    differenceObject.seconds = Math.floor((differenceMillis / 1000) % 24);
    //object that contains all the date time units. 0 if doesnot exist
    return differenceObject;
}

utils.combineDeviceChannelIDs = function (deviceId, channelId)
{
    if (!utils.isNullOrEmpty(deviceId) && !utils.isNullOrEmpty(channelId))
    {
        return deviceId + "-" + channelId;
    }
}

utils.isValidDir = function (directoryString)
{
    var pattern = new RegExp(/^[0-9a-zA-Z\/\-_\s]+$/i);
    return pattern.test(directoryString);
}

/**
 * returns true if array2 contains all the value of array1
 * returns  false if array2 doesnot contains all the value of array1
 *
 */
utils.containsAll = function (array1, array2)
{
    for (var i = 0 , len = array1.length; i < len; i++)
    {
        if ($.inArray(array1[i], array2) == -1)
        {
            return false;
        }
    }
    return true;
}

/**
 * returns true if array2 contains any one of the value of array1
 * returns  false if array2 doesnot contains all of the value of array1
 *
 */
utils.containsAny = function (array1, array2)
{
    for (var i = 0 , len = array1.length; i < len; i++)
    {
        if ($.inArray(array1[i], array2) !== -1)
        {
            return true;
        }
    }
    return false;
}

utils.permutate = function (list, length)
{
    // Copy initial values as arrays
    var perm = list.map(function (val)
    {
        return [val];
    });
    // permutation generator
    var generate = function (perm, maxLen, currLen)
    {
        // Reached desired length
        if (currLen === maxLen)
        {
            return perm;
        }
        // For each existing permutation
        for (var i = 0, len = perm.length; i < len; i++)
        {
            var currPerm = perm.shift();
            // Create new permutation
            for (var k = 0; k < list.length; k++)
            {
                perm.push(currPerm.concat(list[k]));
            }
        }
        // Recurse
        return generate(perm, maxLen, currLen + 1);
    };

    // Start with size 1 because of initial values
    return generate(perm, length, 1);
}

utils.requestUserInput = function (inputName, callback)
{
    var saved = false;
    var inputText = "";
    var contentHtml = $(".input_request_win").html();

    var kendoWindow = $(".input_request_win").kendoWindow({
        visible: false,
        title: inputName,
        resizable: false,
        modal: true,
        width: 200,
        close: function ()
        {
            callback(saved, inputText);
        }
    });
    kendoWindow.data("kendoWindow").content(contentHtml).center().open();

    var $inputBox = $(".input_request_win input[name=userInput]");

    $(".input_request_win .btn_save").click(function ()
    {
        inputText = $inputBox.val();
        if (utils.isNullOrEmpty(inputText))
        {
            return;
        }

        saved = true;
        $(".input_request_win").data("kendoWindow").close();
    });

    $(".input_request_win .btn_cancel").click(function ()
    {
        saved = false;
        $(".input_request_win").data("kendoWindow").close();
    });
}

utils.isListEmpty = function (list)
{
    return list == null || list.length == 0;
};

/**
 * checks if the user is using another computer to access node UI
 *
 */
utils.browsingOnNode = function ()
{
    var hostname = window.location.hostname;
    return (hostname == "localhost" || hostname == "127.0.0.1");
}

utils.toAPITimestamp = function (utcDate)
{
    return kendo.toString(utcDate, kupapi.API_TIME_FORMAT);
};

utils.getLocalTimestamp = function (millis)
{
    var localDt = new Date(millis);
    return kendo.toString(localDt, kupapi.TIME_FORMAT);
};

utils.getUTCTimestamp = function (millis)
{
    var localDt = new Date(millis);
    var utcDt = utils.convertToUTC(localDt);
    return kendo.toString(utcDt, kupapi.TIME_FORMAT);
};

utils.replaceAll = function (str, find, replace)
{
    return str.replace(new RegExp(find, 'g'), replace);
}

utils.randomAlphanumeric = function (length)
{
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    for (var i = 0; i < length; i++)
    {
        text += possible.charAt(Math.floor(Math.random() * possible.length));
    }
    return text;
};

utils.copyToClipBoard = function (text)
{
    var $win = $(".text_copier");
    var $input = $win.find("input");
    var $btn = $win.find(".btn_copy");

    $input.val(text);

    $win.kendoWindow({
        visible: false,
        title: localizeResource("copy"),
        resizable: false,
        modal: true,
        close: function ()
        {
        }
    }).data("kendoWindow").center().open();

    $btn.on('click', function (event)
    {
        try
        {
            var copyTextArea = $input[0];
            copyTextArea.select();
            var successful = document.execCommand('copy');
        }
        catch (e)
        {
            console.error(e);
        }
        $win.data("kendoWindow").close();
    });
};

/**
 * null input means run 24/7.
 * Requires visPrd.js and vis_prd.css
 */
utils.openScheduleViewer = function (periodsOfDays)
{
    visPrd.init(periodsOfDays);
    var contentPage = "/vca/visualizeschedule";
    utils.openPopup(localizeResource('schedule-viewer'), contentPage, null, null, true, function ()
    {
    });
}

utils.padFixedLength = function (num, length, char)
{
    char = char || '0';
    num = num + '';
    return num.length >= length ? num : new Array(length - num.length + 1).join(char) + num;
}

utils.formatTimeZone = function (offsetMins)
{
    var sign = offsetMins < 0 ? "-" : "+";
    var offsetH = utils.padFixedLength(offsetMins / 60, 2);
    var offsetM = utils.padFixedLength(offsetMins % 60, 2);
    return  sign + offsetH + ":" + offsetM;
};

utils.CIDRToNetmask = function (cidr)
{
    var mask = 0xffffffff << (32 - cidr);
    var arr = [
            mask >>> 24,
            mask >> 16 & 0xff,
            mask >> 8 & 0xff,
            mask & 0xff
    ];
    return arr.join(".");
}

utils.displayBase64Image = function (maskBase64)
{
    var dataUrl = "data:image/jpeg;base64," + maskBase64;
    var win = window.open();
    $(win.document.body).html('<img src="' + dataUrl + '" />');
};

utils.getCircularPolygon = function (center, r, N)
{
    var points = [];
    for (var n = 1; n <= N; n++)
    {
        points.push({
            x: r * Math.cos(2 * Math.PI * n / N) + center.x,
            y: r * Math.sin(2 * Math.PI * n / N) + center.y
        });
    }

    return points;
}

utils.bytesToMBString = function (bytes)
{
    return Math.round(bytes / (1024 * 1024)) + " MB";
};

utils.sanitizeForURL = function (str, replaceChar)
{
    return str.replace(/[^a-zA-Z0-9-_.]/g, replaceChar);
};

utils.flashSupported = function (popupInstallLink)
{
    if (FlashDetect.installed)
    {
        return true;
    }

    if (popupInstallLink)
    {
        $(".install_flash_warning").kendoWindow({
            visible: false,
            title: localizeResource("system-message"),
            resizable: false,
            modal: true,
            width: 350,
            close: function ()
            {
            }
        }).data("kendoWindow").center().open();
    }

    return false;
};

utils.disableWithOverlay = function ($targetElement, disable)
{
    var leftBarActive = mainJS.isLeftBarActive();
    var position = $targetElement.position();
    var $header = $("#header");

    var $overlay = $(".ele_busy_overlay");

    $overlay.css({
        position: 'absolute',
        "z-index": 1000,
        top: position.top + $header.outerHeight(),
        left: position.left + (leftBarActive ? 250 : 0),
        width: $targetElement.outerWidth(),
        height: $targetElement.outerHeight(),
        background: "#333",
        opacity: 0.5
    });

    $overlay.toggle(disable);
};

utils.equalIgnoreCase = function (s1, s2)
{
    if (s1 != null && s2 != null)
    {
        return s1.toLowerCase() === s2.toLowerCase();
    }

    return false;
};

utils.doNothing = function ()
{
};