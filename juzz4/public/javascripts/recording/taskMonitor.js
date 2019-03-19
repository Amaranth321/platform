/**
 *
 * @author Aye Maung
 */
function TaskMonitor()
{
    var Types = {
        RUNNING_TASKS: "RUNNING_TASKS"
    };

    var _typeInfo = {
        RUNNING_TASKS: {
            api: "trackrunningtasks",
            socket: null
        }
    }

    var _getTypes = function ()
    {
        Object.keys(Types);
    };

    var _updateProgressTracker = function (evt)
    {
        var taskList = JSON.parse(evt.data);

//        //mock
//        taskList.push({currentTask: "fake task 1", percent: 23});
//        taskList.push({currentTask: "fake task 2", percent: 87});

        //draw each row
        var $taskWin = $(".task_monitor");
        var htmlRowList = [];
        $.each(taskList, function (i, task)
        {
            var template = kendo.template($("#taskProgressTmpl").html());
            htmlRowList.push(template(task));
        });
        $taskWin.find(".grid").html(htmlRowList);

        //close if done
        if (taskList.length == 0)
        {
            $taskWin.slideUp();
        }
        else
        {
            $taskWin.slideDown();
        }
    };

    var _startMonitorRunning = function (apiList)
    {
        var runningType = Types.RUNNING_TASKS;
        _stopMonitor(runningType);

        var wsUrl = _getWSUrl(runningType);
        if (apiList && apiList.length > 0)
        {
            wsUrl += "api-list=" + apiList.join(",");
        }

        _typeInfo[runningType].socket = _openSocket(wsUrl, _updateProgressTracker);
    };

    var _stopMonitor = function (type)
    {
        var socket = _typeInfo[type].socket;
        if (socket)
        {
            socket.close();
        }
    };

    var _stopAll = function ()
    {
        $.each(_getTypes(), function (i, type)
        {
            _stopMonitor(type);
        });
    };

    var _getWSUrl = function (type)
    {
        var api = _typeInfo[type].api;
        return kupapi.getWsServer() + "/ws/" + api + "?";
    };

    var _openSocket = function (wsUrl, onDataReceived)
    {
        var _socket = new WebSocket(wsUrl);

        _socket.onmessage = onDataReceived;

        _socket.onopen = function (evt)
        {
            console.log("opened: " + wsUrl);
        };

        _socket.onclose = function (evt)
        {
            console.log("closed: " + wsUrl);

        };

        _socket.onerror = function (evt)
        {
            console.error(evt);
        };

        return _socket;
    };


    return {
        Types: Types,
        getMonitorTypes: _getTypes,
        startMonitorRunning: _startMonitorRunning,
        stopMonitor: _stopMonitor,
        stopAll: _stopAll
    }
}