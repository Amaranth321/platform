angular.module('kai.periodic', [
    'datatables'
]);

angular.module('kai.periodic')
    .factory('PeriodicService', function(
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

        data.tableDataSource = [];
        data.selectTableData = [];
        data.apiPeriodicreports = [];
        data.apiPeriodicreportsTotal = 0;
        data.requestForReport = [];

        //UI selector
        data.$daterange = '#kupDaterange';
        //UI setting
        data.uiType = [{
            name: 'all',
            text: 'all',
            value: '',
        }, {
            name: 'hourly',
            text: 'hourly',
            value: 'HOURLY',
        }, {
            name: 'daily',
            text: 'daily',
            value: 'DAILY',
        }, {
            name: 'weekly',
            text: 'weekly',
            value: 'WEEKLY',
        }, {
            name: 'monthly',
            text: 'monthly',
            value: 'MONTHLY',
        }];
        data.uiExport = [{
            name: 'pdf',
            text: 'download-pdf',
            value: 'PDF',
            class: 'fa fa-file-pdf-o pdf',
            isShow: false,
        }, {
            name: 'excel',
            text: 'download-excel',
            value: 'EXCEL',
            class: 'fa fa-file-excel-o excel',
            isShow: false,
        }, {
            name: 'csv',
            text: 'download-csv',
            value: 'CSV',
            class: 'kup-csv csv',
            isShow: false,
        }];

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
            type: {},
            startDate: new Date(moment().format(data.startDateFormat)),
            endDate: new Date(moment().format(data.endDateFormat))
        };
        return {
            data: data,
            //Set Data
            setTableData: setTableData,
            setUiExport: setUiExport,
            setSelectTableData: setSelectTableData,
            //Get api 
            getPeriodicReportApi: getPeriodicReportApi,
            downloadPeriodicPeportApi: downloadPeriodicPeportApi,
        };
        /*******************************************************************************
         *
         *  Set Data Function Definition
         *
         *******************************************************************************/
        function setTableData() {
            var opt = data;
            opt.tableDataSource = angular.copy(opt.apiPeriodicreports);
            $.each(opt.tableDataSource, function(i, ds) {
                //the response date is auto change local, so not to using UTCToLocal fn
                // ds.fromDateLocal = kendo.toString(new Date(ds.reportInfo.period.from), opt.timeFormat[0]);
                ds.fromDateLocal = moment(ds.reportInfo.period.from).format(kupOpt.default.formatlDatetime);
                ds.type = (function() {
                    var dsType = ds.reportInfo.frequency;
                    $.each(opt.uiType, function(i, uiType) {
                        if (uiType.value === dsType) {
                            dsType = i18n(uiType.text);
                            return false;
                        }
                    });
                    return dsType;
                })();
            });
        }

        function setSelectTableData(index) {
            var opt = data;
            if (index === undefined) {
                opt.selectTableData = [];
            } else {
                var tableData = opt.tableDataSource[index];
                opt.selectTableData = tableData ? [tableData] : [];
            }
        }

        function setUiExport(index) {
            var opt = data;

            //default all hide
            $.each(opt.uiExport, function(i, ep) {
                ep.isShow = false;
            });

            if (index === undefined) {
                //do something;
            } else {
                var tableData = opt.tableDataSource[index];
                //to show export
                $.each(tableData.gridFsList, function(i, ls) {
                    $.each(opt.uiExport, function(j, ep) {
                        if (ep.value === ls.format) {
                            ep.isShow = true;
                        }
                    });
                });
            }
        }


        /*******************************************************************************
         *
         *  API Function Definition
         *
         *******************************************************************************/
        function getPeriodicReportApi(pageParam, isDefer) {
            var opt = data;
            var frequency = opt.searchInfo.type.value || '';
            var fromDateUTC = kendo.toString(utils.localToUTC(opt.searchInfo.startDate), data.timeFormat[2]);
            var toDateUTC = kendo.toString(utils.localToUTC(opt.searchInfo.endDate), data.timeFormat[2]);
            var param = {
                "skip": pageParam.skip,
                "take": pageParam.take,

                "frequency": frequency,
                "from": fromDateUTC,
                "to": toDateUTC
            };

            var onSuccess = function(response) {
                opt.apiPeriodicreports = response.reports || [];
                opt.apiPeriodicreportsTotal = response.totalCount || 0;
            };
            var onFail = function(response) {
                opt.apiPeriodicreports = [];
                opt.apiPeriodicreportsTotal = opt.apiPeriodicreportsTotal || 0;
            };
            var onError = function() {
                opt.apiPeriodicreports = [];
                opt.apiPeriodicreportsTotal = opt.apiPeriodicreportsTotal || 0;
            };
            return ajaxPost('getperiodicreports', param, onSuccess, onFail, onError, isDefer);
        }

        function downloadPeriodicPeportApi(format) {
            format = format || '';
            var opt = data;
            var selectData = opt.selectTableData[0];

            var param = {
                "report-id": selectData._id,
                "format": format,
            };
            KupApiService.apiDownload('downloadperiodicreport', param);
        }
    })

