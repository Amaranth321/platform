angular.module('kai.reports.crowd', [
	'ui.amcharts',
]);
angular
    .module('kai.reports.crowd')
    .factory("CrowdMapManagerService",
        function(UtilsService) {
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var mapManager = {};
            mapManager.map = null;
            mapManager.mapWidth = null;
            mapManager.mapHeight = null;
            mapManager.backgroundImage = null;
            mapManager.drawnItems = null;
            mapManager.bgImageLoaded = false;

            mapManager.initialize = function(divId, zoomLvl, zoomControl, canPan) {

                //default map is disabled
                //mapManager.map = null;
                mapManager.map = new L.Map(divId, {
                    layers: [new L.TileLayer('', {
                        maxZoom: 18
                    })],
                    center: new L.LatLng(0, 0),
                    zoom: zoomLvl,
                    zoomControl: zoomControl,
                    dragging: canPan
                });

                mapManager.mapWidth = $("#" + divId).width();
                mapManager.mapHeight = $("#" + divId).height();
            }

            mapManager.addImageOverlay = function(imageUrl) {
                mapManager.removeImageOverlay();

                var bgImage = L.imageOverlay(imageUrl, mapManager.map.getBounds());
                mapManager.backgroundImage = bgImage;
                bgImage.addTo(mapManager.map);
                bgImage.bringToBack();
            }

            mapManager.setBackgroundImage = function(url) {
                if (url == null || url.length == 0) {
                    return;
                }
                mapManager.addImageOverlay(url);
                mapManager.bgImageLoaded = true;
            }

            mapManager.setEmptyBackground = function() {
                mapManager.removeImageOverlay();
                var bgImage = L.imageOverlay("/public/css/common/images/empty_background.jpg", mapManager.map.getBounds());
                mapManager.backgroundImage = bgImage;
                bgImage.addTo(mapManager.map);
                bgImage.bringToBack();
            }

            mapManager.setLiveSnapshotBackground = function(coreDeviceId, channelId) {
                mapManager.bgImageLoaded = false;

                utils.showLoadingOverlay();
                vca.getCameraSnapshot(coreDeviceId, channelId, function(jpegUrl) {
                    if (jpegUrl == null) {
                        utils.popupAlert(i18n('error-loading-image'));
                        mapManager.setEmptyBackground();
                    } else {
                        mapManager.setBackgroundImage(jpegUrl);
                    }

                    utils.hideLoadingOverlay();
                });
            }

            mapManager.removeImageOverlay = function() {
                if (mapManager.backgroundImage) {
                    mapManager.map.removeLayer(mapManager.backgroundImage);
                    mapManager.backgroundImage = null;
                }
            }

            mapManager.initializeDrawingTools = function(marker, polyline, polygon, circle, rectangle, showEditOptions) {

                mapManager.drawnItems = new L.FeatureGroup();
                mapManager.map.addLayer(mapManager.drawnItems);

                var editOptions = {
                    featureGroup: mapManager.drawnItems,
                    edit: {
                        selectedPathOptions: {
                            stroke: true,
                            color: 'red',
                            weight: 2,
                            opacity: 0.8
                        }
                    }
                };
                if (showEditOptions) {
                    var drawControl = new L.Control.Draw({
                        position: 'topleft',
                        draw: {
                            marker: marker,
                            polyline: polyline,
                            polygon: polygon,
                            circle: circle,
                            rectangle: rectangle
                        },
                        //        edit: false
                        edit: editOptions
                    });

                    mapManager.map.addControl(drawControl);
                    mapManager.customizeDeleteFunction();
                }

                //    utils.createTooltip("leaflet-draw-draw-polyline", "right", i18n('draw'));
                //    utils.createTooltip("leaflet-draw-draw-rectangle", "right", i18n('draw'));
                //    utils.createTooltip("leaflet-draw-edit-edit", "right", i18n('edit'));
                //    utils.createTooltip("leaflet-draw-edit-remove", "right", i18n('delete'));
            }

            mapManager.removeDrawnItems = function() {
                if (mapManager.drawnItems != null) {
                    mapManager.drawnItems.eachLayer(function(layer) {
                        if (layer.arrows != null) {
                            mapManager.map.removeLayer(layer.arrows);
                        }
                        if (layer.label != null) {
                            mapManager.map.removeLayer(layer.label);
                        }

                        mapManager.map.removeLayer(layer);
                        mapManager.drawnItems.removeLayer(layer);
                    });
                }

                if (typeof peopleCounting !== 'undefined' && peopleCounting != null) {
                    peopleCounting.mapLayersRemoved();
                }

                if (typeof trafficflow !== 'undefined' && trafficflow != null) {
                    trafficflow.mapLayersRemoved();
                }

                if (typeof crowddensity !== 'undefined' && crowddensity != null) {
                    crowddensity.mapLayersRemoved();
                }
            }

            mapManager.removeDrawnLayerByLayerName = function(layerName) {
                mapManager.drawnItems.eachLayer(function(layer) {
                    if (layer.name == layerName) {
                        if (layer.arrows != null) {
                            mapManager.map.removeLayer(layer.arrows);
                        }
                        if (layer.label != null) {
                            mapManager.map.removeLayer(layer.label);
                        }

                        mapManager.map.removeLayer(layer);
                        mapManager.drawnItems.removeLayer(layer);
                    }
                });
                if (typeof crowddensity !== 'undefined' && crowddensity != null) {
                    crowddensity.mapLayersRemoved();
                }
            }

            mapManager.removeDrawnLayerByLayer = function(removeLayer) {
                mapManager.drawnItems.eachLayer(function(layer) {
                    if (layer == removeLayer) {
                        if (layer.arrows != null) {
                            mapManager.map.removeLayer(layer.arrows);
                        }
                        if (layer.label != null) {
                            mapManager.map.removeLayer(layer.label);
                        }

                        mapManager.map.removeLayer(layer);
                        mapManager.drawnItems.removeLayer(layer);
                    }
                });
                if (typeof crowddensity !== 'undefined' && crowddensity != null) {
                    crowddensity.mapLayersRemoved();
                }
            }

            mapManager.customizeDeleteFunction = function() {
                $(".leaflet-draw-edit-remove").hide();
                $(".leaflet-draw-edit-edit").attr("title", i18n('edit'));
                $(".leaflet-draw-edit-edit").parent().append(
                    '<a class="leaflet-draw-edit-remove" ' +
                    'href="javascript:mapManager.removeDrawnItems()" ' +
                    'title="' + i18n('delete-all') + '">' +
                    '</a>'
                );
            }

            mapManager.getEstimatedCenter = function(layer) {
                var bounds = layer.getBounds();
                var pSW = mapManager.map.latLngToContainerPoint(bounds.getSouthWest());
                var pNE = mapManager.map.latLngToContainerPoint(bounds.getNorthEast());

                var centX = ((pNE.x - pSW.x) / 2) + pSW.x;
                var centY = ((pSW.y - pNE.y) / 2) + pNE.y;
                var centLatLng = mapManager.map.containerPointToLatLng(new L.Point(centX, centY));

                return centLatLng;
            }

            mapManager.enableDrawingTools = function(flag) {
                if (mapManager != null) {
                    if (flag) {
                        $(".leaflet-control").show("slide", {
                            direction: "left"
                        });
                    } else {
                        $(".leaflet-control").hide("slide", {
                            direction: "left"
                        });
                    }
                }
            }

            mapManager.addLabel = function(text, latLng, className) {
                if (text == null) {
                    return;
                }

                //calculate label dimensions
                var horizontalPadding = 5;
                var charWidth = 8;
                var charHeight = 19; // line-height
                var totalWidth = (charWidth * text.length) + (2 * horizontalPadding);

                return L.marker(latLng, {
                    icon: L.divIcon({
                        className: className,
                        iconSize: [totalWidth, charHeight],
                        html: text
                    }),
                    draggable: false,
                    zIndexOffset: 1000
                }).addTo(mapManager.map);
            }

            mapManager.bgImageExists = function() {
                return mapManager.bgImageLoaded;
            }

            return mapManager;
        }
    );

