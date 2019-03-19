/**
 * @author keith
 * @since v4.6.3.0 (feature-branch)
 *
 * @param div
 * @returns {{init: init, initWithDragNDrop: initWithDragNDrop, filterTreeView: filterTreeView}}
 * @constructor
 */
var DeviceTreeView = function (div)
{
    var $dropArea;
    var dragEvent;
    var dropEvent;
    var $treeViewDiv = $(div);
    var devTreeElement = {
        treeView : null
    };
    var cache = {
        defaultDeviceTreeData: [],
        deviceTreeMap: {},
        runningAnalyticsMap: {},
    };

    /***************************    Private functions    ***************************/

    var TreeLabelObject = function (label)
    {
        return {
            text: label.name,
            imageUrl: kupapi.CdnPath + "/common/images/treeicons/label-tag-small.png",
            type: "label",
            labelId: label.labelId,
            labelName: label.name,
            labelType: label.type
        }
    }

    var TreeDeviceObject = function (label, device, cameras)
    {
        var deviceIcon = device.status == "CONNECTED" ? kupapi.CdnPath + "/common/images/treeicons/kup-node-small.png" :
                         kupapi.CdnPath + "/common/images/treeicons/kup-node-offline-small.png";
        return {
            text: device.name,
            imageUrl: deviceIcon,
            items: cameras,
            type: "device",
            labelId: label.labelId,
            labelName: label.labelName,
            coreDeviceId: device.deviceId,
            platformDeviceId: device.id+""
        }
    }

    var TreeChannelObject = function (device, camera)
    {
        return {
            text: camera.name,
            imageUrl: kupapi.CdnPath + "/common/images/treeicons/video-camera-small.png",
            type: "channel",
            deviceName: device.name,
            coreDeviceId: device.deviceId,
            platformDeviceId: device.id+"",
            channelId: camera.nodeCoreDeviceId
        }
    }

    var _getRunningAnalyticsReady = function (callback)
    {
        // get running analytics
        listRunningAnalytics("", "ALL", function(respVCAs){
            if (respVCAs.result == "ok")
            {
                    cache.runningAnalyticsMap = {};
                    $.each(respVCAs.instances, function(aInd, vca) {
                        if (!cache.runningAnalyticsMap[vca.coreDeviceId + "-" + vca.channelId])
                        {
                            cache.runningAnalyticsMap[vca.coreDeviceId + "-" + vca.channelId] = "";
                        }
                        cache.runningAnalyticsMap[vca.coreDeviceId + "-" + vca.channelId] += " " + vca.type;
                    });
            }
            callback();
        });
    }

    var _buildDeviceTree = function (callback)
    {
        var bucketLabels = [];
        var treeData = [];
        var labelAll = {
            labelId: "all",
            name: localizeResource("all")
        }

        // get labels
        getLabels(function(resp){
            if(resp.result == "ok")
            {
                bucketLabels = resp.labels;
            }
            else
            {
                return;
            }

            // add 'All' label
            treeData.push(new TreeLabelObject(labelAll));

            // add labels
            $.each(bucketLabels, function(ind, label) {
                treeData.push(new TreeLabelObject(label));
            });

            // form 'Label' layer tree data
            $.each(treeData, function (lInd, labelData){

                // form 'Device' layer tree data
                $.each(DvcMgr.getUserDeviceList(), function(dInd, device) {

                    // device is node (on Cloud side)
                    if (device.node)
                    {
                        // form 'Channel' layer tree data
                        var channelData = [];
                        $.each(device.node.cameras, function (ind, camera) {
                            var deviceLabels = LabelMgr.getAssignedAllLabel(device.deviceId, camera.nodeCoreDeviceId);
                            if (labelData.labelId == "all")
                            {
                                channelData.push(new TreeChannelObject(device, camera));
                            }
                            else if (typeof deviceLabels !== "undefined")
                            {
                                $.each(deviceLabels, function (dInd, devicelabel){
                                    if (labelData.labelId == devicelabel.labelId)
                                    {
                                        channelData.push(new TreeChannelObject(device, camera));
                                    }
                                });
                            }
                        });

                        if (channelData.length > 0)
                        {
                            if (!labelData.items)
                            {
                                labelData.items =[];
                            }
                            var treeDevice = new TreeDeviceObject(labelData, device, channelData);
                            labelData.items.push(treeDevice);
                            cache.deviceTreeMap[treeDevice.labelId + "-" + treeDevice.coreDeviceId] = treeDevice;
                        }
                    }
                    // device is camera (on Node side)
                    else if(labelData.labelId == "all")
                    {
                        var channelData = [];
                        var cameraTmp = {
                            name: localizeResource("channel-id") + " 01",
                            nodeCoreDeviceId: "0"
                        };
                        channelData.push(new TreeChannelObject(device, cameraTmp));
                        var treeDevice = new TreeDeviceObject(labelData, device, channelData);
                        if (!labelData.items)
                        {
                            labelData.items =[];
                        }
                        labelData.items.push(treeDevice);
                        cache.deviceTreeMap[treeDevice.labelId + "-" + treeDevice.coreDeviceId] = treeDevice;
                    }

                    //add device cache map
                    cache.deviceTreeMap[labelData.labelId] = labelData;
                });

                //sort devices
                if (labelData.items)
                {
                    labelData.items.sort(function(a, b){
                        if (a.text < b.text)
                            return -1;
                        if (a.text > b.text)
                            return 1;
                        return 0;
                    });
                }
            });

            cache.defaultDeviceTreeData = treeData;
            _displayDeviceTree(cache.defaultDeviceTreeData, callback);
        });
    }

    var _displayDeviceTree = function (deviceTreeData, callback)
    {
        if(devTreeElement.treeView)
        {
            devTreeElement.treeView.destroy();
            $treeViewDiv.html("");
        }

        devTreeElement.treeView = $treeViewDiv.kendoTreeView({
            dragAndDrop: (dragEvent && dropEvent) ? true : false,
            loadOnDemand: true,
            dataSource: deviceTreeData,
            dragstart: onDragStart,
            drag: onDrag,
            drop: onDrop
        }).data("kendoTreeView");

        // drag and drop feature support
        function onDragStart(e)
        {
            var droppedItem =  e.sender.dataItem(e.sourceNode);
            if(droppedItem.type == "label")
            {
                droppedItem = cache.deviceTreeMap[droppedItem.labelId];
            }
            else if(droppedItem.type == "device")
            {
                droppedItem = cache.deviceTreeMap[droppedItem.labelId + "-" + droppedItem.coreDeviceId];
            }

            if (dragEvent)
            {
                dragEvent(droppedItem, e);
            }
        }
        function onDrag(e) {
            if ($(e.dropTarget).closest($dropArea)[0]) {
                e.setStatusClass("k-add");
            } else {
                e.setStatusClass("k-denied");
            }
        }

        function onDrop(e) {
            if ($(e.dropTarget).closest($dropArea)[0]) {
                e.preventDefault();
                var droppedItem =  e.sender.dataItem(e.sourceNode);
                if(droppedItem.type == "label")
                {
                    droppedItem = cache.deviceTreeMap[droppedItem.labelId];
                }
                else if(droppedItem.type == "device")
                {
                    droppedItem = cache.deviceTreeMap[droppedItem.labelId + "-" + droppedItem.coreDeviceId];
                }

                if (dropEvent)
                {
                    dropEvent(droppedItem, e);
                }
            }
        }

        if (callback)
        {
            callback();
        }
    }

    var _containsIgnoreCase = function (fullword, keyword)
    {
        if (fullword.toLowerCase().indexOf(keyword.toLowerCase()) >= 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /***************************    Public functions    ***************************/

    /**
     * filter device tree view and refresh
     * @param keyword, mandatory field: string word
     * @param vcaType, optional field: string word eg: PCOUNTING
     */
    var filterTreeView = function (keyword, vcaType)
    {
        // clone tree data
        var filterTreeData = JSON.parse(JSON.stringify(cache.defaultDeviceTreeData));

        // filter label
        var labelIndex = filterTreeData.length;
        $.each(JSON.parse(JSON.stringify(cache.defaultDeviceTreeData)).reverse(), function (lInd, label)
        {
            labelIndex--;
            if(!label.items)
            {
                if(!_containsIgnoreCase(label.text, keyword) || vcaType)
                {
                    filterTreeData.splice(labelIndex, 1);
                }
                return;
            }

            var devices = JSON.parse(JSON.stringify(label.items)).reverse(); // clone with reverse array
            var deviceIndex = label.items.length;
            var isContainVCAOnLabel = false;

            // filter device
            $.each(devices, function (dInd, device)
            {
                var isContainVCA = false;
                var channels = JSON.parse(JSON.stringify(device.items)).reverse(); // clone with reverse array
                var channelIndex = device.items.length;
                deviceIndex--;

                // filter channel
                $.each(channels, function (cInd, channel)
                {
                    var runningVCA = cache.runningAnalyticsMap[device.coreDeviceId + "-" + channel.channelId];
                    channelIndex--;

                    if(runningVCA && vcaType && _containsIgnoreCase(runningVCA, vcaType))
                    {
                        isContainVCA = true;
                        isContainVCAOnLabel = true;
                    }

                    //check for keyword match and running vca match
                    // remove channel
                    if (!_containsIgnoreCase(channel.text, keyword) ||
                        (vcaType && !runningVCA) ||
                        (vcaType && !_containsIgnoreCase(runningVCA, vcaType)))
                    {
                        filterTreeData[labelIndex].items[deviceIndex].items.splice(channelIndex, 1);
                    }
                });

                // remove device
                if ((filterTreeData[labelIndex].items[deviceIndex].items.length == 0 && !_containsIgnoreCase(device.text, keyword)) ||
                    (filterTreeData[labelIndex].items[deviceIndex].items.length == 0 && !isContainVCA && vcaType))
                {
                    filterTreeData[labelIndex].items.splice(deviceIndex, 1);
                }
            });

            // remove label
            if ((filterTreeData[labelIndex].items.length == 0 && !_containsIgnoreCase(label.text, keyword)) ||
                (filterTreeData[labelIndex].items.length == 0 && !isContainVCAOnLabel && vcaType))
            {
                filterTreeData.splice(labelIndex, 1);
            }
        });

        // update device treeview
        _displayDeviceTree(filterTreeData);
    }

    /**
     * initialize device tree view
     * @param callback, once process is finished
     */
    var init = function (callback)
    {
        DvcMgr.ready(function() {
            LabelMgr.ready(function () {
                _getRunningAnalyticsReady(function(){
                    _buildDeviceTree(callback);
                });
            });
        });
    }

    /**
     * initialize device tree view with drag and drop functions
     * @param dropArea drop area div or class
     * @param dragCallback, function call: (1) drag datasource item, (2) drag node
     * @param dropCallback, function call: (1) drop datasource item, (2) drop node
     * @param readyCallback, once process is finished
     */
    var initWithDragNDrop = function (dropArea, dragCallback, dropCallback, readyCallback)
    {
        $dropArea = $(dropArea);
        dragEvent = dragCallback;
        dropEvent = dropCallback;
        init(function(){
            readyCallback();
        });
    }

    var getDefaultDeviceTreeData = function ()
    {
        return JSON.parse(JSON.stringify(cache.defaultDeviceTreeData));
    }

    return {
        init: init,
        initWithDragNDrop: initWithDragNDrop,
        filterTreeView: filterTreeView,
        getDefaultDeviceTreeData: getDefaultDeviceTreeData
    }
}