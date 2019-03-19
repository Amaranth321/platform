angular.module('kai.main', []);

angular.module('kai.main');

angular.module('kai.main')
	.service('AlertsNotificationService', function (KupApiService, KupOption, AuthTokenFactory, poller) {
		this.initAlertsNotificationSerice = function() {
			var apiUrl = KupOption.sysApiRootUrl + '/api/' + AuthTokenFactory.getBucket();
			var params = $.param({
		        "session-key" : AuthTokenFactory.getSessionKey()
		    });
		  	return poller.get(apiUrl + '/recvcometnotification', {
				action: 'post',
		      	argumentsArray: [
					params,
		          	{
						headers: {
							'Accept': '*/*',
		                  	'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
		                  	'X-Requested-With': 'XMLHttpRequest'
		              	}
		          	}
		      	],
		      	smart: true,
		      	catchError: true
		  	});
      	}
	})
angular.module('kai.main')
    .factory('HeaderService', function(AuthTokenFactory) {
        var data = {};
        data.isDropdownOpen = {
            notification: false,
            settings: false,
            userState: false
        };

        return {
            data: data,
            getUserRoles: getUserRoles,
            getUserName: getUserName,
        };
        /*******************************************************************************
         *
         *  Function Definition
         *
         *******************************************************************************/
        function getUserRoles() {
            return AuthTokenFactory.getUserRole();
        }

        function getUserName() {
            return AuthTokenFactory.getUserProfile().userName;
        }
    })

angular.module('kai.main')
    .factory('MainService', function(
        KupOption, UtilsService, KupApiService,
        $animate, $timeout
    ) {
        var kupOpt = KupOption;
        var utils = UtilsService;
        var i18n = UtilsService.i18n;
        var notification = UtilsService.notification;
        var ajaxPost = KupApiService.ajaxPost;
        var data = {};

        data.header = {};
        data.header.wrapperToggledMaxWidth = 960
        data.header.isWrapperToggled = ($(window).width() < data.header.wrapperToggledMaxWidth) ? true : false;

        data.navbar = {
            isShow: true,
            isIconLeftArrow: false,
            menu: {
                isShow: {},
                isOpen: {}
            }
        };
        data.block = {
            options: {},
            promise: null,
        };
        data.apiPlatformInformation = {};

        return {
            data: data,
            showMainMenu: showMainMenu,
            getPlatformInformationApi: getPlatformInformationApi,
        };
        /*******************************************************************************
         *
         *  Function Definition
         *
         *******************************************************************************/
        function showMainMenu(checkState) {
            var opt = data;
            if (checkState === undefined) {
                $(".sidebar-sub").toggleClass('sidebar-sub-animation');
                opt.navbar.isIconLeftArrow = !opt.navbar.isIconLeftArrow;
            } else {
                if (checkState) {
                    $(".sidebar-sub").removeClass('sidebar-sub-animation');
                } else {
                    $(".sidebar-sub").addClass('sidebar-sub-animation');
                }
                opt.navbar.isIconLeftArrow = checkState;
            }
        }

        function getPlatformInformationApi() {
            var opt = data;
            var param = {};
            var onSuccess = function(response) {
                opt.apiPlatformInformation = response.info || {};
            };
            var onFail = function(response) {
                opt.apiPlatformInformation = {};
            };
            var onError = function() {
                opt.apiPlatformInformation = {};
            };
            return ajaxPost('getplatforminformation', param, onSuccess, onFail, onError);
        }
    })

angular.module('kai.main')
    .controller('confirmGoAdminSetting', function($scope, data, $uibModalInstance, $window) {
        $scope.cancel = function() {
            $uibModalInstance.dismiss('cancel');
        };
        $scope.yes = function() {
            $window.open(data.link, '_blank');
            $scope.cancel();
        };
    });

