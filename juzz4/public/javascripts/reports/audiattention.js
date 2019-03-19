var audiattention = {};

audiattention.selectedDeviceList = [];
audiattention.selectedChannelList = [];
audiattention.startDate;  //local dateTime
audiattention.endDate;    //local dateTime

audiattention.generateTreeItems = function(callback) {
	var deviceGroups = {};
	var cameraGroups = {};
	var labelGroups = {};
    var treeItems = [];
    
    deviceManager.WaitForReady(function () {
        var deviceList = deviceManager.userDevices;
        var all = localizeResource('all');  //Unlabelled.

        $.each(deviceList, function(index, dItem) {
            if (dItem.model.capabilities.indexOf("video") == -1){
            	return;
            }
            
            if (deviceGroups[dItem.id] == null) {
                deviceGroups[dItem.id] = [];  //look like -> deivceGroups.1=[{},{}]
            }
            if (cameraGroups[dItem.id] == null) {
                cameraGroups[dItem.id] = [];
            }
            
            //create a "all" label include all device.
            if (labelGroups[all] == null) { 
                labelGroups[all] = [];
            }
            if (deviceGroups[dItem.id].length < 1) {
	            if (dItem.model.capabilities.indexOf("node") != -1) {  //device is the node (for this side is setting sub point e.g. ipcamera,channel).
	                var i = 0;
	                while (i < dItem.node.cameras.length) {
	                    deviceGroups[dItem.id].push({
	                        id: dItem.id,
	                        deviceId: dItem.deviceId,
	                        text: dItem.node.cameras[i].name,
	                        group: all,
	                        cameras: dItem.node.cameras[i]
	                    });
	                    cameraGroups[dItem.id].push(dItem.node.cameras[i]);
	                    i++;
	                }
	            } else{
	                var i = 0;
	                while (i < dItem.model.channels) {  //device isn't node
	                    i++;
	                    deviceGroups[dItem.id].push({
	                        id: dItem.id,
	                        deviceId: dItem.deviceId,
	                        text: localizeResource('channel') + " " + i,
	                        group: all,
	                        cameras: {
	                            "name": localizeResource('channel') + " " + i,
	                            "nodeCoreDeviceId": i - 1
	                        }
	                    });
	                    cameraGroups[dItem.id].push({
	                        "name": localizeResource('channel') + " " + i,
	                        "nodeCoreDeviceId": i - 1
	                    });
	                }
	            }
	        }

            //check connection situation to set status icon (device).
            if (dItem.status == DvcMgr.DeviceStatus.DISCONNECTED) {
                labelGroups[all].push({
                    deviceId: dItem.id,
                    text: dItem.name + "",
                    imageUrl: kupapi.CdnPath + "/common/images/device_offline.png",
                    items: deviceGroups[dItem.id]
                });
            } else {
                labelGroups[all].push({
                    deviceId: dItem.id,
                    text: dItem.name + "",
                    items: deviceGroups[dItem.id]  //set sub point.
                });
            }  //create a "all" label finish.
            
            if (dItem.label.length > 0) {
                $.each(dItem.label, function(index, lItem) {
                    /*var lItem = lItem.toUpperCase(); */

                    if (labelGroups[lItem] == null) {
                        labelGroups[lItem] = [];
                    }
                    if (deviceGroups[dItem.id].length < 1) {
                        if (dItem.model.capabilities.indexOf("node") != -1) {
                            var i = 0;
                            while (i < dItem.node.cameras.length) {
                                deviceGroups[dItem.id].push({
                                    id: dItem.id,
                                    deviceId: dItem.deviceId,
                                    text: dItem.node.cameras[i].name,
                                    group: lItem,
                                    cameras: dItem.node.cameras[i]
                                });
                                cameraGroups[dItem.id].push(dItem.node.cameras[i]);
                                i++;
                            }
                            } else{
                            var i = 0;
                            while (i < dItem.model.channels) {
                                i++;
                                deviceGroups[dItem.id].push({
                                    id: dItem.id,
                                    deviceId: dItem.deviceId,
                                    text: localizeResource('channel') + " " + i,
                                    group: lItem,
                                    cameras: {
                                        "name": localizeResource('channel') + " " + i,
                                        "nodeCoreDeviceId": i - 1
                                    }
                                });
                                cameraGroups[dItem.id].push({
                                    "name": localizeResource('channel') + " " + i,
                                    "nodeCoreDeviceId": i - 1
                                });
                            }
                        }
                    }
                    if (dItem.status == DvcMgr.DeviceStatus.DISCONNECTED) {
                        labelGroups[lItem].push({
                            deviceId: dItem.id,
                            text: dItem.name + "",
                            imageUrl: kupapi.CdnPath + "/common/images/device_offline.png",
                            items: deviceGroups[dItem.id]
                        });
                    } else {
                        labelGroups[lItem].push({
                            deviceId: dItem.id,
                            text: dItem.name + "",
                            items: deviceGroups[dItem.id]
                        });
                    }
                });
            }
                
        }); //deviceList loop finish
        
        $.each(labelGroups, function(index, lgroup) { //for every object's key fetch value, index-> key, 1group-> value.
        	treeItems.push({
                id: index,
                text: localizeResource(index),
                devices: deviceGroups[index],
                items: lgroup
            });
        });
        callback(treeItems);
    });
}

