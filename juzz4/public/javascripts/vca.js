var vca = window.vca || {};
vca.chartColors = ["#74A402", "#4395D1", "#E57300", "#A186BE", "#D85171"]; // green,// blue,// orange,// purple,// pink
vca.donutChartColors = ["#0A88E5", "#D85171", "#40A9AF", "#235956", "#F6AE41", "#7F2F44", "#FF6959", "#D8C6AE"];
vca.currentSecurityReportInfo = null;
vca.currentChartbaseUnit = "hours";
vca.deviceListId = null;
vca.channelListId = null;
vca.refreshVcaTable = null;
vca.refreshTimer = null;
vca.peopleCountingChoice = 1;
vca.currentEventType = null;
vca.currentBrowsingReport = null;
vca.reportType = null;
vca.dynamicDivs = [];
vca.donutsChartDivs = [];
vca.areaChartDivs = [];
vca.profilingAreaChartsData = [];
vca.currentActiveCrosssiteProfilingTab = localizeResource("age");
vca.analyticsType = null;
vca.multiSelectDiv = $("#instanceMultiSelectList_taglist").parent();
vca.crosssiteDropdown = [];
vca.historical = [];
vca.active = [];
vca.byGroup = [];
vca.sortedByDate = [];
vca.activeVcas = [];
vca.plpInfo = {};
vca.showHowToImport = false;
vca.hideConversionInfo = false;
vca.userFakePOSDataPref = false;
vca.disableAllPOSCharts = false;
vca._snapshotCache = {};
vca.lookupMap = {};
vca.type = {
    peoplecounting: {
        showDisplayChart: function(id) {
            if (!id) {
                $("#lineChart").show();
                $("#lineChart1").hide();
            }

            if (id === "displayPeopleCounting") {
                $("#lineChart").show();
                $("#lineChart1").hide();
            }

            if (id === "displayAverageOccupancy") {
                $("#lineChart").hide();
                $("#lineChart1").show();
            }
        },
        showDisplayRadio: function(check) {
            if (check) {
                $('#displayRadioGroup').show();
            } else {
                $('#displayRadioGroup').hide();
            }

        },
        getAvgOccData: function(events, fromDate, toDate) {
            var aoFieldName = "avgOccupancy";
            var count = 0;
            //selected tree items,can to any select all,label,device,camera
            var countItem = {};
            var selectedItemDataList = KUP.widget.report.getOpt('selectedItemDataList') || {};
            var selectedValue = (function(selectedItemDataList) {
                var selectedValue = [];
                $.each(selectedItemDataList, function(i, itemData) {
                    if ($.isEmptyObject(itemData)) {
                        return true;
                    }
                    selectedValue.push(itemData);
                });
                return selectedValue;
            })(selectedItemDataList);

            var seriesInfo = [];
            var countList = [];
            countList.push({
                time: fromDate,
                count: 0
            }); // make sure graph starts at
            // fromDate
            countList.push({
                time: toDate,
                count: 0
            }); // and ends at toDate
            var getAvgOcc = function(deviceList, timeString) {
                var avgOccSum = 0;
                //var notZeroCount = 0;
                $.each(deviceList, function(i, cameraList) {
                    $.each(cameraList, function(j, camera) {
                        var timeIndex = kendo.toString(camera.time, 'ddMMyyyyHHmmss');
                        if (timeString === timeIndex) {
                            avgOccSum += camera[aoFieldName + count];
                            //notZeroCount++;
                        }
                    })
                });
                //notZeroCount = notZeroCount || 1;
                //return parseFloat(new Number(avgOccSum / notZeroCount).toFixed(2));
                return avgOccSum;
            };

            $.each(selectedItemDataList, function(uid, itemData) {
                var totalInPerSelection = 0;
                if ($.isEmptyObject(itemData)) {
                    return true;
                }

                //check selected item's type
                if (itemData.type === "label") {
                    var name = itemData.text;
                    var deviceTimeList = {};
                    var avgOccValAryTimeList = {};
                    $.each(itemData.items, function(i, device) {
                        var deviceList = [];
                        $.each(device.items, function(j, camera) {
                            var cameraData = camera;
                            var cameraList = [];
                            $.each(events, function(k, evt) {
                                //var value = parseFloat(new Number(evt.avgOccupancy).toFixed(2));
                                var value = evt.avgOccupancy;
                                if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                                    var countItem = {};
                                    countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                                    //countItem.deviceName = cameraData.deviceName;
                                    //countItem.channelName = cameraData.channelName;
                                    countItem.name = name;
                                    countItem[aoFieldName + count] = value;
                                    countItem.value = value;
                                    cameraList.push(countItem);
                                }
                            });
                            deviceList.push(cameraList);
                        });

                        $.each(deviceList, function(j, cameraList) {
                            $.each(cameraList, function(k, camera) {
                                var timeIndex = kendo.toString(camera.time, 'ddMMyyyyHHmmss');
                                avgOccValAryTimeList[timeIndex] = avgOccValAryTimeList[timeIndex] || [];
                                avgOccValAryTimeList[timeIndex].push(camera['avgOccupancy' + count]);

                                var getAvgOccVal = function() {
                                    var avgVal = 0;
                                    //var count = 0;
                                    $.each(avgOccValAryTimeList[timeIndex], function(l, val) {
                                        avgVal += val;
                                       // count++;
                                    });
                                    //count = count || 1;
                                    //avgVal = avgVal / count;
                                    return avgVal;
                                };

                                deviceTimeList[timeIndex] = deviceTimeList[timeIndex] || {};
                                deviceTimeList[timeIndex].name = name;
                                //deviceTimeList[timeIndex].deviceName = camera.deviceName;
                                deviceTimeList[timeIndex][aoFieldName + count] = getAvgOccVal();
                                deviceTimeList[timeIndex].value = getAvgOccVal();
                                deviceTimeList[timeIndex].time = camera.time;
                                deviceTimeList[timeIndex].itemType = 'label';
                            })
                        })
                    });
                    $.each(deviceTimeList, function(i, deviceList) {
                        countList.push(deviceList);
                    })
                } else if (itemData.type === "device") {
                    var name = itemData.labelName + " - " + itemData.text;
                    var deviceList = []
                    var deviceTimeList = {};
                    $.each(itemData.items, function(i, camera) {
                        var cameraData = camera;
                        var cameraList = [];
                        $.each(events, function(j, evt) {
                            //var value = parseFloat(new Number(evt.avgOccupancy).toFixed(2));
                            var value = evt.avgOccupancy;
                            if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                                var countItem = {};
                                countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                                //countItem.deviceName = cameraData.deviceName;
                                //countItem.channelName = cameraData.channelName;
                                countItem.name = name;
                                countItem[aoFieldName + count] = value;
                                countItem.value = value;
                                cameraList.push(countItem);
                            }
                        });
                        deviceList.push(cameraList);
                    });

                    $.each(deviceList, function(i, cameraList) {
                        $.each(cameraList, function(j, camera) {
                            var timeIndex = kendo.toString(camera.time, 'ddMMyyyyHHmmss');
                            var avgOccVal = getAvgOcc(deviceList, timeIndex);
                            deviceTimeList[timeIndex] = deviceTimeList[timeIndex] || {};
                            deviceTimeList[timeIndex].name = name;
                            //deviceTimeList[timeIndex].deviceName = camera.deviceName;
                            deviceTimeList[timeIndex][aoFieldName + count] = avgOccVal;
                            deviceTimeList[timeIndex].value = avgOccVal;
                            deviceTimeList[timeIndex].time = camera.time;
                            deviceTimeList[timeIndex].itemType = 'device';
                        })
                    })

                    $.each(deviceTimeList, function(i, deviceList) {
                        countList.push(deviceList);
                    })

                } else if (itemData.type === "channel") {
                    var cameraData = itemData,
                        name = cameraData.deviceName + " - " + cameraData.channelName;
                    $.each(events, function(i, evt) {
                        //var value = parseFloat(new Number(evt.avgOccupancy).toFixed(2));
                        var value = evt.avgOccupancy;
                        if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                            var countItem = {};
                            countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                            countItem.name = name;
                            countItem[aoFieldName + count] = value;
                            countItem.value = value;
                            countItem.itemType = 'camera';
                            countList.push(countItem);
                        }
                    });
                }
                seriesInfo.push({
                    "name": name,
                    "field": aoFieldName + count,
                    //"aggregate": "avg",
                    "aggregate": function(values, series, dataItems, category) {
                        var avgVal = 0;
                        var count = 0;
                        var itemType = '';
                        $.each(values, function(i, val) {
                            if (val !== undefined) {
                                itemType = dataItems[i].itemType;
                                avgVal += val;
                                count++;
                            }
                        })
                        count = count || 1;
                        avgVal = Math.round(avgVal / count);

                        if (itemType === 'label') {
                            avgVal = (avgVal < 0) ? 0 : avgVal;
                        } else {
                            avgVal = avgVal;
                        };
                        return avgVal;
                    },
                    "type": "column",
                    //"stack": true,
                });
                count++;
            });

            return {
                seriesInfo: seriesInfo,
                countList: countList
            };
        }
    }
};



vca.states = {
    WAITING: "WAITING",
    RUNNING: "RUNNING",
    NOT_SCHEDULED: "NOT_SCHEDULED",
    DISABLED: "DISABLED",
    ERROR: "ERROR"
}

var _ageChartSeries = [{
    name: localizeResource("below") + " 21",
    field: "ageGroup1",
    aggregate: "sum",
    color: vca.chartColors[0]
}, {
    name: "21-35",
    field: "ageGroup2",
    aggregate: "sum",
    color: vca.chartColors[1]
}, {
    name: "36-55",
    field: "ageGroup3",
    aggregate: "sum",
    color: vca.chartColors[2]
}, {
    name: localizeResource("above") + " 55",
    field: "ageGroup4",
    aggregate: "sum",
    color: vca.chartColors[3]
}];

var _ageLineChartSeries = [{
    name: localizeResource("below") + " 21",
    field: "ageGroup1",
    aggregate: "sum",
    color: vca.donutChartColors[4]
}, {
    name: "21-35",
    field: "ageGroup2",
    aggregate: "sum",
    color: vca.donutChartColors[5]
}, {
    name: "36-55",
    field: "ageGroup3",
    aggregate: "sum",
    color: vca.donutChartColors[6]
}, {
    name: localizeResource("above") + " 55",
    field: "ageGroup4",
    aggregate: "sum",
    color: vca.donutChartColors[7]
}];

var _genderLineChartSeries = [{
    name: localizeResource('male'),
    field: "male",
    aggregate: "sum",
    color: vca.donutChartColors[0]
}, {
    name: localizeResource('female'),
    field: "female",
    aggregate: "sum",
    color: vca.donutChartColors[1]
}];

//add by renzongke for new feature angry
var _emotionLineChartSeries = [{
    name: localizeResource('happy'),
    field: "happy",
    aggregate: "sum",
    color: vca.donutChartColors[2]
}, {
    name: localizeResource('neutral'),
    field: "neutral",
    aggregate: "sum",
    color: vca.donutChartColors[3]
},{
	name: localizeResource('angry'),
	field: "angry",
	aggregate: "sum",
	color: vca.donutChartColors[1]
}];

var _genderChartSeries = [{
    name: localizeResource("male"),
    field: "male",
    aggregate: "sum",
    color: vca.chartColors[1]
}, {
    name: localizeResource("female"),
    field: "female",
    aggregate: "sum",
    color: vca.chartColors[4]
}];

var _emotionChartSeries = [{
    name: localizeResource("happy"),
    field: "happy",
    aggregate: "sum",
    color: vca.chartColors[0]
}, {
    name: localizeResource("neutral"),
    field: "neutral",
    aggregate: "sum",
    color: vca.chartColors[1]
},{
	name: localizeResource("angry"),
	field: "angry",
	aggregate: "sum",
	color: vca.chartColors[2]
}];

vca.profilingCountChart = function(divId, title, seriesInfo, chartData) {

    chartData.sort(function(a, b) {
        return a.time - b.time;
    });

    // $("#" + divId).before($("#vcaChartStepChoices").html());

    // if range is more than 2 days, change stepsize to days
    var rangeMillis = chartData[chartData.length - 1].time - chartData[0].time;
    var twoDaysMillis = (2 * 24 * 60 * 60 * 1000);
    if (rangeMillis > twoDaysMillis) {
        vca.currentChartbaseUnit = "days";
        $(".hourChoice").hide();
        $("#stepDays").click();
    } else {
        vca.currentChartbaseUnit = "hours";
        $(".hourChoice").show();
        $("#stepHours").click();
    }

    $("#" + divId).kendoChart({
        theme: "moonlight",
        dataSource: chartData,
        title: {
            text: title
        },
        legend: {
            visible: true,
            position: "top",
            labels: {
                font: "11px Muli, sans-serif"
            }
        },
        seriesDefaults: {
            type: "column"
        },
        series: seriesInfo,
        valueAxis: {
            line: {
                visible: false
            },
            minorUnit: 1
        },
        categoryAxis: {
            field: "time",
            baseUnit: vca.currentChartbaseUnit,
            labels: {
                rotation: -90,
                // format: "dd-MMM HH:mm",
                timeFormat: "HH:mm",
                font: "11px arial",
                dateFormats: {
                    hours: "dd-MM HH:mm",
                    days: "dd MMM",
                    weeks: "dd MMM",
                    months: "MMM yyyy"
                }
            },
            majorGridLines: {
                visible: true
            }
        },
        tooltip: {
            visible: true,
            format: "{0}",
            template: "#= series.name #: #= value #"
        }
    });

    // stepsize change event
    $("input[name=stepSize]").click(function() {
        var chart = $("#" + divId).data("kendoChart");
        if (chart) {
            chart.options.categoryAxis.baseUnit = this.value;
            vca.currentChartbaseUnit = this.value;
            chart.refresh();
        }
    });
}

vca.percentagePieChart = function(divId, title, data) {
    $("#" + divId).kendoChart({
        theme: "moonlight",
        title: {
            text: title
        },
        legend: {
            position: "left"
        },
        dataSource: {
            data: data
        },
        series: [{
            type: "pie",
            field: "percentage",
            categoryField: "name"
        }],
        seriesDefaults: {
            labels: {
                visible: true,
                template: "#= kendo.format('{0:P}', percentage) #"
            }
        },
        tooltip: {
            visible: true,
            template: "#= category # - #= kendo.format('{0:P}', percentage) #"
        }
    });
}

vca.numberPieChart = function(divId, title, data) {
    $("#" + divId).kendoChart({
        theme: "moonlight",
        dataSource: data,
        title: {
            text: localizeResource(title)
        },
        legend: {
            position: "left",
            labels: {
                font: "11px Muli, sans-serif"
            }
        },
        seriesDefaults: {
            labels: {
                visible: true,
                format: "0"
            }
        },
        series: [{
            type: "pie",
            field: "value",
            categoryField: "name"
        }],
        tooltip: {
            visible: true,
            template: "#= category # : #= value #"
        },
        chartArea: {
            background: "transparent"
        }
    });
}

// normal count chart
vca.createStackCountChart = function(containerId, title, seriesInfo, countList) {

        countList.sort(function(a, b) {
            return a.time - b.time;
        });

        $("#" + containerId).html($("#vcaChartStepChoices").html());

        // if range is more than 2 days, change stepsize to days
        var firstDate = countList[0].time;
        var lastDate = countList[countList.length - 1].time;
        var rangeMillis = lastDate - firstDate;
        var twoDaysMillis = (2 * 24 * 60 * 60 * 1000);

        vca.currentChartbaseUnit = "hours";
        // Draw Chart
        $("#countChart").kendoChart({
            theme: "moonlight",
            chartArea: {
                background: "transparent"
            },
            dataSource: countList,
            title: {
                text: localizeResource('activity-level-zone'),
                font: "16px Muli, sans-serif"
            },
            legend: {
                visible: true,
                position: "bottom",
                padding: 2,
                labels: {
                    font: "12px Muli, sans-serif"
                }
            },
            seriesDefaults: {
                type: "column"
            },
            series: seriesInfo,
            valueAxis: {
                line: {
                    visible: true
                },
                minorUnit: 1,
                title: {
                    text: localizeResource('activity-level'),
                    font: "14px Muli, sans-serif"
                }
            },
            categoryAxis: {
                field: "time",
                baseUnit: vca.currentChartbaseUnit,
                labels: {
                    rotation: -90,
                    // format: "dd-MMM HH:mm",
                    timeFormat: "HH:mm",
                    font: "12px Muli",
                    dateFormats: {
                        hours: "dd-MM HH:mm",
                        days: "dd MMM",
                        weeks: "dd MMM",
                        months: "MMM yyyy"
                    }
                },
                majorGridLines: {
                    visible: true
                }
            },
            tooltip: {
                visible: true,
                format: "{0}",
                template: "#= value #",
                font: "11px Muli, sans-sarif"
            }
        });
        // stepsize change event
        $("input[name=stepSize]").click(function() {
            var chart = $("#countChart").data("kendoChart");
            if (chart) {
                chart.options.categoryAxis.baseUnit = this.value;
                vca.currentChartbaseUnit = this.value;
                chart.refresh();
            }
        });

        if (rangeMillis > twoDaysMillis) {
            $(".hourChoice").hide();
            $("#stepDays").click();
        } else {
            $(".hourChoice").show();
            $("#stepHours").click();
        }
    }
    // normal count chart
