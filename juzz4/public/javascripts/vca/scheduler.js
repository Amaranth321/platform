/**
 * @author Aye Maung
 */
var scheduler = {};
scheduler.jqSummaryText = null;
scheduler.selectedRecurrenceRule = null;

scheduler.dayOfWeekNames = {
    1: localizeResource("monday"),
    2: localizeResource("tuesday"),
    3: localizeResource("wednesday"),
    4: localizeResource("thursday"),
    5: localizeResource("friday"),
    6: localizeResource("saturday"),
    7: localizeResource("sunday")
};

scheduler._startTimeOfDay = new Date(2001, 1, 1, 0, 0);
scheduler._endTimeOfDay = new Date(2001, 1, 1, 23, 59);
scheduler._periodIdMap = {1: [], 2: [], 3: [], 4: [], 5: [], 6: [], 7: []};

scheduler.init = function (summaryId, currentRule) {
    scheduler.jqSummaryText = $("#" + summaryId);
    scheduler.selectedRecurrenceRule = currentRule;

    if (scheduler.selectedRecurrenceRule != null) {
        scheduler._setSummaryText(scheduler.selectedRecurrenceRule.summary);
    }
    else {
        scheduler.removeSchedule();
    }

}

scheduler.openScheduler = function () {
    scheduler._scheduleAnalytics(function () {
        if (scheduler.selectedRecurrenceRule == null) {
            scheduler.removeSchedule();
            return;
        }

        scheduler._setSummaryText(scheduler.selectedRecurrenceRule.summary);
    });
}

scheduler.removeSchedule = function () {
    scheduler.selectedRecurrenceRule = null;
    scheduler.jqSummaryText.html(localizeResource("not-scheduled"));
}

scheduler._scheduleAnalytics = function (callback) {
    var contentPage = "/vca/scheduler";
    var winTitle = localizeResource("scheduler");
    utils.openPopup(winTitle, contentPage, null, null, true, function () {
        callback();
    });
}

scheduler._setSummaryText = function (summary) {
    var template = kendo.template($("#vcaScheduleTmpl").html());
    var result = template(summary);
    scheduler.jqSummaryText.html(result);
}

scheduler._generateUI = function () {
    //generate 7 days of week
    for (var i = 1; i <= 7; i++) {
        var tmplData = {
            dayOfWeek: i
        }
        var htmlPiece = kendo.template($("#dayOfWeekTmpl").html())(tmplData);
        $(".scheduler_container .main_box").append(htmlPiece);
    }

    //checkbox click events
    $(".scheduler_container .main_box input[type=checkbox]").click(function () {
        var dayOfWeek = this.id.replace("chb", "");
        scheduler._setDayEditStatus(dayOfWeek, this.checked);
    });

    scheduler._populateRecurrenceRule(scheduler.selectedRecurrenceRule);
}

scheduler._addPeriod = function (dayOfWeek, startTime, endTime, removable) {
    if (!document.getElementById("chb" + dayOfWeek).checked) {
        return;
    }

    var periodId = scheduler._getAvailableId(dayOfWeek);
    var periodData = {
        dayOfWeek: dayOfWeek,
        periodId: periodId,
        removable: removable
    }

    var htmlPiece = kendo.template($("#periodTmpl").html())(periodData);
    $("#pBox" + dayOfWeek).append(htmlPiece);
    scheduler._createTimeRangePicker(periodData.periodId, startTime, endTime);
    scheduler._periodIdMap[dayOfWeek].push(periodId);
}

scheduler._removePeriod = function (dayOfWeek, periodId) {
    var element = document.getElementById(periodId);
    element.parentNode.removeChild(element);

    utils.removeArrayEntry(periodId, scheduler._periodIdMap[dayOfWeek]);
}

scheduler._setTimePickerStatus = function (periodId, enabled) {
    $("#startTime" + periodId).data("kendoTimePicker").enable(enabled);
    $("#endTime" + periodId).data("kendoTimePicker").enable(enabled);
}

