var soundManager = window.soundManager || {};
soundManager.running = false;

/*
    soundManager.initialize() is called from header.ctrl.js
*/
soundManager.initialize = function () {
    var iDiv = document.createElement('div');
    iDiv.id = "notification_player";
    iDiv.style = "display: none;";
    document.getElementsByTagName('body')[0].appendChild(iDiv);

    $("#notification_player").jPlayer({
        swfPath: "static/js/plugin/jqueryplayer",
        wmode: "window",
        preload: "auto"
    });
    $('#notification_player').jPlayer("setMedia", {
        mp3: "static/files/notification/notification.mp3"
    });
    $('#notification_player').jPlayer("supplied", "mp3");
    $('#notification_player').jPlayer("solution", "html,flash");
}

soundManager.play = function () {
        $("#notification_player").jPlayer("play");
}
