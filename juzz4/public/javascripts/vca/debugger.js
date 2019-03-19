var debugWin = {
    feedViewId: "feedViewBox",
    processedViewId: "processedViewBox",
    instanceId: null,
    httpPort: null,
    fpsRefreshFreq: 1000,
    fpsTimer: null
};

debugWin.start = function (instanceId)
{
    //init values
    debugWin.instanceId = instanceId;
    debugWin.httpPort = null;

    debugWin.getCurrentCommands(function (success)
    {
        if (!success)
        {
            return;
        }

        debugWin.initUI();
    });
}

debugWin.initUI = function ()
{
    debugWin.loading(true);
    utils.createTooltip("changeInfo", "right", localizeResource("msg-vca-change-info"));

    debugWin.displayVideoStream(debugWin.feedViewId, "frame");
    debugWin.initEventOutput();

    //query info
    debugWin.queryVcaInfo(function (vcaInfo)
    {
        debugWin.updateProcessedViewList(vcaInfo.windows);
        debugWin.updateVcaDetails(vcaInfo);
        debugWin.monitorFPS(true);
        debugWin.loading(false);
    });
}

debugWin.getCurrentCommands = function (callback)
{
    if (callback == null)
    {
        callback = function ()
        {
        }
    }

    debugWin.loading(true);
    getVcaCommands("", debugWin.instanceId, function (responseData)
    {
        if (responseData.result == "ok" && responseData.commands != null)
        {
            debugWin.updateCommandBox(responseData.commands);
            callback(true);
        }
        else
        {
            debugWin.updateStatus(localizeResource("failed-to-get-commands"));
            callback(false);
        }

        debugWin.loading(false);
    }, null);
}

debugWin.updateCommandBox = function (commandString)
{
    commandString = commandString.replace(new RegExp("\\\\", "g"), "/");
    var rootPath = commandString.split("resources/vca")[0];
    var rootRemoved = commandString.replace(new RegExp(rootPath, "g"), "####/");
    $("#vcaCommands").html(rootRemoved);

    //update vcaPort
    var cmdArray = commandString.split(" ");
    $.each(cmdArray, function (index, item)
    {
        if (item == "-httpport" || item == "--debugport")
        {
            debugWin.httpPort = cmdArray[index + 1];
            return false;
        }
    });
}

debugWin.queryVcaInfo = function (callback)
{
    if (debugWin.httpPort == null)
    {
        console.log("httpPort is null");
        return;
    }

    function doNothingOnError()
    {
        console.log("vca info call failed");
    }

    ajax(
            "http://" + window.location.hostname + ":" + debugWin.httpPort + "/info",
        {},
        function (response)
        {
            try
            {
                var vcaInfo = JSON.parse(response);
                callback(vcaInfo);
            }
            catch (e)
            {
                console.error(e);
            }
        },
        doNothingOnError,
        "GET"
    );
}

debugWin.updateProcessedViewList = function (windows)
{

    function viewSelected(winItem)
    {
        debugWin.displayVideoStream(debugWin.processedViewId, winItem.name);
        $(".vca_debug_right_box .resolution").html(winItem.size);
    }

    //init view list
    var kendoList = $("#viewList").kendoComboBox({
        dataTextField: "name",
        dataValueField: "name",
        dataSource: [],
        change: function (e)
        {
            viewSelected(this.dataItem());
        }
    }).data("kendoComboBox");

    if (windows == null || windows.length == 0)
    {
        return;
    }

    //remove unprocessed view
    var filtered = [];
    $.each(windows, function (i, win)
    {
        if (win.name == "frame")
        {
            return true;
        }
        filtered.push(win);
    });

    //select the first one by default
    if (filtered.length > 0)
    {
        kendoList.dataSource.data(filtered);
        kendoList.select(0);
        viewSelected(filtered[0]);
    }
}

debugWin.updateVcaDetails = function (vcaInfo)
{
    if (vcaInfo == null)
    {
        return;
    }

    $(".vca_details_wrapper .version").html(vcaInfo.version);
    $(".vca_details_wrapper .vcaUrl").html(debugWin.getVcaUrl());

    var $processFps = $(".vca_details_wrapper .process_fps");
    var $streamFps = $(".vca_details_wrapper .stream_fps");

    //KAI x1
    if (vcaInfo["fps"])
    {
        $processFps.show();
        $processFps.find("span").html(vcaInfo["fps"].toFixed(1));
    }

    //KAI x2
    if (vcaInfo["process_fps"])
    {
        $processFps.show();
        $processFps.find("span").html(vcaInfo["process_fps"].toFixed(1));
    }

    if (vcaInfo["stream_fps"])
    {
        $streamFps.show();
        $streamFps.find("span").html(vcaInfo["stream_fps"].toFixed(1));
    }
}

