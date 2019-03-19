KUP.widget.report.type.passerby = (function($, kup) {
    var _kupReport = kup.widget.report,
        _self = {
            generateReport: function() {
                var opt = _kupReport.getOpt(),
                    map = _kupReport.getMap(),
                    kupOpt = kup.getOpt(),
                    kupEvent = kupOpt.event,
                    i18n = kup.utils.i18n,
                    utils = kup.utils.default,
                    kendo = kup.utils.chart.kendo;

                var vcaEventType = map.vcaEventType[opt.reportType],
                    fromDate = opt.startDate,
                    toDate = opt.endDate,
                    selectedDeviceList = opt.selectedDevices[0].platformDeviceId,
                    selectedChannelList = opt.selectedDevices[0].channelId,
                    groupNames = opt.groupNames,
                    posNames = opt.posNames;


                if (selectedDeviceList.length <= 0 && selectedChannelList.length <= 0) {
                    utils.popupAlert(i18n('no-channel-selected'));
                    return;
                }
                //convert to UTC dates.
                var from = kendo.toString(utils.convertToUTC(opt.startDate), "ddMMyyyyHHmmss"),
                    to = kendo.toString(utils.convertToUTC(opt.endDate), "ddMMyyyyHHmmss");

                kup.utils.block.popup('#main-charts', i18n('generate-report'));
                
                //Single Label View
                if(opt.selectedType == "#singleTab"){
                	
                	//get passer by report
                	window.getAnalyticsReport("", vcaEventType, _kupReport.getDeviceGroups(), selectedDeviceList, "", from, to, {},
                            function(resp) {
                				//get people counting report
	                			window.getAnalyticsReport("", KupEvent.PEOPLE_COUNTING, _kupReport.getDeviceGroups(), selectedDeviceList, "", from, to, {},
	                                function(respPplCount) {
	                					
	                                    var opt = _kupReport.getOpt(),
	                                        selectedInstance = opt.selectedInstance;
	                                    kup.utils.block.close('#main-charts');
	
	                                    //compute data
	                                    if (respPplCount.result == "ok" && respPplCount.data.length > 0) {
	                                        opt.isSuccessReport = true;
	                                        _kupReport.setOpt(opt);
	                                        _kupReport.updateUI.showDiv();
	                                        window.vca.reportType = 'mix';
	                                        
	                                        var pplEvents = respPplCount.data;
	                                        $.each(pplEvents, function(inx, event) {
	                                        	event.passerbyCount = 0;
	                                        	event.peopleIn = event.in;
	                                        });
	                                        
	                                        var passerbyEvents = resp.data;
	                                        $.each(passerbyEvents, function(inx, event) {
	                                        	event.passerbyCount = event.in + event.out;
	                                        	event.count = event.in + event.out;
	                                        	event.peopleIn = 0;
	                                        	pplEvents.push(event);
	                                        });
	                                        
	                                        //Merging & Sorting data
	                                        pplEvents.sort(function(a, b) {
	                                            return a.time - b.time;
	                                        });
	                                        
	                                    	var mergeList = {};
	                                        $.each(pplEvents, function(i, ds) {
	                                        	var indexName = kendo.toString(ds.time, "ddMMyyyyHHmmss");
	                                            if (mergeList[indexName]) {
	                                            	mergeList[indexName].passerbyCount += ds.passerbyCount;
	                                            	mergeList[indexName].peopleIn += ds.peopleIn;
	                                            } else {
	                                            	mergeList[indexName] = {};
	                                            	mergeList[indexName] = ds;
	                                            }
	                                        });

	                                        var filteredList = [];
	                                        $.each(mergeList, function(i, ds) {
	                                        	filteredList.push(ds);
	                                        });
	                                        _self.processReport(vcaEventType, filteredList, selectedInstance, fromDate, toDate, true);
	                                    } else if (resp.result == "ok" && resp.data.length > 0) {
	                                        var dbEvents = resp.data;
	                                        opt.isSuccessReport = true;
	                                        _kupReport.setOpt(opt);
	                                        _kupReport.updateUI.showDiv();
	                                        window.vca.reportType = 'mix';
	                                        $.each(dbEvents, function(inx, event) {
	                                        	event.passerbyCount = event.in + event.out;
	                                        	event.count = event.in + event.out;
	                                        });
	                                        _self.processReport(vcaEventType, dbEvents, selectedInstance, fromDate, toDate, true);
	                                    } else {
	                                        opt.isSuccessReport = false;
	                                        _kupReport.setOpt(opt);
	                                        _kupReport.updateUI.cleanDiv();
	                                        utils.popupAlert(i18n("no-records-found"));
	                                    }
	
	                                },
	                                function() {
	                                    var opt = _kupReport.getOpt();
	                                    opt.isSuccessReport = false;
	                                    _kupReport.setOpt(opt);
	                                    _kupReport.updateUI.cleanDiv();
	                                    kup.utils.block.close('#main-charts');
	                                }
	                            );

                            },
                            function() {
                                var opt = _kupReport.getOpt();
                                opt.isSuccessReport = false;
                                _kupReport.setOpt(opt);
                                _kupReport.updateUI.cleanDiv();
                                kup.utils.block.close('#main-charts');
                            }
                        );
                }
                //Multiple Labels View
                else {
                	window.getAnalyticsReport("", vcaEventType, _kupReport.getDeviceGroups(), selectedDeviceList, "", from, to, {},
                        function(resp) {
                            var opt = _kupReport.getOpt(),
                                selectedInstance = opt.selectedInstance;
                            kup.utils.block.close('#main-charts');

                            if (resp.result == "ok" && resp.data.length > 0) {
                                var dbEvents = resp.data;
                                opt.isSuccessReport = true;
                                _kupReport.setOpt(opt);
                                _kupReport.updateUI.showDiv();
                                window.vca.reportType = 'mix';
                                $.each(dbEvents, function(inx, event) {
                                	event.passerbyCount = event.in + event.out;
                                });
                                _self.processReport(vcaEventType, dbEvents, selectedInstance, fromDate, toDate, false);
                            } else {
                                opt.isSuccessReport = false;
                                _kupReport.setOpt(opt);
                                _kupReport.updateUI.cleanDiv();
                                utils.popupAlert(i18n("no-records-found"));
                            }

                        },
                        function() {
                            var opt = _kupReport.getOpt();
                            opt.isSuccessReport = false;
                            _kupReport.setOpt(opt);
                            _kupReport.updateUI.cleanDiv();
                            kup.utils.block.close('#main-charts');
                        }
                    );
                }
            },
            exportPdf: function() {
                var opt = _kupReport.getOpt(),
                    utils = kup.utils.default,
                    i18n = kup.utils.i18n,
                    chartRadioID = opt.chartRadioType,
                    isSuccessReport = opt.isSuccessReport;


                //vreification
                if (!isSuccessReport) {
                    utils.popupAlert(i18n('please-generate-reports'));
                    return;
                }
                if (vca.currentReportInfo == null) {
                    console.log("missing report info");
                }

                var svg = vca.getPrinterFriendlyChart("lineChart");
                if (svg == null || svg == "") {
                    return;
                }
                var svgStr = [];
                svgStr.push(svg);

                var n = 1;
                svg = vca.getPrinterFriendlyChart("lineChart" + n);
                while (svg != null && svg != "") {
                    svg = svg.replace("<?xml version='1.0' ?>", "");
                    svgStr.push(svg);
                    n++;
                    svg = vca.getPrinterFriendlyChart("lineChart" + n);
                }

                kup.utils.block.popup('#main-charts', i18n('generating-download-file'));
                window.exportVcaSecurityPdf(svgStr.join(""), JSON.stringify(vca.currentReportInfo),
                    function(responseData) {
                        kup.utils.block.close('#main-charts');
                        if (responseData != null && responseData.result == "ok" &&
                            responseData["download-url"] != null) {
                            window.open(responseData["download-url"], '_blank');
                            window.focus();
                        } else {
                            utils.throwServerError(responseData);
                        }
                    },
                    function() {
                        kup.utils.block.close('#main-charts');
                    }
                );
            },
            exportCSV: function(data) {
                var dvcId = data.selectedInstance,
                    channId = data.selectedDevices,
                    fromDate = data.startDate,
                    toDate = data.endDate,
                    groupNames = data.groupNames,
                    posNames = data.posNames,
                    eventType = data.eventType;
                var baseUnit = data.baseUnit;

                var opt = _kupReport.getOpt(),
                    utils = kup.utils.default,
                    i18n = kup.utils.i18n,
                    chartRadioID = opt.chartRadioType,
                    isMultiDeviceSelected = opt.isMultiDeviceSelected,
                    isSuccessReport = opt.isSuccessReport;

                //vreification
                if (!isSuccessReport) {
                    utils.popupAlert(i18n('please-generate-reports'));
                    return;
                }

                if (vca.currentReportInfo == null) {
                    console.log("missing report info");
                }
                
                if (vca.currentReportInfo["total-results"] == "0") {
                	utils.popupAlert(i18n('export-failed-no-passerby-data'));
                    return;
                }

                var selectedGroups = _kupReport.getDeviceGroups();
                var fromStr = kendo.toString(utils.convertToUTC(fromDate), "ddMMyyyyHHmmss");
                var toStr = kendo.toString(utils.convertToUTC(toDate), "ddMMyyyyHHmmss");
                //var baseUnit = vca.currentChartbaseUnit;
                var siteName = $.isArray(groupNames) && groupNames.length > 0 ? groupNames[0] : "";
                kup.utils.block.popup('#main-charts', i18n('generating-download-file'));

                window.exportAggregatedCSVReport(eventType, JSON.stringify(selectedGroups), fromStr, toStr, baseUnit,
                    function(responseData) {
                        kup.utils.block.close('#main-charts');
                        if (responseData != null && responseData.result == "ok" &&
                            responseData["download-url"] != null) {
                            window.open(responseData["download-url"], '_blank');
                            window.focus();
                        } else {
                            utils.throwServerError(responseData);
                        }
                    },
                    function() {
                        kup.utils.block.close('#main-charts');
                    }
                );
            },
            loadSetUI: function() {},
            loadUpdateUI: function() {
                _kupReport.type.peoplecounting.loadUpdateUI();
            },
            processReport: function(vcaEventType, events, selectedValue, fromDate, toDate, isSingleTab) {
                var seriesInfo = [];
                var singleViewDetails = {};
                var count = 0;
                var countList = [];
                var totalPasserBy = 0, totalVisits = 0;
                countList.push({
                    time: fromDate,
                    count: 1
                }); // make sure graph starts at
                // fromDate
                countList.push({
                    time: toDate,
                    count: 1
                }); // and ends at toDate

                //set chart data
                if (vca.reportType == "mix") { //selected tree items,can to any select all,label,device,camera
                    var countItem = {},
                        selectedItemDataList = KUP.widget.report.getOpt('selectedItemDataList') || {},
                        selectedValue = (function(selectedItemDataList) {
                            var selectedValue = [];
                            $.each(selectedItemDataList, function(i, itemData) {
                                if ($.isEmptyObject(itemData)) {
                                    return true;
                                }
                                selectedValue.push(itemData);
                            });
                            return selectedValue;
                        })(selectedItemDataList);
                        
                    
                    $.each(selectedItemDataList, function(uid, itemData) {
                        var totalPasserByPerSelection = 0,
                        	totalPeoplePerSelection = 0,
                        	bestCaptureRate = 0,
                        	avgCaptureRate = 0,
                        	bestCaptureRateDate = "";

                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }

                        //check selected item's type
                        if (itemData.type === "label") {
                            var name = itemData.text,
                                deviceTimeList = {};
                            $.each(itemData.items, function(i, device) {
                                var deviceList = [];
                                $.each(device.items, function(j, camera) {
                                    var cameraData = camera;
                                    var cameraList = [];
                                    $.each(events, function(k, evt) {
                                        if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                                            var countItem = {};
                                            countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                                            countItem.deviceName = cameraData.deviceName;
                                            countItem.channelName = cameraData.channelName;
                                            countItem.name = name;
                                            countItem["count" + count] = parseInt(evt.passerbyCount, 10);
                                            if(evt.peopleIn){
                                            	countItem["peopleIn" + count] = parseInt(evt.peopleIn, 10);
                                            }else{
                                            	countItem["peopleIn" + count] = 0;
                                            }
                                            if(evt.passerbyCount){
                                        		countItem["passerbyCount" + count] = parseInt(evt.passerbyCount, 10);
            	                            }else{
            	                            	countItem["passerbyCount" + count] = 0;
            	                            }
                                            countItem.value = countItem["passerbyCount" + count];
                                            cameraList.push(countItem);

                                            totalPasserBy += countItem["passerbyCount" + count];
                                            totalVisits += parseInt(countItem["peopleIn" + count], 10);
                                            totalPasserByPerSelection += countItem["passerbyCount" + count];
                                            
                                        }
                                    });
                                    deviceList.push(cameraList);
                                });

                                $.each(deviceList, function(j, cameraList) {
                                    $.each(cameraList, function(j, camera) {
                                        var timeIndex = kendo.toString(camera.time, 'ddMMyyyyHHmmss');
                                        if (deviceTimeList[timeIndex]) {
                                            deviceTimeList[timeIndex]["count" + count] += camera["count" + count];
                                            deviceTimeList[timeIndex]["peopleIn" + count] += camera["peopleIn" + count];
                                            deviceTimeList[timeIndex]["passerbyCount" + count] += camera["passerbyCount" + count];
                                            deviceTimeList[timeIndex].value += camera.value;
                                        } else {
                                            deviceTimeList[timeIndex] = {};
                                            deviceTimeList[timeIndex].name = name;
                                            deviceTimeList[timeIndex].deviceName = camera.deviceName;
                                            deviceTimeList[timeIndex]["count" + count] = camera["count" + count];
                                            deviceTimeList[timeIndex]["peopleIn" + count] = camera["peopleIn" + count];
                                            deviceTimeList[timeIndex]["passerbyCount" + count] = camera["passerbyCount" + count];
                                            deviceTimeList[timeIndex].value = camera.value;
                                            deviceTimeList[timeIndex].time = camera.time;
                                        }
                                    })
                                })
                            });
                            $.each(deviceTimeList, function(i, deviceList) {
                                countList.push(deviceList);
                            })
                        } else if (itemData.type === "device") {
                            var name = itemData.labelName + " - " + itemData.text;
                            var deviceList = [],
                                deviceTimeList = {};
                            $.each(itemData.items, function(i, camera) {
                                var cameraData = camera;
                                var cameraList = [];
                                $.each(events, function(j, evt) {
                                    if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                                        var countItem = {};
                                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                                        countItem.deviceName = cameraData.deviceName;
                                        countItem.channelName = cameraData.channelName;
                                        countItem.name = name;
                                        countItem["count" + count] = parseInt(evt.passerbyCount, 10);
                                        if(evt.peopleIn){
                                        	countItem["peopleIn" + count] = parseInt(evt.peopleIn, 10);
                                        }else{
                                        	countItem["peopleIn" + count] = 0;
                                        }
                                        if(evt.passerbyCount){
                                    		countItem["passerbyCount" + count] = parseInt(evt.passerbyCount, 10);
                                        }else{
                                        	countItem["passerbyCount" + count] = 0;
                                        }
                                        countItem.value = countItem["passerbyCount" + count];
                                        cameraList.push(countItem);

                                        totalPasserBy += countItem["passerbyCount" + count];
                                        totalVisits += parseInt(countItem["peopleIn" + count], 10);
                                        totalPasserByPerSelection += countItem["passerbyCount" + count];
                                        
                                    }
                                });
                                deviceList.push(cameraList);
                            });

                            $.each(deviceList, function(i, cameraList) {
                                $.each(cameraList, function(j, camera) {
                                    var timeIndex = kendo.toString(camera.time, 'ddMMyyyyHHmmss');
                                    if (deviceTimeList[timeIndex]) {
                                        deviceTimeList[timeIndex]["count" + count] += camera["count" + count];
                                        deviceTimeList[timeIndex]["peopleIn" + count] += camera["peopleIn" + count];
                                        deviceTimeList[timeIndex]["passerbyCount" + count] += camera["passerbyCount" + count];
                                        deviceTimeList[timeIndex].value += camera.value;
                                    } else {
                                        deviceTimeList[timeIndex] = {};
                                        deviceTimeList[timeIndex].name = name;
                                        deviceTimeList[timeIndex].deviceName = camera.deviceName;
                                        deviceTimeList[timeIndex]["count" + count] = camera["count" + count];
                                        deviceTimeList[timeIndex]["peopleIn" + count] = camera["peopleIn" + count];
                                        deviceTimeList[timeIndex]["passerbyCount" + count] = camera["passerbyCount" + count];
                                        deviceTimeList[timeIndex].value = camera.value;
                                        deviceTimeList[timeIndex].time = camera.time;
                                    }
                                })
                            })

                            $.each(deviceTimeList, function(i, deviceList) {
                                countList.push(deviceList);
                            })

                        } else if (itemData.type === "channel") {
                            var cameraData = itemData,
                                name = cameraData.deviceName + " - " + cameraData.text;
                            $.each(events, function(i, evt) {
                                if (evt.deviceId == cameraData.coreDeviceId && evt.channelId == cameraData.channelId) {
                                    var countItem = {};
                                    countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", "yyyy/MM/dd HH:mm:ss"));
                                    countItem.deviceName = cameraData.deviceName;
                                    countItem.channelName = cameraData.channelName;
                                    countItem.name = name;
                                    countItem["count" + count] = parseInt(evt.passerbyCount, 10);
                                    if(evt.peopleIn){
                                    	countItem["peopleIn" + count] = parseInt(evt.peopleIn, 10);
                                    }else{
                                    	countItem["peopleIn" + count] = 0;
                                    }
                                    if(evt.passerbyCount){
                                		countItem["passerbyCount" + count] = parseInt(evt.passerbyCount, 10);
                                    }else{
                                    	countItem["passerbyCount" + count] = 0;
                                    }
                                    countItem.value = countItem["passerbyCount" + count];
                                    totalPasserBy += countItem["passerbyCount" + count];
                                    totalVisits += parseInt(countItem["peopleIn" + count], 10);
                                    totalPasserByPerSelection += countItem["passerbyCount" + count];
                                    
                                    countList.push(countItem);
                                }
                            });
                        }
                        
                        //set chart data
                        if(totalPasserBy > 0)
                        	avgCaptureRate = (totalVisits / (totalVisits + totalPasserBy)) * 100;
                        else
                        	avgCaptureRate = 0;
                        
                        singleViewDetails.name = name;
                        singleViewDetails.totalPasserByPerSelection = totalPasserByPerSelection;
                        singleViewDetails.totalPeoplePerSelection = totalPeoplePerSelection;
                        singleViewDetails.avgCaptureRate = avgCaptureRate;
                        
                        if (isSingleTab) {
                        	//single view
                            seriesInfo.push({
                                "name": localizeResource("no-of-passerbys"),
                                "field": "passerbyCount" + count,
                                "aggregate": "sum"
                            });
                            seriesInfo.push({
                                "name": localizeResource("no-of-visitors"),
                                "field": "peopleIn" + count,
                                "aggregate": "sum"
                            });
                        } else {
                        	//multiple view
                            seriesInfo.push({
                                "name": name,
                                "field": "count" + count,
                                "aggregate": "sum"
                            });
                        }
                        count++;
                    });
                }

                if (countList.length <= 2) {
                    vca.hideContainers();
                    return;
                }
                vca.showGeneralContainers();
                
                //generate chart
                vca.createLineChart("lineChartContainer",
                	localizeResource("passerby-vs-time"), "line", seriesInfo, countList);

                
                //update report bottom infomation by chart based unit
                if (isSingleTab) {
                	var combinedList = countList.slice();
                    singleViewDetails.bestCaptureRateDate = null;
                    singleViewDetails.bestCaptureRate = 0;
                    
                    //combine data
                    var filteredDataList = [];
                    $.each(combinedList, function(i, combinedData) {
                    	var found = false;
                    	if(!combinedData["peopleIn0"])
                			combinedData["peopleIn0"] = 0;
            			if(!combinedData["passerbyCount0"])
            				combinedData["passerbyCount0"] = 0;
            			
                    	$.each(filteredDataList, function(k, filteredData) {
                        	if(vca.currentChartbaseUnit == "hours") {
                        		var filtredTime = kendo.toString(filteredData.time, "dd MMM yyyy HH:mm");
                        		var orgTime = kendo.toString(combinedData.time, "dd MMM yyyy HH:mm");
                        		if(filtredTime == orgTime){
                        			filteredData["peopleIn0"] += combinedData["peopleIn0"];
                        			filteredData["passerbyCount0"] += combinedData["passerbyCount0"];
                        			found = true;
                        			return false;
                        		}
                            }else {
                            	var filtredTime = kendo.toString(filteredData.time, "dd MMM yyyy");
                        		var orgTime = kendo.toString(combinedData.time, "dd MMM yyyy");
                        		if(filtredTime == orgTime){
                        			filteredData["peopleIn0"] += combinedData["peopleIn0"];
                        			filteredData["passerbyCount0"] += combinedData["passerbyCount0"];
                        			found = true;
                        			return false;
                        		}
                            }
                        });
                    	
                    	if(!found){
                            filteredDataList.push({
                                "time": combinedData.time,
                                "peopleIn0": combinedData.peopleIn0,
                                "passerbyCount0": combinedData.passerbyCount0
                            });
                    	}
                    });
                    
                    //find best capture rate
                    $.each(filteredDataList, function(i, combinedData) {
                    	var tmpPeopleIn = combinedData["peopleIn0"];
                    	var tmpPasserby = combinedData["passerbyCount0"];
                        if(tmpPasserby > 0 || tmpPeopleIn > 0){
                        	var newCaptureRate = tmpPeopleIn/(tmpPeopleIn+tmpPasserby)*100;
                        	singleViewDetails.bestCaptureRateDate = newCaptureRate > singleViewDetails.bestCaptureRate ?
                        			combinedData.time : singleViewDetails.bestCaptureRateDate;
                        	singleViewDetails.bestCaptureRate = newCaptureRate > singleViewDetails.bestCaptureRate ?
                        			newCaptureRate : singleViewDetails.bestCaptureRate;
                        }
                    });
                }
                
                vca.passerByReportBottomAnalysisPortion(totalPasserBy, totalVisits, selectedValue.length, singleViewDetails, isSingleTab);

                var selectedDevices = [];
                $.each(seriesInfo, function(index, value) {
                    selectedDevices.push(" " + value.name);
                })

                vca.currentReportInfo = {
                    "event-type": vcaEventType,
                    "site-name": selectedDevices.toString(),
                    "from": kendo.toString(fromDate, kupapi.TIME_FORMAT),
                    "to": kendo.toString(toDate, kupapi.TIME_FORMAT),
                    "total-results": totalPasserBy + ""
                };
                
                //listener for based unit change
                if (isSingleTab) {
                	$("input[name=lineStepSize]").click(function() {
                		var combinedList = countList.slice();;
                        singleViewDetails.bestCaptureRateDate = null;
                        singleViewDetails.bestCaptureRate = 0;
                        
                        //combine data
                        var filteredDataList = [];
                        $.each(combinedList, function(i, combinedData) {
                        	var found = false;
                        	if(!combinedData["peopleIn0"])
                    			combinedData["peopleIn0"] = 0;
                			if(!combinedData["passerbyCount0"])
                				combinedData["passerbyCount0"] = 0;
                			
                        	$.each(filteredDataList, function(k, filteredData) {
                            	if(vca.currentChartbaseUnit == "hours") {
                            		var filtredTime = kendo.toString(filteredData.time, "dd MMM yyyy HH:mm");
                            		var orgTime = kendo.toString(combinedData.time, "dd MMM yyyy HH:mm");
                            		
                            		if(filtredTime == orgTime){
                            			filteredData["peopleIn0"] += combinedData["peopleIn0"];
                            			filteredData["passerbyCount0"] += combinedData["passerbyCount0"];
                            			found = true;
                            			return false;
                            		}
                                }else {
                                	var filtredTime = kendo.toString(filteredData.time, "dd MMM yyyy");
                            		var orgTime = kendo.toString(combinedData.time, "dd MMM yyyy");
                            		
                            		if(filtredTime == orgTime){
                            			filteredData["peopleIn0"] += combinedData["peopleIn0"];
                            			filteredData["passerbyCount0"] += combinedData["passerbyCount0"];
                            			found = true;
                            			return false;
                            		}
                                }
                            });
                        	
                        	if(!found){
                        		filteredDataList.push({
                        		    "time": combinedData.time,
                                    "peopleIn0": combinedData.peopleIn0,
                                    "passerbyCount0": combinedData.passerbyCount0
                                });
                        	}
                        });

                        //find best capture rate
                        $.each(filteredDataList, function(i, combinedData) {
                        	var tmpPeopleIn = combinedData["peopleIn0"];
                        	var tmpPasserby = combinedData["passerbyCount0"];
                            if(tmpPasserby > 0 || tmpPeopleIn > 0){
                            	var newCaptureRate = tmpPeopleIn/(tmpPeopleIn+tmpPasserby)*100;
                            	singleViewDetails.bestCaptureRateDate = newCaptureRate > singleViewDetails.bestCaptureRate ?
                            			combinedData.time : singleViewDetails.bestCaptureRateDate;
                            	singleViewDetails.bestCaptureRate = newCaptureRate > singleViewDetails.bestCaptureRate ?
                            			newCaptureRate : singleViewDetails.bestCaptureRate;
                            }
                        });
                        
                        vca.passerByReportBottomAnalysisPortion(totalPasserBy, totalVisits, selectedValue.length, singleViewDetails, isSingleTab);
                    });
                }
            }
        };
    return _self;
})(jQuery, KUP);
