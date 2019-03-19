package controllers.api;

import com.google.gson.Gson;
import com.mongodb.gridfs.GridFSDBFile;
import controllers.DefaultController;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.reports.ExportedFile;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import platform.content.export.ReportBuilder;
import platform.db.gridfs.GridFsDetails;
import platform.db.gridfs.GridFsHelper;
import play.Logger;
import play.jobs.Job;
import play.mvc.Http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class APIController extends DefaultController
{
    public static final String TIME_FORMAT = "ddMMyyyyHHmmss";

    protected static String getSessionKey() throws ApiException
    {
        //if session-key parameter is present, use it
        //otherwise check sessionKey in HTTP Session object
        String key = params.get("session-key");
        if (key == null || key.isEmpty())
        {
            params.put("session-key", session.get("sessionKey"));
        }

        return readApiParameter("session-key", true);
    }

    protected static String getCallerBucketId()
    {
        return renderArgs.get("caller-bucket-id").toString();
    }

    protected static String getCallerUserId()
    {
        return renderArgs.get("caller-user-id").toString();
    }

    /**
     * Convenience function to read API input parameters
     *
     * @param key      The parameter name
     * @param required true if parameter is mandatory, false if optional (if mandatory parameter
     *                 is missing, ApiException is thrown)
     *
     * @return Value of the input parameter as string
     */
    protected static String readApiParameter(String key, boolean required) throws ApiException
    {
        Map<String, String[]> inputMap = params.all();

        String value = inputMap.get(key) == null ? "" : inputMap.get(key)[0];
        value = value == null ? "" : value.trim();

        if (value.isEmpty() && required)
        {
            throw new ApiException(String.format("Missing '%s'", key));
        }

        return value;
    }

    /**
     * A simply response with {result:"ok"}
     *
     * @return response map
     */
    protected static void respondOK()
    {
        Map response = new LinkedHashMap();
        response.put("result", "ok");
        renderJSON(response);
    }

    /**
     * A response with {result:"ok", "dataFieldName": data}
     *
     * @return response map
     */
    protected static void respondOK(String dataFieldName, Object data)
    {
        Map response = new LinkedHashMap();
        response.put("result", "ok");
        response.put(dataFieldName, data);
        renderJSON(response);
    }

    /**
     * Handle thrown exceptions inside API calls
     * <p/>
     * For {@link lib.util.exceptions.ApiException}, it will respond with the exception message
     * For {@link java.lang.Exception}, it will respond with "unknown" message and logs exception stack
     *
     * @return response map
     */
    protected static void respondError(Exception e)
    {
        String apiName = request.actionMethod;
        Map errResponse = new LinkedHashMap();
        errResponse.put("result", "error");

        if (e instanceof ApiException)
        {
            Logger.error("[%s] %s", apiName, e.getMessage());
            errResponse.put("reason", e.getMessage());
        }
        else
        {
            Logger.error(e, apiName);
            errResponse.put("reason", "unknown");
        }

        renderJSON(errResponse);
    }

    /**
     * Sends a file stored in GridFs as a downloadable binary
     *
     * @param gridFsDetails
     */
    protected static void respondDownloadFile(GridFsDetails gridFsDetails)
    {
        try
        {
            String filename = Util.sanitizeFilename(gridFsDetails.getFilename());
            GridFSDBFile dbFile = GridFsHelper.getGridFSDBFile(gridFsDetails);

            renderBinary(
                    dbFile.getInputStream(),
                    filename,
                    dbFile.getLength(),
                    false
            );

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            notFound();
        }
    }

    /**
     * Stores the temporary file and reply with the download url.
     * The temp file will be removed by {@link jobs.DbCleanupJob} after one day
     */
    protected static void respondExportedFileUrl(final ReportBuilder builder)
    {
        InputStream fileIS = null;
        try
        {
            fileIS = await(new Job<InputStream>()
            {
                @Override
                public InputStream doJobWithResult()
                {
                    return builder.generate();
                }
            }.now());

            if (fileIS == null)
            {
                throw new ApiException("error-file-export-failed");
            }

            String bucketName = renderArgs.get("bucket").toString();
            ExportedFile exportedFile = ExportedFile.createNew(
                    bucketName,
                    builder.getFilename(),
                    builder.getFileFormat(),
                    fileIS);

            if (exportedFile == null)
            {
                throw new ApiException("error-file-export-failed");
            }

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("download-url", exportedFile.getDownloadUrl());
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
        finally
        {
            if (fileIS != null)
            {
                try
                {
                    fileIS.close();
                }
                catch (IOException e)
                {
                }
            }
        }
    }

    /**
     * @param timestamp browser-sent timestamp in UTC with the format {@link APIController#TIME_FORMAT}
     *
     * @return milliseconds
     */
    public static long toMilliseconds(String timestamp) throws ApiException
    {
        try
        {
            DateTimeFormatter formatter = DateTimeFormat.forPattern(TIME_FORMAT).withZoneUTC();
            DateTime utcDt = DateTime.parse(timestamp, formatter);
            return utcDt.getMillis();
        }
        catch (Exception e)
        {
            throw new ApiException("invalid-timestamp");
        }
    }

    public static int asInt(String intInput) throws ApiException
    {
        try
        {
            return Integer.parseInt(intInput);
        }
        catch (Exception e)
        {
            throw new ApiException("invalid-number-input");
        }
    }

    public static long asLong(String longInput) throws ApiException
    {
        try
        {
            return Long.parseLong(longInput);
        }
        catch (Exception e)
        {
            throw new ApiException("invalid-number-input");
        }
    }

    public static double asDouble(String doubleInput) throws ApiException
    {
        try
        {
            return Double.parseDouble(doubleInput);
        }
        catch (Exception e)
        {
            throw new ApiException("invalid-number-input");
        }
    }

    public static boolean asBoolean(String booleanInput) throws ApiException
    {
        try
        {
            return Boolean.parseBoolean(booleanInput);
        }
        catch (Exception e)
        {
            throw new ApiException("invalid-number-input");
        }
    }

    protected static void logApiAndParams()
    {
        List<String> paramIgnoreList = Arrays.asList(
                "body", "bucket", "session-key"
        );

        List<String> apiIgnoreList = Arrays.asList(
                "recvcometnotification"
        );

        if (apiIgnoreList.contains(request.actionMethod))
        {
            return;
        }

        Map<String, String> inputMap = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : params.all().entrySet())
        {
            if (paramIgnoreList.contains(entry.getKey()))
            {
                continue;
            }

            inputMap.put(entry.getKey(), entry.getValue()[0]);
        }

        Logger.info("[%s] %s", request.actionMethod, new Gson().toJson(inputMap));
    }
    
    /**
     * Use to get user-agent from the request header
     * @return
     */
    protected static String getUserAgent()
    {
        String userAgent = "";
        
        if(request.headers != null && request.headers.containsKey("user-agent"))
        {
            userAgent = request.headers.get("user-agent").toString().toLowerCase();
        }
        
        return userAgent;
    }
}
