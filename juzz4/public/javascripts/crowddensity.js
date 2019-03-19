var crowddensity = window.crowddensity || {};

crowddensity.areaLimit = 10;
crowddensity.START_NUMBER = 1;
crowddensity.polygonStyle = null;
crowddensity.$regionList = null;
crowddensity.recentRegionName = null;
crowddensity.recentRegion = null;
crowddensity.regionNames = [];

crowddensity.initDrawingCanvas = function(canvasId, regionListDivId, options) {

if(!utils.isNullOrEmpty(regionListDivId))
    crowddensity.$regionList = $("#" + regionListDivId);

    mapManager.initialize(canvasId, 20, false, false);
    mapManager.map.doubleClickZoom.disable();
    mapManager.map.scrollWheelZoom.disable();
    var polygonOptions = options;
    if(options == null){
        polygonOptions = {
            stroke: true,
            color: '#ddd',
            opacity: 0.8,
            weight: 1,
            fillColor: '#2E8DEF',
            fillOpacity: 0.4,
            clickable: false
        };
    }
    var polygonOptions = {
        title: localizeResource('draw'),
        allowIntersection: false,
        shapeOptions: polygonOptions
    }

    crowddensity.polygonStyle = polygonOptions.shapeOptions;
    if(!utils.isNullOrEmpty(regionListDivId)) {
        mapManager.initializeDrawingTools(false, false, polygonOptions, false, false, true);
        mapManager.setEmptyBackground();
        crowddensity._initDrawingEvents();
    }else{
        mapManager.initializeDrawingTools(false, false, polygonOptions, false, false, false);
    }

}

crowddensity._initDrawingEvents = function() {

    mapManager.map.on('draw:created', function(e) {
        var boxArray = crowddensity.$regionList.data("kendoListView").dataSource.data();
        if (boxArray.length == crowddensity.areaLimit) {
            return;
        }
        var contentPage = "/vca/crowdregionname";
        var winTitle = localizeResource("region-name");
        var layer = e.layer;
        mapManager.drawnItems.addLayer(layer);
        mapManager.drawnItems.addLayer(layer);
        crowddensity.recentRegion = layer;
        utils.openPopup(winTitle, contentPage, null, null, true, function() {
            if (!utils.isNullOrEmpty(crowddensity.recentRegionName)) {
                var layer = e.layer;
                mapManager.drawnItems.addLayer(layer);
                mapManager.drawnItems.addLayer(layer);
                layer.name = crowddensity.recentRegionName;
                layer.label = mapManager.addLabel(layer.name, mapManager.getEstimatedCenter(layer), 'polygon_label');

                crowddensity.$regionList.data("kendoListView").dataSource.read();
            }else{
                console.log("Name not assigned for region");
                //keep previous region name, since not change.
                var regionNames = crowddensity.regionNames;
                mapManager.removeDrawnLayerByLayer(crowddensity.recentRegion);
                crowddensity.regionNames = regionNames;
            }
            crowddensity.recentRegion = null;

        });
    });

    mapManager.map.on('draw:edited', function(e) {
        var layers = e.layers;
        crowddensity.$regionList.data("kendoListView").dataSource.read();
    });

    mapManager.map.on('draw:drawstart', function(e) {
        var boxArray = crowddensity.$regionList.data("kendoListView").dataSource.data();
        if (boxArray.length == crowddensity.areaLimit) {
            utils.popupAlert(localizeResource("msg-crowd-flow-region-limit"));
        }
    });

    mapManager.map.on('draw:drawstop', function(e) {
        mapManager.map.dragging.disable();
    });

}

//to send back to backend
crowddensity.getBoxesDrawn = function() {
    var boxes = [];

    mapManager.drawnItems.eachLayer(function(layer) {
        var polygonPoints = [];
        $.each(layer._originalPoints, function(index, point) {
            //change points to [0,1] domain
            var nX = (point.x / mapManager.mapWidth);
            var nY = (point.y / mapManager.mapHeight);

            //Ensure the points are within the view
            nX = (nX < 0) ? 0 : nX;
            nX = (nX > 1) ? 1 : nX;
            nY = (nY < 0) ? 0 : nY;
            nY = (nY > 1) ? 1 : nY;

            polygonPoints.push({
                x: nX.toFixed(3),
                y: nY.toFixed(3)
            });
        });

        var box = {
            "name": layer.name,
            "points": polygonPoints
        };

        boxes.push(box);
    });

    return boxes;
}

crowddensity.addExistingAreas = function(areas) {

    if (areas == undefined) {
        return;
    }

    $.each(areas, function(index, area) {
        var latLngPoints = [];
        $.each(area.points, function(f, p) {
            //change points to domain in map dimensions
            p.x = p.x * mapManager.mapWidth;
            p.y = p.y * mapManager.mapHeight;

            var latLng = mapManager.map.containerPointToLatLng(new L.Point(p.x, p.y));
            latLngPoints.push(latLng);
        });

        var polygon = L.polygon(latLngPoints, crowddensity.polygonStyle);
        var layer = polygon.addTo(mapManager.map);
        layer.name = area.name;
        layer.label = mapManager.addLabel(area.name, mapManager.getEstimatedCenter(layer), 'polygon_label');

        mapManager.drawnItems.addLayer(layer);
    });
}

//to populate primary box dropdown
crowddensity.getRectangles = function() {

    if (mapManager.drawnItems == null)
        return [];

    var areaList = [];
    mapManager.drawnItems.eachLayer(function(layer) {
        var data = {
            "name": layer.name
        };
        areaList.push(data);
    });

    return areaList;
}

//to be called by mapManager only
crowddensity.mapLayersRemoved = function() {
    try {
        crowddensity.$regionList.data("kendoListView").dataSource.read();
        crowddensity.regionNames = [];
    } catch (e) {
        console.log(e);
    }
}

crowddensity.removeLayer = function(regionName) {
    //update region names
    var index = crowddensity.regionNames.indexOf(regionName);
    crowddensity.regionNames.splice(index, 1);

    //update canvas
    mapManager.removeDrawnLayerByLayerName(regionName);
}