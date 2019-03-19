/**
 * @author Aye Maung
 */
var bucketSelector = {};

bucketSelector.idPrefix = "bktSelector";
bucketSelector.treeItemTemplate = "bucketSelectorItemTmpl";

bucketSelector.jqTreeId = null;
bucketSelector.kendoTree = null;
bucketSelector.cancelled = false;
bucketSelector.preSelectedId = null;
bucketSelector.subTreeLimiterId = null;
bucketSelector.bucketNameMap = null;
bucketSelector.selectedSorceNode = ''; //get kendo treeview's selected data

/**
 * This will open up the tree of buckets that are visible to the caller
 * User floorLimiterBucketId to cut off a sub-tree
 * This is useful when the selection is not applicable for that sub-tree
 * e.g. child bucket should not be selectable as a parent
 *
 * @param title                 title
 * @param preSelectedId         current selection (optional)
 * @param subTreeLimiterId      bucket to filter out. Inclusive. (optional)
 * @param onSelected            when the user clicks Select. Selected Bucket will be passed as a parameter
 * @param onCancelled           when the user clicks Cancel
 */
bucketSelector.open = function(title, preSelectedId, subTreeLimiterId, onSelected, onCancelled) {

    bucketSelector.preSelectedId = preSelectedId ? preSelectedId : -1;
    bucketSelector.subTreeLimiterId = subTreeLimiterId ? subTreeLimiterId : -1;

    var contentPage = "/bucket/selector";
    utils.openPopup(title, contentPage, null, null, true, function() {
        if (bucketSelector.cancelled) {
            onCancelled();
        } else {
            var node = bucketSelector.selectedSorceNode;
            var dataItem = bucketSelector.kendoTree.dataItem(node);
            onSelected(dataItem);
        }
    });
}

bucketSelector.close = function(cancelled) {
    bucketSelector.cancelled = cancelled;
    $(bucketSelector.jqTreeId).closest(".k-window-content").data("kendoWindow").close();
}

bucketSelector.generateTree = function(divId) {
    bucketSelector.jqTreeId = "#" + divId;
    bucketSelector._loading(true);

    bucketManager.getBucketsAsTree(function(bucketTree) {
        var cleanedTree = bucketSelector._removeSubTree(bucketSelector.subTreeLimiterId, bucketTree),
            onSelect = function(e) {
                bucketSelector.selectedSorceNode = e.node;
            };
        if (!bucketSelector._verifyData(cleanedTree)) {
            return;
        }
        bucketSelector.kendoTree = $(bucketSelector.jqTreeId).kendoTreeView({
            template: kendo.template($("#" + bucketSelector.treeItemTemplate).html()),
            dataSource: bucketTree,
            select: onSelect
        }).data("kendoTreeView");

        bucketSelector._initSearchBox(bucketTree);
        bucketManager._customizeTreeStyle();
        bucketSelector.kendoTree.expand(".k-item");
        bucketSelector.kendoTree.select($("#" + bucketSelector.idPrefix + bucketSelector.preSelectedId));
        bucketSelector.kendoTree.enable(".k-item .suspended", false);
        bucketSelector.kendoTree.enable(".k-item .deleted", false);
        bucketSelector._loading(false);
    });

}

bucketSelector.collapseFullTree = function() {
    bucketSelector.kendoTree.collapse(".k-item");
}

bucketSelector._initSearchBox = function() {

    $(".bucket_tree .search_bar .input").kendoAutoComplete({
        dataSource: Object.keys(bucketManager.bucketNameMap),
        filter: "contains",
        placeholder: localizeResource("select-by-bucket-name"),
        change: function(e) {
            var bItem = bucketManager.bucketNameMap[this.value()];
            if (bItem && bItem.activated) {
                bucketSelector.kendoTree.select($("#" + bucketSelector.idPrefix + bItem.id));
                bucketSelector.selectedSorceNode = $("#" + bucketSelector.idPrefix + bItem.id);
            }

            this.value("");
        }
    }).data("kendoAutoComplete");

    $(".bucket_tree .search_bar .input").show();
}

bucketSelector._removeSubTree = function(targetBucketId, bucketTree) {
    $.each(bucketTree, function(i, bkt) {
        if (targetBucketId == bkt.id) {
            bucketTree.splice(i, 1);
            return false;
        } else if (bkt.items && bkt.items.length > 0) {
            bucketSelector._removeSubTree(targetBucketId, bkt.items);
        }
    });

    return bucketTree;
}

bucketSelector._verifyData = function(treeReturned) {
    if (treeReturned && treeReturned.length > 0) {
        $(".bucket_selector .no_bucket_msg").hide();
        $(".bucket_selector .button_group .select").show();
        return true;
    } else {
        $(".bucket_selector .no_bucket_msg").show();
        $(".bucket_selector .button_group .select").hide();
        return false;
    }
}

bucketSelector._loading = function(loading) {
    kendo.ui.progress($(".bucket_selector"), loading);
}
