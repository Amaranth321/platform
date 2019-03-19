var accountstatement = {};

accountstatement.currentBucketId = "";
accountstatement.tabName = "users";  //e.g., users, nodes, licenses
accountstatement.toolBarTemplate = kendo.template($("#accountStatementsToolbarTmpl").html());
accountstatement.bucketDivId = [ { name: "userBucketList" }, { name: "nodeBucketList" } ];


accountstatement.initializeUsersGrid = function(columnList) {

	var ds = new kendo.data.DataSource({
		transport: {
			read: function(options){
				if(accountstatement.currentBucketId == ""){
					options.success([]);
					return;
				}
				getBucketUsersByBucketId(accountstatement.currentBucketId, function (successResult){
					if(successResult.result == "ok" && successResult.bucketUsers != null){
						options.success(successResult.bucketUsers);
					}else{
						options.success([]);
						utils.throwServerError(successResult.reason);
					}
				}, null);
			}
		},
		pageSize: 15
	});

	var usersGrid = $("#usersGrid").kendoGrid({
		dataSource: ds,
		pageable: {
            input: true,
            numeric: false,
            pageSizes: false,
            refresh: true
        },
        sortable: true,
        selectable: true,
        resizable: true,
		toolbar: accountstatement.toolBarTemplate(accountstatement.bucketDivId[0]),
		columns: columnList
	}).data("kendoGrid");

	accountstatement.populateBucketElement(accountstatement.bucketDivId[0].name);
	$(".event_filter_box").show("fast");	//after populate bucket item, show tool bar

	accountstatement.refreshUserGrid = function() {
		usersGrid.dataSource.read();
	}
}

accountstatement.initializeNodesGrid = function(columnList) {

	var ds = new kendo.data.DataSource({
		transport: {
			read: function(options){
				if(accountstatement.currentBucketId == ""){
					options.success([]);
					return;
				}
				getBucketDevicesByBucketId(accountstatement.currentBucketId, function (successResult){
					if(successResult.result == "ok" && successResult.devices != null){
						var devices = [];

						$.each(successResult.devices, function(index, element){
							if(element.model.capabilities.indexOf("node") >= 0){
								var deviceItem = {};
								deviceItem.name = element.name;
								deviceItem.deviceKey = element.deviceKey;
								deviceItem.version = element.node.version;
								deviceItem.label = element.label.toString();
								devices.push(deviceItem);
							}
						});
						options.success(devices);
					}else{
						options.success([]);
						utils.popupAlert(localizeResource(successResult.reason));
						utils.throwServerError(successResult.reason);
					}
				}, null);
			}
		},
		pageSize: 15
	});

	var nodeGrid = $("#nodesGrid").kendoGrid({
		dataSource: ds,
		pageable: {
            input: true,
            numeric: false,
            pageSizes: false,
            refresh: true
        },
        sortable: true,
        selectable: true,
        resizable: true,
        toolbar: accountstatement.toolBarTemplate(accountstatement.bucketDivId[1]),
		columns: columnList
	}).data("kendoGrid");

	accountstatement.populateBucketElement(accountstatement.bucketDivId[1].name);
	$(".event_filter_box").show("fast");	//after populate bucket item, show tool bar

	accountstatement.refreshNodeGrid = function() {
		nodeGrid.dataSource.read();
	}
}

accountstatement.initializeLicenseGrid = function() {
    //Hide 'more' column in Licenses tab
    $("#licenseGrid").data("kendoGrid").hideColumn(8);
    $("#btnAddLicense").hide();
}

accountstatement.populateBucketElement = function(divId) {
	$("#" + divId).kendoDropDownList({
		dataTextField: "name",
		dataValueField: "bucketId",
		change: onChange,
        suggest: true,
		dataSource: {
			transport: {
                read: function (options){
                    getBuckets(null, function (successResult){
                        if(successResult.result == "ok"){
                            options.success(successResult.buckets);
                            var selectedBucket = $("#" + divId).data("kendoDropDownList").dataItem();
                            accountstatement.currentBucketId = selectedBucket.id;
                            if(divId.indexOf("user") != -1) {
                            	accountstatement.refreshUserGrid();
                            }else if(divId.indexOf("node") != -1) {
                            	accountstatement.refreshNodeGrid();
                            }
                        }else{
                            options.success([])
                        }
                    }, null)
                }
            }
		}
	});
	function onChange(e){
		accountstatement.applyChangedValue();
	}
}

