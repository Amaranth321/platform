function LabelNotifications()
{
    var $pageMain = $(".label_noti");

    var configs = {
        labelEventTypes: [
            backend.EventType.OCCUPANCY_LIMIT
        ]
    };

    var labelLookupMap = {};
    var storeLabelList = [];
    var eventLookup = {};
    var currentTotalCount = 0;

    var filters = {
        eventType: null,
        labelId: null,
        from: null,
        to: null,
        hideResolved: false
    };

    var kElements = {
        grid: null,
        searchRangeFrom: null,
        searchRangeTo: null
    };

    var generate = function ()
    {
        prepareData(function ()
        {
            initFilterBox();
            initGrid();
        });
    };

    var search = function ()
    {
        if (kElements.grid)
        {
        	kElements.grid.dataSource.page(0);
        }
    };

    var resetFilters = function ()
    {
        //reset label
        if (kElements.labelList)
        {
            kElements.labelList.select(0);
        }

        //reset type
        if (kElements.eventTypes)
        {
            kElements.eventTypes.select(0);
        }

        //reset from and to
        var todayRange = utils.getTodayDateRange();
        filters.from = utils.toAPITimestamp(utils.convertToUTC(todayRange.from));
        filters.to = utils.toAPITimestamp(utils.convertToUTC(todayRange.to));
        if (kElements.searchRangeFrom && kElements.searchRangeTo)
        {
        	kElements.searchRangeFrom.max(todayRange.to);
            kElements.searchRangeFrom.value(todayRange.from);
            
            kElements.searchRangeTo.min(todayRange.from);
            kElements.searchRangeTo.value(todayRange.to);
        }

        //reset hide resolved
        filters.hideResolved = false;
        $pageMain.find(".filters .hide_resolved").prop("checked", filters.hideResolved);

        search();
    };

    var acknowledgeEvent = function (eventId)
    {
        utils.popupConfirm(
            localizeResource("confirmation"),
            localizeResource("confirm-set-resolved"),
            function (proceed)
            {
                if (!proceed)
                {
                    return;
                }

                loading(true);
                acknowledgeNotification(eventId, "", function (responseData)
                {
                    loading(false);
                    search();
                });
            }
        );
    };

    var getEventDetails = function (eventId)
    {
        return eventLookup[eventId];
    };

    var prepareData = function (ready)
    {
        loading(true);
        getUserAccessibleLabels(function (responseData)
        {
            if (responseData.result != "ok")
            {
                utils.throwServerError(responseData);
                ready();
                return;
            }

            responseData.labels.forEach(function (labelObj)
            {
                if (labelObj.type == "STORE")
                {
                    storeLabelList.push(labelObj);
                    labelLookupMap[labelObj.labelId] = labelObj;
                }
            });

            storeLabelList.sort(function (l1, l2)
            {
                return l1.name < l2.name ? -1 : (l1.name > l2.name ? 1 : 0);
            });

            ready();
        });
    };

    var initFilterBox = function ()
    {
        //built event type list
        var supportedEventTypes = [];
        configs.labelEventTypes.forEach(function (type)
        {
            supportedEventTypes.push({
                name: localizeResource(backend.EventTypeInfo[type].displayName),
                value: backend.EventTypeInfo[type].typeName
            });
        });

        kElements.eventTypes = $pageMain.find("input.event_types").kendoDropDownList({
            optionLabel: localizeResource("all"),
            dataTextField: "name",
            dataValueField: "value",
            dataSource: supportedEventTypes
        }).data("kendoDropDownList");

        kElements.labelList = $pageMain.find("input.label_list").kendoDropDownList({
            optionLabel: localizeResource("all"),
            dataTextField: "name",
            dataValueField: "labelId",
            dataSource: storeLabelList
        }).data("kendoDropDownList");

        var todayRange = utils.getTodayDateRange();
        kElements.searchRangeFrom = $pageMain.find(".period_from").kendoDateTimePicker({
            interval: 60,
            format: "dd/MM/yyyy HH:mm",
            timeFormat: "HH:mm",
            value: todayRange.from,
            change: function (e)
            {
                var startDate = kElements.searchRangeFrom.value();
                if (startDate)
                {
                    kElements.searchRangeTo.min(new Date(startDate));
                }
            }
        }).data("kendoDateTimePicker");

        kElements.searchRangeTo = $pageMain.find(".period_to").kendoDateTimePicker({
            interval: 60,
            format: "dd/MM/yyyy HH:mm",
            timeFormat: "HH:mm",
            value: todayRange.to,
            change: function (e)
            {
                var endDate = kElements.searchRangeTo.value();
                if (endDate)
                {
                    kElements.searchRangeFrom.max(new Date(endDate));
                }
            }
        }).data("kendoDateTimePicker");

        kElements.searchRangeFrom.max(kElements.searchRangeTo.value());
        kElements.searchRangeTo.min(kElements.searchRangeFrom.value());
        kElements.searchRangeTo.max(todayRange.to);
    };

    var initGrid = function ()
    {
        kElements.grid = $pageMain.find(".grid").kendoGrid({
            dataSource: {
                transport: {
                    read: function (options)
                    {
                        loading(true);
                        updateFilters();
                        getLabelNotifications(
                            null,
                            filters.from,
                            filters.to,
                            filters.eventType,
                            filters.labelId,
                            options.data.skip,
                            options.data.take,
                            filters.hideResolved,
                            function (responseData)
                            {
                                loading(false);
                                if (responseData.result != "ok")
                                {
                                    utils.throwServerError(responseData);
                                    options.success([]);
                                    return;
                                }

                                var editedList = responseData["notifications"].map(function (noti)
                                {
                                    eventLookup[noti.eventId] = noti;

                                    noti.timestamp = kendo.toString(new Date(noti.time), kupapi.TIME_FORMAT);
                                    noti.labelName = labelLookupMap[noti.labelId].name;
                                    noti.localizedEventType = localizeResource(noti.eventType);
                                    noti.sentVia = noti.notifiedMethods.join(", ");
                                    noti.info = stringifyNotificationData(noti.eventType, noti.notificationData);
                                    return noti;
                                });

                                currentTotalCount = responseData["total-count"];
                                options.success(editedList);
                            }
                        );
                    }
                }, schema: {
                    total: function ()
                    {
                        return currentTotalCount;
                    }
                },
                pageSize: 15,
                serverPaging: true
            },
            pageable: {
                input: true,
                numeric: false,
                pageSizes: [15, 30, 50],
                refresh: true
            },
            height: calculateGridHeight() + "px",
            sortable: false,
            selectable: true,
            resizable: false,
            columns: [
                {
                    field: "ackStatus",
                    title: localizeResource("status"),
                    width: "110px",
                    template: $("#setResolvedColTmpl").html()
                },
                {
                    field: "timestamp",
                    title: localizeResource("time"),
                    width: "150px"
                },
                {
                    field: "localizedEventType",
                    title: localizeResource("event-type"),
                    width: "200px"
                },
                {
                    field: "labelName",
                    title: localizeResource("store-label"),
                    width: "200px"
                },
                {
                    field: "sentVia",
                    title: localizeResource("sendVia")
                },
                {
                    field: "info",
                    title: localizeResource("settings-info")
                },
                {
                    field: "sendVia",
                    title: localizeResource("actions"),
                    width: "150px",
                    template: $("#labelNotiActionsTmpl").html()
                }
            ]
        }).data("kendoGrid");
    };

    var updateFilters = function ()
    {
        //label
        var selectedLabel = kElements.labelList.dataItem();
        filters.labelId = null;
        if (selectedLabel)
        {
            filters.labelId = selectedLabel.labelId;
        }

        //event type
        var selectedEventType = kElements.eventTypes.dataItem();
        filters.eventType = null;
        if (selectedEventType)
        {
            filters.eventType = selectedEventType.value;
        }

        //date range
        var dtFrom = null;
        var dtTo = null;
        if (kElements.searchRangeFrom && kElements.searchRangeTo)
        {
            dtFrom = kElements.searchRangeFrom.value();
            dtTo = kElements.searchRangeTo.value();
        }
        var todayRange = utils.getTodayDateRange();
        if (dtFrom == null)
        {
            dtFrom = todayRange.from;
            kElements.searchRangeFrom.max(dtFrom);
            kElements.searchRangeFrom.value(dtFrom);
        }
        if (dtTo == null)
        {
            dtTo = todayRange.to;
            kElements.searchRangeTo.value(dtTo);
        }
        filters.from = utils.toAPITimestamp(utils.convertToUTC(dtFrom));
        filters.to = utils.toAPITimestamp(utils.convertToUTC(dtTo));

        //hide resolved
        filters.hideResolved = $pageMain.find(".filters .hide_resolved").is(":checked");
    };

    var calculateGridHeight = function ()
    {
        var offset = $pageMain.find(".filter_box").height() + 170;
        return $(window).height() - offset;
    };

    var stringifyNotificationData = function (eventTypeName, notiData)
    {
        var eventType = BackendUtil.parseEventType(eventTypeName);
        switch (eventType)
        {
            case backend.EventType.OCCUPANCY_LIMIT:
                var info = localizeResource("occupancy-limit") + "=" + notiData.occupancyLimit;
                if (!utils.isNullOrEmpty(notiData.message))
                {
                    info += ", " + localizeResource("message") + "='" + notiData.message + "'";
                }
                return info;
        }
    };

    var loading = function (loading)
    {
        kendo.ui.progress($pageMain, loading);
    };

    return {
        generate: generate,
        search: search,
        resetFilters: resetFilters,
        acknowledgeEvent: acknowledgeEvent,
        getEventDetails: getEventDetails
    }
}