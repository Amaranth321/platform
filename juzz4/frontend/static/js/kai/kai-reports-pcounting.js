angular.module('kai.reports.pcounting', [
	'ui.amcharts',
]);
angular.module('kai.reports.pcounting')
    .directive('timecard', function(UtilsService, $window, $document) {
        var localizeResource = UtilsService.i18n;
        var notification = UtilsService.notification;
        var d3 = $window.d3;
        var tip = d3.tip()
            .attr("class", "detailed-stat-box")
            .html(function(data) {
                var detailStatBox =
                    //"<div class='detailed-stat-box'>" +
                    "   <label>" + localizeResource("no-people-in") + "</label>" +
                    "   <span class='people-in'>" + data.count + "</span><br/>";
                //if(timeCard.isPosData) {
                var conversionRate = (data.receipts / data.count) * 100;
                //$(".detailed-stat-box .people-in").html(data.count);
                //$(".detailed-stat-box .conversion-rate").html(conversionRate.toFixed(1) + "%");
                //$(".detailed-stat-box .receipt").html(data.receipts);
                //$(".detailed-stat-box .sales-amount").html((data.sales / 1000).toFixed(1) + "K");
                detailStatBox +=
                    "   <label>" + localizeResource("conversion-rate") + "</label>" +
                    "   <span class='conversion-rate'> " + conversionRate.toFixed(1) + "%" + " </span><br/>" +
                    "   <label>" + localizeResource("receipt") + "</label>" +
                    "   <span class='receipt'> " + data.receipts.toFixed() + " </span><br/>" +
                    "   <label>" + localizeResource("sales-amount") + "</label>" +
                    "   <span class='sales-amount'> " + (data.sales / 1000).toFixed(1) + "K" + "</span><br/>";
                //}
                //detailStatBox += "</div>";
                return detailStatBox;
            });
        var timeCard = {
            monthNames: [
                localizeResource("january"),
                localizeResource("february"),
                localizeResource("march"),
                localizeResource("april"),
                localizeResource("may"),
                localizeResource("june"),
                localizeResource("july"),
                localizeResource("august"),
                localizeResource("september"),
                localizeResource("october"),
                localizeResource("november"),
                localizeResource("december")
            ],
            dayOfWeekNames: [
                localizeResource("sunday"),
                localizeResource("monday"),
                localizeResource("tuesday"),
                localizeResource("wednesday"),
                localizeResource("thursday"),
                localizeResource("friday"),
                localizeResource("saturday")
            ],
            shortDayOfWeekNames: ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"],
            cardType: { COUNTING: "COUNTING", CONVERSION: "CONVERSION" },
            periodType: { WEEKLY: "WEEKS", MONTHLY: "MONTHS", YEARLY: "YEARS" },
            cardBoxId: "timeCardBox",
            cardWidth: 900,
            cardHeight: 700,
            legend: {
                height: 40,
                width: 400,
                blockHeight: 18,
                blockWidth: 40,
                textWidth: 40
            },
            formatter: {
                hour: d3.time.format("%Y%m%d%H"),
                day: d3.time.format("%Y%m%d"),
                week: d3.time.format("%Y%U"),
                month: d3.time.format("%Y%m")
            },
            theme: {
                SUNSHINE: {
                    colorRanges: ["#FBD19B", "#F8C06C", "#F6AD10", "#EA981D", "#D07C00"],
                    hoverColor: "gray",
                    noDataColor: "#FBD19B",
                    cellBorderColor: "#eee"
                },
                INDIGO: {
                    colorRanges: ["#C7CBE5", "#99A3CF", "#7081BB", "#4666AA", "#034EA2"],
                    hoverColor: "gray",
                    noDataColor: "#C7CBE5",
                    cellBorderColor: "#eee"
                }
            },
            _current: {
                theme: null,
                cardType: null,
                periodType: 'WEEKS',
                consolidatedData: null, //raw count+pos
                compiledData: null //separated by period
            },
            animation: {
                enabled: true,
                easing: "linear",
                duration: { weekly: 130, monthly: 30, yearly: 150 }
            },
            posTimeFormat: 'dd/MM/yyyy HH:mm:ss',
            isPosData: false
        }

        /**
         *
         * @param containerId   card container div Id
         * @param cardType      timeCard.cardType
         * @param periodType    timeCard.periodType
         * @param rawDataSet    hourly count+pos data sorted in the descending order
         *                      Use (timeCard.getConsolidatedData) to combine count and pos data
         *
         */
        timeCard.generateCard = function(containerId, cardType, periodType, peopleCountingData, posData) {
            this._initUi(containerId);
            timeCard._setPeriodType(periodType);
            timeCard._setCardType(cardType);
            timeCard._consolidateData(peopleCountingData, posData);
            timeCard._prepareDataByPeriod();
            timeCard._draw();
        }

        timeCard.switchCartType = function(targetCardType) {
            if (timeCard._current.compiledData == null) {
                console.log("card not initialized.");
                return;
            }

            if (targetCardType == timeCard._current.cardType) {
                console.log("Already selected: " + targetCardType);
                return;
            }

            //set theme
            timeCard._clearChildElements(timeCard.cardBoxId);
            timeCard._setCardType(targetCardType);
            timeCard._draw();
        }

        timeCard.switchPeriodType = function(targetPeriodType) {
            if (timeCard._current.compiledData == null) {
                console.log("card not initialized.");
                return;
            }

            if (targetPeriodType == timeCard._current.periodType) {
                console.log("already selected: " + targetPeriodType);
                return;
            }

            timeCard._clearChildElements(timeCard.cardBoxId);
            timeCard._setPeriodType(targetPeriodType);
            timeCard._prepareDataByPeriod();
            timeCard._draw();
        }

        timeCard.removeCard = function() {
            timeCard._current = {
                theme: null,
                cardType: null,
                periodType: null,
                consolidatedData: null,
                compiledData: null
            };

            timeCard._clearChildElements($("#" + timeCard.cardBoxId).parent().attr("id"));
        }

        //returns a list of xml serialized svg images
        timeCard.getSvgList = function(containerId) {
            var svgList = $("#" + containerId + " svg");
            var serializedList = [];
            var xmlSerializer = new XMLSerializer;
            svgList.each(function(idx, svgObj) {
                serializedList.push(xmlSerializer.serializeToString(svgObj));
            });

            return serializedList;
        }

        timeCard._initUi = function(containerId) {
            //clear container
            //timeCard.cardBoxId = timeCard.cardBoxId  + containerId;
            timeCard._clearChildElements(containerId);
            $("#" + containerId).addClass("time-card");

            //add components
            $("#" + containerId).append(
                //'<div class="card_period_selector">' +
                //'   <ul>' +
                //'       <li class="selected">' +
                //'           <input id="radWeekly" type="radio" name="cardPeriodType" value="WEEKLY"/>' +
                //'           <label for="radWeekly">' + localizeResource("weekly") + '</label>' +
                //'       </li>' +
                //'       <li>' +
                //'           <input id="radMonthly" type="radio" name="cardPeriodType" id="blue" value="MONTHLY"/>' +
                //'           <label for="radMonthly">' + localizeResource("monthly") + '</label>' +
                //'       </li>' +
                //'       <li>' +
                //'           <input id="radYearly" type="radio" name="cardPeriodType" id="green" value="YEARLY"/>' +
                //'           <label for="radYearly">' + localizeResource("yearly") + '</label>' +
                //'       </li>' +
                //'   </ul>' +
                //'</div>' +
                //'<div class="card_type_selector">' +
                //'    <input id="radCounting" value="COUNTING" type="radio" name="cardChartType" checked/>' +
                //'    <label for="radCounting">' + localizeResource("no-people-in") + '</label>' +
                //'    <input id="radConversion" value="CONVERSION" type="radio" name="cardChartType"/>' +
                //'    <label for="radConversion">' + localizeResource("conversion-rate") + '</label>' +
                //'</div>' +
                '<div class="timecardbox" id="' + timeCard.cardBoxId + '"></div>'
            );

            //<span style="display:inline-block; width:9px; height:9px; background-color:#f6ad40;"></span>
            //<span style="font-size:12px; color:#666666">People Counting</span>
            //<span style="display:inline-block; width:9px; height:9px; background-color:#4d4d4d;"></span>
            //<span style="font-size:12px; color:#666666">Conversion Rate</span>


            //popover box
            if (d3.selectAll("div.timecard-tooltip").empty()) {

                // timeCard.timeCardTooltip = d3.select("body").append("div")
                //     .attr("class", "timecard-tooltip")
                //     .style("opacity", 0);
            }

            //var popoverBox = $(".detailed-stat-box");
            //if (popoverBox[0] == null) {
            //    var detailStatBox =
            //        "<div class='detailed-stat-box'>" +
            //        "   <label>" + localizeResource("no-people-in") + "</label>" +
            //    "   <span class='people-in'></span><br/>";
            //    if(timeCard.isPosData) {
            //        detailStatBox +=
            //        "   <label>" + localizeResource("conversion-rate") + "</label>" +
            //        "   <span class='conversion-rate'></span><br/>" +
            //        "   <label>" + localizeResource("receipt") + "</label>" +
            //        "   <span class='receipt'></span><br/>" +
            //        "   <label>" + localizeResource("sales-amount") + "</label>" +
            //        "   <span class='sales-amount'></span><br/>";
            //    }
            //    detailStatBox += "</div>";
            //
            //    $("#" + containerId).append(detailStatBox);
            //}
            //handle card change events
            $("#" + containerId + " .card_type_selector input:radio[name='cardChartType']").change(function() {
                timeCard.switchCartType(this.value);
            });

            //handle period change events
            $("#" + containerId + " .card_period_selector input:radio[name='cardPeriodType']").change(function() {
                $(this).parents('li').addClass('selected');
                $(this).parents().siblings('li').removeClass('selected');
                timeCard.switchPeriodType(this.value);
            });
        }

        timeCard._setCardType = function(cardType) {
            timeCard._current.cardType = cardType;

            $(".detailed-stat-box").removeClass("theme-sunshine");
            $(".detailed-stat-box").removeClass("theme-indigo");
            if (cardType == timeCard.cardType.COUNTING) {
                timeCard._current.theme = timeCard.theme.SUNSHINE;
                $(".detailed-stat-box").addClass("theme-sunshine");
            } else {
                timeCard._current.theme = timeCard.theme.INDIGO;
                $(".detailed-stat-box").addClass("theme-indigo");
            }
        }

        timeCard._setPeriodType = function(periodType) {
            timeCard._current.periodType = periodType;
        }

        /**
         *
         * @param peopleCountingData    response.data from getAnalyticsReport
         * @param posData               response.salse from getPosSalesReport
         * @returns {Array}             list of combined data of the form:
         *                                  {
         *                                      date: dateObject,
         *                                      sales: 0,
         *                                      receipts: 0,
         *                                      count: 0
         *                                  }
         *
         */
        timeCard._consolidateData = function(peopleCountingData, posData) {
            //compile pos
            var posHourlyMap = {};
            if (posData == null || posData.length == 0) {
                $(".card_type_selector").hide()
            } else {
                $.each(posData, function(idx, hourlyData) {
                    //var posLocalTime = UtilsService.convertUTCtoLocal(new Date(hourlyData.sales.time));
                    var posLocalTime = UtilsService.convertUTCtoLocal(kendo.parseDate(hourlyData.sales.time, timeCard.posTimeFormat));
                    var hourSpecifier = timeCard.formatter.hour(posLocalTime);
                    posHourlyMap[hourSpecifier] = {
                        date: posLocalTime,
                        sales: hourlyData.sales.amount,
                        receipts: hourlyData.sales.count
                    };
                });
            }

            //compile people counting
            var countHourlyMap = {};
            $.each(peopleCountingData, function(idx, countData) {
                var pLocalTime = UtilsService.convertUTCtoLocal(new Date(countData.date));
                var hourSpecifier = timeCard.formatter.hour(pLocalTime);

                var hourlyTotal = countHourlyMap[hourSpecifier];
                if (hourlyTotal == null) {
                    hourlyTotal = 0;
                }

                hourlyTotal += countData.in;
                countHourlyMap[hourSpecifier] = hourlyTotal;
            });

            //combine pos with analytics
            var combinedList = [];
            $.each(countHourlyMap, function(hourSpecifier, hourlyTotal) {
                if (hourlyTotal == 0) {
                    return true; //ignore
                }

                var localTime = timeCard.formatter.hour.parse(hourSpecifier);
                var combinedData = posHourlyMap[hourSpecifier];
                if (combinedData == null) {
                    combinedData = {
                        sales: 0,
                        receipts: 0
                    }
                }

                combinedData.date = localTime;
                combinedData.count = hourlyTotal;
                combinedList.push(combinedData);
            });

            //sort desc
            combinedList.sort(function(a, b) {
                return a.date - b.date;
            });

            timeCard._current.consolidatedData = combinedList;
        }

        timeCard._prepareDataByPeriod = function() {
            var periodType = timeCard._current.periodType;
            var consolidatedData = timeCard._current.consolidatedData;
            switch (periodType) {
                case timeCard.periodType.WEEKLY:
                    timeCard._current.compiledData = timeCard._separateDataByWeek(consolidatedData);
                    break;

                case timeCard.periodType.MONTHLY:
                    timeCard._current.compiledData = timeCard._separateDataByMonth(consolidatedData);
                    break;

                case timeCard.periodType.YEARLY:
                    timeCard._current.compiledData = timeCard._separateDataByYear(consolidatedData);
                    break;

                default:
                    console.log("unknown period");
                    break;
            }
        }

        timeCard._draw = function() {
            var cardType = timeCard._current.cardType;
            var periodType = timeCard._current.periodType;

            switch (periodType) {
                case timeCard.periodType.WEEKLY:
                    $.each(timeCard._current.compiledData.weeklyDataSet, function(weekSpecifier, weekDataSet) {
                        timeCard._generateOneWeek(
                            timeCard.cardBoxId,
                            cardType,
                            weekSpecifier,
                            weekDataSet,
                            timeCard._current.compiledData.minCount,
                            timeCard._current.compiledData.maxCount);
                    });
                    break;

                case timeCard.periodType.MONTHLY:
                    $.each(timeCard._current.compiledData.monthlyDataSet, function(monthSpecifier, monthDataSet) {
                        var monthDate = timeCard.formatter.month.parse(monthSpecifier);
                        var monthDetails = timeCard._getMonthDateDetails(monthDate);

                        timeCard._generateOneMonth(
                            timeCard.cardBoxId,
                            cardType,
                            monthDetails,
                            monthDataSet,
                            timeCard._current.compiledData.minCount,
                            timeCard._current.compiledData.maxCount);
                    });
                    break;

                case timeCard.periodType.YEARLY:
                    $.each(timeCard._current.compiledData.yearlyDataSet, function(year, yearDataSet) {
                        timeCard._generateOneYear(
                            timeCard.cardBoxId,
                            cardType,
                            year,
                            yearDataSet,
                            timeCard._current.compiledData.minCount,
                            timeCard._current.compiledData.maxCount);
                    });
                    break;

                default:
                    console.log("unknown period");
                    break;
            }
        }

        timeCard._generateOneWeek = function(cardContainerId, cardType, weekSpecifier, hourlyDataSet, minCount, maxCount) {

            var bgColorFn = timeCard._getBgColoringFn(minCount, maxCount);

            //get week start and end
            var dateFormatter = d3.time.format("%Y-%m-%d");
            var weekDates = timeCard._getWeekStartEndDates(weekSpecifier);
            var strWeekStart = dateFormatter(weekDates.startDate);
            var strWeekEnd = dateFormatter(weekDates.endDate);
            var weekTitle = strWeekStart + " " + localizeResource("to") + " " + strWeekEnd;

            //dimensions
            var margin = { top: 20, right: 10, bottom: 0, left: 10 };
            var innerWidth = timeCard.cardWidth - margin.left - margin.right;
            var innerHeight = timeCard.cardHeight - margin.top - margin.bottom;
            var headerHeight = 25;
            var periodGroupWidth = 60;
            var numOfCols = 7; // 7 days
            var numOfRows = 24; // 24 hrs
            var colWidth = (innerWidth - periodGroupWidth) / numOfCols;
            var rowHeight = (innerHeight - timeCard.legend.height - headerHeight) / numOfRows;


            //Create SVG element
            var svgContainer = d3.select("#" + cardContainerId)
                .append("svg")
                .attr("width", timeCard.cardWidth)
                .attr("height", timeCard.cardHeight);

            svgContainer.call(tip);

            var mainWrapper = svgContainer.append("g")
                .attr("transform", function(d, i) {
                    var xOffset = margin.left;
                    var yOffset = margin.top;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });
            timeCard._attachLegend(mainWrapper, cardType);

            //week indicator
            var weekIndicator = mainWrapper.append("text")
                .attr("class", "month_year_indicator")
                .attr("font-size", "14px")
                .attr("font-weight", "bold")
                .attr("x", function() {
                    var posX = innerWidth - 200;
                    return posX;
                })
                .attr("y", function(d, i) {
                    var posY = 15;
                    return posY;
                })
                .text(function(d, i) {
                    return weekTitle;
                });

            //Main Card Box
            var mainCardBox = mainWrapper.append("g")
                .attr("transform", function(d, i) {
                    var xOffset = 0;
                    var yOffset = timeCard.legend.height;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });

            //Column Header Box
            var colHeaderBox = mainCardBox.append("g")
                .attr("transform", function(d, i) {
                    var xOffset = periodGroupWidth;
                    var yOffset = 0;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });

            //Populate headers
            var headers = colHeaderBox.selectAll("text")
                .data(timeCard.dayOfWeekNames)
                .enter()
                .append("text")
                .attr("class", "column_header")
                .attr("text-anchor", "middle")
                .attr("font-size", "12px")
                .attr("x", function(d, i) {
                    var offset = (colWidth / 2);
                    var posX = i * colWidth;
                    return posX + offset;
                })
                .attr("y", function(d, i) {
                    var offset = (rowHeight / 2) + 3;
                    var posY = 0;
                    return posY + offset;
                })
                .text(function(d, i) {
                    return d;
                });

            //Periods Column
            var periodBox = mainCardBox.append("g")
                .attr("transform", function(d, i) {
                    var xOffset = 0;
                    var yOffset = headerHeight;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });

            //populate period labels
            var periodLabels = periodBox.selectAll("text")
                .data(timeCard._getHourlyPeriods())
                .enter()
                .append("text")
                .attr("class", "label_period")
                .attr("font-size", "12px")
                .attr("x", function(d, i) {
                    var offset = 10;
                    var posX = 0;
                    return posX + offset;
                })
                .attr("y", function(d, i) {
                    var offset = (rowHeight / 2) + 3;
                    var posY = i * rowHeight;
                    return posY + offset;
                })
                .text(function(d, i) {
                    return d;
                });

            //Data Cells box
            var dataCellBox = mainCardBox.append("g")
                .attr("transform", function(d, i) {
                    var xOffset = periodGroupWidth;
                    var yOffset = headerHeight;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });

            //create cell groups
            dataCellBox.selectAll("g")
                .data(timeCard._getWeekSlots(weekDates.startDate))
                .enter()
                .append("g")
                .on("mouseenter", function(hourSpecifier) {
                    var data = hourlyDataSet[hourSpecifier];
                    timeCard._handleMouseEnter(d3.select(this), data);
                })
                .on("mouseleave", function(hourSpecifier) {
                    var data = hourlyDataSet[hourSpecifier];
                    timeCard._handleMouseLeave(d3.select(this), data, bgColorFn);
                });

            //colored background rectangles
            dataCellBox.selectAll("g")
                .append("rect")
                .attr("class", "hourly_rect")
                .attr("width", colWidth)
                .attr("height", rowHeight)
                .attr("stroke-width", "1")
                .attr("stroke", timeCard._current.theme.cellBorderColor)
                .attr("x", "0")
                .attr("y", "0")
                .attr("fill", function(hourSpecifier, i) {
                    var data = hourlyDataSet[hourSpecifier];
                    return bgColorFn(data);
                });

            //Cell texts
            dataCellBox.selectAll("g")
                .append("text")
                .attr("text-anchor", "middle")
                .attr("font-size", "12px")
                .attr("fill", "#000")
                .attr("x", colWidth / 2)
                .attr("y", rowHeight / 2 + 4)
                .text(function(hourSpecifier, i) {
                    var data = hourlyDataSet[hourSpecifier];
                    if (data == null) {
                        return "";
                    }
                    return timeCard._getWeeklyCellContent(data);
                });


            //animation
            dataCellBox.selectAll("g")
                .attr("transform", function(hourSpecifier, i) {
                    var posX = i % 7;
                    var posY = Math.floor(i / 7);
                    var xOffset = 0;
                    var yOffset = (posY * rowHeight);
                    return "translate(" + xOffset + ", " + yOffset + ")";
                })
                .transition()
                .ease(timeCard.animation.easing)
                .duration(function(hourSpecifier, i) {
                    var posX = i % 7;
                    var posY = Math.floor(i / 7);
                    var duration = timeCard.animation.enabled ? timeCard.animation.duration.weekly : 0;
                    return duration * posX;
                })
                .attr("transform", function(hourSpecifier, i) {
                    var posX = i % 7;
                    var posY = Math.floor(i / 7);
                    var xOffset = (posX * colWidth);
                    var yOffset = (posY * rowHeight);
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });
            //var legendWrapper = svgContainer.append("g")
            //    .attr("transform", function (d, i) {
            //        var xOffset = margin.left;
            //        var yOffset = 700;
            //        return "translate(" + xOffset + ", " + yOffset + ")";
            //    });
            //legend


        }

        timeCard._generateOneMonth = function(cardContainerId, cardType, monthDetails, dailyDataSet, minCount, maxCount) {

            //compile data
            var bgColorFn = timeCard._getBgColoringFn(minCount, maxCount);

            //dimensions
            var margin = { top: 20, right: 10, bottom: 0, left: 10 };
            var innerWidth = timeCard.cardWidth - margin.left - margin.right;
            var innerHeight = timeCard.cardHeight - margin.top - margin.bottom;
            var numOfCols = 7; // 7 days
            var numOfRows = 6; // 6 weeks
            var headerHeight = 75;
            var colWidth = (innerWidth) / numOfCols;
            var rowHeight = (innerHeight - timeCard.legend.height - headerHeight) / numOfRows;
            var cellPadding = { top: 5, right: 5, bottom: 5, left: 5 };
            var cellInnerWidth = colWidth - cellPadding.left - cellPadding.right;
            var cellInnerHeight = rowHeight - cellPadding.top - cellPadding.bottom;
            var monthTitleWidth = 150;

            //Create SVG element
            var svgContainer = d3.select("#" + cardContainerId)
                .append("svg")
                .attr("width", timeCard.cardWidth)
                .attr("height", timeCard.cardHeight);

            var mainWrapper = svgContainer.append("g")
                .attr("transform", function(d, i) {
                    var xOffset = margin.left;
                    var yOffset = margin.top;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });

            //legend
            timeCard._attachLegend(mainWrapper, cardType);

            //month year indicator
            var monthIndicator = mainWrapper.append("text")
                .attr("class", "month_year_indicator")
                .attr("font-size", "15px")
                .attr("font-weight", "bold")
                .attr("x", function() {
                    var posX = innerWidth - monthTitleWidth;
                    return posX;
                })
                .attr("y", function(d, i) {
                    var posY = 15;
                    return posY;
                })
                .text(function(d, i) {
                    var monthTitle = timeCard.monthNames[monthDetails.monthNumber] + " " + monthDetails.year;
                    return monthTitle;
                });

            //Main Card Box
            var mainCardBox = mainWrapper.append("g")
                .attr("transform", function(d, i) {
                    var xOffset = 0;
                    var yOffset = 0;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });

            //Column Header Box
            var colHeaderBox = mainCardBox.append("g")
                .attr("transform", function(d, i) {
                    var xOffset = 0;
                    var yOffset = 10;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });

            //Populate headers
            var headers = colHeaderBox.selectAll("text")
                .data(timeCard.dayOfWeekNames)
                .enter()
                .append("text")
                .attr("class", "column_header")
                .attr("text-anchor", "middle")
                .style("font-size", "12px")
                .attr("x", function(d, i) {
                    var offset = (colWidth / 2);
                    var posX = i * colWidth;
                    return posX + offset;
                })
                .attr("y", function(d, i) {
                    var offset = (rowHeight / 2) + 3;
                    var posY = 0;
                    return posY + offset;
                })
                .text(function(d, i) {
                    return d;
                });

            //days of the month
            var dayGroup = mainCardBox.append("g")
                .attr("transform", function(d, i) {
                    var xOffset = 0;
                    var yOffset = headerHeight;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });

            //create Hour cell groups
            dayGroup.selectAll("g")
                .data(timeCard._getMonthSlots(monthDetails.monthNumber, monthDetails.year))
                .enter()
                .append("g")
                .on("mouseenter", function(daySpecifier) {
                    var data = dailyDataSet[daySpecifier];
                    timeCard._handleMouseEnter(d3.select(this), data);
                })
                .on("mouseleave", function(daySpecifier) {
                    var data = dailyDataSet[daySpecifier];
                    timeCard._handleMouseLeave(d3.select(this), data, bgColorFn);
                });

            //colored background rectangles
            dayGroup.selectAll("g")
                .append("rect")
                .attr("class", "hourly_rect")
                .attr("width", colWidth)
                .attr("height", rowHeight)
                .attr("stroke-width", "1")
                .attr("stroke", timeCard._current.theme.cellBorderColor)
                .attr("x", "0")
                .attr("y", "0")
                .attr("fill", function(daySpecifier, i) {
                    var data = dailyDataSet[daySpecifier];
                    return bgColorFn(data);
                });

            //Cell texts
            dayGroup.selectAll("g")
                .append("text")
                .attr("text-anchor", "middle")
                .attr("font-size", "12px")
                .attr("fill", "#000")
                .attr("x", colWidth / 2)
                .attr("y", rowHeight / 2 + 4)
                .text(function(daySpecifier, i) {
                    var date = timeCard.formatter.day.parse(daySpecifier);
                    var data = dailyDataSet[daySpecifier];
                    return timeCard._getMonthlyCellContent(date, data);
                });

            //Day Number texts
            dayGroup.selectAll("g")
                .append("text")
                .attr("text-anchor", "right")
                .attr("font-size", "12px")
                .attr("fill", "#000")
                .attr("x", colWidth - 24)
                .attr("y", 20)
                .text(function(daySpecifier, i) {
                    var date = timeCard.formatter.day.parse(daySpecifier);
                    return date.getDate();
                });

            //animation
            dayGroup.selectAll("g")
                .attr("transform", function(daySpecifier, i) {
                    var date = timeCard.formatter.day.parse(daySpecifier);
                    var dayOfWeek = date.getDay();
                    var weekOfMonth = Math.floor((date.getDate() - 1 + monthDetails.week1EmptySlots) / 7);
                    var xOffset = dayOfWeek * colWidth;
                    var yOffset = 0;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                })
                .transition()
                .ease(timeCard.animation.easing)
                .duration(function(daySpecifier, i) {
                    var date = timeCard.formatter.day.parse(daySpecifier);
                    var duration = timeCard.animation.enabled ? timeCard.animation.duration.monthly : 0;
                    return duration * date.getDate();
                })
                .attr("transform", function(daySpecifier, i) {
                    var date = timeCard.formatter.day.parse(daySpecifier);
                    var dayOfWeek = date.getDay();
                    var weekOfMonth = Math.floor((date.getDate() - 1 + monthDetails.week1EmptySlots) / 7);
                    var xOffset = dayOfWeek * colWidth;
                    var yOffset = weekOfMonth * rowHeight;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });
        }

        timeCard._generateOneYear = function(cardContainerId, cardType, year, monthlyDataSet, minCount, maxCount) {

            var bgColorFn = timeCard._getBgColoringFn(minCount, maxCount);

            //dimensions
            var margin = { top: 20, right: 10, bottom: 0, left: 10 };
            var innerWidth = timeCard.cardWidth - margin.left - margin.right;
            var innerHeight = timeCard.cardHeight - margin.top - margin.bottom;
            var headerHeight = 25;
            var numOfCols = 4; // 4x3
            var numOfRows = 3;
            var colWidth = innerWidth / numOfCols;
            var rowHeight = (innerHeight - timeCard.legend.height - headerHeight) / numOfRows;

            //Create SVG element
            var svgContainer = d3.select("#" + cardContainerId)
                .append("svg")
                .attr("width", timeCard.cardWidth)
                .attr("height", timeCard.cardHeight);

            var mainWrapper = svgContainer.append("g")
                .attr("transform", function(d, i) {
                    var xOffset = margin.left;
                    var yOffset = margin.top;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });

            //year indicator
            var yearIndicator = mainWrapper.append("text")
                .attr("class", "month_year_indicator")
                .attr("font-size", "17px")
                .attr("font-weight", "bold")
                .attr("x", function() {
                    var posX = innerWidth - 60;
                    return posX;
                })
                .attr("y", function(d, i) {
                    var posY = 15;
                    return posY;
                })
                .text(function(d, i) {
                    return year;
                });

            //legend
            timeCard._attachLegend(mainWrapper, cardType);

            //Main Card Box
            var mainCardBox = mainWrapper.append("g")
                .attr("transform", function(d, i) {
                    var xOffset = 0;
                    var yOffset = timeCard.legend.height;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });

            //generate 12 months
            for (var i = 0; i < 12; i++) {
                var monthNumber = i;
                var monthDetails = timeCard._getMonthDateDetails(new Date(year, monthNumber, 1));

                var monthDataSet = monthlyDataSet[monthNumber];
                if (monthDataSet == null) {
                    monthDataSet = {};
                }

                timeCard._generateMiniMonth(mainCardBox, colWidth, rowHeight, monthDetails, monthDataSet, bgColorFn);
            }
        }

        timeCard._generateMiniMonth = function(parent, width, height, monthDetails, dailyDataSet, bgColorFn) {

            var listOfDays = timeCard._getMonthSlots(monthDetails.monthNumber, monthDetails.year);

            //dimensions
            var margin = { top: 8, right: 5, bottom: 8, left: 5 };
            var innerWidth = (width - margin.left - margin.right);
            var innerHeight = (height - margin.top - margin.bottom);
            var monthTitleHeight = 15;
            var headerHeight = 22;
            var numOfCols = 7; // 7 days
            var numOfRows = 6; // 6 weeks
            var colWidth = innerWidth / numOfCols;
            var rowHeight = (innerHeight - monthTitleHeight - headerHeight) / numOfRows;
            var location = {
                x: (monthDetails.monthNumber) % 4,
                y: Math.floor(monthDetails.monthNumber / 4)
            }

            var miniMonth = parent.append("g")
                .attr("transform", function(d, i) {
                    var xOffset = (location.x * width) + margin.left;
                    var yOffset = (location.y * height) + margin.top;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });

            //month Indicator
            var monthIndicator = miniMonth.append("text")
                .attr("class", "month_year_indicator")
                .attr("font-size", "14px")
                .attr("font-weight", "bold")
                .attr("x", function() {
                    var posX = 2;
                    return posX;
                })
                .attr("y", function(d, i) {
                    var posY = 10;
                    return posY;
                })
                .text(function(d, i) {
                    return timeCard.monthNames[monthDetails.monthNumber];
                });

            var monthContainer = miniMonth.append("g")
                .attr("transform", function(d, i) {
                    return "translate(0, " + monthTitleHeight + ")";
                });

            //Header Group
            var headerGroup = monthContainer.append("g")
                .attr("transform", function(d, i) {
                    return "translate(0, 0)";
                });
            var headers = headerGroup.selectAll("text")
                .data(timeCard.shortDayOfWeekNames)
                .enter()
                .append("text")
                .attr("class", "column_header")
                .attr("text-anchor", "middle")
                .style("font-size", "12px")
                .attr("x", function(d, i) {
                    var offset = (colWidth / 2); //text position adjustment
                    var posX = i * colWidth;
                    return posX + offset;
                })
                .attr("y", function(d, i) {
                    var offset = (rowHeight / 2) + 3; //text position adjustment
                    var posY = 0;
                    return posY + offset;
                })
                .text(function(d, i) {
                    return d;
                });

            //days of the month
            var dayGroup = monthContainer.append("g")
                .attr("transform", function(d, i) {
                    return "translate(0, " + headerHeight + ")";
                });

            //create cell groups
            dayGroup.selectAll("g")
                .data(listOfDays)
                .enter()
                .append("g")
                .on("mouseenter", function(daySpecifier) {
                    var data = dailyDataSet[daySpecifier];
                    timeCard._handleMouseEnter(d3.select(this), data);
                })
                .on("mouseleave", function(daySpecifier) {
                    var data = dailyDataSet[daySpecifier];
                    timeCard._handleMouseLeave(d3.select(this), data, bgColorFn);
                });

            //colored background rectangles
            dayGroup.selectAll("g")
                .append("rect")
                .attr("class", "hourly_rect")
                .attr("width", colWidth)
                .attr("height", rowHeight)
                .attr("stroke-width", 1)
                .attr("stroke", timeCard._current.theme.cellBorderColor)
                .attr("x", "0")
                .attr("y", "0")
                .attr("fill", function(daySpecifier, i) {
                    var data = dailyDataSet[daySpecifier];
                    return bgColorFn(data);
                });

            //Day Number texts
            dayGroup.selectAll("g")
                .append("text")
                .attr("text-anchor", "middle")
                .attr("font-size", "11px")
                .attr("fill", "#000")
                .attr("x", colWidth / 2)
                .attr("y", rowHeight / 2 + 4)
                .text(function(daySpecifier, i) {
                    var date = timeCard.formatter.day.parse(daySpecifier);
                    return date.getDate();
                });

            //animation
            dayGroup.selectAll("g")
                .attr("transform", function(daySpecifier, i) {
                    var date = timeCard.formatter.day.parse(daySpecifier);
                    var dayOfWeek = date.getDay();
                    var weekOfMonth = Math.floor((date.getDate() - 1 + monthDetails.week1EmptySlots) / 7);
                    var xOffset = dayOfWeek * colWidth;
                    var yOffset = 0;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                })
                .transition()
                .ease(timeCard.animation.easing)
                .duration(function(daySpecifier, i) {
                    var date = timeCard.formatter.day.parse(daySpecifier);
                    var weekOfMonth = Math.floor((date.getDate() - 1 + monthDetails.week1EmptySlots) / 7);
                    var duration = timeCard.animation.enabled ? timeCard.animation.duration.yearly : 0;
                    return duration * weekOfMonth;
                })
                .attr("transform", function(daySpecifier, i) {
                    var date = timeCard.formatter.day.parse(daySpecifier);
                    var dayOfWeek = date.getDay();
                    var weekOfMonth = Math.floor((date.getDate() - 1 + monthDetails.week1EmptySlots) / 7);
                    var xOffset = dayOfWeek * colWidth;
                    var yOffset = weekOfMonth * rowHeight;
                    return "translate(" + xOffset + ", " + yOffset + ")";
                });
        }

        timeCard._compileDataByDay = function(hourlyDataSet) {
            var dailyMap = {};
            var dayFormatter = d3.time.format("%Y%m%d");

            $.each(hourlyDataSet, function(idx, hourlyData) {
                var year = hourlyData.date.getFullYear();
                var month = hourlyData.date.getMonth();
                var day = hourlyData.date.getDate();
                var daySpecifier = dayFormatter(hourlyData.date);

                var targetDay = dailyMap[daySpecifier];
                if (targetDay == null) {
                    targetDay = {
                        date: new Date(year, month, day, 1),
                        count: hourlyData.count,
                        receipts: hourlyData.receipts,
                        sales: hourlyData.sales
                    };
                } else {
                    targetDay.count += hourlyData.count;
                    targetDay.receipts += hourlyData.receipts;
                    targetDay.sales += hourlyData.sales;
                }

                dailyMap[daySpecifier] = targetDay;
            });

            var compiledList = [];
            $.each(dailyMap, function(idx, dayData) {
                compiledList.push(dayData);
            });

            return compiledList;
        }

        timeCard._separateDataByWeek = function(hourlyDataSet) {
            var compiledData = {
                weeklyDataSet: {},
                minCount: 0,
                maxCount: 0
            }

            if (hourlyDataSet == null || hourlyDataSet.length == 0) {
                notification('success', localizeResource("no-data"));
                return compiledData;
            }

            var weeklyDataSet = {};
            var minCount = hourlyDataSet[0].count;
            var maxCount = hourlyDataSet[0].count;

            //populate weekly bins
            $.each(hourlyDataSet, function(idx, hourlyData) {
                var weekSpecifier = parseInt(timeCard.formatter.week(hourlyData.date));
                if (weeklyDataSet[weekSpecifier] == null) {
                    weeklyDataSet[weekSpecifier] = {};
                }

                var hourSpecifier = timeCard.formatter.hour(hourlyData.date);
                weeklyDataSet[weekSpecifier][hourSpecifier] = hourlyData;

                //find min and max
                if (hourlyData.count < minCount) {
                    minCount = hourlyData.count;
                } else if (hourlyData.count > maxCount) {
                    maxCount = hourlyData.count;
                }
            });

            compiledData.weeklyDataSet = weeklyDataSet;
            compiledData.minCount = minCount;
            compiledData.maxCount = maxCount;
            return compiledData;
        }

        //returns a map of months with data array for each month
        timeCard._separateDataByMonth = function(hourlyDataSet) {
            var compiledData = {
                monthlyDataSet: {},
                minCount: 0,
                maxCount: 0
            }

            if (hourlyDataSet == null || hourlyDataSet.length == 0) {
                notification.popupAlert(localizeResource("no-data"));
                return compiledData;
            }

            var dailyDataSet = timeCard._compileDataByDay(hourlyDataSet);
            var monthlyDataSet = {};
            var minCount = dailyDataSet[0].count;
            var maxCount = dailyDataSet[0].count;

            //populate monthly bins
            $.each(dailyDataSet, function(idx, dailyData) {
                var monthSpecifier = parseInt(timeCard.formatter.month(dailyData.date));
                if (monthlyDataSet[monthSpecifier] == null) {
                    monthlyDataSet[monthSpecifier] = [];
                }

                var daySpecifier = timeCard.formatter.day(dailyData.date);
                monthlyDataSet[monthSpecifier][daySpecifier] = dailyData;

                //find max/min
                if (dailyData.count < minCount) {
                    minCount = dailyData.count;
                } else if (dailyData.count > maxCount) {
                    maxCount = dailyData.count;
                }
            });

            compiledData.monthlyDataSet = monthlyDataSet;
            compiledData.minCount = minCount;
            compiledData.maxCount = maxCount;
            return compiledData;
        }

        //returns a map of years with an array of months for each year
        timeCard._separateDataByYear = function(hourlyDataSet) {
            var compiledData = {
                yearlyDataSet: {},
                minCount: 0,
                maxCount: 0
            };

            if (hourlyDataSet == null || hourlyDataSet.length == 0) {
                notification.popupAlert(localizeResource("no-data"));
                return compiledData;
            }

            var monthlyCompiledData = timeCard._separateDataByMonth(hourlyDataSet);
            var yearlyDataSet = {};

            //populate monthly bins
            $.each(monthlyCompiledData.monthlyDataSet, function(monthSpecifier, monthlyData) {
                var monthDate = timeCard.formatter.month.parse(monthSpecifier);
                var year = monthDate.getFullYear();
                if (yearlyDataSet[year] == null) {
                    yearlyDataSet[year] = [];
                }
                yearlyDataSet[year][monthDate.getMonth()] = monthlyData;
            });

            compiledData.yearlyDataSet = yearlyDataSet;
            compiledData.minCount = monthlyCompiledData.minCount;
            compiledData.maxCount = monthlyCompiledData.maxCount;
            return compiledData;
        }

        timeCard._getHourlyPeriods = function() {
            var hourList = [
                "12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"
            ];

            var fullDay = [];

            //morning
            $.each(hourList, function(idx, hr) {
                fullDay.push(hr + " " + localizeResource("am"));
            });

            //evening
            $.each(hourList, function(idx, hr) {
                fullDay.push(hr + " " + localizeResource("pm"));
            });

            return fullDay;
        }

        //returns all possible slots for the week identified by start date
        timeCard._getWeekSlots = function(startDate) {
            var oneFullWeekSlots = [];
            var year = startDate.getFullYear();
            var month = startDate.getMonth();
            var firstDate = startDate.getDate();
            for (var hour = 0; hour < 24; hour++) {
                for (var day = 0; day < 7; day++) {
                    var hourObj = new Date(year, month, firstDate + day, hour);
                    oneFullWeekSlots.push(timeCard.formatter.hour(hourObj));
                }
            }

            return oneFullWeekSlots;
        }

        //returns all possible days for the month
        timeCard._getMonthSlots = function(monthNumber, year) {
            var lastDate = new Date(year, monthNumber + 1, 0);
            var fullMonthSlots = [];
            var dayNumber = 1;
            while (dayNumber <= lastDate.getDate()) {
                var dayObj = new Date(year, monthNumber, dayNumber);
                fullMonthSlots.push(timeCard.formatter.day(dayObj));
                dayNumber++;
            }

            return fullMonthSlots;
        }

        timeCard._showPopover = function(data) {
            var conversionRate = (data.receipts / data.count) * 100;
            $(".detailed-stat-box .people-in").html(data.count);
            $(".detailed-stat-box .conversion-rate").html(conversionRate.toFixed(1) + "%");
            $(".detailed-stat-box .receipt").html(data.receipts);
            $(".detailed-stat-box .sales-amount").html((data.sales / 1000).toFixed(1) + "K");
            d3.select(".detailed-stat-box").style("left", d3.event.pageX + "px");
            d3.select(".detailed-stat-box").style("top", d3.event.pageY - 50 + "px");
            //$(".detailed-stat-box").show();
        }

        timeCard._hidePopover = function() {
            //$(".detailed-stat-box").hide();
        }

        timeCard._attachLegend = function(parent, cardType) {
            var currentTheme = this._current.theme;
            var lowLabel = localizeResource("low");
            var highLabel = localizeResource("high");

            //Legend
            var legendGroup = parent.append("g");
            var legends = legendGroup.selectAll("rect")
                .data(currentTheme.colorRanges)
                .enter()
                .append("rect")
                .attr("width", timeCard.legend.blockWidth)
                .attr("height", timeCard.legend.blockHeight)
                .attr("fill", function(d, i) {
                    return d;
                })
                .attr("x", function(d, i) {
                    var posX = timeCard.legend.textWidth + (i * timeCard.legend.blockWidth);
                    return posX;
                })
                .attr("y", function(d, i) {
                    var posY = 0;
                    return posY;
                })
                .text(function(d, i) {
                    return d;
                });
            var lowCountingText = legendGroup.append("text")
                .attr("class", "label_legend")
                .attr("text-anchor", "middle")
                .attr("font-size", "12px")
                .attr("x", function(d, i) {
                    var posX = timeCard.legend.textWidth / 2;
                    return posX;
                })
                .attr("y", function(d, i) {
                    var posY = 12;
                    return posY;
                })
                .text(lowLabel);
            var highCountingText = legendGroup.append("text")
                .attr("class", "label_legend")
                .attr("text-anchor", "middle")
                .attr("font-size", "12px")
                .attr("x", function() {
                    var offset = timeCard.legend.textWidth / 2;
                    var posX = timeCard.legend.textWidth + (currentTheme.colorRanges.length * timeCard.legend.blockWidth);
                    return posX + offset;
                })
                .attr("y", function(d, i) {
                    var posY = 12;
                    return posY;
                })
                .text(highLabel);
        }

        timeCard._getMonthDateDetails = function(targetDate) {
            var targetYear = targetDate.getFullYear();
            var targetMonth = targetDate.getMonth();
            var firstDate = new Date(targetYear, targetMonth, 1);
            var lastDate = new Date(targetYear, targetMonth + 1, 0);
            var week1EmptySlots = firstDate.getDay();

            return {
                firstDate: firstDate,
                lastDate: lastDate,
                week1EmptySlots: week1EmptySlots,
                monthNumber: targetMonth,
                year: targetYear
            }
        }

        timeCard._getBgColoringFn = function(minCount, maxCount) {

            if (minCount == maxCount) { //when there is only one data
                minCount = 0;
            }

            var currentTheme = timeCard._current.theme;
            var minVal = 0;
            var maxVal = 100;

            if (timeCard._current.cardType == timeCard.cardType.COUNTING) {
                minVal = minCount;
                maxVal = maxCount;
            }

            var d3scale = d3.scale.quantize()
                .domain([minVal, maxVal])
                .range(d3.range(currentTheme.colorRanges.length));

            return function(data) {
                var bgColor = currentTheme.noDataColor;
                if (data == null) {
                    return bgColor;
                }

                var conversionRate = (data.receipts / data.count) * 100;
                conversionRate = conversionRate > 100 ? 100 : conversionRate;
                var pCount = data.count;
                var value = (timeCard._current.cardType == timeCard.cardType.COUNTING) ? pCount : conversionRate;

                var colorIndex = d3scale(value);
                return currentTheme.colorRanges[colorIndex];
            };
        }

        timeCard._getWeeklyCellContent = function(data) {

            var conversionRate = (data.receipts / data.count) * 100;
            var pCount = data.count;
            var primaryVal = "";
            var secondaryVal = "";
            if (this._current.cardType == timeCard.cardType.COUNTING) {
                var template;
                //if (timeCard.isPosData) {
                var template = "val1 (val2)";
                primaryVal = pCount;
                secondaryVal = conversionRate.toFixed(0) + "%";
                return template.replace("val1", primaryVal).replace("val2", secondaryVal);
                // } else {
                //  template = "val1";
                //     primaryVal = pCount;
                //     secondaryVal = conversionRate.toFixed(0) + "%";
                //     return  template.replace("val1", primaryVal);
                // }
            } else {
                var template = "val1 (val2)";
                primaryVal = conversionRate.toFixed(0) + "%";
                secondaryVal = pCount;
                return template.replace("val1", primaryVal).replace("val2", secondaryVal);
            }


        }

        timeCard._getMonthlyCellContent = function(date, data) {
            var displayValue = "";
            if (data != null) {
                displayValue = timeCard._getWeeklyCellContent(data);
            }

            return displayValue;
        }

        timeCard._getWeekStartEndDates = function(weekSpecifier) {
            var weekDates = {
                startDate: null,
                endDate: null
            }

            var weekNumber = parseInt(weekSpecifier.substring(4));
            var dayOfYear = weekNumber * 7;
            var weekStartDate = timeCard.formatter.week.parse(weekSpecifier);
            var missingDayCount = weekStartDate.getDay();

            weekStartDate.setDate(weekStartDate.getDate() + (dayOfYear - missingDayCount));
            weekDates.startDate = weekStartDate;

            var weekEndDate = new Date(weekStartDate.getFullYear(), weekStartDate.getMonth(), weekStartDate.getDate() + 6);
            weekDates.endDate = weekEndDate;

            return weekDates;
        }

        timeCard._handleMouseEnter = function(targetElement, data) {
            if (data == null) {
                return;
            }
            tip.show(data);
            //timeCard._showPopover(data);
            targetElement.style("background", timeCard._current.theme.hoverColor);
        }

        timeCard._handleMouseLeave = function(targetElement, data, bgColorFn) {
            //timeCard._hidePopover();
            tip.hide(data);
            targetElement.style("background", bgColorFn(data));
        }

        timeCard._clearChildElements = function(divId) {
            var container = document.getElementById(divId);
            if (container == null) {
                return;
            }

            while (container.firstChild) {
                container.removeChild(container.firstChild);
            }
        }

        var render = function() {

        }

        var link = function(scope, elem, attrs) {
            scope.$watch('timecardOptions', function(newVal, oldVal, scope) {
                var options = angular.fromJson(newVal);
                var id = $(elem).attr('id');
                var posData = [];
                if (scope.timecardOptions.peopleCountingData) {
                    if (scope.timecardOptions.isShowDemo) {
                        //if (scope.timecardOptions.posData && scope.timecardOptions.posData.length > 0) {
                        timeCard.isPosData = true;
                        posData = scope.timecardOptions.posData;
                    } else {
                        timeCard.isPosData = false;
                        posData = [];
                    }
                    timeCard.generateCard(id, timeCard.cardType.COUNTING, timeCard._current.periodType, scope.timecardOptions.peopleCountingData, posData);
                }
            }, true);

            scope.$watch('cartType', function(newVal, oldVal, scope) {
                //notification('success', newVal);
                timeCard.switchCartType(newVal);
            });

            scope.$watch('timeUnit', function(newVal, oldVal, scope) {
                //notification('success', newVal);
                timeCard.switchPeriodType(newVal.toUpperCase());
            });
        };

        return {
            restrict: 'A',
            scope: {
                timecardOptions: '=',
                cartType: '=',
                timeUnit: '='
            },
            link: link
        };
    });

