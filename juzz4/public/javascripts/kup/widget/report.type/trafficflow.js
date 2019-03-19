KUP.widget.report.type.trafficflow = (function($, kup) {
    var currentReportInfo = null;

    function generateSVGImage(jsonOutput, regions, imgDataUrl) {
        var options = {
            cssSelector: "flowContainer",
            width: 600,
            height: 450,
            showRegionsByDefault: false
        };

        // var flowData = KaiFlow.convertToFlowData(regions, jsonOutput);
        var flowData = KaiFlow.convertToFlowDataFromAnalyticsData(regions, jsonOutput);
        KaiFlow.generate(options, regions, flowData, imgDataUrl);
    }

    var verifyWin8ForIE = function ()
    {
        var parser = new UAParser();
        var browserInfo = parser.getBrowser();
        var osInfo = parser.getOS();

        try
        {
            if (browserInfo.name == "IE" &&
                osInfo.name == "Windows" &&
                parseInt(osInfo.version) < 8)
            {
                utils.popupAlert(localizeResource("msg-min-win8-for-ie-traffic-flow"));
                return false;
            }
        }
        catch (e)
        {
            console.error(e);
        }

        return true;
    };

    var _kupReport = kup.widget.report,
        _self = {
            generateReport: function() {
                if (!verifyWin8ForIE())
                {
                    return;
                }

                currentReportInfo = null;
                $("#flowContainer").empty();
                var opt = _kupReport.getOpt(),
                    map = _kupReport.getMap(),
                    vcaEventType = map.vcaEventType[opt.reportType],
                    kupOpt = kup.getOpt(),
                    kupEvent = kupOpt.event,
                    i18n = kup.utils.i18n,
                    utils = kup.utils.default,
                    fromDate = opt.startDate,
                    toDate = opt.endDate;

                if (opt.selectedDeviceList.length <= 0 && opt.selectedChannelList.length <= 0) {
                    utils.popupAlert(i18n('no-channel-selected'));
                    return;
                }

                if (opt.selectedInstance.length > 1) {
                    utils.popupAlert(localizeResource("multiple-device-channel-not-supported"));
                    return;
                }

                var instance = opt.selectedInstance[0],
                    selectedPlatformDeviceId = instance.platformDeviceId,
                    selectedChannelId = instance.channelId;
                //convert to UTC dates.
                var from = kendo.toString(utils.convertToUTC(opt.startDate), "ddMMyyyyHHmmss"),
                    to = kendo.toString(utils.convertToUTC(opt.endDate), "ddMMyyyyHHmmss");

                //convert to UTC dates
                var fromStr = kendo.toString(utils.convertToUTC(fromDate), "ddMMyyyyHHmmss");
                var toStr = kendo.toString(utils.convertToUTC(toDate), "ddMMyyyyHHmmss");

                kup.utils.block.popup('#main-charts', i18n('generate-report'));
                listRunningAnalytics("", analyticsType.TRAFFIC_FLOW, function(respAnalytics) {
                        kup.utils.block.close('#main-charts');
                        if (respAnalytics.result != "ok") {
                            utils.throwServerError(respAnalytics);
                            return;
                        } else if (respAnalytics.instances == null && respAnalytics.instances.length == 0) {
                            utils.popupAlert(localizeResource("no-analytics-running"));
                            return;
                        }

                        var instance = null;
                        $.each(respAnalytics.instances, function(i, inst) {
                            if (inst.platformDeviceId+"" == selectedPlatformDeviceId && inst.channelId == selectedChannelId) {
                                instance = inst;
                                return false;
                            }
                        });

                        if (instance == null) {
                            utils.popupAlert(localizeResource("no-analytics-running"));
                            return;
                        }

                        var jsonThreshold = utils.tryParseJson(instance.thresholds);
                        if (jsonThreshold === {} || typeof jsonThreshold.regions === "undefined") {
                            utils.popupAlert(localizeResource("corrupted-vca-configuration"));
                            return;
                        } else {
                            regions = jsonThreshold.regions;
                            sourceName = jsonThreshold.sourceName;
                        }

                        var dbRegions = {
                            "sourceName": sourceName,
                            "regions": regions
                        };
                        var regionString = JSON.stringify(dbRegions);

                        getLiveVideoUrl("", instance.coreDeviceId, selectedChannelId, "http/jpeg", null, function(resp) {
                            var bgImageUrl = "";
                            if (resp.result == "ok" && resp.url.length > 0)
                                bgImageUrl = resp.url[0];

                            getAnalyticsReport("", KupEvent.TRAFFIC_FLOW, _kupReport.getDeviceGroups(), [selectedPlatformDeviceId], selectedChannelId, fromStr, toStr, {}, function(responseData) {
                                if (responseData.result == "ok") {
                                    if (responseData.data.length == 0)
                                        utils.popupAlert(localizeResource("no-records-found"));
                                    else {
                                        generateSVGImage(responseData.data, dbRegions, bgImageUrl);

                                        deviceManager.WaitForReady(function() {
                                            var reportInfo = {};
                                            deviceManager.attachDeviceDetails(reportInfo, selectedPlatformDeviceId, null, selectedChannelId);

                                            currentReportInfo = {
                                                "event-type": KupEvent.TRAFFIC_FLOW,
                                                "device-name": reportInfo.deviceName,
                                                "channel": reportInfo.channelName,
                                                "from": kendo.toString(fromDate, kupapi.TIME_FORMAT),
                                                "to": kendo.toString(toDate, kupapi.TIME_FORMAT)
                                            };
                                        });
                                        opt.isSuccessReport = true;
                                        _kupReport.setOpt(opt);
                                    }
                                } else {
                                    opt.isSuccessReport = false;
                                    _kupReport.setOpt(opt);
                                    utils.throwServerError(responseData);
                                }
                            });
                        });
                    },
                    function() {
                        kup.utils.block.close('#main-charts');
                    }
                );
            },
            exportCSV: function(data) {
                var dvcId = data.selectedInstance,
                     channId = data.selectedDevices,
                     fromDate = data.startDate,
                     toDate = data.endDate,
                     groupNames = data.groupNames,
                     posNames = data.posNames;
                var baseUnit = data.baseUnit;
                var opt = _kupReport.getOpt(),
                     utils = kup.utils.default,
                     i18n = kup.utils.i18n,
                     chartRadioID = opt.chartRadioType,
                     isMultiDeviceSelected = opt.isMultiDeviceSelected,
                     isSuccessReport = opt.isSuccessReport;

                //only a single camera export is allowed for traffic flow
                var targetDvc = dvcId[0];

                //verification
                if (!isSuccessReport || targetDvc == null) {
                     utils.popupAlert(i18n('please-generate-reports'));
                     return;
                }

                kup.utils.block.popup('#main-charts', i18n('generating-download-file'));

                var fromStr = kendo.toString(utils.convertToUTC(fromDate), "ddMMyyyyHHmmss");
                var toStr = kendo.toString(utils.convertToUTC(toDate), "ddMMyyyyHHmmss");

                window.exportDataLogs(
                    "csv",
                    KupEvent.TRAFFIC_FLOW,
                    targetDvc.platformDeviceId,
                    targetDvc.channelId,
                    fromStr,
                    toStr,
                    function(responseData) {
                        kup.utils.block.close('#main-charts');
                        if (responseData != null && responseData.result == "ok" &&
                            responseData["download-url"] != null) {
                            window.open(responseData["download-url"], '_blank');
                            window.focus();
                        } else {
                            utils.throwServerError(responseData);
                        }
                    },
                    function() {
                        kup.utils.block.close('#main-charts');
                    }
                );
            },
            exportPdf: function(data) {
                currentReportImage = KaiFlow.getSerializedSVG();

                if (currentReportImage == null || currentReportInfo == null || Object.keys(currentReportInfo).length == 0) {
                    console.log("missing report info");
                    return;
                }

                exportTrafficFlowPdf(currentReportImage, JSON.stringify(currentReportInfo));
            },
            loadSetUI: function() {},
            loadUpdateUI: function() {
                _kupReport.type.peoplecounting.loadUpdateUI();
            }
        };
    return _self;
})(jQuery, KUP);
