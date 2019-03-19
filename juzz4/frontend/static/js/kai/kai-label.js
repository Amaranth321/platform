angular.module('kai.label', [
    'kendo.directives',
]);

angular
    .module('kai.label')
    .factory("LabelModalService",
        function(
            KupOption,
            UtilsService, KupApiService, PromiseFactory, AuthTokenFactory, DeviceTreeService
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var ajaxPost = KupApiService.ajaxPost;

            var data = {
                currectStep: 0, //0 :not show modal, 1 : step1,2 : step2 ,3 :show map
                isOpenModal: false,
                isUpdateMode: false,
                isMapSuccessLoad: false,
                timePicker: setTimePicker(),
                //UI setting
                uiLabelForm: {
                    labelName: '',
                    labelType: setLabelType(),
                    location: setLabelFormLocation(),
                    occupancy: setLabelFormOccupancy(),
                    holidays: setLabelFormHolidays(),
                    periods: setLabelFormPeriods(),
                },
                uiModal: {
                    isShowSaveMap: false,
                    isShowCancel: false,
                    isShowSave: false,
                    isShowNext: false,
                    isSshowPrev: false,
                }
            };
            return {
                data: data,
                initLabelForm: initLabelForm,
                updateLabelForm: updateLabelForm,
                setTimePicker: setTimePicker,
                getSelectLabelType: getSelectLabelType,
                getSelectPeriodsType: getSelectPeriodsType,
                getSelectHolidays: getSelectHolidays,
                getPeriodsOfDays: getPeriodsOfDays,
            };
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function setLabelType(selectData) {
                var labelType = [{
                    name: 'store',
                    value: 'STORE',
                    class: 'kup-store',
                    isSelect: true,
                    isShow: true,
                }, {
                    name: 'geographic-area',
                    value: 'REGION',
                    class: 'kup-GeographicArea',
                    isSelect: false,
                    isShow: false,
                }, {
                    name: 'other',
                    value: 'OTHERS',
                    class: 'fa-tag',
                    isSelect: false,
                    isShow: true,
                }];

                if (selectData) {
                    $.each(labelType, function(i, type) {
                        if (type.value === selectData) {
                            type.isSelect = true;
                        } else {
                            type.isSelect = false;
                        }
                    });
                }
                return labelType;

            }

            function setLabelFormLocation(selectData) {
                var location = {
                    address: '',
                    lat: 0,
                    lng: 0,
                    timeZoneId: '',
                    //tzOffsetMins: 0
                };
                if (selectData) {
                    location = selectData;
                }
                return location;
            }

            function setLabelFormOccupancy(selectData) {
                var occupancy = {
                    isSelect: false,
                    rule: [{
                        value: 100,
                        msg: ''
                    }],
                    ruleDefault: {
                        value: 100,
                        msg: ''
                    }
                };
                if (selectData) {
                    occupancy.isSelect = selectData.enabled;
                    if (selectData.limits) {
                        occupancy.rule = (function() {
                            var tmpAry = [];
                            $.each(selectData.limits, function(i, limit) {
                                tmpAry.push({
                                    value: limit.limit,
                                    msg: limit.alertMessage
                                });
                            });
                            return tmpAry;
                        })();
                    }
                }
                return occupancy;
            }

            function setLabelFormHolidays(selectData) {
                var holidays = [{
                    name: 'mon',
                    nameFull: 'monday',
                    value: 1,
                    isSelect: false,
                }, {
                    name: 'tue',
                    nameFull: 'tuesday',
                    value: 2,
                    isSelect: false,
                }, {
                    name: 'wed',
                    nameFull: 'wednesday',
                    value: 3,
                    isSelect: false,
                }, {
                    name: 'thu',
                    nameFull: 'thursday',
                    value: 4,
                    isSelect: false,
                }, {
                    name: 'fri',
                    nameFull: 'friday',
                    value: 5,
                    isSelect: false,
                }, {
                    name: 'sat',
                    nameFull: 'saturday',
                    value: 6,
                    isSelect: false,
                }, {
                    name: 'sun',
                    nameFull: 'sunday',
                    value: 7,
                    isSelect: false,
                }];

                if (selectData) {
                    $.each(holidays, function(i, day) {
                        $.each(selectData, function(j, val) {
                            if (day.value === val) {
                                day.isSelect = true;
                                return false;
                            }
                        });
                    });
                }
                return holidays;
            }

            function setLabelFormPeriods(selectData) {
                var periods = {
                    everyday: {
                        value: 'EVERYDAY',
                        isSelect: true,
                        rule: [{
                            weekStart: 1,
                            weekEnd: 7,
                            timeStart: 28800,
                            timeEnd: 64800,
                        }],
                    },
                    nonStop: {
                        value: 'NON_STOP',
                        isSelect: false,
                        rule: {
                            timeLowest: 28800,
                        }
                    },
                    custom: {
                        value: 'CUSTOM',
                        isSelect: false,
                        rule: [{
                            weekStart: 1,
                            weekEnd: 5,
                            timeStart: 28800,
                            timeEnd: 64800,
                        }],
                        ruleDefault: {
                            weekStart: 1,
                            weekEnd: 5,
                            timeStart: 28800,
                            timeEnd: 64800,
                        }
                    }
                };
                if (selectData) {
                    $.each(periods, function(type, periodData) {
                        if (periodData.value === selectData.repeat) {
                            periodData.isSelect = true;
                            if (selectData.repeat === 'NON_STOP') {
                                periodData.rule = {
                                    timeLowest: parseInt(selectData.lowestTrafficHour * 60 * 60)
                                };
                            } else {
                                periodData.rule = [];
                                $.each(selectData.periods, function(i, periodData2) {
                                    periodData.rule.push({
                                        weekStart: periodData2.from,
                                        weekEnd: periodData2.to,
                                        timeStart: periodData2.period.startMinutes * 60,
                                        timeEnd: periodData2.period.endMinutes * 60,
                                    });
                                });
                            }

                        } else {
                            periodData.isSelect = false;
                        }
                    });
                }
                return periods;
            }

            function initLabelForm() {
                var opt = data;
                opt.uiLabelForm = {
                    labelName: '',
                    labelType: setLabelType(),
                    location: setLabelFormLocation(),
                    occupancy: setLabelFormOccupancy(),
                    holidays: setLabelFormHolidays(),
                    periods: setLabelFormPeriods(),
                };
                console.log(opt.uiLabelForm);
            }

            function updateLabelForm(labelData) {
                var opt = data;
                if (labelData) {
                    opt.uiLabelForm = {
                        labelName: labelData.labelName,
                        labelType: setLabelType(labelData.data.type),
                        location: setLabelFormLocation(labelData.data.info.location || false),
                        occupancy: setLabelFormOccupancy(labelData.data.info.occupancySettings || false),
                        holidays: setLabelFormHolidays(labelData.data.info.schedule && labelData.data.info.schedule.holidays || []),
                        periods: setLabelFormPeriods(labelData.data.info.schedule && labelData.data.info.schedule.weeklyPeriods),
                    };
                } else {
                    initLabelForm();
                }

            }

            function setTimePicker() {
                var timePicker = [];
                var config = {};
                var hours = 24;

                var padLeft = function(str, len) {
                    str = '' + str;
                    return str.length >= len ? str : new Array(len - str.length + 1).join("0") + str;
                };

                var timeToSec = function(hour, min, sec) {
                    hour = parseInt(hour, 10) || 0;
                    min = parseInt(min, 10) || 0;
                    sec = parseInt(sec, 10) || 0;
                    return hour * 60 * 60 + min * 60 + sec;
                }

                for (var i = 0; i < hours; i++) {
                    config = {
                        name: padLeft(i, 2) + ' : 00',
                        value: timeToSec(i)
                    };
                    timePicker.push(config);
                };
                return timePicker;
            }

            function getSelectLabelType() {
                var opt = data;
                var labelType = {};
                $.each(opt.uiLabelForm.labelType, function(i, type) {
                    if (type.isSelect) {
                        labelType = type.value;
                        return false;
                    }
                });
                return labelType;
            }

            function getSelectPeriodsType() {
                var opt = data;
                var periodsType = {};
                $.each(opt.uiLabelForm.periods, function(type, time) {
                    if (time.isSelect) {
                        periodsType = type;
                        return false;
                    }
                });
                return periodsType;
            }

            function getSelectHolidays() {
                var opt = data;
                var holidays = [];
                $.each(opt.uiLabelForm.holidays, function(i, dayData) {
                    if (dayData.isSelect) {
                        holidays.push(dayData.value);
                    }
                });
                return holidays;
            }

            function getPeriodsOfDays() {
                var opt = data;
                var periodsType = getSelectPeriodsType();
                var periodsOfDays = [];
                var secToMin = function(sec) {
                    return Math.round(sec / 60);
                }

                $.each(opt.uiLabelForm.periods[periodsType].rule, function(i, rule) {
                    var config = {
                        from: rule.weekStart,
                        to: rule.weekEnd,
                        period: {
                            startMinutes: secToMin(rule.timeStart),
                            endMinutes: secToMin(rule.timeEnd)
                        }
                    };

                    periodsOfDays.push(config);
                });
                return periodsOfDays;
            }
        });

