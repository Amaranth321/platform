var sketchManager = {};
sketchManager.drawingFrame = null;

sketchManager.options = {
    color: "red",
    width: 15,
    opacity: 50
}

sketchManager.initDrawingFrame = function (divId, onLoaded) {
    $("#" + divId).html(kendo.template($("#sketchingToolTmpl").html()));
    sketchManager.drawingFrame = window.frames['drawingArea'];
    utils.loadIframe("drawingArea", "/" + kupBucket + "/vca/drawmask");
    $("#drawingArea").load(onLoaded);
}

sketchManager.initSketchTool = function (initialPath, options) {
    sketchManager.options = options;
    sketchManager.drawingFrame.initSketchTool(initialPath, options);
}

sketchManager.updateBackgroundImage = function (coreDeviceId, channelId) {
    sketchManager.drawingFrame.updateBackgroundImage(coreDeviceId, channelId);
}

sketchManager.setEmptyBackground = function () {
    sketchManager.drawingFrame.setEmptyBackground();
}

sketchManager.getBase64MaskImage = function () {
    return sketchManager.drawingFrame.getMaskImage().replace("data:image/jpeg;base64,", "");
}

sketchManager.getMaskPath = function () {
    return sketchManager.drawingFrame.getMaskPath();
}

sketchManager.enableDrawingTools = function (isEnabled) {
    sketchManager.drawingFrame.enableDrawingTools(isEnabled);
}

sketchManager.bgImageExists = function () {
    return sketchManager.drawingFrame.bgImageExists();
}
