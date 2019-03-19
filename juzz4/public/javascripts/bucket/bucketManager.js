/**
 * @author Aye Maung
 */
var bucketManager = {
    userBucketId: null,
    jqTreeId: null,
    selectedBucket: null,
    bucketIdMap: null,
    bucketNameMap: null,
    notiSetts: BktNotiSetts,

    kendoBucketTree: null,
    kendoFeatureTree: null,
    kendoTapstrip: null,
    kendoSearchBox: null,
    kendoUserGrid: null,
    kendoLogGrid: null,

    bucketTree: {
        divId: "bucketMgrTree",
        showDeletedBuckets: false,
        itemTemplate: "bucketDetailsItemTmpl",
        icons: {
            root: kupapi.CdnPath + "/common/images/company_item_root.png",
            normal: kupapi.CdnPath + "/common/images/company_item.png",
            inactive: kupapi.CdnPath + "/common/images/company_inactive.png",
            deleted: kupapi.CdnPath + "/common/images/company_deleted.png"
        }
    },
    featureTree: {
        divId: "bktFeatureTree",
        itemTemplate: "featureItemTmpl",
        rootIcon: kupapi.CdnPath + "/common/images/feature_type.png",
        childIconFolder: kupapi.CdnPath + "/common/images/features/"
    },
    current: {
        "notification-settings": {
            settings: {},
            modifiedTypes: []
        }
    }
};

bucketManager.generatePage = function (userBucketId)
{
    bucketManager.initUI();
    bucketManager.userBucketId = userBucketId;
    bucketManager._loading(true);
    bucketManager.jqTreeId = "#" + bucketManager.bucketTree.divId;
    bucketManager.getBucketsAsTree(function (bucketTree)
    {
        if (bucketTree.length == 0)
        {
            console.log("no buckets loaded");
            return;
        }

        bucketManager._initBucketTree(bucketTree);
        bucketManager._initTabStrip();
        bucketManager._initSearchBox();

        //select the first node by default
        $("#parentEditIcon").attr("style", "visibility: hidden");
        $("#deactivateBttn").attr("style", "visibility: hidden");
        $("#activateBttn").attr("style", "visibility: hidden");
        bucketManager.selectTreeNode(bucketTree[0].id);
        bucketManager._loading(false);
    });
}

bucketManager.initUI = function ()
{
    mainJS.toggleLeftBar();

    var winHeight = $(window).height() - 145;
    $(".bucket_manager .left_panel").height(winHeight);
    $(".bucket_manager .left_panel #bucketMgrTree").height(winHeight - 60);

    $(".bucket_manager .bucket_details").height(winHeight);
    $(".bucket_manager .bucket_details .tab_content").height("auto");

    //hide footer due to bug for now
    $("#footer").hide();
}

bucketManager.getBucketsAsTree = function (callback)
{
    bucketManager.bucketIdMap = {};
    bucketManager.bucketNameMap = {};

    getBucketsPlusDeleted("", bucketManager.bucketTree.showDeletedBuckets, function (responseData)
    {
        if (responseData.result != "ok" || responseData.buckets == null)
        {
            utils.throwServerError(responseData.reason);
            callback([]);
            return;
        }

        var bucketTree = bucketManager._convertToBucketTree(responseData.buckets);
        callback(bucketTree);
    }, null);
}

bucketManager.add = function ()
{
    var contentPage = "/bucket/add";
    utils.openPopup(localizeResource('add-bucket'), contentPage, null, null, true, bucketManager.onPopupClosed);
}

bucketManager.editInfo = function ()
{
    if (!bucketManager.verifySelection())
    {
        return;
    }

    if (bucketManager.selectedBucket.name == "superadmin")
    {
        utils.popupAlert(localizeResource('msg-cannot-edit-default-bucket'));
        return;
    }

    var contentPage = "/bucket/edit/" + bucketManager.selectedBucket.id;
    utils.openPopup(localizeResource('edit-bucket'), contentPage, null, null, true, bucketManager.onPopupClosed);
}

bucketManager.editBucketInfo = function ()
{
    var bucketId = this.selectedBucket.id,
        bucketOriginInfo = this.bucketIdMap[bucketId],
        setInput = (function ()
        {
            if (bucketOriginInfo.activated)
            {
                $("#deactivateBttn").attr("style", "visibility: visible");
                $(".details_tab .suspend").show();
                $(".details_tab .activate").hide();
            }
            else
            {
                $("#activateBttn").attr("style", "visibility: visible");
                $(".details_tab .suspend").hide();
                $(".details_tab .activate").show();
            }
        })();
    $(".k-widget.k-dropdown.k-header").attr("style", "display: inline-block !important");
    $("#parentEditIcon").attr("style", "visibility: visible");
    $(".details_tab .info input[name='emailverificationofusers']").attr('disabled', false);
    $('#bucketInfo').removeClass('defaultStatus').addClass('modifyStatus');
    $("#userLimit").kendoNumericTextBox({
        format: "# ",
        min: 1,
        step: 1
    }).data("kendoNumericTextBox");
}

