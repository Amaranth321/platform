KUP.widget.report.type.attention = (function($, kup) {
    var _kupReport = kup.widget.report,
        _self = {
            generateReport: function() {
                var opt = _kupReport.getOpt(),
                    map = _kupReport.getMap(),
                    kupOpt = kup.getOpt(),
                    KupEvent = kupOpt.event,
                    deviceManager = kup.utils.deviceManager,
                    i18n = kup.utils.i18n,
                    utils = kup.utils.default,
                    kendo = kup.utils.chart.kendo;

                var vcaEventType = map.vcaEventType[opt.reportType],
                    fromDate = opt.startDate,
                    toDate = opt.endDate,
                    selectedDeviceList = opt.selectedDevices[0].platformDeviceId,
                    selectedChannelList = opt.selectedDevices[0].channelId;

                if (selectedDeviceList.length <= 0 && selectedChannelList.length <= 0) {
                    utils.popupAlert(i18n('no-channel-selected'));
                    return;
                }
                //convert to UTC dates.
                var from = kendo.toString(utils.convertToUTC(opt.startDate), "ddMMyyyyHHmmss");
                var to = kendo.toString(utils.convertToUTC(opt.endDate), "ddMMyyyyHHmmss");

                kup.utils.block.popup('#main-charts', i18n('generate-report'));
                window.getAnalyticsReport("", vcaEventType, _kupReport.getDeviceGroups(), selectedDeviceList, "", from, to, {},
                    function(resp) {
                        var opt = _kupReport.getOpt(),
                            kupOpt = kup.getOpt(),
                            KupEvent = kupOpt.event,
                            deviceManager = kup.utils.deviceManager,
                            i18n = kup.utils.i18n,
                            utils = kup.utils.default,
                            kendo = kup.utils.chart.kendo;
                        kup.utils.block.close('#main-charts');
                        if (resp.result == "ok" && resp.data.length > 0) {
                            var dbEvents = resp.data || [];
                            opt.isSuccessReport = true;
                            _kupReport.setOpt(opt);
                            _kupReport.updateUI.showDiv();
                            window.vca.reportType = 'mix';
                            window.vca.attentionReport(dbEvents, fromDate, toDate);
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
            exportExcel: function(data) {
                var opt = _kupReport.getOpt(),
                    utils = kup.utils.default,
                    i18n = kup.utils.i18n;
                utils.popupAlert(i18n("attention's report don't support to export Excel"));
            },
            exportPdf: function() {
                var opt = _kupReport.getOpt(),
                    utils = kup.utils.default,
                    i18n = kup.utils.i18n;
                utils.popupAlert(i18n("attention's report don't support to export PDF"));
            },
            loadSetUI: function() {},
            loadUpdateUI: function() {
                _kupReport.type.peoplecounting.loadUpdateUI();
            }
        };
    return _self;
})(jQuery, KUP);
