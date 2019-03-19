angular.module('kai.reports.loitering', [
	'ui.amcharts',
]);
angular
    .module('kai.reports.loitering')
    .factory("LoiteringService",
        function(KupOption, AmchartsTheme, UtilsService, PromiseFactory, KupApiService, ReportsService, AuthTokenFactory, $q, $timeout) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var ajaxPost = KupApiService.ajaxPost;
            var reports = ReportsService;

            var data = {
                //time format 
                timeFormat0: 'yyyy/MM/dd HH:mm:ss',
                timeFormat1: 'dd/MM/yyyy HH:mm:ss',
                timeFormat2: 'ddMMyyyyHHmmss',


                //selected data
                selectedGridList: {},
                selectedSitenameList: [],

                //selected chart data
                selectedChartDataSource: [],
                selectedChartDataSourceByTimeunit: [],
                selectedChartGraphs: [],
                selectedChartSortByDate: [],

                //from api reponse data
                apiAnalyticsReportList: [],

                //current info
                currentTab: 'chart', //value is uiTab name
                currentTimeunitForChart: 'hours', //value is uiTimeunit name

                //request List
                requestForReport: [],

                //UI selector
                $chart: '#loiteringChart',
                $chartForPdf: '#loiteringChartForPdf',

                //UI setting
                uiTab: [{
                    name: 'chart',
                    text: 'charts',
                    isActive: true,
                }],
                uiTimeunit: [{
                    name: 'hours',
                    text: 'hourly',
                    isActiveForChart: true,
                    isShowForChart: true,
                    chartPeriod: 'hh',
                }, {
                    name: 'days',
                    text: 'daily',
                    isActiveForChart: false,
                    isShowForChart: true,
                    chartPeriod: 'DD',
                }, {
                    name: 'weeks',
                    text: 'weekly',
                    isActiveForChart: false,
                    isShowForChart: true,
                    chartPeriod: 'WW',
                }, {
                    name: 'months',
                    text: 'monthly',
                    isActiveForChart: false,
                    isShowForChart: true,
                    chartPeriod: 'MM',
                }, {
                    name: 'years',
                    text: 'yearly',
                    isActiveForChart: false,
                    isShowForChart: false,
                    chartPeriod: 'YYYY',
                }],
                uiExport: {
                    isOpen: false,
                },
                uiGrid: {
                    isShow: true,
                    totalVisit: '...',
                    avgVisit: '...',
                    highestVisit: '...',
                    lowestVisit: '...',
                    leastEventBox: [],
                    topEventBox: [],
                    totalEventBox: [],
                },
                uiNodata: {
                    isShow: true,
                },
                uiChart: {
                    options: {},
                },
                uiChartForPdf: {
                    width: '900px',
                    height: '400px',
                    options: {},
                }
            };
            return {
                data: data,

                setInitData: setInitData,
                setInitTab: setInitTab,
                setInitTimeunit: setInitTimeunit,

                setSelectedChartData: setSelectedChartData,
                setGridData: setGridData,
                setChartTimeunitLabel: setChartTimeunitLabel,
                setChartTheme: setChartTheme,

                setChartData: setChartData,
                setChartOptions: setChartOptions,
                setChartForPdfOptions: setChartForPdfOptions,

                getAnalyticsReportApi: getAnalyticsReportApi,
                exportAggregatedCsvReportApi: exportAggregatedCsvReportApi,
                exportVcaSecurityPdfApi: exportVcaSecurityPdfApi,

                generateReport: generateReport,
                generateChart: generateChart,
            };
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function setInitData() {
                data.currentTab = 'chart';
                data.currentTimeunitForChart = 'hours';
                data.uiNodata.isShow = true;

                setInitTab();
                setInitTimeunit();

            }

            function setInitTab() {
                //default least tab to active 
                $.each(data.uiTab, function(i, tab) {
                    if (i == 0) {
                        tab.isActive = true;
                    } else {
                        tab.isActive = false;
                    }
                });
            }

            function setInitTimeunit() {
                //default least unit to active
                $.each(data.uiTimeunit, function(i, unit) {
                    if (i == 0) {
                        unit.isActiveForChart = true;
                    } else {
                        unit.isActiveForChart = false;
                    }
                });
            }

            function setSelectedChartData() {
                var opt = data;
                var reportsOpt = reports.data;
                var selectedItemDataList = reportsOpt.selectedItemDataList || {};
                var events = opt.apiAnalyticsReportList;
                var fromDate = reports.data.dateRange.startDate;
                var toDate = reports.data.dateRange.endDate;

                var seriesInfo = [];
                var eventDetails = [];
                var count = 0;
                var totalPlpVisit = 0;
                var countList = [];
                var filterEvents = [];
                var sitenameList = [];

                // opt.selectedGridList = [];
                // opt.selectedChartDataSource = [];
                // opt.selectedChartGraphs = [];

                //set base data source 
                (function() {
                    $.each(selectedItemDataList, function(uid, itemData) {
                        var totalInPerSelection = 0;
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
                        //check selected item's type
                        if (itemData.isAll || itemData.isLabel) {
                            var name = itemData.text,
                                deviceTimeList = {};
                            $.each(itemData.items, function(i, device) {
                                var deviceList = [];
                                $.each(device.items, function(j, camera) {
                                    var cameraData = camera.data;
                                    var cameraList = [];
                                    $.each(events, function(k, evt) {
                                        if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                            var countItem = {};
                                            countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                            //countItem["deviceName" + count] = cameraData.deviceName;
                                            //countItem["channelName" + count] = cameraData.channelName;
                                            //countItem["name" + count] = name;
                                            countItem["count" + count] = parseInt(evt.count, 10);
                                            //countItem["value" + count] = parseInt(evt.count, 10);
                                            cameraList.push(countItem);

                                            totalPlpVisit += parseInt(evt.count, 10);
                                            totalInPerSelection += parseInt(evt.count, 10);
                                            filterEvents.push(evt);
                                        }
                                    });
                                    deviceList.push(cameraList);
                                });

                                $.each(deviceList, function(j, cameraList) {
                                    $.each(cameraList, function(j, camera) {
                                        var timeIndex = kendo.toString(camera.time, data.timeFormat2);
                                        if (deviceTimeList[timeIndex]) {
                                            deviceTimeList[timeIndex]["count" + count] += camera["count" + count];
                                            //deviceTimeList[timeIndex]["value" + count] += camera["value" + count];
                                        } else {
                                            deviceTimeList[timeIndex] = {};
                                            //deviceTimeList[timeIndex]["name" + count] = ["name" + count];
                                            //deviceTimeList[timeIndex]["deviceName" + count] = camera["deviceName" + count];
                                            deviceTimeList[timeIndex]["count" + count] = camera["count" + count];
                                            //deviceTimeList[timeIndex]["value" + count] = camera["value" + count];
                                            deviceTimeList[timeIndex].time = camera.time;
                                        }
                                    })
                                })

                            });
                            $.each(deviceTimeList, function(i, deviceList) {
                                countList.push(deviceList);
                            })
                        };

                        if (itemData.isDevice) {
                            var name = itemData.labelName + " - " + itemData.text;
                            var deviceList = [],
                                deviceTimeList = {};
                            $.each(itemData.items, function(i, camera) {
                                var cameraData = camera.data;
                                var cameraList = [];
                                $.each(events, function(j, evt) {
                                    if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                        var countItem = {};
                                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                        //countItem["deviceName" + count] = cameraData.deviceName;
                                        //countItem["channelName" + count] = cameraData.channelName;
                                        //countItem["name" + count] = name;
                                        countItem["count" + count] = parseInt(evt.count, 10);
                                        //countItem["value" + count] = parseInt(evt.count, 10);
                                        cameraList.push(countItem);

                                        totalPlpVisit += parseInt(evt.count, 10);
                                        totalInPerSelection += parseInt(evt.count, 10);
                                        filterEvents.push(evt);
                                    }
                                });
                                deviceList.push(cameraList);
                            });
                            $.each(deviceList, function(i, cameraList) {
                                $.each(cameraList, function(j, camera) {
                                    var timeIndex = kendo.toString(camera.time, data.timeFormat2);
                                    if (deviceTimeList[timeIndex]) {
                                        deviceTimeList[timeIndex]["count" + count] += camera["count" + count];
                                        //deviceTimeList[timeIndex]["value" + count] += camera["value" + count];
                                    } else {
                                        deviceTimeList[timeIndex] = {};
                                        //deviceTimeList[timeIndex]["name" + count] = camera["name" + count];
                                        //deviceTimeList[timeIndex]["deviceName" + count] = camera["deviceName" + count];
                                        deviceTimeList[timeIndex]["count" + count] = camera["count" + count];
                                        //deviceTimeList[timeIndex]["value" + count] = camera["value" + count];
                                        deviceTimeList[timeIndex].time = camera.time;
                                    }
                                })
                            })

                            $.each(deviceTimeList, function(i, deviceList) {
                                countList.push(deviceList);
                            })
                        };

                        if (itemData.isCamera) {
                            var cameraData = itemData.data,
                                name = cameraData.deviceName + " - " + cameraData.channelName;
                            $.each(events, function(i, evt) {
                                if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                    var countItem = {};
                                    countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                    // countItem["deviceName" + count] = cameraData.deviceName;
                                    //countItem["channelName" + count] = cameraData.channelName;
                                    //countItem["name" + count] = name;
                                    countItem["count" + count] = parseInt(evt.count, 10);
                                    //countItem["value" + count] = parseInt(evt.count, 10);
                                    totalPlpVisit += parseInt(evt.count, 10);
                                    totalInPerSelection += parseInt(evt.count, 10);
                                    countList.push(countItem);
                                    filterEvents.push(evt);
                                }
                            });
                        }

                        sitenameList.push(name);

                        eventDetails.push({
                            "uid": uid,
                            "totalPerSelection": totalInPerSelection
                        });

                        seriesInfo.push({
                            "id": "g" + count,
                            "valueAxis": "v" + count,
                            "valueField": "count" + count,
                            "type": "smoothedLine",
                            //"lineColor": data.chartLineColors[count],
                            "bullet": "round",
                            "bulletBorderThickness": 1,
                            "hideBulletsCount": 30,
                            "title": name,
                            "fillAlphas": 0,
                            "lineThickness": 2,
                            "balloonText": name + " :<b>[[value]]</b>",
                        });
                        count++;
                    });
                })();

                //set eventDetails;
                (function() {
                    $.each(reports.data.selectedItemDataList, function(uid, item) {
                        $.each(eventDetails, function(i, grid) {
                            if (grid.uid != uid) {
                                return true;
                            }
                            if (item.isLabel || item.isAll) {
                                grid.labelText = item.text;
                            }
                            if (item.isDevice) {
                                grid.labelText = item.labelName;
                                grid.deviceText = item.text;
                            }
                            if (item.isCamera) {
                                grid.labelText = item.labelName;
                                grid.deviceText = item.deviceName;
                                grid.cameraText = item.text;
                            }
                        });
                    });
                })();

                //set countList;
                (function() {
                    var mergeCountList = {};
                    $.each(countList, function(i, ds) {
                        var timeIndex = kendo.toString(ds.time, data.timeFormat2);
                        if (mergeCountList[timeIndex]) {
                            mergeCountList[timeIndex] = $.extend(true, mergeCountList[timeIndex], ds);
                        } else {
                            mergeCountList[timeIndex] = {};
                            mergeCountList[timeIndex] = ds;
                        }
                    });

                    $.each(mergeCountList, function(time, ds) {
                        $.each(eventDetails, function(i, grid) {
                            if (!ds['count' + i]) {
                                ds['count' + i] = 0;
                                ds['count' + i] = 0;
                            }
                        });
                    });

                    countList = [];
                    $.each(mergeCountList, function(time, ds) {
                        countList.push(ds);
                    });
                    countList.sort(function(a, b) {
                        return a.time - b.time;
                    });
                })();

                opt.selectedGridList = eventDetails;
                opt.selectedChartDataSource = countList;
                opt.selectedChartGraphs = seriesInfo;
                opt.selectedSitenameList = sitenameList;

                return opt;
            }

            function setGridData() {
                var opt = data;
                var totalVisits = (function() {
                    var count = 0;
                    $.each(opt.selectedGridList, function(i, grid) {
                        count += grid.totalPerSelection;
                    });
                    return count;
                })();
                var selectedDeviceLength = opt.selectedGridList.length;

                var dataItems = angular.copy(opt.selectedChartDataSource);
                var selectedChartSortByDate = angular.copy(opt.selectedChartDataSourceByTimeunit);

                //set selectedChartSortByDate
                (function() {
                    $.each(selectedChartSortByDate, function(i, ds) {
                        ds.aggregate = (function() {
                            var count = 0;
                            for (var j = 0; j < selectedDeviceLength; j++) {
                                ds['count' + j] = ds['count' + j] || 0;
                                count += ds['count' + j];
                            }
                            return count;
                        })();
                    });

                    selectedChartSortByDate.sort(function(a, b) {
                        return b.aggregate - a.aggregate;
                    });

                    opt.selectedChartSortByDate = selectedChartSortByDate;
                })();

                //set grid data
                (function() {
                    var highestInfo = angular.copy(opt.selectedChartSortByDate).shift();
                    var lowestInfo = (function(){
                        var ds = angular.copy(opt.selectedChartSortByDate);
                        var lowestInfo = {};
                        $.each(ds, function(i, info) {
                            if(info.aggregate){
                                lowestInfo = info;
                            }
                            if(!info.aggregate){
                                return false;
                            }
                        });
                        return lowestInfo;
                    })();

                    opt.uiGrid.topEventBox = (function() {
                        var data = angular.copy(opt.selectedGridList);
                        return data.sort(function(a, b) {
                            return b.totalPerSelection - a.totalPerSelection;
                        });
                    })();

                    opt.uiGrid.leastEventBox = (function() {
                        var data = angular.copy(opt.selectedGridList);
                        return data.sort(function(a, b) {
                            return a.totalPerSelection - b.totalPerSelection;
                        });
                    })();

                    opt.uiGrid.totalEventBox = (function() {
                        var data = angular.copy(opt.selectedGridList);
                        return data;
                    })();

                    opt.uiGrid.totalVisit = kendo.toString(totalVisits, "n0");
                    opt.uiGrid.avgVisit = kendo.toString(Math.round(totalVisits / selectedDeviceLength), "n0");

                    opt.uiGrid.highestVisit = (highestInfo) ? kendo.toString(highestInfo.aggregate, "n0") : 0;
                    opt.uiGrid.lowestVisit = (lowestInfo) ? kendo.toString(lowestInfo.aggregate, "n0") : 0;

                    opt.uiGrid.highestVisitDate = (function() {
                        var returnData = '';
                        //var highestDate = angular.copy(opt.selectedChartSortByDate).shift().time;
                        returnData = (highestInfo) ? i18n('on') + " " + setChartTimeunitLabel(highestInfo.time) : '...';
                        return returnData;
                    })();

                    opt.uiGrid.lowestVisitDate = (function() {
                        var returnData = '';
                        //var lowestDate = angular.copy(opt.selectedChartSortByDate).pop().time;
                        returnData = (lowestInfo) ? i18n('on') + " " + setChartTimeunitLabel(lowestInfo.time) : '...';
                        return returnData;
                    })();
                })();
                return opt.uiGrid;
            }

            function setChartData() {
                var timeunit = data.currentTimeunitForChart;
                var dataSource = angular.copy(data.selectedChartDataSource);
                var selectedItemLength = reports.data.selectedSaveDataList.length;
                var dataTimeList = {};
                var dataSourceByTimeunit = [];

                //set dataSourceByTimeunit
                (function() {

                    var tmp = {};
                    for (var i = 0; i < selectedItemLength; i++) {
                        tmp['count' + i] = 0;
                    }

                    if (timeunit == 'hours') {
                        var start = moment(reports.data.dateRange.startDate);
                        var end = moment(reports.data.dateRange.endDate);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "hours")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = i.format();
                        }
                        $.each(dataSource, function(index, ds) {
                            var index = moment(ds.time).diff(start, 'hours');
                            countList[index] = ds;
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'days') {
                        var start = moment(reports.data.dateRange.startDate);
                        var end = moment(reports.data.dateRange.endDate);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "days")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = new Date(i);
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'days');
                            for (var j = 0; j < selectedItemLength; j++) {
                                countList[index]["count" + j] += ds["count" + j];
                            }
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'weeks') {
                        var weekStart = kupOpt.dateOfWeekStart;
                        var start = moment(reports.data.dateRange.startDate).isoWeekday(weekStart);
                        var end = moment(reports.data.dateRange.endDate).isoWeekday(weekStart);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "weeks")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = new Date(i);
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'weeks');
                            for (var j = 0; j < selectedItemLength; j++) {
                                countList[index]["count" + j] += ds["count" + j];
                            }
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'months') {
                        var start = moment(reports.data.dateRange.startDate).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var end = moment(reports.data.dateRange.endDate).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "month")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = i.format();
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'months');
                            for (var j = 0; j < selectedItemLength; j++) {
                                if (ds["count" + j] != undefined) {
                                    countList[index]["count" + j] += ds["count" + j];
                                }
                            }
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'years') {
                        var start = moment(reports.data.dateRange.startDate).month(0).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var end = moment(reports.data.dateRange.endDate).month(0).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "years")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = i.format();
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'year');
                            for (var j = 0; j < selectedItemLength; j++) {
                                if (ds["count" + j] != undefined) {
                                    countList[index]["count" + j] += ds["count" + j];
                                }
                            }
                        });
                        dataSourceByTimeunit = countList;
                    }
                })();

                data.selectedChartDataSourceByTimeunit = dataSourceByTimeunit;
                return dataSourceByTimeunit;
            }

            function setChartOptions() {
                var chartSetting = (function() {
                    var setting = {};
                    $.each(data.uiTimeunit, function(i, unit) {
                        if (unit.name === data.currentTimeunitForChart) {
                            setting.minPeriod = unit.chartPeriod;
                            //setting.categoryBalloonDateFormat = unit.chartCategoryBalloonDateFormat;
                            return false;
                        }
                    });
                    return setting;
                })();

                var chartOptions = {
                    //data setting
                    dataProvider: angular.copy(data.selectedChartDataSourceByTimeunit),
                    graphs: angular.copy(data.selectedChartGraphs),

                    //config setting
                    type: "serial",
                    theme: AuthTokenFactory.getTheme(),
                    //fontFamily: 'Verdana',
                    //fontSize: 11,
                    zoomOutText: i18n('show-all'),
                    valueAxes: [{
                        "axisColor": "chartCursor",
                        "axisThickness": 2,
                        "axisAlpha": 1,
                        "position": "left",
                    }],
                    chartScrollbar: {
                        "autoGridCount": true,
                        "graph": "g0",
                        "scrollbarHeight": 40,
                    },
                    chartCursor: {
                        cursorPosition: "mouse",
                        categoryBalloonFunction: function() {
                            var time = arguments[0];
                            return setChartTimeunitLabel(time);
                        },
                    },
                    categoryField: "time",
                    categoryAxis: {
                        firstDayOfWeek: kupOpt.dateOfWeekStart,
                        gridAlpha: 0,
                        //minorGridEnabled: true,
                        markPeriodChange: true,
                        boldLabels: true,
                        minPeriod: chartSetting.minPeriod,
                        parseDates: true,
                        equalSpacing: true,
                        dateFormats: [{
                            period: 'fff',
                            format: 'JJ:NN:SS'
                        }, {
                            period: 'ss',
                            format: 'JJ:NN:SS'
                        }, {
                            period: 'mm',
                            format: 'JJ:NN'
                        }, {
                            period: 'hh',
                            format: 'JJ:NN'
                        }, {
                            period: 'DD',
                            format: 'MMM DD'
                        }, {
                            period: 'WW',
                            format: 'MMM DD'
                        }, {
                            period: 'MM',
                            format: 'MMM'
                        }, {
                            period: 'YYYY',
                            format: 'YYYY'
                        }],
                        // categoryFunction: function(date) {
                        //     return arguments[1].time;
                        // },
                        labelFunction: function(dateText) {
                            //console.info(arguments);
                            var time = arguments[1];
                            switch(chartSetting.minPeriod){
                                case "hh":
                                    dateText = moment(time).format(kupOpt.default.formatAxisHourly);
                                break;
                                case "DD":
                                    dateText = moment(time).format(kupOpt.default.formatAxisDaily);
                                break;
                                case "WW":
                                    dateText = moment(time).startOf('week').isoWeekday(kupOpt.default.isoWeekStart).format(kupOpt.default.formatAxisWeekly) + " - \n" + moment(time).endOf('week').format(kupOpt.default.formatAxisWeekly);
                                break;
                                case "MM":
                                    dateText = moment(time).format(kupOpt.default.formatAxisMonthly);
                                break;
                            }
                            // var weekStart = kupOpt.dateOfWeekStart;
                            // if (chartSetting.minPeriod == 'WW' && arguments[3] == 'WW') {
                            //     var getTimeStart = (time.getDay() < weekStart) ?
                            //         new Date(time.getFullYear(), time.getMonth(), time.getDate() - (7 - weekStart - time.getDay())) :
                            //         new Date(time.getFullYear(), time.getMonth(), time.getDate() - (time.getDay() - weekStart));
                            //     var getTimeEnd = (time.getDay() < weekStart) ?
                            //         new Date(time.getFullYear(), time.getMonth(), time.getDate() + (weekStart - time.getDay() - 1)) :
                            //         new Date(time.getFullYear(), time.getMonth(), time.getDate() + (6 - (time.getDay() - weekStart)));
                            //     dateText = getTimeStart.toLocaleDateString("en-us", {
                            //         month: "short",
                            //         day: 'numeric'
                            //     }) + "- \n" + getTimeEnd.toLocaleDateString("en-us", {
                            //         month: "short",
                            //         day: 'numeric'
                            //     });
                            // }
                            return dateText;
                        },
                    },
                    legend: {
                        useGraphSettings: false,
                        valueWidth: 0,
                        valueFunction: function() {
                            return "";
                        }
                    },
                };

                data.uiChart.options = chartOptions;
                return chartOptions;
            }

            function setChartForPdfOptions() {
                var chartSetting = (function() {
                    var setting = {};
                    $.each(data.uiTimeunit, function(i, unit) {
                        if (unit.name === data.currentTimeunitForChart) {
                            setting.minPeriod = unit.chartPeriod;
                            //setting.categoryBalloonDateFormat = unit.chartCategoryBalloonDateFormat;
                            return false;
                        }
                    });
                    return setting;
                })();

                var chartOptions = {
                    //data setting
                    dataProvider: angular.copy(data.selectedChartDataSourceByTimeunit),
                    graphs: angular.copy(data.selectedChartGraphs),

                    //config setting
                    type: "serial",
                    theme: 'white',
                    fontFamily: 'Verdana',
                    fontSize: 11,
                    //zoomOutText: i18n('show-all'),
                    valueAxes: [{
                        "axisColor": "chartCursor",
                        "axisThickness": 2,
                        "axisAlpha": 1,
                        "position": "left",
                    }],
                    // chartScrollbar: {
                    //     "autoGridCount": true,
                    //     "graph": "g0",
                    //     "scrollbarHeight": 40,
                    // },
                    chartCursor: {
                        cursorPosition: "mouse",
                        categoryBalloonFunction: function() {
                            var time = arguments[0];
                            return setChartTimeunitLabel(time);
                        },
                    },
                    categoryField: "time",
                    categoryAxis: {
                        firstDayOfWeek: kupOpt.dateOfWeekStart,
                        gridAlpha: 0,
                        //minorGridEnabled: true,
                        markPeriodChange: true,
                        boldLabels: true,
                        minPeriod: chartSetting.minPeriod,
                        parseDates: true,
                        equalSpacing: true,
                        dateFormats: [{
                            period: 'fff',
                            format: 'JJ:NN:SS'
                        }, {
                            period: 'ss',
                            format: 'JJ:NN:SS'
                        }, {
                            period: 'mm',
                            format: 'JJ:NN'
                        }, {
                            period: 'hh',
                            format: 'JJ:NN'
                        }, {
                            period: 'DD',
                            format: 'MMM DD'
                        }, {
                            period: 'WW',
                            format: 'MMM DD'
                        }, {
                            period: 'MM',
                            format: 'MMM'
                        }, {
                            period: 'YYYY',
                            format: 'YYYY'
                        }],
                        // categoryFunction: function(date) {
                        //     return arguments[1].time;
                        // },
                        labelFunction: function(dateText) {
                            //console.info(arguments);
                            var time = arguments[1];
                            switch(chartSetting.minPeriod){
                                case "hh":
                                    dateText = moment(time).format(kupOpt.default.formatAxisHourly);
                                break;
                                case "DD":
                                    dateText = moment(time).format(kupOpt.default.formatAxisDaily);
                                break;
                                case "WW":
                                    dateText = moment(time).startOf('week').isoWeekday(kupOpt.default.isoWeekStart).format(kupOpt.default.formatAxisWeekly) + " - \n" + moment(time).endOf('week').format(kupOpt.default.formatAxisWeekly);
                                break;
                                case "MM":
                                    dateText = moment(time).format(kupOpt.default.formatAxisMonthly);
                                break;
                            }
                            // var weekStart = kupOpt.dateOfWeekStart;
                            // if (chartSetting.minPeriod == 'WW' && arguments[3] == 'WW') {
                            //     var getTimeStart = (time.getDay() < weekStart) ?
                            //         new Date(time.getFullYear(), time.getMonth(), time.getDate() - (7 - weekStart - time.getDay())) :
                            //         new Date(time.getFullYear(), time.getMonth(), time.getDate() - (time.getDay() - weekStart));
                            //     var getTimeEnd = (time.getDay() < weekStart) ?
                            //         new Date(time.getFullYear(), time.getMonth(), time.getDate() + (weekStart - time.getDay() - 1)) :
                            //         new Date(time.getFullYear(), time.getMonth(), time.getDate() + (6 - (time.getDay() - weekStart)));
                            //     dateText = getTimeStart.toLocaleDateString("en-us", {
                            //         month: "short",
                            //         day: 'numeric'
                            //     }) + "- \n" + getTimeEnd.toLocaleDateString("en-us", {
                            //         month: "short",
                            //         day: 'numeric'
                            //     });
                            // }
                            return dateText;
                        },
                    },
                    legend: {
                        useGraphSettings: false,
                        valueWidth: 0,
                        valueFunction: function() {
                            return "";
                        }
                    },
                };

                data.uiChartForPdf.options = chartOptions;
                return chartOptions;
            }

            function getAnalyticsReportApi(isDefer) {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;
                var selectedDeviceList = reportsOpt.selectedDevices[0].platformDeviceId;
                var selectedChannelList = reportsOpt.selectedDevices[0].channelId;
                var fromDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.startDate), data.timeFormat2);
                var toDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.endDate), data.timeFormat2);
                var param = {
                    "event-type": vcaEventType,
                    "device-id": JSON.stringify(selectedDeviceList),
                    "channel-id": "",
                    "from": fromDateUTC,
                    "to": toDateUTC,
                    "parameters": JSON.stringify({}),
                };

                var onSuccess = function(response) {
                    opt.apiAnalyticsReportList = response.data || [];
                };
                var onFail = function(response) {
                    opt.apiAnalyticsReportList = [];
                };
                var onError = function() {
                    opt.apiAnalyticsReportList = [];
                };
                return ajaxPost('getanalyticsreport', param, onSuccess, onFail, onError, isDefer);
            }

            function exportAggregatedCsvReportApi(periodType) {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;
                var fromDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.startDate), opt.timeFormat2);
                var toDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.endDate), opt.timeFormat2);
                var baseUnit = periodType;
                var selectedGroups = [];
                var apiServerUrl = AuthTokenFactory.getApiRootUrl();
                var type = "";

                $.each(reportsOpt.selectedItemDataList, function(uid, itemData) {
                    var devices = [];
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

                    //set selectedGroups 
                    (function() {
                        var groupname = "";
                        if (itemData.isLabel || itemData.isAll) {
                            groupname = itemData.labelName;
                        }
                        if (itemData.isDevice) {
                            groupname = itemData.labelName + " - " + itemData.text;
                        }
                        if (itemData.isCamera) {
                            groupname = itemData.labelName + " - " + itemData.deviceName + " - " + itemData.text;
                        }

                        selectedGroups.push({
                            "groupName": groupname,
                            "devicePairs": devices,
                            "type": type
                        });
                    })();
                });

                var param = {
                    "event-type": vcaEventType,
                    "selected-groups": JSON.stringify(selectedGroups),
                    "time-zone-offset": KupApiService.data.timeZoneOffset,
                    "from": fromDateUTC,
                    "to": toDateUTC,
                    "base-unit": baseUnit,
                };

                return KupApiService.exportDoc(param, 'exportaggregatedcsvreport');
            }

            function exportVcaSecurityPdfApi() {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;
                var apiServerUrl = AuthTokenFactory.getApiRootUrl();
                
                var chartSvgTitle = "<svg version='1.1' width='900' height='30'><text y='15' x='0' transform='translate(450)' text-anchor='middle' font-size='20' font-weight='bold'>" + i18n('occurrence-vs-time') + "</text></svg>";
                var chartOpt = setChartForPdfOptions();
                var chartPdf = window.AmCharts.makeChart(opt.$chartForPdf.slice(1), chartOpt);
                //get svg 
                var svgData = (function() {
                    var svg = '';
                    var svgData = [];
                    var chartInfo = {};
                    if (opt.currentTab == 'chart') {
                        svgData.push(chartSvgTitle);
                        $.each($(opt.$chartForPdf).find('svg'), function(i, el) {
                            svgData.push("<svg version='1.1' width='" + $(el).css('width') + "' height='" + $(el).css('height') + "'>" + $(el).html() + "</svg>");
                        });
                    }
                    return svgData;
                })();

                var totalVisits = (function() {
                    var count = 0;
                    $.each(opt.selectedGridList, function(i, grid) {
                        count += grid.totalPerSelection;
                    });
                    return count;
                })();

                var reportInfo = {
                    "event-type": vcaEventType,
                    "site-name": opt.selectedSitenameList.toString(),
                    "from": kendo.toString(reportsOpt.dateRange.startDate, opt.timeFormat1),
                    "to": kendo.toString(reportsOpt.dateRange.endDate, opt.timeFormat1),
                    "total-results": totalVisits + ""
                };

                var param = {
                    "time-zone-offset": KupApiService.data.timeZoneOffset,
                    "svg-string": svgData.join(""),
                    "report-info": JSON.stringify(reportInfo),
                };

                return KupApiService.exportDoc(param, 'exportvcasecuritypdf');
            }

            function setChartTimeunitLabel(dateFormat) {
                var dateText = '';
                var time = new Date(dateFormat);
                var weekStart = kupOpt.dateOfWeekStart;
                var dateLang = "en-us";
                var dateOpt = {
                    weekday: "short",
                    month: "short",
                    day: 'numeric',
                    year: "numeric"
                };
                if (data.currentTimeunitForChart == 'hours') {
                    dateText = moment(time).format(kupOpt.default.formatHourly);
                    // dateText = time.toLocaleDateString(dateLang, dateOpt) + ", " + time.getHours() + ":00";
                }
                if (data.currentTimeunitForChart == 'days') {
                    dateText = moment(time).format(kupOpt.default.formatDaily);
                    // dateText = time.toLocaleDateString(dateLang, dateOpt);
                }
                if (data.currentTimeunitForChart == 'weeks') {
                    dateText = moment(time).startOf('week').isoWeekday(kupOpt.default.isoWeekStart).format(kupOpt.default.formatWeekly) + " - \n" + moment(time).endOf('week').format(kupOpt.default.formatWeekly);
                    // var getTimeStart = (time.getDay() < weekStart) ?
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() - (7 - weekStart - time.getDay())) :
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() - (time.getDay() - weekStart));
                    // var getTimeEnd = (time.getDay() < weekStart) ?
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() + (weekStart - time.getDay() - 1)) :
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() + (6 - (time.getDay() - weekStart)));
                    // dateText = getTimeStart.toLocaleDateString(dateLang, dateOpt) + " - " + getTimeEnd.toLocaleDateString(dateLang, dateOpt);
                }
                if (data.currentTimeunitForChart == 'months') {
                    dateText = moment(time).format(kupOpt.default.formatMonthly);
                    // dateOpt = {
                    //     month: "short",
                    //     year: "numeric"
                    // };
                    // dateText = time.toLocaleDateString(dateLang, dateOpt);
                }
                if (data.currentTimeunitForChart == 'years') {
                    dateOpt = {
                        year: "numeric"
                    };
                    dateText = time.toLocaleDateString(dateLang, dateOpt);
                }
                return dateText;
            }

            function setChartTheme() {
                var theme = AuthTokenFactory.getTheme();
                window.AmCharts.themes[theme] = AmchartsTheme[theme];
            }

            function generateChart() {
                setChartTheme();
                setChartData();
                setChartOptions();
                setGridData();
            }

            function generateReport() {
                setInitData();
                var opt = data;

                //cancel last time request
                $.each(opt.requestForReport, function(i, request) {
                    request.cancel && request.cancel();
                });

                //check report status
                if (!reports.isSelectCamera()) {
                    notification('error', i18n('no-channel-selected'));
                    reports.isSuccessReport(false);
                    return false;
                }

                //set generate report promise
                opt.requestForReport = [
                    getAnalyticsReportApi(true)
                ];

                var dfd = $q.defer();
                $timeout(function() {
                    $q.all(opt.requestForReport)
                        .finally(function() {
                            setSelectedChartData();
                            if (opt.selectedChartDataSource.length <= 0) {
                                dfd.reject();
                                return;
                            }
                            reports.isSuccessReport(true);
                            generateChart();
                            dfd.resolve();
                        });
                }, 500);
                return dfd.promise;
            }
        });