bucketManager.updateBucketInfo = function ()
{
    //1.validate
    var bucketId = this.selectedBucket.id,
        bucketOriginInfo = this.bucketIdMap[bucketId],
        bucketUpdateInfo = {
            id: viewModel.bucketId,
            parentId: viewModel.parentId,
            name: bucketOriginInfo.name,
            path: viewModel.path,
            description: viewModel.description
        },
        bucketSettingsUpdateInfo = {
            bucketId: viewModel.bucketId,
            userLimit: viewModel.userLimit,
            emailverificationofusersenabled: viewModel.emailverificationofusersenabled,
            customLogo: viewModel.customLogo,
            binaryData: viewModel.binaryData,
            mapSource: viewModel.mapSource
        };

    bucketModel._loading(true);
    updateBucket("", bucketUpdateInfo, function (responseData)
    {
        if (responseData.result == "ok")
        {
            updateBucketSettings("", bucketSettingsUpdateInfo, function (responseData)
            {
                if (responseData.result == "ok")
                {

                    utils.slideDownInfo(bucketUpdateInfo.name + " " + localizeResource("bucket-updated"));
                    bucketManager.refreshBucketTree();
                }
                else
                {
                    utils.throwServerError(responseData);
                }
                bucketModel._loading(false);
                bucketManager.cancelBucketInfo();
            }, null);
            utils.slideDownInfo(bucketUpdateInfo.name + " " + localizeResource("bucket-updated"));
            bucketManager.refreshBucketTree();
        }
        else
        {
            utils.throwServerError(responseData);
        }
        bucketModel._loading(false);
        bucketManager.cancelBucketInfo();
    }, null);
}

bucketManager.cancelBucketInfo = function ()
{
    $('#bucketInfo').removeClass('modifyStatus').addClass('defaultStatus');
    $("#parentEditIcon").attr("style", "visibility: hidden");
    $("#deactivateBttn").attr("style", "visibility: hidden");
    $("#activateBttn").attr("style", "visibility: hidden");
    $(".k-widget.k-tooltip.k-tooltip-validation.k-invalid-msg").hide();
    $(".k-widget.k-numerictextbox").hide();
    $(".k-widget.k-dropdown.k-header").attr("style", "display: none !important");
    $(".details_tab .info input[name='emailverificationofusers']").attr('disabled', true)
}

bucketManager.editSettings = function ()
{
    if (!bucketManager.verifySelection())
    {
        return;
    }

    var contentPage = "/bucket/settings/" + bucketManager.selectedBucket.id;
    utils.openPopup(localizeResource('settings'), contentPage, null, null, true, bucketManager.onPopupClosed);
}

bucketManager.editFeatures = function ()
{
    if (!bucketManager.verifySelection())
    {
        return;
    }

    var contentPage = "/bucket/assignfeatures/" + bucketManager.selectedBucket.id;
    utils.openPopup(localizeResource('assign-features'), contentPage, null, null, true, bucketManager.onPopupClosed);
}

bucketManager.suspend = function ()
{
    if (!bucketManager.verifySelection())
    {
        return;
    }

    utils.popupConfirm(localizeResource('confirmation'), localizeResource('msg-confirm-bucket-suspend'),
        function (choice)
        {
            if (choice)
            {

                bucketManager._loading(true);
                deactivateBucket("", bucketManager.selectedBucket.id, function (responseData)
                {
                    bucketManager._loading(false);
                    if (responseData == null || responseData.result != "ok")
                    {
                        utils.throwServerError(responseData);
                        return;
                    }

                    utils.slideDownInfo(localizeResource("update-successful"));
                    bucketManager.refreshBucketTree();
                }, null);
            }
        });
}

bucketManager.unsuspend = function ()
{
    if (!bucketManager.verifySelection())
    {
        return;
    }

    utils.popupConfirm(localizeResource('confirmation'), localizeResource('confirm-activation') + "?",
        function (choice)
        {
            if (choice)
            {

                bucketManager._loading(true);
                activateBucket("", bucketManager.selectedBucket.id, function (responseData)
                {
                    bucketManager._loading(false);

                    if (responseData == null || responseData.result != "ok")
                    {
                        utils.throwServerError(responseData);
                        return;
                    }

                    utils.slideDownInfo(localizeResource("update-successful"));
                    bucketManager.refreshBucketTree();
                }, null);
            }
        });
}

