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
        date: moment(),
        queryablePeriod: null,  //period shrunk to nearest files, null if there are no files in the selection
        files: [],
        requestHistoryMap: {}
    },
    kendo: {
        calendar: null
    },
    autoRefresh: {
        enabled: true,
        freq: 10000
    },
    cache: {
        bucketUsers: {}
    },
    storageWidget: null,
    jwInstance: null,
    API_TIME_FORMAT: "DDMMYYYYHHmmss"
};

var isPageUnloading = false;

cloudPlayback.init = function ()
{
    cloudPlayback._loading(true);
    cloudPlayback._adjustUI();
    cloudPlayback._initCalendar();

    setTimeout(function ()
    {   //wait for left menu
        recSB.init(cloudPlayback.periodBarId, cloudPlayback._periodChangedHandler);
    }, 300);

    //init search parameters
    cloudPlayback._setCamera(null, null);
    cloudPlayback._setDate(moment());
    cloudPlayback._startRefreshTimer();
    cloudPlayback._initCache();
    cloudPlayback._initStorageInfo();

    //hidden page for debugging
    var keyListener = new window.keypress.Listener();
    keyListener.simple_combo("ctrl alt l", function ()
    {
        cloudPlayback.viewActiveRequests();
    });

    // leaving this page
    $(window).on('beforeunload', function ()
    {
        isPageUnloading = true;
    });

    //open camera selector
    cloudPlayback.triggerCameraChange();
    cloudPlayback._loading(false);
};

cloudPlayback.triggerCameraChange = function ()
{
    var selectedCameraId = null;
    if (cloudPlayback.selected.node != null && cloudPlayback.selected.nodeCamera != null)
    {
        selectedCameraId = camsltr._getCameraIdentifier(cloudPlayback.selected.node, cloudPlayback.selected.nodeCamera);
    }

    var nodeFilter = {
        models: DvcMgr.getModelIdList(DvcMgr.NodeType.UBUNTU),
        minRelease: 4.4,
        recordingEnabled: false
    };

    camsltr.open(localizeResource("select-camera"), selectedCameraId, nodeFilter,
        function (node, selectedCamera)
        {
            cloudPlayback._setCamera(node, selectedCamera);
        }
    );
};

cloudPlayback.searchRecordings = function (paramsChanged, onCompleted)
{
    onCompleted = onCompleted || utils.doNothing;

    if (cloudPlayback.selected.node == null ||
        cloudPlayback.selected.nodeCamera == null ||
        cloudPlayback.selected.date == null)
    {
        onCompleted();
        return;
    }

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
        function (responseData)
        {
            cloudPlayback._loading(false);
            if (responseData.result != "ok")
            {
                utils.throwServerError(responseData);
            }
            else
            {
                recSB.generate(
                    cloudPlayback.selected.date.toDate(),
                    responseData.files,
                    paramsChanged
                );
            }

            //reload info
            cloudPlayback._updateStorageInfo();
            cloudPlayback._loadActiveRequests();

            onCompleted();
        }
    );
};

cloudPlayback.requestRecordings = function ()
{
    if (
        cloudPlayback.selected.node == null ||
        cloudPlayback.selected.nodeCamera == null ||
        cloudPlayback.selected.date == null ||
        cloudPlayback.selected.queryablePeriod == null
    )
    {
        return;
    }

    //estimate total size
    var totalSize = 0;
    $.each(cloudPlayback.selected.files, function (i, f)
    {
        if (recSB.RecStatus.isRequestable(f.status))
        {
            totalSize += f.fileSize;
        }
    });

    utils.popupConfirm(localizeResource('confirmation'),
        localizeResource('msg-confirm-recording-request', utils.bytesToMBString(totalSize)),
        function (choice)
        {
            if (!choice)
            {
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
                function (responseData)
                {
                    if (responseData.result != "ok")
                    {
                        utils.throwServerError(responseData);
                    }

                    cloudPlayback.searchRecordings(false, function ()
                    {
                        cloudPlayback._loading(false);
                    });
                }
            );
        });
};

