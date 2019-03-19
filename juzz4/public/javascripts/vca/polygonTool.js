/**
 * Notes:
 * -    Requires manager.leaflet.js
 * -    Only one tool allowed per page due to the singleton nature of manager.leaflet.js
 * -    currently used by Area Intrusion, Perimeter Defense and Loitering
 *
 * @author Aye Maung
 */
function PolygonTool(canvasId)
{
    var cfg = {
        canvas: {
            divId: canvasId,
            width: null,
            height: null
        },
        style: {
            polygon: {
                title: localizeResource('draw'),
                allowIntersection: false,
                shapeOptions: {
                    stroke: true,
                    color: '#ddd',
                    opacity: 0.9,
                    weight: 1,
                    fillColor: '#CD1625',
                    fillOpacity: 0.5,
                    clickable: false
                }
            }
        },
        current: {
            regions: [],
            bgImage: null
        }
    };

    var _initialize = function ()
    {
        _initLeafletCanvas();
        _handleDrawingEvents();


        _loading(true);
        setTimeout(function ()
        {
            _loading(false);
        }, 2000);
    };

    var _initLeafletCanvas = function ()
    {
        mapManager.initialize(cfg.canvas.divId, 20, false, false);
        mapManager.map.doubleClickZoom.disable();
        mapManager.map.scrollWheelZoom.disable();
        mapManager.initializeDrawingTools(false, false, cfg.style.polygon, false, false, true);
        mapManager.setEmptyBackground();

        //save dimensions
        var $leafletArea = $("#" + cfg.canvas.divId);
        cfg.canvas.width = $leafletArea.width();
        cfg.canvas.height = $leafletArea.height();
    };

    var _handleDrawingEvents = function ()
    {
        mapManager.map.on('draw:created', function (e)
        {
            var layer = e.layer;
            mapManager.drawnItems.addLayer(layer);
        });

        mapManager.map.on('draw:drawstop', function (e)
        {
            mapManager.map.dragging.disable();
        });
    };

    var _getNormalizedPoint = function (point)
    {
        var nX = (point.x / cfg.canvas.width);
        var nY = (point.y / cfg.canvas.height);

        //Ensure the points are within the view
        nX = (nX < 0) ? 0 : nX;
        nX = (nX > 1) ? 1 : nX;
        nY = (nY < 0) ? 0 : nY;
        nY = (nY > 1) ? 1 : nY;

        //limit 3 decimals
        nX = parseFloat(nX.toFixed(4));
        nY = parseFloat(nY.toFixed(4));

        return { x: nX, y: nY};
    };

    var _getLatLngPoint = function (normalizedPoint)
    {
        //change points to domain in map dimensions
        var llX = normalizedPoint.x * cfg.canvas.width;
        var llY = normalizedPoint.y * cfg.canvas.height;

        return mapManager.map.containerPointToLatLng(new L.Point(llX, llY));
    };

    var _updateBackground = function (imageUrl)
    {
        if (utils.isNullOrEmpty(imageUrl))
        {
            mapManager.setEmptyBackground();
        }
        else
        {
            mapManager.setBackgroundImage(imageUrl);
        }
    };

    var _setEditable = function (editable)
    {
        mapManager.enableDrawingTools(editable);
    };

    var _loading = function (loading)
    {
        kendo.ui.progress($("#" + cfg.canvas.divId), loading);
    };

    _initialize();

    return {
        updateBackground: function (imageUrl)
        {
            _updateBackground(imageUrl);
        },

        addRegions: function (regions)
        {
            if (regions == null)
            {
                console.error("regions is null");
                return;
            }

            $.each(regions, function (index, region)
            {
                var latLngPoints = [];
                $.each(region.points, function (i, p)
                {
                    latLngPoints.push(_getLatLngPoint(p));
                });

                //polygon
                var polygon = L.polygon(latLngPoints, cfg.style.polygon.shapeOptions);
                var layer = polygon.addTo(mapManager.map);

                mapManager.drawnItems.addLayer(layer);
            });
        },

        getRegions: function ()
        {
            var regions = [];
            mapManager.drawnItems.eachLayer(function (layer)
            {
                var polygonPoints = [];
                $.each(layer._originalPoints, function (index, point)
                {
                    polygonPoints.push(_getNormalizedPoint(point));
                });

                var region = {
                    "name": layer.name,
                    "points": polygonPoints
                };

                regions.push(region);
            });

            return regions;
        },

        setCameraBackground: function (coreDeviceId, channelId)
        {
            _loading(true);
            DvcMgr.getCameraSnapshot(coreDeviceId, channelId, function (jpegUrl)
            {
                if (jpegUrl == null)
                {
                    utils.popupAlert(localizeResource('error-loading-image'));
                }

                _updateBackground(jpegUrl);
                _loading(false);
            });
        },

        isImageLoaded: function ()
        {
            return mapManager.bgImageExists();
        },

        setEditable: _setEditable
    };
};



