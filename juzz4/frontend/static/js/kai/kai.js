/* mapping ./main.js: shim.kai */
angular.module('kai', [
    /* 3-party module */
    'oc.lazyLoad',
    'ngAnimate',
    'ngMessages',
    'ui.router',
    'ui.bootstrap',
    'frapontillo.bootstrap-switch',
    'nya.bootstrap.select',
    'cgNotify',
    'pascalprecht.translate',
    'ws',
    'emguo.poller',
    'ui.mCustomScrollbar',
    'cgBusy',
    /* custom module */
    'kai.common',
    'angularMoment'
]);

angular.module('kai')
    .config(['$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise('/login');
        $stateProvider
            .state('login', {
                url: '/login',
                controller: 'LoginCtrl',
                controllerAs: 'loginCtrl',
                templateUrl: 'app/login/login.tmpl.html',
                resolve: {
                    loginMdl: ['$ocLazyLoad', function($ocLazyLoad) {
                        return $ocLazyLoad.load({
                            files: ['kai-login']
                        });
                    }]
                }
            })
            .state('main', {
                abstract: true,
                url: '/main',
                controller: 'MainCtrl',
                controllerAs: 'mainCtrl',
                templateUrl: 'app/main/main.tmpl.html',
                resolve: {
                    mainMdl: ['$ocLazyLoad', function($ocLazyLoad) {
                        return $ocLazyLoad.load({
                            files: ['kai-main']
                        });
                    }]
                }
            })
            /**
             * Others
             */
            .state('main.dashboard', {
                url: '/dashboard',
                controller: 'DashboardController',
                controllerAs: 'dashboardCtrl',
                templateUrl: 'app/dashboard/dashboard.tmpl.html',
                resolve: {
                    dashboardMdl: ['$ocLazyLoad', 'mainMdl', function($ocLazyLoad, mainMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-dashboard']
                        });
                    }]
                }
            })
            .state('main.profile', {
                url: '/profile',
                controller: 'ProfileController',
                controllerAs: 'profileCtrl',
                templateUrl: 'app/profile/profile.tmpl.html',
                resolve: {
                    profileMdl: ['$ocLazyLoad', 'mainMdl', function($ocLazyLoad, mainMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-profile']
                        });
                    }]
                }
            })
            /**
             * Monitoring
             */
            .state('main.liveview', {
                url: '/liveview',
                controller: 'LiveviewController',
                controllerAs: 'liveviewCtrl',
                templateUrl: 'app/liveview/liveview.tmpl.html',
                resolve: {
                    liveviewMdl: ['$ocLazyLoad', 'mainMdl', function($ocLazyLoad, mainMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-liveview']
                        });
                    }]
                }
            })
            /**
             * Recording
             */
            .state('main.cloudplayback', {
                url: '/cloudplayback',
                controller: 'CloudplaybackController',
                controllerAs: 'cloudplaybackCtrl',
                templateUrl: 'app/cloudplayback/cloudplayback.tmpl.html',
                resolve: {
                    cloudplaybackMdl: ['$ocLazyLoad', 'mainMdl', function($ocLazyLoad, mainMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-cloudplayback']
                        });
                    }]
                }
            })
            /**
             * Admin Settings
             */
            .state('main.label', {
                url: '/label',
                controller: 'LabelController',
                controllerAs: 'labelCtrl',
                templateUrl: 'app/label/label.tmpl.html',
                resolve: {
                    labelMdl: ['$ocLazyLoad', 'mainMdl', function($ocLazyLoad, mainMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-label']
                        });
                    }]
                }
            })
            .state('main.pos', {
                url: '/pos',
                controller: 'PosController',
                controllerAs: 'posCtrl',
                templateUrl: 'app/pos/pos.tmpl.html',
                resolve: {
                    posMdl: ['$ocLazyLoad', 'mainMdl', function($ocLazyLoad, mainMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-pos']
                        });
                    }]
                }
            })
            /**
             * Reports
             */
            .state('main.reports', {
                url: '/reports',
                controller: 'ReportsController',
                controllerAs: 'reportsCtrl',
                templateUrl: 'app/reports/reports.tmpl.html',
                resolve: {
                    reportsMdl: ['$ocLazyLoad', 'mainMdl', function($ocLazyLoad, mainMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-reports']
                        });
                    }]
                }
            })
            //REPORTS BUSINESS INTELLIGENCE
            .state('main.reports.traffic', {
                url: '/traffic',
                controller: 'TrafficController',
                controllerAs: 'trafficCtrl',
                templateUrl: 'app/reports/traffic/traffic.tmpl.html',
                resolve: {
                    reportsTrafficMdl: ['$ocLazyLoad', 'mainMdl', 'reportsMdl', function($ocLazyLoad, mainMdl, reportsMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-reports-traffic']
                        });
                    }]
                }
            })
            .state('main.reports.pcounting', {
                url: '/pcounting',
                controller: 'PcountingController',
                controllerAs: 'pcountingCtrl',
                templateUrl: 'app/reports/pcounting/pcounting.tmpl.html',
                resolve: {
                    reportsPcountingMdl: ['$ocLazyLoad', 'mainMdl', 'reportsMdl', function($ocLazyLoad, mainMdl, reportsMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-reports-pcounting']
                        });
                    }]
                }
            })
            .state('main.reports.crowd', {
                url: '/crowd',
                controller: 'CrowdController',
                controllerAs: 'crowdCtrl',
                templateUrl: 'app/reports/crowd/crowd.tmpl.html',
                resolve: {
                    reportsCrowdMdl: ['$ocLazyLoad', 'mainMdl', 'reportsMdl', function($ocLazyLoad, mainMdl, reportsMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-reports-crowd']
                        });
                    }]
                }
            })
            .state('main.reports.profiling', {
                url: '/profiling',
                controller: 'ProfilingController',
                controllerAs: 'profilingCtrl',
                templateUrl: 'app/reports/profiling/profiling.tmpl.html',
                resolve: {
                    reportsProfilingMdl: ['$ocLazyLoad', 'mainMdl', 'reportsMdl', function($ocLazyLoad, mainMdl, reportsMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-reports-profiling']
                        });
                    }]
                }
            })
            .state('main.reports.attention', {
                url: '/attention',
                controller: 'AttentionController',
                controllerAs: 'attentionCtrl',
                templateUrl: 'app/reports/attention/attention.tmpl.html',
                resolve: {
                    reportsAttentionMdl: ['$ocLazyLoad', 'mainMdl', 'reportsMdl', function($ocLazyLoad, mainMdl, reportsMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-reports-attention']
                        });
                    }]
                }
            })
            //REPORTS SECURITY
            .state('main.reports.perimeter', {
                url: '/perimeter',
                controller: 'PerimeterController',
                controllerAs: 'perimeterCtrl',
                templateUrl: 'app/reports/perimeter/perimeter.tmpl.html',
                resolve: {
                    reportsPerimeterMdl: ['$ocLazyLoad', 'mainMdl', 'reportsMdl', function($ocLazyLoad, mainMdl, reportsMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-reports-perimeter']
                        });
                    }]
                }
            })
            .state('main.reports.loitering', {
                url: '/loitering',
                controller: 'LoiteringController',
                controllerAs: 'loiteringCtrl',
                templateUrl: 'app/reports/loitering/loitering.tmpl.html',
                resolve: {
                    reportsLoiteringMdl: ['$ocLazyLoad', 'mainMdl', 'reportsMdl', function($ocLazyLoad, mainMdl, reportsMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-reports-loitering']
                        });
                    }]
                }
            })
            .state('main.reports.intrusion', {
                url: '/intrusion',
                controller: 'IntrusionController',
                controllerAs: 'intrusionCtrl',
                templateUrl: 'app/reports/intrusion/intrusion.tmpl.html',
                resolve: {
                    reportsIntrusionMdl: ['$ocLazyLoad', 'mainMdl', 'reportsMdl', function($ocLazyLoad, mainMdl, reportsMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-reports-intrusion']
                        });
                    }]
                }
            })
            .state('main.reports.objcounting', {
                url: '/objcounting',
                controller: 'ObjcountingController',
                controllerAs: 'objcountingCtrl',
                templateUrl: 'app/reports/objcounting/objcounting.tmpl.html',
                resolve: {
                    reportsObjcountingMdl: ['$ocLazyLoad', 'mainMdl', 'reportsMdl', function($ocLazyLoad, mainMdl, reportsMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-reports-objcounting']
                        });
                    }]
                }
            })
            .state('main.reports.videoblur', {
                url: '/videoblur',
                controller: 'VideoblurController',
                controllerAs: 'videoblurCtrl',
                templateUrl: 'app/reports/videoblur/videoblur.tmpl.html',
                resolve: {
                    reportsVideoblurMdl: ['$ocLazyLoad', 'mainMdl', 'reportsMdl', function($ocLazyLoad, mainMdl, reportsMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-reports-videoblur']
                        });
                    }]
                }
            })
            .state('main.reports.face', {
                url: '/face',
                controller: 'FaceController',
                controllerAs: 'faceCtrl',
                templateUrl: 'app/reports/face/face.tmpl.html',
                resolve: {
                    reportsFaceMdl: ['$ocLazyLoad', 'mainMdl', 'reportsMdl', function($ocLazyLoad, mainMdl, reportsMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-reports-face']
                        });
                    }]
                }
            })
            //Other report
            .state('main.periodic', {
                url: '/periodic',
                controller: 'PeriodicController',
                controllerAs: 'periodicCtrl',
                templateUrl: 'app/periodic/periodic.tmpl.html',
                resolve: {
                    periodicMdl: ['$ocLazyLoad', 'mainMdl', function($ocLazyLoad, mainMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-periodic']
                        });
                    }]
                }
            })
            /**
             * Notification Management
             */
            .state('main.notification', {
                url: '/notification',
                controller: 'NotificationController',
                controllerAs: 'notificationCtrl',
                templateUrl: 'app/notification/notification.tmpl.html',
                resolve: {
                    notificationMdl: ['$ocLazyLoad', 'mainMdl', function($ocLazyLoad, mainMdl) {
                        return $ocLazyLoad.load({
                            files: ['kai-notification']
                        });
                    }]
                }
            })

    }]);

