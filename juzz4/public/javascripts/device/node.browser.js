function NodeBrowser()
{
    var $pageMain = $(".node_browser");

    var kendoGrid = null;
    var filter = {};
    var inputList = {};

    var currentTotalCount = 0;

    var generate = function ()
    {
        mainJS.toggleLeftBar();
        initGrid();
        captureKeystrokes();
    };

    var searchTermChanged = function (fieldName, element)
    {
        inputList[fieldName] = element;
        var $ele = $(element);
        if ($ele.is(":checkbox"))
        {
            if (element.checked)
            {
                filter[fieldName] = true;
            }
            else
            {
                delete filter[fieldName];
            }

            //search immediately for checkboxes
            search();
        }
        else
        {
            filter[fieldName] = element.value;
        }
    };

    var search = function ()
    {
        if (!kendoGrid)
        {
            return;
        }

        kendoGrid.dataSource.read();
    };

    var clearSearch = function ()
    {
        filter = {};
        $.each(inputList, function (i, e)
        {
            var $input = $(e);
            if ($input.is(":checkbox"))
            {
                $input.prop("checked", false);
            }
            else
            {
                $input.val("");
            }
        });
        search();
    };

    var initGrid = function ()
    {
        function openLogs(e)
        {
            e.preventDefault();
            var dataItem = this.dataItem($(e.currentTarget).closest("tr"));
            var contentPage = "/device/devicelogs/" + dataItem.platformDeviceId;
            utils.openPopup(localizeResource("device-logs"), contentPage, null, null, true, utils.doNothing);
        }

        kendoGrid = $(".node_browser .grid").kendoGrid({
            dataSource: {
                transport: {
                    read: function (options)
                    {
                        var url = "/api/superadmin/searchregisterednodes";
                        var params = filter;
                        params["skip"] = options.data.skip;
                        params["take"] = options.data.take;

                        if (options.data.sort && options.data.sort.length > 0)
                        {
                            var sortInfo = options.data.sort[0];
                            var sortField = sortInfo.field;
                            sortField = (sortInfo.dir == "asc" ? "" : "-") + sortField;
                            params["sort-by"] = sortField;
                        }
                        else
                        {
                            delete params["sort-by"];
                        }

                        loading(true);
                        ajaxPost(url, params, function (res)
                        {
                            loading(false);
                            if (res.result != "ok")
                            {
                                options.success([]);
                                utils.throwServerError(res);
                                return;
                            }

                            currentTotalCount = res["total-count"];
                            options.success(res.nodes);
                        }, null);
                    }
                },
                pageSize: 15,
                serverPaging: true,
                serverSorting: true,
                schema: {
                    total: function ()
                    {
                        return currentTotalCount;
                    }
                }
            },
            pageable: {
                input: true,
                numeric: false,
                pageSizes: [15, 30, 50],
                refresh: true
            },
            filterable: false,
            selectable: false,
            sortable: true,
            resizable: true,
            detailTemplate: $("#subGridsTmpl").html(),
            height: function ()
            {
                return (window.innerHeight - 300) + "px";
            },
            detailInit: function (e)
            {
                var detailRow = e.detailRow;
                var cameraList = e.data.cameraSummaryList;
                var vcaList = e.data.vcaSummaryList;

                detailRow.find(".camera_grid").kendoGrid({
                    dataSource: cameraList,
                    columns: [
                        {
                            field: "status", width: "35px", title: "&nbsp;",
                            template: kendo.template($("#nodeStatusTemplate").html())
                        },
                        {field: "name"},
                        {field: "modelName"},
                        {field: "host"},
                        {field: "port"},
                        {field: "recordingEnabled"},
                        {field: "cameraCoreDeviceId"}
                    ],
                    sortable: true,
                    selectable: false
                });

                detailRow.find(".vca_grid").kendoGrid({
                    dataSource: {
                        transport: {
                            read: function (options)
                            {
                                $.each(vcaList, function (index, inst)
                                {
                                    $.each(cameraList, function (index, cam)
                                    {
                                        if (inst.cameraCoreDeviceId === cam.cameraCoreDeviceId)
                                        {
                                            inst.channelName = cam.name;
                                        }
                                    });
                                });
                                options.success(vcaList);
                            }
                        }
                    },
                    columns: [
                        {
                            field: "status", width: "35px", title: "&nbsp;",
                            template: kendo.template($("#runningStatusTmpl").html())
                        },
                        {field: "type", width: "200px", template: "#= localizeResource(type) #"},
                        {field: "channelName", title: "camera", width: "200px"},
                        {field: "avgStatusChangeCount", width: "200px"},
                        {field: "scheduleSummary", title: "Schedule"}
                    ],
                    sortable: true,
                    selectable: false
                });
            },
            columns: [
                {
                    field: "status",
                    width: "35px",
                    title: "&nbsp;",
                    template: kendo.template($("#nodeStatusTemplate").html())
                },
                {
                    field: "bucketName"
                },
                {
                    field: "nodeName"
                },
                {
                    field: "version",
                    width: "70px"
                },
                {
                    field: "modelName",
                    width: "180px"
                },
                {
                    field: "softwareStatus",
                    width: "120px",
                    template: kendo.template($("#nodeSoftwareTemplate").html())
                },
                {
                    field: "timezone",
                    width: "150px"
                },
                {
                    field: "ipAddress",
                    width: "80px"
                },
                {
                    field: "mac",
                    width: "120px"
                },
                {
                    field: "platformDeviceId",
                    width: "120px"
                },
                {
                    field: "coreDeviceId",
                    width: "100px"
                },
                {
                    command: [
                        {text: "Logs", click: openLogs}
                    ],
                    title: "actions",
                    width: "150px"
                }
            ]
        }).data("kendoGrid");
    };

    var captureKeystrokes = function ()
    {
        var keyListener = new window.keypress.Listener();
        keyListener.simple_combo("enter", function ()
        {
            search();
        });
    };

    var loading = function (loading)
    {
        kendo.ui.progress($pageMain.find(".search"), loading);
        //kendo.ui.progress($pageMain, loading);
    };

    return {
        generate: generate,
        searchTermChanged: searchTermChanged,
        search: search,
        clearSearch: clearSearch
    }
}