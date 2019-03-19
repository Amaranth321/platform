/**
 * @author Aye Maung
 */
function ServerConfigs()
{
    var $main = $(".server_configs");

    var current = {
        dbConfigs: null
    };

    var generate = function ()
    {
        retrieveSettings(function ()
        {
            var template = kendo.template($("#serverConfigsTmpl").html());

            var $configBody = $main.find(".body");
            $configBody.html("");
            $configBody.append(template(current.dbConfigs));
        });
    };

    var applyChanges = function ()
    {
        //new configs
        var newConfigs = {};
        var modifiedList = [];
        $.each(current.dbConfigs, function (category, configs)
        {
            newConfigs[category] = {};
            var $category = $main.find("." + category);
            $.each(configs, function (key, oldVal)
            {
                var newVal;
                var $conf = $category.find("." + key);
                if ($conf.is(":checkbox"))
                {
                    newVal = $conf.is(":checked");
                }
                else if ($conf.is("input[type=number]"))
                {
                    newVal = parseInt($conf.val());
                }
                else if ($conf.is("input[type=text]") || $conf.is("input[type=password]"))
                {
                    newVal = $conf.val();
                }
                else
                {
                    console.error("Unknown type for ", key);
                    return true;
                }

                newConfigs[category][key] = newVal;

                if (newVal != oldVal)
                {
                    modifiedList.push({
                        category: category,
                        key: key,
                        oldVal: oldVal,
                        newVal: newVal
                    });
                }
            });
        });

        if (modifiedList.length < 1)
        {
            utils.popupAlert(localizeResource("no-config-changes"));
            return;
        }

        //log changes
        modifiedList.forEach(function (entry)
        {
            var displayStr = "[" + entry.category + "] " + entry.key + " " + entry.oldVal + " => " + entry.newVal;
            console.log(displayStr);
        });

        //confirm
        utils.popupConfirm(localizeResource("confirmation"), localizeResource("confirm-apply-server-configs"),
            function (proceed)
            {
                if (!proceed)
                {
                    return;
                }

                loading(true);
                updateServerConfigurations(newConfigs, function (responseData)
                {
                    loading(false);
                    if (responseData.result != "ok")
                    {
                        utils.throwServerError(responseData);
                        return;
                    }

                    utils.slideDownInfo(localizeResource("update-successful"));
                    generate();
                });
            }
        );
    };

    var clearChanges = function ()
    {
        generate();
    };

    var retrieveSettings = function (ready)
    {
        loading(true);
        getServerConfigurations(function (responseData)
        {
            loading(false);
            current.dbConfigs = responseData.configurations;
            ready();
        });
    };

    var loading = function (loading)
    {
        kendo.ui.progress($main, loading);
    };

    return {
        generate: generate,
        applyChanges: applyChanges,
        clearChanges: clearChanges
    };
}