vca.createCountChart = function(containerId, title, chartType, countList) {

    countList.sort(function(a, b) {
        return a.time - b.time;
    });

    $("#" + containerId).html($("#vcaChartStepChoices").html());

    // if range is more than 2 days, change stepsize to days
    var firstDate = countList[0].time;
    var lastDate = countList[countList.length - 1].time;
    var rangeMillis = lastDate - firstDate;
    var twoDaysMillis = (2 * 24 * 60 * 60 * 1000);
    if (rangeMillis > twoDaysMillis) {
        vca.currentChartbaseUnit = "days";
        $(".hourChoice").hide();
        $("#stepDays").click();
    } else {
        vca.currentChartbaseUnit = "hours";
        $(".hourChoice").show();
        $("#stepHours").click();
    }

    // Draw Chart
    $("#countChart").kendoChart({
        theme: "moonlight",
        dataSource: countList,
        title: {
            text: title,
            font: "16px Muli, sans-serif"
        },
        legend: {
            visible: false,
            position: "bottom"
        },
        seriesDefaults: {
            type: chartType
        },
        series: [{
            name: "Events",
            field: "count",
            aggregate: "sum"
        }],
        valueAxis: {
            line: {
                visible: false
            },
            minorUnit: 1
        },
        categoryAxis: {
            field: "time",
            baseUnit: vca.currentChartbaseUnit,
            labels: {
                rotation: -90,
                // format: "dd-MMM HH:mm",
                timeFormat: "HH:mm",
                font: "12px Muli",
                dateFormats: {
                    hours: "dd-MM HH:mm",
                    days: "dd MMM",
                    weeks: "dd MMM",
                    months: "MMM yyyy"
                }
            },
            majorGridLines: {
                visible: true
            }
        },
        tooltip: {
            visible: true,
            format: "{0}",
            template: "#= value #",
            font: "11px Muli, sans-sarif"
        },
        seriesClick: vca.onSeriesClick

    });

    // stepsize change event
    $("input[name=stepSize]").click(function() {
        var chart = $("#countChart").data("kendoChart");
        if (chart) {
            chart.options.categoryAxis.baseUnit = this.value;
            vca.currentChartbaseUnit = this.value;
            chart.refresh();
        }
    });

}

vca.onSeriesClick = function(e) {

    if (vca.currentEventType == KupEvent.PEOPLE_COUNTING)
        return;

    var selectedInstance = $("#instanceList").data("kendoDropDownList").dataItem();
    if (selectedInstance == null || selectedInstance.id == "None") {
        utils.popupAlert(localizeResource('no-channel-selected'));
        return;
    } else if (selectedInstance.deviceName == localizeResource('historical')) {
        var selectedDevice = $("#dvcList").data("kendoDropDownList").dataItem();
        if (selectedDevice.name == "None") {
            utils.popupAlert(localizeResource('no-device-selected'))
            return;
        }

        selectedDvcId = selectedDevice.id;
        selectedChannId = $("#channList").data("kendoDropDownList").value();
        if (utils.isNullOrEmpty(selectedChannId)) {
            utils.popupAlert(localizeResource('empty-camera'));
            return;
        }

    } else {
        selectedDvcId = selectedInstance.platformDeviceId;
        selectedChannId = selectedInstance.channelId;
    }
    var deviceDetails = [];
    // deviceManager.attachDeviceDetails(deviceDetails)
    var selectedToDate = $("#end").data("kendoDateTimePicker").value();
    var fromDate = new Date(e.category);
    var toDate = new Date(fromDate);
    if (e.sender._plotArea.axisX.children.length > 1) {
        if (vca.currentChartbaseUnit == "hours") {
            toDate.setHours(toDate.getHours() + 1);
            toDate.setMinutes(00);
            toDate.setSeconds(00);
        } else if (vca.currentChartbaseUnit == "days") {
            toDate.setHours(23, 59, 00);
        } else if (vca.currentChartbaseUnit == "weeks") {
            toDate.setDate(fromDate.getDate() + 6);
            toDate.setHours(23, 59, 00);
        } else if (vca.currentChartbaseUnit == "months") {
            toDate.setMonth(fromDate.getMonth() + 1, 0);
            toDate.setHours(23, 59, 00);
        }
        if (toDate > selectedToDate)
            toDate = $("#end").data("kendoDateTimePicker").value();
    } else {
        fromDate = $("#start").data("kendoDateTimePicker").value();
        toDate = $("#end").data("kendoDateTimePicker").value();
    }
}

// audience profiling
vca.profilingDonutChart = function(containerId, title, countList, labelname) {
    // Draw Chart
    var winId = Math.random().toString(8).slice(2);
    $("#" + containerId).append(
        '<div class="donut-chart ' + labelname + '">' +
        '<div id="' + winId + '"></div>' +
        '<div id="chart-label' + winId + '" class="overlay">' + labelname + '</div>' +
        '</div>'
    );

    vca.dynamicDivs.push(winId);
    vca.donutsChartDivs.push(winId);

    $("#" + winId).kendoChart({
        theme: "bootstrap",
        legend: {
            visible: false
        },
        title: {
            text: title,
            color: "#EAAC00",
            font: "bold 16px Muli, sans-serif"
        },
        chartArea: {
            background: ""
        },
        seriesDefaults: {
            type: "donut",
            startAngle: 180,
            padding: 80,
            holeSize: 60,
        },
        series: countList,
        /* tooltip: {
             visible: true,
             template: "#= dataItem.category #: #= kendo.format('{0:P}', percentage)#",
             font: "12px Muli,sans-serif"
         },*/
        labels: {
            visible: true,
            background: "transparent",
            position: "outsideEnd",
            template: "#= dataItem.category #: #= kendo.format('{0:P}', percentage)#",
            font: "11px Muli, sans-serif"
        },
    });
}

// normal count chart
vca.profilingAreaChart = function(containerId, title, seriesInfo, countList) {
    countList.sort(function(a, b) {
        return a.time - b.time;
    });
    
    console.info("countList:");
    console.info(countList);
    //$("#" + containerId).html($("#vcaStackChartStepChoices").html());

    // if range is more than 2 days, change stepsize to days
    var rangeMillis = countList[countList.length - 1].time - countList[0].time;
    var twoDaysMillis = (2 * 24 * 60 * 60 * 1000);
    if (rangeMillis > twoDaysMillis) {
        vca.currentChartbaseUnit = "days";
        $(".stackChartHourChoice").hide();
        $("#stackChartstepDays").click();
    } else {
        vca.currentChartbaseUnit = "hours";
        $(".stackChartHourChoice").show();
        $("#stackChartstepHours").click();
    }

    var winId = Math.random().toString(8).slice(2);
    $("#" + containerId).append('<div id="' + winId + '" class="vca_report_profiling_chart"></div>');
    vca.dynamicDivs.push(winId);
    vca.areaChartDivs.push(winId);
    // Draw Chart
    $("#" + winId).kendoChart({
        theme: "moonlight",
        dataSource: {
            data: countList
        },
        title: {
            text: title,
            color: "#EAAC00",
            font: "bold 16px Muli, sans-serif"
        },
        legend: {
            position: "bottom",
            labels: {
                font: "11px Muli, sans-serif"
            }
        },
        seriesDefaults: {
            type: "line",
            style: "smooth"
        },
        series: seriesInfo,
        valueAxis: {
            line: {
                visible: true
            }
        },
        categoryAxis: {
            field: "time",
            baseUnit: vca.currentChartbaseUnit,
            labels: {
                rotation: -90,
                // format: "dd-MMM HH:mm",
                timeFormat: "HH:mm",
                font: "12px Muli",
                dateFormats: {
                    hours: "dd-MM HH:mm",
                    days: "dd MMM",
                    weeks: "dd MMM",
                    months: "MMM yyyy"
                }
            },
            majorGridLines: {
                visible: true
            }
        },
        tooltip: {
            visible: true,
            shared: true,
            format: "N0",
            font: "12px Muli, sans-serif"
        }
    });

    // stepsize change event
    $("input[name=stackStepSize]").click(function() {
        var chart = $("#" + winId).data("kendoChart");
        if (chart) {
            chart.options.categoryAxis.baseUnit = this.value;
            vca.currentChartbaseUnit = this.value;
            chart.refresh();
        }
    });
}

vca.createLineChart = function(containerId, title, chartType, seriesInfo, countList) {
    vca.createMultiAxisLineChart(containerId, {
        dataSource: countList,
        title: {
            text: title
        },
        series: seriesInfo,
        valueAxis: {
            line: {
                visible: false
            },
            minorUnit: 1
        }
    });
}


// be able to create multiple charts:
// vca.createMultiAxisLineChart(containerId, lineChart, { chart1 parameters here ... }, { chart1 parameters here ... });
vca.createMultiAxisLineChart = function(containerId, lineChart) {
    var defaultSeries = {
        markers: {
            visible: true
        }
    };
    var defaultValueAxis = {
        title: {
            font: "12px Arial,Helvetica,sans-serif",
            color: "#d9d9d9"
        },
        labels: {
            color: "#d9d9d9"
        }
    };
    var defaultParam = {
        theme: "moonlight",
        chartArea: {
            background: "transparent"
        },
        title: {
            font: "16px Muli, sans-serif"
        },
        seriesDefaults: {
            type: "line",
            style: "smooth"
        },
        series: {},
        legend: {
            position: "bottom",
            labels: {
                font: "11px Muli, sans-serif"
            }
        },
        sort: {
            field: "time",
            dir: "asc"
        },
        valueAxis: {},
        categoryAxis: {
            justified: true,
            field: "time",
            // baseUnitStep: "auto",
            baseUnit: vca.currentChartbaseUnit,
            labels: {
                color: "#d9d9d9",
                rotation: -90,
                // format: "dd-MMM HH:mm",
                timeFormat: "HH:mm",
                font: "11px Muli, sans-serif",
                dateFormats: {
                    hours: "dd-MM HH:mm",
                    days: "dd MMM",
                    weeks: "dd MMM",
                    months: "MMM yyyy"
                }
            },
            majorGridLines: {
                visible: true
            },
            crosshair: {
                visible: true
            }
        },
        tooltip: {
            background: "transparent",
            font: "11px Muli, sans-serif",
            visible: true,
            shared: true,
            format: "{0:N0}",
            sharedTemplate: kendo.template($("#vcaLineChartTooktip").html())
        },
        dataBound: vca.onLineChartDataBound
    };

    var countList = lineChart.dataSource;
    countList.sort(function(a, b) {
        return a.time - b.time;
    });
    $("#" + containerId).html($("#vcaLineChartStepChoices").html());

    // if range is more than 2 days, change stepsize to days
    var rangeMillis = countList[countList.length - 1].time - countList[0].time;
    var twoDaysMillis = (2 * 24 * 60 * 60 * 1000);
    if (rangeMillis > twoDaysMillis) {
        vca.currentChartbaseUnit = "days";
        $(".hourChoice").hide();
        $("#lineChartStepDays").click();
    } else {
        vca.currentChartbaseUnit = "hours";
        $(".hourChoice").show();
        $("#lineChartStepHours").click();
    }

    lineChart.seriesInfo = vca._applyDefaultParam(defaultSeries, lineChart.seriesInfo);
    lineChart.valueAxis = vca._applyDefaultParam(defaultValueAxis, lineChart.valueAxis);

    defaultParam.categoryAxis.baseUnit = vca.currentChartbaseUnit;
    var lineChartParams = $.extend(true, {}, defaultParam, lineChart);
    var lineWidth = $("#lineChart").width() || $("#timeCardWrapper").width();
    $("#lineChart").width(lineWidth).kendoChart(lineChartParams);
    if (arguments.length > 2) {
        for (var i = 2; i < arguments.length; i++) {
            var params = arguments[i];
            params.seriesInfo = vca._applyDefaultParam(defaultSeries, params.seriesInfo);
            params.valueAxis = vca._applyDefaultParam(defaultValueAxis, params.valueAxis);

            var chartParams = $.extend(true, {}, defaultParam, params);
            chartParams.categoryAxis.baseUnit = defaultParam.categoryAxis.baseUnit;

            var $chartDiv = $(document.createElement("div"));
            $("#lineChart").after($chartDiv);
            $chartDiv.attr("id", "lineChart" + (i - 1));
            $chartDiv.kendoChart(chartParams);
        }
    }

    // stepsize change event
    $("input[name=lineStepSize]").click(function() {
        var n = 1;
        var chart = $("#lineChart").data("kendoChart");
        while (chart) {
            chart.options.categoryAxis.baseUnit = this.value;
            vca.currentChartbaseUnit = this.value;
            chart.refresh();

            chart = $("#lineChart" + (n++)).data("kendoChart");
        }
    });

}

vca._applyDefaultParam = function(defaultParam, target) {
    if ($.isArray(target)) {
        var extendArray = [];
        $.each(target, function(i, param) {
            extendArray.push($.extend(true, {}, defaultParam, param));
        });
        return extendArray;
    } else
        return $.extend({}, defaultParam, target);
};

vca.onLineChartDataBound = function(e) {
    vca.sortedByDate = [];
    var chart = $("#lineChart").data("kendoChart");
    var dataItems = chart.options.categoryAxis.dataItems;
    var totalItemsAsPerDate = {};
    $.each(dataItems, function(index, data) {
        if (!utils.isNullOrEmpty(data.deviceName)) {
            if (vca.currentChartbaseUnit == "hours") {
                var date = kendo.toString(data.time, "ddMMyyyyHH");
                date += "0000";
            } else {
                var date = kendo.toString(data.time, "ddMMyyyy");
                date += "000000";
            }
            if (totalItemsAsPerDate[date] == null) {
                totalItemsAsPerDate[date] = [];
                totalItemsAsPerDate[date].date = date;
                totalItemsAsPerDate[date].totalCount = 0;
            }
            totalItemsAsPerDate[date].totalCount = (totalItemsAsPerDate[date].totalCount + parseInt(data.value));
        }
    });
    $.each(totalItemsAsPerDate, function(index, value) {
        vca.sortedByDate.push({
            "date": value.date,
            "aggregate": value.totalCount
        });
    });
    vca.sortedByDate.sort(function(a, b) {
        return b.aggregate - a.aggregate;
    });
}

vca.createAgeChart = function(divId, title, ageData) {
    vca.profilingCountChart(divId, title, _ageChartSeries, ageData);
}

vca.createGenderChart = function(divId, title, genderData) {
    vca.profilingCountChart(divId, title, _genderChartSeries, genderData);
}

vca.createEmotionChart = function(divId, title, emotionData) {
    vca.profilingCountChart(divId, title, _emotionChartSeries, emotionData)
}

vca.generateVcaListPage = function(pageInfo) {
    $("#vcaPageWrapper").append(kendo.template($("#vcaPageTmpl").html())(pageInfo));
    $("#vcaTabStrip").kendoTabStrip({
        animation: {
            open: {
                effects: "fadeIn"
            }
        },
        select: function(e) {
            if ($(e.item).find("> .k-link").text() === localizeResource('events') && $("#vcaEventGrid").html() === "") {
                vca.initEventPage(pageInfo);
            }
        }
    });

    vca.initVcaInstanceTable("vcaInstanceGrid", pageInfo.vcaType);
    // response field selection
    var fieldList = [];
    fieldList.push("time");
    fieldList.push("deviceName");
    fieldList.push("deviceId");
    fieldList.push("channelId");
    datalog.initializeEventTable("vcaEventGrid", pageInfo.eventType, true, fieldList.join());
    $("#vcaTabStrip").show();
}

vca.initVcaInstanceTable = function(divId, analyticsType) {

    var dsVcaInstances = new kendo.data.DataSource({
        transport: {
            read: function(options) {
                listRunningAnalytics("", analyticsType, function(responseData) {
                    if (responseData.result == "ok" && responseData.instances != null) {
                    	console.info(responseData);
                        var instances = responseData.instances;

                        //cache
                        $.each(instances, function(index, inst) {
                            vca.lookupMap[inst.instanceId] = inst;
                        });

                        VcaMgr.attachDetails(instances, options.success);

                    } else {
                        utils.throwServerError(responseData);
                        options.success([]);
                    }
                }, null);
            }
        },
        pageSize: 50,
        group: {
            field: "groupType",
            dir: "desc"
        }
    });

    var vcaGrid = $("#" + divId).kendoGrid({
        dataSource: dsVcaInstances,
        pageable: {
            input: true,
            numeric: false,
            pageSizes: [15, 30, 50],
            refresh: false
        },
        sortable: true,
        selectable: true,
        resizable: true,
        toolbar: kendo.template($("#vcaToolbarTmpl").html()),
        height: function() {
            return $(window).height() - 235 + "px";
        },
        columns: [{
            field: "running",
            title: "&nbsp;",
            width: "30px",
            template: $("#runningStatusTmpl").html()
        }, {
            field: "deviceName",
            title: localizeResource("device-name"),
            width: "250px",
            template: $("#deviceWithStatusTmpl").html()
        }, {
            field: "channelName",
            title: localizeResource("channel"),
            width: "250px",
            template: $("#channelWithStatusTmpl").html()
        }, {
            field: "address",
            title: localizeResource("location"),
            width: "150px"
        }, {
            field: "recurrenceRule",
            title: localizeResource("operating-schedule"),
            template: $("#recurrenceRuleTmpl").html()
        }, {
            field: "enabled",
            title: localizeResource("actions"),
            width: "190px",
            template: $("#actionBtnGroupTmpl").html()
        }, {
            field: "groupType",
            title: localizeResource("type"),
            groupHeaderTemplate: $("#vcaGroupHeaderTmpl").html()
        }]
    }).data("kendoGrid");
    vcaGrid.hideColumn("groupType");

    $("#" + divId + " .k-grid-content").css({
        "overflow-y": "auto"
    });

    $("#nodenameFilter").keyup(filterResults);
    $("#cameraFilter").keyup(filterResults);
    $("#locationFilter").keyup(filterResults);

    $("#vaclrFilter").click(function(e) {
        $("#nodenameFilter").val("");
        $("#cameraFilter").val("");
        $("#locationFilter").val("");
        $("#" + divId).data("kendoGrid").dataSource.filter([]);
    });

    function filterResults() {
        var deviceFilter = {
            logic: "and",
            filters: []
        };
        deviceFilter.filters.push({
            field: "deviceName",
            operator: "contains",
            value: $("#nodenameFilter").val()
        });
        deviceFilter.filters.push({
            field: "channelName",
            operator: "contains",
            value: $("#cameraFilter").val()
        });
        deviceFilter.filters.push({
            field: "address",
            operator: "contains",
            value: $("#locationFilter").val()
        });

        $("#" + divId).data("kendoGrid").dataSource.filter(deviceFilter);
    }

    $("#btnAddAnalytics").click(function(e) {
        vca.openAddAnalytics(analyticsType);
    });


    // refresh grid periodically
    vca.refreshVcaTable = function() {
        vcaGrid.dataSource.read();
        vca.stopVcaGridRefresh();

        vca.refreshTimer = setInterval(function() {
            vcaGrid.dataSource.read();
        }, 5 * 1000);
    }

    vca.refreshVcaTable();
}

vca.stopVcaGridRefresh = function() {
    if (vca.refreshTimer) {
        clearTimeout(vca.refreshTimer);
    }
}

