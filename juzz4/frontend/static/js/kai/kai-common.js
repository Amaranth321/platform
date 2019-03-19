/* mapping ./main.js: shim.kai-common */
angular.module('kai.common', [
    'ngCookies'
]);

angular.module('kai.common')
    .constant('KupOption', {

        default: {
            theme: 'black',
            language: 'en',
            formatlDatetime: 'MM/DD/YYYY HH:mm:ss',
            formatHourly: 'dddd, MMM, DD, YYYY, hh:mmA',
            formatDaily: 'dddd, MMM, DD, YYYY',
            formatWeekly: 'MMM DD, YYYY',
            formatMonthly: 'MMM DD, YYYY',
            formatAxisHourly: 'hh:mm A',
            formatAxisDaily: 'ddd, MMM DD',
            formatAxisWeekly: 'dddd, MMM DD',
            formatAxisMonthly: 'MMM, YYYY',
            isoWeekStart: 7
        },

        /**
         * [System Setting]
         */
        sysApiRootUrl: window.location.origin,
        sysNgMessageUrl: 'app/widgets/angular-message.tmpl.html',

        /**
         * [Kendo TimeFormat]
         * to access api or show ui text
         * ex:kendo.parseDate('22/12/2015 16:00:00','dd/MM/yyyy HH:mm:ss')
         * ex:kendo.toString(new Date(),'ddMMyyyyHHmmss')
         */
        kendoTimeFormat: ['yyyy/MM/dd HH:mm:ss', 'dd/MM/yyyy HH:mm:ss', 'ddMMyyyyHHmmss', 'yyyyMMdd_HHmmss'],

        /**
         * [Moment TimeFormat]
         * to set daterangepicker ,and other using moment.js plugin
         */
        momentTimeFormat: ['YYYY/MM/DD HH:mm', 'DD/MM/YYYY HH:mm'],
        momentTimeFormatForStart: ['YYYY/MM/DD 00:00', 'DD/MM/YYYY 00:00'],
        momentTimeFormatForEnd: ['YYYY/MM/DD 23:59', 'DD/MM/YYYY 23:59'],

        /**
         * [jwPlayer Plugin]
         * https://support.jwplayer.com/customer/portal/articles/1413089-javascript-api-reference
         * ex:jwplayer.key = KupOption.jwplayerKey;
         */
        jwplayerKey: '5Z17dqkPunTznQ1qVqS7HB3q4xFwVjzvWKGK9w==',
        jwplayerFlashPlayerUrl: 'static/js/plugin/jwplayer/jwplayer.flash.swf',
        jwplayerHtml5PlayerUrl: 'static/js/plugin/jwplayer/jwplayer.html5.js',
        jwplayerCustomSkinUrl: 'static/js/plugin/jwplayer/skin/six.xml',
        jwplayerCustomSkinFullUrl: 'static/js/plugin/jwplayer/skin/sixfull.xml',

        /**
         * [Datatables Plugin]
         * http://l-lin.github.io/angular-datatables/
         */
        datatables: {
            displayLength: 25,
            prcessingId: 'DataTables_Table_0_processing',
        },

        /**
         * [Device Status]
         */

        deviceStatus: {
            unknown: 'UNKNOWN',
            connected: 'CONNECTED',
            disconnected: 'DISCONNECTED'
        },

        /**
         * [Common List Setting]
         */
        language: [{
            name: "en",
            text: "english",
            value: "en"
        }, {
            name: "zh-tw",
            text: "chinese-traditional",
            value: "zh-tw"
        }, {
            name: "zh-cn",
            text: "chinese-simplified",
            value: "zh-cn"
        }],

        theme: [{
            name: "white",
            text: "white",
            value: "white"
        }, {
            name: "black",
            text: "black",
            value: "black"
        }],

        age: [{
            name: 'age1',
            text: 'below20'
        }, {
            name: 'age2',
            text: '20-35'
        }, {
            name: 'age3',
            text: '36-55'
        }, {
            name: 'age4',
            text: 'above55'
        }],

        label: [{
            name: 'store',
            text: 'store',
            value: 'STORE',
            class: 'kup-store',
        }, {
            name: 'geographic-area',
            text: 'geographic-area',
            value: 'REGION',
            class: 'kup-GeographicArea',
        }, {
            name: 'other',
            text: 'other',
            value: 'OTHERS',
            class: 'fa fa-tag',
        }],

        eventType: [
            'event-vca-traffic',
            'event-vca-people-counting',
            'event-vca-crowd',
            'event-vca-audienceprofiling',
            'event-vca-intrusion',
            'event-vca-perimeter',
            'event-vca-loitering',
            'event-vca-object-counting',
            'event-vca-video-blur',
            'event-vca-face',
            'event-occupancy-limit'
        ],

        /**
         * [VCA]
         */
        vcaType: ['business', 'security'],
        vca: {
            traffic: {
                name: 'traffic',
                ngPreFix: 'Traffic',
                class: 'HumanTrafficFlow',
                eventType: 'event-vca-traffic',
                analyticsType: 'TRAFFIC',
                label: 'HT',
                typeId: 0,
            },
            pcounting: {
                name: 'pcounting',
                ngPreFix: 'Pcounting',
                class: 'PeopleCounting',
                eventType: 'event-vca-people-counting',
                analyticsType: 'PCOUNTING',
                label: 'PC',
                typeId: 0,
            },
            crowd: {
                name: 'crowd',
                ngPreFix: 'Crowd',
                class: 'CrowdDensity',
                eventType: 'event-vca-crowd',
                analyticsType: 'CROWD',
                label: 'CD',
                typeId: 0,
            },
            profiling: {
                name: 'profiling',
                ngPreFix: 'Profiling',
                class: 'AudienceProfiling',
                eventType: 'event-vca-audienceprofiling',
                analyticsType: 'PROFILING',
                label: 'AP',
                typeId: 0,
            },
            attention: {
                name: 'attention',
                ngPreFix: 'Attention',
                class: 'AudienceAttention',
                eventType: 'event-vca-audienceprofiling',
                analyticsType: 'PROFILING',
                label: 'AA',
                typeId: 0,
            },
            intrusion: {
                name: 'intrusion',
                ngPreFix: 'Intrusion',
                class: 'IntrusionDetection',
                eventType: 'event-vca-intrusion',
                analyticsType: 'INTRUSION',
                label: 'ID',
                typeId: 1,
            },
            perimeter: {
                name: 'perimeter',
                ngPreFix: 'Perimeter',
                class: 'PerimeterDefense',
                eventType: 'event-vca-perimeter',
                analyticsType: 'PERIMETER',
                label: 'PD',
                typeId: 1,
            },
            loitering: {
                name: 'loitering',
                ngPreFix: 'Loitering',
                class: 'LoiteringDetection',
                eventType: 'event-vca-loitering',
                analyticsType: 'LOITERING',
                label: 'LD',
                typeId: 1,
            },
            objcounting: {
                name: 'objcounting',
                ngPreFix: 'Objcounting',
                class: 'TripWireCounting',
                eventType: 'event-vca-object-counting',
                analyticsType: 'OBJCOUNTING',
                label: 'TC',
                typeId: 1,
            },
            videoblur: {
                name: 'videoblur',
                ngPreFix: 'Videoblur',
                class: 'CameraTampering',
                eventType: 'event-vca-video-blur',
                analyticsType: 'VIDEOBLUR',
                label: 'CT',
                typeId: 1,
            },
            face: {
                name: 'face',
                ngPreFix: 'Face',
                class: 'FaceIndexing',
                eventType: 'event-vca-face',
                analyticsType: 'FACE',
                label: 'FI',
                typeId: 1,
            },
            passerby: {
                name: 'passerby',
                ngPreFix: 'PasserByCounting',
                class: 'PasserBy',
                eventType: 'event-vca-passerby',
                analyticsType: 'PASSERBY',
                label: 'PBC',
                typeId: 1,
            },
        },
        /**
         * [Need to type]
         */

        //login page slide images
        slide_images: [{
            image: 'static/images/assets/common/login/slideshow/slide1.jpg'
        }, {
            image: 'static/images/assets/common/login/slideshow/slide2.jpg'
        }, {
            image: 'static/images/assets/common/login/slideshow/slide3.jpg'
        }, {
            image: 'static/images/assets/common/login/slideshow/slide4.jpg'
        }, {
            image: 'static/images/assets/common/login/slideshow/slide5.jpg'
        }],
        dateOfWeekStart: 0, //sunday is 0, monday is 1,
        loadingTimer: 1500, //ms,use for utils block timer
    });

(function() {
    'use strict';
    angular.module('kai.common')
        .filter('percentage', percentage)
        .filter('moment', moment);

    /*****************************************************************************************************************
     *  [public function definition]
     *
     *
     *****************************************************************************************************************/

    //https://gist.github.com/jeffjohnson9046/9470800
    function percentage($filter) {
        return function(input, decimals) {
            input = $.isNumeric(input) ? input : 0;
            return $filter('number')(input * 100, decimals) + '%';
        };
    }

    function moment($filter) {
        return function(date, format) {
            // return moment().format(format);
            return window.moment(date).format(format);
        };
    }
})();

(function() {
    'use strict';
    angular
        .module('kai.common')
        .directive('kupSidebarToggle', kupSidebarToggle)
        .directive('kupOptionsClass', ['$parse', kupOptionsClass])
        .directive('kupSelectpicker', ['$parse', kupSelectpicker])
        .directive('kupKeyEnter', kupKeyEnter)
        .directive('kupScrollButtom', kupScrollButtom);

    /*****************************************************************************************************************
     *  [public function definition]
     *
     *
     *****************************************************************************************************************/
    function kupSidebarToggle() {
        return {
            restrict: 'A',
            scope: {},
            link: function(scope, elem, attrs) {
                jQuery(elem).on('click', function(e) {
                    e.preventDefault();
                    jQuery("#kupWrapper").toggleClass("toggled", 400);
                    setTimeout(function() {
                        jQuery(document).resize();
                    }, 400);
                });
            }
        }
    }

    /**
     * [kupSelectpicker description]
     * @example
     * https://github.com/joaoneto/angular-bootstrap-select
     * http://codepen.io/joaoneto/pen/azoEdG?editors=101
     */
    function kupSelectpicker($parse) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                element.selectpicker($parse(attrs.selectpicker)());
                element.selectpicker('refresh');

                scope.$watch(attrs.ngModel, function(newVal, oldVal) {
                    scope.$parent[attrs.ngModel] = newVal;
                    scope.$evalAsync(function() {
                        if (!attrs.ngOptions || /track by/.test(attrs.ngOptions)) element.val(newVal);
                        element.selectpicker('refresh');
                    });
                });

                scope.$on('$destroy', function() {
                    scope.$evalAsync(function() {
                        element.selectpicker('destroy');
                    });
                });
            }
        };
    }

    /**
     * [kupOptionsClass description]
     * @example
     * http://stackoverflow.com/questions/15264051/how-to-use-ng-class-in-select-with-ng-options
     *
     */
    function kupOptionsClass($parse) {
        return {
            require: 'select',
            link: function(scope, elem, attrs, ngSelect) {
                var optionsSourceStr = attrs.ngOptions.split(' ').pop(),
                    getOptionsClass = $parse(attrs.kupOptionsClass);

                scope.$watch(optionsSourceStr, function(items) {
                    angular.forEach(items, function(item, index) {
                        var classes = getOptionsClass(item),
                            option = elem.find('option[value=' + index + ']');
                        if (angular.isObject(classes)) {
                            angular.forEach(classes, function(add, className) {
                                if (add) {
                                    angular.element(option).addClass(className);
                                }
                            });
                        } else if (angular.isString(classes)) {
                            angular.element(option).addClass(classes);
                        }

                    });
                });
            }
        };
    }

    /**
     * [kupKeyEnter description]
     * @example
     * http://stackoverflow.com/questions/17470790/how-to-use-a-keypress-event-in-angularjs
     */
    function kupKeyEnter() {
        return function(scope, elem, attrs) {
            elem.bind("keydown keypress", function(e) {
                if (e.which === 13) {
                    scope.$apply(function() {
                        scope.$eval(attrs.kupKeyEnter);
                    });
                    e.preventDefault();
                }
            });
        };
    }

    /**
     * [kupScrollButtom description]
     * @example
     * http://jsfiddle.net/evaneus/r6n9v/5/
     * http://stackoverflow.com/questions/3898130/how-to-check-if-a-user-has-scrolled-to-the-bottom
     */
    function kupScrollButtom() {
        return function(scope, elm, attr) {
            var funCheckBounds = function(evt) {
                if ($('#kupContainer').find("div").height() + $('footer').height() <= $('#kupContainer').height() + $('#kupContainer').scrollTop()) {
                    scope.$apply(attr.kupScrollButtom);
                }
            };
            angular.element('#kupContainer').bind('scroll load', funCheckBounds);
        };
    }

})();

