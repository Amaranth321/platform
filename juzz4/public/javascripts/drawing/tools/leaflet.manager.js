/**
 * Experimental version. Don't use this just yet
 *
 * This manager will replace manager.leaflet.js which uses a much older version.
 * Only add common and re-usable functionality to this manager.
 * Page-specific codes should not be here.
 *
 * @author Aye Maung
 */
var LeafletManager = (function ()
{
    var toolTypes = {
        RECTANGLE: "RECTANGLE"
    };

    var newDrawingArea = function ($container, toolType)
    {
        localizeTooltips();

        switch (toolType)
        {
            case toolTypes.RECTANGLE:
                return new RectangleDrawingTool($container);

            default:
                throw new TypeError("Unsupported tool type : ", toolType);
        }
    };

    var configurationsForDisabledMap = function ()
    {
        return {
            center: [0, 0],
            zoom: 20,
            dragging: false,
            attributionControl: false,
            zoomControl: false,
            scrollWheelZoom: false,
            doubleClickZoom: false,
            touchZoom: false
        };
    };

    var toNormalizedPoint = function (point, mapWidth, mapHeight)
    {
        var nX = (point.x / mapWidth);
        var nY = (point.y / mapHeight);

        //Ensure the points are within the view
        nX = (nX < 0) ? 0 : nX;
        nX = (nX > 1) ? 1 : nX;
        nY = (nY < 0) ? 0 : nY;
        nY = (nY > 1) ? 1 : nY;

        //limit 3 decimals
        nX = parseFloat(nX.toFixed(3));
        nY = parseFloat(nY.toFixed(3));

        return { x: nX, y: nY};
    };

    /**
     *
     * Private functions
     *
     */

    var localizeTooltips = function ()
    {
        //don't modify the localized strings here.
        //Instead, override the target field in their own instances
        var localizedObj = {
            draw: {
                toolbar: {
                    actions: {
                        title: localizeResource("leaflet.default.actions.text"),
                        text: localizeResource("leaflet.default.actions.text")
                    },
                    undo: {
                        title: localizeResource("leaflet.default.undo.text"),
                        text: localizeResource("leaflet.default.undo.text")
                    },
                    buttons: {
                        polyline: localizeResource("leaflet.default.buttons.polyline"),
                        polygon: localizeResource("leaflet.default.buttons.polygon"),
                        rectangle: localizeResource("leaflet.default.buttons.rectangle"),
                        circle: localizeResource("leaflet.default.buttons.circle"),
                        marker: localizeResource("leaflet.default.buttons.marker")
                    }
                },
                handlers: {
                    circle: {
                        tooltip: {
                            start: localizeResource("leaflet.default.circle.handler")
                        }
                    },
                    polygon: {
                        tooltip: {
                            start: localizeResource("leaflet.default.polygon.tooltip.start"),
                            cont: localizeResource("leaflet.default.polygon.tooltip.cont"),
                            end: localizeResource("leaflet.default.polygon.tooltip.end")
                        }
                    },
                    polyline: {
                        error: localizeResource("leaflet.default.polyline.error"),
                        tooltip: {
                            start: localizeResource("leaflet.default.polyline.tooltip.start"),
                            cont: localizeResource("leaflet.default.polyline.tooltip.cont"),
                            end: localizeResource("leaflet.default.polyline.tooltip.end")
                        }
                    },
                    rectangle: {
                        tooltip: {
                            start: localizeResource("leaflet.default.rectangle.tooltip.start")
                        }
                    },
                    simpleshape: {
                        tooltip: {
                            end: localizeResource("leaflet.default.simpleshape.tooltip.end")
                        }
                    }
                }
            },
            edit: {
                toolbar: {
                    actions: {
                        save: {
                            title: localizeResource("leaflet.default.actions.save.text"),
                            text: localizeResource("leaflet.default.actions.save.text")
                        },
                        cancel: {
                            title: localizeResource("leaflet.default.actions.save.cancel"),
                            text: localizeResource("leaflet.default.actions.save.cancel")
                        }
                    },
                    buttons: {
                        edit: localizeResource("leaflet.default.actions.edit.text"),
                        editDisabled: localizeResource("leaflet.default.actions.edit.disabled"),
                        remove: localizeResource("leaflet.default.actions.remove.text"),
                        removeDisabled: localizeResource("leaflet.default.actions.remove.disabled")
                    }
                },
                handlers: {
                    edit: {
                        tooltip: {
                            text: localizeResource("leaflet.default.actions.edit.handler.text"),
                            subtext: localizeResource("leaflet.default.actions.edit.handler.subtext")
                        }
                    },
                    remove: {
                        tooltip: {
                            text: localizeResource("leaflet.default.actions.remove.handler.text")
                        }
                    }
                }
            }
        };

        $.extend(true, L.drawLocal, localizedObj);
    };

    return {
        ToolType: toolTypes,
        newDrawingArea: newDrawingArea,
        configurationsForDisabledMap: configurationsForDisabledMap,
        toNormalizedPoint: toNormalizedPoint
    }
})();
