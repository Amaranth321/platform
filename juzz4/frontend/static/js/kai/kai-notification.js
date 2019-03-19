angular.module('kai.notification', [
    'datatables'
]);

angular
    .module('kai.notification')
    .factory("NotificationModalService",
        function(
            KupOption,
            UtilsService, KupApiService
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var ajaxPost = KupApiService.ajaxPost;

            var data = {
                isLoadVideo: false,
                isOpenModal: false,
            };
            return {
                data: data,
            };
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
        });

angular.module('kai.notification')
    .factory('NotificationService', function(
        KupOption, UtilsService, KupApiService, AuthTokenFactory,
        DTOptionsBuilder, DTColumnDefBuilder, DTColumnBuilder,
        $q, $filter, $timeout
    ) {
        var kupOpt = KupOption;
        var utils = UtilsService;
        var i18n = UtilsService.i18n;
        var notification = UtilsService.notification;
        var ajaxPost = KupApiService.ajaxPost;
        var data = {};

        data.timeFormat = kupOpt.kendoTimeFormat;

        data.dateRange = 7;
        data.dateFormat = kupOpt.momentTimeFormat[0];

        data.startDateFormat = kupOpt.momentTimeFormatForStart[0];
        data.endDateFormat = kupOpt.momentTimeFormatForEnd[0];

        data.defaultSelectDeviceText = i18n('please-select-device');
        data.defaultSelectCameraText = i18n('please-select-camera');
        data.defaultSelectCameraTextForEmpty = i18n('no-cameras');
        data.defaultSelectLocationText = i18n('please-select-location');
        data.notMappingDeviceText = i18n('there-is-no-matching-device');

        data.deviceDataSource = [];
        data.locationDataSource = [];
        data.tableDataSource = [];

        data.selectTableData = [];
        data.apiUserDevices = [];
        data.apiAlerts = [];
        data.apiAlertsTotal = 0;
        data.apiExportAlerts = '';
        data.apiLabels = [];
        data.requestForReport = [];

        data.$daterange = '#kupDaterange';
        data.uiType = (function() {
            var type = [];
            var allEventType = [];
            var occupancyEvent = 'event-occupancy-limit';

            //vca security type
            $.each(kupOpt.vca, function(key, vca) {
                if (vca.typeId !== 1) {
                    return true;
                }
                type.push({
                    name: vca.name,
                    text: vca.name,
                    value: vca.eventType,
                    class: vca.class
                });
                allEventType.push(vca.eventType);
            });
            allEventType.push(occupancyEvent);

            //all type
            type.unshift({
                name: 'all',
                text: 'all-security-events',
                value: allEventType.toString(',')
            });

            //occupancy type
            type.push({
                name: 'occupancy',
                text: 'occupancy-limit',
                value: occupancyEvent,
                class: ''
            });
            return type;
        })();
        data.uiDateRangesList = [{
            name: i18n('today'),
            data: [
                moment(new Date(moment().format(data.startDateFormat))),
                moment(new Date(moment().format(data.endDateFormat)))
            ]
        }, {
            name: i18n('yesterday'),
            data: [
                moment(new Date(moment().subtract(1, 'days').format(data.startDateFormat))),
                moment(new Date(moment().subtract(1, 'days').format(data.endDateFormat)))
            ]
        }, {
            name: i18n('last-7-days'),
            data: [
                moment(new Date(moment().subtract(6, 'days').format(data.startDateFormat))),
                moment(new Date(moment().format(data.endDateFormat)))
            ]
        }, {
            name: i18n('last-30-days'),
            data: [
                moment(new Date(moment().subtract(29, 'days').format(data.startDateFormat))),
                moment(new Date(moment().format(data.endDateFormat)))
            ]
        }, {
            name: i18n('this-month'),
            data: [
                moment(new Date(moment().startOf('month').format(data.startDateFormat))),
                moment(new Date(moment().endOf('month').format(data.endDateFormat)))
            ]
        }, {
            name: i18n('last-month'),
            data: [
                moment(new Date(moment().subtract(1, 'month').startOf('month').format(data.startDateFormat))),
                moment(new Date(moment().subtract(1, 'month').endOf('month').format(data.endDateFormat)))
            ]
        }];
        data.uiTable = {};
        data.uiTableBlock = {
            options: {}
        };

        data.searchInfo = {
            //call api
            type: data.uiType[0],
            startDate: new Date(moment().format(data.startDateFormat)),
            endDate: new Date(moment().format(data.endDateFormat)),

            //front end filter
            keyword: "",
            device: {},
            camera: {},
            location: {},
            limit: 0,
        };
        return {
            data: data,
            initSearchData: initSearchData,
            setSearchData: setSearchData,
            setTableData: setTableData,

            getEventInfoByType: getEventInfoByType,
            getUserDevicesApi: getUserDevicesApi,
            getAlertsApi: getAlertsApi,
            exportAlertsApi: exportAlertsApi,
            getLabelsApi: getLabelsApi,

        };
        /*******************************************************************************
         *
         *  Set Data Function Definition
         *
         *******************************************************************************/
        function initSearchData() {
            var opt = data;
            opt.searchInfo.keyword = "";
            opt.searchInfo.device = opt.deviceDataSource[0];
            opt.searchInfo.camera = opt.searchInfo.device.cameraData[0];
            opt.searchInfo.location = opt.locationDataSource[0];
            opt.searchInfo.limit = 0;
        }

        function setSearchData() {
            var opt = data;
            var deviceDataSource = [];
            var locationDataSource = [{
                locationId: '',
                locationName: opt.defaultSelectLocationText
            }];

            var getDefaultCamera = function(isEmpty) {
                var defaultCamera = {};
                if (isEmpty) {
                    defaultCamera = {
                        cameraId: '',
                        cameraName: opt.defaultSelectCameraTextForEmpty
                    }
                } else {
                    defaultCamera = {
                        cameraId: '',
                        cameraName: opt.defaultSelectCameraText
                    }
                }
                return defaultCamera;
            };

            deviceDataSource.push({
                deviceId: '',
                deviceName: opt.defaultSelectDeviceText,
                cameraData: [getDefaultCamera(false)]
            });
            $.each(opt.apiUserDevices, function(i, device) {
                var carmeraData = (device.node) ? device.node.cameras : [];

                locationDataSource = (function() {
                    var tmpAry = angular.copy(locationDataSource);
                    var isPush = false;
                    $.each(tmpAry, function(j, location) {
                        if (location.locationName === device.address) {
                            isPush = false;
                            return false;
                        }
                        isPush = true;
                    });
                    if (isPush) {
                        tmpAry.push({
                            locationId: i,
                            locationName: device.address
                        });
                    }
                    return tmpAry;
                })();
                deviceDataSource.push({
                    deviceId: device.id,
                    deviceName: device.name,
                    cameraData: (function() {
                        var isEmpty = !carmeraData.length ? true : false;
                        var cameraInfo = [];
                        cameraInfo.push(getDefaultCamera(isEmpty));
                        $.each(carmeraData, function(j, camera) {
                            cameraInfo.push({
                                cameraId: camera.nodeCoreDeviceId,
                                cameraName: camera.name
                            });
                        });
                        return cameraInfo;
                    })(),
                    fullData: device,
                });
            });
            opt.deviceDataSource = deviceDataSource;
            opt.locationDataSource = locationDataSource;

            opt.searchInfo.device = deviceDataSource[0];
            opt.searchInfo.camera = deviceDataSource[0].cameraData;
            opt.searchInfo.location = locationDataSource[0];
        }

        function setTableData() {
            var opt = data;
            opt.tableDataSource = angular.copy(opt.apiAlerts);

            $.each(opt.tableDataSource, function(i, alert) {
                //base info
                alert.deviceInfo = (function() {
                    var deviceInfo = {};
                    $.each(opt.apiUserDevices, function(j, device) {
                        if (device.deviceId === alert.deviceId) {
                            deviceInfo = device;
                            return false;
                        }

                    });
                    return deviceInfo;
                })();

                alert.deviceInfo = (function() {
                    var deviceInfo = {};
                    $.each(opt.apiUserDevices, function(j, device) {
                        if (device.deviceId === alert.deviceId) {
                            deviceInfo = device;
                            return false;
                        }
                    });
                    return deviceInfo;
                })();

                alert.cameraInfo = (function() {
                    var cameraInfo = {};
                    $.each(opt.apiUserDevices, function(j, device) {
                        if (device.deviceId !== alert.deviceId) {
                            return true;
                        }
                        $.each(device.node.cameras, function(k, camera) {
                            if (camera.nodeCoreDeviceId === alert.channelId) {
                                cameraInfo = camera;
                                return false;
                            }
                        });
                    });
                    return cameraInfo;
                })();

                //state
                alert.isMapping = (!$.isEmptyObject(alert.deviceInfo) && !$.isEmptyObject(alert.cameraInfo)) ? true : false;
                alert.isOccupancy = (alert.eventType === 'event-occupancy-limit') ? true : false;

                //table field to mapping DTColumnBuilder.newColumn
                alert.timeLocal = kendo.toString(UtilsService.UTCToLocal(new Date(alert.time)), opt.timeFormat[0]);
                alert.timeLocal = moment(alert.timeLocal).format(KupOption.default.formatlDatetime);
                alert.alertTypeText = (function() {
                    var text = '';
                    var labelId = '';
                    if (alert.isOccupancy) {
                        $.each(opt.apiUserDevices, function(j, device) {
                            if (device.deviceId !== alert.deviceId) {
                                return true;
                            }
                            $.each(device.channelLabels, function(k, cLabel) {
                                if (cLabel.channelId !== alert.channelId) {
                                    return true;
                                }
                                labelId = cLabel.labels[0] || '';
                                $.each(opt.apiLabels, function(l, label) {
                                    if (labelId !== label.labelId) {
                                        return true;
                                    }
                                    text = '<span><span><i class="kup-store"></i>' + label.name + '</span></span>';
                                });
                            });

                        });
                    } else {
                        if (alert.isMapping) {
                            text = '<span><span class="kupNode"><i class="kup-node"></i>' + alert.deviceInfo.name + '</span><span class="kupDragLine"> | </span><span class="kupCamera"><i class="fa fa-video-camera"></i>' + alert.cameraInfo.name + '</span></span>';
                        } else {
                            text = '<span>' + opt.notMappingDeviceText + '</span>';
                        }
                    }
                    return text;
                })();
                alert.eventTypeText = i18n(getEventInfoByType(alert.eventType).text) || '';
                alert.location = alert.deviceInfo.address || '';

                //other info
                alert.timeLocalShortText = (function() {
                    var displayTime = '';
                    var differentObject = UtilsService.getDateDifference(new Date(), new Date(alert.timeLocal));
                    if (differentObject.days > 0)
                        displayTime = differentObject.days + " " + i18n("day(s)-ago");
                    else if (differentObject.hours > 0)
                        displayTime = differentObject.hours + " " + i18n("hour(s)-ago");
                    else if (differentObject.minutes > 0)
                        displayTime = differentObject.minutes + " " + i18n("minute(s)-ago");
                    else
                        displayTime = differentObject.seconds + " " + i18n("second(s)-ago");
                    return displayTime
                })();


            });
            return opt.tableDataSource;
        }

        /*******************************************************************************
         *
         *  Get Data Function Definition
         *
         *******************************************************************************/
        function getEventInfoByType(eventType) {
            var opt = data;
            var eventInfo = {};
            $.each(opt.uiType, function(i, type) {
                if (type.value === eventType) {
                    eventInfo = type;
                    return false;
                }
            });
            return eventInfo;
        }

        /*******************************************************************************
         *
         *  API Function Definition
         *
         *******************************************************************************/

        function getUserDevicesApi(isDefer) {
            var opt = data;
            var param = {};
            var onSuccess = function(response) {
                opt.apiUserDevices = response.devices || [];
            };
            var onFail = function(response) {
                opt.apiUserDevices = [];
            };
            var onError = function() {
                opt.apiUserDevices = [];
            };
            return ajaxPost('getuserdevices', param, onSuccess, onFail, onError, isDefer);
        }


        function getAlertsApi(pageParam, isDefer) {
            var opt = data;
            var eventType = opt.searchInfo.type.value || '';
            var eventId = '';
            var deviceId = (opt.searchInfo.device && opt.searchInfo.device.deviceId) ? opt.searchInfo.device.deviceId : '';
            var channelId = (opt.searchInfo.camera && opt.searchInfo.camera.cameraId) ? opt.searchInfo.camera.cameraId : '';
            var fromDateUTC = kendo.toString(utils.localToUTC(opt.searchInfo.startDate), data.timeFormat[2]);
            var toDateUTC = kendo.toString(utils.localToUTC(opt.searchInfo.endDate), data.timeFormat[2]);
            var param = {
                "skip": pageParam.skip,
                "take": pageParam.take,

                "from": fromDateUTC,
                "to": toDateUTC,
                "device-id": deviceId,
                "channel-id": channelId,
                "event-id": eventId,
                "event-type": eventType
            };

            var onSuccess = function(response) {
                opt.apiAlerts = response.alerts || [];
                opt.apiAlertsTotal = response.totalcount || 0;
            };
            var onFail = function(response) {
                opt.apiAlerts = [];
                opt.apiAlertsTotal = opt.apiAlertsTotal || 0;
            };
            var onError = function() {
                opt.apiAlerts = [];
                opt.apiAlertsTotal = opt.apiAlertsTotal || 0;
            };
            return ajaxPost('getalerts', param, onSuccess, onFail, onError, isDefer);
        }

        function exportAlertsApi(format, isDefer) {
            format = format || '';
            var opt = data;
            var eventType = opt.searchInfo.type.value || '';
            var deviceId = (opt.searchInfo.device && opt.searchInfo.device.deviceId) ? opt.searchInfo.device.deviceId : '';
            var channelId = (opt.searchInfo.camera && opt.searchInfo.camera.cameraId) ? opt.searchInfo.camera.cameraId : '';
            var fromDateUTC = kendo.toString(utils.localToUTC(opt.searchInfo.startDate), data.timeFormat[2]);
            var toDateUTC = kendo.toString(utils.localToUTC(opt.searchInfo.endDate), data.timeFormat[2]);
            var param = {
                "file-format": format,
                "time-zone-offset": UtilsService.getTimezoneOffset(),
                "device-id": deviceId,
                "channel-id": channelId,
                "event-type": eventType,
                "from": fromDateUTC,
                "to": toDateUTC,

            };
            return KupApiService.exportDoc(param, 'exportalerts');
        }

        function getLabelsApi(isDefer) {
            var opt = data;
            var param = {};
            var onSuccess = function(response) {
                opt.apiLabels = response.labels || [];
            };
            var onFail = function(response) {
                opt.apiLabels = [];
            };
            var onError = function() {
                opt.apiLabels = [];
            };
            return ajaxPost('getlabels', param, onSuccess, onFail, onError, isDefer);
        }
    })

