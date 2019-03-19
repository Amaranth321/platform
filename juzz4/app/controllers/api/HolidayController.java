package controllers.api;

import java.lang.reflect.Type;
import java.util.*;

import lib.util.ResultMap;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.Holiday;
import platform.UserProvisioning;
import play.i18n.Messages;
import play.mvc.With;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import controllers.interceptors.APIInterceptor;

/**
 * @author KAI Square
 *         publicapi (hidden from API documentation)
 * @sectiontitle Holiday Management
 * @sectiondesc Holidays & National Holiday & Significant days & event days import on platform.
 */

@With(APIInterceptor.class)
public class HolidayController extends APIController
{

	private static class CountrySerializer implements JsonSerializer<Locale> {
		private Locale locale;
		private CountrySerializer(Locale locale) {
			this.locale = locale;
		}
		@Override 
		public JsonElement serialize(Locale src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject obj = new JsonObject();
			obj.addProperty("code", src.getCountry());
			obj.addProperty("name", src.getDisplayCountry(locale));
			return obj;
		}
	}
	
    /**
     * @param title         The holiday title.
     * @param des           The holiday description.
     * @param isEvent       Holiday is the event day or not.
     * @param isHoliday     Holiday is the holiday or not.
     * @param isSignificant Holiday is the significant day or not.
     * @param from          The holiday begin date, Timestamp with format ddMMyyyyHHmmss.
     * @param to            The holiday end date, Timestamp with format ddMMyyyyHHmmss.
     * @param countries     The holiday is in which countries
     *
     * @servtitle Add a new Holiday
     * @httpmethod POST
     * @uri /api/{bucket}/addholiday
     * @responsejson {
     * "result":"ok"
     * }
     * @responsejson{ "result":"error",
     * "reason":"unknown"
     * }
     */
    public static void addholiday()
    {
        try
        {
            String title = readApiParameter("title", true);
            String description = readApiParameter("des", false);
            boolean isEvent = asBoolean(readApiParameter("isEvent", true));
            boolean isHoliday = asBoolean(readApiParameter("isHoliday", true));
            boolean isSignificant = asBoolean(readApiParameter("isSignificant", true));
            long fromMillis = toMilliseconds(readApiParameter("from", true));
            long toMillis = toMilliseconds(readApiParameter("to", true));
            String countries = readApiParameter("countries", true);

            Holiday sameHoliday = Holiday.find("title", title).get();
            if (null != sameHoliday) {
                throw new ApiException("holiday-already-exists");
            }

            if (Util.isNullOrEmpty(description))
                description = "";

            Set<String> countryList = new HashSet<>();
            Collections.addAll(countryList, countries.split(","));

            Holiday holiday = new Holiday();
            holiday.setTitle(title);
            holiday.setDescription(description);
            holiday.setIsEvent(isEvent);
            holiday.setIsHoliday(isHoliday);
            holiday.setIsSignificant(isSignificant);
            holiday.setFrom(fromMillis);
            holiday.setTo(toMillis);
            holiday.setCountries(countryList);
            holiday.save();

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Get all holidays
     * @httpmethod POST
     * @uri /api/{bucket}/getholidays
     * @responsejson {
     * "result":"ok",
     * "datas":[
     * {
     * "title":"New Year",
     * "description":"New Year",
     * "isEvent":true,
     * "isHoliday":true,
     * "isSignificant":false,
     * "from":1420012800000,
     * "to":1420099199000,"
     * countries":["Taiwan"]
     * },{
     * "title":"Dragon boat festival",
     * "description":"Dragon boat festival",
     * "isEvent":true,
     * "isHoliday":true,
     * "isSignificant":false,
     * "from":1434528000000,
     * "to":1434873599000,
     * "countries":["Taiwan"]
     * }
     * ]
     * }
     */
    public static void getholidays()
    {
    	try {
            String userId = getCallerUserId();
            String language = UserProvisioning.getUserProfile(userId).getLanguage();
            Locale locale;
        	switch (language)
            {
            	case "zh-tw": locale = Locale.TRADITIONAL_CHINESE;
        		break;
        		case "zh-cn" : locale = Locale.SIMPLIFIED_CHINESE;
        		break;
        		default: locale = Locale.ENGLISH;
        	}
        	
            List<Holiday> holidayList = Holiday.findAll();
            for (Holiday holiday : holidayList) 
            {
            	Set<String> countryList = new HashSet<>();
            	for (String country : holiday.getCountries()) {
            		if (country.equals("worldwide")) {
            			countryList.add(Messages.get("worldwide"));
            		} else {
            			countryList.add(new Locale("", country).getDisplayCountry(locale));
            		}
            	}
            	holiday.setCountries(countryList);
            }

            ResultMap map = new ResultMap();
            map.put("result", "ok");
            map.put("datas", holidayList);
            renderJSON(map);
            
    	} catch (Exception e) {
    		respondError(e);
    	}
    }

    public static void deleteholiday()
    {
        try
        {
            String holidayId = readApiParameter("holidayId", true);

            Holiday holiday = Holiday.findById(holidayId);
            if (null == holiday)
            {
                throw new ApiException("Invalid-holiday-id");
            }

            holiday.delete();

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    public static void updateholiday()
    {
        try
        {
            String id = readApiParameter("id", true);
            String title = readApiParameter("title", true);
            String description = readApiParameter("des", false);
            boolean isEvent = asBoolean(readApiParameter("isEvent", true));
            boolean isHoliday = asBoolean(readApiParameter("isHoliday", true));
            boolean isSignificant = asBoolean(readApiParameter("isSignificant", true));
            long fromMillis = toMilliseconds(readApiParameter("from", true));
            long toMillis = toMilliseconds(readApiParameter("to", true));
            String countries = readApiParameter("countries", true);

            Holiday holiday = Holiday.findById(id);
            if (null == holiday)
            {
                throw new ApiException("invalid-holiday-id");
            }

            Holiday sameHoliday = Holiday.find("title", title).get();
            if (null != sameHoliday && !sameHoliday.getIdAsStr().equals(id))
            {
                throw new ApiException("holiday-already-exists");
            }
            
            if (Util.isNullOrEmpty(description))
                description = "";

            Set<String> countryList = new HashSet<>();
            Collections.addAll(countryList, countries.split(","));

            holiday.setTitle(title);
            holiday.setDescription(description);
            holiday.setIsEvent(isEvent);
            holiday.setIsHoliday(isHoliday);
            holiday.setIsSignificant(isSignificant);
            holiday.setFrom(fromMillis);
            holiday.setTo(toMillis);
            holiday.setCountries(countryList);
            holiday.save();

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
    
    public static void getcountrylist() 
    {
    	Map resultMap = new ResultMap();
    	try {
    		String[] countryCodes = Locale.getISOCountries();
        	
        	List<Locale> countryList = new ArrayList<>();
        	for (String code : countryCodes) {
        		countryList.add(new Locale("", code));
        	}

            String userId = getCallerUserId();
            String language = UserProvisioning.getUserProfile(userId).getLanguage();
            Locale locale;
        	switch (language)
            {
	        	case "zh-tw": locale = Locale.TRADITIONAL_CHINESE;
	    		break;
        		case "zh-cn" : locale = Locale.SIMPLIFIED_CHINESE;
        		break;
        		default: locale = Locale.ENGLISH;
        	}
        	
        	resultMap.put("result", "ok");
        	resultMap.put("countryList", countryList);
        	renderJSON(resultMap, new CountrySerializer(locale));
        	
    	} catch (Exception e) {
    		respondError(e);
    	}
    }


}
