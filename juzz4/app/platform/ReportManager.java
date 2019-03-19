package platform;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.*;
import play.Logger;
import play.Play;
import play.modules.morphia.Model.MorphiaQuery;

import java.io.File;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReportManager
{

    private static ReportManager instance = new ReportManager();
    public static final String REPORT_DIRECTORY = "/public/files/tmp/";         //this must be under /public/

    static
    {
        //REPORT_DIRECTORY
        String reportFolder = Play.applicationPath + REPORT_DIRECTORY;
        File reportFolderFile = new File(reportFolder);
        if (!reportFolderFile.exists())
        {
            reportFolderFile.mkdir();
        }
    }

    private ReportManager()
    {
    }

    public static ReportManager getInstance()
    {
        return instance;
    }

    public class DateDeserializer implements JsonDeserializer<Date>
    {

        @Override
        public Date deserialize(JsonElement element, Type arg1, JsonDeserializationContext arg2) throws JsonParseException
        {
            String date = element.getAsString();

            SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
            try
            {
                return formatter.parse(date);
            }
            catch (ParseException e)
            {
                Logger.error("%s: %s", "Exception in ReportManager.DateDeserializer.deserialize()", e.toString());
                return null;
            }
        }
    }

    public String getTmpDirectory()
    {
        return new File(Play.applicationPath + REPORT_DIRECTORY).getAbsolutePath();
    }

    public ReportQueryHistory getReportQueryHistory(String userId, String eventType)
    {
        try
        {
            return ReportQueryHistory.q()
                    .filter("userId", Long.parseLong(userId))
                    .filter("eventType", eventType)
                    .get();
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
        }
        return null;
    }

    public void saveReportQueryHistory(String userId, String eventType, String selectedDevice, String dateFr, String dateTo) throws ApiException
    {
        try
        {
            MongoUser user = MongoUser.getById(userId);
            if (user == null)
            {
                throw new ApiException("invalid-user-id");
            }
            List<DeviceSelected> selectedDevices = new Gson().fromJson(selectedDevice, new TypeToken<List<DeviceSelected>>(){}.getType());

            ReportQueryHistory reportQueryObj = getReportQueryHistory(userId, eventType);
            if (reportQueryObj == null)
            {
                reportQueryObj = new ReportQueryHistory();
                reportQueryObj.userId = Long.parseLong(user.getUserId());
            }

            reportQueryObj.eventType = eventType;
            reportQueryObj.dateFrom = dateFr;
            reportQueryObj.dateTo = dateTo;
            reportQueryObj.deviceSelected = selectedDevices;
            reportQueryObj.save();
        }
        catch (ApiException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
        }
    }

    /**
     * Internal api
     *
     * @param platformDeviceId will be null if called from labelManager.java but should not be null
     *                         if called from deviceManager.java
     * @param label            will be null if called from deviceManager.java but should not be null
     *                         if called from labelManager.java
     * @param currentUserId    will be 0L if called from deviceManager.java but should not be 0L
     *                         if called from labelManager.java
     */
    public void removeReportQueryHistory(String deviceId, String label, String userId)
    {
        try
        {
            if (Util.isNullOrEmpty(deviceId) && Util.isNullOrEmpty(label))
            {
                return;
            }
            MorphiaQuery query = ReportQueryHistory.q();
            if (!Util.isNullOrEmpty(deviceId))
            {
                query.filter("deviceSelected.deviceId", deviceId);
            }
            else if (!Util.isNullOrEmpty(label))
            {
                query.filter("deviceSelected.label", label);
            }
            else if (!userId.equals(""))
            {
                query.filter("userId", Long.parseLong(userId));
            }
            query.delete();

        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
        }
    }

    public void updatePOSSalesData(String bucketId, Date dateFr, Date dateTo, String name, String posData) throws ApiException
    {
        try
        {
            MongoBucket bucket = MongoBucket.getById(bucketId);
            if (bucket == null)
            {
                throw new ApiException("invalid-bucket-id");
            }

            MorphiaQuery query = PosDataReport.q();
            query.filter("bucket", bucketId);
            query.filter("name", name);
            query.filter("sales.time >=", dateFr);
            query.filter("sales.time <", dateTo);
            query.delete();

            Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new DateDeserializer()).create();
            List<SalesRecord> POSDataList = gson.fromJson(posData, new TypeToken<List<SalesRecord>>(){}.getType());

            PosDataReport posDataReport;
            for (SalesRecord salesRecord : POSDataList)
            {
                posDataReport = new PosDataReport();
                posDataReport.bucket = bucketId;
                posDataReport.name = name;
                posDataReport.sales = salesRecord;
                posDataReport.save();
            }
        }
        catch (ApiException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
        }
    }

    public List<PosDataReport> getPOSSalesData(String bucketId, Date dateFr, Date dateTo, String name, String parserType) throws ApiException
    {
        List<PosDataReport> POSData = new ArrayList<>();
        try
        {
            MongoBucket bucket = MongoBucket.getById(bucketId);
            if (bucket == null)
            {
                throw new ApiException("invalid-bucket-id");
            }

            MorphiaQuery query = PosDataReport.q();
            query.filter("bucket", bucketId);
            query.filter("name", name);
            query.filter("sales.time >=", dateFr);
            query.filter("sales.time <", dateTo);

            if (!"".equals(parserType))
            {
                query.field("parserType").equal(parserType);
            }
            query.order("sales.time");

            POSData = query.asList();

            return POSData;
        }
        catch (ApiException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
        }
        return POSData;
    }
}
