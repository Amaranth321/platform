var baiduMapManager = window.baiduMapManager || {};
baiduMapManager.map = null;
baiduMapManager.markerClusterer = null;
baiduMapManager.markerLayers = {};
baiduMapManager.markerTypes = ["poi", "devices", "vehicles", "camera", "idle", "acceleration", "braking", "left", "right", "up", "down"];    // use index to specify type, string name is for reference only
baiduMapManager.infoBox = null;
baiduMapManager.userDevices = [];
baiduMapManager.eventsOnMap = [];
baiduMapManager.polyline = null;
baiduMapManager.drawingManager = null;
baiduMapManager.saveMarker = null;
infoBoxStatus = false;
baiduMapManager.geocoder = null;//new google.maps.Geocoder();
baiduMapManager.infoMarkerList = [];
baiduMapManager.infoContentList = [];
baiduMapManager.infoWindowListOpen = false;


baiduMapManager.initialize = function (mapDivId, centerLat, centerLng, zLvl, zoomControl, callback) {
	
	var map = new BMap.Map(mapDivId);   
	map.centerAndZoom(new BMap.Point(centerLng,centerLat), zLvl);	// 初始化地图,设置中心点坐标和地图级别
	map.addControl(new BMap.ScaleControl());                    	// 添加比例尺控件
	map.addControl(new BMap.OverviewMapControl());              	//添加缩略地图控件
	map.enableScrollWheelZoom();                            		//启用滚轮放大缩小
//	map.addControl(new BMap.MapTypeControl());          			//添加地图类型控件	
//	map.setCurrentCity("北京");          								// 设置地图显示的城市 此项是必须设置的
	if(zoomControl){
		map.addControl(new BMap.NavigationControl());           	// 添加平移缩放控件
	}

	baiduMapManager.map = map;
    
    //clustering
	var mcOptions = {gridSize: 50, maxZoom: 15, minClusterSize: 2};
    baiduMapManager.markerClusterer = new BMapLib.MarkerClusterer(baiduMapManager.map, mcOptions);
    baiduMapManager.resetLayers();

    callback();
}

baiduMapManager.resetLayers = function () {
    $.each(baiduMapManager.markerTypes, function (index, item) {
        baiduMapManager.markerLayers[index] = {};
    });

    baiduMapManager.markerClusterer.clearMarkers();

    if (baiduMapManager.polyline != null)
        baiduMapManager.polyline.setMap(null);
}

baiduMapManager.addMapEventListener = function (eventType, func) {
    switch (eventType) {
        case "click":
        	baiduMapManager.map.addEventListener("click",func);        	
            break;
        default:
            break;
    }
}

baiduMapManager.addAutoCompleteSearchListener = function (inputFieldId) {
    var autocomplete = new BMap.Autocomplete({
    	"input" : inputFieldId,
    	"location" : baiduMapManager.map
    	});

    autocomplete.addEventListener("onconfirm", function(e) {    //鼠标点击下拉列表后的事件
        var _value = e.item.value;
        var myValue = _value.province +  _value.city +  _value.district +  _value.street +  _value.business;
        var myGeo = new BMap.Geocoder();
        myGeo.getPoint(myValue, function(point){
          if (point) {
        	  baiduMapManager.map.centerAndZoom(point,14);
          }
        }, "北京");
        
     
    });
}

baiduMapManager.contentString = function(marker, kendoInfoJson) {
	for (var i = 0; i<baiduMapManager.infoMarkerList.length ; i++) {
		if(baiduMapManager.infoMarkerList[i].getTitle()==marker.getTitle())
			return;
    }
	baiduMapManager.infoMarkerList.push(marker);
	baiduMapManager.infoContentList.push(kendoInfoJson);
}

