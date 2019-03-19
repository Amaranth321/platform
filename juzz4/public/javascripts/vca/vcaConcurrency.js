/**
 * @author Aye Maung
 */
var vcaCcy = {
    containerId: null,
    nodeId: null,
    minConcurrencyFilter: 1,
    padding: {top: 40, right: 15, bottom: 30, left: 10},
    dayOfWeekNames: [
        localizeResource("monday"),
        localizeResource("tuesday"),
        localizeResource("wednesday"),
        localizeResource("thursday"),
        localizeResource("friday"),
        localizeResource("saturday"),
        localizeResource("sunday")
    ],
    current: {
        concurrencyLimit: null,
        hslColorRange: [],
        vcaDetailsMap: {}
    }
};

vcaCcy.generate = function (containerId, nodeId, concurrencyLimit)
{
    vcaCcy.loading(true);

    vcaCcy.containerId = containerId;
    vcaCcy.nodeId = nodeId;
    vcaCcy.current.concurrencyLimit = concurrencyLimit;
    vcaCcy.current.hslColorRange = utils.generateHslColorRange(225, 0, concurrencyLimit);

    getVcaConcurrencyStatus(nodeId, vcaCcy.minConcurrencyFilter, function (responseData)
    {
        var concurrencyMap = responseData.status;

        //prepare data for UI
        $.each(concurrencyMap, function (dow, periodsMap)
        {
            $.each(periodsMap, function (periodStr, vcaList)
            {
                var detailsList = [];
                $.each(vcaList, function (i, vca)
                {
                    var details = {
                        type: localizeResource(vca.type),
                        program: vca.program,
                        deviceName: DvcMgr.getDeviceName(vca.coreDeviceId),
                        channelName: DvcMgr.getChannelName(vca.coreDeviceId, vca.channelId),
                        scheduleSummary: vca.recurrenceRule ? vca.recurrenceRule.summary : localizeResource("not-scheduled")
                    };
                    detailsList.push(details);
                });

                periodsMap[periodStr] = detailsList;
            })
        });

        vcaCcy._draw(concurrencyMap);
        vcaCcy.loading(false);
    });
};

vcaCcy.clear = function ()
{
    var container = document.getElementById(vcaCcy.containerId);
    if (container == null)
    {
        return;
    }

    while (container.firstChild)
    {
        container.removeChild(container.firstChild);
    }
};

vcaCcy.loading = function (loading)
{
    kendo.ui.progress($("#" + vcaCcy.containerId), loading);
};

