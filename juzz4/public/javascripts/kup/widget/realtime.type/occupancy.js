KUP.widget.realtime.type.currentoccupancy = (function($, kup) {
    var _kupMonitoring = kup.widget.realtime,
        _self = {
    		timeFormat0: 'yyyy/MM/dd HH:mm:ss',
            timeFormat1: 'dd/MM/yyyy HH:mm:ss',
            timeFormat2: 'ddMMyyyyHHmmss',
            timeFormat3: 'dd/MM HH:00',
            runningAnalytics: null,
            generateReport: function() {
                var opt = _kupMonitoring.getOpt(),
                    map = _kupMonitoring.getMap(),
                    kupOpt = kup.getOpt(),
                    kupEvent = kupOpt.event,
                    i18n = kup.utils.i18n,
                    utils = kup.utils.default,
                    kendo = kup.utils.chart.kendo;

                var vcaEventType = map.vcaEventType[opt.monitorType],
                    fromDate = opt.startDate,
                    toDate = opt.endDate,
                    selectedDeviceList = opt.selectedDevices[0].platformDeviceId,
                    selectedChannelList = opt.selectedDevices[0].channelId,
                    groupNames = opt.groupNames,
                    posNames = opt.posNames;

                listRunningAnalytics("", analyticsType.PEOPLE_COUNTING, function(responseData) {
                    if (responseData.result == "ok" && responseData.instances != null) {
                        runningAnalytics = [];
                        $.each(responseData.instances, function (ind, instance){
                        	if (instance.vcaState == "RUNNING") { 
                        		runningAnalytics.push(instance);
                        	}
                        });
                    }
                }, null);

                if (selectedDeviceList.length <= 0 && selectedChannelList.length <= 0 && _kupMonitoring.getDeviceGroups().length <= 0) {
                    utils.popupAlert(i18n('no-store-label-selected'));
                    return;
                }
                //get last 24 hours data & convert to UTC dates.
                startDateForDefaultRange = new Date(moment().subtract(23, 'h').format('YYYY/MM/DD HH:00:00')),
                endDateForDefaultRange = new Date(moment().format('YYYY/MM/DD HH:59:59'));
                var from = kendo.toString(utils.convertToUTC(startDateForDefaultRange), "ddMMyyyyHHmmss"),
                    to = kendo.toString(utils.convertToUTC(endDateForDefaultRange), "ddMMyyyyHHmmss");

                kup.utils.block.popup('#main-charts', i18n('generate-report'));
                window.getAnalyticsReport("", vcaEventType, _kupMonitoring.getDeviceGroups(), selectedDeviceList, "", from, to, {},
                    function(resp) {
                        var opt = _kupMonitoring.getOpt(),
                            selectedInstance = opt.selectedInstance;
                        kup.utils.block.close('#main-charts');

                        if (resp.result == "ok") {
                            var dbEvents = resp.data;
                            opt.isSuccessReport = true;
                            _kupMonitoring.setOpt(opt);
                            _kupMonitoring.updateUI.showDiv();
                            _self.processData(dbEvents);
                        }
                    },
                    function() {
                        var opt = _kupMonitoring.getOpt();
                        opt.isSuccessReport = false;
                        _kupMonitoring.setOpt(opt);
                        _kupMonitoring.updateUI.cleanDiv();
                        kup.utils.block.close('#main-charts');
                    }
                );
            },
            loadClearUI: function() {
                $("#currentOccupancyTable tbody").empty();
            },
            loadUpdateUI: function(uiOutputData) {
                var tableData = 
                	"<tr>"+
                		"<td>"+uiOutputData.siteName+"</td>"+
                		"<td><span class=\"kupPoint\" style=\"font-size: x-large;\">"+uiOutputData.currentOcc+"</span></td>"+
                		"<td><span class=\"kupPoint\">"+uiOutputData.minOcc+"</span> ("+kendo.toString(uiOutputData.minTime, _self.timeFormat3)+")</td>"+
                		"<td><span class=\"kupPoint\">"+uiOutputData.maxOcc+"</span> ("+kendo.toString(uiOutputData.maxTime, _self.timeFormat3)+")</td>"+
                	"</tr>";
                $("#currentOccupancyTable tbody").append(tableData);
            },
            processData: function(events) {
            	_self.loadClearUI();
            	var opt = _kupMonitoring.getOpt();
            	var countList = [];
                var filterEvents = [];
                var uiOutputDataList = [];
                
            	//from api returned data to table
            	$.each(opt.selectedItemDataList, function(uid, itemData) {
            		
            		if ($.isEmptyObject(itemData)) {
                        return true;
                    }
            		itemData.withRunningAnaltyics = false;
            		//check selected item's type
                    if (itemData.isAll || itemData.isLabel) {
                        var name = itemData.text,
                            deviceTimeList = {};
                        $.each(itemData.items, function(i, device) {
                            var deviceList = [];
                            $.each(device.items, function(j, camera) {
                                var cameraData = camera.data;
                                var cameraList = [];
                                $.each(events, function(k, evt) {
                                    if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                        var countItem = {};
                                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date, _self.timeFormat0));
                                        countItem["count" + uid] = parseInt(evt.count, 10);
                                        countItem["currentOccupancy" + uid] = parseInt(evt.currentOccupancy, 10);
                                        countItem["avgOccupancy" + uid] = Math.round(evt.avgOccupancy);

                                        cameraList.push(countItem);
                                        filterEvents.push(evt);
                                        
                                        $.each(runningAnalytics, function (ind, instance){
                                        	if (instance.deviceId == cameraData.coreDeviceId && evt.channelId == instance.channelId) { 
                                        		itemData.withRunningAnaltyics = true;
                                        	}
                                        });
                                    }
                                });
                                deviceList.push(cameraList);
                            });

                            $.each(deviceList, function(j, cameraList) {
                                $.each(cameraList, function(j, camera) {
                                    var timeIndex = kendo.toString(camera.time, _self.timeFormat2);
                                    if (deviceTimeList[timeIndex]) {
                                        deviceTimeList[timeIndex]["count" + uid] += camera["count" + uid];
                                        deviceTimeList[timeIndex]["currentOccupancy" + uid] += camera["currentOccupancy" + uid];
                                        deviceTimeList[timeIndex]["avgOccupancy" + uid] += camera["avgOccupancy" + uid];
                                    } else {
                                        deviceTimeList[timeIndex] = {};
                                        deviceTimeList[timeIndex]["count" + uid] = camera["count" + uid];
                                        deviceTimeList[timeIndex]["currentOccupancy" + uid] = camera["currentOccupancy" + uid];
                                        deviceTimeList[timeIndex]["avgOccupancy" + uid] = camera["avgOccupancy" + uid];
                                        deviceTimeList[timeIndex].time = camera.time;
                                    }
                                })
                            })

                        });
                        $.each(deviceTimeList, function(i, deviceList) {
                            countList.push(deviceList);
                        })
                    };

                    if (itemData.isDevice) {
                        var name = itemData.parentName + " - " + itemData.text;
                        var deviceList = [],
                            deviceTimeList = {};
                        $.each(itemData.items, function(i, camera) {
                            var cameraData = camera.data;
                            var cameraList = [];
                            $.each(events, function(j, evt) {
                                if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                    var countItem = {};
                                    countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date, _self.timeFormat0));
                                    countItem["count" + uid] = parseInt(evt.count, 10);
                                    countItem["currentOccupancy" + uid] = parseInt(evt.currentOccupancy, 10);
                                    countItem["avgOccupancy" + uid] = Math.round(evt.avgOccupancy);

                                    cameraList.push(countItem);
                                    filterEvents.push(evt);
                                    
                                    $.each(runningAnalytics, function (ind, instance){
                                    	if (instance.deviceId == cameraData.coreDeviceId && evt.channelId == instance.channelId) { 
                                    		itemData.withRunningAnaltyics = true;
                                    	}
                                    });
                                }
                            });
                            deviceList.push(cameraList);
                        });
                        $.each(deviceList, function(i, cameraList) {
                            $.each(cameraList, function(j, camera) {
                                var timeIndex = kendo.toString(camera.time, _self.timeFormat2);
                                if (deviceTimeList[timeIndex]) {
                                    deviceTimeList[timeIndex]["count" + uid] += camera["count" + uid];
                                    deviceTimeList[timeIndex]["currentOccupancy" + uid] += camera["currentOccupancy" + uid];
                                    deviceTimeList[timeIndex]["count" + uid] += camera["count" + uid];
                                } else {
                                    deviceTimeList[timeIndex] = {};
                                    deviceTimeList[timeIndex]["count" + uid] = camera["count" + uid];
                                    deviceTimeList[timeIndex]["currentOccupancy" + uid] = camera["currentOccupancy" + uid];
                                    deviceTimeList[timeIndex]["avgOccupancy" + uid] = camera["avgOccupancy" + uid];
                                    deviceTimeList[timeIndex].time = camera.time;
                                }
                            })
                        })

                        $.each(deviceTimeList, function(i, deviceList) {
                            countList.push(deviceList);
                        })
                    };

                    if (itemData.isCamera) {
                        var cameraData = itemData.data,
                            name = cameraData.deviceName + " - " + cameraData.channelName;
                        $.each(events, function(i, evt) {
                            if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                var countItem = {};
                                countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date, _self.timeFormat0));
                                countItem["count" + uid] = parseInt(evt.count, 10);
                                countItem["currentOccupancy" + uid] = parseInt(evt.currentOccupancy, 10);
                                countItem["avgOccupancy" + uid] = Math.round(evt.avgOccupancy);
                                countList.push(countItem);
                                filterEvents.push(evt);
                                
                                $.each(runningAnalytics, function (ind, instance){
                                	if (instance.deviceId == cameraData.coreDeviceId && evt.channelId == instance.channelId) { 
                                		itemData.withRunningAnaltyics = true;
                                	}
                                });
                            }
                        });
                    }
                    
                    itemData.uiDisplaySiteName = name;
            	});
            	
            	//Merging & Sorting data
            	var mergeCountList = {};
                $.each(countList, function(i, ds) {
                    var timeIndex = kendo.toString(ds.time, _self.timeFormat2);
                    if (mergeCountList[timeIndex]) {
                        mergeCountList[timeIndex] = $.extend(true, mergeCountList[timeIndex], ds);
                    } else {
                        mergeCountList[timeIndex] = {};
                        mergeCountList[timeIndex] = ds;
                    }
                });

                countList = [];
                $.each(mergeCountList, function(time, ds) {
                    countList.push(ds);
                });
                countList.sort(function(a, b) {
                    return a.time - b.time;
                });
            	
            	//compute data & display into UI
                var uiObjectList = [];
                $.each(opt.selectedItemDataList, function(uid, itemData) {
                	if ($.isEmptyObject(itemData)) {
                        return true;
                    }
                	
                    var sparkLineData = [];
                    
                    //Compute spake line chart data from chart datasource
                    $.each(countList, function(time, ds) {
                    	if(ds["count"+uid]){
                    		sparkLineData.push({
                                "time": ds["time"],
                                "count": ds["count"+uid],
                                "currentOccupancy": ds["currentOccupancy"+uid],
                                "avgOccupancy": ds["avgOccupancy"+uid],
                            });
                    	}
                    });

                    //Pump in dummy data to prevent empty data
                    if(sparkLineData.length == 0){
                    	sparkLineData.push({
                            "time": kendo.parseDate(new Date(), 'yyyy/MM/dd HH:00:00'),
                            "count": 0,
                            "currentOccupancy": 0,
                            "avgOccupancy": 0,
                        });
                    }
                    
                    //Set to '0' if the current hour no data
                    var currentHour = new Date();
                    currentHour.setMinutes(0);
                    currentHour.setSeconds(0);
                    currentHour.setMilliseconds(0);
                	if(sparkLineData[sparkLineData.length-1].time.getTime() !== currentHour.getTime() 
                			&& !itemData.withRunningAnaltyics){
                		sparkLineData.push({
                            "time": kendo.parseDate(new Date(), 'yyyy/MM/dd HH:00:00'),
                            "count": 0,
                            "currentOccupancy": 0,
                            "avgOccupancy": 0,
                        });
                	}
                        
                    //Analysis Occupancy Data
                    var maxTime = sparkLineData[0].time,
                        minTime = sparkLineData[0].time;
                    var maxOcc = sparkLineData[0].avgOccupancy ,
                        minOcc = sparkLineData[0].avgOccupancy;
                    var currentOcc = 0;
                    for(var c1=0; c1<sparkLineData.length; c1++){
                        var dataA = sparkLineData[c1];
                        if(dataA.avgOccupancy >= maxOcc){
                            maxTime = dataA.time;
                            maxOcc = dataA.avgOccupancy<0?0:dataA.avgOccupancy; //prase negative number to 0
                        }
                        if(dataA.avgOccupancy <= minOcc){
                            minTime = dataA.time;
                            minOcc = dataA.avgOccupancy<0?0:dataA.avgOccupancy; //prase negative number to 0
                        }
                        currentOcc = dataA.currentOccupancy<0?0:dataA.currentOccupancy; //prase negative number to 0
                    }
                    
                    uiObjectList.push({
                        "siteName": itemData.uiDisplaySiteName,
                        "data": sparkLineData,
                        "maxTime":maxTime,
                        "maxOcc":maxOcc,
                        "minTime":minTime,
                        "minOcc":minOcc,
                        "currentOcc":currentOcc,
                    });
                });
                
                //Sorting display table
                uiObjectList.sort(function(a, b) {
                	var aName = a.siteName.toLowerCase();
                	var bName = b.siteName.toLowerCase(); 
                	return ((aName < bName) ? -1 : ((aName > bName) ? 1 : 0));
                });
                $.each(uiObjectList, function(ind, uiObj) {
                	_self.loadUpdateUI(uiObj);
                });
            }
        };
    return _self;
})(jQuery, KUP);
