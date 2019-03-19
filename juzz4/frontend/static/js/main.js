requirejs.config({
    baseUrl: 'static/',
    urlArgs: '',
    waitSeconds: 0,
    
    /**
     *  [css loader]
     */
    map: {
        '*': {
            'css': 'js/plugin/require-css/require-css.min'
        }
    },

    /**
     *  [defined path namespace]
     */
    paths: {
        /**
         *  [css namespace]
         */

        //3-party plugin
        'bootstrap-custom<css>': 'css/assets/plugin/bootstrap/bootstrap.custom.min',
        'bootstrap-toggle<css>': 'css/assets/plugin/bootstrap/bootstrap-toggle.min',
        'bootstrap-switch<css>': 'css/assets/plugin/bootstrap-switch/bootstrap-switch.min',
        'bootstrap-daterangepicker<css>': 'css/assets/plugin/bootstrap-daterangepicker/bootstrap-daterangepicker.min',

        'font-awesome<css>': 'css/assets/plugin/fontawesome/css/font-awesome.min',

        'dataTables<css>': 'css/assets/plugin/dataTables/css/dataTables.min',
        'dataTables-custom<css>': 'css/assets/plugin/dataTables/css/dataTables.custom.min',
        'dataTables-responsive<css>': 'css/assets/plugin/dataTables/css/dataTables.responsive.min',

        'kendo-common<css>': 'css/assets/plugin/kendo/kendo.common.min',
        'kendo-flat<css>': 'css/assets/plugin/kendo/kendo.flat.min',
        'kendo-dataviz<css>': 'css/assets/plugin/kendo/kendo.flat.min',
        'kendo-dataviz-flat<css>': 'css/assets/plugin/kendo/kendo.dataviz.flat.min',

        'leaflet<css>': 'css/assets/plugin/leaflet/leaflet.min',
        'leaflet-draw<css>': 'css/assets/plugin/leaflet/leaflet.draw.min',
        'leaflet-kai<css>': 'css/assets/plugin/leaflet/leaflet.kai.min',

        'angular-notify<css>': 'css/assets/plugin/angular-notify/angular-notify.min',
        'angular-busy<css>': 'css/assets/plugin/angular-busy/angular-busy.min',
        'malihu-custom-scrollbar<css>': 'css/assets/plugin/malihu-custom-scrollbar/jquery.mCustomScrollbar.min',
        'timecard<css>': 'css/assets/plugin/kup-timecard/timecard.min',
        'kupicons<css>': 'css/assets/plugin/kup-icons/kupicons.min',

        //core plugin
        'common<css>': 'css/common.min',

        /**
         *  [js namespace]
         */

        //base plugin
        'jquery': 'js/plugin/jquery/jquery.min',
        'underscore': 'js/plugin/underscore/underscore-min',
        'bootstrap': 'js/plugin/bootstrap/bootstrap.min',
        'angular': 'js/plugin/angular/angular.min',

        //other plugin
        'sound-manager': 'js/plugin/sound-manager/sound-manager.min',
        'progressbar': 'js/plugin/progressbar/progressbar.min',

        //link to old ui plugin
        'backendTypes<http>': '<apiUrl>/public/javascripts/_generated/backend.types',

        //3-party plugin
        'numeral': 'js/plugin/numeral/numeral.min',
        'kendo': 'js/plugin/kendo/kendo.all.min',
        'jqueryplayer': 'js/plugin/jqueryplayer/jquery.jplayer.min',

        'jwplayer': 'js/plugin/jwplayer/jwplayer.min',

        'bootstrap-switch': 'js/plugin/bootstrap-switch/bootstrap-switch.min',
        'bootstrap-select': 'js/plugin/bootstrap-select/bootstrap-select.min',
        'ui-bootstrap': 'js/plugin/angular-ui-bootstrap/ui-bootstrap-tpls.min',
        'malihu-custom-scrollbar': 'js/plugin/malihu-custom-scrollbar/jquery.mCustomScrollbar.concat.min',
        'datetimepicker': 'js/plugin/datetimepicker/jquery.datetimepicker.min',

        'raphael': 'js/plugin/raphael/raphael.min',
        'morris': 'js/plugin/morris/morris.min',

        'moment': 'js/plugin/moment/moment.min',
        'moment-range': 'js/plugin/moment-range/moment-range.min',
        'moment-locales': 'js/plugin/moment-locales/moment-locales.min',
        'moment-with-locales': 'js/plugin/moment-with-locales/moment-with-locales.min',
        'angular-moment': 'js/plugin/angular-moment/angular-moment.min',
        'moment-locale-zh-tw': 'js/plugin/angular-moment/locale/zh-tw.min',
        'moment-locale-zh-cn': 'js/plugin/angular-moment/locale/zh-cn.min',

        'bootstrap-daterangepicker': 'js/plugin/bootstrap-daterangepicker/daterangepicker.min',

        'amcharts': 'js/plugin/amcharts/amcharts',
        'amcharts-serial': 'js/plugin/amcharts/serial',
        'amcharts-amstock': 'js/plugin/amcharts/amstock',

        'datatables': 'js/plugin/datatables/js/jquery.dataTables.min',
        'datatables-responsive': 'js/plugin/datatables/js/dataTables.responsive.min',

        'jszip': 'js/plugin/jszip/jszip.min',
        'filesaver': 'js/plugin/FileSaver/FileSaver.min',

        'd3': 'js/plugin/d3/d3.v3.min',
        'd3-cubism': 'js/plugin/d3/cubism.v1.min',
        'd3-tip': 'js/plugin/d3-tip/index.min',

        'heatmap': 'js/plugin/heatmap/heatmap.min',

        'leaflet-src': 'js/plugin/leaflet/leaflet-src.min',
        'leaflet': 'js/plugin/leaflet/leaflet.min',
        'leaflet-draw': 'js/plugin/leaflet/leaflet.draw.min',

        //angular 3-party module
        'ocLazyLoad-require': 'js/plugin/ocLazyLoad/ocLazyLoad.require.min',
        'angular-ui-router': 'js/plugin/angular-ui-router/angular-ui-router.min',
        'angular-cookies': 'js/plugin/angular-cookies/angular-cookies.min',
        'angular-bootstrap': 'js/plugin/angular-bootstrap/ui-bootstrap-tpls.min',
        'angular-bootstrap-switch': 'js/plugin/angular-bootstrap-switch/angular-bootstrap-switch.min',
        'nya-bootstrap-select': 'js/plugin/nya-bootstrap-select/nya-bs-select.min',
        'angular-notify': 'js/plugin/angular-notify/angular-notify.min',
        'angular-animate': 'js/plugin/angular-animate/angular-animate.min',

        'angular-translate': 'js/plugin/angular-translate/angular-translate.min',
        'angular-translate-loader-static-files': 'js/plugin/angular-translate-loader-static-files/angular-translate-loader-static-files.min',

        'angular-ws': 'js/plugin/angular-ws/angular-ws.min',
        'angular-poller': 'js/plugin/angular-poller/angular-poller.min',
        'angular-kendo': 'js/plugin/angular-kendo/angular-kendo.min',
        'angular-morris': 'js/plugin/angular-morris/angular-morris.min',
        'angular-messages': 'js/plugin/angular-messages/angular-messages.min',
        'angular-malihu-custom-scrollbar': 'js/plugin/angular-malihu-custom-scrollbar/angular-malihu-custom-scrollbar.min',
        'angular-busy': 'js/plugin/angular-busy/angular-busy.min',
        'angular-datetimepicker': 'js/plugin/angular-datetimepicker/angular-datetimepicker.min',
        'angular-amcharts': 'js/plugin/angular-amcharts/angular-amcharts.min',

        'angular-datatables': 'js/plugin/angular-datatables/angular-datatables.min',
        'angular-datatables-select': 'js/plugin/angular-datatables/plugins/select/angular-datatables.select.min',
        'angular-datatables-columnfilter': 'js/plugin/angular-datatables/plugins/columnfilter/angular-datatables.columnfilter.min',

        'angular-fullscreen': 'js/plugin/angular-fullscreen/angular-fullscreen.min',
        'angular-screenfull': 'js/plugin/angular-screenfull/angular-screenfull.min',

        'angular-mc-resizer': 'js/plugin/angular-mc-resizer/angular-mc-resizer.min',

        //index module
        'kai': 'js/kai/kai.min',
        'kai-common': 'js/kai/kai-common.min',

        //main module
        'kai-login': 'js/kai/kai-login.min',
        'kai-main': 'js/kai/kai-main.min',

        //Not type
        'kai-dashboard': 'js/kai/kai-dashboard.min',
        'kai-profile': 'js/kai/kai-profile.min',

        //Admin setting
        'kai-pos': 'js/kai/kai-pos.min',
        'kai-label': 'js/kai/kai-label.min',

        //Monitoring
        'kai-liveview': 'js/kai/kai-liveview.min',

        //Recording
        'kai-cloudplayback': 'js/kai/kai-cloudplayback.min',

        //Reports
        'kai-periodic': 'js/kai/kai-periodic.min',
        'kai-reports': 'js/kai/kai-reports.min',
        'kai-reports-traffic': 'js/kai/kai-reports-traffic.min',
        'kai-reports-pcounting': 'js/kai/kai-reports-pcounting.min',
        'kai-reports-crowd': 'js/kai/kai-reports-crowd.min',
        'kai-reports-profiling': 'js/kai/kai-reports-profiling.min',
        'kai-reports-attention': 'js/kai/kai-reports-attention.min',
        'kai-reports-perimeter': 'js/kai/kai-reports-perimeter.min',
        'kai-reports-loitering': 'js/kai/kai-reports-loitering.min',
        'kai-reports-intrusion': 'js/kai/kai-reports-intrusion.min',
        'kai-reports-objcounting': 'js/kai/kai-reports-objcounting.min',
        'kai-reports-videoblur': 'js/kai/kai-reports-videoblur.min',
        'kai-reports-face': 'js/kai/kai-reports-face.min',

        //Notification Management
        'kai-notification': 'js/kai/kai-notification.min',
    },

    /**
     *  [defined load dependance]
     */
    shim: {
        //base plugin / 3-party plugin
        'kendo': ['css!kendo-common<css>', 'css!kendo-flat<css>', 'css!kendo-dataviz<css>', 'css!kendo-dataviz-flat<css>'],
        'bootstrap': ['jquery'],
        'bootstrap-switch': ['css!bootstrap-switch<css>', 'jquery'],
        'malihu-custom-scrollbar': ['css!malihu-custom-scrollbar<css>', 'jquery'],
        'sound-manager': ['jqueryplayer'],
        'angular': [
            'underscore',
            'bootstrap',
            'bootstrap-switch',
            'sound-manager',
            'malihu-custom-scrollbar'
        ],
        'moment-range': ['moment'],
        'bootstrap-daterangepicker': ['css!bootstrap-daterangepicker<css>', 'jquery', 'moment'],

        'amcharts-serial': {
            deps: ['amcharts'],
            exports: 'AmCharts',
            init: function() {
                AmCharts.isReady = true;
            }
        },
        'amcharts-amstock': {
            deps: ['amcharts-serial'],
            exports: 'AmCharts',
            init: function() {
                AmCharts.isReady = true;
            }
        },
        //'d3-tip': ['d3'],
        'leaflet-draw': ['css!leaflet<css>', 'css!leaflet-draw<css>', 'css!leaflet-kai<css>', 'leaflet'],

        //angular 3-party module
        'ocLazyLoad-require': ['angular'],
        'angular-ui-router': ['angular'],
        'angular-bootstrap': ['angular'],
        'angular-bootstrap-switch': ['angular'],
        'nya-bootstrap-select': ['angular'],
        'angular-notify': ['css!angular-notify<css>', 'angular'],
        'angular-animate': ['angular'],
        'angular-ws': ['angular'],
        'angular-cookies': ['angular'],

        'angular-translate': ['angular'],
        'angular-translate-loader-static-files': ['angular-translate'],

        'angular-poller': ['angular'],
        'angular-messages': ['angular'],
        'angular-malihu-custom-scrollbar': ['angular'],
        'angular-busy': ['css!angular-busy<css>', 'angular'],
        'angular-kendo': ['kendo', 'angular'],
        'angular-amcharts': ['amcharts-amstock', 'angular'],
        'angular-morris': ['raphael', 'morris', 'angular'],
        'ui-bootstrap': ['angular', 'angular-animate'],
        'datetimepicker': ['moment'],

        'angular-datatables': ['css!dataTables<css>', 'css!dataTables-custom<css>', 'css!dataTables-responsive<css>', 'jquery', 'datatables', 'datatables-responsive', 'angular'],
        'angular-datatables-columnfilter': ['angular-datatables'],

        'angular-fullscreen': ['angular'],
        'angular-screenfull': ['angular'],

        'angular-mc-resizer': ['angular'],

        'angular-moment': ['angular', 'moment', 'moment-locale-zh-tw', 'moment-locale-zh-cn'],

        //index module
        'kai': [
            /* 3-party module */
            'ocLazyLoad-require',
            'angular-moment',
            'angular-animate',
            'angular-messages',
            'angular-ui-router',
            'angular-bootstrap',
            'angular-bootstrap-switch',
            'nya-bootstrap-select',
            'angular-notify',
            'angular-translate',
            'angular-translate-loader-static-files',
            'angular-ws',
            'angular-poller',
            'angular-malihu-custom-scrollbar',
            'angular-busy',
            /* core module */
            'kai-common',
        ],
        'kai-common': [
            'css!bootstrap-custom<css>',
            'css!bootstrap-toggle<css>',
            'css!font-awesome<css>',
            'css!kupicons<css>',
            'css!common<css>',
            'angular-cookies'
        ],

        //main module
        'kai-login': ['ui-bootstrap'],
        'kai-main': ['kai-notification'],

        //Not type
        'kai-dashboard': ['numeral', 'angular-kendo', 'angular-morris'],
        'kai-profile': [],

        //Admin setting
        'kai-pos': ['kendo', 'moment-range', 'bootstrap-daterangepicker', 'angular-datatables'],
        'kai-label': ['kendo'],

        //Monitoring
        'kai-liveview': ['jwplayer', 'kendo', 'angular-fullscreen'],

        //Recording
        'kai-cloudplayback': ['kendo', 'jwplayer', 'd3', 'progressbar', 'backendTypes<http>'],

        //Reports
        'kai-periodic': ['kendo', 'bootstrap-daterangepicker', 'angular-datatables'],
        'kai-reports': ['kendo', 'bootstrap-daterangepicker'],
        'kai-reports-traffic': ['raphael'],
        'kai-reports-pcounting': ['css!timecard<css>', 'angular-amcharts', 'd3-tip'],
        'kai-reports-crowd': ['raphael', 'heatmap', 'leaflet-draw', 'angular-amcharts'],
        'kai-reports-profiling': ['numeral', 'angular-morris', 'angular-amcharts', 'angular-datatables'],
        'kai-reports-attention': ['angular-amcharts'],
        'kai-reports-perimeter': ['angular-amcharts'],
        'kai-reports-loitering': ['angular-amcharts'],
        'kai-reports-intrusion': ['angular-amcharts'],
        'kai-reports-objcounting': ['angular-amcharts'],
        'kai-reports-videoblur': ['angular-amcharts'],
        'kai-reports-face': ['jszip', 'filesaver'],

        //Notification Management
        'kai-notification': ['jwplayer', 'kendo', 'bootstrap-daterangepicker', 'angular-datatables'],
    }
});


/**
 *  [Start the main app logic]
 */
require(['kai'], function() {
    angular.bootstrap(document.querySelector("html"), ['kai']);
});

//need to change this config later, because this config is init loading,not amd loading
require(['jszip'], function(JSZip) {
    window.JSZip = JSZip;
});

require(['d3', 'd3-tip'], function(d3, d3tip) {
    window.d3 = d3;
    window.d3.tip = d3tip;
});

require(['progressbar'], function(progressbar) {
    window.ProgressBar = progressbar;
});
