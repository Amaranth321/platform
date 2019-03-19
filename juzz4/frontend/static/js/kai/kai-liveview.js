angular.module('kai.liveview', [
	'FBAngular',//full screen plugin
]);

angular.module('kai.liveview')
    .factory('LiveviewPlayerService', function(
        KupOption, UtilsService, KupApiService, AuthTokenFactory, DeviceTreeService,
        LiveviewService,
        $q, $filter, $timeout
    ) {
        var kupOpt = KupOption;
        var utils = UtilsService;
        var i18n = UtilsService.i18n;
        var localizeResource = UtilsService.i18n;

        var kupPlayer = {};
        kupPlayer.retryOnErrorList = {};

        kupPlayer.init = function(slotId, playerId, urlList, playerType, deviceDetail, showControlbar, showPlaylist) {

            if (playerType == "jw")
                kupPlayer._playWithJW(playerId, urlList, showControlbar, showPlaylist, slotId, deviceDetail);

            else if (playerType == "vlc")
                kupPlayer._playWithVlc(playerId, urlList, showControlbar, slotId);
        }

        /**
         * This will not work if the player is hidden or not initialized
         *
         */
        kupPlayer.getCurrentPosition = function(playerId) {
            var jwPlayer = jwplayer(playerId);
            if (jwPlayer == null) {
                console.error("getCurrentPosition: player not initialized");
                return null;
            }

            return jwPlayer.getPosition();
        }

        /**
         * This will not work if the player is hidden or not initialized
         *
         */
        kupPlayer.setCurrentPosition = function(playerId, timeInSeconds) {
            var jwPlayer = jwplayer(playerId);
            if (jwPlayer == null) {
                console.error("getCurrentPosition: player not initialized");
                return;
            }

            jwPlayer.seek(timeInSeconds);
        }


        kupPlayer._playWithJW = function(playerId, urlList, showControlbar, showPlaylist, slotId, deviceDetail) {

            var customSkin = "static/js/plugin/jwplayer/skin/six.xml";
            var listbar = false;
            var autoStart = (urlList.length > 1) ? false : true;

            if (showControlbar) {
                customSkin = "static/js/plugin/jwplayer/skin/liveview.xml";
            }

            if (showPlaylist) {
                listbar = {
                    position: 'right',
                    size: 260
                }
            }

            var playlist = [];
            $.each(urlList, function(index, url) {
                var fileName = index + 1;
                playlist.push({
                    title: fileName,
                    image: "static/images/assets/common/play.png",
                    file: url
                });
            });

            // if (deviceDetail != null) {
            //     if (!utils.isNullOrEmpty(deviceDetail.deviceName))
            //         $("#" + slotId).html('<div id=' + playerId + '></div><div class="device_description" style="display:none;">' + deviceDetail.channelName + '<p title="' + deviceDetail.deviceName + '">' + deviceDetail.deviceName + '</p></div>');
            //     else {
            //         $("#" + slotId).html('<div id=' + playerId + '></div><div class="device_description" style="display:none;">' + localizeResource('no-device') + '</div>');
            //     }
            // } else {
            //     $("#" + slotId).html('<div id=' + playerId + '></div>');
            // }

            var jwPlayer = jwplayer(playerId).setup({
                playlist: playlist,
                width: "100%",
                height: "100%",
                listbar: listbar,
                flashplayer: "static/js/plugin/jwplayer/jwplayer.flash.swf",
                html5player: "static/js/plugin/jwplayer/jwplayer.html5.min.js",
                autostart: autoStart,
                mute: true,
                rtmp: {
                    bufferlength: 2
                },
                skin: customSkin,
                captions: {
                    back: false,
                    color: 'cc00000',
                    fontsize: 20
                },
                controls: !!showControlbar,
                analytics: {
                    enabled: false,
                    cookies: false
                },
                events: {
                    onDisplayClick: function(event) {
                        var opt = LiveviewService.data;
                        var viewType = this.id.split("_")[1];
                        var playerIndex = parseInt(this.id.split("_")[2], 10);
                        var state = this.getState();
                        //console.info(state);
                        $.each(opt.playerList, function(type, playerData) {
                            if (type !== viewType) {
                                return true;
                            }
                            var player = playerData[playerIndex];
                            if (state === 'PLAYING' || state === 'BUFFERING') {
                                opt.playerList[type][playerIndex].isPlay = true;
                            }
                            if (state === 'PAUSED' || state === 'IDLE') {
                                opt.playerList[type][playerIndex].isPlay = false;
                            }
                        });
                        LiveviewService.playerSelected(viewType, playerIndex);
                    }
                }
            });

            // jwPlayer.addButton(kupapi.CdnPath + "/common/images/refresh.png", localizeResource("refresh-stream"), function() {
            //     jwPlayer.stop(true);
            //     jwPlayer.play(true);
            // }, "refresh_stream");

            // Try to resume after error occurs
            // jwPlayer.onError(function(error) {
            //     console.log("JW Error: slot-" + slotId + ", Type:" + error.type + ", Message:" + error.message);

            //            var activeTimer = kupPlayer.retryOnErrorList[slotId];
            //            if (activeTimer) {
            //                clearTimeout(activeTimer);
            //            }

            //            kupPlayer.retryOnErrorList[slotId] = setTimeout(function () {

            //     setTimeout(function() {
            //         jwPlayer.play(true);
            //     }, 5000);
            // });
            // jwPlayer.onReady(function() {
            //     $("#" + playerId + "_wrapper").append("<div class='closeIcon' onclick='kupPlayer.removeJwPlayer(event)'></div>");
            // })
            // kupPlayer.removeJwPlayer = function(e) {
            //     var playerId = e.currentTarget.parentNode.id.replace("_wrapper", "");;
            //     var liveViewBoxId = playerId.replace("player_", "");
            //     jwplayer(playerId).remove();
            //     $("#" + liveViewBoxId).html("");
            //     $("#" + liveViewBoxId).append("<span class='dropTarget'>No Node</span>");
            //     delete slotSettingAssignments.group[liveViewBoxId];
            //     delete slotSettingAssignments.camera[liveViewBoxId];
            //     delete slotSettingAssignments.channel[liveViewBoxId];
            //     setTimeout(function() {
            //         $("#groupName").html("");
            //     }, 200);
            //     saveSlotSettings();
            // }
        }

        kupPlayer._playWithVlc = function(playerId, urlList, toolbarFlag, slotId) {

            var embedCode = '<OBJECT classid="clsid:9BE31822-FDAD-461B-AD51-BE1D1C159921" ' + 'codebase="http://downloads.videolan.org/pub/videolan/vlc/latest/win32/axvlc.cab" ' + 'width="100%" ' + 'height="100%" ' + 'id="' + playerId + '" ' + 'events="True"> ' + '<param name="MRL" value="" /> ' + '<param name="ShowDisplay" value="True" /> ' + '<param name="AutoLoop" value="False" /> ' + '<param name="AutoPlay" value="False" /> ' + '<param name="toolbar" value="' + toolbarFlag + '" /> ' + '<param name="StartTime" value="0" /> ' + '<EMBED pluginspage="" ' + 'type="application/x-vlc-plugin" ' + 'version="VideoLAN.VLCPlugin.2" ' + 'width="100%" ' + 'height="100%" ' + 'toolbar="' + toolbarFlag + '" ' + 'text="Waiting for video" ' + 'name="' + playerId + '"> ' + '</EMBED> ' + '</OBJECT> ';

            if (utils.detectBrowser() == "ie") {
                getVLC(slotId).innerHTML = embedCode;
            } else {
                document.getElementById(slotId).innerHTML = embedCode;
            }

            var vlc = getVLC(playerId);
            $.each(urlList, function(index, item) {
                vlc.playlist.add(item);
            });

            vlc.playlist.play();
            vlc.audio.toggleMute();

            if (utils.detectBrowser() == "ie") {
                vlc.style.width = "100%";
                vlc.style.height = "100%";
            }
        }

        kupPlayer.getJWUrlList = function(urlList) {
            var playlist = [];
            $.each(urlList, function(index, url) {
                var fileName = index + 1;
                playlist.push({
                    title: fileName,
                    image: "static/images/assets/common/play.png",
                    file: url
                });
            });
            return playlist;
        }

        // VLC helper functions
        function getVLC(name) {
            if (window.document[name]) {
                return window.document[name];
            }
            if (navigator.appName.indexOf("Microsoft Internet") == -1) {
                if (document.embeds && document.embeds[name])
                    return document.embeds[name];
            } else // if (navigator.appName.indexOf("Microsoft Internet")!=-1)
            {
                return document.getElementById(name);
            }
        }

        function doAdd(playerId, targetURL) {
            var vlc = getVLC(playerId);
            var options = [":vout-filter=deinterlace", ":deinterlace-mode=linear"];
            if (vlc) {
                vlc.playlist.add(targetURL, "", options);
                options = [];
                var itemCount = doItemCount();
            }
        }
        return {
            kupPlayer: kupPlayer,
        };
    })

