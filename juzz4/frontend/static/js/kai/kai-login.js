angular.module('kai.login', []);

angular.module('kai.login')
    .factory("LoginService", function(KupOption, AuthTokenFactory, $http, KupApiService) {
        var ajaxPost = KupApiService.ajaxPost;
        var data = {
            alertList: [],
            alertIndex: 0,

            loginForm: {
                company: "",
                username: "",
                password: "",
                rememberMe: false,
            },
            resetForm: {
                company: "",
                username: "",
                email: "",
            },
            showStatus: {
                loginForm: true,
                resetForm: false,
                alertList: false,
            },
            termsUrl: KupOption.sysApiRootUrl + '/kaisquare/footer/index'
        };

        return {
            data: data,
            getData: getData,
            login: login
        }

        /*******************************************************************************
         *
         *  Function Definition
         *
         *******************************************************************************/
        function getData(key) {
            var deepCopy = angular.copy(data);
            return (!!key) ? deepCopy[key] : deepCopy;
        }

        function login(api_name, param, onSuccess, onFail, onError, isDefer) {
            return ajaxPost(api_name, param, onSuccess, onFail, onError, isDefer);
        }
    });

angular.module('kai.login')
    .controller('LoginCtrl', function(KupOption, AuthTokenFactory, UserService, UtilsService, LoginService, $scope, $state, $http, $interval, $timeout, $q, $filter) {
        var loginCtrl = this;
        var i18n = UtilsService.i18n;
        var notification = UtilsService.notification;
        var block = UtilsService.block;
        var getData = LoginService.getData;

        loginCtrl.server = KupOption.sysApiRootUrl;

        loginCtrl.alertList = getData('alertList');
        loginCtrl.alertIndex = getData('alertIndex');
        loginCtrl.loginForm = getData('loginForm');
        loginCtrl.resetForm = getData('resetForm');
        loginCtrl.showStatus = getData('showStatus');

        loginCtrl.locked_time_remaining = 0;
        loginCtrl.login_attempt_remaining = 0;

        loginCtrl.events = {
            login: login,
            resetPassword: resetPassword,
            showLoginForm: showLoginForm,
            showResetForm: showResetForm,
            closeAlert: closeAlert,
            switchAlert: switchAlert,
            cleanResetForm: cleanResetForm,
        };

        loginCtrl.block = {
            options: {},
            timer: KupOption.loadingTimer
        }

        loginCtrl.data = LoginService.data;
        init();

        /*******************************************************************************
         *
         *  Function Definition
         *
         *******************************************************************************/
        function init() {
            getAnnouncementList();
            slideInit();
            rememberMe();
        }

        function cleanResetForm(type) {
            if (type === 'comany') {
                loginCtrl.resetForm.email = "";
            }
            if (type === 'email') {
                loginCtrl.resetForm.username = "";
                loginCtrl.resetForm.company = "";
            }
        }

        function slideInit() {
            $scope.slides = KupOption.slide_images;
            $('.carousel').carousel();
        }

        function getAnnouncementList() {
            var apiUrl = loginCtrl.server + '/api/superadmin';
            return $http.post(apiUrl + '/getannouncementlist',
                $.param({
                    "session-key": '',
                }), {
                    headers: {
                        'Accept': '*/*',
                        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                }
            ).then(
                function(response) {
                    var data = response.data;
                    if (data.result === 'ok') {
                        if (data.announcements.length > 0) {
                            loginCtrl.alertList = data.announcements;
                            loginCtrl.showStatus.alertList = true;
                        }
                        $interval(switchAlert, 3000);
                    }
                },
                function() {
                    console.error('api-error');
                }
            );
        }

        function switchAlert() {
            var length = loginCtrl.alertList.length;
            if (loginCtrl.alertIndex == length - 1) {
                loginCtrl.alertIndex = 0;
            } else {
                loginCtrl.alertIndex++;
            }
        }

        function closeAlert() {
            loginCtrl.showStatus.alertList = false;
        }

        function changeValidStatus(inputName) {
            if ($scope.loginForm[inputName].$invalid) {
                $scope.loginForm[inputName].$dirty = true;
            }
        }

        function login() {
            if ($scope.loginForm.$invalid) {
                changeValidStatus('company');
                changeValidStatus('username');
                changeValidStatus('password');
                return false;
            }
            var loginInfo = loginCtrl.loginForm;
            var loginPromise = function() {
                loginCtrl.isSubmit = 1;
                UserService.login(loginInfo.company, loginInfo.username, loginInfo.password, loginInfo.rememberMe).then(
                    function(response) {
                        var data = response.data;
                        if (data.result === 'ok') {
                            $state.go('main.dashboard');
                            $timeout(function() {
                                loginCtrl.isSubmit = 0;
                            }, 5000);
                        } else {
                            if (data.result === 'error' && data.reason === 'msg-account-locked') {
                                loginCtrl.locked_time_remaining = parseInt(data['locked-time-remaining'] / 60 / 1000);
                                loginCtrl.login_error_msg = i18n('msg-account-locked').replace('%s', loginCtrl.locked_time_remaining);
                            } else {
                                if (data.result === 'error' && data['login-attempt-remaining'] > 0) {
                                    loginCtrl.locked_time_remaining = data['login-attempt-remaining'];
                                    loginCtrl.login_error_msg = i18n('msg-incorrect-login').replace('%s', parseInt(loginCtrl.locked_time_remaining));
                                }
                                notification('error', i18n('information-not-verified'));
                            }
                            loginCtrl.isSubmit = 0;
                        }
                    },
                    function() {
                        notification('error', i18n('server-error'));
                        loginCtrl.isSubmit = 0;
                    }
                ).finally();
            };
            loginPromise();
        }

        function resetPassword() {
            var loginInfo = loginCtrl.resetForm;

            if ($scope.resetForm.email.$invalid && ($scope.resetForm.company.$invalid || $scope.resetForm.username.$invalid)) {
                changeValidStatus('company');
                changeValidStatus('username');
                return false;
            }

            if ($scope.resetForm.email.$valid) {
                loginInfo.company = "";
                loginInfo.username = "";
            }

            UserService.forgotPassword(loginCtrl.server, loginInfo.company, loginInfo.username, loginInfo.email).then(
                function(response) {
                    var data = response.data;
                    if (data.result === 'ok') {
                        notification('success', i18n('msg-reset-email-sent'));
                    } else {
                        notification('error', i18n('information-not-verified'));
                    }
                },
                function() {
                    notification('error', i18n('server-error'));
                }
            );
        }

        function showLoginForm() {
            loginCtrl.showStatus.loginForm = true;
            loginCtrl.showStatus.resetForm = false;
        }

        function showResetForm() {
            loginCtrl.showStatus.loginForm = false;
            loginCtrl.showStatus.resetForm = true;
        }

        function rememberMe() {
            var sessionKey = AuthTokenFactory.getSessionKey();
            if (sessionKey) {
                $state.go('main.dashboard');
            }
        }
    });
