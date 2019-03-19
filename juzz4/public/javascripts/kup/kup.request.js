 /**
  * [Javascript Module Pattern]
  * ajax module
  * @param  {object} $   plug-in:jQuery
  * @param  {object} kup KUP module
  * @return {object}     public member
  * @author andy.gan@kaisquare.com.tw
  */
 KUP.request = (function($, kup) {
     var _ajax = function(options) {
             if (!options || $.isEmptyObject(options)) {
                 return;
             }
             var opts = options.opts || {},
                 data = options.data || {},
                 callback = options.callback || function() {},
                 callbackFail = options.callbackFail || function() {};

             var kupOpt = kup.getOpt(),
                 defaultOpts = {
                     url: kupOpt.apiUrl,
                     async: true,
                     type: "POST",
                     data: data,
                     dataType: 'json',
                     cache: false,
                     lock: false,
                     force: false,
                     silent: false

                 };
             if (typeof options === 'object') {
                 opts = $.extend({}, defaultOpts, opts);
             }
             //opts.lock && !opts.lock.isLock && kup.lock(opts.lock.id,true);
             return $.ajax(opts)
                 .done(function(data) {
                     if (data) {
                         if (data.result === 'error') {
                             !opts.silent && kup.utils.ajaxMsg.error(data);
                             if (!opts.force) {
                                 return;
                             }
                         }
                         callback(data);
                     }
                 })
                 .fail(function(jqXHR) {
                     !opts.silent && kup.utils.ajaxMsg.fatal(jqXHR);
                     callbackFail(jqXHR);
                     
                 });
         },
         ajaxPost = function(url, data, callback, callbackFail) {
             var setRequest = {
                 data: data,
                 opts: {
                     url: url
                 },
                 callback: callback,
                 callbackFail: callbackFail
             };
             return _ajax(setRequest);
         },
         _self = {
             getReportHtml: function(config) {
                 var data = config.data || {},
                     onSuccess = config.onSuccess || function() {},
                     onFailure = config.onFailure || function() {};

                 var kupOpt = kup.getOpt(),
                     url = '/' + kupOpt.bucket + '/report/' + data.reportType,
                     params = {};

                 var setRequest = {
                     data: params,
                     opts: {
                         url: url,
                         dataType: 'html',
                         type: "get",
                     },
                     callback: onSuccess,
                     callbackFail: onFailure
                 };
                 return _ajax(setRequest);
             },
             //not to use for now
             // getUserDevices: function(config) {
             //     var data = config.data || {},
             //         onSuccess = config.onSuccess || function() {},
             //         onFailure = config.onFailure || function() {};

             //     var kupOpt = kup.getOpt(),
             //         url = kupOpt.api.url + 'getuserdevices',
             //         params = {
             //             "session-key": kupOpt.sessionKey
             //         };
             //     return ajaxPost(url, params, onSuccess, onFailure);
             // },
             // getCurrentActiveSessions: function(config) {
             //     var data = config.data || {},
             //         onSuccess = config.onSuccess || function() {},
             //         onFailure = config.onFailure || function() {};

             //     var kupOpt = kup.getOpt(),
             //         url = kupOpt.api.url + 'getcurrentactivesessions',
             //         params = {
             //             "session-key": kupOpt.sessionKey,
             //             "bucket-id": kupOpt.bucket
             //         };
             //     return ajaxPost(url, params, onSuccess, onFailure);
             // },
             // getDashboard: function(config) {
             //     var data = config.data || {},
             //         onSuccess = config.onSuccess || function() {},
             //         onFailure = config.onFailure || function() {};

             //     var kupOpt = kup.getOpt(),
             //         url = kupOpt.api.url + 'getdashboard',
             //         params = {
             //             "session-key": kupOpt.sessionKey,
             //             "days": data.days || 7,
             //             "time-zone-offset": config.data.timeZone
             //         };
             //     return ajaxPost(url, params, onSuccess, onFailure);
             // },
             // listRunningAnalytics: function(config) {
             //     var data = config.data || {},
             //         onSuccess = config.onSuccess || function() {},
             //         onFailure = config.onFailure || function() {};

             //     var kupOpt = kup.getOpt(),
             //         url = kupOpt.api.url + 'listrunninganalytics',
             //         params = {
             //             "session-key": kupOpt.sessionKey,
             //             "analytics-type": kupOpt.analyticsType.ALL
             //         };
             //     return ajaxPost(url, params, onSuccess, onFailure);
             // },
             // listPosNames: function(config) {
             //     var data = config.data || {},
             //         onSuccess = config.onSuccess || function() {},
             //         onFailure = config.onFailure || function() {};

             //     var kupOpt = kup.getOpt(),
             //         url = kupOpt.api.url + 'listposnames',
             //         params = {
             //             "session-key": kupOpt.sessionKey,
             //             "parser-type": data.parserType || ""
             //         };
             //     return ajaxPost(url, params, onSuccess, onFailure);
             // },
             // saveReportQueryHhistory: function(config) {
             //     var data = config.data || {},
             //         onSuccess = config.onSuccess || function() {},
             //         onFailure = config.onFailure || function() {};

             //     var kupOpt = kup.getOpt(),
             //         url = kupOpt.api.url + 'savereportqueryhistory',
             //         params = {
             //             "event-type": data.eventType,
             //             "date-from": data.dateFrom,
             //             "date-to": data.dateTo,
             //             "device-selected": data.deviceSelected
             //         };
             //     return ajaxPost(url, params, onSuccess, onFailure);
             // },
             // getReportQueryHistory: function(config) {
             //     var data = config.data || {},
             //         onSuccess = config.onSuccess || function() {},
             //         onFailure = config.onFailure || function() {};

             //     var kupOpt = kup.getOpt(),
             //         url = kupOpt.api.url + 'getreportqueryhistory',
             //         params = {
             //             "event-type": data.eventType
             //         };
             //     return ajaxPost(url, params, onSuccess, onFailure);
             // },
             // keepSessionAlive: function(config) {
             //     var data = config.data || {},
             //         onSuccess = config.onSuccess || function() {},
             //         onFailure = config.onFailure || function() {};

             //     var kupOpt = kup.getOpt(),
             //         url = kupOpt.api.url + 'keepalive',
             //         params = {
             //             "session-key": kupOpt.sessionKey
             //         };

             //     var setRequest = {
             //         data: params,
             //         opts: {
             //             url: url,
             //             force: true,
             //             silent: true
             //         },
             //         callback: onSuccess,
             //         callbackFail: onFailure
             //     };
             //     return _ajax(setRequest);
             // }
         };
     return _self;
 })(jQuery, KUP);
