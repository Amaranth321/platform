var checkingAccessDone = false;

var liveViewAccess = false;
var currentOccupancyAccess = false;
var liveTrackAccess = false;
var poiAccess = false;

var cloudPlaybackAccess = false;
var nodePlaybackAccess = false;

var faceIndexAccess = false;
var humanTrafficAccess = false;
var peopleCountingAccess = false;
var passerbyAccess = false;
var crowdDetectionAccess = false;
var profilingAccess = false;
var intrusionAccess = false;
var perimeterDefenseAccess = false;
var loiteringAccess = false;
var objCountingAccess = false;
var videoBlurAccess = false;

var alertListAccess = false;
var faceReportAccess = false;
var trafficReportAccess = false;
var pCountingReportAccess = false;
var passerbyReportAccess = false;
var crowdReportAccess = false;
var profilingReportAccess = false;
var intrusionReportAccess = false;
var pdefenseReportAccess = false;
var loiteringReportAccess = false;
var objCountingReportAccess = false;
var vblurReportAccess = false;
var audiAttentionReportAccess = false;

var manageBucketAccess = false;
var manageAccessKeys = false;
var manageUserAccess = false;
var manageDeviceAccess = false;
var nodeListAccess = false;
var manageInventoryAccess = false;
var manageRoleAccess = false;
var manageSoftwareUpdateAccess = false;
var announcementAccess = false;
var auditLogAccess = false;
var labelMgmtAccess = false;
var manageperiodicReportAccess = false;
var periodicReportListAccess = false;
var manageposReportAccess = false;

var historicalAlertListAccess = false;
var labelSettingsAccess = false;
var labelNotificationsAccess = false;

var remoteShellAccess = false;
var nodelogAccess = false;
var localLicenseAccess = false;
var globalLicenseAccess = false;
var accountStatementsAccess = false;
var nodeInformationAccess = false;
var holidayCalendarAccess = false;
var scheduleTasksAccess =false;

var nodeBrowserAccess = false;
var internalSettingsAccess = false;


var contentReportsUIConfig = {
    // Reports BI
    trafficReportAccess: [],
    pCountingReportAccess: ['#showPeopleCountingChart'],
    crowdReportAccess: [],
    profilingReportAccess: ['#showGenderProfilingChart'],
    audiAttentionReportAccess: [],
    // Reports security
    intrusionReportAccess: ["#showSecurityAlertsChart"],
    pdefenseReportAccess: ["#showSecurityAlertsChart"],
    loiteringReportAccess: ["#showSecurityAlertsChart"],
    objCountingReportAccess: ["#showSecurityAlertsChart"],
    vblurReportAccess: ["#showSecurityAlertsChart"],
    periodicReportListAccess: ["#showSecurityAlertsChart"],
    faceReportAccess: ["#showSecurityAlertsChart"]
};

var onReadyActionList = [];

function registerOnAuthorizedEvent(onReadyAction){
    onReadyActionList.push(onReadyAction);
}

//will be called inside page head
function checkAndUpdateFeaturesForUI() {
    getUserFeatures("", function (responseData) {
        if (responseData.result == "ok" && responseData.features != null) {
            updateAccess(responseData.features);
            updateUI();
            checkingAccessDone = true;
        } else {
            utils.throwServerError(responseData);
        }
    }, null);
}

