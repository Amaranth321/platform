/**
 * @author Aye Maung
 */
var BktNotiSetts = (function ()
{
    var bucket = null;
    var loadingFn = null;
    var currentSettings = {};

    var init = function (targetbucket, parentLoadingFn)
    {
        bucket = targetbucket;
        loadingFn = parentLoadingFn;
        generate();
    }

    var generate = function ()
    {
        loadingFn(true);
        getBucketNotificationSettings(bucket.id, function (responseData)
        {
            currentSettings = responseData.settings;
            var isBktUser = utils.equalIgnoreCase(bucket.name, kupBucket);

            populateGrid();
            setEditStatus(!isBktUser);
            loadingFn(false);
        }, null);
    };

    var saveChanges = function ()
    {
        var modifiedSettings = {};
        var totalCalls = 0;

        loadingFn(true);
        $.each(currentSettings, function (evtType, setts)
        {
            var notiChecked = $(".noti_setts").find("input[name=" + evtType + "_notification]").is(':checked');
            var vidChecked = $(".noti_setts").find("input[name=" + evtType + "_video]").is(':checked');

            //no changes
            if (setts.notificationEnabled === notiChecked &&
                setts.videoRequired === vidChecked)
            {
                return true;
            }

            modifiedSettings[evtType] = {
                notificationEnabled: notiChecked,
                videoRequired: vidChecked
            };
            totalCalls++;
        });

        if (totalCalls == 0)
        {
            loadingFn(false);
            return;
        }

        var completedCalls = 0;
        $.each(modifiedSettings, function (evtType, setts)
        {
            updateBucketNotificationSettings(
                bucket.id,
                evtType,
                setts.notificationEnabled,
                setts.videoRequired,
                function (responseData)
                {
                    completedCalls++;
                    if (totalCalls === completedCalls)
                    {
                        utils.slideDownInfo(localizeResource("update-successful"));
                        loadingFn(false);
                        generate();
                    }
                }, null);
        });
    };

    var restoreDefaults = function ()
    {
        utils.popupConfirm(
            localizeResource("confirmation"),
            localizeResource("msg-confirm-restore-defaults"),
            function (choice)
            {
                if (!choice)
                {
                    return;
                }

                loadingFn(true);
                restoreBucketNotificationSettings(bucket.id, function (responseData)
                {
                    utils.slideDownInfo(localizeResource("update-successful"));
                    generate();
                    loadingFn(false);
                });
            });
    };

    var populateGrid = function ()
    {
        var $gridBox = $(".noti_setts .grid .body");
        $gridBox.html("");
        $.each(currentSettings, function (evtType, setts)
        {
            $gridBox.append(generateRow(evtType, setts));
        });
    };

    var generateRow = function (eventType, settings)
    {
        var settingData = {
            eventType: eventType,
            notificationEnabled: settings.notificationEnabled,
            videoRequired: settings.videoRequired
        };

        var template = kendo.template($("#eventSettingsRowTmpl").html());
        return template(settingData);
    };

    var setEditStatus = function (editable)
    {
        var $allBtns = $(".noti_setts .k-button");
        var $allChecks = $(".noti_setts input[type=checkbox]");
        if (editable)
        {
            $allBtns.show();
            $allChecks.removeAttr("disabled");
        }
        else
        {
            $allBtns.hide();
            $allChecks.attr("disabled", true);
        }
    };

    return {
        init: init,
        saveChanges: saveChanges,
        restoreDefaults: restoreDefaults
    }
})();