audiattention.initDeviceList = function() {
	var isDragging = false;
	var draggedDevice = null;
	var parent = null;
	var treeview = null
	audiattention.generateTreeItems(function(treeItems) {
		treeview = $("#deviceTree").kendoTreeView({
			dragAndDrop: true,
            dataSource: treeItems,
            dragstart: function(e) {
            	if($("#deviceTree").attr("isDrag") == "false") {
            		e.preventDefault();
            	} else {
            		isDragging = true;
            		$(".drop-here").css('display', 'block');
                	draggedDevice = this.dataItem(e.sourceNode);
                	if(!draggedDevice.hasChildren) {
                		parent = this.parent(e.sourceNode);
                	}
            	}
            },
            drag: function(e) {
            	if(e.dropTarget.className == "drop-here") {  //if drag over "drop-here" change icon for "+".
            		e.setStatusClass("k-add");
            	}
            },
            drop: function(e) {
            	if(e.dropTarget.className != "drop-here") {  //if drag drop is "drop-here" doesn't execute dropping.
            		e.setValid(false);
            	}
            	isDragging = false;
            	$(".drop-here").css('display', 'none')
            	e.preventDefault();
            }
		}).data("kendoTreeView");
		$("#deviceTree").find('img').addClass("node-status-image");
	});
	
	$(".drop-place").kendoDropTarget({
		drop: function(e) {  //execute faster than treeview's drop event.
			var deviceId = "";
			var channelId = "";
			var name = draggedDevice.text;
			if(draggedDevice.deviceId == undefined) {  //drag label.
				var len = draggedDevice.items.length;
				$.each(draggedDevice.items, function(index, device){
					audiattention.selectedDeviceList.push(device.deviceId);
					if(index == len - 1 ) {
						deviceId += device.deviceId;
					} else {
						deviceId += device.deviceId + ",";
					}
				})
			} else if(draggedDevice.cameras != undefined) {  //drag camera.
				if(!draggedDevice.hasChildren) {
					audiattention.selectedDeviceList.push(draggedDevice.id);
					audiattention.selectedChannelList.push(draggedDevice.cameras.nodeCoreDeviceId);
					deviceId += draggedDevice.id;
					channelId += draggedDevice.cameras.nodeCoreDeviceId;
					name = treeview.text(parent) + " _ " + draggedDevice.text;
				}
			} else {
				audiattention.selectedDeviceList.push(draggedDevice.deviceId);
				deviceId += draggedDevice.deviceId;
			}
			
			var htmlString = "<span class='pos-r is_active' deviceId='" + deviceId + "' channelId='" + channelId + 
			"'>" + name + "<a href='#' class='pos-r btn-remove ir' onclick='audiattention.removeSelectedDevice(event)'></a></span>";
			$(htmlString).insertBefore('.drop-here');
			
			$("#deviceTree").attr("isDrag", "false")  //for single device select.
			$('#deviceTree').off("mouseover", "span");
		}
    });
	
	$('#deviceTree').on("mouseover", "span", function(){
		$(".drop-here").css('display', 'block')
	})
	$('#deviceTree').on("mouseout", "span", function(){
		if(!isDragging) {
			$(".drop-here").css('display', 'none')
		}
	})
	
	//search device treeview feature.
	$('#search-term').on('keyup', function() {

        $('span.k-in > span.highlight').each(function() {
            $(this).parent().text($(this).parent().text());
        });

        // ignore if no search term.
        if ($.trim($(this).val()) == '') {
            return;
        }

        var term = this.value.toUpperCase();
        var tlen = term.length;

        $('#deviceTree span.k-in').each(function(index) {
            var text = $(this).text();
            var html = '';
            var q = 0;
            while ((p = text.toUpperCase().indexOf(term, q)) >= 0)
            {
                html += text.substring(q, p) + '<span class="highlight">' + text.substr(p, tlen) + '</span>';
                q = p + tlen;
            }

            if (q > 0) {
                html += text.substring(q);
                $(this).html(html);

                $(this).parentsUntil('.k-treeview').filter('.k-item').each(
                        function(index, element) {
                            treeview.expand($(this));
                            $(this).data('search-term', term);
                        }
                );
            }
        });

        $('#deviceTree .k-item').each(function() {
            if ($(this).data('search-term') != term) {
                treeview.collapse($(this));
            }
        });
    });

}