angular.module('kai.common')
    .factory('AuthTokenFactory', function AuthTokenFactory(KupOption, $window) {
        var store = $window.localStorage;
        var sessionKey = 'session-key';
        var apiRootUrlKey = 'api-root-url';
        var bucketKey = 'bucket';
        var userIdKey = 'user-id';
        var themeKey = "theme";
        var userName = "user-name";
        var userEmail = "user-email";
        var userRole = "user-role";
        var userLanguage = "user-language";

        var data = store;

        return {
            data: data,
            getSessionKey: getSessionKey,
            setSessionKey: setSessionKey,
            getApiRootUrl: getApiRootUrl,
            setApiRootUrl: setApiRootUrl,
            getBucket: getBucket,
            setBucket: setBucket,
            getUserId: getUserId,
            setUserId: setUserId,
            getTheme: getTheme,
            setTheme: setTheme,
            setUserProfile: setUserProfile,
            getUserProfile: getUserProfile,
            setUserRole: setUserRole,
            getUserRole: getUserRole,
            getUserLanguage: getUserLanguage,
            setUserLanguage: setUserLanguage,
            clearStore: clearStore
        };

        function getTheme() {
            return store.getItem(themeKey) || KupOption.default.theme;
        }

        function setTheme(themeKeyValue) {
            if (themeKeyValue) {
                store.setItem(themeKey, themeKeyValue);
            } else {
                store.removeItem(themeKey);
            }
        }

        function setUserLanguage(language) {
            if (language) {
                store.setItem(userLanguage, language);
            } else {
                store.removeItem(userLanguage);
            }
        }

        function getSessionKey() {
            return store.getItem(sessionKey);
        }

        function setSessionKey(sessionKeyValue) {
            if (sessionKeyValue) {
                store.setItem(sessionKey, sessionKeyValue);
            } else {
                store.removeItem(sessionKey);
            }
        }

        function getApiRootUrl() {
            return store.getItem(apiRootUrlKey);
        }

        function setApiRootUrl(apiRootUrl) {
            if (apiRootUrl) {
                store.setItem(apiRootUrlKey, apiRootUrl);
            } else {
                store.removeItem(apiRootUrlKey);
            }
        }

        function getBucket() {
            return store.getItem(bucketKey);
        }

        function setBucket(bucket) {
            if (bucket) {
                store.setItem(bucketKey, bucket);
            } else {
                store.removeItem(bucketKey);
            }
        }

        function getUserId() {
            return store.getItem(userIdKey);
        }

        function setUserId(userId) {
            if (userId) {
                store.setItem(userIdKey, userId);
            } else {
                store.removeItem(userIdKey);
            }
        }

        function setUserProfile(userProfile) {
            if (userProfile) {
                store.setItem(userName, userProfile.name);
                store.setItem(userEmail, userProfile.email);
                store.setItem(userLanguage, userProfile.language);
            } else {
                store.removeItem(userName);
                store.removeItem(userEmail);
            }
        }

        function getUserLanguage() {
            return store.getItem(userLanguage) || KupOption.default.language;
        }

        function getUserProfile() {
            return {
                userName: store.getItem(userName),
                userEmail: store.getItem(userEmail)
            };
        }

        function setUserRole(userRoles) {
            if (!userRoles) {
                store.setItem(userRole, '');
                return false;
            }
            
            if (userRoles.length > 1) {
                roleDisplayText = "Multiple Roles";
                var roles = _.map(userRoles, function(role) {
                    return role.name;
                });
                // multipleRoles = roles.join(", ");    
            } else {
                roleDisplayText = userRoles[0].name;
            }
            store.setItem(userRole, roleDisplayText);
        }

        function getUserRole() {
            return store.getItem(userRole);
        }

        function clearStore() {
            store.clear();
        }
    });