baiduMapManager.addNewMarker = function (id, type, lat, lng, imgUrl, infoJson, callback) {	
	var point = new BMap.Point(lng, lat);	
	var myIcon = new BMap.Icon(imgUrl,new BMap.Size(32,37));
	var marker = null;
	
	setTimeout(function(){
		//create marker and event    			
		marker = new BMap.Marker(point,{icon:myIcon});
		var title = "";
		if (infoJson){title = infoJson.name!= null?infoJson.name:infoJson.vehicleLicensePlate;}	            
        marker.setTitle(title);
		marker.setPosition(point);
		if (infoJson) {
	    	var kTemplate = kendo.template($("#InfoWindowTemplate").html());
	    	var kTemplate2 = kendo.template($("#infoBoxTemplate").html());
			var infowindow = new BMap.InfoWindow(kTemplate(infoJson));
	        marker.addEventListener("click", function(){this.openInfoWindow(infowindow);});  
	        baiduMapManager.contentString(marker, kTemplate2(infoJson));
	        baiduMapManager.markerClusterer.addMarker(marker); 
	    }		    
	    baiduMapManager.markerLayers[type][id] = marker;
	    callback(marker);
	    
	}, 300);
}

baiduMapManager.addNewEventMarker = function (id, type, lat, lng, imgUrl, vehInfo, callback) {
	var point = new BMap.Point(lng, lat);	
	var myIcon = new BMap.Icon(imgUrl,new BMap.Size(32,37));
	var marker = null;
	
	setTimeout(function(){
		//create marker and event    			
		marker = new BMap.Marker(point,{icon:myIcon});
		var title = "";
		if (vehInfo){title = vehInfo.name!= null?vehInfo.name:vehInfo.vehicleLicensePlate;}	            
        marker.setTitle(title);
		marker.setPosition(point);
		if (vehInfo) {
	    	var kTemplate = kendo.template($("#InfoWindowTemplate").html());
	    	var kTemplate2 = kendo.template($("#infoBoxTemplate").html());
			var infowindow = new BMap.InfoWindow(kTemplate(vehInfo));
	        marker.addEventListener("click", function(){this.openInfoWindow(infowindow);});  
	        baiduMapManager.contentString(marker, kTemplate2(vehInfo));
	        baiduMapManager.markerClusterer.addMarker(marker); 
	    }		    
	    baiduMapManager.markerLayers[type][id] = marker;
	    callback(marker);
	    
	}, 300);
}


baiduMapManager.updateMarker = function (marker) {
    baiduMapManager.markerClusterer.addMarker(marker);
}

baiduMapManager.clearMarkersByType = function (type) {
    var toRemove = baiduMapManager.markerLayers[type];
    $.each(toRemove, function (index, item) {
        baiduMapManager.markerClusterer.removeMarker(item);
    });
    baiduMapManager.markerLayers[type] = {};
}

baiduMapManager.clearAllMarkers = function () {
    baiduMapManager.markerClusterer.clearMarkers();
    baiduMapManager.resetLayers();
}

baiduMapManager.setVisibilityByType = function (type, isVisible) {
    var markers = baiduMapManager.markerLayers[type];
    $.each(markers, function (index, item) {
        if (isVisible) {
        	baiduMapManager.markerClusterer.addMarker(item);
        }
        else {
        	baiduMapManager.markerClusterer.removeMarker(item);
        }
    });
}

baiduMapManager.loadPoiMarkers = function () {
    baiduMapManager.clearMarkersByType(0);
    getBucketPois("", function (responseData) {
        if (responseData.result == "ok" && responseData.pois != null) {
            $.each(responseData.pois, function (index, item) {
                baiduMapManager.addNewMarker(item._id, 0, item.latitude, item.longitude, mapIcons[item.type], item, function(e){});
            });
        }
    }, null);

}

baiduMapManager.loadDevices = function () {
    baiduMapManager.clearMarkersByType(1);
    getUserDevices("", function (responseData) {
        if (responseData.result == "ok" && responseData.devices != null) {
            baiduMapManager.userDevices = responseData.devices;
            $.each(baiduMapManager.userDevices, function (index, dvc) {
                baiduMapManager.addNewMarker(dvc.id, 1, dvc.latitude, dvc.longitude, mapIcons["camera"], dvc, function(e){});
            });
        }
    }, null);
}