vca.generateSecurityReport = function(vcaEventType, dvcId, channId, fromDate, toDate) {

    // convert to UTC dates
    var fromStr = kendo.toString(utils.convertToUTC(fromDate), "ddMMyyyyHHmmss");
    var toStr = kendo.toString(utils.convertToUTC(toDate), "ddMMyyyyHHmmss");

    vca.currentEventType = vcaEventType;
    getAnalyticsReport("", vcaEventType, null, dvcId, channId, fromStr, toStr, {}, function(resp) {
        if (resp.result == "ok") {
            dbEvents = resp.data;
            if (dbEvents.length == 0) {
                utils.popupAlert(localizeResource("no-records-found"));
                $("#chartContainer").html("");
                $(".vca_report_pdf_exportbtn").hide();
                return;
            }

            var countList = [];
            countList.push({
                time: fromDate,
                count: 0
            }); // make sure graph
            // starts at
            // fromDate
            countList.push({
                time: toDate,
                count: 0
            }); // and ends at
            // toDate

            $.each(dbEvents, function(index, evt) {
                var countItem = {};
                countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date, "yyyy/MM/dd HH:mm:ss"));
                countItem.count = evt.count;
                countList.push(countItem);
            });

            vca.createCountChart("chartContainer", localizeResource("occurrence-vs-time"), "column", countList);
            $(".vca_report_pdf_exportbtn").show();

            deviceManager.WaitForReady(function() {
                var anyEvt = dbEvents[0];
                deviceManager.attachDeviceDetails(anyEvt, null, anyEvt.deviceId, channId);

                vca.currentReportInfo = {
                    "event-type": vcaEventType,
                    "device-name": anyEvt.deviceName,
                    "channel": anyEvt.channelName,
                    "from": kendo.toString(fromDate, kupapi.TIME_FORMAT),
                    "to": kendo.toString(toDate, kupapi.TIME_FORMAT),
                    "total-results": dbEvents.length + ""
                };
            })
        }
    });
}

vca.generateAudienceProfileCharts = function(profilingEvents, fromDate, toDate, callback) {
    var genderChartData = [];
    var ageChartData = [];
    var emotionChartData = [];
    var genderPieData = [];
    var agePieData = [];
    var emotionPieData = [];

    var maleCount = 0;
    var femaleCount = 0;
    var ageGroup1Count = 0;
    var ageGroup2Count = 0;
    var ageGroup3Count = 0;
    var ageGroup4Count = 0;
    var happyCount = 0;
    var neutralCount = 0;
    //add by renzongke for angry
    var angryCount = 0;

    // compile gender, age, emotion data
    $.each(profilingEvents, function(index, evt) {
        var time = utils.convertUTCtoLocal(kendo.parseDate(evt.date, "yyyy/MM/dd HH:mm:ss"));
        var detail = evt;
        
        console.info("evt");
        console.info(detail)
        // check whether id is already used
        // if ($.inArray(details.id, uniqueIdList) != -1) {
        // return true;
        // }
        // uniqueIdList.push(details.id);

        // Gender statistics
        var gChartPt = {};
        // if (details.genderavg < 0.5) {
        // gChartPt.male = 1;
        // gChartPt.female = 0;
        maleCount += evt.male;
        // }
        // else {
        // gChartPt.male = 0;
        // gChartPt.female = 1;
        femaleCount += evt.female;
        // }
        gChartPt.time = time;
        gChartPt.male = evt.male;
        gChartPt.female = evt.female;
        genderChartData.push(gChartPt);

        // Emotion statistics
        var eChartPt = {};
        // if (details.smileavg < 0.5) {
        // eChartPt.happy = 0;
        // eChartPt.neutral = 1;
        neutralCount += evt.neutral;
        // }
        // else {
        // eChartPt.happy = 1;
        // eChartPt.neutral = 0;
        happyCount += evt.happy;
        
        //add by renzongke
        angryCount += evt.angry;
        console.info("angryCount::"+angryCount);
        // }
        eChartPt.time = time;
        eChartPt.happy = evt.happy;
        eChartPt.neutral = evt.neutral;
        eChartPt.angry= evt.angry;
        emotionChartData.push(eChartPt);

        // Age statistics
        var aChartPt = {};
        // if (details.ageavg < 0.5) {
        // aChartPt.ageGroup1 = 1;
        // aChartPt.ageGroup2 = 0;
        // aChartPt.ageGroup3 = 0;
        // aChartPt.ageGroup4 = 0;
        ageGroup1Count += evt.age1;
        // }
        // else if (details.ageavg < 1.5) {
        // aChartPt.ageGroup1 = 0;
        // aChartPt.ageGroup2 = 1;
        // aChartPt.ageGroup3 = 0;
        // aChartPt.ageGroup4 = 0;
        ageGroup2Count += evt.age2;
        // }
        // else if (details.ageavg < 2.5) {
        // aChartPt.ageGroup1 = 0;
        // aChartPt.ageGroup2 = 0;
        // aChartPt.ageGroup3 = 1;
        // aChartPt.ageGroup4 = 0;
        ageGroup3Count += evt.age3;
        // }
        // else {
        // aChartPt.ageGroup1 = 0;
        // aChartPt.ageGroup2 = 0;
        // aChartPt.ageGroup3 = 0;
        // aChartPt.ageGroup4 = 1;
        ageGroup4Count += evt.age4;
        // }
        aChartPt.time = time;
        aChartPt.ageGroup1 = evt.age1;
        aChartPt.ageGroup2 = evt.age2;
        aChartPt.ageGroup3 = evt.age3;
        aChartPt.ageGroup4 = evt.age4;
        ageChartData.push(aChartPt);

    });

    // make sure the chart shows From date till To date
    ageChartData.push({
        time: fromDate,
        ageGroup1: 0,
        ageGroup2: 0,
        ageGroup3: 0,
        ageGroup4: 0
    });
    ageChartData.push({
        time: toDate,
        ageGroup1: 0,
        ageGroup2: 0,
        ageGroup3: 0,
        ageGroup4: 0
    });
    genderChartData.push({
        time: fromDate,
        male: 0,
        female: 0
    });
    genderChartData.push({
        time: toDate,
        male: 0,
        female: 0
    });
    emotionChartData.push({
        time: fromDate,
        happy: 0,
        neutral: 0,
        angry: 0
    });
    emotionChartData.push({
        time: toDate,
        happy: 0,
        neutral: 0,
        angry: 0
    });

    // Gender Piechart Data
    var malePercent = ((maleCount / (maleCount + femaleCount)) * 100);
    var femalePercent = ((femaleCount / (maleCount + femaleCount)) * 100);
    if (malePercent > 0)
        genderPieData.push({
            "name": localizeResource('male'),
            "percentage": malePercent,
            "color": vca.chartColors[1]
        });
    if (femalePercent > 0)
        genderPieData.push({
            "name": localizeResource('female'),
            "percentage": femalePercent,
            "color": vca.chartColors[4]
        });

    // Age Piechart Data
    var ageTotal = ageGroup1Count + ageGroup2Count + ageGroup3Count + ageGroup4Count;
    var g1 = ((ageGroup1Count / ageTotal) * 100);
    var g2 = ((ageGroup2Count / ageTotal) * 100);
    var g3 = ((ageGroup3Count / ageTotal) * 100);
    var g4 = ((ageGroup4Count / ageTotal) * 100);
    if (g1 > 0)
        agePieData.push({
            "name": _ageChartSeries[0].name,
            "percentage": g1,
            "color": vca.chartColors[0]
        });
    if (g2 > 0)
        agePieData.push({
            "name": _ageChartSeries[1].name,
            "percentage": g2,
            "color": vca.chartColors[1]
        });
    if (g3 > 0)
        agePieData.push({
            "name": _ageChartSeries[2].name,
            "percentage": g3,
            "color": vca.chartColors[2]
        });
    if (g4 > 0)
        agePieData.push({
            "name": _ageChartSeries[3].name,
            "percentage": g4,
            "color": vca.chartColors[3]
        });

    // Emotion Piechart Data
    var emotionTotal = happyCount + neutralCount + angryCount;
    var happyPercent = ((happyCount / emotionTotal) * 100);
    var neutralPercent = ((neutralCount / emotionTotal) * 100);
    var angryPercent = ((angryCount / emotionTotal) * 100);
    if (happyPercent > 0)
        emotionPieData.push({
            "name": localizeResource('happy'),
            "percentage": happyPercent,
            "color": vca.chartColors[0]
        });
    if (neutralPercent > 0)
        emotionPieData.push({
            "name": localizeResource('neutral'),
            "percentage": neutralPercent,
            "color": vca.chartColors[1]
        });
    if (angryCount > 0)
        emotionPieData.push({
            "name": localizeResource('angry'),
            "percentage": angryPercent,
            "color": vca.chartColors[2]
        });
    	


    // Draw Charts
    vca.createAgeChart("ageChart", localizeResource('age') + " " + localizeResource('breakdown'), ageChartData);
    vca.percentagePieChart("agePie", localizeResource('age') + " " + localizeResource('percentage'), agePieData);

    vca.createGenderChart("genderChart", localizeResource('gender') + " " + localizeResource('breakdown'), genderChartData);
    vca.percentagePieChart("genderPie", localizeResource('gender') + " " + localizeResource('percentage'), genderPieData);

    vca.createEmotionChart("emotionChart", localizeResource('emotion') + " " + localizeResource('breakdown'), emotionChartData);
    vca.percentagePieChart("emotionPie", localizeResource('emotion') + " " + localizeResource('percentage'), emotionPieData);

    callback();
}

vca.openHelpGuide = function(vcaType) {
    var contentPage = "/content/helpvideo/" + vcaType;
    utils.openPopup(localizeResource("user-guide"), contentPage, null, null, true, function() {

    });
}

vca.initUserDevicesLabels = function(labelListId) {

    var jqlabelListId = "#" + labelListId;

    var multi = $(jqlabelListId).kendoMultiSelect({
        dataSource: {
            transport: {
                read: function(options) {
                	getLabels(function(resp){
                		if(resp.result == "ok")
                		{
                			options.success(resp.labels);
                		}
                		else
                		{
                			options.success([]);
                		}
                	});
                }
            }
        },
        dataTextField: "name",
        dataValueField: "labelId",
        placeholder: localizeResource('filter-labels'),
        animation: {
            close: {
                effects: "fadeOut zoom:out",
                duration: 300
            },
            open: {
                effects: "fadeIn zoom:in",
                duration: 300
            }
        },
        change: vca.initUserDeviceAndChannelDropdownsByLabels
    }).data("kendoMultiSelect");
}

vca.initUserDeviceAndChannelDropdownsByLabels = function(event) {

    var jqDeviceListId = "#" + vca.deviceListId;
    var jqChannelListId = "#" + vca.channelListId;
    var selectedLabels = [];
    var selectedLabelIds = [];
    var deviceListbyLabels = [];
    if (event.sender._dataItems.length > 0) {
        $(jqDeviceListId).kendoDropDownList({
            optionLabel: "None",
            dataTextField: "name",
            dataValueField: "deviceId",
            dataSource: {
                transport: {
                    read: function(options) {
                        selectedLabels = event.sender._dataItems;
                        $.each(selectedLabels, function(index, label) {
                        	selectedLabelIds.push(label.labelId);
                        });
                        
                        $.each(deviceManager.userDevices, function(index, device) {
                        	$.each(device.channelLabels, function(indexCL, channelLabel) {
                        		
                        		//get device's channel labels
                        		for(var i=0; i <channelLabel.labels.length; i++) {
                        			//get ui selected labels
                            		for(var t=0; t <selectedLabelIds.length; t++) {
                            			//if label found
                            			if (channelLabel.labels[i] == selectedLabelIds[t]) {
                                			var isExist = false;
                                			//avoid duplicate list
                                			$.each(deviceListbyLabels, function(indexDev, selectedDevice) {
                                        		if (selectedDevice.id == device.id) {
                                        			isExist = true;
                                        			return false;
                                        		}
                                            });
                                			//push new device is not exists
                                			if (!isExist) {
                                				deviceListbyLabels.push(device);
                                			}
                                		}
                            		}
                        		}
                        		
                            });
                        });
                        options.success(deviceListbyLabels);
                    }
                }
            },
            select: function(e) {
                var deviceItem = this.dataItem(e.item.index());
                var channelList = $(jqChannelListId).data("kendoDropDownList");
                var NodewithSelectedLabel = [];
                if (deviceItem.name != "None") {
                    if (deviceItem.model.capabilities.indexOf("node") != -1) {
                        $.each(deviceItem.node.cameras, function(index, camera) {
                        	$.each(deviceItem.channelLabels, function(indexCL, channelLabel) {
                        		// if channelLabel is that camera
                        		if(channelLabel.channelId == camera.nodeCoreDeviceId){
                        			var isFound = false;
                        			//get device's channel labels
                            		for(var i=0; i <channelLabel.labels.length; i++) {
                            			//get ui selected labels
                                		for(var t=0; t <selectedLabelIds.length; t++) {
                                			//if label found
                                			if (channelLabel.labels[i] == selectedLabelIds[t]) {
                                				isFound = true;
                                    		}
                                		}
                            		}
                            		
                            		if(isFound) {
                            			NodewithSelectedLabel.push(camera);
                            		}
                        		}
                            });
                        });
                        utils.populateNodeNames(NodewithSelectedLabel, vca.channelListId);
                    } else {
                        utils.populateChannels(deviceItem.model.channels, vca.channelListId);
                    }

                    channelList.select(0);
                    channelList.enable(true);
                } else {
                    channelList.enable(false);
                }
            },
            dataBound: function() {
                var deviceList = $(jqDeviceListId).data("kendoDropDownList");
                deviceList.select(0);
            }

        });
    } else {
        var jqDeviceListId = "#" + vca.deviceListId;
        var jqChannelListId = "#" + vca.channelListId;
        $(jqDeviceListId).data("kendoDropDownList").text("");
        $(jqDeviceListId).data("kendoDropDownList").value("");
        $(jqChannelListId).data("kendoDropDownList").text("");
        $(jqChannelListId).data("kendoDropDownList").value("");
        $(jqChannelListId).data("kendoDropDownList").enable(false);
        vca.initUserDeviceAndChannelDropdowns(vca.deviceListId, vca.channelListId);
    }

    $(jqChannelListId).kendoDropDownList({
        dataTextField: "name",
        dataValueField: "nodeCoreDeviceId",
        dataSource: []
    });
}

vca.initUserDeviceAndChannelDropdowns = function(deviceListId, channelListId) {
    vca.deviceListId = deviceListId;
    vca.channelListId = channelListId;
    var jqDeviceListId = "#" + deviceListId;
    var jqChannelListId = "#" + channelListId;
    $(jqDeviceListId).kendoDropDownList({
        optionLabel: "None",
        dataTextField: "name",
        dataValueField: "deviceId",
        dataSource: {
            transport: {
                read: function(options) {
                    deviceManager.WaitForReady(function() {
                        options.success(deviceManager.userDevices);
                    });
                }
            }
        },
        select: function(e) {
            var deviceItem = this.dataItem(e.item.index());
            var channelList = $(jqChannelListId).data("kendoDropDownList");

            if (deviceItem.name != "None") {
                if (deviceItem.model.capabilities.indexOf("node") != -1) {
                    utils.populateNodeNames(deviceItem.node.cameras, channelListId);
                } else {
                    utils.populateChannels(deviceItem.model.channels, channelListId);
                }

                channelList.select(0);
                channelList.enable(true);
            } else {
                channelList.enable(false);
            }
        }
    });

    $(jqChannelListId).kendoDropDownList({
        dataTextField: "name",
        dataValueField: "nodeCoreDeviceId",
        dataSource: []
    });
}

// Use callback to get the master event list
vca.getEventsBySegmentedCalls = function(vcaEventType, dvcId, channId, fromStr, toStr, fields, callback) {

    var totalEventCount = 0;
    var masterList = [];
    var eventsPerCall = 500;
    vca.currentEventType = vcaEventType;
    utils.showLoadingTextOverlay("Retrieving Data ... &nbsp;&nbsp;0%", false);

    // this is a recursive call until skip > totalEventCount
    // the last call is make when take > (total - skip)
    function sendRecursiveRequest(skip, take) {

        getEvents("", vcaEventType, "", skip, take, dvcId, channId, fromStr, toStr, null, null, fields,
            function(responseData) {
                if (responseData.result == "ok" && responseData.events != null) {

                    totalEventCount = parseInt(responseData.totalcount);
                    masterList = masterList.concat(responseData.events);
                } else {
                    utils.hideLoadingOverlay();
                    callback(masterList, dvcId, channId);
                    return;
                }

                skip += take;
                if (skip >= totalEventCount) {
                    callback(masterList, dvcId, channId);
                    utils.hideLoadingOverlay();
                    return;
                }

                // update percent finished
                var currentPercent = (skip / totalEventCount) * 100;
                utils.showLoadingTextOverlay("Retrieving Data ... &nbsp;&nbsp;" + currentPercent.toFixed(0) + "%", false);
                sendRecursiveRequest(skip, take);

                console.log("totalEventCount:" + totalEventCount + " currentCount:" + masterList.length + " skip:" + skip + " take:" + take);

            }, null);
    }

    sendRecursiveRequest(0, eventsPerCall);
};

vca.getPrinterFriendlyChart = function(chartId) {
    var chart = $("#" + chartId).data("kendoChart");
    var svg = null;
    if (chart) {
        // Printer Friendly Theme
        chart.setOptions({
            theme: "blueopal",
            valueAxis: {
                labels: {
                    color: "#000000"
                },
                majorGridLines: {
                    color: "#000000"
                }
            },
            categoryAxis: {
                color: "#000000",
                labels: {
                    color: "#000000",
                },
                majorGridLines: {
                    color: "#000000",
                }
            }
        });
        chart.options.categoryAxis.baseUnit = vca.currentChartbaseUnit;
        chart.refresh();
        var svg = chart.svg();

        // Web UI Theme
        chart.setOptions({
            theme: "moonlight",
            valueAxis: {
                labels: {
                    color: "#d9d9d9"
                },
                majorGridLines: {
                    color: "#3e424d"
                }
            },
            categoryAxis: {
                color: "#d9d9d9",
                labels: {
                    color: "#d9d9d9",
                },
                majorGridLines: {
                    color: "#3e424d",
                }
            }
        });
        chart.options.categoryAxis.baseUnit = vca.currentChartbaseUnit;
        chart.refresh();
    }

    return svg;
}

