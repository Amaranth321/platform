package controllers.api;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;
import com.google.code.geocoder.model.GeocoderStatus;
import com.google.gson.Gson;
import controllers.interceptors.LoginInterceptor;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import platform.LocationManager;
import platform.access.UserSessionManager;
import platform.db.cache.proxies.CachedLoginSession;
import play.Logger;
import play.mvc.With;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author KAI Square
 *         publicapi (hidden from API documentation)
 * @sectiontitle Generic Geocode Service
 * @sectiondesc APIs for getting geocode by selected services (Google or Baidu)
 */

@With(LoginInterceptor.class)
public class GeoServices extends APIController
{
    public static String BAIDU_KEY = "BVqcGIp8NRN7sq4LlUbju3zf";

    /**
     * @param address address to be reverse-gecoded. Mandatory
     *
     * @servtitle Returns location info
     * @httpmethod POST
     * @uri "/api/reversegeocode"
     * @responsejson {
     * "result": "ok",
     * "lat": 1.3161811,
     * "lng": 103.7649377
     * @responsejson {
     * "result": "error",
     * "reason": "ERROR"
     * }
     */
    public static void reversegeocode()
    {
        Map responseMap = new LinkedHashMap();
        try
        {
            String address = readApiParameter("address", true);

            String callerBucketId = getBucketId();
            String mapSource = LocationManager.getInstance().getMapSource(callerBucketId);
            Logger.info(Util.whichFn() + "Map used : %s", mapSource);

            if ("google".equals(mapSource))
            {
                Map gsmap = googleReversegeocode(address);
                responseMap.put("result", "ok");
                responseMap.put("lat", gsmap.get("lat"));
                responseMap.put("lng", gsmap.get("lng"));
            }
            else
            {
                Map bsmap = baiduReversegeocode(address);
                responseMap.put("result", "ok");
                responseMap.put("lat", bsmap.get("lat"));
                responseMap.put("lng", bsmap.get("lng"));
            }

        }
        catch (ApiException apiEx)
        {
            Logger.error(Util.getStackTraceString(apiEx));
            responseMap.put("result", "error");
            responseMap.put("reason", apiEx.getMessage());
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            responseMap.put("result", "error");
            responseMap.put("reason", "unknown");
        }

        renderJSON(responseMap);
    }

    /**
     * For internal use, not an api
     */
    private static Map googleReversegeocode(String address)
    {
        Map responseMap = new LinkedHashMap();
        try
        {
            final Geocoder geocoder = new Geocoder();
            GeocoderRequest geocoderRequest = new GeocoderRequestBuilder()
                    .setAddress(address)
                    .setLanguage("en")
                    .getGeocoderRequest();

            GeocodeResponse geocoderResponse = geocoder.geocode(geocoderRequest);
            GeocoderStatus status = geocoderResponse.getStatus();
            if (!status.equals(GeocoderStatus.OK))
            {
                throw new ApiException(status.value());
            }

            GeocoderResult geocodeResult = geocoderResponse.getResults().get(0);
            responseMap.put("lat", geocodeResult.getGeometry().getLocation().getLat());
            responseMap.put("lng", geocodeResult.getGeometry().getLocation().getLng());

            return responseMap;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return null;
        }
    }

    /**
     * For internal use, not an api
     */
    private static Map baiduReversegeocode(String address)
    {
        try
        {
            String urlString = "http://api.map.baidu.com/geocoder/v2/?";
            StringBuilder builder = new StringBuilder();
            builder.append("ak=").append(URLEncoder.encode(BAIDU_KEY, "UTF-8"));
            builder.append("&output=").append(URLEncoder.encode("json", "UTF-8"));
            builder.append("&address=").append(URLEncoder.encode(address, "UTF-8"));
            String data = builder.toString();
            URL url = new URL(urlString + data);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            Writer writer = new OutputStreamWriter(conn.getOutputStream());
            writer.close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response = reader.readLine();

            Map responseJson = new Gson().fromJson(response, Map.class);
            Map location = (Map) responseJson.get("result");
            Map latlng = (Map) location.get("location");

            return latlng;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return null;
        }
    }

    /**
     * For internal use, not an api
     */
    private static String getBucketId()
    {
        String sessionKey = session.get("sessionKey");
        UserSessionManager sessionManager = UserSessionManager.getInstance();
        if (!sessionManager.isSessionValid(sessionKey))
        {
            return "";
        }

        CachedLoginSession cachedSession = sessionManager.findSession(sessionKey);
        return String.valueOf(cachedSession.getBucketId());
    }
}
