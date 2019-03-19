angular.module('kai.reports.traffic', []);

angular
    .module('kai.reports.traffic')
    .factory("TrafficSvgService",
        function(KupOption, UtilsService,AuthTokenFactory, $http) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var KaiFlow = {};

            KaiFlow.paper = null;

            KaiFlow.cfg = {
                cssSelector: null,
                width: null,
                height: null,
                showRegionsByDefault: false
            };

            KaiFlow.style = {
                region: {
                    'fill': '#F6AE40',
                    'opacity': "0.3",
                    'stroke': '#222',
                    "stroke-width": 1
                },
                arrow: {
                    opacity: "0.8",
                    stroke: ["#205FA4", "#ff2a3f", "#8dc051", "#fe642f", "#f6ae40", "#428bca", "#32a8ad", "#ffe400", "#b777d5"],
                    end: "short",
                    resizeFactor: 0.7
                },
                label: {
                    "font-size": "18px",
                    "font-weight": "550",
                    fill: "#f5f5f5",
                    stroke: "#ddd",
                    "stroke-width": 0,
                    bgFill: "#000",
                    bgOpacity: 0.5
                }
            }

            KaiFlow.current = {
                regionName: null,
                showRegions: null,
                regions: [],
                curveArrows: [],
                labels: []
            };

            KaiFlow.data = {
                sourceName: null,
                regionMap: {},
                flowMap: {}
            }

            KaiFlow.generate = function(configs, regions, flowData, bgImageUrl) {
                KaiFlow.remove();
                KaiFlow.cfg = $.extend(KaiFlow.cfg, configs);
                //KaiFlow.initUI();
                KaiFlow.initRaphael();
                KaiFlow.prepareData(regions, flowData);
                KaiFlow.setBgImage(bgImageUrl);
                KaiFlow.drawRegions();
            }

            KaiFlow.remove = function() {
                $("#" + KaiFlow.cfg.cssSelector).empty();

                var prevShowRegions = KaiFlow.cfg.showRegionsByDefault;
                if (KaiFlow.current.showRegions != null) {
                    prevShowRegions = KaiFlow.current.showRegions;
                }

                KaiFlow.current = {
                    regionName: null,
                    showRegions: prevShowRegions,
                    regions: [],
                    curveArrows: [],
                    labels: []
                };

                KaiFlow.data = {
                    sourceName: null,
                    regionMap: {},
                    flowMap: {}
                }

            }

            KaiFlow.toggleRegions = function() {
                KaiFlow.setRegionsVisibility(!KaiFlow.current.showRegions);

            }

            KaiFlow.setRegionsVisibility = function(visible) {
                if (visible) {
                    for (var i = 0; i < KaiFlow.current.regions.length; i++) {
                        KaiFlow.current.regions[i].attr(KaiFlow.style.region);
                        KaiFlow.current.showRegions = true;
                    }
                } else {
                    for (var i = 0; i < KaiFlow.current.regions.length; i++) {
                        KaiFlow.current.regions[i].attr({
                            'opacity': "0"
                        });
                        KaiFlow.current.showRegions = false;
                    }
                }
            }

            KaiFlow.getSerializedSVG = function() {
                var svgList = $("#" + KaiFlow.cfg.cssSelector + " svg");
                if (!svgList || svgList.length == 0) {
                    console.error("no svg image found");
                }

                var xmlSerializer = new XMLSerializer;
                return xmlSerializer.serializeToString(svgList[0]);
            }

            KaiFlow.openInNewWin = function() {
                var svgString = KaiFlow.getSerializedSVG();
                var win = window.open();
                $(win.document.body).html(svgString);
            }

            //use base64 dataUrl only. Export will not work with direct links
            KaiFlow.setBgImage = function(imageUrl) {
                var emptyDataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAALklEQVQ4T2NUUlL6z4AH3Lt3D580A+OoAcMiDICRjDcdAKMZfzoYNYCBceiHAQDsuisx5wDZVAAAAABJRU5ErkJggg==";

                if (!imageUrl || imageUrl.length == 0) {
                    imageUrl = emptyDataUrl;
                }

                KaiFlow.paper.image(imageUrl, 0, 0, KaiFlow.cfg.width, KaiFlow.cfg.height);
            };

            KaiFlow.convertToFlowDataFromAnalyticsData = function(regionMap, trafficFlowOutput) {
                var flowData = [];
                var sourceName = regionMap.sourceName;
                var regionsCalculation = {};
                $.each(trafficFlowOutput, function(i, o) {
                    var region = regionsCalculation[o.from + "-" + o.to];
                    if (typeof region == 'undefined') {
                        region = {
                            "from": o.from,
                            "to": o.to,
                            "count": o.count
                        };
                        regionsCalculation[o.from + "-" + o.to] = region;
                    } else {
                        region.count += o.count;
                    }
                });

                $.each(regionsCalculation, function(o, r) {
                    //        flowData.push(r.from + "," + r.to + "," + r.count);
                    flowData.push(r);
                });

                return flowData;
            };

            //custom parser to read vca output
            KaiFlow.convertToFlowData = function(regionMap, trafficFlowOutput) {
                var flowData = [];
                var tios = trafficFlowOutput.tios[0];
                var orderedCountList = tios.src2dst;

                var sourceName = regionMap.sourceName;
                var listWithoutSource = [];
                regionMap.regions.forEach(function(region, i) {
                    if (region.name != sourceName) {
                        listWithoutSource.push(region.name);
                    }
                });

                listWithoutSource.forEach(function(name, i) {
                    var count = orderedCountList[i];
                    flowData.push({
                        from: sourceName,
                        to: name,
                        count: count
                    });
                });

                console.log("sourceName         : " + sourceName);
                console.log("listWithoutSource  : " + listWithoutSource);
                console.log("orderedCountList   : " + orderedCountList);
                console.log(flowData);

                return flowData;
            }

            KaiFlow.prepareData = function(flowRegions, flowData) {
                KaiFlow.data.sourceName = flowRegions.sourceName;

                //Replace missing flows with zero counts
                var allFlows = KaiFlow.populateMissingFlows(flowRegions.regions, flowData);

                //prepare regions
                KaiFlow.data.regionMap = {};
                flowRegions.regions.forEach(function(region, i) {
                    KaiFlow.data.regionMap[region.name] = region;
                });

                //prepare flows
                for (var i = 0; i < allFlows.length; i++) {
                    var srcName = allFlows[i].from;
                    var dstName = allFlows[i].to;
                    var count = allFlows[i].count;

                    if (!KaiFlow.data.flowMap[srcName]) {
                        KaiFlow.data.flowMap[srcName] = {
                            total: 0,
                            dstList: []
                        };
                    }

                    KaiFlow.data.flowMap[srcName].dstList.push({
                        name: dstName,
                        count: count
                    });
                    KaiFlow.data.flowMap[srcName].total += parseInt(count);
                }
            }

            KaiFlow.initUI = function() {
                var btnHtml = '<div>' + ' <a href="javascript:KaiFlow.toggleRegions()" class="k-button">' + i18n("show-hide-regions") + '</a>' + '</div>';

                $("#" + KaiFlow.cfg.cssSelector).append(btnHtml);
            }

            KaiFlow.callback = function() {
                // $('tspan').attr('dy', 0);
                // console.log(123);
                // setTimeout(function() {
                //     console.log($("tspan"));    
                // }, 2000);
            }

            KaiFlow.initRaphael = function() {
                KaiFlow.paper = Raphael(KaiFlow.cfg.cssSelector, '100%', KaiFlow.cfg.height);
                KaiFlow.paper.setViewBox(0, 0, KaiFlow.cfg.width, KaiFlow.cfg.height);

                // Custom attribute function that returns segment of curve
                KaiFlow.paper.ca.arc = function(x) {
                    var length = this.attr('length');
                    var path = this.attr('original');
                    var subPath = Raphael.getSubpath(path, 0, x * length);
                    return {
                        path: subPath
                    };
                }

                // Empty functions to allow us to add static custom attributes
                KaiFlow.paper.ca.length = function() {}
                KaiFlow.paper.ca.original = function() {}
            }

            KaiFlow.drawRegions = function() {
                if (!KaiFlow.data.regionMap) {
                    console.log("No regions to draw");
                    return;
                }

                $.each(KaiFlow.data.regionMap, function(name, region) {
                    var regionPath = KaiFlow.paper
                        .path(KaiFlow.getPathString(region))
                        .attr(KaiFlow.style.region)
                        .data('regionName', region.name)
                        .hover(function() {
                            KaiFlow.drawTrafficFlows(this.data('regionName'));
                        });

                    //save path reference
                    region.path = regionPath;
                    KaiFlow.data.regionMap[region.name] = region;
                    KaiFlow.current.regions.push(regionPath);
                });

                if (Object.keys(KaiFlow.data.flowMap).length == 0) {
                    KaiFlow.handleAllZeroFlows();
                } else {
                    KaiFlow.drawTrafficFlows(KaiFlow.data.sourceName);
                }

                KaiFlow.setRegionsVisibility(KaiFlow.current.showRegions);
                $("tspan").attr("dy", "0");
            }

            KaiFlow.drawTrafficFlows = function(srcName) {
                if (srcName === KaiFlow.current.regionName) {
                    return;
                }
                if (Object.keys(KaiFlow.data.flowMap).length == 0) {
                    console.log("No flows to draw");
                    return;
                }

                KaiFlow.removeCurrentFlows();
                KaiFlow.current.regionName = srcName;

                var srcRegion = KaiFlow.data.regionMap[srcName];
                var srcCenter = KaiFlow.getPolygonCentroid(srcRegion);
                var srcFlow = KaiFlow.data.flowMap[srcName];

                if (srcFlow == null) {
                    console.log("No flows for : " + srcName);
                    return;
                }

                var srcTotal = srcFlow.total;
                var dstFlowList = srcFlow.dstList;
                var accruedPercent = 0;

                dstFlowList.forEach(function(dst, i) {
                    var percent = 0;
                    if (srcTotal != 0) { //prevent NaN
                        percent = Math.round((dst.count / srcTotal) * 100);
                    }

                    //ensure percent adds up to 100%
                    accruedPercent += percent;
                    if (i == (dstFlowList.length - 1)) { //last item
                        if (accruedPercent > 100) {
                            console.log("accrued percent: " + accruedPercent);
                            console.log("last percent: " + percent);
                            percent = percent - (accruedPercent - 100);
                            console.log("adjusted: " + percent);
                        }
                    }

                    //print arrows
                    var dstCenter = KaiFlow.getPolygonCentroid(KaiFlow.data.regionMap[dst.name]);
                    KaiFlow.drawArrow(srcCenter, dstCenter, percent, i);

                    //print percent
                    //        var labelLocation = KaiFlow.midpoint(srcCenter, dstCenter);
                    var labelLocation = dstCenter;
                    KaiFlow.printLabel(percent, labelLocation);
                });
            }

            KaiFlow.handleAllZeroFlows = function() {

                var srcRegion = KaiFlow.data.regionMap[KaiFlow.data.sourceName];
                var srcCenter = KaiFlow.getPolygonCentroid(srcRegion);

                var i = 0;
                $.each(KaiFlow.data.regionMap, function(name, region) {
                    if (name === KaiFlow.data.sourceName) {
                        return true;
                    }

                    //print arrows
                    var dstCenter = KaiFlow.getPolygonCentroid(region);
                    KaiFlow.drawArrow(srcCenter, dstCenter, 0, i++);

                    //print labels
                    //        var labelLocation = KaiFlow.midpoint(srcCenter, dstCenter);
                    var labelLocation = dstCenter;
                    KaiFlow.printLabel(0, labelLocation);
                });
            };

            KaiFlow.removeCurrentFlows = function() {
                for (var i = 0; i < KaiFlow.current.curveArrows.length; i++) {
                    KaiFlow.current.curveArrows[i].remove();
                }

                for (var i = 0; i < KaiFlow.current.labels.length; i++) {
                    KaiFlow.current.labels[i].remove();
                }
            }

            KaiFlow.drawArrow = function(srcCenter, dstCenter, percent, arrowIndex) {
                var factor = KaiFlow.getCurveFactor(srcCenter, dstCenter);
                var qPath = KaiFlow.qPath(srcCenter, KaiFlow.quadraticMidpointFromConnection(srcCenter, dstCenter, factor), dstCenter);
                var length = Raphael.getTotalLength(qPath);
                var width = KaiFlow.getArrowWidth(percent);

                var curvePath = KaiFlow.paper.path(qPath);
                curvePath.attr({
                    opacity: 0,
                    original: qPath,
                    length: length,
                    'arrow-end': KaiFlow.style.arrow.end,
                    'stroke': KaiFlow.style.arrow.stroke[arrowIndex],
                    'stroke-width': width
                });

                curvePath.attr({
                    arc: 0.5
                });
                KaiFlow.current.curveArrows.push(curvePath);
                // Animate
                var animation = Raphael.animation({
                    arc: 1.1,
                    opacity: KaiFlow.style.arrow.opacity
                }, 500, '>');
                curvePath.animate(animation.delay(arrowIndex * 0));
            }

            KaiFlow.printLabel = function(percent, labelLocation) {
                //label group
                var group = KaiFlow.paper.set();

                //label bg rectangle
                var labelBg = KaiFlow.paper
                    .rect(labelLocation.x, labelLocation.y, 44, 26)
                    .attr({
                        fill: KaiFlow.style.label.bgFill,
                        opacity: KaiFlow.style.label.bgOpacity,
                        "stroke-width": 0
                    });
                group.push(labelBg);

                //label text
                var arrowLabel = KaiFlow.paper
                    .text(labelLocation.x + 22, labelLocation.y + 13, percent + "%")
                    .attr(KaiFlow.style.label);
                group.push(arrowLabel);

                KaiFlow.current.labels.push(group);
            };

            KaiFlow.getPathString = function(polygon) {
                if (!polygon || polygon.points.length < 3) {
                    console.log("invalid polygon");
                    return "";
                }

                var firstPoint;
                var pathString;
                polygon.points.forEach(function(point, i) {
                    var xy = KaiFlow.getProjectedXY(point);
                    if (i == 0) {
                        pathString += "M";
                        firstPoint = xy;
                    } else {
                        pathString += " L"
                    }

                    pathString += xy.x + "," + xy.y;

                    //close the polygon
                    if (i == (polygon.points.length - 1)) {
                        pathString += " L" + firstPoint.x + "," + firstPoint.y + " Z";
                    }
                });

                return pathString;
            }

            KaiFlow.getBoundsCenter = function(path) {
                var bbox = path.getBBox();
                var cX = Math.floor(bbox.x + bbox.width / 2.0);
                var cY = Math.floor(bbox.y + bbox.height / 2.0);
                return {
                    x: cX,
                    y: cY
                };
            }

            KaiFlow.getPolygonCentroid = function(polygon) {
                var points = polygon.points;
                var centroid = {
                    x: 0,
                    y: 0
                };
                for (var i = 0; i < points.length; i++) {
                    var point = KaiFlow.getProjectedXY(points[i]);
                    centroid.x += point.x;
                    centroid.y += point.y;
                }
                centroid.x /= points.length;
                centroid.y /= points.length;
                return centroid;
            }

            KaiFlow.getProjectedXY = function(p) {
                return {
                    x: Math.round(parseFloat(p.x) * KaiFlow.cfg.width),
                    y: Math.round(parseFloat(p.y) * KaiFlow.cfg.height)
                };
            }

            KaiFlow.normalizeXY = function(p) {
                return {
                    x: p.x / KaiFlow.cfg.width,
                    y: p.y / KaiFlow.cfg.height
                };
            }

            KaiFlow.distance = function(p0, p1) {
                return Math.sqrt(Math.pow(p1.x - p0.x, 2) + Math.pow(p1.x - p0.x, 2));
            }

            KaiFlow.midpoint = function(p0, p1) {
                return {
                    x: p0.x + (p1.x - p0.x) / 2,
                    y: p0.y + (p1.y - p0.y) / 2
                };
            }

            KaiFlow.dir = function(p0, p1) {
                var l = this.distance(p0, p1);
                return {
                    x: (p1.x - p0.x) / l,
                    y: (p1.y - p0.y) / l
                };
            }

            KaiFlow.orthogonal = function(dir) {
                return {
                    x: -dir.y,
                    y: dir.x
                };
            }

            KaiFlow.quadraticMidpointFromConnection = function(p0, p1, k) {
                var l = this.distance(p0, p1);
                var d = this.dir(p0, p1);
                var orth = this.orthogonal(d);
                var mid = this.midpoint(p0, p1);
                var kl = l * k;
                return {
                    x: mid.x + kl * orth.x,
                    y: mid.y + kl * orth.y
                };
            }

            KaiFlow.qPath = function(start, mid, end) {
                var path = "M" + start.x + " " + start.y;
                path += " Q" + mid.x + " " + mid.y;
                path += " " + end.x + " " + end.y;
                return path;
            }

            KaiFlow.getCurveFactor = function(srcPoint, dstPoint) {
                var curvature = 0.2;
                var svgCenter = {
                    x: 0.5,
                    y: 0.5
                };
                var onLeftSide = KaiFlow.isLeft(KaiFlow.normalizeXY(srcPoint), svgCenter, KaiFlow.normalizeXY(dstPoint));

                return onLeftSide ? -curvature : curvature;
            }

            KaiFlow.isLeft = function(lineStart, lineEnd, checkPt) {
                var a = lineStart;
                var b = lineEnd;
                var c = checkPt;
                return ((b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)) > 0;
            }

            KaiFlow.getArrowWidth = function(percent) {
                var width = percent * KaiFlow.style.arrow.resizeFactor;

                //min
                if (width < 20) {
                    return 20;
                }
                //max
                if (width > 60) {
                    return 60;
                }

                return width;
            };

            KaiFlow.populateMissingFlows = function(regions, flowsWithData) {
                var regionNames = [];
                $.each(regions, function(idx, r) {
                    regionNames.push(r.name);
                });

                var all = utils.permutate(regionNames, 2);
                var allFlowData = [];
                $.each(all, function(idx, pair) {
                    if (pair[0] != pair[1]) {
                        allFlowData.push({
                            from: pair[0],
                            to: pair[1],
                            count: 0
                        });
                    }
                });

                //create lookup map
                var inputFlowMap = {};
                $.each(flowsWithData, function(i, flow) {
                    inputFlowMap[flow.from + "" + flow.to] = flow.count;
                });

                $.each(allFlowData, function(i, flow) {
                    if (inputFlowMap[flow.from + "" + flow.to]) {
                        flow.count = inputFlowMap[flow.from + "" + flow.to];
                    }
                });

                return allFlowData;
            };

            return KaiFlow;
        }
    );