bucketManager.delete = function ()
{
    if (!bucketManager.verifySelection())
    {
        return;
    }

    var bktName = bucketManager.selectedBucket.name;
    if (bktName == "superadmin" || bucketManager.selectedBucket.id == 2)
    {
        utils.popupAlert(localizeResource('msg-cannot-delete-default-bucket'));
        return;
    }

    utils.popupConfirm(localizeResource('confirmation'), localizeResource('msg-delete-bucket-confirm', bktName),
        function (choice)
        {
            if (choice)
            {
                var idToReselect = bucketManager.selectedBucket.parentId;
                bucketManager._loading(true);
                removeBucket("", bucketManager.selectedBucket.id, function (responseData)
                {
                    bucketManager._loading(false);
                    if (responseData.result != "ok")
                    {
                        utils.throwServerError(responseData);
                        return;
                    }

                    utils.slideDownInfo(localizeResource('bucket-deleted'));
                    bucketManager.selectedBucket = null;
                    bucketManager.refreshBucketTree();
                    bucketManager.loadCurrentTab();
                    bucketManager.selectTreeNode(idToReselect);
                }, null);
            }
        });
}

bucketManager.restore = function ()
{
    if (!bucketManager.verifySelection())
    {
        return;
    }

    var bktName = bucketManager.selectedBucket.name;
    utils.popupConfirm(localizeResource('confirmation'), localizeResource('confirm-restore-bucket', bktName),
        function (choice)
        {
            if (choice)
            {
                bucketManager._loading(true);
                restoreBucket("", bucketManager.selectedBucket.id, function (responseData)
                {
                    bucketManager._loading(false);
                    if (responseData.result != "ok")
                    {
                        utils.throwServerError(responseData);
                        return;
                    }

                    utils.slideDownInfo(localizeResource('bucket-restored', bktName));
                    bucketManager.refreshBucketTree();
                    bucketManager.loadCurrentTab();
                    bucketManager.selectTreeNode(bucketManager.selectedBucket.id);
                }, null);
            }
        });
}

bucketManager.onPopupClosed = function ()
{
}

bucketManager.refreshBucketTree = function (selectedBucketId)
{
    bucketManager._loading(true);
    console.log("Refreshing bucket tree.");
    bucketManager.getBucketsAsTree(function (bucketTree)
    {
        bucketManager.kendoBucketTree.dataSource.data(bucketTree);
        bucketManager.kendoSearchBox.dataSource.data(Object.keys(bucketManager.bucketNameMap));
        bucketManager._customizeTreeStyle();
        bucketManager._loading(false);

        //select a node if specified
        if (selectedBucketId)
        {
            bucketManager.selectTreeNode(selectedBucketId);
        }
        //or the previously selected
        else if (bucketManager.selectedBucket)
        {
            bucketManager.selectTreeNode(bucketManager.selectedBucket.id);
        }
        //or the first node by default
        else
        {
            bucketManager.selectTreeNode(bucketTree[0].id);
        }
    });
}

bucketManager.verifySelection = function ()
{
    if (bucketManager.selectedBucket == null)
    {
        utils.popupAlert(localizeResource("msg-select-a-bucket"));
        return false;
    }

    return true;
}

bucketManager.expandFullTree = function ()
{
    bucketManager.kendoBucketTree.expand(".k-item");
}

bucketManager.collapseFullTree = function ()
{
    bucketManager.kendoBucketTree.collapse(".k-item");
}

bucketManager.loadCurrentTab = function ()
{
    var selectedTab = bucketManager.kendoTapstrip.select();
    var index = $(selectedTab).index();

    switch (index)
    {
        case 0:
            bucketManager._loadInfoTab();
            bucketManager._bindInfoUpdate();
            break;

        case 1:
            bucketManager._loadUsersTab();
            break;

        case 2:
            bucketManager._loadAnalyticsTab();
            break;

        case 3:
            bucketManager._loadNotificationsTab();
            break;

        case 4:
            bucketManager._loadPasswordPolicyTab();
            break;

        case 5:
            bucketManager._loadLogsTab();
            break;

        default:
            break;
    }
}

bucketManager.selectTreeNode = function (bucketId)
{
    // expand all parent nodes
    $("#bkt" + bucketId).parentsUntil('.k-treeview').filter('.k-item').each(
        function (index, element)
        {
            bucketManager.kendoBucketTree.expand($(this));
        }
    );

    bucketManager.kendoBucketTree.select($("#bkt" + bucketId));
}

bucketManager.showDeletedBuckets = function (showDeleted)
{
    bucketManager.bucketTree.showDeletedBuckets = showDeleted;
    bucketManager.refreshBucketTree();
    bucketManager.expandFullTree();
};