audiattention.removeSelectedDevice = function(e) {
	e.preventDefault();
	var selectedDevice = $(e.currentTarget).parent('span');
	var deviceIds = selectedDevice.attr('deviceId').split(",");
	var channelId = selectedDevice.attr('channelId');
	$.each(deviceIds, function(index, id) {
		audiattention.selectedDeviceList.splice(audiattention.selectedDeviceList.indexOf(id), 1);
	})
	if(channelId != "") {
		audiattention.selectedChannelList.splice(audiattention.selectedChannelList.indexOf(channelId), 1);
	}
	$(selectedDevice).remove();
	$("#deviceTree").attr("isDrag", "true");
	
	$('#deviceTree').on("mouseover", "span", function(){
		$(".drop-here").css('display', 'block')
	})
}

audiattention.initDateRangePicker = function() {
	var pickerData = $('#calendar-reservation').daterangepicker({  //.daterangepicker(options, CallbackFn).
		  ranges: {
		     'Today': [moment(), moment()],
		     'Yesterday': [moment().subtract('days', 1), moment().subtract('days', 1)],
		     'Last 7 Days': [moment().subtract('days', 6), moment()],
		     'Last 30 Days': [moment().subtract('days', 29), moment()],
		     'This Month': [moment().startOf('month'), moment().endOf('month')],
		     'Last Month': [moment().subtract('month', 1).startOf('month'), moment().subtract('month', 1).endOf('month')]
		  },
		  startDate:moment().subtract('days', 1),  //default time.
		  endDate: moment()
		}, CallbackFn).data("daterangepicker");
	
	CallbackFn(pickerData.startDate, pickerData.endDate);
	$('#calendar-reservation').on('apply.daterangepicker', audiattention.generateChart);
	
	function CallbackFn(start, end) {
		audiattention.startDate = new Date(start.format('YYYY/MM/DD HH:mm:ss'));
		audiattention.endDate = new Date(end.format('YYYY/MM/DD HH:mm:ss'));
	    $('#calendar-reservation span').html(start.format('MMMM D, YYYY') + ' - ' + end.format('MMMM D, YYYY'));
	}
}

