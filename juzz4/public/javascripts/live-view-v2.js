/**
 * @author Keith
 * @description Support new device tree apis and refactor old live-view.js code
 */

var LiveViewManager = function (applicationType, maxCameraView)
{
    var deviceTreeView = new DeviceTreeView("#deviceTree");
    var streamType = "rtmp/h264";
    var uiElements = {
        numericBox: null
    };
    var userPrefs = {
        slots: {
            group: {},
            camera: {},
            channel: {},
        },
        autoRotate: false,
        autoRotateTime: 0
    };
    var keepAliveSessions = [];
    var rotation = {
        index: 0,
        devicePairs: [],
        interval: null
    };

    var _init = function()
    {
        // init Device Tree
        var onDrag = function (dataItem, e)
        {
            // disallowed label to drag
            if (dataItem.type === "label")
            {
                e.preventDefault();
            }
        };

        var onDrop = function (dataItem, e)
        {
            // channel
            if (dataItem.type === "channel")
            {
                var slotId = $(e.dropTarget).parentsUntil("ul", "li")[0].id;
                var group = dataItem.deviceName;
                var coreDeviceId = dataItem.coreDeviceId;
                var cameraId = dataItem.channelId;

                _selectViewSlot(slotId);
                _removeSlot(slotId);

                // load to rotation in case when user enable rotation
                rotation.devicePairs.push({
                    group: group,
                    coreDeviceId: coreDeviceId,
                    channelId: cameraId
                });

                // update userpref for update to server
                userPrefs.slots.group[slotId] = group;
                userPrefs.slots.camera[slotId] = coreDeviceId;
                userPrefs.slots.channel[slotId] = cameraId;
                _changeRotation(false);

                var deviceDetail = [];
                var deviceDetails = deviceManager.attachDeviceDetails(deviceDetail, null, coreDeviceId, cameraId);
                deviceDetails.displayName = deviceDetails.deviceName + " / " + deviceDetails.channelName;
                _loadVideo(slotId, coreDeviceId, cameraId, deviceDetails);
            }
            else // device
            {
                rotation.devicePairs = [];
                var group = dataItem.labelName;
                $.each(dataItem.items, function(index, item)
                {
                    item.group = group;
                    rotation.devicePairs.push(item);
                });
                _changeRotation(true);
            }
        };

        deviceTreeView.initWithDragNDrop(".videoBox1, .videoBox4, .videoBox9, .videoBox16",
            onDrag,
            onDrop,
            function ()
            {
                _initUserPrefViews(function ()
                {
                    _initDeviceTreeSearch();
                    _initScreenDisplayTools();
                    _initAutoRotateTools();
                    _initDefaultView();
                    setInterval(function() {
                        _keepAliveChecker();
                    }, 200 * 1000); //200 seconds per check
                });
            }
        );
    };

    var _fullScreen = function ()
    {
        var target = $('#liveViewBox')[0];
        if (screenfull.enabled) {
            screenfull.request(target);
        }
    }

    var _selectViewSlot = function (slotId)
    {
        $.each($(".view_list_box .video_list li"), function(index, data)
        {
            var id = $(data).attr('id');
            var brColor = $("#" + id).css('border-top-color');
            if (brColor != "rgb(232, 0, 0)") {
                $("#" + id).css("border", "4px #565656 solid");
            }
        });
        $("#" + slotId).css("border", "4px #fea204 solid");
    }

    var _initAutoRotateTools = function ()
    {
        // rotate seconds
        uiElements.numericBox = $("#timeInterval").kendoNumericTextBox({
            format: "# sec",
            value: userPrefs.autoRotateTime,
            min: 15,
            change: function()
            {
                var value = this.value();
                if (value == null || isNaN(value))
                {
                    this.value(15);
                };
            }
        }).data("kendoNumericTextBox");

        // rotate timer
        $("#autoRotationdiv").show();
        if (userPrefs.autoRotate) {
            $("#autoRotation").attr("checked", "checked");
            $("#applyAutoRotate").removeClass("k-state-disabled");
            $("#applyAutoRotate").removeAttr('disabled');
            uiElements.numericBox.enable(true);
        } else {
            $("#autoRotation").removeAttr('checked');
            $("#applyAutoRotate").addClass("k-state-disabled");
            $("#applyAutoRotate").attr("disabled", "disabled");
            uiElements.numericBox.enable(false);
        }

        $("#autoRotation").click(function () {
            userPrefs.autoRotateTime = uiElements.numericBox.value();
            userPrefs.autoRotate = $("#autoRotation").attr("checked");
            if (userPrefs.autoRotate)
            {
                _changeRotation(true);
            }
            else
            {
                _changeRotation(false);
            }
        });

        // rotate set button
        $("#applyAutoRotate").click(function () {
            userPrefs.autoRotateTime = uiElements.numericBox.value();
            if ($("#applyAutoRotate").attr("disabled") != "disabled") {
                _changeRotation(true);
            }
            else
            {
                _changeRotation(false);
            }
        });
    };

    var _initDeviceTreeSearch = function ()
    {
        var keypressOnHold = null;
        $('#search-term').keyup(function (e) {
            var executeSearch = function (searchObj) {
                var filterText = $(searchObj).val();
                deviceTreeView.filterTreeView(filterText);
            }

            //clear timeout if user typing fast
            if(keypressOnHold != null)
            {
                clearTimeout(keypressOnHold);
                keypressOnHold = null;
            }

            var searchObj = this;
            keypressOnHold = setTimeout(function(){
                console.log("search keyword: " +$(searchObj).val());
                executeSearch(searchObj);
            },300);
        });
    };

    var _initScreenDisplayTools = function ()
    {
        if (utils.detectBrowser()) {
            if (utils.detectBrowser() != 'ie') {
                $(".fullScreenFeature").show();
            } else if (utils.detectBrowser() == 'ie' && $.browser.version >= 11) {
                $(".fullScreenFeature").show();
            }
        }

        if (screenfull.enabled) {
            document.addEventListener(screenfull.raw.fullscreenchange, function() {
                var className = $('#vList li').attr('class');
                var classNames = className.split(" ");
                var targetClassName = null;
                if ($.inArray("videoBox1", classNames) != -1 || $.inArray("videoBox1_fullscreen", classNames) != -1)
                {
                    targetClassName = "videoBox1";
                }
                else if ($.inArray("videoBox4", classNames) != -1 || $.inArray("videoBox4_fullscreen", classNames) != -1)
                {
                    targetClassName = "videoBox4";
                }
                else if ($.inArray("videoBox9", classNames) != -1 || $.inArray("videoBox9_fullscreen", classNames) != -1)
                {
                    targetClassName = "videoBox9";
                }
                else if ($.inArray("videoBox16", classNames) != -1 || $.inArray("videoBox16_fullscreen", classNames) != -1)
                {
                    targetClassName = "videoBox16";
                }

                if (screenfull.isFullscreen)
                {
                    $(".view_block").hide();
                    if(targetClassName != null)
                    {
                        $("#vList li").removeClass(targetClassName);
                        $("#vList li").addClass(targetClassName+"_fullscreen");
                        $("#vList").addClass("vListFullScreen");
                    }
                }
                else
                {
                    $(".view_block").show();
                    if(targetClassName != null)
                    {
                        $("#vList li").removeClass(targetClassName+"_fullscreen");
                        $("#vList li").addClass(targetClassName);
                        $("#vList").removeClass("vListFullScreen");
                    }

                    if(applicationType != "cloud" && maxCameraView < 9){
                        $("#view9link").parent().hide();
                    }else{
                        $("#view9link").parent().show();
                    }
                }
            });
        }

        $("#vList").on("mouseenter", "li", function(e)
        {
            if ($(e.currentTarget).children(".device_description").length > 0)
            {
                kendo.fx($(e.currentTarget).children(".device_description")).expand("vertical").stop().play();
            }
        });
        $("#vList").on("mouseleave", "li", function(e)
        {
            if ($(e.currentTarget).children(".device_description").length > 0)
            {
                kendo.fx($(e.currentTarget).children(".device_description")).expand("vertical").stop().reverse();
            }
        });
    };

    var _initUserPrefViews = function (callback)
    {
        getUserPrefs("", function(responseData)
        {
            if (responseData.result == "ok" && responseData.prefs != null)
            {
                var defaultSlots = utils.tryParseJson(responseData.prefs.slotSettingAssignments);
                userPrefs.autoRotate = responseData.prefs.autoRotation;
                userPrefs.autoRotateTime = parseInt(responseData.prefs.autoRotationTime);

                //Avoid NaN value
                if (isNaN(userPrefs.autoRotateTime) || userPrefs.autoRotateTime < 15)
                {
                    userPrefs.autoRotateTime = 15;
                }

                if (defaultSlots.group == null)
                {
                    defaultSlots.group = {};
                }

                if (defaultSlots.camera == null)
                {
                    defaultSlots.camera = {};
                }

                if (defaultSlots.channel == null)
                {
                    defaultSlots.channel = {};
                }

                //Load default slots to userPref
                $.each(defaultSlots.group, function(slotId, group)
                {
                    var isExists = false;
                    var coreDeviceId = defaultSlots.camera[slotId];
                    var channelId = defaultSlots.channel[slotId];

                    var allDevices = deviceTreeView.getDefaultDeviceTreeData()[0].items;
                    $.each(allDevices, function(i, item){
                        $.each(item.items, function(i, channel)
                        {
                            if(channel.coreDeviceId === coreDeviceId && channel.channelId === channelId)
                            {
                                isExists = true;
                                return true;
                            }
                        });

                        if(isExists)
                        {
                            return true;
                        }
                    });

                    if(isExists)
                    {
                        userPrefs.slots.group[slotId] = defaultSlots.group[slotId];
                        userPrefs.slots.camera[slotId] = defaultSlots.camera[slotId];
                        userPrefs.slots.channel[slotId] = defaultSlots.channel[slotId];
                    }
                });
            }
            callback();
        }, null);
    };

    var _initDefaultView = function ()
    {
        var viewDivs = [];
        $("#vList>li").each(function() {
            viewDivs.push($(this).attr("id"));
        });

        var coreDeviceIdForRotate = 0;
        $.each(userPrefs.slots.group, function(slotId, group) {
            $.each(viewDivs, function(ind, viewDiv) {
                if(slotId === viewDiv)
                {
                    coreDeviceIdForRotate = userPrefs.slots.camera[slotId];
                    if(!userPrefs.autoRotate)
                    {
                        var group = group;
                        var coreDeviceId = userPrefs.slots.camera[slotId];
                        var channelId = userPrefs.slots.channel[slotId];

                        rotation.devicePairs.push({
                            group: group,
                            coreDeviceId: coreDeviceId,
                            channelId: channelId
                        });

                        var deviceDetail = [];
                        var deviceDetails = deviceManager.attachDeviceDetails(deviceDetail, null, coreDeviceId, channelId);
                        deviceDetails.displayName = deviceDetails.deviceName + " / " + deviceDetails.channelName;
                        _loadVideo(slotId, coreDeviceId, channelId, deviceDetails);
                    }
                    return true;
                }
            });
        });

        if(userPrefs.autoRotate) {
            var allDevices = deviceTreeView.getDefaultDeviceTreeData()[0].items;
            $.each(allDevices, function (i, item) {
                $.each(item.items, function (i, channel) {
                    if (channel.coreDeviceId === coreDeviceIdForRotate) {
                        rotation.devicePairs.push({
                            group: item.labelName,
                            coreDeviceId: channel.coreDeviceId,
                            channelId: channel.channelId
                        });
                    }
                });
            });
            _rotateViews();
        }
    }

    var _saveUserPref = function ()
    {
        saveUserprefs("",
            userPrefs.slots, "",
            userPrefs.autoRotate,
            userPrefs.autoRotateTime, "",
            function(resp) {}
        );
    }

    var _loadVideo = function (slotId, deviceId, channelId, deviceDetails)
    {
        getLiveVideoUrl("", deviceId, channelId, streamType, null, function(resp)
        {
            if (resp.result == "ok" && resp.url != null)
            {
                kupPlayer.init(slotId, "player_" + slotId, resp.url, "jw", deviceDetails, false, false);
                var sessionDetail = {};
                sessionDetail.streamingSessionKey = $.trim(resp["streaming-session-key"]);
                sessionDetail.slotId = slotId;
                sessionDetail.deviceId = deviceId;
                sessionDetail.channelId = channelId;
                sessionDetail.deviceDetails = deviceDetails;

                var isExistSlot = false;
                $.each(keepAliveSessions, function(sIndex, session)
                {
                    if (session.slotId == sessionDetail.slotId)
                    {
                        keepAliveSessions[sIndex] = sessionDetail;
                        isExistSlot = true;
                    }
                });
                console.log(sessionDetail.slotId + " is " + isExistSlot);
                if (!isExistSlot)
                {
                    keepAliveSessions.push(sessionDetail);   
                }

            } else {
                kupPlayer.init(slotId, "player_" + slotId, "", "jw", null, false, false);
            }
        }, null);
    };

    var _keepAliveChecker = function ()
    {
        $.each(keepAliveSessions, function(index, item) {
            var sessionDetail = item;
            keepAliveLiveVideoUrl("", sessionDetail.streamingSessionKey, function(resp)
            {
                //request new stream url, if keepalive is failed.
                if ((resp.result == "error") || (resp.status == false))
                {
                    _loadVideo(sessionDetail.slotId,
                        sessionDetail.deviceId,
                        sessionDetail.channelId,
                        sessionDetail.deviceDetails);
                }
            }, null);
        });
    };

    var _removeSlot = function (slotId)
    {
        delete userPrefs.slots.group[slotId];
        delete userPrefs.slots.camera[slotId];
        delete userPrefs.slots.channel[slotId];

        var newDevicePairMap = {};
        $.each(userPrefs.slots.group, function(divId, group){
            if(divId.substring(0, 1) === slotId.substring(0, 1))
            {
                var group = userPrefs.slots.group[divId];
                var coreDeviceId = userPrefs.slots.camera[divId];
                var channelId = userPrefs.slots.channel[divId];

                newDevicePairMap[coreDeviceId+"_"+channelId] = {
                    group: group,
                    coreDeviceId: coreDeviceId,
                    channelId: channelId
                };
            }
        });

        rotation.devicePairs = [];
        $.each(newDevicePairMap, function(key, devicePair){
            rotation.devicePairs.push(devicePair);
        });

        $.each(keepAliveSessions, function(index, item) {
            var sessionDetail = item;
            if(sessionDetail.slotId === slotId)
            {
                keepAliveSessions.splice(index, 1);
                return false;
            }
        });
    };

    var _rotateViews = function ()
    {
        //clear interval
        if(rotation.interval != null)
        {
            clearInterval(rotation.interval);
            rotation.interval = null;
        }

        if(userPrefs.autoRotate == false)
        {
            _saveUserPref();
            return;
        }

        var processLiveview = function ()
        {
            _expireLiveVedioAlive();
            var viewDivs = [];
            $("#vList>li").each(function() {
                viewDivs.push($(this).attr("id"));
            });

            var i = 0;
            while (i < viewDivs.length && rotation.devicePairs.length > 0) {
                if (rotation.index >= rotation.devicePairs.length)
                {
                    rotation.index = 0;
                }

                userPrefs.slots.group[viewDivs[i]] = rotation.devicePairs[rotation.index].group;
                userPrefs.slots.camera[viewDivs[i]] = rotation.devicePairs[rotation.index].coreDeviceId;
                userPrefs.slots.channel[viewDivs[i]] = rotation.devicePairs[rotation.index].channelId;
                var deviceDetail = [];
                var deviceDetails = deviceManager.attachDeviceDetails(deviceDetail,
                    null,
                    rotation.devicePairs[rotation.index].coreDeviceId,
                    rotation.devicePairs[rotation.index].channelId);

                if(typeof rotation.devicePairs[rotation.index].group === "undefined"
                    || rotation.devicePairs[rotation.index].group === deviceDetails.deviceName)
                {
                    deviceDetails.displayName = deviceDetails.deviceName + " / " + deviceDetails.channelName;
                }
                else
                {
                    deviceDetails.displayName = rotation.devicePairs[rotation.index].group + " / " + deviceDetails.deviceName + " / " + deviceDetails.channelName;
                }
                _loadVideo(viewDivs[i], rotation.devicePairs[rotation.index].coreDeviceId, rotation.devicePairs[rotation.index].channelId, deviceDetails);
                i++;
                rotation.index++;
            }
            _saveUserPref();
        }
        processLiveview();
        rotation.interval = setInterval(processLiveview, userPrefs.autoRotateTime * 1000);
    };

    var _expireLiveVedioAlive = function ()
    {
        $.each(keepAliveSessions, function(index, item) {
            expireLiveVideoUrl("", item.streamingSessionKey, function(resposeData) {}, null);
        });
    };

    var _changeRotation = function (isEnabled)
    {
        if(isEnabled)
        {
            $("#autoRotation").attr("checked", "checked");
            $("#applyAutoRotate").removeAttr('disabled');
            $("#applyAutoRotate").removeClass("k-state-disabled");
            uiElements.numericBox.enable(true);
            userPrefs.autoRotate = true;
        }
        else
        {
            $("#autoRotation").removeAttr('checked');
            $("#applyAutoRotate").attr("disabled", "disabled");
            $("#applyAutoRotate").addClass("k-state-disabled");
            uiElements.numericBox.enable(false);
            userPrefs.autoRotate = false;
        }
        _rotateViews();
    };

    var _viewPortOverlay = function (evt) {
        $.each($(".view_list_box .video_list li"), function(index, data) {
            var id = $(data).attr('id');
            var brColor = $("#" + id).css('border-top-color');

            if (brColor != "rgb(254, 162, 4)") {
                $("#" + id).css("border", "4px #565656 solid"); // change
            }
        });

        $.each(userPrefs.slots.group, function(slotId, group) {
            if (document.getElementById(slotId) && slotId == "111") {
                selectViewSlot(slotId);
            }
            /* device not applicable for this page */
            var deviceId = userPrefs.slots.camera[slotId];
            var channelId = userPrefs.slots.channel[slotId];
            if (!document.getElementById(slotId) || deviceId == -1) {
                return true;
            }
            if (evt.deviceId === deviceId && evt.channelId === channelId.toString()) {
                $("#" + slotId).css("border", "4px #E80000 solid"); // change
            }
        });
    }

    return {
        init: _init,
        fullScreen: _fullScreen,
        selectViewSlot: _selectViewSlot,
        removeSlot: _removeSlot,
        saveUserPref: _saveUserPref,
        loadVideo: _loadVideo,
        viewPortOverlay: _viewPortOverlay
    };
}