bucketManager._initBucketTree = function (treeData)
{

    bucketManager.kendoBucketTree = $(bucketManager.jqTreeId).kendoTreeView({
        template: kendo.template($("#" + bucketManager.bucketTree.itemTemplate).html()),
        dataSource: treeData,
        loadOnDemand: false,
        change: function (e)
        {
            bucketManager._selectionChanged(e.node);
        }
    }).data("kendoTreeView");

    bucketManager._customizeTreeStyle();
    $(".bucket_manager .left_panel").show();
    bucketManager.expandFullTree();
}

bucketManager._customizeTreeStyle = function ()
{
    var childItem = $(".bucket_tree .child_item").closest(".k-top, .k-mid, .k-bot");
    childItem.addClass("lineage_line");

    var rootItem = $(".bucket_tree .root_item").closest(".k-top, .k-mid, .k-bot");
    rootItem.addClass("lineage_line");
    rootItem.addClass("separate_root");

    var rootGroup = $(".bucket_tree .root_item").closest(".k-item");
    rootGroup.css("border-left", "0px");
}

bucketManager._selectionChanged = function (newNode)
{
    if (newNode == null)
    {
        newNode = bucketManager.kendoBucketTree.select();
    }
    bucketManager.selectedBucket = bucketManager.kendoBucketTree.dataItem(newNode);
    getBucketSetting(bucketManager.selectedBucket.id,
        function (resp)
        {
            if (resp.result == "ok")
            {
                $.extend(true, bucketManager.selectedBucket, {
                    userLimit: resp.userLimit,
                    mapSource: resp.mapSource,
                    logoBlobId: resp.logoBlobId,
                    emailVerificationOfUsersEnabled: resp.emailVerificationOfUsersEnabled
                });

                // bucketManager.selectedBucket.userLimit = resp.userLimit;
                // bucketManager.selectedBucket.mapSource = resp.mapSource;
                // bucketManager.selectedBucket.logoBlobId = resp.logoBlobId;
                // bucketManager.selectedBucket.emailVerificationOfUsersEnabled = resp.emailVerificationOfUsersEnabled;
                bucketManager.loadCurrentTab();
                bucketManager.cancelBucketInfo();
            }
            else
            {
                utils.popupAlert(localizeResource("server-error"));
            }

        },
        function ()
        {
            utils.popupAlert(localizeResource("server-error"));
        }
    );
}

bucketManager._initTabStrip = function ()
{
    bucketManager.kendoTapstrip = $(".bucket_details .tab_strip").kendoTabStrip({
        animation: {
            open: {
                effects: "fadeIn"
            }
        },
        change: bucketManager.loadCurrentTab
    }).data("kendoTabStrip");

    $(".bucket_details .tab_strip").show();
}

bucketManager._initSearchBox = function ()
{

    bucketManager.kendoSearchBox = $("#bktSearchBox").kendoAutoComplete({
        dataSource: Object.keys(bucketManager.bucketNameMap),
        filter: "contains",
        placeholder: localizeResource("select-by-bucket-name"),
        change: function (e)
        {
            var bItem = bucketManager.bucketNameMap[this.value()];
            if (bItem)
            {
                bucketManager.selectTreeNode(bItem.id);
            }

            this.value("");
        }
    }).data("kendoAutoComplete");

}

bucketManager._refreshFeatureTree = function ()
{
    console.log("Refreshing feature tree");
    var jqFTree = $("#" + bucketManager.featureTree.divId);

    if (bucketManager.kendoFeatureTree == null)
    {
        bucketManager.kendoFeatureTree = jqFTree.kendoTreeView({
            template: kendo.template($("#" + bucketManager.featureTree.itemTemplate).html()),
            dataSource: [],
            select: function (e)
            {
                e.preventDefault();
            }
        }).data("kendoTreeView");
    }

    if (bucketManager.selectedBucket == null)
    {
        console.log("no bucket selected");
        bucketManager.kendoFeatureTree.dataSource.data([]);
        return;
    }

    var featureTreeData = bucketManager._convertToFeatureTree(bucketManager.selectedBucket.features);
    bucketManager.kendoFeatureTree.dataSource.data(featureTreeData);
}

