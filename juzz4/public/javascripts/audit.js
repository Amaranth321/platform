var audit = {};
audit.parmasdata = [];
audit.auditDetails = [];
var filterParams = {
    timeobject: "",
    bucketName: "",
    userName: "",
    serviceName: "",
    remoteIp: "",
    startDateStr: "",
    endDateStr: ""
};

var nowDt = new Date();
audit.today = {
    start: new Date(nowDt.getFullYear(), nowDt.getMonth(), nowDt.getDate(), 0, 0, 0),
    end: new Date(nowDt.getFullYear(), nowDt.getMonth(), nowDt.getDate(), 23, 59, 59)
}

audit.initializeGrid = function (divId, showFilter) {
	// sync time range with ui
	filterParams.startDateStr = kendo.toString(audit.today.start, "ddMMyyyyHHmmss");
	filterParams.endDateStr = kendo.toString(audit.today.end, "ddMMyyyyHHmmss");
	
    var auditCount = 0;
    var dsAudit = new kendo.data.DataSource({
        transport: {
            read: function (options) {
                getAuditLog("",
                    filterParams.bucketName,
                    filterParams.userName,
                    filterParams.serviceName,
                    filterParams.remoteIp,
                    options.data.skip,
                    options.data.take,
                    filterParams.startDateStr,
                    filterParams.endDateStr,
                    function onSuccess(responseData) {
                        var results = [];
                        if (responseData.result == "ok" && responseData.auditLogs != null) {
                            $.each(responseData.auditLogs, function (index, audit) {
                                var localTime = kendo.parseDate(new Date(audit.timeobject), kupapi.TIME_FORMAT);
                                var auditEdited = {
                                    "id": audit._id,
                                    "timeobject": kendo.toString(localTime, kupapi.TIME_FORMAT),
                                    "bucketName": audit.bucketName,
                                    "userName": audit.userName,
                                    "serviceName": audit.serviceName,
                                    "remoteIp": audit.remoteIp,
                                    "headers": audit.headers,
                                    "params": audit.params,
                                    "exception": audit.exception,
                                    "result": utils.isNullOrEmpty(audit.result) ? "" : audit.result
                                }
                                results.push(auditEdited);
                            });
                            auditCount = responseData.totalcount;
                            options.success(results);
                        } else {
                            options.success([]);
                            utils.throwServerError(responseData);

                        }
                    }, null);
            }
        },
        pageSize: 15,
        serverPaging: true,
        schema: {
            total: function () {
                return auditCount;
            }
        }
    });
    var auditGrid = $("#" + divId).kendoGrid({
        dataSource: dsAudit,
        pageable: {
            input: true,
            numeric: false,
            pageSizes: [15, 30, 50],
            refresh: true
        },
        sortable: true,
        selectable: true,
        resizable: false,
        toolbar: kendo.template($("#toolbarTemplate").html()),
        height: function () {
            return  $(window).height() + "px";
        },
        columns: [
            {field: "bucketName", title: localizeResource('company-name'), width: "150px"},
            {field: "userName", title: localizeResource('username'), width: "150px"},
            {field: "serviceName", title: localizeResource('activity-type'), width: "150px"},
            {field: "timeobject", title: localizeResource('date'), width: "150px"},
            {field: "remoteIp", title: localizeResource('ip'), width: "150px"},
            {field: "result", title: localizeResource('result'), width: "150px"},
            {command: [
                {text: localizeResource('details'), click: showDetails}
            ], title: localizeResource('actions'), width: "200px"}
        ]
    }).data("kendoGrid");

    if (showFilter) {
        audit._prepareFilterElements(auditGrid);
    }
};

audit._prepareFilterElements = function (auditGrid) {

    utils.createDateTimeRangeSelection("startDateTime", "endDateTime");
    var start = $("#startDateTime").data("kendoDateTimePicker");
    var end = $("#endDateTime").data("kendoDateTimePicker");

    //set today
    start.value(audit.today.start);
    end.value(audit.today.end);

    $("#startDateTime").attr("readonly", "readonly");
    $("#endDateTime").attr("readonly", "readonly");
    $("#startDateTime").kendoTooltip({
        filter: "a",
        width: 120,
        position: "top"
    }).data("kendoTooltip");
    $("#endDateTime").kendoTooltip({
        filter: "a",
        width: 120,
        position: "top"
    }).data("kendoTooltip");

    $("#clrFilter").click(function () {
        clearFilter(auditGrid);
    });
    $("#btnFilter").click(function () {
        setSelectedFilterParams(filterParams);
        auditGrid.dataSource.page(1);
    });

    $("#btnPdf").click(function () {
        setSelectedFilterParams(filterParams);
        exportAuditLog("pdf",
            filterParams.bucketName,
            filterParams.userName,
            filterParams.serviceName,
            filterParams.remoteIp,
            "",
            "",
            filterParams.startDateStr,
            filterParams.endDateStr);
    });
    $("#btnExcel").click(function () {
        setSelectedFilterParams(filterParams);
        exportAuditLog("xls",
            filterParams.bucketName,
            filterParams.userName,
            filterParams.serviceName,
            filterParams.remoteIp,
            "",
            "",
            filterParams.startDateStr,
            filterParams.endDateStr);
    });
};

