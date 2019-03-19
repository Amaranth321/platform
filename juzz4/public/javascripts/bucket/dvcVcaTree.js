/**
 * @author Aye Maung
 */
var dvcVcaTree = {};

dvcVcaTree.iconFolder = kupapi.CdnPath + "/common/images/";

dvcVcaTree.ItemType = Object.freeze({
    BUCKET: "bucket",
    NODE: "device",
    CAMERA: "camera",
    VCA: "vca"
});

dvcVcaTree._configs = {
    bucketId: null,
    bucketName: null,
    cssSelector: null
}

dvcVcaTree._innerWidth = null;
dvcVcaTree._innerHeight = null;
dvcVcaTree._margin = {top: 0, right: 0, bottom: 0, left: 0};

dvcVcaTree.d3 = {
    resetZoom: null
}

dvcVcaTree._setConfigs = function (configs)
{
    this._configs = $.extend(this._configs, configs);
}

dvcVcaTree.generate = function (configs, deviceList, analyticsList)
{
    dvcVcaTree.remove(configs.cssSelector);
    dvcVcaTree._setConfigs(configs);
    dvcVcaTree._initUi();
    dvcVcaTree._setWidthHeight(deviceList.length, analyticsList.length);
    var treeData = dvcVcaTree._prepareTreeData(deviceList, analyticsList);
    dvcVcaTree._drawTreeWithIcons(treeData);
}

dvcVcaTree.remove = function (cssSelector)
{
    $(cssSelector).empty();
}

dvcVcaTree.reset = function ()
{
    dvcVcaTree.d3.resetZoom();
}

dvcVcaTree._initUi = function ()
{
    //popover box
    $(this._configs.cssSelector).append(dvcVcaTree._getPopupHtml());

    var toolbarHtml = '' +
                      '<div class="top_bar">' +
                      '   <a href="javascript:bucketManager.loadCurrentTab()" class="k-button"><span class="k-icon k-i-refresh"></span></a>' +
                      '   <a href="javascript:dvcVcaTree.reset()" class="k-button">' + localizeResource("reset-view") + '</a>' +
                      '   <span class="summary_box">' +
                      '       <span id="nodeCount" class="count">0</span>' + '<label>' + localizeResource("nodes") + '</label>' +
                      '       <span id="cameraCount" class="count">0</span>' + '<label>' + localizeResource("cameras") + '</label>' +
                      '       <span id="vcaCount" class="count">0</span>' + '<label>' + localizeResource("analytics") + '</label>' +
                      '   </span>' +
                      '</div>';

    $(this._configs.cssSelector).append(toolbarHtml);
}

dvcVcaTree._getPopupHtml = function ()
{
    return "" +
           "<div class='hover_win'>" +
           "   <div class='name'></div>" +
           "   <div class='model'></div>" +
           "   <div class='status'></div>" +
           "   <div class='version'></div>" +
           "   <div class='schedule'></div>" +
           "</div>";
}

dvcVcaTree._loading = function (loading)
{
    kendo.ui.progress($(dvcVcaTree._configs.cssSelector), loading);
}

dvcVcaTree._setSummary = function (nodeCount, camCount, vcaCount)
{
    $("#nodeCount").html(nodeCount);
    $("#cameraCount").html(camCount);
    $("#vcaCount").html(vcaCount);
}

