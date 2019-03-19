var bucketModel = {};

bucketModel = kendo.observable({
    "id": null,
    "parentId": null,
    "name": null,
    "path": null,
    "description": null,
    "currentBucket": null,
    "originalParent": null,
    "currentParent": null,

    init: function (currentBucketId) {
        this.reset();
        kendo.bind($(".bucket_model_form"), this);

        if (currentBucketId) {
            bucketModel._loading(true);
            getBuckets("", function (responseData) {
                if (responseData.result != "ok" || responseData.buckets == null) {
                    utils.throwServerError(responseData.reason);
                    bucketModel._loading(false);
                    return;
                }

                var bucketMap = {};
                $.each(responseData.buckets, function (i, bkt) {
                    bucketMap[bkt.id] = bkt;
                });

                var currentBucket = bucketMap[currentBucketId];
                bucketModel.setCurrentBucket(currentBucket);

                var currentParent = bucketMap[currentBucket.parentId];
                bucketModel.setParentBucket(currentParent);
                bucketModel.originalParent = currentParent;

                bucketModel._loading(false);
            }, null);
        }
    },

    reset: function () {
        this.set("id", "");
        this.set("parentId", 0);
        this.set("name", "");
        this.set("path", "kaisquare");
        this.set("description", "");
        this.set("currentBucket", null);
        this.set("originalParent", null);
        this.set("currentParent", null);
    },

    setCurrentBucket: function (currentBucket) {
        bucketModel.set("id", currentBucket.id);
        bucketModel.set("parentId", currentBucket.parentId);
        bucketModel.set("name", currentBucket.name);
        this.set("path", currentBucket.path);
        this.set("description", currentBucket.description);
        this.set("currentBucket", currentBucket);
    },

    setParentBucket: function (parentBucket) {
        if (parentBucket == null) {
            $(".bucket_model_form input[name=parent]").val(localizeResource("no-parent-bucket"));
            bucketModel.set("parentId", 0);
            bucketModel.set("currentParent", null);
        } else {
            $(".bucket_model_form input[name=parent]").val(parentBucket.name);
            bucketModel.set("parentId", parentBucket.id);
            bucketModel.set("currentParent", parentBucket);
            viewModel.set("parentId", parentBucket.id);
            viewModel.set("parentName", parentBucket.name);
        }
    },

    chooseParent: function (isCreation) {
        if (isCreation) {
            bucketModel.currentBucket = {
                id: -1,
                parentId: -1
            }
        }else{
            bucketModel.currentBucket = {
                id: bucketManager.selectedBucket.id,
                parentId: bucketManager.selectedBucket.parentId
            }            
        }

        bucketSelector.open(localizeResource('select-parent-bucket'),
            bucketModel.currentBucket.parentId,
            bucketModel.currentBucket.id,
            function (selectedBucket) {
                bucketModel.setParentBucket(selectedBucket);
            },
            function () {
            }
        );
    },

    validate: function () {
        this.sanitizeInputs();

        var validator = $(".bucket_model_form").kendoValidator().data("kendoValidator");
        if (!validator.validate()) {
            return false;
        }

        if (!this._isValidBucketName(this.get("name"))) {
            var nameInput = "' " + this.get("name") + " '";
            utils.popupAlert(localizeResource("msg-contain-nonalphanumeric-space") + "<br><br>" + nameInput);
            return false;
        }

        if (!utils.isValidDir(this.get("path"))) {
            var pathInput = "' " + this.get("path") + " '";
            utils.popupAlert(localizeResource("invalid-path") + "<br><br>" + pathInput);
            return false;
        }

        return true;
    },

    sanitizeInputs: function () {
        this.set("name", this.get("name").trim());
        this.set("path", this.get("path").trim());
        this.set("description", utils.removeLineBreaks(this.get("description")));
    },

    addNew: function () {
        if (!this.validate()) {
            return;
        }

        bucketModel._loading(true);
        addBucket("", this, function (responseData) {
            if (responseData.result == "ok") {
                utils.slideDownInfo(bucketModel.get("name") + " " + localizeResource("bucket-created"));
                bucketModel.close();
                bucketModel.refreshParent(responseData["bucket-id"]);
            }
            else {
                utils.throwServerError(responseData);
            }

            bucketModel._loading(false);
        }, null);
    },

    update: function () {
        if (!this.validate()) {
            return;
        }

        if (this.originalParent.id != this.currentParent.id) {
            utils.popupConfirm(localizeResource("confirmation"),
                localizeResource("msg-bucket-parent-change-warning"),
                function (choice) {
                    if (choice) {
                        bucketModel._sendUpdateApi();
                    }
                });
        }
        else {
            bucketModel._sendUpdateApi();
            return
        }
    },

    _sendUpdateApi: function () {
        bucketModel._loading(true);
        updateBucket("", this, function (responseData) {
            if (responseData.result == "ok") {
                utils.slideDownInfo(bucketModel.get("name") + " " + localizeResource("bucket-updated"));
                bucketModel.close();
                bucketModel.refreshParent();
            }
            else {
                utils.throwServerError(responseData);
            }

            bucketModel._loading(false);
        }, null);
    },

    _isValidBucketName: function (nameString) {
        //alphanumeric, no spaces, allow dash and underscore
        var pattern = new RegExp(/^[a-zA-Z0-9-_]*$/i);
        return pattern.test(nameString);
    },

    _loading: function (loading) {
        kendo.ui.progress($(".bucket_model_container"), loading);
    },

    refreshParent: function (bucketId) {
        if (bucketManager.kendoBucketTree) {
            bucketManager.refreshBucketTree(bucketId);
        }
    },

    close: function () {
        $(".bucket_model_form").closest(".k-window-content").data("kendoWindow").close();
    }
});