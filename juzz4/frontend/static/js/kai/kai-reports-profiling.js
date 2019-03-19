angular.module('kai.reports.profiling', [
    'ui.morris',
    'ui.amcharts',
    'datatables'
]);

angular
    .module('kai.reports.profiling')
    .factory("ProfilingService",
        function(
            KupOption, AmchartsTheme, UtilsService, PromiseFactory, KupApiService, ReportsService, AuthTokenFactory,
            DTOptionsBuilder, DTColumnDefBuilder, DTColumnBuilder, MorrisChartTheme,
            $q, $filter, $timeout
        ) {
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

                //data format
                dataFormat0: {
                    male: 0,
                    female: 0,
                    happy: 0,
                    neutral: 0,
                    age1: 0,
                    age2: 0,
                    age3: 0,
                    age4: 0,
                },
                dataFormat1: {
                    gender: {
                        male: 0,
                        female: 0
                    },
                    age: {
                        age1: 0,
                        age2: 0,
                        age3: 0,
                        age4: 0,
                    },
                    emotion: {
                        happy: 0,
                        neutral: 0
                    }
                },
                dataFormat2: {
                    age: [],
                    gender: [],
                    emotion: []
                },
                dataFormat3: {
                    age: {},
                    gender: {},
                    emotion: {}
                },
                chartTypeList: ['gender', 'emotion', 'age'],
                pieTheme: '',
                colorTheme: {
                    gender: ["#32a8ad", "#ff675d"],
                    emotion: ["#f6ae40", "#8aba50"],
                    age: ["#ff6530", "#f6ae40", "#8aba50", "#337caf"]
                },
                ageList: KupOption.age,

                //selected data
                selectedDataSource: {
                    gender: [],
                    emotion: [],
                    age: []
                },
                selectedChartDataSource: {
                    gender: [],
                    emotion: [],
                    age: []
                },
                selectedChartGraphs: {
                    gender: [],
                    emotion: [],
                    age: []
                },

                selectedBarDataSource: {
                    gender: [],
                    emotion: [],
                    age: []
                },
                selectedBarGraphs: {
                    gender: [],
                    emotion: [],
                    age: []
                },

                selectedPieDataSource: {
                    gender: {},
                    emotion: {},
                    age: {}
                },
                selectedTableDataSource: [],
                selectedTotalCount: 0,
                selectedSitenameList: [],

                //from api reponse data
                apiAnalyticsReportList: [],

                //current info
                currentTab: 'summary', //value is uiTab name
                currentTimeunitForChart: { //value is uiTimeunit name
                    gender: 'hours',
                    emotion: 'hours',
                    age: 'hours'
                },

                //request List
                requestForReport: [],

                //UI selector
                // $chart: '#profilingChart',
                // $chartForPdf: '#profilingChartForPdf',

                //UI setting
                uiTab: [{
                    name: 'summary',
                    text: 'summary',
                    class: 'kup-summary',
                    isActive: true,
                }, {
                    name: 'comparison',
                    text: 'comparison',
                    class: 'kup-comparison',
                    isActive: false,
                }],

                uiExport: {
                    isOpen: false,
                },
                uiNodata: {
                    isShow: true,
                },
                uiTimeunit: {
                    gender: getUiTimeunitData(),
                    emotion: getUiTimeunitData(),
                    age: getUiTimeunitData()
                },
                uiChart: {
                    gender: getUiChartData(),
                    emotion: getUiChartData(),
                    age: getUiChartData()
                },
                uiBar: {
                    gender: getUiChartData(),
                    emotion: getUiChartData(),
                    age: getUiChartData()
                },
                uiPie: {
                    gender: getUiPieData(),
                    emotion: getUiPieData(),
                    age: getUiPieData()
                },
                uiChartForPdf: {
                    gender: getUiChartForPdf(),
                    emotion: getUiChartForPdf(),
                    age: getUiChartForPdf()
                },
                uiTable: {
                    options: DTOptionsBuilder
                        .newOptions()
                        //.withPaginationType('full_numbers')
                        .withOption('paging', false)
                        .withOption('info', false)
                        .withOption('responsive', true),
                    columnDefs: []
                }
            };

            return {
                data: data,

                setInitData: setInitData,
                setInitTab: setInitTab,
                setInitTimeunit: setInitTimeunit,

                setSelectedData: setSelectedData,

                setChartData: setChartData,
                setChartOptions: setChartOptions,
                setChartForPdfOptions: setChartForPdfOptions,
                setChartTimeunitLabel: setChartTimeunitLabel,
                setChartTheme: setChartTheme,

                setBarData: setBarData,
                setBarOptions: setBarOptions,

                setPieData: setPieData,
                setPieOptions: setPieOptions,
                setPieTheme: setPieTheme,

                getAnalyticsReportApi: getAnalyticsReportApi,
                exportAggregatedCsvReportApi: exportAggregatedCsvReportApi,
                exportVcaSecurityPdfApi: exportVcaSecurityPdfApi,

                generateReport: generateReport,
                generateChart: generateChart,
                generateBar: generateBar,
                generatePie: generatePie,
            };
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function getUiTimeunitData() {
                return [{
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
                }];
            }

            function getUiChartData() {
                return {
                    options: {}
                };
            }

            function getUiPieData() {
                return {
                    options: {}
                };
            }

            function getUiChartForPdf() {
                return {
                    width: '900px',
                    height: '400px',
                    options: {},
                };
            }

            function setInitData() {
                data.currentTab = 'summary';
                data.currentTimeunitForChart = {
                    gender: 'hours',
                    emotion: 'hours',
                    age: 'hours'
                };
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
                $.each(data.chartTypeList, function(i, type) {
                    $.each(data.uiTimeunit[type], function(j, unit) {
                        if (j == 0) {
                            unit.isActiveForChart = true;
                        } else {
                            unit.isActiveForChart = false;
                        }
                    });
                });
            }

            function setSelectedData() {
                var opt = data;
                var reportsOpt = reports.data;
                var selectedItemDataList = reportsOpt.selectedItemDataList || {};
                var events = opt.apiAnalyticsReportList;
                var fromDate = reports.data.dateRange.startDate;
                var toDate = reports.data.dateRange.endDate;

                var dataFormat = angular.copy(opt.dataFormat1);
                var tableList = {};
                var lineCountList = angular.copy(opt.dataFormat2);
                var lineSeriesInfo = angular.copy(opt.dataFormat2);

                var barCountList = angular.copy(opt.dataFormat2);
                var barSeriesInfo = angular.copy(opt.dataFormat2);

                var sitenameList = [];
                var totalCount = 0;

                var getTableData = function(uid, itemName, evt) {
                    var tmpObj = {};
                    evt = evt || angular.copy(opt.dataFormat0);
                    if (tableList[uid]) {
                        tmpObj = angular.copy(tableList[uid]);
                        $.each(dataFormat, function(type, df) {
                            $.each(df, function(key, val) {
                                tmpObj[type][key] += evt[key];
                                tmpObj[type]['total'] = (tmpObj[type]['total']) ? tmpObj[type]['total'] + evt[key] : evt[key];
                            });
                        });
                    } else {
                        tmpObj = angular.copy(opt.dataFormat1);
                        tmpObj.itemName = itemName;
                        $.each(dataFormat, function(type, df) {
                            $.each(df, function(key, val) {
                                tmpObj[type][key] = evt[key];
                                tmpObj[type]['total'] = (tmpObj[type]['total']) ? tmpObj[type]['total'] + evt[key] : evt[key];
                            });
                        });
                    }
                    return tmpObj;
                };

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

                //set lineCountList
                (function() {
                    $.each(selectedItemDataList, function(uid, itemData) {
                        //var totalInPerSelection = 0;
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
                                    $.each(events, function(k, evt) {
                                        if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                            var cameraList = [];
                                            var countItem = angular.copy(opt.dataFormat3);
                                            countItem.gender.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                            countItem.emotion.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                            countItem.age.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));

                                            $.each(dataFormat, function(type, df) {
                                                $.each(df, function(key, val) {
                                                    countItem[type][key] = evt[key];
                                                });
                                            });
                                            cameraList.push(countItem);
                                            deviceList.push(cameraList);
                                            tableList[uid] = getTableData(uid, name, evt);
                                        }
                                    });
                                    tableList[uid] = tableList[uid] || getTableData(uid, name);
                                });

                                $.each(deviceList, function(j, cameraList) {
                                    $.each(cameraList, function(k, camera) {
                                        var timeIndex = kendo.toString(camera.gender.time, data.timeFormat2);
                                        deviceTimeList[timeIndex] = deviceTimeList[timeIndex] || {};
                                        $.each(camera, function(key1, type) {
                                            deviceTimeList[timeIndex][key1] = deviceTimeList[timeIndex][key1] || {
                                                time: type.time
                                            };
                                            $.each(type, function(key2, detail) {
                                                if (key2 !== 'time') {
                                                    deviceTimeList[timeIndex][key1][key2] = (deviceTimeList[timeIndex][key1][key2] || 0) + detail;
                                                }

                                            });
                                        });
                                    })
                                })

                            });
                            $.each(deviceTimeList, function(i, deviceList) {
                                $.each(lineCountList, function(type, detail) {
                                    lineCountList[type].push(deviceList[type]);
                                })
                            })
                        };

                        if (itemData.isDevice) {
                            var name = itemData.labelName + " - " + itemData.text;
                            var deviceList = [],
                                deviceTimeList = {};
                            $.each(itemData.items, function(i, camera) {
                                var cameraData = camera.data;
                                $.each(events, function(j, evt) {
                                    if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                        var cameraList = [];
                                        var countItem = angular.copy(opt.dataFormat3);
                                        countItem.gender.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                        countItem.emotion.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                        countItem.age.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));

                                        $.each(dataFormat, function(type, df) {
                                            $.each(df, function(key, val) {
                                                countItem[type][key] = evt[key];
                                            });
                                        });
                                        cameraList.push(countItem);
                                        deviceList.push(cameraList);
                                        tableList[uid] = getTableData(uid, name, evt);
                                    }
                                });
                                tableList[uid] = tableList[uid] || getTableData(uid, name);
                            });

                            $.each(deviceList, function(i, cameraList) {
                                $.each(cameraList, function(j, camera) {
                                    var timeIndex = kendo.toString(camera.gender.time, data.timeFormat2);
                                    deviceTimeList[timeIndex] = deviceTimeList[timeIndex] || {};
                                    $.each(camera, function(key1, type) {
                                        deviceTimeList[timeIndex][key1] = deviceTimeList[timeIndex][key1] || {
                                            time: type.time
                                        };
                                        $.each(type, function(key2, detail) {
                                            if (key2 !== 'time') {
                                                deviceTimeList[timeIndex][key1][key2] = (deviceTimeList[timeIndex][key1][key2] || 0) + detail;
                                            }

                                        });
                                    });
                                })
                            })
                            $.each(deviceTimeList, function(i, deviceList) {
                                $.each(lineCountList, function(type, detail) {
                                    lineCountList[type].push(deviceList[type]);
                                })
                            })
                        };

                        if (itemData.isCamera) {
                            var cameraData = itemData.data,
                                name = cameraData.deviceName + " - " + cameraData.channelName;
                            $.each(events, function(i, evt) {
                                if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                    var countItem = angular.copy(opt.dataFormat3);
                                    countItem.gender.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                    countItem.emotion.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                    countItem.age.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                    $.each(dataFormat, function(type, df) {
                                        $.each(df, function(key, val) {
                                            countItem[type][key] = evt[key];
                                        });
                                    });
                                    lineCountList.gender.push(countItem.gender);
                                    lineCountList.emotion.push(countItem.emotion);
                                    lineCountList.age.push(countItem.age);
                                    tableList[uid] = getTableData(uid, name, evt);
                                }
                            });
                            tableList[uid] = tableList[uid] || getTableData(uid, name);

                        }

                        sitenameList.push(name);
                        //count++;
                    });
                })();

                //merge lineCountList with different items
                (function() {
                    var mergeCountList = {};
                    $.each(lineCountList, function(type, typeDataList) {
                        $.each(typeDataList, function(i, typeData) {
                            var timeIndex = kendo.toString(typeData.time, data.timeFormat2);
                            mergeCountList[timeIndex] = mergeCountList[timeIndex] || {};
                            mergeCountList[timeIndex][type] = mergeCountList[timeIndex][type] || {
                                time: typeData.time
                            };
                            $.each(typeData, function(key, val) {
                                if (key !== 'time') {
                                    mergeCountList[timeIndex][type][key] = (mergeCountList[timeIndex][type][key] || 0) + val;
                                }
                            });
                        });
                    });
                    lineCountList = angular.copy(opt.dataFormat2);
                    $.each(mergeCountList, function(time, ds) {
                        $.each(ds, function(type, typeData) {
                            lineCountList[type] = lineCountList[type] || [];
                            lineCountList[type].push(typeData);
                        });
                    });
                })();

                //sort lineCountList;
                (function() {
                    $.each(lineCountList, function(type, list) {
                        lineCountList[type].sort(function(a, b) {
                            return a.time - b.time;
                        });
                    });
                })();

                //set line seriesInfo;
                (function() {
                    var getGraphsInfo = function(count, fieldName) {
                        return {
                            "id": "g" + count,
                            "valueAxis": "v" + count,
                            "valueField": fieldName,
                            "type": "smoothedLine",
                            //"lineColor": data.chartLineColors[count],
                            "bullet": "round",
                            "bulletBorderThickness": 1,
                            "hideBulletsCount": 30,
                            "title": i18n(fieldName),
                            "fillAlphas": 0,
                            "lineThickness": 2,
                            "balloonText": i18n(fieldName) + " :<b>[[value]]</b>",
                        };
                    };
                    var count;
                    $.each(opt.dataFormat1, function(type, list) {
                        count = 0;
                        $.each(list, function(key, val) {
                            lineSeriesInfo[type].push(getGraphsInfo(count, key));
                            count++;
                        });
                    });
                })();

                //set bar count list;
                (function() {
                    var tmpObj = {};
                    $.each(tableList, function(uid, list) {
                        $.each(list, function(type, val) {
                            tmpObj = val;
                            tmpObj.itemName = list.itemName;
                            if (barCountList[type]) {
                                barCountList[type].push(tmpObj);
                            }
                        });
                    });
                })();
                //set bar seriesInfo;
                (function() {
                    var getGraphsInfo = function(count, fieldName) {
                        return {
                            "balloonText": i18n(fieldName) + " :<b>[[value]]</b>",
                            "fillAlphas": 0.8,
                            "labelText": "[[value]]",
                            "lineAlpha": 0.3,
                            "title": i18n(fieldName),
                            "type": "column",
                            "color": "#000000",
                            "valueField": fieldName
                        };
                    };
                    var count;
                    $.each(opt.dataFormat1, function(type, list) {
                        count = 0;
                        $.each(list, function(key, val) {
                            barSeriesInfo[type].push(getGraphsInfo(count, key));
                            count++;
                        });
                    });
                })();

                //set select data
                opt.selectedDataSource = angular.copy(lineCountList);
                opt.selectedTableDataSource = angular.copy(tableList);

                opt.selectedChartDataSource = angular.copy(lineCountList);
                opt.selectedChartGraphs = angular.copy(lineSeriesInfo);

                opt.selectedBarDataSource = angular.copy(barCountList);
                opt.selectedBarGraphs = angular.copy(barSeriesInfo);

                opt.selectedPieDataSource = angular.copy(lineCountList);

                opt.selectedSitenameList = sitenameList;
                opt.selectedTotalCount = totalCount;

            }

            function setChartData(type) {
                var timeunit = data.currentTimeunitForChart[type];
                var dataSource = angular.copy(data.selectedDataSource[type]);
                var selectedItemLength = reports.data.selectedSaveDataList.length;
                var dataTimeList = {};
                var dataSourceByTimeunit = [];

                var dataFormat = angular.copy(data.dataFormat1[type]);
                //set dataSourceByTimeunit
                (function() {

                    if (timeunit == 'hours') {
                        var start = moment(reports.data.dateRange.startDate);
                        var end = moment(reports.data.dateRange.endDate);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "hours")) {
                            var index = countList.length;
                            countList[index] = {};
                            $.each(dataFormat, function(key, val) {
                                countList[index].time = i.format();
                                countList[index][key] = 0;
                            });
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
                            $.each(dataFormat, function(key, val) {
                                countList[index].time = i.format();
                                countList[index][key] = 0;
                            });
                        }
                        $.each(dataSource, function(index, ds) {
                            var index = moment(ds.time).diff(start, 'days');
                            $.each(dataFormat, function(key, val) {
                                countList[index][key] += ds[key];
                            });
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
                            $.each(dataFormat, function(key, val) {
                                countList[index].time = i.format();
                                countList[index][key] = 0;
                            });
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'weeks');
                            $.each(dataFormat, function(key, val) {
                                countList[index][key] += ds[key];
                            });
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
                            $.each(dataFormat, function(key, val) {
                                countList[index].time = i.format();
                                countList[index][key] = 0;
                            });
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'months');
                            $.each(dataFormat, function(key, val) {
                                countList[index][key] += ds[key];
                            });
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
                            $.each(dataFormat, function(key, val) {
                                countList[index].time = i.format();
                                countList[index][key] = 0;
                            });
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'years');
                            $.each(dataFormat, function(key, val) {
                                countList[index][key] += ds[key];
                            });
                        });
                        dataSourceByTimeunit = countList;
                    }
                })();

                data.selectedChartDataSource[type] = dataSourceByTimeunit;
            }

            function setChartOptions(type) {
                var opt = data;
                var chartSetting = (function() {
                    var setting = {};
                    $.each(data.uiTimeunit[type], function(i, unit) {
                        if (unit.name === data.currentTimeunitForChart[type]) {
                            setting.minPeriod = unit.chartPeriod;
                            //setting.categoryBalloonDateFormat = unit.chartCategoryBalloonDateFormat;
                            return false;
                        }
                    });
                    return setting;
                })();

                var chartOptions = {
                    //data setting
                    dataProvider: angular.copy(data.selectedChartDataSource[type]),
                    graphs: angular.copy(data.selectedChartGraphs[type]),

                    //config setting
                    type: "serial",
                    theme: AuthTokenFactory.getTheme(),
                    colors: opt.colorTheme[type],
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
                            return setChartTimeunitLabel(time, type);
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

                data.uiChart[type].options = chartOptions;
                return chartOptions;
            }

            function setChartForPdfOptions(type) {
                var opt = data;
                var chartSetting = (function() {
                    var setting = {};
                    $.each(data.uiTimeunit[type], function(i, unit) {
                        if (unit.name === data.currentTimeunitForChart[type]) {
                            setting.minPeriod = unit.chartPeriod;
                            //setting.categoryBalloonDateFormat = unit.chartCategoryBalloonDateFormat;
                            return false;
                        }
                    });
                    return setting;
                })();

                var chartOptions = {
                    //data setting
                    dataProvider: angular.copy(data.selectedChartDataSource[type]),
                    graphs: angular.copy(data.selectedChartGraphs[type]),

                    //config setting
                    type: "serial",
                    theme: 'white',
                    colors: opt.colorTheme[type],
                    titles: [{
                        text: i18n("occurrence-vs-time")
                            //size: 15
                    }],
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
                            return setChartTimeunitLabel(time, type);
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

                data.uiChartForPdf[type].options = chartOptions;
                return chartOptions;
            }

            function setBarData(type) {

            }

            function setBarOptions(type) {
                var opt = data;
                var chartOptions = {
                    //data setting
                    dataProvider: angular.copy(data.selectedBarDataSource[type]),
                    graphs: angular.copy(data.selectedBarGraphs[type]),

                    //config setting
                    theme: AuthTokenFactory.getTheme(),
                    colors: opt.colorTheme[type],
                    categoryField: "itemName",
                    "type": "serial",
                    "rotate": true,
                    "startDuration": 1,
                    "categoryAxis": {
                        //firstDayOfWeek: kupOpt.dateOfWeekStart,
                        "gridPosition": "start"
                    },
                    "trendLines": [],

                    "guides": [],
                    "valueAxes": [{
                        "stackType": "100%",
                        "title": ""
                    }],
                    "allLabels": [],
                    "balloon": {},
                    legend: {
                        useGraphSettings: false,
                        valueWidth: 0,
                        valueFunction: function() {
                            return "";
                        }
                    },
                };

                data.uiBar[type].options = chartOptions;
                return chartOptions;
            }

            function setPieData(type) {
                var opt = data;
                var dataSource = angular.copy(opt.selectedDataSource[type]);
                var dataSourceForPie = {};

                $.each(dataSource, function(i, ds) {
                    $.each(ds, function(key, val) {
                        if (key === 'time') {
                            return true;
                        }
                        dataSourceForPie[key] = dataSourceForPie[key] + val || val;
                    });
                });

                $.each(dataSourceForPie, function(key, val) {
                    dataSourceForPie.total = dataSourceForPie.total + val || val;
                });

                opt.selectedPieDataSource[type] = dataSourceForPie;
            }

            function setPieOptions(type) {
                var opt = data;
                var theme = angular.copy(MorrisChartTheme[opt.pieTheme]);

                var dataSource = opt.selectedPieDataSource[type];
                var isEmpty = (function() {
                    var isEmpty = true;
                    $.each(dataSource, function(key, val) {
                        if (val !== 0) {
                            isEmpty = false;
                            return false;
                        }
                    });
                    return isEmpty;
                })();

                var dataList = (function(data) {
                    var list = [],
                        index = 0;
                    $.each(data, function(k, v) {
                        if (k === 'total') {
                            return true;
                        };
                        list[index] = {
                            label: i18n(k),
                            value: v
                        };
                        index++;
                    });
                    return list;
                })(dataSource);

                var config = (isEmpty) ? {} : {
                    data: dataList,
                    colors: opt.colorTheme[type],
                    labelColor: theme.labelColor,
                    formatter: function(val, data) {
                        var total = 0;
                        $.each(this.data, function(i, info) {
                            total += info.value;
                        });
                        val = $filter('percentage')(val / total, 2);
                        return val;
                    },
                    fontFamily: "Muli",
                    backgroundColor: theme.backgroundColor,
                    resize: true
                };
                data.uiPie[type].options = config;
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
                    "base-unit": baseUnit
                };

                return KupApiService.exportDoc(param, 'exportaggregatedcsvreport');
            }

            function exportVcaSecurityPdfApi() {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;
                var apiServerUrl = AuthTokenFactory.getApiRootUrl();

                //get svg 
                var svgData1 = (function() {
                    var svgData = [];
                    $.each($('#profillingPieGender').find('svg'), function(i, el) {
                        svgData.push("<svg version='1.1' width='" + $(el).css('width') + "' height='" + $(el).css('height') + "'>" + $(el).html() + "</svg>");
                    });

                    $.each($('#profillingPieAge').find('svg'), function(i, el) {
                        svgData.push("<svg version='1.1' width='" + $(el).css('width') + "' height='" + $(el).css('height') + "'>" + $(el).html() + "</svg>");
                    });

                    $.each($('#profillingPieEmotion').find('svg'), function(i, el) {
                        svgData.push("<svg version='1.1' width='" + $(el).css('width') + "' height='" + $(el).css('height') + "'>" + $(el).html() + "</svg>");
                    });
                    return svgData;
                })();

                var svgData2 = (function() {
                    var svgData = [];
                    var chartPdfGender = window.AmCharts.makeChart('profilingChartForPdfGender', setChartForPdfOptions('gender'));
                    var chartPdfAge = window.AmCharts.makeChart('profilingChartForPdfAge', setChartForPdfOptions('age'));
                    var chartPdfEmotion = window.AmCharts.makeChart('profilingChartForPdfEmotion', setChartForPdfOptions('emotion'));

                    $.each($('#profilingChartForPdfGender').find('svg'), function(i, el) {
                        svgData.push("<svg version='1.1' width='" + $(el).css('width') + "' height='" + $(el).css('height') + "'>" + $(el).html() + "</svg>");
                    });

                    $.each($('#profilingChartForPdfAge').find('svg'), function(i, el) {
                        svgData.push("<svg version='1.1' width='" + $(el).css('width') + "' height='" + $(el).css('height') + "'>" + $(el).html() + "</svg>");
                    });

                    $.each($('#profilingChartForPdfEmotion').find('svg'), function(i, el) {
                        svgData.push("<svg version='1.1' width='" + $(el).css('width') + "' height='" + $(el).css('height') + "'>" + $(el).html() + "</svg>");
                    });
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
                    "svg-string1": svgData1.join(""),
                    "svg-string2": svgData2.join(""),
                    "report-info": angular.toJson(reportInfo),
                };
                return KupApiService.exportDoc(param, 'exportaudienceprofilingpdf');
            }

            function setChartTimeunitLabel(dateFormat, type) {
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
                if (data.currentTimeunitForChart[type] == 'hours') {
                    dateText = moment(time).format(kupOpt.default.formatHourly);
                    // dateText = time.toLocaleDateString(dateLang, dateOpt) + ", " + time.getHours() + ":00";
                }
                if (data.currentTimeunitForChart[type] == 'days') {
                    dateText = moment(time).format(kupOpt.default.formatDaily);
                    // dateText = time.toLocaleDateString(dateLang, dateOpt);
                }
                if (data.currentTimeunitForChart[type] == 'weeks') {
                    dateText = moment(time).startOf('week').isoWeekday(kupOpt.default.isoWeekStart).format(kupOpt.default.formatWeekly) + " - \n" + moment(time).endOf('week').format(kupOpt.default.formatWeekly);
                    // var getTimeStart = (time.getDay() < weekStart) ?
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() - (7 - weekStart - time.getDay())) :
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() - (time.getDay() - weekStart));
                    // var getTimeEnd = (time.getDay() < weekStart) ?
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() + (weekStart - time.getDay() - 1)) :
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() + (6 - (time.getDay() - weekStart)));
                    // dateText = getTimeStart.toLocaleDateString(dateLang, dateOpt) + " - " + getTimeEnd.toLocaleDateString(dateLang, dateOpt);
                }
                if (data.currentTimeunitForChart[type] == 'months') {
                    dateText = moment(time).format(kupOpt.default.formatMonthly);
                    // dateOpt = {
                    //     month: "short",
                    //     year: "numeric"
                    // };
                    // dateText = time.toLocaleDateString(dateLang, dateOpt);
                }
                if (data.currentTimeunitForChart[type] == 'years') {
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

            function setPieTheme() {
                var opt = data;
                var theme = AuthTokenFactory.getTheme();
                opt.pieTheme = theme;
            }

            function generateChart(type) {
                var opt = data;
                setChartTheme();
                if (type) {
                    setChartData(type);
                    setChartOptions(type);
                } else {
                    $.each(opt.chartTypeList, function(i, chartType) {
                        setChartData(chartType);
                        setChartOptions(chartType);
                    });
                }

            }

            function generateBar(type) {
                var opt = data;
                setChartTheme();
                if (type) {
                    setBarData(type);
                    setBarOptions(type);
                } else {
                    $.each(opt.chartTypeList, function(i, chartType) {
                        setBarData(chartType);
                        setBarOptions(chartType);
                    });
                }
            }

            function generatePie() {
                var opt = data;
                setPieTheme();
                $.each(opt.chartTypeList, function(i, type) {
                    setPieData(type);
                    setPieOptions(type);
                });
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
                
                console.info(opt.apiAnalyticsReportList);
                $timeout(function() {
                    $q.all(opt.requestForReport)
                        .finally(function() {
                            setSelectedData();
                            if (opt.apiAnalyticsReportList.length <= 0) {
                                reports.isSuccessReport(false);
                                dfd.reject();
                                return;
                            }
                            reports.isSuccessReport(true);
                            generateChart();
                            generateBar();
                            generatePie();
                            dfd.resolve();
                        });
                }, 500);
                return dfd.promise;
            }
        });