audiattention.generateChart = function() {
	if(audiattention.selectedDeviceList.length <= 0 && audiattention.selectedChannelList.length <= 0) {
		utils.popupAlert(localizeResource('no-channel-selected'));
		return;
	}
	
	//convert to UTC dates.
	var from = kendo.toString(utils.convertToUTC(audiattention.startDate), "ddMMyyyyHHmmss");
    var to = kendo.toString(utils.convertToUTC(audiattention.endDate), "ddMMyyyyHHmmss");
	getAnalyticsReport("", KupEvent.PROFILING, null, audiattention.selectedDeviceList, "", from, to, {},
			function(resp) {
				if(resp.result == "ok") {
					if(resp.data.length <= 0) {
						utils.popupAlert(localizeResource("no-records-found"));
						return;
					}
					
					//checking response's data whether match with channel list.
					var dbEvents = [];
					$.each(resp.data, function(index, evt) {
							if(audiattention.selectedChannelList.length > 0) {
								$.each(audiattention.selectedChannelList, function(index, channelId) {
									if(evt.channelId == channelId) {
										dbEvents.push(evt);
									}
								})
							} else {
								dbEvents = resp.data
							}
					});
					
					var categoryNames = ["under 5s", "5 to 10s", "10 to 20s", "20 to 30s", "30 to 60s",
					                     "1 to 3m", "3 to 5m", "5 to 8m", "8 to 10m", "10 to 15m", "15 to 30m"]
					var values = [0,0,0,0,0,0,0,0,0,0,0];
					var attentionCountList = [];
					var twoDaysMillis = (2 * 24 * 60 * 60 * 1000);
					var rangeMillis = audiattention.endDate - audiattention.startDate;
					var chartBaseUnit = "hours";
					var step = 1;
					
					//make sure graph starts at startDate and ends at endDate.
					attentionCountList.push({totalDuration: 0, eventNum: 0, date: audiattention.startDate});
					attentionCountList.push({totalDuration: 0, eventNum: 0, date: audiattention.endDate});
					
					// if range is more than 2 days, change stepsize to days.
					if(rangeMillis > twoDaysMillis) {
						chartBaseUnit = "days";
						step = Math.ceil(rangeMillis / 1000 / 60 / 60 / 24 / 60);  //reduce number of vertical line.
					}
					
					$.each(dbEvents, function(index, event) {
						var countItem = {};
						countItem.totalDuration = event.duration;
						countItem.eventNum = event.count;
						countItem.date = utils.convertUTCtoLocal(new Date(event.date));
						attentionCountList.push(countItem);
						
						$.each(event, function(key, value) {
							switch(key) {
							  case "dur0_5s": values[0] = values[0] + value;	
									break;
							  case "dur5_10s": values[1] = values[1] + value;	
							        break;  
							  case "dur10_20s": values[2] = values[2] + value;	
								    break;
							  case "dur20_30s": values[3] = values[3] + value;	
						            break;
							  case "dur30_60s": values[4] = values[4] + value;	
									break;
							  case "dur1_3m": values[5] = values[5] + value;	
						        	break;  
							  case "dur3_5m": values[6] = values[6] + value;	
							    	break;
							  case "dur5_8m": values[7] = values[7] + value;	
					            	break;
							  case "dur8_10m": values[8] = values[8] + value;	
				            		break; 
							  case "dur10_15m": values[9] = values[9] + value;	
						    		break;
							  case "dur15_30m": values[10] = values[10] + value;	
				            		break;
							}
						});
					})

					$("#barChart").kendoChart({
						theme: "black",
						title: {
				            text: "Visitor segmentation by attention span",
				            color: "#f6ae40",
				            font: "bold 16px Muli,sans-serif"
				        },
				        series: [{ 
				        	type: "column",
				        	data: values,
				        	color: "#f6ae40"
				        }],
				        categoryAxis: {
				        	categories: categoryNames,
				        	majorGridLines: {
				                visible: false  //display vertical grid.
				            }
				        },
				        valueAxis: {
				            line: {
				                visible: false  //display y axis.
				            }
				        },
				        tooltip: {
				            visible: true,
				            shared: true,
				            format: "N0",
				            font: "12px Muli,sans-serif"
				        }
					});
					
					$("#lineChart").html($("#vcaChartStepChoices").html());
					$("#countChart").kendoChart({
						theme: "black",
						dataSource: {
				            data: attentionCountList
				        },
						title: {
				            text: "Attention span variation over time",
				            color: "#f6ae40",
				            font: "bold 16px Muli,sans-serif"
				        },
				        legend: {
				            position: "top"
				        },
				        series: [{ 
				        	type: "column",
				        	field: "totalDuration",
				        	name: "average duration",
				        	color: "#f6ae40",
				        	//aggregate: "sum",
				        	aggregate: function(values, series, dataItems, category) {
				        		var totalDur = 0;
				        		var totalEvt = 0;
				        		$.each(dataItems, function(index, element) {
				        			totalDur += element.totalDuration;
				        			totalEvt += element.eventNum;
				        		})
				        		return totalDur / totalEvt
				        	},
				        	categoryField: "date",  //group by date, specify horizontal field display what.
				        	axis: "durationAvg"
				        },{
				        	type: "line",
				        	field: "eventNum",
				        	name: "number of faces",
				        	color: "#fe6e2c",
				        	aggregate: "sum",
				        	categoryField: "date",
				        	axis: "eventNum"
				        }],
				        valueAxis: [{
				        	name: "durationAvg",
		                    title: { text: "Average duration" },
				        	color: "#f6ae40",
				        	line: { visible: true },
				        	labels: { template: "#= Math.floor(value/60) #m #= value % 60 #s" }
		                }, {
		                	name: "eventNum",
		                    title: { text: "Number of faces" },
		                    color: "#fe6e2c",
		                    line: { visible: true }
		                }],
				        categoryAxis: {
				        	field: "date",
				            baseUnit: chartBaseUnit,
				            labels: {
				            	step: step,  //Render label every second.
				                rotation: -90,
				                timeFormat: "HH:mm",
				                font: "12px Muli",
				                dateFormats: {
				                    hours: "dd-MM HH:mm",
				                    days: "dd MMM",
				                    weeks: "dd MMM",
				                    months: "MMM yyyy"
				                }
				            },
				            axisCrossingValues: [0, 100000],
				        	majorGridLines: {
				                visible: true
				            }
				        },
				        tooltip: {
				            visible: true,
				            shared: true,
				            format: "N0",
				            font: "12px Muli,sans-serif"
				        }
					});
					
					if(chartBaseUnit == "hours") {
						$(".hourChoice").show();
				        $("#stepHours").click();
					} else if(chartBaseUnit == "days") {
						$(".hourChoice").hide();
				        $("#stepDays").click();
					}
					
					// stepsize change event.
					$("input[name=stepSize]").click(function() {
						var chart = $("#countChart").data("kendoChart");
						if(chart) {
							chart.options.categoryAxis.baseUnit = this.value;
							chart.refresh();
						}
					})
					
				} else {
					utils.popupAlert(resp.reason);
				}
			}, null);
	
}

