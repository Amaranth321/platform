var utils = {};
var alertMsgTimeOut = null;
var infoMsgTimeOut = null;
var activePopupId = "";
var flag = 0;
var flag1 = 0;
var loadingTimer = null;

utils.throwServerError = function (responseData)
{

    utils.hideLoadingOverlay();

    var errorMsg = "";
    var redirect = false;

    if (responseData == null || responseData.reason == null)
    {
        console.warn("Server error! responseData:" + JSON.stringify(responseData));
        return;
    }
    else
    {
        if (responseData.reason == "timeout")
        {
            errorMsg = localizeResource("session-expired") + "!";
            redirect = true;
        }
        else
        {
            errorMsg = localizeResource(responseData.reason);
        }
    }

    utils.popupAlert(errorMsg, function ()
    {
        if (redirect)
        {
            if (document.getElementById("iframePopup"))
            {
                window.parent.location.href = "/" + kupBucket;
            }
            else
            {
                window.location.href = "/" + kupBucket;
            }
        }
    });
}

utils.popupAlert = function (alertMsg, callback)
{

    var contentHtml = '<div style="min-height:40px">' + alertMsg + '</div>';

    var kendoWindow = $("<div />").kendoWindow({
        visible: false,
        title: localizeResource("system-message"),
        resizable: false,
        modal: true,
        width: 300,
        close: callback
    });

    kendoWindow.data("kendoWindow").content(contentHtml).center().open();
    $(".k-window-title").css("height", "30px");
}

utils.detectBrowser = function ()
{
    function testCSS(prop)
    {
        return prop in document.documentElement.style;
    }

    var isOpera = !!(window.opera && window.opera.version);
    var isFirefox = testCSS('MozBoxSizing');
    var isSafari = Object.prototype.toString.call(window.HTMLElement).indexOf('Constructor') > 0;
    var isChrome = !isSafari && testCSS('WebkitTransform');
    var isIE = /* @cc_on!@ */ false || testCSS('msTransform');

    var browser = "";
    if (isOpera)
    {
        browser = "opera";
    }
    else if (isFirefox)
    {
        browser = "firefox";
    }
    else if (isSafari)
    {
        browser = "safari";
    }
    else if (isChrome)
    {
        browser = "chrome";
    }
    else if (isIE)
    {
        browser = "ie";
    }

    return browser;
}

utils.detectOS = function ()
{
    var OSName = "Unknown OS";
    if (navigator.appVersion.indexOf("Win") != -1)
    {
        OSName = "Windows";
    }
    if (navigator.appVersion.indexOf("Mac") != -1)
    {
        OSName = "MacOS";
    }
    if (navigator.appVersion.indexOf("X11") != -1)
    {
        OSName = "UNIX";
    }
    if (navigator.appVersion.indexOf("Linux") != -1)
    {
        OSName = "Linux";
    }
    return OSName;
}

utils.convertToUTC = function (dateObj)
{
    return new Date(dateObj.getUTCFullYear(),
        dateObj.getUTCMonth(),
        dateObj.getUTCDate(),
        dateObj.getUTCHours(),
        dateObj.getUTCMinutes(),
        dateObj.getUTCSeconds());
}

utils.convertToHourBoundriesUTC = function (dateObj, type)
{
    if (type == "from")
    {
        dateObj.setUTCMinutes(00);
        dateObj.setUTCSeconds(00);
        return new Date(dateObj.getUTCFullYear(),
            dateObj.getUTCMonth(),
            dateObj.getUTCDate(),
            dateObj.getUTCHours(),
            dateObj.getUTCMinutes(),
            dateObj.getUTCSeconds());
    }
    else if (type == "to")
    {
        if (dateObj.getUTCMinutes() > 0)
        {
            dateObj.setUTCHours(dateObj.getUTCHours() + 1);
            dateObj.setUTCMinutes(00);
            dateObj.setUTCSeconds(00);
            return new Date(dateObj.getUTCFullYear(),
                dateObj.getUTCMonth(),
                dateObj.getUTCDate(),
                dateObj.getUTCHours(),
                dateObj.getUTCMinutes(),
                dateObj.getUTCSeconds());
        }
        else
        {
            dateObj.setUTCMinutes(00);
            dateObj.setUTCSeconds(00);
            return new Date(dateObj.getUTCFullYear(),
                dateObj.getUTCMonth(),
                dateObj.getUTCDate(),
                dateObj.getUTCHours(),
                dateObj.getUTCMinutes(),
                dateObj.getUTCSeconds());
        }
    }
}

