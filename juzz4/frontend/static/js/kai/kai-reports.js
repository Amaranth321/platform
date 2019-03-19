angular.module('kai.reports',[
    'kendo.directives',
]);
angular
    .module('kai.reports')
    .factory("ReportsService",
        function(
            KupOption,
            UtilsService, PromiseFactory, AuthTokenFactory, KupApiService, DeviceTreeService,
            $http
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var ajaxPost = KupApiService.ajaxPost;

            var data = {
                //UI selector
                $section: '#reportsSection',
                $treeview: '#reportsTreeview',
                $search: '#reportsSearch',
                $dragTarget: '#reportsDragTarget',
                $dropHere: '#reportsDropHere',
                $daterange: '#kupDaterange',
                $applyBtn: '#applyBtn',
                $cancelBtn: '#cancelBtn',

                reportType: '', //current report page
                //for check status
                isSuccessDrag: false,
                isDoneReport: true, //true is finish generate reports, whether success or error
                isSuccessReport: false, //true is success generate reports 
                isDefaultDropData: false, //set drap & drop UI
                isUpdateSelectedItem: false, //check is show apply/cancel btn

                //dateRangePicker selectded
                dateFormat: 'ddMMyyyyHHmmss',
                dateRange: {
                    days: 7, //default date range
                    startDate: '',
                    endDate: '',
                },

                //default item info
                defaultItemDataList: {},
                defaultDateRange: {
                    startDate: '',
                    endDate: ''
                },

                //is draging item info
                draggedItemDataList: {},

                //for save selected item info
                selectedItemDataList: {},
                selectedSaveDataList: [],
                selectedGroupNames: [],
                selectedInstance: [],
                selectedDevices: [],

                //from api reponse data
                apiPosNamesList: [],
                apiRunningAnalyticsList: [],
                apiQueryHistoryData: {},

                //other info 
                deviceTreeData: [],
                uiDateRangesList: [{
                    name: i18n('today'),
                    data: [
                        moment(),
                        moment()
                    ]
                }, {
                    name: i18n('yesterday'),
                    data: [
                        moment().subtract(1, 'days'),
                        moment().subtract(1, 'days')
                    ]
                }, {
                    name: i18n('last-7-days'),
                    data: [
                        moment().subtract(6, 'days'),
                        moment()
                    ]
                }, {
                    name: i18n('last-30-days'),
                    data: [
                        moment().subtract(29, 'days'),
                        moment()
                    ]
                }, {
                    name: i18n('this-month'),
                    data: [
                        moment().startOf('month'),
                        moment().endOf('month')
                    ]
                }, {
                    name: i18n('last-month'),
                    data: [
                        moment().subtract(1, 'month').startOf('month'),
                        moment().subtract(1, 'month').endOf('month')
                    ]
                }],
            };
            var menuData = {
                isShow: true,
            };

            var searchData = {
                value: '',
            };

            var optionsData = {
                isOpen: false,
                isOnlyShowVca: true,
            };

            return {
                data: data,
                menuData: menuData,
                searchData: searchData,
                optionsData: optionsData,

                setDeviceTreeData: setDeviceTreeData,

                //set select data
                setSelectedInstance: setSelectedInstance,
                setSelectedDevices: setSelectedDevices,
                setSelectedSaveDataList: setSelectedSaveDataList,
                setSelectedGroupNames: setSelectedGroupNames,

                //set default data
                setDefaultItemDataList: setDefaultItemDataList,
                //setDefaultElementList: setDefaultElementList,
                setDefaultDateRange: setDefaultDateRange,

                //get data info
                getDeviceTreeData: getDeviceTreeData,
                getSelectedItemsLength: getSelectedItemsLength,

                //set api 
                setListPosNamesApi: setListPosNamesApi,
                setListRunningAnalyticsApi: setListRunningAnalyticsApi,
                setSaveReportQueryHistoryApi: setSaveReportQueryHistoryApi,
                setGetReportQueryHistoryApi: setGetReportQueryHistoryApi,

                //init api 
                initListPosNamesApi: initListPosNamesApi,
                initListRunningAnalyticsApi: initListRunningAnalyticsApi,
                initSaveReportQueryHistoryApi: initSaveReportQueryHistoryApi,
                initGetReportQueryHistoryApi: initGetReportQueryHistoryApi,

                //exec api 
                execSaveReportQueryHistoryApi: execSaveReportQueryHistoryApi,
                execGetReportQueryHistoryApi: execGetReportQueryHistoryApi,

                //check data
                isSelectCamera: isSelectCamera,
                isSuccessReport: isSuccessReport,
                isSingleCamera: isSingleCamera,
                isSelectItem: isSelectItem,
                isUpdateSelectedItem: isUpdateSelectedItem,
                isShowNoDataGuide: isShowNoDataGuide,
            };


            function getSelectedItemsLength() {
                var opt = data,
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


            function getDeviceTreeData() {
                var opt = data;
                return DeviceTreeService.initDeviceTree().finally(function() {
                    setDeviceTreeData();
                });
            }

            function setDeviceTreeData() {
                var opt = data;
                var treeItems = DeviceTreeService.getDeviceTree();

                opt.deviceTreeData = (function() {
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

            function setSelectedInstance() {
                var opt = data,
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
                return opt.selectedInstance;
            }

            function setSelectedDevices() {
                var opt = data;
                opt.selectedDevices = [];

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
                    opt.selectedDevices.push({
                        "groupName": groupname,
                        "devicePairs": devices,
                        "type": type
                    });

                });
                return opt.selectedDevices;
            }

            function setSelectedSaveDataList() {
                var opt = data,
                    selectedItemDataList = opt.selectedItemDataList;

                opt.selectedSaveDataList = [];
                $.each(selectedItemDataList, function(uid, itemData) {
                    if ($.isEmptyObject(itemData)) {
                        return true;
                    }
                    if (itemData.isAll || itemData.isLabel) { //drag root or labels
                        opt.selectedSaveDataList.push({
                            label: itemData.labelName,
                            deviceId: null,
                            channelId: null
                        });
                    } else if (itemData.isDevice) { //drag device
                        opt.selectedSaveDataList.push({
                            label: itemData.labelName,
                            deviceId: itemData.deviceId,
                            channelId: null
                        });
                    } else if (itemData.isCamera) { //drag camera
                        if (itemData.hasChildren) {
                            return;
                        }
                        opt.selectedSaveDataList.push({
                            label: itemData.labelName,
                            deviceId: itemData.deviceId,
                            channelId: itemData.cameraId
                        });
                    }
                });
                return opt.selectedSaveDataList;
            }

            function setSelectedGroupNames() {
                var opt = data,
                    selectedItemDataList = opt.selectedItemDataList,
                    groupNames = [];

                $.each(selectedItemDataList, function(uid, itemData) {
                    if ($.isEmptyObject(itemData)) {
                        return true;
                    }
                    if (!itemData.isLabel) {
                        //groupNames = [];
                        return true;
                    }
                    groupNames.push(itemData.text);
                });
                opt.selectedGroupNames = groupNames;
                return opt.selectedGroupNames;
            }

            function setListPosNamesApi(onSuccess, onFail, onError) {
                var opt = data;
                var param = {
                    "session-key": AuthTokenFactory.getSessionKey(),
                    "parser-type": '',
                };
                return ajaxPost('listposnames', param, onSuccess, onFail, onError);
            }

            function setListRunningAnalyticsApi(onSuccess, onFail, onError) {
                var opt = data;
                var analyticsType = kupOpt.vca[data.reportType].analyticsType;
                var param = {
                    "session-key": AuthTokenFactory.getSessionKey(),
                    "analytics-type": analyticsType,
                };
                return ajaxPost('listrunninganalytics', param, onSuccess, onFail, onError);
            }

            function setSaveReportQueryHistoryApi(onSuccess, onFail, onError) {
                var opt = data;
                var vcaEventType = kupOpt.vca[data.reportType].eventType;
                var param = {
                    "session-key": AuthTokenFactory.getSessionKey(),
                    "event-type": vcaEventType,
                    "date-from": kendo.toString(utils.convertToUTC(opt.dateRange.startDate), opt.dateFormat),
                    "date-to": kendo.toString(utils.convertToUTC(opt.dateRange.endDate), opt.dateFormat),
                    "device-selected": JSON.stringify(opt.selectedSaveDataList),
                };
                return ajaxPost('savereportqueryhistory', param, onSuccess, onFail, onError);
            }

            function setGetReportQueryHistoryApi(onSuccess, onFail, onError) {
                var opt = data;
                var vcaEventType = kupOpt.vca[data.reportType].eventType;
                var param = {
                    "session-key": AuthTokenFactory.getSessionKey(),
                    "event-type": vcaEventType,
                };
                return ajaxPost('getreportqueryhistory', param, onSuccess, onFail, onError);
            }


            function setDefaultItemDataList() {
                var opt = data,
                    itemDataQuery = opt.apiQueryHistoryData.deviceSelected || [],
                    itemData = $(opt.$treeview).data('kendoTreeView').dataSource.data() || [];
                opt.defaultItemDataList = {};
                $.each(itemDataQuery, function(i, queryData) {
                    if (queryData.label !== "" && queryData.deviceId !== "" && queryData.channelId !== "") { // is camera
                        $.each(itemData, function(j, labelData) {
                            if (labelData.labelName === queryData.label) {
                                $.each(labelData.items, function(k, deviceData) {
                                    if (deviceData.deviceId === parseInt(queryData.deviceId, 10)) {
                                        var uid = "";
                                        $.each(deviceData.items, function(l, cameraData) {
                                            if (cameraData.cameraId === parseInt(queryData.channelId, 10)) {
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
                            if (labelData.labelName === queryData.label) {
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
                            if (labelData.labelName === queryData.label) {
                                var uid = labelData.uid;
                                opt.defaultItemDataList[uid] = labelData;
                                return false;
                            }
                        });
                    }
                });
                return opt.defaultItemDataList;
            }

            // function setDefaultElementList() {
            //     var opt = data,
            //         defaultItemDataList = opt.defaultItemDataList;

            //     opt.defaultElementList = {};
            //     $.each(defaultItemDataList, function(i, data) {
            //         var uid = data.uid;
            //         opt.defaultElementList[uid] = $("li[data-uid='" + uid + "']").get(0);
            //     });
            //     return opt.defaultElementList;
            // }

            function setDefaultDateRange() {
                var opt = data,
                    apiQueryHistoryData = opt.apiQueryHistoryData,
                    //dateRangePicker = $('#calendar-reservation').data('daterangepicker'),
                    startDateForDefaultRange = new Date(moment().subtract(opt.dateRange.days - 1, 'days').format('YYYY/MM/DD 00:00:00')),
                    endDateForDefaultRange = new Date(moment().format('YYYY/MM/DD 23:59:59'));

                opt.defaultDateRange.startDate = (function() {
                    var startDate = kendo.parseDate(apiQueryHistoryData.dateFrom || '', opt.dateFormat),
                        startDateToLocal = (startDate) ? utils.convertUTCtoLocal(startDate) : startDateForDefaultRange;
                    return startDateToLocal;
                })();
                opt.defaultDateRange.endDate = (function() {
                    var endDate = kendo.parseDate(apiQueryHistoryData.dateTo || '', opt.dateFormat),
                        endDateToLocal = (endDate) ? utils.convertUTCtoLocal(endDate) : endDateForDefaultRange;
                    return endDateToLocal;
                })();
                return opt.defaultDateRange;
            }

            function initListPosNamesApi() {
                var opt = data;
                var onSuccess = function(response) {
                    var names = response.names || [];
                    $.each(names, function(i, obj) {
                        opt.apiPosNamesList.push(obj.name);
                    });
                };
                var onFail = function(response) {};
                var onError = function() {};
                return setListPosNamesApi(onSuccess, onFail, onError);
            }

            function initListRunningAnalyticsApi() {
                var opt = data;
                var onSuccess = function(response) {
                    opt.apiRunningAnalyticsList = response.instances || [];
                };
                var onFail = function(response) {};
                var onError = function() {};
                return setListRunningAnalyticsApi(onSuccess, onFail, onError);
            }

            function initSaveReportQueryHistoryApi() {
                var opt = data;
                var onSuccess = function(response) {};
                var onFail = function(response) {};
                var onError = function() {};
                return setSaveReportQueryHistoryApi(onSuccess, onFail, onError);
            }

            function initGetReportQueryHistoryApi() {
                var opt = data;
                var onSuccess = function(response) {
                    opt.apiQueryHistoryData = response.query || {};
                };
                var onFail = function(response) {};
                var onError = function() {};
                return setGetReportQueryHistoryApi(onSuccess, onFail, onError);
            }

            function execSaveReportQueryHistoryApi() {
                var opt = data;
                var onSuccess = function(response) {};
                var onFail = function(response) {};
                var onError = function() {};
                return setSaveReportQueryHistoryApi(onSuccess, onFail, onError);
            }

            function execGetReportQueryHistoryApi() {
                var opt = data;
                var onSuccess = function(response) {
                    opt.apiQueryHistoryData = response.query || {};
                    setDefaultItemDataList();
                    //setDefaultElementList();
                    setDefaultDateRange();
                };
                var onFail = function(response) {};
                var onError = function() {};
                return setGetReportQueryHistoryApi(onSuccess, onFail, onError);
            }

            function isSelectCamera() {
                var opt = data;
                var selectedDeviceList = opt.selectedDevices;
                var selectedChannelList = opt.selectedDevices;
                var check = true;
                if (selectedDeviceList.length <= 0 && selectedChannelList.length <= 0) {
                    check = false;
                }
                return check;
            }

            function isSuccessReport(isSuccess) {
                var opt = data;
                var check = opt.isSuccessReport = (isSuccess === undefined) ? opt.isSuccessReport : isSuccess;
                return check;
            }

            function isSingleCamera() {
                var opt = data;
                var check = false;
                if (opt.selectedInstance.length === 1) {
                    check = true;
                }
                return check;
            }

            function isSelectItem() {
                var opt = data;
                var check = false;
                $.each(opt.selectedItemDataList, function(uid, dataList) {
                    if (!$.isEmptyObject(dataList)) {
                        check = true;
                        return false;
                    }
                });
                return check;
            }

            function isUpdateSelectedItem() {
                var opt = data;
                return opt.isUpdateSelectedItem;
            }

            function isShowNoDataGuide() {
                var opt = data;
                return opt.isDoneReport && isSelectItem();
            }
        });

angular
    .module('kai.reports')
    .controller('ReportsController',
        function(
            KupOption,
            RouterStateService, ReportsService, UtilsService, PromiseFactory, MainService, DeviceTreeService, AuthTokenFactory,
            $scope, $timeout, $q, $location, $animate
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var block = UtilsService.block;
            var isCurrentPage = RouterStateService.isCurrentPage;
            var mainCtrl = $scope.$parent.mainCtrl;
            var reportsCtrl = this;

            //reports all UI ctrl
            reportsCtrl.data = ReportsService.data;
            reportsCtrl.fn = {
                showMainMenu: MainService.showMainMenu,
                saveReportsInfo: saveReportsInfo,
                cancelReportsInfo: cancelReportsInfo,
                isEmptyDevice: DeviceTreeService.isEmptyDevice,
                isSelectItem: ReportsService.isSelectItem,
                isSuccessReport: ReportsService.isSuccessReport,
                isUpdateSelectedItem: ReportsService.isUpdateSelectedItem,
                isShowNoDataGuide: ReportsService.isShowNoDataGuide,
                getTheme: AuthTokenFactory.getTheme,
            };

            //reports content UI ctrl
            reportsCtrl.content = {};

            //reports menu UI ctrl
            reportsCtrl.menu = {};
            reportsCtrl.menu.data = ReportsService.menuData;

            //reports search UI ctrl
            reportsCtrl.search = {};
            reportsCtrl.search.data = ReportsService.searchData;

            //reports options UI ctrl
            reportsCtrl.options = {};
            reportsCtrl.options.data = ReportsService.optionsData;

            //reports treeview UI ctrl
            reportsCtrl.treeview = {};
            reportsCtrl.treeview.data = {};
            reportsCtrl.treeview.options = getTreeviewOptions();

            //reports dragTarget UI ctrl
            reportsCtrl.dragTarget = {};
            reportsCtrl.dragTarget.data = {};
            reportsCtrl.dragTarget.options = getDragTargetOptions();

            init();

            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function init() {
                //watch router
                $scope.$watch(function() {
                    return angular.toJson(RouterStateService.getRouterState());
                }, function(newValue, oldValue) {
                    var routerState = angular.fromJson(newValue).toState;
                    var routerCheck = /\.reports/.test(routerState.name);
                    var reportType = (routerCheck) ? /\.reports\.(\S*)/.exec(routerState.name)[1] : '';
                    if (!routerCheck) {
                        return false;
                    }
                    //when switch report, update report info
                    initOpt();
                    reportsCtrl.data.reportType = reportType;
                    reportsCtrl.content.title = reportType;
                    reportsCtrl.menu.data.isShow = true;
                    reportsCtrl.menu.title = reportType;
                    reportsCtrl.menu.class = kupOpt.vca[reportType].class;
                    reportsCtrl.search.data.value = "";
                    reportsCtrl.options.data.isOpen = false;
                    reportsCtrl.options.data.isOnlyShowVca = true;

                    mainCtrl.block.promise = loadUI();
                }, true);

                //watch options UI
                $scope.$watch('reportsCtrl.options.data', function(newVal, oldVal) {
                    var opt = reportsCtrl.data;
                    if (newVal.isOnlyShowVca !== oldVal.isOnlyShowVca) {
                        setTreeviewForFilter();
                    }
                }, true);

                //watch search UI
                $scope.$watch('reportsCtrl.search.data', function(newVal, oldVal) {
                    var opt = reportsCtrl.data;
                    if (newVal.value !== oldVal.value) {
                        $timeout(function() {
                            setTreeviewForFilter();
                        }, 300);
                    }
                }, true);
            }

            function loadUI() {
                var dfd = $q.defer();
                $timeout(function() {
                    ReportsService.getDeviceTreeData()
                        .finally(function() {
                            setUI();
                            loadReportInfo()
                                .finally(function() {
                                    dfd.resolve();
                                });
                        })
                });
                return dfd.promise;
            }

            function loadReportInfo() {
                var pageUrl = $location.path();
                return $q.all([
                        ReportsService.initListPosNamesApi(),
                        ReportsService.initGetReportQueryHistoryApi()
                    ])
                    .then(
                        //on success
                        function(data) {
                            var opt = reportsCtrl.data;
                            ReportsService.setDefaultItemDataList();
                            ReportsService.setDefaultDateRange();
                            updateUI();

                            if (opt.selectedInstance.length > 0) {
                                generateReport();
                            }
                        },
                        //on error
                        function(data) {
                            if (isCurrentPage(pageUrl)) {
                                notification('error', i18n("server-error"));
                            }
                        })
                    .finally(function() {
                        if (isCurrentPage(pageUrl)) {
                            setTreeviewForFilter();
                        }
                    });
            }

            function setUI() {
                setTreeview();
                setDateRangePicker();
            }

            function updateUI() {
                updateDateRangePicker();
                updateRemoveDropPlace();
                updateDefaultDropPlace();
                defaultExpandTreeview();
            }

            function initOpt() {
                var opt = reportsCtrl.data;

                opt.isDoneReport = true;
                opt.isSuccessReport = false;
                opt.isDefaultDropData = true;
                opt.isUpdateSelectedItem = false;

                opt.draggedItemDataList = {};
                opt.selectedItemDataList = {};
            }

            function setTreeview() {
                var opt = reportsCtrl.data;
                var treeview = $(opt.$treeview).data("kendoTreeView");
                //when switch report page,must to filter {},before reset the data
                treeview.dataSource.filter({});
                treeview.dataSource.data(opt.deviceTreeData);
            }

            function setDateRangePicker() {
                var opt = reportsCtrl.data;
                var days = opt.dateRange.days;
                var showDefaultDate = function(start, end) {
                    $scope.$apply(function() {
                        var opt = reportsCtrl.data;
                        opt.dateRange.startDate = new Date(start.format('YYYY/MM/DD HH:mm:ss'));
                        opt.dateRange.endDate = new Date(end.format('YYYY/MM/DD HH:mm:ss'));
                        $(opt.$daterange).find('span').text(moment(start).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly) + ' - ' + moment(end).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly));
                    });
                };
                var ranges = (function() {
                    var obj = {};
                    $.each(opt.uiDateRangesList, function(i, range) {
                        obj[range.name] = range.data;
                    });
                    return obj;
                })();
                var pickerData = $(opt.$daterange).daterangepicker({
                    showDropdowns: true,
                    // showWeekNumbers: true,
                    // timePicker: true,
                    // timePickerIncrement: 60,
                    // timePicker12Hour: true,
                    // timePickerSeconds: false,
                    opens: 'left',
                    drops: 'down',
                    buttonClasses: ['btn', 'btn-sm'],
                    applyClass: 'btn-warning',
                    cancelClass: 'btn-link cancel',

                    //"minDate": moment(new Date(moment().subtract(1, 'days')),
                    "maxDate": moment(),

                    //separator: ' to ',
                    ranges: ranges,
                    startDate: moment().subtract(days - 1, 'days'),
                    endDate: moment()
                }, showDefaultDate).data("daterangepicker");
                var customRangeBtn = pickerData.locale.customRangeLabel;

                $(opt.$daterange).find('span').text(moment(pickerData.startDate).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly) + ' - ' + moment(pickerData.endDate).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly));
                $(".daterangepicker .ranges ul li:contains('" + customRangeBtn + "')").on('click', function(e) {
                    var $calendarControl = $('.daterangepicker.dropdown-menu');
                    if ($calendarControl.hasClass('show-calendar')) {
                        e.stopPropagation();
                        $calendarControl.removeClass('show-calendar');
                    }
                })
            };

            function updateDateRangePicker() {
                var opt = reportsCtrl.data,
                    startDate = opt.defaultDateRange.startDate,
                    endDate = opt.defaultDateRange.endDate,
                    $picker = $(opt.$daterange),
                    pickerData = $picker.data('daterangepicker');

                opt.dateRange.startDate = opt.defaultDateRange.startDate;
                opt.dateRange.endDate = opt.defaultDateRange.endDate;

                pickerData.setStartDate(startDate);
                pickerData.setEndDate(endDate);
                
                $(opt.$daterange).find('span').text(moment(pickerData.startDate).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly) + ' - ' + moment(pickerData.endDate).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly));
                $picker.off('apply.daterangepicker').on('apply.daterangepicker', function(event) {
                    $scope.$apply(function() {
                        event.preventDefault();
                        ReportsService.execSaveReportQueryHistoryApi();
                        generateReport();
                    });
                });
            }

            function updateRemoveDropPlace(uid) {
                var $selector = (uid) ? $('.kupDropItem[data-uid="' + uid + '"] > button') : $('.kupDropItem')
                $selector.remove();
            }

            function updateDefaultDropPlace() {
                var opt = reportsCtrl.data,
                    dataLength = Object.keys(opt.defaultItemDataList).length;
                $(reportsCtrl.data.$treeview).data('kendoTreeView').trigger('dragstart');
                $(reportsCtrl.data.$treeview).data('kendoTreeView').trigger('drag');
                $(reportsCtrl.data.$treeview).data('kendoTreeView').trigger('drop');
                $(reportsCtrl.data.$section).data('kendoDropTarget').trigger('drop');
                opt.isDefaultDropData = false;
            }

            function showSelectedDevice(e) {
                $(e.currentTarget).children('span:eq(0)').toggle();
                $(e.currentTarget).children('span:eq(1)').toggle();
            }

            function removeSelectedDevice(e) {
                e.preventDefault();
                var opt = reportsCtrl.data;
                var selectedDevice = $(e.currentTarget).parent('span');
                var deviceIds = selectedDevice.attr('deviceId').split(",");
                var channelId = selectedDevice.attr('channelId');
                var uid = selectedDevice.attr('data-uid');
                var setOption = (function() {
                        //set tree view
                        $(selectedDevice).remove();
                        removeDragActiveState(uid);

                        //clear object
                        opt.draggedItemDataList[uid] = {};
                        opt.selectedItemDataList[uid] = {};

                        opt.isUpdateSelectedItem = true;
                    })(),
                    setData = (function() {
                        ReportsService.setSelectedInstance();
                        ReportsService.setSelectedDevices();
                        ReportsService.setSelectedSaveDataList();
                        ReportsService.setSelectedGroupNames();
                    })(),
                    setUI = (function() {
                        showUpdateBtn();
                    })();
            }

            function saveReportsInfo() {
                ReportsService.execSaveReportQueryHistoryApi();
                generateReport();
            }

            function cancelReportsInfo() {
                var opt = reportsCtrl.data;
                opt.isDefaultDropData = true;
                return ReportsService.execGetReportQueryHistoryApi()
                    .success(function() {
                        updateDateRangePicker();
                        updateRemoveDropPlace();
                        updateDefaultDropPlace();
                        generateReport();
                    })
                    .error(function() {
                        notification('error', i18n('server-error'));
                    });
            }

            function getTreeviewOptions() {
                return {
                    dragAndDrop: true,
                    template: "# if(item.isOnline == false){ #" +
                        "#=item.text # <i class='fa fa-times-circle-o' uib-tooltip='" + i18n('offline') + "' tooltip-placement='right'></i>" +
                        "# } else { #" +
                        "#=item.text #" +
                        "# } #",
                    dataSource: reportsCtrl.data.deviceTreeData,
                    expand: OnKendoExpand,
                    dragstart: function(e) {
                        var opt = reportsCtrl.data;
                        var treeview = $(reportsCtrl.data.$treeview).data("kendoTreeView");
                        var path = $location.path();

                        $(opt.$dropHere).show();
                        opt.isSuccessDrag = true;
                        opt.draggedItemDataList = {};

                        if (opt.isDefaultDropData) {
                            opt.draggedItemDataList = opt.defaultItemDataList;
                        }
                        if (!opt.isDefaultDropData) {
                            var uid = treeview.dataItem(e.sourceNode).uid;
                            var draggedItem = treeview.dataItem(e.sourceNode);

                            if (draggedItem.isLabel && !draggedItem.items.length) {
                                notification('warning', "<a ui-sref='main.label' style='text-decoration: underline;'>" + i18n('select-empty-label-error-information') + "</a>", 5000);
                                opt.isSuccessDrag = false;
                                return false;
                            }
                            if (path === '/main/reports/attention' && ReportsService.isSelectItem()) {
                                notification('warning', i18n('multiple-device-not-supported'));
                                opt.isSuccessDrag = false;
                                return false;
                            }
                            if ((path === '/main/reports/traffic' || path === '/main/reports/crowd' || path === '/main/reports/face') && (opt.selectedDevices.length >= 1 || !treeview.dataItem(e.sourceNode).isCamera)) {
                                notification('warning', i18n('multiple-device-channel-not-supported'));
                                opt.isSuccessDrag = false;
                                return false;
                            }
                            opt.draggedItemDataList[uid] = draggedItem;
                        }
                        $.each(opt.draggedItemDataList, function(uid, dataList) {
                            if (checkDragActiveState(uid)) {
                                e.preventDefault();
                                return false;
                            }
                        });
                    },
                    drag: function(e) {
                        var opt = reportsCtrl.data;
                        if (!opt.isDefaultDropData) {
                            if (
                                ($(e.dropTarget).attr('id') === "reportsSection" || $(e.dropTarget).parents(opt.$section).attr('id') === 'reportsSection') &&
                                opt.isSuccessDrag
                            ) {
                                e.setStatusClass("k-add");
                            }
                        }
                    },
                    drop: function(e) {
                        e.preventDefault();
                        var opt = reportsCtrl.data;
                        $(opt.$dropHere).hide();
                        if (!opt.isDefaultDropData) {
                            if (
                                ($(e.dropTarget).attr('id') !== "reportsSection" && $(e.dropTarget).parents(opt.$section).attr('id') !== 'reportsSection') ||
                                !opt.isSuccessDrag
                            ) {
                                e.setValid(false);
                            }
                        }
                    }
                };
            }

            function getDragTargetOptions() {
                return {
                    drop: function(e) {
                        var opt = reportsCtrl.data;
                        if (!opt.isSuccessDrag) {
                            return false;
                        }
                        var treeview = $(opt.$treeview).data("kendoTreeView");
                        var setOption = (function() {
                            $.each(opt.draggedItemDataList, function(uid, draggedDevice) {
                                var deviceId = "",
                                    channelId = "",
                                    nameTag = "";
                                //set selected var
                                if (draggedDevice.isAll || draggedDevice.isLabel) {
                                    var len = draggedDevice.items.length;

                                    var deviceDragged = draggedDevice.items;
                                    $.each(deviceDragged, function(index, device) {
                                        if (index == len - 1) {
                                            deviceId += device.deviceId;
                                        } else {
                                            deviceId += device.deviceId + ",";
                                        }
                                    });
                                    nameTag = '<span style="display:none;"><span class="kupLabel"><i class="' + draggedDevice.labelClass + '"></i>' + draggedDevice.labelName + '</span></span>';
                                    nameTag += '<span><span class="kupLabel"><i class="' + draggedDevice.labelClass + '"></i>' + draggedDevice.text + '</span></span>';
                                }
                                if (draggedDevice.isDevice) {
                                    deviceId = draggedDevice.deviceId;
                                    nameTag = '<span style="display:none;"><span class="kupLabel"><i class="' + draggedDevice.labelClass + '"></i>' + draggedDevice.labelName + '</span><span class="kupDragLine">|</span><span class="kupNode"><i class="kup-node"></i>' + draggedDevice.text + '</span></span>';
                                    nameTag += '<span><span class="kupNode"><i class="kup-node"></i>' + draggedDevice.text + '</span></span>';
                                }
                                if (draggedDevice.isCamera) {
                                    if (draggedDevice.hasChildren) {
                                        return;
                                    }
                                    deviceId = draggedDevice.deviceId;
                                    channelId = draggedDevice.cameraId;
                                    nameTag = '<span style="display:none;"><span class="kupLabel"><i class="' + draggedDevice.labelClass + '"></i>' + draggedDevice.labelName + '</span><span class="kupDragLine">|</span><span class="kupNode"><i class="kup-node"></i>' + draggedDevice.deviceName + '</span><span class="kupDragLine">|</span><span class="kupCamera"><i class="fa fa-video-camera"></i>' + draggedDevice.text + '</span></span>';
                                    nameTag += '<span><span class="kupCamera"><i class="fa fa-video-camera"></i>' + draggedDevice.text + '</span></span>';
                                }

                                opt.selectedItemDataList[uid] = opt.draggedItemDataList[uid];
                                opt.isUpdateSelectedItem = true;
                                opt.isDoneReport = false;

                                //add drop place div
                                var htmlString = "<span class='kupDropItem' data-uid='" + uid + "' deviceId='" + deviceId + "' channelId='" + channelId + "'>" + nameTag + "<button class='btn btn-link'><i class='fa fa-times'></i></button></span>";
                                $(htmlString).insertBefore(reportsCtrl.data.$dropHere);
                            })
                        })();
                        var setData = (function() {
                            ReportsService.setSelectedInstance();
                            ReportsService.setSelectedDevices();
                            ReportsService.setSelectedSaveDataList();
                            ReportsService.setSelectedGroupNames();
                        })();
                        var setEvent = (function() {
                            //bind event & set isDrag item
                            $(opt.$dragTarget + ' > span.kupDropItem').off('click').on('click', function(e) {
                                showSelectedDevice(e);
                            });
                            $(opt.$dragTarget + ' button').off('click').on('click', function(e) {
                                removeSelectedDevice(e);
                            });
                            addDragActiveState();
                        })();
                        var setUI = (function() {
                            showUpdateBtn();
                        })();
                    }

                };
            }

            function generateReport() {
                var opt = reportsCtrl.data;
                var reportType = opt.reportType;
                var serviceName = kupOpt.vca[reportType].ngPreFix + "Service";
                var execGenerateReports = function() {
                    $timeout(function() {
                        if (!angular.element(document.getElementsByTagName("html")[0]).injector().has(serviceName)) {
                            execGenerateReports();
                            return false;
                        }
                        showUpdateBtn();
                        mainCtrl.block.promise = angular.element(document.getElementsByTagName("html")[0]).injector().get(serviceName).generateReport();
                    }, 300);
                };
                //set report status
                opt.isUpdateSelectedItem = false;
                opt.isSuccessReport = false;
                opt.isDoneReport = true;

                //exec generate report
                execGenerateReports();
            }

            function showUpdateBtn() {
                var opt = reportsCtrl.data;
                var selectedItemsLength = ReportsService.getSelectedItemsLength(),
                    isUpdateSelectedItem = opt.isUpdateSelectedItem;

                var $applyBtn = $(opt.$applyBtn),
                    $cancelBtn = $(opt.$cancelBtn);
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
            }

            function setTreeviewForFilter() {
                var opt = reportsCtrl.data;
                var optionsData = reportsCtrl.options.data;
                var isOnlyShowVca = optionsData.isOnlyShowVca;
                var filterSearchVal = $.trim(reportsCtrl.search.data.value);
                var treeview = $(opt.$treeview).data("kendoTreeView");
                var analyticsType = kupOpt.vca[opt.reportType].analyticsType;

                var filterVca = (!isOnlyShowVca) ? {} : {
                    field: "filterVca",
                    operator: "contains",
                    value: analyticsType
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
                                if (filterSearchVal === "") {}
                                if (filterSearchVal !== "") {
                                    if (filterSearchCheck.test($.trim(labelFilterText))) {
                                        uidExpandList.push(labelData.uid);
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
                                        if (filterSearchVal === "") {}
                                        if (filterSearchVal !== "") {
                                            if (filterSearchCheck.test($.trim(deviceFilterText))) {
                                                uidExpandList.push(deviceData.uid);
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

                    //add tree state
                    addDragActiveState();
                };

                $timeout(function() { execFilter() });
            }

            function checkDragActiveState(uid) {
                var opt = reportsCtrl.data;
                var check = false;
                if ($(opt.$treeview).find('[data-uid="' + uid + '"]').children('div').children('span.k-in').hasClass('kupDragActive')) {
                    check = true;
                }
                return check;
            }

            function addDragActiveState(uid) {
                var opt = reportsCtrl.data;
                if (uid) {
                    $(opt.$treeview).find('[data-uid="' + uid + '"]').children('div').children('span.k-in').addClass('kupDragActive');
                }
                if (!uid) {
                    $.each(opt.draggedItemDataList, function(uid, dataList) {
                        if (!$.isEmptyObject(dataList)) {
                            $(opt.$treeview).find('[data-uid="' + uid + '"]').children('div').children('span.k-in').addClass('kupDragActive');
                        }
                    });
                }
            }

            function removeDragActiveState(uid) {
                var opt = reportsCtrl.data;
                if (uid) {
                    $(opt.$treeview).find('[data-uid="' + uid + '"]').children('div').children('span.k-in').removeClass('kupDragActive');
                }
                if (!uid) {
                    $.each(opt.draggedItemDataList, function(uid, dataList) {
                        if (!$.isEmptyObject(dataList)) {
                            $(opt.$treeview).find('[data-uid="' + uid + '"]').children('div').children('span.k-in').removeClass('kupDragActive');
                        }
                    });
                }
            }

            function defaultExpandTreeview() {
                var opt = reportsCtrl.data;
                var treeview = $(opt.$treeview).data("kendoTreeView");
                var expandUidList = [];
                $.each(opt.defaultItemDataList, function(uid, dataList) {
                    if (!$.isEmptyObject(dataList)) {
                        var parent = treeview.parent(treeview.findByUid(uid));
                        while (parent.length > 0) {
                            var parentUid = parent.attr('data-uid');
                            expandUidList.push(parentUid);
                            parent = treeview.parent(treeview.findByUid(parentUid));
                        }
                    }
                });
                $.each(expandUidList, function(i, uid) {
                    treeview.expand('[data-uid="' + uid + '"]');
                });
            }

            function OnKendoExpand(e) {
                // var opt = reportsCtrl.data;
                // var treeview = $(opt.$treeview).data("kendoTreeView");
                // if (treeview.collapsingOthers) {
                //     treeview.collapsingOthers = false;
                // } else {
                //     treeview.collapse('.k-item');
                //     treeview.collapsingOthers = true;
                //     treeview.expand(e.node);
                // }
            }
        });