audiattention.initEmailOptions = function() {
	$(".email-options").find(".parent").click(function(e){
		e.stopPropagation();
		$(this).next(".options").slideToggle(300);
		$(".export-options").find(".parent").next(".inner").slideUp(300);
		$(".permalink").find(".parent").next(".inner").slideUp(300);
	});
	$("#email-input").click(function(){
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
			if($(this).hasClass("is_active")){
				$(this).removeClass("is_active");
			}

			else{
				$(this).addClass("is_active")
			}
		}
		, function() {
			$(this).removeClass("is_active");
		}
	);
}

audiattention.initExportOptions = function() {
	$("#main-header").find(".export-options").find("li:last").addClass("last");

	$(".export-options").find(".parent").click(function(e){
		e.stopPropagation();
		$(this).next(".inner").slideToggle(300);
		$(".email-options").find(".parent").next(".options").slideUp(300);
		$(".permalink").find(".parent").next(".inner").slideUp(300);
	});
}

audiattention.initPermalinkOptions = function() {
	$("#main-header").find(".permalink").find("li:last").addClass("last");

	$(".permalink").find(".parent").click(function(e){
		e.stopPropagation();
		$(this).next(".inner").slideToggle(300);
		$(".export-options").find(".parent").next(".inner").slideUp(300);
		$(".email-options").find(".parent").next(".options").slideUp(300);
	});
}

$(document).ready(function() {
	audiattention.initDeviceList();
	audiattention.initDateRangePicker();
	audiattention.initEmailOptions();
	audiattention.initExportOptions();
	audiattention.initPermalinkOptions();
	
	// Radio buttons functionality.
	$("input.u-radio").uniform();
});