/**
 * Convert UTC to Local time.
 * @param {Date} dateObj
 * @return {Date}
 */
utils.convertUTCtoLocal = function (dateObj)
{
    if (dateObj != null)
    {
        var d = new Date();
        dateObj.setMinutes(dateObj.getMinutes() - d.getTimezoneOffset());
        return dateObj;
    }
}

utils.popupConfirm = function (title, confirmMsg, callback)
{
    var choice = false;
    $("#popupWin .confirm_msg").html(confirmMsg);
    var contentHtml = $("#popupWin").html();
    var kendoWindow = $("#popupWin").kendoWindow({
        visible: false,
        title: title,
        resizable: false,
        modal: true,
        width: 300,
        close: onClosed
    });
    $("#popupWin").parent().find(".k-window-action").css("visibility", "hidden");

    function onClosed()
    {
        callback(choice);
    }

    kendoWindow.data("kendoWindow").content(contentHtml).center().open();

    $("#btnConfirmOk").click(function ()
    {
        choice = true;
        $("#popupWin").data("kendoWindow").close();
    });

    $("#btnConfirmCancel").click(function ()
    {
        choice = false;
        $("#popupWin").data("kendoWindow").close();
    });
}

utils.slideDownAlert = function (timestamp, alertType, deviceName)
{

    var localDt = utils.convertUTCtoLocal(kendo.parseDate(timestamp, kupapi.TIME_FORMAT));
    timestamp = kendo.toString(localDt, kupapi.TIME_FORMAT);

    var msgHtml = '<div class="alert_box_title">' + timestamp + '</div>' + '<div class="alert_box_content">' + '<label class="alert_box_label">' + localizeResource('device-name') + '</label>: ' + deviceName + '<br/>' + '<label class="alert_box_label">' + localizeResource('event-type') + '</label>: ' + localizeResource(alertType) + '<br/>' + '</div>';

    var notifyBox = document.getElementById("notifyBox") ? $("#notifyBox") : window.parent.$("#notifyBox");
    var alertBox = document.getElementById("alertBox") ? $("#alertBox") : window.parent.$("#alertBox");

    clearTimeout(alertMsgTimeOut);
    clearTimeout(infoMsgTimeOut);
    notifyBox.hide();

    alertBox.html(msgHtml);
    alertBox.show("slide", {
        direction: "up"
    }, 1000);
    alertMsgTimeOut = setTimeout(function ()
    {
        alertBox.hide("slide", {
            direction: "up"
        }, 1000);
    }, 8000);
}

utils.slideDownInfo = function (notifyMsg)
{

    var msgHtml = '<div class="success_image"><span>' + notifyMsg + '</span></div>';

    var notifyBox = null;
    var alertBox = null;

    if (document.getElementById("notifyBox"))
    {
        notifyBox = $("#notifyBox");
        alertBox = $("#alertBox");

        clearTimeout(alertMsgTimeOut);
        clearTimeout(infoMsgTimeOut);
        alertBox.hide();
        notifyBox.html(msgHtml);
        notifyBox.fadeIn(1000);

        infoMsgTimeOut = setTimeout(function ()
        {
            notifyBox.fadeOut(1000);
        }, 3000);
    }
    else
    {
        notifyBox = window.parent.$("#notifyBox");
        alertBox = window.parent.$("#alertBox");

        clearTimeout(window.parent.alertMsgTimeOut);
        clearTimeout(window.parent.infoMsgTimeOut);
        alertBox.hide();
        notifyBox.html(msgHtml);
        notifyBox.fadeIn(1000);

        window.parent.infoMsgTimeOut = window.parent.setTimeout(function ()
        {
            notifyBox.fadeOut(1000);
        }, 3000);
    }
}

utils.removeLineBreaks = function (nlStr)
{
    return nlStr.replace(new RegExp('\n', 'g'), ' ');
}

// converts string to json
utils.getJSonObject = function (value)
{
    return $.parseJSON(value.replace(/&quot;/ig, '"'));
}

utils.showLoadingOverlay = function ()
{
    $(".darkened-overlay").show();
    $(".loading-icon-overlay").show();
    kendo.ui.progress($(".k-window-content"), true);
}

utils.showLoadingTextOverlay = function (displayText, showCloseButton)
{
    $("#loadingText").html(displayText);
    $(".darkened-overlay").show();
    $(".loading-text-overlay").show();

    // count the time taken for close button
    if (showCloseButton)
    {
        $("#loadingTimeSeconds").html("");
        $("#loadingTimeSeconds").show();
        var timeTaken = 0;

        loadingTimer = setInterval(function ()
        {
            timeTaken++;
            $("#loadingTimeSeconds").html(timeTaken + "s");

            if (timeTaken > 10)
            {
                $("#btnLoadingCancel").show();
            }
        }, 1000);
    }
}

