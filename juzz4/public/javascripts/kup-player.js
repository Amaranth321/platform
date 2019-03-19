var kupPlayer = {};
kupPlayer.retryOnErrorList = {};
var playerList = [];
var refreshIntervalTime = 1000*60*60*1

kupPlayer.init = function (slotId, playerId, urlList, playerType, deviceDetail, showControlbar, showPlaylist) {

    if (playerType == "jw")
        kupPlayer._playWithJW(playerId, urlList, showControlbar, showPlaylist, slotId, deviceDetail);

    else if (playerType == "vlc")
        kupPlayer._playWithVlc(playerId, urlList, showControlbar, slotId);
}

/**
 * This will not work if the player is hidden or not initialized
 *
 */
kupPlayer.getCurrentPosition = function (playerId) {
    var jwPlayer = jwplayer(playerId);
    if (jwPlayer == null) {
        console.error("getCurrentPosition: player not initialized");
        return null;
    }

    return jwPlayer.getPosition();
}

/**
 * This will not work if the player is hidden or not initialized
 *
 */
kupPlayer.setCurrentPosition = function (playerId, timeInSeconds) {
    var jwPlayer = jwplayer(playerId);
    if (jwPlayer == null) {
        console.error("getCurrentPosition: player not initialized");
        return;
    }

    jwPlayer.seek(timeInSeconds);
}


kupPlayer._playWithJW = function (playerId, urlList, showControlbar, showPlaylist, slotId, deviceDetail) {

    var listbar = false;
    var autoStart = (urlList.length > 1) ? false : true;

    if (showPlaylist) {
        listbar = {
            position: 'right',
            size: 260
        }
    }

    var playlist = [];
    $.each(urlList, function (index, url) {
        var fileName = index + 1;
        playlist.push({
            title: fileName,
            image: kupapi.CdnPath + "/common/images/play.png",
            file: url
        });
    });

    if (deviceDetail != null) {
        if (deviceDetail.displayName)
        {
            $("#" + slotId).html('<div id=' + playerId + '></div><div class="device_description" style="display:none;">' + deviceDetail.displayName + '</div>');
        }
        else
        {
            $("#" + slotId).html('<div id=' + playerId + '></div><div class="device_description" style="display:none;">' + localizeResource('no-device') + '</div>');
        }
    } else {
        $("#" + slotId).html('<div id=' + playerId + '></div>');
    }

    var jwPlayer = jwplayer(playerId).setup({
        playlist: playlist,
        width: "100%",
        height: "100%",
        listbar: listbar,
        image: kupapi.CdnPath + '/common/images/loading_icon.gif',
        autostart: autoStart,
        displaytitle: false,
        mute: true,
        rtmp: {
            bufferlength: 2
        },
        captions: {
            back: false,
            color: 'cc00000',
            fontsize: 20
        },
        base: '/public/javascripts/jwplayer7/',
        analytics: { enabled: false, cookies: false }
    });

    jwPlayer.addButton(kupapi.CdnPath + "/common/images/refresh.png", localizeResource("refresh-stream"), function () {
        jwPlayer.stop(true);
        jwPlayer.play(true);
    }, "refresh_stream");

    // Try to resume after error occurs
    jwPlayer.onError(function (error) {
        console.log("JW Error: slot-" + slotId + ", Type:" + error.type + ", Message:" + error.message);

        setTimeout(function () {
            jwPlayer.play(true);
        }, 5000);
    });

    jwPlayer.onReady(function () {
        //add remove button
        $("#" + playerId + "").append("<div class='closeIcon' onclick='kupPlayer.removeJwPlayer(event)'></div>");
    })

    //only show control while in playing state
    jwPlayer.on('buffer', function (oldstate, viewable){
        $("#" + playerId + " .jw-controlbar").hide();
    });
    jwPlayer.on('play', function (oldstate, viewable){
        $("#" + playerId + " .jw-controlbar").show();
    });

    kupPlayer.removeJwPlayer = function (e) {
        var playerId = e.currentTarget.parentNode.id.replace("_wrapper", "");

        var liveViewBoxId = playerId.replace("player_", "");
        jwplayer(playerId).remove();
        $("#" + liveViewBoxId).html("");
        $("#" + liveViewBoxId).append("<span class='dropTarget'>"+localizeResource('no-camera')+"</span>");

        liveViewMng.removeSlot(liveViewBoxId);
        setTimeout(function () {
            $("#groupName").html("");
        }, 200);
        liveViewMng.saveUserPref();

        // clear player list
        $.each(playerList, function(index, player){
        	if ((typeof player != 'undefined') && (playerId == player.id)) {
        		playerList.splice(index, 1);
        	}
        });
    }
    playerList.push(jwPlayer);
}

kupPlayer._playWithVlc = function (playerId, urlList, toolbarFlag, slotId) {

    var embedCode = '<OBJECT classid="clsid:9BE31822-FDAD-461B-AD51-BE1D1C159921" ' + 'codebase="http://downloads.videolan.org/pub/videolan/vlc/latest/win32/axvlc.cab" ' + 'width="100%" ' + 'height="100%" ' + 'id="' + playerId + '" ' + 'events="True"> ' + '<param name="MRL" value="" /> ' + '<param name="ShowDisplay" value="True" /> ' + '<param name="AutoLoop" value="False" /> ' + '<param name="AutoPlay" value="False" /> ' + '<param name="toolbar" value="' + toolbarFlag + '" /> ' + '<param name="StartTime" value="0" /> ' + '<EMBED pluginspage="" ' + 'type="application/x-vlc-plugin" ' + 'version="VideoLAN.VLCPlugin.2" ' + 'width="100%" ' + 'height="100%" ' + 'toolbar="' + toolbarFlag + '" ' + 'text="Waiting for video" ' + 'name="' + playerId + '"> ' + '</EMBED> ' + '</OBJECT> ';

    if (utils.detectBrowser() == "ie") {
        getVLC(slotId).innerHTML = embedCode;
    } else {
        document.getElementById(slotId).innerHTML = embedCode;
    }

    var vlc = getVLC(playerId);
    $.each(urlList, function (index, item) {
        vlc.playlist.add(item);
    });

    vlc.playlist.play();
    vlc.audio.toggleMute();

    if (utils.detectBrowser() == "ie") {
        vlc.style.width = "100%";
        vlc.style.height = "100%";
    }
}

// VLC helper functions
function getVLC(name) {
    if (window.document[name]) {
        return window.document[name];
    }
    if (navigator.appName.indexOf("Microsoft Internet") == -1) {
        if (document.embeds && document.embeds[name])
            return document.embeds[name];
    } else // if (navigator.appName.indexOf("Microsoft Internet")!=-1)
    {
        return document.getElementById(name);
    }
}

function doAdd(playerId, targetURL) {
    var vlc = getVLC(playerId);
    var options = [ ":vout-filter=deinterlace", ":deinterlace-mode=linear" ];
    if (vlc) {
        vlc.playlist.add(targetURL, "", options);
        options = [];
        var itemCount = doItemCount();
    }
}

function getPlayerList() {
	return playerList;
}