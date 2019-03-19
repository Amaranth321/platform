/**
 * @author Aye Maung
 */
var visPrd = {
    containerId: null,
    periodsOfDays: null,
    padding: {top: 20, right: 30, bottom: 40, left: 10},
    dayOfWeekNames: [
        localizeResource("monday"),
        localizeResource("tuesday"),
        localizeResource("wednesday"),
        localizeResource("thursday"),
        localizeResource("friday"),
        localizeResource("saturday"),
        localizeResource("sunday")
    ]
};

visPrd.init = function (periodsOfDays) {
    //null means run 24/7
    if (periodsOfDays == null) {
        periodsOfDays = {};
        for (var i = 1; i <= 7; i++) {
            periodsOfDays[i] = [
                {startMinutes: 0, endMinutes: 24 * 60}
            ];
        }
    }

    //display days without periods also
    for (var i = 1; i <= 7; i++) {
        periodsOfDays[i] = periodsOfDays[i] || [];
    }

    visPrd.periodsOfDays = periodsOfDays;
};

visPrd.generate = function (containerId) {
    visPrd.containerId = containerId;
    visPrd._draw();
};

visPrd.clear = function () {
    var container = document.getElementById(visPrd.containerId);
    if (container == null) {
        return;
    }

    while (container.firstChild) {
        container.removeChild(container.firstChild);
    }
};

visPrd._draw = function () {
    visPrd.clear();

    //set container size
    var $wrapper = $("#" + visPrd.containerId).parent();
    var svgW = $wrapper.width();
    var svgH = $wrapper.height();

    //bar dimensions
    var dayRowH = 25;
    var dayLabelW = 90;

    //set draw area size
    var innerW = svgW - (visPrd.padding.left + visPrd.padding.right) - dayLabelW;
    var innerH = svgH - (visPrd.padding.top + visPrd.padding.bottom);

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
    var svgContainer = d3.select("#" + visPrd.containerId)
        .append("svg")
        .attr("width", svgW)
        .attr("height", svgH);

    var contentWrapper = svgContainer.append("g")
        .attr("transform", "translate(" + (visPrd.padding.left + dayLabelW) + ", " + visPrd.padding.top + ")");

    //Color segments
    $.each(visPrd.periodsOfDays, function (dayOfWeek, periods) {
        var dayWrapper = contentWrapper
            .append("g")
            .attr("transform", "translate(" + 0 + ", " + ((dayOfWeek - 1) * dayRowH * 2) + ")");

        //periods
        dayWrapper.selectAll("rect")
            .data(periods)
            .enter()
            .append("rect")
            .attr("shape-rendering", "crispEdges")
            .attr("stroke-width", 0)
            .attr("stroke", "#505050")
            .attr("x", function (d, i) {
                var offsetMillis = d.startMinutes * 60 * 1000;
                var startDt = new Date(start.getTime() + offsetMillis);
                return xScale(startDt);
            })
            .attr("y", 0)
            .attr("width", function (d) {
                var durWidth = innerW * ((d.endMinutes - d.startMinutes) / (24 * 60));
                return durWidth;
            })
            .attr("height", dayRowH)
            .attr("fill", "#F6AE40")
            .attr("fill-opacity", 0.9);

        //grey background box
        dayWrapper
            .append("rect")
            .attr("shape-rendering", "crispEdges")
            .attr("x", -dayLabelW)
            .attr("width", innerW + dayLabelW)
            .attr("height", dayRowH)
            .attr("fill-opacity", 0.2);

        //day name label
        dayWrapper
            .append("text")
            .attr("fill", "#F6AE40")
            .attr("x", -dayLabelW + 10)
            .attr("y", dayRowH / 2 + 2)
            .text(function (d, i) {
                return visPrd.dayOfWeekNames[dayOfWeek - 1];
            });
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
        .classed("minor", function (d) {
            return d.getMinutes();
        });
};