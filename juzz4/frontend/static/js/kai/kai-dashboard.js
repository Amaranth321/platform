angular
    .module('kai.dashboard', [
        'ui.kendo',
        'ui.morris'
    ]);

angular
    .module('kai.dashboard')
    .factory("DashboardService",
        function(KupOption, AuthTokenFactory, UtilsService, KupApiService, $http) {
            var i18n = UtilsService.i18n;
            var ajaxPost = KupApiService.ajaxPost;

            var data = {
                defaultReloadTime: 30000, //ms
                intervalApi: null,
                dayRange: 7,
                countTextData: {
                    analyticsCount: '...',
                    activeNodesCount: '...',
                    activeCamerasCount: '...',
                    loggedInUsersCount: '...'
                },
                chartTextData: {
                    showPeoleVisite: 0,
                    showVisitedToday: 0,
                    showAlerts: 0,
                    showMale: 0,
                    showFemale: 0,
                    showMalePercentage: '0%',
                    showFemalePercentage: '0%'
                },
                isEmptyChartData: {
                    peopleCounting: false,
                    securityAlerts: false,
                    genderProfiling: false
                },
                chartData: {
                    peopleCounting: [],
                    securityAlerts: [],
                    genderProfiling: []
                },
                chartConfig: {
                    peopleCounting: {},
                    securityAlerts: {},
                    genderProfiling: {}
                },
                chartTheme: {
                    peopleCounting: {},
                    securityAlerts: {},
                    genderProfiling: {}
                },
                genderColor: ["#32a8ad", "#ff675d"]
            };

            function getData() {
                return data;
            }

            function getAnalyticsData(isDefer) {
                var params = {
                    "analytics-type": 'ALL'
                };
                var onSuccess = function(response) {
                    return response;
                };
                var onFail = function(response) {};
                var onError = function() {};
                return ajaxPost('listrunninganalytics', params, onSuccess, onFail, onError, isDefer);
            }

            function getOnlineNodesData(isDefer) {
                var params = {};
                var onSuccess = function(response) {
                    return response;
                };
                var onFail = function(response) {};
                var onError = function() {};
                return ajaxPost('getuserdevices', params, onSuccess, onFail, onError, isDefer);
            }

            function getOnlineCamerasCount(devicesData) {
                var count = 0;
                jQuery.each(devicesData, function(i, device) {
                    count += device.node.cameras.length;
                });
                return count;
                // var count = 0;
                // jQuery.each(devicesData, function(i, device) {
                //     count = count + parseInt(device.model.channels, 10);
                // });
                // return count;
            }

            function getCurrentActiveSessions(isDefer) {
                var params = {
                    "bucket-id": AuthTokenFactory.getBucket()
                };
                var onSuccess = function(response) {
                    return response;
                };
                var onFail = function(response) {};
                var onError = function() {};
                return ajaxPost('getcurrentactivesessions', params, onSuccess, onFail, onError, isDefer);
            }

            function extractChartConfig(data) {

            }

            function getDashboardData(isDefer) {
                var params = {
                    "days": data.dayRange,
                    "time-zone-offset": KupApiService.data.timeZoneOffset
                };
                var onSuccess = function(response) {
                    var securityAlerts = response.eventscount || [],
                        peopleCounting = response.peoplecounting || {},
                        genderProfiling = response.genderprofilingcount || {};
                    var genderProfilingToTal = genderProfiling.male + genderProfiling.female,
                        securityAlertsText = (function(data) {
                            var tmp = 0;
                            for (var i = data.length - 1; i >= 0; i--) {
                                tmp += parseInt(data[i], 10);
                            }
                            return tmp;
                        })(securityAlerts),
                        getCommaNum = function(number) {
                            return window.numeral(number).format('0,0');
                        },
                        getPercentNum = function(number) {
                            return window.numeral(number).format('0%');
                        };
                    data.chartData = {
                        peopleCounting: peopleCounting.counts,
                        securityAlerts: securityAlerts,
                        genderProfiling: genderProfiling
                    };
                    data.isEmptyChartData = {
                        peopleCounting: (function(data) {
                            data.total = data.total || 0;
                            return (parseInt(data.total, 10) === 0) ? true : false;
                        })(peopleCounting),
                        securityAlerts: (function(data) {
                            var tmp = 0;
                            $.each(data, function(i, v) {
                                tmp += parseInt(v, 10);
                            });
                            return (tmp === 0) ? true : false;
                        })(securityAlerts),
                        genderProfiling: (function(data) {
                            data.male = data.male || 0;
                            data.female = data.female || 0;
                            return (parseInt(data.male, 10) === 0 && parseInt(data.female, 10) === 0) ? true : false;
                        })(genderProfiling)
                    };
                    data.chartTextData = {
                        showPeoleVisite: getCommaNum(peopleCounting.total),
                        showVisitedToday: getCommaNum(peopleCounting.today),
                        showAlerts: getCommaNum(securityAlertsText),
                        showMale: getCommaNum(genderProfiling.male),
                        showFemale: getCommaNum(genderProfiling.female),
                        showMalePercentage: getPercentNum(genderProfiling.male / genderProfilingToTal),
                        showFemalePercentage: getPercentNum(genderProfiling.female / genderProfilingToTal)
                    };
                    return response;
                };
                var onFail = function(response) {};
                var onError = function() {};
                return ajaxPost('getdashboard', params, onSuccess, onFail, onError, isDefer);
            }

            return {
                data: data,
                getData: getData,
                getAnalyticsData: getAnalyticsData,
                getOnlineNodesData: getOnlineNodesData,
                getOnlineCamerasCount: getOnlineCamerasCount,
                getCurrentActiveSessions: getCurrentActiveSessions,
                getDashboardData: getDashboardData,
                getPeopleCountingChartConfig: getPeopleCountingChartConfig,
                getSecurityAlertsChartConfig: getSecurityAlertsChartConfig,
                getGenderProfilingsChartConfig: getGenderProfilingsChartConfig

            }


            function getChartConfig(pdata) {
                var theme = AuthTokenFactory.getTheme(),
                    dataList = (function(pdata) {
                        var list = angular.copy(pdata) || [];
                        list.unshift(null);
                        list.push(null);
                        return list;
                    })(pdata),
                    days = getData().dayRange,
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
                    valueAxisFormat = (function(pdata) {
                        var min = 0;
                        if (pdata.length > 0) {
                            min = (function(pdata) {
                                var tmp = pdata.length ? pdata[0].counts : 0;
                                for (var i = data.length - 1; i >= 0; i--) {
                                    if (data[i] <= tmp) {
                                        tmp = pdata[i];
                                    }
                                }
                                return tmp;
                            })(pdata);
                        }
                        return (min >= 1000) ? "#= value/1000 #k" : "#= value #";
                    })(data);
                var config = {
                    theme: theme,
                    legend: {
                        visible: false,
                        position: "top",
                        labels: {
                            font: "11px Muli, sans-serif"
                        }
                    },
                    chartArea: {},
                    seriesDefaults: {
                        type: "area",
                        line: {
                            width: 1
                        },
                        area: {
                            line: {
                                style: "smooth"
                            }
                        },
                        tooltip: {
                            visible: true,
                            template: "#= value #",
                            font: "400 12px/ Myriad Pro, sans-serif",
                            border: {
                                width: 1
                            }
                        }
                    },
                    series: [{
                        data: dataList
                    }],
                    valueAxis: {
                        labels: {
                            template: valueAxisFormat,
                            font: "400 12px Myriad Pro, sans-serif"
                        },
                        line: {
                            visible: false
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
                        labels: {
                            font: "400 12px Myriad Pro, sans-serif"
                        },
                        majorGridLines: {
                            visible: false,
                            width: 1
                        }
                    }
                };
                return config;
            }

            function getPeopleCountingChartConfig(chartData) {
                chartData = chartData || data.chartData.peopleCounting;
                data.chartConfig.peopleCounting = getChartConfig(chartData);
                return data.chartConfig.peopleCounting;
            }

            function getSecurityAlertsChartConfig(chartData) {
                chartData = chartData || data.chartData.securityAlerts;
                data.chartConfig.securityAlerts = getChartConfig(chartData);
                return data.chartConfig.securityAlerts;
            }

            function getGenderProfilingsChartConfig(theme, pdata) {
                pdata = pdata || data.chartData.genderProfiling;
                theme.colors = data.genderColor;
                var isEmpty = (function() {
                    var isEmpty = true;
                    $.each(pdata, function(key, val) {
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
                        list[index] = {
                            label: i18n(k),
                            value: v
                        };
                        index++;
                    });
                    return list;
                })(pdata);

                var config = (isEmpty) ? {} : {
                    data: dataList,
                    colors: theme.colors,
                    labelColor: theme.labelColor,
                    formatter: function(val, data) {
                        var formatNum = window.numeral,
                            total = 0;
                        $.each(this.data, function(i, info) {
                            total += info.value;
                        });
                        val = formatNum(val / total).format('0%');
                        return val;
                    },
                    fontFamily: "Muli",
                    backgroundColor: theme.backgroundColor,
                    resize: true
                };

                data.chartConfig.genderProfiling = config;
                return data.chartConfig.genderProfiling;
            }
        }
    );

angular
    .module('kai.dashboard')
    .controller("DashboardController",
        function(
            KupOption,
            RouterStateService, UtilsService, PromiseFactory, AuthTokenFactory,
            DashboardService, KendoChartTheme, MorrisChartTheme,
            $http, $scope, $q, $rootScope, $interval
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var mainCtrl = $scope.$parent.mainCtrl;

            var dashboardCtrl = this;
            dashboardCtrl.data = DashboardService.data;

            init();
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function init() {
                initOpt();
                mainCtrl.block.promise = loadUI();
                setWatch();
            }

            function initOpt() {
                var opt = dashboardCtrl.data;
                opt.isEmptyChartData = {
                    peopleCounting: false,
                    securityAlerts: false,
                    genderProfiling: false
                };
            }

            function loadUI() {
                var opt = dashboardCtrl.data;
                var getAnalytics = function(isDefer) {
                    return DashboardService.getAnalyticsData(isDefer).then(function success(response) {
                        if (response.instances) {
                            var analyticsCount = response.instances.length;
                            dashboardCtrl.data.countTextData.analyticsCount = analyticsCount;
                        }
                    });
                };

                var getOnlineNodes = function(isDefer) {
                    return DashboardService.getOnlineNodesData(isDefer).then(function success(response) {
                        if (response.devices) {
                            var activeNodesCount = 0;
                            var activeCamerasCount = 0;
                            $.each(response.devices, function(i, dvc) {
                                if (dvc.status === kupOpt.deviceStatus.disconnected) {
                                    return true;
                                }
                                activeNodesCount++;
                                $.each(dvc.node.cameras, function(j, cmr) {
                                    if (cmr.status === kupOpt.deviceStatus.disconnected) {
                                        return true;
                                    }
                                    activeCamerasCount++;
                                });
                            });
                            dashboardCtrl.data.countTextData.activeNodesCount = activeNodesCount;
                            dashboardCtrl.data.countTextData.activeCamerasCount = activeCamerasCount;
                        }
                    });
                };


                var getDashboard = function(isDefer) {
                    return DashboardService.getDashboardData(isDefer).then(function success(data) {
                        if (data) {
                            var theme = AuthTokenFactory.getTheme();
                            kendo.dataviz.ui.registerTheme(theme, KendoChartTheme[theme]);
                            DashboardService.getPeopleCountingChartConfig();
                            DashboardService.getSecurityAlertsChartConfig();
                            DashboardService.getGenderProfilingsChartConfig(MorrisChartTheme[theme]);

                            dashboardCtrl.data.chartTextData = DashboardService.data.chartTextData;
                        }
                    });
                };

                if (opt.intervalApi) {
                    $interval.cancel(opt.intervalApi);
                }
                opt.intervalApi = $interval(function() {
                    getAnalytics(false);
                    getOnlineNodes(false);
                }, opt.defaultReloadTime);

                return $q.all([
                    getAnalytics(true),
                    getOnlineNodes(true),
                    getDashboard(true)
                ]);
            }

            function setWatch() {
                //watch router
                $scope.$watch(function() {
                    return angular.toJson(RouterStateService.getRouterState());
                }, function(newValue, oldValue) {
                    var opt = dashboardCtrl.data;
                    var routerState = angular.fromJson(newValue).toState;
                    var routerCheck = /\.dashboard/.test(routerState.name);
                    if (!routerCheck) {
                        $interval.cancel(opt.intervalApi);
                    }
                }, true);
                //watch theme 
                $scope.$watch(function() {
                    return AuthTokenFactory.getTheme();
                }, function(newVal, oldVal) {
                    if (newVal != oldVal) {
                        var theme = newVal;
                        kendo.dataviz.ui.registerTheme(theme, KendoChartTheme[theme]);
                        DashboardService.getPeopleCountingChartConfig();
                        DashboardService.getSecurityAlertsChartConfig();
                        DashboardService.getGenderProfilingsChartConfig(MorrisChartTheme[theme]);
                    }
                }, true);
                $scope.$watch(function() {
                    return AuthTokenFactory.getUserProfile().userName;
                }, function(newVal, oldVal) {
                    $rootScope.userName = oldVal;
                    if (newVal != oldVal) {
                        $rootScope.userName = AuthTokenFactory.getUserProfile().userName;
                    }
                }, true);
                $scope.$watch(function() {
                    return AuthTokenFactory.getUserRole();
                }, function(newVal, oldVal) {
                    $rootScope.userRole = oldVal;
                    if (newVal != oldVal) {
                        $rootScope.userRole = AuthTokenFactory.getUserRole();
                    }
                }, true);
            }
        }
    );