angular
    .module('kai.reports.pcounting')
    .factory("PcountingService",
        function(KupOption, AmchartsTheme, UtilsService, PromiseFactory, KupApiService, ReportsService, AuthTokenFactory, $q, $timeout) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var ajaxPost = KupApiService.ajaxPost;
            var reports = ReportsService;
            var apiServerUrl = AuthTokenFactory.getApiRootUrl();

            var data = {
                //ui selector
                $chart: '#pcountingChart',
                $chartForPdf: '#pcountingChartForPdf',

                $posChart: '#pcountingPosChart',
                $posChartForPdf: '#pcountingPosChartForPdf',

                $timeCard: '#timeCardBox',
                //time format 
                timeFormat0: 'yyyy/MM/dd HH:mm:ss',
                timeFormat1: 'dd/MM/yyyy HH:mm:ss',
                timeFormat2: 'ddMMyyyyHHmmss',
                //selected data
                selectedGridList: {},
                selectedSitenameList: [],
                //selected chart data
                selectedChartDataSource: [],
                selectedChartDataSourceByTimeunit: [],
                selectedChartGraphs: [{
                    connect: true,
                    valueField: "count0"
                }],
                selectedChartSortByDate: [],
                filterEvents: [],
                //selected pos chart data
                selectedPosChartDataSource: [],
                selectedPosChartDataSourceByTimeunit: [],
                selectedPosChartValueAxes1: [],
                selectedPosChartValueAxes2: [],
                selectedPosChartGraphs1: [],
                selectedPosChartGraphs2: [],
                selectedPosDataList: [],

                //from api reponse data
                apiAnalyticsReportList: [],
                apiPosSalesReportList: [],
                //current info
                currentTab: 'chart', //value is tabData name
                currentTimeunitForChart: 'hours', //value is timeunitData name
                currentTimeunitForTimecard: 'weeks', //value is timeunitData name

                //other info
                //isPOSDataAvailable: false,
                posFakeData: [],
                cardType: "COUNTING", //vlaue is 'COUNTING','CONVERSION'
                //request List
                requestForReport: [],
                dataSourceByTimeunit: []
            };
            //UI setting
            var timeunitData = [{
                name: 'hours',
                text: 'hourly',
                isActiveForChart: true,
                isActiveForTimecard: false,
                isShowForChart: true,
                isShowForTimecard: false,
                chartPeriod: 'hh',
                //chartCategoryBalloonDateFormat: 'MMM DD, YYYY, JJ:NN',
            }, {
                name: 'days',
                text: 'daily',
                isActiveForChart: false,
                isActiveForTimecard: false,
                isShowForChart: true,
                isShowForTimecard: false,
                chartPeriod: 'DD',
                //chartCategoryBalloonDateFormat: 'MMM DD, YYYY',
            }, {
                name: 'weeks',
                text: 'weekly',
                isActiveForChart: false,
                isActiveForTimecard: true,
                isShowForChart: true,
                isShowForTimecard: true,
                chartPeriod: 'WW',
                // chartCategoryBalloonDateFormat: 'MMM DD, YYYY',
            }, {
                name: 'months',
                text: 'monthly',
                isActiveForChart: false,
                isActiveForTimecard: false,
                isShowForChart: true,
                isShowForTimecard: true,
                chartPeriod: 'MM',
                //chartCategoryBalloonDateFormat: 'MMM, YYYY',
            }, {
                name: 'years',
                text: 'yearly',
                isActiveForChart: false,
                isActiveForTimecard: false,
                isShowForChart: false,
                isShowForTimecard: true,
                chartPeriod: 'YYYY',
                //chartCategoryBalloonDateFormat: 'YYYY',
            }];

            var tabData = [{
                id: 'pcountingTabChart',
                name: 'chart',
                text: 'charts',
                isActive: true,
                isShow: true,
                className: "fa-bar-chart"
            }, {
                id: 'pcountingTabTimecard',
                name: 'timecard',
                text: 'time-card',
                isActive: false,
                isShow: true,
                className: "fa-clock-o"
            }];
            var exportData = {
                isOpen: false,
            };
            var posData = {
                isShow: true,
                isShowDemo: false,
            };
            var chartData = {
                options: {},
            };

            var chartForPdfData = {
                width: '900px',
                height: '400px',
                options: {},
            };

            var posChartData = {
                options: {},
            };

            var posChartForPdfData = {
                width: '900px',
                height: '600px',
                options: {},
            };

            var timecardData = {
                options: {},
            };

            // var timecardPOSData = {
            //     options: {},
            // };
            var gridData = {
                isShow: true,
                totalVisit: '...',
                avgVisit: '...',
                highestVisit: '...',
                lowestVisit: '...',
                leastEventBox: [],
                topEventBox: [],
                totalEventBox: [],
            };

            return {
                data: data,
                timeunitData: timeunitData,
                tabData: tabData,
                exportData: exportData,
                posData: posData,

                chartData: chartData,
                chartForPdfData: chartForPdfData,

                posChartData: posChartData,
                posChartForPdfData: posChartForPdfData,

                timecardData: timecardData,
                //timecardPOSData: timecardPOSData,
                gridData: gridData,

                setSelectedChartData: setSelectedChartData,
                setSelectedPosChartData: setSelectedPosChartData,
                setSelectedPosDataList: setSelectedPosDataList,

                setGridData: setGridData,
                setPosData: setPosData,
                setTimeunitData: setTimeunitData,
                setTimecardData: setTimecardData,
                setPosFakeData: setPosFakeData,
                setChartTimeunitLabel: setChartTimeunitLabel,
                setChartTheme: setChartTheme,

                setChartData: setChartData,
                setChartOptions: setChartOptions,
                setChartForPdfOptions: setChartForPdfOptions,

                setPosChartData: setPosChartData,
                setPosChartOptions: setPosChartOptions,
                setPosChartForPdfOptions: setPosChartForPdfOptions,

                getAnalyticsReportApi: getAnalyticsReportApi,
                getPosSalesReportApi: getPosSalesReportApi,
                exportSecurityCrossSiteCSVApi: exportSecurityCrossSiteCSVApi,
                exportAggregatedCsvReportApi: exportAggregatedCsvReportApi,
                exportPeopleCountingPdfApi: exportPeopleCountingPdfApi,

                generateReport: generateReport,
                generateChart: generateChart,
                generatePosChart: generatePosChart,
            };
            /*******************************************************************************
             *
             *  Function Definition
             *
             *******************************************************************************/
            function setSelectedChartData() {
                var opt = data;
                var reportsOpt = reports.data;
                var selectedItemDataList = reportsOpt.selectedItemDataList || {};
                var events = opt.apiAnalyticsReportList;
                var fromDate = reports.data.dateRange.startDate;
                var toDate = reports.data.dateRange.endDate;

                var seriesInfo = [];
                var eventDetails = [];
                var count = 0;
                var totalPlpVisit = 0;
                var countList = [];
                var filterEvents = [];
                var sitenameList = [];

                // opt.selectedGridList = [];
                // opt.selectedChartDataSource = [];
                // opt.selectedChartGraphs = [];

                //set base data source 
                (function() {
                    $.each(selectedItemDataList, function(uid, itemData) {
                        var totalInPerSelection = 0;
                        if ($.isEmptyObject(itemData)) {
                            return true;
                        }
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
                                            countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                            //countItem["deviceName" + count] = cameraData.deviceName;
                                            //countItem["channelName" + count] = cameraData.channelName;
                                            //countItem["name" + count] = name;
                                            countItem["count" + count] = parseInt(evt.in, 10);
                                            //countItem["value" + count] = parseInt(evt.in, 10);
                                            cameraList.push(countItem);

                                            totalPlpVisit += parseInt(evt.in, 10);
                                            totalInPerSelection += parseInt(evt.in, 10);
                                            filterEvents.push(evt);
                                        }
                                    });
                                    deviceList.push(cameraList);
                                });

                                $.each(deviceList, function(j, cameraList) {
                                    $.each(cameraList, function(j, camera) {
                                        var timeIndex = kendo.toString(camera.time, data.timeFormat2);
                                        if (deviceTimeList[timeIndex]) {
                                            deviceTimeList[timeIndex]["count" + count] += camera["count" + count];
                                            //deviceTimeList[timeIndex]["value" + count] += camera["value" + count];
                                        } else {
                                            deviceTimeList[timeIndex] = {};
                                            //deviceTimeList[timeIndex]["name" + count] = ["name" + count];
                                            //deviceTimeList[timeIndex]["deviceName" + count] = camera["deviceName" + count];
                                            deviceTimeList[timeIndex]["count" + count] = camera["count" + count];
                                            //deviceTimeList[timeIndex]["value" + count] = camera["value" + count];
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
                            var name = itemData.labelName + " - " + itemData.text;
                            var deviceList = [],
                                deviceTimeList = {};
                            $.each(itemData.items, function(i, camera) {
                                var cameraData = camera.data;
                                var cameraList = [];
                                $.each(events, function(j, evt) {
                                    if (evt.deviceId == cameraData.deviceId && evt.channelId == cameraData.channelId) {
                                        var countItem = {};
                                        countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                        //countItem["deviceName" + count] = cameraData.deviceName;
                                        //countItem["channelName" + count] = cameraData.channelName;
                                        //countItem["name" + count] = name;
                                        countItem["count" + count] = parseInt(evt.in, 10);
                                        //countItem["value" + count] = parseInt(evt.in, 10);
                                        cameraList.push(countItem);

                                        totalPlpVisit += parseInt(evt.in, 10);
                                        totalInPerSelection += parseInt(evt.in, 10);
                                        filterEvents.push(evt);
                                    }
                                });
                                deviceList.push(cameraList);
                            });
                            $.each(deviceList, function(i, cameraList) {
                                $.each(cameraList, function(j, camera) {
                                    var timeIndex = kendo.toString(camera.time, data.timeFormat2);
                                    if (deviceTimeList[timeIndex]) {
                                        deviceTimeList[timeIndex]["count" + count] += camera["count" + count];
                                        //deviceTimeList[timeIndex]["value" + count] += camera["value" + count];
                                    } else {
                                        deviceTimeList[timeIndex] = {};
                                        //deviceTimeList[timeIndex]["name" + count] = camera["name" + count];
                                        //deviceTimeList[timeIndex]["deviceName" + count] = camera["deviceName" + count];
                                        deviceTimeList[timeIndex]["count" + count] = camera["count" + count];
                                        //deviceTimeList[timeIndex]["value" + count] = camera["value" + count];
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
                                    countItem.time = utils.convertUTCtoLocal(kendo.parseDate(evt.date + " " + evt.hour + ":00:00", data.timeFormat0));
                                    // countItem["deviceName" + count] = cameraData.deviceName;
                                    //countItem["channelName" + count] = cameraData.channelName;
                                    //countItem["name" + count] = name;
                                    countItem["count" + count] = parseInt(evt.in, 10);
                                    //countItem["value" + count] = parseInt(evt.in, 10);
                                    totalPlpVisit += parseInt(evt.in, 10);
                                    totalInPerSelection += parseInt(evt.in, 10);
                                    countList.push(countItem);
                                    filterEvents.push(evt);
                                }
                            });
                        }

                        sitenameList.push(name);

                        eventDetails.push({
                            "uid": uid,
                            "totalPerSelection": totalInPerSelection,
                            "spriteCssClass": itemData.spriteCssClass
                        });

                        seriesInfo.push({
                            "id": "g" + count,
                            "valueAxis": "v" + count,
                            "valueField": "count" + count,
                            "type": "smoothedLine",
                            //"lineColor": data.chartLineColors[count],
                            "bullet": "round",
                            "bulletBorderThickness": 1,
                            "hideBulletsCount": 30,
                            "title": name,
                            "fillAlphas": 0,
                            "lineThickness": 2,
                            "balloonText": name + " :<b>[[value]]</b>",
                        });
                        count++;
                    });
                })();

                //set eventDetails;
                (function() {
                    $.each(reports.data.selectedItemDataList, function(uid, item) {
                        $.each(eventDetails, function(i, grid) {
                            if (grid.uid != uid) {
                                return true;
                            }
                            if (item.isLabel || item.isAll) {
                                grid.labelText = item.text;
                            }
                            if (item.isDevice) {
                                grid.labelText = item.labelName;
                                grid.deviceText = item.text;
                            }
                            if (item.isCamera) {
                                grid.labelText = item.labelName;
                                grid.deviceText = item.deviceName;
                                grid.cameraText = item.text;
                            }
                        });
                    });
                })();

                //set countList;
                // (function() {
                // var mergeCountList = {};
                // $.each(countList, function(i, ds) {
                //     var timeIndex = kendo.toString(ds.time, data.timeFormat2);
                //     if (mergeCountList[timeIndex]) {
                //         mergeCountList[timeIndex] = $.extend(true, mergeCountList[timeIndex], ds);
                //     } else {
                //         mergeCountList[timeIndex] = {};
                //         mergeCountList[timeIndex] = ds;
                //     }
                // });

                // $.each(mergeCountList, function(time, ds) {
                //     $.each(eventDetails, function(i, grid) {
                //         if (!ds['count' + i]) {
                //             ds['count' + i] = 0;
                //             ds['count' + i] = 0;
                //         }
                //     });
                // });

                // countList = [];
                // $.each(mergeCountList, function(time, ds) {
                //     countList.push(ds);
                // });
                // countList.sort(function(a, b) {
                //     return a.time - b.time;
                // });
                // })();

                opt.selectedGridList = eventDetails;
                opt.selectedChartDataSource = countList;
                opt.selectedChartGraphs = seriesInfo;
                opt.filterEvents = filterEvents;
                opt.selectedSitenameList = sitenameList;

                return opt;
            }

            function setSelectedPosDataList() {
                var sales = angular.copy(data.apiPosSalesReportList);

                data.selectedPosDataList = [];
                data.posFakeData = [];
                if (!sales.length) {
                    setPosFakeData();
                    $.each(data.posFakeData, function(i, salesData) {
                        //var localTime = kendo.parseDate(salesData.time, data.timeFormat1);
                        var localTime = salesData.time;
                        var obj = {
                            name: data.selectedSitenameList[0],
                            sales: {
                                time: kendo.toString(utils.convertToUTC(localTime), data.timeFormat1),
                                count: salesData.count,
                                amount: salesData.amount
                            }
                        }
                        sales.push(obj);
                    });
                }
                data.selectedPosDataList = sales;
            }

            function setSelectedPosChartData() {
                var countList = angular.copy(data.selectedChartDataSource);
                var sales = angular.copy(data.selectedPosDataList);
                var seriesInfo = [];
                var valueAxes = [];

                var dataSales = [];
                var dataCount = [];
                var dataList = [];
                var mergeDataList = {};

                //set dataList
                (function() {
                    //get dataCount
                    $.each(countList, function(i, cl) {
                        dataCount.push({
                            time: cl.time,
                            count: cl.count0
                        });
                    });
                    //set dataSales
                    $.each(sales, function(i, obj) {
                        dataSales.push({
                            time: utils.UTCToLocal(kendo.parseDate(obj.sales.time, data.timeFormat1)),
                            amount: obj.sales.amount,
                            receipt: obj.sales.count,
                        });
                    });

                    // get dataList  
                    $.each(dataCount, function(i, ds) {
                        var timeIndex = kendo.toString(ds.time, data.timeFormat2);
                        if (mergeDataList[timeIndex]) {
                            mergeDataList[timeIndex].time = ds.time;
                            mergeDataList[timeIndex].count += ds.count;
                            mergeDataList[timeIndex].amount = 0;
                            mergeDataList[timeIndex].receipt = 0;
                        } else {
                            mergeDataList[timeIndex] = {};
                            mergeDataList[timeIndex].time = ds.time;
                            mergeDataList[timeIndex].count = ds.count;
                            mergeDataList[timeIndex].amount = 0;
                            mergeDataList[timeIndex].receipt = 0;
                        }
                    });

                    $.each(dataSales, function(i, ds) {
                        var timeIndex = kendo.toString(ds.time, data.timeFormat2);
                        if (mergeDataList[timeIndex]) {
                            mergeDataList[timeIndex].count = mergeDataList[timeIndex].count;
                            mergeDataList[timeIndex].amount = (mergeDataList[timeIndex].amount || 0) + ds.amount;
                            mergeDataList[timeIndex].receipt = (mergeDataList[timeIndex].receipt || 0) + ds.receipt;
                            //mergeDataList[timeIndex] = $.extend(true, mergeDataList[timeIndex], ds);
                        } else {
                            mergeDataList[timeIndex] = {};
                            mergeDataList[timeIndex].time = ds.time;
                            mergeDataList[timeIndex].count = mergeDataList[timeIndex].count || 0;
                            mergeDataList[timeIndex].amount = ds.amount;
                            mergeDataList[timeIndex].receipt = ds.receipt;
                        }
                    });



                    $.each(mergeDataList, function(time, ds) {
                        ds.count = ds.count || 0;
                        ds.amount = ds.amount || 0;
                        ds.receipt = ds.receipt || 0;
                        ds.conversion = (ds.receipt > 0 && ds.count > 0) ? Math.round(ds.receipt / ds.count * 100) / 100 : 0;
                        dataList.push(ds);
                    });
                    dataList.sort(function(a, b) {
                        return a.time - b.time;
                    });
                })();

                //set valueAxes
                (function() {
                    valueAxes = [{
                        id: "v0",
                        position: "left",
                        title: i18n("no-people-in"),
                        //axisColor: data.chartLineColors[0],
                    }, {
                        id: "v1",
                        position: "right",
                        title: i18n("sales-amount"),
                        //axisColor: data.chartLineColors[1],
                    }, {
                        id: "v2",
                        position: "right",
                        title: i18n("receipt"),
                        //axisColor: data.chartLineColors[2],
                        offset: 50,
                    }, {
                        id: "v3",
                        position: "left",
                        title: i18n("conversion-rate"),
                        //axisColor: data.chartLineColors[3],
                    }];
                })();

                //set seriesInfo;
                (function() {
                    seriesInfo = [{
                        id: "g0",
                        valueAxis: "v0",
                        valueField: 'count',
                        type: "smoothedLine",
                        //lineColor: data.chartLineColors[0],
                        bullet: "round",
                        bulletBorderThickness: 1,
                        hideBulletsCount: 30,
                        title: i18n("no-people-in"),
                        fillAlphas: 0,
                        lineThickness: 2,
                        balloonText: i18n("no-people-in") + " :<b>[[value]]</b>",
                        useDataSetColors: false,
                    }, {
                        id: "g1",
                        valueAxis: "v1",
                        valueField: 'amount',
                        type: "smoothedLine",
                        //lineColor: data.chartLineColors[1],
                        bullet: "round",
                        bulletBorderThickness: 1,
                        hideBulletsCount: 30,
                        title: i18n("sales-amount"),
                        fillAlphas: 0,
                        lineThickness: 2,
                        balloonText: i18n("sales-amount") + " :<b>[[value]]</b>",
                        useDataSetColors: false,
                    }, {
                        id: "g2",
                        valueAxis: "v2",
                        valueField: 'receipt',
                        type: "smoothedLine",
                        //lineColor: data.chartLineColors[2],
                        bullet: "round",
                        bulletBorderThickness: 1,
                        hideBulletsCount: 30,
                        title: i18n("receipt"),
                        fillAlphas: 0,
                        lineThickness: 2,
                        balloonText: i18n("receipt") + " :<b>[[value]]</b>",
                        useDataSetColors: false,
                    }, {
                        id: "g3",
                        valueAxis: "v3",
                        valueField: "conversion",
                        type: "column",
                        cornerRadiusTop: 2,
                        //lineColor: data.chartLineColors[3],
                        title: i18n("conversion-rate"),
                        fillAlphas: 1,
                        balloonText: i18n("conversion-rate") + ":<b>[[value]]</b>",
                        useDataSetColors: false,
                    }];

                })();

                data.selectedPosChartDataSource = dataList;
                data.selectedPosChartValueAxes1 = [valueAxes[0], valueAxes[1], valueAxes[2]];
                data.selectedPosChartValueAxes2 = [valueAxes[3]];
                data.selectedPosChartGraphs1 = [seriesInfo[0], seriesInfo[1], seriesInfo[2]];
                data.selectedPosChartGraphs2 = [seriesInfo[3]];
                return data;
            }

            function setPosData() {
                var selectedSaveDataList = reports.data.selectedSaveDataList;
                var selectedGroupNames = reports.data.selectedGroupNames;
                var selectedItemDataList = reports.data.selectedItemDataList;
                posData.isShow = (selectedGroupNames.length == 1 && selectedSaveDataList.length == 1) ? true : false;
                posData.isShowDemo = (function() {
                    var check = false;
                    var count = 0;
                    var countLabel = 0;
                    $.each(selectedItemDataList, function(uid, item) {
                        if ($.isEmptyObject(item)) {
                            return true;
                        }
                        if (item.isLabel) {
                            countLabel++;
                        }
                        count++;
                    });

                    check = (count === 1 && countLabel === 1) ? true : false;
                    return check;
                })();
            }

            function setTimeunitData() {
                //var reportsOpt = reports.data;
                //var selectedItemDataList = reportsOpt.selectedItemDataList || {};
                // var events = data.apiAnalyticsReportList;
                var fromDate = reports.data.dateRange.startDate;
                var toDate = reports.data.dateRange.endDate;
                var dateDiff = utils.getDateDifference(fromDate, toDate);

                //hide and not active all timeunit
                $.each(timeunitData, function(i, unit) {
                    //unit.isShowForChart = true;
                    //unit.isShowForTimecard = false;

                    unit.isActiveForChart = false;
                    unit.isActiveForTimecard = false;
                });


                //default least unit to active chart
                $.each(timeunitData, function(i, unit) {
                    if (unit.isShowForChart) {
                        unit.isActiveForChart = true;
                        return false;
                    }
                });
                //default least unit to active timecard
                $.each(timeunitData, function(i, unit) {
                    if (unit.isShowForTimecard) {
                        unit.isActiveForTimecard = true;
                        return false;
                    }
                });

                //default least unit to current chart timeunit 
                $.each(timeunitData, function(i, unit) {
                    if (unit.isActiveForChart) {
                        data.currentTimeunitForChart = unit.name;
                        return false;
                    }
                });
                //default least unit to current timecard timeunit 
                $.each(timeunitData, function(i, unit) {
                    if (unit.isActiveForTimecard) {
                        data.currentTimeunitForTimecard = unit.name;
                        return false;
                    }
                });
            }

            function setGridData() {
                var opt = data;
                var totalVisits = (function() {
                    var count = 0;
                    $.each(opt.selectedGridList, function(i, grid) {
                        count += grid.totalPerSelection;
                    });
                    return count;
                })();
                var selectedDeviceLength = opt.selectedGridList.length;

                var dataItems = angular.copy(opt.selectedChartDataSource);
                var selectedChartSortByDate = angular.copy(opt.selectedChartDataSourceByTimeunit);

                //set selectedChartSortByDate
                (function() {
                    $.each(selectedChartSortByDate, function(i, ds) {
                        ds.aggregate = (function() {
                            var count = 0;
                            for (var j = 0; j < selectedDeviceLength; j++) {
                                ds['count' + j] = ds['count' + j] || 0;
                                count += ds['count' + j];
                            }
                            return count;
                        })();
                    });

                    selectedChartSortByDate.sort(function(a, b) {
                        return b.aggregate - a.aggregate;
                    });

                    opt.selectedChartSortByDate = selectedChartSortByDate;
                })();

                //set grid data
                (function() {
                    var highestInfo = angular.copy(opt.selectedChartSortByDate).shift();
                    var lowestInfo = (function() {
                        var ds = angular.copy(opt.selectedChartSortByDate);
                        var lowestInfo = {};
                        $.each(ds, function(i, info) {
                            if (info.aggregate) {
                                lowestInfo = info;
                            }
                            if (!info.aggregate) {
                                return false;
                            }
                        });
                        return lowestInfo;
                    })();

                    gridData.topEventBox = (function() {
                        var data = angular.copy(opt.selectedGridList);
                        return data.sort(function(a, b) {
                            return b.totalPerSelection - a.totalPerSelection;
                        });
                    })();

                    gridData.leastEventBox = (function() {
                        var data = angular.copy(opt.selectedGridList);
                        return data.sort(function(a, b) {
                            return a.totalPerSelection - b.totalPerSelection;
                        });
                    })();

                    gridData.totalEventBox = (function() {
                        var data = angular.copy(opt.selectedGridList);
                        return data;
                    })();

                    gridData.totalVisit = kendo.toString(totalVisits, "n0");
                    gridData.avgVisit = kendo.toString(Math.round(totalVisits / selectedDeviceLength), "n0");

                    gridData.highestVisit = (highestInfo) ? kendo.toString(highestInfo.aggregate, "n0") : 0;
                    gridData.lowestVisit = (lowestInfo) ? kendo.toString(lowestInfo.aggregate, "n0") : 0;

                    gridData.highestVisitDate = (function() {
                        var returnData = '';
                        //var highestDate = angular.copy(opt.selectedChartSortByDate).shift().time;
                        returnData = (highestInfo) ? i18n('on') + " " + setChartTimeunitLabel(highestInfo.time) : '...';
                        return returnData;
                    })();

                    gridData.lowestVisitDate = (function() {
                        var returnData = '';
                        //var lowestDate = angular.copy(opt.selectedChartSortByDate).pop().time;
                        returnData = (lowestInfo) ? i18n('on') + " " + setChartTimeunitLabel(lowestInfo.time) : '...';
                        return returnData;
                    })();
                })();
                return gridData;
            }

            function setChartData() {
                var timeunit = data.currentTimeunitForChart;
                var dataSource = angular.copy(data.selectedChartDataSource);
                var selectedItemLength = reports.data.selectedSaveDataList.length;
                var fromDate = reports.data.dateRange.startDate;
                var toDate = reports.data.dateRange.endDate;
                var dataTimeList = {};
                var dataSourceByTimeunit = [];

                //set dataSourceByTimeunit
                (function() {

                    var tmp = {};
                    for (var i = 0; i < selectedItemLength; i++) {
                        tmp['count' + i] = 0;
                    }

                    if (timeunit == 'hours') {
                        var start = moment(fromDate);
                        var end = moment(toDate);
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
                        var start = moment(fromDate);
                        var end = moment(toDate);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "days")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = new Date(i);
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'days');
                            for (var j = 0; j < selectedItemLength; j++) {
                                if (ds["count" + j] != undefined) {
                                    countList[index]["count" + j] += ds["count" + j];
                                }
                            }
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'weeks') {
                        var weekStart = kupOpt.dateOfWeekStart;
                        var start = moment(fromDate).isoWeekday(weekStart);
                        var end = moment(toDate).isoWeekday(weekStart);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "weeks")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = new Date(i);
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'weeks');
                            for (var j = 0; j < selectedItemLength; j++) {
                                if (ds["count" + j] != undefined) {
                                    countList[index]["count" + j] += ds["count" + j];
                                }
                            }
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'months') {
                        var start = moment(fromDate).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var end = moment(toDate).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "month")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = i.format();
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'months');
                            for (var j = 0; j < selectedItemLength; j++) {
                                if (ds["count" + j] != undefined) {
                                    countList[index]["count" + j] += ds["count" + j];
                                }
                            }
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'years') {
                        var start = moment(fromDate).month(0).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var end = moment(toDate).month(0).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "years")) {
                            var index = countList.length;
                            countList[index] = {};
                            countList[index] = angular.copy(tmp);
                            countList[index].time = i.format();
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'year');
                            for (var j = 0; j < selectedItemLength; j++) {
                                if (ds["count" + j] != undefined) {
                                    countList[index]["count" + j] += ds["count" + j];
                                }
                            }
                        });
                        dataSourceByTimeunit = countList;
                    }
                })();

                (function() {
                    $.each(dataSourceByTimeunit, function(i, ds) {
                        if (typeof ds.time === 'string') {
                            ds.time = moment(ds.time).toDate();
                        }
                    });
                })();


                data.selectedChartDataSourceByTimeunit = dataSourceByTimeunit;
                return dataSourceByTimeunit;
            }

            function setPosChartData() {
                var timeunit = data.currentTimeunitForChart;
                var dataSource = angular.copy(data.selectedPosChartDataSource);
                var selectedItemLength = reports.data.selectedSaveDataList.length;
                var dataTimeList = {};
                var dataSourceByTimeunit = [];

                //set dataSourceByTimeunit
                (function() {
                    if (timeunit == 'hours') {
                        var countList = [];
                        var start = moment(reports.data.dateRange.startDate);
                        var end = moment(reports.data.dateRange.endDate);
                        for (var i = start; i <= end; i = moment(i).add(1, "hours")) {
                            countList.push({
                                time: i.format(),
                                count: 0,
                                amount: 0,
                                receipt: 0
                            });
                        }
                        $.each(dataSource, function(index, ds) {
                            var index = moment(ds.time).diff(start, 'hours');
                            countList[index] = ds;
                            countList[index]["conversion"] = (ds.receipt > 0 && ds.count > 0) ? Math.round(ds.receipt / ds.count * 100) / 100 : 0;
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'days') {
                        var countList = [];
                        var start = moment(reports.data.dateRange.startDate);
                        var end = moment(reports.data.dateRange.endDate);
                        for (var i = start; i <= end; i = moment(i).add(1, "days")) {
                            countList.push({
                                time: i.format(),
                                count: 0,
                                amount: 0,
                                receipt: 0
                            });
                        }

                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'days');
                            countList[index]["count"] += ds["count"];
                            countList[index]["amount"] += ds["amount"];
                            countList[index]["receipt"] += ds["receipt"];
                            countList[index]["conversion"] = (countList[index]["receipt"] > 0 && countList[index]["count"] > 0) ? Math.round(countList[index]["receipt"] / countList[index]["count"] * 100) / 100 : 0;
                        });
                        dataSourceByTimeunit = countList;

                    }
                    if (timeunit == 'weeks') {
                        var weekStart = kupOpt.dateOfWeekStart;
                        var start = moment(reports.data.dateRange.startDate).isoWeekday(weekStart);
                        var end = moment(reports.data.dateRange.endDate).isoWeekday(weekStart);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "weeks")) {
                            countList.push({
                                time: i.format(),
                                count: 0,
                                amount: 0,
                                receipt: 0
                            });
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'weeks');
                            countList[index]["count"] += ds["count"];
                            countList[index]["amount"] += ds["amount"];
                            countList[index]["receipt"] += ds["receipt"];
                            countList[index]["conversion"] = (countList[index]["receipt"] > 0 && countList[index]["count"] > 0) ? Math.round(countList[index]["receipt"] / countList[index]["count"] * 100) / 100 : 0;
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'months') {
                        var start = moment(reports.data.dateRange.startDate).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var end = moment(reports.data.dateRange.endDate).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "month")) {
                            countList.push({
                                time: i.format(),
                                count: 0,
                                amount: 0,
                                receipt: 0
                            });
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'month');
                            countList[index]["count"] += ds["count"];
                            countList[index]["amount"] += ds["amount"];
                            countList[index]["receipt"] += ds["receipt"];
                            countList[index]["conversion"] = (countList[index]["receipt"] > 0 && countList[index]["count"] > 0) ? Math.round(countList[index]["receipt"] / countList[index]["count"] * 100) / 100 : 0;
                        });
                        dataSourceByTimeunit = countList;
                    }
                    if (timeunit == 'years') {
                        var start = moment(reports.data.dateRange.startDate).month(0).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var end = moment(reports.data.dateRange.endDate).month(0).date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
                        var countList = [];
                        for (var i = start; i <= end; i = moment(i).add(1, "years")) {
                            countList.push({
                                time: i.format(),
                                count: 0,
                                amount: 0,
                                receipt: 0
                            });
                        }
                        $.each(dataSource, function(i, ds) {
                            var index = moment(ds.time).diff(start, 'years');
                            countList[index]["count"] += ds["count"];
                            countList[index]["amount"] += ds["amount"];
                            countList[index]["receipt"] += ds["receipt"];
                            countList[index]["conversion"] = (countList[index]["receipt"] > 0 && countList[index]["count"] > 0) ? Math.round(countList[index]["receipt"] / countList[index]["count"] * 100) / 100 : 0;
                        });
                        dataSourceByTimeunit = countList;
                    }
                })();

                (function() {
                    $.each(dataSourceByTimeunit, function(i, ds) {
                        if (typeof ds.time === 'string') {
                            ds.time = moment(ds.time).toDate();
                        }
                    });
                })();

                data.selectedPosChartDataSourceByTimeunit = dataSourceByTimeunit;
                return dataSourceByTimeunit;
            }

            function setChartOptions() {
                var chartSetting = (function() {
                    var setting = {};
                    $.each(timeunitData, function(i, unit) {
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
                    // "export": {
                    //     "enabled": true,
                    //     "position": "bottom-right"
                    // }
                };

                chartData.options = chartOptions;
                return chartOptions;
            }

            function setChartForPdfOptions() {
                var chartSetting = (function() {
                    var setting = {};
                    $.each(timeunitData, function(i, unit) {
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
                    theme: 'white',
                    //fontFamily: 'Verdana',
                    //fontSize: 11,
                    zoomOutText: i18n('show-all'),
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
                        minPeriod: chartSetting.minPeriod,
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

                chartForPdfData.options = chartOptions;
                return chartForPdfData;
            }

            function setPosChartOptions() {
                var chartSetting = (function() {
                    var setting = {};
                    $.each(timeunitData, function(i, unit) {
                        if (unit.name === data.currentTimeunitForChart) {
                            setting.minPeriod = unit.chartPeriod;
                            return false;
                        }
                    });
                    return setting;
                })();

                var posChartOptions = {
                    //ui setting
                    type: "stock",
                    theme: AuthTokenFactory.getTheme(),
                    firstDayOfWeek: kupOpt.dateOfWeekStart,
                    //pathToImages: "http://www.amcharts.com/lib/3/images/",
                    dataSets: [{
                        fieldMappings: [{
                            fromField: "count",
                            toField: "count"
                        }, {
                            fromField: "amount",
                            toField: "amount"
                        }, {
                            fromField: "receipt",
                            toField: "receipt"
                        }, {
                            fromField: "conversion",
                            toField: "conversion"
                        }],
                        dataProvider: angular.copy(data.selectedPosChartDataSourceByTimeunit),
                        categoryField: "time"
                    }],

                    categoryAxesSettings: {
                        minPeriod: chartSetting.minPeriod,
                        equalSpacing: true,
                    },

                    valueAxesSettings: {
                        inside: true,
                        axisAlpha: 1,
                        tickLength: 5,
                        gridAlpha: 0,
                        axisThickness: 2
                    },

                    chartScrollbarSettings: {
                        graph: "g3",
                        usePeriod: chartSetting.minPeriod,
                        position: "top"
                    },

                    chartCursorSettings: {
                        cursorPosition: 'mouse',
                        valueBalloonsEnabled: true,

                    },
                    panelsSettings: {
                        usePrefixes: true,
                        marginLeft: 50,
                        marginRight: 100
                    },

                    panels: [{
                        showCategoryAxis: false,
                        //title: "Value",
                        percentHeight: 60,
                        valueAxes: angular.copy(data.selectedPosChartValueAxes1),
                        stockGraphs: angular.copy(data.selectedPosChartGraphs1),
                        stockLegend: {
                            periodValueTextComparing: "[[percents.value.close]]%",
                            periodValueTextRegular: "[[value.close]]",
                            //valueTextRegular: " ",
                            //markerType: "none"
                        }
                    }, {
                        //title: i18n("conversion-rate"),
                        percentHeight: 40,
                        valueAxes: angular.copy(data.selectedPosChartValueAxes2),
                        stockGraphs: angular.copy(data.selectedPosChartGraphs2),
                        stockLegend: {
                            periodValueTextComparing: "[[percents.value.close]]%",
                            periodValueTextRegular: "[[value.close]]",
                            //valueTextRegular: " ",
                            //markerType: "none"
                        },
                        chartCursor: {
                            categoryBalloonFunction: function() {
                                var time = arguments[0];
                                return setChartTimeunitLabel(time);
                            }
                        },
                        categoryAxis: {
                            markPeriodChange: true,
                            boldLabels: true,
                            minPeriod: chartSetting.minPeriod,
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
                    }],
                };
                posChartData.options = posChartOptions;
                return posChartOptions;
            }

            function setPosChartForPdfOptions() {
                var chartSetting = (function() {
                    var setting = {};
                    $.each(timeunitData, function(i, unit) {
                        if (unit.name === data.currentTimeunitForChart) {
                            setting.minPeriod = unit.chartPeriod;
                            return false;
                        }
                    });
                    return setting;
                })();

                var posChartOptions = {
                    //ui setting
                    type: "stock",
                    theme: 'white',
                    //pathToImages: "http://www.amcharts.com/lib/3/images/",
                    dataSets: [{
                        fieldMappings: [{
                            fromField: "count",
                            toField: "count"
                        }, {
                            fromField: "amount",
                            toField: "amount"
                        }, {
                            fromField: "receipt",
                            toField: "receipt"
                        }, {
                            fromField: "conversion",
                            toField: "conversion"
                        }],
                        dataProvider: angular.copy(data.selectedPosChartDataSourceByTimeunit),
                        categoryField: "time"
                    }],

                    categoryAxesSettings: {
                        minPeriod: chartSetting.minPeriod,
                        equalSpacing: true,
                    },

                    valueAxesSettings: {
                        inside: true,
                        axisAlpha: 1,
                        tickLength: 5,
                        gridAlpha: 0,
                        axisThickness: 2
                    },

                    chartScrollbarSettings: {
                        graph: "g3",
                        usePeriod: chartSetting.minPeriod,
                        position: "top"
                    },

                    chartCursorSettings: {
                        cursorPosition: 'mouse',
                        valueBalloonsEnabled: true,

                    },
                    panelsSettings: {
                        usePrefixes: true,
                        marginLeft: 50,
                        marginRight: 100
                    },

                    panels: [{
                        showCategoryAxis: false,
                        //title: "Value",
                        percentHeight: 60,
                        valueAxes: angular.copy(data.selectedPosChartValueAxes1),
                        stockGraphs: angular.copy(data.selectedPosChartGraphs1),
                        stockLegend: {
                            periodValueTextComparing: "[[percents.value.close]]%",
                            periodValueTextRegular: "[[value.close]]",
                            //valueTextRegular: " ",
                            //markerType: "none"
                        }
                    }, {
                        //title: i18n("conversion-rate"),
                        percentHeight: 40,
                        valueAxes: angular.copy(data.selectedPosChartValueAxes2),
                        stockGraphs: angular.copy(data.selectedPosChartGraphs2),
                        stockLegend: {
                            periodValueTextComparing: "[[percents.value.close]]%",
                            periodValueTextRegular: "[[value.close]]",
                            //valueTextRegular: " ",
                            //markerType: "none"
                        },
                        chartCursor: {
                            categoryBalloonFunction: function() {
                                var time = arguments[0];
                                return setChartTimeunitLabel(time);
                            }
                        },
                        categoryAxis: {
                            markPeriodChange: true,
                            boldLabels: true,
                            minPeriod: chartSetting.minPeriod,
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
                    }],
                };
                posChartForPdfData.options = posChartOptions;
                return posChartForPdfData;
            }

            function setTimecardData(isShowDemo) {
                posData.isShowDemo = isShowDemo;
                timecardData.options.peopleCountingData = data.filterEvents;
                timecardData.options.posData = (isShowDemo) ? data.selectedPosDataList : [];
                return timecardData;
            }

            // function setPosTimecardData() {
            //     posData.isShowDemo = true;
            //     timecardPOSData.options.peopleCountingData = data.filterEvents;
            //     timecardPOSData.options.posData = data.selectedPosDataList;
            //     return timecardPOSData;
            // }

            function setPosFakeData() {
                var countList = angular.copy(data.selectedChartDataSource);
                var posFakeData = [];

                $.each(countList, function(index, data) {
                    var randomConversionRate = utils.getRandomDecimal(0.1, 0.4);
                    var POSData = {
                        time: kendo.toString(data.time, data.timeFormat1),
                        count: data['count0'] ? parseInt(randomConversionRate * data['count0'], 10) : 0,
                        amount: data['count0'] ? data['count0'] * utils.getRandomInteger(0, 100) : 0
                    }
                    posFakeData.push(POSData);
                });
                data.posFakeData = posFakeData;
                return posFakeData;
            }

            function getPosSalesReportApi(isDefer) {
                var opt = data;
                var reportsOpt = reports.data;

                var groupNames = reports.data.selectedGroupNames;
                var siteName = $.isArray(groupNames) && groupNames.length > 0 ? groupNames[0] : "";

                var fromDate = reports.data.dateRange.startDate;
                var toDate = reports.data.dateRange.endDate;

                var UTCFrom = utils.convertToUTC(fromDate);
                var UTCTo = utils.convertToUTC(toDate);
                var fromStr = kendo.toString(UTCFrom, data.timeFormat2);
                var toStr = kendo.toString(UTCTo, data.timeFormat2);

                var param = {
                    "from": fromStr,
                    "to": toStr,
                    "name": siteName,
                    "parser-type": '',
                };

                var onSuccess = function(response) {
                    opt.apiPosSalesReportList = response.sales || [];
                };
                var onFail = function(response) {
                    opt.apiPosSalesReportList = [];
                };
                var onError = function() {
                    opt.apiPosSalesReportList = [];
                };
                return ajaxPost('getpossalesreport', param, onSuccess, onFail, onError, isDefer);
            }

            function getAnalyticsReportApi(isDefer) {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;

                var selectedDeviceList = reportsOpt.selectedDevices[0].platformDeviceId;
                var selectedChannelList = reportsOpt.selectedDevices[0].channelId;

                var fromDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.startDate), data.timeFormat2);
                var toDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.endDate), data.timeFormat2);

                var param = {
                    "event-type": vcaEventType,
                    "device-id": JSON.stringify(selectedDeviceList),
                    "channel-id": "",
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

            function exportSecurityCrossSiteCSVApi() {
                var opt = data;
                var reportsOpt = reports.data;

                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;
                var dvcId = reportsOpt.selectedInstance;
                var channId = reportsOpt.selectedDevices;
                var groupNames = reportsOpt.selectedGroupNames;
                var posNames = reportsOpt.apiPosNamesList;
                var fromDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.startDate), opt.timeFormat2);
                var toDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.endDate), opt.timeFormat2);
                var baseUnit = opt.currentTimeunitForChart;
                var siteName = $.isArray(groupNames) && groupNames.length > 0 ? groupNames[0] : "";
                var devices = (function() {
                    var devices = [];
                    $.each(dvcId, function(i, dv) {
                        if (devices.length < 1) {
                            devices.push([dv.platformDeviceId + "-" + dv.channelId]);
                        } else {
                            var flag = true;
                            $.each(devices, function(i, t) {
                                if ($.inArray(dv.platformDeviceId + "-" + dv.channelId, t) === 0) {
                                    flag = false;
                                    return false;
                                }
                            });
                            if (flag) {
                                devices.push([dv.platformDeviceId + "-" + dv.channelId]);
                            }
                        }
                    });
                    return devices;
                })();
                var totalVisits = (function() {
                    var count = 0;
                    $.each(opt.selectedGridList, function(i, grid) {
                        count += grid.totalPerSelection;
                    });
                    return count;
                })();
                var reportInfo = {
                    "event-type": vcaEventType,
                    "site-name": opt.selectedSitenameList.toString(),
                    "from": kendo.toString(reportsOpt.dateRange.startDate, opt.timeFormat1),
                    "to": kendo.toString(reportsOpt.dateRange.endDate, opt.timeFormat1),
                    "total-results": totalVisits + ""
                };


                var param = {
                    "device-id": devices.toString(),
                    "time-zone-offset": KupApiService.data.timeZoneOffset,
                    "from": fromDateUTC,
                    "to": toDateUTC,
                    "base-unit": baseUnit,
                    "report-info": JSON.stringify(reportInfo),
                    "sitename": siteName
                };

                var onSuccess = function(response) {
                    if (response["download-url"] != null) {
                        var openUrl = apiServerUrl + response["download-url"];
                        window.open(openUrl, '_blank');
                        window.focus();
                    }
                };
                var onFail = function(response) {};
                var onError = function() {};
                return ajaxPost('exportsecuritycrosssitecsv', param, onSuccess, onFail, onError);
            }

            function exportAggregatedCsvReportApi(periodType) {
                var opt = data;
                var reportsOpt = reports.data;

                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;
                var fromDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.startDate), opt.timeFormat2);
                var toDateUTC = kendo.toString(utils.convertToUTC(reportsOpt.dateRange.endDate), opt.timeFormat2);
                var baseUnit = periodType;
                var selectedGroups = [];
                var type = "";
                $.each(reportsOpt.selectedItemDataList, function(uid, itemData) {
                    var devices = [];
                    if ($.isEmptyObject(itemData)) {
                        return true;
                    }
                    if (itemData.isAll || itemData.isLabel) { //drag root or labels
                        var device = itemData.items || [];
                        $.each(device, function(i, deviceData) {
                            var camera = deviceData.items || [];
                            $.each(camera, function(j, cameraData) {
                                devices.push({
                                    "coreDeviceId": cameraData.data.platformDeviceId + "",
                                    "channelId": cameraData.data.channelId
                                });
                            });
                        });
                        type = 'labels';
                    } else if (itemData.isDevice) { //drag device
                        var camera = itemData.items || [];
                        $.each(camera, function(i, cameraData) {
                            devices.push({
                                "coreDeviceId": cameraData.data.platformDeviceId + "",
                                "channelId": cameraData.data.channelId
                            });
                        });
                        type = 'devices';
                    } else if (itemData.isCamera) { //drag camera
                        if (itemData.hasChildren) {
                            return;
                        }
                        var cameraData = itemData;
                        devices.push({
                            "coreDeviceId": cameraData.data.platformDeviceId + "",
                            "channelId": cameraData.data.channelId
                        });
                        type = 'devices';
                    }

                    //set selectedGroups 
                    (function() {
                        var groupname = "";
                        if (itemData.isLabel || itemData.isAll) {
                            groupname = itemData.labelName;
                        }
                        if (itemData.isDevice) {
                            groupname = itemData.labelName + " - " + itemData.text;
                        }
                        if (itemData.isCamera) {
                            groupname = itemData.labelName + " - " + itemData.deviceName + " - " + itemData.text;
                        }

                        selectedGroups.push({
                            "groupName": groupname,
                            "devicePairs": devices,
                            "type": type
                        });
                    })();
                });

                var param = {
                    "event-type": vcaEventType,
                    "selected-groups": JSON.stringify(selectedGroups),
                    "time-zone-offset": KupApiService.data.timeZoneOffset,
                    "from": fromDateUTC,
                    "to": toDateUTC,
                    "base-unit": baseUnit
                };

                return KupApiService.exportDoc(param, 'exportaggregatedcsvreport');
            }

            function exportPeopleCountingPdfApi() {
                var opt = data;
                var reportsOpt = reports.data;
                var vcaEventType = kupOpt.vca[reportsOpt.reportType].eventType;
                var svgTitle = "<svg version='1.1' width='900' height='30'><text y='15' x='0' transform='translate(450)' text-anchor='middle' font-size='20' font-weight='bold'>" + i18n('vistors-vs-time') + "</text></svg>";

                //get svg 
                var svgData = (function() {
                    var svg = '';
                    var svgData = [];
                    var chartInfo = {};

                    svgData.push(svgTitle);
                    if (opt.currentTab == 'chart') {
                        if (posData.isShowDemo) {
                            var chartOpt = setPosChartForPdfOptions().options;
                            var chartPdf = window.AmCharts.makeChart(opt.$posChartForPdf.slice(1), chartOpt);
                            $.each($(opt.$posChartForPdf).find('svg'), function(i, el) {
                                svgData.push("<svg version='1.1' width='" + $(el).css('width') + "' height='" + $(el).css('height') + "'>" + $(el).html() + "</svg>");
                            });
                        }
                        if (!posData.isShowDemo) {
                            var chartOpt = setChartForPdfOptions().options;
                            var chartPdf = window.AmCharts.makeChart(opt.$chartForPdf.slice(1), chartOpt);
                            $.each($(opt.$chartForPdf).find('svg'), function(i, el) {
                                svgData.push("<svg version='1.1' width='" + $(el).css('width') + "' height='" + $(el).css('height') + "'>" + $(el).html() + "</svg>");
                            });
                        }

                    }
                    if (opt.currentTab == 'timecard') {
                        $.each($(opt.$timeCard), function(i, el) {
                            svgData.push($(el).html());
                        });
                    }

                    return svgData;
                })();

                var totalVisits = (function() {
                    var count = 0;
                    $.each(opt.selectedGridList, function(i, grid) {
                        count += grid.totalPerSelection;
                    });
                    return count;
                })();

                var reportInfo = {
                    "event-type": vcaEventType,
                    "site-name": opt.selectedSitenameList.toString(),
                    "from": kendo.toString(reportsOpt.dateRange.startDate, opt.timeFormat1),
                    "to": kendo.toString(reportsOpt.dateRange.endDate, opt.timeFormat1),
                    "total-results": totalVisits + ""
                };

                var param = {
                    "time-zone-offset": KupApiService.data.timeZoneOffset,
                    "svg-string": svgData.join(""),
                    "report-info": JSON.stringify(reportInfo),
                };

                return KupApiService.exportDoc(param, 'exportpeoplecountingpdf');
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
                    dateText = moment(time).format(kupOpt.default.formatHourly);
                    // dateText = time.toLocaleDateString(dateLang, dateOpt) + ", " + time.getHours() + ":00";
                }
                if (data.currentTimeunitForChart == 'days') {
                    dateText = moment(time).format(kupOpt.default.formatDaily);
                    // dateText = time.toLocaleDateString(dateLang, dateOpt);
                }
                if (data.currentTimeunitForChart == 'weeks') {
                    dateText = moment(time).startOf('week').isoWeekday(kupOpt.default.isoWeekStart).format(kupOpt.default.formatWeekly) + " - \n" + moment(time).endOf('week').format(kupOpt.default.formatWeekly);
                    // var getTimeStart = (time.getDay() < weekStart) ?
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() - (7 - weekStart - time.getDay())) :
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() - (time.getDay() - weekStart));
                    // var getTimeEnd = (time.getDay() < weekStart) ?
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() + (weekStart - time.getDay() - 1)) :
                    //     new Date(time.getFullYear(), time.getMonth(), time.getDate() + (6 - (time.getDay() - weekStart)));
                    // dateText = getTimeStart.toLocaleDateString(dateLang, dateOpt) + " - " + getTimeEnd.toLocaleDateString(dateLang, dateOpt);
                }
                if (data.currentTimeunitForChart == 'months') {
                    dateText = moment(time).format(kupOpt.default.formatMonthly);
                    // dateOpt = {
                    //     month: "short",
                    //     year: "numeric"
                    // };
                    // dateText = time.toLocaleDateString(dateLang, dateOpt);
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
                setGridData();
            }

            function generatePosChart() {
                setChartTheme();
                setPosChartData();
                setPosChartOptions();
            }

            function generateReport() {
                var opt = data;
                var reportsOpt = reports.data;

                opt.cardType = "COUNTING";
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

                //set generate report promise
                opt.requestForReport = [
                    getAnalyticsReportApi(true),
                    getPosSalesReportApi(true)
                ];

                //return promise
                var dfd = $q.defer();
                $timeout(function() {
                    $q.all(opt.requestForReport)
                        .finally(function() {
                            setSelectedChartData();
                            setSelectedPosDataList();
                            setSelectedPosChartData();
                            setPosData();
                            setTimeunitData();
                            if (reportsOpt.selectedDevices.length > 1) {
                                $timeout(function() {
                                    angular.element('#pcountingTabChart').triggerHandler('click');
                                });
                                tabData[1].isShow = false;
                            }
                            if (reportsOpt.selectedDevices.length <= 1) {
                                tabData[1].isShow = true;
                            }
                            if (opt.selectedChartDataSource.length <= 0) {
                                reports.isSuccessReport(false);
                                dfd.reject();
                                return;
                            }
                            reports.isSuccessReport(true);
                            generateChart();
                            generatePosChart();
                            setTimecardData(posData.isShowDemo);
                            dfd.resolve();
                        });
                }, 500);
                return dfd.promise;
            }
        });