bucketManager._loadInfoTab = function ()
{
    var b = bucketManager.selectedBucket;
    if (b == null)
    {
        bucketManager._resetInfoTab();
        return;
    }

    //don't allow user to edit his own bucket
    var allowEdit = !bucketManager.selectedBucket.deleted && (bucketManager.selectedBucket.id != bucketManager.userBucketId);
    if (allowEdit)
    {
        $(".bucket_details .k-button").show();
        $(".bucket_details .btn_restore").hide();
    }
    else
    {
        $(".bucket_details .k-button").hide();
    }


    var status;
    if (b.deleted)
    {
        status = localizeResource("deleted");
        $(".details_tab .status").removeClass("active");
        $(".details_tab .status").addClass("suspended");
    }
    else if (b.activated)
    {
        status = localizeResource("active");
        $(".details_tab .status").removeClass("suspended");
        $(".details_tab .status").addClass("active");

    }
    else
    {
        status = localizeResource("deactivated");
        $(".details_tab .status").removeClass("active");
        $(".details_tab .status").addClass("suspended");
    }

    var logoUrl = "/" + kupBucket + "/content/bucketlogo?id=" + b.id + "&rand=" + utils.getRandomInteger(1000, 9000);

    $(".details_tab .logo").css("background", "url('" + logoUrl + "') 0 0 no-repeat");
    $(".details_tab .info .name").html(b.name);
    $(".details_tab .info .path").html(b.path);
    $(".details_tab .info .status").html(status);
    $(".details_tab .info .description").html(b.description);
    $(".details_tab .info .parentName").html(b.parentName);
    $(".details_tab .info .userLimit").html(b.userLimit);
    $(".details_tab .info .mapSource").html(b.mapSource);
    if (b.emailVerificationOfUsersEnabled)
    {
        $(".details_tab .info input[name='emailverificationofusers']").prop('checked', true).attr('disabled', true);
    }
    else
    {
        $(".details_tab .info input[name='emailverificationofusers']").attr('disabled', true);
    }
    $(".details_tab .button_group").show();

    bucketManager._refreshFeatureTree();

    if (!allowEdit)
    {
        $(".bucket_details .k-button").hide();
        if (bucketManager.selectedBucket.deleted)
        {
            $(".bucket_details .btn_restore").show();
        }
    }


}

bucketManager._resetInfoTab = function ()
{
    $(".details_tab .logo").css("background", "transparent");
    $(".details_tab .info .name").html("");
    $(".details_tab .info .path").html("");
    $(".details_tab .info .status").html("");
    $(".details_tab .info .description").html("");

    $(".details_tab .suspend").hide();
    $(".details_tab .activate").hide();
    $(".details_tab .button_group").hide();

    bucketManager._refreshFeatureTree();
}

bucketManager._loadUsersTab = function ()
{
    console.log("Refreshing user list");

    if (bucketManager.kendoUserGrid)
    {
    	bucketManager.kendoUserGrid.dataSource.page(0);
    	bucketManager.kendoUserGrid.dataSource.read();
        return;
    }

    bucketManager.kendoUserGrid = $(".bucket_manager #userList").kendoGrid({
        dataSource: {
            transport: {
                read: function (options)
                {
                    if (bucketManager.selectedBucket == null)
                    {
                        console.log("bucket not selected");
                        options.success([]);
                        return;
                    }

                    getBucketUsersByBucketId(bucketManager.selectedBucket.id, onSuccess, null);

                    function onSuccess(responseData)
                    {
                        if (responseData.result != "ok" || responseData.bucketUsers == null)
                        {
                            utils.throwServerError(responseData);
                            options.success([]);
                            return;
                        }

                        options.success(responseData.bucketUsers);
                    }
                }
            },
            pageSize: 15
        },
        pageable: {
            input: true,
            numeric: false,
            pageSizes: false,
            refresh: true
        },
        sortable: true,
        filterable: false,
        selectable: true,
        resizable: false,
        columns: [
            {
                field: "userName",
                title: localizeResource("username"),
                width: "200px"
            },
            {
                field: "name",
                title: localizeResource("full-name"),
                width: "200px"
            },
            {
                field: "email",
                title: localizeResource("email"),
                width: "300px"
            },
            {
                field: "phone",
                title: localizeResource("phone"),
                width: "200px"
            },
            {
                command: [
                    {
                        text: localizeResource("edit"),
                        click: function (e)
                        {
                            e.preventDefault();

                            if (bucketManager.selectedBucket.deleted)
                            {
                                return;
                            }

                            var dataItem = this.dataItem($(e.currentTarget).closest("tr"));
                            var contentPage = "/user/edit/" + dataItem.userId;
                            utils.openPopup(localizeResource("edit-details"), contentPage, null, null, true, function ()
                            {
                                bucketManager.kendoUserGrid.dataSource.read();
                            });
                        }
                    }
                ],
                title: localizeResource("actions"),
                width: "100px"
            }
        ]
    }).data("kendoGrid");

}