angular
    .module('kai.notification')
    .controller('NotificationModalController',
        function(
            KupOption,
            RouterStateService, UtilsService, PromiseFactory, AuthTokenFactory,
            NotificationService, NotificationModalService,
            $scope, $rootScope, $timeout, $uibModalInstance, selectTableData
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var notificationModalCtrl = this;

            notificationModalCtrl.data = NotificationModalService.data;
            notificationModalCtrl.selectTableData = selectTableData;

            notificationModalCtrl.fn = {
                closeModal: closeModal,
                cancelModal: cancelModal,
                getEventInfoByType: getEventInfoByType,
                downloadVideo: downloadVideo,
            };
            init();

            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function init() {
                setWatch();
            }

            function setWatch() {
                $scope.$watch('notificationModalCtrl.data.isOpenModal', function(newVal, oldVal) {
                    var opt = notificationModalCtrl.data;
                    if (newVal) {
                        opt.isLoadVideo = false;
                        setPlayer('kupPlayer');
                        $timeout(function(){
                            opt.isLoadVideo = true;
                        },500);             
                    }
                }, true);
            }

            function setPlayer(playerId) {
                var opt = notificationModalCtrl.data;
                if (window.jwplayer) {
                    window.jwplayer.key = kupOpt.jwplayerKey;
                }
                var url = selectTableData.eventVideoUrl;
                var videoUrl = kupOpt.sysApiRootUrl + url;
                var playLink = videoUrl + "?action=play";
                if (url) {        
                    window.jwplayer(playerId).setup({
                        file: playLink,
                        width: "100%",
                        height: "100%",
                        autostart: false,
                        mute: true,
                        primary: utils.detectBrowser() == "firefox" ? "flash" : "html5",
                        flashplayer: kupOpt.jwplayerFlashPlayerUrl,
                        html5player: kupOpt.jwplayerHtml5PlayerUrl
                    });
                }
            }

            function closeModal() {
                $uibModalInstance.close();
            }

            function cancelModal() {
                $uibModalInstance.dismiss('cancel');
            }

            function getEventInfoByType(eventType) {
                return NotificationService.getEventInfoByType(eventType);
            }

            function downloadVideo() {
                var url = selectTableData.eventVideoUrl;
                var downloadUrl = url + "?action=download&customName=" + kendo.toString(new Date(selectTableData.timeMillis), kupOpt.kendoTimeFormat[3]) + "_" + selectTableData.eventType+ "_" + selectTableData.deviceId+ "_" + selectTableData.channelId + ".mp4";
                if (url) {
                    UtilsService.urlDownload(downloadUrl);
                }
            }
        });