angular
    .module('kai.reports.pcounting')
    .controller('PcountingController',
        function(
            KupOption,
            RouterStateService, UtilsService, PromiseFactory, AuthTokenFactory,
            ReportsService, PcountingService,
            $scope, $timeout, $q, $rootScope
        ) {
            var kupOpt = KupOption;
            var utils = UtilsService;
            var i18n = UtilsService.i18n;
            var notification = UtilsService.notification;
            var reports = ReportsService;
            var pcountingCtrl = this;

            //UI controller
            pcountingCtrl.data = PcountingService.data;
            pcountingCtrl.fn = {
                setTab: setTab,
                setTimeunitForChart: setTimeunitForChart,
                setTimeunitForTimecard: setTimeunitForTimecard,
                setPosData: setPosData,
                exportPdf: exportPdf,
                exportCsv: exportCsv,
                downloadPosFile: downloadPosFile,
            };
            // $scope.$on('pos:available', function(event, isPOSDataAvailable) {
            //     // you could inspect the data to see if what you care about changed, or just update your own scope
            //     setPosData(isPOSDataAvailable);
            // });

            pcountingCtrl.isPOSDataAvailable = PcountingService.data.isPOSDataAvailable;
            pcountingCtrl.tab = {};
            pcountingCtrl.tab.data = PcountingService.tabData;

            pcountingCtrl.timeunit = {};
            pcountingCtrl.timeunit.data = PcountingService.timeunitData;

            pcountingCtrl.export = {};
            pcountingCtrl.export.data = PcountingService.exportData;

            pcountingCtrl.pos = {};
            pcountingCtrl.pos.data = PcountingService.posData;

            pcountingCtrl.chart = {};
            pcountingCtrl.chart.data = PcountingService.chartData;

            pcountingCtrl.chartForPdf = {};
            pcountingCtrl.chartForPdf.data = PcountingService.chartForPdfData;

            pcountingCtrl.posChart = {};
            pcountingCtrl.posChart.data = PcountingService.posChartData;

            pcountingCtrl.posChartForPdf = {};
            pcountingCtrl.posChartForPdf.data = PcountingService.posChartForPdfData;

            pcountingCtrl.timecard = {};
            pcountingCtrl.timecard.data = PcountingService.timecardData;

            // pcountingCtrl.posTimecard = {};
            // pcountingCtrl.posTimecard.data = PcountingService.timecardPOSData;
            pcountingCtrl.grid = {};
            pcountingCtrl.grid.data = PcountingService.gridData;

            pcountingCtrl.data = PcountingService.data;
            pcountingCtrl.noData = {};
            pcountingCtrl.noData.data = {
                isShow: true,
            };

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
                    pcountingCtrl.noData.data.isShow = !reportsOpt.isSuccessReport;
                }, true);

                //watch theme 
                $scope.$watch(function() {
                    return AuthTokenFactory.getTheme();
                }, function(newVal, oldVal) {
                    if (newVal == oldVal) {
                        return false;
                    }
                    PcountingService.setChartTheme();
                    PcountingService.generateChart();
                    PcountingService.generatePosChart();
                }, true);

                $scope.$watch('pcountingCtrl.pos.data.isShowDemo', function(newVal, oldVal) {
                    pcountingCtrl.timecard.data.options.isShowDemo = newVal;
                }, true);
            }

            function setTab(tabName) {
                var opt = pcountingCtrl.data;
                var tabData = pcountingCtrl.tab.data;
                $.each(tabData, function(i, data) {
                    data.isActive = (tabName === data.name) ? true : false;
                });
                opt.currentTab = tabName;
            }

            function setPosData(isShowDemo) {
                var opt = pcountingCtrl.data;
                var posData = pcountingCtrl.pos.data;
                posData.isShowDemo = isShowDemo;
                pcountingCtrl.timecard.data = PcountingService.timecardData;
                if (!isShowDemo) {
                    opt.cardType = 'COUNTING';
                }
            }

            function setTimeunitForChart(unitName) {
                var opt = pcountingCtrl.data;
                var timeunitData = pcountingCtrl.timeunit.data;
                $.each(timeunitData, function(i, data) {
                    data.isActiveForChart = (unitName === data.name) ? true : false;
                });
                opt.currentTimeunitForChart = unitName;
                PcountingService.generateChart();
                PcountingService.generatePosChart();
            }

            function setTimeunitForTimecard(unitName) {
                var opt = pcountingCtrl.data;
                var posData = pcountingCtrl.pos.data;
                var timeunitData = pcountingCtrl.timeunit.data;
                $.each(timeunitData, function(i, data) {
                    data.isActiveForTimecard = (unitName === data.name) ? true : false;
                });
                opt.currentTimeunitForTimecard = unitName;
                PcountingService.setTimecardData(posData.isShowDemo);
            }

            function exportCsv(periodType) {
                if (!reports.isSuccessReport()) {
                    notification('error', i18n('please-generate-reports'));
                    return false;
                }

                var warningNotify = notification('warning', i18n('exporting-to-csv'), 0);
                PcountingService.exportAggregatedCsvReportApi(periodType)
                    .finally(function() {
                        warningNotify.close();
                    });
            }

            function exportPdf() {
                if (!reports.isSuccessReport()) {
                    notification('error', i18n('please-generate-reports'));
                    return false;
                }

                var warningNotify = notification('warning', i18n('exporting-to-pdf'), 0);
                PcountingService.exportPeopleCountingPdfApi()
                    .finally(function() {
                        warningNotify.close();
                    });
            }

            function downloadPosFile() {
                var openUrl = 'static/files/download/userguide.zip';
                window.open(openUrl, '_blank');
                window.focus();
            }
        });