vca.openAddAnalytics = function(vcaType) {
    var contentPage = "/vca";
    var winTitle = localizeResource("add-analytics");

    switch (vcaType) {
        case analyticsType.AREA_INTRUSION:
            contentPage += "/intrusionadd";
            break;

        case analyticsType.PERIMETER_DEFENSE:
            contentPage += "/pdefenseadd";
            break;

        case analyticsType.AREA_LOITERING:
            contentPage += "/loiteringadd";
            break;

        case analyticsType.OBJECT_COUNTING:
            contentPage += "/objectcountingadd";
            break;

        case analyticsType.VIDEO_BLUR:
            contentPage += "/videobluradd";
            break;

        case analyticsType.TRAFFIC_FLOW:
            contentPage += "/trafficflowadd";
            break;

        case analyticsType.PEOPLE_COUNTING:
            contentPage += "/peoplecountingadd";

        case analyticsType.CROWD_DETECTION:
            contentPage += "/crowdadd";
            break;

        case analyticsType.AUDIENCE_PROFILING:
            contentPage += "/profilingadd";
            break;

        case analyticsType.FACE_INDEXING:
            contentPage += "/faceindexingadd";
            break;

        default:
            utils.popupAlert(localizeResource("unknown-analytics-type"));
            return;
    }

    vca.stopVcaGridRefresh();
    utils.openPopup(winTitle, contentPage, null, null, true, function() {
        vca.clearSketchManager();
        vca.refreshVcaTable();
    });
}

vca.onAddAnalyticsSuccess = function(responseData) {
    if (responseData == null || responseData.result != "ok") {
        utils.throwServerError(responseData);
        return;
    }

    var infoMsg = (kupapi.applicationType == "cloud") ? 'analytics-request-sent' : 'analytics-added';
    utils.slideDownInfo(localizeResource(infoMsg));
    vca.closePopupWindow();
    utils.hideLoadingOverlay();
}

vca.onUpdateAnalyticsSuccess = function(responseData) {
    if (responseData == null || responseData.result != "ok") {
        utils.throwServerError(responseData);
        return;
    }

    var infoMsg = (kupapi.applicationType == "cloud") ? 'analytics-request-sent' : 'configuration-updated';
    utils.slideDownInfo(localizeResource(infoMsg));
    vca.closePopupWindow();
    utils.hideLoadingOverlay();
}

vca.onAnalyticsActionSuccess = function(responseData) {
    if (responseData == null || responseData.result != "ok") {
        utils.throwServerError(responseData);
        return;
    }

    var infoMsg = (kupapi.applicationType == "cloud") ? 'analytics-request-sent' : 'action-successful';
    utils.slideDownInfo(localizeResource(infoMsg));
    vca.refreshVcaTable();
    utils.hideLoadingOverlay();
}

vca.closePopupWindow = function() {
    $(".popup_wrapper").closest(".k-window-content").data("kendoWindow").close();
}

vca.applyVcaAction = function(vcaType, action, instanceId) {
    var apiFunction = null;
    switch (vcaType) {
        case analyticsType.AREA_INTRUSION:
            switch (action) {
                case "activate":
                    apiFunction = activateAreaIntrusion;
                    break;
                case "deactivate":
                    apiFunction = deactivateAreaIntrusion;
                    break;
                case "delete":
                    apiFunction = removeAreaIntrusion;
                    break;
                default:
                    utils.popupAlert(localizeResource("unknown-vca-action"));
                    return;
            }
            break;

        case analyticsType.PERIMETER_DEFENSE:
            switch (action) {
                case "activate":
                    apiFunction = activatePerimeterDefense;
                    break;
                case "deactivate":
                    apiFunction = deactivatePerimeterDefense;
                    break;
                case "delete":
                    apiFunction = removePerimeterDefense;
                    break;
                default:
                    utils.popupAlert(localizeResource("unknown-vca-action"));
                    return;
            }
            break;

        case analyticsType.AREA_LOITERING:
            switch (action) {
                case "activate":
                    apiFunction = activateAreaLoitering;
                    break;
                case "deactivate":
                    apiFunction = deactivateAreaLoitering;
                    break;
                case "delete":
                    apiFunction = removeAreaLoitering;
                    break;
                default:
                    utils.popupAlert(localizeResource("unknown-vca-action"));
                    return;
            }
            break;

        case analyticsType.OBJECT_COUNTING:
            switch (action) {
                case "activate":
                    apiFunction = activateObjectCounting;
                    break;
                case "deactivate":
                    apiFunction = deactivateObjectCounting;
                    break;
                case "delete":
                    apiFunction = removeObjectCounting;
                    break;
                default:
                    utils.popupAlert(localizeResource("unknown-vca-action"));
                    return;
            }
            break;

        case analyticsType.VIDEO_BLUR:
            switch (action) {
                case "activate":
                    apiFunction = activateVideoBlur;
                    break;
                case "deactivate":
                    apiFunction = deactivateVideoBlur;
                    break;
                case "delete":
                    apiFunction = removeVideoBlur;
                    break;
                default:
                    utils.popupAlert(localizeResource("unknown-vca-action"));
                    return;
            }
            break;

        case analyticsType.TRAFFIC_FLOW:
            switch (action) {
                case "activate":
                    apiFunction = activateTrafficFlow;
                    break;
                case "deactivate":
                    apiFunction = deactivateTrafficFlow;
                    break;
                case "delete":
                    apiFunction = removeTrafficFlow;
                    break;
                default:
                    utils.popupAlert(localizeResource("unknown-vca-action"));
                    return;
            }
            break;

        case analyticsType.PEOPLE_COUNTING:
            switch (action) {
                case "activate":
                    apiFunction = activatePeopleCounting;
                    break;
                case "deactivate":
                    apiFunction = deactivatePeopleCounting;
                    break;
                case "delete":
                    apiFunction = removePeopleCounting;
                    break;
                default:
                    utils.popupAlert(localizeResource("unknown-vca-action"));
                    return;
            }
            break;

        case analyticsType.CROWD_DETECTION:
            switch (action) {
                case "activate":
                    apiFunction = activateCrowdDetection;
                    break;
                case "deactivate":
                    apiFunction = deactivateCrowdDetection;
                    break;
                case "delete":
                    apiFunction = removeCrowdDetection;
                    break;
                default:
                    utils.popupAlert(localizeResource("unknown-vca-action"));
                    return;
            }
            break;

        case analyticsType.AUDIENCE_PROFILING:
            switch (action) {
                case "activate":
                    apiFunction = activateAudienceProfiling;
                    break;
                case "deactivate":
                    apiFunction = deactivateAudienceProfiling;
                    break;
                case "delete":
                    apiFunction = removeAudienceProfiling;
                    break;
                default:
                    utils.popupAlert(localizeResource("unknown-vca-action"));
                    return;
            }
            break;

        case analyticsType.FACE_INDEXING:
            switch (action) {
                case "activate":
                    apiFunction = activateFaceIndexing;
                    break;
                case "deactivate":
                    apiFunction = deactivateFaceIndexing;
                    break;
                case "delete":
                    apiFunction = removeFaceIndexing;
                    break;
                default:
                    utils.popupAlert(localizeResource("unknown-vca-action"));
                    return;
            }
            break;

        default:
            utils.popupAlert(localizeResource("unknown-vca-type"));
            return;
    }

    utils.popupConfirm(localizeResource('confirmation'), localizeResource('confirm-analytics-' + action),
        function(choice) {
            if (choice) {
                utils.showLoadingOverlay();
                apiFunction("", instanceId, vca.onAnalyticsActionSuccess, null);
            }
        });
}

vca.openDebugger = function(instanceId) {
    vca.stopVcaGridRefresh();
    var contentPage = "/vca/debug/" + instanceId;
    utils.openPopup(localizeResource('vca-debugger'), contentPage, null, null, true, function() {
        vca.refreshVcaTable();
        debugWin.monitorFPS(false);
    });
}

vca.initReportVcaList = function(analyticsType) {

    var dsReportInstances = new kendo.data.DataSource({
        transport: {
            read: function(options) {
                listRunningAnalytics("", analyticsType, function(responseData) {
                    if (responseData.result == "ok") {

                        deviceManager.WaitForReady(function() {
                            var instances = [];
                            var crossSiteInstances = [];
                            if (responseData.instances != null && responseData.instances.length > 0) {
                                instances = responseData.instances;
                                crossSiteInstances = responseData.instances;

                                //get device details
                                $.each(instances, function(index, inst) {
                                    deviceManager.attachDeviceDetails(inst, inst.platformDeviceId, null, inst.channelId);
                                });
                            } else {
                                $("#otherGroup").show("fast");
                            }

                            vca.initMultiSelectList(crossSiteInstances);
                            var other = {};
                            other.id = 9999;
                            other.deviceName = localizeResource('historical');
                            instances.push(other);
                            options.success(instances);
                        });
                    } else {
                        options.success([]);
                        utils.throwServerError(responseData);
                    }
                }, null);
            }
        }
    });

    $("#instanceList").kendoDropDownList({
        dataTextField: "deviceName",
        dataValueField: "id",
        template: kendo.template($("#reportDropDownTmpl").html()),
        dataSource: dsReportInstances,
        change: function(e) {
            var dataItem = this.dataItem();
            if (dataItem != null && dataItem.deviceName == localizeResource('historical')) {
                $("#otherGroup").show("fast");
            } else {
                $("#otherGroup").hide("fast");
            }
        }
    });
}


vca.initHistoricalMultiSelectOptions = function(selectedOption) {
    kendo.ui.progress($(".k-multiselect-wrap.k-floatwrap"), true);
    if (selectedOption != "-3") {
        if ($('input:checkbox[name=activecamera]').is(':checked')) {
            $("#multiselect_text").text(localizeResource('choose-cameras'));
            vca.reportType = 'normal';
            return vca.activeVcas;
        } else {
            vca.historical = [];
            if (vca.historical.length <= 0) {
                deviceManager.WaitForReady(function() {
                    devices = deviceManager.userDevices;
                    $.each(devices, function(index, device) {
                        if (device.model.capabilities.indexOf("node") != -1) {
                            $.each(device.node.cameras, function(index, deviceCamera) {

                                var data = {};
                                data.platformDeviceId = device.id;
                                data.deviceId = device.deviceId;
                                data.channelId = deviceCamera.nodeCoreDeviceId;
                                data.deviceName = device.name;
                                data.channelName = deviceCamera.name;
                                data._id = utils.getRandomInteger(1000, 99999);
                                vca.historical.push(data);
                            });
                        } else {
                            for (i = 0; i < device.model.channels; i++) {
                                var data = {};
                                data.platformDeviceId = device.id;
                                data.deviceId = device.deviceId;
                                data.channelId = i.toString();
                                data.deviceName = device.name;
                                data.channelName = (i + 1);
                                data._id = utils.getRandomInteger(1000, 99999);
                                vca.historical.push(data);
                            }
                        }
                    });
                });
            }
        }
        kendo.ui.progress($(".k-multiselect-wrap.k-floatwrap"), false);
        $("#multiselect_text").text(localizeResource('choose-cameras'));
        vca.reportType = 'normal';
        return vca.historical;
    } else {
        var uniqueLabels = deviceManager.userDevicesLabels;
        vca.byGroup.length = 0;
        $.each(uniqueLabels, function(index, label) {
            var data = {};
            data.labelName = label;
            data.deviceDetails = deviceManager.labelDeviceDetails[label];
            var deviceId = [];
            var deviceChannelIds = [];
            $.each(deviceManager.labelDeviceDetails[label], function(index, value) {
                var deviceChannelId = utils.combineDeviceChannelIDs(value.deviceId, value.cameras.nodeCoreDeviceId.toString());
                deviceId.push(value.deviceId);
                deviceChannelIds.push(deviceChannelId);
            });
            data.deviceIds = deviceId.toString();
            data.deviceChannelIds = deviceChannelIds.toString();
            data.selectedOption = localizeResource('by-group');
            data._id = utils.getRandomInteger(1000, 99999);
            vca.byGroup.push(data);
        });
        kendo.ui.progress($(".k-multiselect-wrap.k-floatwrap"), false);
        $("#multiselect_text").text(localizeResource('choose-groups'));
        vca.reportType = 'group';
        return vca.byGroup;
    }
}

vca.initMultiSelectList = function(instances) {
    $("#instanceMultiSelectList").kendoMultiSelect({
        dataTextField: "deviceName",
        dataValueField: "_id",
        itemTemplate: kendo.template($("#reportMultiSelectTmpl").html()),
        tagTemplate: kendo.template($("#reportMultiSelectTmpl").html()),
        dataSource: instances
    });
    kendo.ui.progress($(".k-multiselect-wrap.k-floatwrap"), false);
}

vca.generateCrossSiteReport = function(vcaEventType, selectedDevices, selectedValues, fromDate, toDate, groupNames, posNames) {

    var UTCFrom = utils.convertToUTC(fromDate);
    var UTCTo = utils.convertToUTC(toDate);
    var fromStr = currentFromDate = kendo.toString(UTCFrom, "ddMMyyyyHHmmss");
    var toStr = currentToDate = kendo.toString(UTCTo, "ddMMyyyyHHmmss");
    if (selectedDevices[0].platformDeviceId.length > 1) {
        $("#btnLineChartReportExportexl").hide();
    } else {
        $("#btnLineChartReportExportexl").show();
    }
    // response field selection
    var fieldList = [];
    fieldList.push("time");
    fieldList.push("deviceName");
    fieldList.push("deviceId");
    fieldList.push("channelId");
    fieldList.push("data");
    utils.showLoadingOverlay();
    getAnalyticsReport("", vcaEventType, null, selectedDevices[0].platformDeviceId, "", fromStr, toStr, {}, function(resp) {
        if (resp.result == "ok" && resp.data.length > 0) {
            var dbEvents = resp.data;

            if (vcaEventType == KupEvent.PEOPLE_COUNTING) {
                vca.showPeopleCountingContainers();
                vca.processPlpCountingCrossSiteInfo(dbEvents, selectedValues, fromDate, toDate, groupNames, posNames);
            } else if (vcaEventType == KupEvent.PROFILING) {
                vca.donutsChartDivs = [];
                vca.areaChartDivs = [];
                vca.showProfilingContainers();
                vca.processProfilingCrossSiteInfo(dbEvents, selectedValues, fromDate, toDate);
            } else { //security crossite reports
                vca.showGeneralContainers();
                vca.processCrossSiteInfo(vcaEventType, dbEvents, selectedValues, fromDate, toDate);
            }
            utils.hideLoadingOverlay();
        } else {
            vca.hideContainers();
            $.each(vca.dynamicDivs, function(index, divId) {
                var div = document.getElementById(divId);
                if (div) {
                    div.parentNode.removeChild(div);
                }
            });

            timeCard.removeCard();
        }
    });
}

vca.generatePeopleCountingTimeCard = function(peopleCountingData, posData) {
    timeCard.generateCard("timeCardWrapper", timeCard.cardType.COUNTING, timeCard.periodType.WEEKLY, peopleCountingData, posData);
    $(".visual_type_selector").show();
}

vca.showPeopleCountingContainers = function() {
    $(".plpcount_buttom_wrapper").show();
}

vca.showProfilingContainers = function() {
    $("#donutChartContainer").show();
    $("#crosssite_report_container").show();
    $(".profile_event_details_container").show();
}

vca.showGeneralContainers = function() {
    $(".vca_line_chart_report_pdf_exportbtn").show();
    $(".plpcount_buttom_wrapper").show();
}

vca.hideContainers = function() {
    utils.hideLoadingOverlay();
    utils.popupAlert(localizeResource("no-records-found"));

    $(".visual_type_selector").hide();
    $("#timeCardBox").html("");
    $("#lineChartContainer").html("");
    $("#POSImport").html("");
    $("#donutChartContainer").hide();
    $("#crosssite_report_container").hide();
    $(".profile_event_details_container").hide();
    $(".plpcount_buttom_wrapper").hide();
    $(".vca_line_chart_report_pdf_exportbtn").hide();
}