utils.hideLoadingOverlay = function ()
{
    $(".darkened-overlay").hide();
    $(".loading-icon-overlay").hide();
    $(".loading-text-overlay").hide();
    $("#btnLoadingCancel").hide();
    $("#loadingTimeSeconds").hide();
    kendo.ui.progress($(".k-window-content"), false);

    if (loadingTimer)
    {
        clearInterval(loadingTimer);
    }
}

utils.loadIframe = function (iframeName, url)
{
    var $iframe = $('#' + iframeName);
    if ($iframe.length)
    {
        $iframe.attr('src', url);
        return false;
    }
}

utils.preloadImage = function (imgUrl, onSuccess, onFailure)
{
    var img = new Image();
    utils.showLoadingTextOverlay(localizeResource("loading-image"), true);
    $(img).load(function ()
    {
        utils.hideLoadingOverlay();
        onSuccess();
    }).error(function ()
    {
        utils.hideLoadingOverlay();
        onFailure();
    }).attr('src', imgUrl);
}

// divId can be id or class name
utils.createTooltip = function (divId, position, content)
{
    var contentHtml = "<span style='font-size:11px;text-align:left; display: inline-block; max-width:200px'>" + content + "</span>"

    var domElement;
    if (document.getElementById(divId))
    {
        domElement = $("#" + divId);
    }
    else
    {
        domElement = $("." + divId);
    }

    domElement.kendoTooltip({
        position: position,
        content: contentHtml
    }).data("kendoTooltip");
}

utils.createDateTimeRangeSelection = function (fromDivId, toDivId)
{

    var now = new Date();
    var todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
    var todayEnd = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 0, 0, -1);

    var start = $("#" + fromDivId).kendoDateTimePicker({
        interval: 60,
        format: "dd/MM/yyyy HH:mm",
        timeFormat: "HH:mm:ss",
        value: todayStart,
        change: function (e)
        {
            //prevent Kendo return null when date time no-change
            if (e.sender._value == null)
            {
                $("#" + fromDivId).data("kendoDateTimePicker").value(e.sender.options.value);
            }

            var startDate = start.value();
            if (startDate)
            {
                end.min(new Date(startDate));
            }
        }
    }).data("kendoDateTimePicker");

    var end = $("#" + toDivId).kendoDateTimePicker({
        interval: 60,
        format: "dd/MM/yyyy HH:mm",
        timeFormat: "HH:mm:ss",
        value: todayEnd,
        change: function (e)
        {
            //prevent Kendo return null when date time no-change
            if (e.sender._value == null)
            {
                $("#" + toDivId).data("kendoDateTimePicker").value(e.sender.options.value);
            }

            var endDate = end.value();
            if (endDate)
            {
                start.max(new Date(endDate));
            }
        }
    }).data("kendoDateTimePicker");

    start.max(end.value());
    end.min(start.value());
    end.max(todayEnd);
}

utils.checkInternetAccess = function (onlineCallback, offlineCallback)
{
    var img = document.createElement("img");
    img.onload = onlineCallback;
    img.onerror = offlineCallback;
    img.src = "http://pic2.pbsrc.com/navigation/brand-logo.png?v=" + Math.random();
}

utils.getRandomInteger = function (min, max)
{
    return Math.floor((Math.random() * max) + min);
}

utils.getRandomDecimal = function (min, max)
{
    return (Math.random() * max) + min;
}

utils.getScript = function (src)
{
    document.write('<script type="text/javascript" src="' + src + '"></script>');
}

utils.captureEnterKey = function (divId, callback)
{

    $("#" + divId).bind('keypress', function (e)
    {
        var code = (e.keyCode ? e.keyCode : e.which);
        if (code == 13)
        { // ENTER keycode
            callback();
        }
    });
}

utils.httpGet = function (theUrl)
{
    var xmlHttp = null;
    xmlHttp = new XMLHttpRequest();
    xmlHttp.open("GET", theUrl, false);
    xmlHttp.send(null);
    return xmlHttp.responseText;
}

