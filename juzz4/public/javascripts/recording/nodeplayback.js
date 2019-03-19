/**
 * Note: All dates here are using moment.js types
 *
 * @author Aye Maung
 *
 */
var nodePlayback = {
    periodBarId: "recStatusBar",
    jw: {
        playerId: "jwClPlyr"
    },
    selected: {
        camera: null,
        date: moment(),
        period: null,
        files: []
    },
    kendo: {
        calendar: null
    },
    API_TIME_FORMAT: "DDMMYYYYHHmmss",
    streaming: {
        protocol: "rtmp/h264",
        ttlSeconds: 300,
        freqSeconds: 20,
        active: {
            timer: null,
            sessions: {}
        }
    },
    autoRefresh: {
        timer: null,
        freq: 30000, //30s. Note: a new file comes in every 15 mins
        currentFileCount: 0
    }
};

nodePlayback.init = function ()
{
    nodePlayback._adjustUI();
    nodePlayback._initCameraPicker();
    nodePlayback._initCalendar();
    nodePlayback._startRefreshTimers();
    nodePlayback._listRunningTasks();

    //wait for left menu
    setTimeout(function ()
    {
        recSB.init(nodePlayback.periodBarId, nodePlayback._periodChangedHandler);
    }, 300);
};

nodePlayback.searchRecordings = function (paramsChanged)
{
    var selection = nodePlayback.selected;
    if (selection.camera == null || selection.date == null)
    {
        return;
    }

    // //mock
    // recSB.generate(
    //     selection.date.toDate(),
    //     nodePlayback._getMockFiles(nodePlayback.selected.date),
    //     paramsChanged
    // );
    // return;

    var targetDt = moment(selection.date);
    var from = moment(targetDt.startOf("day"));
    var to = moment(targetDt.endOf("day"));

    //send api
    nodePlayback._loading(paramsChanged);
    getRecordedFileList(
        selection.camera.id,
        "0",    //both platform and core only support single-channel cameras
        nodePlayback._getAPITimestamp(from),
        nodePlayback._getAPITimestamp(to),
        function (responseData)
        {
            nodePlayback._loading(false);
            if (responseData.result != "ok")
            {
                utils.throwServerError(responseData);
                return;
            }

            var recordingFiles = responseData.files;

            //if search returns the same files. no need to update
            if (!paramsChanged && (recordingFiles.length == nodePlayback.autoRefresh.currentFileCount))
            {
                return;
            }
            else
            {
                nodePlayback.autoRefresh.currentFileCount = recordingFiles.length;
            }

            recSB.generate(selection.date.toDate(), recordingFiles, paramsChanged);
        }, null
    );
};

nodePlayback.clearPeriodSelection = function ()
{
    nodePlayback._cancelStreaming();
    recSB.fn.clearBrush();
};

nodePlayback.stream = function ()
{
    var currentSelections = nodePlayback.selected;
    if (currentSelections.period == null)
    {
        return;
    }

    //prevent multiple clicks
    if (nodePlayback._isPlaylistLoaded())
    {
        return;
    }

    // //mock
    // nodePlayback._playStream("rtmp://fms.12E5.edgecastcdn.net/0012E5/mp4:videos/8Juv1MVa-485.mp4");
    // return;

    nodePlayback._cancelStreaming();

    //split requests based on missing recordings
    var splitPeriods = nodePlayback._getSplitPeriodsByGaps();
    if (splitPeriods.length == 0)
    {
        return;
    }

    //request streams for each split period
    nodePlayback._loading(true);
    var completedRequestCount = 0;
    $.each(splitPeriods, function (i, period)
    {
        var requestTime = moment();
        getPlaybackVideoUrl(
            currentSelections.camera.id,
            "0",
            nodePlayback.streaming.protocol,
            nodePlayback._getAPITimestamp(period[0]),
            nodePlayback._getAPITimestamp(period[1]),
            nodePlayback.streaming.ttlSeconds,
            function (responseData)
            {
                if (responseData.result != "ok" || responseData.urls.length == 0)
                {
                    utils.throwServerError(responseData);
                }
                else
                {
                    //save response
                    nodePlayback.streaming.active.sessions[period[0].toDate().getTime()] = {
                        period: period,
                        url: responseData.urls[0],
                        sessionKey: responseData["streaming-session-key"],
                        expiryTime: requestTime.add(responseData["ttl-seconds"], "seconds")
                    };
                }

                //detect last return
                completedRequestCount++;
                if (completedRequestCount == splitPeriods.length)
                {
                    nodePlayback._loadStreamList();
                    nodePlayback._loading(false);
                }
            }
        );
    });
};

