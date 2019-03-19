 /**
  * [Javascript Module Pattern]
  * utils module
  * @param  {object} $   plug-in:jQuery
  * @param  {object} kup KUP module
  * @return {object}     public member
  * @author andy.gan@kaisquare.com.tw
  */
 KUP.utils = (function($, kup) {
     var _self = {
         //native api
         default: window.utils,
         i18n: window.localizeResource,
         events: window.events,
         deviceManager: window.deviceManager,
         //extension api
         chart: {
             kendo: window.kendo,
             morris: window.Morris
         },
         format: {
             numeral: window.numeral
         },
         namespace: function(ns, nsString) {
             var parent = ns,
                 parts = nsString.split('.').slice(1),
                 pl, i;

             pl = parts.length;
             for (i = 0; i < pl; i++) {
                 //create a property if it doesnt exist
                 if (typeof parent[parts[i]] === 'undefined') {
                     parent[parts[i]] = {};
                 }

                 parent = parent[parts[i]];
             }
             return ns;
         },
         deepCopy: function(obj) {
             var deepCopy;
             if ($.isArray(obj)) {
                 deepCopy = $.extend(true, [], obj);
             } else {
                 deepCopy = $.extend(true, {}, obj);
             }
             return deepCopy;
         },
         /**
          * [jQuery BlockUI Plugin]
          * @http://malsup.com/jquery/block/
          */
         block: {
             msg: function(msg) {
                 $('.loading-text-overlay #loadingText').html(msg);
             },
             popup: function(selector, msg) {
                 _self.block.msg(msg);
                 $(selector).block({
                     message: $(".loading-text-overlay"),
                     css: {
                         border: 'none'
                     }
                 });
             },
             close: function(selector, callBack, sec) {
                 var callBack = callBack || function() {},
                     sec = sec || 1000;
                 setTimeout(function() {
                     $(selector).unblock({
                         onUnblock: function() {
                             callBack();
                         }
                     });
                 }, sec);
             }
         },
         ajaxMsg: {
             error: function(data) {
                 console.log("API response is error:", data);
             },
             fatal: function(data) {
                 console.log("API status is " + data.status + ":", data);
             }
         }
     };
     return _self;
 })(jQuery, KUP);
