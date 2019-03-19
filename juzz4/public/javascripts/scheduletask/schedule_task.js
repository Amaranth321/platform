function ScheduledListingPage()
{
    var $pageMain = $(".scheduleTsk_listing");
    var getDevicesAPI = getUserDevices;

    var kElement = {
        tabstrip: null,
        otaScheduleGrid: null,
        searchRangeFrom: null,
        searchRangeTo: null
    };

    var searchInputs = {
        otaScheduleName: null
    };

    var filters = {
        from: null,
        to: null,
    };

    var naText = localizeResource("n/a");

    /**
     *
     * Public functions
     *
     */

    var generate = function ()
    {
        initTapStrip();
    };

    var addScheduleTask = function ()
    {
        var contentPage = "/scheduletask/add";
        var title = localizeResource("assign-ota-schedule");
        if (kupapi.onCloud())
        {
            utils.openPopup(title, contentPage, 400, 500, true, refreshOTAGrid);
        }
    };

    var editSchedule = function (scheduleId)
    {
        var contentPage = "/scheduletask/detail/" + scheduleId;
        utils.openPopup(localizeResource("ota-schedule-detail"), contentPage, 700, null, true, refreshOTAGrid);
    };

    var deleteSchedule = function (scheduleId)
    {
        utils.popupConfirm(localizeResource("confirmation"), localizeResource("confirm-delete") + "?",
            function (choice)
            {
                if (choice)
                {
                    loading(true);
                    deleteNodeUpdateSchedule(scheduleId, function (responseData)
                    {
                        loading(false)
                        if (responseData.result != "ok")
                        {
                            utils.throwServerError(responseData);
                            return;
                        }
                        refreshOTAGrid();
                        utils.slideDownInfo(localizeResource("delete-successful"));
                    }, null);
                }
            });
    };

    var clearSearch = function ()
    {
        //reset from and to
        var todayRange = utils.getTodayDateRange();
        filters.from = utils.toAPITimestamp(utils.convertToUTC(todayRange.from));
        filters.to = utils.toAPITimestamp(utils.convertToUTC(todayRange.to));
        if (kElement.searchRangeFrom && kElement.searchRangeTo)
        {
            kElement.searchRangeFrom.max(todayRange.to);
            kElement.searchRangeFrom.value(todayRange.from);

            kElement.searchRangeTo.min(todayRange.from);
            kElement.searchRangeTo.value(todayRange.to);
        }
        refreshOTAGrid();
    };

    var refreshOTAGrid = function ()
    {
        if (kElement.otaScheduleGrid)
        {
            kElement.otaScheduleGrid.dataSource.read();
        }
    };
    /**
     *
     * Private functions
     *
     */

    var initTapStrip = function ()
    {
        $pageMain.find(".tab_strip").show();
        kElement.tabstrip = $pageMain.find(".tab_strip").kendoTabStrip({
            animation: {
                open: {
                    effects: "fadeIn"
                }
            },
            change: function (e)
            {
                var selectedTab = kElement.tabstrip.select();
                switch ($(selectedTab).index())
                {
                    case 0:
                        if (!kElement.otaScheduleGrid)
                        {
                            initOTAScheduleGrid();
                        }
                        break;

                    case 1:
                        break;
                }
            }
        }).data("kendoTabStrip");
        kElement.tabstrip.select(0);
    };

    var initOTAScheduleGrid = function ()
    {
        if (kupapi.onCloud())
        {
            initScheduledOTA();
        }
    };

    var initScheduledOTA = function ()
    {
        initOTAFilter();
        onOTAFilterTimeChanged();

        var columnList = [
            {
                field: "time",
                title: localizeResource("scheduled-time"),
            },
            {
                field: "numberOfNodes",
                title: localizeResource("no-of-nodes"),
            },
            {
                field: "status",
                title: localizeResource("status"),
            },
            {
                field: "scheduledTime",
                title: localizeResource("actions"),
                template: $("#scheduledActionsTmpl").html(),
                width: "180px"
            }
        ];

        kElement.otaScheduleGrid = $pageMain.find(".otaSchedule_grid").kendoGrid({
            dataSource: {
                transport: {
                    read: function (options)
                    {

                        listNodeUpdateSchedules(filters.from, filters.to, function (responseData)
                        {
                            if (responseData.result != "ok")
                            {
                                utils.throwServerError(responseData);
                                return;
                            }

                            var displayList = [];
                            $.each(responseData.tasks, function (i, scheduleTsk)
                            {
                                scheduleTsk.time = kendo.toString(new Date(scheduleTsk.scheduledTime), kupapi.TIME_FORMAT);
                                if(scheduleTsk.scheduledNodeIds)
                                    scheduleTsk.numberOfNodes = scheduleTsk.scheduledNodeIds.length;
                                else
                                    scheduleTsk.numberOfNodes = 0;
                                scheduleTsk.status = localizeResource(scheduleTsk.status.toLowerCase());
                                displayList.push(scheduleTsk);
                            });
                            options.success(displayList);
                        });
                    }
                },
                pageSize: 15
            },
            pageable: {
                input: true,
                numeric: false,
                pageSizes: [15, 30, 50],
                refresh: true
            },
            sortable: true,
            selectable: true,
            resizable: false,
            height: calculateGridHeight() + "px",
            columns: columnList
        }).data("kendoGrid");
    };

    var initOTAFilter = function ()
    {
        var todayRange = utils.getTodayDateRange();
        kElement.searchRangeFrom = $pageMain.find(".ota_period_from").kendoDateTimePicker({
            interval: 60,
            format: "dd/MM/yyyy HH:mm",
            timeFormat: "HH:mm",
            value: todayRange.from,
            change: function (e)
            {
                var startDate = kElement.searchRangeFrom.value();
                if (startDate)
                {
                    kElement.searchRangeTo.min(new Date(startDate));
                }
                onOTAFilterTimeChanged();
            }
        }).data("kendoDateTimePicker");

        kElement.searchRangeTo = $pageMain.find(".ota_period_to").kendoDateTimePicker({
            interval: 60,
            format: "dd/MM/yyyy HH:mm",
            timeFormat: "HH:mm",
            value: todayRange.to,
            change: function (e)
            {
                var endDate = kElement.searchRangeTo.value();
                if (endDate)
                {
                    kElement.searchRangeFrom.max(new Date(endDate));
                }
                onOTAFilterTimeChanged();
            }
        }).data("kendoDateTimePicker");

        kElement.searchRangeFrom.max(kElement.searchRangeTo.value());
        kElement.searchRangeTo.min(kElement.searchRangeFrom.value());
    };

    var onOTAFilterTimeChanged = function ()
    {
        //date range
        var dtFrom = null;
        var dtTo = null;
        if (kElement.searchRangeFrom && kElement.searchRangeTo)
        {
            dtFrom = kElement.searchRangeFrom.value();
            dtTo = kElement.searchRangeTo.value();
        }
        var todayRange = utils.getTodayDateRange();
        if (dtFrom == null)
        {
            dtFrom = todayRange.from;
            kElement.searchRangeFrom.max(dtFrom);
            kElement.searchRangeFrom.value(dtFrom);
        }
        if (dtTo == null)
        {
            dtTo = todayRange.to;
            kElement.searchRangeTo.value(dtTo);
        }
        filters.from = utils.toAPITimestamp(utils.convertToUTC(dtFrom));
        filters.to = utils.toAPITimestamp(utils.convertToUTC(dtTo));
    }



    var getAssignedLabels = function (nodeDevice)
    {
        //labels are assigned to cameras, not the node itself
        var cameraLabelList = [];
        $.each(nodeDevice.channelLabels, function (i, assignment)
        {
            $.each(assignment.labels, function (i, labelId)
            {
                var labelObj = LabelMgr.getLabelById(labelId);

                if (cameraLabelList.indexOf(labelObj.name) == -1)
                {
                    cameraLabelList.push(labelObj.name);
                }
            });
        });
        cameraLabelList.sort();
        return cameraLabelList;
    };

    var calculateGridHeight = function ()
    {
        var offset = $pageMain.find(".default_title_bar").height() + 290;
        return $(window).height() - offset;
    };

    var loading = function (loading)
    {
        kendo.ui.progress($pageMain, loading);
    };

    return {
        generate: generate,
        editSchedule: editSchedule,
        deleteSchedule: deleteSchedule,
        addScheduleTask: addScheduleTask,

        clearSearch: clearSearch,
        refreshOTAGrid: refreshOTAGrid
    }
}