angular.module('kai.liveview')
    .factory('LiveviewService', function(
        KupOption, UtilsService, KupApiService, AuthTokenFactory, DeviceTreeService,
        $q, $filter, $timeout
    ) {
        var kupOpt = KupOption;
        var utils = UtilsService;
        var i18n = UtilsService.i18n;
        var notification = UtilsService.notification;
        var ajaxPost = KupApiService.ajaxPost;
        var data = {};

        //UI selector
        data.$section = '#liveviewSection';
        data.$treeview = '#liveviewTreeview';
        data.$search = '#liveviewSearch';
        data.$dragTarget = '#liveviewDragTarget';

        //UI setting
        data.uiMenu = {
            isShow: true
        };
        data.uiFilterVca = {
            isOpen: false,
            list: (function() {
                var list = [];
                $.each(kupOpt.vca, function(i, vca) {
                    list.push({
                        name: vca.name,
                        type: vca.analyticsType,
                        isActive: false
                    });
                });
                list.unshift({
                    name: 'all',
                    type: 'ALL',
                    isActive: true
                });
                return list;
            })(),
        };
        data.uiTreeview = {
            options: {},
            list: [],
            searchValue: '',
        };
        data.uiDragTarget = {
            options: {},
            list: [],
            isShow: false,
        };

        data.dragTargetIdPrefix = 'liveviewDragTarget';
        data.playerIdPrefix = 'liveviewPlayer';

        data.uiViewType = [{
            name: 'oneview',
            text: '1-view',
            classBtn: 'kup-oneview',
            classGroup: 'kupOneView',
            isActive: true,
            count: 1,
            dragTargetIdPrefix: 'liveviewDragTarget_oneview_',
            playerIdPrefix: 'liveviewPlayer_oneview_',
        }, {
            name: 'fourview',
            text: '4-view',
            classBtn: 'kup-fourviews',
            classGroup: 'kupFourViews',
            isActive: false,
            count: 4,
            dragTargetIdPrefix: 'liveviewDragTarget_fourview_',
            playerIdPrefix: 'liveviewPlayer_fourview_',
        }, {
            name: 'nineview',
            text: '9-view',
            classBtn: 'kup-nineviews',
            classGroup: 'kupNineViews',
            isActive: false,
            count: 9,
            dragTargetIdPrefix: data.dragTargetIdPrefix + '_nineview_',
            playerIdPrefix: data.playerIdPrefix + '_nineview_',
        }];
        //common data
        data.isInitEnd = false;
        data.isSuccessDrag = false;
        data.isDragging = false;
        data.isFullScreen = false;
        data.isPlayerFullScreen = false;
        data.currectFullScreenId = '';
        data.draggedItemData = {};

        data.autoRotation = false;
        data.autoRotationTime = 15; //sec
        data.autoRotationTimeMin = 15; //sec
        data.autoRotationTimeMax = 3600; //sec
        data.autoRotationPromise = null;

        data.playerList = (function() {
            var playerList = {};
            $.each(data.uiViewType, function(i, type) {
                playerList[type.name] = new Array(type.count);
            });
            return playerList;
        })();

        //api response data
        data.apiUserPrefs = {};
        data.apiLiveVideoUrl = (function() {
            var apiLivevideourl = {};
            $.each(data.uiViewType, function(i, type) {
                apiLivevideourl[type.name] = new Array(type.count);
            });
            return apiLivevideourl;
        })();



        return {
            data: data,
            initData: initData,
            setTreeList: setTreeList,
            setCurrectViewType: setCurrectViewType,
            setCurrectFilterVca: setCurrectFilterVca,
            setPlayerList: setPlayerList,

            getCurrectFilterVca: getCurrectFilterVca,
            getDeviceTree: getDeviceTree,
            getUserPrefsApi: getUserPrefsApi,
            saveUserPrefsApi: saveUserPrefsApi,
            getLiveVideoUrlApi: getLiveVideoUrlApi,
            getActiveViewType: getActiveViewType,
            getSelectedPlayer: getSelectedPlayer,
            cancelSelectedPlayer: cancelSelectedPlayer,
            playerSelected: playerSelected,
        };
        /*******************************************************************************
         *
         *  Function Definition
         *
         *******************************************************************************/
        function initData() {
            var opt = data;
            var playerList = {};
            $.each(opt.uiViewType, function(i, type) {
                playerList[type.name] = new Array(type.count);
            });
            opt.playerList = playerList;
        }

        function setTreeList() {
            var opt = data;
            var treeItems = DeviceTreeService.getDeviceTree();

            opt.uiTreeview.list = (function() {
                var tree = angular.copy(treeItems);
                var treeList = [];
                $.each(tree, function(i, label) {
                    var deviceData = label.items;
                    $.each(deviceData, function(j, device) {
                        if (!device.isNode) {
                            device.items = [];
                        }
                    });
                    treeList.push(label);
                });
                return treeList;
            })();
        }

        function setPlayerList(viewTypeName, playerIndex, playerId, cameraData) {
            var opt = data;
            opt.playerList[viewTypeName][playerIndex] = {
                playerId: playerId,
                cameraData: cameraData,
                isAssignCamera: true,
                isLoadVideo: false,
                isLoadVideoing: false,
                isSelected: false,
                isShowInfo: false,
                isPlay: true,
            }
        }

        function setCurrectViewType(index) {
            var opt = data;
            $.each(opt.uiViewType, function(i, list) {
                list.isActive = (index == i) ? true : false;
            })
        }

        function setCurrectFilterVca(index) {
            var opt = data;
            $.each(opt.uiFilterVca.list, function(i, list) {
                list.isActive = (index == i) ? true : false;
            })
        }

        function getCurrectFilterVca() {
            var opt = data;
            var sortItems = [];
            $.each(opt.uiFilterVca.list, function(i, list) {
                if (list.isActive) {
                    sortItems = list;
                    return false;
                }
            })
            return sortItems;
        }

        function getDeviceTree() {
            var opt = data;
            return DeviceTreeService.initDeviceTree()
                .finally(function() {
                    setTreeList();
                });
        }

        function getUserPrefsApi(isDefer) {
            var opt = data;
            var param = {};

            var onSuccess = function(response) {
                opt.apiUserPrefs = response.prefs || {};
                opt.autoRotation = response.prefs.autoRotation;
                opt.autoRotationTime = response.prefs.autoRotationTime;
            };
            var onFail = function(response) {
                opt.apiUserPrefs = {};
            };
            var onError = function() {
                opt.apiUserPrefs = {};
            };
            return ajaxPost('getuserprefs', param, onSuccess, onFail, onError, isDefer);
        }

        function saveUserPrefsApi(isDefer) {
            var opt = data;
            var param = {
                'auto-rotation': opt.autoRotation,
                'auto-rotation-time': opt.autoRotationTime,
            };

            var onSuccess = function(response) {
                notification('success', i18n('update-successful'));
            };
            var onFail = function(response) {
                notification('warning', i18n('update-failed'));
            };
            var onError = function() {
                notification('warning', i18n('update-failed'));
            };
            return ajaxPost('saveuserprefs', param, onSuccess, onFail, onError, isDefer);
        }

        function getLiveVideoUrlApi(viewTypeName, playerIndex, cameraData, isDefer) {
            var opt = data;
            var streamType = (function() {
                var streamType = "http/mjpeg";
                if (cameraData.fullData.model.capabilities.indexOf("h264") != -1) {
                    streamType = "rtmp/h264";
                }
                return streamType;
            })();

            var param = {
                'device-id': cameraData.deviceId,
                'channel-id': cameraData.cameraId,
                'stream-type': streamType
            };

            var onSuccess = function(response) {
                opt.apiLiveVideoUrl[viewTypeName][playerIndex] = {
                    'streamingSessionKey': response['streaming-session-key'],
                    'ttlSeconds': response['ttl-seconds'],
                    'url': response['url'],
                };
            };
            var onFail = function(response) {
                opt.apiLiveVideoUrl[viewTypeName][playerIndex] = {};
            };
            var onError = function() {
                opt.apiLiveVideoUrl[viewTypeName][playerIndex] = {};
            };
            return ajaxPost('getlivevideourl', param, onSuccess, onFail, onError, isDefer);
        }

        function getActiveViewType() {
            var opt = data;
            var viewType = {};
            $.each(opt.uiViewType, function(i, type) {
                if (type.isActive) {
                    viewType = type;
                    return false;
                }
            });
            return viewType;
        }

        function getSelectedPlayer() {
            var opt = data;
            var playerInfo = {};
            $.each(opt.playerList, function(type, playerAry) {
                $.each(playerAry, function(i, player) {
                    if (player && player.isSelected) {
                        playerInfo = player;
                        return false;
                    }
                });
            });
            return playerInfo;
        }

        function cancelSelectedPlayer() {
            var opt = data;
            var playerInfo = {};
            $.each(opt.playerList, function(type, playerAry) {
                $.each(playerAry, function(i, player) {
                    if (player) {
                        player.isSelected = false;
                    }
                });
            });
        }

        function playerSelected(viewTypeName, playerIndex) {
            var opt = data;
            if (opt.playerList[viewTypeName][playerIndex] && opt.playerList[viewTypeName][playerIndex].isAssignCamera) {
                cancelSelectedPlayer();
                opt.playerList[viewTypeName][playerIndex].isSelected = true;
            }
        }
    })