vca.processCrossSiteInfo = function(vcaEventType, events, selectedValue, fromDate, toDate) {
    var seriesInfo = [];
    var eventDetails = [];
    var count = 0;
    var countList = [];
    var totalData = 0;
    countList.push({
        time: fromDate,
        count: 1
    }); // make sure graph starts at
    // fromDate
    countList.push({
        time: toDate,
        count: 1
    }); // and ends at toDate

    //set chart data
    if (vca.reportType == "mix") { //selected tree items,can to any select all,label,device,camera
        var countItem = {},
            selectedItemDataList = KUP.widget.report.getOpt('selectedItemDataList') || {},
            selectedValue = (function(selectedItemDataList) {
                var selectedValue = [];
                $.each(selectedItemDataList, function(i, itemData) {
                    if ($.isEmptyObject(itemData)) {
                        return true;
                    }
                    selectedValue.push(itemData);
                });
                return selectedValue;
            })(selectedItemDataList);
        $.each(selectedItemDataList, function(uid, itemData) {
            var totalInPerSelection = 0;

            if ($.isEmptyObject(itemData)) {
                return true;
            }

            //check selected item's type
            if (itemData.type === "label") {
                var name = itemData.text,
                    deviceTimeList = {};
                $.each(itemData.items, function(i, device) {
                    var deviceList = [];
                    $.each(device.items, function(j, camera) {
                        var cameraData = camera;
                        var cameraList = [];
                        $.each(events, function(k, evt) {
                            if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                                var countItem = {};
                                countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                                countItem.deviceName = cameraData.deviceName;
                                countItem.channelName = cameraData.channelName;
                                countItem.name = name;
                                countItem["count" + count] = parseInt(evt.count, 10);
                                countItem.value = parseInt(evt.count, 10);
                                cameraList.push(countItem);

                                totalData += parseInt(evt.count, 10);
                                totalInPerSelection += parseInt(evt.count, 10);
                            }
                        });
                        deviceList.push(cameraList);
                    });

                    $.each(deviceList, function(j, cameraList) {
                        $.each(cameraList, function(j, camera) {
                            var timeIndex = kendo.toString(camera.time, 'ddMMyyyyHHmmss');
                            if (deviceTimeList[timeIndex]) {
                                deviceTimeList[timeIndex]["count" + count] += camera["count" + count];
                                deviceTimeList[timeIndex].value += camera.value;
                            } else {
                                deviceTimeList[timeIndex] = {};
                                deviceTimeList[timeIndex].name = name;
                                deviceTimeList[timeIndex].deviceName = camera.deviceName;
                                deviceTimeList[timeIndex]["count" + count] = camera["count" + count];
                                deviceTimeList[timeIndex].value = camera.value;
                                deviceTimeList[timeIndex].time = camera.time;
                            }
                        })
                    })
                });
                $.each(deviceTimeList, function(i, deviceList) {
                    countList.push(deviceList);
                })
            } else if (itemData.type === "device") {
                var name = itemData.labelName + " - " + itemData.text;
                var deviceList = [],
                    deviceTimeList = {};
                $.each(itemData.items, function(i, camera) {
                    var cameraData = camera;
                    var cameraList = [];
                    $.each(events, function(j, evt) {
                        if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                            var countItem = {};
                            countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                            countItem.deviceName = cameraData.deviceName;
                            countItem.channelName = cameraData.channelName;
                            countItem.name = name;
                            countItem["count" + count] = parseInt(evt.count, 10);
                            countItem.value = parseInt(evt.count, 10);
                            cameraList.push(countItem);

                            totalData += parseInt(evt.count, 10);
                            totalInPerSelection += parseInt(evt.count, 10);
                        }
                    });
                    deviceList.push(cameraList);
                });

                $.each(deviceList, function(i, cameraList) {
                    $.each(cameraList, function(j, camera) {
                        var timeIndex = kendo.toString(camera.time, 'ddMMyyyyHHmmss');
                        if (deviceTimeList[timeIndex]) {
                            deviceTimeList[timeIndex]["count" + count] += camera["count" + count];
                            deviceTimeList[timeIndex].value += camera.value;
                        } else {
                            deviceTimeList[timeIndex] = {};
                            deviceTimeList[timeIndex].name = name;
                            deviceTimeList[timeIndex].deviceName = camera.deviceName;
                            deviceTimeList[timeIndex]["count" + count] = camera["count" + count];
                            deviceTimeList[timeIndex].value = camera.value;
                            deviceTimeList[timeIndex].time = camera.time;
                        }
                    })
                })

                $.each(deviceTimeList, function(i, deviceList) {
                    countList.push(deviceList);
                })

            } else if (itemData.type === "channel") {
                var cameraData = itemData,
                    name = cameraData.deviceName + " - " + cameraData.text;
                $.each(events, function(i, evt) {
                    if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                        var countItem = {};
                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                        countItem.deviceName = cameraData.deviceName;
                        countItem.channelName = cameraData.channelName;
                        countItem.name = name;
                        countItem["count" + count] = parseInt(evt.count, 10);
                        countItem.value = parseInt(evt.count, 10);
                        totalData += parseInt(evt.count, 10);
                        totalInPerSelection += parseInt(evt.count, 10);
                        countList.push(countItem);
                    }
                });
            }
            //set chart data
            eventDetails.push({
                "name": name,
                "totalPerSelection": totalInPerSelection
            });
            //if (showPosSales)
            //  name = localizeResource("no-people-in");
            if (totalInPerSelection > 0) {
                seriesInfo.push({
                    "name": name,
                    "field": "count" + count,
                    "aggregate": "sum"
                });
            } else {
                //seriesInfo.push({"name": name, "field": "count" + count });
                seriesInfo.push({
                    "name": name,
                    "field": "count" + count,
                    "aggregate": "sum"
                });
            }
            count++;
        });
    } else { // reportType is 'normal' or 'group'
        $.each(selectedValue, function(index, value) {
            var totalPerSelection = 0;
            var name = "";
            if (vca.reportType == "normal") {
                name = value.deviceName + " - " + value.channelName;
            } else {
                name = value.labelName;
            }
            $.each(events, function(index, evt) {
                if (vca.reportType == "normal") {
                    if ((evt.deviceId == value.coreDeviceId || evt.deviceId == value.deviceId) && evt.channelId == value.channelId) {
                        var countItem = {};
                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                        countItem.deviceName = value.deviceName;
                        countItem.channelName = value.channelName;
                        countItem.name = value.channelId;
                        countItem["count" + count] = evt.count;
                        countItem.value = evt.count;
                        totalPerSelection += evt.count;
                        totalData += evt.count;
                        countList.push(countItem);
                    }
                } else {
                    var deviceChannelId = utils.combineDeviceChannelIDs(evt.deviceId, evt.channelId);
                    if (value.deviceChannelIds.indexOf(deviceChannelId) != -1) {
                        var countItem = {};
                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                        countItem.deviceName = value.labelName;
                        countItem.channelName = value.channelName;
                        countItem.name = value.channelId;
                        countItem["count" + count] = evt.count;
                        countItem.value = evt.count;
                        totalPerSelection += evt.count;
                        totalData += evt.count;
                        countList.push(countItem);
                    }
                }
            });

            eventDetails.push({
                "name": name,
                "totalPerSelection": totalPerSelection
            });
            if (totalPerSelection > 0) {
                seriesInfo.push({
                    "name": name,
                    "field": "count" + count,
                    "aggregate": "sum"
                });
            } else {
                seriesInfo.push({
                    "name": name,
                    "field": "count" + count,
                    "aggregate": "sum"
                });
            }
            count++;
        });
    }

    if (countList.length <= 2) {
        vca.hideContainers();
        return;
    }
    vca.showGeneralContainers();
    
    vca.createLineChart("lineChartContainer",
    	localizeResource("occurrence-vs-time"), "line", seriesInfo, countList);

    vca.reportBottomAnalysisPortion(totalData, selectedValue.length, eventDetails);

    var selectedDevices = [];
    $.each(seriesInfo, function(index, value) {
        selectedDevices.push(" " + value.name);
    })

    vca.currentReportInfo = {
        "event-type": vcaEventType,
        "site-name": selectedDevices.toString(),
        "from": kendo.toString(fromDate, kupapi.TIME_FORMAT),
        "to": kendo.toString(toDate, kupapi.TIME_FORMAT),
        "total-results": totalData + ""
    };
}

vca.passerByReportBottomAnalysisPortion = function(totalPasserBy, totalVisits, selectedDeviceLength, singleViewDetails, isSingleTab) {

    $("#totalPasserBy").html("");
    $("#avgPasserByOrTotalVisit").html("");
    
    $("#totalPasserBy").text(kendo.toString(totalPasserBy, "n0"));
    if(isSingleTab){
    	//single view -- display total people count
    	$("#avgPasserByOrTotalVisit").text(kendo.toString(totalVisits, "n0"));
    	$(".avgPasserByOrTotalVisit_title").html(localizeResource("total-visits"));
    	$(".per_site_description").text(localizeResource("plp-visit"));
        $("#totalCaptureRate").text(kendo.toString(singleViewDetails.avgCaptureRate, "n0") + "%");
        $("#bestCaptureRate").text(kendo.toString(singleViewDetails.bestCaptureRate, "n0") + "%");
        $("#bestCaptureRateDate").text("");
        //dont show capture rate, if totalPasserBy is zero
        if(totalPasserBy > 0){
        	if(vca.currentChartbaseUnit == "hours" && singleViewDetails.bestCaptureRate > 0) {
            	$("#bestCaptureRateDate").text(kendo.toString(singleViewDetails.bestCaptureRateDate, "on dd MMM yyyy h:mm tt"));
            }else if(singleViewDetails.bestCaptureRate > 0){
            	$("#bestCaptureRateDate").text(kendo.toString(singleViewDetails.bestCaptureRateDate, "D"));
            }
        	$("#signleViewPortion").show();
        }else{
        	$("#signleViewPortion").hide();
        }
        
    }else{
    	//multiple view -- display average
        var averagePasserBy = Math.round(totalPasserBy / selectedDeviceLength);
        $("#avgPasserByOrTotalVisit").text(kendo.toString(averagePasserBy, "n0"));
        $(".avgPasserByOrTotalVisit_title").html(localizeResource("average-passer-by"));
        $(".per_site_description").text(localizeResource("passer-by-per-site"));
        $("#signleViewPortion").hide();
    }
    
    vca.initTooltip();
}

vca.cachePlpCountingInfo = function(events, selectedValue, fromDate, toDate, groupNames, posNames) {
    vca.plpInfo = {
        events: events,
        selectedValue: selectedValue,
        fromDate: fromDate,
        toDate: toDate,
        groupNames: groupNames,
        posNames: posNames
    }
}

vca.setUserFakePOSDataPref = function(showPosDemo) {
    vca.userFakePOSDataPref = !showPosDemo;
    saveUserprefs("", "", "", "", "", vca.userFakePOSDataPref, function(responseData) {}, function(responseData) {
        console.log(responseData.reason);
    });
}

vca.toggleConversionRatio = function(showPosDemo) {
    vca.setUserFakePOSDataPref(showPosDemo);
    vca.processPlpCountingCrossSiteInfo(vca.plpInfo.events,
        vca.plpInfo.selectedValue,
        vca.plpInfo.fromDate,
        vca.plpInfo.toDate,
        vca.plpInfo.groupNames,
        vca.plpInfo.posNames,
        vca.userFakePOSDataPref
    );
}