// width and height fields are optional, set null to auto-resize based on
// content
utils.openPopup = function (title, contentUrl, width, height, isModel, onPopupClosed)
{
    var winId = Math.random().toString(8).slice(2);
    $(document.body).append('<div id="' + winId + '" style="overflow: hidden"></div>');

    function onClosed(e)
    {
        $(".k-animation-container").css("display", "none");
        $(".k-list-container").css("display", "none");
        $(".pac-container").css("display", "none");
        $(".k-list-container").css("transform", "");
        $("#" + winId).html("");
        onPopupClosed(e);
    }

    var winOptions = {};
    winOptions.title = title;
    winOptions.content = "/" + kupBucket + contentUrl;
    console.info("winOptions.content::::"+winOptions.content)
    winOptions.resizable = false;
    winOptions.modal = isModel;
    winOptions.close = onClosed;
    winOptions.visible = false;

    if (!utils.isNullOrEmpty(width))
    {
        winOptions.width = width;
    }

    if (!utils.isNullOrEmpty(height))
    {
        winOptions.height = height;
    }

    var autoAdjust = utils.isNullOrEmpty(width) || utils.isNullOrEmpty(height);
    if (autoAdjust)
    {
        winOptions.refresh = function ()
        {
            this.center();
        }
    }

    var kendoWin = $("#" + winId).kendoWindow(winOptions).data("kendoWindow").center().open();
}

utils.openInsideIframe = function (frameId, title, contentUrl, width, height, onPopupClosed)
{
    $(document.body).append('<div id="' + frameId + '" style="overflow: hidden"></div>');
    var $popupWin = $("#" + frameId);

    function onClosed(e)
    {
        $popupWin.remove();
        onPopupClosed(e);
    }

    var winOptions = {};
    winOptions.title = title;
    winOptions.content = "/" + kupBucket + contentUrl;
    winOptions.resizable = false;
    winOptions.modal = true;
    winOptions.iframe = true;
    winOptions.width = width;
    winOptions.height = height;
    winOptions.close = onClosed;

    $popupWin.kendoWindow(winOptions).data("kendoWindow").center().open();
}

utils.formatDrmsDate = function (datetime)
{
    // datetime=parse_datetime(datetime);
    var dateObj = new Date(datetime);
    var hr = dateObj.getHours();
    var min = dateObj.getMinutes();
    if (hr < 10)
    {
        hr = "0" + hr;
    }
    if (min < 10)
    {
        min = "0" + min;
    }
    var dateStr = hr + ":" + min;
    return dateStr;
}

utils.formatDate = function (date)
{
    if (date != "")
    {
        var tempDate = date.split(" ");
        var tempTime = tempDate[3].split(":");
        if (tempDate[1] == 'Jan')
        {
            tempDate[1] = '01';
        }
        else if (tempDate[1] == 'Feb')
        {
            tempDate[1] = '02';
        }
        else if (tempDate[1] == 'Mar')
        {
            tempDate[1] = '03';
        }
        else if (tempDate[1] == 'Apr')
        {
            tempDate[1] = '04';
        }
        else if (tempDate[1] == 'May')
        {
            tempDate[1] = '05';
        }
        else if (tempDate[1] == 'Jun')
        {
            tempDate[1] = '06';
        }
        else if (tempDate[1] == 'Jul')
        {
            tempDate[1] = '07';
        }
        else if (tempDate[1] == 'Aug')
        {
            tempDate[1] = '08';
        }
        else if (tempDate[1] == 'Sep')
        {
            tempDate[1] = '09';
        }
        else if (tempDate[1] == 'Oct')
        {
            tempDate[1] = '10';
        }
        else if (tempDate[1] == 'Nov')
        {
            tempDate[1] = '11';
        }
        else if (tempDate[1] == 'Dec')
        {
            tempDate[1] = '12';
        }
        var formatedDate = tempDate[2] + tempDate[1] + tempDate[5] + tempTime[0] + tempTime[1] + tempTime[2];
        return formatedDate;
    }
    return "";
}
utils.convertDrmsDate = function (timeType, date, range)
{
    var tempDate = null;
    tempDate = date;
    if (range == "start")
    {
        if (timeType == "daily")
        {
            return tempDate;
        }
        else if (timeType == "weekly")
        {
            return (new Date(tempDate - (tempDate.getDay() * 86400000)));
        }
        else if (timeType == "monthly")
        {
            tempDate.setMonth(tempDate.getMonth(), 1);
            return tempDate;
        }
        else if (timeType == "yearly")
        {
            tempDate.setMonth(0, 1);
            return tempDate;
        }
    }
    else if (range == "end")
    {
        if (timeType == "daily")
        {
            tempDate.setHours(23, 59, 59);
            return tempDate;
        }
        else if (timeType == "weekly")
        {
            tempDate.setDate(tempDate.getDate() + (6 - tempDate.getDay()));
            tempDate.setHours(23, 59, 59);
            return tempDate;
        }
        else if (timeType == "monthly")
        {
            tempDate.setMonth(tempDate.getMonth() + 1, 0);
            return tempDate;
        }
        else if (timeType == "yearly")
        {
            tempDate.setMonth(11, 31);
            tempDate.setHours(23, 59, 59);
            return tempDate;
        }
    }
}

