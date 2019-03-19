package platform.node;

import com.google.gson.Gson;
import lib.util.exceptions.ApiException;
import models.licensing.LicenseStatus;
import models.licensing.NodeLicense;
import org.apache.commons.io.IOUtils;
import platform.CryptoManager;
import play.Logger;

import java.io.File;

/**
 * To be used by node platform only.
 * Use to manage Node license with file encryption
 *
 * @author Keith Chong
 */
public class NodeLicenseManager
{
    private static final String NODE_LICENSE_FILEPATH = "../";
    private static final String NODE_LICENSE_FILENAME = "license.cert";
    private static NodeLicense nodeLicense = null;
    private static NodeLicenseManager instance = null;

    public static NodeLicenseManager getInstance() throws ApiException
    {
        if (instance == null)
        {
            instance = new NodeLicenseManager();
        }
        return instance;
    }

    private NodeLicenseManager()
    {
        File dir = new File(NODE_LICENSE_FILEPATH);
        if (!dir.exists())
        {
            dir.mkdir();
        }
    }

    private NodeLicense decryptLicenseFile()
    {
        if (nodeLicense != null)
        {
            return nodeLicense;
        }

        try
        {
            byte[] data = CryptoManager.readEncryptedFile(NODE_LICENSE_FILEPATH, NODE_LICENSE_FILENAME);
            String str = IOUtils.toString(data, "UTF-8");

            if (str == null)
            {
                return null;
            }

            //convert Json String to NodeLicense object
            NodeLicense license = new Gson().fromJson(str, NodeLicense.class);
            nodeLicense = license;
            return license;
        }
        catch (Exception e)
        {
            Logger.error(e.getMessage());
            return null;
        }
    }

    private boolean encryptLicenseFile(NodeLicense nodeLicense)
    {
        try
        {
            //Convert NodeLicense object to Json String
            String str = new Gson().toJson(nodeLicense);
            boolean result = CryptoManager.createEncryptFile(str.getBytes(),
                                                             NODE_LICENSE_FILEPATH,
                                                             NODE_LICENSE_FILENAME);

            if (result)
            {
                this.nodeLicense = nodeLicense;
            }

            return result;
        }
        catch (Exception e)
        {
            Logger.error(e.getMessage());
            return false;
        }
    }

    public NodeLicense getLicense()
    {
        return decryptLicenseFile();
    }

    public boolean setLicense(NodeLicense nodeLicense)
    {
        return encryptLicenseFile(nodeLicense);
    }

    public void setStatus(LicenseStatus licenseStatus)
    {
        NodeLicense license = getLicense();
        license.status = licenseStatus;
        setLicense(license);
    }
}
