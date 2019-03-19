var mapManager = window.mapManager || {};
mapManager.map = null;
mapManager.mapWidth = null;
mapManager.mapHeight = null;
mapManager.backgroundImage = null;
mapManager.drawnItems = null;
mapManager.bgImageLoaded = false;

mapManager.initialize = function (divId, zoomLvl, zoomControl, canPan) {

    //default map is disabled
    mapManager.map = new L.Map(divId, {
        layers: [new L.TileLayer('', {maxZoom: 18})],
        center: new L.LatLng(0, 0),
        zoom: zoomLvl,
        zoomControl: zoomControl,
        dragging: canPan
    });

    mapManager.mapWidth = $("#" + divId).width();
    mapManager.mapHeight = $("#" + divId).height();
}

mapManager.addImageOverlay = function (imageUrl) {
    mapManager.removeImageOverlay();

    var bgImage = L.imageOverlay(imageUrl, mapManager.map.getBounds());
    mapManager.backgroundImage = bgImage;
    bgImage.addTo(mapManager.map);
    bgImage.bringToBack();
}

mapManager.setBackgroundImage = function (url) {
    if (url == null || url.length == 0) {
        return;
    }
    mapManager.addImageOverlay(url);
    mapManager.bgImageLoaded = true;
}

mapManager.setEmptyBackground = function () {
    mapManager.removeImageOverlay();
    var bgImage = L.imageOverlay("/public/css/common/images/empty_background.jpg", mapManager.map.getBounds());
    mapManager.backgroundImage = bgImage;
    bgImage.addTo(mapManager.map);
    bgImage.bringToBack();
}

mapManager.setLiveSnapshotBackground = function (coreDeviceId, channelId) {
    mapManager.bgImageLoaded = false;

    utils.showLoadingOverlay();
    DvcMgr.getCameraSnapshot(coreDeviceId, channelId, function (jpegUrl) {
        if (jpegUrl == null) {
            utils.popupAlert(localizeResource('error-loading-image'));
            mapManager.setEmptyBackground();
        } else {
            mapManager.setBackgroundImage(jpegUrl);
        }

        utils.hideLoadingOverlay();
    });
}

mapManager.removeImageOverlay = function () {
    if (mapManager.backgroundImage) {
        mapManager.map.removeLayer(mapManager.backgroundImage);
        mapManager.backgroundImage = null;
    }
}

mapManager.initializeDrawingTools = function (marker, polyline, polygon, circle, rectangle, showEditOptions) {

    mapManager.drawnItems = new L.FeatureGroup();
    mapManager.map.addLayer(mapManager.drawnItems);

    var editOptions = {
        featureGroup: mapManager.drawnItems,
        edit: {
            selectedPathOptions: {
                stroke: true,
                color: 'red',
                weight: 2,
                opacity: 0.8
            }
        }
    };
    if (showEditOptions) {
        var drawControl = new L.Control.Draw({
            position: 'topleft',
            draw: {
                marker: marker,
                polyline: polyline,
                polygon: polygon,
                circle: circle,
                rectangle: rectangle
            },
            //        edit: false
            edit: editOptions
        });

        mapManager.map.addControl(drawControl);
        mapManager.customizeDeleteFunction();
    }

//    utils.createTooltip("leaflet-draw-draw-polyline", "right", localizeResource('draw'));
//    utils.createTooltip("leaflet-draw-draw-rectangle", "right", localizeResource('draw'));
//    utils.createTooltip("leaflet-draw-edit-edit", "right", localizeResource('edit'));
//    utils.createTooltip("leaflet-draw-edit-remove", "right", localizeResource('delete'));
}

mapManager.removeDrawnItems = function () {
    if (mapManager.drawnItems != null) {
        mapManager.drawnItems.eachLayer(function (layer) {
            if (layer.arrows != null) {
                mapManager.map.removeLayer(layer.arrows);
            }
            if (layer.label != null) {
                mapManager.map.removeLayer(layer.label);
            }

            mapManager.map.removeLayer(layer);
            mapManager.drawnItems.removeLayer(layer);
        });
    }

    if (typeof peopleCounting !== 'undefined' && peopleCounting != null) {
        peopleCounting.mapLayersRemoved();
    }

    if (typeof passerbyCounting !== 'undefined' && passerbyCounting != null) {
        passerbyCounting.mapLayersRemoved();
    }

    if (typeof trafficflow !== 'undefined' && trafficflow != null) {
        trafficflow.mapLayersRemoved();
    }

    if (typeof crowddensity !== 'undefined' && crowddensity != null) {
        crowddensity.mapLayersRemoved();
    }
}

mapManager.removeDrawnLayerByLayerName = function (layerName) {
    mapManager.drawnItems.eachLayer(function (layer) {
        if (layer.name == layerName) {
            if (layer.arrows != null) {
                mapManager.map.removeLayer(layer.arrows);
            }
            if (layer.label != null) {
                mapManager.map.removeLayer(layer.label);
            }

            mapManager.map.removeLayer(layer);
            mapManager.drawnItems.removeLayer(layer);
        }
    });
    if (typeof crowddensity !== 'undefined' && crowddensity != null) {
        crowddensity.mapLayersRemoved();
    }
}

mapManager.removeDrawnLayerByLayer = function (removeLayer) {
    mapManager.drawnItems.eachLayer(function (layer) {
        if (layer == removeLayer) {
            if (layer.arrows != null) {
                mapManager.map.removeLayer(layer.arrows);
            }
            if (layer.label != null) {
                mapManager.map.removeLayer(layer.label);
            }

            mapManager.map.removeLayer(layer);
            mapManager.drawnItems.removeLayer(layer);
        }
    });
    if (typeof crowddensity !== 'undefined' && crowddensity != null) {
        crowddensity.mapLayersRemoved();
    }
}

mapManager.customizeDeleteFunction = function () {
    $(".leaflet-draw-edit-remove").hide();
    $(".leaflet-draw-edit-edit").attr("title", localizeResource('edit'));
    $(".leaflet-draw-edit-edit").parent().append(
            '<a class="leaflet-draw-edit-remove" ' +
            'href="javascript:mapManager.removeDrawnItems()" ' +
            'title="' + localizeResource('delete-all') + '">' +
            '</a>'
    );
}

mapManager.getEstimatedCenter = function (layer) {
    var bounds = layer.getBounds();
    var pSW = mapManager.map.latLngToContainerPoint(bounds.getSouthWest());
    var pNE = mapManager.map.latLngToContainerPoint(bounds.getNorthEast());

    var centX = ((pNE.x - pSW.x) / 2) + pSW.x;
    var centY = ((pSW.y - pNE.y) / 2) + pNE.y;
    var centLatLng = mapManager.map.containerPointToLatLng(new L.Point(centX, centY));

    return centLatLng;
}

mapManager.enableDrawingTools = function (flag) {
    if (mapManager != null) {
        if (flag) {
            $(".leaflet-control").show();
        }
        else {
            $(".leaflet-control").hide();
        }
    }
}

mapManager.addLabel = function (text, latLng, className) {
    if (text == null) {
        return;
    }

    //calculate label dimensions
    var horizontalPadding = 5;
    var charWidth = 8;
    var charHeight = 19;    // line-height
    var totalWidth = (charWidth * text.length) + (2 * horizontalPadding);

    return L.marker(latLng, {
        icon: L.divIcon({
            className: className,
            iconSize: [totalWidth, charHeight],
            html: text
        }),
        draggable: false,
        zIndexOffset: 1000
    }).addTo(mapManager.map);
}

mapManager.bgImageExists = function () {
    return mapManager.bgImageLoaded;
}