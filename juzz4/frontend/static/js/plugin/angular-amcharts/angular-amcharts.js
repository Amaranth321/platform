(function() {
    'use strict';
    angular.module('ui.amcharts', [])
        .directive('uiAmcharts', function() {
            return {
                restrict: 'A',
                scope: {
                    uiAmchartsOptions: '='
                },
                link: function(scope, elem, attrs) {
                    scope.$watch('uiAmchartsOptions', function(newVal) {
                        var options = angular.fromJson(newVal);
                        var id = $(elem).attr('id');
                        var chartName = 'AmchartsInfo';
                        var categoryBalloonFnList = (function() {
                            var categoryBalloonFnList = [];
                            if (options && options.type == 'stock' && options.panels) {
                                $.each(options.panels, function(i, pn) {
                                    if (pn.chartCursor) {
                                        categoryBalloonFnList.push(pn.chartCursor.categoryBalloonFunction || false);
                                    } else {
                                        categoryBalloonFnList.push(false);
                                    }
                                });
                            }
                            return categoryBalloonFnList;
                        })();

                        if (!$.isEmptyObject(options)) {
                            window[chartName] = window[chartName] || {};
                            window[chartName][id] = new AmCharts.makeChart(id, options);

                            //when reload uiAmchartsOptions,the content is change,let amchart error
                            arguments[2].uiAmchartsOptions = {};

                            //because categoryBalloonFunction can't use config
                            if (categoryBalloonFnList.length > 0) {
                                $.each(categoryBalloonFnList, function(i, fn) {
                                    if ($.isFunction(fn)) {
                                        window[chartName][id].panels[i].chartCursor.categoryBalloonFunction = fn;
                                    }
                                });
                            }
                        }
                    }, true)
                }
            }
        })
        .constant('AmchartsTheme', {
            "white": {
                "AmChart": {
                    "color": "#808080"
                },
                "AmCoordinateChart": {
                    "colors": ["#f6ae40", "#32a8ad", "#ff675d", "#8aba50", "#337caf", "#f6ae40", "#32a8ad", "#ff675d", "#8aba50", "#337caf"]
                },
                "AmPieChart": {
                    "colors": ["#32a8ad", "#ff675d", "#8aba50", "#337caf", "#f6ae40", "#32a8ad", "#ff675d", "#8aba50", "#337caf", "#f6ae40"]
                },
                "AmStockChart": {
                    "colors": ["#ff675d", "#8aba50", "#337caf", "#f6ae40", "#32a8ad", "#ff675d", "#8aba50", "#337caf", "#f6ae40", "#32a8ad"]
                },
                "AmSlicedChart": {
                    "outlineAlpha": 1,
                    "outlineThickness": 2,
                    "labelTickColor": "#000000",
                    "labelTickAlpha": 0.3
                },
                "AmRectangularChart": {
                    "zoomOutButtonColor": "#000000",
                    "zoomOutButtonRollOverAlpha": 0.15,
                    "zoomOutButtonImage": "lens.png"
                },
                "AxisBase": {
                    "axisColor": "#808080",
                    "axisAlpha": "1",
                    "gridAlpha": "1",
                    "gridColor": "#d9d9d9"
                },
                "ChartScrollbar": {
                    "backgroundColor": "#000000",
                    "backgroundAlpha": 0.2,
                    "graphFillAlpha": 0.5,
                    "graphLineAlpha": 0,
                    "selectedBackgroundColor": "#FFFFFF",
                    "selectedBackgroundAlpha": 0.25,
                    "gridAlpha": 0.15
                },
                "ChartCursor": {
                    "cursorColor": "#000000",
                    "color": "#FFFFFF",
                    "cursorAlpha": 0.5
                },
                "AmLegend": {
                    "color": "#808080"
                },
                "AmGraph": {
                    "lineAlpha": "1"
                },
                "GaugeArrow": {
                    "color": "#000000",
                    "alpha": 0.8,
                    "nailAlpha": 0,
                    "innerRadius": "40%",
                    "nailRadius": 15,
                    "startWidth": 15,
                    "borderAlpha": 0.8,
                    "nailBorderAlpha": 0
                },
                "GaugeAxis": {
                    "tickColor": "#000000",
                    "tickAlpha": 1,
                    "tickLength": 15,
                    "minorTickLength": 8,
                    "axisThickness": 3,
                    "axisColor": "#000000",
                    "axisAlpha": 1,
                    "bandAlpha": 0.8
                },
                "TrendLine": {
                    "lineColor": "#c03246",
                    "lineAlpha": 0.8
                },
                "AreasSettings": {
                    "alpha": 0.8,
                    "color": "#000000",
                    "colorSolid": "#000000",
                    "unlistedAreasAlpha": 0.4,
                    "unlistedAreasColor": "#000000",
                    "outlineColor": "#FFFFFF",
                    "outlineAlpha": 0.5,
                    "outlineThickness": 0.5,
                    "rollOverColor": "#3c5bdc",
                    "rollOverOutlineColor": "#FFFFFF",
                    "selectedOutlineColor": "#FFFFFF",
                    "selectedColor": "#f15135",
                    "unlistedAreasOutlineColor": "#FFFFFF",
                    "unlistedAreasOutlineAlpha": 0.5
                },
                "LinesSettings": {
                    "color": "#000000",
                    "alpha": 0.8
                },
                "ImagesSettings": {
                    "alpha": 0.8,
                    "labelColor": "#000000",
                    "color": "#000000",
                    "labelRollOverColor": "#3c5bdc"
                },
                "ZoomControl": {
                    "buttonRollOverColor": "#3c5bdc",
                    "buttonFillColor": "#f15135",
                    "buttonFillAlpha": 0.8,
                    "buttonBorderColor": "#000000",
                    "gridBackgroundColor": "#000000",
                    "gridAlpha": 0.8
                },
                "SmallMap": {
                    "mapColor": "#000000",
                    "rectangleColor": "#f15135",
                    "backgroundColor": "#FFFFFF",
                    "backgroundAlpha": 0.7,
                    "borderThickness": 1,
                    "borderAlpha": 0.8
                },
                "PeriodSelector": {
                    "color": "#000000"
                },
                "PeriodButton": {
                    "color": "#000000",
                    "backgroundColor": "#FFFFFF",
                    "borderStyle": "solid",
                    "borderColor": "#a9a9a9",
                    "borderWidth": "1px",
                    "MozBorderRadius": "5px",
                    "borderRadius": "5px",
                    "margin": "1px",
                    "outline": "none"
                },
                "PeriodButtonSelected": {
                    "color": "#000000",
                    "backgroundColor": "#b9cdf5",
                    "borderStyle": "solid",
                    "borderColor": "#b9cdf5",
                    "borderWidth": "1px",
                    "MozBorderRadius": "5px",
                    "borderRadius": "5px",
                    "margin": "1px",
                    "outline": "none"
                },
                "PeriodInputField": {
                    "background": "transparent",
                    "borderStyle": "solid",
                    "borderColor": "#a9a9a9",
                    "borderWidth": "1px",
                    "outline": "none"
                },
                "DataSetSelector": {
                    "selectedBackgroundColor": "#b9cdf5",
                    "rollOverBackgroundColor": "#a8b0e4"
                },
                "DataSetCompareList": {
                    "borderStyle": "solid",
                    "borderColor": "#a9a9a9",
                    "borderWidth": "1px"
                },
                "DataSetSelect": {
                    "borderStyle": "solid",
                    "borderColor": "#a9a9a9",
                    "borderWidth": "1px",
                    "outline": "none"
                }
            },
            "black": {
                "AmChart": {
                    "color": "#808080"
                },
                "AmCoordinateChart": {
                    "colors": ["#f6ae40", "#32a8ad", "#ff675d", "#8aba50", "#337caf", "#f6ae40", "#32a8ad", "#ff675d", "#8aba50", "#337caf"]
                },
                "AmPieChart": {
                    "colors": ["#32a8ad", "#ff675d", "#8aba50", "#337caf", "#f6ae40", "#32a8ad", "#ff675d", "#8aba50", "#337caf#", "f6ae40"]
                },
                "AmStockChart": {
                    "colors": ["#ff675d", "#8aba50", "#337caf", "#f6ae40", "#32a8ad", "#ff675d", "#8aba50", "#337caf#", "f6ae40", "#32a8ad"]
                },
                "AmSlicedChart": {
                    "outlineAlpha": 1,
                    "outlineThickness": 2,
                    "labelTickColor": "#FFFFFF",
                    "labelTickAlpha": 0.3
                },
                "AmRectangularChart": {
                    "zoomOutButtonColor": "#FFFFFF",
                    "zoomOutButtonRollOverAlpha": 0.15,
                    "zoomOutButtonImage": "lensWhite.png"
                },
                "AxisBase": {
                    "axisColor": "#808080",
                    "axisAlpha": "1",
                    "gridAlpha": "1",
                    "gridColor": "#3d3d3d"
                },
                "ChartScrollbar": {
                    "backgroundColor": "#FFFFFF",
                    "backgroundAlpha": 0.2,
                    "graphFillAlpha": 0.5,
                    "graphLineAlpha": 0,
                    "selectedBackgroundColor": "#000000",
                    "selectedBackgroundAlpha": 0.25,
                    "gridAlpha": 0.15
                },
                "ChartCursor": {
                    "cursorColor": "#FFFFFF",
                    "color": "#000000",
                    "cursorAlpha": 0.5
                },
                "AmLegend": {
                    "color": "#d9d9d9"
                },
                "AmGraph": {
                    "lineAlpha": "1"
                },
                "GaugeArrow": {
                    "color": "#FFFFFF",
                    "alpha": 0.8,
                    "nailAlpha": 0,
                    "innerRadius": "40%",
                    "nailRadius": 15,
                    "startWidth": 15,
                    "borderAlpha": 0.8,
                    "nailBorderAlpha": 0
                },
                "GaugeAxis": {
                    "tickColor": "#FFFFFF",
                    "tickAlpha": 1,
                    "tickLength": 15,
                    "minorTickLength": 8,
                    "axisThickness": 3,
                    "axisColor": "#FFFFFF",
                    "axisAlpha": 1,
                    "bandAlpha": 0.8
                },
                "TrendLine": {
                    "lineColor": "#c03246",
                    "lineAlpha": 0.8
                },
                "AreasSettings": {
                    "alpha": 0.8,
                    "color": "#FFFFFF",
                    "colorSolid": "#000000",
                    "unlistedAreasAlpha": 0.4,
                    "unlistedAreasColor": "#FFFFFF",
                    "outlineColor": "#000000",
                    "outlineAlpha": 0.5,
                    "outlineThickness": 0.5,
                    "rollOverColor": "#3c5bdc",
                    "rollOverOutlineColor": "#000000",
                    "selectedOutlineColor": "#000000",
                    "selectedColor": "#f15135",
                    "unlistedAreasOutlineColor": "#000000",
                    "unlistedAreasOutlineAlpha": 0.5
                },
                "LinesSettings": {
                    "color": "#FFFFFF",
                    "alpha": 0.8
                },
                "ImagesSettings": {
                    "alpha": 0.8,
                    "labelColor": "#FFFFFF",
                    "color": "#FFFFFF",
                    "labelRollOverColor": "#3c5bdc"
                },
                "ZoomControl": {
                    "buttonRollOverColor": "#3c5bdc",
                    "buttonFillColor": "#f15135",
                    "buttonFillAlpha": 0.8,
                    "buttonBorderColor": "#FFFFFF",
                    "gridBackgroundColor": "#FFFFFF",
                    "gridAlpha": 0.8
                },
                "SmallMap": {
                    "mapColor": "#FFFFFF",
                    "rectangleColor": "#f15135",
                    "backgroundColor": "#000000",
                    "backgroundAlpha": 0.7,
                    "borderThickness": 1,
                    "borderAlpha": 0.8
                },
                "PeriodSelector": {
                    "color": "#e7e7e7"
                },
                "PeriodButton": {
                    "color": "#e7e7e7",
                    "backgroundColor": "#282828",
                    "borderStyle": "solid",
                    "borderColor": "#585858",
                    "borderWidth": "1px",
                    "MozBorderRadius": "5px",
                    "borderRadius": "5px",
                    "margin": "1px",
                    "outline": "none"
                },
                "PeriodButtonSelected": {
                    "color": "#FFFFFF",
                    "backgroundColor": "#414c7b",
                    "borderStyle": "solid",
                    "borderColor": "#3f4b80",
                    "borderWidth": "1px",
                    "MozBorderRadius": "5px",
                    "borderRadius": "5px",
                    "margin": "1px",
                    "outline": "none"
                },
                "PeriodInputField": {
                    "color": "#e7e7e7",
                    "background": "transparent",
                    "borderStyle": "solid",
                    "borderColor": "#585858",
                    "borderWidth": "1px",
                    "outline": "none"
                },
                "DataSetSelector": {
                    "color": "#e7e7e7",
                    "selectedBackgroundColor": "#414c7b",
                    "rollOverBackgroundColor": "#000000"
                },
                "DataSetCompareList": {
                    "color": "#e7e7e7",
                    "borderStyle": "solid",
                    "borderColor": "#585858",
                    "borderWidth": "1px"
                },
                "DataSetSelect": {
                    "borderStyle": "solid",
                    "borderColor": "#585858",
                    "borderWidth": "1px",
                    "outline": "none"
                }
            }
        });
})();