utils.populateChannels = function (channelCount, divId)
{
    var channelListId = "#" + divId;
    var channList = [];
    var dataSource = null;
    for (var i = 0; i < channelCount; i++)
    {
        var value = i + 1;
        channList.push({
            "name": value,
            "nodeCoreDeviceId": i
        });
    }
    if (channList.length > 0)
    {
        dataSource = new kendo.data.DataSource({
            data: channList
        });
        $(channelListId).data("kendoDropDownList").setDataSource(dataSource);
    }
    else
    {
        dataSource = new kendo.data.DataSource({
            data: []
        });
        $(channelListId).data("kendoDropDownList").setDataSource(dataSource);
        $(channelListId).data("kendoDropDownList").element.val("");
        $(channelListId).data("kendoDropDownList").text("");
    }
}

utils.populateNodeNames = function (nodeList, divId)
{
    var channelListId = "#" + divId;
    var dataSource = null;
    if (nodeList.length > 0)
    {
        dataSource = new kendo.data.DataSource({
            data: nodeList
        });
        $(channelListId).data("kendoDropDownList").setDataSource(dataSource);
    }
    else
    {
        dataSource = new kendo.data.DataSource({
            data: []
        });
        $(channelListId).data("kendoDropDownList").setDataSource(dataSource);
        $(channelListId).data("kendoDropDownList").element.val("");
        $(channelListId).data("kendoDropDownList").text("");
    }
}

utils.collapseAllRows = function (gridId)
{
    var grid = $('#' + gridId).data('kendoGrid');
    grid.tbody.find('>tr.k-grouping-row').each(function (e)
    {
        grid.collapseRow(this);
    });
}

utils.expandAllRows = function (gridId)
{
    var grid = $('#' + gridId).data('kendoGrid');
    grid.tbody.find('>tr.k-grouping-row').each(function (e)
    {
        grid.expandRow(this);
    });
}

utils.isNullOrEmpty = function (str)
{
    return str == null || str == "";
}

/**
 *
 * @param presetCompanyId
 *            optional, if specified, companyId input will be set and hidden
 *            from user
 * @param allowOTP
 *            (true/false) allow OTP login option
 * @param callback
 *            Upon login success, sessionKey will be passed in
 *            callback(sessionKey), empty string on cancellation
 */
utils.popupAuthentication = function (presetCompanyId, allowOTP, callback)
{

    var presetInfo = {
        "companyId": presetCompanyId,
        "allowOTP": allowOTP
    };
    var url = "/login/authenticate/" + JSON.stringify(presetInfo);

    utils.openPopup(localizeResource("authentication"), url, 335, null, true, function ()
    {
        callback(authWin.sessionKeyResult);
    });
}

utils.centerDivObject = function (jqDivObject)
{
    var winWidth = $(window).width();
    var winHeight = $(window).height();

    var divWidth = jqDivObject.width();
    var divHeight = jqDivObject.height();

    var top = (winHeight - divHeight) / 2;
    var left = (winWidth - divWidth) / 2;

    jqDivObject.css("position", "absolute");
    jqDivObject.css("top", top);
    jqDivObject.css("left", left);
}

utils.sameday = function (date1, date2)
{
    return date1.getUTCFullYear() == date2.getUTCFullYear() && date1.getUTCMonth() == date2.getUTCMonth() && date1.getUTCDate() == date2.getUTCDate();
}

utils.tryParseJson = function (str)
{
    try
    {
        var ret = JSON.parse(str);
        if (ret == "")
        {
            ret = {};
        }
        return ret;
    } catch (e)
    {
        return {};
    }
}

utils.getMapSize = function (map)
{
    return Object.keys(map).length;
};

utils.modulo = function (number, divisor)
{
    return number % divisor;
}

utils.checkFlashPlayer = function (browser)
{
    if (browser == 'ie')
    {
        try
        {
            new ActiveXObject("ShockwaveFlash.ShockwaveFlash");
        } catch (e)
        {
            return false;
        }
    }

    if (browser == 'firefox' || browser == 'chrome' || browser == 'opera')
    {
        var isExistFlashPlayer = navigator.plugins["Shockwave Flash"];
        if (!isExistFlashPlayer)
        {
            return false;
        }
    }

    return true;
}

