package controllers.interceptors;

import controllers.api.APIController;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.MongoInventoryItem;
import models.licensing.NodeLicenseInfo;
import platform.CloudLicenseManager;
import platform.InventoryManager;
import play.mvc.Before;

/**
 * Authenticates calls from nodes. See {@link controllers.api.NodeToCloud}.
 * Calls with valid license-number and registration-number are accepted.
 *
 * @author Aye Maung
 * @since v4.3
 */
public class NodeLicenseOwner extends APIController
{
    @Before(priority = 1)
    static void validate()
    {
        try
        {
            String licenseNumber = Util.removeNonAlphanumeric(readApiParameter("license-number", true));
            String registrationNumber = readApiParameter("registration-number", true);

            //check license
            NodeLicenseInfo licenseInfo = CloudLicenseManager.getInstance().getNodeLicenseInfo(licenseNumber);
            if (licenseInfo == null)
            {
                throw new ApiException("invalid-license-number");
            }

            //check registration
            MongoInventoryItem invItem = MongoInventoryItem.getByRegistrationNumber(registrationNumber);
            if (invItem == null)
            {
                throw new ApiException("invalid-registration-number");
            }
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    @Before
    static void setCORS()
    {
        //allow all to access our API
        response.accessControl("*", null, false);

        //just return headers for the OPTIONS request
        if (request.method.equalsIgnoreCase("OPTIONS"))
        {
            response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "X-Requested-With");
            response.setHeader("Access-Control-Max-Age", "1800");
            ok();
        }
    }
}

