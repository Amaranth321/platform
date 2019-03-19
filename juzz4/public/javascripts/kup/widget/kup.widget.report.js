/**
 * [Javascript Module Pattern]
 * widget for report
 * @param  {object} $   plug-in:jQuery
 * @param  {object} kup KUP module
 * @return {object}     public member
 * @author andy.gan@kaisquare.com.tw
 */

KUP.widget.report = (function($, kup) {
    var _option = {
            //current vca info
            reportType: '', //current report page          
            menuTabType: '#pCountingReportTab', //if url not to assign report type
            selectedType: '#singleTab', //#singleTab,#multipleTab
            chartRadioType: '#charts', //#charts,#timecard
            //for check status
            isSuccessReport: false, //true is success generate reports           
            isMultiDeviceSelected: false, // true if label containing multiple device is selected and disable excel
            isSingleType: true,
            isDragging: false,
            isDefaultDropData: true,
            isUpdateSelectedItem: false,
            isOnlyShowVcaTreeView: false,
            //dateRangePicker selected
            days: 7, //default date range
            startDate: '',
            endDate: '',
            //default item info
            defaultElementList: {},
            defaultItemDataList: {},
            defaultDateRange: {
                startDate: '',
                endDate: ''
            },
            //for save selected item info
            selectedElementList: {},
            selectedItemDataList: {},
            selectedSaveDataList: [],
            selectedGroupNames: [],
            //for attention report  
            selectedDeviceList: [],
            selectedChannelList: [],
            //for other vca report
            selectedInstance: [],
            selectedDevices: [],
            //from api reponse data
            apiUserDevicesList: [],
            apiLabels: [],
            apiRunningAnalyticsList: [],
            apiQueryHistoryData: {},
            //other info
            posNames: [], //from POS api
            groupNames: [], //for only select label
            hideConversionRation: false,
            keypressOnHold: null,
            //for treeview drag type
            label: "label",
            device: "device",
            channel: "channel",
            deviceTreeView: null,
            isDeviceTreeViewinitialized: false,
            selectedItems: [],
        },
        _map = {
            /*reportType: ['trafficflow', 'peoplecounting', 'passerby', 'crowd', 'profiling', 'attention', 'intrusion', 'pdefense', 'loitering', 'objcounting', 'videoblur', 'faceindexing'],*/
    		//add by rezongke for object-detection report
    		reportType: ['trafficflow', 'peoplecounting', 'passerby', 'crowd', 'profiling', 'attention', 'intrusion', 'pdefense', 'loitering', 'objcounting', 'videoblur', 'faceindexing','objdetect'],
            vcaAnalyticsType: {
                //VCA INTEL
                trafficflow: kup.getOpt('analyticsType').TRAFFIC_FLOW,
                peoplecounting: kup.getOpt('analyticsType').PEOPLE_COUNTING,
                passerby: kup.getOpt('analyticsType').PASSERBY,
                crowd: kup.getOpt('analyticsType').CROWD_DETECTION,
                profiling: kup.getOpt('analyticsType').AUDIENCE_PROFILING,
                attention: kup.getOpt('analyticsType').AUDIENCE_PROFILING,
                objdetect: kup.getOpt('analyticsType').OBJECT_DETECTION,
                //VCA SECURITY
                intrusion: kup.getOpt('analyticsType').AREA_INTRUSION,
                pdefense: kup.getOpt('analyticsType').PERIMETER_DEFENSE,
                loitering: kup.getOpt('analyticsType').AREA_LOITERING,
                objcounting: kup.getOpt('analyticsType').OBJECT_COUNTING,
                videoblur: kup.getOpt('analyticsType').VIDEO_BLUR,
                faceindexing: kup.getOpt('analyticsType').FACE_INDEXING
                
            },
            vcaEventType: {
                //VCA INTEL
                trafficflow: kup.getOpt('event').TRAFFIC_FLOW,
                peoplecounting: kup.getOpt('event').PEOPLE_COUNTING,
                passerby: kup.getOpt('event').PASSERBY,
                crowd: kup.getOpt('event').CROWD_DETECTION,
                profiling: kup.getOpt('event').PROFILING,
                attention: kup.getOpt('event').PROFILING,
                objdetect: kup.getOpt('event').OBJECT_DETECTION,
                //VCA SECURITY
                intrusion: kup.getOpt('event').INTRUSION,
                pdefense: kup.getOpt('event').PERIMETER_DEFENSE,
                loitering: kup.getOpt('event').LOITERING,
                objcounting: kup.getOpt('event').OBJECT_COUNTING,
                videoblur: kup.getOpt('event').VIDEO_BLUR,
                faceindexing: kup.getOpt('event').FACE_INDEXING
            },
            menuTab: {
                //VCA INTEL
                trafficflow: '#trafficReportTab',
                peoplecounting: '#pCountingReportTab',
                passerby: '#passerbyReportTab',
                crowd: '#crowdReportTab',
                profiling: '#profilingReportTab',
                attention: '#attentionReportTab',
                objdetect: '#objDetectReportTab',
                //VCA SECURITY
                intrusion: '#intrusionReportTab',
                pdefense: '#pdefenseReportTab',
                loitering: '#loiteringReportTab',
                objcounting: '#objCountingReportTab',
                videoblur: '#videoBlurReportTab',
                faceindexing: '#faceReportTab'
            },
            title: {
                //VCA INTEL
                trafficflow: 'report-traffic-flow',
                peoplecounting: 'report-people-counting',
                passerby: 'analytics-passerby',
                crowd: 'report-crowd-detection',
                profiling: 'report-audience-profiling',
                attention: 'report-audience-attention',
                objdetect: 'report-object-detection',
                //VCA SECURITY
                intrusion: 'report-area-intrusion',
                pdefense: 'report-perimeter-defense',
                loitering: 'report-area-loitering',
                objcounting: 'report-object-counting',
                videoblur: 'report-video-blur',
                faceindexing: 'report-face-indexing'
            },
            selectedType: {
                //VCA INTEL
                trafficflow: ['#singleTab'],
                peoplecounting: ['#singleTab', '#multipleTab'],
                passerby: ['#singleTab', '#multipleTab'],
                crowd: ['#singleTab'],
                profiling: ['#singleTab', '#multipleTab'],
                attention: ['#singleTab'],
                objdetect: ['#singleTab'],
                //VCA SECURITY
                intrusion: ['#singleTab', '#multipleTab'],
                pdefense: ['#singleTab', '#multipleTab'],
                loitering: ['#singleTab', '#multipleTab'],
                objcounting: ['#singleTab', '#multipleTab'],
                videoblur: ['#singleTab', '#multipleTab'],
                faceindexing: ['#singleTab']
            },
            chartRadio: {
                //VCA INTEL
                trafficflow: [],
                peoplecounting: ['#charts', '#timecard'],
                passerby: ['#charts'],
                crowd: [],
                profiling: ['#charts'],
                attention: ['#charts'],
                //VCA SECURITY
                intrusion: ['#charts'],
                pdefense: ['#charts'],
                loitering: ['#charts'],
                objcounting: ['#charts'],
                videoblur: ['#charts'],
                faceindexing: ['#charts']
            },
            chartRadioToShowDiv: {
                //VCA INTEL
                trafficflow: {},
                peoplecounting: {
                    '#charts': ['#lineChartContainer'],
                    '#timecard': ['#timeCardWrapper']
                },
                passerby: {
                    '#charts': ['#lineChartContainer']
                },
                crowd: {},
                profiling: {
                    '#charts': ['#donutChartContainer', '#crosssite_report_container'],
                },
                attention: {
                    '#charts': ['#barChart', '#lineChart']
                },
                //VCA SECURITY
                intrusion: {
                    '#charts': ['#lineChartContainer']
                },
                pdefense: {
                    '#charts': ['#lineChartContainer']
                },
                loitering: {
                    '#charts': ['#lineChartContainer']
                },
                objcounting: {
                    '#charts': ['#lineChartContainer']
                },
                videoblur: {
                    '#charts': ['#lineChartContainer']
                },
                faceindexing: {}
            },
            successReportToShowDiv: {
                //VCA INTEL
                trafficflow: [],
                peoplecounting: ['.plpcount_buttom_wrapper'],
                passerby: ['.plpcount_buttom_wrapper'],
                crowd: [],
                profiling: ['#donutChartContainer', '#crosssite_report_container', '.report_buttom_wrapper'],
                attention: [],
                //VCA SECURITY
                intrusion: ['.plpcount_buttom_wrapper'],
                pdefense: ['.plpcount_buttom_wrapper'],
                loitering: ['.plpcount_buttom_wrapper'],
                objcounting: ['.plpcount_buttom_wrapper'],
                videoblur: ['.plpcount_buttom_wrapper'],
                faceindexing: ['.plpcount_buttom_wrapper']
            },
            loadVcaToShowExport: {
                //VCA INTEL
                trafficflow: ['#pdfOutput', '#csvOutput'],
                peoplecounting: ['#pdfOutput', '#csvOutput'],
                passerby: ['#csvOutput'], //no pdf in beta version
                crowd: ['#pdfOutput', '#csvOutput'],
                profiling: ['#pdfOutput', '#csvOutput'],
                attention: [],
                //VCA SECURITY
                intrusion: ['#pdfOutput', '#csvOutput'],
                pdefense: ['#pdfOutput', '#csvOutput'],
                loitering: ['#pdfOutput', '#csvOutput'],
                objcounting: ['#pdfOutput', '#csvOutput'],
                videoblur: ['#pdfOutput', '#csvOutput'],
                faceindexing: ['#pdfOutput', '#csvOutput']
            },
            selectedTypeToShowExport: {
                "#singleTab": ['#pdfOutput', '#csvOutput'],
                "#multipleTab": ['#pdfOutput', '#csvOutput']
            }
        },
        _self = {
            type: {},
            setOpt: function(config) {
                _option = $.extend(false, {}, _option, config || {});
            },
            getOpt: function(key) {
                var deepCopy = $.extend(true, {}, _option);
                return (!!key) ? deepCopy[key] : deepCopy;
            },
            getMap: function(key) {
                var deepCopy = $.extend(true, {}, _map);
                return (!!key) ? deepCopy[key] : deepCopy;
            },
            getData: {
                selectedItemsLength: function() {
                    var opt = _self.getOpt(),
                        count = 0,
                        selectedItemDataList = opt.selectedItemDataList;
                    $.each(selectedItemDataList, function(uid, itemData) {
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
                        count++;
                    });
                    return count;
                }
            },
            setData: {
                selectedInstance: function() {
                    var opt = _self.getOpt(),
                        selectedItemDataList = opt.selectedItemDataList;

                    opt.selectedInstance = [];
                    $.each(selectedItemDataList, function(uid, itemData) {
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
                        if (itemData.type === opt.label) { //drag root or labels
                            var device = itemData.items || [];
                            $.each(device, function(i, deviceData) {
                                var camera = deviceData.items || [];
                                $.each(camera, function(j, cameraData) {
                                    opt.selectedInstance.push(cameraData);
                                });
                            });
                        } else if (itemData.type === opt.device) { //drag device
                            var camera = itemData.items || [];
                            $.each(camera, function(i, cameraData) {
                                opt.selectedInstance.push(cameraData);
                            });
                        } else if (itemData.type === opt.channel) { //drag camera
                            var cameraData = itemData;
                            opt.selectedInstance.push(cameraData);
                        }
                    });
                    _self.setOpt(opt);
                    return opt.selectedInstance;
                },
                selectedSaveDataList: function() {
                    var opt = _self.getOpt(),
                        selectedItemDataList = opt.selectedItemDataList;

                    opt.selectedSaveDataList = [];
                    $.each(selectedItemDataList, function(uid, itemData) {
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
                        if (itemData.type === opt.label) { //drag root or labels
                            opt.selectedSaveDataList.push({
                                label: itemData.text,
                                deviceId: null,
                                channelId: null
                            });
                        } else if (itemData.type === opt.device) { //drag device
                            opt.selectedSaveDataList.push({
                                label: itemData.groupName,
                                deviceId: itemData.platformDeviceId,
                                channelId: null
                            });
                        } else if (itemData.type === opt.channel) { //drag camera
                            opt.selectedSaveDataList.push({
                                label: itemData.groupName,
                                deviceId: itemData.platformDeviceId,
                                channelId: itemData.channelId
                            });
                        }
                    });
                    _self.setOpt(opt);
                    return opt.selectedSaveDataList;
                },
                selectedDevices: function() {
                    var opt = _self.getOpt(),
                        selectedInstance = opt.selectedInstance,
                        deviceIds = [],
                        channelIds = [],
                        selectedGroup = {};

                    opt.selectedDevices = [];
                    var apiRunningAnalyticsList=opt.apiRunningAnalyticsList;
                    $.each(selectedInstance, function(index, inst) {                    	
                    	if(opt.isOnlyShowVcaTreeView==true){                   		
                    		 $.each(apiRunningAnalyticsList, function(i, apiRunningAnalytic){
                    			 if(apiRunningAnalytic.platformDeviceId==inst.platformDeviceId
                     					&&apiRunningAnalytic.channelId==inst.channelId){
                     				deviceIds.push(inst.platformDeviceId);
                         			channelIds.push(inst.channelId);                        
                     			}
                    		 });
                    	}else{
                    			deviceIds.push(inst.platformDeviceId);
                    			channelIds.push(inst.channelId);
                    		}
                    });
                    selectedGroup.platformDeviceId = deviceIds;
                    selectedGroup.channelId = channelIds;
                    opt.selectedDevices.push(selectedGroup);

                    _self.setOpt(opt);
                    return opt.selectedDevices;
                },
                groupNames: function() {
                    var opt = _self.getOpt(),
                        selectedItemDataList = opt.selectedItemDataList,
                        groupNames = [];

                    $.each(selectedItemDataList, function(uid, itemData) {
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
                        if (itemData.type != opt.label) {
                            groupNames = [];
                            return false;
                        }
                        groupNames.push(itemData.labelName);
                    });
                    opt.groupNames = groupNames;
                    _self.setOpt(opt);
                    return opt.groupNames;
                },
                defaultItemDataList: function() {
                    var opt = _self.getOpt(),
                        itemDataQuery = opt.apiQueryHistoryData.deviceSelected || [],
                        itemData = opt.deviceTreeView.getDefaultDeviceTreeData();
                    opt.defaultItemDataList = {};
                    $.each(itemDataQuery, function(i, queryData) {
                        if (queryData.deviceId !== "" && queryData.channelId !== "") { // is camera
                        	$.each(itemData, function(j, labelData) {
                                $.each(labelData.items, function(k, deviceData) {
                                    if (deviceData.platformDeviceId == queryData.deviceId) {
                                        var uid = "";
                                        $.each(deviceData.items, function(l, cameraData) {
                                            if (cameraData.channelId == queryData.channelId) {
                                                uid = deviceData.platformDeviceId + "_" + cameraData.channelId;
                                                opt.defaultItemDataList[uid] = cameraData;
                                                return false;
                                            }
                                        });
                                        return false;
                                    }
                                });
                                return false;
                            });
                        } else if (queryData.deviceId !== "") { //is device
                        	$.each(itemData, function(j, labelData) {
                                $.each(labelData.items, function(k, deviceData) {
                                    if (deviceData.platformDeviceId == queryData.deviceId) {
                                        var uid = labelData.labelId + "_" + deviceData.platformDeviceId;
                                        opt.defaultItemDataList[uid] = deviceData;
                                        return false;
                                    }
                                });
                                return false;
                            });
                        } else {
                            $.each(itemData, function(j, labelData) { //is root or label
                                if (labelData.text.toLowerCase() === queryData.label.toLowerCase()) {
                                    var uid = labelData.labelId;
                                    opt.defaultItemDataList[uid] = labelData;
                                    return false;
                                }
                            });
                        }
                    });
                    _self.setOpt(opt);
                    return opt.defaultItemDataList;
                },
                defaultElementList: function() {
                    var opt = _self.getOpt(),
                        defaultItemDataList = opt.defaultItemDataList;

                    opt.defaultElementList = {};
                    $.each(defaultItemDataList, function(uid, data) {
                        opt.defaultElementList[uid] = data;
                    });
                    _self.setOpt(opt);
                    return opt.defaultElementList;
                },
                defaultDateRange: function() {
                    var opt = _self.getOpt(),
                        kupOpt = kup.getOpt(),
                        utils = kup.utils.default,
                        apiQueryHistoryData = opt.apiQueryHistoryData,
                        //dateRangePicker = $('#calendar-reservation').data('daterangepicker'),
                        startDateForDefaultRange = new Date(moment().subtract('days', opt.days - 1).format('YYYY/MM/DD 00:00:00')),
                        endDateForDefaultRange = new Date(moment().format('YYYY/MM/DD 23:59:59'));

                    opt.defaultDateRange.startDate = (function() {
                        var startDate = kendo.parseDate(apiQueryHistoryData.dateFrom || '', kupOpt.dateFormat),
                            startDateToLocal = (startDate) ? utils.convertUTCtoLocal(startDate) : startDateForDefaultRange;
                        return startDateToLocal;
                    })();
                    opt.defaultDateRange.endDate = (function() {
                        var endDate = kendo.parseDate(apiQueryHistoryData.dateTo || '', kupOpt.dateFormat),
                            endDateToLocal = (endDate) ? utils.convertUTCtoLocal(endDate) : endDateForDefaultRange;
                        return endDateToLocal;
                    })();
                    _self.setOpt(opt);
                },
            },
            setUI: {
                menuTab: function() {
                    var opt = _self.getOpt(),
                        callBack = {
                            onSelect: function onSelect(e) {
                                var opt = _self.getOpt(),
                                    isDefaultDropData = opt.isDefaultDropData;
                                opt.isSingleType = (e.item.id === 'singleTab') ? true : false;
                                opt.selectedType = "#" + e.item.id;
                                $(e.item).addClass('is_active').siblings().removeClass("is_active");
                                _self.setOpt(opt);
                                _self.updateUI.showExport();
                                if (!isDefaultDropData) {
                                    _self.updateUI.removeDropPlace();
                                }

                            }
                        }
                    $("#subMenuTab").kendoTabStrip({
                        select: callBack.onSelect
                    });
                },
                /**
                 * [semantic ui]
                 * @http://semantic-ui.com/modules/checkbox.html
                 */
                menuOptions: function() {
                    $('#showVcaSwitch').checkbox({
                        onChecked: function() {
                            var opt = _self.getOpt();
                            opt.isOnlyShowVcaTreeView = true;
                            _self.setOpt(opt);
                            _self.updateUI.switchTreeView();
                        },
                        onUnchecked: function() {
                            var opt = _self.getOpt();
                            opt.isOnlyShowVcaTreeView = false;
                            _self.setOpt(opt);
                            _self.updateUI.switchTreeView();
                        }
                    });
                    $('#deivceOptionsBtn').popup({
                        position: 'bottom center',
                    });
                    $('#deivceOptionsBtn').on('click', function(event) {
                        event.preventDefault();
                        $('#deivceOptions').slideToggle("slow");
                    });
                    $('body').on('click', function(event) {
                        if ($('#deivceOptions').css('display') === 'none') {
//                            return;
                        }
                        if ($(event.target).parents('#deivceOptions').attr('id') === $('#deivceOptions').attr('id') ||
                            $(event.target).attr('id') === $('#deivceOptions').attr('id') ||
                            $(event.target).attr('id') === $('#deivceOptionsBtn').attr('id')) {
                            return;
                        }
//                        $('#deivceOptions').slideToggle("slow");
                    });
                },
                dropPlace: function() {},
                updateBtn: function() {
                    var utils = kup.utils.default,
                        i18n = kup.utils.i18n;

                    $('#applyBtn').on('click', function(event) {
                        $('#applyBtn').hide();
                        $('#cancelBtn').hide();
                        event.preventDefault();
                        _self.exec.saveReportQueryHhistory();
                        _self.exec.generateReport();
                    });
                    $('#cancelBtn').on('click', function(event) {
                        $('#applyBtn').hide();
                        $('#cancelBtn').hide();
                        event.preventDefault();
                        var opt = _self.getOpt(),
                            applyArg = [];

                        applyArg.push(_self.exec.getReportQueryHistory());
                        opt.isDefaultDropData = true;
                        _self.setOpt(opt);

                        $.when.apply(
                            $, applyArg
                        ).always(function() {
                            //do something
                        }).done(function() {
                            _self.updateUI.dateRangePicker();
                            _self.updateUI.removeDropPlace();
                            _self.setUI.onDragFuc();
                            _self.setUI.onDropFuc();
                            _self.updateUI.defaultDropPlace();
                            //re-generate report
                            _self.exec.generateReport();
                        }).fail(function() {
                            var opt = _self.getOpt();
                            opt.isDefaultDropData = false;
                            _self.setOpt(opt);
                            utils.popupAlert(i18n('server-error'));
                        });
                    });
                },
                onDragFuc: function(dataItem, e) {
                    var opt = _self.getOpt(),
                        isDefaultDropData = opt.isDefaultDropData,
                        isSingleType = opt.isSingleType;

                    //check default drag or not
                    if (typeof dataItem == 'undefined') {
                        isDefaultDropData = true;
                    } else {
                        isDefaultDropData = false;
                    }

                    //clear object
                    draggedElementList = {};
                    draggedDeviceList = {};

                    if (isDefaultDropData) {
                        draggedElementList = opt.defaultElementList;
                        draggedDeviceList = opt.defaultItemDataList;
                    } else {
                        var uid = "";
                        //drag is label or all items
                        if (dataItem.type === opt.label) {
                            uid = dataItem.labelId;
                        }

                        //drag is device item
                        if (dataItem.type === opt.device) {
                            uid = dataItem.labelId + "_" + dataItem.platformDeviceId;
                        }

                        //drag is camera item
                        if (dataItem.type === opt.channel) {
                            uid = dataItem.platformDeviceId + "_" + dataItem.channelId;
                        }

                        draggedElementList[uid] = dataItem;
                        draggedDeviceList[uid] = dataItem;
                    }

                    //block same item dragging
                    $.each(draggedDeviceList, function(uid, draggedDevice) {
                        if (isSingleType) {
                            if (opt.selectedItems.length >= 1) {
                                e.preventDefault();
                                return false;
                            }
                        } else {
                            if($.inArray(uid, opt.selectedItems) != -1) {
                                e.preventDefault();
                                return false;
                            }
                        }

                        //check drag item empty or not
                        var type = draggedDevice.type;
                        if (type === opt.label) {
                            if (typeof draggedDevice.items == 'undefined') {
                                e.preventDefault();
                                return false;
                            }
                        }

                        if (type === opt.device) {
                            if (typeof draggedDevice.items == 'undefined') {
                                e.preventDefault();
                                return false;
                            }
                        }

                        if (!isDefaultDropData) {
                            $(".drop-here").css('display', 'none');
                            opt.isDragging = false;
                            _self.setOpt(opt);
                        }
                    });
                },
                onDropFuc: function(dataItem, e) {
                    var opt = _self.getOpt(),
                        isDefaultDropData = opt.isDefaultDropData,
                        isSingleType = opt.isSingleType;

                    var removeSelectedDevice = function(e) {
                        e.preventDefault();
                        var opt = _self.getOpt(),
                            selectedDevice = $(e.currentTarget).parent('span'),
                            deviceIds = selectedDevice.attr('deviceId').split(","),
                            channelId = selectedDevice.attr('channelId'),
                            uid = selectedDevice.attr('data-uid'),
                            labelName = opt.selectedItemDataList[uid].id || '',
                            selectedGroupNames = opt.selectedGroupNames,
                            setOption = (function() {
                                //set tree view
                                $(selectedDevice).remove();
                                $("#deviceTree").removeClass('isDrag');

                                //delete item need to remove from array
                                if ($.inArray(uid, opt.selectedItems) != -1) {
                                    opt.selectedItems.splice(opt.selectedItems.indexOf(uid) ,1);
                                }

                                //set selected var
                                $.each(deviceIds, function(index, id) {
                                    opt.selectedDeviceList.splice(opt.selectedDeviceList.indexOf(id), 1);
                                })
                                if (channelId != "") {
                                    opt.selectedChannelList.splice(opt.selectedChannelList.indexOf(channelId), 1);
                                }
                                $.each(selectedGroupNames, function(i, name) {
                                    if (labelName === name) {
                                        opt.selectedGroupNames.splice(i, 1);
                                    }
                                });
                                //clear object,if use 'delete',the 'labelName' have some unknow error
                                opt.selectedElementList[uid] = {};
                                opt.selectedItemDataList[uid] = {};

                                opt.isUpdateSelectedItem = true;
                                _self.setOpt(opt);
                            })(),
                            setData = (function() {
                                _self.setData.selectedInstance();
                                _self.setData.selectedDevices();
                                _self.setData.selectedSaveDataList();
                                _self.setData.groupNames();
                            })(),
                            setUI = (function() {
                                _self.updateUI.showUpdateBtn();
                            })();
                    };

                    var setOption = (function() {
                            $.each(draggedDeviceList, function(uid, draggedDevice) {
                                var deviceId = "",
                                    channelId = "",
                                    type = draggedDevice.type,
                                    name = draggedDevice.text;

                                //set selected var
                                if (type === opt.label) { //drag root or label
                                    var len = draggedDevice.items.length;
                                    opt.selectedGroupNames.push(name);
                                    var deviceDragged = draggedDevice.items;
                                    $.each(deviceDragged, function(index, device) {
                                        opt.selectedDeviceList.push(device.platformDeviceId);
                                        if (index == len - 1) {
                                            deviceId += device.platformDeviceId;
                                        } else {
                                            deviceId += device.platformDeviceId + ",";
                                        }
                                    });
                                } else if (type === opt.device) { //drag device
                                    opt.selectedGroupNames = [];
                                    opt.selectedDeviceList.push(draggedDevice.platformDeviceId);
                                    deviceId += draggedDevice.platformDeviceId;
                                    name = draggedDevice.labelName + " - " + draggedDevice.text;
                                } else if (type === opt.channel) { //drag camera.
                                    opt.selectedGroupNames = [];
                                    opt.selectedDeviceList.push(draggedDevice.platformDeviceId);
                                    opt.selectedChannelList.push(draggedDevice.channelId);
                                    deviceId += draggedDevice.platformDeviceId;
                                    channelId += draggedDevice.channelId;
                                    name = draggedDevice.deviceName + " - " + draggedDevice.text;
                                }

                                opt.selectedElementList[uid] = draggedElementList[uid];
                                opt.selectedItemDataList[uid] = draggedDeviceList[uid];
                                opt.isUpdateSelectedItem = true;

                                //save selected items
                                opt.selectedItems.push(uid);

                                //add drop place div
                                var htmlString = "<span class='drop-item pos-r isActive' data-uid='" + uid + "' deviceId='" + deviceId + "' channelId='" + channelId +
                                    "'>" + name + "<a class='pos-r btn-remove ir'></a></span>";
                                $(htmlString).insertBefore('.drop-here');
                            })
                            _self.setOpt(opt);
                        })(),
                        setData = (function() {
                            _self.setData.selectedInstance();
                            _self.setData.selectedDevices();
                            _self.setData.selectedSaveDataList();
                            _self.setData.groupNames();
                        })(),
                        setEvent = (function() {
                            //bind event & set isDrag item
                            $('.drop-item > a').on('click', removeSelectedDevice);
                            if ((isDefaultDropData && !$.isEmptyObject(draggedDeviceList)) ||
                                (!isDefaultDropData && isSingleType)) {
                                $("#deviceTree").addClass('isDrag');
                            }
                        })(),
                        setUI = (function() {
                            _self.updateUI.showUpdateBtn();
                        })();
                },
                treeView: function() {
                    var opt = _self.getOpt(),
                        kupOpt = kup.getOpt(),
                        KupEvent = kupOpt.event,
                        deviceManager = kup.utils.deviceManager,
                        i18n = kup.utils.i18n;

                    var initializedFuc = function() {
                        //set data when init
                        var draggedDeviceList = _self.setData.defaultItemDataList();
                        var draggedElementList = _self.setData.defaultElementList();

                        //update default selected area
                        _self.updateUI.switchTreeView();
                        _self.updateUI.defaultDropPlace();

                        var opt = _self.getOpt();
                        opt.isDeviceTreeViewinitialized = true;
                        _self.setOpt(opt);
                    };

                    //generate tree view
                    opt.deviceTreeView = new DeviceTreeView("#deviceTree");
                    opt.deviceTreeView.initWithDragNDrop(".drop-here, #main-header, #main-charts", _self.setUI.onDragFuc, _self.setUI.onDropFuc, initializedFuc);

                    //set opt
                    _self.setOpt(opt);

                    $("#deviceTree").find('img').addClass("node-status-image");

                    $('#deviceTree').on("mouseover", "span", function() {
                        var $item = $(this).parent().parent();
                        if (!$item.hasClass('isDrag') && !$("#deviceTree").hasClass('isDrag')) {
                            $(".drop-here").css('display', 'block')
                        }

                    });
                    $('#deviceTree').on("mouseout", "span", function() {
                        var opt = _self.getOpt();
                        if (!opt.isDragging) {
                            $(".drop-here").css('display', 'none')
                        }
                    });
                },
                searchTreeView: function() {
                    //hide or show when searching
                    $('#search-term').keyup(function (e) {
                        var opt = _self.getOpt(),
                            isOnlyShowVcaTreeView = opt.isOnlyShowVcaTreeView,
                            vcaEventType = _map.vcaAnalyticsType[opt.reportType];


                        var executeSearch = function (searchObj) {
                            var filterText = $(searchObj).val();
                            if (isOnlyShowVcaTreeView) {
                                opt.deviceTreeView.filterTreeView(filterText, vcaEventType);
                            } else {
                                opt.deviceTreeView.filterTreeView(filterText);
                            }
                        }

                        //clear timeout if user typing fast
                        if(opt.keypressOnHold != null)
                        {
                            clearTimeout(opt.keypressOnHold);
                            opt.keypressOnHold = null;
                        }

                        var searchObj = this;
                        opt.keypressOnHold = setTimeout(function(){
                            console.log("search keyword: " +$(searchObj).val());
                            executeSearch(searchObj);
                        },300);
                        _self.setOpt(opt);
                    });
                },

                /**
                 * [bootstrap-daterangepicker]
                 * @http://www.dangrossman.info/2012/08/20/a-date-range-picker-for-twitter-bootstrap/
                 */
                dateRangePicker: function() {
                    var opt = _self.getOpt(),
                        days = opt.days,
                        showDefaultDate = function(start, end) {
                            var opt = _self.getOpt();
                            opt.startDate = new Date(start.format('YYYY/MM/DD HH:mm:ss'));
                            opt.endDate = new Date(end.format('YYYY/MM/DD HH:mm:ss'));
                            _self.setOpt(opt);
                            $('#calendar-reservation').find('span').html(start.format('MMMM D, YYYY') + ' - ' + end.format('MMMM D, YYYY'));
                        },
                        pickerData = $('#calendar-reservation').daterangepicker({
                                ranges: {
                                    'Today': [moment(), moment()],
                                    'Yesterday': [moment().subtract('days', 1), moment().subtract('days', 1)],
                                    'Last 7 Days': [moment().subtract('days', 6), moment()],
                                    'Last 30 Days': [moment().subtract('days', 29), moment()],
                                    'This Month': [moment().startOf('month'), moment().endOf('month')],
                                    'Last Month': [moment().subtract('month', 1).startOf('month'), moment().subtract('month', 1).endOf('month')]
                                },
                                startDate: moment().subtract('days', days - 1),
                                endDate: moment()
                            },
                            showDefaultDate
                        ).data("daterangepicker"),
                        customRangeBtn = pickerData.locale.customRangeLabel;
                    showDefaultDate(pickerData.startDate, pickerData.endDate);
                    $(".daterangepicker .ranges ul li:contains('" + customRangeBtn + "')").on('click', function(e) {
                        var $calendarControl = $('.daterangepicker.dropdown-menu');
                        if ($calendarControl.hasClass('show-calendar')) {
                            e.stopPropagation();
                            $calendarControl.removeClass('show-calendar');
                        }
                    })
                },
                email: function() {
                    $(".email-options").find(".parent").click(function(e) {
                        e.stopPropagation();
                        $(this).next(".options").slideToggle(300);
                        $(".export-options").find(".parent").next(".inner").slideUp(300);
                        $(".permalink").find(".parent").next(".inner").slideUp(300);
                    });

                    $("#email-input").click(function() {
                        return false;
                    })
                    $("#email-input").focus(function() {
                        $(this).addClass("is_active")
                    });
                    $("#email-input").blur(function() {
                        $(this).removeClass("is_active")
                    });
                    $("#email-input").hover(
                        function() {
                            if ($(this).hasClass("is_active")) {
                                $(this).removeClass("is_active");
                            } else {
                                $(this).addClass("is_active")
                            }
                        },
                        function() {
                            $(this).removeClass("is_active");
                        }
                    );
                },
                export: function() {
                    $("#mainOutputDetail, #csvOutputDetail").find("li:last").addClass("last");

                    $(".export-options").find(".parent").click(function(e) {
                        e.stopPropagation();
                        $('#mainOutputDetail').slideToggle(300);
                        $(".email-options").find(".parent").next(".options").slideUp(300);
                        $(".permalink").find(".parent").next(".inner").slideUp(300);
                    });

                    $("#csvOutput").click(function(e) {
                        e.stopPropagation();
                        $('#csvOutputDetail').slideToggle(300);
                    });

                    //bind event
                    $('#pdfOutput').on('click', function(e) {
                        e.preventDefault();
                        var opt = _self.getOpt(),
                            reportType = opt.reportType,
                            utils = kup.utils.default;

                        var data = {
                            selectedInstance: opt.selectedInstance,
                            selectedDevices: opt.selectedDevices,
                            startDate: opt.startDate,
                            endDate: opt.endDate,
                            groupNames: opt.groupNames,
                            posNames: opt.posNames
                        };
                        _self.type[reportType].exportPdf(data);
                    });

                    var getCsvData = function(baseUnit) {
                        var opt = _self.getOpt(),
                            reportType = opt.reportType,
                            utils = kup.utils.default;
                        var data = {
                            selectedInstance: opt.selectedInstance,
                            selectedDevices: opt.selectedDevices,
                            startDate: opt.startDate,
                            endDate: opt.endDate,
                            groupNames: opt.groupNames,
                            posNames: opt.posNames,
                            selectedItemDataList: opt.selectedItemDataList,
                            eventType: _map.vcaEventType[reportType],
                            baseUnit: baseUnit
                        };
                        return data;
                    };

                    $('#csvOutputForH').on('click', function(e) {
                        e.preventDefault();
                        var opt = _self.getOpt(),
                            reportType = opt.reportType,
                            utils = kup.utils.default;
                        var data = getCsvData('hours');
                        _self.type[reportType].exportCSV(data);
                    });
                    $('#csvOutputForD').on('click', function(e) {
                        e.preventDefault();
                        var opt = _self.getOpt(),
                            reportType = opt.reportType,
                            utils = kup.utils.default;
                        var data = getCsvData('days');
                        _self.type[reportType].exportCSV(data);
                    });
                    $('#csvOutputForW').on('click', function(e) {
                        e.preventDefault();
                        var opt = _self.getOpt(),
                            reportType = opt.reportType,
                            utils = kup.utils.default;
                        var data = getCsvData('weeks');
                        _self.type[reportType].exportCSV(data);
                    });
                    $('#csvOutputForM').on('click', function(e) {
                        e.preventDefault();
                        var opt = _self.getOpt(),
                            reportType = opt.reportType,
                            utils = kup.utils.default;
                        var data = getCsvData('months');
                        _self.type[reportType].exportCSV(data);
                    });
                },
                permaLink: function() {
                    $("#main-header").find(".permalink").find("li:last").addClass("last");
                    $(".permalink").find(".parent").click(function(e) {
                        e.stopPropagation();
                        $(this).next(".inner").slideToggle(300);
                        $(".export-options").find(".parent").next(".inner").slideUp(300);
                        $(".email-options").find(".parent").next(".options").slideUp(300);
                    });
                },
                chartRadio: function() {
                    var $chartRadio = $("#chartRadio");

                    $chartRadio.find("input.u-radio").uniform();
                    $chartRadio.find('form > div').on('click', 'strong', function(event) {
                        event.preventDefault();
                        var $radioSpanAll = $(this).parents('form').find('input').parent(),
                            $radioSpan = $(this).prev().find('span').eq(1);
                        $radioSpanAll.removeClass('checked');
                        $radioSpan.addClass('checked').click();
                    });

                }
            },
            updateUI: {
                title: function() {
                    var opt = _self.getOpt(),
                        reportType = opt.reportType,
                        title = _map.title,
                        i18n = kup.utils.i18n;
                    $('#reportTitle').html(i18n(title[reportType]));
                },

                /**
                 * [bootstrap-daterangepicker]
                 * @http://www.dangrossman.info/2012/08/20/a-date-range-picker-for-twitter-bootstrap/
                 */
                dateRangePicker: function() {
                    var opt = _self.getOpt(),
                        startDate = opt.defaultDateRange.startDate,
                        endDate = opt.defaultDateRange.endDate,
                        $picker = $('#calendar-reservation'),
                        pickerData = $picker.data('daterangepicker');

                    opt.startDate = opt.defaultDateRange.startDate;
                    opt.endDate = opt.defaultDateRange.endDate;
                    _self.setOpt(opt);

                    pickerData.setStartDate(startDate);
                    pickerData.setEndDate(endDate);
                    $picker.find('span').html(pickerData.startDate.format('MMMM D, YYYY') + ' - ' + pickerData.endDate.format('MMMM D, YYYY'));
                    $picker.off('apply.daterangepicker').on('apply.daterangepicker', function(event) {
                        event.preventDefault();
                        _self.exec.saveReportQueryHhistory();
                        _self.exec.generateReport();
                    });
                },
                menuTab: function() {
                    var opt = _self.getOpt(),
                        reportType = opt.reportType,
                        title = _map.title,
                        i18n = kup.utils.i18n,
                        selectedType = _map.selectedType[reportType],
                        $selectedType = $("#subMenuTab").find('li');
                    $('#subMenuTitle').removeClass(function() {
                        var toReturn = '',
                            classes = this.className.split(' ');
                        for (var i = 0; i < classes.length; i++) {
                            if (/report-\w*-\w*/.test(classes[i])) { /* Filters */
                                toReturn += classes[i] + ' ';
                            }
                        }
                        return toReturn; /* Returns all classes to be removed */
                    }).addClass(title[reportType]).parent().contents().last()[0].textContent = i18n(title[reportType]);
                    //set show/hide
                    $selectedType.hide();
                    $.each(selectedType, function(i, divID) {
                        $(divID).show();
                    });
                    $.each($selectedType, function(i, selector) {
                        if ($(selector).css('display') !== 'none') {
                            $(selector).click();
                            return false;
                        }
                    });
                    if (selectedType.length > 1) {
                        $("#subMenuTab").show();
                    } else {
                        $("#subMenuTab").hide();
                    }
                },
                defaultDropPlace: function() {
                    var opt = _self.getOpt(),
                        dataLength = Object.keys(opt.defaultItemDataList).length;

                    if (dataLength <= 1) {
                        $('#singleTab').click();
                    } else {
                        $('#multipleTab').click();
                    }

                    var opt = _self.getOpt();
                    opt.isDefaultDropData = false;
                    _self.setOpt(opt);
                },
                removeDropPlace: function(uid) {
                    var $selector = (uid) ? $('.drop-item[data-uid="' + uid + '"] > a') : $('.drop-item > a')
                    $selector.click();
                },
                chartRadio: function() {
                    var opt = _self.getOpt(),
                        reportType = opt.reportType,
                        chartTypes = _map.chartRadio[reportType];
                    //bind event
                    $.each(chartTypes, function(i, chart) {
                        var $radioSpan = $(chart).parent();
                        $radioSpan.off('click', "**").on('click', function() {
                            var opt = _self.getOpt(),
                                radioID = $(this).find('input').attr('id');
                            opt.chartRadioType = radioID;
                            _self.setOpt(opt);
                            _self.updateUI.showChart();
                        });
                    });

                    //check checked
                    $.each(chartTypes, function(i, chart) {
                        var $radioSpan = $(chart).parent();
                        $radioSpan.removeClass('checked');
                        if (i === 0) {
                            $radioSpan.addClass('checked').click();
                        }
                    });

                    //check show
                    $("#chartRadio").hide().find('form').children().hide();
                    if (chartTypes.length >= 1) {
                        $("#chartRadio").show();
                    };
                    $.each(chartTypes, function(i, chart) {
                        $(chart).parentsUntil('form').eq(4).show();
                    });
                },
                switchTreeView: function() {
                    var opt = _self.getOpt(),
                        isOnlyShowVcaTreeView = opt.isOnlyShowVcaTreeView,
                        vcaEventType = _map.vcaAnalyticsType[opt.reportType];

                    if (isOnlyShowVcaTreeView) {
                        opt.deviceTreeView.filterTreeView($('#search-term').val(), vcaEventType);
                    } else {
                        opt.deviceTreeView.filterTreeView($('#search-term').val());
                    }
                },
                showExport: function() {
                    var opt = _self.getOpt(),
                        reportType = opt.reportType,
                        selectedType = opt.selectedType,
                        showDivForVca = _map.loadVcaToShowExport[reportType],
                        showDivForSelectedType = _map.selectedTypeToShowExport[selectedType],
                        $exportType = $("#mainOutputDetail").find('li');

                    //hide all div
                    $.each($exportType, function(i, selector) {
                        $(selector).hide();
                    });
                    //show currect vca andd selectedType div
                    $.each(showDivForVca, function(i, exportIDForVca) {
                        $.each(showDivForSelectedType, function(j, exportIDForType) {
                            if (exportIDForVca === exportIDForType) {
                                $(exportIDForVca).show();
                                return false;
                            }
                        });
                    });
                    if (showDivForVca.length < 1) {
                        $("#exportOutput").hide();
                    } else {
                        $("#exportOutput").show();
                    }

                    //control csv export layout position
                    if (showDivForVca.length == 1) {
                        $(".inner-csv").css("top","25px");
                    } else {
                        $(".inner-csv").css("top","65px");
                    }
                },
                showChart: function() {
                    var opt = _self.getOpt(),
                        reportType = opt.reportType,
                        showDiv = _map.chartRadioToShowDiv[reportType],
                        $chartRadio = $("#chartRadio").find('form').children();

                    $.each($chartRadio, function(i, radio) {
                        var $radioSpan = $(radio).find('input').parent(),
                            radioID = $(radio).find('input').attr('id');
                        if (!showDiv['#' + radioID]) {
                            return true;
                        }
                        $.each(showDiv['#' + radioID], function(j, divID) {
                            $(divID).hide();
                            if ($radioSpan.hasClass('checked')) {
                                $(divID).show();
                            }
                        });
                    });
                },
                showDiv: function() {
                    var opt = _self.getOpt(),
                        isSuccessReport = opt.isSuccessReport,
                        reportType = opt.reportType,
                        showDiv = _map.successReportToShowDiv[reportType];

                    $('#main-charts > div').show();
                    $.each(showDiv, function(i, val) {
                        $(val).hide();
                        if (isSuccessReport) {
                            $(val).show();
                        }
                    });

                },
                showUpdateBtn: function() {
                    var opt = _self.getOpt(),
                        selectedItemsLength = _self.getData.selectedItemsLength(),
                        isUpdateSelectedItem = opt.isUpdateSelectedItem,
                        $updateBtn = $('#main-header .modifyConfirm'),
                        $applyBtn = $('#main-header #applyBtn'),
                        $cancelBtn = $('#main-header #cancelBtn');

                    $updateBtn.show();
                    if (isUpdateSelectedItem && selectedItemsLength > 0) {
                        $applyBtn.show();
                    } else {
                        $applyBtn.hide();
                    }

                    if (isUpdateSelectedItem) {
                        $cancelBtn.show();
                    } else {
                        $cancelBtn.hide();
                    }
                },
                cleanDiv: function() {
                    $('#main-charts > div').hide();
                },
                excelSupport: function() {
                    var opt = _self.getOpt(),
                        selectedDeviceList = opt.selectedDevices[0].platformDeviceId;

                    if (selectedDeviceList.length > 1) {
                        var flag = false;
                        $.each(selectedDeviceList, function(i, v) {
                            if (i > 0) {
                                if (selectedDeviceList[i - 1] !== v) {
                                    flag = true;
                                    return false;
                                }
                            }
                        });
                        opt.isMultiDeviceSelected = flag;
                    } else {
                        opt.isMultiDeviceSelected = false;
                    }
                    _self.setOpt(opt);
                }
            },
            verification: {
                reportType: function(reportType) {
                    var opt = _self.getOpt(),
                        reportAll = _map.reportType,
                        isVer = false;
                    $.each(reportAll, function(i, report) {
                        if (report === reportType) {
                            isVer = true;
                            return false;
                        }
                    });
                    return isVer;
                }
            },
            exec: {
                generateReport: function() {
                    var opt = _self.getOpt();
                    opt.isUpdateSelectedItem = false;
                    _self.setOpt(opt);
                    _self.updateUI.showUpdateBtn();
                    _self.type[opt.reportType].generateReport();
                },
                saveReportQueryHhistory: function() {
                    var opt = _self.getOpt(),
                        kupOpt = kup.getOpt(),
                        utils = kup.utils.default,
                        vcaEventType = _map.vcaEventType[opt.reportType],
                        requestOpt = {
                            eventType: vcaEventType,
                            dateFrom: kendo.toString(utils.convertToUTC(opt.startDate), kupOpt.dateFormat),
                            dateTo: kendo.toString(utils.convertToUTC(opt.endDate), kupOpt.dateFormat),
                            deviceSelected: JSON.stringify(opt.selectedSaveDataList),
                            onSuccess: function() {},
                            onFailure: function() {}
                        };
                    return window.saveReportQueryHistory(
                        requestOpt.eventType,
                        requestOpt.dateFrom,
                        requestOpt.dateTo,
                        requestOpt.deviceSelected,
                        requestOpt.onSuccess,
                        requestOpt.onFailure
                    );
                },
                getReportQueryHistory: function() {
                    var opt = _self.getOpt(),
                        vcaEventType = _map.vcaEventType[opt.reportType],
                        requestOpt = {
                            eventType: vcaEventType,
                            onSuccess: function(data) {
                                var opt = _self.getOpt();
                                opt.apiQueryHistoryData = data.query || {};
                                _self.setOpt(opt);
                                _self.setData.defaultItemDataList();
                                _self.setData.defaultElementList();
                                _self.setData.defaultDateRange();
                            },
                            onFailure: function() {}
                        };
                    return window.getReportQueryHistory(
                        requestOpt.eventType,
                        requestOpt.onSuccess,
                        requestOpt.onFailure
                    );
                }
            },
            initOpt: function() {
                var opt = _self.getOpt();
                opt.isMultiDeviceSelected = false;
                opt.isSuccessReport = false;
                opt.isSingleType = true;
                opt.isDragging = false;
                opt.isDefaultDropData = true;
                opt.isUpdateSelectedItem = false;
                _self.setOpt(opt);
            },
            init: function() {
                var opt = _self.getOpt(),
                    getUrl = window.location.href,
                    getLocation = getUrl.indexOf('#'),
                    getReport = getUrl.substring(getLocation + 1),
                    reportTab = (getLocation === -1 || !getReport || !_self.verification.reportType(getReport)) ? opt.menuTabType : _map.menuTab[getReport],
                    reportType = (function(tab) {
                        var report = '';
                        $.each(_map.menuTab, function(k, v) {
                            if (v === tab) {
                                report = k;
                                return false;
                            }
                        })
                        return report;
                    })(reportTab);
                var getUserDevicesOpt = {
                        sessionKey: '',
                        onSuccess: function(data) {
                            var opt = _self.getOpt(),
                                devices = data.devices || [];
                            $.each(devices, function(i, dvc) {
                                if (utils.checkDeviceCompleteInfo(dvc)) {
                                    opt.apiUserDevicesList.push(dvc);
                                }
                            });
                            _self.setOpt(opt);
                        },
                        onFailure: function() {}
                    },
                    getLabelsOpt = {
                        sessionKey: '',
                        onSuccess: function(data) {
                            var opt = _self.getOpt();
                            opt.apiLabels = data.labels || [];
                            _self.setOpt(opt);
                        },
                        onFailure: function() {}
                    },
                    listPosNamesOpt = {
                        sessionKey: '',
                        parserType: '',
                        onSuccess: function(data) {
                            var opt = _self.getOpt(),
                                names = data.names || [];
                            $.each(names, function(i, obj) {
                                opt.posNames.push(obj.name);
                            });
                            _self.setOpt(opt);
                        },
                        onFailure: function() {}
                    };

                opt.reportType = reportType;
                _self.setOpt(opt);

                //get data by ajax
                kup.utils.block.popup('#main', kup.utils.i18n('retrieving-data'));
                $.when(
                    window.getUserDevices(
                        getUserDevicesOpt.sessionKey,
                        getUserDevicesOpt.onSuccess,
                        getUserDevicesOpt.onFailure
                    ),
                    window.getLabels ? window.getLabels(
                        getLabelsOpt.onSuccess
                    ) : (function() {})(),
                    window.listPosNames(
                        listPosNamesOpt.sessionKey,
                        listPosNamesOpt.parserType,
                        listPosNamesOpt.onSuccess,
                        listPosNamesOpt.onFailure
                    )
                ).always(function() {
                    //set data
                    //set sub menu ui
                    _self.setUI.treeView();
                    _self.setUI.searchTreeView();
                    _self.setUI.menuTab();
                    _self.setUI.menuOptions();
                    //set content ui
                    _self.setUI.dropPlace();
                    _self.setUI.dateRangePicker();
                    _self.setUI.updateBtn();
                    _self.setUI.email();
                    _self.setUI.export();
                    _self.setUI.permaLink();
                    _self.setUI.chartRadio();
                    //trigger submenu events
                    $(reportTab).find('a').click();
                });
            },
            getDeviceGroups: getDeviceGroups,
        };
    return _self;
    /*******************************************************************************
     *
     *  Function Definition
     *
     *******************************************************************************/
    function getDeviceGroups() {
        var opt = _self.getOpt();
        var selectedGroups = [];

        $.each(opt.selectedItemDataList, function(uid, itemData) {
            var groupname = "";
            var devices = [];
            var type = "";
            if ($.isEmptyObject(itemData)) {
                return true;
            }
            if (itemData.type === opt.label) { //drag root or labels
                var device = itemData.items || [];
                $.each(device, function(i, deviceData) {
                    var camera = deviceData.items || [];
                    $.each(camera, function(j, cameraData) {
                    	 var apiRunningAnalyticsList=opt.apiRunningAnalyticsList;                 	
                         	if(opt.isOnlyShowVcaTreeView==true){                   		
                         		 $.each(apiRunningAnalyticsList, function(i, apiRunningAnalytic){
                         			 if(apiRunningAnalytic.platformDeviceId==cameraData.platformDeviceId
                          					&&apiRunningAnalytic.channelId==cameraData.channelId){
                         				devices.push({
                                            "coreDeviceId": cameraData.platformDeviceId + "",
                                            "channelId": cameraData.channelId
                                        });
                          			}
                         		 });
                         	}else{
                         		devices.push({
                                    "coreDeviceId": cameraData.platformDeviceId + "",
                                    "channelId": cameraData.channelId
                                });
                         		}

//                        devices.push({
//                            "coreDeviceId": cameraData.platformDeviceId + "",
//                            "channelId": cameraData.channelId
                        });
                });
                type = 'labels';
                groupname = itemData.labelName;
            }  else if (itemData.type === opt.device) { //drag device
                var camera = itemData.items || [];
                $.each(camera, function(i, cameraData) {
                    devices.push({
                        "coreDeviceId": cameraData.platformDeviceId + "",
                        "channelId": cameraData.channelId
                    });
                });
                type = 'devices';
                groupname = itemData.labelName + " - " + itemData.text;
            } else if (itemData.type === opt.channel) { //drag camera
                var cameraData = itemData;
                devices.push({
                    "coreDeviceId": cameraData.platformDeviceId + "",
                    "channelId": cameraData.channelId
                });
                type = 'devices';
                groupname = itemData.deviceName + " - " + itemData.text;
            }
            selectedGroups.push({
                "groupName": groupname,
                "devicePairs": devices,
                "type": type
            });
        });
        return selectedGroups;
    }
})(jQuery, KUP);