bucketManager._loadAnalyticsTab = function ()
{
    if (bucketManager.selectedBucket == null)
    {
        return;
    }

    console.log("Refreshing analytics tree");
    var bucketId = bucketManager.selectedBucket.id;
    var bucketName = bucketManager.selectedBucket.name;

    //get devices
    bucketManager._loading(true);
    getBucketDevicesByBucketId(bucketId, function (responseData)
    {
        bucketManager._loading(false);
        if (responseData.result != "ok" || responseData.devices == null)
        {
            utils.throwServerError(responseData);
            return;
        }

        var deviceList = responseData.devices;

        //get analytics
        bucketManager._loading(true);
        listAnalyticsByBucketId("", bucketId, analyticsType.ALL, function (responseData)
        {
            bucketManager._loading(false);
            if (responseData.result != "ok" || responseData.instances == null)
            {
                utils.throwServerError(responseData);
                return;
            }

            var analyticsList = responseData.instances;

            var options = {
                bucketId: bucketId,
                bucketName: bucketName,
                cssSelector: ".dvc_vca_tree"
            };

            //generate tree
            dvcVcaTree.generate(options, deviceList, analyticsList);

            //re-adjust height for bucket manager
            var tabContentHeight = $(".tab_content").eq(2).height();
            $(options.cssSelector + " svg").height(tabContentHeight - 50);

        }, null);
    }, null);
}

bucketManager._loadNotificationsTab = function ()
{
    if (bucketManager.selectedBucket == null)
    {
        return;
    }

    console.log("loading notifications");
    bucketManager.notiSetts.init(bucketManager.selectedBucket, bucketManager._loading);
}

bucketManager._loadLogsTab = function ()
{
    console.log("Refreshing change logs");

    if (bucketManager.kendoLogGrid)
    {
    	bucketManager.kendoLogGrid.dataSource.page(0);
    	bucketManager.kendoLogGrid.dataSource.read();
        return;
    }

    bucketManager.kendoLogGrid = $(".bucket_manager #changeList").kendoGrid({
        dataSource: {
            transport: {
                read: function (options)
                {
                    if (bucketManager.selectedBucket == null)
                    {
                        console.log("bucket not selected");
                        options.success([]);
                        return;
                    }

                    getBucketLogs("", bucketManager.selectedBucket.id, function (responseData)
                    {
                        if (responseData.result != "ok" || responseData.logs == null)
                        {
                            utils.throwServerError(responseData);
                            options.success([]);
                            return;
                        }

                        options.success(responseData.logs);
                    }, null);
                }
            },
            pageSize: 20
        },
        pageable: {
            input: true,
            numeric: false,
            pageSizes: false,
            refresh: true
        },
        sortable: true,
        filterable: false,
        selectable: true,
        resizable: false,
        detailTemplate: "<div class='log_changes'></div>",
        detailInit: function (e)
        {
            var detailRow = e.detailRow;
            var changeList = "";
            e.data.changes.forEach(function (change)
            {
                changeList += "<div class='entry'>" + change + "</div>";
            });
            detailRow.find(".log_changes").html(changeList);
        },
        columns: [
            { field: "time", title: localizeResource("time"), width: "200px",
                template: "#= kendo.toString(new Date(time), kupapi.TIME_FORMAT)#"},
            { field: "username", title: localizeResource("username"), width: "150px" },
            { field: "remoteIp", title: localizeResource("ip"), width: "150px"},
            { field: "changes", title: localizeResource("actions"),
                template: "#= (changes.length > 1)? localizeResource('multiple-actions') : changes[0] #"}
        ]
    }).data("kendoGrid");

}

bucketManager._convertToBucketTree = function (bucketList)
{
    bucketManager.bucketIdMap = {};

    // create bucket map for lookup
    bucketList.forEach(function (bucket)
    {

        //actual bucket has too many data inside. this will make it lighter
        var bucketListTmp = bucketList;
        var cleanBkt = {
            id: bucket.id,
            parentId: (bucket.parentId == null) ? 0 : bucket.parentId,
            name: bucket.name,
            path: bucket.path,
            activated: bucket.activated,
            deleted: bucket.deleted,
            features: bucket.features,
            description: bucket.description,
            parentName: (function (bucketList, currentId)
            {
                var parentName = '';
                $.each(bucketList, function (i, bucket)
                {
                    if (bucket.id === currentId)
                    {
                        parentName = bucket.name;
                        return false;
                    }
                });
                return parentName;
            })(bucketListTmp, bucket.parentId || bucket.id),
            userLimit: 20
        };

        bucketManager.bucketIdMap[bucket.id] = cleanBkt;
        bucketManager.bucketNameMap[bucket.name] = cleanBkt;
    });

    // create the tree array
    var bucketTree = [];
    bucketList.forEach(function (bucket)
    {
        if (!bucketManager.bucketTree.showDeletedBuckets)
        {
            if (bucket.deleted)
            {
                return;
            }
        }

        var parent = bucketManager.bucketIdMap[bucket.parentId];
        var cleanBkt = bucketManager.bucketIdMap[bucket.id];

        if (parent)
        {
            cleanBkt.isRoot = false;

            //initialize child list
            if (parent.items == null)
            {
                parent.items = [];
            }

            parent.items.push(cleanBkt);

        }
        else
        {
            cleanBkt.isRoot = true;
            bucketTree.push(cleanBkt);
        }
    });

    //Set cascaded states and Icons
    bucketTree = bucketManager._setCascadedStatesAndIcons(bucketTree, true);  //roots are active
    return bucketTree;
}