angular
    .module('kai.label')
    .factory("LabelService",
        function(
            KupOption,
            UtilsService, KupApiService, PromiseFactory, AuthTokenFactory, DeviceTreeService, 
            LabelModalService,
            $filter
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var ajaxPost = KupApiService.ajaxPost;

            var data = {
                //UI selector
                $section: '#labelSection',
                $treeview: '#labelTreeview',
                $search: '#labelSearch',
                $dragTarget: '#labelDragTarget',

                //UI setting
                uiMenu: {
                    isShow: true
                },
                uiFilterVca: {
                    isOpen: false,
                    list: (function() {
                        var list = [];
                        $.each(kupOpt.vca, function(i, vca) {
                            list.push({
                                name: vca.name,
                                type: vca.analyticsType,
                                isActive: false
                            });
                        });
                        list.unshift({
                            name: 'all',
                            type: 'ALL',
                            isActive: true
                        });
                        return list;
                    })(),
                },
                uiTreeview: {
                    data: {},
                    options: {},
                    list: [],
                    searchValue: '',
                },
                uiDragTarget: {
                    data: {},
                    options: {},
                    list: [],
                    isShow: false,
                },
                uiLabels: {
                    list: [],
                    listByFilter: [],
                    searchLabelValue: '',

                    selectCameraList: [],
                    selectCameraListByFilter: [],
                    searchCameraValue: '',

                    isAddShow: false,
                    updateValue: '',
                    addValue: '',
                    classCount: 5,
                },
                uiGuider: {
                    isShow: true,
                },
            };
            var initData = angular.copy(data);
            return {
                data: data,
                initData: initData,

                setInitData: setInitData,
                setTreeList: setTreeList,
                setLabelList: setLabelList,


                setSelectLabel: setSelectLabel,
                setSelectLabelById: setSelectLabelById,
                setCameraList: setCameraList,
                setCameraListById: setCameraListById,

                filterLabelList: filterLabelList,
                filterCameraList: filterCameraList,

                getDeviceTree: getDeviceTree,
                getSelectLabel: getSelectLabel,
                getLabelById: getLabelById,

                selectLabel: selectLabel,
                selectLabelById: selectLabelById,
                setFilterLabelCamera: setFilterLabelCamera,

                addLabelApi: addLabelApi,
                removeLabelApi: removeLabelApi,
                updateLabelApi: updateLabelApi,
                assignChannelLabelApi: assignChannelLabelApi,
                unassignChannelLabelApi: unassignChannelLabelApi,
            };
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function setInitData() {
                data = angular.copy(initData);
                return data;
            }

            function getDeviceTree() {
                var opt = data;
                return DeviceTreeService.initDeviceTree()
                    .finally(function() {
                        setTreeList();
                        setLabelList();
                    });
            }

            function getSelectLabel() {
                var opt = data;
                var labelData = "";
                $.each(opt.uiLabels.listByFilter, function(i, label) {
                    if (label.isSelect) {
                        labelData = label;
                        return false;
                    }
                });
                return labelData;
            }

            function getLabelById(id) {
                var opt = data;
                var labelData;
                $.each(opt.uiLabels.listByFilter, function(i, label) {
                    if (id === label.labelId) {
                        labelData = label;
                    }
                });
                return labelData;
            }

            function setTreeList() {
                var opt = data;
                var treeItems = DeviceTreeService.getDeviceTree();

                opt.uiTreeview.list = (function() {
                    var tree = angular.copy(treeItems);
                    var treeList = [];
                    $.each(tree, function(i, label) {
                        if (label.isAll) {
                            var deviceData = label.items;
                            $.each(deviceData, function(j, device) {
                                if (!device.isNode) {
                                    //device.items = [];
                                }
                            });
                            treeList.push(label);
                        }
                    });
                    return treeList;
                })();
            }

            function setLabelList() {
                var opt = data;
                var treeItems = DeviceTreeService.getDeviceTree();

                opt.uiLabels.list = (function() {
                    var tree = angular.copy(treeItems);
                    var labelList = [];
                    $.each(tree, function(i, label) {
                        if (label.isLabel) {
                            labelList.push(label);
                        }
                    });

                    $.each(labelList, function(i, label) {
                        label.isSelect = false;
                        label.isUpdateShow = false;
                    });
                    return labelList;
                })();
                opt.uiLabels.list = $filter('orderBy')(opt.uiLabels.list, '+text');
            }

            function setSelectLabel(index) {
                index = index || 0;
                var opt = data;
                var labelData = "";
                $.each(opt.uiLabels.listByFilter, function(i, label) {
                    label.isSelect = (i === index) ? true : false;
                });
            }

            function setSelectLabelById(id) {
                var opt = data;
                $.each(opt.uiLabels.listByFilter, function(i, label) {
                    if (label.labelId === id) {
                        label.isSelect = true;
                    } else {
                        label.isSelect = false;
                    }
                });
            }


            function setCameraList(index) {
                index = index || 0;
                var opt = data;
                opt.uiLabels.selectCameraList = [];
                opt.uiLabels.selectCameraListByFilter = [];
                $.each(opt.uiLabels.listByFilter, function(i, label) {
                    var labelData = label.items;
                    if (i === index) {
                        $.each(labelData, function(j, device) {
                            var deviceData = device.items;
                            $.each(deviceData, function(k, camera) {
                                var cameraData = camera;
                                var vcaData = cameraData.filterVca.split(',');
                                cameraData.vca = [];
                                $.each(vcaData, function(k, vcaName) {
                                    $.each(KupOption.vca, function(key, vcaOpt) {
                                        if (vcaOpt.analyticsType === vcaName) {
                                            cameraData.vca.push(vcaOpt);
                                            return false;
                                        }
                                    })
                                });
                                opt.uiLabels.selectCameraList.push(cameraData);
                            });
                        });
                    }
                });
                opt.uiLabels.selectCameraList = $filter('orderBy')(opt.uiLabels.selectCameraList, ['+data.deviceName', '+text']);
                opt.uiLabels.selectCameraListByFilter = angular.copy(opt.uiLabels.selectCameraList);
            }

            function setCameraListById(id) {
                var opt = data;
                opt.uiLabels.selectCameraList = [];
                opt.uiLabels.selectCameraListByFilter = [];
                $.each(opt.uiLabels.listByFilter, function(i, label) {
                    var labelData = label.items;
                    if (label.labelId === id) {
                        $.each(labelData, function(j, device) {
                            var deviceData = device.items;
                            $.each(deviceData, function(k, camera) {
                                var cameraData = camera;
                                var vcaData = cameraData.filterVca.split(',');
                                cameraData.vca = [];
                                $.each(vcaData, function(k, vcaName) {
                                    $.each(KupOption.vca, function(key, vcaOpt) {
                                        if (vcaOpt.analyticsType === vcaName) {
                                            cameraData.vca.push(vcaOpt);
                                            return false;
                                        }
                                    })
                                });
                                opt.uiLabels.selectCameraList.push(cameraData);
                            });
                        });
                    }
                });
                opt.uiLabels.selectCameraList = $filter('orderBy')(opt.uiLabels.selectCameraList, ['+data.deviceName', '+text']);
                opt.uiLabels.selectCameraListByFilter = angular.copy(opt.uiLabels.selectCameraList);
            }

            function filterLabelList(searchVal) {
                var opt = data;
                opt.uiLabels.listByFilter = $filter('filter')(opt.uiLabels.list, {
                    text: searchVal
                });
            }

            function filterCameraList(searchVal) {
                var opt = data;
                opt.uiLabels.selectCameraListByFilter = $filter('filter')(opt.uiLabels.selectCameraList, function(value, index, array) {
                    var check1 = false;
                    var check2 = false;
                    var filterRegExp = new RegExp(searchVal, 'i');
                    if (value.text) {
                        check1 = filterRegExp.test(value.text);
                    }
                    if (value.data) {
                        check2 = filterRegExp.test(value.data.deviceName);
                    }
                    return check1 || check2;
                });
            }

            function addLabelApi() {
                var opt = data;
                var optModal = LabelModalService.data;
                var optModalForm = optModal.uiLabelForm;
                var labelName = optModal.uiLabelForm.labelName;
                var labelType = LabelModalService.getSelectLabelType();
                var periodsType = LabelModalService.getSelectPeriodsType();
                var labelInfo = {};

                //set labelInfo 
                (function() {
                    if (labelType !== 'OTHERS') {
                        labelInfo = {
                            location: {},
                            schedule: {}
                        };

                        //location
                        labelInfo.location = optModalForm.location;

                        //schedule
                        labelInfo.schedule.holidays = LabelModalService.getSelectHolidays();
                        labelInfo.schedule.weeklyPeriods = {};
                        labelInfo.schedule.weeklyPeriods.repeat = optModalForm.periods[periodsType].value;
                        if (periodsType === 'nonStop') {
                            labelInfo.schedule.weeklyPeriods.lowestTrafficHour = parseInt(optModalForm.periods[periodsType].rule.timeLowest / 60 / 60, 10);
                        } else {
                            labelInfo.schedule.weeklyPeriods.periods = LabelModalService.getPeriodsOfDays();
                            labelInfo.schedule.weeklyPeriods.lowestTrafficHour = 0;
                        }
                    }
                })();


                var param = {
                    "label-name": labelName,
                    "label-type": labelType,
                    "label-info": JSON.stringify(labelInfo)
                };

                var onSuccess = function(response) {};
                var onFail = function(response) {};
                var onError = function() {};
                return ajaxPost('addlabel', param, onSuccess, onFail, onError);
            }

            function updateLabelApi(param) {
                var opt = data;
                var optModal = LabelModalService.data;
                var optModalForm = optModal.uiLabelForm;
                var labelId = param.labelId;
                var labelName = optModal.uiLabelForm.labelName;
                var labelType = LabelModalService.getSelectLabelType();
                var periodsType = LabelModalService.getSelectPeriodsType();
                var labelInfo = {};

                //set labelInfo 
                (function() {
                    if (labelType !== 'OTHERS') {
                        labelInfo = {
                            location: {},
                            schedule: {}
                        };

                        //location
                        labelInfo.location = optModalForm.location;

                        //schedule
                        labelInfo.schedule.holidays = LabelModalService.getSelectHolidays();
                        labelInfo.schedule.weeklyPeriods = {};
                        labelInfo.schedule.weeklyPeriods.repeat = optModalForm.periods[periodsType].value;
                        if (periodsType === 'nonStop') {
                            labelInfo.schedule.weeklyPeriods.lowestTrafficHour = parseInt(optModalForm.periods[periodsType].rule.timeLowest / 60 / 60, 10);
                        } else {
                            labelInfo.schedule.weeklyPeriods.periods = LabelModalService.getPeriodsOfDays();
                            labelInfo.schedule.weeklyPeriods.lowestTrafficHour = 0;
                        }
                    }
                })();
                var param = {
                    "label-id": labelId,
                    "label-name": labelName,
                    "label-type": labelType,
                    "label-info": JSON.stringify(labelInfo)
                };
                var onSuccess = function(response) {};
                var onFail = function(response) {};
                var onError = function() {};
                return ajaxPost('updatelabel', param, onSuccess, onFail, onError);
            }


            function removeLabelApi(param) {
                var opt = data;
                var labelId = param.labelId;
                var param = {
                    "label-id": labelId,
                };
                var onSuccess = function(response) {};
                var onFail = function(response) {};
                var onError = function() {};
                return ajaxPost('removelabel', param, onSuccess, onFail, onError);
            }

            function assignChannelLabelApi(param) {
                var opt = data;
                var param = {
                    "label-id": param.labelId,
                    "platform-device-id": param.platformDeviceId,
                    "channel-id": param.cameraId,
                };
                var onSuccess = function(response) {};
                var onFail = function(response) {};
                var onError = function() {};
                return ajaxPost('assignchannellabel', param, onSuccess, onFail, onError);
            }

            function unassignChannelLabelApi(param) {
                var opt = data;
                var param = {
                    "label-id": param.labelId,
                    "platform-device-id": param.platformDeviceId,
                    "channel-id": param.cameraId,
                };
                var onSuccess = function(response) {};
                var onFail = function(response) {};
                var onError = function() {};
                return ajaxPost('unassignchannellabel', param, onSuccess, onFail, onError);
            }

            function selectLabel(index) {
                setSelectLabel(index);
                setCameraList(index);
            }

            function selectLabelById(id) {
                setSelectLabelById(id);
                setCameraListById(id);

            }

            function setFilterLabelCamera() {
                filterLabelList();
                filterCameraList();
            }
        });

