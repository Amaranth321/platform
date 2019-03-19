/**
 *
 * @author Aye Maung
 *
 */
var VcaMgr = (function ()
{
    var vcaStatus = {
        WAITING: "WAITING",
        RUNNING: "RUNNING",
        NOT_SCHEDULED: "NOT_SCHEDULED",
        DISABLED: "DISABLED",
        ERROR: "ERROR"
    };

    var programType = {
        KAI_X1: "KAI_X1",
        KAI_X2: "KAI_X2",
        KAI_X3: "KAI_X3"
    };

    var vcaLookupMap = {};

    var attachDetails = function (instanceList)
    {
        $.each(instanceList, function (i, inst)
        {
            var device = DvcMgr.getDevice(inst.coreDeviceId);
            inst.deviceName = DvcMgr.getDeviceName(inst.coreDeviceId);
            inst.channelName = DvcMgr.getChannelName(inst.coreDeviceId, inst.channelId);
            inst.address = device.address;
            inst.latitude = device.latitude;
            inst.longitude = device.longitude;
            inst.deviceStatus = DvcMgr.getDeviceStatus(inst.coreDeviceId);
            inst.channelStatus = DvcMgr.getChannelStatus(inst.coreDeviceId, inst.channelId);
            inst.vcaType = BackendUtil.parseVcaType(inst.type);
            inst.runningOnCloud = !DvcMgr.isKaiNode(device) && kupapi.onCloud();

            //store label
            var storeLabel = LabelMgr.getAssignedStoreLabel(inst.coreDeviceId, inst.channelId);
            inst.storeLabelName = storeLabel ? storeLabel.name : localizeResource("no-store-labels-assigned");

            vcaLookupMap[inst.instanceId] = inst;
        });
    };

    var viewSchedule = function (instanceId)
    {
        var instance = vcaLookupMap[instanceId];
        var periodsOfDays = (instance && instance.recurrenceRule) ? instance.recurrenceRule.periodsOfDays : null;
        visPrd.init(periodsOfDays);
        var contentPage = "/vca/visualizeschedule";
        utils.openPopup(localizeResource('schedule-viewer'), contentPage, null, null, true, function ()
        {
        });
    };

    var openGmaskTool = function (currentRegions, coreDeviceId, channelId, onClosed)
    {
        if (coreDeviceId == null || channelId == null)
        {
            console.error("Null IDs");
            return;
        }

        utils.showLoadingOverlay();
        DvcMgr.getCameraSnapshot(coreDeviceId, channelId, function (jpegUrl)
        {
            utils.hideLoadingOverlay();

            gMaskTool.init(jpegUrl, currentRegions);

            //open popup
            var contentPage = "/vca/gMaskTool";
            var winTitle = localizeResource("configure-gmask");
            utils.openPopup(winTitle, contentPage, null, null, true, function ()
            {
                onClosed(gMaskTool.saveChanges, gMaskTool.currentRegions);
            });
        });
    };

    var updateGmaskStatus = function (gmaskRegions)
    {
        if (gmaskRegions == null || gmaskRegions.length == 0)
        {
            $(".config_gmask .regions").text(localizeResource("none"));
        }
        else
        {
            var template = kendo.template($("#gmaskNameTmpl").html());
            var result = template(gmaskRegions);
            $(".config_gmask .regions").html(result);
        }
    };

    var removeGmaskRegion = function (regionName)
    {
        var index = -1;
        $.each(gmaskRegions, function (i, r)
        {
            if (r.name == regionName)
            {
                index = i;
                return false;
            }
        });

        if (index < 0)
        {
            return;
        }

        gmaskRegions.splice(index, 1);
        updateGmaskStatus(gmaskRegions);
    };

    var viewVcaPrograms = function (nodeId)
    {
        var contentPage = "/vca/supportedprograms?nodeId=" + nodeId;
        utils.openPopup(localizeResource('supported-vcas'), contentPage, null, null, true, function ()
        {
        });
    };

    var viewNodeVcaConcurrency = function (nodeId, nodeName, maxVcaConcurrency)
    {
        var contentPage = "/vca/vcaconcurrency?nodeId=" + nodeId + "&maxVcaConcurrency=" + maxVcaConcurrency;
        var title = localizeResource('Scheduled-vca-load') + " (" + nodeName + ")";
        utils.openPopup(title, contentPage, null, null, true, function ()
        {
        });
    };

    var getInstanceList = function (vcaType, onReady)
    {
        var typeName = "all";
        if (vcaType)
        {
            typeName = getVcaTypeInfo(vcaType).typeName;
        }
        listRunningAnalytics("", typeName, function (responseData)
        {
            if (responseData.result != "ok")
            {
                console.error(JSON.stringify(responseData, null, 2));
                onReady([]);
                return;
            }

            var instanceList = responseData.instances;
            attachDetails(instanceList);
            onReady(instanceList);
        });
    };

    var getInstanceDetails = function (instanceId)
    {
        return vcaLookupMap[instanceId];
    };

    var getVcaTypeInfo = function (vcaType)
    {
        if (vcaType == null)
        {
            return null;
        }
        return backend.VcaTypeInfo[vcaType];
    };

    return {
        VcaStatus: vcaStatus,
        Program: programType,
        viewSchedule: viewSchedule,
        openGmaskTool: openGmaskTool,   //Requires gmaskTool.js
        updateGmaskStatus: updateGmaskStatus,
        removeGmaskRegion: removeGmaskRegion,
        viewVcaPrograms: viewVcaPrograms,
        viewNodeVcaConcurrency: viewNodeVcaConcurrency,
        getInstanceList: getInstanceList,
        getInstanceDetails: getInstanceDetails,
        getVcaTypeInfo: getVcaTypeInfo
    }

})();