function showDetails(e) {
    e.preventDefault();
    var dataItem = this.dataItem($(e.currentTarget).closest("tr"));
    getAuditLogDetails("", dataItem.id, function onSuccess(responseData) {
        audit.auditDetails = [];
        audit.parmasdata = [];
        if (responseData.result == "ok" && responseData.auditlog != null) {
            auditData = responseData.auditlog;
            $.each(auditData.params, function (index, item) {
                audit.parmasdata.push({
                    "name": index,
                    "value": item
                });
            });

            var localTime = kendo.parseDate(new Date(auditData.timeobject), kupapi.TIME_FORMAT);
            if (!utils.isNullOrEmpty(auditData.headers.host)) {
                audit.auditDetails.push({
                    "id": auditData._id,
                    "bucketId": auditData.bucketId,
                    "timeobject": kendo.toString(localTime, kupapi.TIME_FORMAT),
                    "bucketName": auditData.bucketName,
                    "userName": auditData.userName,
                    "serviceName": auditData.serviceName,
                    "remoteIp": auditData.remoteIp,
                    "userId": auditData.userId,
                    "host": auditData.headers.host.values[0],
                    "referer": auditData.headers.referer.values[0],
                    "params": auditData.params,
                    "exception": auditData.exception
                });
            } else {
                audit.auditDetails.push({
                    "id": auditData._id,
                    "bucketId": auditData.bucketId,
                    "timeobject": kendo.toString(localTime, kupapi.TIME_FORMAT),
                    "bucketName": auditData.bucketName,
                    "userName": auditData.userName,
                    "serviceName": auditData.serviceName,
                    "remoteIp": auditData.remoteIp,
                    "userId": auditData.userId,
                    "host": "",
                    "referer": "",
                    "exception": auditData.exception
                });
            }

            var contentPage = "/auditlog/auditdetails/" + dataItem.id;
            utils.openPopup(localizeResource('audit-detail'), contentPage, null, null, true, function()
            {

            });

        } else {
            utils.throwServerError(responseData);
        }
    }, null);
}

function clearFilter(auditGrid) {
    $("#compIdFilter").val("");
    $("#compNameFilter").val("");
    $("#userNameFilter").val("");
    $("#ipFilter").val("");
    $("#activityFilter").val("");
    var startDateTimePicker = $("#startDateTime").data("kendoDateTimePicker");
    startDateTimePicker.max(audit.today.end);
    startDateTimePicker.value(audit.today.start);
    
    var endDateTimePicker = $("#endDateTime").data("kendoDateTimePicker");
    endDateTimePicker.min(audit.today.start);
    endDateTimePicker.value(audit.today.end);
    
    // sync params with ui
	setSelectedFilterParams(filterParams);
	
    auditGrid.dataSource.page(1);
}
;

function setSelectedFilterParams(filterParams) {
    filterParams.bucketName = $("#compNameFilter").val();
    filterParams.userName = $("#userNameFilter").val();
    filterParams.serviceName = $("#activityFilter").val();
    filterParams.remoteIp = $("#ipFilter").val();
    //update range filters
    //empty time fields are allowed
    var startTimeValue = $("#startDateTime").data("kendoDateTimePicker").value();
    var endTimeValue = $("#endDateTime").data("kendoDateTimePicker").value();
    if (startTimeValue == null && endTimeValue == null) {
        filterParams.startDateStr = "";
        filterParams.endDateStr = "";
    } else {
        var invalidDates = (startTimeValue == null || endTimeValue == null);
        if (invalidDates) {
            utils.popupAlert(localizeResource('invalid-dates'));
            return;
        }

        filterParams.startDateStr = kendo.toString(startTimeValue, "ddMMyyyyHHmmss");
        filterParams.endDateStr = kendo.toString(endTimeValue, "ddMMyyyyHHmmss");
    }
}
;