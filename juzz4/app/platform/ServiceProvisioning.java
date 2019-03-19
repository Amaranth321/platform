package platform;

import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.MongoUser;
import play.Logger;

public class ServiceProvisioning
{
    public static boolean isServiceProvisioned(String userId, String serviceName) throws ApiException
    {
        try
        {
            MongoUser user = MongoUser.getById(userId);
            if (user == null)
            {
                throw new ApiException();
            }

            for (String svcName : user.getServiceNames())
            {
                if (svcName.equalsIgnoreCase(serviceName))
                {
                    return true;
                }
            }

            return false;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }
}