nodePlayback.zippedDownload = function ()
{
    var estSize = nodePlayback.estimateDownloadSize();
    utils.popupConfirm(
        localizeResource('confirmation'),
        localizeResource('zipped-download-confirmation', utils.bytesToMBString(estSize)),
        function (choice)
        {
            if (!choice)
            {
                return;
            }

            downloadZippedRecordings(
                nodePlayback.selected.camera.id,
                "0",
                nodePlayback._getAPITimestamp(nodePlayback.selected.period[0]),
                nodePlayback._getAPITimestamp(nodePlayback.selected.period[1])
            );
        });
};

nodePlayback.exportToUSB = function ()
{
    var contentPage = "/playback/nodeusbexport";
    utils.openPopup(localizeResource('title-export-to-usb'), contentPage, null, null, true, function ()
    {
    });
};

nodePlayback.estimateDownloadSize = function ()
{
    var totalSize = 0;
    $.each(nodePlayback.selected.files, function (i, f)
    {
        totalSize += f.fileSize;
    });
    return totalSize;
};

nodePlayback._initCameraPicker = function ()
{
    var noSelectionName = localizeResource("select-camera");
    var ddList = $(".rec_mgr .camera_list").kendoDropDownList({
        optionLabel: noSelectionName,
        dataTextField: "name",
        dataValueField: "deviceId",
        dataSource: {
            transport: {
                read: function (options)
                {
                    getUserDevices("", function (responseData)
                    {
                        if (responseData.result != "ok" || responseData.devices == null)
                        {
                            utils.throwServerError(responseData);
                            options.success([]);
                            return;
                        }

                        options.success(responseData.devices);
                    }, null);
                }
            }
        },
        change: function ()
        {
            var dataItem = ddList.dataItem();
            if (dataItem.name == noSelectionName)
            {
                recSB.reset();
                nodePlayback.selected.camera = null;
                return;
            }

            nodePlayback.selected.camera = dataItem;
            nodePlayback.searchRecordings(true);
        }
    }).data("kendoDropDownList");
};

nodePlayback._initCalendar = function ()
{
    var now = moment();
    nodePlayback.kendo.calender = $(".rec_mgr").find(".calendar").kendoCalendar({
        format: "d MMM yyyy",
        max: now.toDate(),
        value: nodePlayback.selected.date.toDate(),
        footer: false,
        change: function ()
        {
            nodePlayback._setDate(moment(this.value()));
        }
    }).data("kendoCalendar");
};

nodePlayback._adjustUI = function ()
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

nodePlayback._startRefreshTimers = function ()
{
    //check new files
    nodePlayback.autoRefresh.timer = setInterval(function ()
    {
        if (!recSB.isSelectionActive())
        {
            nodePlayback.searchRecordings(false);
        }
    }, nodePlayback.autoRefresh.freq);

    //keep streaming links alive
    nodePlayback.streaming.active.timer = setInterval(
        function ()
        {
            var now = moment();
            var safetyGap = (nodePlayback.streaming.freqSeconds * 2);

            $.each(nodePlayback.streaming.active.sessions, function (startTime, session)
            {
                var refreshTime = moment(session.expiryTime).subtract(safetyGap, "seconds");
                if (now.isAfter(refreshTime))
                {
                    var newExpiry = moment(refreshTime).add(nodePlayback.streaming.ttlSeconds, "seconds");
                    keepAlivePlaybackVideoUrl("", session.sessionKey);
                    session.expiryTime = newExpiry;

                    console.log(
                        "KeepAlive", nodePlayback._formatPeriod(session.period),
                        "extended till", newExpiry.format("h:mm:ss")
                    );
                }
            });

        }, nodePlayback.streaming.freqSeconds * 1000
    );
}

nodePlayback._setDate = function (selectedDate)
{
    if (!selectedDate.isValid())
    {
        console.log("invalid date");
        return;
    }

    nodePlayback.selected.date = selectedDate;
    nodePlayback.searchRecordings(true);
};

nodePlayback._periodChangedHandler = function (newPeriod, selectedFiles)
{
    nodePlayback._cancelStreaming();
    var momentPeriod = [moment(newPeriod[0]), moment(newPeriod[1])];

    //for debugging
    selectedFiles.forEach(function (f)
    {
        var timeFmt = "hh:mm:ss";
        console.log(
            "Period:", moment(f.startTime).format(timeFmt), "-", moment(f.endTime).format(timeFmt),
            ", Size:", utils.bytesToMBString(f.fileSize),
            ", Rounded:", moment(f.roundedStart).format(timeFmt), "-", moment(f.roundedEnd).format(timeFmt)
        );
    });

    //detect selection cancelled
    var selectionCancelled = momentPeriod[0].isSame(momentPeriod[1]);
    if (selectionCancelled)
    {
        nodePlayback.selected.period = null;
        nodePlayback.selected.files = [];
    }
    else
    {
        nodePlayback.selected.period = momentPeriod;
        nodePlayback.selected.files = selectedFiles;
    }

    nodePlayback._updateToolBoxBtns(selectedFiles);
};

