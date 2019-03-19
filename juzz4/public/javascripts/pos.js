/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var pos = {};
var label = null;
pos.filterParams = {
    siteName: "",
    startDateStr: "",
    endDateStr: "",
    localStartDate: "",
    localEndDate: ""
}
pos.salesData = [];
pos.updateGridId = null;
pos.actualPOSData = [];
pos.calledFrom = location.href;

pos.initManual = function() {
    var ds = new kendo.data.DataSource({
        transport: {
            read: function(options) {
                getPosSalesReport("",
                        pos.filterParams.startDateStr,
                        pos.filterParams.endDateStr,
                        pos.filterParams.siteName, "", function(responseData) {
                            pos.salesData = [];
                            pos.actualPOSData = [];
                            if (responseData.result === "ok" && responseData.sales !== null) {
                                $.each(responseData.sales, function(index, data) {
                                    var date1 = utils.convertUTCtoLocal(kendo.parseDate(data.sales.time, kupapi.TIME_FORMAT));
                                    var localTime = kendo.toString(date1, kupapi.TIME_FORMAT);
                                    var salesData = {
                                        time: localTime,
                                        count: data.sales.count,
                                        amount: data.sales.amount
                                    }
                                    pos.salesData.push(salesData);
                                });
                                pos.actualPOSData = pos.salesData;
                                options.success(pos.salesData);
                            } else {
                                options.success([]);
                                utils.throwServerError(responseData);
                            }
                        });
            }
        },
        pageSize: 15
    });

    var POSDataGrid = $("#manualGrid").kendoGrid({
        dataSource: ds,
        pageable: {
            input: true,
            numeric: false,
            pageSizes: [15, 30, 50],
            refresh: true
        },
        sortable: false,
        filterable: false,
        selectable: true,
        resizable: false,
        autoBind: false,
        toolbar: kendo.template($("#posGridToolbar").html()),
        columns: [
            {field: "time", title: localizeResource("datetime")},
            {field: "count", title: localizeResource("no-of-receipt")},
            {field: "amount", title: localizeResource("sales-amount")}
        ]
    }).data("kendoGrid");

    pos.refreshGrid = function() {
        POSDataGrid.dataSource.read();
    }

    var labelList = $("#labelList").kendoDropDownList({
        dataSource: {
            transport: {
                read: function(options) {
                    getUserAccessibleLabels(function(responseData)
                    {
                        var labelNames = [];
                        $.each(responseData.labels, function(i, labelObj)
                        {
                            if(labelObj.type == "STORE")
                            {
                                labelNames.push(labelObj.name);
                            }
                        });

                        if(labelNames.length > 0)
                        {
                            $("#manualGrid").show();
                        }
                        options.success(labelNames);
                        
                        //set parameters
                        if (pos.setFilterParams()) {
                            pos.refreshGrid();
                        }
                    });
                }
            }
        }
    }).data("kendoMultiSelect");

    utils.createDateTimeRangeSelection("startDateTime", "endDateTime");
    var start = $("#startDateTime").data("kendoDateTimePicker");
    var end = $("#endDateTime").data("kendoDateTimePicker");
    start.value("");
    end.value("");

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
    if (typeof startDate === "undefined" || startDate == null)
        startDate = new Date();
    if (typeof endDate === "undefined" || endDate == null)
        endDate = new Date();

    $("#startDateTime").data("kendoDateTimePicker").value(
            new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate(), 0, 0, 0));
    $("#endDateTime").data("kendoDateTimePicker").value(
            new Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate(), 23, 59, 59));
}

pos.applyFilter = function() {
    var selectedLabel = $("#labelList").data("kendoDropDownList").dataItem();
    if(!selectedLabel){
        utils.popupAlert(localizeResource('create-label-pos'));
        return;
    }
    if (pos.setFilterParams()) {
        pos.refreshGrid();
    }
}

pos.setFilterParams = function() {
    pos.filterParams.siteName = $("#labelList").data("kendoDropDownList").value();
    var startTimeValue = $("#startDateTime").data("kendoDateTimePicker").value();
    var endTimeValue = $("#endDateTime").data("kendoDateTimePicker").value();
    var invalidDates = (startTimeValue == null || endTimeValue == null);
    if (invalidDates) {
        utils.popupAlert(localizeResource('invalid-dates'));
        return false;
    }
    pos.filterParams.localStartDate = startTimeValue;
    pos.filterParams.localEndDate = endTimeValue;
    pos.filterParams.startDateStr = kendo.toString(utils.convertToUTC(startTimeValue), "ddMMyyyyHHmmss");
    pos.filterParams.endDateStr = kendo.toString(utils.convertToUTC(endTimeValue), "ddMMyyyyHHmmss");
    return true;
}