angular
    .module('kai.reports.crowd')
    .factory("CrowdSvgService",
        function(UtilsService, CrowdDensityService, CrowdMapManagerService) {
            var crowddensity = CrowdDensityService;
            var mapManager = CrowdMapManagerService;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            return {
                showCrowdHeatmap: showCrowdHeatmap,
                drawRegions: drawRegions,
                showHeatmapLegend: showHeatmapLegend,
                toggleRegions: toggleRegions
            };
            crowddensity.hide = 1;

            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
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

            function toggleRegions() {
                $(".leaflet-zoom-animated").toggle();
            }
        }
    );

angular
    .module('kai.reports.crowd')
    .factory("CrowdService",
        function(KupOption, AmchartsTheme, UtilsService, PromiseFactory, KupApiService, ReportsService, AuthTokenFactory, CrowdSvgService, $q, $timeout) {

            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var ajaxPost = KupApiService.ajaxPost;
            var reports = ReportsService;

            var data = {
                //time format 
                timeFormat0: 'yyyy/MM/dd HH:mm:ss',
                timeFormat1: 'dd/MM/yyyy HH:mm:ss',
                timeFormat2: 'ddMMyyyyHHmmss',


                //selected data
                selectedGridList: {},
                selectedSitenameList: [],
                selectedVcaData: {},

                //selected chart data
                selectedChartDataSource: [],
                selectedChartDataSourceByTimeunit: [],
                selectedChartGraphs: [],
                selectedChartSortByDate: [],

                //from api reponse data
                apiAnalyticsReportList: [],
                apiLiveVideoUrl: [],

                //current info
                currentTab: 'chart', //value is uiTab name
                currentTimeunitForChart: 'hours', //value is uiTimeunit name

                //request List
                requestForReport: [],

                //UI selector
                $report: '#crowdReport',
                $chart: '#crowdChart',
                $chartForPdf: '#crowdChartForPdf',
                $heatmap: '#heatmapSnapshot',

                //UI setting
                uiTab: [{
                    name: 'chart',
                    text: 'charts',
                    isActive: true,
                }],
                uiTimeunit: [{
                    name: 'hours',
                    text: 'hourly',
                    isActiveForChart: true,
                    isShowForChart: true,
                    chartPeriod: 'hh',
                }, {
                    name: 'days',
                    text: 'daily',
                    isActiveForChart: false,
                    isShowForChart: true,
                    chartPeriod: 'DD',
                }, {
                    name: 'weeks',
                    text: 'weekly',
                    isActiveForChart: false,
                    isShowForChart: true,
                    chartPeriod: 'WW',
                }, {
                    name: 'months',
                    text: 'monthly',
                    isActiveForChart: false,
                    isShowForChart: true,
                    chartPeriod: 'MM',
                }, {
                    name: 'years',
                    text: 'yearly',
                    isActiveForChart: false,
                    isShowForChart: false,
                    chartPeriod: 'YYYY',
                }],

                uiExport: {
                    isOpen: false,
                },

                uiNodata: {
                    isShow: true,
                },
                uiChart: {
                    options: {},
                },
                uiChartForPdf: {
                    width: '900px',
                    height: '400px',
                    options: {},
                }
            };
            return {
                data: data,

                setInitData: setInitData,
                setInitTab: setInitTab,
                setInitTimeunit: setInitTimeunit,

                setSelectedChartData: setSelectedChartData,
                setChartTimeunitLabel: setChartTimeunitLabel,
                setChartTheme: setChartTheme,

                setChartData: setChartData,
                setChartOptions: setChartOptions,
                setChartForPdfOptions: setChartForPdfOptions,

                listRunningAnalyticsApi: listRunningAnalyticsApi,
                getAnalyticsReportApi: getAnalyticsReportApi,
                getLiveVideoUrlApi: getLiveVideoUrlApi,
                exportAggregatedCsvReportApi: exportAggregatedCsvReportApi,
                exportCrowdDensityPdfApi: exportCrowdDensityPdfApi,

                generateReport: generateReport,
                generateChart: generateChart,
                generateUiReport: generateUiReport,
            };
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function setInitData() {
                data.currentTab = 'chart';
                data.currentTimeunitForChart = 'hours';
                data.uiNodata.isShow = true;

                setInitTab();
                setInitTimeunit();

            }

            function setInitTab() {
                //default least tab to active 
                $.each(data.uiTab, function(i, tab) {
                    if (i == 0) {
                        tab.isActive = true;
                    } else {
                        tab.isActive = false;
                    }
                });
            }

            function setInitTimeunit() {
                //default least unit to active
                $.each(data.uiTimeunit, function(i, unit) {
                    if (i == 0) {
                        unit.isActiveForChart = true;
                    } else {
                        unit.isActiveForChart = false;
                    }
                });
            }

            function setSelectedChartData() {
                var opt = data;
                var reportsOpt = reports.data;
                var events = opt.apiAnalyticsReportList;
                var vcaList = JSON.parse(opt.selectedVcaData.thresholds || '{}').regions || [];
                var seriesInfo = [];
                var countList = [];
                var countTimeList = {};

                var width = jQuery(opt.$heatmap).width();
                var height = jQuery(opt.$heatmap).height();
                var heatmapColumns = events.length ? events[0].columns : 0;
                var heatmapRows = events.length ? events[0].rows : 0;
                var colWidth = width / heatmapColumns || 0;
                var rowHeight = height / heatmapRows || 0;

                //set data
                (function() {
                    $.each(vcaList, function(i, region) {
                        //var name = fieldName + i;
                        var name = region.name;
                        var vertexX = [];
                        var vertexY = [];
                        var vertexN = region.points.length;
                        var getTotalValues = function(trackData) {
                            var totalValues = 0;
                            $.each(trackData, function(i, d) {
                                var trackX = d.x * colWidth;
                                var trackY = d.y * rowHeight;
                                var i, j, c = 0;
                                for (i = 0, j = vertexN - 1; i < vertexN; j = i++) {
                                    if (((vertexY[i] > trackY) != (vertexY[j] > trackY)) &&
                                        (trackX < (vertexX[j] - vertexX[i]) * (trackY - vertexY[i]) / (vertexY[j] - vertexY[i]) + vertexX[i]))
                                        c = !c;
                                }
                                if (c) {
                                    totalValues += d.value;
                                }

                            });
                            return totalValues;
                        };

                        seriesInfo.push({
                            "id": "g" + i,
                            "valueAxis": "v" + i,
                            "valueField": "count" + i,
                            "type": "column",
                            //"lineColor": data.chartLineColors[count],
                            //"bullet": "round",
                            //"bulletBorderThickness": 1,
                            //"hideBulletsCount": 30,
                            "title": name,
                            "fillAlphas": 1,
                            "lineThickness": 2,
                            "balloonText": name + " :<b>[[value]]</b>",
                        });
                        $.each(region.points, function(j, coordinates) {
                            vertexX.push(coordinates.x * width);
                            vertexY.push(coordinates.y * height);
                        });
                        $.each(events, function(j, evt) {
                            var totalValues = getTotalValues(evt.tracks);
                            var countItem = {};
                            countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                            countItem["count" + i] = isNaN(totalValues) ? 0 : Math.floor(totalValues);
                            countList.push(countItem);
                        });
                    });
                })();

                // update data and sort
                (function() {
                    $.each(countList, function(i, cl) {
                        var timeIndex = kendo.toString(cl.time, data.timeFormat2);
                        if (countTimeList[timeIndex]) {
                            countTimeList[timeIndex] = $.extend(true, countTimeList[timeIndex], cl);
                        } else {
                            countTimeList[timeIndex] = cl;
                        }
                    })

                    countList = [];
                    $.each(countTimeList, function(i, ctl) {
                        countList.push(ctl);
                    })

                    $.each(countList, function(i, cl) {
                        $.each(vcaList, function(j, vl) {
                            if (!cl['count' + j]) {
                                cl['count' + j] = 0;
                            }
                        });
                    });

                    countList.sort(function(a, b) {
                        return a.time - b.time;
                    });
                })();

                opt.selectedChartDataSource = countList;
                opt.selectedChartGraphs = seriesInfo;
            }

            function setChartData() {
                var timeunit = data.currentTimeunitForChart;
                var dataSource = angular.copy(data.selectedChartDataSource);
                var selectedItemLength = reports.data.selectedSaveDataList.length;
                var dataTimeList = {};
                var dataSourceByTimeunit = [];

                //set dataSourceByTimeunit
                (function() {

                    var tmp = {};
                    for (var i = 0; i <= selectedItemLength; i++) {
                        tmp['count' + i] = 0;
                    }

                    if (timeunit == 'hours') {
                        var start = moment(reports.data.dateRange.startDate);
                        var end = moment(reports.data.dateRange.endDate);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "hours")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = i.format();
                        }
                        $.each(dataSource, function(index, ds) {
                            var index = moment(ds.time).diff(start, 'hours');
                            countList[index] = ds;
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'days') {
                        var start = moment(reports.data.dateRange.startDate);
                        var end = moment(reports.data.dateRange.endDate);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "days")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = new Date(i);
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'days');
                            for (var j = 0; j <= selectedItemLength; j++) {
                                countList[index]["count" + j] += ds["count" + j];
                            }
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'weeks') {
                        var weekStart = kupOpt.dateOfWeekStart;
                        var start = moment(reports.data.dateRange.startDate).isoWeekday(weekStart);
                        var end = moment(reports.data.dateRange.endDate).isoWeekday(weekStart);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "weeks")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = new Date(i);
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'weeks');
                            for (var j = 0; j <= selectedItemLength; j++) {
                                countList[index]["count" + j] += ds["count" + j];
                            }
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'months') {
                        var start = moment(reports.data.dateRange.startDate).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var end = moment(reports.data.dateRange.endDate).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "month")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = i.format();
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'months');
                            for (var j = 0; j <= selectedItemLength; j++) {
                                countList[index]["count" + j] += ds["count" + j];
                            }
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'years') {
                        var start = moment(reports.data.dateRange.startDate).month(0).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var end = moment(reports.data.dateRange.endDate).month(0).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "years")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = i.format();
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'year');
                            for (var j = 0; j <= selectedItemLength; j++) {
                                countList[index]["count" + j] += ds["count" + j];
                            }
                        });
                        dataSourceByTimeunit = countList;
                    }
                })();

                data.selectedChartDataSourceByTimeunit = dataSourceByTimeunit;
                return dataSourceByTimeunit;
            }

            function setChartOptions() {
                var chartSetting = (function() {
                    var setting = {};
                    $.each(data.uiTimeunit, function(i, unit) {
                        if (unit.name === data.currentTimeunitForChart) {
                            setting.minPeriod = unit.chartPeriod;
                            //setting.categoryBalloonDateFormat = unit.chartCategoryBalloonDateFormat;
                            return false;
                        }
                    });
                    return setting;
                })();

                var chartOptions = {
                    //data setting
                    dataProvider: angular.copy(data.selectedChartDataSourceByTimeunit),
                    graphs: angular.copy(data.selectedChartGraphs),

                    //config setting
                    type: "serial",
                    theme: AuthTokenFactory.getTheme(),
                    //fontFamily: 'Verdana',
                    //fontSize: 11,
                    zoomOutText: i18n('show-all'),
                    valueAxes: [{
                        "axisColor": "chartCursor",
                        "axisThickness": 2,
                        "axisAlpha": 1,
                        "position": "left",
                    }],
                    chartScrollbar: {
                        "autoGridCount": true,
                        "graph": "g0",
                        "scrollbarHeight": 40,
                    },
                    chartCursor: {
                        cursorPosition: "mouse",
                        categoryBalloonFunction: function() {
                            var time = arguments[0];
                            return setChartTimeunitLabel(time);
                        },
                    },
                    categoryField: "time",
                    categoryAxis: {
                        firstDayOfWeek: kupOpt.dateOfWeekStart,
                        gridAlpha: 0,
                        //minorGridEnabled: true,
                        markPeriodChange: true,
                        boldLabels: true,
                        minPeriod: chartSetting.minPeriod,
                        parseDates: true,
                        equalSpacing: true,
                        // dateFormats: [{
                        //     period: 'fff',
                        //     format: 'JJ:NN:SS'
                        // }, {
                        //     period: 'ss',
                        //     format: 'JJ:NN:SS'
                        // }, {
                        //     period: 'mm',
                        //     format: 'JJ:NN'
                        // }, {
                        //     period: 'hh',
                        //     format: 'JJ:NN'
                        // }, {
                        //     period: 'DD',
                        //     format: 'MMM DD'
                        // }, {
                        //     period: 'WW',
                        //     format: 'MMM DD'
                        // }, {
                        //     period: 'MM',
                        //     format: 'MMM'
                        // }, {
                        //     period: 'YYYY',
                        //     format: 'YYYY'
                        // }],
                        // categoryFunction: function(date) {
                        //     return arguments[1].time;
                        // },
                        labelFunction: function(dateText) {
                            //console.info(arguments);
                            var time = arguments[1];
                            switch (chartSetting.minPeriod) {
                                case "hh":
                                    dateText = moment(time).format(kupOpt.default.formatAxisHourly);
                                    break;
                                case "DD":
                                    dateText = moment(time).format(kupOpt.default.formatAxisDaily);
                                    break;
                                case "WW":
                                    dateText = moment(time).startOf('week').isoWeekday(kupOpt.default.isoWeekStart).format(kupOpt.default.formatAxisWeekly) + " - \n" + moment(time).endOf('week').format(kupOpt.default.formatAxisWeekly);
                                    break;
                                case "MM":
                                    dateText = moment(time).format(kupOpt.default.formatAxisMonthly);
                                    break;
                            }

                            // if (chartSetting.minPeriod == 'WW' && arguments[3] == 'WW') {
                            //     // var getTimeStart = (time.getDay() < weekStart) ?
                            //     //     new Date(time.getFullYear(), time.getMonth(), time.getDate() - (7 - weekStart - time.getDay())) :
                            //     //     new Date(time.getFullYear(), time.getMonth(), time.getDate() - (time.getDay() - weekStart));
                            //     // var getTimeEnd = (time.getDay() < weekStart) ?
                            //     //     new Date(time.getFullYear(), time.getMonth(), time.getDate() + (weekStart - time.getDay() - 1)) :
                            //     //     new Date(time.getFullYear(), time.getMonth(), time.getDate() + (6 - (time.getDay() - weekStart)));
                            //     // dateText = getTimeStart.toLocaleDateString("en-us", {
                            //     //     month: "short",
                            //     //     day: 'numeric'
                            //     // }) + "- \n" + getTimeEnd.toLocaleDateString("en-us", {
                            //     //     month: "short",
                            //     //     day: 'numeric'
                            //     // });
                            //     dateText = moment(time).startOf('week').isoWeekday(kupOpt.default.isoWeekStart).format(kupOpt.default.formatWeekly) + " - \n" + moment(time).endOf('week').format(kupOpt.default.formatWeekly);
                            // }else if(chartSetting.minPeriod == 'MM') {
                            //     dateText = moment(time).format(kupOpt.default.formatMonthly);
                            // }

                            return dateText;
                        },
                    },
                    legend: {
                        useGraphSettings: false,
                        valueWidth: 0,
                        valueFunction: function() {
                            return "";
                        }
                    },
                };

                data.uiChart.options = chartOptions;
                return chartOptions;
            }

            function setChartForPdfOptions() {
                var chartSetting = (function() {
                    var setting = {};
                    $.each(data.uiTimeunit, function(i, unit) {
                        if (unit.name === data.currentTimeunitForChart) {
                            setting.minPeriod = unit.chartPeriod;
                            //setting.categoryBalloonDateFormat = unit.chartCategoryBalloonDateFormat;
                            return false;
                        }
                    });
                    return setting;
                })();

                var chartPeriod = 'hours';
                var chartOptions = {
                    //data setting
                    dataProvider: angular.copy(data.selectedChartDataSourceByTimeunit),
                    graphs: angular.copy(data.selectedChartGraphs),

                    //config setting
                    type: "serial",
                    theme: 'white',
                    fontFamily: 'Verdana',
                    fontSize: 11,
                    //zoomOutText: i18n('show-all'),
                    valueAxes: [{
                        "axisColor": "chartCursor",
                        "axisThickness": 2,
                        "axisAlpha": 1,
                        "position": "left",
                    }],
                    // chartScrollbar: {
                    //     "autoGridCount": true,
                    //     "graph": "g0",
                    //     "scrollbarHeight": 40,
                    // },
                    chartCursor: {
                        cursorPosition: "mouse",
                        categoryBalloonFunction: function() {
                            var time = arguments[0];
                            return setChartTimeunitLabel(time);
                        },
                    },
                    categoryField: "time",
                    categoryAxis: {
                        firstDayOfWeek: kupOpt.dateOfWeekStart,
                        gridAlpha: 0,
                        //minorGridEnabled: true,
                        markPeriodChange: true,
                        boldLabels: true,
                        minPeriod: chartPeriod,
                        parseDates: true,
                        equalSpacing: true,
                        dateFormats: [{
                            period: 'fff',
                            format: 'JJ:NN:SS'
                        }, {
                            period: 'ss',
                            format: 'JJ:NN:SS'
                        }, {
                            period: 'mm',
                            format: 'JJ:NN'
                        }, {
                            period: 'hh',
                            format: 'JJ:NN'
                        }, {
                            period: 'DD',
                            format: 'MMM DD'
                        }, {
                            period: 'WW',
                            format: 'MMM DD'
                        }, {
                            period: 'MM',
                            format: 'MMM'
                        }, {
                            period: 'YYYY',
                            format: 'YYYY'
                        }],
                        // categoryFunction: function(date) {
                        //     return arguments[1].time;
                        // },
                        labelFunction: function(dateText) {
                            //console.info(arguments);
                            var time = arguments[1];
                            switch (chartSetting.minPeriod) {
                                case "hh":
                                    dateText = moment(time).format(kupOpt.default.formatAxisHourly);
                                    break;
                                case "DD":
                                    dateText = moment(time).format(kupOpt.default.formatAxisDaily);
                                    break;
                                case "WW":
                                    dateText = moment(time).startOf('week').isoWeekday(kupOpt.default.isoWeekStart).format(kupOpt.default.formatAxisWeekly) + " - \n" + moment(time).endOf('week').format(kupOpt.default.formatAxisWeekly);
                                    break;
                                case "MM":
                                    dateText = moment(time).format(kupOpt.default.formatAxisMonthly);
                                    break;
                            }
                            // var weekStart = kupOpt.dateOfWeekStart;
                            // if (chartSetting.minPeriod == 'WW' && arguments[3] == 'WW') {
                            //     var getTimeStart = (time.getDay() < weekStart) ?
                            //         new Date(time.getFullYear(), time.getMonth(), time.getDate() - (7 - weekStart - time.getDay())) :
                            //         new Date(time.getFullYear(), time.getMonth(), time.getDate() - (time.getDay() - weekStart));
                            //     var getTimeEnd = (time.getDay() < weekStart) ?
                            //         new Date(time.getFullYear(), time.getMonth(), time.getDate() + (weekStart - time.getDay() - 1)) :
                            //         new Date(time.getFullYear(), time.getMonth(), time.getDate() + (6 - (time.getDay() - weekStart)));
                            //     dateText = getTimeStart.toLocaleDateString("en-us", {
                            //         month: "short",
                            //         day: 'numeric'
                            //     }) + "- \n" + getTimeEnd.toLocaleDateString("en-us", {
                            //         month: "short",
                            //         day: 'numeric'
                            //     });
                            // }
                            return dateText;
                        },
                    },
                    legend: {
                        useGraphSettings: false,
                        valueWidth: 0,
                        valueFunction: function() {
                            return "";
                        }
                    },
                };

                data.uiChartForPdf.options = chartOptions;
                return chartOptions;
            }

            function getAnalyticsReportApi(isDefer) {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;

                var instance = reportsOpt.selectedInstance[0];
                var selectedDeviceId = instance.platformDeviceId;
                var selectedChannelId = instance.channelId;
                var fromDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.startDate), data.timeFormat2);
                var toDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.endDate), data.timeFormat2);
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

                    // if (!$.isEmptyObject(vcaData)) {
                    //     regions = JSON.parse(vcaData.thresholds);
                    // }

                    opt.apiRunningAnalyticsList = response.instances || [];
                    opt.selectedVcaData = vcaData;
                    //opt.selectedRegions = regions;
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

            function exportAggregatedCsvReportApi(periodType) {
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

            function exportCrowdDensityPdfApi() {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;
                var dvcId = reportsOpt.selectedInstance;
                var fromDateUTC = kendo.toString(reportsOpt.dateRange.startDate, opt.timeFormat1);
                var toDateUTC = kendo.toString(reportsOpt.dateRange.endDate, opt.timeFormat1);

                var base64Image = (function() {
                    var canvas = $(opt.$heatmap).find("canvas").get(0);
                    var canvasData = canvas.toDataURL();
                    return canvasData.substring(canvasData.indexOf(",") + 1).trim();
                })();

                var base64RegionImage = (function() {
                    var svgList = $(".leaflet-map-pane").find("svg");
                    var xmlSerializer = new XMLSerializer;
                    var svg = xmlSerializer.serializeToString(svgList[0]);
                    var encodedData = window.btoa(svg);
                    return encodedData;
                })();

                var chartSvgTitle = "<svg version='1.1' width='900' height='30'><text y='15' x='0' transform='translate(450)' text-anchor='middle' font-size='20' font-weight='bold'>" + i18n('activity-level-zone') + "</text></svg>";
                var chartOpt = setChartForPdfOptions();
                var chartPdf = window.AmCharts.makeChart(opt.$chartForPdf.slice(1), chartOpt);
                var svgData = (function() {
                    var svg = '';
                    var svgData = [];
                    var chartInfo = {};
                    if (opt.currentTab == 'chart') {
                        svgData.push(chartSvgTitle);
                        $.each($(opt.$chartForPdf).find('svg'), function(i, el) {
                            svgData.push("<svg version='1.1' width='" + $(el).css('width') + "' height='" + $(el).css('height') + "'>" + $(el).html() + "</svg>");
                        });
                    }
                    return svgData;
                })();

                var reportInfo = {
                    "event-type": vcaEventType,
                    "device-name": dvcId[0].deviceName,
                    "channel": dvcId[0].channelName,
                    "from": fromDateUTC,
                    "to": toDateUTC
                };

                var param = {
                    "device-id": dvcId[0].deviceId,
                    "channel-id": dvcId[0].channelId,
                    "time-zone-offset": KupApiService.data.timeZoneOffset,
                    "base64-image": base64Image,
                    "base64-region-image": base64RegionImage,
                    "svg-image": svgData.join(""),
                    "report-info": angular.toJson(reportInfo)
                };
                return KupApiService.exportDoc(param, 'exportcrowddensitypdf');
            }

            function setChartTimeunitLabel(dateFormat) {
                var dateText = '';
                var time = new Date(dateFormat);
                var weekStart = kupOpt.dateOfWeekStart;
                var dateLang = "en-us";
                var dateOpt = {
                    weekday: "short",
                    month: "short",
                    day: 'numeric',
                    year: "numeric"
                };
                if (data.currentTimeunitForChart == 'hours') {
                    // dateText = time.toLocaleDateString(dateLang, dateOpt) + ", " + time.getHours() + ":00";
                    dateText = moment(time).format(kupOpt.default.formatHourly);
                }
                if (data.currentTimeunitForChart == 'days') {
                    // dateText = time.toLocaleDateString(dateLang, dateOpt);
                    dateText = moment(time).format(kupOpt.default.formatDaily);
                }
                if (data.currentTimeunitForChart == 'weeks') {
                    // var getTimeStart = (time.getDay() < weekStart) ?
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() - (7 - weekStart - time.getDay())) :
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() - (time.getDay() - weekStart));
                    // var getTimeEnd = (time.getDay() < weekStart) ?
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() + (weekStart - time.getDay() - 1)) :
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() + (6 - (time.getDay() - weekStart)));
                    // // dateText = getTimeStart.toLocaleDateString(dateLang, dateOpt) + " - " + getTimeEnd.toLocaleDateString(dateLang, dateOpt);
                    // dateText = moment(getTimeStart).format(kupOpt.default.formatWeekly) + " - " + moment(getTimeEnd).format(kupOpt.default.formatWeekly);
                    dateText = moment(time).startOf('week').isoWeekday(kupOpt.default.isoWeekStart).format(kupOpt.default.formatWeekly) + " - \n" + moment(time).endOf('week').format(kupOpt.default.formatWeekly);
                }
                if (data.currentTimeunitForChart == 'months') {
                    // dateOpt = {
                    //     month: "short",
                    //     year: "numeric"
                    // };
                    // dateText = time.toLocaleDateString(dateLang, dateOpt);
                    dateText = moment(time).format(kupOpt.default.formatMonthly);
                }
                if (data.currentTimeunitForChart == 'years') {
                    dateOpt = {
                        year: "numeric"
                    };
                    dateText = time.toLocaleDateString(dateLang, dateOpt);
                }
                return dateText;
            }

            function setChartTheme() {
                var theme = AuthTokenFactory.getTheme();
                window.AmCharts.themes[theme] = AmchartsTheme[theme];
            }

            function generateChart() {
                setChartTheme();
                setChartData();
                setChartOptions();
            }

            function generateHeatmap() {
                var opt = data;
                var reportsOpt = reports.data;
                var events = opt.apiAnalyticsReportList;
                var vcaList = JSON.parse(opt.selectedVcaData.thresholds || '{}').regions || [];

                var max = 1;
                var width = jQuery(opt.$heatmap).width();
                var height = jQuery(opt.$heatmap).height();
                var heatmapColumns = events[0].columns;
                var heatmapRows = events[0].rows;
                var colWidth = width / heatmapColumns || 0;
                var rowHeight = height / heatmapRows || 0;
                var radius = Math.max(colWidth, rowHeight) * 1.2;
                var heatmap = jQuery(opt.$heatmap).empty() && window.h337.create({
                    container: jQuery(opt.$heatmap).get(0),
                    maxOpacity: .5,
                    radius: radius,
                    blur: .75,
                    // update the legend whenever there's an extrema change
                    onExtremaChange: function onExtremaChange(data) {}
                });
                var gridPoints = [];
                var d = [];
                for (var i = 0; i < heatmapColumns; i++) {
                    gridPoints[i] = Array.apply(null, new Array(heatmapRows)).map(Number.prototype.valueOf, 0);
                }
                $.each(events, function(i, d) {
                    var tracks = d.tracks;
                    $.each(tracks, function(i, t) {
                        //hit
                        gridPoints[t.x][t.y] += t.value;
                        max = Math.max(max, gridPoints[t.x][t.y]);
                    });
                });

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

                CrowdSvgService.showHeatmapLegend(100);
                CrowdSvgService.drawRegions(opt.$heatmap.substring(1), vcaList);
                $(opt.$heatmap).css("background-image", "url(" + opt.apiLiveVideoUrl[0] + ")");
            }

            function generateUiReport() {
                generateHeatmap();
                generateChart();
            }

            function generateReport() {
                setInitData();
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
                    getAnalyticsReportApi(true),
                    getLiveVideoUrlApi(true),
                    listRunningAnalyticsApi(true),
                ];

                var dfd = $q.defer();
                $timeout(function() {
                    $q.all(opt.requestForReport)
                        .finally(function() {
                            setSelectedChartData();
                            if (opt.selectedChartDataSource.length <= 0) {
                                reports.isSuccessReport(false);
                                dfd.reject();
                                return;
                            }
                            reports.isSuccessReport(true);
                            dfd.resolve();
                        });
                }, 500);
                return dfd.promise;
            }
        });

