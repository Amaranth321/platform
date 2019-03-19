/**
 * [Javascript Module Pattern]
 * KUP module
 * @param  {object} $ plug-in:jQuery
 * @return {object}   KUP module
 * @author andy.gan@kaisquare.com.tw
 */
var KUP = (function($) {
    var _option = {
            bucket: '', //is login's company name
            sessionKey: window.sessionKey || "",
            analyticsType: window.analyticsType,
            event: window.KupEvent,
            api: window.kupapi,
            dateFormat: 'ddMMyyyyHHmmss'
        },
        _self = {
            widget: {},
            setOpt: function(config) {
                _option = $.extend(false, {}, _option, config || {});
            },
            getOpt: function(key) {
                var deepCopy = $.extend(true, {}, _option);
                return (!!key) ? deepCopy[key] : deepCopy;
            },
            namespace: function(ns, nsString) {
                var kup = _self;
                return kup.utils.namespace(ns, nsString);
            },
            init: function() {
                var kup = _self,
                    keepSessionAliveOpt = {
                        sessionKey: '',
                        onSuccess: function(data) {
                            if (data.result === 'error') {
                                //go to login page
                                window.location.href = window.location.protocol + '//' + window.location.host;
                            }
                        },
                        onFailure: function() {}
                    };
                //check session alive
                setInterval(function() {
                    window.keepSessionAlive(
                        keepSessionAliveOpt.sessionKey,
                        keepSessionAliveOpt.onSuccess,
                        keepSessionAliveOpt.onFailure
                    );
                }, 30000);
            }
        };
    return _self;
})(jQuery);
