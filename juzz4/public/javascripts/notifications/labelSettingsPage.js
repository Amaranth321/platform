function LabelSettingsPage()
{
    var $settsMain = $(".label_setts");

    var tabs = {
        occSetts: null
    };

    var kElements = {
        tabstrip: null
    };

    var generate = function ()
    {
        loading(true);
        DvcMgr.ready(function ()
        {
            LabelMgr.ready(function ()
            {
                initTapstrip();
                loading(false);
            });
        });
    };

    var initTapstrip = function ()
    {
        var $tabstrip = $settsMain.find(".tab_strip");
        kElements.tabstrip = $tabstrip.kendoTabStrip({
            animation: {
                open: {
                    effects: "fadeIn"
                }
            },
            change: function (e)
            {
                var selectedTab = kElements.tabstrip.select();
                switch ($(selectedTab).index())
                {
                    case 0:
                        if (tabs.occSetts == null)
                        {
                            tabs.occSetts = new OccupancySettings();
                            tabs.occSetts.generate();
                        }
                        break;
                }
            }
        }).data("kendoTabStrip");
        $tabstrip.show();

        //auto-select the first tab
        kElements.tabstrip.select(0);
    };

    var loading = function (loading)
    {
        kendo.ui.progress($settsMain, loading);
    };

    return {
        generate: generate,
        tabs: tabs
    }
}
