package platform.node;

import com.google.gson.JsonObject;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidEnvironmentException;
import models.MongoBucket;
import models.licensing.NodeLicense;
import models.node.CloudSession;
import platform.Environment;
import play.Logger;
import play.i18n.Messages;
import play.libs.WS;
import play.libs.WS.WSRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * For node-cloud communications
 *
 * @author Aye Maung
 */
public class HttpApiClient
{

    private static HttpApiClient instance = null;
    private static NodeManager nodeManager = null;

    private String cloudUrl;

    private HttpApiClient()
    {
        nodeManager = NodeManager.getInstance();
    }

    public static HttpApiClient getInstance()
    {
        if (!Environment.getInstance().onKaiNode())
        {
            throw new InvalidEnvironmentException();
        }

        if (instance == null)
        {
            instance = new HttpApiClient();
        }

        return instance;
    }

    public void setCloudUrl(String cloudUrl)
    {
        this.cloudUrl = cloudUrl;
    }

    public CloudSession userLogin(String username, String password) throws ApiException
    {
        return userLogin(nodeManager.getBucket().getName(), username, password);
    }

    /**
     * Login to cloud with user account
     *
     * @param bucketName bucket name
     * @param username   Username
     * @param password   Password
     *
     * @return CloudSession on success, null on failure
     */
    public CloudSession userLogin(String bucketName, String username, String password) throws ApiException
    {

        String apiUrl = String.format("%s/api/%s/login", cloudUrl, bucketName);

        Logger.info(String.format("Logging into cloud as %s (%s)", username, apiUrl));
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("user-name", username);
        params.put("password", password);
        JsonObject response = WS.url(apiUrl).params(params).post().getJson().getAsJsonObject();

        Logger.info("Login response: " + response);
        String result = response.get("result").getAsString();
        if (!result.equals("ok"))
        {
            throwApiError(response);
        }

        CloudSession cloudSession = new CloudSession();
        cloudSession.key = response.get("session-key").getAsString();
        cloudSession.bucketName = bucketName;
        cloudSession.userId = response.get("user-id").getAsLong();
        cloudSession.expiry = response.get("expiry").getAsLong();
        cloudSession.save();

        return cloudSession;
    }

    /**
     * Login to cloud with OTP
     *
     * @param otp One time pass
     *
     * @return CloudSession on success, null on failure
     */
    public CloudSession otpLogin(String otp) throws ApiException
    {
        CloudSession activeSession = findActiveSession(otp);
        if (activeSession != null)
        {
            return activeSession;
        }

        MongoBucket nodeBucket = nodeManager.getBucket();
        String apiUrl = String.format("%s/api/otplogin", cloudUrl);
        Logger.info(String.format("Logging into cloud with OTP (%s)", apiUrl));
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("otp", otp);
        JsonObject response = WS.url(apiUrl).params(params).post().getJson().getAsJsonObject();

        Logger.info("Login response: " + response);
        String result = response.get("result").getAsString();
        if (!result.equals("ok"))
        {
            throwApiError(response);
        }

        CloudSession newSession = new CloudSession();
        newSession.otp = otp;
        newSession.key = response.get("session-key").getAsString();
        newSession.bucketName = nodeBucket.getName();
        newSession.userId = response.get("user-id").getAsLong();
        newSession.expiry = response.get("expiry").getAsLong();
        newSession.save();

        return newSession;
    }

    /**
     * Sends the HTTP POST call to cloud server
     * For api that does not require login
     *
     * @param api API name
     * @param params  params to be sent with post call
     *
     * @return JsonObject, null if result is not "ok"
     */
    public JsonObject postAPICall(String bucketName, NodeToCloudAPI api, Map params) throws ApiException
    {
        Logger.info("Sending API [%s - %s] ...", api.getApiName(), params.toString());
        String apiUrl = String.format("%s/api/%s/%s", cloudUrl, bucketName, api.getApiName());
        JsonObject response = WS.url(apiUrl).params(params).post().getJson().getAsJsonObject();

        Logger.info("API response: " + response);
        String result = response.get("result").getAsString();
        if (!result.equals("ok"))
        {
            throwApiError(response);
        }

        return response;
    }

    /**
     * Sends the HTTP POST call to cloud server
     * <p/>
     * Note: login first to get cloudSession
     *
     * @param cloudSession on successful login
     * @param api      API name
     * @param params       params to be sent with post call
     *
     * @return JsonObject, null if result is not "ok"
     */
    public JsonObject postAPICall(CloudSession cloudSession, NodeToCloudAPI api, Map params) throws ApiException
    {
        Logger.info("Sending API ...(%s - %s)", api.getApiName(), params.toString());
        if (cloudSession == null)
        {
            Logger.error("CloudSession is null");
            return null;
        }

        params.put("session-key", cloudSession.key);
        String apiUrl = String.format("%s/api/%s/%s", cloudUrl, cloudSession.bucketName, api.getApiName());
        WSRequest request = WS.url(apiUrl).params(params);
        JsonObject response = request.post().getJson().getAsJsonObject();
        Logger.info("API response: " + response);
        String result = response.get("result").getAsString();
        if (!result.equals("ok"))
        {
            Logger.error("API Call Failed: " + api.getApiName());
            throwApiError(response);
        }

        return response;
    }

    public JsonObject postNodeToCloudCall(NodeLicense license, NodeToCloudAPI api, Map params) throws ApiException
    {
        params = params == null ? new LinkedHashMap() : params;
        params.put("license-number", license.licenseNumber);
        params.put("registration-number", license.registrationNumber);
        return postAPICall(api, params);
    }

    private CloudSession findActiveSession(String otp)
    {
        CloudSession cloudSession = CloudSession.find("otp", otp).first();
        if (cloudSession != null)
        {
            Long now = Environment.getInstance().getCurrentUTCTimeMillis();
            if (cloudSession.expiry < now)
            {
                return null;
            }
        }

        return cloudSession;
    }

    private JsonObject postAPICall(NodeToCloudAPI api, Map params) throws ApiException
    {
        return postAPICall(nodeManager.getBucket().getName(), api, params);
    }

    private void throwApiError(JsonObject response) throws ApiException
    {
        String errorReason = Messages.get("unknown");
        try
        {
            errorReason = response.get("reason").getAsString();
        }
        catch (Exception e)
        {
        }

        throw new ApiException(errorReason);
    }

}