function updateAccess(features) {
    var featuresAllowed = "";
    for (var i = 0; i < features.length; i++) {
        featuresAllowed += features[i].name + ';';
    }

    liveViewAccess = (featuresAllowed.indexOf("live-view") != -1);
    currentOccupancyAccess = (featuresAllowed.indexOf("current-occupancy") != -1);
    liveTrackAccess = (featuresAllowed.indexOf("live-track") != -1);
    poiAccess = (featuresAllowed.indexOf("poi") != -1);

    cloudPlaybackAccess = (featuresAllowed.indexOf("cloud-playback") != -1);
    nodePlaybackAccess = (featuresAllowed.indexOf("node-playback") != -1);

    faceIndexAccess = (featuresAllowed.indexOf("analytics-face-indexing") != -1);
    humanTrafficAccess = (featuresAllowed.indexOf("analytics-traffic-flow") != -1);
    peopleCountingAccess = (featuresAllowed.indexOf("analytics-people-counting") != -1);
    passerbyAccess = (featuresAllowed.indexOf("analytics-passerby") != -1);
    crowdDetectionAccess = (featuresAllowed.indexOf("analytics-crowd-detection") != -1);
    profilingAccess = (featuresAllowed.indexOf("analytics-audience-profiling") != -1);
    intrusionAccess = (featuresAllowed.indexOf("analytics-area-intrusion") != -1);
    perimeterDefenseAccess = (featuresAllowed.indexOf("analytics-perimeter-defense") != -1);
    loiteringAccess = (featuresAllowed.indexOf("analytics-area-loitering") != -1);
    objCountingAccess = (featuresAllowed.indexOf("analytics-object-counting") != -1);
    videoBlurAccess = (featuresAllowed.indexOf("analytics-video-blur") != -1);

    alertListAccess = (featuresAllowed.indexOf("alert-list") != -1);
    faceReportAccess = (featuresAllowed.indexOf("report-face-indexing") != -1);
    trafficReportAccess = (featuresAllowed.indexOf("report-traffic-flow") != -1);
    pCountingReportAccess = (featuresAllowed.indexOf("report-people-counting") != -1);
    passerbyReportAccess = (featuresAllowed.indexOf("report-passerby") != -1);
    crowdReportAccess = (featuresAllowed.indexOf("report-crowd-detection") != -1);
    profilingReportAccess = (featuresAllowed.indexOf("report-audience-profiling") != -1);
    intrusionReportAccess = (featuresAllowed.indexOf("report-area-intrusion") != -1);
    pdefenseReportAccess = (featuresAllowed.indexOf("report-perimeter-defense") != -1);
    loiteringReportAccess = (featuresAllowed.indexOf("report-area-loitering") != -1);
    objCountingReportAccess = (featuresAllowed.indexOf("report-object-counting") != -1);
    vblurReportAccess = (featuresAllowed.indexOf("report-video-blur") != -1);
    periodicReportListAccess = (featuresAllowed.indexOf("periodic-reports") != -1);
    audiAttentionReportAccess = (featuresAllowed.indexOf("report-audience-attention") != -1);

    manageBucketAccess = (featuresAllowed.indexOf("bucket-management") != -1);
    manageAccessKeys = (featuresAllowed.indexOf("access-key-management") != -1);
    manageUserAccess = (featuresAllowed.indexOf("user-management") != -1);
    manageDeviceAccess = (featuresAllowed.indexOf("device-management") != -1);
    nodeListAccess = (featuresAllowed.indexOf("kai-nodes") != -1);
    manageInventoryAccess = (featuresAllowed.indexOf("inventory-management") != -1);
    manageRoleAccess = (featuresAllowed.indexOf("role-management") != -1);
    manageSoftwareUpdateAccess = (featuresAllowed.indexOf("software-update-management") != -1) && kupapi.applicationType == "cloud";
    manageperiodicReportAccess = (featuresAllowed.indexOf("periodic-report-management") != -1);
    manageposReportAccess = (featuresAllowed.indexOf("pos-management") != -1);
    announcementAccess = (featuresAllowed.indexOf("announcement-management") != -1);
    auditLogAccess = (featuresAllowed.indexOf("audit-log") != -1);
    labelMgmtAccess = (featuresAllowed.indexOf("label-management") != -1);

    historicalAlertListAccess = (featuresAllowed.indexOf("historical-alerts") != -1);
    labelSettingsAccess = (featuresAllowed.indexOf("label-settings") != -1);
    labelNotificationsAccess = (featuresAllowed.indexOf("label-notifications") != -1);

    //customer support
    remoteShellAccess = (featuresAllowed.indexOf("remote-shell") != -1);
    nodelogAccess = (featuresAllowed.indexOf("node-log-management") != -1);
    localLicenseAccess = (featuresAllowed.indexOf("local-license-management") != -1);
    globalLicenseAccess = (featuresAllowed.indexOf("global-license-management") != -1);
    accountStatementsAccess = (featuresAllowed.indexOf("account-statements") != -1);
    nodeInformationAccess = (featuresAllowed.indexOf("node-information") != -1);
    holidayCalendarAccess = (featuresAllowed.indexOf("holiday-calendar") != -1);
    scheduleTasksAccess = (featuresAllowed.indexOf("schedule-tasks") != -1);

    nodeBrowserAccess = (featuresAllowed.indexOf("node-browser") != -1);
    internalSettingsAccess = (featuresAllowed.indexOf("server-internal-settings") != -1);

}

