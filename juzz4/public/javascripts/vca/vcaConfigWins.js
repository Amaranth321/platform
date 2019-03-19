/**
 * This is for keeping configured data from the popup windows.
 * Hence, data are temporary and usable only while the popup is active
 *
 */
var VcaConfigWins = (function ()
{
    var captureWin = {
        snapshotUrl: null,
        frameId: null,
        sizeBounds: null,

        openWin: function (snapshotUrl, currentData, onWinClosed)
        {
            captureWin.frameId = utils.randomAlphanumeric(10);
            captureWin.snapshotUrl = snapshotUrl;
            captureWin.sizeBounds = currentData;

            var winTitle = localizeResource("capture-size-range");
            var contentPage = "/vca/capturesizelimit";
            utils.openInsideIframe(captureWin.frameId, winTitle, contentPage, 540, 472, onWinClosed);
        },
        closeWin: function (captureSizeBounds)
        {
            captureWin.sizeBounds = captureSizeBounds;
            $("#" + captureWin.frameId).data("kendoWindow").close();
        },
        getSizeBounds: function ()
        {
            return captureWin.sizeBounds;
        },
        getSnapshot: function ()
        {
            return captureWin.snapshotUrl;
        }
    };

    var chooseProgramWin = {
        choice: null,

        openWin: function (onWinClosed)
        {
            var winTitle = localizeResource("choose-exe-program");
            var contentPage = "/vca/chooseprogram";
            utils.openPopup(winTitle, contentPage, null, null, true, function ()
            {
                onWinClosed(chooseProgramWin.choice);
                chooseProgramWin.choice = null;
            });
        },
        setChoice: function (choice)
        {
            chooseProgramWin.choice = choice;
        }
    };

    return {
        captureSize: {
            openWin: captureWin.openWin,
            closeWin: captureWin.closeWin,
            getSizeBounds: captureWin.getSizeBounds,
            getSnapshot: captureWin.getSnapshot
        },
        programChoice: {
            openWin: chooseProgramWin.openWin,
            setChoice: chooseProgramWin.setChoice
        }
    }
})();