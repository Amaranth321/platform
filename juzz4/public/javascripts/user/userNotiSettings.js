/**
 * @author Aye Maung
 */
var UserNotiSetts = (function ()
{
    var allowedMethods = [];
    var currentSettings = {};

    var generate = function ()
    {
        loading(true);
        getAllowedNotifyMethods(function (responseData)
        {
            allowedMethods = responseData.methods;

            getUserNotificationSettings(function (responseData)
            {
                loading(false);
                currentSettings = responseData.settings;
                populateGrid();
            });
        });
    };

    var saveChanges = function ()
    {
        loading(true);
        var modifiedSettings = {};
        var totalCalls = 0;

        var $box = $(".noti-setts .body");
        $.each(currentSettings, function (evtType, origMethods)
        {
            var evtMethods = [];
            $.each(allowedMethods, function (i, method)
            {
                var $chb = $box.find("span[name=" + evtType + "] input[name=" + method + "]");
                if ($chb.is(":checked"))
                {
                    evtMethods.push(method);
                }
            });

            if (_.isEqual(origMethods.sort(), evtMethods.sort()))
            {
                return true;
            }

            modifiedSettings[evtType] = evtMethods;
            totalCalls++;
        });

        if (totalCalls == 0)
        {
            loading(false);
            return;
        }

        var completedCalls = 0;
        $.each(modifiedSettings, function (evtType, methods)
        {
            updateUserNotificationSettings(evtType, methods, function (responseData)
            {
                completedCalls++;
                if (completedCalls === totalCalls)
                {
                    loading(false);
                    utils.slideDownInfo(localizeResource("update-successful"));
                    generate();
                }
            });
        });
    };

    var populateGrid = function ()
    {
        var $box = $(".noti-setts .body");
        $box.html("");

        $.each(currentSettings, function (evtType, methods)
        {
            //generate html
            $box.append(generateRowHtml(evtType));

            //check if enabled
            $.each(methods, function (i, method)
            {
                var $chb = $box.find("span[name=" + evtType + "] input[name=" + method + "]");
                $chb.prop("checked", true);
            });
        });

        //hide method if not allowed
        var checkGroups = $box.find(".check_list label");
        $.each(checkGroups, function (i, lb)
        {
            var $lb = $(lb);
            if (allowedMethods.indexOf($lb.attr("name")) == -1)
            {
                $lb.hide();
            }
        });
    };

    var generateRowHtml = function (eventType)
    {
        var tmplData = {
            eventType: eventType
        };

        var template = kendo.template($("#notiSettingsRowTmpl").html());
        return template(tmplData);
    };

    var loading = function (loading)
    {
        kendo.ui.progress($(".noti-setts"), loading);
    };

    return {
        generate: generate,
        saveChanges: saveChanges
    }
})();