function updateUI() {

    //
    // id ending with 'Tab' are from the left menu bar
    //
    var rightMenuExists = false;
    var rightMenu;

    if ($("#menu-panelbar").data("kendoPanelBar")) {
        rightMenuExists = true;
        rightMenu = $("#menu-panelbar").data("kendoPanelBar");
    }
    var updateContentReportsUI = (function (config) {
        var showList = [],
            hideList = [],
            finalHideList = [];
        //get show,hide list
        $.each(config, function (report, selectorAry) {
            $.each(selectorAry, function (i, selector) {
                if (window[report]) {
                    $(selector).css("display", "inline-block");
                    showList.push(selector);
                } else {
                    hideList.push(selector);
                }

            });
        });
        //get want to hide's list
        $.each(hideList, function (i, hideVal) {
            var tmp = true;
            $.each(showList, function (j, showVal) {
                if (hideVal === showVal) {
                    tmp = false;
                    return false;
                }
            });
            if (tmp) {
                finalHideList.push(hideVal)
            }
        });
        //remove or hide  html
        $.each(finalHideList, function (i, hideVal) {
            $(hideVal).hide();
        });
    })(contentReportsUIConfig);

    //Live Monitoring
    if (!liveViewAccess) {
        if (rightMenuExists)
        {
            rightMenu.remove("#liveViewTab");
        }
    }
    else
    {
        $(".liveView_block").show();
    }

    if (!currentOccupancyAccess) {
        if (rightMenuExists)
            rightMenu.remove("#currentOccupancyTab");
    }
    if (!liveTrackAccess) {
        if (rightMenuExists)
            rightMenu.remove("#liveTrackTab");
    }
    if (!poiAccess) {
        if (rightMenuExists)
            rightMenu.remove("#poiTab");
    }

    //Recording
    if (kupapi.applicationType != "cloud" || !cloudPlaybackAccess) {
        if (rightMenuExists)
            rightMenu.remove("#cloudPlaybackTab");
    }
    if (kupapi.applicationType != "node" || !nodePlaybackAccess) {
        if (rightMenuExists)
            rightMenu.remove("#nodePlaybackTab");
    }

    //VCA BI
    if (!faceIndexAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#faceIndexingTab");
        }
    }
    if (!humanTrafficAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#trafficFlowTab");
        }
    }
    if (!peopleCountingAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#peopleCountingTab");
        }
    }
    if (!passerbyAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#passerbyTab");
        }
    }
    if (!crowdDetectionAccess) {

        if (rightMenuExists) {
            rightMenu.remove("#crowdDetectionTab");
        }
    }
    if (!profilingAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#profilingTab");
        }
    }

    // VCA security
    if (!intrusionAccess) {
        if (rightMenuExists)
            rightMenu.remove("#intrusionTab");
    }
    if (!perimeterDefenseAccess) {
        if (rightMenuExists)
            rightMenu.remove("#perimeterDefenseTab");
    }
    if (!loiteringAccess) {
        if (rightMenuExists)
            rightMenu.remove("#areaLoiteringTab");
    }
    if (!objCountingAccess) {
        if (rightMenuExists)
            rightMenu.remove("#objectCountingTab");
    }
    if (!videoBlurAccess) {
        if (rightMenuExists)
            rightMenu.remove("#blurVideoTab");
    }

    // Reports BI
    if (!faceReportAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#faceReportTab");
        }
    }
    if (!trafficReportAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#trafficReportTab");
        }
    }
    if (!pCountingReportAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#pCountingReportTab");
        }
    }
    if (!passerbyReportAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#passerbyReportTab");
        }
    }
    if (!crowdReportAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#crowdReportTab");
        }
    }
    if (!profilingReportAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#profilingReportTab");
        }
    }
    if (!audiAttentionReportAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#attentionReportTab");
        }
    }

    // Reports security
    if (!intrusionReportAccess) {
        if (rightMenuExists)
            rightMenu.remove("#intrusionReportTab");
    }
    if (!pdefenseReportAccess) {
        if (rightMenuExists)
            rightMenu.remove("#pdefenseReportTab");
    }
    if (!loiteringReportAccess) {
        if (rightMenuExists)
            rightMenu.remove("#loiteringReportTab");
    }
    if (!objCountingReportAccess) {
        if (rightMenuExists)
            rightMenu.remove("#objCountingReportTab");
    }
    if (!vblurReportAccess) {
        if (rightMenuExists)
            rightMenu.remove("#videoBlurReportTab");
    }
    if (!periodicReportListAccess) {

        if (rightMenuExists)
            rightMenu.remove("#periodicReportListTab");
    }

    //Admin Settings
    if (!manageBucketAccess) {
        if (rightMenuExists)
            rightMenu.remove("#bucketListTab");
    }
    if (!manageAccessKeys) {
        if (rightMenuExists)
            rightMenu.remove("#accessKeysTab");
    }
    if (!manageDeviceAccess) {
        if (rightMenuExists)
            rightMenu.remove("#deviceListTab");
    }
    if (!nodeListAccess) {
        if (rightMenuExists)
            rightMenu.remove("#nodeListTab");
    }
    if (!manageUserAccess) {
        if (rightMenuExists)
            rightMenu.remove("#userListTab");
    }
    if (!manageInventoryAccess) {
        if (rightMenuExists)
            rightMenu.remove("#inventoryTab");
    }
    if (!manageRoleAccess) {
        if (rightMenuExists)
            rightMenu.remove("#roleListTab");
    }
    if (!manageSoftwareUpdateAccess) {
        if (rightMenuExists)
            rightMenu.remove("#softwareUpdateTab");
    }
    if (!announcementAccess) {
        if (rightMenuExists)
            rightMenu.remove("#announcementListTab");
    }
    if (!manageperiodicReportAccess) {
        if (rightMenuExists)
            rightMenu.remove("#periodicReportTab");
    }
    if (!manageposReportAccess) {
        if (rightMenuExists)
            rightMenu.remove("#POSReportTab");
    }
    if (!auditLogAccess) {
        if (rightMenuExists)
            rightMenu.remove("#auditlogTab");
    }
    if (!labelMgmtAccess) {
        if (rightMenuExists)
            rightMenu.remove("#labelManagementTab");
    }
    if (!localLicenseAccess) {
        if (rightMenuExists)
            rightMenu.remove("#localLicensesTab");
    }

    //notification Management
    if (!historicalAlertListAccess) {
        if (rightMenuExists)
            rightMenu.remove("#historicalalertTab");
        $("#goToSecurityAlert").remove();
    }
    if (!labelSettingsAccess) {
        if (rightMenuExists)
            rightMenu.remove("#labelSettingsTab");
    }
    if (!labelNotificationsAccess) {
        if (rightMenuExists)
            rightMenu.remove("#labelNotificationsTab");
        $("#goToLabelAlert").remove();
    }

    //customer support
    if (!remoteShellAccess) {
        if (rightMenuExists) {
            rightMenu.remove("#remoteShellTab");
        }
    }
    if (!nodelogAccess) {
        if (rightMenuExists)
            rightMenu.remove("#pullnodelog");
    }
    if (!globalLicenseAccess) {
        if (rightMenuExists)
            rightMenu.remove("#globalLicensesTab");
    }
    if (!accountStatementsAccess) {
        if (rightMenuExists)
            rightMenu.remove("#accountStatements");
    }
    if (!nodeInformationAccess) {
        if (rightMenuExists)
            rightMenu.remove("#nodeInformation");
    }
    if (!holidayCalendarAccess) {
        if (rightMenuExists)
            rightMenu.remove("#holidayCalendar");
    }
    if (!scheduleTasksAccess) {
        if (rightMenuExists)
            rightMenu.remove("#scheduleTasks");
    }
    if (!nodeBrowserAccess) {
        if (rightMenuExists)
            rightMenu.remove("#nodeBrowser");
    }
    if (!internalSettingsAccess) {
        if (rightMenuExists)
            rightMenu.remove("#internalSettings");
    }


    // Remove Empty Tab groups
    if (rightMenuExists) {
        if ($("#liveMonitoringTab li").size() == 0)
            rightMenu.remove("#liveMonitoringTab");
        if ($("#recordingTab li").size() == 0)
            rightMenu.remove("#recordingTab");

        if (!humanTrafficAccess &&
            !peopleCountingAccess &&
            !passerbyAccess &&
            !crowdDetectionAccess &&
            !profilingAccess)
            rightMenu.remove("#vcaBiTitle");

        if (!faceIndexAccess && !intrusionAccess && !perimeterDefenseAccess && !loiteringAccess && !objCountingAccess && !videoBlurAccess)
            rightMenu.remove("#vcaSecurityTitle");

        if (!trafficReportAccess &&
            !pCountingReportAccess &&
            !passerbyReportAccess &&
            !crowdReportAccess &&
            !profilingReportAccess &&
            !audiAttentionReportAccess)
            rightMenu.remove("#biReportTitle");

        if (!faceReportAccess && !intrusionReportAccess && !pdefenseReportAccess && !loiteringReportAccess && !objCountingReportAccess && !vblurReportAccess)
            rightMenu.remove("#securityReportTitle");

        if (!periodicReportListAccess)
            rightMenu.remove("#otherReportTitle");


        if ($("#biTab li").size() == 0)
            rightMenu.remove("#biTab");
        if ($("#vehicleManagementTab li").size() == 0)
            rightMenu.remove("#vehicleManagementTab");
        if ($("#reportTab li").size() == 0)
            rightMenu.remove("#reportTab");
        if ($("#adminSettingsTab li").size() == 0)
            rightMenu.remove("#adminSettingsTab");

        if ($("#notificationManagementTab li").size() == 0)
            rightMenu.remove("#notificationManagementTab");

        if ($("#customerSupportTab li").size() == 0)
            rightMenu.remove("#customerSupportTab");

        if ($("#cloudServerTab li").size() == 0)
            rightMenu.remove("#cloudServerTab");
    }

    activateMenus();
}