angular
    .module('kai.reports.crowd')
    .factory("CrowdDensityService",
        function(UtilsService,CrowdMapManagerService) {           
            var mapManager = CrowdMapManagerService;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var crowddensity = {};

            crowddensity.areaLimit = 10;
            crowddensity.START_NUMBER = 1;
            crowddensity.polygonStyle = null;
            crowddensity.$regionList = null;
            crowddensity.recentRegionName = null;
            crowddensity.recentRegion = null;
            crowddensity.regionNames = [];

            crowddensity.initDrawingCanvas = function(canvasId, regionListDivId, options) {

                if (!utils.isNullOrEmpty(regionListDivId))
                    crowddensity.$regionList = $("#" + regionListDivId);

                mapManager.initialize(canvasId, 20, false, false);
                mapManager.map.doubleClickZoom.disable();
                mapManager.map.scrollWheelZoom.disable();
                var polygonOptions = options;
                if (options == null) {
                    polygonOptions = {
                        stroke: true,
                        color: '#ddd',
                        opacity: 0.8,
                        weight: 1,
                        fillColor: '#2E8DEF',
                        fillOpacity: 0.4,
                        clickable: false
                    };
                }
                var polygonOptions = {
                    title: i18n('draw'),
                    allowIntersection: false,
                    shapeOptions: polygonOptions
                }

                crowddensity.polygonStyle = polygonOptions.shapeOptions;
                if (!utils.isNullOrEmpty(regionListDivId)) {
                    mapManager.initializeDrawingTools(false, false, polygonOptions, false, false, true);
                    mapManager.setEmptyBackground();
                    crowddensity._initDrawingEvents();
                } else {
                    mapManager.initializeDrawingTools(false, false, polygonOptions, false, false, false);
                }

            }

            crowddensity._initDrawingEvents = function() {

                mapManager.map.on('draw:created', function(e) {
                    var boxArray = crowddensity.$regionList.data("kendoListView").dataSource.data();
                    if (boxArray.length == crowddensity.areaLimit) {
                        return;
                    }
                    var contentPage = "/vca/crowdregionname";
                    var winTitle = i18n("region-name");
                    var layer = e.layer;
                    mapManager.drawnItems.addLayer(layer);
                    mapManager.drawnItems.addLayer(layer);
                    crowddensity.recentRegion = layer;
                    utils.openPopup(winTitle, contentPage, null, null, true, function() {
                        if (!utils.isNullOrEmpty(crowddensity.recentRegionName)) {
                            var layer = e.layer;
                            mapManager.drawnItems.addLayer(layer);
                            mapManager.drawnItems.addLayer(layer);
                            layer.name = crowddensity.recentRegionName;
                            layer.label = mapManager.addLabel(layer.name, mapManager.getEstimatedCenter(layer), 'polygon_label');

                            crowddensity.$regionList.data("kendoListView").dataSource.read();
                        } else {
                            console.log("Name not assigned for region");
                            mapManager.removeDrawnLayerByLayer(crowddensity.recentRegion);
                        }
                        crowddensity.recentRegion = null;

                    });
                });

                mapManager.map.on('draw:edited', function(e) {
                    var layers = e.layers;
                    crowddensity.$regionList.data("kendoListView").dataSource.read();
                });

                mapManager.map.on('draw:deleted', function(e) {
                    var layers = e.layers;
                    crowddensity.$regionList.data("kendoListView").dataSource.read();
                });

                mapManager.map.on('draw:drawstart', function(e) {
                    var boxArray = crowddensity.$regionList.data("kendoListView").dataSource.data();
                    if (boxArray.length == crowddensity.areaLimit) {
                        utils.popupAlert(i18n("msg-crowd-flow-region-limit"));
                    }
                });

                mapManager.map.on('draw:drawstop', function(e) {
                    mapManager.map.dragging.disable();
                });

            }

            //to send back to backend
            crowddensity.getBoxesDrawn = function() {
                var boxes = [];

                mapManager.drawnItems.eachLayer(function(layer) {
                    var polygonPoints = [];
                    $.each(layer._originalPoints, function(index, point) {
                        //change points to [0,1] domain
                        var nX = (point.x / mapManager.mapWidth);
                        var nY = (point.y / mapManager.mapHeight);

                        //Ensure the points are within the view
                        nX = (nX < 0) ? 0 : nX;
                        nX = (nX > 1) ? 1 : nX;
                        nY = (nY < 0) ? 0 : nY;
                        nY = (nY > 1) ? 1 : nY;

                        polygonPoints.push({
                            x: nX.toFixed(3),
                            y: nY.toFixed(3)
                        });
                    });

                    var box = {
                        "name": layer.name,
                        "points": polygonPoints
                    };

                    boxes.push(box);
                });

                return boxes;
            }

            crowddensity.addExistingAreas = function(areas) {

                if (areas == undefined) {
                    return;
                }

                $.each(areas, function(index, area) {
                    var latLngPoints = [];
                    $.each(area.points, function(f, p) {
                        //change points to domain in map dimensions
                        p.x = p.x * mapManager.mapWidth;
                        p.y = p.y * mapManager.mapHeight;

                        var latLng = mapManager.map.containerPointToLatLng(new L.Point(p.x, p.y));
                        latLngPoints.push(latLng);
                    });

                    var polygon = L.polygon(latLngPoints, crowddensity.polygonStyle);
                    var layer = polygon.addTo(mapManager.map);
                    layer.name = area.name;
                    layer.label = mapManager.addLabel(area.name, mapManager.getEstimatedCenter(layer), 'polygon_label');

                    mapManager.drawnItems.addLayer(layer);
                });
            }

            //to populate primary box dropdown
            crowddensity.getRectangles = function() {

                if (mapManager.drawnItems == null)
                    return [];

                var areaList = [];
                mapManager.drawnItems.eachLayer(function(layer) {
                    var data = {
                        "name": layer.name
                    };
                    areaList.push(data);
                });

                return areaList;
            }

            //to be called by mapManager only
            crowddensity.mapLayersRemoved = function() {
                try {
                    crowddensity.$regionList.data("kendoListView").dataSource.read();
                } catch (e) {
                    console.log(e);
                }
            }

            crowddensity.removeLayer = function(regionName) {
                mapManager.removeDrawnLayerByLayerName(regionName);
            }

            return crowddensity;
        }
    );