angular.module('kai.main')
    .controller('HeaderController', function(_, KupOption, UserService, HeaderService, SoundManagerService, UtilsService, AlertsNotificationService, $log, $window, $scope, AuthTokenFactory, $uibModal, $state) {

        var headerCtrl = this;
        var i18n = UtilsService.i18n;

        headerCtrl.data = HeaderService.data;
        // headerCtrl.data.wrapperToggledMaxWidth = 960;
        // headerCtrl.data.isWrapperToggled = ($(window).width() < headerCtrl.data.wrapperToggledMaxWidth) ? true : false;
        // headerCtrl.data.isDropdownOpen = {
        //     notification: false,
        //     settings: false,
        //     userState: false
        // };

        function convertUTCToLocal(time) {
            var d = new Date();
            var dateObj = $window.moment(time, "DD/MM/YYYY HH:mm:ss").toDate();
            dateObj.setMinutes(dateObj.getMinutes() - d.getTimezoneOffset());
            return $window.moment(dateObj).format("DD/MM/YYYY HH:mm:ss");
        }

        headerCtrl.count = $window.notification_count = 0;
        headerCtrl.events = [];

        headerCtrl.fn = {
            logout: logout,
            confirmGoAdminSetting: confirmGoAdminSetting
        }

        function confirmGoAdminSetting() {
            var data = {
                'link': KupOption.sysApiRootUrl
            };
            var modalInstance = $uibModal.open({
                animation: true,
                templateUrl: 'confirmGoAdminSetting.html',
                controller: 'confirmGoAdminSetting',
                windowClass: 'modal kupModal',
                resolve: {
                    data: function() {
                        return data;
                    }
                }
            });
        }

        //var eventString1 = "{\"id\":\"55612fcee4b031606368a0c9\",\"data\":\"{\\\"device-id\\\":\\\"24\\\",\\\"channel-id\\\":\\\"1\\\",\\\"duration\\\":\\\"3\\\"}\",\"type\":\"event-vca-loitering\",\"time\":\"24/05/2015 01:56:30\",\"deviceId\":\"24\",\"channelId\":\"1\",\"binaryData\":\"[B@459c1798\",\"deviceName\":\"BVT Node 4\"}";
        //var eventString2 = "{\"id\":\"556176ede4b0cb66e43b3a78\",\"data\":\"{\\\"device-id\\\":\\\"3\\\",\\\"channel-id\\\":\\\"0\\\",\\\"fgpercent\\\":\\\"6\\\",\\\"fgpixels\\\":\\\"235\\\"}\",\"type\":\"event-vca-intrusion\",\"time\":\"24/05/2015 06:59:57\",\"deviceId\":\"3\",\"channelId\":\"0\",\"binaryData\":\"[B@4f83a953\",\"deviceName\":\"Virtual AMTK IP Camera\"}";
        //var eventString3 = "{\"id\":\"556177a2e4b0cb66e43b3b6e\",\"data\":\"{\\\"device-id\\\":\\\"24\\\",\\\"channel-id\\\":\\\"3\\\",\\\"fgpercent\\\":\\\"68\\\",\\\"fgpixels\\\":\\\"200\\\"}\",\"type\":\"event-vca-perimeter\",\"time\":\"24/05/2015 07:02:58\",\"deviceId\":\"24\",\"channelId\":\"3\",\"binaryData\":\"[B@63ed0e84\",\"deviceName\":\"BVT Node 4\"}";
        //var testEvents = [eventString1, eventString2, eventString3];
        //
        //angular.forEach(testEvents, function(eventString) {
        //    // parse event json into json object
        //    var eventObj = angular.fromJson(eventString);
        //    // convert alert time into local time
        //    var eventLocalTime = convertUTCToLocal(eventObj.time);
        //    $log.log("eventLocalTime " + eventLocalTime);
        //    var newEvent = {};
        //    newEvent.type = i18n(eventObj.type);
        //    newEvent.typeKey = eventObj.type;
        //    newEvent.deviceName = eventObj.deviceName;
        //    newEvent.eventTime = $window.moment(eventLocalTime, "DD/MM/YYYY HH:mm:ss").toDate();
        //    newEvent.eventLocalTime = eventLocalTime;
        //    newEvent.eventDisplayTime = '';
        //    headerCtrl.events.push(newEvent);
        //});
        //event-vca-intrusion,event-vca-perimeter,event-vca-loitering,event-vca-object-counting,event-vca-video-blur

        var eventTypeClasses = {};
        eventTypeClasses['event-vca-intrusion'] = "IntrusionDetection";
        eventTypeClasses['event-vca-perimeter'] = "PerimeterDefense";
        eventTypeClasses['event-vca-loitering'] = "LoiteringDetection";
        eventTypeClasses['event-vca-object-counting'] = "TripWireCounting";
        eventTypeClasses['event-vca-video-blur'] = "CameraTampering";

        headerCtrl.userInfo = function() {
            $scope.$watch(function() {
                return angular.toJson(AuthTokenFactory.data);
            }, function(newVal, oldVal) {
                headerCtrl.multipleRoles = "";
                headerCtrl.userName = HeaderService.getUserName();
                headerCtrl.roleDisplayText = HeaderService.getUserRoles();
            }, true);
        }
        headerCtrl.userInfo();
        headerCtrl.getEventClass = function(event) {
            // $log.log("event type is  " + event.typeKey);
            var eventCSSClass = eventTypeClasses[event.typeKey];
            // $log.log("event css class is " + eventCSSClass);
            return eventCSSClass;
        };
        headerCtrl.adjustAlertTime = function() {
            // $log.log("time " + new Date());
            angular.forEach(headerCtrl.events, function(event) {
                var differentObject = headerCtrl.getDateDifference(new Date(), $window.moment(event.eventLocalTime, "DD/MM/YYYY HH:mm:ss").toDate());
                var displayTime = '';
                if (differentObject.days > 0)
                    displayTime = differentObject.days + " " + i18n("day(s)-ago");
                else if (differentObject.hours > 0)
                    displayTime = differentObject.hours + " " + i18n("hour(s)-ago");
                else if (differentObject.minutes > 0)
                    displayTime = differentObject.minutes + " " + i18n("minute(s)-ago");
                else
                    displayTime = differentObject.seconds + " " + i18n("second(s)-ago");

                event.eventDisplayTime = displayTime;

            });
        };

        // $scope.$watch(function() {
        //     return headerCtrl.data.isDropdownOpen.notification;
        // }, function(newVal, oldVal) {
        //     if (!newVal) {
        //         headerCtrl.showCount = 0;
        //         headerCtrl.count = 0;
        //         headerCtrl.events = [];
        //     }
        // });

        headerCtrl.cleanCount = function() {
            headerCtrl.showCount = 0;
            headerCtrl.count = 0;
            headerCtrl.events = [];
            $state.go('main.notification');
        };

        headerCtrl.getDateDifference = function(date1, date2) {
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
        };


        // add to current currents array
        // increase total number of alerts notification received count

        headerCtrl.showCount = '';

        var alertsNotificationPoller = AlertsNotificationService.initAlertsNotificationSerice();
        alertsNotificationPoller.promise.then(null, null, function(result) {
            // $log.log("notification " + JSON.stringify(result));
            if (result.status === 200) {
                if (result.data.result == "ok" && result.data.event != null) {
                    // $log.log("success of notification " + result);
                    // parse event json into json object
                    var eventObj = angular.fromJson(result.data.event);
                    // convert alert time into local time
                    var eventLocalTime = convertUTCToLocal(eventObj.time);
                    var newEvent = {};
                    newEvent.type = i18n(eventObj.type);
                    newEvent.typeKey = eventObj.type;
                    newEvent.deviceName = eventObj.deviceName;
                    newEvent.eventTime = $window.moment(eventLocalTime, "DD/MM/YYYY HH:mm:ss").toDate();
                    newEvent.eventLocalTime = eventLocalTime;
                    newEvent.deviceId = eventObj.deviceId;
                    newEvent.eventId = eventObj.id;
                    newEvent.eventDisplayTime = '';
                    if (headerCtrl.count > 2) {
                        headerCtrl.events.shift();
                    }
                    headerCtrl.count++;
                    headerCtrl.showCount = (headerCtrl.count > 99) ? '99+' : headerCtrl.count;
                    headerCtrl.events.push(newEvent);
                    headerCtrl.playSound();
                }
                // Success handler ...
            } else {
                $log.log("failure of notification " + result);
                // Error handler: (data, status, headers, config)
                if (result.status === 503) {
                    // Stop poller or provide visual feedback to the user etc
                    alertsNotificationPoller.stopAll();
                }
            }
        });

        headerCtrl.playSound = function() {
            if (headerCtrl.soundOpen) {
                SoundManagerService.play();
            }
        }
        headerCtrl.soundOpen = true;
        headerCtrl.soundStatusMsg = 'notification-sound-on-label';
        headerCtrl.toggleNotificationSound = function(event) {
            headerCtrl.soundOpen = !headerCtrl.soundOpen;
            headerCtrl.soundStatusMsg = headerCtrl.soundOpen ? 'notification-sound-on-label' : 'notification-sound-off-label';
            event.stopPropagation();
        };

        headerCtrl.getSoundOnOffClass = function() {
            return headerCtrl.soundOpen ? "soundOn" : "soundOff";
        };
        headerCtrl.getVolumnOnOffClass = function() {
            return headerCtrl.soundOpen ? "fa-volume-up" : "fa-volume-off";
        };

        function logout() {
            UserService.logout()
                .finally(function() {
                    window.location = "/";
                });
        }



    });

