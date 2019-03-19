var mapManager = window.mapManager || {};
mapManager.map = null;
mapManager.markerClusterer = null;
mapManager.markerLayers = {};
mapManager.markerTypes = ["poi", "devices", "vehicles", "camera", "idle", "acceleration", "braking", "left", "right", "up", "down"];    // use index to specify type, string name is for reference only
mapManager.infoBox = null;
mapManager.userDevices = [];
mapManager.userVehicles = [];
mapManager.devicesOnMap = [];
mapManager.vehiclesOnMap = [];
mapManager.eventsOnMap = [];
mapManager.polyline = null;
mapManager.drawingManager = null;
mapManager.saveMarker = null;
infoBoxStatus = false;
mapManager.infoMarkerList = [];
mapManager.infoContentList = [];


mapManager.initialize = function (mapDivId, centerLat, centerLng, zLvl, panControl, streetViewControl, zoomControl, callback) {

    var defaultOptions = {
        zoom: zLvl,
        center: new google.maps.LatLng(centerLat, centerLng),
        mapTypeId: google.maps.MapTypeId.ROADMAP,
        mapTypeControl: false,
        mapTypeControlOptions: {
            style: google.maps.MapTypeControlStyle.DROPDOWN_MENU
        },

        panControl: panControl,
        streetViewControl: streetViewControl,
        zoomControl: zoomControl,
        zoomControlOptions: {
            position: google.maps.ControlPosition.TOP_LEFT
        }
    };
    mapManager.map = new google.maps.Map(document.getElementById(mapDivId), defaultOptions);

    //clustering
    var mcOptions = {gridSize: 50, maxZoom: 15, minimumClusterSize: 2};
    mapManager.markerClusterer = new MarkerClusterer(mapManager.map, [], mcOptions);
    mapManager.resetLayers();

    mapManager.infoBox = new InfoBox({
        disableAutoPan: false,
        maxWidth: 150,
        pixelOffset: new google.maps.Size(-140, 0),
        zIndex: null,
        boxStyle: {
            background: "url('http://google-maps-utility-library-v3.googlecode.com/svn/trunk/infobox/examples/tipbox.gif') no-repeat",
            opacity: 0.85,
            width: "289px"
        },
        closeBoxMargin: "12px 4px 2px 2px",
        closeBoxURL: "http://www.google.com/intl/en_us/mapfiles/close.gif",
        infoBoxClearance: new google.maps.Size(1, 1)
    });


    callback();
}
mapManager.resetLayers = function () {
    $.each(mapManager.markerTypes, function (index, item) {
        mapManager.markerLayers[index] = {};
    });

    mapManager.markerClusterer.clearMarkers();

    if (mapManager.polyline != null)
        mapManager.polyline.setMap(null);
}

mapManager.addMapEventListener = function (eventType, func) {
    switch (eventType) {
        case "click":
            google.maps.event.addListener(mapManager.map, "click", func);
            break;
        default:
            break;
    }
}

mapManager.addAutoCompleteSearchListener = function (inputFieldId) {
    var searchInput = document.getElementById(inputFieldId);
    var autocomplete = new google.maps.places.Autocomplete(searchInput);
    autocomplete.bindTo('bounds', mapManager.map);
    google.maps.event.addListener(autocomplete, 'place_changed', function () {
        var place = autocomplete.getPlace();
        if (place.geometry.viewport) {
            mapManager.map.fitBounds(place.geometry.viewport);
        } else {
            mapManager.map.setCenter(place.geometry.location);
            mapManager.map.setZoom(MAP_FOCUSED_ZOOM);
        }
    });
}

mapManager.contentString = function(marker, kendoInfoJson) {
	for (var i = 0; i<mapManager.infoMarkerList.length ; i++) {
		if(mapManager.infoMarkerList[i].getTitle()==marker.getTitle())
			return;
    }
	mapManager.infoMarkerList.push(marker);
	mapManager.infoContentList.push(kendoInfoJson);
}