angular
    .module('kai.reports.loitering')
    .controller('LoiteringController',
        function(
            KupOption,
            RouterStateService, UtilsService, PromiseFactory, AuthTokenFactory,
            ReportsService, LoiteringService,
            $scope, $timeout, $q, $rootScope
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var reports = ReportsService;
            var loiteringCtrl = this;

            //UI controller
            loiteringCtrl.data = LoiteringService.data;
            loiteringCtrl.fn = {
                setTab: setTab,
                setTimeunitForChart: setTimeunitForChart,
                exportPdf: exportPdf,
                exportCsv: exportCsv,
            };

            init();

            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function init() {
                //watch reportsServer data 
                $scope.$watch(function() {
                    return angular.toJson(reports.data);
                }, function(newVal, oldVal) {
                    var reportsOpt = angular.fromJson(newVal);
                    loiteringCtrl.data.uiNodata.isShow = !reportsOpt.isSuccessReport;
                }, true);

                //watch theme 
                $scope.$watch(function() {
                    return AuthTokenFactory.getTheme();
                }, function(newVal, oldVal) {
                    if (newVal == oldVal) {
                        return false;
                    }
                    LoiteringService.setChartTheme();
                    LoiteringService.generateChart();
                }, true);
            }

            function setTab(tabName) {
                var opt = loiteringCtrl.data;
                var tabData = opt.uiTab;
                $.each(tabData, function(i, data) {
                    data.isActive = (tabName === data.name) ? true : false;
                });
                opt.currentTab = tabName;
            }

            function setTimeunitForChart(unitName) {
                var opt = loiteringCtrl.data;
                var timeunitData = opt.uiTimeunit;
                $.each(timeunitData, function(i, data) {
                    data.isActiveForChart = (unitName === data.name) ? true : false;
                });
                opt.currentTimeunitForChart = unitName;
                LoiteringService.generateChart();
            }

            function exportCsv(periodType) {
                if (!reports.isSuccessReport()) {
                    notification('error', i18n('please-generate-reports'));
                    return false;
                }
                
                var warningNotify = notification('warning', i18n('exporting-to-csv'), 0);
                LoiteringService.exportAggregatedCsvReportApi(periodType)
                    .finally(function() {
                        warningNotify.close();
                    });
            }

            function exportPdf() {
                if (!reports.isSuccessReport()) {
                    notification('error', i18n('please-generate-reports'));
                    return false;
                }
                
                var warningNotify = notification('warning', i18n('exporting-to-pdf'), 0);
                LoiteringService.exportVcaSecurityPdfApi()
                    .finally(function() {
                        warningNotify.close();
                    });
            }
        });
