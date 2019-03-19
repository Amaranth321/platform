/**
 * Requires leaflet.manager.js
 *
 * DO NOT add page specific codes here.
 *
 * @author Aye Maung
 */
function RectangleDrawingTool($container)
{
    //constants
    var EMPTY_BG_IMG = "/public/css/common/images/empty_background.jpg";

    //instance fields
    var $drawingArea = null;
    var mapInstance = null;
    var drawnItems = new L.FeatureGroup();
    var backgroundImage = null;
    var drawnItemLimit = 0;
    var mapWidth = 0;
    var mapHeight = 0;
    var rectShapeOptions = null;

    var listeners = {
        onDrawStarted: null,
        onDrawStopped: null,
        onCreated: null,
        onEditStarted: null,
        onEditStopped: null,
        onEdited: null,
        onDeleteStarted: null,
        onDeleteStopped: null,
        onDeleted: null
    };

    var generate = function (colorOptions)
    {
        //remove any element inside
        $container.empty();

        //init map
        var mapDivId = generateRandomDivId();
        insertDrawingAreaHtml(mapDivId);
        mapInstance = L.map(mapDivId, LeafletManager.configurationsForDisabledMap());

        //drawing
        mapInstance.addLayer(drawnItems);
        mapInstance.addControl(getDrawControl(colorOptions));

        setBackgroundImage(null);
        initEventHandlers();
    };

    var addListeners = function (onDrawStarted, onDrawStopped, onCreated, onEditStarted, onEditStopped, onEdited, onDeleteStarted, onDeleteStopped, onDeleted)
    {
        listeners.onDrawStarted = onDrawStarted;
        listeners.onDrawStopped = onDrawStopped;
        listeners.onCreated = onCreated;
        listeners.onEditStarted = onEditStarted;
        listeners.onEditStopped = onEditStopped;
        listeners.onEdited = onEdited;
        listeners.onDeleteStarted = onDeleteStarted;
        listeners.onDeleteStopped = onDeleteStopped;
        listeners.onDeleted = onDeleted;
    };

    var setBackgroundImage = function (imageUrl)
    {
        //remove current
        if (backgroundImage)
        {
            mapInstance.removeLayer(backgroundImage);
        }

        if (utils.isNullOrEmpty(imageUrl))
        {
            imageUrl = EMPTY_BG_IMG;
        }

        //set image
        backgroundImage = L.imageOverlay(imageUrl, mapInstance.getBounds());
        backgroundImage.addTo(mapInstance);
        backgroundImage.bringToBack();
    };

    var setDrawnItemLimit = function (limit)
    {
        drawnItemLimit = limit;
    };

    var addRectangle = function (rectangle)
    {
        var topLeft = getLatLngPoint(rectangle.topLeftPoint);
        var bottomRight = getLatLngPoint(
            {
                x: rectangle.topLeftPoint.x + rectangle.width,
                y: rectangle.topLeftPoint.y + rectangle.height
            }
        );

        var layer = L.rectangle(L.latLngBounds(topLeft, bottomRight), rectShapeOptions).addTo(mapInstance);
        drawnItems.addLayer(layer);

        if (drawnItemLimitReached())
        {
            setDrawBtnVisibility(false);
        }
    };

    var removeAll = function ()
    {
        drawnItems.getLayers().forEach(function (layer)
        {
            mapInstance.removeLayer(layer);
            drawnItems.removeLayer(layer);
        });

        setDrawBtnVisibility(true);
    };

    var getMapSize = function ()
    {
        return { width: mapWidth, height: mapHeight};
    };

    var addMarker = function (normalizedPoint)
    {
        L.marker(getLatLngPoint(normalizedPoint)).addTo(mapInstance);
    };


    /**
     *
     * Private
     *
     */

    var insertDrawingAreaHtml = function (areaDivId)
    {
        var leftOffset = 0; //for drawing action icons in the future
        mapWidth = $container.width() - leftOffset;
        mapHeight = $container.height();

        $container.append('<div id="' + areaDivId + '"></div>');
        var $area = $container.find("#" + areaDivId);
        $area.width(mapWidth.toFixed(0));
        $area.height(mapHeight.toFixed(0));
        $area.css("margin-left", leftOffset);

        $drawingArea = $container.find("#" + areaDivId);
    };

    var getDrawControl = function (colorOptions)
    {
        rectShapeOptions = {
            stroke: true,
            color: colorOptions.borderColor,
            weight: 1.5,
            opacity: colorOptions.borderOpacity,
            fill: true,
            fillColor: colorOptions.fillColor,
            fillOpacity: colorOptions.fillOpacity,
            fillRule: "evenodd",
            clickable: true
        };

        return new L.Control.Draw({
            position: 'topleft',
            draw: {
                rectangle: {
                    shapeOptions: rectShapeOptions
                },
                polyline: false,
                polygon: false,
                circle: false,
                marker: false
            },
            edit: {
                featureGroup: drawnItems
            }
        });
    };

    var initEventHandlers = function ()
    {
        mapInstance.on('draw:drawstart', function (e)
        {
            listeners.onDrawStarted();
        });

        mapInstance.on('draw:drawstop', function (e)
        {
            listeners.onDrawStopped();
        });

        mapInstance.on('draw:created', function (e)
        {
            if (drawnItemLimitReached())
            {
                return;
            }
            drawnItems.addLayer(e.layer);

            var newRect = toRectangleRegion(e.layer);
            listeners.onCreated(newRect);
            if (drawnItemLimitReached())
            {
                setDrawBtnVisibility(false);
            }
        });

        mapInstance.on('draw:editstart', function (e)
        {
            listeners.onEditStarted();
        });

        mapInstance.on('draw:editstop', function (e)
        {
            listeners.onEditStopped();
        });

        mapInstance.on('draw:edited', function (e)
        {
            var editedList = [];
            e.layers.getLayers().forEach(function (layer)
            {
                editedList.push(toRectangleRegion(layer));
            });

            listeners.onEdited(editedList);
        });

        mapInstance.on('draw:deletestart', function (e)
        {
            listeners.onDeleteStarted();
        });

        mapInstance.on('draw:deletestop', function (e)
        {
            listeners.onDeleteStopped();
        });

        mapInstance.on('draw:deleted', function (e)
        {
            var deletedList = [];
            e.layers.getLayers().forEach(function (layer)
            {
                deletedList.push(toRectangleRegion(layer));
            });

            listeners.onDeleted(deletedList);
            if (!drawnItemLimitReached())
            {
                setDrawBtnVisibility(true);
            }
        });
    };

    var drawnItemLimitReached = function ()
    {
        return drawnItems.getLayers().length >= drawnItemLimit;
    };

    var setDrawBtnVisibility = function (visible)
    {
        var $drawBtn = $(".leaflet-draw-toolbar-top");
        if (visible)
        {
            $drawBtn.css("visibility", "visible");
        }
        else
        {
            $drawBtn.css("visibility", "hidden");
        }
    };

    var generateRandomDivId = function ()
    {
        return "leaflet_" + utils.randomAlphanumeric(10);
    };

    var getLatLngPoint = function (normalizedPoint)
    {
        //change points to domain in map dimensions
        var llX = normalizedPoint.x * mapWidth;
        var llY = normalizedPoint.y * mapHeight;

        return mapInstance.containerPointToLatLng(L.point(llX, llY));
    };

    var toRectangleRegion = function (layer)
    {
        if (!layer)
        {
            console.error("layer is null");
        }

        var northWest = mapInstance.latLngToContainerPoint(layer.getBounds().getNorthWest());
        var southEast = mapInstance.latLngToContainerPoint(layer.getBounds().getSouthEast());

        var topLeft = LeafletManager.toNormalizedPoint(northWest, mapWidth, mapHeight);
        var bottomRight = LeafletManager.toNormalizedPoint(southEast, mapWidth, mapHeight);
        var rectWidth = bottomRight.x - topLeft.x;
        var rectHeight = bottomRight.y - topLeft.y;

        return  {
            name: layer.name || "",
            topLeftPoint: topLeft,
            width: parseFloat(rectWidth.toFixed(3)),
            height: parseFloat(rectHeight.toFixed(3))
        };
    };

    var loading = function (loading)
    {
        kendo.ui.progress($drawingArea, loading);
    };

    return {
        generate: generate,
        addListeners: addListeners,
        setBackgroundImage: setBackgroundImage,
        setDrawnItemLimit: setDrawnItemLimit,
        addMarker: addMarker,
        addRectangle: addRectangle,
        removeAll: removeAll,
        getMapSize: getMapSize
    }
}