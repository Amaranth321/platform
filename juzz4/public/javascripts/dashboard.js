var dashboard = {};
dashboard.currentChartbaseUnit = null;
dashboard.range = "";
dashboard.chartColors = ["#74A402", "#0A88E5", "#E57300", "#A186BE", "#D85171"];  //green, blue, orange, purple, pink


dashboard.numberPieChart = function(divId, title, data) {
    $("#" + divId).kendoChart({
        theme: "bootstrap",
        title: {
            text: title,
            color: "#EAAC00",
            font: "bold 16px Muli,sans-serif"
        },
        legend: {
            visible: true,
            labels: {
                font: "11px Muli, sans-serif",
                color: "#ffffff"
              }
        },
        chartArea: {
            background: "#212A33"
        },
        series: [{
                type: "pie",
                padding: 0,
                categoryField: "sex",
                field: "value",
                data: [{
                        sex: "male",
                        value: data.male,
                        color: dashboard.chartColors[1]
                    }, {
                        sex: "female",
                        value: data.female,
                        color: dashboard.chartColors[4]
                    }]
            }],
        tooltip: {
            visible: true,
            template: "#= category # : #= value #",
            font: "11px Muli, sans-sarif"
        }
    });
}

dashboard.lineChart = function (divId, title, chartData) {
	dashboard.currentChartbaseUnit = "days";
    if (parseInt(dashboard.range) == 1) {
    	dashboard.currentChartbaseUnit = "hours";
    }
	
	$("#" + divId).kendoChart({
        theme: "moonlight",
        dataSource: chartData,
        legend: {
            visible: true,
            position: "top",
            labels: {
                font: "11px Muli, sans-serif"
              }
        },
        seriesDefaults: {
            type: "line"
        },
        series: [
                 { name: title, field: "count", aggregate: "sum"}
             ],
        valueAxis: {
            line: {
                visible: false
            },
            minorUnit: 1
        },
        categoryAxis: {
            field: "time",
            baseUnit: dashboard.currentChartbaseUnit,
            labels: {
                rotation: -90,
                timeFormat: "HH:mm",
                font: "11px Muli, sans-sarif",
                dateFormats: {
                    hours: "dd-MM HH:mm",
                    days: "dd MMM",
                    weeks: "dd MMM",
                    months: "MMM yyyy"
                }
            },
            majorGridLines: {
                visible: true
            }
        },
        tooltip: {
            visible: true,
            format: "{0}",
            template: "#= series.name #: #= value #",
            font: "11px Muli, sans-sarif"
        }
    });
}

$(window).on("resize", function() {
      kendo.resize($(".event_container"));
    });
