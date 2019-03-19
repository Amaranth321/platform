angular.module('kai.reports.face', []);
angular
    .module('kai.reports.face')
    .factory("FaceService",
        function(KupOption, UtilsService, PromiseFactory, KupApiService, ReportsService, AuthTokenFactory, $filter, $q, $timeout) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var ajaxPost = KupApiService.ajaxPost;
            var reports = ReportsService;

            var data = {
                //all status 
                //isInitLoad: true,
                isScrollingload: false,

                //time format 
                timeFormat0: 'yyyy/MM/dd HH:mm:ss',
                timeFormat1: 'dd/MM/yyyy HH:mm:ss',
                timeFormat2: 'ddMMyyyyHHmmss',

                //from api reponse data
                apiAnalyticsReportList: [],
                apiEventsList: [],
                apiEventsCount: 0,
                apiEventBlobBase64Img: '',

                apiAllEventsList: [],
                apiAllEventsCount: 0,

                //events count data
                eventsCountTotal: 0,
                eventsCountCurrect: 0,
                eventsCountRange: 18,

                //request List
                requestForReport: [],

                //UI setting
                uiGridList: {
                    total: [],
                    select: [],
                    img: {},
                },
                uiSelectionBar: {
                    isShow: false,
                },
                uiLoading: {
                    isShow: false,
                },
                uiNodata: {
                    isShow: true,
                },
                uiSortItems: {
                    isShow: false,
                    isDropdownOpen: false,
                    list: [{
                        name: "new-to-old",
                        type: "date",
                        isActive: true,
                    }, {
                        name: "old-to-new",
                        type: "date",
                        isActive: false,
                    }, {
                        name: "low-to-high",
                        type: "duration",
                        isActive: false,
                    }, {
                        name: "high-to-low",
                        type: "duration",
                        isActive: false,
                    }],
                },
                uiExport: {
                    isOpen: false,
                },
            };
            return {
                data: data,

                getAnalyticsReportApi: getAnalyticsReportApi,
                getEventsApi: getEventsApi,
                getEventBlobApi: getEventBlobApi,
                exportCsvApi: exportCsvApi,
                exportPdfApi: exportPdfApi,

                generateReport: generateReport,
                generateGridList: generateGridList,

                setInitData: setInitData,
                setGridListData: setGridListData,
                sortGridList: sortGridList,
                getCurrectSortItems: getCurrectSortItems,
            };
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function setInitData() {
                data.isScrollingload = false;

                data.eventsCountTotal = 0;
                data.eventsCountCurrect = 0;

                data.uiSelectionBar.isShow = false;
                data.uiNodata.isShow = true;
                data.uiSortItems.isShow = false;
                data.uiGridList = {
                    total: [],
                    select: [],
                    img: {},
                };
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

            function getEventsApi(isDefer, isAll) {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;

                var selectedDeviceList = reportsOpt.selectedDevices[0].devicePairs[0].coreDeviceId;
                var selectedChannelList = reportsOpt.selectedDevices[0].devicePairs[0].channelId;

                var fromDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.startDate), data.timeFormat2);
                var toDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.endDate), data.timeFormat2);

                var fieldList = ["time", "deviceName", "deviceId", "channelId", "blobId", "data"];
                var take = isAll ? 0 : opt.eventsCountRange;
                var skip = isAll ? 0 : opt.eventsCountCurrect;

                var param = {
                    "event-type": vcaEventType,
                    "event-id": '',
                    "skip": skip,
                    "take": take,
                    "device-id": selectedDeviceList.toString(),
                    "channel-id": selectedChannelList.toString(),
                    "bound": null,
                    "rad": null,
                    "from": fromDateUTC,
                    "to": toDateUTC,
                    "fields": fieldList.join()
                };
                var onSuccess = function(response) {
                    if (isAll) {
                        opt.apiAllEventsList = response.events || [];
                        opt.apiAllEventsCount = response.totalcount || 0;
                    } else {
                        opt.apiEventsList = response.events || [];
                        opt.apiEventsCount = response.totalcount || 0;
                    }
                };
                var onFail = function(response) {
                    if (isAll) {
                        opt.apiAllEventsList = [];
                        opt.apiAllEventsCount = 0;
                    } else {
                        opt.apiEventsList = [];
                        opt.apiEventsCount = 0;
                    }
                };
                var onError = function() {
                    if (isAll) {
                        opt.apiAllEventsList = [];
                        opt.apiAllEventsCount = 0;
                    } else {
                        opt.apiEventsList = [];
                        opt.apiEventsCount = 0;
                    }
                };
                return ajaxPost('getevents', param, onSuccess, onFail, onError, isDefer);
            }

            function getEventBlobApi(eventData) {
                var opt = data;
                var eventId = eventData.id;
                var blobId = eventData.blobId;
                var param = {
                    "event-id": eventId,
                    "blob-id": blobId,
                };
                var reportsOpt = reports.data;
                var onSuccess = function(response) {
                    if (response["image-base64"] != null && response["image-base64"].length > 100) {
                        opt.apiEventBlobBase64Img = "data:image/jpeg;base64," + response['image-base64'];
                    } else {
                        opt.apiEventBlobBase64Img = '';
                    }
                    if (opt.uiGridList.img[eventData.id]) {
                        opt.uiGridList.img[eventData.id].isLoading = false;
                        opt.uiGridList.img[eventData.id].url = opt.apiEventBlobBase64Img;
                    }
                };
                var onFail = function(response) {
                    if (opt.uiGridList.img[eventData.id]) {
                        opt.uiGridList.img[eventData.id].isLoading = false;
                        opt.uiGridList.img[eventData.id].url = "";
                    }
                };
                var onError = function() {
                    if (opt.uiGridList.img[eventData.id]) {
                        opt.uiGridList.img[eventData.id].isLoading = false;
                        opt.uiGridList.img[eventData.id].url = "";
                    }
                };
                return ajaxPost('geteventbinarydata', param, onSuccess, onFail, onError);
            }

            function setGridListData() {
                var opt = data;
                var tmpGridList = angular.copy(opt.apiEventsList);
                var tmpGridListForImg = {};
                opt.eventsCountTotal = opt.apiEventsCount;

                $.each(tmpGridList, function(i, grid) {
                    if (grid.data) {
                        grid.data = angular.fromJson(grid.data);
                        grid.duration = parseInt(grid.data.duration, 10);
                        grid.durationSec = parseFloat((grid.data.duration / 1000).toFixed(1), 10);
                    }
                    if (grid.time) {
                        grid.timeStamp = (function() {
                            var time = grid.time.split(/\D/);
                            var timeStamp = "";

                            timeStamp = new Date(time[2], time[1] - 1, time[0], time[3], time[4], time[5]).getTime();
                            return timeStamp;
                        })();
                        grid.timeLocal = UtilsService.UTCToLocal(grid.timeStamp);
                        grid.timeLocalStamp = grid.timeLocal.getTime();
                        grid.dateFormat = kendo.toString(grid.timeLocal, opt.timeFormat1).split(' ')[0];
                        grid.timeFormat = kendo.toString(grid.timeLocal, opt.timeFormat1).split(' ')[1];
                    }
                    if (!grid.isSelect) {
                        grid.isSelect = false;
                    }

                    tmpGridListForImg[grid.id] = {
                        eventId: grid.id,
                        blobId: grid.blobId,
                        url: "",
                        isLoading: true,
                    }
                });

                if (opt.isScrollingload) {
                    opt.eventsCountCurrect = (function() {
                        var all = opt.eventsCountCurrect + tmpGridList.length;
                        var count = 0;
                        if (all > opt.apiEventsCount) {
                            count = opt.eventsCountTotal;
                        } else {
                            count = all;
                        }
                        return count;
                    })();
                    opt.uiGridList.total = opt.uiGridList.total.concat(tmpGridList);
                    opt.uiGridList.img = $.extend(opt.uiGridList.img, tmpGridListForImg);
                } else {
                    opt.eventsCountCurrect = (function() {
                        var all = opt.eventsCountTotal;
                        var count = 0;
                        if (all > opt.eventsCountRange) {
                            count = opt.eventsCountRange;
                        } else {
                            count = all;
                        }
                        return count;
                    })();
                    opt.uiGridList.total = tmpGridList;
                    opt.uiGridList.img = tmpGridListForImg;
                }
            }

            function sortGridList() {
                var sortType = getCurrectSortItems().name;
                if (sortType == 'new-to-old') {
                    data.uiGridList.total = $filter('orderBy')(data.uiGridList.total, ['-timeLocalStamp']);
                }
                if (sortType == 'old-to-new') {
                    data.uiGridList.total = $filter('orderBy')(data.uiGridList.total, ['+timeLocalStamp']);
                }
                if (sortType == 'low-to-high') {
                    data.uiGridList.total = $filter('orderBy')(data.uiGridList.total, ['+duration']);
                }
                if (sortType == 'high-to-low') {
                    data.uiGridList.total = $filter('orderBy')(data.uiGridList.total, ['-duration']);
                }
            }

            function exportCsvApi(periodType) {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;
                var dvcId = reportsOpt.selectedInstance;
                var fromDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.startDate), opt.timeFormat2);
                var toDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.endDate), opt.timeFormat2);
                var param = {
                    "file-format": 'csv',
                    "event-type": vcaEventType,
                    "from": fromDateUTC,
                    "to": toDateUTC,
                    "time-zone-offset": KupApiService.data.timeZoneOffset,
                    "device-id": dvcId[0].platformDeviceId,
                    "channel-id": dvcId[0].channelId
                };

                return KupApiService.exportDoc(param, 'exportdatalogs');
            }

            function exportPdfApi() {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;
                var deviceName = (function() {
                    var name = '';
                    $.each(reportsOpt.selectedItemDataList, function(uid, itemData) {
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
                        if (itemData.isCamera) {
                            name = itemData.deviceName;
                        }
                    });
                    return name;
                })();
                var channelName = (function() {
                    var name = '';
                    $.each(reportsOpt.selectedItemDataList, function(uid, itemData) {
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
                        if (itemData.isCamera) {
                            name = itemData.cameraName;
                        }
                    });
                    return name;
                })();

                var eventIds = (function() {
                    var idList = [];
                    $.each(opt.apiAllEventsList, function(i, eventData) {
                        idList.push(eventData.id);
                    });
                    return idList;
                })();

                var reportInfo = {
                    "event-type": vcaEventType,
                    "device-name": deviceName,
                    "channel": channelName,
                    "from": kendo.toString(reportsOpt.dateRange.startDate, opt.timeFormat1),
                    "to": kendo.toString(reportsOpt.dateRange.endDate, opt.timeFormat1),
                    "total-results": opt.apiAllEventsCount + ""
                };

                var param = {
                    "time-zone-offset": KupApiService.data.timeZoneOffset,
                    "svg-string": "",
                    "report-info": JSON.stringify(reportInfo),
                    "event-ids": JSON.stringify(eventIds)
                };

                return KupApiService.exportDoc(param, 'exportfaceindexingpdf');
            }


            function getCurrectSortItems() {
                var sortItems = [];
                $.each(data.uiSortItems.list, function(i, list) {
                    if (list.isActive) {
                        sortItems = list;
                        return false;
                    }
                })
                return sortItems;
            }


            function generateGridList() {
                var opt = data;
                return getEventsApi(true)
                    .finally(function() {
                        setGridListData();
                        sortGridList();
                        $.each(opt.apiEventsList, function(i, et) {
                            getEventBlobApi(et);
                        });
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
                if (!reports.isSingleCamera()) {
                    notification('error', i18n("multiple-device-channel-not-supported"));
                    reports.isSuccessReport(false);
                    return false;
                }

                //set generate report promise
                opt.requestForReport = [
                    getAnalyticsReportApi(true),
                    generateGridList()
                ];

                //return promise
                var dfd = $q.defer();
                $timeout(function() {
                    $q.all(opt.requestForReport)
                        .finally(function() {
                            if (opt.apiEventsList.length <= 0) {
                                reports.isSuccessReport(false);
                                opt.uiSortItems.isShow = false;
                                dfd.reject();
                                return;
                            }
                            reports.isSuccessReport(true);
                            opt.uiSortItems.isShow = true;
                            dfd.resolve();
                        });
                }, 500);
                return dfd.promise;
            }
        });

