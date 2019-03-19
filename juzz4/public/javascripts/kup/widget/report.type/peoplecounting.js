KUP.widget.report.type.peoplecounting = (function($, kup) {

    function getUserFakePOSDataPref(callback) {
        if (vca.showHowToImport) {
            vca.userFakePOSDataPref = false;
            callback();
            return;
        }
        getUserPrefs("", function(userPrefResponse) {
            if (userPrefResponse !== null && userPrefResponse.result === "ok") {
                vca.userFakePOSDataPref = (userPrefResponse.prefs.POSFakeDataEnabled === undefined) ? vca.userFakePOSDataPref : userPrefResponse.prefs.POSFakeDataEnabled;
                callback();
            }
        }, function() {
            kup.utils.block.close('#main-charts');
        });
    }
    
    function isShowAvgOcc(opt) {
        var check = true;
        $.each(opt.selectedItemDataList, function(i, item) {
            if ($.isEmptyObject(item)) {
                return true;
            }
            if (item.type === opt.device || item.type === opt.channel) {
                check = false;
                return false;
            }
        });
        return check;
    }

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
                    posNames = opt.posNames,
                    isShowChart = (groupNames.length === 1) ? vca.userFakePOSDataPref : true;

                //Only show pos data of that label is STORE type
            	vca.disableAllPOSCharts = false;
            	$.each(opt.selectedItemDataList, function (uid, item){
                	if (!$.isEmptyObject(item) && item.labelType != "STORE")
            		{
                		vca.disableAllPOSCharts = true;
                		isShowChart = true;
            		}
                });
                
                if (selectedDeviceList.length <= 0 && selectedChannelList.length <= 0) {
                    utils.popupAlert(i18n('no-channel-selected'));
                    return;
                }
                //convert to UTC dates.
                var from = kendo.toString(utils.convertToUTC(opt.startDate), "ddMMyyyyHHmmss"),
                    to = kendo.toString(utils.convertToUTC(opt.endDate), "ddMMyyyyHHmmss");

                getUserFakePOSDataPref(function() {
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
                                window.vca.processPlpCountingCrossSiteInfo(dbEvents, selectedInstance, fromDate, toDate, groupNames, posNames, isShowChart);
                            } else {
                                opt.isSuccessReport = false;
                                _kupReport.setOpt(opt);
                                _kupReport.updateUI.cleanDiv();
                                utils.popupAlert(i18n("no-records-found"));
                            }

                            $('#displayAverageOccupancy').parent().toggle(isShowAvgOcc(opt));
                        },
                        function() {
                            var opt = _kupReport.getOpt();
                            opt.isSuccessReport = false;
                            _kupReport.setOpt(opt);
                            _kupReport.updateUI.cleanDiv();
                            kup.utils.block.close('#main-charts');
                        }
                    );
                });
            },
            exportCSV: function(data) {
                _kupReport.type.intrusion.exportCSV(data);
            },
            exportPdf: function() {
                var opt = _kupReport.getOpt(),
                    utils = kup.utils.default,
                    i18n = kup.utils.i18n,
                    chartRadioID = opt.chartRadioType,
                    isSuccessReport = opt.isSuccessReport;

                //verification
                if (!isSuccessReport) {
                    utils.popupAlert(i18n('please-generate-reports'));
                    return;
                }
                if (vca.currentReportInfo == null) {
                    console.log("missing report info");
                }

                var svg = "";
                if (chartRadioID === 'charts') {
                    svg = vca.getPrinterFriendlyChart("lineChart");
                } else {
                    svg = timeCard.getSvgList("timeCardWrapper");
                }
                //    var svg = vca.getPrinterFriendlyChart("lineChart");
                if (svg == null || svg == "") {
                    return;
                }
                var svgStr = [];
                svgStr.push(svg);

                var n = 1;
                if (chartRadioID === 'charts') {
                    //"#lineChart1" is used to display average occupancy
                    if (isShowAvgOcc(opt)) {
                        svg = vca.getPrinterFriendlyChart("lineChart" + n);
                    } else {
                        svg = "";
                    }
                } else {
                    svg = timeCard.getSvgList("timeCardWrapper" + n);
                }

                while (svg != null && svg != "") {
                    svg = svg.replace("<?xml version='1.0' ?>", "");
                    svgStr.push(svg);
                    n++;
                    if (chartRadioID === 'charts') {
                        if (isShowAvgOcc(opt)) {
                            svg = vca.getPrinterFriendlyChart("lineChart" + n);
                        }
                    } else {
                        svg = timeCard.getSvgList("timeCardWrapper" + n);
                    }
                }
                kup.utils.block.popup('#main-charts', i18n('generating-download-file'));
                window.exportPeopleCountingPdf(svgStr.join(""), JSON.stringify(vca.currentReportInfo),
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
            loadSetUI: function() {

            },
            loadUpdateUI: function() {
                _kupReport.initOpt();
                _kupReport.updateUI.switchTreeView();
                _kupReport.updateUI.menuTab();
                _kupReport.updateUI.title();
                _kupReport.updateUI.dateRangePicker();
                _kupReport.updateUI.removeDropPlace();
                _kupReport.updateUI.defaultDropPlace();
                _kupReport.updateUI.showExport();
                _kupReport.updateUI.chartRadio();
                _kupReport.updateUI.showDiv();
            }
        };
    return _self;
})(jQuery, KUP);
