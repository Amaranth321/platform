(function() {
    'use strict';
    angular.module('ui.morris', [])
        .directive('uiMorrisDonut', function() {
            return {
                restrict: 'A',
                scope: {
                    uiMorrisDonutOptions: '='
                },
                link: function(scope, elem, attrs) {
                    scope.$watch('uiMorrisDonutOptions', function(newVal) {
                        if (!newVal || !scope.uiMorrisDonutOptions) {
                            return false;
                        };
                        var options = angular.fromJson(newVal);
                        jQuery(elem).html('');
                        var morris = Morris.Donut({
                            element: elem,
                            data: options.data,
                            colors: options.colors,
                            labelColor: options.labelColor,
                            formatter: options.formatter,
                            fontFamily: options.fontFamily,
                            backgroundColor: options.backgroundColor,
                            resize: options.resize
                        });
                    })
                }
            }
        })
        .constant("MorrisChartTheme", {
            "white": {
                "colors": ["#f6ae40", "#32a8ad", "#ff675d", "#8aba50", "#337caf", "#f6ae40", "#32a8ad", "#ff675d", "#8aba50", "#337caf"],
                "labelColor": "#4c4c4c",
                "fontFamily": "Muli",
                "backgroundColor": "#ffffff"
            },
            "black": {
                "colors": ["#f6ae40", "#32a8ad", "#ff675d", "#8aba50", "#337caf", "#f6ae40", "#32a8ad", "#ff675d", "#8aba50", "#337caf"],
                "labelColor": "#ffffff",
                "fontFamily": "Muli",
                "backgroundColor": "#282828"
            }
        });
})();