vcaCcy._draw = function (concurrencyMap)
{
    vcaCcy.clear();

    //display days without periods also
    for (var i = 1; i <= 7; i++)
    {
        concurrencyMap[i] = concurrencyMap[i] || {};
    }

    //set container size
    var $wrapper = $("#" + vcaCcy.containerId).parent();
    var svgW = $wrapper.width();
    var svgH = $wrapper.height();

    //bar dimensions
    var dayRowH = 25;
    var dayLabelW = 90;

    var legendHeight = 20;

    //set draw area size
    var innerW = svgW - (vcaCcy.padding.left + vcaCcy.padding.right + dayLabelW);
    var innerH = svgH - (vcaCcy.padding.top + vcaCcy.padding.bottom);

    var now = new Date();
    var start = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
    var end = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 0, 0, 0);

    var xScale = d3.time.scale()
        .domain([start, end])
        .range([0, innerW]);

    /**
     *
     * Drawing starts
     *
     */
    var svgContainer = d3.select("#" + vcaCcy.containerId)
        .append("svg")
        .attr("width", svgW)
        .attr("height", svgH);

    //legend
    vcaCcy._attachLegend(svgContainer, legendHeight, vcaCcy.padding.left + 2, 10);

    var contentWrapper = svgContainer.append("g")
        .attr("transform", "translate(" + (vcaCcy.padding.left + dayLabelW) + ", " + vcaCcy.padding.top + ")");

    //Color segments
    $.each(concurrencyMap, function (dayOfWeek, periodsMap)
    {
        var dayWrapper = contentWrapper
            .append("g")
            .attr("transform", "translate(" + 0 + ", " + ((dayOfWeek - 1) * dayRowH * 2) + ")");

        //grey background box
        dayWrapper
            .append("rect")
            .attr("shape-rendering", "crispEdges")
            .attr("x", -dayLabelW)
            .attr("width", innerW + dayLabelW)
            .attr("height", dayRowH)
            .attr("fill", "#151515")
            .attr("fill-opacity", 0.6);

        //day name label
        dayWrapper
            .append("text")
            .attr("fill", "#F6AE40")
            .attr("x", -dayLabelW + 10)
            .attr("y", dayRowH / 2 + 2)
            .text(function (d, i)
            {
                return vcaCcy.dayOfWeekNames[dayOfWeek - 1];
            });

        //cell wrapper
        var periodBars = dayWrapper.selectAll("g")
            .data(Object.keys(periodsMap))
            .enter()
            .append("g")
            .attr("transform", function (d, i)
            {
                var period = vcaCcy._asPeriodObj(d);
                var offsetMillis = period.startMinutes * 60 * 1000;
                var startDt = new Date(start.getTime() + offsetMillis);
                var x = xScale(startDt);

                return "translate(" + x + ",0)";
            })
            .on("mouseenter", function (d)
            {
                vcaCcy._showVcaList(periodsMap[d]);
            })
            .on("mouseleave", function (d)
            {
                vcaCcy._hideVcaList();
            });

        //color boxes
        periodBars.append("rect")
            .attr("class", "period_rect")
            .attr("width", function (d)
            {
                return vcaCcy._calcDurationWidth(d, innerW);
            })
            .attr("height", dayRowH)
            .attr("fill", function (d)
            {
                return vcaCcy._getColor(periodsMap[d].length);
            })
            .attr("fill-opacity", 0.9);
    });

    //labels
    contentWrapper.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + innerH + ")")
        .call(d3.svg.axis()
            .scale(xScale)
            .orient("bottom")
            .ticks(d3.time.hours, 1)
            .tickPadding(5)
            .tickFormat(d3.time.format("%H")))
        .selectAll("text")
        .attr("x", -5)
        .style("text-anchor", null);

    //ticks
    contentWrapper.append("g")
        .attr("class", "x grid")
        .attr("transform", "translate(0," + innerH + ")")
        .call(d3.svg.axis()
            .scale(xScale)
            .orient("bottom")
            .ticks(d3.time.minutes, 30)
            .tickSize(-innerH)
            .tickFormat(""))
        .selectAll(".tick")
        .classed("minor", function (d)
        {
            return d.getMinutes();
        });
};

vcaCcy._asPeriodObj = function (periodString)
{
    var prdArr = periodString.split(":");
    return {
        startMinutes: prdArr[0],
        endMinutes: prdArr[1]
    }
};

vcaCcy._calcDurationWidth = function (periodString, maxWidth)
{
    var period = vcaCcy._asPeriodObj(periodString);
    return maxWidth * ((period.endMinutes - period.startMinutes) / (24 * 60));
};

vcaCcy._getColor = function (concurrency)
{
    return vcaCcy.current.hslColorRange[concurrency - 1];
};

vcaCcy._attachLegend = function (svgContainer, h, x, y)
{
    var rowHeight = h;
    var legendColorWidth = 12;
    var legendLabelWidth = 200;

    var legendWrapper = svgContainer.append("g")
        .attr("transform", "translate(" + x + ", " + y + ")");

    //color
    var maxColor = vcaCcy.current.hslColorRange[vcaCcy.current.hslColorRange.length - 1];
    legendWrapper
        .append("rect")
        .attr("width", legendColorWidth)
        .attr("height", legendColorWidth)
        .attr("fill", maxColor)
        .attr("fill-opacity", 0.8)
        .attr("x", 0)
        .attr("y", 0);

    //label
    legendWrapper
        .append("text")
        .attr("width", legendLabelWidth)
        .attr("height", rowHeight)
        .attr("fill", "#bbb")
        .attr("x", legendColorWidth + 7)
        .attr("y", rowHeight / 2)
        .text(localizeResource("vca-concurrency-max"));
};

vcaCcy._showVcaList = function (vcaList)
{
    var $hoverWin = $(".vca_ccy .hover_win");
    var template = kendo.template($("#concurrentVcaList").html());

    $hoverWin.css("left", d3.event.pageX);
    $hoverWin.css("top", d3.event.pageY + 15);
    $hoverWin.html(template(vcaList));
    $hoverWin.show();
};

vcaCcy._hideVcaList = function ()
{
    var $hoverWin = $(".vca_ccy .hover_win");
    $hoverWin.hide();
};

