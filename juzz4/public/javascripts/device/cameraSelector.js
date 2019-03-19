/**
 *
 * This library is designed for selecting cameras only
 * DO NOT modify this file to allow the selection of other device types (Label, Node, etc ...)
 *
 * @author Aye Maung
 */
var camsltr = {
    treeItemTemplate: "camSelectorItemTmpl",
    iconFolder: "/public/css/common/images/treeicons/",
    jqTreeId: null,
    kendoTree: null,
    preSelectedId: null,
    cancelled: true,
    selectedCamera: null,
    parentLookupMap: {},    // nodes
    cameraLookupMap: {},
    searchSource: [],
    deviceFilter: {
        models: null,
        minRelease: null,
        recordingEnabled: null
    }
};

camsltr.ItemType = Object.freeze({
    LABEL: "label",
    NODE4: "node4",
    NODE1: "node1",
    CAMERA: "camera"
});

//key must match ItemType
camsltr.icons = {
    label: camsltr.iconFolder + "label.png",
    node4: camsltr.iconFolder + "node.png",
    node1: camsltr.iconFolder + "node.png",
    camera: camsltr.iconFolder + "node_cam.png"
}

camsltr.open = function (title, preSelectedId, deviceFilter, onSelected, onCancelled)
{
    camsltr.preSelectedId = preSelectedId;
    camsltr.deviceFilter = $.extend(camsltr.deviceFilter, deviceFilter);
    var contentPage = "/device/camselector";
    utils.openPopup(title, contentPage, null, null, true, function ()
    {
        if (camsltr.cancelled)
        {
            if (onCancelled)
            {
                onCancelled();
            }
        }
        else
        {
            var parentDevice = camsltr.parentLookupMap[camsltr.selectedCamera.id];
            onSelected(parentDevice, camsltr.selectedCamera.data);
        }
    });
}

camsltr.close = function (cancelled)
{
    camsltr.cancelled = cancelled;
    $(camsltr.jqTreeId).closest(".k-window-content").data("kendoWindow").close();
}

camsltr.generateTree = function (divId)
{
    camsltr.jqTreeId = "#" + divId;
    camsltr._loading(true);

    camsltr.getDevicesAsTree(function (deviceTree)
    {
        camsltr.kendoTree = $(camsltr.jqTreeId).kendoTreeView({
            template: kendo.template($("#" + camsltr.treeItemTemplate).html()),
            dataSource: deviceTree,
            select: function (e)
            {
                var selectedItem = camsltr.kendoTree.dataItem(e.node);
                camsltr._selectTreeItem(selectedItem, false);
            }
        }).data("kendoTreeView");

        $(camsltr.jqTreeId + " .k-in").on("dblclick", function (e)
        {
            var dbClickedNode = $(e.target).closest(".k-item");
            var dataItem = camsltr.kendoTree.dataItem(dbClickedNode);
            if (dataItem.items.length == 0)
            {
                var camera = dataItem;
                if (camera.selectable)
                {
                    camsltr._selectTreeItem(camera, false);
                    camsltr.close(false);
                }
            }
        });

        camsltr._initSearchBox(deviceTree);
        camsltr._customizeTreeStyle();
        camsltr._allowSubmit(false);
        camsltr.kendoTree.enable(".disabled_item", false);

        //if there are only a few nodes, expand the tree
        if (Object.keys(deviceTree).length < 4)
        {
            camsltr.kendoTree.expand("> .k-group > .k-item");
        }

        if (camsltr.preSelectedId)
        {
            camsltr._selectTreeItem(camsltr.cameraLookupMap[camsltr.preSelectedId], true);
        }

        camsltr._loading(false);
    });
}

camsltr.collapseFullTree = function ()
{
    camsltr.kendoTree.collapse(".k-item");
}

camsltr.getDevicesAsTree = function (callback)
{
    getUserDevices("", function (responseData)
    {
        if (responseData.result != "ok" || responseData.devices == null)
        {
            utils.throwServerError(responseData.reason);
            callback([]);
            return;
        }

        var deviceTree = camsltr._convertToDeviceTree(responseData.devices);
        callback(deviceTree);
    }, null);
}

