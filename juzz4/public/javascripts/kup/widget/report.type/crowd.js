KUP.widget.report.type.crowd = (function($, kup) {
    var currentReportImage, currentReportRegionImage, currentStackChartImage, currentReportInfo;

    function showCrowdHeatmap(heatmapBase64Image, maximum) {

        //hardcode max to 100
        maximum = 100;

        $('#heatmapSnapshot').html('<img src="' + heatmapBase64Image + '" width="100%" height="100%" />');
        showHeatmapLegend(maximum);
    }

    function drawRegions(divId, regions) {
        var shapeOptions = {
            stroke: true,
            color: '#CD1625',
            weight: 1,
            opacity: 0.7,
            fillColor: '#CD1625',
            fillOpacity: 0.35,
            clickable: false
        };
        if (mapManager.map != undefined) {
            mapManager.map.remove();
        }
        crowddensity.initDrawingCanvas(divId, "", shapeOptions);
        crowddensity.addExistingAreas(regions);
    }

    function showHeatmapLegend(maximum) {
        var mark1 = maximum / 5;
        var mark2 = mark1 * 2;
        var mark3 = mark1 * 3;
        var mark4 = mark1 * 4;

        $("#mark0").html(0);
        $("#mark1").html(mark1.toFixed(0));
        $("#mark2").html(mark2.toFixed(0));
        $("#mark3").html(mark3.toFixed(0));
        $("#mark4").html(mark4.toFixed(0));
        $("#mark5").html(maximum);
    }
    
    function cloneCanvas(oldCanvas) {
	    //create a new canvas
	    var newCanvas = document.createElement('canvas');
	    var context = newCanvas.getContext('2d');

	    if (typeof oldCanvas != 'undefined') {
	    	//set dimensions
		    newCanvas.width = oldCanvas.width;
		    newCanvas.height = oldCanvas.height;
		  
		    //apply the old canvas to the new one
		    context.drawImage(oldCanvas, 0, 0);
	    }

	    //return the new canvas
	    return newCanvas;
	}

    var _kupReport = kup.widget.report,
        _self = {
            generateReport: function() {
                currentReportInfo = null;
                $("#crowdStackChart").empty();
                $('#heatmapSnapshot').empty();
                $("#heatmapWrapper").hide();
                var opt = _kupReport.getOpt(),
                    map = _kupReport.getMap(),
                    vcaEventType = map.vcaEventType[opt.reportType],
                    fromDate = opt.startDate,
                    toDate = opt.endDate;

                if (opt.selectedDeviceList.length <= 0 && opt.selectedChannelList.length <= 0) {
                    utils.popupAlert(i18n('no-channel-selected'));
                    return;
                }
                
                if (opt.selectedInstance.length > 1) {
                    utils.popupAlert(localizeResource("multiple-device-channel-not-supported"));
                    return;
                }

                var instance = opt.selectedInstance[0];
                if (utils.isNullOrEmpty(instance))
                    return;
                var selectedDeviceId = instance.platformDeviceId,
                    selectedChannelId = instance.channelId;
                //convert to UTC dates.
                var from = kendo.toString(utils.convertToUTC(opt.startDate), "ddMMyyyyHHmmss"),
                    to = kendo.toString(utils.convertToUTC(opt.endDate), "ddMMyyyyHHmmss");

                kup.utils.block.popup('#main-charts', i18n('generate-report'));
                var bgImageUrl = "";
                getLiveVideoUrl("", instance.coreDeviceId, selectedChannelId, "http/jpeg", null, function(resp) {
                    if (resp.result == "ok" && resp.url.length > 0) {
                        $("#heatmapSnapshot").css("background-image", "url(" + resp.url[0] + ")");
                        bgImageUrl = resp.url[0];
                    }
                });
                var getAnalyticsReportResponse = [];
                getAnalyticsReport("", KupEvent.CROWD_DETECTION, _kupReport.getDeviceGroups(), [selectedDeviceId], selectedChannelId, from, to, {},
                    function(resp) {
                        if (resp.result == "ok" && resp.data.length > 0) {
                            do {
                                var $heatmapCanvas = $("#heatmapSnapshot").empty();
                                if (resp.data.length == 0) {
                                    utils.popupAlert("no-records-found");
                                    break;
                                }

                                $("#heatmapWrapper").show();
                                var data = getAnalyticsReportResponse = resp.data;
                                var gradientCfg = {};
                                var allTracks = [];
                                var max = 1;
                                var width = $heatmapCanvas.width();
                                var height = $heatmapCanvas.height();
                                heatmapColumns = data[0].columns;
                                heatmapRows = data[0].rows;
                                colWidth = width / heatmapColumns;
                                rowHeight = height / heatmapRows;
                                var radius = Math.max(colWidth, rowHeight) * 1.2;
                                var heatmap = h337.create({
                                    container: document.getElementById('heatmapSnapshot'),
                                    maxOpacity: .5,
                                    radius: radius,
                                    blur: .75,
                                    // update the legend whenever there's an extrema change
                                    onExtremaChange: function onExtremaChange(data) {}
                                });
                                var gridPoints = [];
                                for (var i = 0; i < heatmapColumns; i++)
                                    gridPoints[i] = Array.apply(null, new Array(heatmapRows)).map(Number.prototype.valueOf, 0);
                                $.each(data, function(i, d) {
                                    var tracks = d.tracks;
                                    $.each(tracks, function(i, t) {
                                        //hit
                                        gridPoints[t.x][t.y] += t.value;
                                        max = Math.max(max, gridPoints[t.x][t.y]);
                                    });
                                });
                                var d = [];
                                for (var i = 0; i < gridPoints.length; i++) {
                                    for (var j = 0; j < gridPoints[i].length; j++) {
                                        if (gridPoints[i][j] <= 0)
                                            continue;

                                        var x = colWidth * i + colWidth / 2;
                                        var y = rowHeight * j + rowHeight / 2;
                                        d.push({
                                            x: x,
                                            y: y,
                                            value: gridPoints[i][j]
                                        });
                                    }
                                }
                                gridPoints.length = 0
                                gridPoints = null;
                                heatmap.setData({
                                    min: 0,
                                    max: max,
                                    data: d
                                });
                                showHeatmapLegend(100);

                                deviceManager.WaitForReady(function() {
                                    var reportInfo = {};
                                    deviceManager.attachDeviceDetails(reportInfo, selectedDeviceId, null, selectedChannelId);

                                    currentReportInfo = {
                                        "event-type": KupEvent.CROWD_DETECTION,
                                        "device-name": reportInfo.deviceName,
                                        "channel": reportInfo.channelName,
                                        "from": kendo.toString(fromDate, kupapi.TIME_FORMAT),
                                        "to": kendo.toString(toDate, kupapi.TIME_FORMAT)
                                    };
                                });

                                var canvas = $("#heatmapSnapshot > canvas").get(0);
                                var canvasData = canvas.toDataURL();
                                currentReportImage = canvasData.substring(canvasData.indexOf(",") + 1).trim();
                            } while (false);

                            listRunningAnalytics("", analyticsType.CROWD_DETECTION, function(responseData) {
                                var seriesInfo = [];
                                var countList = [];
                                var settings;
                                if (responseData.result == "ok" && responseData.instances != null) {
                                    $.each(responseData.instances, function(index, instance) {
                                        if (instance.platformDeviceId == selectedDeviceId && instance.channelId == selectedChannelId) {
                                            settings = JSON.parse(instance.thresholds);
                                            countList.push({
                                                time: opt.startDate,
                                                level: 0
                                            }); //make sure graph starts at fromDate
                                            countList.push({
                                                time: opt.endDate,
                                                level: 0
                                            }); //and ends at toDate
                                            if (settings.regions !== undefined) {
                                                $.each(settings.regions, function(index, region) {
                                                    var field = "level" + index;
                                                    seriesInfo.push({
                                                        name: region.name,
                                                        field: field,
                                                        aggregate: "sum",
                                                        color: vca.chartColors[index]
                                                    });
                                                    var width = $heatmapCanvas.width();
                                                    var height = $heatmapCanvas.height();
                                                    var vertexX = [];
                                                    var vertexY = [];
                                                    var vertexN = region.points.length;
                                                    $.each(region.points, function(index, coordinates) {
                                                        vertexX.push(coordinates.x * width);
                                                        vertexY.push(coordinates.y * height);
                                                    });
                                                    $.each(getAnalyticsReportResponse, function(index, reportData) {
                                                        var totalValues = 0;
                                                        $.each(reportData.tracks, function(i, d) {
                                                            var trackX = d.x * colWidth;
                                                            var trackY = d.y * rowHeight;
                                                            pnpoly(vertexN, vertexX, vertexY, trackX, trackY);

                                                            function pnpoly(vertexN, vertexX, vertexY, trackX, trackY) {
                                                                var i, j, c = 0;
                                                                for (i = 0, j = vertexN - 1; i < vertexN; j = i++) {
                                                                    if (((vertexY[i] > trackY) != (vertexY[j] > trackY)) &&
                                                                        (trackX < (vertexX[j] - vertexX[i]) * (trackY - vertexY[i]) / (vertexY[j] - vertexY[i]) + vertexX[i]))
                                                                        c = !c;
                                                                }
                                                                if (c) {
                                                                    totalValues += d.value;
                                                                }
                                                            }
                                                        });
                                                        var data = {};
                                                        data.time = utils.convertUTCtoLocal(kendo.parseDate(reportData.date, "yyyy/MM/dd HH:mm:ss"));
                                                        data[field] = isNaN(totalValues) ? 0 : Math.floor(totalValues);
                                                        countList.push(data);
                                                    });
                                                });
                                            }
                                        }
                                    });
                                    if (countList.length > 2) {
                                        vca.createStackCountChart("crowdStackChart", localizeResource("stack-chart"), seriesInfo, countList);
                                        drawRegions("heatmapSnapshot", settings.regions);
                                    }
                                }
                            });
                            opt.isSuccessReport = true;
                            _kupReport.setOpt(opt);
                            _kupReport.updateUI.showDiv();
                        } else {
                            opt.isSuccessReport = false;
                            _kupReport.setOpt(opt);
                            _kupReport.updateUI.cleanDiv();
                            utils.popupAlert(kup.utils.i18n("no-records-found"));
                        }
                        kup.utils.block.close('#main-charts');
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
                    posNames = data.posNames;
                var baseUnit = data.baseUnit;
                var opt = _kupReport.getOpt(),
                    utils = kup.utils.default,
                    i18n = kup.utils.i18n,
                    chartRadioID = opt.chartRadioType,
                    isMultiDeviceSelected = opt.isMultiDeviceSelected,
                    isSuccessReport = opt.isSuccessReport;

                //only a single camera export is allowed for crowd
                var targetDvc = dvcId[0];

                //verification
                if (!isSuccessReport || targetDvc == null) {
                    utils.popupAlert(i18n('please-generate-reports'));
                    return;
                }

                kup.utils.block.popup('#main-charts', i18n('generating-download-file'));

                var fromStr = kendo.toString(utils.convertToUTC(fromDate), "ddMMyyyyHHmmss");
                var toStr = kendo.toString(utils.convertToUTC(toDate), "ddMMyyyyHHmmss");

                window.exportDataLogs(
                    "csv",
                    KupEvent.CROWD_DETECTION,
                    targetDvc.platformDeviceId,
                    targetDvc.channelId,
                    fromStr,
                    toStr,
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
            exportPdf: function(data) {
                if (currentReportImage == null || currentReportInfo == null || Object.keys(currentReportInfo).length == 0) {
                    console.log("missing report info");
                    return;
                }

                currentReportRegionImage = "";
                var canvas = cloneCanvas($(".leaflet-map-pane canvas").get(0));
                if (canvas) {
                	var ctx = canvas.getContext("2d");
                	$.each($(".leaflet-marker-pane div"), function(i, overlayText) {
                	   var position = $(overlayText).position();
                	   ctx.font = "20px Arial";
                	   ctx.fillStyle = 'white';
                	   ctx.fillText($(overlayText).text(), canvas.width / 4 + position.left,canvas.height / 4 + position.top);
                	});
                	
                    var canvasData = canvas.toDataURL();
                    currentReportRegionImage = canvasData.substring(canvasData.indexOf(",") + 1).trim();
                }
                var svg = vca.getPrinterFriendlyChart("countChart");
                var svgStr = [];
                if (!utils.isNullOrEmpty(svg)) {
                    svgStr.push(svg);

                    var n = 1;
                    svg = vca.getPrinterFriendlyChart("countChart" + n);
                    while (svg != null && svg != "") {
                        svg = svg.replace("<?xml version='1.0' ?>", "");
                        svgStr.push(svg);
                        n++;
                        svg = vca.getPrinterFriendlyChart("crowdStackChart" + n);
                    }
                }

                var dvcId = data.selectedInstance[0].platformDeviceId,
                    channId = data.selectedInstance[0].channelId;
                kup.utils.block.popup('#main-charts', i18n('generate-report'));
                exportCrowdReport("", dvcId, channId, currentReportImage, currentReportRegionImage, svgStr.join(""), JSON.stringify(currentReportInfo),
                    function(responseData) {
                        if (responseData != null && responseData.result == "ok" &&
                            responseData["download-url"] != null) {
                            window.open(responseData["download-url"], '_blank');
                            window.focus();
                        } else {
                            utils.throwServerError(responseData);
                        }

                        kup.utils.block.close('#main-charts');
                    },
                    function() {
                        kup.utils.block.close('#main-charts');
                    });
            },
            loadSetUI: function() {},
            loadUpdateUI: function() {
                _kupReport.type.peoplecounting.loadUpdateUI();
                $("#heatmapWrapper").hide();
            }
        };
    return _self;
})(jQuery, KUP);