function activateMenus() {

    $("#menu-panelbar").css('visibility', 'visible');

    var rightMenuBar = $("#menu-panelbar").data("kendoPanelBar");

    if (rightMenuBar == null)
        return;

    //This will expand the right menu bar based on the current page
    setTimeout(function () {

        if (location.href.indexOf("live/fullview") != -1) {
            rightMenuBar.expand($("#liveViewTab").parent().parent()).
                select($("#liveViewTab").parent().parent());
            $("#liveViewTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("/realtime/currentoccupancy") != -1) {
            rightMenuBar.expand($("#currentOccupancyTab").parent().parent()).
            select($("#currentOccupancyTab").parent().parent());
            $("#currentOccupancyTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("/location/track") != -1) {
            rightMenuBar.expand($("#liveTrackTab").parent().parent()).
                select($("#liveTrackTab").parent().parent());
            $("#liveTrackTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("playback/cloud") != -1) {
            rightMenuBar.expand($("#cloudPlaybackTab").parent().parent())
                .select($("#cloudPlaybackTab").parent().parent());
            $("#cloudPlaybackTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("playback/node") != -1) {
            rightMenuBar.expand($("#nodePlaybackTab").parent().parent())
                .select($("#nodePlaybackTab").parent().parent());
            $("#nodePlaybackTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("poi/list") != -1) {
            rightMenuBar.expand($("#poiTab").parent().parent()).
                select($("#poiTab").parent().parent());
            $("#poiTab").css('color', '#f6ae40');

            //Reports bi
        } else if (location.href.indexOf("event/list") != -1) {
            rightMenuBar.expand($("#eventTab").parent().parent()).
                select($("#eventTab").parent().parent());
            $("#eventTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/faceindexing") != -1) {
            rightMenuBar.expand($("#faceReportTab").parent().parent()).
                select($("#faceReportTab").parent().parent());
            $("#faceReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/trafficflow") != -1) {
            rightMenuBar.expand($("#trafficReportTab").parent().parent())
                .select($("#trafficReportTab").parent().parent());
            $("#trafficReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/peoplecounting") != -1) {
            rightMenuBar.expand($("#pCountingReportTab").parent().parent())
                .select($("#pCountingReportTab").parent().parent());
            $("#pCountingReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/passerby") != -1) {
            rightMenuBar.expand($("#passerbyReportTab").parent().parent())
                .select($("#passerbyReportTab").parent().parent());
            $("#passerbyReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/crowd") != -1) {
            rightMenuBar.expand($("#crowdReportTab").parent().parent()).
                select($("#crowdReportTab").parent().parent());
            $("#crowdReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/profiling") != -1) {
            rightMenuBar.expand($("#profilingReportTab").parent().parent()).
                select($("#profilingReportTab").parent().parent());
            $("#profilingReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/attention") != -1) {
            rightMenuBar.expand($("#attentionReportTab").parent().parent()).
                select($("#attentionReportTab").parent().parent());
            $("#attentionReportTab").css('color', '#f6ae40');

            //Reports security
        } else if (location.href.indexOf("report/intrusion") != -1) {
            rightMenuBar.expand($("#intrusionReportTab").parent().parent()).
                select($("#intrusionReportTab").parent().parent());
            $("#intrusionReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/pdefense") != -1) {
            rightMenuBar.expand($("#pdefenseReportTab").parent().parent()).
                select($("#pdefenseReportTab").parent().parent());
            $("#pdefenseReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/loitering") != -1) {
            rightMenuBar.expand($("#loiteringReportTab").parent().parent())
                .select($("#loiteringReportTab").parent().parent());
            $("#loiteringReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/objcounting") != -1) {
            rightMenuBar.expand($("#objCountingReportTab").parent().parent()).
                select($("#objCountingReportTab").parent().parent());
            $("#objCountingReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/videoblur") != -1) {
            rightMenuBar.expand($("#videoBlurReportTab").parent().parent()).
                select($("#videoBlurReportTab").parent().parent());
            $("#videoBlurReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/listperiodicreports") != -1) {
            rightMenuBar.expand($("#periodicReportListTab").parent().parent()).
                select($("#periodicReportListTab").parent().parent());
            $("#periodicReportListTab").css('color', '#f6ae40');
            //vca bi
        } else if (location.href.indexOf("vca/peoplecounting") != -1) {
            rightMenuBar.expand($("#peopleCountingTab").parent().parent()).
                select($("#peopleCountingTab").parent().parent());
            $("#peopleCountingTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("vca/passerby") != -1) {
            rightMenuBar.expand($("#passerbyTab").parent().parent()).
                select($("#passerbyTab").parent().parent());
            $("#passerbyTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("vca/profiling") != -1) {
            rightMenuBar.expand($("#profilingTab").parent().parent()).
                select($("#profilingTab").parent().parent());
            $("#profilingTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("vca/faceindexing") != -1) {
            rightMenuBar.expand($("#faceIndexingTab").parent().parent()).
                select($("#faceIndexingTab").parent().parent());
            $("#faceIndexingTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("vca/trafficflow") != -1) {
            rightMenuBar.expand($("#trafficFlowTab").parent().parent()).
                select($("#trafficFlowTab").parent().parent());
            $("#trafficFlowTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("vca/crowd") != -1) {
            rightMenuBar.expand($("#crowdDetectionTab").parent().parent()).
                select($("#crowdDetectionTab").parent().parent());
            $("#crowdDetectionTab").css('color', '#f6ae40');
            //vca security
        } else if (location.href.indexOf("vca/areaintrusion") != -1) {
            rightMenuBar.expand($("#intrusionTab").parent().parent()).
                select($("#intrusionTab").parent().parent());
            $("#intrusionTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("vca/pdefense") != -1) {
            rightMenuBar.expand($("#perimeterDefenseTab").parent().parent()).
                select($("#perimeterDefenseTab").parent().parent());
            $("#perimeterDefenseTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("vca/loitering") != -1) {
            rightMenuBar.expand($("#areaLoiteringTab").parent().parent()).
                select($("#areaLoiteringTab").parent().parent());
            $("#areaLoiteringTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("vca/objectcounting") != -1) {
            rightMenuBar.expand($("#objectCountingTab").parent().parent()).
                select($("#objectCountingTab").parent().parent());
            $("#objectCountingTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("vca/blur") != -1) {
            rightMenuBar.expand($("#blurVideoTab").parent().parent()).
                select($("#blurVideoTab").parent().parent());
            $("#blurVideoTab").css('color', '#f6ae40');

            //admin settings
        } else if (location.href.indexOf("bucket/list") != -1 || location.href.indexOf("bucket/manager") != -1) {
            rightMenuBar.expand($("#bucketListTab").parent().parent()).
                select($("#bucketListTab").parent().parent());
            $("#bucketListTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("access/list") != -1) {
            rightMenuBar.expand($("#accessKeysTab").parent().parent()).
                select($("#accessKeysTab").parent().parent());
            $("#accessKeysTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("device/list") != -1) {
            rightMenuBar.expand($("#deviceListTab").parent().parent()).
                select($("#deviceListTab").parent().parent());
            $("#deviceListTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("node/list") != -1) {
            rightMenuBar.expand($("#nodeListTab").parent().parent()).
                select($("#nodeListTab").parent().parent());
            $("#nodeListTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("inventory/list") != -1) {
            rightMenuBar.expand($("#inventoryTab").parent().parent()).
                select($("#inventoryTab").parent().parent());
            $("#inventoryTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("softwareupdate/list") != -1) {
            rightMenuBar.expand($("#softwareUpdateTab").parent().parent()).
                select($("#softwareUpdateTab").parent().parent());
            $("#softwareUpdateTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("user/list") != -1) {
            rightMenuBar.expand($("#userListTab").parent().parent()).
                select($("#userListTab").parent().parent());
            $("#userListTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("role/list") != -1) {
            rightMenuBar.expand($("#roleListTab").parent().parent()).
                select($("#roleListTab").parent().parent());
            $("#roleListTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("announcement/list") != -1) {
            rightMenuBar.expand($("#announcementListTab").parent().parent()).
                select($("#announcementListTab").parent().parent());
            $("#announcementListTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/periodicsettings") != -1) {
            rightMenuBar.expand($("#periodicReportTab").parent().parent()).
                select($("#periodicReportTab").parent().parent());
            $("#periodicReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("report/possettings") != -1) {
            rightMenuBar.expand($("#POSReportTab").parent().parent()).
                select($("#POSReportTab").parent().parent());
            $("#POSReportTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("auditlog/list") != -1) {
            rightMenuBar.expand($("#auditlogTab").parent().parent()).
                select($("#auditlogTab").parent().parent());
            $("#auditlogTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("license/localnodelicenses") != -1) {
            rightMenuBar.expand($("#localLicensesTab").parent().parent()).
                select($("#localLicensesTab").parent().parent());
            $("#localLicensesTab").css('color', '#f6ae40');

            // Notification Management
        } else if (location.href.indexOf("notification/securityalerts") != -1) {
            rightMenuBar.expand($("#historicalalertTab").parent().parent()).
                select($("#historicalalertTab").parent().parent());
            $("#historicalalertTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("notification/labelsettings") != -1) {
            rightMenuBar.expand($("#labelSettingsTab").parent().parent()).
                select($("#labelSettingsTab").parent().parent());
            $("#labelSettingsTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("notification/labelnotifications") != -1) {
            rightMenuBar.expand($("#labelNotificationsTab").parent().parent()).
                select($("#labelNotificationsTab").parent().parent());
            $("#labelNotificationsTab").css('color', '#f6ae40');

            //Customer support
        } else if (location.href.indexOf("support/remoteshell") != -1) {
            rightMenuBar.expand($("#remoteShellTab").parent().parent()).
                select($("#remoteShellTab").parent().parent());
            $("#remoteShellTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("support/nodeloglist") != -1) {
            rightMenuBar.expand($("#pullnodelog").parent().parent()).
                select($("#pullnodelog").parent().parent());
            $("#pullnodelog").css('color', '#f6ae40');
        } else if (location.href.indexOf("license/globalnodelicenses") != -1) {
            rightMenuBar.expand($("#globalLicensesTab").parent().parent()).
                select($("#globalLicensesTab").parent().parent());
            $("#globalLicensesTab").css('color', '#f6ae40');
        } else if (location.href.indexOf("support/accountstatements") != -1) {
            rightMenuBar.expand($("#accountStatements").parent().parent()).
                select($("#accountStatements").parent().parent());
            $("#accountStatements").css('color', '#f6ae40');
        } else if (location.href.indexOf("support/nodeinformation") != -1) {
            rightMenuBar.expand($("#nodeInformation").parent().parent()).
                select($("#nodeInformation").parent().parent());
            $("#nodeInformation").css('color', '#f6ae40');
        } else if (location.href.indexOf("/support/holidaycalendar") != -1) {
            rightMenuBar.expand($("#holidayCalendar").parent().parent()).
            select($("#holidayCalendar").parent().parent());
            $("#holidayCalendar").css('color', '#f6ae40');
        } else if (location.href.indexOf("/scheduletask/list") != -1) {
            rightMenuBar.expand($("#scheduleTasks").parent().parent()).
            select($("#scheduleTasks").parent().parent());
            $("#scheduleTasks").css('color', '#f6ae40');

            //cloud server
        } else if (location.href.indexOf("/cloudmanager/nodebrowser") != -1) {
            rightMenuBar.expand($("#nodeBrowser").parent().parent()).
                select($("#nodeBrowser").parent().parent());
            $("#nodeBrowser").css('color', '#f6ae40');
        } else if (location.href.indexOf("/cloudmanager/settings") != -1) {
            rightMenuBar.expand($("#internalSettings").parent().parent()).
                select($("#internalSettings").parent().parent());
            $("#internalSettings").css('color', '#f6ae40');
        }


        //intialize submenu if exist (always at last)
        intializeSubmenu(1000);

        $.each(onReadyActionList, function(i, action)
        {
            action();
        })
    }, 100);
}

function submitLogout() {
    logout("", function (responseData) {
        if (responseData.result == "ok") {
            notificationManager.clearCookie();
            window.location.href = "/" + kupBucket;
        } else {
            utils.popupAlert(responseData.reason);
        }
    }, null);
}

function intializeSubmenu(time) {
    if (document.getElementById("subMenu") != null) {
        setTimeout(function () {
            $("li.toggle-bundle").show();
            var width = $("#sidebar").width();
            $(".sidebar-inner").animate({
                'marginLeft': "-" + width + "px"
            }, 300, function () {
                $("#sidebar").animate({
                    scrollTop: 0
                }, 600);
            });
        }, time);
    }
}
