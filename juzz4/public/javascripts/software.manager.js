/**
 * @author Aye Maung
 */

function SftMgr()
{
    var acceptedPackageNames = ["kainode.update", "nodeone.tgz"];

    var $mainWrapper = $(".soft_mgr");
    var kendoFileGrid = null;

    var initFileUploader = function ()
    {
        $mainWrapper.find("#updateFile").kendoUpload({
            multiple: false,
            async: {
                saveUrl: "/api/" + kupBucket + "/uploadsoftwareupdate",
                autoUpload: true
            },
            localization: {
                select: localizeResource("upload-newer-version")
            },
            upload: function (e)
            {
                var fileName = e.files[0].name.toLowerCase();
                if (acceptedPackageNames.indexOf(fileName) == -1)
                {
                    var msg = "Only '" + acceptedPackageNames.join("' or '") + "' files are allowed";
                    utils.popupAlert(msg);
                    e.preventDefault();
                }
            },
            success: function (e)
            {
                refreshGrid();
            },
            error: function (e)
            {
                var errorText = "";
                if (e.XMLHttpRequest.response)
                {
                    errorText = e.XMLHttpRequest.response;
                }
                else
                {
                    errorText = "session-expired";
                }

                utils.popupAlert(localizeResource(errorText));
            }
        });
    };

    var initFileGrid = function ()
    {
        var deleteFile = function (e)
        {
            e.preventDefault();
            var dataItem = this.dataItem($(e.currentTarget).closest("tr"));
            utils.popupConfirm(localizeResource("confirmation"), localizeResource("confirm-delete") + "?",
                function (choice)
                {
                    if (choice)
                    {
                        loading(true);
                        removeSoftwareUpdate("", dataItem.fileServerId, function (responseData)
                        {
                            loading(false);
                            if (responseData.result != "ok")
                            {
                                utils.popupAlert(responseData.reason);
                            }

                            refreshGrid();
                        }, null);
                    }
                });
        };

        var downloadFile = function (e)
        {
            e.preventDefault();
            var dataItem = this.dataItem($(e.currentTarget).closest("tr"));
            downloadSoftwareUpdate("", dataItem.fileServerId);
        };

        kendoFileGrid = $mainWrapper.find(".grid_files").kendoGrid({
            dataSource: {
                transport: {
                    read: function (options)
                    {
                        getSoftwareUpdateList("", function (responseData)
                        {
                            options.success(responseData.files);
                        });
                    }
                },
                pageSize: 30,
                group: {
                    field: "model.name"
                }
            },
            pageable: {
                input: false,
                numeric: false,
                pageSizes: false,
                refresh: true
            },
            sortable: false,
            selectable: true,
            height: $("#container").height() - 255 + "px",
            columns: [
                {
                    field: "version",
                    title: localizeResource("version")
                },
                {
                    field: "fileSize",
                    title: localizeResource("file-size"),
                    template: "#= utils.bytesToMBString(fileSize)  #"
                },
                {
                    field: "host",
                    title: localizeResource("host")
                },
                {
                    field: "port",
                    title: localizeResource("port")
                },
                {
                    field: "uploadedTime",
                    title: localizeResource("uploaded-date"),
                    template: "#= kendo.toString(new Date(uploadedTime), kupapi.TIME_FORMAT) #"
                },
                {
                    field: "model.name",
                    title: "&nbsp;",
                    groupHeaderTemplate: $("#modelGroupHeaderTmpl").html()
                },
                {
                    command: [
                        { text: localizeResource("download"), click: downloadFile},
                        { text: localizeResource("delete"), click: deleteFile}
                    ], title: "", width: "170px"
                }
            ]
        }).data("kendoGrid");
        kendoFileGrid.hideColumn("model.name");
    };

    var refreshGrid = function ()
    {
        kendoFileGrid.dataSource.read();
    };

    var loading = function (loading)
    {
        kendo.ui.progress($mainWrapper, loading);
    };

    return {
        generate: function ()
        {
            initFileUploader();
            initFileGrid();
        }
    };
}