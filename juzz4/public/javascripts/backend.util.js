/**
 * @author Aye Maung
 */
var BackendUtil = (function ()
{
    var _throwMissingTypeFile = function ()
    {
        throw new TypeError("backend.VcaTypes not found. Check backend.types.js file");
    };

    var parseVcaType = function (typeName)
    {
        if (!backend || !backend.VcaTypeInfo)
        {
            _throwMissingTypeFile();
        }

        var parsedType = null;
        $.each(backend.VcaTypeInfo, function (type, typeInfo)
        {
            if (typeInfo.typeName === typeName)
            {
                parsedType = type;
            }
        });
        return parsedType;
    };

    var parseEventType = function(typeName)
    {
        if (!backend || !backend.VcaTypeInfo)
        {
            _throwMissingTypeFile();
        }

        var parsedType = null;
        $.each(backend.EventTypeInfo, function (type, typeInfo)
        {
            if (typeInfo.typeName === typeName)
            {
                parsedType = type;
            }
        });
        return parsedType;
    };

    return {
        parseVcaType: parseVcaType,
        parseEventType: parseEventType
    };
})();



