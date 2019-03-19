angular.module('kai.reports.attention', [
	'ui.amcharts',
]);
angular
    .module('kai.reports.attention')
    .factory("AttentionService",
        function(KupOption, AmchartsTheme, UtilsService, PromiseFactory, KupApiService, ReportsService, AuthTokenFactory, $filter, $q, $timeout) {
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
                selectedTotalCount: 0,
                selectedSitenameList: [],

                //selected bar chart data
                selectedBarChartDataSource: [],
                selectedBarChartDataSourceByTimeunit: [],
                selectedBarChartValueAxes: [],
                selectedBarChartGraphs: [],

                //selected line chart data
                selectedLineChartDataSource: [],
                selectedLineChartDataSourceByTimeunit: [],
                selectedLineChartValueAxes: [],
                selectedLineChartGraphs: [],

                //from api reponse data
                apiAnalyticsReportList: [],

                //current info
                currentTab: 'chart', //value is uiTab name
                currentTimeunitForLineChart: 'hours', //value is uiTimeunit name

                //request List
                requestForReport: [],

                //UI selector
                $barChart: '#attentionBarChart',
                $barChartForPdf: '#attentionBarChartForPdf',
                $lineChart: '#attentionLineChart',
                $lineChartForPdf: '#attentionLineChartForPdf',

                //UI setting
                uiTab: [{
                    name: 'chart',
                    text: 'charts',
                    isActive: true,
                }],
                uiTimeunit: [{
                    name: 'hours',
                    text: 'hourly',
                    isActiveForLineChart: true,
                    isShowForLineChart: true,
                    chartPeriod: 'hh',
                }, {
                    name: 'days',
                    text: 'daily',
                    isActiveForLineChart: false,
                    isShowForLineChart: true,
                    chartPeriod: 'DD',
                }, {
                    name: 'weeks',
                    text: 'weekly',
                    isActiveForLineChart: false,
                    isShowForLineChart: true,
                    chartPeriod: 'WW',
                }, {
                    name: 'months',
                    text: 'monthly',
                    isActiveForLineChart: false,
                    isShowForLineChart: true,
                    chartPeriod: 'MM',
                }, {
                    name: 'years',
                    text: 'yearly',
                    isActiveForLineChart: false,
                    isShowForLineChart: false,
                    chartPeriod: 'YYYY',
                }],
                uiExport: {
                    isOpen: false,
                },
                uiNodata: {
                    isShow: true,
                },
                uiLineChart: {
                    options: {},
                },
                uiBarChart: {
                    options: {},
                },
                uiLineChartForPdf: {
                    width: '900px',
                    height: '400px',
                    options: {},
                },
                uiBarChartForPdf: {
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

                setChartTheme: setChartTheme,
                setChartData: setChartData,

                setBarChartData: setBarChartData,
                setBarChartOptions: setBarChartOptions,
                setBarChartForPdfOptions: setBarChartForPdfOptions,

                setLineChartData: setLineChartData,
                setLineChartOptions: setLineChartOptions,
                setLineChartTimeunitLabel: setLineChartTimeunitLabel,
                setLineChartForPdfOptions: setLineChartForPdfOptions,

                getAnalyticsReportApi: getAnalyticsReportApi,
                exportAggregatedCsvReportApi: exportAggregatedCsvReportApi,
                exportVcaSecurityPdfApi: exportVcaSecurityPdfApi,

                generateReport: generateReport,
                generateBarChart: generateBarChart,
                generateLineChart: generateLineChart,
            };
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function setInitData() {
                data.currentTab = 'chart';
                data.currentTimeunitForLineChart = 'hours';
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
                        unit.isActiveForLineChart = true;
                    } else {
                        unit.isActiveForLineChart = false;
                    }
                });
            }

            function setChartData() {
                var opt = data;
                var reportsOpt = reports.data;
                var selectedItemDataList = reportsOpt.selectedItemDataList || {};
                var events = opt.apiAnalyticsReportList;
                var fromDate = reports.data.dateRange.startDate;
                var toDate = reports.data.dateRange.endDate;

                var dbEvents = angular.copy(opt.apiAnalyticsReportList);
                var categoryNames = ["under 5s", "5 to 10s", "10 to 20s", "20 to 30s", "30 to 60s",
                    "1 to 3m", "3 to 5m", "5 to 8m", "8 to 10m", "10 to 15m", "15 to 30m"
                ];
                var durValues = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
                var countDurValues = function(eventData) {
                    $.each(eventData, function(key, value) {
                        switch (key) {
                            case "dur0_5s":
                                durValues[0] = durValues[0] + value;
                                break;
                            case "dur5_10s":
                                durValues[1] = durValues[1] + value;
                                break;
                            case "dur10_20s":
                                durValues[2] = durValues[2] + value;
                                break;
                            case "dur20_30s":
                                durValues[3] = durValues[3] + value;
                                break;
                            case "dur30_60s":
                                durValues[4] = durValues[4] + value;
                                break;
                            case "dur1_3m":
                                durValues[5] = durValues[5] + value;
                                break;
                            case "dur3_5m":
                                durValues[6] = durValues[6] + value;
                                break;
                            case "dur5_8m":
                                durValues[7] = durValues[7] + value;
                                break;
                            case "dur8_10m":
                                durValues[8] = durValues[8] + value;
                                break;
                            case "dur10_15m":
                                durValues[9] = durValues[9] + value;
                                break;
                            case "dur15_30m":
                                durValues[10] = durValues[10] + value;
                                break;
                        }
                    });
                };
                //for bar chart
                var durList = [];
                var seriesInfo1 = [];
                var valueAxes1 = [];
                //for line chart
                var attentionCountList = [];
                var seriesInfo2 = [];
                var valueAxes2 = [];

                var sitenameList = [];
                var totalCount = 0;

                //get total count
                (function() {
                    $.each(events, function(i, evt) {
                        $.each(selectedItemDataList, function(uid, itemData) {
                            if ($.isEmptyObject(itemData)) {
                                return true;
                            }

                            if (itemData.isAll || itemData.isLabel) {
                                $.each(itemData.items, function(j, device) {
                                    $.each(device.items, function(k, camera) {
                                        var cameraData = camera.data;
                                        if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                            totalCount += evt.male;
                                            totalCount += evt.female;
                                        }
                                    });
                                });
                            }
                            if (itemData.isDevice) {
                                $.each(itemData.items, function(j, camera) {
                                    var cameraData = camera.data;
                                    if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                        totalCount += evt.male;
                                        totalCount += evt.female;
                                    }
                                });

                            }
                            if (itemData.isCamera) {
                                var cameraData = itemData.data;
                                if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                    totalCount += evt.male;
                                    totalCount += evt.female;
                                }
                            }
                        });
                    });
                })();

                //set attentionCountList
                (function() {
                    $.each(selectedItemDataList, function(uid, itemData) {
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
                        if (itemData.isAll || itemData.isLabel) {
                            var name = itemData.text;
                            var deviceTimeList = {};
                            $.each(itemData.items, function(i, device) {
                                var deviceList = [];
                                $.each(device.items, function(j, camera) {
                                    var cameraData = camera.data;
                                    var cameraList = [];
                                    $.each(dbEvents, function(k, event) {
                                        if (event.deviceId == cameraData.deviceId && event.channelId == cameraData.channelId) {
                                            var countItem = {};
                                            countItem.totalDuration = event.duration;
                                            countItem.eventNum = event.count;
                                            countItem.date = utils.convertUTCtoLocal(new Date(event.date));
                                            cameraList.push(countItem);
                                            countDurValues(event);
                                        }
                                    });
                                    deviceList.push(cameraList);
                                });
                                $.each(deviceList, function(i, cameraList) {
                                    $.each(cameraList, function(j, camera) {
                                        var timeIndex = kendo.toString(camera.date, opt.timeFormat2);
                                        if (deviceTimeList[timeIndex]) {
                                            deviceTimeList[timeIndex].totalDuration += camera.totalDuration;
                                            deviceTimeList[timeIndex].eventNum += camera.eventNum;
                                        } else {
                                            deviceTimeList[timeIndex] = {};
                                            deviceTimeList[timeIndex].totalDuration = camera.totalDuration;
                                            deviceTimeList[timeIndex].eventNum = camera.eventNum;
                                            deviceTimeList[timeIndex].date = camera.date;
                                        }
                                    })
                                })
                            });
                            $.each(deviceTimeList, function(i, deviceList) {
                                attentionCountList.push(deviceList);
                            })
                        } else if (itemData.isDevice) {
                            var name = itemData.labelName + " - " + itemData.text;
                            var deviceList = [],
                                deviceTimeList = {};
                            $.each(itemData.items, function(i, camera) {
                                var cameraData = camera.data;
                                var cameraList = [];
                                $.each(dbEvents, function(j, event) {
                                    if (event.deviceId == cameraData.deviceId && event.channelId == cameraData.channelId) {
                                        var countItem = {};
                                        countItem.totalDuration = event.duration;
                                        countItem.eventNum = event.count;
                                        countItem.date = utils.convertUTCtoLocal(new Date(event.date));
                                        cameraList.push(countItem);
                                        countDurValues(event);
                                    }
                                });
                                deviceList.push(cameraList);
                            });

                            $.each(deviceList, function(i, cameraList) {
                                $.each(cameraList, function(j, camera) {
                                    var timeIndex = kendo.toString(camera.date, opt.timeFormat2);
                                    if (deviceTimeList[timeIndex]) {
                                        deviceTimeList[timeIndex].totalDuration += camera.totalDuration;
                                        deviceTimeList[timeIndex].eventNum += camera.eventNum;
                                    } else {
                                        deviceTimeList[timeIndex] = {};
                                        deviceTimeList[timeIndex].totalDuration = camera.totalDuration;
                                        deviceTimeList[timeIndex].eventNum = camera.eventNum;
                                        deviceTimeList[timeIndex].date = camera.date;
                                    }
                                })
                            })
                            $.each(deviceTimeList, function(i, deviceList) {
                                attentionCountList.push(deviceList);
                            })


                        } else if (itemData.isCamera) {
                            var cameraData = itemData.data;
                            var name = cameraData.deviceName + " - " + cameraData.channelName;
                            $.each(dbEvents, function(i, event) {
                                if (event.deviceId == cameraData.deviceId && event.channelId == cameraData.channelId) {
                                    var countItem = {};
                                    countItem.totalDuration = event.duration;
                                    countItem.eventNum = event.count;
                                    countItem.date = utils.convertUTCtoLocal(new Date(event.date));
                                    attentionCountList.push(countItem);
                                    countDurValues(event);
                                }
                            });
                        }
                        sitenameList.push(name);
                    });

                })();

                //set attentionCountList by sort time and count in the same time
                (function() {
                    var mergeCountList = {};
                    $.each(attentionCountList, function(i, ds) {
                        var timeIndex = kendo.toString(ds.date, data.timeFormat2);
                        if (mergeCountList[timeIndex]) {
                            mergeCountList[timeIndex].eventNum += ds.eventNum;
                            mergeCountList[timeIndex].totalDuration += ds.totalDuration;

                        } else {
                            mergeCountList[timeIndex] = {};
                            mergeCountList[timeIndex] = ds;
                        }
                    });

                    attentionCountList = [];
                    $.each(mergeCountList, function(time, ds) {
                        ds.time = ds.date;
                        ds.avgDuration = (ds.eventNum) ? parseFloat((ds.totalDuration / ds.eventNum).toFixed(2), 10) : 0;
                        attentionCountList.push(ds);
                    });
                    attentionCountList.sort(function(a, b) {
                        return a.time - b.time;
                    });
                })();

                //set duration list
                (function() {
                    $.each(categoryNames, function(i, name) {
                        durList.push({
                            category: name,
                            value: 0
                        });
                    });
                    $.each(durValues, function(i, value) {
                        durList[i].value = value;
                    });
                })();

                //set seriesInfo;
                (function() {
                    seriesInfo1 = [{
                        "id": "g0",
                        "valueAxis": "v0",
                        "valueField": "value",
                        "type": "column",
                        //"lineColor": data.chartLineColors[count],
                        //"bullet": "round",
                        //"bulletBorderThickness": 1,
                        //"hideBulletsCount": 30,
                        "title": i18n("span-duration"),
                        "fillAlphas": 1,
                        "lineThickness": 2,
                        "balloonText": "[[category]]: <b>[[value]]</b>",
                    }];
                    seriesInfo2 = [{
                        "id": "g0",
                        "valueAxis": "v0",
                        "valueField": "avgDuration",
                        "type": "column",
                        //"lineColor": data.chartLineColors[count],
                        //"bullet": "round",
                        //"bulletBorderThickness": 1,
                        //"hideBulletsCount": 30,
                        "title": i18n("average-duration"),
                        "fillAlphas": 1,
                        "lineThickness": 2,
                        "balloonText": i18n("average-duration") + ": <b>[[avgDuration]]s</b>",
                    }, {
                        "id": "g1",
                        "valueAxis": "v1",
                        "valueField": "eventNum",
                        "type": "smoothedLine",
                        //"lineColor": data.chartLineColors[count],
                        "bullet": "round",
                        "bulletBorderThickness": 1,
                        "hideBulletsCount": 30,
                        "title": i18n("number-of-faces"),
                        "fillAlphas": 0,
                        "lineThickness": 2,
                        "balloonText": i18n("number-of-faces") + ": <b>[[eventNum]]</b>",
                    }];
                })();

                //set valueAxes
                (function() {
                    valueAxes1 = [{
                        "axisColor": "chartCursor",
                        "axisThickness": 2,
                        "axisAlpha": 1,
                        "position": "left",
                    }];
                    valueAxes2 = [{
                        id: "v0",
                        "axisColor": "chartCursor",
                        "axisThickness": 2,
                        "axisAlpha": 1,
                        "position": "left",
                        "duration": "ss",
                        "durationUnits": {
                            "hh": "h ",
                            "mm": "m",
                            "ss": "s"
                        },
                    }, {
                        id: "v1",
                        "axisColor": "chartCursor",
                        "axisThickness": 2,
                        "gridAlpha": 0,
                        "axisAlpha": 1,
                        "position": "right",
                    }];
                })();

                //set chart data
                (function() {
                    data.selectedBarChartDataSource = durList;
                    data.selectedBarChartGraphs = seriesInfo1;
                    data.selectedBarChartValueAxes = valueAxes1;

                    data.selectedLineChartDataSource = attentionCountList;
                    data.selectedLineChartGraphs = seriesInfo2;
                    data.selectedLineChartValueAxes = valueAxes2;

                    data.selectedSitenameList = sitenameList;
                    data.selectedTotalCount = totalCount;
                })();
                return data;
            }

            function setBarChartData() {
                data.selectedBarChartDataSourceByTimeunit = data.selectedBarChartDataSource;
                return data.selectedBarChartDataSource;
            }

            function setLineChartData() {
                var timeunit = data.currentTimeunitForLineChart;
                var dataSource = angular.copy(data.selectedLineChartDataSource);
                var selectedItemLength = reports.data.selectedSaveDataList.length;
                var dataTimeList = {};
                var dataSourceByTimeunit = [];

                //set dataSourceByTimeunit
                (function() {

                    var tmp = {
                        avgDuration: 0,
                        eventNum: 0,
                        totalDuration: 0
                    };

                    if (timeunit == 'hours') {
                        var start = moment(reports.data.dateRange.startDate);
                        var end = moment(reports.data.dateRange.endDate);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "hours")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].date = i.toDate();
                            countList[index].time = i.toDate();
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
                            countList[index].date = i.toDate();
                            countList[index].time = i.toDate();
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'days');
                            countList[index]["eventNum"] += ds["eventNum"];
                            countList[index]["totalDuration"] += ds["totalDuration"];
                        });
                        $.each(countList, function(key, ds) {
                            var index = moment(ds.time).diff(start, 'days');
                            countList[index].avgDuration = (!isNaN(ds.eventNum) && !isNaN(ds.totalDuration) && ds.eventNum > 0) ? Math.round(ds.totalDuration / ds.eventNum * 100) / 100 : 0;
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
                            countList[index].date = i.toDate();
                            countList[index].time = i.toDate();
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'weeks');
                            countList[index]["eventNum"] += ds["eventNum"];
                            countList[index]["totalDuration"] += ds["totalDuration"];
                        });
                        $.each(countList, function(key, ds) {
                            var index = moment(ds.time).diff(start, 'weeks');
                            countList[index].avgDuration = (!isNaN(ds.eventNum) && !isNaN(ds.totalDuration) && ds.eventNum > 0) ? Math.round(ds.totalDuration / ds.eventNum * 100) / 100 : 0;
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
                            countList[index].date = i.toDate();
                            countList[index].time = i.toDate();
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'month');
                            countList[index]["eventNum"] += ds["eventNum"];
                            countList[index]["totalDuration"] += ds["totalDuration"];
                        });
                        $.each(countList, function(key, ds) {
                            var index = moment(ds.time).diff(start, 'month');
                            countList[index].avgDuration = (!isNaN(ds.eventNum) && !isNaN(ds.totalDuration) && ds.eventNum > 0) ? Math.round(ds.totalDuration / ds.eventNum * 100) / 100 : 0;
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
                            countList[index].date = i.toDate();
                            countList[index].time = i.toDate();
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'years');
                            countList[index]["eventNum"] += ds["eventNum"];
                            countList[index]["totalDuration"] += ds["totalDuration"];
                        });
                        $.each(countList, function(key, ds) {
                            var index = moment(ds.time).diff(start, 'years');
                            countList[index].avgDuration = (!isNaN(ds.eventNum) && !isNaN(ds.totalDuration) && ds.eventNum > 0) ? Math.round(ds.totalDuration / ds.eventNum * 100) / 100 : 0;
                        });
                        dataSourceByTimeunit = countList;
                    }

                    // dataSourceByTimeunit.sort(function(a, b) {
                    //     return a.time - b.time;
                    // });
                })();

                data.selectedLineChartDataSourceByTimeunit = dataSourceByTimeunit;
                return dataSourceByTimeunit;
            }

            function setBarChartOptions() {
                var chartOptions = {
                    //data setting
                    dataProvider: angular.copy(data.selectedBarChartDataSourceByTimeunit),
                    graphs: angular.copy(data.selectedBarChartGraphs),
                    valueAxes: angular.copy(data.selectedBarChartValueAxes),

                    //config setting
                    type: "serial",
                    theme: AuthTokenFactory.getTheme(),
                    //fontFamily: 'Verdana',
                    //fontSize: 11,
                    zoomOutText: i18n('show-all'),
                    chartScrollbar: {
                        "autoGridCount": true,
                        "graph": "g0",
                        "scrollbarHeight": 40,
                    },
                    chartCursor: {
                        cursorPosition: "mouse",
                    },
                    categoryField: "category",
                    categoryAxis: {
                        firstDayOfWeek: kupOpt.dateOfWeekStart,
                        gridAlpha: 0,
                        minorGridEnabled: true,
                        markPeriodChange: true,
                        boldLabels: true,
                        equalSpacing: true,
                        categoryFunction: function(date) {
                            return arguments[1].category;
                        },
                        labelFunction: function(dateText) {
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

                data.uiBarChart.options = chartOptions;
                return chartOptions;
            }

            function setBarChartForPdfOptions() {
                var chartOptions = {
                    //data setting
                    dataProvider: angular.copy(data.selectedBarChartDataSourceByTimeunit),
                    graphs: angular.copy(data.selectedBarChartGraphs),
                    valueAxes: angular.copy(data.selectedBarChartValueAxes),

                    //config setting
                    type: "serial",
                    theme: 'white',
                    //fontFamily: 'Verdana',
                    //fontSize: 11,
                    //zoomOutText: i18n('show-all'),
                    // chartScrollbar: {
                    //     "autoGridCount": true,
                    //     "graph": "g0",
                    //     "scrollbarHeight": 40,
                    // },
                    chartCursor: {
                        cursorPosition: "mouse",
                    },
                    categoryField: "category",
                    categoryAxis: {
                        firstDayOfWeek: kupOpt.dateOfWeekStart,
                        gridAlpha: 0,
                        minorGridEnabled: true,
                        markPeriodChange: true,
                        boldLabels: true,
                        equalSpacing: true,
                        categoryFunction: function(date) {
                            return arguments[1].category;
                        },
                        labelFunction: function(dateText) {
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

                data.uiBarChartForPdf.options = chartOptions;
                return chartOptions;
            }

            function setLineChartOptions() {
                var chartSetting = (function() {
                    var setting = {};
                    $.each(data.uiTimeunit, function(i, unit) {
                        if (unit.name === data.currentTimeunitForLineChart) {
                            setting.minPeriod = unit.chartPeriod;
                            return false;
                        }
                    });
                    return setting;
                })();

                var chartOptions = {
                    //data setting
                    dataProvider: angular.copy(data.selectedLineChartDataSourceByTimeunit),
                    graphs: angular.copy(data.selectedLineChartGraphs),
                    valueAxes: angular.copy(data.selectedLineChartValueAxes),

                    //config setting
                    type: "serial",
                    theme: AuthTokenFactory.getTheme(),
                    //fontFamily: 'Verdana',
                    //fontSize: 11,
                    zoomOutText: i18n('show-all'),
                    chartScrollbar: {
                        "autoGridCount": true,
                        "graph": "g0",
                        "scrollbarHeight": 40,
                    },
                    chartCursor: {
                        cursorPosition: "mouse",
                        categoryBalloonFunction: function() {
                            var time = arguments[0];
                            return setLineChartTimeunitLabel(time);
                        },
                    },
                    categoryField: "time",
                    categoryAxis: {
                        firstDayOfWeek: kupOpt.dateOfWeekStart,
                        gridAlpha: 0,
                        minorGridEnabled: true,
                        markPeriodChange: true,
                        boldLabels: true,
                        minPeriod: chartSetting.minPeriod,
                        parseDates: true,
                        equalSpacing: true,
                        // dateFormats: [{
                        //     period: 'fff',
                        //     format: 'JJ:NN:SS'
                        // }, {
                        //     period: 'ss',
                        //     format: 'JJ:NN:SS'
                        // }, {
                        //     period: 'mm',
                        //     format: 'JJ:NN'
                        // }, {
                        //     period: 'hh',
                        //     format: 'JJ:NN'
                        // }, {
                        //     period: 'DD',
                        //     format: 'MMM DD'
                        // }, {
                        //     period: 'WW',
                        //     format: 'MMM DD'
                        // }, {
                        //     period: 'MM',
                        //     format: 'MMM'
                        // }, {
                        //     period: 'YYYY',
                        //     format: 'YYYY'
                        // }],
                        categoryFunction: function(date) {
                            return arguments[1].time;
                        },
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

                data.uiLineChart.options = chartOptions;
                return chartOptions;
            }

            function setLineChartForPdfOptions() {
                var chartSetting = (function() {
                    var setting = {};
                    $.each(data.uiTimeunit, function(i, unit) {
                        if (unit.name === data.currentTimeunitForLineChart) {
                            setting.minPeriod = unit.chartPeriod;
                            return false;
                        }
                    });
                    return setting;
                })();

                var chartOptions = {
                    //data setting
                    dataProvider: angular.copy(data.selectedLineChartDataSourceByTimeunit),
                    graphs: angular.copy(data.selectedLineChartGraphs),
                    valueAxes: angular.copy(data.selectedLineChartValueAxes),

                    //config setting
                    type: "serial",
                    theme: 'white',
                    //fontFamily: 'Verdana',
                    //fontSize: 11,
                    //zoomOutText: i18n('show-all'),
                    // chartScrollbar: {
                    //     "autoGridCount": true,
                    //     "graph": "g0",
                    //     "scrollbarHeight": 40,
                    // },
                    chartCursor: {
                        cursorPosition: "mouse",
                        categoryBalloonFunction: function() {
                            var time = arguments[0];
                            return setLineChartTimeunitLabel(time);
                        },
                    },
                    categoryField: "time",
                    categoryAxis: {
                        firstDayOfWeek: kupOpt.dateOfWeekStart,
                        gridAlpha: 0,
                        minorGridEnabled: true,
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
                        categoryFunction: function(date) {
                            return arguments[1].time;
                        },
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

                data.uiLineChartForPdf.options = chartOptions;
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
                    "device-id-list": JSON.stringify(selectedDeviceList),
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

            function setLineChartTimeunitLabel(dateFormat) {
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
                if (data.currentTimeunitForLineChart == 'hours') {
                    // dateText = time.toLocaleDateString(dateLang, dateOpt) + ", " + time.getHours() + ":00";
                    dateText = moment(time).format(kupOpt.default.formatHourly);
                }
                if (data.currentTimeunitForLineChart == 'days') {
                    // dateText = time.toLocaleDateString(dateLang, dateOpt);
                    dateText = moment(time).format(kupOpt.default.formatDaily);
                }
                if (data.currentTimeunitForLineChart == 'weeks') {
                    // var getTimeStart = (time.getDay() < weekStart) ?
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() - (7 - weekStart - time.getDay())) :
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() - (time.getDay() - weekStart));
                    // var getTimeEnd = (time.getDay() < weekStart) ?
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() + (weekStart - time.getDay() - 1)) :
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() + (6 - (time.getDay() - weekStart)));
                    // dateText = getTimeStart.toLocaleDateString(dateLang, dateOpt) + " - " + getTimeEnd.toLocaleDateString(dateLang, dateOpt);
                    dateText = moment(time).startOf('week').isoWeekday(kupOpt.default.isoWeekStart).format(kupOpt.default.formatWeekly) + " - \n" + moment(time).endOf('week').format(kupOpt.default.formatWeekly);
                }
                if (data.currentTimeunitForLineChart == 'months') {
                    // dateOpt = {
                    //     month: "short",
                    //     year: "numeric"
                    // };
                    // dateText = time.toLocaleDateString(dateLang, dateOpt);
                    dateText = moment(time).format(kupOpt.default.formatAxisMonthly);
                }
                if (data.currentTimeunitForLineChart == 'years') {
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
                    "base-unit": baseUnit
                };

                return KupApiService.exportDoc(param, 'exportaggregatedcsvreport');
            }

            function exportVcaSecurityPdfApi() {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;

                var lineSvgTitle = "<svg version='1.1' width='900' height='30'><text y='15' x='0' transform='translate(450)' text-anchor='middle' font-size='20' font-weight='bold'>" + i18n('attention-span-variation-over-time') + "</text></svg>";
                var lineChartOpt = setLineChartForPdfOptions();
                var lineChartPdf = window.AmCharts.makeChart(opt.$lineChartForPdf.slice(1), lineChartOpt);

                var barSvgTitle = "<svg version='1.1' width='900' height='30'><text y='15' x='0' transform='translate(450)' text-anchor='middle' font-size='20' font-weight='bold'>" + i18n('visitor-segmentation-by-attention-span') + "</text></svg>";
                var barChartOpt = setBarChartForPdfOptions();
                var barChartPdf = window.AmCharts.makeChart(opt.$barChartForPdf.slice(1), barChartOpt);

                //get svg 
                var svgData = (function() {
                    var svg = '';
                    var svgData = [];
                    var chartInfo = {};
                    if (opt.currentTab == 'chart') {
                        svgData.push(barSvgTitle);
                        $.each($(opt.$barChartForPdf).find('svg'), function(i, el) {
                            svgData.push("<svg version='1.1' width='" + $(el).css('width') + "' height='" + $(el).css('height') + "'>" + $(el).html() + "</svg>");
                        });
                        svgData.push(lineSvgTitle);
                        $.each($(opt.$lineChartForPdf).find('svg'), function(i, el) {
                            svgData.push("<svg version='1.1' width='" + $(el).css('width') + "' height='" + $(el).css('height') + "'>" + $(el).html() + "</svg>");
                        });
                    }
                    return svgData;
                })();

                var reportInfo = {
                    "event-type": vcaEventType,
                    "site-name": opt.selectedSitenameList.toString(),
                    "from": kendo.toString(reportsOpt.dateRange.startDate, opt.timeFormat1),
                    "to": kendo.toString(reportsOpt.dateRange.endDate, opt.timeFormat1),
                    "total-results": opt.selectedTotalCount + ""
                };

                var param = {
                    "time-zone-offset": KupApiService.data.timeZoneOffset,
                    "svg-string": svgData.join(""),
                    "report-info": JSON.stringify(reportInfo),
                };
                return KupApiService.exportDoc(param, 'exportvcasecuritypdf');
            }

            function generateBarChart() {
                setChartTheme();
                setBarChartData();
                setBarChartOptions();
            }

            function generateLineChart() {
                setChartTheme();
                setLineChartData();
                setLineChartOptions();
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

                //return promise
                var dfd = $q.defer();
                $timeout(function() {
                    $q.all(opt.requestForReport)
                        .finally(function() {
                            setChartData();
                            if (opt.apiAnalyticsReportList.length <= 0) {
                                reports.isSuccessReport(false);
                                dfd.reject();
                                return;
                            }
                            if (opt.selectedLineChartDataSource.length <= 0) {
                                reports.isSuccessReport(false);
                                dfd.reject();
                                return;
                            }
                            reports.isSuccessReport(true);
                            generateBarChart();
                            generateLineChart();
                            dfd.resolve();
                        });
                }, 500);
                return dfd.promise;
            }
        });

angular
    .module('kai.reports.attention')
    .controller('AttentionController',
        function(
            KupOption,
            RouterStateService, UtilsService, PromiseFactory, AuthTokenFactory,
            ReportsService, AttentionService,
            $scope, $timeout, $q
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var reports = ReportsService;
            var attentionCtrl = this;

            //UI controller
            attentionCtrl.data = AttentionService.data;
            attentionCtrl.fn = {
                setTab: setTab,
                setTimeunitForLineChart: setTimeunitForLineChart,

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
                setWatch();
            }

            function setWatch() {
                //watch reportsServer data 
                $scope.$watch(function() {
                    return angular.toJson(reports.data);
                }, function(newVal, oldVal) {
                    var reportsOpt = angular.fromJson(newVal);
                    attentionCtrl.data.uiNodata.isShow = !reportsOpt.isSuccessReport;
                }, true);

                //watch theme 
                $scope.$watch(function() {
                    return AuthTokenFactory.getTheme();
                }, function(newVal, oldVal) {
                    if (newVal == oldVal) {
                        return false;
                    }
                    AttentionService.setChartTheme();
                    AttentionService.generateLineChart();
                    AttentionService.generateBarChart();
                }, true);
            }

            function setTab(tabName) {
                var opt = attentionCtrl.data;
                var tabData = opt.uiTab;
                $.each(tabData, function(i, data) {
                    data.isActive = (tabName === data.name) ? true : false;
                });
                opt.currentTab = tabName;
            }

            function setTimeunitForLineChart(unitName) {
                var opt = attentionCtrl.data;
                var timeunitData = opt.uiTimeunit;
                $.each(timeunitData, function(i, data) {
                    data.isActiveForLineChart = (unitName === data.name) ? true : false;
                });
                opt.currentTimeunitForLineChart = unitName;
                AttentionService.generateLineChart();
            }

            function exportCsv(periodType) {
                if (!reports.isSuccessReport()) {
                    notification('error', i18n('please-generate-reports'));
                    return false;
                }

                var warningNotify = notification('warning', i18n('exporting-to-csv'), 0);
                AttentionService.exportAggregatedCsvReportApi(periodType)
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
                AttentionService.exportVcaSecurityPdfApi()
                    .finally(function() {
                        warningNotify.close();
                    });
            }
        });