bucketManager._setCascadedStatesAndIcons = function (bucketTreeList, activated)
{
    if (bucketTreeList == null)
    {
        return [];
    }

    bucketTreeList.forEach(function (bkt)
    {
        //set child to deactivated if the parent is not active
        if (!activated)
        {
            bkt.activated = false;
        }
        bkt.items = bucketManager._setCascadedStatesAndIcons(bkt.items, bkt.activated);
        bkt.imageUrl = bucketManager._getIcon(bkt);
    });

    return bucketTreeList;
};

bucketManager._getIcon = function (bucket)
{
    if (bucket.isRoot)
    {
        return bucketManager.bucketTree.icons.root;
    }

    if (bucket.deleted)
    {
        return bucketManager.bucketTree.icons.deleted;
    }

    if (!bucket.activated)
    {
        return bucketManager.bucketTree.icons.inactive;
    }

    return bucketManager.bucketTree.icons.normal;
}

bucketManager._convertToFeatureTree = function (featureList)
{

    var featureGroups = {};
    $.each(featureList, function (idx, fItem)
    {
        if (featureGroups[fItem.type] == null)
        {
            featureGroups[fItem.type] = [];
        }

        featureGroups[fItem.type].push({
            id: fItem.name,
            levelOnePosition: fItem.levelOnePosition,
            levelTwoPosition: fItem.levelTwoPosition,
            name: localizeResource(fItem.name),
            imageUrl: bucketManager.featureTree.childIconFolder + fItem.name + ".png"
        });
    });

    var treeItems = [];
    $.each(featureGroups, function (fType, fgroup)
    {

        // sort Level2
        fgroup.sort(function (f1, f2)
        {
            if (f1.levelOnePosition == f2.levelOnePosition)
            {
                return f1.levelTwoPosition - f2.levelTwoPosition;
            }
            else
            {
                return f1.levelOnePosition - f2.levelOnePosition;
            }
        });

        treeItems.push({
            id: "type" + fgroup[0].levelOnePosition,
            levelOnePosition: fgroup[0].levelOnePosition,
            name: localizeResource(fType) + " (" + fgroup.length + ")",
            imageUrl: bucketManager.featureTree.rootIcon,
            items: fgroup
        });
    });

    //sort Level1
    treeItems.sort(function (g1, g2)
    {
        return g1.levelOnePosition - g2.levelOnePosition;
    });

    return treeItems;
}

bucketManager._loading = function (loading)
{
    kendo.ui.progress($(".bucket_manager .title_bar"), loading);
}

bucketManager._bindInfoUpdate = function ()
{
    var selectedBucket = this.selectedBucket;
    viewModel = kendo.observable({
        "bucketId": selectedBucket.id.toString(),
        "userLimit": (selectedBucket.userLimit == 0) ? 10 + '' : selectedBucket.userLimit.toString(),
        "emailverificationofusersenabled": (selectedBucket.emailVerificationOfUsersEnabled) ? true : false,
        "customLogo": "${customLogo}" == "true" ? true : false,
        "binaryData": "",
        "description": selectedBucket.description,
        "path": selectedBucket.path,
        "parentId": selectedBucket.parentId,
        "parentName": selectedBucket.parentName,
        "mapSource": selectedBucket.mapSource
    });
    kendo.bind($("#bucketInfo"), viewModel);
    var validator = $("#bucketInfo").kendoValidator().data("kendoValidator");

    $("#mapsourceList").kendoDropDownList({
        dataTextField: "mapName",
        dataValueField: "mapId",
        dataSource: [
            {
                mapName: "Google",
                mapId: "google"
            },
            {
                mapName: "Baidu",
                mapId: "baidu"
            }
        ],
        change: function (e)
        {
            var selectedMap = $("#mapsourceList").data("kendoDropDownList").dataItem(e.sender.selectedIndex);
            viewModel.set("mapSource", selectedMap.mapId);
        },
        value: viewModel.mapSource
    });
}