angular
    .module('kai.reports.crowd')
    .controller('CrowdController',
        function(
            KupOption,
            RouterStateService, UtilsService, PromiseFactory, AuthTokenFactory,
            ReportsService, CrowdService,
            $scope, $timeout, $q, CrowdSvgService, $rootScope
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var reports = ReportsService;
            var crowdCtrl = this;

            //UI controller
            crowdCtrl.data = CrowdService.data;
            crowdCtrl.fn = {
                setTab: setTab,
                setTimeunitForChart: setTimeunitForChart,
                exportPdf: exportPdf,
                exportCsv: exportCsv,
                showRegion: showRegion,
            };

            crowdCtrl.region = {};
            crowdCtrl.region.data = {
                isShow: true,
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
                    var opt = crowdCtrl.data;
                    var reportsOpt = angular.fromJson(newVal);
                    var generateUiReport = function() {
                        $timeout(function() {
                            if (!$(opt.$report).is(":visible")) {
                                generateUiReport();
                                return false;
                            }
                            CrowdService.generateUiReport();
                        }, 300);
                    };
                    crowdCtrl.data.uiNodata.isShow = !reportsOpt.isSuccessReport;
                    if (!crowdCtrl.data.uiNodata.isShow) {
                        generateUiReport();
                    }
                }, true);

                //watch theme 
                $scope.$watch(function() {
                    return AuthTokenFactory.getTheme();
                }, function(newVal, oldVal) {
                    if (newVal == oldVal) {
                        return false;
                    }
                    CrowdService.setChartTheme();
                    CrowdService.generateChart();
                }, true);
            }

            function setTab(tabName) {
                var opt = crowdCtrl.data;
                var tabData = opt.uiTab;
                $.each(tabData, function(i, data) {
                    data.isActive = (tabName === data.name) ? true : false;
                });
                opt.currentTab = tabName;
            }

            function setTimeunitForChart(unitName) {
                var opt = crowdCtrl.data;
                var timeunitData = opt.uiTimeunit;
                $.each(timeunitData, function(i, data) {
                    data.isActiveForChart = (unitName === data.name) ? true : false;
                });
                opt.currentTimeunitForChart = unitName;
                CrowdService.generateChart();
            }

            function exportCsv(periodType) {
                if (!reports.isSuccessReport()) {
                    notification('error', i18n('please-generate-reports'));
                    return false;
                }
                // 
                var warningNotify = notification('warning', i18n('exporting-to-csv'), 0);
                CrowdService.exportAggregatedCsvReportApi(periodType)
                    .finally(function() {
                        warningNotify.close();
                    });
            }

            function exportPdf() {
                if (!reports.isSuccessReport()) {
                    notification('error', i18n('please-generate-reports'));
                    return false;
                }
                // 
                var warningNotify = notification('warning', i18n('exporting-to-pdf'), 0);
                CrowdService.exportCrowdDensityPdfApi()
                    .finally(function() {
                        warningNotify.close();
                    });
            }

            function showRegion(isShow) {
                crowdCtrl.region.data.isShow = isShow;
                CrowdSvgService.toggleRegions();
            }
        });