vca.processPlpCountingCrossSiteInfo = function(events, selectedValue, fromDate, toDate, groupNames, posNames, hideConversionInfo) {
    vca.cachePlpCountingInfo(events, selectedValue, fromDate, toDate, groupNames, posNames);
    vca.hideConversionInfo = hideConversionInfo;
    var seriesInfo = [];
    var eventDetails = [];
    var count = 0;
    var totalPlpVisit = 0;
    var countList = [];
    var filterEvents = [];
    var siteName = $.isArray(groupNames) && groupNames.length > 0 ? groupNames[0] : "";
    var showPosSales = groupNames.length == 1 ? (vca.disableAllPOSCharts == true ? false : true) : false;
    countList.push({
        time: fromDate,
        count: 0
    }); // make sure graph starts at
    // fromDate
    countList.push({
        time: toDate,
        count: 0
    }); // and ends at toDate

    //set chart data
    if (vca.reportType == "mix") { //selected tree items,can to any select all,label,device,camera
        var countItem = {},
            selectedItemDataList = KUP.widget.report.getOpt('selectedItemDataList') || {},
            selectedValue = (function(selectedItemDataList) {
                var selectedValue = [];
                $.each(selectedItemDataList, function(i, itemData) {
                    if ($.isEmptyObject(itemData)) {
                        return true;
                    }
                    selectedValue.push(itemData);
                });
                return selectedValue;
            })(selectedItemDataList);
        $.each(selectedItemDataList, function(uid, itemData) {
            var totalInPerSelection = 0;

            if ($.isEmptyObject(itemData)) {
                return true;
            }

            //check selected item's type
            if (itemData.type === 'label') {
                var name = itemData.text,
                    deviceTimeList = {};
                $.each(itemData.items, function(i, device) {
                    var deviceList = [];
                    $.each(device.items, function(j, camera) {
                        var cameraData = camera;
                        var cameraList = [];
                        $.each(events, function(k, evt) {
                            if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                                var countItem = {};
                                countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                                countItem.deviceName = device.text;
                                countItem.channelName = cameraData.text;
                                countItem.name = name;
                                countItem["count" + count] = parseInt(evt.in, 10);
                                countItem.value = parseInt(evt.in, 10);
                                cameraList.push(countItem);

                                totalPlpVisit += parseInt(evt.in, 10);
                                totalInPerSelection += parseInt(evt.in, 10);
                                filterEvents.push(evt);
                            }
                        });
                        deviceList.push(cameraList);
                    });

                    $.each(deviceList, function(j, cameraList) {
                        $.each(cameraList, function(j, camera) {
                            var timeIndex = kendo.toString(camera.time, 'ddMMyyyyHHmmss');
                            if (deviceTimeList[timeIndex]) {
                                deviceTimeList[timeIndex]["count" + count] += camera["count" + count];
                                deviceTimeList[timeIndex].value += camera.value;
                            } else {
                                deviceTimeList[timeIndex] = {};
                                deviceTimeList[timeIndex].name = name;
                                deviceTimeList[timeIndex].deviceName = camera.deviceName;
                                deviceTimeList[timeIndex]["count" + count] = camera["count" + count];
                                deviceTimeList[timeIndex].value = camera.value;
                                deviceTimeList[timeIndex].time = camera.time;
                            }
                        })
                    })
                });
                $.each(deviceTimeList, function(i, deviceList) {
                    countList.push(deviceList);
                })
            } else if (itemData.type === 'device') {
                var name = itemData.labelName + " - " + itemData.text;
                var deviceList = [],
                    deviceTimeList = {};
                $.each(itemData.items, function(i, camera) {
                    var cameraData = camera;
                    var cameraList = [];
                    $.each(events, function(j, evt) {
                        if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                            var countItem = {};
                            countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                            countItem.deviceName = itemData.text;
                            countItem.channelName = cameraData.text;
                            countItem.name = name;
                            countItem["count" + count] = parseInt(evt.in, 10);
                            countItem.value = parseInt(evt.in, 10);
                            cameraList.push(countItem);

                            totalPlpVisit += parseInt(evt.in, 10);
                            totalInPerSelection += parseInt(evt.in, 10);
                            filterEvents.push(evt);
                        }
                    });
                    deviceList.push(cameraList);
                });

                $.each(deviceList, function(i, cameraList) {
                    $.each(cameraList, function(j, camera) {
                        var timeIndex = kendo.toString(camera.time, 'ddMMyyyyHHmmss');
                        if (deviceTimeList[timeIndex]) {
                            deviceTimeList[timeIndex]["count" + count] += camera["count" + count];
                            deviceTimeList[timeIndex].value += camera.value;
                        } else {
                            deviceTimeList[timeIndex] = {};
                            deviceTimeList[timeIndex].name = name;
                            deviceTimeList[timeIndex].deviceName = camera.deviceName;
                            deviceTimeList[timeIndex]["count" + count] = camera["count" + count];
                            deviceTimeList[timeIndex].value = camera.value;
                            deviceTimeList[timeIndex].time = camera.time;
                        }
                    })
                })

                $.each(deviceTimeList, function(i, deviceList) {
                    countList.push(deviceList);
                })

            } else if (itemData.type === 'channel') {
                var cameraData = itemData,
                    name = cameraData.deviceName + " - " + cameraData.text;
                $.each(events, function(i, evt) {
                    if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                        var countItem = {};
                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                        countItem.deviceName = name.split("-")[0] || '';
                        countItem.channelName = name.split("-")[1] || '';
                        countItem.name = name;
                        countItem["count" + count] = parseInt(evt.in, 10);
                        countItem.value = parseInt(evt.in, 10);
                        totalPlpVisit += parseInt(evt.in, 10);
                        totalInPerSelection += parseInt(evt.in, 10);
                        countList.push(countItem);
                        filterEvents.push(evt);
                    }
                });
            }
            //set chart data
            eventDetails.push({
                "name": name,
                "totalPerSelection": totalInPerSelection
            });
            //if (showPosSales)
            //  name = localizeResource("no-people-in");
            if (totalInPerSelection > 0) {
                seriesInfo.push({
                    "name": name,
                    "field": "count" + count,
                    "aggregate": "sum"
                });
            } else {
                //seriesInfo.push({"name": name, "field": "count" + count });
                seriesInfo.push({
                    "name": name,
                    "field": "count" + count,
                    "aggregate": "sum"
                });
            }
            count++;
        });
    } else { // reportType is 'normal' or 'group'
        $.each(selectedValue, function(index, value) {
            var totalInPerSelection = 0;
            var name = "";
            if (vca.reportType == "normal") {
                name = value.deviceName + " - " + value.channelName;
            } else {
                name = value.labelName;
            }
            $.each(events, function(index, evt) {
                if (vca.reportType == "normal") {
                    if (evt.deviceId == value.deviceId && evt.channelId == value.channelId) {
                        var countItem = {};
                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                        countItem.deviceName = value.deviceName;
                        countItem.channelName = value.channelName;
                        countItem.name = value.channelId;
                        countItem["count" + count] = parseInt(evt.in);
                        countItem.value = parseInt(evt.in);
                        totalPlpVisit += parseInt(evt.in);
                        totalInPerSelection += parseInt(evt.in);
                        countList.push(countItem);
                        filterEvents.push(evt);
                    }
                } else {
                    var deviceChannelId = utils.combineDeviceChannelIDs(evt.deviceId, evt.channelId);
                    if (value.deviceChannelIds.indexOf(deviceChannelId) !== -1) {
                        var countItem = {};
                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                        countItem.deviceName = value.labelName;
                        countItem.channelName = value.channelName;
                        countItem.name = value.channelId;
                        countItem["count" + count] = parseInt(evt.in);
                        countItem.value = parseInt(evt.in);
                        countItem.plpCounting = parseInt(evt.in);
                        totalPlpVisit += parseInt(evt.in);
                        totalInPerSelection += parseInt(evt.in);
                        countList.push(countItem);
                        filterEvents.push(evt);
                    }
                }
            });

            eventDetails.push({
                "name": name,
                "totalPerSelection": totalInPerSelection
            });
            //        if (showPosSales)
            //            name = localizeResource("no-people-in");
            if (totalInPerSelection > 0) {
                seriesInfo.push({
                    "name": name,
                    "field": "count" + count,
                    "aggregate": "sum"
                });
            } else {
                //seriesInfo.push({"name": name, "field": "count" + count });
                seriesInfo.push({
                    "name": name,
                    "field": "count" + count,
                    "aggregate": "sum"
                });
            }
            count++;
        });
    }

    $("#POSImport").hide();
    $("#POSImport").html('');
    if (vca.hideConversionInfo && !vca.showHowToImport) {
        $("#POSImport").html("<div class='hidden-conversion'><div class='hidden-left-pos'>" +
            "<span class='pos-image'></span>" +
            "<span>" + localizeResource('compare-pos-data') + "</span></div>" +
            "<div><a href='javascript:vca.toggleConversionRatio(true)' class='k-button'>" + localizeResource('show-demo') + "</a>" +
            "<a href='javascript:pos.editPOSData()' class='k-button'>" + localizeResource('click-import-own-data') + "</a></div></div>");
    } else if (!vca.hideConversionInfo && !vca.showHowToImport) {
        $("#POSImport").html("<div class='unhidden-conversion'><div class='unhidden-left-pos'>" +
            "<span class='pos-image'></span>" +
            "<span>" + localizeResource('compare-pos-data') + "</span>" +
            "<span>" + localizeResource('conversion-fake-data') + "</span></div>" +
            "<div><a href='javascript:vca.toggleConversionRatio(false)' class='k-button'>" + localizeResource('hide-conversion') + "</a>" +
            "<a href='javascript:pos.editPOSData()' class='k-button'>" + localizeResource('click-import-own-data') + "</a></div></div>");
    } else if (vca.showHowToImport) {
        $("#POSImport").html("<div class='unhidden-conversion-learn'><div class='unhidden-left-pos'>" +
            "<span class='pos-image'></span>" +
            "<span style=' margin-top: 10px;'>" + localizeResource('learn-how-to-import') + "</span>" +
            "<span></span></div>" +
            "<div><a class='k-button' href='/" + kupBucket + "/content/downloadGuide?guideName=pos_user_guide.pdf'>" + localizeResource('how-to-import') + "</a></div></div>");
        $("#POSImport").show();
    }

    var UTCFrom = utils.convertToUTC(fromDate);
    var UTCTo = utils.convertToUTC(toDate);
    var fromStr = currentFromDate = kendo.toString(UTCFrom, "ddMMyyyyHHmmss");
    var toStr = currentToDate = kendo.toString(UTCTo, "ddMMyyyyHHmmss");
    pos.salesData = pos.constructPOSFakeData(fromDate, toDate);
    var FakeData = vca.constructPOSChartFakeData(countList);
    pos.filterParams = {
        siteName: siteName,
        startDateStr: fromStr,
        endDateStr: toStr
    }
    if (showPosSales) {
        vca.type.peoplecounting.showDisplayRadio(false);
        var conversionList = [];
        var dateDiff = utils.getDateDifference(fromDate, toDate);
        var categoryLength = dateDiff.days < 2 ? 50 : dateDiff.days;
        getPosSalesReport("", fromStr, toStr, siteName, "", function(resp) {
            vca.showHowToImport = false;
            if (resp.result == "ok") {
                if (resp.sales.length <= 0) {
                    $("#POSImport").show();
                    if (vca.hideConversionInfo)
                        return;
                    $.each(FakeData, function(index, salesData) {
                        var localTime = kendo.parseDate(salesData.time, kupapi.TIME_FORMAT);
                        var obj = {
                            name: siteName,
                            sales: {
                                time: kendo.toString(utils.convertToUTC(localTime), kupapi.TIME_FORMAT),
                                count: salesData.count,
                                amount: salesData.amount
                            }
                        }
                        resp.sales.push(obj);
                    });
                }
                var sales = resp.sales;
                countList.sort(function(a, b) {
                    return a.time - b.time;
                });
                sales.sort(function(a, b) {
                    return new Date(a.sales.time) - new Date(b.sales.time);
                });
                var i = 0,
                    j = 0;
                var dataList = [];
                var pushData = function(list, obj) {
                    if (list.length > 0) {
                        var defaultObj = {
                            time: 0,
                            count: 0,
                            amount: 0,
                            receipt: 0
                        };
                        var last = list[list.length - 1];
                        if (last.time - obj.time == 0)
                            list[list.length - 1] = $.extend(defaultObj, last, obj);
                        else
                            list.push($.extend(defaultObj, obj));
                    } else
                        list.push(obj);

                    var data = list[list.length - 1];
                    data.conversion = !isNaN(data.amount) && !isNaN(data.receipt) && data.count > 0 ? Math.round(data.receipt / data.count * 100) / 100 : 0;
                };
                var pushSales = function(time) {
                    for (n = j; n < sales.length; n++) {
                        var salesTime = utils.convertUTCtoLocal(kendo.parseDate(sales[n].sales.time, kupapi.TIME_FORMAT));
                        //var salesTime = utils.convertUTCtoLocal(new Date(sales[n].sales.time));
                        if (time == null || salesTime < time) {
                            pushData(dataList, {
                                time: salesTime,
                                amount: sales[n].sales.amount,
                                receipt: sales[n].sales.count
                            });
                            j++;
                        } else
                            break;
                    }
                }
                for (i = 0; i < countList.length; i++) {
                    var o1 = $.extend({}, countList[i]);
                    var o2;

                    if (dataList.length > 0 && o1.time - dataList[dataList.length - 1].time == 0) {
                        var o = dataList[dataList.length - 1];
                        o1.value += o.count;
                        o1.amount = o.amount;
                        o1.receipt = o.receipt;
                    }

                    pushSales(o1.time);
                    o2 = j < sales.length ? sales[j] : {
                        time: 0,
                        sales: {
                            amount: 0,
                            count: 0
                        }
                    };
                    var salesTime = utils.convertUTCtoLocal(kendo.parseDate(o2.sales.time, kupapi.TIME_FORMAT));
                    var salesTime = utils.convertUTCtoLocal(new Date(o2.sales.time));
                    if (o1.time - salesTime == 0) {
                        o1.amount = o2.sales.amount;
                        o1.receipt = o2.sales.count;
                        j++;
                    }
                    pushData(dataList, {
                        time: o1.time,
                        count: o1.value || o1.count || 0,
                        amount: o1.amount || 0,
                        receipt: o1.receipt || 0,
                    });
                }
                pushSales(null);
                $.each(resp.sales, function(i, obj) {
                    // console.log(obj.sales.time + ", " + obj.sales.amount + ", " +obj.sales.count);
                    countList.push({
                        time: utils.convertUTCtoLocal(kendo.parseDate(obj.sales.time, kupapi.TIME_FORMAT)),
                        amount: obj.sales.amount,
                        receipt: obj.sales.count
                    });
                    //countList.push({time: utils.convertUTCtoLocal(new Date(obj.sales.time)), amount: obj.sales.amount, receipt: obj.sales.count});
                });

                // $.each(dataList, function(i, obj) {
                //  console.log("time = " + obj.time + ", count: " + obj.count + ", amount = " + obj.amount + ", receipt = " + obj.receipt + ", conversion = " + obj.conversion);
                // });

                $("#lineChartContainer").height(550);
                seriesInfo.push({
                    name: localizeResource("sales-amount"),
                    field: "amount",
                    aggregate: "sum",
                    axis: "amount",
                    color: "#024da1",
                    tooltip: {
                    	format:"{0:N2}"
                    }
                });
                seriesInfo.push({
                    name: localizeResource("receipt"),
                    field: "receipt",
                    aggregate: "sum",
                    axis: "receipt",
                    color: "#fa710f",
                });
                vca.createMultiAxisLineChart("lineChartContainer", {
                    dataSource: countList,
                    chartArea: {
                        height: 250,
                    },
                    series: seriesInfo,
                    valueAxis: [{
                        name: "count",
                        title: {
                            text: localizeResource("no-people-in")
                        },
                        line: {
                            visible: false
                        },
                        minorUnit: 2,
                        labels: {
                            template: "#= utils.convertToCurrency(value) #"
                        }
                    }, {
                        name: "amount",
                        title: {
                            text: localizeResource("sales-amount")
                        },
                        labels: {
                            template: "#= utils.convertToCurrency(value) #"
                        }
                    }, {
                        name: "receipt",
                        title: {
                            text: localizeResource("receipt")
                        },
                        labels: {
                            template: "#= utils.convertToCurrency(value) #"
                        }
                    }],
                    legend: {
                        position: "top"
                    },
                    categoryAxis: {
                        axisCrossingValue: [0, categoryLength + 1, categoryLength + 1],
                        majorGridLines: {
                            visible: false
                        }
                    },
                    tooltip: {
                        colors: ["#f6ae40", "#024da1", "#fa710f"]
                    }
                }, {
                    dataSource: dataList,
                    chartArea: {
                        height: 250
                    },
                    seriesDefaults: {
                        type: "column"
                    },
                    categoryAxis: {
                        majorGridLines: {
                            visible: false
                        }
                    },
                    series: [{
                        name: localizeResource("conversion-rate"),
                        field: "conversion",
                        axis: "conversion",
                        color: "#d85171",
                        aggregate: function(values, series, dataItems, category) {
                            var peopleIn = 0;
                            var receipts = 0;
                            for (var i = 0; i < dataItems.length; i++) {
                                var item = dataItems[i];
                                peopleIn += item.count;
                                receipts += item.receipt;
                            }
                            return peopleIn > 0 ? Math.round(receipts / peopleIn * 100) / 100 : 0;
                        },
                        markers: {
                            visible: false
                        }
                    }],
                    legend: {
                        position: "top"
                    },
                    valueAxis: [{
                        name: "conversion",
                        title: {
                            text: localizeResource("conversion-rate")
                        },
                        line: {
                            visible: false
                        },
                        minorUnit: 1,
                        min: 0,
                        max: 1,
                        labels: {
                            format: "{0:p0}"
                        }
                    }],
                    tooltip: {
                        format: "{0:p0}",
                        colors: ["#d85171"]
                    },
                    dataBound: function() {}
                });

                vca.generatePeopleCountingTimeCard(events, resp.sales);
                vca.reportBottomAnalysisPortion(totalPlpVisit, selectedValue.length, eventDetails);

                // var $chart = $(document.createElement("div"));
                // $chart.attr("id", "lineChart2");
                // $("#lineChart1").after($chart);
                // $(".plpcount_buttom_wrapper").css("visibility", "hidden");
                // vca.peopleCountingWithD3("#lineChart2", $("#lineChart1").width(), 607, dataList);
            }
        });
    }

    if (vca.hideConversionInfo) {
        $("#lineChartContainer").height(440);
        if (countList.length <= 2) {
            vca.hideContainers();
            return;
        }
        vca.showPeopleCountingContainers();

        // vca.createLineChart("lineChartContainer",
        //     localizeResource("visitor-vs-time"), "line", seriesInfo, countList);

        var avgOccData = vca.type.peoplecounting.getAvgOccData(events, fromDate, toDate);
        vca.createMultiAxisLineChart("lineChartContainer", {
            dataSource: countList,
            title: {
                text: localizeResource("visitor-vs-time")
            },
            series: seriesInfo,
            valueAxis: {
                line: {
                    visible: false
                },
                minorUnit: 1
            }
        }, {
            "theme": "moonlight",
            "chartArea": {
                "background": "transparent"
            },
            "title": {
                "font": "16px Muli, sans-serif",
                "text": localizeResource("average-occupancy-vs-time")
            },
            // "seriesDefaults": {
            //     "type": "bar",
            //     "style": "smooth"
            // },
            "series": avgOccData.seriesInfo,
            "legend": {
                "position": "bottom",
                "labels": {
                    "font": "11px Muli, sans-serif"
                }
            },
            "sort": {
                "field": "time",
                "dir": "asc"
            },
            "valueAxis": {
                "title": {
                    "font": "12px Arial,Helvetica,sans-serif",
                    "color": "#d9d9d9"
                },
                "labels": {
                    "color": "#d9d9d9"
                },
                "line": {
                    "visible": false
                },
                "minorUnit": 1,
                "axisCrossingValue": [-1e6, -1e6],
            },
            "categoryAxis": {
                "justified": true,
                "field": "time",
                "baseUnit": vca.currentChartbaseUnit,
                "labels": {
                    "color": "#d9d9d9",
                    "rotation": -90,
                    "timeFormat": "HH:mm",
                    "font": "11px Muli, sans-serif",
                    "dateFormats": {
                        "hours": "dd-MM HH:mm",
                        "days": "dd MMM",
                        "weeks": "dd MMM",
                        "months": "MMM yyyy"
                    }
                },
                "majorGridLines": {
                    "visible": true
                },
                "crosshair": {
                    "visible": true
                }
            },
            "tooltip": {
                "background": "transparent",
                "font": "11px Muli, sans-serif",
                "visible": true,
                "shared": true,
                "format": "{0:N0}"
            },
            "dataSource": avgOccData.countList,
            "seriesInfo": {
                "markers": {
                    "visible": true
                }
            }
        });
        $('input[name="displayRadio"]').off('change').on('change', function(event) {
            event.preventDefault();
            var displayId = $(this).attr('id');
            vca.type.peoplecounting.showDisplayChart(displayId);
        });
        vca.type.peoplecounting.showDisplayRadio(true);
        vca.type.peoplecounting.showDisplayChart();
        vca.generatePeopleCountingTimeCard(filterEvents, null);
        vca.reportBottomAnalysisPortion(totalPlpVisit, selectedValue.length, eventDetails);
    }

    var selectedDevices = [];
    $.each(seriesInfo, function(index, value) {
        selectedDevices.push(" " + value.name);
    })
    vca.currentReportInfo = {
        "event-type": KupEvent.PEOPLE_COUNTING,
        "site-name": selectedDevices.toString(),
        "from": kendo.toString(fromDate, kupapi.TIME_FORMAT),
        "to": kendo.toString(toDate, kupapi.TIME_FORMAT),
        "total-results": totalPlpVisit + ""
    };

}

vca.constructPOSChartFakeData = function(countList) {
    var POSFakeData = [];
    var countData = $.extend(true, {}, countList);
    $.each(countData, function(index, data) {
        var randomConversionRate = utils.getRandomDecimal(0.1, 0.4);
        var POSData = {
            time: kendo.toString(data.time, kupapi.TIME_FORMAT),
            count: data.value ? randomConversionRate * data.value : 0,
            amount: data.value ? data.value * utils.getRandomInteger(0, 100) : 0
        }
        POSFakeData.push(POSData);
    });
    return POSFakeData;
}

vca.peopleCountingWithD3 = function(containerId, cWidth, cHeight, dataList) {
    console.log("containerId = " + containerId + ", width = " + cWidth + ", height = " + cHeight);
    var margin = {
            top: 10,
            right: 10,
            bottom: 100,
            left: 40
        },
        margin2 = {
            top: 430,
            right: 10,
            bottom: 20,
            left: 40
        },
        width = cWidth - margin.left - margin.right,
        height = 500 - margin.top - margin.bottom,
        height2 = 500 - margin2.top - margin2.bottom;
    var parseDate = d3.time.format("%b %Y").parse;

    var x = d3.time.scale().range([0, width]),
        x2 = d3.time.scale().range([0, width]),
        y = d3.scale.linear().range([height, 0]),
        y2 = d3.scale.linear().range([height2, 0]);

    var xAxis = d3.svg.axis().scale(x).orient("bottom"),
        xAxis2 = d3.svg.axis().scale(x2).orient("bottom"),
        yAxis = d3.svg.axis().scale(y).orient("left");

    var area = d3.svg.area()
        .interpolate("monotone")
        .x(function(d) {
            return x(d.time);
        })
        .y0(height)
        .y1(function(d) {
            return y(d.receipt);
        });

    var area2 = d3.svg.area()
        .interpolate("monotone")
        .x(function(d) {
            return x2(d.time);
        })
        .y0(height2)
        .y1(function(d) {
            return y2(d.receipt);
        });

    var svg = d3.select(containerId).append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom);

    svg.append("defs").append("clipPath")
        .attr("id", "clip")
        .append("rect")
        .attr("width", width)
        .attr("height", height);

    var focus = svg.append("g")
        .attr("class", "focus")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    var brush = d3.svg.brush()
        .x(x2)
        .on("brush", function brushed() {
            x.domain(brush.empty() ? x2.domain() : brush.extent());
            focus.select(".area").attr("d", area);
            focus.select(".x.axis").call(xAxis);
        });

    var context = svg.append("g")
        .attr("class", "context")
        .attr("transform", "translate(" + margin2.left + "," + margin2.top + ")");

    x.domain(d3.extent(dataList.map(function(d) {
        return d.time;
    })));
    y.domain([0, d3.max(dataList.map(function(d) {
        return d.receipt;
    }))]);
    x2.domain(x.domain());
    y2.domain(y.domain());

    focus.append("path")
        .datum(dataList)
        .attr("class", "area")
        .attr("d", area);

    focus.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);

    focus.append("g")
        .attr("class", "y axis")
        .call(yAxis);

    context.append("path")
        .datum(dataList)
        .attr("class", "area")
        .attr("d", area2);

    context.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height2 + ")")
        .call(xAxis2);

    context.append("g")
        .attr("class", "x brush")
        .call(brush)
        .selectAll("rect")
        .attr("y", -6)
        .attr("height", height2 + 7);


};

vca.reportBottomAnalysisPortion = function(totalVisits, selectedDeviceLength, eventDetails) {

    // show event details (top 5 least active cameras)
    eventDetails.sort(function(a, b) {
        return a.totalPerSelection - b.totalPerSelection;
    });

    $("#leastEventBox").html("");
    $.each(eventDetails, function(index, data) {
        if (index < 5) {
            //East Gatesaasa
            var info = "<div class='device_countdown'>" +
                "<span class='device_name' title='" + data.name + "'>" + data.name + "</span>" +
                "<span>" + kendo.toString(data.totalPerSelection, "n0") + "</span></div>";
            $("#leastEventBox").append(info);
        } else {
            return false;
        }
    });

    // show event details (top 5 most active cameras)
    eventDetails.sort(function(a, b) {
        return b.totalPerSelection - a.totalPerSelection;
    });
    $("#topEventBox").html("");
    $.each(eventDetails, function(index, data) {
        if (index < 5) {
            var info = "<div class='device_countdown'>" +
                "<span class='device_name' title='" + data.name + "'>" + data.name + "</span>" +
                "<span>" + kendo.toString(data.totalPerSelection, "n0") + "</span></div>";
            $("#topEventBox").append(info);
        } else {
            return false;
        }
    });

    $("#totalVisit").html("");
    $("#avgVisit").html("");
    var averageVisit = Math.round(totalVisits / selectedDeviceLength);
    $("#totalVisit").text(kendo.toString(totalVisits, "n0"));
    $("#avgVisit").text(kendo.toString(averageVisit, "n0"));
    $("#highestVisit").text(kendo.toString(vca.sortedByDate[0].aggregate, "n0"));
    $("#lowestVisit").text(kendo.toString(vca.sortedByDate[vca.sortedByDate.length - 1].aggregate, "n0"));
    if (vca.currentChartbaseUnit == "hours") {
        var highestDate = kendo.parseDate(vca.sortedByDate[0].date, "ddMMyyyyHHmmss");
        $("#highestVisitDate").text(localizeResource('on') + " " + kendo.toString(highestDate, "d MMM yyyy  h:mm tt"));
        var totalLength = vca.sortedByDate.length;
        var lowestDate = kendo.parseDate(vca.sortedByDate[totalLength - 1].date, "ddMMyyyyHHmmss");
        $("#lowestVisitDate").text(localizeResource('on') + " " + kendo.toString(lowestDate, "d MMM yyyy h:mm tt"));
    } else {
        var highestDate = kendo.parseDate(vca.sortedByDate[0].date, "ddMMyyyyHHmmss");
        $("#highestVisitDate").text(localizeResource('on') + " " + kendo.toString(new Date(highestDate), "D"));
        var totalLength = vca.sortedByDate.length;
        var lowestDate = kendo.parseDate(vca.sortedByDate[totalLength - 1].date, "ddMMyyyyHHmmss");
        $("#lowestVisitDate").text(localizeResource('on') + " " + kendo.toString(new Date(lowestDate), "D"));
    }

    $("#totalList").html("");

    $.each(eventDetails, function(index, data) {
        $("#totalList").append("<span class='visit_count'>" + kendo.toString(data.totalPerSelection, "n0") + "</span>" + "<span class='visit_description' style='margin-bottom: 52px;'>" + data.name + "</span>");
    });

    vca.initTooltip();
}