debugWin.updateStatus = function (msg)
{
    $("#statusText").html(msg);
}

debugWin.monitorFPS = function (enabled)
{
    if (enabled)
    {
        if (debugWin.fpsTimer == null)
        {
            debugWin.fpsTimer = setInterval(function ()
            {
                debugWin.queryVcaInfo(debugWin.updateVcaDetails);
            }, debugWin.fpsRefreshFreq);
        }

    }
    else
    {
        if (debugWin.fpsTimer != null)
        {
            clearInterval(debugWin.fpsTimer);
        }
    }
}

debugWin.initEventOutput = function ()
{
    var eventUrl = debugWin.getVcaUrl() + "/events";
    var $wrapper = $(".right_column .event_wrapper");
    $wrapper.height($(".debug_win .left_column").height() - 74);

    if (utils.detectBrowser() != "chrome")
    {
        $wrapper.html('<div style="margin-top: 20px">Real time EventSource is currently supported on Chrome browser only</div>');
        return;
    }

    var htmlIframe = '<iframe src="' + eventUrl + '"></iframe>';
    $wrapper.html(htmlIframe);
    var $iFrame = $wrapper.find("iframe").height($wrapper.height());

    /*
     Below is the better way to consume EventSource
     But, 'Access-Control-Allow-Origin' header needed from vca for this to work
     */

//    var evtSrc = new EventSource(eventUrl);
//    evtSrc.onmessage = function (event) {
//        console.log(event.data);
//        $wrapper.append(event.data);
//    }
//
//    evtSrc.onerror = function (event) {
//        console.log(event);
//    }

}

debugWin.displayVideoStream = function (viewBoxId, viewType)
{
    if (debugWin.httpPort == null)
    {
        return;
    }

    debugWin.useIFramePlayer(viewBoxId, viewType);
//    debugWin.useImgPlayer(viewBoxId, viewType);
}

debugWin.applyNewCommands = function ()
{
    var cmdStr = $("#editCommands").val();
    if (cmdStr == "")
    {
        debugWin.updateStatus(localizeResource("emtpy-value"));
        return;
    }

    debugWin.loading(true);
    setVcaCommands("", debugWin.instanceId, cmdStr, function (responseData)
    {
        debugWin.loading(false);
        if (responseData.result == "ok")
        {
            var response = responseData.message;

            if (Math.floor(response) == response && $.isNumeric(response))
            {
                debugWin.updateStatus("syntax error");
                return;
            }

            debugWin.updateStatus(responseData.message);
            debugWin.getCurrentCommands();
        }
        else
        {
            debugWin.updateStatus(localizeResource("update-failed"));
        }
    }, null);
}

debugWin.getVcaUrl = function ()
{
    var host = (window.location.hostname == "localhost") ? "127.0.0.1" : window.location.hostname;
    return "http://" + host + ":" + debugWin.httpPort;
}

debugWin.useIFramePlayer = function (viewBoxId, viewType)
{
    var jsUrl = debugWin.getVcaUrl() + "/" + viewType + ".js.html";
    var flashUrl = debugWin.getVcaUrl() + "/" + viewType + ".html";

    var htmlIframe = '<iframe src="' + jsUrl + '"></iframe>';
    $("#" + viewBoxId).html(htmlIframe);
}

debugWin.useImgPlayer = function (viewBoxId, viewType)
{
    var playerId = viewBoxId + "_player";
    $("#" + viewBoxId).html('<img id="' + playerId + '" style="width:100%;height:100%;overflow:hidden"/>');

    function getRandLink()
    {
        var jpgUrl = debugWin.getVcaUrl() + "/" + viewType + ".jpg";
        return jpgUrl + "?timestamp=" + new Date().getTime();
    }

    function setImage()
    {
        $("#" + playerId).attr("src", getRandLink());
    }

    $("#" + playerId)
        .load(setImage)
        .error(setImage)
        .attr("src", getRandLink());
}

debugWin.loading = function (isLoading)
{
    kendo.ui.progress($(".debug_win"), isLoading);
}