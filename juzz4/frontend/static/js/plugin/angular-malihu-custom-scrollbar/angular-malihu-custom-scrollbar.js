(function() {
    'use strict';
    angular.module('ui.mCustomScrollbar', [])
        .directive('uiMCustomScrollbar', function() {
            return {
                restrict: 'A',
                scope: {
                    msOptions: '=',
                    msStyleContainer: '=',
                    msStyleBar: '='
                },
                link: function(scope, elem, attrs) {
                    scope.$watch('msOptions', function(newVal) {
                        var options = angular.fromJson(newVal) || { scrollInertia: 0 };
                        jQuery(elem).mCustomScrollbar(options);
                        jQuery(elem)
                            .hover(function() {
                                jQuery(elem).find('.mCSB_scrollTools').removeClass('animate-fadeout');
                            }, function() {
                                jQuery(elem).find('.mCSB_scrollTools').addClass('animate-fadeout');
                            });
                        jQuery(elem).find('.mCSB_scrollTools').addClass('animate-opacity');
                    });
                    scope.$watch('msStyleContainer', function(newVal) {
                        var style = angular.fromJson(newVal) || {
                            'margin-right': '0px'
                        };
                        jQuery(elem).find('.mCSB_container').css(style);
                    });
                    scope.$watch('msStyleBar', function(newVal) {
                        var style = angular.fromJson(newVal) || {
                            'right': '-4px'
                        };
                        jQuery(elem).find('.mCSB_scrollTools').css(style);
                    });
                }
            }
        });
})();