vca.initTooltip = function() {
    $(".device_name").kendoTooltip({
        filter: "span",
        width: 120,
        animation: {
            close: {
                effects: "fade:out"
            }
        },
        position: "top"
    }).data("kendoTooltip");
}

vca.processProfilingCrossSiteInfo = function(events, selectedValue, fromDate, toDate) {
    var seriesInfo = [];
    var chartDetails = [];
    var count = 0;
    var countList = [];
    vca.profilingAreaChartsData = [];
    var selectedDevices = [];
    var topIndex = 0;

    // empty previously created dynamic div for donut chart
    $.each(vca.dynamicDivs, function(index, divId) {
        var div = document.getElementById(divId);
        if (div) {
            div.parentNode.removeChild(div);
        }
    });
    //set chart data
    if (vca.reportType == "mix") { //selected tree items,can to any select all,label,device,camera
        var countItem = {},
            selectedItemDataList = KUP.widget.report.getOpt('selectedItemDataList') || {},
            selectedValue = (function(selectedItemDataList) {
                var selectedValue = [];
                $.each(selectedItemDataList, function(i, itemData) {
                    if ($.isEmptyObject(itemData)) {
                        return true;
                    }
                    selectedValue.push(itemData);
                });
                return selectedValue;
            })(selectedItemDataList);

        $.each(selectedItemDataList, function(uid, itemData) {
            if ($.isEmptyObject(itemData)) {
                return true;
            }
            console.info("itemData");
            console.info(itemData)
            var totalAge1 = 0;
            var totalAge2 = 0;
            var totalAge3 = 0;
            var totalAge4 = 0;
            var totalMale = 0;
            var totalFemale = 0;
            var totalHappy = 0;
            var totalNeutral = 0;
            var totalAngry = 0;

            //check items type 
            if (itemData.type === "label") {
                var name = itemData.text,
                    deviceTimeList = {};

                selectedDevices.push(name);
                var stackChartData = [];
                stackChartData.push({
                    time: fromDate,
                    ageGroup1: 0,
                    ageGroup2: 0,
                    ageGroup3: 0,
                    ageGroup4: 0,
                    male: 0,
                    female: 0,
                    happy: 0,
                    neutral: 0,
                    angry: 0,
                    name: name
                });
                stackChartData.push({
                    time: toDate,
                    ageGroup1: 0,
                    ageGroup2: 0,
                    ageGroup3: 0,
                    ageGroup4: 0,
                    male: 0,
                    female: 0,
                    happy: 0,
                    neutral: 0,
                    angry: 0,
                    name: name
                });
                $.each(itemData.items, function(i, device) {
                    var deviceList = [];
                    $.each(device.items, function(j, camera) {
                        var cameraData = camera;
                        var cameraList = [];
                        $.each(events, function(k, evt) {
                            var countItem = {};
                            if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                                countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                                countItem.ageGroup1 = evt.age1;
                                countItem.ageGroup2 = evt.age2;
                                countItem.ageGroup3 = evt.age3;
                                countItem.ageGroup4 = evt.age4;
                                countItem.male = evt.male;
                                countItem.female = evt.female;
                                countItem.happy = evt.happy;
                                countItem.neutral = evt.neutral;
                                countItem.angry= evt.angry;
                                countItem.deviceCamera = name;
                                countItem.name = name;
                                countItem.stack = countItem.deviceCamera;
                                countItem.count = topIndex;
                                totalAge1 += evt.age1;
                                totalAge2 += evt.age2;
                                totalAge3 += evt.age3;
                                totalAge4 += evt.age4;
                                totalMale += evt.male;
                                totalFemale += evt.female;
                                totalHappy += evt.happy;
                                totalNeutral += evt.neutral;
                                totalAngry += evt.angry;
                                name = name;
                                cameraList.push(countItem);
                            }
                        });
                        deviceList.push(cameraList);
                    });

                    $.each(deviceList, function(j, cameraList) {
                        $.each(cameraList, function(j, camera) {
                            var timeIndex = kendo.toString(camera.time, 'ddMMyyyyHHmmss');
                            if (deviceTimeList[timeIndex]) {
                                deviceTimeList[timeIndex].ageGroup1 += camera.ageGroup1;
                                deviceTimeList[timeIndex].ageGroup2 += camera.ageGroup2;
                                deviceTimeList[timeIndex].ageGroup3 += camera.ageGroup3;
                                deviceTimeList[timeIndex].ageGroup4 += camera.ageGroup4;
                                deviceTimeList[timeIndex].male += camera.male;
                                deviceTimeList[timeIndex].female += camera.female;
                                deviceTimeList[timeIndex].happy += camera.happy;
                                deviceTimeList[timeIndex].neutral += camera.neutral;
                                deviceTimeList[timeIndex].angry += camera.angry;
                            } else {
                                deviceTimeList[timeIndex] = camera;
                            }
                        })
                    })
                });
                $.each(deviceTimeList, function(i, deviceList) {
                    stackChartData.push(deviceList);
                })
            } else if (itemData.type === "device") {
                var name = itemData.labelName + " - " + itemData.text;
                var deviceList = [],
                    deviceTimeList = {};

                selectedDevices.push(name);
                var stackChartData = [];
                stackChartData.push({
                    time: fromDate,
                    ageGroup1: 0,
                    ageGroup2: 0,
                    ageGroup3: 0,
                    ageGroup4: 0,
                    male: 0,
                    female: 0,
                    happy: 0,
                    neutral: 0,
                    angry: 0,
                    name: name
                });
                stackChartData.push({
                    time: toDate,
                    ageGroup1: 0,
                    ageGroup2: 0,
                    ageGroup3: 0,
                    ageGroup4: 0,
                    male: 0,
                    female: 0,
                    happy: 0,
                    neutral: 0,
                    angry: 0,
                    name: name
                });
                $.each(itemData.items, function(i, camera) {
                    var cameraData = camera;
                    var cameraList = [];
                    $.each(events, function(j, evt) {
                    	
                        var countItem = {};
                        if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                            countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                            countItem.ageGroup1 = evt.age1;
                            countItem.ageGroup2 = evt.age2;
                            countItem.ageGroup3 = evt.age3;
                            countItem.ageGroup4 = evt.age4;
                            countItem.male = evt.male;
                            countItem.female = evt.female;
                            countItem.happy = evt.happy;
                            countItem.neutral = evt.neutral;
                            countItem.angry = evt.angry;
                            countItem.deviceCamera = name;
                            countItem.name = name;
                            countItem.stack = countItem.deviceCamera;
                            countItem.count = topIndex;
                            totalAge1 += evt.age1;
                            totalAge2 += evt.age2;
                            totalAge3 += evt.age3;
                            totalAge4 += evt.age4;
                            totalMale += evt.male;
                            totalFemale += evt.female;
                            totalHappy += evt.happy;
                            totalNeutral += evt.neutral;
                            totalAngry += evt.angry;
                            name = name;
                            cameraList.push(countItem);
                        }
                    });
                    deviceList.push(cameraList);
                });
                $.each(deviceList, function(i, cameraList) {
                    $.each(cameraList, function(j, camera) {
                        var timeIndex = kendo.toString(camera.time, 'ddMMyyyyHHmmss');
                        if (deviceTimeList[timeIndex]) {
                            deviceTimeList[timeIndex].ageGroup1 += camera.ageGroup1;
                            deviceTimeList[timeIndex].ageGroup2 += camera.ageGroup2;
                            deviceTimeList[timeIndex].ageGroup3 += camera.ageGroup3;
                            deviceTimeList[timeIndex].ageGroup4 += camera.ageGroup4;
                            deviceTimeList[timeIndex].male += camera.male;
                            deviceTimeList[timeIndex].female += camera.female;
                            deviceTimeList[timeIndex].happy += camera.happy;
                            deviceTimeList[timeIndex].neutral += camera.neutral;
                            deviceTimeList[timeIndex].angry += camera.angry;
                        } else {
                            deviceTimeList[timeIndex] = camera;
                        }
                    })
                })
                $.each(deviceTimeList, function(i, deviceList) {
                    stackChartData.push(deviceList);
                })
            } else if (itemData.type === "channel") {
                var cameraData = itemData,
                    name = cameraData.deviceName + " - " + cameraData.text;

                selectedDevices.push(name);
                var stackChartData = [];
                stackChartData.push({
                    time: fromDate,
                    ageGroup1: 0,
                    ageGroup2: 0,
                    ageGroup3: 0,
                    ageGroup4: 0,
                    male: 0,
                    female: 0,
                    happy: 0,
                    neutral: 0,
                    angry: 0,
                    name: name
                });
                stackChartData.push({
                    time: toDate,
                    ageGroup1: 0,
                    ageGroup2: 0,
                    ageGroup3: 0,
                    ageGroup4: 0,
                    male: 0,
                    female: 0,
                    happy: 0,
                    neutral: 0,
                    angry: 0,
                    name: name
                });

                $.each(events, function(index, evt) {
                    var countItem = {};
                    if (evt.deviceId === cameraData.coreDeviceId && evt.channelId === cameraData.channelId) {
                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                        countItem.ageGroup1 = evt.age1;
                        countItem.ageGroup2 = evt.age2;
                        countItem.ageGroup3 = evt.age3;
                        countItem.ageGroup4 = evt.age4;
                        countItem.male = evt.male;
                        countItem.female = evt.female;
                        countItem.happy = evt.happy;
                        countItem.neutral = evt.neutral;
                        countItem.angry = evt.angry;
                        countItem.deviceCamera = name;
                        countItem.name = name;
                        countItem.stack = countItem.deviceCamera;
                        countItem.count = topIndex;
                        totalAge1 += evt.age1;
                        totalAge2 += evt.age2;
                        totalAge3 += evt.age3;
                        totalAge4 += evt.age4;
                        totalMale += evt.male;
                        totalFemale += evt.female;
                        totalHappy += evt.happy;
                        totalNeutral += evt.neutral;
                        totalAngry += evt.angry;
                        name = name;
                        stackChartData.push(countItem);
                    }
                });

            }

            // Donut chart processing
            var totalGender = totalMale + totalFemale;
            var totalEmotion = totalHappy + totalNeutral + totalAngry;
            count += totalGender;

            var malePercentage = (totalMale / totalGender) * 100;
            var femalePercentage = (totalFemale / totalGender) * 100;
            var happyPercentage = (totalHappy / totalEmotion) * 100;
            var neutralPercentage = (totalNeutral / totalEmotion) * 100;
            var angryPercentage = (totalAngry / totalEmotion) * 100;
            //age percents
            var ageTotals = [totalAge1, totalAge2, totalAge3, totalAge4];
            var agePercentList = utils.getPerfectlyRoundedPercents(ageTotals, 2);
            var age1Percentage = agePercentList[0];
            var age2Percentage = agePercentList[1];
            var age3Percentage = agePercentList[2];
            var age4Percentage = agePercentList[3];

            var seriesGender = [];
            var seriesAge = [];
            var seriesEmotion = [];
            seriesGender = [{
                name: "Gender",
                data: [{
                    "category": localizeResource('male'),
                    "value": malePercentage,
                    "color": vca.donutChartColors[0],
                }, {
                    "category": localizeResource('female'),
                    "value": femalePercentage,
                    "color": vca.donutChartColors[1]
                }],
                labels: {
                    visible: true,
                    background: "transparent",
                    position: "outsideEnd",
                    color: "#D5A701",
                    font: "11px Arial,Helvetica,sans-serif",
                    template: "#= category #: #= kendo.format('{0:P}', percentage)#"
                }
            }];
            seriesAge = [{
                name: "Age",
                data: [{
                    "category": localizeResource("below") + " 21",
                    "value": age1Percentage,
                    "color": vca.donutChartColors[4]
                }, {
                    "category": "21-35",
                    "value": age2Percentage,
                    "color": vca.donutChartColors[5]
                }, {
                    "category": "36-55",
                    "value": age3Percentage,
                    "color": vca.donutChartColors[6]
                }, {
                    "category": localizeResource("above") + " 55",
                    "value": age4Percentage,
                    "color": vca.donutChartColors[7]
                }],
                labels: {
                    visible: true,
                    background: "transparent",
                    position: "outsideEnd",
                    color: "#D5A701",
                    font: "11px Arial,Helvetica,sans-serif",
                    template: "#= category #: #= kendo.format('{0:P}', percentage)#"
                }
            }];
            seriesEmotion = [{
                name: "Emotion",
                data: [{
                    "category": localizeResource("happy"),
                    "value": happyPercentage,
                    "color": vca.donutChartColors[2]
                }, {
                    "category": localizeResource("neutral"),
                    "value": neutralPercentage,
                    "color": vca.donutChartColors[3]
                },{
                	"category": localizeResource("angry"),
                    "value": angryPercentage,
                    "color": vca.donutChartColors[1]
                }],
                labels: {
                    visible: true,
                    background: "transparent",
                    position: "outsideEnd",
                    color: "#D5A701",
                    font: "11px Arial,Helvetica,sans-serif",
                    template: "#= category #: #= kendo.format('{0:P}', percentage)#"
                }
            }];

            vca.profilingAreaChartsData.push(stackChartData);
            vca.generateProfilingDonutChart("donutChartContainer", name, seriesGender, "Gender");
            vca.generateProfilingDonutChart("donutChartContainer", name, seriesAge, "Age");
            vca.generateProfilingDonutChart("donutChartContainer", name, seriesEmotion, "Emotion");

            // Stack chart Data
            var chartData = {};
            chartData.groupDetail = name;
            // count chart Data
            chartData.totalAgeGroup1 = totalAge1;
            chartData.totalAgeGroup2 = totalAge2;
            chartData.totalAgeGroup3 = totalAge3;
            chartData.totalAgeGroup4 = totalAge4;
            chartData.totalMale = totalMale;
            chartData.totalFemale = totalFemale;
            chartData.totalHappy = totalHappy;
            chartData.totalNeutral = totalNeutral;
            chartData.totalAngry = totalAngry;

            chartDetails.push(chartData);
            topIndex++;
        });
    } else { // reportType is 'normal' or 'group'
        $.each(selectedValue, function(topIndex, value) {
            var name = "";
            if (vca.reportType == "normal") {
                name = value.deviceName + " - " + value.channelName;
            } else {
                name = value.labelName;
            }
            selectedDevices.push(name);
            var stackChartData = [];
            stackChartData.push({
                time: fromDate,
                ageGroup1: 0,
                ageGroup2: 0,
                ageGroup3: 0,
                ageGroup4: 0,
                male: 0,
                female: 0,
                happy: 0,
                neutral: 0,
                angry: 0,
                name: name
            });
            stackChartData.push({
                time: toDate,
                ageGroup1: 0,
                ageGroup2: 0,
                ageGroup3: 0,
                ageGroup4: 0,
                male: 0,
                female: 0,
                happy: 0,
                neutral: 0,
                angry: 0,
                name: name
            });
            var totalAge1 = 0;
            var totalAge2 = 0;
            var totalAge3 = 0;
            var totalAge4 = 0;
            var totalMale = 0;
            var totalFemale = 0;
            var totalHappy = 0;
            var totalNeutral = 0;
            var totalAngry = 0;

            $.each(events, function(index, evt) {
                var countItem = {};
                if (vca.reportType === "normal") {
                    if (evt.deviceId === value.deviceId && evt.channelId === value.channelId) {
                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                        countItem.ageGroup1 = evt.age1;
                        countItem.ageGroup2 = evt.age2;
                        countItem.ageGroup3 = evt.age3;
                        countItem.ageGroup4 = evt.age4;
                        countItem.male = evt.male;
                        countItem.female = evt.female;
                        countItem.happy = evt.happy;
                        countItem.neutral = evt.neutral;
                        countItem.angry = evt.angry;
                        countItem.deviceCamera = name;
                        countItem.name = name;
                        countItem.stack = countItem.deviceCamera;
                        countItem.count = topIndex;
                        totalAge1 += evt.age1;
                        totalAge2 += evt.age2;
                        totalAge3 += evt.age3;
                        totalAge4 += evt.age4;
                        totalMale += evt.male;
                        totalFemale += evt.female;
                        totalHappy += evt.happy;
                        totalNeutral += evt.neutral;
                        totalAngry += evt.angry;
                        name = name;
                        stackChartData.push(countItem);
                    }
                } else {
                    var deviceChannelId = utils.combineDeviceChannelIDs(evt.deviceId, evt.channelId);
                    if (value.deviceChannelIds.indexOf(deviceChannelId) !== -1) {
                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                        countItem.ageGroup1 = evt.age1;
                        countItem.ageGroup2 = evt.age2;
                        countItem.ageGroup3 = evt.age3;
                        countItem.ageGroup4 = evt.age4;
                        countItem.male = evt.male;
                        countItem.female = evt.female;
                        countItem.happy = evt.happy;
                        countItem.neutral = evt.neutral;
                        countItem.angry = evt.angry;
                        countItem.deviceCamera = value.labelName;
                        countItem.stack = countItem.deviceCamera;
                        countItem.count = topIndex;
                        countItem.name = name;
                        totalAge1 += evt.age1;
                        totalAge2 += evt.age2;
                        totalAge3 += evt.age3;
                        totalAge4 += evt.age4;
                        totalMale += evt.male;
                        totalFemale += evt.female;
                        totalHappy += evt.happy;
                        totalNeutral += evt.neutral;
                        totalAngry += evt.angry;
                        name = name;
                        stackChartData.push(countItem);
                    }
                }
            });
            // Donut chart processing

            var totalAge = totalAge1 + totalAge2 + totalAge3 + totalAge4;
            var totalGender = totalMale + totalFemale;
            var totalEmotion = totalHappy + totalNeutral + totalAngry;
            count += totalGender;

            var malePercentage = (totalMale / totalGender) * 100;
            var femalePercentage = (totalFemale / totalGender) * 100;
            var happyPercentage = (totalHappy / totalEmotion) * 100;
            var neutralPercentage = (totalNeutral / totalEmotion) * 100;
            var angryPercentage = (totalAngry / totalEmotion) * 100;

            //age percents
            var ageTotals = [totalAge1, totalAge2, totalAge3, totalAge4];
            var agePercentList = utils.getPerfectlyRoundedPercents(ageTotals, 2);
            var age1Percentage = agePercentList[0];
            var age2Percentage = agePercentList[1];
            var age3Percentage = agePercentList[2];
            var age4Percentage = agePercentList[3];

            var seriesGender = [];
            var seriesAge = [];
            var seriesEmotion = [];
            seriesGender = [{
                name: "Gender",
                data: [{
                    "category": localizeResource('male'),
                    "value": malePercentage,
                    "color": vca.donutChartColors[0],
                }, {
                    "category": localizeResource('female'),
                    "value": femalePercentage,
                    "color": vca.donutChartColors[1]
                }],
                labels: {
                    visible: true,
                    background: "transparent",
                    position: "outsideEnd",
                    color: "#D5A701",
                    font: "11px Arial,Helvetica,sans-serif",
                    template: "#= category #: #= kendo.format('{0:P}', percentage)#"
                }
            }];
            seriesAge = [{
                name: "Age",
                data: [{
                    "category": localizeResource("below") + " 21",
                    "value": age1Percentage,
                    "color": vca.donutChartColors[4]
                }, {
                    "category": "21-35",
                    "value": age2Percentage,
                    "color": vca.donutChartColors[5]
                }, {
                    "category": "36-55",
                    "value": age3Percentage,
                    "color": vca.donutChartColors[6]
                }, {
                    "category": localizeResource("above") + " 55",
                    "value": age4Percentage,
                    "color": vca.donutChartColors[7]
                }],
                labels: {
                    visible: true,
                    background: "transparent",
                    position: "outsideEnd",
                    color: "#D5A701",
                    font: "11px Arial,Helvetica,sans-serif",
                    template: "#= category #: #= kendo.format('{0:P}', percentage)#"
                }
            }];
            seriesEmotion = [{
                name: "Emotion",
                data: [{
                    "category": localizeResource("happy"),
                    "value": happyPercentage,
                    "color": vca.donutChartColors[2]
                }, {
                    "category": localizeResource("neutral"),
                    "value": neutralPercentage,
                    "color": vca.donutChartColors[3]
                },{
                    "category": localizeResource("angry"),
                    "value": angryPercentage,
                    "color": vca.donutChartColors[1]
                }],
                labels: {
                    visible: true,
                    background: "transparent",
                    position: "outsideEnd",
                    color: "#D5A701",
                    font: "11px Arial,Helvetica,sans-serif",
                    template: "#= category #: #= kendo.format('{0:P}', percentage)#"
                }
            }];

            vca.profilingAreaChartsData.push(stackChartData);
            vca.generateProfilingDonutChart("donutChartContainer", name, seriesGender, "Gender");
            vca.generateProfilingDonutChart("donutChartContainer", name, seriesAge, "Age");
            vca.generateProfilingDonutChart("donutChartContainer", name, seriesEmotion, "Emotion");

            // Stack chart Data
            var chartData = {};
            chartData.groupDetail = name;
            // count chart Data
            chartData.totalAgeGroup1 = totalAge1;
            chartData.totalAgeGroup2 = totalAge2;
            chartData.totalAgeGroup3 = totalAge3;
            chartData.totalAgeGroup4 = totalAge4;
            chartData.totalMale = totalMale;
            chartData.totalFemale = totalFemale;
            chartData.totalHappy = totalHappy;
            chartData.totalNeutral = totalNeutral;
            chartData.totalAngry = totalAngry;

            chartDetails.push(chartData);

        });
    }
    var recordFlag = false;
    $.each(vca.profilingAreaChartsData, function(index, data) {
        if (data.length > 2) {
            recordFlag = true;
        }
    });
    if (!recordFlag) {
        vca.hideContainers();
        return;
    }

    if (vca.currentActiveCrosssiteProfilingTab === localizeResource("age")) {
        vca.generateAgeAreaCharts("ageAreaCharts");
        vca.hideProfilingDonutChart("Age", false);
        vca.hideProfilingDonutChart("Emotion", true);
        vca.hideProfilingDonutChart("Gender", true);
    } else if (vca.currentActiveCrosssiteProfilingTab === localizeResource("emotion")) {
        vca.generateEmotionAreaCharts("emotionAreaCharts");
        vca.hideProfilingDonutChart("Age", true);
        vca.hideProfilingDonutChart("Emotion", false);
        vca.hideProfilingDonutChart("Gender", true);
    } else if (vca.currentActiveCrosssiteProfilingTab === localizeResource("gender")) {
        vca.generateGenderAreaCharts("genderAreaCharts");
        vca.hideProfilingDonutChart("Age", true);
        vca.hideProfilingDonutChart("Emotion", true);
        vca.hideProfilingDonutChart("Gender", false);
    }
    vca.currentReportInfo = {
        "event-type": KupEvent.PROFILING,
        "site-name": selectedDevices.toString(),
        "from": kendo.toString(fromDate, kupapi.TIME_FORMAT),
        "to": kendo.toString(toDate, kupapi.TIME_FORMAT),
        "total-results": count + ""
    };
    chartDetails.sort(function(a, b) {
        return b.totalHappy - a.totalHappy;
    });
    chartDetails.sort(function(a, b) {
        return b.totalAngry - a.totalAngry;
    });
    vca.initAudienceProfilingButtomPortion("topHappyBox", chartDetails, "totalHappy");

    chartDetails.sort(function(a, b) {
        return b.totalAgeGroup2 - a.totalAgeGroup2;
    });
    vca.initAudienceProfilingButtomPortion("topYoungBox", chartDetails, "totalAgeGroup2");

    chartDetails.sort(function(a, b) {
        return b.totalMale - a.totalMale;
    });
    vca.initAudienceProfilingButtomPortion("topMaleBox", chartDetails, "totalMale");

    chartDetails.sort(function(a, b) {
        return b.totalNeutral - a.totalNeutral;
    });
    vca.initAudienceProfilingButtomPortion("topUnHappyBox", chartDetails, "totalNeutral");

    chartDetails.sort(function(a, b) {
        return b.totalAgeGroup4 - a.totalAgeGroup4;
    });
    vca.initAudienceProfilingButtomPortion("topMatureBox", chartDetails, "totalAgeGroup4");

    chartDetails.sort(function(a, b) {
        return b.totalFemale - a.totalFemale;
    });
    vca.initAudienceProfilingButtomPortion("topFemaleBox", chartDetails, "totalFemale");

    vca.initTooltip();
}