scheduler._createTimeRangePicker = function (periodId, startDateTime, endDateTime) {

    //set defaults if null
    startDateTime = (startDateTime == null) ? scheduler._startTimeOfDay : startDateTime;
    endDateTime = (endDateTime == null) ? scheduler._endTimeOfDay : endDateTime;

    var startPickerId = "#startTime" + periodId;
    var endPickerId = "#endTime" + periodId;

    function startChange() {
        var startTime = kStart.value();
        if (startTime) {
            startTime = new Date(startTime);
            startTime.setMinutes(startTime.getMinutes() + this.options.interval);
            kEnd.min(startTime);
        }
    }

    function endChange() {
        var endTime = kEnd.value();
        if (endTime) {
            endTime = new Date(endTime);
            endTime.setMinutes(endTime.getMinutes() - this.options.interval);
            kStart.max(endTime);
        }
    }

    var kStart = $(startPickerId).kendoTimePicker({
        format: "HH:mm",
        interval: 30,
        min: scheduler._startTimeOfDay,
        max: scheduler._endTimeOfDay,
        value: startDateTime,
        change: startChange
    }).data("kendoTimePicker");

    var kEnd = $(endPickerId).kendoTimePicker({
        format: "HH:mm",
        interval: 30,
        min: scheduler._startTimeOfDay,
        max: scheduler._endTimeOfDay,
        value: endDateTime,
        change: endChange
    }).data("kendoTimePicker");
}

//id will have dayOfWeek prefix. e.g. monday period 2 => 12
scheduler._getAvailableId = function (dayOfWeek) {
    var idList = scheduler._periodIdMap[dayOfWeek];
    for (var i = 1; i < 100; i++) {
        var nextId = dayOfWeek + "" + i;
        if ($.inArray(nextId, idList) == -1) {
            return nextId;
        }
    }
}

scheduler._resetDayPeriods = function (dayOfWeek) {
    var myNode = document.getElementById("pBox" + dayOfWeek);
    while (myNode.firstChild) {
        myNode.removeChild(myNode.firstChild);
    }

    document.getElementById("chb" + dayOfWeek).checked = true;
    scheduler._periodIdMap[dayOfWeek] = [];
    scheduler._addPeriod(dayOfWeek, null, null, false);
}

scheduler._setDayEditStatus = function (dayOfWeek, enabled) {
    scheduler._resetDayPeriods(dayOfWeek);
    scheduler._setTimePickerStatus(dayOfWeek + "1", enabled);
    document.getElementById("chb" + dayOfWeek).checked = enabled;

    if (enabled) {
        $("#btnAdd" + dayOfWeek).removeClass("k-state-disabled");
    } else {
        $("#btnAdd" + dayOfWeek).addClass("k-state-disabled");
    }
}

scheduler._disableAllDays = function () {
    for (var i = 1; i <= 7; i++) {
        scheduler._setDayEditStatus(i, false);
    }

    $("#summaryInput").val("");
}

scheduler._getRecurrenceRule = function () {
    var recurrence = {};

    recurrence.periodsOfDays = {};
    var invalidRanges = [];
    for (var i = 1; i <= 7; i++) {
        var chBoxId = "chb" + i;
        if (document.getElementById(chBoxId).checked == false) {
            continue;
        }

        var periods = [];
        $.each(scheduler._periodIdMap[i], function (idx, periodId) {
            var startTime = $("#startTime" + periodId).data("kendoTimePicker").value();
            var endTime = $("#endTime" + periodId).data("kendoTimePicker").value();
            if (startTime == null || endTime == null) {
                invalidRanges.push(scheduler.dayOfWeekNames[i]);
                return false;
            }

            var startMinutes = startTime.getHours() * 60 + startTime.getMinutes();
            var endMinutes = endTime.getHours() * 60 + endTime.getMinutes();
            endMinutes = endMinutes == 1439 ? 1440 : endMinutes;

            if (startMinutes >= endMinutes) {
                invalidRanges.push(scheduler.dayOfWeekNames[i]);
                return false;
            }

            var period = {
                startMinutes: startMinutes,
                endMinutes: endMinutes
            };

            periods.push(period);
        });

        //check overlaps
        for (var j = 0; j < periods.length - 1; j++) {
            var current = periods[j];
            var next = periods[j + 1];
            if (current.endMinutes > next.startMinutes) {
                invalidRanges.push(scheduler.dayOfWeekNames[i]);
                break;
            }
        }

        recurrence.periodsOfDays[i] = periods;
    }

    if (invalidRanges.length > 0) {
        utils.popupAlert(localizeResource("invalid-time-range") + "<br/><br/>" + invalidRanges);
        return null;
    }

    if (utils.getMapSize(recurrence.periodsOfDays) == 0) {
        utils.popupAlert(localizeResource("no-schedule-day-selected"));
        return null;
    }

    //check summary text
    recurrence.summary = $("#summaryInput").val().trim();
    if (recurrence.summary.length == 0) {
        utils.popupAlert(localizeResource("empty-summary"));
        return null;
    }

    return recurrence;
}

