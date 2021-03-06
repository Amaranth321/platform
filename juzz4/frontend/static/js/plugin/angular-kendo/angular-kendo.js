(function() {
    'use strict';
    angular.module('ui.kendo', [])
        .directive('uiKendoChart', function() {
            return {
                restrict: 'A',
                scope: {
                    uiKendoChartOptions: '='
                },
                link: function(scope, elem, attrs) {
                    scope.$watch('uiKendoChartOptions', function(newVal) {
                        var options = angular.fromJson(newVal);
                        if (scope.uiKendoChartOptions) {
                            //scope.objKendoChart = jQuery(elem).kendoChart(options);
                            jQuery(elem).kendoChart(options);
                        }
                    })
                    jQuery(window).resize(function() {
                        jQuery(elem).data("kendoChart") && jQuery(elem).data("kendoChart").refresh();
                    });
                }
            }
        })
        .constant('KendoChartTheme', {
            "white": {
                "chart": {
                    "title": {
                        "color": "#4c4c4c"
                    },
                    "legend": {
                        "labels": {
                            "color": "#666666"
                        }
                    },
                    "chartArea": {
                        "background": "#ffffff"
                    },
                    "seriesDefaults": {
                        "labels": {
                            "color": "#808080"
                        }
                    },
                    "axisDefaults": {
                        "line": {
                            "color": "#808080"
                        },
                        "labels": {
                            "color": "#808080"
                        },
                        "minorGridLines": {
                            "color": "#ff0000"
                        },
                        "majorGridLines": {
                            "color": "#d9d9d9"
                        },
                        "title": {
                            "color": "#808080"
                        }
                    },
                    "seriesColors": [
                        "#2fa8ab",
                        "#f6ae40",
                        "#f6ae40",
                        "#ffff00",
                        "#ff8517",
                        "#e34a00"
                    ],
                    "tooltip": {
                        "background": "#2fa8ab",
                        "color": "#ffffff",
                        "opacity": 0.9
                    }
                },
                "gauge": {
                    "pointer": {
                        "color": "#ff0000"
                    },
                    "scale": {
                        "rangePlaceholderColor": "#7030a0",
                        "labels": {
                            "color": "#002060"
                        },
                        "minorTicks": {
                            "color": "#00b0f0"
                        },
                        "majorTicks": {
                            "color": "#00b0f0"
                        },
                        "line": {
                            "color": "#00b0f0"
                        }
                    }
                }
            },
            "black": {
                "chart": {
                    "title": {
                        "color": "#ffffff"
                    },
                    "legend": {
                        "labels": {
                            "color": "#f6ae40"
                        }
                    },
                    "chartArea": {
                        "background": "#282828"
                    },
                    "seriesDefaults": {

                        "labels": {
                            "color": "#808080"
                        }
                    },
                    "axisDefaults": {
                        "line": {
                            "color": "#808080"
                        },
                        "labels": {
                            "color": "#808080"
                        },
                        "minorGridLines": {
                            "color": "#ff0000"
                        },
                        "majorGridLines": {
                            "color": "#4c4c4c"
                        },
                        "title": {
                            "color": "#808080"
                        }
                    },
                    "seriesColors": [
                        "#f6ae40",
                        "#f6ae40",
                        "#f6ae40",
                        "#ffff00",
                        "#ff8517",
                        "#e34a00"
                    ],
                    "tooltip": {
                        "background": "#f6ae40",
                        "color": "#282828",
                        "opacity": 0.9
                    }
                },
                "gauge": {
                    "pointer": {
                        "color": "#ff0000"
                    },
                    "scale": {
                        "rangePlaceholderColor": "#7030a0",
                        "labels": {
                            "color": "#002060"
                        },
                        "minorTicks": {
                            "color": "#00b0f0"
                        },
                        "majorTicks": {
                            "color": "#00b0f0"
                        },
                        "line": {
                            "color": "#00b0f0"
                        }
                    }
                }
            }
        });
})();