cloudPlayback.deleteRequestedFiles = function ()
{
    if (
        cloudPlayback.selected.node == null ||
        cloudPlayback.selected.nodeCamera == null ||
        cloudPlayback.selected.date == null ||
        cloudPlayback.selected.queryablePeriod == null
    )
    {
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
        function (responseData)
        {
            cloudPlayback._loading(false);

            if (responseData.result != "ok")
            {
                utils.throwServerError(responseData);
                return;
            }

            var confirmationMsg = localizeResource("confirm-cancel-delete-recordings");

            //requester list
            var pendingRequests = responseData.requests;
            if (pendingRequests.length > 0)
            {
                var uniqueIdList = [];
                var userList = [];
                $.each(pendingRequests, function (i, req)
                {
                    var userId = req.requesterUserId;
                    if (uniqueIdList.indexOf(userId) == -1 && (userId !== kupapi.currentUserId))
                    {
                        uniqueIdList.push(userId);
                        userList.push(cloudPlayback.cache.bucketUsers[userId]);
                    }
                });

                if (userList.length > 0)
                {
                    var template = kendo.template($("#requesterTmpl").html());
                    confirmationMsg = template(userList) + confirmationMsg;
                }
            }

            utils.popupConfirm(localizeResource('confirmation'), confirmationMsg,
                function (choice)
                {
                    if (!choice)
                    {
                        return;
                    }

                    //request backend
                    cloudPlayback._loading(true);
                    deleteCloudRecordings(
                        cloudPlayback.selected.node.id,
                        cloudPlayback.selected.nodeCamera.nodeCoreDeviceId,
                        cloudPlayback._getAPITimestamp(from),
                        cloudPlayback._getAPITimestamp(to),
                        function (responseData)
                        {
                            if (responseData.result != "ok")
                            {
                                utils.throwServerError(responseData);
                            }

                            cloudPlayback.searchRecordings(false, function ()
                            {
                                cloudPlayback._loading(false);
                            });
                        }
                    );
                });
        }
    );
};

cloudPlayback.clearPeriodSelection = function ()
{
    cloudPlayback._setPlayerVisibility(false);
    recSB.fn.clearBrush();
};

cloudPlayback.downloadSelection = function ()
{
    var downloadableList = [];

    $.each(cloudPlayback.selected.files, function (i, f)
    {
        if (f.status == recSB.RecStatus.COMPLETED)
        {
            downloadableList.push(f);
        }
    });

    utils.popupConfirm(
        localizeResource("confirmation"),
        localizeResource("confirm-uploaded-file-download", downloadableList.length),
        function (proceed)
        {
            if (!proceed)
            {
                return;
            }

            $.each(downloadableList, function (i, f)
            {
                window.open(cloudPlayback._getDownloadLink(f));
            });
        });
}

cloudPlayback.viewActiveRequests = function ()
{
    if (cloudPlayback.selected.node == null ||
        cloudPlayback.selected.nodeCamera == null)
    {
        return;
    }

    var nodeId = cloudPlayback.selected.node.id;
    var nodeCameraId = cloudPlayback.selected.nodeCamera.nodeCoreDeviceId;
    var cameraName = cloudPlayback.selected.node.name + " - " + cloudPlayback.selected.nodeCamera.name;
    var winTitle = localizeResource("active-upload-requests", cameraName);
    var contentUrl = "/playback/browseuploadrequests?deviceId=" + nodeId + "&channelId=" + nodeCameraId;
    utils.openPopup(winTitle, contentUrl, null, null, true, utils.doNothing);
};

cloudPlayback.getActiveRequests = function (date)
{
    var daySpecifier = cloudPlayback._getDateSpecifier(date);
    var requests = cloudPlayback.selected.requestHistoryMap[daySpecifier];
    return requests ? requests : [];
};