scheduler._populateRecurrenceRule = function (recurrenceRule) {
    scheduler._disableAllDays();
    if (recurrenceRule == null) {
        return;
    }

    $("#summaryInput").val(recurrenceRule.summary);

    for (var i = 1; i <= 7; i++) {
        var dayOfWeek = i;
        var dayPeriods = recurrenceRule.periodsOfDays[dayOfWeek];
        if (dayPeriods == null || dayPeriods.length == 0) {
            continue;
        }

        //enable day
        scheduler._setDayEditStatus(dayOfWeek, true);

        $.each(dayPeriods, function (idx, period) {

            //start
            var startHours = (period.startMinutes / 60);
            var startMins = utils.modulo(period.startMinutes, 60);
            var startDate = new Date(2001, 1, 1, startHours, startMins);

            //end
            period.endMinutes = period.endMinutes == 1440 ? 1439 : period.endMinutes;
            var endHours = (period.endMinutes / 60);
            var endMins = utils.modulo(period.endMinutes, 60);
            var endDate = new Date(2001, 1, 1, endHours, endMins);

            if (idx == 0) { //first period pickers are already added
                var firstPeriodId = dayOfWeek + "1";

                var startKPicker = $("#startTime" + firstPeriodId).data("kendoTimePicker");
                startKPicker.value(startDate);
                startKPicker.enable(true);

                var endKPicker = $("#endTime" + firstPeriodId).data("kendoTimePicker");
                endKPicker.value(endDate);
                endKPicker.enable(true);

            } else {
                scheduler._addPeriod(dayOfWeek, startDate, endDate, true);
            }
        });
    }
}

scheduler._savePreset = function () {
    var recurrenceRule = scheduler._getRecurrenceRule();
    if (recurrenceRule == null) {
        return;
    }

    var contentUrl = "/vca/savepreset";
    utils.openPopup(localizeResource('save-as-preset'), contentUrl, 280, null, true, function () {
        if (!saveAsWin.choice) {
            return;
        }

        addSchedulePreset("", saveAsWin.presetName, recurrenceRule, function (responseData) {
            if (responseData == null || responseData.result != "ok") {
                utils.throwServerError(responseData);
                return;
            }
        }, null);
    });

}

scheduler._loadPreset = function () {
    var contentUrl = "/vca/presetloader";
    utils.openPopup(localizeResource('load-preset'), contentUrl, null, null, true, function () {
        if (!presetLoader.choice) {
            return;
        }
        scheduler._populateRecurrenceRule(presetLoader.recurrenceRule);
    });
}

scheduler._confirmAndSubmit = function () {
    scheduler.selectedRecurrenceRule = scheduler._getRecurrenceRule();
    if (scheduler.selectedRecurrenceRule == null) {
        return;
    }

    scheduler._closePopup();
}

scheduler._preview = function () {
    var rule = scheduler._getRecurrenceRule();
    if (rule == null) {
        return;
    }

    visPrd.init(rule.periodsOfDays);
    var contentPage = "/vca/visualizeschedule";
    utils.openPopup(localizeResource('schedule-viewer'), contentPage, null, null, true, function ()
    {
    });
};

scheduler._closePopup = function () {
    $("#schedulerCancelBtn").closest(".k-window-content").data("kendoWindow").close();
}