mapManager.addNewMarker = function (id, type, lat, lng, imgUrl, infoJson) {

    var marker = new google.maps.Marker({
        position: new google.maps.LatLng(lat, lng),
        draggable: false,
        icon: imgUrl
    });

    var title = "";
    if (infoJson) {title = infoJson.name!= null?infoJson.name:infoJson.vehicleLicensePlate;}
    marker.setTitle(title);
    var kTemplate = kendo.template($("#infoBoxTemplate").html());
    google.maps.event.addListener(marker, 'click', function () {
        if (infoJson) {
            mapManager.openInfoBox(this, kTemplate(infoJson));
        }
    });
    if (infoJson) {mapManager.contentString(marker, kTemplate(infoJson));};
    mapManager.markerLayers[type][id] = marker;
    mapManager.markerClusterer.addMarker(marker);
    return marker;
}

infowindow = new google.maps.InfoWindow({
    'size': new google.maps.Size(100, 100)
});


mapManager.addNewEventMarker = function (id, type, lat, lng, imgUrl, vehInfo) {
    var marker = new google.maps.Marker({
        position: new google.maps.LatLng(lat, lng),
        draggable: false,
        icon: imgUrl
    });
    
    var title = "";
    if (vehInfo) {title = vehInfo.name!= null?vehInfo.name:vehInfo.vehicleLicensePlate;}
    marker.setTitle(title);
    var kTemplate = kendo.template($("#eventinfoBoxTemplate").html());
    google.maps.event.addListener(marker, 'click', function () {
        if (vehInfo) {            
            mapManager.openInfoBox(this, kTemplate(vehInfo));
        }
    });
    if (infoJson) {mapManager.contentString(marker, kTemplate(vehInfo));};
    mapManager.markerLayers[type][id] = marker;
    mapManager.markerClusterer.addMarker(marker);
    return marker;
}


mapManager.updateMarker = function (marker) {
    mapManager.markerClusterer.addMarker(marker);
}

mapManager.clearMarkersByType = function (type) {
    var toRemove = mapManager.markerLayers[type];
    $.each(toRemove, function (index, item) {
        mapManager.markerClusterer.removeMarker(item);
    });
    mapManager.markerLayers[type] = {};
}

mapManager.clearAllMarkers = function () {
    mapManager.markerClusterer.clearMarkers();
    mapManager.resetLayers();
}

mapManager.setVisibilityByType = function (type, isVisible) {

    if (type == 2) {
        mapManager.clearMarkersByType(2);
        if (isVisible)
            mapManager.loadVehicles();

        return;
    }

    var markers = mapManager.markerLayers[type];
    $.each(markers, function (index, item) {
        if (isVisible) {
            mapManager.markerClusterer.addMarker(item);
        }
        else {
            mapManager.markerClusterer.removeMarker(item);
        }
    });

}

mapManager.openInfoBox = function (marker, displayContent) {
    mapManager.infoBox.close();
    mapManager.infoBox.setContent(displayContent);
    mapManager.infoBox.open(mapManager.map, marker);
    mapManager.map.panTo(marker.getPosition());

}

mapManager.prepareUserMapItems = function () {
    
    mapManager.clearMarkersByType(0);
    getBucketPois("", function (responseData) {
        if (responseData.result == "ok" && responseData.pois != null) {
            $.each(responseData.pois, function (index, item) {
                mapManager.addNewMarker(item._id, 0, item.latitude, item.longitude, mapIcons[item.type], item);
            });
        }
    }, null);
    
    deviceManager.WaitForReady(function() {
        mapManager.clearMarkersByType(1);
        $.each(deviceManager.userDevices, function(index, dvc) {
            mapManager.addNewMarker(dvc.id, 1, dvc.latitude, dvc.longitude, mapIcons["camera"], dvc);
        });
    });
}



mapManager.drawPolyLine = function (strokeColor, strokeWeight, locations, callback) {

    //clear map
    mapManager.resetLayers();

    var latlngPoints = [];
    var bounds = new google.maps.LatLngBounds();

    //convert to google latlng objects
    //loc.timestamp is available for each point
    $.each(locations, function (index, loc) {
        var latlngPoint = new google.maps.LatLng(loc.latitude, loc.longitude);
        latlngPoints.push(latlngPoint);
        bounds.extend(latlngPoint);
    });

    mapManager.polyline = new google.maps.Polyline({
        path: latlngPoints,
        strokeColor: strokeColor,
        strokeWeight: strokeWeight
    });

    mapManager.map.fitBounds(bounds);
    setTimeout(function () {
        mapManager.polyline.setMap(mapManager.map);
        callback();
    }, 1000);
}