angular.module('kai')
    .config(['$translateProvider', function($translateProvider) {
        //default language
        $translateProvider.preferredLanguage('en');
        // $translateProvider.preferredLanguage('zh-tw');

        //fallback language if entry is not found in current language
        $translateProvider.fallbackLanguage('en');

        //load language entries from files
        $translateProvider.useStaticFilesLoader({
            prefix: 'static/files/i18n/messages.', //relative path Eg: /languages/
            suffix: '.json' //file extension
        });
    }])
    .config(['$ocLazyLoadProvider', function($ocLazyLoadProvider) {
        $ocLazyLoadProvider.config({
            jsLoader: requirejs,
            debug: false,
            events: true,
        });
    }])
    .run(function($rootScope, $state, RouterStateService) {
        $rootScope.$on('$stateChangeStart',
            function(event, toState, toParams, fromState, fromParams) {
                var routerInfo = {
                    event: event,
                    toState: toState,
                    toParams: toParams,
                    fromState: fromState,
                    fromParams: fromParams,
                };
                RouterStateService.setRouterState(routerInfo);;
            });
    });

angular.module('kai')
    .controller('IndexCtrl', function($scope, AuthTokenFactory, ws, $log, KupOption, UserService, $translate, $location, $filter, UtilsService) {
        var kupOpt = KupOption;
        var indexCtrl = this;

        indexCtrl.data = {
            termsUrl: kupOpt.sysApiRootUrl + '/kaisquare/footer/index'
        };

        indexCtrl.currentTheme = AuthTokenFactory.getTheme();
        indexCtrl.currentLanguage = AuthTokenFactory.getUserLanguage();

        init();

        /*******************************************************************************
         *
         *  Function Definition
         *
         *******************************************************************************/
        function init() {
            //set wacth
            $scope.$watch('indexCtrl.currentTheme', function(newVal, oldVal) {
                AuthTokenFactory.setTheme(newVal);
            });

            $scope.$watch('indexCtrl.currentLanguage', function(newVal, oldVal) {
                AuthTokenFactory.setUserLanguage(newVal);
                moment.locale(AuthTokenFactory.getUserLanguage());
                $translate.use(AuthTokenFactory.getUserLanguage());
            });
        }
    });
