var recSB = {
    containerId: null,
    showLegend: (kupapi.applicationType == "cloud"),
    current: {
        date: new Date(),
        periodMillis: [0, 0],
        files: []
    },
    minDuration: 15,    // hardcoded to match 15-min files
    fn: {
        clearBrush: null
    },
    evt: {
        periodChanged: null
    },
    padding: {top: 10, right: 20, bottom: 10, left: 20}
};

recSB.RecStatus = Object.freeze({
    UNREQUESTED: "UNREQUESTED",
    REQUESTED: "REQUESTED",
    UPLOADING: "UPLOADING",
    COMPLETED: "COMPLETED",
    RETRYING: "RETRYING",
    ABORTED: "ABORTED",
    MISSING: "MISSING",

    isRequestable: function (status)
    {
        var list = [
            this.UNREQUESTED,
            this.ABORTED
        ];
        return list.indexOf(status) != -1;
    },
    isDeletable: function (status)
    {
        var list = [
            this.REQUESTED,
            this.UPLOADING,
            this.COMPLETED,
            this.RETRYING
        ];
        return list.indexOf(status) != -1;
    },
    allowedToPlay: function (status)
    {
        var list = [
            this.UPLOADING,
            this.COMPLETED
        ];
        return list.indexOf(status) != -1;
    }
});

recSB.statusColors = {
    UNREQUESTED: "#004D60", // dark blue tint
    REQUESTED: "#f6ae40",   // orange
    UPLOADING: "#428bca",   // blue
    COMPLETED: "#8dc051",   // green
    RETRYING: "#7E3F12",    // brown
    ABORTED: "#ff2a3f",     // red
    MISSING: "transparent"
};

recSB.init = function (containerId, periodChanged)
{
    recSB.containerId = containerId;
    recSB.evt.periodChanged = periodChanged;
    recSB._draw();
};

recSB.generate = function (targetDate, streamFiles, animate)
{
    recSB.current.date = targetDate;
    recSB.current.files = recSB._prepareFiles(streamFiles);
    recSB._draw(animate);
    recSB._handleEmptyRecordingsList();
};

recSB.reset = function ()
{
    recSB.current.files = [];
    recSB._draw(false);
};

recSB.remove = function ()
{
    if (recSB.containerId == null)
    {
        return;
    }

    if (recSB.fn.clearBrush)
    {
        recSB.fn.clearBrush();
    }

    var container = document.getElementById(recSB.containerId);
    if (container == null)
    {
        return;
    }

    while (container.firstChild)
    {
        container.removeChild(container.firstChild);
    }
};

recSB.isSelectionActive = function ()
{
    return $("#movingToolBox").is(":visible");
};

recSB._prepareFiles = function (files)
{
    $.each(files, function (i, f)
    {
        //recording files doesn't always start/end exactly at 15-min marks
        //hence, this rounding is necessary to prevent mis-selections on UI
        //original fields are not modified
        f.roundedStart = recSB._roundTo15MinMarks(f.startTime);
        f.roundedEnd = recSB._roundTo15MinMarks(f.endTime);

        //usually, diffs are very minor. Hence, need to log this
        var warnThreshold = 5 * 1000;
        if (
            Math.abs(f.startTime - f.roundedStart) > warnThreshold ||
            Math.abs(f.endTime - f.roundedEnd) > warnThreshold
        )
        {
            var timeFmt = "hh:mm:ss";
            console.warn(
                "Rounded", moment(f.startTime).format(timeFmt) + "-" + moment(f.endTime).format(timeFmt),
                "to", moment(f.roundedStart).format(timeFmt) + "-" + moment(f.roundedEnd).format(timeFmt)
            );
        }

        //small slices will not be visible, hence minWidth will be set for them
        var minWidth = 2 * 60 * 1000; //2 mins
        if (f.roundedEnd - f.roundedStart < minWidth)
        {
            f.roundedEnd = f.roundedStart + minWidth;
        }
    });

    return files;
};

