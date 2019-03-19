angular.module('kai.profile', []);

angular.module('kai.profile')
    .directive('matchPassword', function() {
        return {
            restrict: 'A',
            require: ['^ngModel', '^form'],
            link: function(scope, element, attrs, ctrls) {
                var formController = ctrls[1];
                var ngModel = ctrls[0];
                var otherPasswordModel = formController[attrs.matchPassword];

                ngModel.$validators.passwordMatch = function(modelValue, viewValue) {
                    var password = modelValue || viewValue;
                    var otherPassword = otherPasswordModel.$modelValue || otherPasswordModel.viewValue;
                    return password === otherPassword;
                };

            } // end link
        }; // end return
    });
angular.module('kai.profile')
    .service('ProfileService', function(KupOption, UtilsService, KupApiService, $http, AuthTokenFactory) {
        var kupOpt = KupOption;
        var utils = UtilsService;
        var i18n = UtilsService.i18n;
        var notification = UtilsService.notification;
        var ajaxPost = KupApiService.ajaxPost;

        this.getUserProfile = function() {
            var param = {};
            var onSuccess = function() {};
            var onFail = function() {};
            var onError = function() {};
            return ajaxPost('getuserprofile', param, onSuccess, onFail, onError);
        };

        this.getUserRolesByUserId = function() {
            var param = {
                "user-id": AuthTokenFactory.getUserId()
            };
            var onSuccess = function() {};
            var onFail = function() {};
            var onError = function() {};
            return ajaxPost('getuserrolesbyuserid', param, onSuccess, onFail, onError);
        };

        this.updateUserProfile = function(userName, phone, email, fullName, language) {
            var param = {
                "user-name": userName,
                "phone": phone,
                "email": email,
                "name": fullName,
                "language": language
            };
            var onSuccess = function() {
                notification('success', i18n('profile-update-success'));
            };
            var onFail = function(data) {
                notification('error', i18n(data.reason));
            };
            var onError = function() {
                notification('error', i18n('server-error'));
            };
            return ajaxPost('updateuserprofile', param, onSuccess, onFail, onError);
        };


        this.getUserPrefs = function() {
            var param = {};
            var onSuccess = function() {};
            var onFail = function() {};
            var onError = function() {};
            return ajaxPost('getuserprefs', param, onSuccess, onFail, onError);
        };

        this.updateUserPrefs = function(prefs) {
            var param = {
                "theme": prefs.theme
            };
            var onSuccess = function() {
                notification('success', i18n('general-prefs-update-success'));
            };
            var onFail = function(data) {
                notification('error', i18n(data.reason));
            };
            var onError = function() {
                notification('error', i18n('server-error'));
            };
            return ajaxPost('saveuserprefs', param, onSuccess, onFail, onError);
        };


        this.changePassword = function(oldPassword, newPassword) {
            var param = {
                "old-password": oldPassword,
                "new-password": newPassword
            };
            var onSuccess = function() {
                notification('success', i18n('password-reset-success'));
            };
            var onFail = function(data) {
                notification('error', i18n(data.reason));
            };
            var onError = function() {
                notification('error', i18n('server-error'));
            };
            return ajaxPost('changepassword', param, onSuccess, onFail, onError);
        };

        this.getUserNotificationPreferences = function() {
            var param = {};
            var onSuccess = function() {};
            var onFail = function() {};
            var onError = function() {};
            return ajaxPost('getusernotificationprefs', param, onSuccess, onFail, onError);
        };

        this.updateUserNotificationPreferences = function(prefs) {
            var param = {
                'prefs-sms-moderate-enabled': prefs.sms.moderate,
                'prefs-sms-low-enabled': prefs.sms.low,
                'prefs-screen-moderate-enabled': prefs.onScreen.moderate,
                'prefs-screen-low-enabled': prefs.onScreen.low,
                'prefs-mobile-low-enabled': prefs.mobileApp.low,
                'prefs-email-low-enabled': prefs.email.low,
                'prefs-email-moderate-enabled': prefs.email.moderate,
                'prefs-mobile-moderate-enabled': prefs.mobileApp.moderate,
                'prefs-mobile-critical-enabled': prefs.mobileApp.critical,
                'prefs-mobile-mess-enabled': prefs.mobileApp.mess,
                'prefs-email-critical-enabled': prefs.email.critical,
                'prefs-screen-critical-enabled': prefs.onScreen.critical,
                'prefs-sms-critical-enabled': prefs.sms.critical
            };
            var onSuccess = function() {
                notification('success', i18n('notification-pref-update-success'));
            };
            var onFail = function(data) {
                notification('error', i18n(data.reason));
            };
            var onError = function() {
                notification('error', i18n('server-error'));
            };
            return ajaxPost('setusernotificationprefs', param, onSuccess, onFail, onError);
        };


        this.getUserMobileDevices = function() {
            var param = {};
            var onSuccess = function() {};
            var onFail = function() {};
            var onError = function() {};
            return ajaxPost('getusermobiledevices', param, onSuccess, onFail, onError);
        };

        this.unlinkUserMobileDevice = function(identifier) {
            var param = {
                "identifier": identifier
            };
            var onSuccess = function() {
                notification('success', i18n('mobile-device-unlink-success'));
            };
            var onFail = function(data) {
                notification('error', i18n(data.reason));
            };
            var onError = function() {
                notification('error', i18n('server-error'));
            };
            return ajaxPost('removemobiledeviceofuser', param, onSuccess, onFail, onError);
        };

        this.getUserNotificationSettings = function() {
            var param = {};
            var onSuccess = function() {};
            var onFail = function() {};
            var onError = function() {};
            return ajaxPost('getusernotificationsettings', param, onSuccess, onFail, onError);
        };

        this.updateUserNotificationSettings = function(eventType, notifyMethods) {
            var param = {
                "event-type": eventType,
                "notify-methods": angular.toJson(notifyMethods)
            };
            var onSuccess = function() {};
            var onFail = function(data) {};
            var onError = function() {};
            return ajaxPost('updateusernotificationsettings', param, onSuccess, onFail, onError);
        }

        this.getAllowedNotifyMethods = function() {
            var param = {};
            var onSuccess = function() {};
            var onFail = function() {};
            var onError = function() {};
            return ajaxPost('getallowednotifymethods', param, onSuccess, onFail, onError);
        };

    })