pos.editPOSData = function() {
    var contentPage = "/report/posedit";
    utils.openPopup(localizeResource("import-sales-data"), contentPage, null, null, true, function() {
        var $grid = $("#manualGrid").data("kendoGrid");
        if($grid)
        {
            $grid.dataSource.read();
        }
        else
        {
            window.location.reload();
        }
    });
}

pos.initPOSPopup = function(divId, POSData, POSfilterParam) {
    pos.filterParams = POSfilterParam;
    pos.updateGridId = divId;

    if (POSData.length <= 0) {
        POSData = pos.constructPOSFakeData(pos.filterParams.localStartDate, pos.filterParams.localEndDate);
    }
    var POSDataGrid = $("#" + pos.updateGridId).kendoGrid({
        dataSource: {
            data: POSData,
            pageSize: 15,
            schema: {
                model: {
                    fields: {
                        time: {editable: false},
                        count: {type: "number"},
                        amount: {type: "number"}
                    }
                }
            }
        },
        pageable: {
            input: true,
            numeric: false,
            pageSizes: [15, 30, 50],
            refresh: true
        },
        sortable: false,
        filterable: false,
        resizable: false,
        toolbar: kendo.template($("#POSSaveGridToolbar").html()),
        columns: [
            {field: "time", title: localizeResource("datetime")},
            {field: "count", title: localizeResource("no-of-receipt"), editor: pos.countFormat},
            {field: "amount", title: localizeResource("sales-amount"), editor: pos.amountFormat}
        ],
        editable: true
    }).data("kendoGrid");
}

pos.countFormat = function(container, options) {
	$('<input name="' + options.field + '"/>')
    	.appendTo(container)
        .kendoNumericTextBox({
            decimals: 0,
            step: 1,
            min: 0
        });
}

pos.amountFormat = function(container, options) {
	$('<input name="' + options.field + '"/>')
    	.appendTo(container)
        .kendoNumericTextBox({
            min: 0
        });
}

pos.updatePOS = function() {
    if (utils.isNullOrEmpty(pos.filterParams.siteName)) {
        utils.popupAlert(localizeResource('label-missing'));
        return;
    }
    var displayedData = $("#" + pos.updateGridId).data("kendoGrid").dataSource.data();
    var updatedJsonObj = [];

    //check if pos data has been changed, save only if changed
    var hasChanges = false;
    $.each(pos.actualPOSData, function(index, value) {
        if (value.amount !== displayedData[index].amount
                || value.count !== displayedData[index].count)
            hasChanges = true;
        if (hasChanges)
            return false;
    });
    if (hasChanges) {
        $.each(displayedData, function(value, data) {
            var dateObj = kendo.parseDate(data.time, kupapi.TIME_FORMAT);
            var obj = {
                time: kendo.toString(utils.convertToUTC(dateObj), "ddMMyyyyHHmmss"),
                count: data.count,
                amount: data.amount
            }
            updatedJsonObj.push(obj);
        });
        var updatedPOSJsonString = JSON.stringify(updatedJsonObj);
        updatePOSSalesData("", pos.filterParams, updatedPOSJsonString, function(response) {
            if (response.result === "ok") {
                if (pos.calledFrom.indexOf("report/posconfig") !== -1) { //called from pos management page
                    pos.closePOSPopup();
                    pos.refreshGrid();
                    utils.slideDownInfo(localizeResource('update-successful'));
                }
                else { //called from people counting report
                    console.log("called from people counting report POS.js");
                    if(typeof vca != "undefined")
                    {
                        vca.showHowToImport = true;
                    }
                    $(".daterangepicker .applyBtn").trigger("click");
                    pos.closePOSPopup();
                }
            }
        }, null);
    } else {
        utils.popupAlert(localizeResource('pos-no-change'), function() {
            pos.closePOSPopup();
        });

    }
}

pos.closePOSPopup = function() {
    $("#canclePOS").closest(".k-window-content").data("kendoWindow").close();
}

pos.constructPOSFakeData = function(fromDate, toDate) {

    if(fromDate >= toDate)
    {
        return [];
    }

    function generatePOSData(dateObj)
    {
        return {
            time: kendo.toString(new Date(dateObj), kupapi.TIME_FORMAT),
            count: 0,
            amount: 0
        };
    }

    var POSFakeData = [];
    pos.actualPOSData = [];
    var movingDate = new Date(fromDate);
    do {
        POSFakeData.push(generatePOSData(movingDate));
        movingDate.setHours(movingDate.getHours() + 1);
    } while (movingDate < toDate);

    pos.actualPOSData = POSFakeData;
    return POSFakeData;
}

pos.downloadPOSPDF = function(){
    window.open(responseData["download-url"], '_blank');
    window.focus();
    
}