baiduMapManager.prepareUserMapItems = function (callback) {
    getUserDevices("", function (responseData) {
        if (responseData.result == "ok" && responseData.devices != null) {
            baiduMapManager.userDevices = responseData.devices;
            
            
            getUserVehicles("", function (responseData) {
                if (responseData.result == "ok" && responseData.vehicles != null) {
                    baiduMapManager.userVehicles = responseData.vehicles;
                }
                callback();
            }, null);
        } else {
            callback();
        }
    }, null);

}

baiduMapManager.drawPolyLine = function (strokeColor, strokeWeight, locations, callback) {

    //clear map
    baiduMapManager.resetLayers();

    var latlngPoints = [];
    var bounds = new google.maps.LatLngBounds();

    //convert to google latlng objects
    //loc.timestamp is available for each point
    $.each(locations, function (index, loc) {
        var latlngPoint = new google.maps.LatLng(loc.latitude, loc.longitude);
        latlngPoints.push(latlngPoint);
        bounds.extend(latlngPoint);
    });

    baiduMapManager.polyline = new google.maps.Polyline({
        path: latlngPoints,
        strokeColor: strokeColor,
        strokeWeight: strokeWeight
    });

    baiduMapManager.map.fitBounds(bounds);
    setTimeout(function () {
        baiduMapManager.polyline.setMap(baiduMapManager.map);
        callback();
    }, 1000);
}

//Legacy function. Deprecated. Use the new one below
baiduMapManager.devicePageGeocode = function (request, containers) {
    baiduMapManager.geocoder.geocode(request, function (results, status) {
        if (status == google.maps.GeocoderStatus.OK) {
            value = results[0].geometry.location.toString();
            value = value.replace('(', '');
            value = value.replace(')', '');
            var coords = value.split(',');
            lat = parseFloat(coords[0]);
            lng = parseFloat(coords[1]);

            containers.lat.data("kendoNumericTextBox").value(lat);
            containers.lng.data("kendoNumericTextBox").value(lng);

            containers.errorBox.hide();
        } else if (status == google.maps.GeocoderStatus.ZERO_RESULTS) {
            containers.errorBox.show();
        } else {
            containers.errorBox.show();
        }
    });
}

//Geocodes address to latitude, longitude
//use callback
baiduMapManager.getLatLngByAddress = function (address, callback) {

    baiduMapManager.geocoder.geocode({'address': address}, function (results, status) {

        var latlngResult = {
            "lat": 0,
            "lng": 0
        }

        if (status == google.maps.GeocoderStatus.OK) {
            var returnVal = results[0].geometry.location.toString();
            returnVal = returnVal.replace('(', '').replace(')', '');
            var coords = returnVal.split(',');

            latlngResult.lat = parseFloat(coords[0]);
            latlngResult.lng = parseFloat(coords[1]);
        }

        callback(latlngResult);
    });
}

//baiduMapManager.baiduToGoogleLatLng = function (point,callback) {
//	var gc = new BMap.Geocoder(); 
//	gc.getLocation(point, function(rs){
//        var addComp = rs.addressComponents;
//        if(addComp.province =="香港特别行政区" || addComp.province =="澳门特别行政区" || addComp.province ==""){
//        	BMap.Convertor.translateBaiduToGoogle(point,function (pointb){
//           	 	var newX = 2*point.lng-pointb.lng;
//                var newY = 2*point.lat-pointb.lat;
//                alert("Baidu HK&Macau : "+point.lat+","+point.lng);
//                alert("Baidu HK&Macau : "+newY+","+newX);
//           	 	callback(new BMap.Point(newX, newY));	
//            });
//        }else {
//        	alert("Baidu ORG Mainland : "+(point.lat)+","+(point.lng));
//        	alert("Baidu Mainland : "+(point.lat-0.0061)+","+(point.lng-0.0063));
//        	callback(new BMap.Point((point.lng-0.0063), (point.lat-0.0061)));	
//        } 
//    });     
//}

