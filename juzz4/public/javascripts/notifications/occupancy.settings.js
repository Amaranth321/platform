function OccupancySettings()
{
    var $occTabContainer;
    var userDeviceLabels = [];

    var configs = {
        defaultStartingLimit: 100,
        maxOccupancyLimitEntries: 5
    };

    var elements = {
        $chbEnabled: null,
        $limitsHolder: null,
        $intervalInput: null,
        $settingsContainer: null
    };

    var kLabelList;

    var generate = function ()
    {
        $occTabContainer = $(".occ_setts");
        adjustHeight();
        initElements();
    };

    var cancelChanges = function ()
    {
        var selectedLabel = kLabelList.dataItem();
        var origSettings = selectedLabel.occSettings;
        setEnabled(origSettings);
        loadLimits(origSettings);
        loadIntervalSettings(origSettings);
    };

    var saveChanges = function ()
    {
        var selectedLabel = kLabelList.dataItem();
        var isEnabled = elements.$chbEnabled.is(":checked");
        var occupancyLimits = [];
        var minNotifyInterval = 0;

        //only need to verify other info if notification is enabled
        if (isEnabled)
        {
            $.each(elements.$limitsHolder.children(), function (i, grp)
            {
                var $grp = $(grp);
                var groupId = $grp.attr("id").replace("occGrp", "");

                occupancyLimits.push({
                    limit: $grp.find("#occLimitVal" + groupId).data("kendoNumericTextBox").value(),
                    alertMessage: $grp.find("#occLimitMsg" + groupId).val()
                });
            });
            minNotifyInterval = elements.$intervalInput.val();

            if (occupancyLimits.length == 0)
            {
                utils.popupAlert(localizeResource("error-empty-occupancy-limits"));
                return;
            }
        }

        loading(true);
        updateLabelOccupancySettings(
            selectedLabel.labelId,
            isEnabled,
            occupancyLimits,
            minNotifyInterval,
            function (responseData)
            {
                loading(false);
                if (responseData.result == "ok")
                {
                    utils.slideDownInfo(localizeResource("update-successful"));
                    kLabelList.dataSource.read();
                }
                else
                {
                    utils.throwServerError(responseData);
                }
            });
    };

    var enableNotifications = function (enabled)
    {
        if (enabled)
        {
            elements.$settingsContainer.slideDown();
        }
        else
        {
            elements.$settingsContainer.slideUp();
        }
    };

    var adjustHeight = function ()
    {
        var offset = $occTabContainer.find(".header_box").height() + 204;
        $occTabContainer.height($(window).height() - offset);
    };

    var initElements = function ()
    {
        userDeviceLabels = LabelMgr.getUserAccessibleStoreLabels();
        if (userDeviceLabels.length == 0)
        {
            $occTabContainer.find(".error_msg").show();
            return;
        }

        //init jquery elements
        elements.$settingsContainer = $occTabContainer.find(".label_setts");
        elements.$chbEnabled = $occTabContainer.find(".chb_enable_occ");
        elements.$limitsHolder = $occTabContainer.find(".limits_holder");
        elements.$intervalInput = $occTabContainer.find(".min_interval");

        //label dropdown selection
        kLabelList = $occTabContainer.find(".label_list").kendoDropDownList({
            dataTextField: "name",
            dataValueField: "labelId",
            dataSource: {
                transport: {
                    read: function (options)
                    {
                        var totalReturned = 0;
                        loading(true);
                        $.each(userDeviceLabels, function (i, labelObj)
                        {
                            getLabelOccupancySettings(labelObj.labelId, function (responseData)
                            {
                                totalReturned++;
                                labelObj.occSettings = responseData.settings;

                                if (totalReturned == userDeviceLabels.length)
                                {
                                    loading(false);
                                    options.success(userDeviceLabels);
                                    loadCurrentSettings();
                                }
                            });
                        });
                    }
                }
            },
            template: $("#occDropDownEntryTmpl").html(),
            change: loadCurrentSettings
        }).data("kendoDropDownList");

        //show the container first
        $occTabContainer.find(".main_wrapper").show();
    };

    var loadCurrentSettings = function ()
    {
        var selectedLabel = kLabelList.dataItem();

        //update save button
        var $btnLabelText = $occTabContainer.find(".btn_group .label_name");
        $btnLabelText.html("&nbsp;(" + selectedLabel.name + ")");

        //load settings
        var occSettings = selectedLabel.occSettings;
        loadAssignedCameras(selectedLabel.labelId);
        setEnabled(occSettings);
        loadLimits(occSettings);
        loadIntervalSettings(occSettings);
    };

    var loadAssignedCameras = function (labelId)
    {
        var $assignees = $occTabContainer.find(".assignees");
        $assignees.empty();

        //find cameras assigned to this label
        var assignees = LabelMgr.getStoreLabelAssignees(labelId);
        var templateData = [];
        assignees.forEach(function (camera)
        {
            templateData.push({
                deviceName: DvcMgr.getDeviceName(camera.coreDeviceId),
                channelName: DvcMgr.getChannelName(camera.coreDeviceId, camera.channelId)
            });
        });

        var template = kendo.template($("#labelAssigneesTmpl").html());
        $assignees.html(template(templateData));
    };

    var setEnabled = function (labelSettings)
    {
        elements.$chbEnabled.prop('checked', labelSettings.enabled);
        enableNotifications(labelSettings.enabled);
    };

    var loadLimits = function (labelSettings)
    {
        elements.$limitsHolder.empty();
        $.each(labelSettings.limits, function (i, limitObj)
        {
            addLimitInput(limitObj.limit, limitObj.alertMessage);
        });
    };

    var loadIntervalSettings = function (labelSettings)
    {
        elements.$intervalInput.kendoNumericTextBox({
            format: "# " + localizeResource("seconds"),
            min: 1,
            step: 1,
            value: labelSettings.minNotifyIntervalSeconds
        });
    };

    var addLimitInput = function (currentValue, msg)
    {
        var currentCount = elements.$limitsHolder.children().length;
        if (currentCount >= configs.maxOccupancyLimitEntries)
        {
            utils.popupAlert(localizeResource("msg-occ-limit-count-reached", configs.maxOccupancyLimitEntries));
            return;
        }

        //params
        var groupId = utils.randomAlphanumeric(10);
        currentValue = currentValue || configs.defaultStartingLimit;
        msg = msg || "";

        //append html
        var template = kendo.template($("#occupancyLimitGroupTmpl").html());
        elements.$limitsHolder.append(template({groupId: groupId, msg: msg}));

        //init
        elements.$limitsHolder.find("#occLimitVal" + groupId).kendoNumericTextBox({
            format: "# " + localizeResource("people"),
            min: 1,
            step: 1,
            value: currentValue
        });
    };

    var removeLimitInput = function (groupId)
    {
        elements.$limitsHolder.find("#occGrp" + groupId).remove();
    };

    var loading = function (loading)
    {
        kendo.ui.progress($occTabContainer.find(".main_wrapper"), loading);
    };

    return {
        generate: generate,
        enableNotifications: enableNotifications,
        addLimitInput: addLimitInput,
        removeLimitInput: removeLimitInput,
        cancelChanges: cancelChanges,
        save: saveChanges
    }
}