angular
    .module('kai.common')
    .factory("DeviceTreeService",
        function(
            KupOption,
            UtilsService, KupApiService, PromiseFactory, AuthTokenFactory,
            $q, $timeout
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var ajaxPost = KupApiService.ajaxPost;

            var data = {
                deviceTree: [],
                deviceTreeType: [{
                    type: 'all',
                    subType: 'all',
                    spriteClass: 'fa fa-tag',
                }, {
                    type: 'label',
                    subType: 'STORE',
                    spriteClass: 'kup-storeTiny',
                }, {
                    type: 'label',
                    subType: 'REGION',
                    spriteClass: 'fa fa-tag',
                }, {
                    type: 'label',
                    subType: 'OTHERS',
                    spriteClass: 'fa fa-tag',
                }, {
                    type: 'node',
                    subType: 'node',
                    spriteClass: 'kup-node',
                }, {
                    type: 'camera',
                    subType: 'camera',
                    spriteClass: 'fa fa-video-camera',
                }, ],
                //api response data
                apiRunningAnalyticsList: [],
                apiLabels: [],
                apiUserDevices: [],

            };
            return {
                data: data,
                getDeviceTree: getDeviceTree,
                setDeviceTree: setDeviceTree,
                initDeviceTree: initDeviceTree,
                getDeviceDetails: getDeviceDetails,
                isEmptyDevice: isEmptyDevice
            };

            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function getDeviceTree() {
                return angular.copy(data.deviceTree);
            }

            function getDeviceTreeType(type, subType) {
                var opt = data;
                var deviceType;
                $.each(opt.deviceTreeType, function(i, device) {
                    if (type === device.type && subType === device.subType) {
                        deviceType = device;
                    }
                });
                return deviceType;
            }

            function setDeviceTree() {
                var opt = data;
                var deviceList = opt.apiUserDevices;
                var labelList = opt.apiLabels;
                var treeData = [];
                var isActiveVcaForCamera = function(cameraData) {
                    var isActiveVca = false;
                    var deviceId = cameraData.deviceId;
                    var cameraId = cameraData.cameraId;
                    $.each(opt.apiRunningAnalyticsList, function(i, vcaData) {
                        if (deviceId === parseInt(vcaData.coreDeviceId, 10) && cameraId === parseInt(vcaData.channelId, 10) && vcaData.enabled) {
                            isActiveVca = true;
                            return false;
                        }
                    });
                    return isActiveVca;
                };

                var isActiveVcaForDevice = function(cameraData) {
                    var isActiveVca = false;
                    var isBreak = false;
                    $.each(opt.apiRunningAnalyticsList, function(i, vcaData) {
                        $.each(cameraData, function(j, camera) {
                            var deviceId = camera.deviceId;
                            var cameraId = camera.cameraId;
                            if (deviceId === parseInt(vcaData.coreDeviceId, 10) && cameraId === parseInt(vcaData.channelId, 10) && vcaData.enabled) {
                                isActiveVca = true;
                                isBreak = true;
                                return false;
                            }
                        })
                        if (isBreak) {
                            return false;
                        }
                    });
                    return isActiveVca;
                }

                var isActiveVcaForLabel = function(deviceData) {
                    var isActiveVca = false;
                    var isBreak = false;
                    $.each(opt.apiRunningAnalyticsList, function(i, vcaData) {
                        $.each(deviceData, function(j, device) {
                            var cameraData = device.items;
                            $.each(cameraData, function(k, camera) {
                                var deviceId = camera.data.deviceId;
                                var cameraId = camera.data.channelId;
                                if (deviceId === parseInt(vcaData.coreDeviceId, 10) && cameraId === parseInt(vcaData.channelId, 10) && vcaData.enabled) {
                                    isActiveVca = true;
                                    isBreak = true;
                                    return false;
                                }
                            })
                            if (isBreak) {
                                return false;
                            }
                        });
                        if (isBreak) {
                            return false;
                        }
                    });
                    return isActiveVca;
                }

                var filterVcaForCamera = function(cameraData) {
                    var filterAry = [];
                    var deviceId = cameraData.deviceId;
                    var cameraId = cameraData.cameraId;
                    $.each(opt.apiRunningAnalyticsList, function(i, vcaData) {
                        if (deviceId === parseInt(vcaData.coreDeviceId, 10) && cameraId === parseInt(vcaData.channelId, 10) && vcaData.enabled) {
                            var vcaType = vcaData.type;
                            var flag = true;
                            $.each(filterAry, function(i, vca) {
                                if (vca === vcaType) {
                                    flag = false;
                                    return false;
                                }
                            });
                            if (flag) {
                                filterAry.push(vcaData.type);
                            }
                        }
                    });
                    return filterAry.toString();
                };

                var filterVcaForDevice = function(cameraData) {
                    var filterAry = [];
                    $.each(cameraData, function(i, camera) {
                        var deviceId = camera.deviceId;
                        var cameraId = camera.cameraId;
                        $.each(opt.apiRunningAnalyticsList, function(j, vcaData) {
                            if (deviceId === parseInt(vcaData.coreDeviceId, 10) && cameraId === parseInt(vcaData.channelId, 10) && vcaData.enabled) {
                                var vcaType = vcaData.type;
                                var flag = true;
                                $.each(filterAry, function(i, vca) {
                                    if (vca === vcaType) {
                                        flag = false;
                                        return false;
                                    }
                                });
                                if (flag) {
                                    filterAry.push(vcaData.type);
                                }
                            }
                        });
                    })
                    return filterAry.toString();
                }

                var filterVcaForLabel = function(deviceData) {
                    var filterAry = [];
                    $.each(deviceData, function(i, device) {
                        var cameraData = device.items;
                        $.each(cameraData, function(j, camera) {
                            var deviceId = camera.deviceId;
                            var cameraId = camera.cameraId;
                            $.each(opt.apiRunningAnalyticsList, function(k, vcaData) {
                                if (deviceId === parseInt(vcaData.coreDeviceId, 10) && cameraId === parseInt(vcaData.channelId, 10) && vcaData.enabled) {
                                    var vcaType = vcaData.type;
                                    var flag = true;
                                    $.each(filterAry, function(i, vca) {
                                        if (vca === vcaType) {
                                            flag = false;
                                            return false;
                                        }
                                    });
                                    if (flag) {
                                        filterAry.push(vcaData.type);
                                    }
                                }
                            })
                        })
                    });
                    return filterAry.toString();
                }

                var filterTextForCamera = function(cameraData) {
                    var filterAry = [cameraData.labelName, cameraData.deviceName, cameraData.cameraName];
                    return filterAry.toString();
                };

                var filterTextForDevice = function(daviceData, cameraData) {
                    var filterAry = [daviceData.labelName, daviceData.deviceName];
                    $.each(cameraData, function(i, camera) {
                        filterAry.push(camera.cameraName);
                    });
                    return filterAry.toString();
                };

                var filterTextForLabel = function(labelData, deviceData) {
                    var filterAry = [labelData.labelName];
                    $.each(deviceData, function(i, device) {
                        var cameraData = device.items;
                        filterAry.push(device.deviceName);
                        $.each(cameraData, function(j, camera) {
                            filterAry.push(camera.cameraName);
                        });
                    });
                    return filterAry.toString();
                };

                var isDeviceInLabel = function(labelId, deviceId) {
                    var check = false;
                    var isBreak = false;
                    $.each(deviceList, function(i, device) {
                        if (parseInt(device.deviceId, 10) === deviceId) {
                            $.each(device.channelLabels, function(j, camera) {
                                $.each(camera.labels, function(k, label) {
                                    if (label === labelId) {
                                        check = true;
                                        isBreak = true;
                                        return false;
                                    }
                                });
                                if (isBreak) {
                                    return false;
                                }
                            });
                        }
                        if (isBreak) {
                            return false;
                        }

                    });
                    return check;
                };

                var isCameraInLabel = function(labelId, deviceId, cameraId) {
                    var check = false;
                    var isBreak = false;
                    $.each(deviceList, function(i, device) {
                        if (parseInt(device.deviceId, 10) === deviceId) {
                            $.each(device.channelLabels, function(j, camera) {
                                $.each(camera.labels, function(k, label) {
                                    if (label === labelId && parseInt(camera.channelId, 10) === cameraId) {
                                        check = true;
                                        isBreak = true;
                                        return false;
                                    }
                                });
                                if (isBreak) {
                                    return false;
                                }
                            });
                        }
                        if (isBreak) {
                            return false;
                        }

                    });
                    return check;
                };

                var isNodePower = function(capabilities) {
                    var check = false;
                    if (capabilities.indexOf("node") !== -1) {
                        check = true;
                    }
                    return check;
                };

                var isVideoPower = function(capabilities) {
                    var check = false;
                    if (capabilities.indexOf("video") !== -1) {
                        check = true;
                    }
                    return check;
                };

                var setBasicAll = function(label) {
                    var labelId = label.labelId;
                    var labelName = label.name;
                    var labelData = {
                        isAll: true,
                        labelId: labelId,
                        labelName: labelName,
                        text: labelName,
                        items: [],
                    };
                    return labelData;
                };

                var setBasicLabel = function(label) {
                    var labelId = label.labelId;
                    var labelName = label.name;

                    var labelData = {
                        isLabel: true,
                        labelId: labelId,
                        labelName: labelName,
                        text: labelName,
                        data: {
                            type: label.type,
                            info: label.info
                        },
                        items: [],
                    };
                    return labelData;
                };

                var setBasicDevice = function(label, device) {
                    var labelId = label.labelId;
                    var labelName = label.name;
                    var deviceId = parseInt(device.deviceId, 10);
                    var platformDeviceId = parseInt(device.id, 10);
                    var deviceName = device.name;
                    var isNode = isNodePower(device.model.capabilities);

                    var deviceData = {
                        isDevice: true,
                        isOnline: (device.status !== kupOpt.deviceStatus.disconnected) ? true : false,
                        isNode: isNode,
                        labelId: labelId,
                        labelName: labelName,
                        deviceId: deviceId,
                        platformDeviceId : platformDeviceId,
                        deviceName: deviceName,
                        text: deviceName,
                        data: {
                            capabilities: device.model.capabilities.split(" "),
                        },
                        items: [],
                    };
                    return deviceData;
                };

                var setBasicCamera = function(label, device, camera) {
                    var labelId = label.labelId;
                    var labelName = label.name;
                    var deviceId = parseInt(device.deviceId, 10);
                    var platformDeviceId = parseInt(device.id, 10);
                    var deviceName = device.name;
                    var cameraId = parseInt(camera.nodeCoreDeviceId, 10);
                    var cameraName = camera.name;
                    var isNode = isNodePower(device.model.capabilities);

                    var cameraData = {
                        isCamera: true,
                        isOnline: (device.status !== kupOpt.deviceStatus.disconnected) ? true : false,
                        isNode: isNode,
                        labelId: labelId,
                        labelName: labelName,
                        deviceId: deviceId,
                        platformDeviceId: platformDeviceId,
                        deviceName: deviceName,
                        cameraId: cameraId,
                        cameraName: cameraName,
                        text: cameraName,
                        data: {
                            platformDeviceId: platformDeviceId,
                            deviceId: deviceId,
                            deviceName: deviceName,
                            channelId: cameraId,
                            channelName: cameraName,
                            camera: camera,
                        },
                        fullData: device
                    };
                    return cameraData;
                };

                //set basic tree data,label is all
                (function() {
                    var label = {
                        labelId: '',
                        name: 'all'
                    };
                    var labelData = setBasicAll(label);
                    $.each(deviceList, function(j, device) {
                        var deviceId = parseInt(device.deviceId, 10);
                        if (!isVideoPower(device.model.capabilities)) {
                            return true;
                        }
                        var deviceData = setBasicDevice(label, device);
                        if (isNodePower(device.model.capabilities)) {
                            $.each(device.node.cameras, function(k, camera) {
                                var cameraId = parseInt(camera.nodeCoreDeviceId, 10);
                                var cameraData = setBasicCamera(label, device, camera);
                                deviceData.items.push(cameraData);
                            })
                        }
                        labelData.items.push(deviceData);
                    })
                    treeData.push(labelData);
                })();

                //set basic tree data,,label is label
                (function() {
                    $.each(labelList, function(i, label) {
                        var labelId = label.labelId;
                        var labelData = setBasicLabel(label);
                        $.each(deviceList, function(j, device) {
                            var deviceId = parseInt(device.deviceId, 10);
                            if (!isVideoPower(device.model.capabilities)) {
                                return true;
                            }
                            if (isDeviceInLabel(labelId, deviceId)) {
                                var deviceData = setBasicDevice(label, device);
                                if (isNodePower(device.model.capabilities)) {
                                    $.each(device.node.cameras, function(k, camera) {
                                        var cameraId = parseInt(camera.nodeCoreDeviceId, 10);
                                        if (isCameraInLabel(labelId, deviceId, cameraId)) {
                                            var cameraData = setBasicCamera(label, device, camera);
                                            deviceData.items.push(cameraData);
                                        }
                                    })
                                }
                                labelData.items.push(deviceData);
                            }
                        })
                        treeData.push(labelData);
                    });
                })();

                //add vca , filter,css,kendo infomation
                (function() {
                    $.each(treeData, function(i, label) {
                        var deviceData = label.items;
                        label.filterText = filterTextForLabel(label, deviceData);
                        label.filterVca = filterVcaForLabel(deviceData);
                        label.isActiveVca = isActiveVcaForLabel(deviceData);
                        label.spriteCssClass = (function() {
                            var className = '';
                            if (label.isAll) {
                                className = getDeviceTreeType('all', 'all').spriteClass;
                            }
                            if (label.isLabel) {
                                className = getDeviceTreeType('label', label.data.type).spriteClass;
                            }
                            return className;
                        })();
                        label.labelClass = label.spriteCssClass;
                        label.expanded = false;

                        $.each(deviceData, function(j, device) {
                            var cameraData = device.items;
                            device.filterText = filterTextForDevice(device, cameraData);
                            device.filterVca = filterVcaForDevice(cameraData);
                            device.isActiveVca = isActiveVcaForDevice(cameraData);
                            device.spriteCssClass = getDeviceTreeType('node', 'node').spriteClass;
                            device.labelClass = label.labelClass;
                            device.expanded = false;

                            $.each(cameraData, function(k, camera) {
                                camera.filterText = filterTextForCamera(camera);
                                camera.filterVca = filterVcaForCamera(camera);
                                camera.isActiveVca = isActiveVcaForCamera(camera);
                                camera.spriteCssClass = getDeviceTreeType('camera', 'camera').spriteClass;
                                camera.labelClass = label.labelClass;
                            });
                        });
                    });
                })();

                opt.deviceTree = treeData;
            }

            function initDeviceTree() {
                var opt = data;
                var request = [
                    listRunningAnalyticsApi(),
                    getLabelsApi(),
                    getUserDevicesApi()
                ];

                var promise = $q.all(request)
                    .finally(function() {
                        setDeviceTree();
                    });

                return promise;
            }

            function listRunningAnalyticsApi() {
                var opt = data;
                var analyticsType = 'ALL';
                var param = {
                    "analytics-type": analyticsType,
                };
                var onSuccess = function(response) {
                    opt.apiRunningAnalyticsList = response.instances || [];
                };
                var onFail = function(response) {
                    opt.apiRunningAnalyticsList = [];
                };
                var onError = function() {
                    opt.apiRunningAnalyticsList = [];
                };
                return ajaxPost('listrunninganalytics', param, onSuccess, onFail, onError, true);
            }

            function getLabelsApi() {
                var opt = data;
                var param = {};
                var onSuccess = function(response) {
                    opt.apiLabels = response.labels || [];
                };
                var onFail = function(response) {
                    opt.apiLabels = [];
                };
                var onError = function() {
                    opt.apiLabels = [];
                };
                return ajaxPost('getlabels', param, onSuccess, onFail, onError, true);
            }

            function getUserDevicesApi() {
                var opt = data;
                var param = {};
                var onSuccess = function(response) {
                    opt.apiUserDevices = response.devices || [];
                };
                var onFail = function(response) {
                    opt.apiUserDevices = [];
                };
                var onError = function() {
                    opt.apiUserDevices = [];
                };
                return ajaxPost('getuserdevices', param, onSuccess, onFail, onError, true);
            }

            function getDeviceDetails(platformDeviceId, coreDeviceId, channelId) {
                var opt = data;
                var localizeResource = i18n;
                var targetObj = {};

                targetObj.deviceName = localizeResource("n/a");
                targetObj.channelName = localizeResource("n/a");
                targetObj.nodeVersion = -1; //not node

                $.each(opt.apiUserDevices, function(index, dvc) {
                    if (dvc.id == platformDeviceId || dvc.deviceId == coreDeviceId) {
                        targetObj.deviceName = dvc.name;
                        targetObj.address = dvc.address;
                        targetObj.latitude = dvc.latitude;
                        targetObj.longitude = dvc.longitude;
                        targetObj.coreDeviceId = dvc.deviceId;
                        targetObj.groupType = "type-running-on-" + kupapi.applicationType;
                        targetObj.deviceStatus = dvc.status;

                        if (channelId == null || channelId === "") {
                            targetObj.channelName = "";
                        } else if (kupapi.applicationType == "node" || dvc.model.capabilities.indexOf("node") == -1) {
                            targetObj.channelName = parseInt(channelId) + 1 + "";
                        }
                        //for Kai Nodes
                        else {
                            $.each(dvc.node.cameras, function(index, cameraItem) {
                                if (cameraItem.nodeCoreDeviceId == channelId) {
                                    targetObj.channelName = cameraItem.name;
                                    targetObj.groupType = "type-running-on-node";
                                    return false;
                                }
                            });

                            if (targetObj.channelName == null) {
                                console.log(targetObj.deviceName + " :node camera not found nodeCoreDeviceId=" + channelId);
                                targetObj.channelName = parseInt(channelId) + 1 + "";
                                targetObj.groupType = "unknown-type";
                            }

                            targetObj.nodeVersion = dvc.node.version;
                        }

                        return false;
                    }
                });

                return targetObj;
            }

            function isEmptyDevice() {
                var opt = data;
                var check = false;
                if (!opt.apiUserDevices.length) {
                    check = true;
                }
                return check;
            }
        }
    );
