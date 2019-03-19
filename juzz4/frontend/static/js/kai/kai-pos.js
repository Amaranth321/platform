angular.module('kai.pos', [
    'datatables'
]);

angular.module('kai.pos')
    .factory('PosService', function(
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
        data.dateRange = 1;
        data.dateFormat = kupOpt.momentTimeFormat[0];
        data.startDateFormat = kupOpt.momentTimeFormatForStart[0];
        data.endDateFormat = kupOpt.momentTimeFormatForEnd[0];

        data.searchInfo = {
            label: {},
            startDate: new Date(moment(moment().format(data.startDateFormat)).subtract(data.dateRange - 1, 'days')),
            endDate: new Date(moment(moment().format(data.endDateFormat)))
        };
        data.ftpInfo = {};
        data.reportList = [];
        data.reportInputList = {};
        data.requestList = [];

        //api response data
        data.apiGetLabels = [];
        data.apiGetPosSalesReport = [];
        data.apiGetPosSettings = {};

        //UI selector
        data.$daterange = '#kupDaterange';
        //UI setting
        data.uiTab = [{
            name: 'automated',
            text: 'automated',
            isActive: true,
            class: "fa-repeat"
        }, {
            name: 'manual',
            text: 'manual',
            isActive: false,
            class: "fa-hand-o-up"
        }];
        data.uiLabel = [];
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
                name: i18n('last-2-days'),
                data: [
                    moment(new Date(moment().subtract(2, 'days').format(data.startDateFormat))),
                    moment(new Date(moment().format(data.endDateFormat)))
                ]
            }
            // , {
            //     name: i18n('last-7-days'),
            //     data: [
            //         moment(new Date(moment().subtract(6, 'days').format(data.startDateFormat))),
            //         moment(new Date(moment().format(data.endDateFormat)))
            //     ]
            // }, {
            //     name: i18n('last-30-days'),
            //     data: [
            //         moment(new Date(moment().subtract(29, 'days').format(data.startDateFormat))),
            //         moment(new Date(moment().format(data.endDateFormat)))
            //     ]
            // }, {
            //     name: i18n('this-month'),
            //     data: [
            //         moment(new Date(moment().startOf('month').format(data.startDateFormat))),
            //         moment(new Date(moment().endOf('month').format(data.endDateFormat)))
            //     ]
            // }, {
            //     name: i18n('last-month'),
            //     data: [
            //         moment(new Date(moment().subtract(1, 'month').startOf('month').format(data.startDateFormat))),
            //         moment(new Date(moment().subtract(1, 'month').endOf('month').format(data.endDateFormat)))
            //     ]
            // }
        ];
        data.uiTable = {
            options: DTOptionsBuilder.newOptions()
                //common setting
                .withLanguage(UtilsService.getAngularDatatablesI18n())
                .withPaginationType('full_numbers')
                .withOption('paging', true)
                .withOption('info', true)
                .withOption('responsive', false)
                .withOption('ordering', false)
                .withOption('processing', true)
                .withDisplayLength(25),

            columnDefs: [
                DTColumnDefBuilder.newColumnDef(0),
                DTColumnDefBuilder.newColumnDef(1),
                DTColumnDefBuilder.newColumnDef(2)
            ]
        };

        return {
            data: data,
            setFtpData: setFtpData,
            setLabelData: setLabelData,
            setReportData: setReportData,
            getLabelsApi: getLabelsApi,
            getPosSettingsApi: getPosSettingsApi,
            getPosSalesReportApi: getPosSalesReportApi,
            updatePosSalesDataApi: updatePosSalesDataApi,
            updatePosSettingsApi: updatePosSettingsApi,
            //generateReport: generateReport,
            switchTab: switchTab,
        };
        /*******************************************************************************
         *
         *  Function Definition
         *
         *******************************************************************************/
        function setFtpData() {
            var opt = data;
            opt.ftpInfo = angular.copy(opt.apiGetPosSettings.ftpDetails);
            opt.ftpInfo.enabled = opt.apiGetPosSettings.enabled || false;
        }

        function setLabelData() {
            var opt = data;
            var labelList = angular.copy(opt.apiGetLabels);
            var labelList = (function() {
                var labelList = angular.copy(opt.apiGetLabels);
                $.each(labelList, function(i, label) {
                    label.class = "";
                    $.each(kupOpt.label, function(j, labelOpt) {
                        if (label.type === labelOpt.value) {
                            label.class = labelOpt.class;
                            return false;
                        }
                    });
                });
                return labelList;
            })();
            opt.uiLabel = labelList;
            opt.searchInfo.label = labelList[0] || {};
        }

        function setReportData() {
            var opt = data;
            var apiReporeList = angular.copy(opt.apiGetPosSalesReport);
            var dateRangeList = (function() {
                var acc = [];
                moment.range(opt.searchInfo.startDate, new Date(moment(moment(opt.searchInfo.endDate).format(opt.dateFormat)).add(1, 'h'))).by('h', function(moment) {
                    acc.push(new Date(moment));
                }, true);
                return acc;
            })();

            var reportList = [];
            var reportInputList = {};

            //set reportList
            (function() {
                $.each(dateRangeList, function(i, time) {
                    var isPush = true;
                    $.each(apiReporeList, function(j, report) {
                        if (time.getTime() === report.sales.time.getTime()) {
                            reportList.push({
                                time: time,
                                amount: report.sales.amount,
                                count: report.sales.count
                            });
                            isPush = false;
                            return false;
                        }

                    });
                    if (isPush) {
                        reportList.push({
                            time: time,
                            amount: 0,
                            count: 0
                        });
                    }

                });
            })();

            //set reportInputList
            (function() {
                $.each(reportList, function(i, report) {
                    reportInputList[report.time] = {
                        amount: report.amount,
                        count: report.count,
                        amountId: 'posReportAmount' + report.time.getTime(),
                        countId: 'posReportCount' + report.time.getTime(),
                        isShowCount: false,
                        isShowAmount: false,

                    }
                });
            })();
            //console.info(reportList);
            opt.reportList = reportList;
            opt.reportInputList = reportInputList;
        }

        function getLabelsApi(isDefer) {
            var opt = data;
            var param = {};

            var onSuccess = function(response) {
                opt.apiGetLabels = $filter('orderBy')(response.labels, "+name") || [];
            };
            var onFail = function(response) {
                opt.apiGetLabels = [];
            };
            var onError = function() {
                opt.apiGetLabels = [];
            };
            return ajaxPost('getlabels', param, onSuccess, onFail, onError, isDefer);
        }

        function getPosSalesReportApi(isDefer) {
            var opt = data;
            var labelName = opt.searchInfo.label.name || "";
            var fromDateUTC = kendo.toString(utils.localToUTC(opt.searchInfo.startDate), opt.timeFormat[2]);
            var toDateUTC = kendo.toString(utils.localToUTC(opt.searchInfo.endDate), opt.timeFormat[2]);
            var param = {
                'from': fromDateUTC,
                'to': toDateUTC,
                'name': labelName,
                'parser-type': '',
            };

            var onSuccess = function(response) {
                $.each(response.sales || [], function(i, report) {
                    response.sales[i].sales.time = UtilsService.UTCToLocal(kendo.parseDate(report.sales.time, opt.timeFormat[1]));
                });
                opt.apiGetPosSalesReport = response.sales || [];
            };
            var onFail = function(response) {
                opt.apiGetPosSalesReport = [];
            };
            var onError = function() {
                opt.apiGetPosSalesReport = [];
            };
            return ajaxPost('getpossalesreport', param, onSuccess, onFail, onError, isDefer);
        }

        function updatePosSalesDataApi(isDefer) {
            var opt = data;
            var fromDateUTC = kendo.toString(utils.localToUTC(opt.searchInfo.startDate), opt.timeFormat[2]);
            var toDateUTC = kendo.toString(utils.localToUTC(opt.searchInfo.endDate), opt.timeFormat[2]);
            var posName = opt.searchInfo.label.name || "";
            var posData = (function() {
                var reportList = opt.reportInputList;
                var posData = [];
                $.each(reportList, function(time, report) {
                    posData.push({
                        time: kendo.toString(utils.localToUTC(time), opt.timeFormat[2]),
                        count: report.count,
                        amount: report.amount
                    });
                });
                return angular.toJson(posData);
            })();
            var param = {
                'from': fromDateUTC,
                'to': toDateUTC,
                'POSName': posName,
                'POSData': posData,
            };

            var onSuccess = function(response) {};
            var onFail = function(response) {};
            var onError = function() {};
            return ajaxPost('updatepossalesdata', param, onSuccess, onFail, onError, isDefer);
        }

        function getPosSettingsApi(isDefer) {
            var opt = data;
            var param = {};

            var onSuccess = function(response) {
                opt.apiGetPosSettings = response.settings || {};
            };
            var onFail = function(response) {
                opt.apiGetPosSettings = {};
            };
            var onError = function() {
                opt.apiGetPosSettings = [];
            };
            return ajaxPost('getpossettings', param, onSuccess, onFail, onError, isDefer);
        }

        function updatePosSettingsApi(isDefer) {
            var opt = data;
            var isEnabled = opt.ftpInfo.enabled;
            var ftpInfo = (function() {
                var ftpInfo = angular.copy(opt.ftpInfo);
                delete ftpInfo['enabled'];
                return angular.toJson(ftpInfo);
            })();
            var param = {
                'import-enabled': isEnabled,
                'ftp-details': ftpInfo
            };

            var onSuccess = function(response) {
                notification('success', i18n("saved-successfully"));
            };
            var onFail = function(response) {
                notification('error', i18n('saved-failed'));
            };
            var onError = function() {
                notification('error', i18n('saved-failed'));
            };
            return ajaxPost('updatepossettings', param, onSuccess, onFail, onError, isDefer);
        }

        // function generateReport() {
        //     var opt = data;
        //     //cancel last time request
        //     $.each(opt.requestList, function(i, request) {
        //         request.cancel && request.cancel();
        //     });
        //     //set generate report promise
        //     opt.requestList = [
        //         getPosSalesReportApi(true)
        //     ];
        //     return $q.all(opt.requestList)
        //         .finally(function() {
        //             setReportData();
        //         });
        // }

        function switchTab(tabName) {
            var opt = data;
            $.each(opt.uiTab, function(i, tab) {
                tab.isActive = (tabName === tab.name) ? true : false;
            });
        }
    })

