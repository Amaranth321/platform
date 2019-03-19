function ScheduleTaskManager(treeDiv, searchTextDiv)
{
    var $treeViewDiv = $("#"+treeDiv);
    var $searchText =  $("#"+searchTextDiv);
    var treeItems = [];
    var orgList = [];

    var kElement = {
        bucketDvcTree: null,
        scheduleTime: null
    }

    var generate = function ()
    {
        _initBucketDevTree();
        _initScheduleTime();
    };

    var generateReadOnly = function (assignedScheduleId)
    {
        _initReadOnlyNodeGrid(assignedScheduleId);
    }

    var addNew = function ()
    {
        var sT = utils.toAPITimestamp(utils.convertToUTC(kElement.scheduleTime.value()));
        var selectedNodeIDs = [];
        $.each(orgList, function (index, fItem) {
            if (document.getElementById(fItem.id).checked == true) {
                selectedNodeIDs.push(fItem.id+"");
            }
        });

        utils.showLoadingOverlay();
        scheduleNodeUpdates(sT, selectedNodeIDs, function(resp){
            utils.hideLoadingOverlay();
            if (resp.result != "ok") {
                utils.throwServerError(responseData);
            }
            utils.slideDownInfo(localizeResource('action-successful'));
            close();
        }, null);
    }

    var close = function ()
    {
        $searchText.closest(".k-window-content").data("kendoWindow").close();
    }

    /**
     * private functions
     */
    var _initBucketDevTree = function ()
    {
        _generateTreeItems(function () {
            kElement.bucketDvcTree = $treeViewDiv.kendoTreeView({
                checkboxes: {
                    checkChildren: true,
                    template: "<input type='checkbox' id='#= item.id #' #if(!item.isLoaded){# style='display: none;' #}else{# #}#/>"
                },
                dataSource: treeItems,
                select: function (e) {
                    _loadBucketNodeForTreeview(e);
                }
            }).data("kendoTreeView");
        })

        // make contains selector with case insensitive
        $.expr[":"].contains = $.expr.createPseudo(function(arg) {
            return function( elem ) {
                return $(elem).text().toUpperCase().indexOf(arg.toUpperCase()) >= 0;
            };
        });

        // init search for treeview
        $searchText.keyup(function (e) {
            var filterText = $(this).val();
            if (filterText !== "")
            {
                $("#"+treeDiv+" .k-group .k-group .k-in").closest("li").hide();
                $("#"+treeDiv+" .k-group .k-in").closest("li").hide();
                $("#"+treeDiv+" .k-group .k-group .k-in:contains(" + filterText + ")").each(function () {
                    $(this).parents("ul, li").each(function () {
                        $(this).show();
                        kElement.bucketDvcTree.expand($(this).parents("li"));
                    });
                });
                $("#"+treeDiv+" .k-group .k-in:contains(" + filterText + ")").each(function () {
                    $(this).parents("ul, li").each(function () {
                        $(this).show();
                    });
                });
            }
            else {
                $("#"+treeDiv+" .k-group").find("li").show();
            }
        });
    }

    var _initReadOnlyNodeGrid = function (assignedScheduleId)
    {
        var columnList = [
            {
                field: "bucketName",
                title: localizeResource("bucket-name"),
            },
            {
                field: "nodeName",
                title: localizeResource("node-name"),
            },
            {
                field: "version",
                title: localizeResource("version"),
                width: "180px"
            }
        ];

        kElement.bucketDvcTree = $treeViewDiv.kendoGrid({
            dataSource: {
                transport: {
                    read: function (options)
                    {
                        getNodeUpdateSchedule(assignedScheduleId, function (responseData) {
                            if (responseData.result == "ok" && responseData.nodes != null) {
                                options.success(responseData.nodes);
                            }
                            else
                            {
                                options.success([]);
                            }

                            if (responseData.result == "ok" && responseData.taskInfo != null) {
                                $("#scheduleTime").text(utils.getLocalTimestamp(responseData.taskInfo.scheduledTime));
                            }

                        }, null);
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
            columns: columnList,
            height: "350px",
        }).data("kendoGrid");
    }

    var _initScheduleTime = function () {
        var now = new Date();
        now.setHours(now.getHours()+1);
        var nextHour =  new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours(), 0, 0, 0);

        var todayRange = utils.getTodayDateRange();
        kElement.scheduleTime = $(".scheduleTime").kendoDateTimePicker({
            interval: 60,
            format: "dd/MM/yyyy HH:mm",
            timeFormat: "HH:mm",
            value: todayRange.from,
            change: function (e)
            {
            },
            min: nextHour
        }).data("kendoDateTimePicker");
    }

    var _loadBucketNodeForTreeview = function (element) {
        var treeview = kElement.bucketDvcTree;
        var dataItem = treeview.dataItem(element.node);

        //if no loaded before, fetch nodes for that bucket
        if(!dataItem.isLoaded) {
            _loading(true);
            dataItem.isLoaded = true;
            var bID = dataItem.id.substring(7, dataItem.id.length);
            getBucketDevicesByBucketId(bID, function(repsDev){
                if (repsDev.result == "ok" && repsDev.devices != null) {
                    $.each(repsDev.devices, function (index, dev){
                        dev.text = dev.name;
                        dev.isLoaded = true;
                        dev.imageUrl = "/public/css/common/images/treeicons/node.png"
                        treeview.append(dev, treeview.findByUid(dataItem.uid));
                        $("#"+dataItem.id).show();
                        $("#"+dev.id).show();
                        orgList.push(dev);
                    });
                }
                _loading(false);
            }, null);
        }
    }

    var _generateTreeItems = function (callback) {
        treeItems = [];
        //get buckets
        getBuckets("", function (responseData) {
            if (responseData.result == "ok" && responseData.buckets != null) {
                $.each(responseData.buckets, function (index, bucket) {
                    treeItems.push({
                        id: "bucket_"+bucket.id,
                        text: bucket.name,
                        imageUrl: "/public/css/common/images/features/bucket-management.png",
                        items: [],
                        isLoaded: false
                    });
                });
            }
            callback();
        }, null);
    }

    var _loading = function (loading)
    {
        kendo.ui.progress($("#" + treeDiv), loading);
    }

    return {
        generate: generate,
        generateReadOnly: generateReadOnly,
        addNew: addNew,
        close: close
    }
}