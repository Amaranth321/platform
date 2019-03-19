(function() {
    'use strict';
    angular.module('ui.datetimepicker', [])
        .directive('uiDatetimepicker', function() {
            return {
                restrict: 'A',
                scope: {
                    uiDatetimepickerOptions: '='
                },
                link: function(scope, elem, attrs) {
                    scope.$watch('uiDatetimepickerOptions', function(newVal) {
                        var options = angular.fromJson(newVal);
                       jQuery(elem).datetimepicker(options);
                    })
                }
            }
        })
})();