angular
    .module('kai.notification')
    .controller('NotificationController', function(
        KupOption, RouterStateService, UtilsService, AuthTokenFactory,
        NotificationService, NotificationModalService,
        DTOptionsBuilder, DTColumnBuilder,
        $scope, $q, $timeout, $uibModal, $rootScope
    ) {
        var kupOpt = KupOption;
        var utils = UtilsService;
        var i18n = UtilsService.i18n;
        var notification = UtilsService.notification;
        var block = UtilsService.block;
        var mainCtrl = $scope.$parent.mainCtrl;
        var notificationCtrl = this;

        //UI controller
        notificationCtrl.data = NotificationService.data;
        notificationCtrl.data.uiTable = {
            dtInstance: {},
            options: DTOptionsBuilder.newOptions()
                //common setting
                .withLanguage(UtilsService.getAngularDatatablesI18n())
                .withPaginationType('full_numbers')
                .withOption('paging', true)
                .withOption('info', true)
                .withOption('responsive', true)
                .withOption('ordering', false)
                .withOption('processing', true)
                .withDisplayLength(kupOpt.datatables.displayLength)
                .withOption('fnRowCallback', function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
                    $('td', nRow).bind('click', function() {
                        var $tbody = $(this).parents('tbody');
                        var index = $tbody.find('tr').index($(this).parent('tr'));

                        //selected row to active
                        $tbody.find('tr').removeClass('kupActive');
                        $(this).parent('tr').addClass('kupActive');
                        $scope.$apply(function() {
                            showDialog(index);
                        });
                    });
                    return nRow;
                })

            //server side pagination setting
                .withOption('serverSide', true)
                .withDataProp('data')
                .withFnServerData(function(sSource, aoData, fnCallback, oSettings) {
                    //All the parameters you need is in the aoData variable
                    var draw = aoData[0].value;
                    var order = aoData[2].value;
                    var start = aoData[3].value;
                    var length = aoData[4].value;

                    var pageParam = {
                        skip: start,
                        take: length,
                    };
                    oSettings.jqXHR = NotificationService.getAlertsApi(pageParam)
                        .finally(function() {
                            setTableData();
                            var opt = notificationCtrl.data;
                            var records = {
                                "draw": draw,
                                "recordsTotal": opt.apiAlertsTotal,
                                "recordsFiltered": opt.apiAlertsTotal,
                                "data": opt.tableDataSource
                            }
                            fnCallback(records);
                        });
                }),
            columnDefs: [
                DTColumnBuilder.newColumn('timeLocal').withTitle(i18n('date-time')),
                DTColumnBuilder.newColumn('alertTypeText').withTitle(i18n('device-label')),
                DTColumnBuilder.newColumn('eventTypeText').withTitle(i18n('event-type')),
                DTColumnBuilder.newColumn('location').withTitle(i18n('happened-at'))
            ]
        };
        notificationCtrl.fn = {
            exportReport: exportReport,
            generateReport: generateReport,
            clearSearch: clearSearch,

            addLimit: addLimit,
            subLimit: subLimit,
        };

        init();
        /*******************************************************************************
         *
         *  Init Function Definition
         *
         *******************************************************************************/
        function init() {
            mainCtrl.block.promise = setUI();
            setWatch();
        }

        function setWatch() {
            $scope.$watch('notificationCtrl.data.searchInfo', function(newVal, oldVal) {
                if (angular.toJson(newVal) !== angular.toJson(oldVal)) {
                    generateReport();
                }
            }, true);
        }

        function setUI() {
            var opt = notificationCtrl.data;
            var requestAll = [
                NotificationService.getUserDevicesApi(true),
                NotificationService.getLabelsApi(true)
            ];
            setDateRangePicker();
            return $q.all(requestAll).finally(function() {
                setSearchData();
            });

        }

        /*******************************************************************************
         *
         *  Search UI and Report Function Definition
         *
         *******************************************************************************/

        function generateReport() {
            var opt = notificationCtrl.data;
            opt.uiTable.dtInstance.DataTable.ajax.reload();
        }

        function exportReport(format) {
            var opt = notificationCtrl.data;
            var warningNotify = notification('warning', i18n('exporting-to-' + format), 0);

            NotificationService.exportAlertsApi(format)
                .finally(function() {
                    warningNotify.close();
                    if (opt.apiExportAlerts) {
                        UtilsService.urlDownload(opt.apiExportAlerts);
                    }
                });
        }

        function setTableData() {
            NotificationService.setTableData();
        }

        function clearSearch() {
            NotificationService.initSearchData();
        }

        function setSearchData() {
            NotificationService.setSearchData();
        }

        function setDateRangePicker() {
            var opt = notificationCtrl.data;
            var dateFormat = opt.dateFormat;
            var startDateFormat = opt.startDateFormat;
            var endDateFormat = opt.endDateFormat;

            var defaultDate = {
                startDate: moment(opt.searchInfo.startDate),
                endDate: moment(opt.searchInfo.endDate)
            };
            var showDate = function(start, end) {
                $scope.$apply(function() {
                    var opt = notificationCtrl.data;
                    opt.searchInfo.startDate = new Date(start.format(dateFormat));
                    opt.searchInfo.endDate = new Date(end.format(dateFormat));
                    $(opt.$daterange).find('span').html(start.format(dateFormat) + ' - ' + end.format(dateFormat));
                });
            };

            var rangesList = opt.uiDateRangesList;
            var ranges = (function() {
                var obj = {};
                $.each(rangesList, function(i, range) {
                    obj[range.name] = range.data;
                });
                return obj;
            })();
            var locale = {
                customRangeLabel: i18n('custom-range')
            };
            var pickerData = $(opt.$daterange).daterangepicker({
                showDropdowns: true,
                //showWeekNumbers: true,
                timePicker: true,
                timePickerIncrement: 59,
                timePicker24Hour: true,
                timePickerSeconds: false,
                opens: 'left',
                drops: 'down',
                buttonClasses: ['btn', 'btn-sm'],
                applyClass: 'btn-warning',
                cancelClass: 'btn-link cancel',
                //separator: ' to ',
                ranges: ranges,
                locale: locale,
                startDate: defaultDate.startDate,
                endDate: defaultDate.endDate,
            }, showDate).data("daterangepicker");
            var customRangeBtn = pickerData.locale.customRangeLabel;


            $(opt.$daterange).find('span').html(pickerData.startDate.format(startDateFormat) + ' - ' + pickerData.endDate.format(endDateFormat));
            $(".daterangepicker .ranges ul li:contains('" + customRangeBtn + "')").on('click', function(e) {
                var $calendarControl = $('.daterangepicker.dropdown-menu');
                if ($calendarControl.hasClass('show-calendar')) {
                    e.stopPropagation();
                    $calendarControl.removeClass('show-calendar');
                }
            });
        };

        function addLimit() {
            var opt = notificationCtrl.data;
            opt.searchInfo.limit++;
        }

        function subLimit() {
            var opt = notificationCtrl.data;
            if (opt.searchInfo.limit > 0) {
                opt.searchInfo.limit--;
            }
        }

        function showDialog(index) {
            var opt = notificationCtrl.data;
            var optModal = NotificationModalService.data;
            var modalInstance = $uibModal.open({
                animation: true,
                templateUrl: 'notificationModal.tmpl.html',
                controller: 'NotificationModalController',
                controllerAs: 'notificationModalCtrl',
                windowClass: 'modal kupModal',
                size: '',
                resolve: {
                    selectTableData: function() {
                        return opt.tableDataSource[index];
                    }
                }
            });

            modalInstance.opened.then(
                //open fn
                function() {
                    optModal.isOpenModal = true;
                });

            modalInstance.result.then(
                //close fn
                function() {
                    optModal.isOpenModal = false;
                },
                //cancel fn
                function() {
                    optModal.isOpenModal = false;
                });
        }
    });