dvcVcaTree._prepareTreeData = function (deviceList, vcaInstances)
{
    var nodeCount = 0;
    var cameraCount = 0;

    var root = dvcVcaTree._createTreeItem(
        dvcVcaTree._configs.bucketId,
        dvcVcaTree._configs.bucketName,
        dvcVcaTree.ItemType.BUCKET,
        dvcVcaTree._configs.bucket);

    var rootItemList = [];

    //for vca lookup
    var vcaInstMap = {};
    $.each(vcaInstances, function (idx, inst)
    {
        var vcaItem = dvcVcaTree._createTreeItem(
            inst.instanceId,
            localizeResource(inst.type),
            dvcVcaTree.ItemType.VCA,
            inst);

        var uniqueDvcId = inst.platformDeviceId + "_" + inst.channelId;
        if (vcaInstMap[uniqueDvcId] == null)
        {
            vcaInstMap[uniqueDvcId] = [];
        }

        vcaInstMap[uniqueDvcId].push(vcaItem);
    });

    // compile devices
    $.each(deviceList, function (idx, dvc)
    {
        var isNode = (dvc.model.capabilities.indexOf("node") != -1);

        var deviceItem = dvcVcaTree._createTreeItem(
            dvc.id,
            dvc.name,
            isNode ? dvcVcaTree.ItemType.NODE : dvcVcaTree.ItemType.CAMERA,
            dvc);

        //nodes
        if (isNode)
        {
            nodeCount++;
            $.each(dvc.node.cameras, function (idx, nodeCam)
            {
                var nodeCamItem = dvcVcaTree._createTreeItem(
                    dvc.id + "_" + nodeCam.nodeCoreDeviceId,
                    nodeCam.name,
                    dvcVcaTree.ItemType.CAMERA,
                    nodeCam);

                //retrieve vca list
                var camVcaList = vcaInstMap[dvc.id + "_" + nodeCam.nodeCoreDeviceId];
                if (camVcaList)
                {
                    nodeCamItem.items = camVcaList;
                }

                deviceItem.items.push(nodeCamItem);
                cameraCount++;
            });
        }
        //cameras on cloud
        else
        {
            cameraCount++;

            //retrieve vca list
            var deviceVcaList = vcaInstMap[dvc.id + "_" + 0];
            if (deviceVcaList)
            {
                deviceItem.items = deviceVcaList;
            }
        }

        rootItemList.push(deviceItem);
    });

    rootItemList.sort(function (d1, d2)
    {
        return d1.data.model.modelId - d2.data.model.modelId;
    });

    root.items = rootItemList;

    //update summary
    dvcVcaTree._setSummary(nodeCount, cameraCount, vcaInstances.length);

    return root;
}

dvcVcaTree._createTreeItem = function (id, name, type, dataObj)
{
    return {
        id: type + id,
        name: name,
        type: type,
        data: dataObj,
        items: []
    };
}

