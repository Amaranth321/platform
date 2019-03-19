function SecurityAlertsPage()
{
    var $pageMain = $(".security_alerts");

    var filters = {
        eventTypes: ["event-vca-object-detection"],
        deviceId: null,
        channelId: null,
        from: null,
        to: null,
        hideResolved: true
    };

    var kElements = {
        grid: null,
        searchDvcList: null,
        searchChnList: null,
        searchRangeFrom: null,
        searchRangeTo: null
    };

    var eventLookup = {};
    var currentTotalCount = 0;

    var allOption = localizeResource("all");

    var emptyDataSource = new kendo.data.DataSource({
        data: []
    });

    var generate = function ()
    {
        loading(true);
        DvcMgr.ready(function ()
        {
            initFilterBox();
            initGrid();
            loading(false);
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
        //reset device and channel
        filters.deviceId = null;
        filters.channelId = null;
        if (kElements.searchDvcList && kElements.searchChnList)
        {
            kElements.searchDvcList.select(0);
            kElements.searchChnList.setDataSource(emptyDataSource);
            kElements.searchChnList.text("");
        }

        //reset from and to
        var todayRange = utils.getTodayDateRange();
        filters.from = utils.toAPITimestamp(utils.convertToUTC(todayRange.from));
        filters.to = utils.toAPITimestamp(utils.convertToUTC(todayRange.to));
        if (kElements.searchRangeFrom && kElements.searchRangeTo)
        {
        	kElements.searchRangeFrom.max(todayRange.to);
        	kElements.searchRangeFrom.value(todayRange.from)
        	
        	kElements.searchRangeTo.min(todayRange.from);
        	kElements.searchRangeTo.value(todayRange.to);
        }

        //reset event type
        filters.eventType = null;
        if (kElements.eventTypes)
        {
            kElements.eventTypes.select(0);
        }

        //reset hide resolved
        filters.hideResolved = false;
        $pageMain.find(".filters .hide_resolved").prop("checked", filters.hideResolved);

        search();
    };

    var exportFilteredList = function (fileType)
    {
        exportAlerts(
            fileType,
            filters.deviceId,
            filters.channelId,
            filters.eventTypes,
            filters.from,
            filters.to,
            filters.hideResolved
        );
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

    var initFilterBox = function ()
    {
//        var dsEventTypes = [];
//        getEligibleEventTypes().forEach(function (typeName)
//        {
//            dsEventTypes.push({name: localizeResource(typeName), value: typeName});
//        });
//        kElements.eventTypes = $pageMain.find(".event_types").kendoDropDownList({
//            optionLabel: allOption,
//            dataTextField: "name",
//            dataValueField: "value",
//            dataSource: dsEventTypes
//        }).data("kendoDropDownList");

        /**
         * Devices
         *
         */
        kElements.searchDvcList = $pageMain.find(".device_list").kendoDropDownList({
            optionLabel: allOption,
            dataTextField: "name",
            dataValueField: "deviceId",
            suggest: true,
            dataSource: {
                transport: {
                    read: function (options)
                    {
                        options.success(DvcMgr.getUserDeviceList());
                    }
                }
            },
            change: function (e)
            {
                var dvcItem = this.dataItem();
                if (dvcItem == null || dvcItem.name == allOption)
                {
                    kElements.searchChnList.setDataSource(emptyDataSource);
                    return;
                }

                kElements.searchChnList.setDataSource(new kendo.data.DataSource({
                    data: getDeviceChannelList(dvcItem)
                }));
            }
        }).data("kendoDropDownList");

        kElements.searchChnList = $pageMain.find(".channel_list").kendoDropDownList({
            optionLabel: allOption,
            dataTextField: "name",
            dataValueField: "nodeCoreDeviceId",
            dataSource: emptyDataSource
        }).data("kendoDropDownList");

        /**
         *
         * Period
         *
         */
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

    var getColumnList = function ()
    {
        var columnList = [];

        if (kupapi.onCloud())
        {
            columnList.push({
                field: "ackStatus",
                title: localizeResource("status"),
                width: "110px",
                template: $("#setResolvedColTmpl").html()
            });
        }

        columnList.push({
            field: "timestamp",
            title: localizeResource("time"),
            width: "150px"
        });

        columnList.push({
            field: "localizedEventType",
            title: localizeResource("event-type")
        });

        columnList.push({
            field: "deviceName",
            title: localizeResource("device-name")
        });

        columnList.push({
            field: "channelName",
            title: localizeResource("channel")
        });

        columnList.push({
            field: "happenedAt",
            title: localizeResource("happen-at")
        });

        columnList.push({
            field: "sendVia",
            title: localizeResource("sendVia")
        });

        columnList.push({
            field: "sendVia",
            title: localizeResource("actions"),
            width: "150px",
            template: $("#securityAlertActionsTmpl").html()
        });

        return columnList;
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
                        getAlerts(
                            null,
                            filters.eventTypes,
                            options.data.skip,
                            options.data.take,
                            filters.deviceId,
                            filters.channelId,
                            filters.from,
                            filters.to,
                            filters.hideResolved,
                            function (responseData)
                            {
                            	console.info(responseData);
                                loading(false);
                                if (responseData.result != "ok")
                                {
                                    utils.throwServerError(responseData);
                                    options.success([]);
                                    return;
                                }

                                var editedList = responseData["alerts"].map(function (noti)
                                {
                                    eventLookup[noti.eventId] = noti;

                                    noti.timestamp = kendo.toString(new Date(noti.timeMillis), kupapi.TIME_FORMAT);
                                    noti.localizedEventType = localizeResource(noti.eventType);
                                    noti.deviceName = DvcMgr.getDeviceName(noti.deviceId);
                                    noti.channelName = DvcMgr.getChannelName(noti.deviceId, noti.channelId);
                                    noti.happenedAt = DvcMgr.getDevice(noti.deviceId).address;
                                    return noti;
                                });

                                currentTotalCount = responseData["totalcount"];
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
            columns: getColumnList()
        }).data("kendoGrid");
    };

    var getEligibleEventTypes = function ()
    {
        var eligibleList = [];
        $.each(backend.EventType, function (type, name)
        {
            var typeInfo = backend.EventTypeInfo[type];
            if (typeInfo.isSecurityVcaEvent)
            {
                eligibleList.push(typeInfo.typeName);
            }
        });
        return eligibleList
    };

    var getDeviceChannelList = function (dvcItem)
    {
        if (DvcMgr.isKaiNode(dvcItem))
        {
            return dvcItem.node.cameras;
        }
        else
        {
            var chnList = [];
            chnList.push({
                "name": "1",
                "nodeCoreDeviceId": 0
            });
            return chnList;
        }
    };

    var updateFilters = function ()
    {
        //eventTypes
        var selectedEventType = kElements.eventTypes.dataItem();
        if (selectedEventType.name == allOption)
        {
            filters.eventTypes = getEligibleEventTypes().join(",");
        }
        else
        {
            filters.eventTypes = selectedEventType.value;
        }

        //device and channel
        var dvcId = null;
        var chnId = null;
        if (kElements.searchDvcList && kElements.searchChnList)
        {
            var selectedDvc = kElements.searchDvcList.dataItem();
            var selectedChn = kElements.searchChnList.dataItem();
            dvcId = selectedDvc ? selectedDvc.id : null;
            chnId = selectedChn ? selectedChn.nodeCoreDeviceId : null;
        }
        filters.deviceId = dvcId;
        filters.channelId = chnId;


        //date range
        var dtFrom = null;
        var dtTo = null;
        if (kElements.searchRangeFrom && kElements.searchRangeTo)
        {
            dtFrom = kElements.searchRangeFrom.value();
            dtTo = kElements.searchRangeTo.value();
        }
        if (dtFrom == null && dtTo == null)
        {
            var todayRange = utils.getTodayDateRange();
            dtFrom = todayRange.from;
            dtTo = todayRange.to;
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

    var loading = function (loading)
    {
        kendo.ui.progress($pageMain, loading);
    };

    return {
        generate: generate,
        search: search,
        resetFilters: resetFilters,
        acknowledgeEvent: acknowledgeEvent,
        getEventDetails: getEventDetails,
        exportFilteredList: exportFilteredList
    }
}