bucketManager._loadPasswordPolicyTab = function () {
    var b = bucketManager.selectedBucket;
    if (b == null) {
        bucketManager._resetInfoTab();
        return;
    }
    $(".bucket_details .password_details").hide();
    bucketManager._loading(true);
    getBucketPasswordPolicy(bucketManager.selectedBucket.id,
        function (resp) {
    		bucketManager._loading(false);
            if (resp.result == "ok") {
                var passwordPolicy = resp.passwordPolicy;
                $("#minPassLength").kendoNumericTextBox({
                    format: "# ",
                    min: 8,
                    step: 1,
                    value: passwordPolicy.minimumPasswordLength
                }).data("kendoNumericTextBox");

                $("#passExpirationDays").kendoNumericTextBox({
                    format: "# ",
                    min: 15,
                    step: 1,
                    value: passwordPolicy.passwordExpirationDays
                }).data("kendoNumericTextBox");
                
                $("#passReuseCheckTimes").kendoNumericTextBox({
                    format: "# ",
                    min: 1,
                    step: 1,
                    value: passwordPolicy.numberOfReusePasswordPrevention
                }).data("kendoNumericTextBox");
                
                $("input[name='uppercaseRequired']").prop('checked', passwordPolicy.requiredUppercase);
                $("input[name='lowercaseRequired']").prop('checked', passwordPolicy.requiredLowercase);
                $("input[name='numbericRequired']").prop('checked', passwordPolicy.requiredNumeric);
                $("input[name='specialCharRequired']").prop('checked', passwordPolicy.requiredSpecialChar);
                $("input[name='enablePassExpiration']").prop('checked', passwordPolicy.enabledPasswordExpiration);
                $("input[name='enablePassReuseCheck']").prop('checked', passwordPolicy.preventedPasswordReuse);
                $("input[name='firstLoginChangeRequired']").prop('checked', passwordPolicy.requiredFirstLoginPasswordCheck);
                if (passwordPolicy.enabledPasswordExpiration) {
                	$("#passExpirationNumberBox").show();
                }else {
                	$("#passExpirationNumberBox").hide();
                }
                if (passwordPolicy.preventedPasswordReuse) {
                	$("#passReuseNumberBox").show();
                }else {
                	$("#passReuseNumberBox").hide();
                }
                
                $('#enablePassExpiration').change(function() {
                    $('#enablePassExpiration').val($(this).is(':checked'));
                    if($(this).is(':checked')){
                    	$("#passExpirationNumberBox").show();
                    }else{
                    	$("#passExpirationNumberBox").hide();
                    }
                });
                
                $('#enablePassReuseCheck').change(function() {
                    $('#enablePassReuseCheck').val($(this).is(':checked'));
                    if($(this).is(':checked')){
                    	$("#passReuseNumberBox").show();
                    }else{
                    	$("#passReuseNumberBox").hide();
                    }
                });
                
                utils.createTooltip("specialCharacterInfo", "bottom", localizeResource('password-non-alphanumeric-info')+" !\"#$%&'()*+,-./:;<=>?@[\]^_`{|}~\\");
                utils.createTooltip("passExpirationInfo", "bottom", localizeResource('password-expiration-info'));
                utils.createTooltip("preventPassReuseInfo", "bottom", localizeResource('prevent-password-reuse-info'));
                
                $(".bucket_details .password_details .k-button").show();
                $(".bucket_details .password_details").show();
            } else {
                utils.popupAlert(localizeResource("server-error"));
            }
        }
    , null);
}

bucketManager.cancelBucketPasswordPolicyInfo = function () {
	bucketManager._loadPasswordPolicyTab();
}

bucketManager.updateBucketPasswordPolicyInfo = function () {
	var newPasswordPolicy = {};
	newPasswordPolicy.minPassLength = $("#minPassLength").data("kendoNumericTextBox").value();
	newPasswordPolicy.passExpirationDays = $("#passExpirationDays").data("kendoNumericTextBox").value();
	newPasswordPolicy.passReuseCheckTimes = $("#passReuseCheckTimes").data("kendoNumericTextBox").value();
	
	newPasswordPolicy.uppercaseRequired = $("#uppercaseRequired").is(':checked');
	newPasswordPolicy.lowercaseRequired = $("#lowercaseRequired").is(':checked');
	newPasswordPolicy.numbericRequired = $("#numbericRequired").is(':checked');
	newPasswordPolicy.specialCharRequired = $("#specialCharRequired").is(':checked');
	newPasswordPolicy.enablePassExpiration = $("#enablePassExpiration").is(':checked');
	newPasswordPolicy.enablePassReuseCheck = $("#enablePassReuseCheck").is(':checked');
	newPasswordPolicy.requiredfirstLoginPasswordChange = $("#firstLoginChangeRequired").is(':checked');
	newPasswordPolicy.emailWhenPasswordExpired = false;

	bucketModel._loading(true);
	updateBucketPasswordPolicy(bucketManager.selectedBucket.id, newPasswordPolicy, function (responseData) {
        if (responseData.result == "ok") {
            utils.slideDownInfo(bucketManager.selectedBucket.name + " " + localizeResource("bucket-updated"));
            bucketManager.refreshBucketTree();
        } else {
            utils.throwServerError(responseData);
        }
        bucketModel._loading(false);
	}, null);
}