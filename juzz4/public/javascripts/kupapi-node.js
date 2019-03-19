//KAI Node API for communicating with remote platform
(function ($) {

    var KAI_NODE_API = "/nodeapi/";
    var nodeUrl,
        nodeBucket,
        sessionKey,
        userId,
        defaultErrorHandler = function fnDefaultErrorHandler(jqXHR, extra) {
            utils.popupAlert(extra);
        };

    function checkSession() {
        return typeof sessionKey === 'string' && sessionKey != '';
    }

    kainode = {
        init: function (bucket, errorHandler) {
            nodeBucket = bucket;
            nodeUrl = KAI_NODE_API + nodeBucket;
            if ($.isFunction(defaultErrorHandler))
                defaultErrorHandler = errorHandler;
        },

        getSession: function () {
            return sessionKey;
        },

        authenticateOTP: function (otp, onSuccess, onFailure) {
            var params = {
                "otp": otp
            };
            ajaxPost(KAI_NODE_API + 'authotp', params, function (response) {
                if (response.result == 'ok') {
                    sessionKey = response['session-key'];
                    userId = response['user-id'];
                }

                if (onSuccess)
                    onSuccess.call(kainode, response);

            }, onFailure);
        },

        checkLicenseStatus: function (otp, licenseNumber, onSuccess, onFailure) {
            var params = {
                "otp": otp,
                "license-number": licenseNumber
            };
            ajaxPost(KAI_NODE_API + 'checklicensestatus', params, onSuccess, onFailure);
        },

        register: function (regInfo, onSuccess, onFailure) {
            var params = {
                "otp": regInfo.otp,
                "license-number": regInfo.licenseNumber,
                "registration-number": regInfo.registrationNumber,
                "device-name": regInfo.deviceName,
                "device-address": regInfo.address,
                "device-latitude": regInfo.latitude,
                "device-longitude": regInfo.longitude
            };
            ajaxPost(KAI_NODE_API + 'register', params, onSuccess, onFailure);
        },

        replaceNode: function (regInfo, onSuccess, onFailure) {
            var params = {
                "otp": regInfo.otp,
                "license-number": regInfo.licenseNumber,
                "registration-number": regInfo.registrationNumber
            };
            ajaxPost(KAI_NODE_API + 'replace', params, onSuccess, onFailure);
        },

        resetNode: function (otp, licenseNumber, onSuccess, onFailure) {
            var params = {
                "otp": otp,
                "license-number": licenseNumber
            };
            ajaxPost(KAI_NODE_API + 'reset', params, onSuccess, onFailure);
        },

        getNetworkStatus: function (onSuccess, onFailure) {
            ajaxPost(nodeUrl + "/getnetworkstatus", {}, onSuccess, onFailure);
        },

        getNodeInfo: function (onSuccess, onFailure) {
            ajaxPost("/nodeapi/getnodeinfo", {}, onSuccess, onFailure);
        },

        getNodeVersion: function (onSuccess, onFailure) {
            ajaxPost("/nodeapi/getnodeversion", {}, onSuccess, onFailure);
        }
    };

    window.kainode = kainode;

})(jQuery);