/**
 *
 * @param sessionKey        session key
 * @param server            Web Socket server address
 * @param apiName           api name
 * @param onDataReceived    handler for new data
 * @param $logBox           [optional] log box to append results
 *
 * @author Aye Maung
 */
function VcaFeedSubscriber(sessionKey, server, apiName, onDataReceived, $logBox)
{
    var _dataHandler = onDataReceived;
    var _$logger = $logBox;
    var _socket = null;
    var _wsUrl = null;

    var _initURL = function ()
    {
        _wsUrl = server + "/ws/" + apiName;
        if (sessionKey)
        {
            _wsUrl += "?session-key=" + sessionKey;
        }
    };

    var _open = function ()
    {
        if (_socket != null && _socket.readyState == WebSocket.OPEN)
        {
            return;
        }

        _close();
        _initURL();
        _socket = new WebSocket(_wsUrl);
        _socket.onopen = _opened;
        _socket.onclose = _closed;
        _socket.onmessage = _received;
        _socket.onerror = _errorOccurred;
    };

    var _close = function ()
    {
        if (_socket)
        {
            _socket.close();
        }
    };

    var _opened = function (evt)
    {
        _log("Connected [" + _wsUrl + "]");
    };

    var _closed = function (evt)
    {
        _log("Connection closed [" + _wsUrl + "]");
    };

    var _received = function (evt)
    {
        if (_dataHandler)
        {
            _dataHandler(evt.data);
        }
        _log(evt.data);
    };

    var _errorOccurred = function (evt)
    {
        console.error(evt);
    };

    var _log = function (log)
    {
        var timedLog = moment().format("HH:mm:ss") + " ~ " + log + "\n";
        if (_$logger)
        {
            _$logger.prepend("\n");
            _$logger.prepend(timedLog);
        }
        else
        {
            console.log(timedLog);
        }
    };

    var _clearLogs = function ()
    {
        if (_$logger)
        {
            _$logger.html("");
        }
    };

    return {
        open: _open,
        close: _close,
        clearLogs: _clearLogs
    };
}