cloudPlayback._setCamera = function (node, nodeCamera)
{
    //update display
    var $cameraSelector = $(".rec_mgr .filter-sidebar .camera");
    var nodeName = localizeResource("select-camera");
    var cameraName = ". . .";
    if (node && nodeCamera)
    {
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

cloudPlayback._setDate = function (selectedDate)
{
    if (!selectedDate.isValid())
    {
        console.log("invalid date");
        return;
    }

    cloudPlayback.selected.date = selectedDate;
    cloudPlayback.searchRecordings(true);
};

cloudPlayback._startRefreshTimer = function ()
{
    (function delayedRefresh()
    {
        setTimeout(function ()
        {
            if (cloudPlayback.autoRefresh.enabled && !recSB.isSelectionActive())
            {
                cloudPlayback.searchRecordings(false, delayedRefresh);
            }
            else
            {
                delayedRefresh();
            }

        }, cloudPlayback.autoRefresh.freq);
    })();
};

cloudPlayback._setAutoRefresh = function (enabled)
{
    cloudPlayback.autoRefresh.enabled = enabled;
};

cloudPlayback._initCache = function ()
{
    //users
    getBucketUsers("", function (responseData)
    {
        if (responseData.result != "ok")
        {
            utils.throwServerError(responseData);
            return;
        }

        $.each(responseData.users, function (i, user)
        {
            cloudPlayback.cache.bucketUsers[user.userId] = user;
        });
    });
};

cloudPlayback._initStorageInfo = function ()
{
    var color = {
        ok: "#3498DB",
        warning: "#f6ae40",
        danger: "#CF000F"
    };

    cloudPlayback.storageWidget = new ProgressBar.Line('.camera_info_box .storage_info .progress-bar', {
        duration: 500,
        step: function (state, circle)
        {
            var percent = circle.value();
            var colorHex;
            if (percent > 0.9)
            {
                colorHex = color.danger;
            }
            else if (percent > 0.7)
            {
                colorHex = color.warning;
            }
            else
            {
                colorHex = color.ok;
            }
            circle.path.setAttribute('stroke', colorHex);
        }
    });
};

cloudPlayback._updateStorageInfo = function ()
{
    var node = cloudPlayback.selected.node;
    var nodeCam = cloudPlayback.selected.nodeCamera;
    var $cameraInfoBox = $(".camera_info_box");

    if (!node || !nodeCam)
    {
        $cameraInfoBox.hide();
        return;
    }

    getNodeCameraStorage(node.id, nodeCam.nodeCoreDeviceId, function (responseData)
    {
        //storage values
        var limitMB = responseData.info.recordingLimitMB;
        var usageMB = responseData.info.recordingUsageMB;
        var remainingMB = limitMB - usageMB;
        var percentUsed = usageMB / limitMB;

        //if over the limit, adjust the values
        if (usageMB > limitMB)
        {
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

cloudPlayback._updatePlayList = function (selectedFiles)
{
    //prepare list for jw player
    var playList = [];
    $.each(selectedFiles, function (idx, file)
    {
        if (recSB.RecStatus.allowedToPlay(file.status) && !utils.isNullOrEmpty(file.url))
        {
            var periodName = cloudPlayback._formatPeriodForJw([moment(file.startTime), moment(file.endTime)]);
            playList.push({
                title: periodName,
                //image: kupapi.CdnPath + "/common/images/play_jw_opaque.png",
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

    if (!isEmpty)
    {
        cloudPlayback._playWithJW(playList);
    }
};

cloudPlayback._playWithJW = function (playlist)
{
    cloudPlayback.jwInstance = jwplayer(cloudPlayback.jw.playerId).setup({
        playlist: playlist,
        width: "100%",
        height: "100%",
        autostart: false,
        mute: true,
        base: '/public/javascripts/jwplayer7/',
        listbar: {
            position: 'right',
            size: 280
        }
    });

    cloudPlayback.jwInstance.onError(function (e)
    {
        if (isPageUnloading)
        {
            console.log("page unloading. skipped error prompt");
            return;
        }
        if (e && e.message === "Error loading media: File could not be played")
        {
            utils.popupAlert(localizeResource("error-cloud-bandwidth-limit"));
        }
    });
};

cloudPlayback._adjustUI = function ()
{
    //hide left menu
    mainJS.toggleLeftBar();

    //this box uses absolute positioning, so we need to manually adjust it
    var $toolBox = $("#movingToolBox");
    var leftBarWidth = $("#sidebar").outerWidth();

    mainJS.whenLeftBarOpened(function ()
    {
        if ($toolBox.is(":visible"))
        {
            $toolBox.animate({
                right: "-=" + leftBarWidth
            }, 200);
        }

        utils.disableWithOverlay($(".rec_mgr"), true);
    });

    mainJS.whenLeftBarClosed(function ()
    {
        if ($toolBox.is(":visible"))
        {
            $toolBox.animate({
                right: "+=" + leftBarWidth
            }, 200);
        }

        utils.disableWithOverlay($(".rec_mgr"), false);
    });
};

cloudPlayback._initCalendar = function ()
{
    //remove elements if already initialized
    if (cloudPlayback.kendo.calender)
    {
        cloudPlayback.kendo.calender.destroy();
        $(".rec_mgr .calendar").html("");
    }

    var now = moment();
    cloudPlayback.kendo.calender = $(".rec_mgr").find(".calendar").kendoCalendar({
        format: "d MMM yyyy",
        max: now.toDate(),
        value: cloudPlayback.selected.date.toDate(),
        footer: false,
        month: {
            content: $("#pickerMonthEntryTmpl").html()
        },
        change: function ()
        {
            cloudPlayback._setDate(moment(this.value()));
        }
    }).data("kendoCalendar");
};

cloudPlayback._loadActiveRequests = function ()
{
    if (cloudPlayback.selected.nodeCamera == null)
    {
        cloudPlayback.selected.requestHistoryMap = {};
        return;
    }

    getRecordingUploadRequests(
        cloudPlayback.selected.node.id,
        cloudPlayback.selected.nodeCamera.nodeCoreDeviceId,
        function (responseData)
        {
            var historyMap = {};
            $.each(responseData.requests, function (i, request)
            {
                var daySpecifier = cloudPlayback._getDateSpecifier(request.period.from);
                var requestsOfDay = historyMap[daySpecifier];
                if (requestsOfDay == null)
                {
                    requestsOfDay = [];
                }
                requestsOfDay.push(request);
                historyMap[daySpecifier] = requestsOfDay;
            });

            cloudPlayback.selected.requestHistoryMap = historyMap;
            cloudPlayback._initCalendar();
        });
};

cloudPlayback._periodChangedHandler = function (newPeriod, selectedFiles)
{
    var momentPeriod = [moment(newPeriod[0]), moment(newPeriod[1])];

    cloudPlayback.selected.files = selectedFiles;
    if (selectedFiles.length > 0)
    {
        //shrink selection to the actual period with files
        var firstFile = selectedFiles[0];
        var lastFile = selectedFiles[selectedFiles.length - 1];
        cloudPlayback.selected.queryablePeriod = [moment(firstFile.startTime), moment(lastFile.endTime)];
    }

    //for debugging
    selectedFiles.forEach(function (f)
    {
        var timeFmt = "hh:mm:ss";
        console.log(
            "Status:", f.status,
            ", Period:", moment(f.startTime).format(timeFmt), "-", moment(f.endTime).format(timeFmt),
            ", Size:", utils.bytesToMBString(f.fileSize),
            ", Progress:", f.progress,
            ", Rounded:", moment(f.roundedStart).format(timeFmt), "-", moment(f.roundedEnd).format(timeFmt),
            ", url:", f.url
        );
    });

    //detect selection cancelled
    var selectionCancelled = momentPeriod[0].isSame(momentPeriod[1]);
    if (selectionCancelled)
    {
        cloudPlayback.selected.queryablePeriod = null;
    }

    cloudPlayback._setAutoRefresh(selectionCancelled);
    cloudPlayback._updateToolBoxBtns(selectedFiles);
    cloudPlayback._updatePlayList(selectedFiles);
    cloudPlayback._setPlayerVisibility(!selectionCancelled);
};

cloudPlayback._updateToolBoxBtns = function (selectedFiles)
{
    //check if request, download, cancel options should be available
    var showRequestBtn = false;
    var showCancelBtn = false;
    var showDownloadBtn = false;
    var $requestBtn = $("#movingToolBox .options .request");
    var $cancelBtn = $("#movingToolBox .options .cancel");
    var $downloadBtn = $("#movingToolBox .options .download");

    $.each(selectedFiles, function (idx, file)
    {
        if (recSB.RecStatus.isRequestable(file.status))
        {
            showRequestBtn = true;
        }
        if (recSB.RecStatus.isDeletable(file.status))
        {
            showCancelBtn = true;
        }
        if (file.status == recSB.RecStatus.COMPLETED)
        {
            showDownloadBtn = true;
        }
    });

    $requestBtn.toggle(showRequestBtn);
    $cancelBtn.toggle(showCancelBtn);
    $downloadBtn.toggle(showDownloadBtn);
};

cloudPlayback._getDownloadLink = function (file)
{
    var filename = cloudPlayback.selected.nodeCamera.name
                   + "_"
                   + moment(file.startTime).format("HHmmss") + "-" + moment(file.endTime).format("HHmmss")
                   + ".mp4";

    var errorLink = window.location.origin + "/errorlanding/corebandwidthlimit";

    return file.url.download + "?action=download" +
           "&customname=" + utils.sanitizeForURL(filename, "") +
           "&errorlink=" + encodeURIComponent(errorLink);
};

cloudPlayback._formatPeriod = function (period)
{
    var format = "h:mm a";
    var start = period[0].format(format);
    var end = period[1].format(format);
    return start + " - " + end;
};

cloudPlayback._formatPeriodForJw = function (period)
{
    var format = "h:mm:ss";
    var start = period[0].format(format);
    var end = period[1].format(format);
    return start + " - " + end;
};

cloudPlayback._getMockFiles = function (dt)
{
    var mockList = [];
    var states = Object.keys(recSB.RecStatus);
    var zeroZero = dt.startOf("day");
    var start = zeroZero.valueOf();

    for (var i = 0; i < 96; i++)
    {
        var oneSlot = 15 * 60 * 1000;
        var current = start + (i * oneSlot);
        if (utils.getRandomInteger(0, 4) == 1)
        {
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

cloudPlayback._setPlayerVisibility = function (visible)
{
    var $displayArea = $(".rec_mgr .player_area");
    $displayArea.toggle(visible);
};

cloudPlayback._getNodeTimeZone = function ()
{
    var tz = {
        name: cloudPlayback.selected.node.node.settings.timezone,
        tzOffsetMins: cloudPlayback.selected.node.node.settings.tzOffsetMins
    };
    return tz;
};

cloudPlayback._getAPITimestamp = function (browserDt)
{
    var utcDt = moment.utc(browserDt);
    return utcDt.format(cloudPlayback.API_TIME_FORMAT);
};

cloudPlayback._loading = function (loading)
{
    kendo.ui.progress($(".rec_mgr"), loading);
};

cloudPlayback._getDateSpecifier = function (date)
{
    var specifierFormat = "DD-MM-YYYY";
    return moment(date).format(specifierFormat);
};