vca.initAudienceProfilingButtomPortion = function(divId, sortedData, preference) {
    var div = "#" + divId;
    $(div).html("");
    $.each(sortedData, function(index, data) {
        if (index < 3) {
            var info = "<div class='device_countdown'>" +
                "<span class='device_name' title='" + data.groupDetail + "'>" + data.groupDetail + "</span>" +
                "<span>" + kendo.toString(data[preference], "n0") + "</span></div>";
            $(div).append(info);
        } else {
            return false;
        }
    });
}

vca.generateAgeAreaCharts = function(divId) {
    var loadDelay = 1500;
    $.each(vca.profilingAreaChartsData, function(index, ageData) {
        setTimeout(function() {
            var name = ageData[0].name;
            vca.profilingAreaChart(divId, name, _ageLineChartSeries, ageData);
        }, loadDelay);
        loadDelay += 1000;
    });

}

vca.generateGenderAreaCharts = function(divId) {
    var loadDelay = 1500;
    $.each(vca.profilingAreaChartsData, function(index, genderData) {
        setTimeout(function() {
            var name = genderData[0].name;
            vca.profilingAreaChart(divId, name, _genderLineChartSeries, genderData);
        }, loadDelay);
        loadDelay += 1000;
    });
}

vca.generateEmotionAreaCharts = function(divId) {
	console.info("divId:"+divId);
    var loadDelay = 1500;
    $.each(vca.profilingAreaChartsData, function(index, emotionData) {
    	console.info("emotionData:");
    	console.info(emotionData);
        setTimeout(function() {
            var name = emotionData[0].name;
            vca.profilingAreaChart(divId, name, _emotionLineChartSeries, emotionData);
        }, loadDelay);
        loadDelay += 1000;

    });
}

vca.generateProfilingDonutChart = function(divId, name, donutChartData, labelname) {
    vca.profilingDonutChart(divId, name, donutChartData, labelname);
}

vca.enableConfigPage = function(kendoElementList, isEnabled) {
    //input and buttons
    $(".vca_content .right_box a").toggle(isEnabled);
    $(".vca_content .right_box input").prop('disabled', !isEnabled);

    // kendo elements
    $.each(kendoElementList, function(idx, item) {
        item.enable(isEnabled);
    });

    // sketching tool
    if (typeof sketchManager != 'undefined' && !sketchManager && !sketchManager.drawingFrame) {
        sketchManager.enableDrawingTools(isEnabled);
    }

    // leaflet tool
    if (typeof mapManager != 'undefined' && !mapManager) {
        mapManager.enableDrawingTools(isEnabled);
    }
}

vca.getInstanceById = function(instanceId, callback) {

    if (utils.isNullOrEmpty(instanceId)) {
        console.log("missing instanceId " + instanceId);
        return;
    }

    listRunningAnalytics("", analyticsType.ALL, function(responseData) {
        if (responseData == null || responseData.result != "ok") {
            utils.throwServerError(responseData);
            return;
        }

        $.each(responseData.instances, function(index, item) {
            if (item.instanceId == instanceId) {
                callback(item);
                return false;
            }
        });
    }, null);
}

vca.clearSketchManager = function() {
    if (typeof sketchManager != 'undefined' && !sketchManager) {
        sketchManager.drawingFrame = null;
    }
}

vca.parseAdditionalParams = function(paramString) {
    var paramMap = {};
    var paramIndicator = "--";
    paramString = paramString.replace(/\s+/g, ' ').trim();
    if (utils.isNullOrEmpty(paramString)) {
        return {};
    }

    var paramList = paramString.split(" ");
    for (var i = 0; i < paramList.length; i++) {
        var key = paramList[i];
        if (key.indexOf(paramIndicator) == -1) {
            return null;
        }

        var value = "";
        var next = paramList[i + 1];
        if (next != null && next.indexOf(paramIndicator) == -1) {
            value = next;
            i++;
        }

        key = key.replace(paramIndicator, "");
        paramMap[key] = value;
    }

    return paramMap;
}

vca.convertParamsToString = function(paramsMap) {
    var paramString = "";
    $.each(paramsMap, function(key, value) {
        paramString += "--" + key + " " + value + " ";
    });
    return paramString;
}

vca.hideProfilingDonutChart = function(labelName, isHide) {
    if (isHide) {
        $("." + labelName + "").hide();
    } else {
        $("." + labelName + "").show();
    }
}

vca.initMultiSelectByCameras = function() {
    $("#instanceMultiSelectList").data("kendoMultiSelect").value([]);
    if ($("input:checkbox[name=activecamera]").is(":checked")) {
        if (vca.activeVcas.length < 1) {
            $("#instanceMultiSelectList").data("kendoMultiSelect").readonly();
        }
    } else {
        $("#instanceMultiSelectList").data("kendoMultiSelect").enable();
    }
    $("#instanceMultiSelectList").data("kendoMultiSelect")
        .dataSource.data(vca.initHistoricalMultiSelectOptions("-2"));
    kendo.ui.progress($(".k-multiselect-wrap.k-floatwrap"), false);
};

vca.openErrorLog = function(instanceId) {
    vca.stopVcaGridRefresh();
    var contentPage = "/vca/errorlog/" + instanceId;
    utils.openPopup(localizeResource('vca-logs'), contentPage, null, null, true, function() {
        vca.refreshVcaTable();
    });
}

vca.attentionReport = function(dbEvents, fromDate, toDate) {
    var filterEvents = [];
    var categoryNames = ["under 5s", "5 to 10s", "10 to 20s", "20 to 30s", "30 to 60s",
        "1 to 3m", "3 to 5m", "5 to 8m", "8 to 10m", "10 to 15m", "15 to 30m"
    ]
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
    var attentionCountList = [];
    var twoDaysMillis = (2 * 24 * 60 * 60 * 1000);
    var rangeMillis = toDate - fromDate;
    var chartBaseUnit = "hours";
    var step = 1;

    //make sure graph starts at startDate and ends at endDate.
    attentionCountList.push({
        totalDuration: 0,
        eventNum: 0,
        date: fromDate
    });
    attentionCountList.push({
        totalDuration: 0,
        eventNum: 0,
        date: toDate
    });

    // if range is more than 2 days, change stepsize to days.
    if (rangeMillis > twoDaysMillis) {
        chartBaseUnit = "days";
        step = Math.ceil(rangeMillis / 1000 / 60 / 60 / 24 / 60); //reduce number of vertical line.
    }

    if (vca.reportType == "mix") {
        var selectedItemDataList = KUP.widget.report.getOpt('selectedItemDataList') || {};
        $.each(selectedItemDataList, function(uid, itemData) {
            if ($.isEmptyObject(itemData)) {
                return true;
            }
            if (itemData.type === "label") {
                var deviceTimeList = {};
                $.each(itemData.items, function(i, device) {
                    var deviceList = [];
                    $.each(device.items, function(j, camera) {
                        var cameraData = camera;
                        var cameraList = [];
                        $.each(dbEvents, function(k, event) {
                            if (event.deviceId === cameraData.coreDeviceId && event.channelId === cameraData.channelId) {
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
                            var timeIndex = kendo.toString(camera.date, 'ddMMyyyyHHmmss');
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
            } else if (itemData.type === "device") {
                var deviceList = [],
                    deviceTimeList = {};
                $.each(itemData.items, function(i, camera) {
                    var cameraData = camera;
                    var cameraList = [];
                    $.each(dbEvents, function(j, event) {
                        if (event.deviceId === cameraData.coreDeviceId && event.channelId === cameraData.channelId) {
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
                        var timeIndex = kendo.toString(camera.date, 'ddMMyyyyHHmmss');
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


            } else if (itemData.type === "channel") {
                var cameraData = itemData;
                $.each(dbEvents, function(i, event) {
                    if (event.deviceId === cameraData.coreDeviceId && event.channelId === cameraData.channelId) {
                        var countItem = {};
                        countItem.totalDuration = event.duration;
                        countItem.eventNum = event.count;
                        countItem.date = utils.convertUTCtoLocal(new Date(event.date));
                        attentionCountList.push(countItem);
                        countDurValues(event);
                    }
                });
            }
        });
    } else {
        return;
    }
    $("#barChart").kendoChart({
        theme: "black",
        title: {
            text: "Visitor segmentation by attention span",
            color: "#f6ae40",
            font: "bold 16px Muli, sans-serif"
        },
        series: [{
            type: "column",
            data: durValues,
            color: "#f6ae40"
        }],
        categoryAxis: {
            categories: categoryNames,
            majorGridLines: {
                visible: false //display vertical grid.
            }
        },
        valueAxis: {
            line: {
                visible: false //display y axis.
            }
        },
        tooltip: {
            visible: true,
            shared: true,
            format: "N0",
            font: "12px Muli, sans-serif"
        }
    });

    $("#lineChart").html($("#vcaChartStepChoices").html());
    $("#countChart").kendoChart({
        theme: "black",
        dataSource: {
            data: attentionCountList
        },
        title: {
            text: "Attention span variation over time",
            color: "#f6ae40",
            font: "bold 16px Muli, sans-serif"
        },
        legend: {
            position: "top"
        },
        series: [{
            type: "column",
            field: "totalDuration",
            name: "average duration",
            color: "#f6ae40",
            //aggregate: "sum",
            aggregate: function(values, series, dataItems, category) {
                var totalDur = 0;
                var totalEvt = 0;
                $.each(dataItems, function(index, element) {
                    totalDur += element.totalDuration;
                    totalEvt += element.eventNum;
                })
                
                if(totalEvt == 0)
                	return 0;
                
                totalDur = totalDur / totalEvt;
                return totalDur.toFixed(1);
            },
            categoryField: "date", //group by date, specify horizontal field display what.
            axis: "durationAvg"
        }, {
            type: "line",
            field: "eventNum",
            name: "number of faces",
            color: "#fe6e2c",
            aggregate: "sum",
            categoryField: "date",
            axis: "eventNum",
            tooltip: {
                format: "N0",
                font: "12px Muli, sans-serif",
            }
        }],
        valueAxis: [{
            name: "durationAvg",
            title: {
                text: "Average duration"
            },
            color: "#f6ae40",
            line: {
                visible: true
            },
            labels: {
                template: "#= Math.floor(value/60) #m #= value % 60 #s"
            }
        }, {
            name: "eventNum",
            title: {
                text: "Number of faces"
            },
            color: "#fe6e2c",
            line: {
                visible: true
            }
        }],
        categoryAxis: {
            field: "date",
            baseUnit: chartBaseUnit,
            labels: {
                step: step, //Render label every second.
                rotation: -90,
                timeFormat: "HH:mm",
                font: "12px Muli",
                dateFormats: {
                    hours: "dd-MM HH:mm",
                    days: "dd MMM",
                    weeks: "dd MMM",
                    months: "MMM yyyy"
                }
            },
            axisCrossingValues: [0, 100000],
            majorGridLines: {
                visible: true
            }
        },
        tooltip: {
            visible: true,
            shared: true,
            format: "N1",
            font: "12px Muli, sans-serif"
        }
    });

    if (chartBaseUnit == "hours") {
        $(".hourChoice").show();
        $("#stepHours").click();
    } else if (chartBaseUnit == "days") {
        $(".hourChoice").hide();
        $("#stepDays").click();
    }

    // stepsize change event.
    $("input[name=stepSize]").click(function() {
        var chart = $("#countChart").data("kendoChart");
        if (chart) {
            chart.options.categoryAxis.baseUnit = this.value;
            chart.refresh();
        }
    })
};

//input range [1,10]
vca.convertToMaskPercent = function(perimeterSensitivity) {
    var percent = (-2) * perimeterSensitivity + 22;
    return percent;
}

//input range [2,20]
vca.convertToPerimeterSensitivity = function(maskPercent) {
    var sensitivity;
    if (maskPercent < 2) {
        sensitivity = 10;
    } else if (maskPercent > 20) {
        sensitivity = 1;
    } else {
        sensitivity = (22 - maskPercent) / 2;
    }

    return sensitivity;
}
