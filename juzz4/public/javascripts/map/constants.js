//  SEA: 13.447368, 108.193359, zoom:5
//  SG: 1.3567, 103.82, zoom:12
//  TW: 23.6978,120.9605, zoom:8

var MAP_CENTER_LAT = 13.447368;
var MAP_CENTER_LNG = 108.193359;
var MAP_ZOOM = 4;
var MAP_FOCUSED_ZOOM = 16;
var MAP_ICON_URL = kupapi.CdnPath + "/common/images/mapicons/";


var poiTypes = [ {
	text : localizeResource('landmark'),
	value : 'landmark'
}, {
	text : localizeResource('bus-stop'),
	value : 'bus-stop'
}, {
	text : localizeResource('gas-station'),
	value : 'gas-station'
}, {
	text : localizeResource('wifi-access'),
	value : 'wifi-access'
} ];

var mapIcons = {
	"landmark" : MAP_ICON_URL + "landmark.png",
	"bus-stop" : MAP_ICON_URL + "busstop.png",
	"gas-station" : MAP_ICON_URL + "gasstation.png",
	"wifi-access" : MAP_ICON_URL + "wifi.png",

	"site" : MAP_ICON_URL + "site.png",
	"camera" : MAP_ICON_URL + "camera.png",
	"moving-vehicle" : MAP_ICON_URL + "movingvehicle.png",
	"markerA" : MAP_ICON_URL + "markerA.png",
	"markerB" : MAP_ICON_URL + "markerB.png"
}