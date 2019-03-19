// Localize a string resource.
//
// Examples:
// localizeResource("some-message");
// localizeResource("hello-message", "Yee Fan");
//
// The second example assumes that the localized string contains format
// placeholders such as "%s". For example, if the localized resource string of
// "hello-message" is "Hello, %s!", then "Hello, Yee Fan!" is returned.

var localizeResource = function () {
    if (arguments.length < 1) {
        console.log("Call to localizeResource needs at least one argument.");
        return "";
    }
    var localizedString = i18n.apply(null, arguments);
    return localizedString;
};