angular
    .module('kai.label')
    .controller('LabelModalController',
        function(
            KupOption,
            RouterStateService, UtilsService, PromiseFactory, AuthTokenFactory,
            LabelService, LabelModalService, GoogleMapService,
            $scope, $rootScope, $timeout, $uibModalInstance, updateLabelData
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;

            var labelModalCtrl = this;
            labelModalCtrl.data = LabelModalService.data;
            labelModalCtrl.fn = {
                isLabelExist: isLabelExist,
                isCustomOperationHourInvalid:  isCustomOperationHourInvalid,

                closeModal: closeModal,
                cancelModal: cancelModal,
                saveModal: saveModal,

                selectLabelType: selectLabelType,
                selectOccupancy: selectOccupancy,

                addOccupancy: addOccupancy,
                subOccupancy: subOccupancy,
                addOccupancyItem: addOccupancyItem,
                delOccupancyItem: delOccupancyItem,

                selectPeriods: selectPeriods,
                addCustomized: addCustomized,
                delCustomized: delCustomized,

                showPrev: showPrev,
                showNext: showNext,
                showSave: showSave,
                showSaveMap: showSaveMap,
                showCancel: showCancel,
                goStep: goStep,

                addLabel: addLabel,
                updateLabel: updateLabel,
                getSelectLabelType: getSelectLabelType,
                saveMap: saveMap,
            };
            init();
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function init() {
                var opt = labelModalCtrl.data;
                opt.currectStep = 1;
                opt.isUpdateMode = (updateLabelData) ? true : false;
                LabelModalService.updateLabelForm(updateLabelData);
                setWatch();
            }

            function setWatch() {

                $scope.$watch('labelModalCtrl.data.uiLabelForm.location', function(newVal, oldVal) {
                    console.warn(newVal);
                }, true);

                $scope.$watch('labelModalCtrl.data.uiLabelForm.occupancy.value', function(newVal, oldVal) {
                    if (/\D/.test(newVal)) {
                        labelModalCtrl.data.uiLabelForm.occupancy.value = 0;
                    }
                }, true);

                $scope.$watch('labelModalCtrl.data.uiLabelForm.labelType', function(newVal, oldVal) {
                    showBtn();
                }, true);

                $scope.$watch(function() {
                    return GoogleMapService.data.isSuccessLoad;
                }, function(newVal, oldVal) {
                    var opt = labelModalCtrl.data;
                    opt.isMapSuccessLoad = newVal;
                }, true);

                $scope.$watch(function() {
                    return GoogleMapService.data.searchMap;
                }, function(newVal, oldVal) {
                    if (newVal.isSuccessSearch !== oldVal.isSuccessSearch) {
                        showBtn();
                    }
                }, true);

                $scope.$watch(function() {
                    return GoogleMapService.data.searchBox;
                }, function(newVal, oldVal) {
                    var opt = labelModalCtrl.data;
                    if (newVal.isTriggerSearch) {

                        if(oldVal.location != "")
                        {
                            newVal.isTriggerSearch = false;
                        }

                        opt.uiLabelForm.location = {
                            address: newVal.location,
                            lat: newVal.lat,
                            lng: newVal.lng,
                            timeZoneId: newVal.timeZoneId,
                            //tzOffsetMins: parseInt(newVal.timeZoneOffset / 60, 10),
                        };
                    }
                }, true);

                $scope.$watch('labelModalCtrl.data.currectStep', function(newVal, oldVal) {
                    var opt = labelModalCtrl.data;
                    var searchBoxTimer = function() {
                        $timeout(function() {
                            if ($('.pac-container').length <= 0) {
                                searchBoxTimer();
                                return false;
                            }
                            $('.pac-container').css('z-index', 100000);

                        }, 300);
                    };
                    if (newVal === 2) {
                        var loadSearchBoxTimer = function() {
                            $timeout(function() {
                                if ($('#labelStep2').css('display') === 'none') {
                                    loadSearchBoxTimer();
                                    return false;
                                }
                                GoogleMapService.loadSearchBox('labelSearchBox');
                                searchBoxTimer();
                            }, 300);
                        };
                        loadSearchBoxTimer();
                    }
                    if (newVal === 3) {
                        var loadSearchMapTimer = function() {
                            $timeout(function() {
                                if ($('#labelStep3').css('display') === 'none') {
                                    loadSearchMapTimer();
                                    return false;
                                }
                                GoogleMapService.loadSearchMap('labelSearchMap', 'labelSearchBoxForMap', opt.uiLabelForm.location.address);
                                searchBoxTimer();
                            }, 300);
                        };
                        loadSearchMapTimer();
                    }
                    showBtn();
                }, true);
            }

            function setFormValidation() {
                var opt = labelModalCtrl.data;
                var isLatlng = (function() {
                    var check = false;
                    if (opt.uiLabelForm.location.address && opt.uiLabelForm.location.lat && opt.uiLabelForm.location.lng) {
                        check = true;
                    }
                    return check;
                })();
                $scope.labelForm.labelSearchBox.$setValidity("isLatlng", isLatlng);
            }

            function closeModal() {
                $uibModalInstance.close();
            }

            function cancelModal() {
                $uibModalInstance.dismiss('cancel');
            }

            function selectLabelType(index) {
                var opt = labelModalCtrl.data;
                $.each(opt.uiLabelForm.labelType, function(i, type) {
                    type.isSelect = (i === index) ? true : false;
                });
            }

            function selectOccupancy(isSelect) {
                var opt = labelModalCtrl.data;
                //if (isSelect !== opt.uiLabelForm.occupancy.isSelect) {
                opt.uiLabelForm.occupancy.isSelect = isSelect;
                //}
            }

            function addOccupancy(index) {
                var opt = labelModalCtrl.data;
                var rangeNum = 100;
                var occupancy = parseInt(opt.uiLabelForm.occupancy.rule[index].value, 10);
                opt.uiLabelForm.occupancy.rule[index].value = occupancy + rangeNum;
            }

            function subOccupancy(index) {
                var opt = labelModalCtrl.data;
                var rangeNum = 100;
                var occupancy = parseInt(opt.uiLabelForm.occupancy.rule[index].value, 10);
                if (occupancy >= rangeNum) {
                    opt.uiLabelForm.occupancy.rule[index].value = occupancy - rangeNum;
                } else {
                    opt.uiLabelForm.occupancy.rule[index].value = 0;
                }
            }

            function addOccupancyItem() {
                var opt = labelModalCtrl.data;
                var addItems = angular.copy(opt.uiLabelForm.occupancy.ruleDefault);
                opt.uiLabelForm.occupancy.rule.push(addItems);
            }

            function delOccupancyItem(index) {
                var opt = labelModalCtrl.data;
                if (index !== 0) {
                    opt.uiLabelForm.occupancy.rule.splice(index, 1);
                }
            }

            function selectPeriods(periodsType) {
                var opt = labelModalCtrl.data;
                $.each(opt.uiLabelForm.periods, function(type, data) {
                    if (type === periodsType) {
                        if (!data.isSelect) {
                            data.isSelect = true;
                        }
                    } else {
                        data.isSelect = false;
                    }
                });
            }

            function addCustomized() {
                var opt = labelModalCtrl.data;
                var addItems = angular.copy(opt.uiLabelForm.periods.custom.ruleDefault);
                opt.uiLabelForm.periods.custom.rule.push(addItems);
            }

            function delCustomized(index) {
                var opt = labelModalCtrl.data;
                if (index !== 0) {
                    opt.uiLabelForm.periods.custom.rule.splice(index, 1);
                }
            }

            function saveModal() {
                var labelId = updateLabelData.labelId || '';
                var selectLabelType = LabelModalService.getSelectLabelType();
                if (selectLabelType !== 'OTHERS' && $scope.labelForm.labelSearchBox.$invalid) {
                    changeValidStatus('labelSearchBox');
                    return false;
                }

                if (selectLabelType !== 'OTHERS' && $scope.labelForm.labelTimeZoneId.$invalid && labelModalCtrl.data.isMapSuccessLoad) {
                    changeValidStatus('labelTimeZoneId');
                    return false;
                }

                if (labelId) {
                    updateLabel(labelId);
                } else {
                    addLabel();
                }
            }

            function addLabel() {
                if ($scope.labelForm.labelName.$invalid) {
                    changeValidStatus('labelName');
                    return false;
                }
                LabelService.addLabelApi()
                    .error(function() {
                        notification('error', i18n("server-error"));
                    })
                    .success(function(response) {
                        if (response.reason) {
                            notification('error', i18n(response.reason));
                        } else {
                            closeModal();
                            LabelModalService.initLabelForm();
                            LabelService.getDeviceTree().finally(function() {
                                LabelService.setFilterLabelCamera();
                                LabelService.selectLabelById(response['label-id']);
                            });

                        }
                    })
                    .finally(function() {});
            }

            function updateLabel(labelId) {
                if ($scope.labelForm.labelName.$invalid) {
                    changeValidStatus('labelName');
                    return false;
                }
                var param = {
                    labelId: labelId
                };

                LabelService.updateLabelApi(param)
                    .error(function() {
                        notification('error', i18n("server-error"));
                    })
                    .success(function(response) {
                        if (response.reason) {
                            notification('error', i18n(response.reason));
                        } else {
                            closeModal();
                            LabelModalService.initLabelForm();
                            LabelService.getDeviceTree().finally(function() {
                                LabelService.setFilterLabelCamera();
                                LabelService.selectLabelById(labelId);
                            });
                        }
                    })
                    .finally(function() {});
            }

            function showBtn() {
                showPrev();
                showNext();
                showSave();
                showSaveMap();
                showCancel();
            }

            function showPrev() {
                var opt = labelModalCtrl.data;
                var isShow = false;
                if (opt.currectStep === 2) {
                    isShow = true;
                }
                opt.uiModal.isShowPrev = isShow;
                return isShow;
            }

            function showNext() {
                var opt = labelModalCtrl.data;
                var isShow = false;
                if (opt.currectStep === 1 && LabelModalService.getSelectLabelType() !== 'OTHERS') {
                    isShow = true;
                }
                opt.uiModal.isShowNext = isShow;
                return isShow;
            }

            function showSave() {
                var opt = labelModalCtrl.data;
                var isShow = false;
                if (opt.currectStep === 1 && LabelModalService.getSelectLabelType() === 'OTHERS') {
                    isShow = true;
                }
                if (opt.currectStep === 2) {
                    isShow = true;
                }
                opt.uiModal.isShowSave = isShow;
                return isShow;
            }

            function showSaveMap() {
                var opt = labelModalCtrl.data;
                var isShow = false;
                if (opt.currectStep === 3 && GoogleMapService.data.searchMap.isSuccessSearch) {
                    isShow = true;
                }
                opt.uiModal.isShowSaveMap = isShow;
                return isShow;
            }

            function showCancel() {
                var opt = labelModalCtrl.data;
                var isShow = false;
                if (opt.currectStep === 1) {
                    isShow = true;
                }
                opt.uiModal.isShowCancel = isShow;
                return isShow;
            }

            function changeValidStatus(inputName) {
                if ($scope.labelForm[inputName].$invalid) {
                    $scope.labelForm[inputName].$dirty = true;
                }
            }

            function goStep(step) {
                var opt = labelModalCtrl.data;

                if ($scope.labelForm.labelName.$invalid) {
                    changeValidStatus('labelName');
                    return false;
                }
                opt.currectStep = step;
            }

            function getSelectLabelType() {
                return LabelModalService.getSelectLabelType();
            }

            function saveMap() {
                var opt = labelModalCtrl.data;
                var optMap = GoogleMapService.data;
                opt.currectStep = 2;
                opt.uiLabelForm.location = {
                    address: optMap.searchMap.location,
                    lat: optMap.searchMap.lat,
                    lng: optMap.searchMap.lng,
                    timeZoneId: optMap.searchMap.timeZoneId,
                    //tzOffsetMins: parseInt(optMap.searchMap.timeZoneOffset / 60, 10),
                };
            }

            function isLabelExist() {
                var labelOpt = LabelService.data;
                var opt = labelModalCtrl.data;
                var isExist = false;
                if (!opt.isUpdateMode) {
                    $.each(labelOpt.uiLabels.list, function(i, label) {
                        if (label.labelName === opt.uiLabelForm.labelName) {
                            isExist = true;
                            return false;
                        }
                    });
                }
                return isExist;
            }

            function isCustomOperationHourInvalid() {
                var isInvalid = false;
                if (labelModalCtrl.data.currectStep != 2) {
                    return false;
                }
                if (!labelModalCtrl.data.uiLabelForm.periods.custom.isSelect) {
                    return false;
                }
                $.each(labelModalCtrl.data.uiLabelForm.periods.custom.rule, function (index, rule) {
                   if (rule.timeEnd <= rule.timeStart) {
                       isInvalid = true;
                       return;
                   }
                });
                return isInvalid;
            }
        });

