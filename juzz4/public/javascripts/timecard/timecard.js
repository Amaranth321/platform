/**
 * Author: Aye Maung
 *
 * Please check with me first before modifying this file
 *
 */

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
    cardType: {COUNTING: "COUNTING", CONVERSION: "CONVERSION"},
    periodType: {WEEKLY: "WEEKLY", MONTHLY: "MONTHLY", YEARLY: "YEARLY"},
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
    ext: {
        timeFormat: {
            pcounting: "yyyy/MM/dd HH:mm:ss",
            pos: "dd/MM/yyyy HH:mm:ss"
        }
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
        periodType: null,
        consolidatedData: null,     //raw count+pos
        compiledData: null          //separated by period
    },
    animation: {
        enabled: true,
        easing: "linear",
        duration: {weekly: 130, monthly: 30, yearly: 150}
    }
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
timeCard.generateCard = function (containerId, cardType, periodType, peopleCountingData, posData) {
    this._initUi(containerId);
    timeCard._setPeriodType(periodType);
    timeCard._setCardType(cardType);
    timeCard._consolidateData(peopleCountingData, posData);
    timeCard._prepareDataByPeriod();
    timeCard._draw();
}

timeCard.switchCartType = function (targetCardType) {
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

timeCard.switchPeriodType = function (targetPeriodType) {
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

timeCard.removeCard = function () {
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
timeCard.getSvgList = function (containerId) {
    var svgList = $("#" + containerId + " svg");
    var serializedList = [];
    var xmlSerializer = new XMLSerializer;
    svgList.each(function (idx, svgObj) {
        serializedList.push(xmlSerializer.serializeToString(svgObj));
    });

    return serializedList;
}

timeCard._initUi = function (containerId) {
    //clear container
    timeCard._clearChildElements(containerId);
    $("#" + containerId).addClass("time-card");

    //add components
    $("#" + containerId).append(
            '<div class="card_period_selector">' +
            '   <ul>' +
            '       <li class="selected">' +
            '           <input id="radWeekly" type="radio" name="cardPeriodType" value="WEEKLY"/>' +
            '           <label for="radWeekly">' + localizeResource("weekly") + '</label>' +
            '       </li>' +
            '       <li>' +
            '           <input id="radMonthly" type="radio" name="cardPeriodType" id="blue" value="MONTHLY"/>' +
            '           <label for="radMonthly">' + localizeResource("monthly") + '</label>' +
            '       </li>' +
            '       <li>' +
            '           <input id="radYearly" type="radio" name="cardPeriodType" id="green" value="YEARLY"/>' +
            '           <label for="radYearly">' + localizeResource("yearly") + '</label>' +
            '       </li>' +
            '   </ul>' +
            '</div>' +
            '<div class="card_type_selector">' +
            '    <input id="radCounting" value="COUNTING" type="radio" name="cardChartType" checked/>' +
            '    <label for="radCounting">' + localizeResource("no-people-in") + '</label>' +
            '    <input id="radConversion" value="CONVERSION" type="radio" name="cardChartType"/>' +
            '    <label for="radConversion">' + localizeResource("conversion-rate") + '</label>' +
            '</div>' +
            '<div id="' + timeCard.cardBoxId + '"></div>'
    );

    //popover box
    var popoverBox = $(".detailed-stat-box");
    if (popoverBox[0] == null) {
        $("#" + containerId).append(
                "<div class='detailed-stat-box'>" +
                "   <label>" + localizeResource("no-people-in") + "</label>" +
                "   <span class='people-in'></span><br/>" +
                "   <label>" + localizeResource("conversion-rate") + "</label>" +
                "   <span class='conversion-rate'></span><br/>" +
                "   <label>" + localizeResource("receipt") + "</label>" +
                "   <span class='receipt'></span><br/>" +
                "   <label>" + localizeResource("sales-amount") + "</label>" +
                "   <span class='sales-amount'></span><br/>" +
                "</div>"
        );
    }

    //handle card change events
    $("#" + containerId + " .card_type_selector input:radio[name='cardChartType']").change(function () {
        timeCard.switchCartType(this.value);
    });

    //handle period change events
    $("#" + containerId + " .card_period_selector input:radio[name='cardPeriodType']").change(function () {
        $(this).parents('li').addClass('selected');
        $(this).parents().siblings('li').removeClass('selected');
        timeCard.switchPeriodType(this.value);
    });
}

timeCard._setCardType = function (cardType) {
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

timeCard._setPeriodType = function (periodType) {
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
timeCard._consolidateData = function (peopleCountingData, posData) {

    //compile pos
    var posHourlyMap = {};
    if (posData == null || posData.length == 0) {
        $(".card_type_selector").hide();
    }
    else {
        $.each(posData, function (idx, hourlyData) {
            var utcDt = kendo.parseDate(hourlyData.sales.time, timeCard.ext.timeFormat.pos);
            var posLocalTime = utils.convertUTCtoLocal(utcDt);
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
    $.each(peopleCountingData, function (idx, countData) {
        var utcDt = kendo.parseDate(countData.date, timeCard.ext.timeFormat.pcounting);
        var pLocalTime = utils.convertUTCtoLocal(utcDt);
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
    $.each(countHourlyMap, function (hourSpecifier, hourlyTotal) {
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
    combinedList.sort(function (a, b) {
        return a.date - b.date;
    });

    timeCard._current.consolidatedData = combinedList;
}

timeCard._prepareDataByPeriod = function () {
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

        default :
            console.log("unknown period");
            break;
    }
}

timeCard._draw = function () {
    var cardType = timeCard._current.cardType;
    var periodType = timeCard._current.periodType;

    switch (periodType) {
        case timeCard.periodType.WEEKLY:
            $.each(timeCard._current.compiledData.weeklyDataSet, function (weekSpecifier, weekDataSet) {
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
            $.each(timeCard._current.compiledData.monthlyDataSet, function (monthSpecifier, monthDataSet) {
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
            $.each(timeCard._current.compiledData.yearlyDataSet, function (year, yearDataSet) {
                timeCard._generateOneYear(
                    timeCard.cardBoxId,
                    cardType,
                    year,
                    yearDataSet,
                    timeCard._current.compiledData.minCount,
                    timeCard._current.compiledData.maxCount);
            });
            break;

        default :
            console.log("unknown period");
            break;
    }
}

timeCard._generateOneWeek = function (cardContainerId, cardType, weekSpecifier, hourlyDataSet, minCount, maxCount) {

    var bgColorFn = timeCard._getBgColoringFn(minCount, maxCount);

    //get week start and end
    var dateFormatter = d3.time.format("%Y-%m-%d");
    var weekDates = timeCard._getWeekStartEndDates(weekSpecifier);
    var strWeekStart = dateFormatter(weekDates.startDate);
    var strWeekEnd = dateFormatter(weekDates.endDate);
    var weekTitle = strWeekStart + " " + localizeResource("to") + " " + strWeekEnd;

    //dimensions
    var margin = {top: 20, right: 10, bottom: 0, left: 10};
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

    var mainWrapper = svgContainer.append("g")
        .attr("transform", function (d, i) {
            var xOffset = margin.left;
            var yOffset = margin.top;
            return "translate(" + xOffset + ", " + yOffset + ")";
        });

    //legend
    timeCard._attachLegend(mainWrapper, cardType);

    //week indicator
    var weekIndicator = mainWrapper.append("text")
        .attr("class", "month_year_indicator")
        .attr("font-size", "14px")
        .attr("font-weight", "bold")
        .attr("x", function () {
            var posX = innerWidth - 200;
            return posX;
        })
        .attr("y", function (d, i) {
            var posY = 15;
            return posY;
        })
        .text(function (d, i) {
            return weekTitle;
        });

    //Main Card Box
    var mainCardBox = mainWrapper.append("g")
        .attr("transform", function (d, i) {
            var xOffset = 0;
            var yOffset = timeCard.legend.height;
            return "translate(" + xOffset + ", " + yOffset + ")";
        });

    //Column Header Box
    var colHeaderBox = mainCardBox.append("g")
        .attr("transform", function (d, i) {
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
        .attr("x", function (d, i) {
            var offset = (colWidth / 2);
            var posX = i * colWidth;
            return posX + offset;
        })
        .attr("y", function (d, i) {
            var offset = (rowHeight / 2) + 3;
            var posY = 0;
            return posY + offset;
        })
        .text(function (d, i) {
            var dtCurrent = new Date(weekDates.startDate);
            dtCurrent.setDate(dtCurrent.getDate() + i);

            return d + " (" + dtCurrent.getDate() + ")";
        });

    //Periods Column
    var periodBox = mainCardBox.append("g")
        .attr("transform", function (d, i) {
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
        .attr("x", function (d, i) {
            var offset = 10;
            var posX = 0;
            return posX + offset;
        })
        .attr("y", function (d, i) {
            var offset = (rowHeight / 2) + 3;
            var posY = i * rowHeight;
            return posY + offset;
        })
        .text(function (d, i) {
            return d;
        });

    //Data Cells box
    var dataCellBox = mainCardBox.append("g")
        .attr("transform", function (d, i) {
            var xOffset = periodGroupWidth;
            var yOffset = headerHeight;
            return "translate(" + xOffset + ", " + yOffset + ")";
        });

    //create cell groups
    dataCellBox.selectAll("g")
        .data(timeCard._getWeekSlots(weekDates.startDate))
        .enter()
        .append("g")
        .on("mouseenter", function (hourSpecifier) {
            var data = hourlyDataSet[hourSpecifier];
            timeCard._handleMouseEnter(d3.select(this), data);
        })
        .on("mouseleave", function (hourSpecifier) {
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
        .attr("fill", function (hourSpecifier, i) {
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
        .text(function (hourSpecifier, i) {
            var data = hourlyDataSet[hourSpecifier];
            if (data == null) {
                return "";
            }
            return timeCard._getWeeklyCellContent(data);
        });


    //animation
    dataCellBox.selectAll("g")
        .attr("transform", function (hourSpecifier, i) {
            var posX = i % 7;
            var posY = Math.floor(i / 7);
            var xOffset = 0;
            var yOffset = (posY * rowHeight);
            return "translate(" + xOffset + ", " + yOffset + ")";
        })
        .transition()
        .ease(timeCard.animation.easing)
        .duration(function (hourSpecifier, i) {
            var posX = i % 7;
            var posY = Math.floor(i / 7);
            var duration = timeCard.animation.enabled ? timeCard.animation.duration.weekly : 0;
            return duration * posX;
        })
        .attr("transform", function (hourSpecifier, i) {
            var posX = i % 7;
            var posY = Math.floor(i / 7);
            var xOffset = (posX * colWidth);
            var yOffset = (posY * rowHeight);
            return "translate(" + xOffset + ", " + yOffset + ")";
        });
}

timeCard._generateOneMonth = function (cardContainerId, cardType, monthDetails, dailyDataSet, minCount, maxCount) {

    //compile data
    var bgColorFn = timeCard._getBgColoringFn(minCount, maxCount);

    //dimensions
    var margin = {top: 20, right: 10, bottom: 0, left: 10};
    var innerWidth = timeCard.cardWidth - margin.left - margin.right;
    var innerHeight = timeCard.cardHeight - margin.top - margin.bottom;
    var numOfCols = 7; // 7 days
    var numOfRows = 6; // 6 weeks
    var headerHeight = 75;
    var colWidth = (innerWidth) / numOfCols;
    var rowHeight = (innerHeight - timeCard.legend.height - headerHeight) / numOfRows;
    var cellPadding = {top: 5, right: 5, bottom: 5, left: 5};
    var cellInnerWidth = colWidth - cellPadding.left - cellPadding.right;
    var cellInnerHeight = rowHeight - cellPadding.top - cellPadding.bottom;
    var monthTitleWidth = 150;

    //Create SVG element
    var svgContainer = d3.select("#" + cardContainerId)
        .append("svg")
        .attr("width", timeCard.cardWidth)
        .attr("height", timeCard.cardHeight);

    var mainWrapper = svgContainer.append("g")
        .attr("transform", function (d, i) {
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
        .attr("x", function () {
            var posX = innerWidth - monthTitleWidth;
            return posX;
        })
        .attr("y", function (d, i) {
            var posY = 15;
            return posY;
        })
        .text(function (d, i) {
            var monthTitle = timeCard.monthNames[monthDetails.monthNumber] + " " + monthDetails.year;
            return monthTitle;
        });

    //Main Card Box
    var mainCardBox = mainWrapper.append("g")
        .attr("transform", function (d, i) {
            var xOffset = 0;
            var yOffset = 0;
            return "translate(" + xOffset + ", " + yOffset + ")";
        });

    //Column Header Box
    var colHeaderBox = mainCardBox.append("g")
        .attr("transform", function (d, i) {
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
        .attr("x", function (d, i) {
            var offset = (colWidth / 2);
            var posX = i * colWidth;
            return posX + offset;
        })
        .attr("y", function (d, i) {
            var offset = (rowHeight / 2) + 3;
            var posY = 0;
            return posY + offset;
        })
        .text(function (d, i) {
            return d;
        });

    //days of the month
    var dayGroup = mainCardBox.append("g")
        .attr("transform", function (d, i) {
            var xOffset = 0;
            var yOffset = headerHeight;
            return "translate(" + xOffset + ", " + yOffset + ")";
        });

    //create Hour cell groups
    dayGroup.selectAll("g")
        .data(timeCard._getMonthSlots(monthDetails.monthNumber, monthDetails.year))
        .enter()
        .append("g")
        .on("mouseenter", function (daySpecifier) {
            var data = dailyDataSet[daySpecifier];
            timeCard._handleMouseEnter(d3.select(this), data);
        })
        .on("mouseleave", function (daySpecifier) {
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
        .attr("fill", function (daySpecifier, i) {
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
        .text(function (daySpecifier, i) {
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
        .text(function (daySpecifier, i) {
            var date = timeCard.formatter.day.parse(daySpecifier);
            return date.getDate();
        });

    //animation
    dayGroup.selectAll("g")
        .attr("transform", function (daySpecifier, i) {
            var date = timeCard.formatter.day.parse(daySpecifier);
            var dayOfWeek = date.getDay();
            var weekOfMonth = Math.floor((date.getDate() - 1 + monthDetails.week1EmptySlots) / 7);
            var xOffset = dayOfWeek * colWidth;
            var yOffset = 0;
            return "translate(" + xOffset + ", " + yOffset + ")";
        })
        .transition()
        .ease(timeCard.animation.easing)
        .duration(function (daySpecifier, i) {
            var date = timeCard.formatter.day.parse(daySpecifier);
            var duration = timeCard.animation.enabled ? timeCard.animation.duration.monthly : 0;
            return duration * date.getDate();
        })
        .attr("transform", function (daySpecifier, i) {
            var date = timeCard.formatter.day.parse(daySpecifier);
            var dayOfWeek = date.getDay();
            var weekOfMonth = Math.floor((date.getDate() - 1 + monthDetails.week1EmptySlots) / 7);
            var xOffset = dayOfWeek * colWidth;
            var yOffset = weekOfMonth * rowHeight;
            return "translate(" + xOffset + ", " + yOffset + ")";
        });
}

timeCard._generateOneYear = function (cardContainerId, cardType, year, monthlyDataSet, minCount, maxCount) {

    var bgColorFn = timeCard._getBgColoringFn(minCount, maxCount);

    //dimensions
    var margin = {top: 20, right: 10, bottom: 0, left: 10};
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
        .attr("transform", function (d, i) {
            var xOffset = margin.left;
            var yOffset = margin.top;
            return "translate(" + xOffset + ", " + yOffset + ")";
        });

    //year indicator
    var yearIndicator = mainWrapper.append("text")
        .attr("class", "month_year_indicator")
        .attr("font-size", "17px")
        .attr("font-weight", "bold")
        .attr("x", function () {
            var posX = innerWidth - 60;
            return posX;
        })
        .attr("y", function (d, i) {
            var posY = 15;
            return posY;
        })
        .text(function (d, i) {
            return year;
        });

    //legend
    timeCard._attachLegend(mainWrapper, cardType);

    //Main Card Box
    var mainCardBox = mainWrapper.append("g")
        .attr("transform", function (d, i) {
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

timeCard._generateMiniMonth = function (parent, width, height, monthDetails, dailyDataSet, bgColorFn) {

    var listOfDays = timeCard._getMonthSlots(monthDetails.monthNumber, monthDetails.year);

    //dimensions
    var margin = {top: 8, right: 5, bottom: 8, left: 5};
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
        .attr("transform", function (d, i) {
            var xOffset = (location.x * width) + margin.left;
            var yOffset = (location.y * height) + margin.top;
            return "translate(" + xOffset + ", " + yOffset + ")";
        });

    //month Indicator
    var monthIndicator = miniMonth.append("text")
        .attr("class", "month_year_indicator")
        .attr("font-size", "14px")
        .attr("font-weight", "bold")
        .attr("x", function () {
            var posX = 2;
            return posX;
        })
        .attr("y", function (d, i) {
            var posY = 10;
            return posY;
        })
        .text(function (d, i) {
            return timeCard.monthNames[monthDetails.monthNumber];
        });

    var monthContainer = miniMonth.append("g")
        .attr("transform", function (d, i) {
            return "translate(0, " + monthTitleHeight + ")";
        });

    //Header Group
    var headerGroup = monthContainer.append("g")
        .attr("transform", function (d, i) {
            return "translate(0, 0)";
        });
    var headers = headerGroup.selectAll("text")
        .data(timeCard.shortDayOfWeekNames)
        .enter()
        .append("text")
        .attr("class", "column_header")
        .attr("text-anchor", "middle")
        .style("font-size", "12px")
        .attr("x", function (d, i) {
            var offset = (colWidth / 2);   //text position adjustment
            var posX = i * colWidth;
            return posX + offset;
        })
        .attr("y", function (d, i) {
            var offset = (rowHeight / 2) + 3;  //text position adjustment
            var posY = 0;
            return posY + offset;
        })
        .text(function (d, i) {
            return d;
        });

    //days of the month
    var dayGroup = monthContainer.append("g")
        .attr("transform", function (d, i) {
            return "translate(0, " + headerHeight + ")";
        });

    //create cell groups
    dayGroup.selectAll("g")
        .data(listOfDays)
        .enter()
        .append("g")
        .on("mouseenter", function (daySpecifier) {
            var data = dailyDataSet[daySpecifier];
            timeCard._handleMouseEnter(d3.select(this), data);
        })
        .on("mouseleave", function (daySpecifier) {
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
        .attr("fill", function (daySpecifier, i) {
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
        .text(function (daySpecifier, i) {
            var date = timeCard.formatter.day.parse(daySpecifier);
            return date.getDate();
        });

    //animation
    dayGroup.selectAll("g")
        .attr("transform", function (daySpecifier, i) {
            var date = timeCard.formatter.day.parse(daySpecifier);
            var dayOfWeek = date.getDay();
            var weekOfMonth = Math.floor((date.getDate() - 1 + monthDetails.week1EmptySlots) / 7);
            var xOffset = dayOfWeek * colWidth;
            var yOffset = 0;
            return "translate(" + xOffset + ", " + yOffset + ")";
        })
        .transition()
        .ease(timeCard.animation.easing)
        .duration(function (daySpecifier, i) {
            var date = timeCard.formatter.day.parse(daySpecifier);
            var weekOfMonth = Math.floor((date.getDate() - 1 + monthDetails.week1EmptySlots) / 7);
            var duration = timeCard.animation.enabled ? timeCard.animation.duration.yearly : 0;
            return duration * weekOfMonth;
        })
        .attr("transform", function (daySpecifier, i) {
            var date = timeCard.formatter.day.parse(daySpecifier);
            var dayOfWeek = date.getDay();
            var weekOfMonth = Math.floor((date.getDate() - 1 + monthDetails.week1EmptySlots) / 7);
            var xOffset = dayOfWeek * colWidth;
            var yOffset = weekOfMonth * rowHeight;
            return "translate(" + xOffset + ", " + yOffset + ")";
        });
}

timeCard._compileDataByDay = function (hourlyDataSet) {
    var dailyMap = {};
    var dayFormatter = d3.time.format("%Y%m%d");

    $.each(hourlyDataSet, function (idx, hourlyData) {
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
        }
        else {
            targetDay.count += hourlyData.count;
            targetDay.receipts += hourlyData.receipts;
            targetDay.sales += hourlyData.sales;
        }

        dailyMap[daySpecifier] = targetDay;
    });

    var compiledList = [];
    $.each(dailyMap, function (idx, dayData) {
        compiledList.push(dayData);
    });

    return compiledList;
}

timeCard._separateDataByWeek = function (hourlyDataSet) {
    var compiledData = {
        weeklyDataSet: {},
        minCount: 0,
        maxCount: 0
    }

    if (hourlyDataSet == null || hourlyDataSet.length == 0) {
        utils.popupAlert(localizeResource("no-data"));
        return compiledData;
    }

    var weeklyDataSet = {};
    var minCount = hourlyDataSet[0].count;
    var maxCount = hourlyDataSet[0].count;

    //populate weekly bins
    $.each(hourlyDataSet, function (idx, hourlyData) {
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

    //if the week spans across two years, data will be split into two sets
    //as last week of this year and first week of next year  (e.g. 2015_52 and 2016_00).
    //Those two should be merged.
    var weekList = Object.keys(weeklyDataSet);
    for (var i = 0; i < weekList.length; i++)
    {
        if (i == weekList.length - 1)
        {
            break;
        }

        var weekFirst = weekList[i];
        var weekSecond = weekList[i + 1];
        var currentStartEnd = timeCard._getWeekStartEndDates(weekFirst);
        var nextStartEnd = timeCard._getWeekStartEndDates(weekSecond);
        if (currentStartEnd.startDate.getTime() == nextStartEnd.startDate.getTime())
        {
            console.log("Week spans across two years. Combining :", weekFirst, weekSecond);
            $.extend(true, weeklyDataSet[weekFirst], weeklyDataSet[weekSecond]);
            delete weeklyDataSet[weekSecond];
        }
    }

    compiledData.weeklyDataSet = weeklyDataSet;
    compiledData.minCount = minCount;
    compiledData.maxCount = maxCount;
    return compiledData;
}

//returns a map of months with data array for each month
timeCard._separateDataByMonth = function (hourlyDataSet) {
    var compiledData = {
        monthlyDataSet: {},
        minCount: 0,
        maxCount: 0
    }

    if (hourlyDataSet == null || hourlyDataSet.length == 0) {
        utils.popupAlert(localizeResource("no-data"));
        return compiledData;
    }

    var dailyDataSet = timeCard._compileDataByDay(hourlyDataSet);
    var monthlyDataSet = {};
    var minCount = dailyDataSet[0].count;
    var maxCount = dailyDataSet[0].count;

    //populate monthly bins
    $.each(dailyDataSet, function (idx, dailyData) {
        var monthSpecifier = parseInt(timeCard.formatter.month(dailyData.date));
        if (monthlyDataSet[monthSpecifier] == null) {
            monthlyDataSet[monthSpecifier] = [];
        }

        var daySpecifier = timeCard.formatter.day(dailyData.date);
        monthlyDataSet[monthSpecifier][daySpecifier] = dailyData;

        //find max/min
        if (dailyData.count < minCount) {
            minCount = dailyData.count;
        }
        else if (dailyData.count > maxCount) {
            maxCount = dailyData.count;
        }
    });

    compiledData.monthlyDataSet = monthlyDataSet;
    compiledData.minCount = minCount;
    compiledData.maxCount = maxCount;
    return compiledData;
}

//returns a map of years with an array of months for each year
timeCard._separateDataByYear = function (hourlyDataSet) {
    var compiledData = {
        yearlyDataSet: {},
        minCount: 0,
        maxCount: 0
    };

    if (hourlyDataSet == null || hourlyDataSet.length == 0) {
        utils.popupAlert(localizeResource("no-data"));
        return compiledData;
    }

    var monthlyCompiledData = timeCard._separateDataByMonth(hourlyDataSet);
    var yearlyDataSet = {};

    //populate monthly bins
    $.each(monthlyCompiledData.monthlyDataSet, function (monthSpecifier, monthlyData) {
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

timeCard._getHourlyPeriods = function () {
    var hourList = [
        "12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"
    ];

    var fullDay = [];

    //morning
    $.each(hourList, function (idx, hr) {
        fullDay.push(hr + " " + localizeResource("am"));
    });

    //evening
    $.each(hourList, function (idx, hr) {
        fullDay.push(hr + " " + localizeResource("pm"));
    });

    return fullDay;
}

//returns all possible slots for the week identified by start date
timeCard._getWeekSlots = function (startDate) {
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
timeCard._getMonthSlots = function (monthNumber, year) {
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

timeCard._showPopover = function (data) {
    var conversionRate = (data.receipts / data.count) * 100;

    //show popover
    $(".detailed-stat-box .people-in").html(data.count);
    $(".detailed-stat-box .conversion-rate").html(conversionRate.toFixed(1) + "%");
    $(".detailed-stat-box .receipt").html(data.receipts);
    $(".detailed-stat-box .sales-amount").html((data.sales / 1000).toFixed(1) + "K");
    $(".detailed-stat-box").css("left", d3.event.pageX);
    $(".detailed-stat-box").css("top", d3.event.pageY - 50);
    $(".detailed-stat-box").show();
}

timeCard._hidePopover = function () {
    $(".detailed-stat-box").hide();
}

timeCard._attachLegend = function (parent, cardType) {
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
        .attr("fill", function (d, i) {
            return d;
        })
        .attr("x", function (d, i) {
            var posX = timeCard.legend.textWidth + (i * timeCard.legend.blockWidth);
            return posX;
        })
        .attr("y", function (d, i) {
            var posY = 0;
            return posY;
        })
        .text(function (d, i) {
            return d;
        });
    var lowCountingText = legendGroup.append("text")
        .attr("class", "label_legend")
        .attr("text-anchor", "middle")
        .attr("font-size", "12px")
        .attr("x", function (d, i) {
            var posX = timeCard.legend.textWidth / 2;
            return posX;
        })
        .attr("y", function (d, i) {
            var posY = 12;
            return posY;
        })
        .text(lowLabel);
    var highCountingText = legendGroup.append("text")
        .attr("class", "label_legend")
        .attr("text-anchor", "middle")
        .attr("font-size", "12px")
        .attr("x", function () {
            var offset = timeCard.legend.textWidth / 2;
            var posX = timeCard.legend.textWidth + (currentTheme.colorRanges.length * timeCard.legend.blockWidth);
            return posX + offset;
        })
        .attr("y", function (d, i) {
            var posY = 12;
            return posY;
        })
        .text(highLabel);
}

timeCard._getMonthDateDetails = function (targetDate) {
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

timeCard._getBgColoringFn = function (minCount, maxCount) {

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

    return function (data) {
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

timeCard._getWeeklyCellContent = function (data) {
    var template = "val1 (val2)";
    var conversionRate = (data.receipts / data.count) * 100;
    var pCount = data.count;
    var primaryVal = "";
    var secondaryVal = "";
    if (this._current.cardType == timeCard.cardType.COUNTING) {
        primaryVal = pCount;
        secondaryVal = conversionRate.toFixed(0) + "%";
    }
    else {
        primaryVal = conversionRate.toFixed(0) + "%";
        secondaryVal = pCount;
    }

    return  template.replace("val1", primaryVal).replace("val2", secondaryVal);
}

timeCard._getMonthlyCellContent = function (date, data) {
    var displayValue = "";
    if (data != null) {
        displayValue = timeCard._getWeeklyCellContent(data);
    }

    return displayValue;
}

timeCard._getWeekStartEndDates = function (weekSpecifier) {
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

timeCard._handleMouseEnter = function (targetElement, data) {
    if (data == null) {
        return;
    }

    timeCard._showPopover(data);
    targetElement.style("background", timeCard._current.theme.hoverColor);
}

timeCard._handleMouseLeave = function (targetElement, data, bgColorFn) {
    timeCard._hidePopover();
    targetElement.style("background", bgColorFn(data));
}

timeCard._clearChildElements = function (divId) {
    var container = document.getElementById(divId);
    if (container == null) {
        return;
    }

    while (container.firstChild) {
        container.removeChild(container.firstChild);
    }
}