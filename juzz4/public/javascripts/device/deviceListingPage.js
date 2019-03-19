function DeviceListingPage(pageTitle, allowAddDevice)
{
    var $pageMain = $(".dvc_listing");
    var getDevicesAPI = getUserDevices;

    var kElement = {
        tabstrip: null,
        deviceGrid: null,
        modelGrid: null,
        labelFilter: null
    };

    var searchInputs = {
        deviceName: null,
        modelName: null
    };

    var naText = localizeResource("n/a");

    /**
     *
     * Public functions
     *
     */

    var generate = function ()
    {
        $pageMain.find(".default_title_bar .title").text(pageTitle);
        initTapStrip();

        if (!allowAddDevice)
        {
            $pageMain.find(".dvc_toolbox .add").hide();
        }
    };

    var viewNodeInfo = function (nodePlatformId)
    {
        var contentPage = "/device/nodeinfo?id=" + nodePlatformId + "&readonly=false";
        utils.openPopup(localizeResource("node-info"), contentPage, null, null, true, utils.doNothing);
    };

    var addDevice = function ()
    {
        var contentPage = "/device/add";
        var title = localizeResource("add-new-device");
        utils.openPopup(title, contentPage, null, null, true, refreshDeviceGrid);
    };

    var editDevice = function (nodePlatformId)
    {
        var contentPage = "/device/edit/" + nodePlatformId;
        utils.openPopup(localizeResource("edit-device"), contentPage, null, null, true, refreshDeviceGrid);
    };

    var openLimitedEdit = function (nodePlatformId)
    {
        var contentPage = "/device/editlimited/" + nodePlatformId;
        utils.openPopup(localizeResource("edit-device"), contentPage, null, null, true, refreshDeviceGrid);
    };

    var showLogs = function (nodePlatformId)
    {
        var contentPage = "/device/devicelogs/" + nodePlatformId;
        utils.openPopup(localizeResource("device-logs"), contentPage, null, null, true, utils.doNothing);
    };

    var deleteDevice = function (nodePlatformId)
    {
        utils.popupConfirm(localizeResource("confirmation"), localizeResource("confirm-delete"),
            function (choice)
            {
                if (choice)
                {
                    loading(true);
                    removeDeviceFromBucket("", nodePlatformId, function (responseData)
                    {
                        loading(false)
                        if (responseData.result != "ok")
                        {
                            utils.throwServerError(responseData);
                            return;
                        }

                        refreshDeviceGrid();
                        utils.slideDownInfo(localizeResource("delete-successful"));
                    }, null);
                }
            });
    };

    var clearSearch = function ()
    {
        //labels
        if (kElement.labelFilter)
        {
            kElement.labelFilter.value("");
        }

        $.each(searchInputs, function (name, $ele)
        {
            $ele.val("");
        });

        onDeviceSearchChanged();
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
                        if (!kElement.deviceGrid)
                        {
                            initDeviceGrid();
                        }
                        break;

                    case 1:
                        if (!kElement.modelGrid)
                        {
                            initModelGrid();
                        }
                        break;
                }
            }
        }).data("kendoTabStrip");
        kElement.tabstrip.select(0);
    };

    var initDeviceGrid = function ()
    {
        LabelMgr.ready(function ()
        {
            if (kupapi.onCloud())
            {
                initDeviceGridForCloud();
            }
            else
            {
                initDeviceGridForNode()
            }

            //setup search filters
            searchInputs.deviceName = $("#inputNameSearch");
            searchInputs.modelName = $("#inputModelSearch");

            searchInputs.deviceName.keyup(onDeviceSearchChanged);
            searchInputs.modelName.keyup(onDeviceSearchChanged);
        });
    };

    var initDeviceGridForCloud = function ()
    {
        var columnList = [
            {
                field: "status",
                title: "&nbsp;",
                width: "45px",
                template: kendo.template($("#deviceStatusTemplate").html())
            },
            {
                field: "cameraLabels",
                title: localizeResource("camera-labels")
            },
            {
                field: "name",
                title: localizeResource("device-name")
            },
            {
                field: "modelName",
                title: localizeResource("model")
            },
            {
                field: "version",
                title: localizeResource("version"),
                width: "80px"
            },
            {
                field: "ip",
                title: localizeResource("ip"),
                width: "120px"
            },
            {
                field: "address",
                title: localizeResource("location")
            },
            {
                field: "name",
                title: localizeResource("actions"),
                template: $("#nodeDeviceActionsTmpl").html(),
                width: "180px"
            }
        ];

        kElement.deviceGrid = $pageMain.find(".device_grid").kendoGrid({
            dataSource: {
                transport: {
                    read: function (options)
                    {
                        getDevicesAPI("", function (responseData)
                        {
                            if (responseData.result != "ok")
                            {
                                utils.throwServerError(responseData);
                                return;
                            }

                            var displayList = [];
                            $.each(responseData.devices, function (i, dvc)
                            {
                                var gridItem = {
                                    platformDeviceId: dvc.id,
                                    coreDeviceId: dvc.deviceId,
                                    name: dvc.name,
                                    modelName: dvc.model.name,
                                    address: dvc.address,
                                    status: dvc.status
                                };

                                if (DvcMgr.isKaiNode(dvc))
                                {
                                    gridItem.isKaiNode = true;
                                    gridItem.version = dvc.node.version;
                                    gridItem.cameraLabels = getAssignedLabels(dvc).join(", ");
                                    gridItem.ip = utils.coalesce(function ()
                                    {
                                        return dvc.node.settings.networkSettings.ipAddress;
                                    });
                                }
                                else
                                {
                                    gridItem.isKaiNode = false;
                                    gridItem.version = naText;
                                    gridItem.ip = dvc.host + ":" + dvc.port;
                                }

                                displayList.push(gridItem);
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
            sortable: false,
            selectable: true,
            resizable: false,
            height: calculateGridHeight() + "px",
            columns: columnList
        }).data("kendoGrid");

        initLabelFilter();
    };

    var initDeviceGridForNode = function ()
    {
        var columnList = [
            {
                field: "status",
                title: "&nbsp;",
                width: "45px",
                template: kendo.template($("#deviceStatusTemplate").html())
            },
            {
                field: "name",
                title: localizeResource("device-name")
            },
            {
                field: "modelName",
                title: localizeResource("model")
            },
            {
                field: "host",
                title: localizeResource("host"),
                width: "120px"
            },
            {
                field: "port",
                title: localizeResource("port"),
                width: "80px"
            },
            {
                field: "login",
                title: localizeResource("login"),
                width: "120px"
            },
            {
                field: "cloudRecordingEnabled",
                title: localizeResource("cloud-recording-enabled"),
                width: "150px"
            },
            {
                field: "name",
                title: localizeResource("actions"),
                template: $("#nodeDeviceActionsTmpl").html(),
                width: "180px"
            }
        ];

        kElement.deviceGrid = $pageMain.find(".device_grid").kendoGrid({
            dataSource: {
                transport: {
                    read: function (options)
                    {
                        getDevicesAPI("", function (responseData)
                        {
                            if (responseData.result != "ok")
                            {
                                utils.throwServerError(responseData);
                                return;
                            }

                            var displayList = [];
                            $.each(responseData.devices, function (i, dvc)
                            {
                                var gridItem = {
                                    platformDeviceId: dvc.id,
                                    coreDeviceId: dvc.deviceId,
                                    name: dvc.name,
                                    modelName: dvc.model.name,
                                    address: dvc.address,
                                    status: dvc.status,
                                    login: dvc.login,
                                    host: dvc.host,
                                    port: dvc.port,
                                    cloudRecordingEnabled: dvc.cloudRecordingEnabled
                                };

                                displayList.push(gridItem);
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
            sortable: false,
            selectable: true,
            resizable: false,
            height: calculateGridHeight() + "px",
            columns: columnList
        }).data("kendoGrid");
    };

    var initLabelFilter = function ()
    {
        var $labelFilter = $pageMain.find(".label_filter");
        $labelFilter.show();

        kElement.labelFilter = $labelFilter.find(".multi_select").kendoMultiSelect({
            dataTextField: "name",
            dataValueField: "name",
            dataSource: {
                transport: {
                    read: function (options)
                    {
                        getLabels(function (responseData)
                        {
                            var sortedLabels = responseData.labels;
                            sortedLabels.sort(function (l1, l2)
                            {
                                return l1.name > l2.name ? 1 : (l1.name < l2.name ? -1 : 0);
                            });
                            options.success(sortedLabels);
                        });
                        (LabelMgr.getUserAccessibleStoreLabels());
                    }
                }
            },
            change: onDeviceSearchChanged
        }).data("kendoMultiSelect");

        //special sizing for kendo selector
        var multiSelectorWidth = 495;
        $labelFilter.find(".ms_wrapper").width(multiSelectorWidth);
        $labelFilter.find(".k-multiselect-wrap").width(multiSelectorWidth);
        kElement.labelFilter.list.width(multiSelectorWidth);
    };

    var initModelGrid = function ()
    {
        kElement.modelGrid = $pageMain.find(".model_grid").kendoGrid({
            dataSource: {
                transport: {
                    read: function (options)
                    {
                        getDeviceModels("", function (responseData)
                        {
                            options.success(responseData["model-list"]);
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
            columns: [
                {
                    field: "modelId",
                    title: localizeResource("model-id"),
                    width: "80px"
                },
                {
                    field: "name",
                    title: localizeResource("name"),
                    width: "350px"
                },
                {
                    field: "channels",
                    title: localizeResource("number-of-channels"),
                    width: "150px"
                },
                {
                    field: "capabilities",
                    title: localizeResource("capabilities")
                }
            ]
        }).data("kendoGrid");
    };

    var refreshDeviceGrid = function ()
    {
        if (kElement.deviceGrid)
        {
            kElement.deviceGrid.dataSource.read();
        }
    };

    var onDeviceSearchChanged = function ()
    {
        var filter = {logic: "and", filters: []};

        //labels
        if (kElement.labelFilter)
        {
            var selectedLabels = kElement.labelFilter.value();
            $.each(selectedLabels, function (i, labelName)
            {
                filter.filters.push({field: "cameraLabels", operator: "contains", value: labelName});
            });
        }

        //device
        var dvcName = searchInputs.deviceName.val();
        if (!utils.isNullOrEmpty(dvcName))
        {
            filter.filters.push({field: "name", operator: "contains", value: dvcName});
        }

        //model
        var modelTerm = searchInputs.modelName.val();
        if (!utils.isNullOrEmpty(modelTerm))
        {
            filter.filters.push({field: "modelName", operator: "contains", value: modelTerm});
        }

        kElement.deviceGrid.dataSource.filter(filter);
    };

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
        viewNodeInfo: viewNodeInfo,
        addDevice: addDevice,
        editDevice: editDevice,
        openLimitedEdit: openLimitedEdit,
        showLogs: showLogs,
        deleteDevice: deleteDevice,
        clearSearch: clearSearch
    }
}