angular
    .module('kai.common')
    .factory("GoogleMapService",
        function(
            KupOption,
            LoaderService, UtilsService, KupApiService, PromiseFactory, AuthTokenFactory,
            $q, $rootScope, $timeout
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var ajaxPost = KupApiService.ajaxPost;

            var data = {
                isSuccessLoad: false,
                mapApiKey: 'AIzaSyCv9ajgEy884HyIZKZkz-BtrXLQ_4XfDWU',
                mapApi: 'https://maps.googleapis.com/maps/api/js?libraries=places&callback=initGoogleMap&key=AIzaSyCv9ajgEy884HyIZKZkz-BtrXLQ_4XfDWU',
                timezoneApi: 'https://maps.googleapis.com/maps/api/timezone/json',
                geocodeApi: 'https://maps.googleapis.com/maps/api/geocode/json?key=AIzaSyCv9ajgEy884HyIZKZkz-BtrXLQ_4XfDWU',
                searchMap: {
                    isSuccessSearch: false,
                    center: {
                        lat: 34,
                        lng: 173
                    },
                    zoom: 1,
                    location: '',
                    lat: 0,
                    lng: 0,
                    timeZoneOffset: 0,
                    timeZoneId: '',
                },
                searchBox: {
                    isTriggerSearch: false,
                    isSuccessSearch: false,
                    location: '',
                    lat: 0,
                    lng: 0,
                    timeZoneOffset: 0,
                    timeZoneId: '',
                }
            }
            return {
                data: data,
                setSearchMap: setSearchMap,
                setSearchBox: setSearchBox,
                loadSearchMap: loadSearchMap,
                loadSearchBox: loadSearchBox,

            };

            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function setSearchMap(mapId, searchBoxId, defaultSearchText) {
                if (!window.google.maps.Map || !window.google.maps.places) {
                    return false;
                }
                var opt = data;
                var mapDom = document.getElementById(mapId);
                var searchBoxDom = document.getElementById(searchBoxId);
                var $map = $('#' + mapId);
                var $searchBox = $('#' + searchBoxId);

                var setSearch = function(location) {
                    $searchBox.val(location);
                };
                var setLocation = function(location) {
                    location = location || '';
                    opt.searchMap.location = location;
                };
                var setLatlng = function(latlng) {
                    opt.searchMap.lat = (latlng) ? latlng.lat() : 0;
                    opt.searchMap.lng = (latlng) ? latlng.lng() : 0;
                };
                var setTimezone = function(latlng) {
                    latlng = (latlng) ? latlng.lat() + "," + latlng.lng() : 0;
                    if (latlng) {
                        $.ajax({
                            url: opt.timezoneApi + "?key=" + opt.mapApiKey + "&location=" + latlng + "&timestamp=" + (Math.round((new Date().getTime()) / 1000)).toString(),
                        }).done(function(response) {
                            //var timeZoneOffset = (response.timeZoneId !== null) ? response.rawOffset : 0;
                            opt.searchMap.timeZoneOffset = response.rawOffset;
                            opt.searchMap.timeZoneId = response.timeZoneId;
                        });
                    } else {
                        opt.searchMap.timeZoneOffset = 0;
                        opt.searchMap.timeZoneId = '';
                    }
                };
                var map = new google.maps.Map(mapDom, {
                    center: opt.searchMap.center,
                    zoom: opt.searchMap.zoom,
                });
                var searchBox = new google.maps.places.Autocomplete(searchBoxDom, {
                    types: ['(regions)']
                });
                var geocoder = new google.maps.Geocoder();
                var marker = new google.maps.Marker();

                var setMarker = function(opt) {
                    marker.setMap(null);
                    marker = new google.maps.Marker(opt);
                };

                var clearMarker = function() {
                    marker.setMap(null);
                };

                var searchChangeFn = function() {
                    var place = searchBox.getPlace() || {};
                    var placeName = $searchBox.val();
                    var placeId = place.place_id || '';
                    var placeIcon = place.icon || '';

                    var makerIcon = {
                        url: placeIcon,
                        size: new google.maps.Size(71, 71),
                        origin: new google.maps.Point(0, 0),
                        anchor: new google.maps.Point(17, 34),
                        scaledSize: new google.maps.Size(25, 25)
                    };

                    geocoder.geocode({
                        'address': placeName
                    }, function(results, status) {
                        $rootScope.$apply(function() {
                            // console.warn("====== Get location from search ======");
                            // console.info(results);
                            if (status === google.maps.GeocoderStatus.OK) {
                                var placeLatlng = results[0].geometry.location;
                                var placeViewport = results[0].geometry.viewport || '';
                                var bounds = new google.maps.LatLngBounds();
                                map.setCenter(placeLatlng);
                                setMarker({
                                    map: map,
                                    //icon: makerIcon, 
                                    title: placeName,
                                    position: placeLatlng
                                });
                                if (placeViewport) {
                                    bounds.union(placeViewport);
                                } else {
                                    bounds.extend(placeLatlng);
                                }
                                map.fitBounds(bounds);

                                setLocation(placeName);
                                setLatlng(placeLatlng);
                                setTimezone(placeLatlng);
                                opt.searchMap.isSuccessSearch = true;
                            } else {
                                console.warn('Google Geocode Status: ' + status);
                                clearMarker();
                                map.setCenter(opt.searchMap.center);
                                map.setZoom(opt.searchMap.zoom);
                                setLocation();
                                setLatlng();
                                setTimezone();
                                opt.searchMap.isSuccessSearch = false;
                            }
                        });
                    });
                };

                var boundsChangedFn = function() {
                    searchBox.setBounds(map.getBounds());
                };

                var mapClickFn = function(event) {
                    var placeLatlng = event.latLng;
                    geocoder.geocode({
                        'latLng': event.latLng
                    }, function(results, status) {
                        $rootScope.$apply(function() {
                            // console.warn("====== Get location from click ======");
                            // console.info(results);
                            if (status === google.maps.GeocoderStatus.OK) {
                                var placeName = (results && results.length > 0) ? results[0].formatted_address : '';
                                setMarker({
                                    position: event.latLng,
                                    map: map
                                });
                                setSearch(placeName);
                                setLocation(placeName);
                                setLatlng(placeLatlng);
                                setTimezone(placeLatlng);
                                opt.searchMap.isSuccessSearch = true;
                            } else {
                                console.warn('Google Geocode Status: ' + status);
                            }
                        });
                    });
                };

                //init 
                opt.searchMap.isSuccessSearch = false;
                map.controls[google.maps.ControlPosition.TOP_LEFT].push(searchBoxDom);
                map.addListener('bounds_changed', boundsChangedFn);
                searchBox.addListener('place_changed', searchChangeFn);
                google.maps.event.addListener(map, 'click', mapClickFn);

                if (defaultSearchText) {
                    $timeout(function() {
                        $('#' + searchBoxId).val(defaultSearchText);
                        google.maps.event.trigger(searchBox, 'place_changed');
                    });
                }
            }

            function setSearchBox(searchBoxId) {
                if (!window.google.maps.Map || !window.google.maps.places) {
                    return false;
                }
                var opt = data;
                var searchBoxDom = document.getElementById(searchBoxId);
                var $searchBox = $('#' + searchBoxId);

                var setSearch = function(location) {
                    $searchBox.val(location);
                };
                var setLocation = function(location) {
                    location = location || '';
                    opt.searchBox.location = location;
                };
                var setLatlng = function(latlng) {
                    opt.searchBox.lat = (latlng) ? latlng.lat() : 0;
                    opt.searchBox.lng = (latlng) ? latlng.lng() : 0;
                };
                var setTimezone = function(latlng) {
                    latlng = (latlng) ? latlng.lat() + "," + latlng.lng() : 0;
                    if (latlng) {
                        $.ajax({
                            url: opt.timezoneApi + "?key=" + opt.mapApiKey + "&location=" + latlng + "&timestamp=" + (Math.round((new Date().getTime()) / 1000)).toString(),
                        }).done(function(response) {
                            //var timeZoneOffset = (response.timeZoneId !== null) ? response.rawOffset : 0;
                            opt.searchBox.timeZoneOffset = response.rawOffset;
                            opt.searchBox.timeZoneId = response.timeZoneId;
                        });
                    } else {
                        opt.searchMap.timeZoneOffset = 0;
                        opt.searchMap.timeZoneId = '';
                    }
                };
                var searchBox = new google.maps.places.Autocomplete(searchBoxDom, {
                    types: ['(regions)']
                });
                var geocoder = new google.maps.Geocoder();

                var searchChangeFn = function() {
                    var placeName = $searchBox.val();
                    geocoder.geocode({
                        'address': placeName
                    }, function(results, status) {
                        $rootScope.$apply(function() {
                            // console.warn("====== Get location from search ======");
                            // console.info(results);               
                            if (status === google.maps.GeocoderStatus.OK) {
                                var location = placeName;
                                var placeLatlng = results[0].geometry.location;
                                setLocation(location);
                                setLatlng(placeLatlng);
                                setTimezone(placeLatlng);
                                opt.searchBox.isTriggerSearch = true;
                                opt.searchBox.isSuccessSearch = true;
                            } else {
                                console.warn('Google Geocode Status: ' + status);
                                setLocation();
                                setLatlng();
                                setTimezone();
                                opt.searchBox.isTriggerSearch = true;
                                opt.searchBox.isSuccessSearch = false;
                            }
                        });
                    });

                };
                //init 
                opt.searchBox.isTriggerSearch = false;
                opt.searchBox.isSuccessSearch = false;
                searchBox.addListener('place_changed', searchChangeFn);
            }

            function loadSearchMap(mapId, searchBoxId, defaultSearchText) {
                var opt = data;
                window.initGoogleMap = function() {
                    setSearchMap(mapId, searchBoxId, defaultSearchText);
                };
                return LoaderService.loadScript(opt.mapApi)
                    .then(function() {
                        opt.isSuccessLoad = true;
                        window.initGoogleMap();
                    })
                    .catch(function() {
                        opt.isSuccessLoad = false;
                        notification('error', i18n("google-map-load-error"));
                    });
            }

            function loadSearchBox(searchBoxId) {
                var opt = data;
                window.initGoogleMap = function() {
                    setSearchBox(searchBoxId);
                };
                return LoaderService.loadScript(opt.mapApi)
                    .then(function() {
                        opt.isSuccessLoad = true;
                        window.initGoogleMap();
                    })
                    .catch(function() {
                        opt.isSuccessLoad = false;
                        notification('error', i18n("google-map-load-error"));
                    });
            }
        }
    );