utils.viewSnapshot = function (coreDeviceId, channelId)
{
    var contentPage = "/device/viewsnapshot/" + coreDeviceId + "-" + channelId;
    utils.openPopup(localizeResource("view-snapshot"), contentPage, null, null, true, function ()
    {
        utils.hideLoadingOverlay();
    });
}

utils.checkDeviceCompleteInfo = function (device)
{

    if (device.model.capabilities.indexOf("node") != -1 && device.node === undefined)
    { //Whether is a node checking in the kup cloud
        console.log(device.name + " doesnot have node properties");
        return false;
    }
    else if (device.model.capabilities.indexOf("node") === -1 && device.model.channels < 1)
    { //Whether is a camera checking in the node
        console.log(device.name + " doesnot have channel on it");
        return false;
    }
    return true;
}

utils.formatSerialNumber = function (serialNumber)
{
    var dashedNumber = "";
    var groupSize = 5;

    for (var i = 0; i < serialNumber.length; i++)
    {
        dashedNumber += serialNumber[i];
        if (serialNumber.length != (i + 1) && utils.modulo(i + 1, groupSize) == 0)
        {
            dashedNumber += " - ";
        }
    }

    return dashedNumber;
}

utils.removeArrayEntry = function (value, Array)
{
    var index = $.inArray(value, Array);
    if (~index)
    {
        Array.splice(index, 1);
    }
}

utils.isValidEmail = function isValidEmailAddress(emailAddress)
{
    var pattern = new RegExp(/^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i);
    return pattern.test(emailAddress);
};

utils.convertToCurrency = function (value)
{
    if (!isNaN(value))
    {
        var number = parseFloat(value);
        if (number >= 1000000000)
        {
            return (number / 1000000000) + "BN"
        }
        else if (number >= 1000000)
        {
            return (number / 100000) + "M"
        }
        else if (number >= 1000)
        {
            return (number / 1000) + "K"
        }
        else
        {
            return value;
        }
    }
    else
    {
        return value;
    }
}

utils.getLatLngByAddress = function (address, callback, onError)
{
    if(kupapi.mapSource == null || kupapi.mapSource != "baidu")
    {
        var geocodeAPI = 'https://maps.googleapis.com/maps/api/geocode/json?key=AIzaSyCv9ajgEy884HyIZKZkz-BtrXLQ_4XfDWU';
        var params = {
            "address": address
        }
        ajax(geocodeAPI, params, function (responseData)
        {
            if (responseData.status != "OK")
            {
                onError(responseData);
                return;
            }
            callback(responseData.results[0].geometry.location);
        }, onError, "GET");
    }
    else
    {
        reverseGeocode(address, function (responseData)
        {
            if (responseData.result != "ok")
            {
                onError(responseData);
                return;
            }
            var location = {
                lng: responseData.lng,
                lat: responseData.lat
            }
            callback(location);
        }, onError);
    }
}

utils.getDateDifference = function (date1, date2)
{
    // Convert both dates to milliseconds
    var date1Millis = date1.getTime();
    var date2Millis = date2.getTime();
    var differenceObject = {};
    // Calculate the difference in milliseconds
    var differenceMillis = (date2Millis > date1Millis) ? (date2Millis - date1Millis) : (date1Millis - date2Millis);
    //get units
    differenceObject.days = Math.floor((differenceMillis / (60 * 60 * 1000)) / 24);
    differenceObject.hours = Math.floor((differenceMillis / (60 * 60 * 1000)) % 24);
    differenceObject.minutes = Math.floor((differenceMillis / (60 * 1000)) % 60);
    differenceObject.seconds = Math.floor((differenceMillis / 1000) % 60);
    //object that contains all the date time units. 0 if doesnot exist
    return differenceObject;
}

utils.combineDeviceChannelIDs = function (deviceId, channelId)
{
    if (!utils.isNullOrEmpty(deviceId) && !utils.isNullOrEmpty(channelId))
    {
        return deviceId + "-" + channelId;
    }
}

utils.isValidDir = function (directoryString)
{
    var pattern = new RegExp(/^[0-9a-zA-Z\/\-_\s]+$/i);
    return pattern.test(directoryString);
}

/**
 * returns true if array2 contains all the value of array1
 * returns  false if array2 doesnot contains all the value of array1
 *
 */
utils.containsAll = function (array1, array2)
{
    for (var i = 0, len = array1.length; i < len; i++)
    {
        if ($.inArray(array1[i], array2) == -1)
        {
            return false;
        }
    }
    return true;
}