angular.module('kai.profile')
    .controller('ProfileController', function(KupOption, UtilsService, ProfileService, AuthTokenFactory, _, $log, $scope, $translate, $q, $timeout, $filter) {
        var kupOpt = KupOption;
        var utils = UtilsService;
        var i18n = UtilsService.i18n;
        var notification = UtilsService.notification;

        var indexCtrl = $scope.$parent.$parent.indexCtrl;
        var mainCtrl = $scope.$parent.mainCtrl;
        var profileCtrl = this;

        var getUserRolesByUserId, getUserProfile, getUserPrefs, getUserMobileDevices, getAllowedNotifyMethods;

        profileCtrl.ngMessageUrl = KupOption.sysNgMessageUrl;
        // get and update profile basic settings
        (function() {
            profileCtrl.company = AuthTokenFactory.getBucket();
            profileCtrl.languages = (function() {
                var list = [];
                $.each(KupOption.language, function(i, lang) {
                    list.push(lang);
                    list[i].text = i18n(lang.text);
                });
                return list;
            })();

            getUserRolesByUserId = ProfileService.getUserRolesByUserId()
                .success(function(data) {
                    if (data.result === 'ok') {
                        var tmpAry = [];
                        $.each(data.roles, function(i, info) {
                            tmpAry.push(info.name);
                        });
                        profileCtrl.role = tmpAry.join(', ');
                    }
                });

            getUserProfile = ProfileService.getUserProfile()
                .success(function(data) {
                    if (data.result === 'ok') {
                        profileCtrl.fullName = data.name;
                        profileCtrl.email = data.email;
                        profileCtrl.phone = data.phone;
                        profileCtrl.userName = data['user-name'];
                        profileCtrl.language = _.where(profileCtrl.languages, {
                            value: data.language
                        })[0];
                    }
                });

            profileCtrl.submitProfileBasicForm = function(isValid) {
                if (isValid) {
                    ProfileService.updateUserProfile(profileCtrl.userName, profileCtrl.phone, profileCtrl.email, profileCtrl.fullName, profileCtrl.language.value)
                        .success(function() {
                            // $translate.use(profileCtrl.language.value);
                            indexCtrl.currentLanguage = profileCtrl.language.value;
                        });
                }
            };
        })();

        // get and update general preferences
        (function() {
            profileCtrl.themes = (function() {
                var list = [];
                $.each(KupOption.theme, function(i, theme) {
                    list.push(theme);
                    list[i].text = i18n(theme.text);
                });
                return list;
            })();
            getUserPrefs = ProfileService.getUserPrefs()
                .success(function(data) {
                    if (data.result === 'ok') {
                        profileCtrl.prefs = data.prefs;
                        profileCtrl.selectedTheme = _.where(profileCtrl.themes, {
                            value: profileCtrl.prefs.theme
                        })[0];
                    }
                });

            profileCtrl.submitGeneralPreferencesForm = function(isValid) {
                if (isValid) {
                    profileCtrl.prefs.theme = profileCtrl.selectedTheme.value;
                    ProfileService.updateUserPrefs(profileCtrl.prefs)
                        .success(function(data) {
                            if (data.result === 'ok') {
                                indexCtrl.currentTheme = profileCtrl.prefs.theme;
                            }
                        })
                }
            };
        })();

        //Password Reset
        (function() {
            profileCtrl.submitProfilePasswordResetForm = function(isValid) {
                if (isValid) {
                    ProfileService.changePassword(profileCtrl.currentPassword, profileCtrl.newPassword);
                }
            }
        })();

        //Notifications
        (function() {
            profileCtrl.formData = [];
            profileCtrl.receiveWeeklySummaryEmail = true;
            // profileCtrl.notifyMethods = {
            //     "ON_SCREEN": {
            //         text: "on-screen"
            //     }
            // };
            getAllowedNotifyMethods = ProfileService.getAllowedNotifyMethods()
                .success(function(data) {
                    if (data.result === 'ok') {
                        profileCtrl.notifyMethods = data.methods;
                    } else {
                        profileCtrl.notifyMethods = [];
                    }
                    getUserNotificationSettings = ProfileService.getUserNotificationSettings()
                        .success(function(data) {
                            if (data.result === 'ok') {
                                var tmpObj = {};
                                $.each(data.settings, function(event, typeList) {
                                    tmpObj[event] = {};
                                    $.each(profileCtrl.notifyMethods, function(i, type) {
                                        tmpObj[event][type] = false;
                                    });
                                    $.each(typeList, function(i, type) {
                                        tmpObj[event][type] = true;
                                    });
                                });
                                $.each(kupOpt.eventType, function(i, eventType) {
                                    $.each(tmpObj, function(event, typeInfo) {
                                        if (eventType !== event) {
                                            return true;
                                        }
                                        var tmpObj = {
                                            event: event,
                                            typeInfo: typeInfo
                                        };
                                        profileCtrl.formData.push(tmpObj);
                                    });
                                });
                            }

                        });
                });

            profileCtrl.submitProfileNotificationPreferencesForm = function(isValid) {
                var requestList = (function() {
                    var request = [];
                    $.each(profileCtrl.formData, function(i, info) {    
                        var event = info.event;
                        var typeList = info.typeInfo;
                        var notifyMethods = [];
                        $.each(typeList, function(type, isActive) {
                            if (isActive) {
                                notifyMethods.push(type);
                            }
                        });
                        request.push(ProfileService.updateUserNotificationSettings(event, notifyMethods));
                    });
                    return request;
                })();
                if (isValid) {
                    $q.all(requestList)
                        .then(
                            function() {
                                notification('success', i18n('notification-setting-update-success'));
                            },
                            function() {
                                notification('error', i18n('server-error'));
                            });
                }
            };
        })();


        //Mobile Device
        (function() {
            getUserMobileDevices = ProfileService.getUserMobileDevices()
                .success(function(data) {
                    if (data['result'] === 'ok') {
                        profileCtrl.mobileDevices = data.mobileDevices;
                    }
                });

            profileCtrl.updateProfileMobileDevicesForm = function(isValid) {
                if (isValid) {
                    angular.forEach(profileCtrl.mobileDevices, function(mobile) {
                        if (mobile.unlink) {
                            ProfileService.unlinkUserMobileDevice(mobile.identifier).then(function() {
                                ProfileService.getUserMobileDevices().success(function(data) {
                                    if (data['result'] === 'ok') {
                                        profileCtrl.mobileDevices = data.mobileDevices;
                                    }
                                });
                                mobile.unlink = false;
                            });
                        }
                    });
                }
            };
        })();

        init();
        /*******************************************************************************
         *
         *  Function Definition
         *
         *******************************************************************************/
        function init() {
            var promise = function() {
                var dfd = $q.defer();
                $timeout(function() {
                    dfd.resolve();
                }, 1000);
                return dfd.promise;
            };
            mainCtrl.block.promise = promise();
        }
    });
