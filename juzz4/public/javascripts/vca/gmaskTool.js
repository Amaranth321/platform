var gMaskTool = {
    iframeId: "gMaskDrawingFrame",
    drawingArea: null,
    snapshotUrl: null,
    currentRegions: [],
    saveChanges: false
}

gMaskTool.init = function (snapshotUrl, currentRegions) {
    gMaskTool.currentRegions = currentRegions;
    gMaskTool.snapshotUrl = snapshotUrl;
};

gMaskTool.prepareDrawingArea = function (onLoaded) {
    utils.loadIframe(gMaskTool.iframeId, "/" + kupBucket + "/vca/gMaskFrame");
    $("#" + gMaskTool.iframeId).load(function () {
        gMaskTool.drawingArea = window.frames[gMaskTool.iframeId].gMaskArea;
        onLoaded();
    });
}

gMaskTool.save = function () {
    gMaskTool.currentRegions = gMaskTool.drawingArea.getRegions();
    gMaskTool.saveChanges = true;
    gMaskTool._closePopup();
}

gMaskTool.cancel = function () {
    gMaskTool.saveChanges = false;
    gMaskTool._closePopup();
}

gMaskTool._closePopup = function () {
    $("#" + gMaskTool.iframeId).closest(".k-window-content").data("kendoWindow").close();
}

gMaskTool.loading = function (loading) {
    kendo.ui.progress($(".g_mask"), loading);
};