angular
    .module('kai.reports.profiling')
    .controller('ProfilingController',
        function(
            KupOption,
            RouterStateService, UtilsService, PromiseFactory, AuthTokenFactory,
            ReportsService, ProfilingService,
            DTOptionsBuilder, DTColumnDefBuilder,
            $scope, $timeout, $q, $rootScope
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var reports = ReportsService;
            var profilingCtrl = this;

            //UI controller
            profilingCtrl.data = ProfilingService.data;
            profilingCtrl.fn = {
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
                    profilingCtrl.data.uiNodata.isShow = !reportsOpt.isSuccessReport;
                }, true);

                //watch theme 
                $scope.$watch(function() {
                    return AuthTokenFactory.getTheme();
                }, function(newVal, oldVal) {
                    if (newVal == oldVal) {
                        return false;
                    }
                    ProfilingService.setChartTheme();
                    ProfilingService.generateChart();
                    ProfilingService.generateBar();

                    ProfilingService.setPieTheme();
                    ProfilingService.generatePie();
                }, true);

                $scope.$watch(function() {
                    return reports.data.selectedInstance;
                }, function(newVal, oldVal) {
                    updateUI();
                }, true);
            }

            function updateUI() {
                if (reports.data.selectedInstance.length > 1) {
                    profilingCtrl.data.uiTab = [{
                        name: 'summary',
                        text: 'summary',
                        class: 'kup-summary',
                        isActive: true,
                    }, {
                        name: 'comparison',
                        text: 'comparison',
                        class: 'kup-comparison',
                        isActive: false,
                    }];
                } else {
                    profilingCtrl.data.uiTab = [{
                        name: 'summary',
                        text: 'summary',
                        class: 'kup-summary',
                        isActive: true,
                    }];
                }
            }

            function setTab(tabName) {
                var opt = profilingCtrl.data;
                var tabData = opt.uiTab;
                $.each(tabData, function(i, data) {
                    data.isActive = (tabName === data.name) ? true : false;
                });
                opt.currentTab = tabName;
            }

            function setTimeunitForChart(unitName, type) {
                var opt = profilingCtrl.data;
                var timeunitData = opt.uiTimeunit[type];
                $.each(timeunitData, function(i, data) {
                    data.isActiveForChart = (unitName === data.name) ? true : false;
                });
                opt.currentTimeunitForChart[type] = unitName;
                ProfilingService.generateChart(type);
            }

            function exportCsv(periodType) {
                if (!reports.isSuccessReport()) {
                    notification('error', i18n('please-generate-reports'));
                    return false;
                }
                
                var warningNotify = notification('warning', i18n('exporting-to-csv'), 0);
                ProfilingService.exportAggregatedCsvReportApi(periodType)
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
                ProfilingService.exportVcaSecurityPdfApi()
                    .finally(function() {
                        warningNotify.close();
                    });
            }
        });
