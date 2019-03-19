/**
 * call DvcMgr.ready() and LabelMgr.ready() first
 *
 * @author Aye Maung
 */
var LabelMgr = (function ()
{
    var cache = {
        labelIdMap: {},
        labelNameMap: {},
        deviceAllLabelMap: {},      //key is device id pair, value is the label obj
        deviceStoreLabelMap: {},    //key is device id pair, value is the label obj
        allLabelAssignees: {},      //key is labelId, value is a list of device channel pairs
        storeLabelAssignees: {}     //key is labelId, value is a list of device channel pairs
    };

    var ready = function (onReadyDone)
    {
        getUserAccessibleLabels(function (responseData)
        {
            if (responseData.result != "ok")
            {
                utils.throwServerError(responseData);
                updateLabelCache([]);
            }
            else
            {
                updateLabelCache(responseData.labels);
            }
            onReadyDone();
        });
    };

    var updateLabelCache = function (labels)
    {
        //clear cache
        cache.deviceAllLabelMap = {};
        cache.deviceStoreLabelMap = {};

        cache.allLabelAssignees = {};
        cache.storeLabelAssignees = {};

        //lookup maps
        labels.forEach(function (labelObj)
        {
            cache.labelIdMap[labelObj.labelId] = labelObj;
            cache.labelNameMap[labelObj.name] = labelObj;
        });

        $.each(DvcMgr.getUserDeviceList(), function (i, dvc)
        {
            if (!DvcMgr.isKaiNode(dvc))
            {
                return true;
            }

            dvc.channelLabels.forEach(function (labelAssignment)
            {
                var channelId = labelAssignment["channelId"];
                var assignedLabelIdList = labelAssignment["labels"];
                var cacheKey = DvcMgr.composeIdPairKey(dvc.deviceId, channelId);
                cache.deviceAllLabelMap[cacheKey] = [];

                $.each(assignedLabelIdList, function (i, labelId)
                {
                    var labelObj = cache.labelIdMap[labelId];


                    //update device-label cache
                    cache.deviceAllLabelMap[cacheKey].push(labelObj);
                    //update label-devices cache
                    var currentAssigneesAll = cache.allLabelAssignees[labelId];
                    currentAssigneesAll = currentAssigneesAll || [];
                    currentAssigneesAll.push({coreDeviceId: dvc.deviceId, channelId: channelId});
                    cache.allLabelAssignees[labelId] = currentAssigneesAll;

                    if (labelObj && labelObj.type == "STORE")
                    {
                        //update device-label cache
                        cache.deviceStoreLabelMap[cacheKey] = labelObj;

                        //update label-devices cache
                        var currentAssignees = cache.storeLabelAssignees[labelId];
                        currentAssignees = currentAssignees || [];
                        currentAssignees.push({coreDeviceId: dvc.deviceId, channelId: channelId});
                        cache.storeLabelAssignees[labelId] = currentAssignees;
                        return false;
                    }
                });
            });
        });
    };

    var getLabelById = function (labelId)
    {
        return cache.labelIdMap[labelId];
    };

    var getLabelByName = function (labelName)
    {
        return cache.labelNameMap[labelName];
    };

    var getUserAccessibleStoreLabels = function ()
    {
        var list = [];
        Object.keys(cache.storeLabelAssignees).forEach(function (labelId)
        {
            list.push(getLabelById(labelId));
        });

        list.sort(function (l1, l2)
        {
            return l1.name > l2.name ? 1 : (l1.name < l2.name ? -1 : 0);
        });
        return list;
    };

    var getUserAccessibleAllLabels = function ()
    {
        var list = [];
        Object.keys(cache.allLabelAssignees).forEach(function (labelId)
        {
            list.push(getLabelById(labelId));
        });

        list.sort(function (l1, l2)
        {
            return l1.name > l2.name ? 1 : (l1.name < l2.name ? -1 : 0);
        });
        return list;
    };

    var getAssignedStoreLabel = function (coreDeviceId, channelId)
    {
        var cacheKey = DvcMgr.composeIdPairKey(coreDeviceId, channelId);
        return cache.deviceStoreLabelMap[cacheKey];
    };

    var getAssignedAllLabel = function (coreDeviceId, channelId)
    {
        var cacheKey = DvcMgr.composeIdPairKey(coreDeviceId, channelId);
        return cache.deviceAllLabelMap[cacheKey];
    };

    var getStoreLabelAssignees = function (labelId)
    {
        var assignees = cache.storeLabelAssignees[labelId];
        return assignees ? assignees : [];
    };

    return {
        ready: ready,
        getLabelById: getLabelById,
        getLabelByName: getLabelByName,
        getUserAccessibleAllLabels: getUserAccessibleAllLabels,
        getUserAccessibleStoreLabels: getUserAccessibleStoreLabels,
        getAssignedStoreLabel: getAssignedStoreLabel,
        getAssignedAllLabel: getAssignedAllLabel,
        getStoreLabelAssignees: getStoreLabelAssignees
    }
})();