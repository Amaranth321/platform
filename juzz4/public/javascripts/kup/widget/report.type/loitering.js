KUP.widget.report.type.loitering = (function($, kup) {
    var _kupReport = kup.widget.report,
        _self = {
            generateReport: function() {
                _kupReport.type.intrusion.generateReport();
            },
            exportPdf: function() {
                _kupReport.type.intrusion.exportPdf();
            },
            exportCSV: function(data) {
                _kupReport.type.intrusion.exportCSV(data);
            },
            loadSetUI: function() {},
            loadUpdateUI: function() {
                 _kupReport.type.peoplecounting.loadUpdateUI();
            }
        };
    return _self;
})(jQuery, KUP);