angular
    .module('kai.reports.traffic')
    .factory("TrafficService",
        function(KupOption, UtilsService, PromiseFactory, KupApiService, ReportsService, AuthTokenFactory, TrafficSvgService, $q, $timeout) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var ajaxPost = KupApiService.ajaxPost;
            var reports = ReportsService;
            var apiServerUrl = AuthTokenFactory.getApiRootUrl();
            var kaiFlow = TrafficSvgService;

            var data = {
                //ui selector
                $flow: '#trafficSvg',
                //time format 
                timeFormat0: 'yyyy/MM/dd HH:mm:ss',
                timeFormat1: 'dd/MM/yyyy HH:mm:ss',
                timeFormat2: 'ddMMyyyyHHmmss',
                //selected data
                selectedVcaData: {},
                selectedRegions: {},
                //from api reponse data
                apiAnalyticsReportList: [],
                apiRunningAnalyticsList: [],
                apiLiveVideoUrl: [],
                //request List
                requestForReport: [],
            };

            return {
                data: data,

                getAnalyticsReportApi: getAnalyticsReportApi,
                exportSecurityCrossSiteCSVApi: exportSecurityCrossSiteCSVApi,
                exportTrafficFlowPdfApi: exportTrafficFlowPdfApi,
                listRunningAnalyticsApi: listRunningAnalyticsApi,
                getLiveVideoUrlApi: getLiveVideoUrlApi,

                generateReport: generateReport,
                generateSVGImage: generateSVGImage,
            };
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function getAnalyticsReportApi(isDefer) {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;

                var fromDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.startDate), data.timeFormat2);
                var toDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.endDate), data.timeFormat2);

                var instance = reportsOpt.selectedInstance[0];
                var selectedDeviceId = instance.platformDeviceId;
                var selectedChannelId = instance.channelId;
                var coreDeviceId = instance.deviceId;

                var param = {
                    "event-type": vcaEventType,
                    "device-id-list": JSON.stringify([selectedDeviceId]),
                    "channel-id": selectedChannelId,
                    "from": fromDateUTC,
                    "to": toDateUTC,
                    "parameters": JSON.stringify({}),
                };

                var onSuccess = function(response) {
                    opt.apiAnalyticsReportList = response.data || [];
                };
                var onFail = function(response) {
                    opt.apiAnalyticsReportList = [];
                };
                var onError = function() {
                    opt.apiAnalyticsReportList = [];
                };
                return ajaxPost('getanalyticsreport', param, onSuccess, onFail, onError, isDefer);
            }

            function exportSecurityCrossSiteCSVApi(periodType) {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;
                var dvcId = reportsOpt.selectedInstance;
                var fromDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.startDate), opt.timeFormat2);
                var toDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.endDate), opt.timeFormat2);
                var param = {
                    "file-format": "csv",
                    "event-type": vcaEventType,
                    "from": fromDateUTC,
                    "to": toDateUTC,
                    "time-zone-offset": KupApiService.data.timeZoneOffset,
                    "device-id": dvcId[0].platformDeviceId,
                    "channel-id": dvcId[0].channelId
                };

                return KupApiService.exportDoc(param, 'exportdatalogs');
            }

            function exportTrafficFlowPdfApi() {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;
                var dvcId = reportsOpt.selectedInstance;
                var fromDateUTC = kendo.toString(reportsOpt.dateRange.startDate, opt.timeFormat1);
                var toDateUTC = kendo.toString(reportsOpt.dateRange.endDate, opt.timeFormat1);
                var base64Image = (function() {
                    var svgList = $(opt.$flow).find('svg').attr({ width: 600 });
                    var xmlSerializer = new XMLSerializer;
                    var svg = xmlSerializer.serializeToString(svgList[0]);
                    $(opt.$flow).find('svg').attr({ width: '100%' });
                    return svg;
                })();
                var reportInfo = {
                    "event-type": vcaEventType,
                    "device-name": dvcId[0].deviceName,
                    "channel": dvcId[0].channelName,
                    "from": fromDateUTC,
                    "to": toDateUTC
                };
                var param = {
                    "time-zone-offset": KupApiService.data.timeZoneOffset,
                    "base64-image": base64Image,
                    "report-info": JSON.stringify(reportInfo)
                };

                return KupApiService.exportDoc(param, 'exporttrafficflowpdf');
            }

            function listRunningAnalyticsApi(isDefer) {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaAnalyticsType = kupOpt.vca[reportsOpt.reportType].analyticsType;
                var param = {
                    "analytics-type": vcaAnalyticsType,
                };
                var instance = reportsOpt.selectedInstance[0];
                var selectedDeviceId = instance.platformDeviceId;
                var selectedChannelId = instance.channelId;
                var coreDeviceId = instance.deviceId;

                var onSuccess = function(response) {
                    var vcaData = {};
                    var regions = {};
                    $.each(response.instances, function(i, inst) {
                        if (inst.platformDeviceId == selectedDeviceId && inst.channelId == selectedChannelId) {
                            vcaData = inst;
                            return false;
                        }
                    });

                    if (!$.isEmptyObject(vcaData)) {
                        regions = utils.tryParseJson(vcaData.thresholds);
                    }

                    opt.apiRunningAnalyticsList = response.instances || [];
                    opt.selectedVcaData = vcaData;
                    opt.selectedRegions = regions;
                };
                var onFail = function(response) {
                    opt.apiRunningAnalyticsList = [];
                    opt.selectedVcaData = {};
                    opt.selectedRegions = {};
                };
                var onError = function() {
                    opt.apiRunningAnalyticsList = [];
                    opt.selectedVcaData = {};
                    opt.selectedRegions = {};
                };
                return ajaxPost('listrunninganalytics', param, onSuccess, onFail, onError, isDefer);
            }

            function getLiveVideoUrlApi(isDefer) {
                var opt = data;
                var reportsOpt = reports.data;

                var instance = reportsOpt.selectedInstance[0];
                var selectedDeviceId = instance.platformDeviceId;
                var selectedChannelId = instance.channelId;
                var coreDeviceId = instance.deviceId;
                var streamType = "http/jpeg";

                var param = {
                    "device-id": coreDeviceId,
                    "channel-id": selectedChannelId,
                    "stream-type": streamType,
                };

                var onSuccess = function(response) {
                    opt.apiLiveVideoUrl = response.url || [];
                };
                var onFail = function(response) {
                    opt.apiLiveVideoUrl = [];
                };
                var onError = function() {
                    opt.apiLiveVideoUrl = [];
                };
                return ajaxPost('getlivevideourl', param, onSuccess, onFail, onError, isDefer);
            }


            function generateSVGImage(imgH, imgW) {
                imgW = imgW || 600;
                imgH = imgH || 450
                var jsonOutput = data.apiAnalyticsReportList;
                var regions = data.selectedRegions;
                var imgDataUrl = data.apiLiveVideoUrl[0] || '';
                var options = {
                    cssSelector: data.$flow.substring(1),
                    width: imgW,
                    height: imgH,
                    showRegionsByDefault: false
                };

                if (jsonOutput.length <= 0 || $.isEmptyObject(regions)) {
                    return false;
                }
                // var flowData = kaiFlow.convertToFlowData(regions, jsonOutput);
                var flowData = kaiFlow.convertToFlowDataFromAnalyticsData(regions, jsonOutput);
                kaiFlow.generate(options, regions, flowData, imgDataUrl);
            }

            function generateReport() {
                var opt = data;

                //cancel last time request
                $.each(opt.requestForReport, function(i, request) {
                    request.cancel && request.cancel();
                });

                //check report status
                if (!reports.isSelectCamera()) {
                    notification('error', i18n('no-channel-selected'));
                    reports.isSuccessReport(false);
                    return false;
                }
                if (!reports.isSingleCamera()) {
                    notification('error', i18n("multiple-device-channel-not-supported"));
                    reports.isSuccessReport(false);
                    return false;
                }

                //set generate report promise
                opt.requestForReport = [
                    listRunningAnalyticsApi(true),
                    getLiveVideoUrlApi(true),
                    getAnalyticsReportApi(true)
                ];

                //return promise
                var dfd = $q.defer();
                $timeout(function() {
                    $q.all(opt.requestForReport)
                        .finally(function() {
                            if (!opt.apiRunningAnalyticsList.length) {
                                reports.isSuccessReport(false);
                                dfd.reject();
                                return;
                            }
                            if (!opt.apiLiveVideoUrl.length) {
                                reports.isSuccessReport(false);
                                dfd.reject();
                                return;
                            }
                            if (!opt.apiAnalyticsReportList.length) {
                                reports.isSuccessReport(false);
                                dfd.reject();
                                return;
                            }
                            if ($.isEmptyObject(opt.selectedVcaData)) {
                                reports.isSuccessReport(false);
                                dfd.reject();
                                return;
                            }
                            if ($.isEmptyObject(opt.selectedRegions) || !opt.selectedRegions.regions) {
                                notification('error', i18n("corrupted-vca-configuration"));
                                reports.isSuccessReport(false);
                                dfd.reject();
                                return;
                            }
                        
                            reports.isSuccessReport(true);
                            generateSVGImage();
                            dfd.resolve();
                        });
                }, 500);
                return dfd.promise;
            }
        });