dvcVcaTree._drawTreeWithIcons = function (vcaTreeData)
{
    var w = dvcVcaTree._innerWidth;
    var h = dvcVcaTree._innerHeight;
    var leftEndOffset = 100;
    var rightEndOffset = 250;
    var depthDistance = 220;
    var iconSize = 26;

    var zoomListener = d3.behavior.zoom()
        .scaleExtent([0.5, 2])
        .on("zoom", zoomed);

    var d3Tree = d3.layout.cluster()
        .children(function (d)
        {
            return d.items;
        })
        .separation(function (d)
        {
            return 2;
        })
        .size([h, w - rightEndOffset]);

    var nodes = d3Tree.nodes(vcaTreeData);
    var links = d3Tree.links(nodes);

    nodes.forEach(function (d)
    {
        d.y = d.depth * depthDistance;
    });

    var diagonal = d3.svg.diagonal()
        .projection(function (d)
        {
            return [d.y, d.x];
        });

    var svg = d3.select(dvcVcaTree._configs.cssSelector).append("svg")
        .attr("width", w)
        .attr("height", h)
        .append("g")
        .attr("transform", "translate(" + leftEndOffset + ",0)")
        .call(zoomListener);

    var rect = svg.append("rect")
        .attr("width", w)
        .attr("height", h)
        .style("fill", "none")
        .style("pointer-events", "all");

    var container = svg.append("g");

    var link = container.selectAll(".link")
        .data(links)
        .enter().append("path")
        .attr("class", "edge_link")
        .attr("d", diagonal);

    var node = container.selectAll(".node")
        .data(nodes)
        .enter().append("g")
        .attr("class", "d3_node")
        .attr("transform", function (d)
        {
            return "translate(" + d.y + "," + d.x + ")";
        });

    node.append("image")
        .attr("xlink:href", function (d)
        {
            return dvcVcaTree._getIcon(d);
        })
        .attr("x", function (d, i)
        {
            if (d.type == dvcVcaTree.ItemType.BUCKET)
            {
                return -iconSize + 3;
            }
            return -3;
        })
        .attr("y", function (d, i)
        {
            return -iconSize / 2;
        })
        .attr("width", iconSize)
        .attr("height", iconSize);

    node.append("text")
        .attr("class", function (d)
        {
            return d.type;
        })
        .attr("dx", function (d)
        {
            return (d.type == dvcVcaTree.ItemType.VCA) ? iconSize : -7;
        })
        .attr("dy", function (d)
        {
            if (d.type == dvcVcaTree.ItemType.BUCKET)
            {
                return iconSize;
            }
            return 5;
        })
        .attr("text-anchor", function (d)
        {
            return (d.type == dvcVcaTree.ItemType.VCA) ? "start" : "end";
        })
        .text(function (d)
        {
            return d.name;
        })
        .on("click", function (d)
        {
            nodeClicked(d);
        })
        .on("mouseenter", function (d)
        {
            dvcVcaTree._handleMouseEnter(d);
            nodeHovered(d);
        })
        .on("mouseleave", function (d)
        {
            dvcVcaTree._handleMouseLeave(d);
            nodeUnhovered(d);
        });

    d3.select(self.frameElement).style("height", h + "px");


    /**
     *
     * Event Handler and helper functions
     *
     */
    dvcVcaTree.d3.resetZoom = function ()
    {
        zoomListener.scale(1);
        zoomListener.translate([0, 0]);
        container
            .transition()
            .attr("transform", "translate(0,0)scale(1)");
    }

    dvcVcaTree.d3.zoomToNode = function (treeId)
    {
        var nodes = container.selectAll("node")
            .filter(function (current)
            {
                return current.id == treeId;
            });

        //in progress

//        var reverseTranslate = d3.transform(d3.select(nodes[0].parentNode).attr("transform")).translate;
//        var x = reverseTranslate[0];
//        var y = reverseTranslate[1];
//        console.log(nodes[0].getBoundingClientRect());
    }

    function zoomed()
    {
        container.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
    }

    function getParentLinks(selected)
    {
        var parentLinks = [];
        if (selected.parent)
        {
            parentLinks.push(selected.parent.id + "_" + selected.id);
            parentLinks = parentLinks.concat(getParentLinks(selected.parent));
        }

        return parentLinks;
    }

    function getChildLinks(selected)
    {
        var childLinks = [];
        if (selected.children)
        {
            selected.children.forEach(function (node, i)
            {
                childLinks.push(selected.id + "_" + node.id);
                childLinks = childLinks.concat(getChildLinks(node));
            });
        }

        return childLinks;
    }

    function getLinksToHighlight(selected)
    {
        var parentList = getParentLinks(selected);
        var childList = getChildLinks(selected);
        var linksToHighlight = parentList.concat(childList);

        return container.selectAll("path")
            .filter(function (current)
            {
                var identifier = current.source.id + "_" + current.target.id;
                return (linksToHighlight.indexOf(identifier) != -1);
            });
    }

    var nodeHovered = function (d)
    {
        var links = getLinksToHighlight(d);
        links
            .transition()
            .style("stroke-width", "1.5")
            .style("stroke", "steelblue");
    };

    var nodeUnhovered = function (d)
    {
        var links = getLinksToHighlight(d);
        links
            .transition()
            .style("stroke-width", "1")
            .style("stroke", "#555");
    };

    var nodeClicked = function (d)
    {
        switch (d.type)
        {
            case dvcVcaTree.ItemType.BUCKET:
                //no action yet
                break;

            case dvcVcaTree.ItemType.NODE:
                dvcVcaTree.openNodeDeviceInfo(d.data.id);
                break;

            case dvcVcaTree.ItemType.CAMERA:
                //no action yet
                break;

            case dvcVcaTree.ItemType.VCA:
                dvcVcaTree.openVcaLog(d.data);
                break;
        }
    };
}