recSB._draw = function (paramsChanged)
{
    recSB.remove();

    //set container size
    var $wrapper = $("#" + recSB.containerId).parent();
    var svgW = $wrapper.width() - recSB.padding.right;
    var svgH = $wrapper.height();

    //set draw area size
    var svgInnerW = svgW - (recSB.padding.left + recSB.padding.right);
    var svgInnerH = svgH - (recSB.padding.top + recSB.padding.bottom);

    //horizontal scale
    var current = recSB.current.date;
    var start = new Date(current.getFullYear(), current.getMonth(), current.getDate(), 0, 0, 0);
    var end = new Date(current.getFullYear(), current.getMonth(), current.getDate() + 1, 0, 0, 0);

    //constants
    var barCount = 96; //15min segments
    var barWidth = svgInnerW / barCount;

    var xScale = d3.time.scale()
        .domain([start, end])
        .range([0, svgInnerW]);

    //selector
    var brush = d3.svg.brush()
        .x(xScale)
        .on("brush", brushed)
        .on("brushend", brushEnded);

    /**
     *
     * Drawing starts
     *
     */
    var svgContainer = d3.select("#" + recSB.containerId)
        .append("svg")
        .attr("width", svgW)
        .attr("height", svgH);

    var globalWrapper = svgContainer.append("g")
        .attr("transform", "translate(" + recSB.padding.left + ", " + recSB.padding.top + ")");

    /**
     *
     * Legend
     *
     */
    var legendHeight = 10;
    var legendColorWidth = 10;
    var legendLabelWidth = 100;
    if (recSB.showLegend)
    {
        var legendWrapper = globalWrapper.append("g")
            .attr("transform", "translate(" + 0 + ", " + 0 + ")");

        var legendEntries = [
            recSB.RecStatus.UNREQUESTED,
            recSB.RecStatus.REQUESTED,
            recSB.RecStatus.UPLOADING,
            recSB.RecStatus.COMPLETED,
            recSB.RecStatus.RETRYING,
            recSB.RecStatus.ABORTED
        ];

        //legend colors
        legendWrapper.selectAll("rect")
            .data(legendEntries)
            .enter()
            .append("rect")
            .attr("width", legendColorWidth)
            .attr("height", legendHeight)
            .attr("fill", function (d, i)
            {
                return recSB.statusColors[d];
            })
            .attr("x", function (d, i)
            {
                var posX = i * (legendColorWidth + legendLabelWidth);
                return posX;
            })
            .attr("y", 0);

        //legend labels
        legendWrapper.selectAll("text")
            .data(legendEntries)
            .enter()
            .append("text")
            .attr("width", legendLabelWidth)
            .attr("height", legendHeight)
            .attr("fill", "#999")
            .attr("x", function (d, i)
            {
                var posX = legendColorWidth + (i * (legendColorWidth + legendLabelWidth));
                return posX + 5;
            })
            .attr("y", legendHeight - 1)
            .text(function (d, i)
            {
                return localizeResource("rec-status-" + d);
            });
    }

    /**
     *
     * Chart
     *
     */
    var chartMarginTop = 15;
    var chartPaddingTop = 10;
    var chartHeight = svgInnerH - (legendHeight + chartMarginTop);
    var chartInnerH = chartHeight - chartPaddingTop;
    var tickLabelHeight = 28;
    var tickHeight = chartInnerH - tickLabelHeight;

    var chartWrapper = globalWrapper.append("g")
        .attr("transform", "translate(" + 0 + ", " + (legendHeight + chartMarginTop) + ")");

    /**
     *
     * Color segments
     *
     */
    var segments = chartWrapper.selectAll("rect")
        .data(recSB.current.files)
        .enter()
        .append("rect")
        .attr("shape-rendering", "crispEdges")
        .attr("stroke-width", 0)
        .attr("stroke", "#505050")
        .attr("fill-opacity", 0.9)
        .attr("x", function (d, i)
        {
            var startDt = new Date(d.roundedStart);
            return xScale(startDt);
        })
        .attr("y", function (d)
        {
            var yOffset = tickHeight - recSB._getSegmentHeight(d, tickHeight);
            return yOffset + chartPaddingTop;
        })
        .attr("width", function (d)
        {
            var durationWidth = barWidth * ((d.roundedEnd - d.roundedStart) / (15 * 60 * 1000));
            return durationWidth;
        })
        .attr("height", function (d, i)
        {
            return recSB._getSegmentHeight(d, tickHeight);
        })
        .attr("fill", function (d, i)
        {
            return recSB.statusColors[d.status];
        })
        .attr("opacity", 0.9);

    //animate all for first time loading
    if (paramsChanged)
    {
        var stillBars = segments.filter(function (d)
        {
            return d.status != recSB.RecStatus.UPLOADING;
        });
        stillBars.attr("opacity", 0);
        stillBars.transition()
            .duration(500)
            .attr("opacity", 0.9);
    }

    /**
     *
     * blink uploading segment
     *
     */
    var progressBars = segments.filter(function (d)
    {
        return (d.status == recSB.RecStatus.UPLOADING);
    });

    var dur = 500;
    (function blinkUploading()
    {
        progressBars
            .transition()
            .duration(dur)
            .attr("fill", recSB.statusColors[recSB.RecStatus.COMPLETED])
            .each("end", function ()
            {
                d3.select(this)
                    .transition()
                    .duration(dur)
                    .attr("fill", function (d, i)
                    {
                        return recSB.statusColors[d.status];
                    });
            });
        setTimeout(blinkUploading, 2 * dur);
    })();

    //hour ticks
    chartWrapper.append("g")
        .attr("class", "x grid")
        .attr("transform", "translate(0, " + (tickHeight + chartPaddingTop) + ")")
        .call(d3.svg.axis()
            .scale(xScale)
            .orient("bottom")
            .ticks(d3.time.minutes, 15)
            .tickSize(-tickHeight)
            .tickFormat(""))
        .selectAll(".tick")
        .classed("minor", function (d)
        {
            return d.getMinutes();
        });

    //tick labels
    chartWrapper.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + (tickHeight + chartPaddingTop) + ")")
        .call(d3.svg.axis()
            .scale(xScale)
            .orient("bottom")
            .ticks(d3.time.hours, 1)
            .tickPadding(4)
            .tickFormat(d3.time.format("%I %p"))
        )
        .selectAll("text")
        .attr("x", -16)
        .style("text-anchor", null)
        .style("font-size", 11);

    //Disable brush if there are no selectable files
    if (utils.isListEmpty(recSB.current.files))
    {
        return;
    }

    /**
     *
     * Brush
     *
     */
    var gBrush = chartWrapper.append("g")
        .attr("class", "brush")
        .call(brush);

    gBrush.selectAll("rect")
        .attr("transform", "translate(0, " + 0 + ")")
        .attr("height", chartHeight);


    /**
     *
     *  Internal Functions
     *
     */
    function brushed(e)
    {
        var origExtent = brush.extent();

        //floor start and end
        var rounded0 = floorMinutes(origExtent[0]);
        var rounded1 = floorMinutes(origExtent[1]);

        //minimum check
        if ((rounded1.getTime() - rounded0.getTime()) < recSB.minDuration)
        {
            rounded1.setMinutes(rounded1.getMinutes() + recSB.minDuration);
        }

        var snappedExtent = [rounded0, rounded1];
        d3.select(this).call(brush.extent(snappedExtent));

        //update info box
        var rectExtent = gBrush.select("rect.extent");
        var position = rectExtent.node().getBoundingClientRect();
        recSB._updatePeriodInfoBox(snappedExtent, position)
    }

    function floorMinutes(dt)
    {
        var minutes = dt.getMinutes();
        var intervals = [0, 15, 30, 45, 60];

        for (var i = 1; i < intervals.length; i++)
        {
            if (minutes < intervals[i])
            {
                var rounded = intervals[i - 1];
                return new Date(dt.getFullYear(), dt.getMonth(), dt.getDate(), dt.getHours(), rounded, 0);
            }
        }

        return 0;
    }

    function brushEnded()
    {
        var period = brush.extent();
        var from = period[0].getTime();
        var to = period[1].getTime();

        //check if period changes
        if (recSB.current.periodMillis[0] == from &&
            recSB.current.periodMillis[1] == to)
        {
            return;
        }

        recSB.current.periodMillis = [from, to];
        var selectedFiles = [];
        recSB.current.files.forEach(function (file)
        {
            var startTime = file.roundedStart;
            if (startTime >= from && startTime < to)
            {
                selectedFiles.push(file);
            }
        });

        recSB.evt.periodChanged(period, selectedFiles);
    }

    recSB.fn.clearBrush = function ()
    {
        //temporarily set min duration to zero
        var tmpDur = recSB.minDuration
        recSB.minDuration = 0;

        //clear
        brush.clear().event(d3.select(".brush"));
        var $movingInfoBox = $("#movingToolBox");
        $movingInfoBox.hide();

        //restore min duration
        recSB.minDuration = tmpDur;
    }
};

