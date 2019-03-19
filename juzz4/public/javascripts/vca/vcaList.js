/**
 * Requires vcaMgr.js and dvcMgr.js
 *
 * @param $vcaList      container as jQuery element
 * @param vcaTypeName   vca type name used in communications
 *
 * @author Aye Maung
 */
function VcaList($vcaList, vcaTypeName)
{
    //parse
    var vcaType = BackendUtil.parseVcaType(vcaTypeName);
    var listAllVca = (vcaType == null);
    var eventType = listAllVca ? null : VcaMgr.getVcaTypeInfo(vcaType).eventType;

    var dualProgramVcaList = [
        backend.VcaType.PEOPLE_COUNTING,
        backend.VcaType.AUDIENCE_PROFILING //add by RenZongKe for GEN2 Audience-profiling
    ];

    var kElement = {
        tabstrip: null,
        vcaGrid: null,
        eventGrid: null,
        searchDvcList: null,
        searchChnList: null,
        searchRangeFrom: null,
        searchRangeTo: null
    };

    var searchInputs = {
        device: null,
        channel: null,
        location: null
    };

    var dataLogFilters = {
        deviceId: null,
        channelId: null,
        from: null,
        to: null
    };

    var emptyDataSource = new kendo.data.DataSource({
        data: []
    });

    var gridHeight = 0;

    /**
     *
     * Public functions
     *
     */

    var generate = function ()
    {
        loading(true);
        gridHeight = calculateGridHeight();
        setTitle();
        initTapstrip();
        initVcaGrid();
        registerShortcuts();
    };

    var clearVcaSearch = function ()
    {
        $.each(searchInputs, function (name, $ele)
        {
            $ele.val("");
        });

        onVcaSearchChanged();
    };

    var addVca = function ()
    {
        if (listAllVca)
        {
            return;
        }

        var contentPage = "/vca/addnew?vcaType=" + vcaType;
        var winTitle = localizeResource("add-analytics");

        //dual programs
        if (dualProgramVcaList.indexOf(vcaType) != -1)
        {
            VcaConfigWins.programChoice.openWin(function (choice)
            {
                if (choice == null)
                {
                    return;
                }
                if(choice == VcaMgr.Program.KAI_X2 && vcaType == backend.VcaType.AUDIENCE_PROFILING){
                	////////
                	choice = "KAI_X3";
                }

                contentPage += "&program=" + choice;
                loading(true);
                console.info("contentPage::"+contentPage);
                utils.openPopup(winTitle, contentPage, null, null, true, function ()
                {
                    refreshVcaGrid();
                });
            });
        }
        else
        {
            loading(true);
            utils.openPopup(winTitle, contentPage, null, null, true, function ()
            {
                refreshVcaGrid();
            });
        }
    };

    var configureVca = function (instanceId, readonly)
    {
        loading(true);
        var inst = VcaMgr.getInstanceDetails(instanceId);
        var contentPage = "/vca/configure?vcaType=" + inst.vcaType +
                          "&instanceId=" + instanceId +
                          "&readonly=" + readonly +
                          "&program=" + inst.program;

        var winTitle = localizeResource("configuration");
        if (readonly)
        {
            winTitle += " (" + localizeResource("readonly-view") + ")";
        }

        utils.openPopup(winTitle, contentPage, null, null, true, function ()
        {
            refreshVcaGrid();
        });

        // readonly or not
        var timer = setInterval(function ()
        {
            var jqBox = $(".popup_wrapper .top_box");
            var sourcelabel = $(".viewSource");
            var sourceBox = $(".sourceBox");
            if (jqBox.length > 0)
            {
                clearInterval(timer);
                if (readonly)
                {
                    sourceBox.hide();
                    jqBox.hide();
                    setTimeout(function ()
                    {
                        if ($(".source").html() != "")
                        {
                            sourcelabel.show();
                        }
                    }, 500);
                }
                else
                {
                    sourceBox.show();
                    jqBox.show();
                    sourcelabel.hide();
                }
            }
        }, 500);
    };

    var activateVca = function (instanceId)
    {
        utils.popupConfirm(
            localizeResource('confirmation'),
            localizeResource('confirm-analytics-activate'),
            function (choice)
            {
                if (choice)
                {
                    loading(true);
                    kupapi.activateVca(instanceId, onActionSuccess);
                }
            });
    };

    var deactivateVca = function (instanceId)
    {
        utils.popupConfirm(
            localizeResource('confirmation'),
            localizeResource('confirm-analytics-deactivate'),
            function (choice)
            {
                if (choice)
                {
                    loading(true);
                    kupapi.deactivateVca(instanceId, onActionSuccess);
                }
            });
    };

    var deleteVca = function (instanceId)
    {
        utils.popupConfirm(
            localizeResource('confirmation'),
            localizeResource('confirm-analytics-delete'),
            function (choice)
            {
                if (choice)
                {
                    loading(true);
                    kupapi.removeVca(instanceId, onActionSuccess);
                }
            });
    };

    var openDebugger = function (instanceId)
    {
        var contentPage = "/vca/debug/" + instanceId;
        utils.openPopup(localizeResource('vca-debugger'), contentPage, null, null, true, function ()
        {
            debugWin.monitorFPS(false);
        });
    };

    var openErrorLog = function (instanceId)
    {
        var contentPage = "/vca/errorlog/" + instanceId;
        utils.openPopup(localizeResource('vca-logs'), contentPage, null, null, true, function ()
        {
        });
    };

    var openOccupancyGrid = function ()
    {
        if (!kupapi.onCloud() || vcaType != backend.VcaType.PEOPLE_COUNTING)
        {
            return;
        }

        var contentPage = "/vca/occupancygrid";
        utils.openPopup(localizeResource('monitor-occupancies'), contentPage, null, null, true, function ()
        {
            OccupancyGrid.stopMonitoring();
        });
    };

    var filterDataLogs = function ()
    {
    	kElement.eventGrid.dataSource.page(0);
    	kElement.eventGrid.dataSource.read();
    };

    var clearDataLogFilters = function ()
    {
        resetDataLogFilters();
        filterDataLogs();
    };

    var exportData = function (fileType)
    {
        exportDataLogs(
            fileType,
            eventType,
            dataLogFilters.deviceId,
            dataLogFilters.channelId,
            dataLogFilters.from,
            dataLogFilters.to);
    };

    /**
     *
     * Private functions
     *
     */

    var setTitle = function ()
    {
        var vcaTypeInfo = VcaMgr.getVcaTypeInfo(vcaType);
        var title = vcaTypeInfo ? vcaTypeInfo.displayName : "running-analytics-list";
        $vcaList.find(".header .title").text(localizeResource(title));
    };

    var initTapstrip = function ()
    {
        kElement.tabstrip = $vcaList.find(".tab_strip").kendoTabStrip({
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
                        //this has auto-refresh
                        break;

                    case 1:
                        if (!kElement.eventGrid)
                        {
                            initEventGrid();
                        }
                        else
                        {
                            kElement.eventGrid.dataSource.read();
                        }
                        break;
                }
            }
        }).data("kendoTabStrip");
    };

    var initVcaGrid = function ()
    {
        var columnList = [
            {
                field: "running",
                title: "&nbsp;",
                width: "30px",
                template: $("#runningStatusTmpl").html()
            },
            {
                field: "deviceName",
                title: localizeResource("device-name"),
                template: $("#deviceWithStatusTmpl").html()
            },
            {
                field: "channelName",
                title: localizeResource("channel"),
                template: $("#channelWithStatusTmpl").html()
            },
            {
                field: "address",
                title: localizeResource("location")
            },
            {
                field: "recurrenceRule",
                title: localizeResource("operating-schedule"),
                template: $("#recurrenceRuleTmpl").html()
            },
            {
                field: "program",
                title: localizeResource("exe-program"),
                template: "#= localizeResource(program) #",
                width: "80px"
            },
            {
                field: "enabled",
                title: localizeResource("actions"),
                width: "190px",
                template: $("#actionBtnGroupTmpl").html()
            },
            {
                field: "storeLabelName",
                groupHeaderTemplate: $("#storeLabelGroupHeaderTmpl").html()
            }
        ];

        if (listAllVca)
        {
            columnList.unshift(
                {
                    field: "vcaType",
                    title: localizeResource("type"),
                    width: "150px"
                }
            );
        }

        var dsVca = {
            transport: {
                read: function (options)
                {
                    //hide loading icon
                    kendo.ui.progress($vcaList.find(".vca_grid"), false);

                    //use typeName for communications
                    VcaMgr.getInstanceList(vcaType, function (instList)
                    {
                        options.success(instList);
                        loading(false);
                    });
                }
            },
            pageSize: 15
        };

        if (kupapi.onCloud())
        {
            dsVca.group = {
                field: "storeLabelName"
            };
        }

        kElement.vcaGrid = $vcaList.find(".vca_grid").kendoGrid({
            dataSource: dsVca,
            pageable: {
                input: true,
                numeric: false,
                pageSizes: [15, 30, 50],
                refresh: false
            },
            sortable: false,
            selectable: false,
            resizable: false,
            height: gridHeight + "px",
            columns: columnList
        }).data("kendoGrid");
        kElement.vcaGrid.hideColumn("storeLabelName");

        monitorVcaChanges();

        //setup search filters
        searchInputs.device = $("#inputDvcSearch");
        searchInputs.channel = $("#inputChnSearch");
        searchInputs.location = $("#inputLocSearch");

        searchInputs.device.keyup(onVcaSearchChanged);
        searchInputs.channel.keyup(onVcaSearchChanged);
        searchInputs.location.keyup(onVcaSearchChanged);
    };

    var monitorVcaChanges = function ()
    {
        if (kupapi.onKaiNode())
        {
            //periodic refresh.
            setInterval(refreshVcaGrid, 2000);
        }
        else
        {
            //refresh when change detected
            var vcaMonitor = new VcaMonitor(function ()
            {
                refreshVcaGrid();
            });
            vcaMonitor.start();
        }
    };

    var refreshVcaGrid = function ()
    {
    	kElement.vcaGrid.dataSource.page(0);
        kElement.vcaGrid.dataSource.read();
    };

    var registerShortcuts = function ()
    {
        var keyListener = new window.keypress.Listener();

        //monitor occupancies
        keyListener.simple_combo("ctrl alt d", function ()
        {
            openOccupancyGrid();
        });
    };

    var onVcaSearchChanged = function ()
    {
        var filter = {logic: "and", filters: []};

        //device
        var dvcTerm = searchInputs.device.val();
        if (!utils.isNullOrEmpty(dvcTerm))
        {
            filter.filters.push({field: "deviceName", operator: "contains", value: dvcTerm});
        }

        //channel
        var chnTerm = searchInputs.channel.val();
        if (!utils.isNullOrEmpty(chnTerm))
        {
            filter.filters.push({field: "channelName", operator: "contains", value: chnTerm});
        }

        //location
        var locTerm = searchInputs.location.val();
        if (!utils.isNullOrEmpty(locTerm))
        {
            filter.filters.push({field: "address", operator: "contains", value: locTerm});
        }

        kElement.vcaGrid.dataSource.filter(filter);
    };

    var initEventGrid = function ()
    {
        if (listAllVca)
        {
            return;
        }

        kElement.eventGrid = $vcaList.find(".event_grid").kendoGrid({
            dataSource: {
                transport: {
                    read: function (options)
                    {
                        refreshDataLogFilters();
                        console.info("eventType:"+eventType);
                        getAnalyticsReport("",
                            eventType,
                            null,
                            dataLogFilters.deviceId,
                            dataLogFilters.channelId,
                            dataLogFilters.from,
                            dataLogFilters.to,
                            null,
                            function (responseData)
                            {
                                if (responseData.result != "ok")
                                {
                                    utils.throwServerError(responseData);
                                    return;
                                }

                                var editedList = responseData.data.map(function (data)
                                {
                                    data.time = kendo.toString(new Date(data.time), kupapi.TIME_FORMAT);
                                    data.deviceName = DvcMgr.getDeviceName(data.deviceId);
                                    data.channelName = DvcMgr.getChannelName(data.deviceId, data.channelId);

                                    if (vcaType == backend.VcaType.CROWD_DETECTION)
                                    {
                                        data.activities = 0;
                                        if (data.tracks)
                                        {
                                            $.each(data.tracks, function (i, t)
                                            {
                                                data.activities += t.value;
                                            });
                                        }
                                    }

                                    return data;
                                });

                                options.success(editedList);
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
            sortable: false,
            selectable: false,
            resizable: false,
            height: gridHeight + "px",
            toolbar: kendo.template($("#dataLogHeaderBarTmpl").html()),
            columns: getDataLogColumnList()
        }).data("kendoGrid");

        initDataLogSearch();
    };

    var refreshDataLogFilters = function ()
    {
        //device and channel
        var dvcId = null;
        var chnId = null;
        if (kElement.searchDvcList && kElement.searchChnList)
        {
            var selectedDvc = kElement.searchDvcList.dataItem();
            var selectedChn = kElement.searchChnList.dataItem();
            dvcId = selectedDvc ? selectedDvc.id : null;
            chnId = selectedChn ? selectedChn.nodeCoreDeviceId : null;
        }

        dataLogFilters.deviceId = dvcId;
        dataLogFilters.channelId = chnId;

        //date range
        var dtFrom = null;
        var dtTo = null;
        if (kElement.searchRangeFrom && kElement.searchRangeTo)
        {
            dtFrom = kElement.searchRangeFrom.value();
            dtTo = kElement.searchRangeTo.value();
        }
        if (dtFrom == null && dtTo == null)
        {
            var todayRange = utils.getTodayDateRange();
            dtFrom = todayRange.from;
            dtTo = todayRange.to;
        }
        dataLogFilters.from = utils.toAPITimestamp(utils.convertToUTC(dtFrom));
        dataLogFilters.to = utils.toAPITimestamp(utils.convertToUTC(dtTo));
    };

    var resetDataLogFilters = function ()
    {
        //reset device and channel
        dataLogFilters.deviceId = null;
        dataLogFilters.channelId = null;
        if (kElement.searchDvcList && kElement.searchChnList)
        {
            kElement.searchDvcList.select(0);
            kElement.searchChnList.setDataSource(emptyDataSource);
            kElement.searchChnList.text("");
        }

        //reset from and to
        var todayRange = utils.getTodayDateRange();
        dataLogFilters.from = utils.toAPITimestamp(utils.convertToUTC(todayRange.from));
        dataLogFilters.to = utils.toAPITimestamp(utils.convertToUTC(todayRange.to));
        
        kElement.searchRangeFrom.max(todayRange.to);
        kElement.searchRangeFrom.value(todayRange.from);
        kElement.searchRangeTo.min(todayRange.from);
        kElement.searchRangeTo.value(todayRange.to);
    };

    var getDataLogColumnList = function ()
    {
        var columnList = [];

        //common
        columnList.push({field: "time", title: localizeResource("time"), width: "150px"});
        columnList.push({field: "deviceName", title: localizeResource("device-name"), width: "200px"});
        columnList.push({field: "channelName", title: localizeResource("channel"), width: "200px"});

        //vca-specific
        switch (vcaType)
        {
            case backend.VcaType.TRAFFIC_FLOW:
                columnList.push({field: "from", title: localizeResource("from")});
                columnList.push({field: "to", title: localizeResource("to")});
                columnList.push({field: "count", title: localizeResource("count")});
                break;

            case backend.VcaType.PEOPLE_COUNTING:
                columnList.push({field: "in", title: localizeResource("in")});
//                columnList.push({field: "avgOccupancy", title: localizeResource("avg-occupancy")});
                break;

            case backend.VcaType.PASSERBY:
                columnList.push({
                    field: "in",
                    title: localizeResource("count"),
                    template: "#= data.in + data.out #"
                });
                break;

            case backend.VcaType.CROWD_DETECTION:
                // columnList.push({field: "activities", title: localizeResource("activities")});
                break;

            case backend.VcaType.AUDIENCE_PROFILING:
                columnList.push({field: "male", title: localizeResource("male")});
                columnList.push({field: "female", title: localizeResource("female")});
                columnList.push({field: "age1", title: localizeResource("age-group-1-count")});
                columnList.push({field: "age2", title: localizeResource("age-group-2-count")});
                columnList.push({field: "age3", title: localizeResource("age-group-3-count")});
                columnList.push({field: "age4", title: localizeResource("age-group-4-count")});
                columnList.push({field: "happy", title: localizeResource("happy")});
                columnList.push({field: "neutral", title: localizeResource("neutral")});
                columnList.push({field: "angry", title: localizeResource("angry")});
//                add by RenZongKe for new feature "race"
//                columnList.push({field: "chinese", title: localizeResource("chinese")});
//                columnList.push({field: "malay", title: localizeResource("malay")});
//                columnList.push({field: "indian", title: localizeResource("indian")});
//                columnList.push({field: "black", title: localizeResource("black")});
//                columnList.push({field: "white", title: localizeResource("white")});
                break;

            default:
                columnList.push({field: "count", title: localizeResource("count")});
                break;
        }

        return columnList;
    };

    var initDataLogSearch = function ()
    {
        var allOption = localizeResource("all");
        var $dataGrid = $(".event_grid");

        kElement.searchDvcList = $dataGrid.find(".device_list").kendoDropDownList({
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
                    kElement.searchChnList.setDataSource(emptyDataSource);
                    return;
                }

                kElement.searchChnList.setDataSource(new kendo.data.DataSource({
                    data: getDeviceChannelList(dvcItem)
                }));
            }
        }).data("kendoDropDownList");

        kElement.searchChnList = $dataGrid.find(".channel_list").kendoDropDownList({
            optionLabel: allOption,
            dataTextField: "name",
            dataValueField: "nodeCoreDeviceId",
            dataSource: emptyDataSource
        }).data("kendoDropDownList");

        var todayRange = utils.getTodayDateRange();
        kElement.searchRangeFrom = $dataGrid.find(".start_time").kendoDateTimePicker({
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
            }
        }).data("kendoDateTimePicker");

        kElement.searchRangeTo = $dataGrid.find(".end_time").kendoDateTimePicker({
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
            }
        }).data("kendoDateTimePicker");

        kElement.searchRangeFrom.max(kElement.searchRangeTo.value());
        kElement.searchRangeTo.min(kElement.searchRangeFrom.value());
        kElement.searchRangeTo.max(todayRange.to);
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
            })
            return chnList;
        }
    };

    var calculateGridHeight = function ()
    {
        var offset = $vcaList.find(".header_box").height() + 225;
        return $(window).height() - offset;
    };

    var loading = function (loading)
    {
        kendo.ui.progress($vcaList, loading);
    };

    var onActionSuccess = function (responseData)
    {
        if (responseData.result != "ok")
        {
            utils.throwServerError(responseData);
            return;
        }

        refreshVcaGrid();
        var infoMsg = (kupapi.applicationType == "cloud") ? 'analytics-request-sent' : 'action-successful';
        utils.slideDownInfo(localizeResource(infoMsg));
    };

    return {
        generate: generate,
        clearVcaSearch: clearVcaSearch,
        addVca: addVca,
        configureVca: configureVca,
        activateVca: activateVca,
        deactivateVca: deactivateVca,
        deleteVca: deleteVca,
        openErrorLog: openErrorLog,
        openDebugger: openDebugger,
        openOccupancyGrid: openOccupancyGrid,
        filterDataLogs: filterDataLogs,
        clearDataLogFilters: clearDataLogFilters,
        exportData: exportData
    };
}

function VcaMonitor(onChanged)
{
    var changeMinGap = 0; //1sec
    var wsUrl = kupapi.getWsServer() + "/ws/monitorvcainstancechange";
    var _socket;
    var timer;

    var _openSocket = function ()
    {
        if (_socket != null && _socket.readyState == WebSocket.OPEN)
        {
            return;
        }

        _socket = new WebSocket(wsUrl);

        _socket.onmessage = function (evt)
        {
            if (timer)
            {
                clearTimeout(timer);
            }

            console.log("VcaMonitor : changed", evt.data);
            timer = setTimeout(onChanged, changeMinGap);
        };

        _socket.onopen = function (evt)
        {
            console.log("VcaMonitor : opened");
        };

        _socket.onclose = function (evt)
        {
            console.log("VcaMonitor : closed");
            console.log("VcaMonitor : reconnecting");
            _openSocket();
        };

        _socket.onerror = function (evt)
        {
            console.error("VcaMonitor :", evt);
        };
    };

    return {
        start: _openSocket
    }
}