nodePlayback._updateToolBoxBtns = function (selectedFiles)
{
    var emptyList = (selectedFiles && selectedFiles.length == 0);
    var browsingOnNode = utils.browsingOnNode();

    var $streamBtn = $("#movingToolBox .options .stream");
    var $zipBtn = $("#movingToolBox .options .zip");
    var $usbBtn = $("#movingToolBox .options .usb");

    if (!emptyList)
    {
        $streamBtn.show();
    }
    else
    {
        $streamBtn.hide();
    }

    if (!emptyList && !browsingOnNode)
    {
        $zipBtn.show();
    }
    else
    {
        $zipBtn.hide();
    }

    if (!emptyList)
    {
        $usbBtn.show();
    }
    else
    {
        $usbBtn.hide();
    }
};

nodePlayback._getSplitPeriodsByGaps = function ()
{
    //the code below basically cuts out gaps. So, a single period is split into multiple sub-periods

    var splitPeriods = [];
    var currentList = null;

    function addToSplitList()
    {
        if (currentList != null)
        {
            var firstFile = currentList[0];
            var lastFile = currentList[currentList.length - 1];
            splitPeriods.push([moment(firstFile.startTime), moment(lastFile.endTime)]);
        }
    }

    $.each(nodePlayback.selected.files, function (i, f)
    {
        if (f.status == recSB.RecStatus.MISSING)
        {
            addToSplitList();
            currentList = null;
        }
        else
        {
            currentList = currentList || [];
            currentList.push(f);

            //last file
            if ((i + 1) == nodePlayback.selected.files.length)
            {
                addToSplitList();
            }
        }
    });

    return splitPeriods;
};

nodePlayback._loadStreamList = function ()
{
    var timestamps = Object.keys(nodePlayback.streaming.active.sessions);
    timestamps.sort();

    //playlist
    var playlist = [];
    $.each(timestamps, function (i, timestamp)
    {
        var session = nodePlayback.streaming.active.sessions[timestamp];
        playlist.push({
            title: nodePlayback._formatPeriod(session.period),
            //image: kupapi.CdnPath + "/common/images/play_jw_opaque.png",
            file: session.url
        });
    });

    nodePlayback._setPlayerVisibility(true);

    var jwInstance = jwplayer(nodePlayback.jw.playerId).setup({
        playlist: playlist,
        width: "100%",
        height: "100%",
        autostart: true,
        mute: true,
        base: '/public/javascripts/jwplayer7/',
        listbar: {
            position: 'right',
            size: 280
        }
    });
};

nodePlayback._isPlaylistLoaded = function ()
{
    return (Object.keys(nodePlayback.streaming.active.sessions).length > 0);
};

nodePlayback._cancelStreaming = function ()
{
    nodePlayback._setPlayerVisibility(false);

    //expire links
    $.each(nodePlayback.streaming.active.sessions, function (startTime, session)
    {
        expirePlaybackVideoUrl(session.sessionKey);
    });
    nodePlayback.streaming.active.sessions = {};
};

nodePlayback._setPlayerVisibility = function (visible)
{
    var $displayArea = $(".rec_mgr .player_area");
    if (visible)
    {
        $displayArea.slideDown();
    }
    else
    {
        $displayArea.slideUp();
    }
};

nodePlayback._listRunningTasks = function ()
{
    var taskMonitor = TaskMonitor();
    taskMonitor.startMonitorRunning(["usbexportrecordings"]);
}

nodePlayback._formatPeriod = function (period)
{
    var format = "h:mm:ss";
    var start = period[0].format(format);
    var end = period[1].format(format);
    return start + " - " + end;
};

nodePlayback._getMockFiles = function (dt)
{
    var mockList = [];
    var zeroZero = dt.startOf("day");
    var start = zeroZero.valueOf();

    for (var i = 0; i < 96; i++)
    {
        var oneSlot = 15 * 60 * 1000;
        var current = start + (i * oneSlot);
        var status = recSB.RecStatus.COMPLETED;
        if (utils.getRandomInteger(0, 3) == 1)
        {
            status = recSB.RecStatus.MISSING;
        }

        mockList.push({
            startTime: current,
            endTime: current + oneSlot - 1,
            status: status,
            fileSize: 10000000
        });
    }

    return mockList;
};

nodePlayback._getAPITimestamp = function (browserDt)
{
    var utcDt = moment.utc(browserDt);
    return utcDt.format(nodePlayback.API_TIME_FORMAT);
};

nodePlayback._loading = function (loading)
{
    kendo.ui.progress($(".rec_mgr"), loading);
};