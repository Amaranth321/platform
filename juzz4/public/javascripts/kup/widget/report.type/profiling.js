KUP.widget.report.type.profiling = (function($, kup) {
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

                $("#donutChartContainer").empty();
                kup.utils.block.popup('#main-charts', i18n('generate-report'));
                window.getAnalyticsReport("", vcaEventType, _kupReport.getDeviceGroups(), selectedDeviceList, "", from, to, {},
                    function(resp) {
                        var opt = _kupReport.getOpt(),
                            selectedInstance = opt.selectedInstance;
                        kup.utils.block.close('#main-charts');
                        console.info("method 'window.getAnalyticsReport' in profiling.js ");
                        console.info(resp);
                        if (resp.result == "ok" && resp.data.length > 0) {
                            opt.isSuccessReport = true;
                            _kupReport.setOpt(opt);
                            _kupReport.updateUI.showDiv();

                            var dbEvents = resp.data;
                            window.vca.reportType = 'mix';
                            window.vca.donutsChartDivs = [];
                            window.vca.areaChartDivs = [];                          
                            window.vca.processProfilingCrossSiteInfo(dbEvents, selectedInstance, fromDate, toDate);
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
            exportCSV: function(data) {
                _kupReport.type.intrusion.exportCSV(data);
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
                var chart1 = 'donutChartContainer',
                    chart2 = 'profilingCrossSiteTabstrip';
                var donuctchartid = $("#" + chart1).children("div").attr("id");
                var areachartid = $("#" + chart2).children("div").children("div").children("div").attr("id");
                var donutSvgArry = [];
                var areaSvgArry = [];
                $.each(vca.donutsChartDivs, function(index, divId) {
                    if(!$("#" + divId).is(":visible")){
                        return true;
                    }
                    var donutsvg = $("#" + divId).data("kendoChart").svg();

                    //Add chart name in svg donut chart
                    //var text = localizeResource("age");
                    // if (donutsvg.indexOf(localizeResource("happy")) >= 0 || donutsvg.indexOf(localizeResource("neutral")) >= 0)
                    //     text = localizeResource("emotion");
                    // else if (donutsvg.indexOf(localizeResource("male")) >= 0 || donutsvg.indexOf(localizeResource("female")) >= 0)
                    //     text = localizeResource("gender");
                    var text = $("#" + divId).parent().find('.overlay').html() || "";
                    var w = donutsvg.indexOf("width") + 7;
                    var h = donutsvg.indexOf("height") + 8;
                    var subWidth = donutsvg.substr(w, 3) / 2;
                    var subHeight = donutsvg.substr(h, 3) / 2 + 25;
                    var subAdd = donutsvg.indexOf("</defs>") + 7;
                    donutsvg = donutsvg.substr(0, subAdd) +
                        '<text x="' + subWidth + '" y="' + subHeight + '" style="font: 18px Arial,Helvetica,sans-serif;" text-anchor="middle" ' +
                        'fill="black" >' + text + '</text>' +
                        donutsvg.substr(subAdd, donutsvg.length);

                    donutSvgArry.push(donutsvg);
                });
                $.each(vca.areaChartDivs, function(index, divId) {
                    if(!$("#" + divId).is(":visible")){
                        return true;
                    }
                    var areasvg = vca.getPrinterFriendlyChart(divId);
                    areaSvgArry.push(areasvg);
                });

                if (donutSvgArry === null || donutSvgArry.length === 0) {
                    return;
                }
                if (areaSvgArry === null || areaSvgArry.length === 0) {
                    return;
                }

                kup.utils.block.popup('#main-charts', i18n('generating-download-file'));
                window.exportProfilingChartReport(donutSvgArry, areaSvgArry, JSON.stringify(vca.currentReportInfo),
                    function(responseData) {
                        kup.utils.block.close('#main-charts');
                        if (responseData !== null && responseData.result === "ok" &&
                            responseData["download-url"] !== null) {
                            window.open(responseData["download-url"], '_blank');
                            window.focus();
                        } else {
                            utils.throwServerError(responseData);
                        }

                        utils.hideLoadingOverlay();
                    },
                    function() {
                        kup.utils.block.close('#main-charts');
                    }
                );

            },
            loadSetUI: function() {
                var i18n = kup.utils.i18n;

                var currentReportInfo = null;
                var tabname = "age";
                vca.currentBrowsingReport = i18n('business-intelligence');
                vca.analyticsType = analyticsType.AUDIENCE_PROFILING;
                //customization for crosssite report profiling page
                $("#crosssite_report_container").append($("#vcaStackChartStepChoices").html());
                $(".vca_chart_header_bar").addClass("profiling_charts_stepbox");

                $("#profilingCrossSiteTabstrip").kendoTabStrip({
                    animation: {
                        open: {
                            effects: "fadeIn"
                        }
                    },
                    select: function(e) {
                        var textName = $(e.item).find(".tabstrip_title_profiling").attr("name");
                        var imgIcon = $(e.item).find(".tabstrip_title_img");
                        var kupapi = kup.getOpt('api');
                        tabname = textName;
                        if (textName == "gender") {
                            vca.currentActiveCrosssiteProfilingTab = i18n('gender');
                            vca.hideProfilingDonutChart("Age", true);
                            vca.hideProfilingDonutChart("Emotion", true);
                            vca.hideProfilingDonutChart("Gender", false);
                        } else if (textName == "emotion") {
                            vca.currentActiveCrosssiteProfilingTab = i18n('emotion');
                            vca.hideProfilingDonutChart("Age", true);
                            vca.hideProfilingDonutChart("Emotion", false);
                            vca.hideProfilingDonutChart("Gender", true);
                        } else if (textName == "age") {
                            vca.currentActiveCrosssiteProfilingTab = i18n('age');
                            vca.hideProfilingDonutChart("Age", false);
                            vca.hideProfilingDonutChart("Emotion", true);
                            vca.hideProfilingDonutChart("Gender", true);
                        }
                        if (textName == "gender" && $("#genderAreaCharts").html() == "") {
                            $("#imgGender").attr("src", kupapi.CdnPath + "/common/images/gender.png");
                            vca.generateGenderAreaCharts("genderAreaCharts");
                            imgIcon.attr("src", kupapi.CdnPath + "/common/images/" + textName + "_color.png");
                        } else if (textName == "emotion" && $("#emotionAreaCharts").html() == "") {
                            $("#imgEmotion").attr("src", kupapi.CdnPath + "/common/images/emotion.png");
                            vca.generateEmotionAreaCharts("emotionAreaCharts");
                            imgIcon.attr("src", kupapi.CdnPath + "/common/images/" + textName + "_color.png");
                        } else if (textName == "age" && $("#ageAreaCharts").html() == "") {
                            $("#imgAge").attr("src", kupapi.CdnPath + "/common/images/age.png");
                            vca.generateAgeAreaCharts("ageAreaCharts");
                            imgIcon.attr("src", kupapi.CdnPath + "/common/images/" + textName + "_color.png");
                        }
                    }
                });
            },
            loadUpdateUI: function() {
            	_kupReport.type.peoplecounting.loadUpdateUI();
            }
        };
    return _self;
})(jQuery, KUP);