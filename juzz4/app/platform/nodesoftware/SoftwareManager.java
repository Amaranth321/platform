package platform.nodesoftware;

import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidFormatException;
import models.MongoDeviceModel;
import models.SoftwareUpdateFile;
import play.modules.morphia.Model;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum SoftwareManager
{
    INSTANCE;

    public static final String UNSET_NODE_VERSION = "0.0.0.0";
    public static final long NODE_UBUNTU_MODEL_ID = 115L;
    public static final long NODE_ONE_AMEGIA_MODEL_ID = 117L;

    public static SoftwareManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * versions must be of the valid format (x.x.x.x) with digits.
     * Exception will be thrown otherwise.
     *
     * @param nodeVer1
     * @param nodeVer2
     *
     * @return positive if nodeVer1 is bigger
     */
    public int compareVersions(String nodeVer1, String nodeVer2)
    {
        int[] split1 = splitNodeVersion(nodeVer1);
        int[] split2 = splitNodeVersion(nodeVer2);

        for (int i = 0; i < split1.length; i++)
        {
            int digit1 = split1[i];
            int digit2 = split2[i];

            if (digit1 != digit2)
            {
                return digit1 - digit2;
            }
        }

        return 0;
    }

    public boolean isValidNodeVersion(String version)
    {
        String pattern = "\\d+.\\d+.\\d+.\\d+";
        return version != null && version.matches(pattern);
    }

    public int[] splitNodeVersion(String version)
    {
        if (!isValidNodeVersion(version))
        {
            throw new InvalidFormatException(version);
        }

        String[] split = version.split("\\.");
        if (split.length != 4)
        {
            throw new InvalidFormatException(version);
        }

        int[] splitDigits = new int[split.length];
        for (int i = 0; i < split.length; i++)
        {
            splitDigits[i] = Integer.parseInt(split[i]);
        }

        return splitDigits;
    }

    public double getReleaseNumber(String nodeVersion)
    {
        int[] digits = splitNodeVersion(nodeVersion);
        return Double.parseDouble(digits[0] + "." + digits[1]);
    }

    public void verifyUploadedFileVersion(long modelId, String version) throws ApiException
    {
        MongoDeviceModel deviceModel = MongoDeviceModel.getByModelId(modelId + "");
        if (deviceModel == null)
        {
            throw new ApiException(String.format("Unsupported device model (id=%s)", modelId));
        }

        //check if already uploaded
        Model.MorphiaQuery query = SoftwareUpdateFile.q().filter("modelId", modelId).filter("version", version);
        if (query.count() > 0)
        {
            throw new ApiException(String.format("Delete the existing %s file first", version));
        }
    }

    public boolean isLatest(long modelId, String version)
    {
        SoftwareUpdateFile cloudLatest = SoftwareUpdateFile.getLatestUploaded(modelId);
        if (cloudLatest == null)
        {
            return true;
        }
        return compareVersions(cloudLatest.getVersion(), version) == 0;
    }
}
