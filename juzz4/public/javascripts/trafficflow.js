var trafficflow = window.trafficflow || {};

trafficflow.areaLimit = 10;
trafficflow.START_NUMBER = 1;
trafficflow.polygonStyle = null;
trafficflow.$regionList = null;

trafficflow.initDrawingCanvas = function (canvasId, regionListDivId) {
    trafficflow.$regionList = $("#" + regionListDivId);

    mapManager.initialize(canvasId, 20, false, false);
    mapManager.map.doubleClickZoom.disable();
    mapManager.map.scrollWheelZoom.disable();

    var polygonOptions = {
        title: localizeResource('draw'),
        allowIntersection: false,
        shapeOptions: {
            stroke: true,
            color: '#ddd',
            opacity: 0.7,
            weight: 1,
            'fillColor': '#F6AE40',
            'fillOpacity': 0.5,
            clickable: false
        }
    }

    trafficflow.polygonStyle = polygonOptions.shapeOptions;
    mapManager.initializeDrawingTools(false, false, polygonOptions, false, false, true);
    mapManager.setEmptyBackground();
    trafficflow._initDrawingEvents();

}

trafficflow._initDrawingEvents = function () {

    mapManager.map.on('draw:created', function (e) {

        var boxArray = trafficflow.$regionList.data("kendoDropDownList").dataSource.data();
        if (boxArray.length == trafficflow.areaLimit) {
            return;
        }

        var layer = e.layer;
        layer.name = "R" + (boxArray.length + trafficflow.START_NUMBER);
        layer.label = mapManager.addLabel(layer.name, mapManager.getEstimatedCenter(layer), 'polygon_label');
        mapManager.drawnItems.addLayer(layer);

        trafficflow.$regionList.data("kendoDropDownList").dataSource.read();
    });

    mapManager.map.on('draw:edited', function (e) {
        var layers = e.layers;
        trafficflow.$regionList.data("kendoDropDownList").dataSource.read();
    });

    mapManager.map.on('draw:deleted', function (e) {
        var layers = e.layers;
        trafficflow.$regionList.data("kendoDropDownList").dataSource.read();
    });

    mapManager.map.on('draw:drawstart', function (e) {
        var boxArray = trafficflow.$regionList.data("kendoDropDownList").dataSource.data();
        if (boxArray.length == trafficflow.areaLimit) {
            utils.popupAlert(localizeResource("msg-traffic-flow-region-limit"));
        }
    });

    mapManager.map.on('draw:drawstop', function (e) {
        mapManager.map.dragging.disable();
    });

}

//to send back to backend
trafficflow.getBoxesDrawn = function () {
    var boxes = [];

    mapManager.drawnItems.eachLayer(function (layer) {
        var polygonPoints = [];
        $.each(layer._originalPoints, function (index, point) {
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

trafficflow.addExistingAreas = function (areas, source) {

    if (areas == undefined || source == undefined) {
        return;
    }

    $.each(areas, function (index, area) {
        var latLngPoints = [];
        $.each(area.points, function (f, p) {
            //change points to domain in map dimensions
            p.x = p.x * mapManager.mapWidth;
            p.y = p.y * mapManager.mapHeight;

            var latLng = mapManager.map.containerPointToLatLng(new L.Point(p.x, p.y));
            latLngPoints.push(latLng);
        });

        var polygon = L.polygon(latLngPoints, trafficflow.polygonStyle);
        var layer = polygon.addTo(mapManager.map);
        layer.name = area.name;
        layer.label = mapManager.addLabel(area.name, mapManager.getEstimatedCenter(layer), 'polygon_label');

        mapManager.drawnItems.addLayer(layer);
    });
}

//to populate primary box dropdown
trafficflow.getRectangles = function () {

    if (mapManager.drawnItems == null)
        return [];

    var areaList = [];
    mapManager.drawnItems.eachLayer(function (layer) {
        areaList.push(layer.name);
    });

    return areaList;
}

//to be called by mapManager only
trafficflow.mapLayersRemoved = function () {
    try {
        trafficflow.$regionList.data("kendoDropDownList").dataSource.read();
    } catch (e) {
        console.log(e);
    }
}