accountstatement.applyChangedValue = function() {
	var selectedBucket = null;	//get current selected bucket
	if (accountstatement.tabName == "users") {
		selectedBucket = $("#" + accountstatement.bucketDivId[0].name).data("kendoDropDownList").dataItem();
		if(selectedBucket == null){
			utils.popupAlert(localizeResource('msg-select-a-bucket'));
	        return;
	    }
		accountstatement.currentBucketId = selectedBucket.id;
		accountstatement.refreshUserGrid();
	}else if (accountstatement.tabName == "nodes") {
		selectedBucket = $("#" + accountstatement.bucketDivId[1].name).data("kendoDropDownList").dataItem();
		if(selectedBucket == null){
			utils.popupAlert(localizeResource('msg-select-a-bucket'));
	        return;
	    }
		accountstatement.currentBucketId = selectedBucket.id;
		accountstatement.refreshNodeGrid();
	}else if (accountstatement.tabName == "licenses") {

	}
}

accountstatement.clearFilter = function() {
	if (accountstatement.tabName == "users") {
		$("#" + accountstatement.bucketDivId[0].name).data("kendoDropDownList").select(0);
		$("#usersGrid").data('kendoGrid').dataSource.data([]);
	}else if (accountstatement.tabName == "nodes") {
		$("#" + accountstatement.bucketDivId[1].name).data("kendoDropDownList").select(0);
		$("#nodesGrid").data('kendoGrid').dataSource.data([]);
	}else if (accountstatement.tabName == "licenses") {

	}
	accountstatement.applyChangedValue();
}

accountstatement.exportPdf = function() {
	var selectedBucket = null;	//get current selected bucket
	if(accountstatement.tabName == "users") {
		selectedBucket = $("#" + accountstatement.bucketDivId[0].name).data("kendoDropDownList").dataItem();
		if(selectedBucket == null){
			utils.popupAlert(localizeResource('msg-select-a-bucket'));
	        return;
	    }
		accountstatement.currentBucketId = selectedBucket.id;
        exportUsersFileByBucketId(accountstatement.currentBucketId, "pdf");

	}else if(accountstatement.tabName == "nodes") {
		selectedBucket = $("#" + accountstatement.bucketDivId[1].name).data("kendoDropDownList").dataItem();
		if(selectedBucket == null){
			utils.popupAlert(localizeResource('msg-select-a-bucket'));
	        return;
	    }
		accountstatement.currentBucketId = selectedBucket.id;
		exportNodesFileByBucketId(accountstatement.currentBucketId, "pdf");
	}else if(accountstatement.tabName == "licenses") {

	}
}

accountstatement.exportXls = function() {
	var selectedBucket = null;	//get current selected bucket
	if(accountstatement.tabName == "users") {
		selectedBucket = $("#" + accountstatement.bucketDivId[0].name).data("kendoDropDownList").dataItem();
		if(selectedBucket == null){
			utils.popupAlert(localizeResource('msg-select-a-bucket'));
	        return;
	    }
		accountstatement.currentBucketId = selectedBucket.id;
		exportUsersFileByBucketId(accountstatement.currentBucketId, "xls");
	}else if(accountstatement.tabName == "nodes") {
		selectedBucket = $("#" + accountstatement.bucketDivId[1].name).data("kendoDropDownList").dataItem();
		if(selectedBucket == null){
			utils.popupAlert(localizeResource('msg-select-a-bucket'));
	        return;
	    }
		accountstatement.currentBucketId = selectedBucket.id;
		exportNodesFileByBucketId(accountstatement.currentBucketId, "xls");
	}else if(accountstatement.tabName == "licenses") {

	}
}