angular.module('kai.main')
    .controller('MainCtrl',
        function(
            KupOption,
            AuthTokenFactory, RouterStateService, UserService, UtilsService, PromiseFactory, KupApiService,
            MainService, NotificationService,
            $scope, $timeout, $uibModal
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var block = UtilsService.block;
            var notification = UtilsService.notification;
            var indexCtrl = $scope.$parent.indexCtrl;
            var mainCtrl = this;

            //main ctrl
            mainCtrl.data = MainService.data;
            mainCtrl.fn = {
                openMenu: openMenu,
                showMainMenu: MainService.showMainMenu,
                toggleSideBar: toggleSideBar,
                viewNotificationDetail: viewNotificationDetail
            };

            //header ctrl
            mainCtrl.header = MainService.data.header;

            //main navbar ctrl
            mainCtrl.navbar = MainService.data.navbar;
            mainCtrl.bucket = AuthTokenFactory.getBucket();

            //main block ctrl
            mainCtrl.block = MainService.data.block;

            init();
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function init() {
                //authToken ctrl
                $scope.$watch(function() {
                    return angular.toJson(AuthTokenFactory.data);
                }, function(newVal, oldVal) {
                    var authToken = angular.fromJson(newVal);
                    //check is auth
                    if (!authToken['bucket'] || !authToken['session-key']) {
                        window.location = "#/login";
                    }
                }, true);

                //router ctrl
                $scope.$watch(function() {
                    return angular.toJson(RouterStateService.getRouterState());
                }, function(newVal, oldVal) {
                    var routerNameNew = angular.fromJson(newVal).toState.name;
                    var routerNameOld = angular.fromJson(oldVal).toState.name;

                    //to fix when quick change page, sometimes the page block can't disappear' 
                    $('.cg-busy').addClass('ng-hide');

                    //check if first time to load page
                    (function() {
                        if (routerNameNew === routerNameOld) {
                            initLoadPage();
                        } else {
                            KupApiService.ajaxCancel();
                        }
                    })();
                }, true);

                $scope.$watch('mainCtrl.block.promise', function(newVal, oldVal) {
                    if (newVal) {
                        mainCtrl.block.options = UtilsService.blockPage(newVal);
                        mainCtrl.block.promise = null;
                    }
                }, true);
            }

            function initLoadPage() {
                MainService.getPlatformInformationApi();
                UserService.setUserFeature()
                    .finally(function() {
                        mainCtrl.navbar.menu.isShow = getMenuStatus().showData;
                        mainCtrl.navbar.menu.isOpen = getMenuStatus().openData;
                    });
                UserService.setUserTheme()
                    .finally(function() {
                        indexCtrl.currentTheme = AuthTokenFactory.getTheme();
                    });
                UserService.setUserProfile()
                    .finally(function() {
                        indexCtrl.currentLanguage = AuthTokenFactory.getUserLanguage();
                    });
            }

            function openMenu(openMenuName) {
                var openMenuInfo = mainCtrl.navbar.menu.isOpen;
                $.each(openMenuInfo, function(menuName, value) {
                    if (openMenuName === menuName) {
                        mainCtrl.navbar.menu.isOpen[menuName] = !mainCtrl.navbar.menu.isOpen[menuName];
                    } else {
                        mainCtrl.navbar.menu.isOpen[menuName] = false;
                    }
                });
            }

            function getMenuStatus() {
                var userFeature = UserService.getUserFeature();
                var menu = {
                    showData: {},
                    openData: {}
                };
                $.each(userFeature, function(i, featureData) {
                    if (!menu.showData[featureData.type]) {
                        menu.showData[featureData.type] = true;
                        menu.openData[featureData.type] = false;
                    }
                    menu.showData[featureData.name] = true;
                });
                return menu;
            }

            function toggleSideBar(callback) {
                var headerOpt = mainCtrl.header;

                $("#kupWrapper").toggleClass("toggled", 400);
                headerOpt.isWrapperToggled = $("#kupWrapper").hasClass('toggled');
                setTimeout(function() {
                    //jQuery(document).resize();
                    callback && callback();
                }, 400);
            };

            function viewNotificationDetail(event) {

                var tmp,
                    pageParam = {
                        'skip': 0,
                        'take': 50
                    };

                NotificationService.getUserDevicesApi().then(function() {
                    NotificationService.getAlertsApi(pageParam).then(function(response) {
                        var alerts = NotificationService.setTableData();
                        angular.forEach(alerts, function(value, key) {
                            if (value.eventId == event.eventId) {
                                event.detail = value;
                            };
                        });
                        angular.forEach(response.data.alerts, function(value, key) {
                            if (value.eventId == event.eventId) {
                                event.eventVideoUrl = value.eventVideoUrl;
                            };
                        });
                    });
                });

                $uibModal.open({
                    animation: true,
                    templateUrl: 'viewNotificationDetail.html',
                    controller: 'viewNotificationDetailCtrl',
                    windowClass: 'modal kupModal',
                    resolve: {
                        event: function() {
                            return {
                                'data': event
                            }
                        }
                    }
                });

            }

        });


angular.module('kai.main').controller('viewNotificationDetailCtrl', function($scope, $uibModalInstance, event, NotificationService, KupOption, AuthTokenFactory, $timeout, UtilsService) {
    $scope.event = event.data;
    $scope.close = function() {
        $uibModalInstance.dismiss('cancel');
    };
    $scope.getEventInfoByType = function(eventType) {
        return NotificationService.getEventInfoByType(eventType);
    };
    $timeout(function() {
        if (window.jwplayer) {
            window.jwplayer.key = KupOption.jwplayerKey;
        }
        var url = $scope.event.eventVideoUrl;
        var videoUrl = KupOption.sysApiRootUrl + url;
        var playLink = videoUrl + "?action=play&session-key=" + AuthTokenFactory.getSessionKey();
        if (url) {
            window.jwplayer("kupPlayer").setup({
                file: playLink,
                width: "100%",
                height: "100%",
                autostart: false,
                mute: true,
                flashplayer: KupOption.jwplayerFlashPlayerUrl,
                html5player: KupOption.jwplayerHtml5PlayerUrl
            });
        };
    }, 1000);
});
