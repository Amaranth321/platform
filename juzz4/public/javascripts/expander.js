var kExpander = kendo.ui.Widget.extend({
    init: function (element, options) {
        kendo.ui.Widget.fn.init.call(this, element, options);
        this._Render();
    },

    options: {
        name: "Expander",
        title: "",
        isExpanded: true,
        defaultDuration: 200
    },

    IsExpanded: false,

    Expand: function (duration) {
        var $this = this;
        if ($this.contents) {
            if (duration == null) {
                duration = $this.options.defaultDuration;
            }

            $this.contents.show(duration, function () {
                $this.IsExpanded = true;
            });
        }

        if ($this.button) {
            $this.button.find("span")
                .removeClass("k-i-arrowhead-s")
                .addClass("k-i-arrowhead-n");
        }
    },

    Collapse: function (duration) {
        var $this = this;
        if ($this.contents) {
            if (duration == null) {
                duration = $this.options.defaultDuration;
            }

            $this.contents.hide(duration, function () {
                $this.IsExpanded = false;
            });
        }

        if ($this.button) {
            $this.button.find("span")
                .removeClass("k-i-arrowhead-n")
                .addClass("k-i-arrowhead-s");
        }
    },

    Toggle: function () {
        if (this.IsExpanded) {
            this.Collapse();
        }
        else {
            this.Expand();
        }
    },

    _Render: function () {
        this._MutateDom();
        this._SetInitialExpandState();
    },

    _MutateDom: function () {
        if (($(this.element).prop("tagName") || "").toString().toUpperCase() == "FIELDSET") {
            this._MutateFromFieldSet();
        }
        else {
            this.contents = $(this.element).attr("data-role", "expanderContents");
            this.fieldSet = $("<fieldset>").attr("style", this.contents.attr("style"));
            this.contents.removeAttr("style");
            this.contents.wrap(this.fieldSet);
            this.fieldSet = this.contents.parent();
            this.legend = $("<legend>").text(this.options.title);
            this.fieldSet.prepend(this.legend);
        }

        this._AddExpanderButton();
    },

    _MutateFromFieldSet: function () {
        this.fieldSet = $(this.element);
        var children = this.fieldSet.find(">:not(legend)");
        this.contents = $("<div>").attr("data-role", "expanderContents");
        this.fieldSet.append(this.contents);
        this.contents.append(children);
        this.legend = this.fieldSet.find(">legend");
        if (this.legend.length == 0) {
            this.legend = $("<legend>").text(this.options.title);
            this.fieldSet.prepend(this.legend);
        }
    },

    _AddExpanderButton: function () {
        var expanderRadius = 11;
        var icon = $("<span>")
            .addClass("k-icon")
            .css({
                "position": "relative",
                "top": "-2px",
                "left": "-5px"
            });
        this.button = $("<a>")
            .addClass("k-button")
            .css({
                width: (expanderRadius * 2) + "px",
                height: (expanderRadius * 2) + "px",
                "border-radius": expanderRadius + "px",
                "margin-right": this.legend.text() ? "4px" : 0
            })
            .click($.proxy(this.Toggle, this));
        this.button.append(icon);
        this.legend.prepend(this.button);
    },

    _SetInitialExpandState: function () {
        if (this.options.isExpanded) {
            this.Expand(0);
        }
        else {
            this.Collapse(0);
        }
    }
});

kendo.ui.plugin(kExpander);