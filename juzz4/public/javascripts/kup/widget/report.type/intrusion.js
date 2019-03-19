KUP.widget.report.type.intrusion = (function($, kup) {
    var _kupReport = kup.widget.report,
        _self = {
            generateReport: function() {
                var opt = _kupReport.getOpt(),
                    map = _kupReport.getMap(),
                    kupOpt = kup.getOpt(),
                    kupEvent = kupOpt.event,
                    i18n = kup.utils.i18n,
                    utils = kup.utils.default,
                    kendo = kup.utils.chart.kendo;

                var vcaEventType = map.vcaEventType[opt.reportType],
                    fromDate = opt.startDate,
                    toDate = opt.endDate,
                    selectedDeviceList = opt.selectedDevices[0].platformDeviceId,
                    selectedChannelList = opt.selectedDevices[0].channelId,
                    groupNames = opt.groupNames,
                    posNames = opt.posNames;


                if (selectedDeviceList.length <= 0 && selectedChannelList.length <= 0) {
                    utils.popupAlert(i18n('no-channel-selected'));
                    return;
                }
                //convert to UTC dates.
                var from = kendo.toString(utils.convertToUTC(opt.startDate), "ddMMyyyyHHmmss"),
                    to = kendo.toString(utils.convertToUTC(opt.endDate), "ddMMyyyyHHmmss");

                kup.utils.block.popup('#main-charts', i18n('generate-report'));
                window.getAnalyticsReport("", vcaEventType, _kupReport.getDeviceGroups(), selectedDeviceList, "", from, to, {},
                    function(resp) {
                        var opt = _kupReport.getOpt(),
                            selectedInstance = opt.selectedInstance;
                        kup.utils.block.close('#main-charts');

                        if (resp.result == "ok" && resp.data.length > 0) {
                            var dbEvents = resp.data;
                            opt.isSuccessReport = true;
                            _kupReport.setOpt(opt);
                            _kupReport.updateUI.showDiv();
                            window.vca.reportType = 'mix';
                            window.vca.processCrossSiteInfo(vcaEventType, dbEvents, selectedInstance, fromDate, toDate);
                        } else {
                            opt.isSuccessReport = false;
                            _kupReport.setOpt(opt);
                            _kupReport.updateUI.cleanDiv();
                            utils.popupAlert(i18n("no-records-found"));
                        }

                    },
                    function() {
                        var opt = _kupReport.getOpt();
                        opt.isSuccessReport = false;
                        _kupReport.setOpt(opt);
                        _kupReport.updateUI.cleanDiv();
                        kup.utils.block.close('#main-charts');
                    }
                );
            },
            exportPdf: function() {
                var opt = _kupReport.getOpt(),
                    utils = kup.utils.default,
                    i18n = kup.utils.i18n,
                    chartRadioID = opt.chartRadioType,
                    isSuccessReport = opt.isSuccessReport;


                //vreification
                if (!isSuccessReport) {
                    utils.popupAlert(i18n('please-generate-reports'));
                    return;
                }
                if (vca.currentReportInfo == null) {
                    console.log("missing report info");
                }

                var svg = vca.getPrinterFriendlyChart("lineChart");
                if (svg == null || svg == "") {
                    return;
                }
                var svgStr = [];
                svgStr.push(svg);

                var n = 1;
                svg = vca.getPrinterFriendlyChart("lineChart" + n);
                while (svg != null && svg != "") {
                    svg = svg.replace("<?xml version='1.0' ?>", "");
                    svgStr.push(svg);
                    n++;
                    svg = vca.getPrinterFriendlyChart("lineChart" + n);
                }

                kup.utils.block.popup('#main-charts', i18n('generating-download-file'));
                window.exportVcaSecurityPdf(svgStr.join(""), JSON.stringify(vca.currentReportInfo),
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
            exportCSV: function(data) {
                var dvcId = data.selectedInstance,
                    channId = data.selectedDevices,
                    fromDate = data.startDate,
                    toDate = data.endDate,
                    groupNames = data.groupNames,
                    posNames = data.posNames,
                    eventType = data.eventType;
                var baseUnit = data.baseUnit;

                var opt = _kupReport.getOpt(),
                    utils = kup.utils.default,
                    i18n = kup.utils.i18n,
                    chartRadioID = opt.chartRadioType,
                    isMultiDeviceSelected = opt.isMultiDeviceSelected,
                    isSuccessReport = opt.isSuccessReport;

                //vreification
                if (!isSuccessReport) {
                    utils.popupAlert(i18n('please-generate-reports'));
                    return;
                }

                if (vca.currentReportInfo == null) {
                    console.log("missing report info");
                }

                var selectedGroups = _kupReport.getDeviceGroups();
                var fromStr = kendo.toString(utils.convertToUTC(fromDate), "ddMMyyyyHHmmss");
                var toStr = kendo.toString(utils.convertToUTC(toDate), "ddMMyyyyHHmmss");
                //var baseUnit = vca.currentChartbaseUnit;
                var siteName = $.isArray(groupNames) && groupNames.length > 0 ? groupNames[0] : "";
                kup.utils.block.popup('#main-charts', i18n('generating-download-file'));

                window.exportAggregatedCSVReport(eventType, JSON.stringify(selectedGroups), fromStr, toStr, baseUnit,
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
            loadSetUI: function() {},
            loadUpdateUI: function() {
                _kupReport.type.peoplecounting.loadUpdateUI();
            }
        };
    return _self;
})(jQuery, KUP);
