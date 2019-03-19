var notificationManager = window.notificationManager || {};
notificationManager.running = false;

notificationManager.initialize = function () {
	//Notification sound disabled
	if(kupapi.notificationSound == "false")
		return;
	
	//Intialize jPlayer
	$(".alertSound").css("display","inline");
	notificationManager.getSoundOnOff();
	changeOnOffIcon(localStorage.isNotificationMute);
	var iDiv = document.createElement('div');
	iDiv.id = "notification_player";
	iDiv.style = "display: none;";
	document.getElementsByTagName('body')[0].appendChild(iDiv);
	
	$("#notification_player").jPlayer({
	     swfPath: "/public/javascripts/jqueryplayer",
	     wmode: "window",
	     preload: "auto"
	});
	$('#notification_player').jPlayer("setMedia", {
		mp3: "/public/files/notification/notification.mp3"
	});
	$('#notification_player').jPlayer("supplied", "mp3");
	$('#notification_player').jPlayer("solution", "html,flash");
}

notificationManager.play = function () {
	//Notification sound disabled
	if(kupapi.notificationSound == "false")
		return;
	
	//disable sound play in sleep time
	if(!notificationManager.running && localStorage.isNotificationMute == "false"){
		notificationManager.running = true;
		$("#notification_player").jPlayer("play");
		window.setTimeout(function() {
			notificationManager.running = false;
		}, kupapi.notificationSlpSecond);
	}
}

notificationManager.setSoundOnOff = function () {
	if(localStorage.isNotificationMute == "true"){
		localStorage.isNotificationMute = "false";
	}else {
		localStorage.isNotificationMute = "true";
	}
	changeOnOffIcon(localStorage.isNotificationMute);
}

notificationManager.getSoundOnOff = function () {	
	if(typeof(Storage) !== "undefined") {
		if(localStorage.isNotificationMute == "undefined" ||
				localStorage.isNotificationMute == null)
			localStorage.isNotificationMute = "false";
		
		return localStorage.isNotificationMute;
	}else{
		return "false";	
	}
}

notificationManager.clearCookie = function () {
	localStorage.removeItem("isNotificationMute");
}

function changeOnOffIcon(isMute){
	if(isMute == "true"){
		$("#alert-icon-id").removeClass("alert-sound-icon-on-unclick alert-sound-icon-on-mouseover alert-sound-icon-off-unclick alert-sound-icon-off-mouseover").addClass("alert-sound-icon-off-unclick");
		$("#alert-icon-id").attr("title",localizeResource("notification-sound-off"));
		$(".alertSound").mouseover(function(){
			$("#alert-icon-id").removeClass("alert-sound-icon-on-unclick alert-sound-icon-on-mouseover alert-sound-icon-off-unclick alert-sound-icon-off-mouseover").addClass("alert-sound-icon-off-mouseover");
		});
		$(".alertSound").mouseout(function(){
			$("#alert-icon-id").removeClass("alert-sound-icon-on-unclick alert-sound-icon-on-mouseover alert-sound-icon-off-unclick alert-sound-icon-off-mouseover").addClass("alert-sound-icon-off-unclick");
		});
		
	}else {
		$("#alert-icon-id").removeClass("alert-sound-icon-on-unclick alert-sound-icon-on-mouseover alert-sound-icon-off-unclick alert-sound-icon-off-mouseover").addClass("alert-sound-icon-on-unclick");
		$("#alert-icon-id").attr("title",localizeResource("notification-sound-on"));
		$(".alertSound").mouseover(function(){
			$("#alert-icon-id").removeClass("alert-sound-icon-on-unclick alert-sound-icon-on-mouseover alert-sound-icon-off-unclick alert-sound-icon-off-mouseover").addClass("alert-sound-icon-on-mouseover");
		});
		$(".alertSound").mouseout(function(){
			$("#alert-icon-id").removeClass("alert-sound-icon-on-unclick alert-sound-icon-on-mouseover alert-sound-icon-off-unclick alert-sound-icon-off-mouseover").addClass("alert-sound-icon-on-unclick");
		});
	}
}