angular
    .module('kai.common')
    .factory("KupApiService",
        function(
            UtilsService, KupOption, AuthTokenFactory, RouterStateService,
            $http, $q, $location, $timeout
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;

            var data = {
                header: {
                    'Accept': '*/*',
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                timeZoneOffset: new Date().getTimezoneOffset() * (-1),
                sessionKey: '',
                rootUrl: '',
                apiList: [], //store all api request
            }
            return {
                data: data,
                ajaxPost: ajaxPost,
                ajaxCancel: ajaxCancel,
                apiDownload: apiDownload,
                exportDoc: exportDoc
            };

            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function setData() {
                data.sessionKey = AuthTokenFactory.getSessionKey();
                data.rootUrl = KupOption.sysApiRootUrl + '/api/';
            }

            function getParam(param) {
                param = ($.isPlainObject(param)) ? param : {};
                param['session-key'] = (param['session-key']) ? param['session-key'] : data.sessionKey;
                return param;
            }

            function getConfig(config) {
                config = ($.isPlainObject(config)) ? config : {};
                config['headers'] = data.header;
                return config;
            }

            function exportDoc(params, url) {
                var onSuccess = function(response) {
                    if (response["download-url"] != null) {
                        UtilsService.urlDownload(response['download-url']);
                    }
                };
                var onFail = function(response) {
                    if(response['reason']){
                        notification('error', i18n(response['reason']));
                    }              
                };
                var onError = function() {
                    notification('error', i18n("server-error"));
                };
                return ajaxPost(url, params, onSuccess, onFail, onError);
            }

            /**
             * [ajaxPost description]
             * @param  {string} apiName   ex:'listposnames'
             * @param  {object} param     ex:{"session-key":'123',"event-type": 'pcounting'}
             * @param  {function} onSuccess api has response, and 'result == ok'
             * @param  {function} onFail    api has response, and 'result != ok'
             * @param  {function} onError   api has error response
             * @param  {Boolean} isDefer   true is return dfd.promise,when call $q.all().finally() to use 
             * @return {object}           return angular.$http object
             */
            function ajaxPost(apiName, param, onSuccess, onFail, onError, isDefer) {
                setData();
                param = getParam(param);
                onSuccess = ($.isFunction(onSuccess)) ? onSuccess : function() {};
                onFail = ($.isFunction(onFail)) ? onFail : function() {};
                onError = ($.isFunction(onError)) ? onError : function() {};
                isDefer = !!isDefer;

                var opt = data;
                var apiUrl = data.rootUrl + AuthTokenFactory.getBucket() + "/" + apiName;
                var dfd = $q.defer();
                var canceller = $q.defer();
                var config = getConfig({
                    timeout: canceller.promise
                });
                var isCancel = false;
                var reguest = $http.post(apiUrl, $.param(param), config)
                    .success(function(response) {
                        if (response['result'] === 'ok') {
                            onSuccess(response);
                        } else {
                            console.warn('API Warning: ' + apiUrl);
                            console.warn(response['reason']);
                            if (response['reason'] && response['reason'] === 'session-expired') {
                                onSessionExpired();
                            } else {
                                onFail(response);
                            }
                        }
                        if (isDefer) {
                            dfd.resolve(response);
                        }
                    })
                    .error(function(err, status) {
                        if (isCancel) {
                            console.warn('API Cancel: ' + apiUrl);
                        } else {
                            console.error('API Error: ' + apiUrl);
                            onError(err);
                        }
                        if (isDefer) {
                            dfd.resolve(err);
                        }
                    });

                //can abort $http promise
                if (isDefer) {
                    dfd.promise.cancel = function() {
                        isCancel = true;
                        canceller.resolve();
                    };
                    opt.apiList.push(dfd.promise);
                } else {
                    reguest.cancel = function() {
                        isCancel = true;
                        canceller.resolve();
                    };
                    opt.apiList.push(reguest);
                }

                return (isDefer) ? dfd.promise : reguest;
            }

            function ajaxCancel() {
                var opt = data;
                $.each(opt.apiList, function(i, request) {
                    request.cancel && request.cancel();
                });
                opt.apiList = [];
            }

            function apiDownload(apiName, param) {
                setData();
                param = getParam(param);
                var apiUrl = data.rootUrl + AuthTokenFactory.getBucket() + "/" + apiName;
                console.log(apiUrl);
                var link = apiUrl + "?" + $.param(param);
                window.open(link, "_blank");
            }

            function onSessionExpired() {
                ajaxCancel();
                notification('error', i18n('session-expired-login'));
                AuthTokenFactory.clearStore();
                $timeout(function() {
                    $location.path('/login');
                }, 300);
            }
        });

/* https://github.com/urish/angular-load/blob/master/angular-load.js */
/* angular-load.js / v0.3.0 / (c) 2014, 2015 Uri Shaked / MIT Licence */
angular.module('kai.common')
    .service('LoaderService', function($document, $q, $timeout) {
        var document = $document[0];

        function loader(createElement) {
            var promises = {};

            return function(url) {
                if (typeof promises[url] === 'undefined') {
                    var deferred = $q.defer();
                    var element = createElement(url);

                    element.onload = element.onreadystatechange = function(e) {
                        $timeout(function() {
                            deferred.resolve(e);
                        });
                    };
                    element.onerror = function(e) {
                        $timeout(function() {
                            deferred.reject(e);
                        });
                    };

                    promises[url] = deferred.promise;
                }

                return promises[url];
            };
        }

        /**
         * Dynamically loads the given script
         * @param src The url of the script to load dynamically
         * @returns {*} Promise that will be resolved once the script has been loaded.
         */
        this.loadScript = loader(function(src) {
            var script = document.createElement('script');

            script.src = src;

            document.body.appendChild(script);
            return script;
        });

        /**
         * Dynamically loads the given CSS file
         * @param href The url of the CSS to load dynamically
         * @returns {*} Promise that will be resolved once the CSS file has been loaded.
         */
        this.loadCSS = loader(function(href) {
            var style = document.createElement('link');

            style.rel = 'stylesheet';
            style.type = 'text/css';
            style.href = href;

            document.head.appendChild(style);
            return style;
        });
    });

/**
 * http://ericnish.io/blog/add-success-and-error-to-angular-promises
 */
angular.module('kai.common')
    .factory('PromiseFactory',
        function($q) {
            return {
                decorate: function(promise) {
                    promise.success = function(callback) {
                        promise.then(callback);
                        return promise;
                    };
                    promise.error = function(callback) {
                        promise.then(null, callback);
                        return promise;
                    };
                },
                defer: function() {
                    var deferred = $q.defer();
                    this.decorate(deferred.promise);
                    return deferred;
                }
            };
        }
    );

angular.module('kai.common')
    .factory('RouterStateService',
        function(KupOption, $location) {
            var routerState = {};
            return {
                setRouterState: setRouterState,
                getRouterState: getRouterState,

                isCurrentPage: isCurrentPage,
            };

            function setRouterState(routerInfo) {
                routerState = routerInfo;
                return routerState;
            }

            function getRouterState() {
                return routerState;
            }

            function isCurrentPage(currectUrl) {
                var currectUrl = currectUrl || $location.path();
                var routerState = getRouterState().toState;

                var isCurrentPage = (function(){
                    var check = new RegExp(routerState.url)
                    return check.test(currectUrl);
                })();
                return isCurrentPage;
            }
        });

angular.module('kai.common')
    .factory('SoundManagerService', function ($window) {
        $window.soundManager.initialize();
        return $window.soundManager;
    })
angular.module('kai.common')
    .factory('_', function ($window) {

        return $window._;
    });
angular.module('kai.common')
    .service('UserService', function($http, AuthTokenFactory, $cookies, $window, KupApiService, PromiseFactory, $state, KupOption, LoaderService) {
        var ajaxPost = KupApiService.ajaxPost;

        var data = {
            apiUserFeature: [],
        };
        return {
            data: data,
            login: login,
            logout: logout,
            forgotPassword: forgotPassword,
            setUserFeature: setUserFeature,
            getUserFeature: getUserFeature,
            setUserProfile: setUserProfile,
            setUserTheme: setUserTheme
        };

        function login(company, username, password, rememberMe) {
            AuthTokenFactory.setBucket(company);
            var params = {
                "user-name": username,
                "password": password,
                "remember-me": rememberMe
            };
            var onSuccess = function(response) {
                AuthTokenFactory.setSessionKey(response['session-key']);
                AuthTokenFactory.setUserId(response['user-id']);
                setUserTheme();
                setUserProfile();
                setUserRole();
            };
            var onFail = function(response) {};
            var onError = function() {};
            return ajaxPost('login', params, onSuccess, onFail, onError, false);
        }

        function setUserTheme() {
            var params = {};
            var onSuccess = function(response) {
                var theme = response.prefs.theme ? response.prefs.theme : KupOption.default.theme;
                AuthTokenFactory.setTheme(theme);
            };
            var onFail = function() {};
            var onError = function() {};
            return ajaxPost('getuserprefs', params, onSuccess, onFail, onError);
        }

        function setUserProfile() {
            var params = {};
            var onSuccess = function(response) {
                var data = {
                    name: response['user-name'],
                    email: response.email,
                    language: response.language ? response.language : KupOption.default.language
                };
                AuthTokenFactory.setUserProfile(data);
            };
            var onFail = function(response) {};
            var onError = function() {};
            return ajaxPost('getuserprofile', params, onSuccess, onFail, onError, false);
        }

        function logout() {
            var onSuccess = function(response) {
                AuthTokenFactory.clearStore();
            };
            var onFail = function(response) {};
            var onError = function() {};
            return ajaxPost('logout', {}, onSuccess, onFail, onError);
        }

        function forgotPassword(rootUrl, company, username, email) {
            var apiUrl = rootUrl + '/api';
            return $http.post(apiUrl + '/forgotpassword',
                $.param({
                    "bucket": company,
                    "user-name": username,
                    "email": email,
                }), {
                    headers: {
                        'Accept': '*/*',
                        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                }
            );
        }

        function setUserFeature() {
            var param = {};
            var onSuccess = function(response) {
                var features = response.features || [];
                data.apiUserFeature = features;
            };
            var onFail = function(response) {
                data.apiUserFeature = [];
            };
            var onError = function() {
                data.apiUserFeature = [];
            };
            return ajaxPost('getuserfeatures', param, onSuccess, onFail, onError, false);
        }

        function getUserFeature() {
            return data.apiUserFeature;
        }

        function setUserRole() {
            var params = {
                "user-id": AuthTokenFactory.getUserId()
            };
            var onSuccess = function(response) {
                AuthTokenFactory.setUserRole(response.roles);
            };
            var onFail = function(response) {
                AuthTokenFactory.setUserRole('');
            };
            var onError = function() {
                AuthTokenFactory.setUserRole('');
            };
            return ajaxPost('getuserrolesbyuserid', params, onSuccess, onFail, onError, false);
        }
    });

angular
    .module('kai.common')
    .factory("UtilsService", function(KupOption, notify, $filter, $rootScope, AuthTokenFactory, $uibModal) {
        var kupOpt = KupOption;
        //new UI utils
        var utils = {
            //angular plugin utils
            i18n: i18n,
            notification: notification,
            block: block,
            blockPage: blockPage,
            getAngularDatatablesI18n: getAngularDatatablesI18n,

            //common js utils
            localToUTC: localToUTC,
            UTCToLocal: UTCToLocal,
            getFullScreenElement: getFullScreenElement,
            getTimezoneOffset: getTimezoneOffset,
            getDateDifference: getDateDifference,
            urlDownload: urlDownload,
            popupConfirm: popupConfirm

        };

        //old platform utils fn, later to remove
        var alertMsgTimeOut = null;
        var infoMsgTimeOut = null;
        var activePopupId = "";
        var flag = 0;
        var flag1 = 0;
        var loadingTimer = null;

        utils.throwServerError = function(responseData) {
            utils.hideLoadingOverlay();

            var errorMsg = "";
            var redirect = false;

            if (responseData == null || responseData.reason == null) {
                console.warn("Server error! responseData:" + JSON.stringify(responseData));
                return;
            } else {
                if (responseData.reason == "timeout") {
                    errorMsg = localizeResource("session-expired") + "!";
                    redirect = true;
                } else {
                    errorMsg = localizeResource(responseData.reason);
                }
            }

            utils.popupAlert(errorMsg, function() {
                if (redirect) {
                    if (document.getElementById("iframePopup"))
                        window.parent.location.href = "/" + kupBucket;
                    else
                        window.location.href = "/" + kupBucket;
                }
            });
        }

        utils.popupAlert = function(alertMsg, callback) {

            var contentHtml = '<div style="min-height:40px">' + alertMsg + '</div>';

            var kendoWindow = $("<div />").kendoWindow({
                visible: false,
                title: localizeResource("system-message"),
                resizable: false,
                modal: true,
                width: 300,
                close: callback
            });

            kendoWindow.data("kendoWindow").content(contentHtml).center().open();
            $(".k-window-title").css("height", "30px");
        }

        utils.detectBrowser = function() {
            function testCSS(prop) {
                return prop in document.documentElement.style;
            }

            var isOpera = !!(window.opera && window.opera.version);
            var isFirefox = testCSS('MozBoxSizing');
            var isSafari = Object.prototype.toString.call(window.HTMLElement).indexOf('Constructor') > 0;
            var isChrome = !isSafari && testCSS('WebkitTransform');
            var isIE = /* @cc_on!@ */ false || testCSS('msTransform');

            var browser = "";
            if (isOpera)
                browser = "opera";
            else if (isFirefox)
                browser = "firefox";
            else if (isSafari)
                browser = "safari";
            else if (isChrome)
                browser = "chrome";
            else if (isIE)
                browser = "ie";

            return browser;
        }

        utils.detectOS = function() {
            var OSName = "Unknown OS";
            if (navigator.appVersion.indexOf("Win") != -1)
                OSName = "Windows";
            if (navigator.appVersion.indexOf("Mac") != -1)
                OSName = "MacOS";
            if (navigator.appVersion.indexOf("X11") != -1)
                OSName = "UNIX";
            if (navigator.appVersion.indexOf("Linux") != -1)
                OSName = "Linux";
            return OSName;
        }

        utils.convertToUTC = function(dateObj) {
            return new Date(dateObj.getUTCFullYear(),
                dateObj.getUTCMonth(),
                dateObj.getUTCDate(),
                dateObj.getUTCHours(),
                dateObj.getUTCMinutes(),
                dateObj.getUTCSeconds());
        }

        // utils.convertToHourBoundriesUTC = function(dateObj, type) {
        //     if (type == "from") {
        //         dateObj.setUTCMinutes(00);
        //         dateObj.setUTCSeconds(00);
        //         return new Date(dateObj.getUTCFullYear(),
        //             dateObj.getUTCMonth(),
        //             dateObj.getUTCDate(),
        //             dateObj.getUTCHours(),
        //             dateObj.getUTCMinutes(),
        //             dateObj.getUTCSeconds());
        //     } else if (type == "to") {
        //         if (dateObj.getUTCMinutes() > 0) {
        //             dateObj.setUTCHours(dateObj.getUTCHours() + 1);
        //             dateObj.setUTCMinutes(00);
        //             dateObj.setUTCSeconds(00);
        //             return new Date(dateObj.getUTCFullYear(),
        //                 dateObj.getUTCMonth(),
        //                 dateObj.getUTCDate(),
        //                 dateObj.getUTCHours(),
        //                 dateObj.getUTCMinutes(),
        //                 dateObj.getUTCSeconds());
        //         } else {
        //             dateObj.setUTCMinutes(00);
        //             dateObj.setUTCSeconds(00);
        //             return new Date(dateObj.getUTCFullYear(),
        //                 dateObj.getUTCMonth(),
        //                 dateObj.getUTCDate(),
        //                 dateObj.getUTCHours(),
        //                 dateObj.getUTCMinutes(),
        //                 dateObj.getUTCSeconds());
        //         }
        //     }
        // }

        utils.convertUTCtoLocal = function(dateObj) {
            if (dateObj != null) {
                var d = new Date();
                dateObj.setMinutes(dateObj.getMinutes() - d.getTimezoneOffset());
                return dateObj;
            }
        }

        utils.slideDownAlert = function(timestamp, alertType, deviceName) {

            var localDt = utils.convertUTCtoLocal(kendo.parseDate(timestamp, kupapi.TIME_FORMAT));
            timestamp = kendo.toString(localDt, kupapi.TIME_FORMAT);

            var msgHtml = '<div class="alert_box_title">' + timestamp + '</div>' + '<div class="alert_box_content">' + '<label class="alert_box_label">' + localizeResource('device-name') + '</label>: ' + deviceName + '<br/>' + '<label class="alert_box_label">' + localizeResource('event-type') + '</label>: ' + localizeResource(alertType) + '<br/>' + '</div>';

            var notifyBox = document.getElementById("notifyBox") ? $("#notifyBox") : window.parent.$("#notifyBox");
            var alertBox = document.getElementById("alertBox") ? $("#alertBox") : window.parent.$("#alertBox");

            clearTimeout(alertMsgTimeOut);
            clearTimeout(infoMsgTimeOut);
            notifyBox.hide();

            alertBox.html(msgHtml);
            alertBox.show("slide", {
                direction: "up"
            }, 1000);
            alertMsgTimeOut = setTimeout(function() {
                alertBox.hide("slide", {
                    direction: "up"
                }, 1000);
            }, 8000);
        }

        utils.slideDownInfo = function(notifyMsg) {

            var msgHtml = '<div class="success_image"><span>' + notifyMsg + '</span></div>';

            var notifyBox = null;
            var alertBox = null;

            if (document.getElementById("notifyBox")) {
                notifyBox = $("#notifyBox");
                alertBox = $("#alertBox");

                clearTimeout(alertMsgTimeOut);
                clearTimeout(infoMsgTimeOut);
                alertBox.hide();
                notifyBox.html(msgHtml);
                notifyBox.fadeIn(1000);

                infoMsgTimeOut = setTimeout(function() {
                    notifyBox.fadeOut(1000);
                }, 3000);
            } else {
                notifyBox = window.parent.$("#notifyBox");
                alertBox = window.parent.$("#alertBox");

                clearTimeout(window.parent.alertMsgTimeOut);
                clearTimeout(window.parent.infoMsgTimeOut);
                alertBox.hide();
                notifyBox.html(msgHtml);
                notifyBox.fadeIn(1000);

                window.parent.infoMsgTimeOut = window.parent.setTimeout(function() {
                    notifyBox.fadeOut(1000);
                }, 3000);
            }
        }

        utils.removeLineBreaks = function(nlStr) {
            return nlStr.replace(new RegExp('\n', 'g'), ' ');
        }

        // converts string to json
        utils.getJSonObject = function(value) {
            return $.parseJSON(value.replace(/&quot;/ig, '"'));
        }

        utils.showLoadingOverlay = function() {
            $(".darkened-overlay").show();
            $(".loading-icon-overlay").show();
            kendo.ui.progress($(".k-window-content"), true);
        }

        utils.showLoadingTextOverlay = function(displayText, showCloseButton) {
            $("#loadingText").html(displayText);
            $(".darkened-overlay").show();
            $(".loading-text-overlay").show();

            // count the time taken for close button
            if (showCloseButton) {
                $("#loadingTimeSeconds").html("");
                $("#loadingTimeSeconds").show();
                var timeTaken = 0;

                loadingTimer = setInterval(function() {
                    timeTaken++;
                    $("#loadingTimeSeconds").html(timeTaken + "s");

                    if (timeTaken > 10) {
                        $("#btnLoadingCancel").show();
                    }
                }, 1000);
            }
        }

        utils.hideLoadingOverlay = function() {
            $(".darkened-overlay").hide();
            $(".loading-icon-overlay").hide();
            $(".loading-text-overlay").hide();
            $("#btnLoadingCancel").hide();
            $("#loadingTimeSeconds").hide();
            kendo.ui.progress($(".k-window-content"), false);

            if (loadingTimer) {
                clearInterval(loadingTimer);
            }
        }

        utils.loadIframe = function(iframeName, url) {
            var $iframe = $('#' + iframeName);
            if ($iframe.length) {
                $iframe.attr('src', url);
                return false;
            }
        }

        utils.preloadImage = function(imgUrl, onSuccess, onFailure) {
            var img = new Image();
            utils.showLoadingTextOverlay(localizeResource("loading-image"), true);
            $(img).load(function() {
                utils.hideLoadingOverlay();
                onSuccess();
            }).error(function() {
                utils.hideLoadingOverlay();
                onFailure();
            }).attr('src', imgUrl);
        }

        // divId can be id or class name
        utils.createTooltip = function(divId, position, content) {
            var contentHtml = "<span style='font-size:11px;text-align:left; display: inline-block; max-width:200px'>" + content + "</span>"

            var domElement;
            if (document.getElementById(divId))
                domElement = $("#" + divId);
            else
                domElement = $("." + divId);

            domElement.kendoTooltip({
                position: position,
                content: contentHtml
            }).data("kendoTooltip");
        }

        utils.createDateTimeRangeSelection = function(fromDivId, toDivId) {

            var today = new Date();

            var start = $("#" + fromDivId).kendoDateTimePicker({
                interval: 60,
                format: "dd/MM/yyyy HH:mm",
                timeFormat: "HH:mm",
                value: kendo.toString(today, "dd/MM/yyyy") + " 00:00",
                change: function(e) {
                    var startDate = start.value();
                    if (utils.isNullOrEmpty(startDate))
                        start.value(kendo.toString(today, "dd/MM/yyyy") + " 00:00");
                    if (startDate) {
                        startDate = new Date(startDate);
                        startDate.setDate(startDate.getDate());
                        end.min(startDate);
                    }
                }
            }).data("kendoDateTimePicker");

            var end = $("#" + toDivId).kendoDateTimePicker({
                interval: 60,
                format: "dd/MM/yyyy HH:mm",
                timeFormat: "HH:mm",
                value: kendo.toString(today, "dd/MM/yyyy") + " 23:59",
                change: function(e) {
                    var endDate = end.value();
                    if (utils.isNullOrEmpty(endDate))
                        end.value(kendo.toString(today, "dd/MM/yyyy") + " 23:59");
                    if (endDate) {
                        endDate = new Date(endDate);
                        endDate.setDate(endDate.getDate());
                        endDate.setHours(endDate.getHours());
                        endDate.setMinutes(endDate.getMinutes());
                        start.max(endDate);
                    }
                }
            }).data("kendoDateTimePicker");

            start.max(end.value());
            end.min(start.value());
        }

        utils.checkInternetAccess = function(onlineCallback, offlineCallback) {
            var img = document.createElement("img");
            img.onload = onlineCallback;
            img.onerror = offlineCallback;
            img.src = "http://pic2.pbsrc.com/navigation/brand-logo.png?v=" + Math.random();
        }

        utils.getRandomInteger = function(min, max) {
            return Math.floor((Math.random() * max) + min);
        }

        utils.getRandomDecimal = function(min, max) {
            return (Math.random() * max) + min;
        }

        utils.getScript = function(src) {
            document.write('<script type="text/javascript" src="' + src + '"></script>');
        }

        utils.captureEnterKey = function(divId, callback) {

            $("#" + divId).bind('keypress', function(e) {
                var code = (e.keyCode ? e.keyCode : e.which);
                if (code == 13) { // ENTER keycode
                    callback();
                }
            });
        }

        utils.httpGet = function(theUrl) {
            var xmlHttp = null;
            xmlHttp = new XMLHttpRequest();
            xmlHttp.open("GET", theUrl, false);
            xmlHttp.send(null);
            return xmlHttp.responseText;
        }

        // width and height fields are optional, set null to auto-resize based on
        // content
        utils.openPopup = function(title, contentUrl, width, height, isModel, onPopupClosed) {
            var winId = Math.random().toString(8).slice(2);
            $(document.body).append('<div id="' + winId + '" style="overflow: hidden"></div>');

            function onClosed(e) {
                $(".k-animation-container").css("display", "none");
                $(".k-list-container").css("display", "none");
                $(".pac-container").css("display", "none");
                $(".k-list-container").css("transform", "");
                $("#" + winId).html("");
                onPopupClosed(e);
            }

            var winOptions = {};
            winOptions.title = title;
            winOptions.content = "/" + kupBucket + contentUrl;
            winOptions.resizable = false;
            winOptions.modal = true;
            winOptions.close = onClosed;
            winOptions.visible = false;

            if (!utils.isNullOrEmpty(width))
                winOptions.width = width;

            if (!utils.isNullOrEmpty(height))
                winOptions.height = height;

            var autoAdjust = utils.isNullOrEmpty(width) || utils.isNullOrEmpty(height);
            if (autoAdjust) {
                winOptions.refresh = function() {
                    this.center();
                }
            }

            var kendoWin = $("#" + winId).kendoWindow(winOptions).data("kendoWindow").center().open();
        }

        utils.formatDrmsDate = function(datetime) {
            // datetime=parse_datetime(datetime);
            var dateObj = new Date(datetime);
            var hr = dateObj.getHours();
            var min = dateObj.getMinutes();
            if (hr < 10)
                hr = "0" + hr;
            if (min < 10)
                min = "0" + min;
            var dateStr = hr + ":" + min;
            return dateStr;
        }

        utils.formatDate = function(date) {
            if (date != "") {
                var tempDate = date.split(" ");
                var tempTime = tempDate[3].split(":");
                if (tempDate[1] == 'Jan')
                    tempDate[1] = '01';
                else if (tempDate[1] == 'Feb')
                    tempDate[1] = '02';
                else if (tempDate[1] == 'Mar')
                    tempDate[1] = '03';
                else if (tempDate[1] == 'Apr')
                    tempDate[1] = '04';
                else if (tempDate[1] == 'May')
                    tempDate[1] = '05';
                else if (tempDate[1] == 'Jun')
                    tempDate[1] = '06';
                else if (tempDate[1] == 'Jul')
                    tempDate[1] = '07';
                else if (tempDate[1] == 'Aug')
                    tempDate[1] = '08';
                else if (tempDate[1] == 'Sep')
                    tempDate[1] = '09';
                else if (tempDate[1] == 'Oct')
                    tempDate[1] = '10';
                else if (tempDate[1] == 'Nov')
                    tempDate[1] = '11';
                else if (tempDate[1] == 'Dec')
                    tempDate[1] = '12';
                var formatedDate = tempDate[2] + tempDate[1] + tempDate[5] + tempTime[0] + tempTime[1] + tempTime[2];
                return formatedDate;
            }
            return "";
        }
        utils.convertDrmsDate = function(timeType, date, range) {
            var tempDate = null;
            tempDate = date;
            if (range == "start") {
                if (timeType == "daily") {
                    return tempDate;
                } else if (timeType == "weekly") {
                    return (new Date(tempDate - (tempDate.getDay() * 86400000)));
                } else if (timeType == "monthly") {
                    tempDate.setMonth(tempDate.getMonth(), 1);
                    return tempDate;
                } else if (timeType == "yearly") {
                    tempDate.setMonth(0, 1);
                    return tempDate;
                }
            } else if (range == "end") {
                if (timeType == "daily") {
                    tempDate.setHours(23, 59, 59);
                    return tempDate;
                } else if (timeType == "weekly") {
                    tempDate.setDate(tempDate.getDate() + (6 - tempDate.getDay()));
                    tempDate.setHours(23, 59, 59);
                    return tempDate;
                } else if (timeType == "monthly") {
                    tempDate.setMonth(tempDate.getMonth() + 1, 0);
                    return tempDate;
                } else if (timeType == "yearly") {
                    tempDate.setMonth(11, 31);
                    tempDate.setHours(23, 59, 59);
                    return tempDate;
                }
            }
        }

        utils.populateChannels = function(channelCount, divId) {
            var channelListId = "#" + divId;
            var channList = [];
            var dataSource = null;
            for (var i = 0; i < channelCount; i++) {
                var value = i + 1;
                channList.push({
                    "name": value,
                    "nodeCoreDeviceId": i
                });
            }
            if (channList.length > 0) {
                dataSource = new kendo.data.DataSource({
                    data: channList
                });
                $(channelListId).data("kendoDropDownList").setDataSource(dataSource);
            } else {
                dataSource = new kendo.data.DataSource({
                    data: []
                });
                $(channelListId).data("kendoDropDownList").setDataSource(dataSource);
                $(channelListId).data("kendoDropDownList").element.val("");
                $(channelListId).data("kendoDropDownList").text("");
            }
        }

        utils.populateNodeNames = function(nodeList, divId) {
            var channelListId = "#" + divId;
            var dataSource = null;
            if (nodeList.length > 0) {
                dataSource = new kendo.data.DataSource({
                    data: nodeList
                });
                $(channelListId).data("kendoDropDownList").setDataSource(dataSource);
            } else {
                dataSource = new kendo.data.DataSource({
                    data: []
                });
                $(channelListId).data("kendoDropDownList").setDataSource(dataSource);
                $(channelListId).data("kendoDropDownList").element.val("");
                $(channelListId).data("kendoDropDownList").text("");
            }
        }

        utils.collapseAllRows = function(gridId) {
            var grid = $('#' + gridId).data('kendoGrid');
            grid.tbody.find('>tr.k-grouping-row').each(function(e) {
                grid.collapseRow(this);
            });
        }

        utils.expandAllRows = function(gridId) {
            var grid = $('#' + gridId).data('kendoGrid');
            grid.tbody.find('>tr.k-grouping-row').each(function(e) {
                grid.expandRow(this);
            });
        }

        utils.isNullOrEmpty = function(str) {
            return str == null || str == "";
        }

        /**
         *
         * @param presetCompanyId
         *            optional, if specified, companyId input will be set and hidden
         *            from user
         * @param allowOTP
         *            (true/false) allow OTP login option
         * @param callback
         *            Upon login success, sessionKey will be passed in
         *            callback(sessionKey), empty string on cancellation
         */
        utils.popupAuthentication = function(presetCompanyId, allowOTP, callback) {

            var presetInfo = {
                "companyId": presetCompanyId,
                "allowOTP": allowOTP
            };
            var url = "/login/authenticate/" + JSON.stringify(presetInfo);

            utils.openPopup(localizeResource("authentication"), url, 335, null, true, function() {
                callback(authWin.sessionKeyResult);
            });
        }

        utils.centerDivObject = function(jqDivObject) {
            var winWidth = $(window).width();
            var winHeight = $(window).height();

            var divWidth = jqDivObject.width();
            var divHeight = jqDivObject.height();

            var top = (winHeight - divHeight) / 2;
            var left = (winWidth - divWidth) / 2;

            jqDivObject.css("position", "absolute");
            jqDivObject.css("top", top);
            jqDivObject.css("left", left);
        }

        utils.sameday = function(date1, date2) {
            return date1.getUTCFullYear() == date2.getUTCFullYear() && date1.getUTCMonth() == date2.getUTCMonth() && date1.getUTCDate() == date2.getUTCDate();
        }

        utils.tryParseJson = function(str) {
            try {
                var ret = JSON.parse(str);
                if (ret == "")
                    ret = {};
                return ret;
            } catch (e) {
                return {};
            }
        }

        utils.getMapSize = function(map) {
            return Object.keys(map).length;
        };

        utils.modulo = function(number, divisor) {
            return number % divisor;
        }

        utils.checkFlashPlayer = function(browser) {
            if (browser == 'ie') {
                try {
                    new ActiveXObject("ShockwaveFlash.ShockwaveFlash");
                } catch (e) {
                    return false;
                }
            }

            if (browser == 'firefox' || browser == 'chrome' || browser == 'opera') {
                var isExistFlashPlayer = navigator.plugins["Shockwave Flash"];
                if (!isExistFlashPlayer) {
                    return false;
                }
            }

            return true;
        }

        utils.viewSnapshot = function(coreDeviceId, channelId) {
            var contentPage = "/device/viewsnapshot/" + coreDeviceId + "-" + channelId;
            utils.openPopup(localizeResource("view-snapshot"), contentPage, null, null, true, function() {
                utils.hideLoadingOverlay();
            });
        }

        utils.checkDeviceCompleteInfo = function(device) {

            if (device.model.capabilities.indexOf("node") != -1 && device.node === undefined) { //Whether is a node checking in the kup cloud
                console.log(device.name + " doesnot have node properties");
                return false;
            } else if (device.model.capabilities.indexOf("node") === -1 && device.model.channels < 1) { //Whether is a camera checking in the node
                console.log(device.name + " doesnot have channel on it");
                return false;
            }
            return true;
        }

        utils.formatSerialNumber = function(serialNumber) {
            var dashedNumber = "";
            var groupSize = 5;

            for (var i = 0; i < serialNumber.length; i++) {
                dashedNumber += serialNumber[i];
                if (serialNumber.length != (i + 1) && utils.modulo(i + 1, groupSize) == 0) {
                    dashedNumber += " - ";
                }
            }

            return dashedNumber;
        }

        utils.removeArrayEntry = function(value, Array) {
            var index = $.inArray(value, Array);
            if (~index) {
                Array.splice(index, 1);
            }
        }

        utils.isValidEmail = function isValidEmailAddress(emailAddress) {
            var pattern = new RegExp(/^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i);
            return pattern.test(emailAddress);
        };

        utils.convertToCurrency = function(value) {
            if (!isNaN(value)) {
                var number = parseFloat(value);
                if (number >= 1000000000)
                    return (number / 1000000000) + "BN"
                else if (number >= 1000000)
                    return (number / 100000) + "M"
                else if (number >= 1000)
                    return (number / 1000) + "K"
                else
                    return value;
            } else
                return value;
        }

        utils.getLatLngByAddress = function(address, callback, onError) {
            reverseGeocode(address, function(responseData) {
                if (responseData.result != "ok") {
                    onError(responseData);
                    return;
                }
                var location = {
                    lng: responseData.lng,
                    lat: responseData.lat
                }
                callback(location);
            }, onError);
        }

        utils.getDateDifference = function(date1, date2) {
            // Convert both dates to milliseconds
            var date1Millis = date1.getTime();
            var date2Millis = date2.getTime();
            var differenceObject = {};
            // Calculate the difference in milliseconds
            var differenceMillis = (date2Millis > date1Millis) ? (date2Millis - date1Millis) : (date1Millis - date2Millis);
            //get units
            differenceObject.days = Math.floor((differenceMillis / (60 * 60 * 1000)) / 24);
            differenceObject.hours = Math.floor((differenceMillis / (60 * 60 * 1000)) % 24);
            differenceObject.minutes = Math.floor((differenceMillis / (60 * 1000)) % 60);
            differenceObject.seconds = Math.floor((differenceMillis / 1000) % 24);
            //object that contains all the date time units. 0 if doesnot exist
            return differenceObject;
        }

        utils.combineDeviceChannelIDs = function(deviceId, channelId) {
            if (!utils.isNullOrEmpty(deviceId) && !utils.isNullOrEmpty(channelId))
                return deviceId + "-" + channelId;
        }

        utils.isValidDir = function(directoryString) {
            var pattern = new RegExp(/^[0-9a-zA-Z\/\-_\s]+$/i);
            return pattern.test(directoryString);
        }

        /**
         * returns true if array2 contains all the value of array1
         * returns  false if array2 doesnot contains all the value of array1
         *
         */
        utils.containsAll = function(array1, array2) {
                for (var i = 0, len = array1.length; i < len; i++) {
                    if ($.inArray(array1[i], array2) == -1) return false;
                }
                return true;
            }
            /**
             * returns true if array2 contains any one of the value of array1
             * returns  false if array2 doesnot contains all of the value of array1
             *
             */
        utils.containsAny = function(array1, array2) {
            for (var i = 0, len = array1.length; i < len; i++) {
                if ($.inArray(array1[i], array2) !== -1) return true;
            }
            return false;
        }

        utils.permutate = function(list, length) {
            // Copy initial values as arrays
            var perm = list.map(function(val) {
                return [val];
            });
            // permutation generator
            var generate = function(perm, maxLen, currLen) {
                // Reached desired length
                if (currLen === maxLen) {
                    return perm;
                }
                // For each existing permutation
                for (var i = 0, len = perm.length; i < len; i++) {
                    var currPerm = perm.shift();
                    // Create new permutation
                    for (var k = 0; k < list.length; k++) {
                        perm.push(currPerm.concat(list[k]));
                    }
                }
                // Recurse
                return generate(perm, maxLen, currLen + 1);
            };

            // Start with size 1 because of initial values
            return generate(perm, length, 1);
        }

        utils.requestUserInput = function(inputName, callback) {
            var saved = false;
            var inputText = "";
            var contentHtml = $(".input_request_win").html();

            var kendoWindow = $(".input_request_win").kendoWindow({
                visible: false,
                title: inputName,
                resizable: false,
                modal: true,
                width: 200,
                close: function() {
                    callback(saved, inputText);
                }
            });
            kendoWindow.data("kendoWindow").content(contentHtml).center().open();

            var $inputBox = $(".input_request_win input[name=userInput]");

            $(".input_request_win .btn_save").click(function() {
                inputText = $inputBox.val();
                if (utils.isNullOrEmpty(inputText)) {
                    return;
                }

                saved = true;
                $(".input_request_win").data("kendoWindow").close();
            });

            $(".input_request_win .btn_cancel").click(function() {
                saved = false;
                $(".input_request_win").data("kendoWindow").close();
            });
        }

        utils.isListEmpty = function(list) {
            return list == null || list.length == 0;
        };

        /**
         * checks if the user is using another computer to access node UI
         *
         */
        utils.browsingOnNode = function() {
            var hostname = window.location.hostname;
            return (hostname == "localhost" || hostname == "127.0.0.1");
        }

        utils.toAPITimestamp = function(utcDate) {
            return kendo.toString(utcDate, kupapi.API_TIME_FORMAT);
        };

        utils.getLocalTimestamp = function(millis) {
            var localDt = new Date(millis);
            return kendo.toString(localDt, kupapi.TIME_FORMAT);
        };

        utils.getUTCTimestamp = function(millis) {
            var localDt = new Date(millis);
            var utcDt = utils.convertToUTC(localDt);
            return kendo.toString(utcDt, kupapi.TIME_FORMAT);
        };

        utils.replaceAll = function(str, find, replace) {
            return str.replace(new RegExp(find, 'g'), replace);
        }


        return utils;

        /*******************************************************************************
         *
         *  Angular plugin utils Function Definition
         *
         *******************************************************************************/

        /**
         * [i18n description]
         * @Fixme : only parse single char formatters eg. %s
         * @param  {string} message want to translate text
         * @return {object}         reurn 'angular-translate' plugin object
         */
        function i18n(message) {
            //return $filter('translate')(message);
            var message = $filter('translate')(message);
            if (arguments.length > 1) {
                // Explicit ordered parameters
                for (var i = 1; i < arguments.length; i++) {
                    var r = new RegExp("%" + i + "\\$\\w", "g");
                    message = message.replace(r, arguments[i]);
                }
                // Standard ordered parameters
                for (var i = 1; i < arguments.length; i++) {
                    message = message.replace(/%\w/, arguments[i]);
                }
            }
            // Decode encoded %% to single %
            message = message.replace("\0%\0", "%");
            // Imbricated messages
            var imbricated = message.match(/&\{.*?\}/g);
            if (imbricated) {
                for (var i = 0; i < imbricated.length; i++) {
                    var imbricated_code = imbricated[i].substring(2, imbricated[i].length - 1).replace(/^\s*(.*?)\s*$/, "$1");
                    message = message.replace(imbricated[i], i18nMessages[imbricated_code] || "");
                }
            }
            return message;
        }

        /**
         * [notification description]
         * @param  {string} type    success/error/warning
         * @param  {string} message show message,support html tag
         * @param  {number} delay   auto hide timer,is ms
         * @return {object}         return angular-notify object
         */
        function notification(type, message, delay) {
            delay = delay || 3000;
            var typeList = [{
                name: 'success',
                messageClass: 'fa fa-check-circle',
                notifyClass: 'alert-success'
            }, {
                name: 'error',
                messageClass: 'fa fa-times-circle',
                notifyClass: 'alert-danger'
            }, {
                name: 'warning',
                messageClass: 'fa fa-exclamation-circle',
                notifyClass: 'alert-warning'
            }, {
                name: 'info',
                messageClass: 'fa fa-info-circle',
                notifyClass: 'alert-info'
            }];
            var typeInfo = (function() {
                var typeInfo = typeList[0];
                jQuery.each(typeList, function(i, data) {
                    if (type === data.name) {
                        typeInfo = data;
                        return false;
                    }
                });
                return typeInfo
            })();

            return notify({
                messageTemplate: '<span class="' + typeInfo.messageClass + '"></span><span>' + message + '</span>',
                classes: typeInfo.notifyClass,
                duration: delay,
                position: 'center'
            });
        }

        function block(promise, type) {
            type = type || "component";

            var message = i18n('loading');
            var typeList = [{
                name: 'login',
                message: message,
                backdrop: false,
                templateUrl: 'app/widgets/loading-login.tmpl.html',
                delay: 300,
                minDuration: 300
            }, {
                name: 'container',
                message: message,
                backdrop: false,
                templateUrl: 'app/widgets/loading.tmpl.html',
                delay: 300,
                minDuration: 300
            }, {
                name: 'component',
                message: message,
                backdrop: false,
                //templateUrl: 'app/widgets/loading.tmpl.html',
                delay: 300,
                minDuration: 300
            }];

            var typeInfo = (function() {
                var typeInfo = typeList[0];
                jQuery.each(typeList, function(i, data) {
                    if (type === data.name) {
                        typeInfo = data;
                        return false;
                    }
                });
                return typeInfo
            })();

            var config = {
                promise: promise,
                message: typeInfo.message,
                backdrop: typeInfo.backdrop,
                templateUrl: typeInfo.templateUrl,
                //delay: typeInfo.delay,
                //minDuration: typeInfo.minDuration
            };
            return config;
        }

        function blockPage(promise) {
            return block(promise, 'container');
        }

        function getAngularDatatablesI18n() {
            return {
                "sEmptyTable": i18n("no-data-available-in-table"),
                "sInfo": i18n("showing-_START_-to-_END_-of-_TOTAL_-entries"),
                "sInfoEmpty": i18n("showing-0-to-0-of-0-entries"),
                "sInfoFiltered": "( " + i18n("filtered-from-_MAX_-total-entries") + " )",
                "sInfoPostFix": "",
                "sInfoThousands": ",",
                "sLengthMenu": i18n("show-_MENU_-entries"),
                "sLoadingRecords": i18n("loading") + "...",
                "sProcessing": '<div class="kupLoading" style="height:100%;background-color: rgba(0, 0, 0, 0.2);"><div class="la-ball-beat kupLoadItem la-yellow"><div></div><div></div><div></div></div></div>',
                "sSearch": i18n("search") + ":",
                "sZeroRecords": i18n("no-matching-records-found"),
                "oPaginate": {
                    "sFirst": i18n("first"),
                    "sLast": i18n("last"),
                    "sNext": i18n("next"),
                    "sPrevious": i18n("previous")
                },
                "oAria": {
                    "sSortAscending": ": " + i18n("activate-to-sort-column-ascending"),
                    "sSortDescending": ": " + i18n("activate-to-sort-column-descending")
                }
            };
        }

        /*******************************************************************************
         *
         *  common  utils Function Definition
         *
         *******************************************************************************/

        function popupConfirm(title, confirmMsg, callback) {
            var ctrl = function($scope, $uibModalInstance) {
                $scope.title = title;
                $scope.text = confirmMsg;
                $scope.cancel = function() {
                    $uibModalInstance.dismiss('cancel');
                };
                $scope.yes = function() {
                    $.isFunction(callback)? callback() : null;
                    $scope.cancel();
                }
            };
            var modalInstance = $uibModal.open({
                animation: true,
                templateUrl: 'confirmModal.html',
                backdrop: 'static',
                windowClass: 'modal kupModal',
                controller: ctrl
            });
        };

        function localToUTC(dateOrTimestamp) {
            var dateObj = new Date(dateOrTimestamp);
            var dateUTC = new Date(dateObj.getUTCFullYear(),
                dateObj.getUTCMonth(),
                dateObj.getUTCDate(),
                dateObj.getUTCHours(),
                dateObj.getUTCMinutes(),
                dateObj.getUTCSeconds());
            return dateUTC;
        };

        function UTCToLocal(dateOrTimestamp) {
            var date = moment(dateOrTimestamp).format("YYYY-MM-DD HH:mm:ss");
            date = moment.utc(date).local();
            date = date.toDate();
            return date;
            // var dateObj = new Date(dateOrTimestamp);
            // var dateLocal = new Date(dateObj + " UTC");
            // return dateLocal;
        }

        function getFullScreenElement() {
            return document.fullscreenElement ||
                document.mozFullScreenElement || document.webkitFullscreenElement || document.msFullscreenElement || '';
        }

        function getTimezoneOffset() {
            return new Date().getTimezoneOffset() * (-1);
        }

        function getDateDifference(date1, date2) {
            // Convert both dates to milliseconds
            var date1Millis = date1.getTime();
            var date2Millis = date2.getTime();
            var differenceObject = {};
            // Calculate the difference in milliseconds
            var differenceMillis = (date2Millis > date1Millis) ? (date2Millis - date1Millis) : (date1Millis - date2Millis);
            //get units
            differenceObject.days = Math.floor((differenceMillis / (60 * 60 * 1000)) / 24);
            differenceObject.hours = Math.floor((differenceMillis / (60 * 60 * 1000)) % 24);
            differenceObject.minutes = Math.floor((differenceMillis / (60 * 1000)) % 60);
            differenceObject.seconds = Math.floor((differenceMillis / 1000) % 24);
            //object that contains all the date time units. 0 if doesnot exist
            return differenceObject;
        };

        /**
         * [urlDownload description]
         * @example: http://pixelscommander.com/en/javascript/javascript-file-download-ignore-content-type/
         * @example: http://pixelscommander.com/polygon/downloadjs/#.VvS_2eJ97IW
         */
        function urlDownload(url) {
            var sUrl = kupOpt.sysApiRootUrl + url;

            var isChrome = navigator.userAgent.toLowerCase().indexOf('chrome') > -1;
            var isSafari = navigator.userAgent.toLowerCase().indexOf('safari') > -1;

            //iOS devices do not support downloading. We have to inform user about this.
            if (/(iP)/g.test(navigator.userAgent)) {
                notification('error', i18n('device-not-support-file-download'));
                return false;
            }

            //If in Chrome or Safari - download via virtual link click
            if (isChrome || isSafari) {
                //Creating new link node.
                var link = document.createElement('a');
                link.href = sUrl;

                if (link.download !== undefined) {
                    //Set HTML5 download attribute. This will prevent file from opening if supported.
                    var fileName = sUrl.substring(sUrl.lastIndexOf('/') + 1, sUrl.length);
                    link.download = fileName;
                }

                //Dispatching click event.
                if (document.createEvent) {
                    var e = document.createEvent('MouseEvents');
                    e.initEvent('click', true, true);
                    link.dispatchEvent(e);
                    return true;
                }
            }

            // Force file download (whether supported by server).
            //var query = '?download';
            window.open(sUrl, '_self');

        }

    });