angular
    .module('kai.reports.traffic')
    .controller('TrafficController',
        function(
            KupOption,
            RouterStateService, UtilsService, PromiseFactory, AuthTokenFactory,
            ReportsService, TrafficService, TrafficSvgService,
            $scope, $timeout, $q, $rootScope
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var reports = ReportsService;
            var kaiFlow = TrafficSvgService;
            var trafficCtrl = this;

            //UI controller
            trafficCtrl.data = TrafficService.data;
            trafficCtrl.fn = {
                setTab: setTab,
                exportPdf: exportPdf,
                exportCsv: exportCsv,
                showRegion: showRegion
            };

            trafficCtrl.tab = {};
            trafficCtrl.tab.data = [{
                name: 'chart',
                text: 'charts',
                isActive: true,
            }];

            trafficCtrl.export = {};
            trafficCtrl.export.data = {
                isShow: false,
            };;


            trafficCtrl.noData = {};
            trafficCtrl.noData.data = {
                isShow: true,
            };

            trafficCtrl.region = {};
            trafficCtrl.region.data = {
                isShow: false,
            }

            init();

            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function init() {
                //watch reportsServer data 
                $scope.$watch(function() {
                    return angular.toJson(reports.data);
                }, function(newVal, oldVal) {
                    var reportsOpt = angular.fromJson(newVal);
                    trafficCtrl.noData.data.isShow = !reportsOpt.isSuccessReport;
                }, true);

            }

            function setTab(tabName) {
                var opt = trafficCtrl.data;
                var tabData = trafficCtrl.tab.data;
                $.each(tabData, function(i, data) {
                    data.isActive = (tabName === data.name) ? true : false;
                });
                opt.currentTab = tabName;
            }

            function exportCsv(periodType) {
                if (!reports.isSuccessReport()) {
                    notification('error', i18n('please-generate-reports'));
                    return false;
                }
                if (!reports.isSingleCamera()) {
                    notification('error', i18n("multiple-device-channel-not-supported"));
                    return false;
                }
                var warningNotify = notification('warning', i18n('exporting-to-csv'), 0);
                TrafficService.exportSecurityCrossSiteCSVApi(periodType)
                    .finally(function() {
                        warningNotify.close();
                    });
            }

            function exportPdf() {
                if (!reports.isSuccessReport()) {
                    notification('error', i18n('please-generate-reports'));
                    return false;
                }
                if (!reports.isSingleCamera()) {
                    notification('error', i18n("multiple-device-channel-not-supported"));
                    return false;
                }
                var warningNotify = notification('warning', i18n('exporting-to-pdf'), 0);
                TrafficService.exportTrafficFlowPdfApi()
                    .finally(function() {
                        warningNotify.close();
                    });
            }

            function showRegion(isShow) {
                trafficCtrl.region.data.isShow = isShow;
                kaiFlow.toggleRegions();
            }
        });