/**
 * returns true if array2 contains any one of the value of array1
 * returns  false if array2 doesnot contains all of the value of array1
 *
 */
utils.containsAny = function (array1, array2)
{
    for (var i = 0, len = array1.length; i < len; i++)
    {
        if ($.inArray(array1[i], array2) !== -1)
        {
            return true;
        }
    }
    return false;
}

utils.permutate = function (list, length)
{
    // Copy initial values as arrays
    var perm = list.map(function (val)
    {
        return [val];
    });
    // permutation generator
    var generate = function (perm, maxLen, currLen)
    {
        // Reached desired length
        if (currLen === maxLen)
        {
            return perm;
        }
        // For each existing permutation
        for (var i = 0, len = perm.length; i < len; i++)
        {
            var currPerm = perm.shift();
            // Create new permutation
            for (var k = 0; k < list.length; k++)
            {
                perm.push(currPerm.concat(list[k]));
            }
        }
        // Recurse
        return generate(perm, maxLen, currLen + 1);
    };

    // Start with size 1 because of initial values
    return generate(perm, length, 1);
}

utils.requestUserInput = function (inputName, callback)
{
    var saved = false;
    var inputText = "";
    var contentHtml = $(".input_request_win").html();

    var kendoWindow = $(".input_request_win").kendoWindow({
        visible: false,
        title: inputName,
        resizable: false,
        modal: true,
        width: 200,
        close: function ()
        {
            callback(saved, inputText);
        }
    });
    kendoWindow.data("kendoWindow").content(contentHtml).center().open();

    var $inputBox = $(".input_request_win input[name=userInput]");

    $(".input_request_win .btn_save").click(function ()
    {
        inputText = $inputBox.val();
        if (utils.isNullOrEmpty(inputText))
        {
            return;
        }

        saved = true;
        $(".input_request_win").data("kendoWindow").close();
    });

    $(".input_request_win .btn_cancel").click(function ()
    {
        saved = false;
        $(".input_request_win").data("kendoWindow").close();
    });
}

utils.isListEmpty = function (list)
{
    return list == null || list.length == 0;
};

/**
 * checks if the user is using another computer to access node UI
 *
 */
utils.browsingOnNode = function ()
{
    var hostname = window.location.hostname;
    return (hostname == "localhost" || hostname == "127.0.0.1");
}

utils.toAPITimestamp = function (utcDate)
{
    return kendo.toString(utcDate, kupapi.API_TIME_FORMAT);
};

utils.getLocalTimestamp = function (millis)
{
    var localDt = new Date(millis);
    return kendo.toString(localDt, kupapi.TIME_FORMAT);
};

utils.getUTCTimestamp = function (millis)
{
    var localDt = new Date(millis);
    var utcDt = utils.convertToUTC(localDt);
    return kendo.toString(utcDt, kupapi.TIME_FORMAT);
};

utils.replaceAll = function (str, find, replace)
{
    return str.replace(new RegExp(find, 'g'), replace);
}

utils.randomAlphanumeric = function (length)
{
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    for (var i = 0; i < length; i++)
    {
        text += possible.charAt(Math.floor(Math.random() * possible.length));
    }
    return text;
};

utils.copyToClipBoard = function (text)
{
    var $win = $(".text_copier");
    var $input = $win.find("input");
    var $btn = $win.find(".btn_copy");

    $input.val(text);

    $win.kendoWindow({
        visible: false,
        title: localizeResource("copy"),
        resizable: false,
        modal: true,
        close: function ()
        {
        }
    }).data("kendoWindow").center().open();

    $btn.on('click', function (event)
    {
        try
        {
            var copyTextArea = $input[0];
            copyTextArea.select();
            var successful = document.execCommand('copy');
        }
        catch (e)
        {
            console.error(e);
        }
        $win.data("kendoWindow").close();
    });
};

utils.padFixedLength = function (num, length, char)
{
    char = char || '0';
    num = num + '';
    return num.length >= length ? num : new Array(length - num.length + 1).join(char) + num;
}

utils.formatTimeZone = function (offsetMins)
{
    var sign = offsetMins < 0 ? "-" : "+";
    var offsetH = utils.padFixedLength(offsetMins / 60, 2);
    var offsetM = utils.padFixedLength(offsetMins % 60, 2);
    return sign + offsetH + ":" + offsetM;
};

