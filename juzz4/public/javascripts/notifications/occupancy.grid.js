/**
 * DvcMgr.ready() must be called before generate()
 *
 * @author Aye Maung
 */

var OccupancyGrid = (function ()
{
    var userLabels = [];
    var monitors = {};
    var $occGrid = $(".occ_grid");

    var generate = function ()
    {
        userLabels = LabelMgr.getUserAccessibleStoreLabels();
        monitorLabels();
    };

    var monitorLabels = function ()
    {
        var $holder = $occGrid.find(".holder");
        if (Object.keys(userLabels).length == 0)
        {
            $holder.html(localizeResource("no-store-labels-assigned"));
            return;
        }

        $.each(userLabels, function (i, labelObj)
        {
            var labelId = labelObj.labelId;
            var template = kendo.template($("#storeBoxTmpl").html());
            var templateData = {
                labelId: labelId,
                name: labelObj.name,
                occupancy: 0
            };
            $holder.append(template(templateData));

            //monitor changes via web socket
            var occMon = new OccupancyMonitor(labelId, labelObj.name, occupancyChanged);
            occMon.open();
            monitors[labelId] = occMon;
        });

        utils.centerKendoWin($occGrid);
    };

    var stopMonitoring = function ()
    {
        $.each(monitors, function (labelId, monitor)
        {
            monitor.close();
        });
    };

    var occupancyChanged = function (occData)
    {
        var labelId = occData.labelId;
        var collectiveOccupancy = 0;
        var perCameraList = [];
        $.each(occData.occupancyMap, function (camera, occupancyObj)
        {
            var deviceName = DvcMgr.getDeviceName(camera.coreDeviceId);
            var channelName = DvcMgr.getChannelName(camera.coreDeviceId, camera.channelId);
            perCameraList.push({
                deviceName: deviceName,
                channelName: channelName,
                occupancy: occupancyObj
            });

            collectiveOccupancy += occupancyObj.count;
        });

        console.log(collectiveOccupancy);
        $occGrid.find("#" + labelId + " .number").html(collectiveOccupancy);
    };

    var loading = function (loading)
    {
        kendo.ui.progress($occGrid, loading);
    };

    return {
        generate: generate,
        stopMonitoring: stopMonitoring
    }
})();

function OccupancyMonitor(labelId, logName, onChanged, onConnectionClosed)
{
    var wsUrl = kupapi.getWsServer() + "/ws/monitorOccupancyChange?label-id=" + labelId;
    var _socket;
    var timer;

    var _openSocket = function ()
    {
        if (_socket != null && _socket.readyState == WebSocket.OPEN)
        {
            return;
        }

        var idPostFix = logName ? "(" + logName + ")" : "";
        _socket = new WebSocket(wsUrl);

        _socket.onmessage = function (evt)
        {
            if (timer)
            {
                clearTimeout(timer);
            }

            console.log("OccupancyMonitor : changed", idPostFix, evt.data);
            if (onChanged)
            {
                onChanged(JSON.parse(evt.data));
            }
        };

        _socket.onopen = function (evt)
        {
            console.log("OccupancyMonitor : opened", idPostFix);
        };

        _socket.onclose = function (evt)
        {
            console.log("OccupancyMonitor : closed", idPostFix);
            if (onConnectionClosed)
            {
                onConnectionClosed();
            }
        };

        _socket.onerror = function (evt)
        {
            console.error("OccupancyMonitor :", evt, idPostFix);
        };
    };

    var _closeSocket = function ()
    {
        if (_socket)
        {
            _socket.close();
        }
    };

    return {
        open: _openSocket,
        close: _closeSocket
    }
}