dvcVcaTree._getIcon = function (d)
{
    var iconUrl = "/public/css/common/images/";
    var iconFile = "";
    switch (d.type)
    {
        case this.ItemType.BUCKET:
            iconFile = "cloud_server.svg";
            break;

        case this.ItemType.NODE:
            var online = (d.data.status != DvcMgr.DeviceStatus.DISCONNECTED);
            iconFile = online ? "device_node.svg" : "device_node_offline.svg";
            break;

        case this.ItemType.CAMERA:
            var online = (d.data.status != DvcMgr.DeviceStatus.DISCONNECTED);
            iconFile = online ? "node_camera.svg" : "node_camera_offline.svg";
            break;

        case this.ItemType.VCA:
            var running = d.data.vcaState == "RUNNING";
            iconFile = running ? "analytics.svg" : "analytics_offline.svg";
            break;
    }

    return iconUrl + iconFile;
}

dvcVcaTree._setWidthHeight = function (deviceCount, vcaCount)
{
    var minHeight = 400;
    deviceCount = (deviceCount * 4); // ~4 cameras per node;
    var bigger = (vcaCount > deviceCount) ? vcaCount : deviceCount;
    var dyanmicHeight = (bigger * 30);

    dvcVcaTree._innerWidth = $(dvcVcaTree._configs.cssSelector).width();
    dvcVcaTree._innerHeight = dyanmicHeight < minHeight ? minHeight : dyanmicHeight;
}

dvcVcaTree._handleMouseEnter = function (d)
{
    if (d == null)
    {
        return;
    }

    dvcVcaTree._hidePopover();
    var hoverPopover = $(dvcVcaTree._configs.cssSelector + " .hover_win");

    //all have name field
    hoverPopover.find(".name").html(d.name);
    hoverPopover.find(".name").show();

    try
    {
        switch (d.type)
        {
            case dvcVcaTree.ItemType.BUCKET:
                return;  //don't popup for bucket type

            case dvcVcaTree.ItemType.NODE:
                var statusText = localizeResource("device-status-" + d.data.status);

                hoverPopover.find(".model").html(d.data.model.name + " (v" + d.data.node.version + ")");
                hoverPopover.find(".model").show();
                hoverPopover.find(".status").html(statusText);
                hoverPopover.find(".status").show();
                break;

            case dvcVcaTree.ItemType.CAMERA:
                hoverPopover.find(".model").html(d.data.model ? d.data.model.name : "Unknown");
                hoverPopover.find(".model").show();

                var statusText = localizeResource("device-status-" + d.data.status);
                hoverPopover.find(".status").html(statusText);
                hoverPopover.find(".status").show();
                break;

            case dvcVcaTree.ItemType.VCA:
                var summary = localizeResource("not-scheduled");
                if (d.data.recurrenceRule)
                {
                    summary = d.data.recurrenceRule.summary;
                }

                var vcaStatus = localizeResource("vca-state-" + d.data.vcaState);
                if (!d.data.enabled)
                {
                    vcaStatus = localizeResource("vca-state-DISABLED");
                }

                hoverPopover.find(".status").html(localizeResource("status") + ": " + vcaStatus);
                hoverPopover.find(".status").show();
                hoverPopover.find(".schedule").html(localizeResource("schedule") + ": " + summary);
                hoverPopover.find(".schedule").show();
                break;

            default :
                return;
        }

        hoverPopover.css("left", d3.event.pageX - 50);
        hoverPopover.css("top", d3.event.pageY - 20);
        hoverPopover.show();
    } catch (e)
    {
        console.error(e);
    }
}

dvcVcaTree._handleMouseLeave = function (d)
{
    dvcVcaTree._hidePopover();
}

dvcVcaTree._hidePopover = function ()
{
    var hoverPopover = $(dvcVcaTree._configs.cssSelector + " .hover_win");
    hoverPopover.find("div").hide();
    hoverPopover.hide();
}

dvcVcaTree.openVcaLog = function (vcaInstance)
{
    var contentPage = "/vca/errorlog/" + vcaInstance.instanceId;
    utils.openPopup(localizeResource('vca-logs'), contentPage, null, null, true, function ()
    {
    });
}

dvcVcaTree.openNodeDeviceInfo = function (nodeId)
{
    var contentPage = "/device/nodeinfo?id=" + nodeId + "&readonly=true";
    utils.openPopup(localizeResource("node-info"), contentPage, null, null, true, function ()
    {
    });
}