recSB._updatePeriodInfoBox = function (period, position)
{
    //update period label
    var $movingInfoBox = $("#movingToolBox");
    var $periodInfo = $movingInfoBox.find(".period_status");
    $periodInfo.html(recSB._formatPeriod(period));

    //magic numbers
    var top = 193;
    var borderWidthOffset = 1;

    //calculate position
    var $svgBox = $("#" + recSB.containerId);
    var boxWidth = $movingInfoBox.outerWidth();
    var menuWidth = $(".filter-sidebar").outerWidth();
    var right = $svgBox.outerWidth() - (position.left + boxWidth) + menuWidth + borderWidthOffset;

    //detect right boundary
    if (right < 10)
    {
        right = 10;
    }

    //move box
    $movingInfoBox.css("position", "absolute");
    $movingInfoBox.css("top", top);
    $movingInfoBox.css("right", right);
    $movingInfoBox.show();
};

recSB._handleEmptyRecordingsList = function ()
{
    var empty = utils.isListEmpty(recSB.current.files);
};

recSB._formatPeriod = function (period)
{
    var format = "h:mm tt";
    var start = kendo.toString(period[0], format);
    var end = kendo.toString(period[1], format);
    return start + " - " + end;
};

recSB._roundTo15MinMarks = function (origMillis)
{
    var plusOrMinusSeconds = 10;
    var dtOrig = moment(origMillis);
    var dtZero = moment(dtOrig).minutes(0).seconds(0).milliseconds(0);

    var roundTimes = [
        dtZero,
        moment(dtZero).add(15, "minutes"),
        moment(dtZero).add(30, "minutes"),
        moment(dtZero).add(45, "minutes"),
        moment(dtZero).add(60, "minutes")
    ];

    var roundedMillis = origMillis;
    $.each(roundTimes, function (i, roundTime)
    {
        var minus = moment(roundTime).subtract(plusOrMinusSeconds, "second");
        var plus = moment(roundTime).add(plusOrMinusSeconds, "second");
        if (dtOrig.isBefore(minus) || dtOrig.isAfter(plus))
        {
            return true;
        }

        roundedMillis = roundTime.valueOf();
        return false;
    });

    return roundedMillis;
};

recSB._getSegmentHeight = function (segment, maxHeight)
{
    if (segment.progress > 0)
    {
        return maxHeight * (segment.progress / 100);
    }

    return maxHeight;
};