angular
    .module('kai.periodic')
    .controller('PeriodicController', function(
        KupOption, RouterStateService, UtilsService, AuthTokenFactory,
        PeriodicService,
        DTOptionsBuilder, DTColumnBuilder,
        $scope, $q, $timeout
    ) {
        var kupOpt = KupOption;
        var utils = UtilsService;
        var i18n = UtilsService.i18n;
        var notification = UtilsService.notification;
        var block = UtilsService.block;
        var mainCtrl = $scope.$parent.mainCtrl;
        var periodicCtrl = this;

        //UI controller
        periodicCtrl.data = PeriodicService.data;
        periodicCtrl.data.uiTable = {
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
                            selectTableData(index);
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
                    oSettings.jqXHR = PeriodicService.getPeriodicReportApi(pageParam)
                        .finally(function() {
                            PeriodicService.setTableData();
                            var opt = periodicCtrl.data;
                            var records = {
                                "draw": draw,
                                "recordsTotal": opt.apiPeriodicreportsTotal,
                                "recordsFiltered": opt.apiPeriodicreportsTotal,
                                "data": opt.tableDataSource
                            }
                            fnCallback(records);
                            selectTableData();
                        });
                }),
            columnDefs: [
                DTColumnBuilder.newColumn('fromDateLocal').withTitle(i18n('date-time')),
                DTColumnBuilder.newColumn('type').withTitle('type'),
            ]
        };
        periodicCtrl.fn = {
            exportReport: exportReport,
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

        function setUI() {
            var promise = function() {
                var dfd = $q.defer();
                $timeout(function() {
                    setDateRangePicker();
                    dfd.resolve();
                });
                return dfd.promise;
            };
            return promise();
        }

        function setWatch() {
            $scope.$watch('periodicCtrl.data.searchInfo', function(newVal, oldVal) {
                if (angular.toJson(newVal) !== angular.toJson(oldVal)) {
                    generateReport();
                }
            }, true);
        }
        /*******************************************************************************
         *
         *  Search UI and Report Function Definition
         *
         *******************************************************************************/
        function generateReport() {
            var opt = periodicCtrl.data;
            opt.uiTable.dtInstance.DataTable.ajax.reload();
        }


        function setDateRangePicker() {
            var opt = periodicCtrl.data;
            var dateFormat = opt.dateFormat;
            var startDateFormat = opt.startDateFormat;
            var endDateFormat = opt.endDateFormat;

            var defaultDate = {
                startDate: moment(opt.searchInfo.startDate),
                endDate: moment(opt.searchInfo.endDate)
            };
            var showDate = function(start, end) {
                $scope.$apply(function() {
                    var opt = periodicCtrl.data;
                    opt.searchInfo.startDate = new Date(start.format(dateFormat));
                    opt.searchInfo.endDate = new Date(end.format(dateFormat));
                    //$(opt.$daterange).find('span').text(moment(start).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly) + ' - ' + moment(end).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly));
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

                //"minDate": moment(new Date(moment().subtract(1, 'days').format(opt.startDateFormat))),
                "maxDate": moment(new Date(moment().format(opt.endDateFormat))),
                //separator: ' to ',
                ranges: ranges,
                locale: locale,
                startDate: defaultDate.startDate,
                endDate: defaultDate.endDate,
            }, showDate).data("daterangepicker");
            var customRangeBtn = pickerData.locale.customRangeLabel;


            //$(opt.$daterange).find('span').html(moment(pickerData.startDate).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly) + ' - ' + moment(pickerData.endDate).locale(AuthTokenFactory.getUserLanguage()).format(KupOption.default.formatMonthly));
            $(opt.$daterange).find('span').html(pickerData.startDate.format(startDateFormat) + ' - ' + pickerData.endDate.format(endDateFormat));
            $(".daterangepicker .ranges ul li:contains('" + customRangeBtn + "')").on('click', function(e) {
                var $calendarControl = $('.daterangepicker.dropdown-menu');
                if ($calendarControl.hasClass('show-calendar')) {
                    e.stopPropagation();
                    $calendarControl.removeClass('show-calendar');
                }
            });
        };

        function selectTableData(index) {
            PeriodicService.setUiExport(index);
            PeriodicService.setSelectTableData(index);
        }

        function exportReport(format) {
            PeriodicService.downloadPeriodicPeportApi(format);
        }
    });
