var tick = 100; // milliseconds
var stepSize = 30; //meters
var followVehicleFlag = false;
var timeoutHandler = null;
var totalDistance = 0;
var currentDistance = 0;
var startLocation = null;
var endLocation = null;
var sliderControl = null;
var isPlaying = false;

var movingCarMarker = new google.maps.Marker({
    icon: mapIcons["moving-vehicle"],
    zIndex: 3
});

var startMarker = new google.maps.Marker({
    icon: mapIcons["markerA"],
    zIndex: 1
});

var endMarker = new google.maps.Marker({
    icon: mapIcons["markerB"],
    zIndex: 1
});

function initPlaybackSettings() {

    totalDistance = mapManager.polyline.Distance();
    startLocation = mapManager.polyline.getPath().getAt(0);
    endLocation = mapManager.polyline.getPath().getAt(mapManager.polyline.getPath().getLength() - 1);

    startMarker.setPosition(startLocation);
    endMarker.setPosition(endLocation);
    mapManager.updateMarker(startMarker);
    mapManager.updateMarker(endMarker);

}

function resetPlaybackControls() {

    if (timeoutHandler != null)
        clearTimeout(timeoutHandler);

    isPlaying = false;
    totalDistance = 0;
    currentDistance = 0;
}

function startAnimation() {
    var startDistance = currentDistance + stepSize;
    timeoutHandler = setTimeout("animateMoving(" + startDistance + ")", 100);
    isPlaying = true;
}

function pauseAnimation() {
    clearTimeout(timeoutHandler);
    isPlaying = false;
}

function stopAnimation() {

    clearTimeout(timeoutHandler);
    isPlaying = false;
    currentDistance = 0;
    sliderControl.value(0);
    movingCarMarker.setPosition(startLocation);
    mapManager.updateMarker(movingCarMarker);
}

function animateMoving(d) {

    if (moveVehicleMarker(d))
        return;

    var adjustedStep = 30 + (15 - mapManager.map.getZoom()) * 10;
    stepSize = adjustedStep < 10 ? 5 : adjustedStep;
    timeoutHandler = setTimeout("animateMoving(" + (d + stepSize) + ")", tick);
}

function moveVehicleMarker(distanceFromStart) {

    var destReached = false;
    var nextLoc = null;

    if (distanceFromStart > totalDistance) {
        nextLoc = endLocation;
        currentDistance = totalDistance;
        destReached = true;

        $("#btnPlay").html('<img src="' + kupapi.CdnPath + '/common/images/play2.png" class="btn_img">');
        isPlaying = false;
    }
    else {
        nextLoc = mapManager.polyline.GetPointAtDistance(distanceFromStart);
        currentDistance = distanceFromStart;
    }

    movingCarMarker.setPosition(nextLoc);
    mapManager.updateMarker(movingCarMarker);
    sliderControl.value(currentDistance);

    if (followVehicleFlag) {
        mapManager.map.panTo(nextLoc);
    }

    return destReached;
}