angular
    .module('kai.reports.face')
    .controller('FaceController',
        function(
            KupOption,
            RouterStateService, UtilsService, PromiseFactory, AuthTokenFactory,
            ReportsService, FaceService,
            $scope, $window, $q
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var reports = ReportsService;
            var faceCtrl = this;

            //UI controller
            faceCtrl.data = FaceService.data;
            faceCtrl.fn = {
                showSelection: showSelection,
                clearSelection: clearSelection,
                downloadImg: downloadImg,

                setCurrectSortItems: setCurrectSortItems,
                getCurrectSortItems: getCurrectSortItems,

                sortGridList: sortGridList,
                selectGridList: selectGridList,
                scrollingloadGridList: scrollingloadGridList,

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
                    faceCtrl.data.uiNodata.isShow = !reportsOpt.isSuccessReport;
                }, true);

                FaceService.setInitData();
            }

            function selectGridList(id) {
                var gridList = faceCtrl.data.uiGridList.total;

                if (id) {
                    if (faceCtrl.data.uiGridList.img[id].isLoading) {
                        return false;
                    }
                    if (!faceCtrl.data.uiGridList.img[id].url) {
                        return false;
                    }
                    $.each(gridList, function(i, grid) {
                        if (grid.id != id) {
                            return true;
                        }
                        grid.isSelect = !grid.isSelect;
                    });
                }

                if (!id) {
                    $.each(gridList, function(i, grid) {
                        grid.isSelect = false;
                    });
                }

                $.each(gridList, function(i, grid) {
                    if (grid.isSelect) {
                        $('#' + grid.id).addClass('kupItemSelected');
                        $('#' + grid.id).find('.fa-circle-thin').addClass('fa-check');
                    } else {
                        $('#' + grid.id).removeClass('kupItemSelected');
                        $('#' + grid.id).find('.fa-circle-thin').removeClass('fa-check');
                    }
                });

                setSelectedGridData();
                showSelection();
            }

            function showSelection() {
                var check = (faceCtrl.data.uiGridList.select.length > 0) ? true : false;
                faceCtrl.data.uiSelectionBar.isShow = check;
            }

            function setSelectedGridData() {
                var selectedGrid = [];
                $.each(faceCtrl.data.uiGridList.total, function(i, grid) {
                    $.each(faceCtrl.data.uiGridList.img, function(id, imgData) {
                        if (id == grid.id && grid.isSelect) {
                            var tmpData = angular.copy(grid);
                            tmpData.imgUrl = imgData.url;
                            selectedGrid.push(tmpData);
                            return false;
                        }
                    });
                });
                faceCtrl.data.uiGridList.select = selectedGrid;
                return selectedGrid;
            }

            function clearSelection() {
                selectGridList();
            }

            function setCurrectSortItems(index) {
                $.each(faceCtrl.data.uiSortItems.list, function(i, list) {
                    list.isActive = (index == i) ? true : false;
                })
            }

            function getCurrectSortItems() {
                return FaceService.getCurrectSortItems();
            }

            function sortGridList() {
                FaceService.sortGridList();
            }

            function scrollingloadGridList() {
                var opt = faceCtrl.data;

                if (!reports.isSuccessReport()) {
                    return false;
                }
                if (opt.uiLoading.isShow) {
                    return false;
                }
                if (opt.eventsCountCurrect >= opt.eventsCountTotal) {
                    return false;
                }

                opt.isScrollingload = true;
                opt.uiLoading.isShow = true;

                FaceService.generateGridList()
                    .finally(function() {
                        opt.isScrollingload = false;
                        opt.uiLoading.isShow = false;
                    });
            }

            function downloadImg() {
                var zip = new JSZip();
                var imgData = faceCtrl.data.uiGridList.select;
                var zipName = i18n('download') + ".zip";
                var dirName = i18n('images');
                var zipData = zip.folder(dirName);

                $.each(imgData, function(i, img) {
                    var imgName = (function() {
                        var time = img.time.split(/\D/);
                        var imgName = time[2] + time[1] + time[0] + '_' + time[3] + time[4] + time[5] + '.jpg';
                        return imgName;
                    })()
                    var imgData = (img.imgUrl) ? img.imgUrl.substr(img.imgUrl.indexOf(',') + 1) : '';
                    zipData.file(imgName, imgData, {
                        base64: true
                    });
                });
                // var content = zip.generate({
                //     type: "blob"
                // });
                window.location = "data:application/zip;base64," + zip.generate({type:"base64"} + zipName);
                // saveAs(content, zipName);
            }

            function exportCsv() {
                if (!reports.isSuccessReport()) {
                    notification('error', i18n('please-generate-reports'));
                    return false;
                }

                var warningNotify = notification('warning', i18n('exporting-to-csv'), 0);
                FaceService.exportCsvApi()
                    .finally(function() {
                        warningNotify.close();
                    });
            }

            function exportPdf() {
                var opt = faceCtrl.data;
                if (!reports.isSuccessReport()) {
                    notification('error', i18n('please-generate-reports'));
                    return false;
                }

                var warningNotify = notification('warning', i18n('exporting-to-pdf'), 0);
                if (!opt.apiAllEventsCount) {
                    FaceService.getEventsApi(false, true)
                        .finally(function() {
                            FaceService.exportPdfApi()
                                .finally(function() {
                                    warningNotify.close();
                                });
                        });
                } else {
                    FaceService.exportPdfApi()
                        .finally(function() {
                            warningNotify.close();
                        });
                }

            }
        });