angular
    .module('kai.liveview')
    .controller('LiveviewController', function(
        KupOption, RouterStateService, UtilsService, AuthTokenFactory, Fullscreen,
        LiveviewService, LiveviewPlayerService, DeviceTreeService, MainService,
        $scope, $q, $timeout, $interval, $filter, $animate, $rootScope
    ) {
        var kupOpt = KupOption;
        var utils = UtilsService;
        var i18n = UtilsService.i18n;
        var notification = UtilsService.notification;
        var block = UtilsService.block;
        var mainCtrl = $scope.$parent.mainCtrl;
        var liveviewCtrl = this;

        if (window.jwplayer) {
            window.jwplayer.key = kupOpt.jwplayerKey;
        }

        //UI controller
        liveviewCtrl.data = LiveviewService.data;
        liveviewCtrl.data.isfirefox = $("html").hasClass("k-ff");
        liveviewCtrl.data.uiTreeview.options = getTreeviewOptions();
        liveviewCtrl.data.uiDragTarget.options = getDragTargetOptions();
        liveviewCtrl.fn = {
            isEmptyDevice: DeviceTreeService.isEmptyDevice,
            showMainMenu: MainService.showMainMenu,
            fullScreen: fullScreen,
            setCurrectViewType: setCurrectViewType,
            setCurrectFilterVca: setCurrectFilterVca,
            getCurrectFilterVca: getCurrectFilterVca,
            getCountAry: getCountAry,
            getSelectedPlayer: getSelectedPlayer,

            //player action
            playerHoverIn: playerHoverIn,
            playerHoverOut: playerHoverOut,
            playerPause: playerPause,
            playerPlay: playerPlay,
            playerReflash: playerReflash,
            playerFullScreen: playerFullScreen,
            playerRemove: playerRemove,
            playerSelected: playerSelected,

            //Rotate action
            addTime: addTime,
            subTime: subTime,
            saveTime: saveTime,
        };
        init();

        /*******************************************************************************
         *
         *  Function Definition
         *
         *******************************************************************************/
        function init() {
            initData();
            setWatch();
        }

        function initData() {
            LiveviewService.initData();
        }

        function setWatch() {
            //watch router
            $scope.$watch(function() {
                return angular.toJson(RouterStateService.getRouterState());
            }, function(newVal, oldVal) {
                var routerState = angular.fromJson(newVal).toState;
                var routerCheck = /\.liveview/.test(routerState.name);
                if (!routerCheck) {
                    disableRotation();
                    return false;
                }
                mainCtrl.block.promise = loadUI();
            }, true);

            //watch control
            $scope.$watch('liveviewCtrl.data.uiFilterVca.list', function(newVal, oldVal) {
                var opt = liveviewCtrl.data;
                if (angular.toJson(newVal) !== angular.toJson(oldVal)) {
                    $timeout(function() {
                        setTreeviewForFilter();
                    }, 300);
                }
            }, true);

            $scope.$watch('liveviewCtrl.data.uiTreeview.searchValue', function(newVal, oldVal) {
                var opt = liveviewCtrl.data;
                if (newVal !== oldVal) {
                    $timeout(function() {
                        setTreeviewForFilter();
                    }, 300);
                }
            }, true);

            $scope.$watch('liveviewCtrl.data.autoRotationTime', function(newVal, oldVal) {
                var opt = liveviewCtrl.data;
                var autoRotationTime = parseInt(newVal, 10);
                if (autoRotationTime > opt.autoRotationTimeMax) {
                    opt.autoRotationTime = opt.autoRotationTimeMax;
                }
                if (autoRotationTime < opt.autoRotationTimeMin) {
                    opt.autoRotationTime = opt.autoRotationTimeMin;
                }
            }, true);

            $scope.$watch('liveviewCtrl.data.playerList', function(newVal, oldVal) {
                var opt = liveviewCtrl.data;
                if (angular.toJson(newVal) !== angular.toJson(oldVal)) {
                    $.each(newVal, function(type, player) {
                        if (LiveviewService.getActiveViewType().name !== type) {
                            return true;
                        }
                        $.each(player, function(i, info) {
                            if (!info || $.isEmptyObject(info.cameraData)) {
                                return true;
                            }
                            if (!oldVal[type][i]) {
                                loadVideo(type, i, info.cameraData);
                                return true;
                            }

                            if (angular.toJson(info.cameraData) !== angular.toJson(oldVal[type][i].cameraData)) {
                                loadVideo(type, i, info.cameraData);
                                return true;
                            }
                        });
                    });
                }
            }, true);

            $scope.$watch('liveviewCtrl.data.uiViewType', function(newVal, oldVal) {
                var opt = liveviewCtrl.data;
                var playerList = opt.playerList;
                var autoPlayPause = function() {
                    $.each(playerList, function(type, player) {
                        $.each(player, function(i, info) {
                            if (!info || !info.isLoadVideo || !info.playerId) {
                                return true;
                            }
                            if (LiveviewService.getActiveViewType().name === type) {
                                playerPlay(type, i);
                            } else {
                                playerPause(type, i);
                            }
                        });
                    });
                }
                if (angular.toJson(newVal) !== angular.toJson(oldVal)) {
                    LiveviewService.cancelSelectedPlayer();
                    //autoPlayPause();
                }
            }, true);

            $scope.$watch('liveviewCtrl.data.autoRotation', function(newVal, oldVal) {
                var opt = liveviewCtrl.data;
                if (newVal) {
                    autoRotation();
                } else {
                    disableRotation();
                }
            }, true);

            $rootScope.$on('FBFullscreen.change', function(fullscreen, isEnabled) {
                var opt = liveviewCtrl.data;
                var fullScreenClass = 'kupFullScreen';                
                opt.isFullScreen = isEnabled;
                opt.currectFullScreenId = $(UtilsService.getFullScreenElement()).attr('id');
                opt.isPlayerFullScreen = (function() {
                    return new RegExp(opt.dragTargetIdPrefix).test(opt.currectFullScreenId);
                })();

                if (isEnabled) {
                    $(opt.$section).find('.' + fullScreenClass).removeClass(fullScreenClass);
                    $('#' + opt.currectFullScreenId).addClass(fullScreenClass);
                } else {
                    $(opt.$section).find('.' + fullScreenClass).removeClass(fullScreenClass);
                }
            });
        }

        function loadUI() {
            var opt = liveviewCtrl.data;
            var dfd = $q.defer();
            $timeout(function() {
                LiveviewService.getDeviceTree()
                    .finally(function() {
                        LiveviewService.getUserPrefsApi()
                            .finally(function() {
                                setTreeview();
                                opt.isInitEnd = true;
                                dfd.resolve();
                            })
                    })
            });
            return dfd.promise;
        }

        function setTreeview() {
            var opt = liveviewCtrl.data;
            var treeview = $(opt.$treeview).data("kendoTreeView");
            treeview.dataSource.filter({});
            treeview.dataSource.data(opt.uiTreeview.list);
        }

        function setTreeviewForFilter() {
            var opt = liveviewCtrl.data;
            var treeview = $(opt.$treeview).data("kendoTreeView");

            var filterVcaVal = getCurrectFilterVca();
            var filterSearchVal = $.trim(opt.uiTreeview.searchValue);

            var filterVca = (filterVcaVal.name === "all") ? {} : {
                field: "filterVca",
                operator: "contains",
                value: filterVcaVal.type
            };
            var filterSearch = (filterSearchVal === "") ? {} : {
                field: "filterText",
                operator: "contains",
                value: filterSearchVal
            };
            var filterConfig = (function(filterAry) {
                var filter = [];
                $.each(filterAry, function(i, data) {
                    if (!$.isEmptyObject(data)) {
                        filter.push(data);
                    }
                });

                if (filter.length === 0) {
                    filter = {};
                };

                return filter;
            })([filterVca, filterSearch]);

            var filterVcaCheck = (filterVcaVal.name == 'all') ? new RegExp('') : new RegExp(filterVcaVal.type);
            var filterSearchCheck = new RegExp(filterSearchVal, 'i');
            var uidExpandList = [];
            var execFilter = function() {
                //init close all tree
                treeview.collapse(".k-item");
                //filter tree
                $.each(treeview.dataSource.data(), function(i, labelData) {
                    if (labelData.children) {
                        var labelFilterText = (function() {
                            var tmpAry = labelData.filterText.split(',');
                            tmpAry.splice(0, 1);
                            return tmpAry.toString(',');
                        })();

                        //set tree expand list
                        (function() {
                            if (filterSearchVal === "") {
                                if (filterVcaVal.name === "all") {}
                                if (filterVcaVal.name !== "all") {
                                    if (filterVcaCheck.test($.trim(labelData.filterVca))) {
                                        uidExpandList.push(labelData.uid);
                                    }
                                }
                            }
                            if (filterSearchVal !== "") {
                                if (filterVcaVal.name === "all") {
                                    if (filterSearchCheck.test($.trim(labelFilterText))) {
                                        uidExpandList.push(labelData.uid);
                                    }
                                }
                                if (filterVcaVal.name !== "all") {
                                    if (filterSearchCheck.test($.trim(labelFilterText)) && filterVcaCheck.test($.trim(labelData.filterVca))) {
                                        uidExpandList.push(labelData.uid);
                                    }
                                }
                            }
                        })();
                        //set filter data
                        labelData.children.filter(filterConfig);

                        $.each(labelData.children.data(), function(j, deviceData) {
                            if (deviceData.children) {
                                var deviceFilterText = (function() {
                                    var tmpAry = deviceData.filterText.split(',');
                                    tmpAry.splice(0, 2);
                                    return tmpAry.toString(',');
                                })();
                                //set tree expand list
                                (function() {
                                    if (filterSearchVal === "") {
                                        if (filterVcaCheck === "all") {}
                                        if (filterVcaCheck !== "all") {
                                            if (filterVcaCheck.test($.trim(deviceData.filterVca))) {
                                                uidExpandList.push(deviceData.uid);
                                            }
                                        }
                                    }
                                    if (filterSearchVal !== "") {
                                        if (filterVcaCheck === "all") {
                                            if (filterSearchCheck.test($.trim(deviceFilterText))) {
                                                uidExpandList.push(deviceData.uid);
                                            }
                                        }
                                        if (filterVcaCheck !== "all") {
                                            if (filterSearchCheck.test($.trim(deviceFilterText)) && filterVcaCheck.test($.trim(deviceData.filterVca))) {
                                                uidExpandList.push(deviceData.uid);
                                            }
                                        }
                                    }
                                })();
                                //set filter data
                                deviceData.children.filter(filterConfig);
                            }
                        });

                    }
                });
                treeview.dataSource.filter(filterConfig);

                //open tree
                $.each(uidExpandList, function(i, uid) {
                    treeview.expand('[data-uid="' + uid + '"]');
                });
            };
            $timeout(function() { execFilter() });

        }

        function setCurrectViewType(index) {
            LiveviewService.setCurrectViewType(index);
        }

        function setCurrectFilterVca(index) {
            LiveviewService.setCurrectFilterVca(index);
        }

        function getCurrectFilterVca() {
            return LiveviewService.getCurrectFilterVca();
        }

        function fullScreen(id) {
            var opt = liveviewCtrl.data;
            if (Fullscreen.isEnabled()) {
                Fullscreen.cancel();
            } else {
                Fullscreen.enable(document.getElementById(id));
            }
        }

        function getTreeviewOptions() {
            return {
                dragAndDrop: true,
                template: "# if(item.isOnline == false){ #" +
                    "#=item.text # <i class='fa fa-times-circle-o' uib-tooltip='" + i18n('offline') + "' tooltip-placement='right'></i>" +
                    "# } else { #" +
                    "#=item.text #" +
                    "# } #",
                dataSource: (function() {
                    var opt = liveviewCtrl.data;
                    return opt.uiTreeview.list;
                })(),
                expand: function(e) {},
                dragstart: function(e) {
                    var opt = liveviewCtrl.data;
                    var treeview = $(opt.$treeview).data("kendoTreeView");
                    var uid = treeview.dataItem(e.sourceNode).uid;
                    var draggedItem = treeview.dataItem(e.sourceNode);

                    opt.dragItem = true;
                    opt.isSuccessDrag = true;
                    opt.draggedItemData = {};

                    if (draggedItem.isLabel && !draggedItem.items.length) {
                        notification('warning', "<a ui-sref='main.label' style='text-decoration: underline;'>" + i18n('select-empty-label-error-information') + "</a>", 5000);
                        opt.isSuccessDrag = false;
                        return false;
                    }

                    if (draggedItem.isOnline === false) {
                        notification('warning', i18n('offline-device-not-supported'));
                        opt.isSuccessDrag = false;
                        return false;
                    }

                    opt.draggedItemData = draggedItem;
                    opt.isDragging = true;

                },
                drag: function(e) {
                    var opt = liveviewCtrl.data;
                    var dropTargetIdName = opt.$dragTarget.substring(1);
                    if (
                        ($(e.dropTarget).attr('id') === dropTargetIdName || $(e.dropTarget).parents(opt.$dragTarget).attr('id') === dropTargetIdName) &&
                        opt.isSuccessDrag
                    ) {
                        e.setStatusClass("k-add");
                    }
                },
                drop: function(e) {
                    e.preventDefault();
                    var opt = liveviewCtrl.data;
                    opt.dragItem = false;
                    var dropTargetIdName = opt.$dragTarget.substring(1);
                    opt.isDragging = false;
                    if (
                        ($(e.dropTarget).attr('id') !== dropTargetIdName && $(e.dropTarget).parents(opt.$dragTarget).attr('id') !== dropTargetIdName) ||
                        !opt.isSuccessDrag
                    ) {
                        e.setValid(false);
                    }
                }
            };
        }

        function getDragTargetOptions() {
            return {
                drop: function(e) {
                    var opt = liveviewCtrl.data;
                    if (!opt.isSuccessDrag) {
                        return false;
                    }
                    var dropTargetIdName = opt.$dragTarget.substring(1);
                    var treeview = $(opt.$treeview).data("kendoTreeView");
                    var uiViewType = LiveviewService.getActiveViewType();
                    var targetId = (function() {
                        var target = $("html").hasClass("k-mobile") ? $(e.dropTarget) : $(e.target);
                        var targetId = target.attr('id') || '';
                        if (targetId.indexOf(dropTargetIdName) !== -1) {
                            targetId = target.attr('id');
                        } else {
                            targetId = target.parentsUntil("[id^='" + dropTargetIdName + "']").last().parent().attr('id');
                        }
                        return targetId;
                    })();
                    var playerId = (function() {
                        var playerId = '';
                        var isAddPlayerId = $('#' + targetId).find("[id*='_wrapper']").attr('id') || '';
                        if (isAddPlayerId) {
                            playerId = isAddPlayerId.replace('_wrapper', '');
                        } else {
                            playerId = $('#' + targetId).find('.kupPlayer').attr('id');
                        }
                        return playerId;
                    })();
                    var playerIdAry = playerId.split('_');
                    var playerIndex = parseInt(playerIdAry[playerIdAry.length - 1], 10);

                    var viewType = LiveviewService.getActiveViewType();
                    var deviceData = angular.copy(opt.draggedItemData);
                    var cameraList = (function() {
                        var cameraList = [];
                        if (deviceData.isAll || deviceData.isLabel) {
                            $.each(deviceData.items, function(i, device) {
                                $.each(device.items, function(j, camera) {
                                    cameraList.push(camera);
                                });
                            });
                        }
                        if (deviceData.isDevice) {
                            $.each(deviceData.items, function(i, camera) {
                                cameraList.push(camera);
                            });
                        }
                        if (deviceData.isCamera) {
                            cameraList.push(deviceData);
                        }
                        return cameraList;
                    })();
                    var uiViewType = LiveviewService.getActiveViewType();

                    //set PlayerList
                    (function() {
                        if (cameraList.length == 1) {
                            LiveviewService.setPlayerList(uiViewType.name, playerIndex, playerId, cameraList[0]);
                        } else {
                            $.each(cameraList, function(i, camera) {
                                if (i < uiViewType.count) {
                                    LiveviewService.setPlayerList(uiViewType.name, i, uiViewType.playerIdPrefix + i, camera);
                                }
                            });
                        }

                    })();
                }
            };
        }

        function loadVideo(viewTypeName, playerIndex, cameraData) {
            var opt = liveviewCtrl.data;
            var playerId = opt.playerList[viewTypeName][playerIndex].playerId;

            jwplayer(playerId) && jwplayer(playerId).getState() && jwplayer(playerId).remove();
            opt.playerList[viewTypeName][playerIndex].isLoadVideoing = true;

            LiveviewService.getLiveVideoUrlApi(viewTypeName, playerIndex, cameraData)
                .success(function() {
                    var opt = liveviewCtrl.data;
                    var playerId = opt.playerList[viewTypeName][playerIndex].playerId;

                    if ($.isEmptyObject(opt.apiLiveVideoUrl[viewTypeName][playerIndex])) {
                        opt.playerList[viewTypeName][playerIndex].isLoadVideo = false;
                    } else {
                        opt.playerList[viewTypeName][playerIndex].isLoadVideo = true;
                        LiveviewPlayerService.kupPlayer._playWithJW(opt.playerList[viewTypeName][playerIndex].playerId, opt.apiLiveVideoUrl[viewTypeName][playerIndex].url, true, false, '', null);
                    }
                })
                .error(function() {
                    var opt = liveviewCtrl.data;
                    opt.playerList[viewTypeName][playerIndex].isLoadVideo = false;
                })
                .finally(function() {
                    opt.playerList[viewTypeName][playerIndex].isLoadVideoing = false;
                });
        }

        function getCountAry(count) {
            return new Array(count);
        };

        function getSelectedPlayer() {
            return LiveviewService.getSelectedPlayer();
        }

        /*******************************************************************************
         *
         *  Player Action Function Definition
         *
         *******************************************************************************/
        function playerHoverIn(viewTypeName, playerIndex) {
            var opt = liveviewCtrl.data;
            if (opt.isDragging) {
                return;
            }
            if (opt.playerList[viewTypeName][playerIndex]) {
                opt.playerList[viewTypeName][playerIndex].isShowInfo = true;
            }
        }

        function playerHoverOut(viewTypeName, playerIndex) {
            var opt = liveviewCtrl.data;
            if (opt.isDragging) {
                return;
            }
            if (opt.playerList[viewTypeName][playerIndex]) {
                opt.playerList[viewTypeName][playerIndex].isShowInfo = false;
            }
        }

        function playerPause(viewTypeName, playerIndex) {
            var opt = liveviewCtrl.data;
            var playerId = opt.playerList[viewTypeName][playerIndex].playerId;
            var playerState = window.jwplayer(playerId).getState();
            if (!playerState) {
                return;
            }
            opt.playerList[viewTypeName][playerIndex].isPlay = false;
            window.jwplayer(playerId).pause(true);

        }

        function playerPlay(viewTypeName, playerIndex) {
            var opt = liveviewCtrl.data;
            var playerId = opt.playerList[viewTypeName][playerIndex].playerId;
            var playerState = window.jwplayer(playerId).getState();
            if (!playerState) {
                return;
            }

            if (playerState === 'IDLE') {
                window.jwplayer(playerId).play(true);
            } else {
                var cameraData = opt.playerList[viewTypeName][playerIndex].cameraData;
                loadVideo(viewTypeName, playerIndex, cameraData);
            }
            opt.playerList[viewTypeName][playerIndex].isPlay = true;
        }

        function playerFullScreen(viewTypeName, playerIndex, id) {
            var opt = liveviewCtrl.data;
            //var playerId = opt.playerList[viewTypeName][playerIndex].playerId;
            var fullScreenId = $(UtilsService.getFullScreenElement()).attr('id');
            if (Fullscreen.isEnabled() && fullScreenId === id) {
                Fullscreen.cancel();
            } else {
                Fullscreen.enable(document.getElementById(id));
            }
        }

        function playerReflash(viewTypeName, playerIndex) {
            var opt = liveviewCtrl.data;
            var playerId = opt.playerList[viewTypeName][playerIndex].playerId;

            /**
             * because url stream have the session control, so reflash fn can't not reload, must to resend api
             */
            //var urlList = LiveviewPlayerService.kupPlayer.getJWUrlList(opt.apiLiveVideoUrl[viewTypeName][playerIndex].url);
            //window.jwplayer(playerId).load(urlList).play(true);

            var cameraData = opt.playerList[viewTypeName][playerIndex].cameraData;
            loadVideo(viewTypeName, playerIndex, cameraData);
            opt.playerList[viewTypeName][playerIndex].isPlay = true;
        }

        function playerRemove(viewTypeName, playerIndex) {
            var opt = liveviewCtrl.data;
            var playerId = opt.playerList[viewTypeName][playerIndex].playerId;
            var playerState = window.jwplayer(playerId).getState();
            if (!playerState) {
                return;
            }
            window.jwplayer(playerId).remove();
            $('#' + playerId).addClass('kupPlayer');
            opt.playerList[viewTypeName][playerIndex] = null;

        }

        function playerSelected(viewTypeName, playerIndex) {
            LiveviewService.playerSelected(viewTypeName, playerIndex);
            // var opt = liveviewCtrl.data;
            // if (opt.playerList[viewTypeName][playerIndex] && opt.playerList[viewTypeName][playerIndex].isAssignCamera) {
            //     LiveviewService.cancelSelectedPlayer();
            //     opt.playerList[viewTypeName][playerIndex].isSelected = true;
            // }
        }
        /*******************************************************************************
         *
         *  Rotate Action Function Definition
         *
         *******************************************************************************/
        function addTime() {
            var opt = liveviewCtrl.data;
            if (opt.autoRotationTime < opt.autoRotationTimeMax) {
                opt.autoRotationTime++;
            } else {
                opt.autoRotationTime = opt.autoRotationTimeMax;
            }
        }

        function subTime() {
            var opt = liveviewCtrl.data;
            if (opt.autoRotationTime > opt.autoRotationTimeMin) {
                opt.autoRotationTime--;
            } else {
                opt.autoRotationTime = opt.autoRotationTimeMin;
            }
        }

        function saveTime() {
            var opt = liveviewCtrl.data;
            autoRotation();
            LiveviewService.saveUserPrefsApi();
        }

        function autoRotation() {
            var opt = liveviewCtrl.data;
            var playerList = opt.playerList;
            var autoRotation = opt.autoRotation;
            var autoRotationTime = opt.autoRotationTime * 1000;

            var autoLoadVideo = function() {
                $.each(playerList, function(type, player) {
                    if (LiveviewService.getActiveViewType().name !== type) {
                        return true;
                    }
                    $.each(player, function(i, info) {
                        if (!info || !info.isAssignCamera || !info.playerId || $.isEmptyObject(info.cameraData)) {
                            return true;
                        }
                        loadVideo(type, i, info.cameraData);
                    });
                });
            };

            //cancel promise
            disableRotation();

            if (autoRotation) {
                opt.autoRotationPromise = $interval(function() {
                    autoLoadVideo();
                }, autoRotationTime);
            }
        };

        function disableRotation() {
            var opt = liveviewCtrl.data;
            if (opt.autoRotationPromise) {
                $interval.cancel(opt.autoRotationPromise);
            }
        }
    });
