/**
 * [Javascript Module Pattern]
 * widget for dashboard
 * @param  {object} $   plug-in:jQuery
 * @param  {object} kup KUP module
 * @return {object}     public member
 * @author andy.gan@kaisquare.com.tw
 */
KUP.widget.dashboard = (function($, kup) {
    var _option = {
            days: 7,
            timeZone: 0,
            response: {
                listRunningAnalytics: {},
                userDevices: {},
                currentActiveSessions: {},
                chartData: {},
                platformInformation: {}
            },
            applicationType: '', // vlaue is node or cloud
        },
        _self = {
            setOpt: function(config) {
                _option = $.extend(false, {}, _option, config || {});
            },
            getOpt: function(key) {
                var deepCopy = $.extend(true, {}, _option);
                return (!!key) ? deepCopy[key] : deepCopy;
            },
            setChart: {
                line: function(divId, data) {
                    var days = _self.getOpt().days,
                        dataList = (function(data) {
                            var list = kup.utils.deepCopy(data) || [];
                            //add data for view
                            list.unshift(null);
                            list.push(null);
                            return list;
                        })(data),
                        dateList = (function(days) {
                            var list = (function(days) {
                                var list = [];
                                for (var i = days - 1; i >= 0; i--) {
                                    var date = new Date(),
                                        formatD = "";
                                    date.setDate(date.getDate() - i);
                                    formatD = date.getMonth() + 1 + "/" + date.getDate();
                                    list.push(formatD);
                                }
                                //add data for view
                                list.unshift("");
                                list.push("");
                                return list;
                            })(days);
                            return list;
                        })(days),
                        maxUnit = (function(data) {
                            var max = (function(data) {
                                    var tmp = 0;
                                    for (var i = data.length - 1; i >= 0; i--) {
                                        if (data[i] > tmp) {
                                            tmp = data[i];
                                        }
                                    }
                                    return tmp.toString();
                                })(data),
                                maxLength = max.length,
                                firstDigit = parseInt(max.charAt(0), 10),
                                maxUnit = (firstDigit % 2 === 0) ? (firstDigit + 2) * Math.pow(10, maxLength - 1) : (firstDigit + 1) * Math.pow(10, maxLength - 1);
                            return maxUnit;
                        })(data),
                        majorUnit = (function(data, unit) {
                            var unit = (function(data, unit) {
                                var avgUnit = 0,
                                    baseUnit = 0;
                                for (var i = data.length - 1; i >= 0; i--) {
                                    avgUnit += data[i];
                                }
                                avgUnit = parseInt(avgUnit / data.length, 10) || 1;
                                baseUnit = (avgUnit / parseFloat("0." + avgUnit.toString()) / 10);
                                return baseUnit * unit;
                            })(data, unit);
                            return unit;
                        })(data, 2),
                        valueAxisFormat = (function(data) {
                            var min = (function(data) {
                                var tmp = data[0] || 0;
                                for (var i = data.length - 1; i >= 0; i--) {
                                    if (data[i] <= tmp) {
                                        tmp = data[i];
                                    }
                                }
                                return tmp;
                            })(data);
                            return (min >= 1000) ? "#= value/1000 #k" : "#= value #";
                        })(data),
                        chartWidth = $("#" + divId).width() - 44,
                        chartHeight = 175,
                        legendOffSetX = $("#" + divId).width() / 2 - 50;
                    $("#" + divId).kendoChart({
                        /*title: {
                             text: "People Counting(Last 7 days)",
                             color: "#ffffff",
                             font: "18px Muli,sans-serif"
                         },*/
                        legend: {
                            visible: false,
                            position: "top",
                            offsetX: legendOffSetX,
                            labels: {
                                /*margin: {
                                    left: 20
                                },*/
                                color: "#f6ae40",
                                font: "11px Muli, sans-serif"
                            }
                        },
                        theme: "moonlight",
                        chartArea: {
                            //width: chartWidth,
                            height: chartHeight,
                            background: "#282828"
                        },
                        seriesDefaults: {
                            type: "line",
                            line: {
                                color: "#F5AD3F",
                                width: 1
                            },
                            tooltip: {
                                visible: true,
                                //format: "{0}",
                                template: "#= value #",
                                font: "400 12px/ Myriad Pro, sans-serif",
                                color: "#262626",
                                border: {
                                    width: 1,
                                    color: "#000000"
                                }
                            },
                        },
                        series: [{
                            //name: title,
                            data: dataList
                        }],
                        valueAxis: {
                            //min: 0,
                            //max: maxUnit,
                            //majorUnit: majorUnit,
                            labels: {
                                //format: "{0}k",
                                template: valueAxisFormat,
                                color: "#8C8B8B",
                                font: "400 12px Myriad Pro, sans-serif"
                            },
                            line: {
                                visible: false,
                            },
                            minorGridLines: {
                                visible: false
                            },
                            majorGridLines: {
                                visible: true,
                                width: 0.5
                            }
                        },
                        categoryAxis: {
                            categories: dateList,
                            justified: true,
                            //type: "date",
                            //baseUnit: "days",
                            labels: {
                                //dateFormats: {
                                //days: "M/d"
                                //},
                                font: "400 12px Myriad Pro, sans-serif",
                                color: "#8C8B8B"
                            },
                            majorGridLines: {
                                visible: false,
                                width: 1
                            }
                        }
                    }).find(".k-tooltip").css({
                        padding: "1.5px 6px",
                        "margin-left": -20,
                        "margin-top": 45,
                    });
                },
                pie: function(divId, data) {
                    var pieChart = kup.utils.chart.morris,
                        dataList = (function(data) {
                            var list = [],
                                index = 0;
                            $.each(data, function(k, v) {
                                list[index] = {
                                    label: kup.utils.i18n(k),
                                    value: v
                                };
                                index++;
                            });
                            return list;
                        })(data);
                    $("#" + divId).html('');
                    pieChart.Donut({
                        element: divId,
                        data: dataList,
                        colors: ["#32a8ad", "#ff675d"],
                        labelColor: "#ffffff",
                        formatter: function(val, data) {
                            var formatNum = kup.utils.format.numeral,
                                total = 0;
                            var dataIndex = (data.label === kup.utils.i18n('male')) ? 1 : 0;
                            $.each(this.data, function(i, info) {
                                total += info.value;
                            });
                            val = getGenderPercentNum(val / total, dataIndex);
                            //val = formatNum(val / total).format('0%');
                            return val;
                        },
                        fontFamily: "Muli",
                        backgroundColor: '#282828',
                        resize: true

                    });
                }
            },
            render: {
                chart: function() {
                    var data = _self.getOpt('response').chartData,
                        isDataZero = {
                            eventscount: (function(data) {
                                var tmp = 0;
                                $.each(data, function(i, v) {
                                    tmp += parseInt(v, 10);
                                });
                                return (tmp === 0) ? true : false;
                            })(data.eventscount),
                            peoplecounting: (function(data) {
                                data.total = data.total || 0;
                                return (parseInt(data.total, 10) === 0) ? true : false;
                            })(data.peoplecounting),
                            profilingcount: (function(data) {
                                data.male = data.male || 0;
                                data.female = data.female || 0;
                                return (parseInt(data.male, 10) === 0 && parseInt(data.female, 10) === 0) ? true : false;
                            })(data.genderprofilingcount),

                        },
                        formatNum = kup.utils.format.numeral,
                        peopleCounting = data.peoplecounting,
                        securityAlerts = data.eventscount,
                        securityAlertsText = (function(data) {
                            var tmp = 0;
                            for (var i = data.length - 1; i >= 0; i--) {
                                tmp += parseInt(data[i], 10);
                            }
                            return tmp;
                        })(data.eventscount),
                        genderProfiling = data.genderprofilingcount,
                        genderProfilingToTal = genderProfiling.male + genderProfiling.female;

                    if (isDataZero.peoplecounting) {
                        $("#showPeopleCountingChart").addClass('noData');
                    } else {
                        $("#showPeopleCountingChart").addClass('isData').find('.noData').hide().nextAll().show();
                        $("#showPeoleVisite").html(getCommaNum(peopleCounting.total));
                        $("#showVisitedToday").html(getCommaNum(peopleCounting.today));
                        _self.setChart.line('peopleCountingChart', peopleCounting.counts);
                    }

                    if (isDataZero.eventscount) {
                        $("#showSecurityAlertsChart").addClass('noData');
                    } else {
                        $("#showSecurityAlertsChart").addClass('isData').find('.noData').hide().nextAll().show();
                        $("#showAlerts").html(getCommaNum(securityAlertsText));
                        _self.setChart.line('securityAlertsChart', securityAlerts);
                    }

                    if (isDataZero.profilingcount) {
                        $("#showGenderProfilingChart").addClass('noData');
                    } else {
                        $("#showGenderProfilingChart").addClass('isData').find('.noData').hide().nextAll().show();
                        $("#showMale").html(getCommaNum(genderProfiling.male));
                        $("#showFemale").html(getCommaNum(genderProfiling.female));

                        $(".genderProfilingWrapper.male >.chartText > span:eq(1)").html(getGenderPercentNum(genderProfiling.male / genderProfilingToTal, 1));
                        $(".genderProfilingWrapper.female >.chartText > span:eq(1)").html(getGenderPercentNum(genderProfiling.female / genderProfilingToTal, 0));
                        _self.setChart.pie('genderProfilingChart', genderProfiling);
                    }
                    //set resize line chart
                    if (!isDataZero.peoplecounting || !isDataZero.eventscount) {
                        $(window).on("resize", function() {
                            kup.utils.chart.kendo.resize($("#peopleCountingChart"));
                            kup.utils.chart.kendo.resize($("#securityAlertsChart"));
                            $("#peopleCountingChart, #securityAlertsChart").find(".k-tooltip").css({
                                padding: "1.5px 6px",
                                "margin-left": -20,
                                "margin-top": 45,
                            });
                        });
                    }

                }
            },
            init: function() {
                var opt = _self.getOpt(),
                    kupOpt = kup.getOpt(),

                    listRunningAnalyticsOpt = {
                        sessionKey: kupOpt.sessionKey,
                        analyticsType: kupOpt.analyticsType.ALL,
                        onSuccess: function(data) {
                            data.instances = data.instances || [];
                            var activeAnalytics = data.instances.filter(function (inst)
                            {
                                return inst.vcaState !== "DISABLED";
                            });

                            var opt = _self.getOpt();
                            opt.response.listRunningAnalytics = data;
                            _self.setOpt(opt);
                            $('#showAnalytics').html(activeAnalytics.length);
                        },
                        onFailure: function() {
                            $('#showAnalytics').html('...');
                        }
                    },
                    getUserDevicesOpt = {
                        sessionKey: kupOpt.sessionKey,
                        onSuccess: function(data) {
                            data.devices = data.devices || [];
                            var opt = _self.getOpt(),
                                KupEvent = kup.getOpt('event'),
                                activeNodes = 0;

                            var getInstalledNodeCount = function(devicesData) {
                                var count = 0;
                                $.each(devicesData, function(i, dvc) {
                                    // is cloud
                                    if (dvc.node) {
                                        count++;
                                    }
                                    //is node
                                    if (!dvc.node) {
                                        count = 1;
                                        return false;
                                    }
                                });
                                return count;
                            }

                            var getInstalledCameraCount = function(devicesData) {
                                var count = 0;
                                jQuery.each(devicesData, function(i, dvc) {
                                    // is cloud
                                    if (dvc.node) {
                                        count = count + parseInt(dvc.node.cameras.length, 10);
                                    }
                                    //is node
                                    if (!dvc.node) {
                                        count++;
                                    }
                                });
                                return count;
                            }

                            opt.response.userDevices = data;
                            _self.setOpt(opt);
                            $('#showActiveNodes').html(getInstalledNodeCount(data.devices));
                            $('#showActiveCamera').html(getInstalledCameraCount(data.devices));
                        },
                        onFailure: function() {
                            $('#showActiveNodes').html('...');
                            $('#showActiveCamera').html('...');
                        }
                    },
                    getCurrentActiveSessionsOpt = {
                        bucketId: kupOpt.bucket,
                        onSuccess: function(data) {
                            var opt = _self.getOpt(),
                                loggedInUsers = data.totalcount || 0;
                            opt.response.userDevices = data;
                            _self.setOpt(opt);
                            $('#showloggedInUsers').html(loggedInUsers);
                        },
                        onFailure: function() {
                            $('#showloggedInUsers').html('...');
                        }
                    },
                    getPlatformInformationOpt = {
                        onSuccess: function(data) {
                            var opt = _self.getOpt();
                            var dhm = function(t) {
                                var cy = 365,
                                    cd = 24 * 60 * 60 * 1000,
                                    ch = 60 * 60 * 1000,
                                    y = Math.floor((t / cd) / cy),
                                    d = Math.floor(t / cd),
                                    h = Math.floor((t - d * cd) / ch),
                                    m = Math.round((t - d * cd - h * ch) / 60000),
                                    pad = function(n) {
                                        return n < 10 ? '0' + n : n;
                                    };
                                var output;
                                if (m === 60) {
                                    h++;
                                    m = 0;
                                }
                                if (h === 24) {
                                    d++;
                                    h = 0;
                                }

                                if (y > 0) {
                                    output = y + 'y' + Math.floor(d % cy) + 'd ' + pad(h) + 'h';
                                } else {
                                    output = d + 'd ' + pad(h) + 'h ' + pad(m) + 'm';
                                }
                                if (d < 0) {
                                    output = '00d 00h 00m';
                                }
                                return output;
                            }
                            var runningTime = (function() {
                                var startTime = data.info.serverStartTime || 0;
                                var nowTime = new Date().getTime();
                                var range = moment.range(startTime, nowTime).valueOf();
                                var runningTime = dhm(range);
                                if (range < 0) {
                                    console.error("Server startTime: ", startTime);
                                }
                                return runningTime;
                            })();

                            $('#showPlatformInformation').html(runningTime);
                            if (data.info.applicationType === 'node') {
                                $('#showActiveNodesDialog').hide();
                                $('#showPlatformInformationDialog').show();
                            } else {
                                $('#showActiveNodesDialog').show();
                                $('#showPlatformInformationDialog').hide();
                            }

                            opt.applicationType = data.info.applicationType || '';
                            opt.response.platformInformation = data.info;
                            _self.setOpt(opt);

                        },
                        onFailure: function() {}
                    };
                getDashboardOpt = {
                    days: opt.days,
                    timeZone: opt.timeZone,
                    onSuccess: function(data) {
                        var opt = _self.getOpt();
                        setData = data;
                        //set complete data and sort
                        setData.eventscount = data.eventscount || [];
                        setData.peoplecounting = data.peoplecounting || {};
                        setData.genderprofilingcount = data.genderprofilingcount || {};
                        opt.response.chartData = setData;
                        //render
                        _self.setOpt(opt);
                        _self.render.chart();
                    },
                    onFailure: function() {}
                };
                kup.utils.block.popup('.dashboard-container', kup.utils.i18n('retrieving-data'));
                //get data by ajax
                $.when(
                        window.listRunningAnalytics(
                            listRunningAnalyticsOpt.sessionKey,
                            listRunningAnalyticsOpt.analyticsType,
                            listRunningAnalyticsOpt.onSuccess,
                            listRunningAnalyticsOpt.onFailure
                        ),
                        window.getUserDevices(
                            getUserDevicesOpt.sessionKey,
                            getUserDevicesOpt.onSuccess,
                            getUserDevicesOpt.onFailure
                        ),
                        window.getPlatformInformation(
                            getPlatformInformationOpt.onSuccess,
                            getPlatformInformationOpt.onFailure
                        ),
                        // window.getCurrentActiveSessions(
                        //     getCurrentActiveSessionsOpt.bucketId,
                        //     getCurrentActiveSessionsOpt.onSuccess,
                        //     getCurrentActiveSessionsOpt.onFailure
                        // ),
                        window.getDashboard(
                            getDashboardOpt.days,
                            getDashboardOpt.timeZone,
                            getDashboardOpt.onSuccess,
                            getDashboardOpt.onFailure
                        )
                    )
                    .always(function() {
                        // $('#showActiveNodesDialog').on('click', function() {
                        //     var code = "dashboard",
                        //         contentPage = "/node/list/?calledFromDashboard=" + code,
                        //         onPopupClosed = function() {};
                        //     kup.utils.default.openPopup("Node List", contentPage, null, null, true, onPopupClosed);
                        // });
                        // $('#showUsersDialog').on('click', function() {
                        //     var contentPage = "/user/loggedinuserlist/",
                        //         onPopupClosed = function() {};
                        //     kup.utils.default.openPopup("Logged In Users List", contentPage, null, null, true, onPopupClosed);
                        // });
                        kup.utils.block.close('.dashboard-container');

                        //interval to updata content
                        setInterval(function() {
                            var opt = _self.getOpt();
                            window.listRunningAnalytics(
                                listRunningAnalyticsOpt.sessionKey,
                                listRunningAnalyticsOpt.analyticsType,
                                listRunningAnalyticsOpt.onSuccess,
                                listRunningAnalyticsOpt.onFailure
                            );
                            window.getUserDevices(
                                getUserDevicesOpt.sessionKey,
                                getUserDevicesOpt.onSuccess,
                                getUserDevicesOpt.onFailure
                            );
                            if (opt.applicationType === 'node') {
                                window.getPlatformInformation(
                                    getPlatformInformationOpt.onSuccess,
                                    getPlatformInformationOpt.onFailure
                                );
                            }
                            // window.getCurrentActiveSessions(
                            //     getCurrentActiveSessionsOpt.bucketId,
                            //     getCurrentActiveSessionsOpt.onSuccess,
                            //     getCurrentActiveSessionsOpt.onFailure
                            // );
                        }, 30000);
                    });
            }
        };
    return _self;

    /*******************************************************************************
     *
     *  Function Definition
     *
     *******************************************************************************/
    function getCommaNum(number) {
        var formatNum = kup.utils.format.numeral;
        return formatNum(number).format('0,0');
    };

    function getGenderPercentNum(number, dataIndex) {
        var roundedStr = (parseFloat(number) * 100).toFixed(2);
        var roundedNum = +roundedStr;
        return roundedNum + "%";
    };


})(jQuery, KUP);