angular
    .module('kai.label')
    .controller('LabelController',
        function(
            KupOption,
            RouterStateService, UtilsService, PromiseFactory, AuthTokenFactory, DeviceTreeService,
            LabelService, LabelModalService, MainService,
            $scope, $timeout, $q, $location, $uibModal, $log, $animate
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var isCurrentPage = RouterStateService.isCurrentPage;
            var mainCtrl = $scope.$parent.mainCtrl;
            var labelCtrl = this;

            //UI controller
            labelCtrl.data = LabelService.data = LabelService.setInitData();
            labelCtrl.data.uiTreeview.options = getTreeviewOptions();
            labelCtrl.data.uiDragTarget.options = getDragTargetOptions();
            labelCtrl.fn = {
                isEmptyDevice: DeviceTreeService.isEmptyDevice,

                showMainMenu: showMainMenu,
                showGuider: showGuider,
                showGuiderCheck: showGuiderCheck,

                setCurrectFilterVca: setCurrectFilterVca,
                getCurrectFilterVca: getCurrectFilterVca,

                selectLabelById: selectLabelById,

                getLabelClass: getLabelClass,
                getLabelTagClass: getLabelTagClass,
                getSelectLabel: getSelectLabel,

                showAddLabel: showAddLabel,
                showUpdateLabel: showUpdateLabel,

                selectLabel: selectLabel,
                deleteLabel: deleteLabel,
                deleteCamera: deleteCamera

            };
            init();
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function init() {
                setWatch();
            }

            function setWatch() {
                //watch router
                $scope.$watch(function() {
                    return angular.toJson(RouterStateService.getRouterState());
                }, function(newVal, oldVal) {
                    var routerState = angular.fromJson(newVal).toState;
                    var routerCheck = /\.label/.test(routerState.name);
                    if (!routerCheck) {
                        return false;
                    }
                    mainCtrl.block.promise = loadUI();
                }, true);

                //watch control
                $scope.$watch('labelCtrl.data.uiFilterVca.list', function(newVal, oldVal) {
                    var opt = labelCtrl.data;
                    if (newVal !== oldVal) {
                        $timeout(function() {
                            setTreeviewForFilter();
                        }, 300);
                    }
                }, true);

                $scope.$watch('labelCtrl.data.uiTreeview.searchValue', function(newVal, oldVal) {
                    var opt = labelCtrl.data;
                    if (newVal !== oldVal) {
                        $timeout(function() {
                            setTreeviewForFilter();
                        }, 300);
                    }
                }, true);

                $scope.$watch('labelCtrl.data.uiLabels.list', function(newVal, oldVal) {
                    var opt = labelCtrl.data;
                    var searchVal = $.trim(opt.uiLabels.searchLabelValue);
                    if (newVal !== oldVal) {
                        $timeout(function() {
                            LabelService.filterLabelList(searchVal);
                        }, 300);
                    }
                }, true);

                $scope.$watch('labelCtrl.data.uiLabels.selectCameraList', function(newVal, oldVal) {
                    var opt = labelCtrl.data;
                    var searchVal = $.trim(opt.uiLabels.searchCameraValue);
                    if (newVal !== oldVal) {
                        $timeout(function() {
                            LabelService.filterCameraList(searchVal);
                        }, 300);
                    }
                }, true);

                $scope.$watch('labelCtrl.data.uiLabels.searchLabelValue', function(newVal, oldVal) {
                    var opt = labelCtrl.data;
                    var searchVal = $.trim(newVal);
                    if (newVal !== oldVal) {
                        $timeout(function() {
                            LabelService.filterLabelList(searchVal);
                            LabelService.selectLabel();
                        }, 300);
                    }
                }, true);

                $scope.$watch('labelCtrl.data.uiLabels.searchCameraValue', function(newVal, oldVal) {
                    var opt = labelCtrl.data;
                    var searchVal = $.trim(newVal);
                    if (newVal !== oldVal) {
                        $timeout(function() {
                            LabelService.filterCameraList(searchVal);
                        }, 300);
                    }
                }, true);

                $scope.$watch('mainCtrl.header.isWrapperToggled', function(newVal, oldVal) {
                    var opt = labelCtrl.data;
                    if (newVal) {
                        opt.uiGuider.isShow = false;
                    }
                }, true);
            }

            function showMainMenu(checkState) {
                var opt = labelCtrl.data;
                opt.uiGuider.isShow = false;
                MainService.showMainMenu(checkState);
            }

            function loadUI() {
                var dfd = $q.defer();
                $timeout(function() {
                    LabelService.getDeviceTree()
                        .finally(function() {
                            setUI();
                            dfd.resolve();
                        })
                });
                return dfd.promise;
            }

            function setUI() {
                setTreeview();
                LabelService.setFilterLabelCamera();
                LabelService.selectLabel();
            }

            function setTreeview() {
                var opt = labelCtrl.data;
                var treeview = $(opt.$treeview).data("kendoTreeView");
                treeview.dataSource.filter({});
                treeview.dataSource.data(opt.uiTreeview.list);
            }

            function getTreeviewOptions() {
                return {
                    dragAndDrop: true,
                    template: "# if(item.isOnline == false){ #" +
                        "#=item.text # <i class='fa fa-times-circle-o' uib-tooltip='" + i18n('offline') + "' tooltip-placement='right'></i>" +
                        "# } else { #" +
                        "#=item.text #" +
                        "# } #",
                    dataSource: labelCtrl.data.uiTreeview.list,
                    //expand: OnKendoExpand,
                    dragstart: function(e) {
                        var opt = labelCtrl.data;
                        var treeview = $(opt.$treeview).data("kendoTreeView");
                        var uid = treeview.dataItem(e.sourceNode).uid;

                        $scope.$apply(function() {
                            opt.uiDragTarget.list = [];
                            opt.uiDragTarget.list.push(treeview.dataItem(e.sourceNode));
                            if (LabelService.getSelectLabel()) {
                                opt.uiDragTarget.isShow = true;
                            }
                        });

                    },
                    drag: function(e) {
                        var opt = labelCtrl.data;
                        if ($(e.dropTarget).attr('id') === "labelDragTarget" ||
                            $(e.dropTarget).parents(opt.$dragTarget).attr('id') === 'labelDragTarget') {
                            e.setStatusClass("k-add");
                        }
                    },
                    drop: function(e) {
                        e.preventDefault();
                        var opt = labelCtrl.data;
                        if ($(e.dropTarget).attr('id') !== "labelDragTarget" &&
                            $(e.dropTarget).parents(opt.$dragTarget).attr('id') !== 'labelDragTarget') {
                            e.setValid(false);
                        }
                        $scope.$apply(function() {
                            opt.uiDragTarget.isShow = false;
                        });

                    }
                };
            }

            function getDragTargetOptions() {
                return {
                    drop: function(e) {
                        var opt = labelCtrl.data;
                        var treeview = $(opt.$treeview).data("kendoTreeView");

                        $scope.$apply(function() {
                            if (LabelService.getSelectLabel() && opt.uiDragTarget.isShow) {
                                addCamera();
                            }
                        });
                    }
                };
            }

            function setTreeviewForFilter() {
                var opt = labelCtrl.data;
                var treeview = $(opt.$treeview).data("kendoTreeView");

                var filterVcaVal = getCurrectFilterVca();
                var filterSearchVal = $.trim(labelCtrl.data.uiTreeview.searchValue);

                var filterVca = (filterVcaVal.name === "all") ? {} : {
                    field: "filterVca",
                    operator: "contains",
                    value: filterVcaVal.type
                };
                var filterSearch = (filterSearchVal === "") ? {} : {
                    field: "filterText",
                    operator: "contains",
                    value: filterSearchVal
                };
                var filterConfig = (function(filterAry) {
                    var filter = [];
                    $.each(filterAry, function(i, data) {
                        if (!$.isEmptyObject(data)) {
                            filter.push(data);
                        }
                    });

                    if (filter.length === 0) {
                        filter = {};
                    };

                    return filter;
                })([filterVca, filterSearch]);

                var filterVcaCheck = (filterVcaVal.name == 'all') ? new RegExp('') : new RegExp(filterVcaVal.type);
                var filterSearchCheck = new RegExp(filterSearchVal, 'i');
                var uidExpandList = [];
                var execFilter = function() {
                    //init close all tree
                    treeview.collapse(".k-item");

                    //filter tree
                    $.each(treeview.dataSource.data(), function(i, labelData) {
                        if (labelData.children) {
                            var labelFilterText = (function() {
                                var tmpAry = labelData.filterText.split(',');
                                tmpAry.splice(0, 1);
                                return tmpAry.toString(',');
                            })();

                            //set tree expand list
                            (function() {
                                if (filterSearchVal === "") {
                                    if (filterVcaVal.name === "all") {}
                                    if (filterVcaVal.name !== "all") {
                                        if (filterVcaCheck.test($.trim(labelData.filterVca))) {
                                            uidExpandList.push(labelData.uid);
                                        }
                                    }
                                }
                                if (filterSearchVal !== "") {
                                    if (filterVcaVal.name === "all") {
                                        if (filterSearchCheck.test($.trim(labelFilterText))) {
                                            uidExpandList.push(labelData.uid);
                                        }
                                    }
                                    if (filterVcaVal.name !== "all") {
                                        if (filterSearchCheck.test($.trim(labelFilterText)) && filterVcaCheck.test($.trim(labelData.filterVca))) {
                                            uidExpandList.push(labelData.uid);
                                        }
                                    }
                                }
                            })();
                            //set filter data
                            labelData.children.filter(filterConfig);

                            $.each(labelData.children.data(), function(j, deviceData) {
                                if (deviceData.children) {
                                    var deviceFilterText = (function() {
                                        var tmpAry = deviceData.filterText.split(',');
                                        tmpAry.splice(0, 2);
                                        return tmpAry.toString(',');
                                    })();
                                    //set tree expand list
                                    (function() {
                                        if (filterSearchVal === "") {
                                            if (filterVcaCheck === "all") {}
                                            if (filterVcaCheck !== "all") {
                                                if (filterVcaCheck.test($.trim(deviceData.filterVca))) {
                                                    uidExpandList.push(deviceData.uid);
                                                }
                                            }
                                        }
                                        if (filterSearchVal !== "") {
                                            if (filterVcaCheck === "all") {
                                                if (filterSearchCheck.test($.trim(deviceFilterText))) {
                                                    uidExpandList.push(deviceData.uid);
                                                }
                                            }
                                            if (filterVcaCheck !== "all") {
                                                if (filterSearchCheck.test($.trim(deviceFilterText)) && filterVcaCheck.test($.trim(deviceData.filterVca))) {
                                                    uidExpandList.push(deviceData.uid);
                                                }
                                            }
                                        }
                                    })();
                                    //set filter data
                                    deviceData.children.filter(filterConfig);
                                }
                            });

                        }
                    });
                    treeview.dataSource.filter(filterConfig);

                    //open tree
                    $.each(uidExpandList, function(i, uid) {
                        treeview.expand('[data-uid="' + uid + '"]');
                    });
                };

                $timeout(function() { execFilter() });
            }

            function setCurrectFilterVca(index) {
                $.each(labelCtrl.data.uiFilterVca.list, function(i, list) {
                    list.isActive = (index == i) ? true : false;
                })
            }

            function getCurrectFilterVca() {
                var sortItems = [];
                $.each(labelCtrl.data.uiFilterVca.list, function(i, list) {
                    if (list.isActive) {
                        sortItems = list;
                        return false;
                    }
                })
                return sortItems;
            }

            function getLabelClass(id) {
                var opt = labelCtrl.data;
                var list = opt.uiLabels.listByFilter;
                var maxNum = opt.uiLabels.classCount;
                var index = (function() {
                    var index;
                    $.each(list, function(i, label) {
                        if (id === label.labelId) {
                            index = i;
                        }
                    });
                    return index;
                })();
                var classNum = (function() {
                    var num = index + 1;
                    if (num > maxNum) {
                        num = (num % maxNum == 0) ? maxNum : num % maxNum;
                    }
                    return num;
                })();
                var key = "kupColor" + classNum;
                var tmpObj = {};

                tmpObj[key] = '$index';
                tmpObj['kupActive'] = list[index].isSelect;
                return tmpObj;
            }

            function getLabelTagClass(type) {
                var className;
                $.each(LabelModalService.data.uiLabelForm.labelType, function(i, label) {
                    if (type === label.value) {
                        className = label.class;
                    }
                });
                return className;
            }

            function deleteLabel(labelData, e) {
                e.preventDefault();
                e.stopPropagation();
                var deleteCtrl = function() {
                    var opt = labelCtrl.data;
                    var param = {
                        labelId: labelData.labelId,
                    };
                    LabelService.removeLabelApi(param)
                        .success(function() {
                            LabelService.getDeviceTree()
                                .finally(function() {
                                    LabelService.setFilterLabelCamera();
                                    LabelService.selectLabel();
                                });
                        })
                        .error(function(response) {
                            notification('error', i18n("server-error"));
                        });
                }
                var msg = i18n('are-you-sure-want-to-delete-label').replace('%s', labelData.labelName);
                utils.popupConfirm(i18n('confirm-delete-label'), msg, deleteCtrl);
            }

            function deleteCamera(cameraData) {
                var isNode = cameraData.isNode;
                var labelData = LabelService.getSelectLabel();
                var param = (function() {
                    var param = {};
                    param = {
                        labelId: labelData.labelId,
                        deviceId: cameraData.deviceId,
                        platformDeviceId: cameraData.platformDeviceId,
                        cameraId: cameraData.cameraId
                    }
                    return param;
                })();

                var deleteNodeCameraApi = function() {
                    LabelService.unassignChannelLabelApi(param)
                        .success(function() {
                            LabelService.getDeviceTree()
                                .finally(function() {
                                    LabelService.setFilterLabelCamera();
                                    LabelService.selectLabelById(labelData.labelId);
                                });
                        })
                        .error(function(response) {
                            notification('error', i18n("server-error"));
                        });
                };
                deleteNodeCameraApi();
            }

            function addCamera() {
                setDragTargetList();
                var opt = labelCtrl.data;
                var requestCount = 0;
                var cameraList = angular.copy(labelCtrl.data.uiDragTarget.list);
                var requestTotal = cameraList.length;
                var labelData = LabelService.getSelectLabel();
                var addNodeCameraApi = function(cameraData) {
                    LabelService.assignChannelLabelApi(cameraData)
                        .success(function(response) {
                            if (response.reason) {
                                notification('error', i18n(response.reason) + '<br>[ ' + i18n('camera-name') + ': ' + cameraData.cameraName + ' ]', 5000);
                            }
                        })
                        .finally(function() {
                            requestCount++;
                            if (requestCount < requestTotal) {
                                addNodeCameraApi(cameraList[requestCount]);
                            }
                            if (requestCount == requestTotal) {
                                LabelService.getDeviceTree()
                                    .finally(function() {
                                        LabelService.setFilterLabelCamera();
                                        LabelService.selectLabelById(labelData.labelId);
                                    })
                                    .error(function(response) {
                                        notification('error', i18n("server-error"));
                                    });
                            }
                        });
                };
                if (requestTotal) {
                    addNodeCameraApi(cameraList[requestCount]);
                }

            }

            function showAddLabel() {
                showUpdateLabel();
            }

            function showUpdateLabel(id) {
                var opt = labelCtrl.data;
                var optModal = LabelModalService.data;
                //labelCtrl.data.uiLabels.isAddShow = isShow;
                var modalInstance = $uibModal.open({
                    animation: true,
                    templateUrl: 'labelModal.tmpl.html',
                    controller: 'LabelModalController',
                    controllerAs: 'labelModalCtrl',
                    windowClass: 'modal kupModal',
                    size: '',
                    resolve: {
                        updateLabelData: function() {
                            return (id) ? LabelService.getLabelById(id) : "";
                        }
                    }
                });

                modalInstance.opened.then(
                    //open fn
                    function() {
                        optModal.isOpenModal = true;
                    });
                modalInstance.result.then(
                    //close fn
                    function() {
                        optModal.currectStep = 0;
                        optModal.isOpenModal = false;
                    },
                    //cancel fn
                    function() {
                        optModal.currectStep = 0;
                        optModal.isOpenModal = false;
                    });
            }

            function setDragTargetList() {
                var opt = labelCtrl.data;
                var cameraList = [];
                var labelData = LabelService.getSelectLabel();
                var obj = {};
                $.each(opt.uiDragTarget.list, function(i, item) {
                    if (item.isAll) {
                        var deviceData = item.items;
                        $.each(deviceData, function(j, device) {
                            var cameraData = device.items;
                            $.each(cameraData, function(k, camera) {
                                obj = {};
                                obj.labelId = labelData.labelId;
                                obj.deviceId = camera.deviceId;
                                obj.platformDeviceId = camera.platformDeviceId;
                                obj.cameraId = camera.cameraId;
                                obj.cameraName = camera.cameraName;
                                obj.isNode = device.isNode;
                                cameraList.push(obj);
                            });
                        });
                    }
                    if (item.isDevice) {
                        var cameraData = item.items;
                        $.each(cameraData, function(j, camera) {
                            obj = {};
                            obj.labelId = labelData.labelId;
                            obj.deviceId = camera.deviceId;
                            obj.platformDeviceId = camera.platformDeviceId;
                            obj.cameraId = camera.cameraId;
                            obj.cameraName = camera.cameraName;
                            obj.isNode = item.isNode;
                            cameraList.push(obj);
                        });

                    }
                    if (item.isCamera) {
                        obj = {};
                        obj.labelId = labelData.labelId;
                        obj.deviceId = item.deviceId;
                        obj.platformDeviceId = item.platformDeviceId;
                        obj.cameraId = item.cameraId;
                        obj.cameraName = item.cameraName;
                        obj.isNode = item.isNode;
                        cameraList.push(obj);
                    }
                });
                opt.uiDragTarget.list = cameraList;
            }

            function getSelectLabel() {
                return LabelService.getSelectLabel();
            }

            function showGuider(isShow) {
                var opt = labelCtrl.data;
                opt.uiGuider.isShow = isShow;
            }

            function showGuiderCheck() {
                var opt = labelCtrl.data;
                var optModal = LabelModalService.data;
                return !DeviceTreeService.isEmptyDevice() && !opt.uiDragTarget.isShow && !opt.uiLabels.selectCameraList.length && getSelectLabel() && opt.uiGuider.isShow && !opt.uiLabels.isAddShow && !optModal.currectStep;
            }

            function selectLabel(index) {
                LabelService.selectLabel(index);
            }

            function selectLabelById(id) {
                LabelService.selectLabelById(id);
            }
        });
