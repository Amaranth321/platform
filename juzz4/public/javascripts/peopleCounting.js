var peopleCounting = window.peopleCounting || {};

peopleCounting.areaCount = 0;
peopleCounting.polygonStyle = null;
peopleCounting.colors = [];

var area1 = null;
var area2 = null;

peopleCounting.initDrawingCanvas = function (canvasId, isSecurity) {
    mapManager.initialize(canvasId, 20, false, false);
    mapManager.map.doubleClickZoom.disable();
    mapManager.map.scrollWheelZoom.disable();
    peopleCounting.colors = isSecurity? ["#CD1625", "#CD1625"] : ["#00A0B1", "#DC572E"];

    var polygonOptions = {
        title: localizeResource('draw'),
        allowIntersection: false,
        shapeOptions: {
            stroke: true,
            color: '#ddd',
            opacity: 0.9,
            weight: 1,
            fillColor: peopleCounting.colors[0],
            fillOpacity: 0.5,
            clickable: false
        }
    }

    peopleCounting.polygonStyle = polygonOptions.shapeOptions;
    mapManager.initializeDrawingTools(false, false, polygonOptions, false, false, true);
    mapManager.setEmptyBackground();
    peopleCounting._initDrawingEvents();

}

peopleCounting._initDrawingEvents = function () {
    area1 = null;
    area2 = null;

    mapManager.map.on('draw:created', function (e) {
        if (peopleCounting.areaCount == 2) {
            return;
        }

        var layer = e.layer;
        if (area1 == null) {
            layer.name = "R1";
            layer.setStyle({"fillColor" : peopleCounting.colors[0]});
            area1 = layer;
        }
        else {
            layer.name = "R2";
            layer.setStyle({"fillColor" : peopleCounting.colors[1]});
            area2 = layer;
        }

        layer.label = mapManager.addLabel(layer.name, mapManager.getEstimatedCenter(layer), 'polygon_label');
        mapManager.drawnItems.addLayer(layer);
        peopleCounting.areaCount++;
    });

    mapManager.map.on('draw:edited', function (e) {
        var layers = e.layers;
    });

    mapManager.map.on('draw:deleted', function (e) {
        var layers = e.layers;

        layers.eachLayer(function (layer) {
            if (layer.name == "R1") {
                area1 = null;
                peopleCounting.areaCount--;
            }
            else if (layer.name == "R2") {
                area2 = null;
                peopleCounting.areaCount--;
            }
        });
    });

    mapManager.map.on('draw:drawstart', function (e) {
        if (peopleCounting.areaCount == 2) {
            utils.popupAlert(localizeResource("msg-people-counting-region-limit"));
        }
    });

    mapManager.map.on('draw:drawstop', function (e) {
        mapManager.map.dragging.disable();
    });

}

peopleCounting.getRegionsDrawn = function () {
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

peopleCounting.addExistingRegions = function (areas) {
    $.each(areas, function (index, area) {
        var latLngPoints = [];
        $.each(area.points, function (f, p) {
            //change points to domain in map dimensions
            p.x = p.x * mapManager.mapWidth;
            p.y = p.y * mapManager.mapHeight;

            var latLng = mapManager.map.containerPointToLatLng(new L.Point(p.x, p.y));
            latLngPoints.push(latLng);
        });

        var polygon = L.polygon(latLngPoints, peopleCounting.polygonStyle);
        var layer = polygon.addTo(mapManager.map);
        layer.name = area.name;
        layer.setStyle({"fillColor" : peopleCounting.colors[index % 2]});
        layer.label = mapManager.addLabel(area.name, mapManager.getEstimatedCenter(layer), 'polygon_label');

        mapManager.drawnItems.addLayer(layer);
        peopleCounting.areaCount++;
    });
}

//to be called by mapManager only
peopleCounting.mapLayersRemoved = function () {
    area1 = null;
    area2 = null;
    peopleCounting.areaCount = 0;
}