angular
    .module('kai.pos')
    .controller('PosController', function(
        KupOption, RouterStateService, UtilsService, AuthTokenFactory,
        PosService,
        $scope, $q, $filter, $timeout
    ) {
        var kupOpt = KupOption;
        var utils = UtilsService;
        var i18n = UtilsService.i18n;
        var notification = UtilsService.notification;
        var block = UtilsService.block;
        var mainCtrl = $scope.$parent.mainCtrl;
        var posCtrl = this;

        //UI controller
        posCtrl.data = PosService.data;
        posCtrl.data.tableTimeFormat = KupOption.default.formatlDatetime;
        posCtrl.fn = {
            switchTab: switchTab,
            updateFtp: updateFtp,
            saveReport: saveReport,
            showInput: showInput,
            hideInput: hideInput,
            hideAllInput: hideAllInput,
        };

        posCtrl.ngMessageUrl = kupOpt.sysNgMessageUrl;

        init();
        /*******************************************************************************
         *
         *  Function Definition
         *
         *******************************************************************************/
        function init() {
            mainCtrl.block.promise = loadUI();
        }

        function loadUI() {
            var getPosSettings = PosService.getPosSettingsApi(true);
            var getLabels = PosService.getLabelsApi(true);

            return $q.all([
                    getPosSettings,
                    getLabels
                ])
                .finally(function() {
                    setFtpUI();
                    setFtpWatch();
                    setTableUI();
                    setTableWatch();
                });
        }

        function switchTab(tabName) {
            PosService.switchTab(tabName);
        }

        /*******************************************************************************
         *
         *  Automated Function Definition
         *
         *******************************************************************************/
        function setFtpWatch() {
            $scope.$watch('posCtrl.data.ftpInfo.enabled', function(newVal, oldVal) {
                var opt = posCtrl.data;
                if (newVal !== oldVal) {
                    updateFtp();
                }
            }, true);
        }

        function setFtpUI() {
            PosService.setFtpData();
        }

        function updateFtp() {
            var opt = posCtrl.data;
            var promise = function() {
                return PosService.updatePosSettingsApi();
            };
            mainCtrl.block.promise = promise();
        }

        /*******************************************************************************
         *
         *  Manual Function Definition
         *
         *******************************************************************************/
        function setTableWatch() {
            $scope.$watch('posCtrl.data.searchInfo', function(newVal, oldVal) {
                //if (angular.toJson(newVal) !== angular.toJson(oldVal)) {
                generateReport();
                //}
            }, true);
        }

        function setTableUI() {
            setLabelSelect();
            setDateRangePicker();
        }

        function generateReport() {
            var opt = posCtrl.data;
            var dbPrcessingId = kupOpt.datatables.prcessingId;
            $('#' + dbPrcessingId).show();
            PosService.getPosSalesReportApi().finally(function() {
                $('#' + dbPrcessingId).hide();
                PosService.setReportData();
            });
        }

        function saveReport() {
            var opt = posCtrl.data;
            var promise = function() {
                return PosService.updatePosSalesDataApi();
            };
            mainCtrl.block.promise = promise();
        }

        function setLabelSelect() {
            PosService.setLabelData();
        }

        function setDateRangePicker() {
            var opt = posCtrl.data;
            var dateFormat = opt.dateFormat;
            var startDateFormat = opt.startDateFormat;
            var endDateFormat = opt.endDateFormat;

            var defaultDate = {
                startDate: moment(opt.searchInfo.startDate),
                endDate: moment(opt.searchInfo.endDate)
            };
            var showDate = function(start, end) {
                $scope.$apply(function() {
                    var opt = posCtrl.data;
                    opt.searchInfo.startDate = new Date(start.format(dateFormat));
                    opt.searchInfo.endDate = new Date(end.format(dateFormat));
                    $(opt.$daterange).find('span').html(moment(start).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly) + ' - ' + moment(end).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly));
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
                timePickerIncrement: 60,
                timePicker24Hour: true,
                //timePickerSeconds: false,
                opens: 'left',
                drops: 'down',
                buttonClasses: ['btn', 'btn-sm'],
                applyClass: 'btn-warning',
                cancelClass: 'btn-link cancel',

                "minDate": moment(new Date(moment().subtract(1, 'days').format(opt.startDateFormat))),
                "maxDate": moment(new Date(moment().format(opt.endDateFormat))),

                //separator: ' to ',
                ranges: ranges,
                locale: locale,
                startDate: defaultDate.startDate,
                endDate: defaultDate.endDate,
            }, showDate).data("daterangepicker");
            var customRangeBtn = pickerData.locale.customRangeLabel;


            $(opt.$daterange).find('span').html(moment(pickerData.startDate).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly) + ' - ' + moment(pickerData.endDate).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly));
            $(".daterangepicker .ranges ul li:contains('" + customRangeBtn + "')").on('click', function(e) {
                var $calendarControl = $('.daterangepicker.dropdown-menu');
                if ($calendarControl.hasClass('show-calendar')) {
                    e.stopPropagation();
                    $calendarControl.removeClass('show-calendar');
                }
            }).hide();
        };

        function showInput(time, key, inputId) {
            var opt = posCtrl.data;
            opt.reportInputList[time][key] = true;
            $timeout(function() {
                $('#' + inputId).focus();
            });

        }

        function hideInput(time, key) {
            var opt = posCtrl.data;
            opt.reportInputList[time][key] = false;
        }

        function hideAllInput() {
            var opt = posCtrl.data;
            $.each(opt.reportInputList, function(time, report) {
                report.isShowCount = false;
                report.isShowAmount = false;
            });
        }

        function showAllInput() {
            var opt = posCtrl.data;
            $.each(opt.reportInputList, function(time, report) {
                report.isShowCount = true;
                report.isShowAmount = true;
            });
        }
    });