utils.CIDRToNetmask = function (cidr)
{
    var mask = 0xffffffff << (32 - cidr);
    var arr = [
        mask >>> 24,
        mask >> 16 & 0xff,
        mask >> 8 & 0xff,
        mask & 0xff
    ];
    return arr.join(".");
}

utils.displayBase64Image = function (maskBase64)
{
    var dataUrl = "data:image/jpeg;base64," + maskBase64;
    var win = window.open();
    $(win.document.body).html('<img src="' + dataUrl + '" />');
};

utils.getCircularPolygon = function (center, r, N)
{
    var points = [];
    for (var n = 1; n <= N; n++)
    {
        points.push({
            x: r * Math.cos(2 * Math.PI * n / N) + center.x,
            y: r * Math.sin(2 * Math.PI * n / N) + center.y
        });
    }

    return points;
}

utils.bytesToMBString = function (bytes)
{
    return Math.round(bytes / (1024 * 1024)) + " MB";
};

utils.sanitizeForURL = function (str, replaceChar)
{
    return str.replace(/[^a-zA-Z0-9-_.]/g, replaceChar);
};

utils.flashSupported = function (popupInstallLink)
{
    if (FlashDetect.installed)
    {
        return true;
    }

    if (popupInstallLink)
    {
        $(".install_flash_warning").kendoWindow({
            visible: false,
            title: localizeResource("system-message"),
            resizable: false,
            modal: true,
            width: 350,
            close: function ()
            {
            }
        }).data("kendoWindow").center().open();
    }

    return false;
};

utils.disableWithOverlay = function ($targetElement, disable)
{
    var leftBarActive = mainJS.isLeftBarActive();
    var position = $targetElement.position();
    var $header = $("#header");

    var $overlay = $(".ele_busy_overlay");

    $overlay.css({
        position: 'absolute',
        "z-index": 1000,
        top: position.top + $header.outerHeight(),
        left: position.left + (leftBarActive ? 250 : 0),
        width: $targetElement.outerWidth(),
        height: $targetElement.outerHeight(),
        background: "#333",
        opacity: 0.5
    });

    $overlay.toggle(disable);
};

utils.equalIgnoreCase = function (s1, s2)
{
    if (s1 != null && s2 != null)
    {
        return s1.toLowerCase() === s2.toLowerCase();
    }

    return false;
};

utils.getTodayDateRange = function ()
{
    var now = new Date();
    return {
        from: new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0),
        to: new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 0, 0, -1, 0)
    }
};

utils.doNothing = function ()
{
};

utils.centerKendoWin = function ($innerContent)
{
    $innerContent.closest(".k-window-content").data("kendoWindow").center();
};

utils.viewEventDetails = function (eventId, notificationSource)
{
    if (utils.isNullOrEmpty(eventId))
    {
        return;
    }

    var contentPage = "/notification/details?eventId=" + eventId + "&source=" + notificationSource;
    utils.openPopup(localizeResource('alert-details'), contentPage, null, null, true, utils.doNothing);
};

utils.enableDiv = function ($div, enabled)
{
    //input and buttons
    $div.find("a").toggle(enabled);
    $div.find("input").prop('disabled', !enabled);
};

utils.generateHslColorRange = function (startHue, endHue, steps)
{
    var colorList = [];
    for (var i = 1; i <= steps; i++)
    {
        var hue = ((i / steps) * (endHue - startHue)) + startHue;
        colorList.push('hsl(' + hue + ', 100%, 50%)')
    }
    return colorList;
};

utils.getPerfectlyRoundedPercents = function (values, decimalCount)
{
    var total = 0;
    $.each(values, function (i, value)
    {
        total += value;
    });

    var percentList = [];
    var percentTotal = 0;
    $.each(values, function (i, value)
    {
        var percent = total === 0 ? 0 : (value * 100 / total);
        var rounded = +percent.toFixed(decimalCount);
        percentList.push(rounded);
        percentTotal += rounded;
    });

    //make sure the percents add up to 100
    var roundingDiff = +((percentTotal - 100).toFixed(decimalCount));
    if (roundingDiff !== 0)
    {
        console.log("Rounding diff:", roundingDiff);
        $.each(percentList, function (i, percent)
        {
            var adjustedPercent = percent - roundingDiff;
            if (arguments[i] > 0 && adjustedPercent > 0)
            {
                console.log("Adjusting", percent, "to", adjustedPercent);
                percentList[i] = adjustedPercent;
                return false;
            }
        });
    }

    return percentList;
};

utils.coalesce = function (callable, defaultVal)
{
    try
    {
        return callable();
    } catch (e)
    {
        console.warn(e)
        return defaultVal || "";
    }
};