camsltr._convertToDeviceTree = function (deviceList)
{
    camsltr.searchSource = [];
    var deviceTree = [];

//    var l1 = "All";
//    var labelNodeCams = camsltr._buildTreeItem(
//        camsltr._getLabelIdentifier(l1),
//        l1,
//        camsltr.ItemType.LABEL,
//        false,
//        l1
//    );

    //compile nodes and cameras
    $.each(deviceList, function (idx, dvc)
    {
        var isNode = DvcMgr.isKaiNode(dvc);

        //cleaned Obj
        var cleanedDvc = dvc;
        delete cleanedDvc.cameras;  //make the object lighter

        //nodes only
        if (isNode)
        {
            //filter version
            if (camsltr.deviceFilter.minRelease &&
                dvc.node.releaseNumber < camsltr.deviceFilter.minRelease)
            {
                return true;
            }

            //filter model
            if (camsltr.deviceFilter.models &&
                camsltr.deviceFilter.models.indexOf(dvc.model.modelId) == -1)
            {
                return true;
            }

            var nodeItem = camsltr._buildTreeItem(
                camsltr._getNodeIdentifier(dvc),
                dvc.name,
                camsltr.ItemType.NODE4,
                false,
                cleanedDvc
            );

            $.each(dvc.node.cameras, function (idx, nodeCam)
            {
                var camDisabled = false;

                //filter recording enable flag
                if (camsltr.deviceFilter.recordingEnabled &&
                    nodeCam.cloudRecordingEnabled !== camsltr.deviceFilter.recordingEnabled)
                {
                    camDisabled = true;
                }

                var cameraItem = camsltr._buildTreeItem(
                    camsltr._getCameraIdentifier(dvc, nodeCam),
                    nodeCam.name,
                    camsltr.ItemType.CAMERA,
                    camDisabled,
                    nodeCam
                );

                nodeItem.items.push(cameraItem);

                if (!camDisabled)
                {
                    //update search dataSource
                    cameraItem.searchableName = cleanedDvc.name + "  -  " + nodeCam.name;
                    camsltr.searchSource.push(cameraItem);

                    //update lookup maps
                    camsltr.parentLookupMap[cameraItem.id] = cleanedDvc;
                    camsltr.cameraLookupMap[cameraItem.id] = cameraItem;
                }
            });

            //only display nodes with cameras
            if (!utils.isListEmpty(nodeItem.items))
            {
//                labelNodeCams.items.push(nodeItem);
                deviceTree.push(nodeItem);
            }
        }
    });

//    deviceTree.push(labelNodeCams);

    //update root types
    $.each(deviceTree, function (i, root)
    {
        root.isRoot = true;
    });

    return deviceTree
}

camsltr._initSearchBox = function ()
{
    var kendoInput = $(".camera_tree .search_bar .input").kendoAutoComplete({
        dataSource: camsltr.searchSource,
        dataTextField: "searchableName",
        filter: "contains",
        placeholder: localizeResource("select-by-node-or-cam"),
        select: function (e)
        {
            var dataItem = this.dataItem(e.item.index());
            camsltr._selectTreeItem(dataItem, true);
        },
        change: function (e)
        {
            this.value("");
        }
    }).data("kendoAutoComplete");

    kendoInput.list.width(345);
    $(".camera_tree .search_bar .input").show();
}

camsltr._selectTreeItem = function (cameraItem, expandToSelection)
{
    camsltr.kendoTree.select($("#" + cameraItem.id));

    if (expandToSelection)
    {
        $("#" + cameraItem.id).parentsUntil('.k-treeview').filter('.k-item').each(
            function (index, element)
            {
                camsltr.kendoTree.expand($(this));
            }
        );
    }

    if (cameraItem.selectable)
    {
        camsltr.selectedCamera = cameraItem;
        camsltr._allowSubmit(true);
    }
    else
    {
        camsltr._allowSubmit(false)
    }
};

camsltr._loading = function (loading)
{
    kendo.ui.progress($(".cam_selector"), loading);
}

camsltr._buildTreeItem = function (identifier, displayName, itemType, disabled, dataObj)
{
    return {
        id: identifier,
        name: displayName,
        type: itemType,
        imageUrl: camsltr.icons[itemType],
        isRoot: false,
        selectable: !disabled && (itemType == camsltr.ItemType.CAMERA) ? true : false,   //don't modify this
        disabled: disabled,
        data: dataObj,
        items: []
    };
}

camsltr._getLabelIdentifier = function (label)
{
    return label;
};

camsltr._getNodeIdentifier = function (nodeDevice)
{
    return nodeDevice.id;
};

camsltr._getCameraIdentifier = function (nodeDevice, nodeCamera)
{
    if (nodeDevice == null)
    {
        return nodeCamera.nodeCoreDeviceId;
    }
    return nodeDevice.id + "_" + nodeCamera.nodeCoreDeviceId;
};

camsltr._customizeTreeStyle = function ()
{
    var childItem = $(".camera_tree .child_item").closest(".k-top, .k-mid, .k-bot");
    childItem.addClass("lineage_line");

    var rootItem = $(".camera_tree .root_item").closest(".k-top, .k-mid, .k-bot");
    rootItem.addClass("lineage_line");
    rootItem.addClass("separate_root");

    var rootGroup = $(".camera_tree .root_item").closest(".k-item");
    rootGroup.css("border-left", "0px");
}

camsltr._allowSubmit = function (allow)
{
    var $btnSubmit = $(".cam_selector .select");
    if (allow)
    {
        $btnSubmit.show();
    }
    else
    {
        $btnSubmit.hide();
    }
};